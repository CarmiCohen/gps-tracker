package com.jd.core.shared

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detects a sudden loud noise in the Sensor's microphone levels (ISSUE-NOV5.2-O11 Group 4).
 *
 * A phone microphone measures relative to full scale (dBFS), not true sound pressure, so the trigger is a jump of
 * [jumpDb] above the learned background level ([floorDbfs]), not an absolute loudness (user decision, 2026-11-08: 30 dB;
 * since ISSUE-NOV10.8-O83 set by the Monitor's `Noise Sensitivity Threshold` slider, [noiseJumpForSensitivity]).
 * The background follows quiet frames within about a second and loud frames only over about ten seconds, so a
 * noise that goes on (a TV, rain) becomes background instead of raising alerts again and again.
 *
 * A noise alerts only when it is loud for [ACOUSTIC_CONFIRM_MS] within [ACOUSTIC_CONFIRM_WINDOW_MS], measured against
 * the background at its first loud frame (ISSUE-NOV17.10-O111: a door slam or a dropped spoon is no intruder;
 * ISSUE-NOV17.12-O112: nor is a coughing fit). After
 * an alert the level must fall below the limit before a new noise counts, so one noise raises one alert; the time
 * between alerts is the Sensor's per-kind cool-down ([AlertCoolDown]), which replaced a lockout here. The
 * debouncing is the [RiseConfirmer] the light detection shares (SIMP-NOV22.4-S2), fed one frame as one span.
 *
 * Feed one level per audio frame of about [ACOUSTIC_FRAME_MS], timed by a monotonic clock. Not thread-safe: one
 * audio thread owns it.
 */
class AcousticDetector(
    @Volatile var jumpDb: Double = ACOUSTIC_JUMP_DB,
    private val minAlertDbfs: Double = ACOUSTIC_MIN_ALERT_DBFS,
    private val warmUpMs: Long = ACOUSTIC_WARM_UP_MS
) {
    /** The learned background level, or null before the first frame. */
    var floorDbfs: Double? = null
        private set

    /**
     * The background the last alert was compared against: the one at its noise's first loud frame, not [floorDbfs],
     * which rises during the noise (ISSUE-NOV18.9-O120). Null before the first alert.
     */
    val alertReferenceDbfs: Double? get() = confirmer.alertReference

    private var startMs = 0L
    private val confirmer = RiseConfirmer(ACOUSTIC_CONFIRM_MS, ACOUSTIC_CONFIRM_WINDOW_MS) { level, reference ->
        level - reference >= jumpDb && level >= minAlertDbfs
    }

    /** Returns true when [levelDbfs] at [nowMs] is an alert. */
    fun onLevel(levelDbfs: Double, nowMs: Long): Boolean {
        val floor = floorDbfs
        if (floor == null) {
            floorDbfs = levelDbfs
            startMs = nowMs
            return false
        }
        val isWarmingUp = nowMs - startMs < warmUpMs
        val isAlert = !isWarmingUp && confirmer.onSpan(levelDbfs, nowMs, ACOUSTIC_FRAME_MS, floor)
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

/** The jump above the background that counts as an alert (user decision, 2026-11-08); the slider's middle. */
const val ACOUSTIC_JUMP_DB = 30.0

const val DEFAULT_NOISE_SENSITIVITY = 0.5f

/**
 * The `Noise Sensitivity Threshold` slider (0.1 to 1.0) as the jump above the background in dB, linear between three
 * points: 0.1 gives 45 dB, 0.5 the 30 dB of [ACOUSTIC_JUMP_DB], 1.0 gives 15 dB (ISSUE-NOV10.8-O83).
 */
fun noiseJumpForSensitivity(sensitivity: Float): Double {
    val s = sensitivity.coerceIn(0.1f, 1.0f).toDouble()
    return if (s <= 0.5) 45.0 + (ACOUSTIC_JUMP_DB - 45.0) * (s - 0.1) / 0.4 else ACOUSTIC_JUMP_DB + (15.0 - ACOUSTIC_JUMP_DB) * (s - 0.5) / 0.5
}

/** The noise slider's limit as the user sees it, for example `30 dB` (the limit itself, like ISSUE-NOV9.4-O67). */
fun noiseLimitLabel(sensitivity: Float): String = "${kotlin.math.round(noiseJumpForSensitivity(sensitivity)).toInt()} dB"

/**
 * The Sensor's noise alert line: the level, the background it was compared against ([AcousticDetector.alertReferenceDbfs])
 * and the jump between them, all rounded the same way, so the printed jump is the one that crossed the limit
 * (ISSUE-NOV18.9-O120: `-46 dBFS, background -73` read as a 27 dB jump under a 30 dB limit).
 */
fun acousticAlertText(levelDbfs: Double, referenceDbfs: Double): String =
    "Acoustic alert: level ${kotlin.math.round(levelDbfs).toInt()} dBFS, background ${kotlin.math.round(referenceDbfs).toInt()} dBFS, " +
        "jump ${kotlin.math.round(levelDbfs - referenceDbfs).toInt()} dB"

/** A jump that stays quieter than this is ignored, so a microphone near digital silence cannot alert on a whisper. */
const val ACOUSTIC_MIN_ALERT_DBFS = -50.0

/** No alerts while the background is first learned. */
const val ACOUSTIC_WARM_UP_MS = 3_000L

/**
 * How long a noise must be loud to alert (ISSUE-NOV17.10-O111); its loud frames may have gaps, like speech. 2 s since
 * ISSUE-NOV17.12-O112 (user decision, 2026-11-18): a coughing fit filled the old 1 s within 2 s.
 */
const val ACOUSTIC_CONFIRM_MS = 2_000L

/** The loud frames of [ACOUSTIC_CONFIRM_MS] must fall within this time from the first one. */
const val ACOUSTIC_CONFIRM_WINDOW_MS = 3_000L

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
