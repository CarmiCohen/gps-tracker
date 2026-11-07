package com.jd.app.sensor

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SentinelLogic

/**
 * Runs [SentinelLogic.validateMovement] once per new GPS fix, against the previous fix, timed by the fixes' own clock
 * (ISSUE-NOV7.17-O42). The sensing loop sends the newest fix again on every tick (100 ms to 10 s) while fixes arrive
 * about every 5 s, so checking each tick divided the drift between two fixes by one tick and reported a phone lying
 * still as `JUMP` or `OUTLIER`.
 */
class MovementCheck {
    private var lastFix: LocationUpdate? = null

    /**
     * The verdict for [fix], whose [LocationUpdate.timestamp] is the fix time. Returns null when [fix] is not newer than
     * the last checked fix, because a fix sent again is not a movement. The first fix has nothing to compare with.
     */
    fun check(fix: LocationUpdate, hasPhysicalMotion: Boolean): SentinelLogic.ValidationResult? {
        val previous = lastFix
        if (previous != null && fix.timestamp <= previous.timestamp) return null
        lastFix = fix.snapshot()
        return if (previous == null) {
            SentinelLogic.ValidationResult(SentinelLogic.SentinelStatus.VALID)
        } else {
            SentinelLogic.validateMovement(previous, fix, hasPhysicalMotion)
        }
    }
}

/** The fields [SentinelLogic.validateMovement] reads, timed by the fix's monotonic clock, so a wall clock change cannot fake a speed. */
fun fixForMovementCheck(lat: Double, lng: Double, accuracy: Float, speed: Float, elapsedRealtimeNanos: Long) =
    LocationUpdate(lat = lat, lng = lng, accuracy = accuracy, speed = speed, timestamp = elapsedRealtimeNanos / 1_000_000L)
