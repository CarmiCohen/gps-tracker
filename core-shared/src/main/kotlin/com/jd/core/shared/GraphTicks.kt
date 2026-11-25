package com.jd.core.shared

import java.util.TimeZone

/** A tick on the Graphs' time axis: its place in the period (0 = start, 1 = end) and its time. */
data class GraphTick(val fraction: Float, val timeMs: Long)

/** The tick step of each period: about six segments (ISSUE-NOV25.17-O143). */
fun ViewPeriod.tickStepMs(): Long = when (this) {
    ViewPeriod.HOUR -> 10 * 60_000L
    ViewPeriod.SIX_HOURS -> 3_600_000L
    ViewPeriod.DAY -> 4 * 3_600_000L
    ViewPeriod.WEEK -> 24 * 3_600_000L
}

/**
 * The time axis ticks of the Graphs (ISSUE-NOV25.17-O143), on whole local times so the labels read as round clock
 * times: 1 h every 10 minutes, 6 h every hour, 24 h every 4 hours (00, 04, 08...) and 7 d every midnight.
 * [offsetMs] gives the phone's UTC offset at a time (daylight saving included).
 */
fun graphTicks(
    period: ViewPeriod,
    startMs: Long,
    endMs: Long,
    offsetMs: (Long) -> Int = { TimeZone.getDefault().getOffset(it) }
): List<GraphTick> {
    if (endMs <= startMs) return emptyList()
    val step = period.tickStepMs()
    val ticks = mutableListOf<GraphTick>()
    // Local wall-clock time of the start, rounded up to the next whole step.
    var local = Math.floorDiv(startMs + offsetMs(startMs) + step - 1, step) * step
    while (true) {
        // Back to UTC with the offset at that time; a tick at most a step past the end ends the loop.
        val utc = local - offsetMs(local - offsetMs(local))
        if (utc > endMs) break
        if (utc >= startMs) ticks += GraphTick((utc - startMs).toFloat() / (endMs - startMs), utc)
        local += step
    }
    return ticks
}
