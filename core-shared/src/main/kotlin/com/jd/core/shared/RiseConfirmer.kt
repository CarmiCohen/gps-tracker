package com.jd.core.shared

/**
 * The debouncing of a sudden rise, shared by the noise ([AcousticDetector]) and the light ([LightDetector]) detection
 * (SIMP-NOV22.4-S2). Their [RiseDetector] (SIMP-NOV22.13-S1) feeds it spans: a value that held for a duration up to
 * a time, with the [LearnedBackground] at that time. A rise alerts when its rising spans add up to [confirmMs] within
 * [windowMs] of the start of its first one, measured against the background at that first span ([alertReference]), so
 * the background rising during the rise does not hide it. After an alert a span must fall below the limit before a
 * new rise counts, so one rise raises one alert; the time between alerts is the Sensor's per-kind cool-down
 * ([AlertCoolDown]).
 *
 * [isRise] tells whether a value is a rise over a given background. Not thread-safe: the detector's thread owns it.
 */
class RiseConfirmer(
    private val confirmMs: Long,
    private val windowMs: Long,
    private val isRise: (value: Double, reference: Double) -> Boolean
) {
    /** The background the last alert was compared against. Null before the first alert. */
    var alertReference: Double? = null
        private set

    private var sinceMs: Long? = null // The start of the first rising span of the rise being confirmed.
    private var reference = 0.0 // The background at that span.
    private var riseMs = 0L // The rising time since then.
    private var needsQuiet = false // After an alert, until a span falls below the limit.

    /** Returns true when [value], held for [durationMs] up to [endMs] over the background [floor], completes an alert. */
    fun onSpan(value: Double, endMs: Long, durationMs: Long, floor: Double): Boolean {
        if (durationMs <= 0L) return false
        if (needsQuiet) {
            if (!isRise(value, floor)) needsQuiet = false
            return false
        }
        if (sinceMs?.let { endMs - it > windowMs } == true) sinceMs = null // Too short: a new start.
        if (sinceMs == null) {
            if (!isRise(value, floor)) return false
            sinceMs = endMs - durationMs
            reference = floor
            riseMs = 0L
        }
        if (isRise(value, reference)) riseMs += durationMs
        if (riseMs < confirmMs) return false
        sinceMs = null
        needsQuiet = true
        alertReference = reference
        return true
    }
}
