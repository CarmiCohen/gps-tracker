package com.gps19.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

/**
 * AudioSynthesizer: Procedural audio generator for sirens and alerts.
 * Sep.03.25:
 * - Idea #240: ContextShadow Automation. Migrated to @Singleton class with 
 *   @ShadowContext injection to eliminate manual wrapper logic (R-ID 240).
 * Sep.02.50:
 * - Issue #005 Hardening: Replaced all android.util.Log calls with Timber 
 *   to ensure log spillage protection on Samsung A15/G990 hardware (R759).
 */
@Singleton
class AudioSynthesizer @Inject constructor(
    @ShadowContext private val shadowContext: Context
) {
    private val isLooping = AtomicBoolean(false)
    private val isForced = AtomicBoolean(false)
    private val silencedUntilRt = AtomicLong(0)
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sirenJob: Job? = null

    fun isPlaying(): Boolean = isLooping.get()
    fun isForced(): Boolean = isForced.get()
    
    fun getSilencedUntilRt(): Long = silencedUntilRt.get()

    fun playShortAlert() {
        scope.launch {
            try {
                for (i in 0 until 8) {
                    playNote(880.0, 0.1, decay = false, ignoreLooping = true, volume = 0.5f, overrideSilence = true)
                    delay(50)
                    playNote(1100.0, 0.15, decay = false, ignoreLooping = true, volume = 0.6f, overrideSilence = true)
                    delay(400)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Timber.e(e, "Error playing short alert")
                }
            }
        }
    }

    /**
     * Enhanced Siren Player
     */
    fun playSiren(
        type: String = "Siren", 
        force: Boolean = false, 
        volume: Float = 1.0f, 
        overrideSilence: Boolean = true, 
        loop: Boolean = true,
        vibrate: Boolean = false,
        timeProvider: TimeProvider,
        isTrackerMode: Boolean = false 
    ) {
        if (isTrackerMode) {
            Timber.d("Siren suppressed: Tracker mode (Stealth enforced)")
            return
        }

        if (!force && timeProvider.elapsedRealtime() < silencedUntilRt.get()) return
        
        if (!overrideSilence) {
            val am = shadowContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                Timber.d("Siren suppressed by silence setting")
                return
            }
        }

        isLooping.set(true)
        if (force) isForced.set(true)
        
        sirenJob?.cancel()
        val currentJob = scope.launch {
            val thisJob = coroutineContext[Job]
            var isAutoStopped = false
            try {
                Timber.d("Siren loop started: $type (force=$force, loop=$loop)")
                val startRt = timeProvider.elapsedRealtime()
                val vibrator = if (vibrate) shadowContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator else null

                while (isActive) {
                    val nowRt = timeProvider.elapsedRealtime()
                    val elapsed = nowRt - startRt
                    if (elapsed >= SIREN_AUTO_STOP_MS) {
                        isAutoStopped = true
                        break
                    }

                    val fadeInFactor = if (elapsed < SIREN_FADE_IN_DURATION_MS) {
                        elapsed.toFloat() / SIREN_FADE_IN_DURATION_MS
                    } else 1.0f
                    
                    val effectiveVolume = volume * fadeInFactor

                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(1000)
                        }
                    }

                    when (type) {
                        "Siren" -> playSirenCycle(effectiveVolume, overrideSilence, timeProvider)
                        "Chimes" -> playChimesCycle(effectiveVolume, overrideSilence, timeProvider)
                        "Pulse" -> playPulseCycle(effectiveVolume, overrideSilence, timeProvider)
                        else -> playSirenCycle(effectiveVolume, overrideSilence, timeProvider)
                    }
                    if (!loop) break
                    yield()
                }
            } catch (e: CancellationException) {
                Timber.d("Siren job cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Siren loop error")
            } finally {
                if (sirenJob == thisJob) {
                    isLooping.set(false)
                    isForced.set(false)
                    if (isAutoStopped) {
                        Timber.d("Siren auto-stopped (${SIREN_AUTO_STOP_MS/1000}s limit reached). Triggering cooldown.")
                        setSilence(SIREN_RESUME_COOLDOWN_MS, timeProvider)
                    }
                    Timber.d("Siren loop finished (flags reset)")
                }
            }
        }
        sirenJob = currentJob
    }

    private suspend fun playSirenCycle(volume: Float, overrideSilence: Boolean, timeProvider: TimeProvider): Long {
        val cycleDuration = 2.0 
        val numSamples = (SIREN_SAMPLE_RATE * cycleDuration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            if (!currentCoroutineContext().isActive) return 0L
            val t = i.toDouble() / SIREN_SAMPLE_RATE
            val progress = (i.toDouble() / numSamples)
            val modulation = sin(2.0 * PI * progress - PI/2) * 0.5 + 0.5 
            val freq = 600.0 + (modulation * 800.0)
            samples[i] = (sin(2.0 * PI * freq * t) * 0.8 * volume * Short.MAX_VALUE).toInt().toShort()
        }
        playBuffer(samples, overrideSilence, timeProvider)
        return (cycleDuration * 1000).toLong()
    }

    private suspend fun playChimesCycle(volume: Float, overrideSilence: Boolean, timeProvider: TimeProvider): Long {
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        var totalDuration = 0L
        for (freq in notes) {
            if (!currentCoroutineContext().isActive) return totalDuration
            playNote(freq, 0.5, decay = true, ignoreLooping = false, volume = volume, overrideSilence = overrideSilence, timeProvider = timeProvider)
            delay(100)
            totalDuration += 600L
        }
        if (currentCoroutineContext().isActive) {
            delay(1000)
            totalDuration += 1000L
        }
        return totalDuration
    }

    private suspend fun playPulseCycle(volume: Float, overrideSilence: Boolean, timeProvider: TimeProvider): Long {
        var totalDuration = 0L
        for (i in 0 until 3) {
            if (!currentCoroutineContext().isActive) return totalDuration
            playNote(1000.0, 0.2, decay = false, ignoreLooping = false, volume = volume, overrideSilence = overrideSilence, timeProvider = timeProvider)
            delay(200)
            totalDuration += 400L
        }
        if (currentCoroutineContext().isActive) {
            delay(1000)
            totalDuration += 1000L
        }
        return totalDuration
    }

    fun stopSiren(silenceDurationMs: Long = 300000, timeProvider: TimeProvider) {
        isLooping.set(false)
        isForced.set(false)
        sirenJob?.cancel()
        setSilence(silenceDurationMs, timeProvider)
        Timber.d("Siren stop requested")
    }

    private fun setSilence(durationMs: Long, timeProvider: TimeProvider) {
        if (durationMs > 0) {
            val newSilenceRt = timeProvider.elapsedRealtime() + durationMs
            while (true) {
                val current = silencedUntilRt.get()
                if (newSilenceRt <= current) break
                if (silencedUntilRt.compareAndSet(current, newSilenceRt)) break
            }
        }
    }

    private suspend fun playBuffer(samples: ShortArray, overrideSilence: Boolean, timeProvider: TimeProvider) = withContext(Dispatchers.Default) {
        if (!isActive) return@withContext
        var audioTrack: AudioTrack? = null
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(if (overrideSilence) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SIREN_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            val durationMs = (samples.size.toDouble() / SIREN_SAMPLE_RATE * 1000).toLong()
            val startRt = timeProvider.elapsedRealtime()
            while (isActive && timeProvider.elapsedRealtime() - startRt < durationMs) {
                delay(50)
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Timber.e(e, "Error in playBuffer")
            }
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {}
        }
    }

    private suspend fun playNote(frequency: Double, duration: Double, decay: Boolean, ignoreLooping: Boolean, volume: Float, overrideSilence: Boolean, timeProvider: TimeProvider? = null) = withContext(Dispatchers.Default) {
        if (!ignoreLooping && !isActive) return@withContext
        val count = (SIREN_SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SIREN_SAMPLE_RATE
            var amplitude = 0.8 * volume
            if (decay) amplitude *= exp(-5.0 * t / duration)
            samples[i] = (sin(2.0 * PI * frequency * t) * amplitude * Short.MAX_VALUE).toInt().toShort()
        }

        var audioTrack: AudioTrack? = null
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(if (overrideSilence) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SIREN_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(count * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, count)
            audioTrack.play()
            
            val durationMs = (duration * 1000).toLong()
            if (timeProvider != null) {
                val startRt = timeProvider.elapsedRealtime()
                while (isActive && timeProvider.elapsedRealtime() - startRt < durationMs) {
                    delay(10)
                }
            } else {
                delay(durationMs)
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Timber.e(e, "Error in playNote")
            }
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {}
        }
    }
}
