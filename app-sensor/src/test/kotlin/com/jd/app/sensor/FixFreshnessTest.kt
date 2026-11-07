package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV7.19-O43: the Sensor reports NO_FIX once its newest fix is older than FIX_STALE_AFTER_MS. */
class FixFreshnessTest {

    private val fixAt = 1_000_000_000_000L // elapsedRealtimeNanos of the fix
    private fun ms(value: Long) = value * 1_000_000L

    @Test
    fun aFixJustTakenIsFresh() {
        assertTrue(isFixFresh(fixAt, fixAt))
        assertTrue(isFixFresh(fixAt, fixAt + ms(5_000)))
    }

    @Test
    fun aFixStaysFreshUntilTheLimit() {
        assertTrue(isFixFresh(fixAt, fixAt + ms(FIX_STALE_AFTER_MS) - 1))
        assertFalse(isFixFresh(fixAt, fixAt + ms(FIX_STALE_AFTER_MS)))
    }

    @Test
    fun anOldFixIsStale() {
        assertFalse("A fix from a minute ago is not the current position", isFixFresh(fixAt, fixAt + ms(60_000)))
    }

    @Test
    fun aFixStampedAheadOfNowCountsAsFresh() {
        assertTrue(isFixFresh(fixAt + ms(500), fixAt))
    }

    @Test
    fun theLimitCoversThreeMissedLocationRequests() {
        // SensorService asks the fused provider for a fix every 5 s.
        assertEquals(15_000L, FIX_STALE_AFTER_MS)
        assertEquals("NO_FIX", NO_FIX_STATUS)
    }
}
