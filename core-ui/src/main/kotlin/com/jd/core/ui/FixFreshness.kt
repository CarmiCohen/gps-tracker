package com.jd.core.ui

/**
 * The one fix freshness rule (SIMP-NOV7.21-S1): how long a fix still counts as the current position. Three times the
 * 5 s interval the Sensor asks the fused provider for. After that the Sensor sends `NO_FIX` instead of the last fix
 * (ISSUE-NOV7.19-O43), its HUD drops the speed, and its GPS LED turns red, so the Sensor's screen and its telemetry
 * agree.
 */
const val FIX_CURRENT_MS = 15_000L

/**
 * True when a fix taken at [fixElapsedNanos] is younger than [FIX_CURRENT_MS] at [nowElapsedNanos]. Both are
 * `SystemClock.elapsedRealtimeNanos` values (`Location.elapsedRealtimeNanos` for the fix), so a wall clock change
 * cannot make an old fix look fresh. A fix stamped in the future (clock source mismatch) counts as fresh.
 */
fun isFixFresh(fixElapsedNanos: Long, nowElapsedNanos: Long): Boolean =
    nowElapsedNanos - fixElapsedNanos < FIX_CURRENT_MS * 1_000_000L
