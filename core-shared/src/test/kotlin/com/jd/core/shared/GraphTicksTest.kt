package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Test

/** ISSUE-NOV25.17-O143: the Graphs' time axis ticks fall on whole local clock times. */
class GraphTicksTest {

    private val hour = 3_600_000L
    private val israel: (Long) -> Int = { 2 * 3_600_000 } // UTC+2, no daylight saving in late November
    private val midnightUtc = 1_795_000_000_000L / (24 * hour) * (24 * hour)

    private fun localHours(ticks: List<GraphTick>) = ticks.map { ((it.timeMs + 2 * hour) % (24 * hour)) / 60_000.0 / 60.0 }

    @Test
    fun aDayPeriodTicksEveryFourWholeLocalHours() {
        // 18:11 to 18:11 next day, local time.
        val start = midnightUtc + 16 * hour + 11 * 60_000L
        val ticks = graphTicks(ViewPeriod.DAY, start, start + 24 * hour, israel)
        assertEquals(listOf(20.0, 0.0, 4.0, 8.0, 12.0, 16.0), localHours(ticks))
        ticks.forEach { assertEquals((it.timeMs - start).toFloat() / (24 * hour), it.fraction, 1e-6f) }
    }

    @Test
    fun anHourPeriodTicksEveryTenMinutes() {
        val start = midnightUtc + 7 * 60_000L
        val ticks = graphTicks(ViewPeriod.HOUR, start, start + hour, israel)
        assertEquals(6, ticks.size)
        ticks.forEach { assertEquals(0L, it.timeMs % (10 * 60_000L)) }
    }

    @Test
    fun sixHoursTickHourlyAndAWeekAtLocalMidnight() {
        val start = midnightUtc + 30 * 60_000L
        assertEquals(6, graphTicks(ViewPeriod.SIX_HOURS, start, start + 6 * hour, israel).size)
        val week = graphTicks(ViewPeriod.WEEK, start, start + ViewPeriod.WEEK.durationMs, israel)
        week.forEach { assertEquals(0.0, localHours(listOf(it)).single(), 0.0) }
    }

    @Test
    fun aTickOnTheStartIsKept() {
        val start = midnightUtc - 2 * hour // local midnight
        assertEquals(start, graphTicks(ViewPeriod.DAY, start, start + 24 * hour, israel).first().timeMs)
    }
}
