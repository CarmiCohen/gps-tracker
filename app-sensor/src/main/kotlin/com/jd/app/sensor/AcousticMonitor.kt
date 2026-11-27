package com.jd.app.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.jd.core.shared.ACOUSTIC_FRAME_MS
import com.jd.core.shared.ACOUSTIC_JUMP_DB
import com.jd.core.shared.AcousticDetector
import com.jd.core.shared.PeakTracker
import com.jd.core.shared.SystemLogger
import com.jd.core.shared.acousticLevelDbfs
import com.jd.core.shared.noiseAlertLimitDbfs
import kotlin.math.max

/**
 * Listens to the Sensor's microphone on its own thread and reports each sudden loud noise found by [AcousticDetector]
 * (ISSUE-NOV5.2-O11 Group 4). Only sound levels are computed; no audio is stored or sent. Only the Sensor listens:
 * the Monitor never opens a microphone (user decision, 2026-11-08; `MonitorMicrophoneGuardTest`).
 */
class AcousticMonitor(
    /** [referenceDbfs] is the background the noise was compared against (ISSUE-NOV18.9-O120). */
    private val onAlert: (levelDbfs: Double, referenceDbfs: Double) -> Unit,
    private val onState: (MicrophoneState) -> Unit
) {
    private val lock = Any()
    private var thread: Thread? = null
    @Volatile private var isRunning = false

    /**
     * True while the phone itself rings, notifies, or plays an alarm ([OwnVibrationWatch]); its own sound is no
     * intruder, so no alert is raised (ISSUE-NOV9.4-O66). The detector still sees the levels.
     */
    @Volatile var isOwnSoundActive = false

    /** The jump above the background that alerts, from the Monitor's `Noise Sensitivity Threshold` slider (ISSUE-NOV10.8-O83). */
    @Volatile var jumpDb: Double = ACOUSTIC_JUMP_DB

    /** The newest frame's level and the learned background in dBFS, null while not listening (ISSUE-NOV10.8-O84 part 2). */
    @Volatile var levelDbfs: Double? = null
        private set
    @Volatile var floorDbfs: Double? = null
        private set

    /** The level that alerts now, null while not listening (ISSUE-NOV25.22-O144). */
    val limitDbfs: Double? get() = floorDbfs?.let { noiseAlertLimitDbfs(it, jumpDb) }

    /** Every frame's level, so the Graphs keep the loudest of each minute (ISSUE-NOV10.8-O84 part 3). */
    val levelPeaks = PeakTracker()

    /** Starts listening; does nothing while already listening. */
    fun start(context: Context) {
        synchronized(lock) {
            if (thread?.isAlive == true) return
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                onState(MicrophoneState.NO_PERMISSION)
                return
            }
            isRunning = true
            thread = Thread(::listen, "AcousticMonitor").apply { start() }
        }
    }

    fun stop() {
        synchronized(lock) {
            isRunning = false
            thread?.join(STOP_WAIT_MS) // A read returns within one frame.
            thread = null
            levelDbfs = null
            floorDbfs = null
        }
    }

    @SuppressLint("MissingPermission") // Checked in start().
    private fun listen() {
        val frameSamples = (SAMPLE_RATE * ACOUSTIC_FRAME_MS / 1000).toInt()
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            SystemLogger.e("Acoustic", "No microphone buffer size ($minBuffer)")
            onState(MicrophoneState.UNAVAILABLE)
            return
        }
        val record = try {
            // VOICE_RECOGNITION has no automatic gain, so a loud noise keeps its jump over the background.
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuffer, frameSamples * 2 * 2)
            )
        } catch (e: Exception) {
            SystemLogger.e("Acoustic", "Microphone open failed: ${e.message}")
            onState(MicrophoneState.UNAVAILABLE)
            return
        }
        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                SystemLogger.e("Acoustic", "Microphone not initialized")
                onState(MicrophoneState.UNAVAILABLE)
                return
            }
            record.startRecording()
            SystemLogger.i("Acoustic", "Listening (jump ${jumpDb.toInt()} dB above background)")
            val detector = AcousticDetector(jumpDb = jumpDb)
            val buffer = ShortArray(frameSamples)
            var silentSinceMs: Long? = null
            var state: MicrophoneState? = null
            fun report(next: MicrophoneState) {
                if (next != state) {
                    state = next
                    onState(next)
                    if (next == MicrophoneState.SILENCED) SystemLogger.w("Acoustic", "The microphone delivers only silence")
                }
            }
            report(MicrophoneState.LISTENING)
            while (isRunning) {
                val read = record.read(buffer, 0, frameSamples)
                if (read < 0) {
                    SystemLogger.e("Acoustic", "Microphone read failed ($read)")
                    report(MicrophoneState.UNAVAILABLE)
                    break
                }
                val nowMs = SystemClock.elapsedRealtime()
                val level = acousticLevelDbfs(buffer, read)
                levelDbfs = level
                if (level == null) {
                    val since = silentSinceMs ?: nowMs.also { silentSinceMs = it }
                    if (nowMs - since >= SILENCED_AFTER_MS) report(MicrophoneState.SILENCED)
                    continue
                }
                silentSinceMs = null
                levelPeaks.offer(level)
                report(MicrophoneState.LISTENING)
                val floor = detector.floorDbfs
                floorDbfs = floor
                detector.jumpDb = jumpDb // A slider change applies at once, without relearning the background.
                if (detector.onLevel(level, nowMs) && !isOwnSoundActive) detector.alertReferenceDbfs?.let { onAlert(level, it) }
            }
        } catch (e: Exception) {
            SystemLogger.e("Acoustic", "Microphone error: ${e.message}")
            onState(MicrophoneState.UNAVAILABLE)
        } finally {
            levelDbfs = null
            floorDbfs = null
            try { record.stop() } catch (_: Exception) {}
            record.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val STOP_WAIT_MS = 1_000L
        const val SILENCED_AFTER_MS = 5_000L
    }
}
