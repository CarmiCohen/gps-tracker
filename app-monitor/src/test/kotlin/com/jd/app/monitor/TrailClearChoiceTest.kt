package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISSUE-NOV27.1-O167: the map's CLEAR keeps the last hours. A kept span deletes only the older rows and marks that
 * cutoff, so neither live rows nor backfill bring the deleted ones back; an earlier, later clear stays in force.
 */
class TrailClearChoiceTest {

    private val now = 1_764_000_000_000L
    private val hour = 3_600_000L

    @Test
    fun theDefaultKeepsTheLastDayOfBothTrails() {
        val choice = TrailClearChoice()
        assertEquals(TrailKeep.HOURS_24, choice.keep)
        assertEquals(TrailScope.BOTH, choice.scope)
        assertEquals(now - 24 * hour, choice.deleteBeforeMs(now))
    }

    @Test
    fun eachSpanCutsAtItsAge() {
        assertEquals(now - hour, TrailClearChoice(TrailKeep.HOUR_1).deleteBeforeMs(now))
        assertEquals(now - 6 * hour, TrailClearChoice(TrailKeep.HOURS_6).deleteBeforeMs(now))
        assertEquals(now - 7 * 24 * hour, TrailClearChoice(TrailKeep.DAYS_7).deleteBeforeMs(now))
        assertEquals("A span marks its cutoff", now - 6 * hour, TrailClearChoice(TrailKeep.HOURS_6).clearAtMs(now))
    }

    @Test
    fun keepingNothingDeletesEveryRowAndMarksNow() {
        assertEquals(Long.MAX_VALUE, TrailClearChoice.EVERYTHING.deleteBeforeMs(now))
        assertEquals("Rows arriving after the clear are kept", now, TrailClearChoice.EVERYTHING.clearAtMs(now))
    }

    @Test
    fun theMarkOfAKeptSpanStopsOnlyOlderRows() {
        val clear = null.raisedTo("T", TrailClearChoice(TrailKeep.HOURS_6).clearAtMs(now))
        assertFalse("A deleted row stays away", clear.keeps(row(now - 7 * hour)))
        assertTrue("A kept row is stored again", clear.keeps(row(now - 5 * hour)))
        assertTrue("Its start is no gap to request", clear.clearedStartOf("T"))
    }

    @Test
    fun anEarlierClearThatReachesLaterWins() {
        val everything = TrailClear("T", atMs = now - hour)
        assertEquals("Rows cleared an hour ago stay away", everything, everything.raisedTo("T", now - 24 * hour))
        assertEquals(TrailClear("T", now), everything.raisedTo("T", now))
        assertEquals("Another tracker's mark is replaced", TrailClear("T", now - 24 * hour), TrailClear("U", now).raisedTo("T", now - 24 * hour))
    }

    @Test
    fun theEventNamesTheSpanTheScopeAndTheRows() {
        assertEquals("Trails older than 6 h cleared (12,340 rows)", TrailClearChoice(TrailKeep.HOURS_6).eventText(12_340))
        assertEquals("Trails cleared (15,020 rows)", TrailClearChoice.EVERYTHING.eventText(15_020))
        assertEquals("Sensor trail older than 7 d cleared (1 rows)", TrailClearChoice(TrailKeep.DAYS_7, TrailScope.SENSOR).eventText(1))
        assertEquals("This phone's trail older than 1 h cleared (0 rows)", TrailClearChoice(TrailKeep.HOUR_1, TrailScope.OWN).eventText(0))
    }

    @Test
    fun theDialogCountReadsAsTheIssueAsks() {
        assertEquals("Deletes 12,340 of 15,020 rows", TrailClearCount(12_340, 15_020).text)
        assertEquals("No rows stored", TrailClearCount(0, 0).text)
    }

    private fun row(timestamp: Long) = TrailEntity("T", timestamp, timestamp, 1.0, 1.0, 5f, "VALID")
}
