package com.jd.app.monitor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** How the siren sounds, taken from the Siren & Audio settings. */
data class SirenSound(
    val waveType: String,
    val gain: Float,
    val useAlarmStream: Boolean,
    val forceMaxVolume: Boolean,
    val vibrate: Boolean
)

/**
 * "Force Maximum" wins over the custom volume, as the settings screen states. Overriding silent mode plays on the
 * alarm stream, which the ringer switch does not mute; otherwise the siren follows the notification stream.
 */
fun AlertSettings.sirenSound(waveType: String) = SirenSound(
    waveType = waveType,
    gain = if (!useMaxVolume && useCustomVolume) alarmVolume.coerceIn(0f, 1f) else 1f,
    useAlarmStream = overrideSilence || useMaxVolume,
    forceMaxVolume = useMaxVolume,
    vibrate = vibrationEnabled
)

private const val SAMPLE_RATE = 22_050

/** One loop of the siren wave as 16-bit mono samples: Siren sweeps, Chimes alternates two tones, Pulse beeps. */
fun sirenWave(waveType: String): ShortArray = when (waveType) {
    "Chimes" -> tones(listOf(880.0, 660.0), toneSeconds = 0.4, decay = true)
    "Pulse" -> tones(listOf(1_000.0, 0.0), toneSeconds = 0.25, decay = false)
    else -> sweep(lowHz = 600.0, highHz = 1_400.0, seconds = 2.0)
}

private fun sweep(lowHz: Double, highHz: Double, seconds: Double): ShortArray {
    val count = (SAMPLE_RATE * seconds).toInt()
    var phase = 0.0
    return ShortArray(count) { i ->
        val position = i.toDouble() / count
        val rise = if (position < 0.5) position * 2 else (1 - position) * 2
        phase += 2 * PI * (lowHz + (highHz - lowHz) * rise) / SAMPLE_RATE
        (sin(phase) * Short.MAX_VALUE * 0.9).toInt().toShort()
    }
}

private fun tones(frequencies: List<Double>, toneSeconds: Double, decay: Boolean): ShortArray {
    val perTone = (SAMPLE_RATE * toneSeconds).toInt()
    return ShortArray(perTone * frequencies.size) { i ->
        val hz = frequencies[i / perTone]
        val t = (i % perTone).toDouble() / SAMPLE_RATE
        val envelope = if (decay) exp(-4 * t / toneSeconds) else 1.0
        (sin(2 * PI * hz * t) * envelope * Short.MAX_VALUE * 0.9).toInt().toShort()
    }
}

/**
 * Plays the siren in a loop until [stop]. Only the Monitor has this class: the Sensor never sounds (Rule 14, R872).
 * A forced maximum volume is restored when the siren stops.
 */
@Singleton
class SirenPlayer @Inject constructor(@ApplicationContext private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null
    private var restoreAlarmVolume: Int? = null
    private var playing: SirenSound? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** The sound now playing, or null. */
    val currentSound: SirenSound? @Synchronized get() = playing

    /** The play state of the audio track, for checks on the device. */
    val trackPlayState: Int? @Synchronized get() = track?.playState

    @Synchronized
    fun play(sound: SirenSound) {
        if (sound == playing) return
        stop()
        val attributes = AudioAttributes.Builder()
            .setUsage(if (sound.useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            if (sound.forceMaxVolume) {
                restoreAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            }
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .build()
                .also { audioManager.requestAudioFocus(it) }

            val wave = sirenWave(sound.waveType)
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(wave.size * 2)
                .build()
                .apply {
                    write(wave, 0, wave.size)
                    setLoopPoints(0, wave.size, -1)
                    setVolume(sound.gain)
                    play()
                }
            if (sound.vibrate) vibrate()
            playing = sound
            _isPlaying.value = true
            SystemLogger.i("Siren", "Siren started: ${sound.waveType}, alarm stream ${sound.useAlarmStream}, max ${sound.forceMaxVolume}")
        } catch (e: Exception) {
            SystemLogger.e("Siren", "Siren failed to start: ${e.message}")
            stop()
        }
    }

    @Synchronized
    fun stop() {
        val wasPlaying = playing != null
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
        vibrator?.cancel()
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
        restoreAlarmVolume?.let { runCatching { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        restoreAlarmVolume = null
        playing = null
        _isPlaying.value = false
        if (wasPlaying) SystemLogger.i("Siren", "Siren stopped")
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator?.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
