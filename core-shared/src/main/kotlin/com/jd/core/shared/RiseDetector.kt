package com.jd.core.shared

/**
 * The steps the noise ([AcousticDetector]) and the light ([LightDetector]) detection share (SIMP-NOV22.13-S1): the
 * first value starts the [LearnedBackground]; every later span is handed to the [RiseConfirmer] (unless the background
 * is still warming up), measured against the background before the span, and then followed by the background. The
 * detectors keep only their span (a 100 ms audio frame, or the light reading held since the last call) and their rise
 * test, [isRise], which tells whether a value is a rise over a given background.
 *
 * Not thread-safe: the detector's thread owns it.
 */
class RiseDetector(
    confirmMs: Long,
    windowMs: Long,
    warmUpMs: Long = BACKGROUND_WARM_UP_MS,
    isRise: (value: Double, reference: Double) -> Boolean
) {
    private val background = LearnedBackground(warmUpMs)
    private val confirmer = RiseConfirmer(confirmMs, windowMs, isRise)

    /** The learned background, or null before the first value. */
    val backgroundLevel: Double? get() = background.level

    /** The background the last alert was compared against. Null before the first alert. */
    val alertReference: Double? get() = confirmer.alertReference

    /**
     * Feeds [value], held for [durationMs] up to [endMs]; returns true when it completes an alert. The first value only
     * starts the background at [endMs], whatever its duration.
     */
    fun onSpan(value: Double, endMs: Long, durationMs: Long): Boolean {
        val floor = background.level
        if (floor == null) {
            background.start(value, endMs)
            return false
        }
        val isAlert = !background.isWarmingUp(endMs) && confirmer.onSpan(value, endMs, durationMs, floor)
        background.follow(value, endMs, durationMs)
        return isAlert
    }
}
