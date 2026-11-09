package com.jd.core.shared

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detects a sudden loud noise in the Sensor's microphone levels (ISSUE-NOV5.2-O11 Group 4).
 *
 * A phone microphone measures relative to full scale (dBFS), not true sound pressure, so the trigger is a jump of
 * [jumpDb] above the learned background level ([floorDbfs]), not an absolute loudness (user decision, 2026-11-08: 30 dB).
 * The background follows quiet frames within about a second and loud frames only over about ten seconds, so a
 * noise that goes on (a TV, rain) becomes background instead of raising alerts again and again.
 *
 * Feed one level per audio frame of about [ACOUSTIC_FRAME_MS], timed by a monotonic clock. Not thread-safe: one
 * audio thread owns it.
 */
class AcousticDetector(
    private val jumpDb: Double = ACOUSTIC_JUMP_DB,
    private val minAlertDbfs: Double = ACOUSTIC_MIN_ALERT_DBFS,
    private val warmUpMs: Long = ACOUSTIC_WARM_UP_MS,
    private val lockoutMs: Long = ACOUSTIC_LOCKOUT_MS
) {
    /** The learned background level, or null before the first frame. */
    var floorDbfs: Double? = null
        private set

    private var startMs = 0L
    private var lastAlertMs: Long? = null

    /** Returns true when [levelDbfs] at [nowMs] is an alert. */
    fun onLevel(levelDbfs: Double, nowMs: Long): Boolean {
        val floor = floorDbfs
        if (floor == null) {
            floorDbfs = levelDbfs
            startMs = nowMs
            return false
        }
        val isWarmingUp = nowMs - startMs < warmUpMs
        val isAlert = !isWarmingUp &&
            levelDbfs - floor >= jumpDb &&
            levelDbfs >= minAlertDbfs &&
            lastAlertMs.let { it == null || nowMs - it >= lockoutMs }
        if (isAlert) lastAlertMs = nowMs
        val alpha = when {
            isWarmingUp -> WARM_UP_ALPHA
            levelDbfs < floor -> FALL_ALPHA
            else -> RISE_ALPHA
        }
        floorDbfs = floor + alpha * (levelDbfs - floor)
        return isAlert
    }

    private companion object {
        const val WARM_UP_ALPHA = 0.2
        const val FALL_ALPHA = 0.1 // About 1 s at 100 ms frames.
        const val RISE_ALPHA = 0.01 // About 10 s at 100 ms frames.
    }
}

/** The jump above the background that counts as an alert (user decision, 2026-11-08). */
const val ACOUSTIC_JUMP_DB = 30.0

/** A jump that stays quieter than this is ignored, so a microphone near digital silence cannot alert on a whisper. */
const val ACOUSTIC_MIN_ALERT_DBFS = -50.0

/** No alerts while the background is first learned. */
const val ACOUSTIC_WARM_UP_MS = 3_000L

/** One noise raises one alert: the next can follow only this much later. */
const val ACOUSTIC_LOCKOUT_MS = 10_000L

/** The length of one audio frame the detector is fed. */
const val ACOUSTIC_FRAME_MS = 100L

/** The quietest level reported for a frame that is not all zeros. */
const val ACOUSTIC_MIN_LEVEL_DBFS = -90.0

/**
 * The level of the first [count] 16-bit samples of [samples] in dBFS, or null when they are all zero: Android delivers
 * zeros when it silences the microphone of an app in the background, and that is no sound, not silence.
 */
fun acousticLevelDbfs(samples: ShortArray, count: Int = samples.size): Double? {
    if (count <= 0) return null
    var sumSquares = 0.0
    for (i in 0 until count) {
        val s = samples[i].toDouble()
        sumSquares += s * s
    }
    if (sumSquares == 0.0) return null
    val rms = sqrt(sumSquares / count)
    return max(ACOUSTIC_MIN_LEVEL_DBFS, 20.0 * log10(rms / 32768.0))
}
