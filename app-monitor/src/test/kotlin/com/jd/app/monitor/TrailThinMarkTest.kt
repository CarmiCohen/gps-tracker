package com.jd.app.monitor

import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.TRAIL_GAP_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV27.1-O168: the thinned-up-to mark and when it may move on. */
class TrailThinMarkTest {

    private val now = 1_764_000_000_000L

    @Test
    fun theMarkBelongsToItsTracker() {
        val mark = TrailThinned("T", now)
        assertEquals(now, mark.upToMsOf("T"))
        assertNull(mark.upToMsOf("U"))
        assertNull((null as TrailThinned?).upToMsOf("T"))
    }

    @Test
    fun theMarkNeverMovesBack() {
        val mark = TrailThinned("T", now)
        assertEquals(mark, mark.thinnedTo("T", now - 1))
        assertEquals(TrailThinned("T", now + 1), mark.thinnedTo("T", now + 1))
        assertEquals("Another tracker's mark is replaced", TrailThinned("U", now - 1), mark.thinnedTo("U", now - 1))
    }

    @Test
    fun theMarkMovesOnlyWithContactNoBackfillAndNoOpenHole() {
        assertTrue(mayAdvanceThinning(now - 5_000, now, isBackfilling = false, openRows = 0L))
        assertFalse("No live row", mayAdvanceThinning(null, now, false, 0L))
        assertFalse("Contact lost", mayAdvanceThinning(now - THIN_CONTACT_MS - 1, now, false, 0L))
        assertFalse("Backfill active", mayAdvanceThinning(now, now, true, 0L))
        assertFalse("A hole still open", mayAdvanceThinning(now, now, false, 3L))
        assertFalse("No gap scan yet", mayAdvanceThinning(now, now, false, null))
    }

    @Test
    fun aLedgerKnowsWhileABackfillForTheSensorRuns() {
        val ledger = BackfillLedger(maxActive = 10, timeoutMs = 30_000L)
        assertFalse(ledger.isBackfilling("T", 0L))
        ledger.onRequestSent(BackfillRequest("T", 1, 9, "r"), 0L)
        assertTrue(ledger.isBackfilling("T", 29_999L))
        assertFalse("Another Sensor", ledger.isBackfilling("U", 0L))
        assertFalse("Timed out", ledger.isBackfilling("T", 30_000L))
        ledger.onRequestSent(BackfillRequest("T", 1, 9, "s"), 40_000L)
        ledger.onRequestComplete("s")
        assertFalse("Answered", ledger.isBackfilling("T", 40_001L))
    }

    @Test
    fun aThinnedOldTrailDrawsTheSamePieces() {
        val day = 86_400_000L
        val start = now - 10 * day
        // A still hour, a 20 minute outage that moved 2 km, then a drive north at 10 m a second.
        val rows = (0 until 360).map { TrailEntity("T", start + it * 10_000L, it.toLong(), 32.0, 35.0, 5f, "VALID") } +
            (0 until 600).map {
                TrailEntity("T", start + 3_600_000L + TRAIL_GAP_MS * 4 + it * 1_000L, 360L + it, 32.018 + it * 0.00009, 35.0, 5f, "VALID")
            }
        val thinned = com.jd.core.shared.thinTrail(rows, now) { it.toTrailPoint() }

        assertTrue(thinned.size < rows.size / 3)
        assertEquals(trailSegments(rows).size, trailSegments(thinned).size)
        assertEquals(2, trailSegments(thinned).size)
    }
}
