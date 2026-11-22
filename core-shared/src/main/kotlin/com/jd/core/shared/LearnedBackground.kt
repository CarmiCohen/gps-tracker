package com.jd.core.shared

import kotlin.math.exp

/**
 * The background level a rise is measured against, learned the same way by the noise ([AcousticDetector]) and the
 * light ([LightDetector]) detection (SIMP-NOV22.10-S1). It is fed spans, like the [RiseConfirmer]: a value that held
 * for a duration up to a time. The background moves towards the value by `1 - exp(-duration / tau)`: quickly while it
 * is first learned ([warmUpMs], [BACKGROUND_WARM_UP_TAU_MS]), within about a second towards a lower value
 * ([BACKGROUND_FALL_TAU_MS]) and only over about ten seconds towards a higher one ([BACKGROUND_RISE_TAU_MS]), so a
 * level that goes on (a TV, a lamp left on) becomes background instead of raising alerts again and again, while a
 * short rise barely moves it.
 *
 * Not thread-safe: the detector's thread owns it.
 */
class LearnedBackground(private val warmUpMs: Long = BACKGROUND_WARM_UP_MS) {
    /** The learned background, or null before the first value. */
    var level: Double? = null
        private set

    private var startMs = 0L

    /** True until [warmUpMs] after the first value; no alerts while it is. */
    fun isWarmingUp(nowMs: Long): Boolean = level == null || nowMs - startMs < warmUpMs

    /** Starts the background at [value], the first one, at [nowMs]. */
    fun start(value: Double, nowMs: Long) {
        level = value
        startMs = nowMs
    }

    /** Moves the background towards [value], which held for [durationMs] up to [endMs]. Call [start] first. */
    fun follow(value: Double, endMs: Long, durationMs: Long) {
        val current = level ?: return
        if (durationMs <= 0L) return
        val tauMs = when {
            isWarmingUp(endMs) -> BACKGROUND_WARM_UP_TAU_MS
            value < current -> BACKGROUND_FALL_TAU_MS
            else -> BACKGROUND_RISE_TAU_MS
        }
        level = current + (1.0 - exp(-durationMs / tauMs)) * (value - current)
    }
}

/** No alerts while the background is first learned. */
const val BACKGROUND_WARM_UP_MS = 3_000L

/** How fast the background follows any value while it is first learned. */
const val BACKGROUND_WARM_UP_TAU_MS = 500.0

/** How fast the background follows a lower value: about a second. */
const val BACKGROUND_FALL_TAU_MS = 1_000.0

/** How fast the background follows a higher value: about ten seconds. */
const val BACKGROUND_RISE_TAU_MS = 10_000.0
