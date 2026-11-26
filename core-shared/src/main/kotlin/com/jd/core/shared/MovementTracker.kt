package com.jd.core.shared

/**
 * Whether a phone is MOVING or STATIONARY, from its fixes, debounced (ISSUE-NOV25.25-O162, user decision 2026-11-26).
 * Until then one fix's speed over 1.5 km/h said MOVING, which a resting phone's GPS noise almost always did.
 *
 * MOVING needs [MOVING_MIN_FIXES] fixes in a row faster than [MOVING_ABOVE_MPS], spanning [MOVING_MIN_SPAN_MS] or more,
 * the last one farther from the first than either fix's accuracy and than [MOVING_MIN_DISTANCE_M] (the GPS error rule
 * of [SentinelLogic.validateMovement]). STATIONARY again once the fixes have been slower for [STATIONARY_AFTER_MS]. GPS
 * only (user decision: no accelerometer). A fix without a speed counts as slow. A fix not newer than the last one is
 * ignored, so a telemetry row repeating its fix may be fed again. The one rule for the Sensor's row clock and the
 * MOVING label of both apps. Not thread-safe: one thread feeds it.
 */
class MovementTracker {
    private class Fix(val timeMs: Long, val lat: Double, val lng: Double, val accuracyM: Float)

    var isMoving = false
        private set

    private var lastTimeMs = Long.MIN_VALUE
    private var runStart: Fix? = null // The first fast fix of the run that may make MOVING.
    private var runFixes = 0
    private var slowSinceMs: Long? = null // While MOVING: the first slow fix of the current slow run.

    /** Feeds one fix ([accuracyM] 0 when unknown, [speedMps] null when not reported); returns [isMoving]. */
    fun onFix(timeMs: Long, lat: Double, lng: Double, accuracyM: Float, speedMps: Float?): Boolean {
        if (timeMs <= lastTimeMs) return isMoving
        lastTimeMs = timeMs
        val isFast = speedMps != null && speedMps > MOVING_ABOVE_MPS
        if (isMoving) {
            if (isFast) {
                slowSinceMs = null
            } else {
                val since = slowSinceMs ?: timeMs.also { slowSinceMs = it }
                if (timeMs - since >= STATIONARY_AFTER_MS) {
                    isMoving = false
                    slowSinceMs = null
                }
            }
            return isMoving
        }
        if (!isFast) {
            runStart = null
            return false
        }
        val start = runStart ?: Fix(timeMs, lat, lng, accuracyM).also { runStart = it; runFixes = 0 }
        runFixes++
        val distanceM = SpatialMath.calculateDistance(start.lat, start.lng, lat, lng)
        val errorM = maxOf(start.accuracyM.toDouble(), accuracyM.toDouble(), MOVING_MIN_DISTANCE_M)
        if (runFixes >= MOVING_MIN_FIXES && timeMs - start.timeMs >= MOVING_MIN_SPAN_MS && distanceM > errorM) {
            isMoving = true
            runStart = null
        }
        return isMoving
    }
}

/** A fix faster than this (1.5 km/h) is a moving one (ISSUE-NOV25.25-O162; the HUD's limit, the row clock had 0.5 m/s). */
const val MOVING_ABOVE_MPS = 1.5f / 3.6f

/** Fast fixes in a row that may make MOVING (ISSUE-NOV25.25-O162). */
const val MOVING_MIN_FIXES = 3

/** ...spanning at least this long. */
const val MOVING_MIN_SPAN_MS = 5_000L

/** ...and moved at least this far (m), and beyond both fixes' accuracy. */
const val MOVING_MIN_DISTANCE_M = 10.0

/** Slow fixes for this long make STATIONARY again. */
const val STATIONARY_AFTER_MS = 10_000L
