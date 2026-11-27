package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV27.1-O168: older trail rows are thinned by age, never cut blindly. */
class TrailThinningTest {

    private val now = 1_764_000_000_000L
    private val second = 1_000L
    private val minute = 60 * second
    private val hour = 60 * minute
    private val day = 24 * hour

    /** About 1.1 m of latitude per 1e-5 degree. */
    private fun point(ts: Long, north: Double = 0.0, status: String = "VALID", accuracy: Float = 5f) =
        TrailPoint(ts, 32.0 + north * 1e-5 / 1.11, 35.0, accuracy, status)

    private fun thin(points: List<TrailPoint>, thinBeforeMs: Long = now - TRAIL_KEEP_ALL_MS) =
        thinTrail(points, now, thinBeforeMs) { it }

    /** Still rows every 10 s over [span] ending at [end]. */
    private fun still(end: Long, span: Long, status: String = "VALID") =
        (0 until span / (10 * second)).map { point(end - span + it * 10 * second, status = status) }

    @Test
    fun theLastDayKeepsEveryRow() {
        val rows = still(now, 23 * hour)
        assertEquals(rows, thin(rows))
    }

    @Test
    fun aStillTrailOfTwoDaysAgoKeepsOneRowAMinute() {
        val rows = still(now - 2 * day, hour) + point(now)
        val kept = thin(rows)
        // One an hour of 10 s rows is 360 rows; a row a minute keeps 60, plus the newest row.
        assertEquals(61, kept.size)
        kept.dropLast(1).zipWithNext().forEach { (a, b) -> assertEquals(minute, b.timestampMs - a.timestampMs) }
    }

    @Test
    fun aStillTrailOfTenDaysAgoKeepsOneRowInTenMinutes() {
        val rows = still(now - 10 * day, hour) + point(now)
        assertEquals(7, thin(rows).size)
    }

    @Test
    fun aDriveOfTwoDaysAgoKeepsARowEvery20Meters() {
        val start = now - 2 * day
        // 5 m a second for 10 minutes: 3 km.
        val rows = (0 until 600).map { point(start + it * second, north = it * 5.0) } + point(now, north = 3_000.0)
        val kept = thin(rows)
        assertEquals("Every fourth row of the drive, plus the newest", 151, kept.size)
        kept.dropLast(1).zipWithNext().forEach { (a, b) ->
            val meters = SpatialMath.calculateDistance(a.lat, a.lng, b.lat, b.lng)
            assertTrue("$meters m", meters in 19.0..21.0)
        }
    }

    @Test
    fun rowsOlderThan31DaysAreDeletedButTheNewestStays() {
        val old = still(now - 40 * day, hour)
        assertEquals(listOf(old.last()), thin(old))
        val newer = point(now - hour)
        assertEquals(listOf(newer), thin(old + newer))
    }

    @Test
    fun alarmAndJumpRowsAndTheirMinuteAroundAreKept() {
        val start = now - 3 * day
        val rows = still(start + hour, hour).toMutableList()
        val alarmAt = rows.indexOfFirst { it.timestampMs == start + 30 * minute }
        rows[alarmAt] = rows[alarmAt].copy(status = SIT_ALERT_STATUS)
        rows[alarmAt + 100] = rows[alarmAt + 100].copy(status = "JUMP")
        val kept = thin(rows + point(now))

        assertTrue(rows[alarmAt] in kept)
        assertTrue(rows[alarmAt + 100] in kept)
        (alarmAt - 6..alarmAt + 6).forEach { assertTrue("Row $it near the alarm", rows[it] in kept) }
        assertTrue("A row 2 minutes off is thinned", rows[alarmAt + 11] !in kept)
    }

    @Test
    fun theEndsOfANoFixRunAreKept() {
        val start = now - 3 * day
        val fixed = still(start + 30 * minute, 30 * minute)
        val noFix = still(start + 60 * minute, 30 * minute, status = "NO_FIX").map { it.copy(lat = 0.0, lng = 0.0) }
        val after = still(start + 90 * minute, 30 * minute)
        val kept = thin(fixed + noFix + after + point(now))

        listOf(fixed.last(), noFix.first(), noFix.last(), after.first()).forEach { assertTrue("$it", it in kept) }
    }

    @Test
    fun bothEndsOfALineBreakAreKept() {
        val start = now - 10 * day
        val before = (0 until 30).map { point(start + it * 10 * second) }
        // 20 minutes later and 2 km away: the line breaks there.
        val after = (0 until 30).map { point(start + 25 * minute + it * 10 * second, north = 2_000.0) }
        val kept = thin(before + after + point(now, north = 2_000.0))

        assertTrue(before.last() in kept)
        assertTrue(after.first() in kept)
    }

    @Test
    fun rowsFromThinBeforeOnAreNotTouched() {
        val rows = still(now - 2 * day, 2 * hour)
        val marker = now - 2 * day - hour
        val kept = thin(rows, thinBeforeMs = marker)
        assertTrue(rows.filter { it.timestampMs >= marker }.all { it in kept })
        assertTrue(kept.size < rows.size)
    }

    @Test
    fun thinningTwiceKeepsTheSameRows() {
        val start = now - 20 * day
        val rows = (0 until 20_000).map { i ->
            // A still hour, then a drive, repeated; a NO_FIX run, an alarm and a coarse fix on the way.
            val ts = start + i * 30 * second
            val moving = (i / 120) % 2 == 1
            val status = when {
                i % 5_000 == 17 -> SIT_ALERT_STATUS
                i % 3_000 in 40..60 -> "NO_FIX"
                else -> "VALID"
            }
            point(ts, north = if (moving) i * 30.0 else (i / 240) * 7_200.0, status = status, accuracy = if (i % 997 == 0) 150f else 5f)
        }
        val once = thin(rows)
        assertTrue("${once.size} kept", once.size < rows.size / 2)
        assertEquals(once, thin(once))
    }

    @Test
    fun theLinePiecesStayTheSame() {
        val start = now - 10 * day
        val rows = (0 until 3_000).map { i ->
            // Still, an outage of 30 minutes with a 3 km move, then a drive.
            val ts = start + i * 10 * second + if (i >= 1_000) 30 * minute else 0L
            val north = when {
                i < 1_000 -> 0.0
                i < 2_000 -> 3_000.0
                else -> 3_000.0 + (i - 2_000) * 15.0
            }
            point(ts, north = north)
        }
        assertEquals(pieces(rows), pieces(thin(rows)))
    }

    /** How many pieces the line of [rows] has (the Monitor's trailSegments rule). */
    private fun pieces(rows: List<TrailPoint>): Int {
        val line = rows.filter { it.isLinePoint() }
        return 1 + line.zipWithNext().count { (a, b) -> breaksTrailLine(a, b) }
    }
}
