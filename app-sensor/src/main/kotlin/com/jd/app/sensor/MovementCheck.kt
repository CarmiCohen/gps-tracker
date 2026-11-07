package com.jd.app.sensor

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SentinelLogic

/**
 * Runs [SentinelLogic.validateMovement] once per new GPS fix, against the previous fix, timed by the fixes' own clock
 * (ISSUE-NOV7.17-O42). The sensing loop sends the newest fix again on every tick (100 ms to 10 s) while fixes arrive
 * about every 5 s, so checking each tick divided the drift between two fixes by one tick and reported a phone lying
 * still as `JUMP` or `OUTLIER`.
 *
 * A fix coarser than [MOVEMENT_CHECK_MAX_ACCURACY_M], or without an accuracy, is neither judged nor kept as the
 * reference (ISSUE-NOV7.19-O43): when the GPS came back after a loss, the A15's first fix was a cell fix 3352 m off at
 * 700 m accuracy, reported as `JUMP`, and the GPS fix after it as `OUTLIER 488 m/s`, on a phone lying still.
 */
class MovementCheck {
    private var lastFix: LocationUpdate? = null

    /**
     * The verdict for [fix], whose [LocationUpdate.timestamp] is the fix time. Returns null when [fix] is not newer than
     * the last checked fix, because a fix sent again is not a movement, and when [fix] is too coarse to tell a movement.
     * The first usable fix has nothing to compare with.
     */
    fun check(fix: LocationUpdate, hasPhysicalMotion: Boolean): SentinelLogic.ValidationResult? {
        val previous = lastFix
        if (previous != null && fix.timestamp <= previous.timestamp) return null
        if (fix.accuracy <= 0f || fix.accuracy > MOVEMENT_CHECK_MAX_ACCURACY_M) return null
        lastFix = fix.snapshot()
        return if (previous == null) {
            SentinelLogic.ValidationResult(SentinelLogic.SentinelStatus.VALID)
        } else {
            SentinelLogic.validateMovement(previous, fix, hasPhysicalMotion)
        }
    }
}

/**
 * The coarsest fix that can tell a movement. Wi-Fi and GPS fixes on the A15 stay well inside it (14 m to 83 m seen);
 * cell fixes (700 m seen) do not, and their error can reach kilometres.
 */
const val MOVEMENT_CHECK_MAX_ACCURACY_M = 100f

/** The fields [SentinelLogic.validateMovement] reads, timed by the fix's monotonic clock, so a wall clock change cannot fake a speed. */
fun fixForMovementCheck(lat: Double, lng: Double, accuracy: Float, speed: Float, elapsedRealtimeNanos: Long) =
    LocationUpdate(lat = lat, lng = lng, accuracy = accuracy, speed = speed, timestamp = elapsedRealtimeNanos / 1_000_000L)
