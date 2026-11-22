package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 243 (SIMP-NOV22.4-S2): the one rise confirmation of the noise and the light detection: rising spans adding up
 * to the confirmation time within the window alert once, against the background at the first span; the next rise
 * counts only after a span below the limit.
 */
class RiseConfirmerTest {

    /** A rise is 10 above the background; 2 s within 3 s. */
    private fun confirmer() = RiseConfirmer(confirmMs = 2_000, windowMs = 3_000) { value, reference -> value - reference >= 10.0 }

    @Test
    fun risingSpansThatAddUpWithinTheWindowAlertOnceAgainstTheFirstBackground() {
        val c = confirmer()
        assertNull(c.alertReference)
        assertFalse(c.onSpan(20.0, endMs = 1_000, durationMs = 1_000, floor = 0.0))
        assertTrue("1 s quiet, then 1 s more: 2 s within 3 s", run { c.onSpan(0.0, 2_000, 1_000, 2.0); c.onSpan(20.0, 3_000, 1_000, 5.0) })
        assertEquals("The background at the first span, not the risen one", 0.0, c.alertReference!!, 0.0)
    }

    @Test
    fun spansThatDoNotFitInTheWindowStartAgain() {
        val c = confirmer()
        assertFalse(c.onSpan(20.0, 1_000, 1_000, 0.0))
        assertFalse(c.onSpan(0.0, 2_900, 1_900, 0.0))
        assertFalse("This span ends 3.5 s after the first began: a new start", c.onSpan(20.0, 3_500, 600, 0.0))
        assertTrue("The new start's 2 s", c.onSpan(20.0, 4_900, 1_400, 0.0))
    }

    @Test
    fun aNewRiseCountsOnlyAfterASpanBelowTheLimit() {
        val c = confirmer()
        assertTrue(c.onSpan(20.0, 2_000, 2_000, 0.0))
        assertFalse("Still loud after the alert", c.onSpan(20.0, 6_000, 4_000, 0.0))
        assertFalse(c.onSpan(0.0, 6_100, 100, 0.0))
        assertTrue(c.onSpan(20.0, 8_100, 2_000, 1.0))
        assertEquals(1.0, c.alertReference!!, 0.0)
    }

    @Test
    fun anEmptySpanChangesNothing() {
        val c = confirmer()
        assertFalse(c.onSpan(20.0, 1_000, 0, 0.0))
        assertFalse("No rise was started by the empty span", c.onSpan(20.0, 3_000, 1_900, 0.0))
        assertTrue(c.onSpan(20.0, 3_100, 100, 0.0))
    }
}
