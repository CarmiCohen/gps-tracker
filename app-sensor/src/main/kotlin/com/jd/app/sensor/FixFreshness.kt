package com.jd.app.sensor

/**
 * How long the newest fix still counts as the current position (ISSUE-NOV7.19-O43): three times the 5 s interval
 * [SensorService] asks the fused provider for. After that the Sensor reports `NO_FIX` instead of sending the last fix
 * again as fresh, so the Monitor can raise `Tracker GPS signal loss` after a real loss, not only before the first fix.
 */
const val FIX_STALE_AFTER_MS = 15_000L

/** The status sent while the Sensor has no current fix. The Monitor's `hasGpsSignal` reads it as no position. */
const val NO_FIX_STATUS = "NO_FIX"

/**
 * True when a fix taken at [fixElapsedNanos] is younger than [FIX_STALE_AFTER_MS] at [nowElapsedNanos]. Both are
 * `SystemClock.elapsedRealtimeNanos` values (`Location.elapsedRealtimeNanos` for the fix), so a wall clock change
 * cannot make an old fix look fresh. A fix stamped in the future (clock source mismatch) counts as fresh.
 */
fun isFixFresh(fixElapsedNanos: Long, nowElapsedNanos: Long): Boolean =
    nowElapsedNanos - fixElapsedNanos < FIX_STALE_AFTER_MS * 1_000_000L
