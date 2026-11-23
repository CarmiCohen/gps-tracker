package com.jd.core.shared

import kotlin.math.round

/**
 * Detects a sudden rise of the ambient light at the Sensor (ISSUE-NOV22.4-O130, user request 2026-11-22): a lamp or a
 * torch switched on in a dark room, or the hidden phone uncovered. It works like the noise detection ([AcousticDetector]):
 * the light alerts when it reaches [riseRatio] times the learned background ([floorLux]) and at least [minRiseLux] more,
 * and stays there for [LIGHT_CONFIRM_MS] within [LIGHT_CONFIRM_WINDOW_MS], measured against the background just before
 * it, so a car's headlights sweeping past do not alert. A darkening never alerts (user decision 2026-11-22:
 * a hand or a pocket over the sensor would). The background is the [LearnedBackground] the noise detection shares
 * (SIMP-NOV22.10-S1): it follows a darker light within about a second and a brighter one only over about ten seconds,
 * so a light that stays on becomes background; after an alert the light must fall below the limit before a new rise
 * counts. The time between alerts is the Sensor's per-kind cool-down ([AlertCoolDown]).
 *
 * The light sensor reports only on a change, so a reading holds until the next one: feed each reading, and the held one
 * again at least every [LIGHT_TICK_MS], timed by a monotonic clock. Each call hands the reading that held since the
 * last one, as one span, to the [RiseConfirmer] and the background (SIMP-NOV22.4-S2), which run as one [RiseDetector]
 * (SIMP-NOV22.13-S1). Not thread-safe: one sensor thread owns it.
 */
class LightDetector(
    @Volatile var riseRatio: Double = LIGHT_RISE_RATIO,
    private val minRiseLux: Double = LIGHT_MIN_RISE_LUX,
    warmUpMs: Long = BACKGROUND_WARM_UP_MS
) {
    private val rise = RiseDetector(LIGHT_CONFIRM_MS, LIGHT_CONFIRM_WINDOW_MS, warmUpMs) { lux, reference ->
        lux - reference >= minRiseLux && lux >= reference * riseRatio
    }

    /** The learned background light, or null before the first reading. */
    val floorLux: Double? get() = rise.backgroundLevel

    /** The background the last alert was compared against: the one just before its rise. Null before the first alert. */
    val alertReferenceLux: Double? get() = rise.alertReference

    private var lastMs = 0L
    private var lastLux: Double? = null // The reading that held from lastMs until now; null before the first.

    /** Feeds [lux], which holds from [nowMs]; returns true when the reading held until [nowMs] completes an alert. */
    fun onLux(lux: Double, nowMs: Long): Boolean {
        // The first reading only starts the background; later, the held reading is the span that ends now.
        val isAlert = rise.onSpan(lastLux ?: lux, nowMs, (nowMs - lastMs).coerceAtLeast(0L))
        lastMs = nowMs
        lastLux = lux
        return isAlert
    }
}

/** The rise over the background that alerts, as a factor (user decision 2026-11-22: 4 times); the slider's middle. */
const val LIGHT_RISE_RATIO = 4.0

const val DEFAULT_LIGHT_SENSITIVITY = 0.5f

/** A rise smaller than this never alerts, so a dark room's sensor flicker of a few lux is no light switched on. */
const val LIGHT_MIN_RISE_LUX = 20.0

/** How long a rise must stay bright to alert: the 2 s of the noise detection (user decision 2026-11-22). */
const val LIGHT_CONFIRM_MS = 2_000L

/** The bright time of [LIGHT_CONFIRM_MS] must fall within this time from the first bright reading. */
const val LIGHT_CONFIRM_WINDOW_MS = 3_000L

/** The held reading is fed again at least this often, since the light sensor reports only on a change. */
const val LIGHT_TICK_MS = 250L

/**
 * The `Light Sensitivity Threshold` slider (0.1 to 1.0) as the rise factor over the background, linear between three
 * points: 0.1 gives 8 times, 0.5 the 4 times of [LIGHT_RISE_RATIO], 1.0 gives 2 times (ISSUE-NOV22.4-O130).
 */
fun lightRatioForSensitivity(sensitivity: Float): Double {
    val s = sensitivity.coerceIn(0.1f, 1.0f).toDouble()
    return if (s <= 0.5) 8.0 + (LIGHT_RISE_RATIO - 8.0) * (s - 0.1) / 0.4 else LIGHT_RISE_RATIO + (2.0 - LIGHT_RISE_RATIO) * (s - 0.5) / 0.5
}

/** The light slider's limit as the user sees it, for example `4×` or `3.2×`. */
fun lightLimitLabel(sensitivity: Float): String = "${factorText(lightRatioForSensitivity(sensitivity))}×"

private fun factorText(factor: Double): String {
    val tenths = round(factor * 10).toInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

/** The Sensor's light alert line: the light, the background it was compared against, and the factor between them. */
fun lightAlertText(lux: Double, referenceLux: Double): String {
    val factor = if (referenceLux > 0.0) ", ${factorText(lux / referenceLux)}×" else ""
    return "Light alert: ${round(lux).toInt()} lx, background ${round(referenceLux).toInt()} lx$factor"
}
