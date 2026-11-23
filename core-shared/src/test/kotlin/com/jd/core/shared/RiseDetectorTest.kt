package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 248 (SIMP-NOV22.13-S1): the one rise detector of the noise and the light detection: the first value only
 * starts the background, no alert while it warms up, a confirmed rise alerts against the background before it, and the
 * background follows every span.
 */
class RiseDetectorTest {

    /** A rise is 10 above the background; 2 s within 3 s. */
    private fun detector(warmUpMs: Long = BACKGROUND_WARM_UP_MS) =
        RiseDetector(confirmMs = 2_000, windowMs = 3_000, warmUpMs = warmUpMs) { value, reference -> value - reference >= 10.0 }

    @Test
    fun theFirstValueOnlyStartsTheBackground() {
        val d = detector(warmUpMs = 0L)
        assertNull(d.backgroundLevel)
        assertFalse("A long first span is no rise", d.onSpan(50.0, endMs = 10_000, durationMs = 10_000))
        assertEquals(50.0, d.backgroundLevel!!, 0.0)
        assertNull(d.alertReference)
    }

    @Test
    fun noAlertWhileTheBackgroundWarmsUp() {
        val d = detector()
        d.onSpan(0.0, 0L, 0L)
        var alerted = false
        for (t in 1..29) alerted = alerted or d.onSpan(100.0, t * 100L, 100L)
        assertFalse("2.9 s loud inside the 3 s warm-up", alerted)
        assertTrue("The warm-up background followed the loud value", d.backgroundLevel!! > 90.0)
    }

    @Test
    fun aConfirmedRiseAlertsAgainstTheBackgroundBeforeItAndTheBackgroundFollows() {
        val d = detector(warmUpMs = 0L)
        d.onSpan(0.0, 0L, 0L)
        assertFalse(d.onSpan(20.0, 1_000L, 1_000L))
        val risen = d.backgroundLevel!!
        assertTrue("The background moved towards the rise", risen > 0.0 && risen < 20.0)
        assertTrue("2 s rising within 3 s", d.onSpan(20.0, 2_000L, 1_000L))
        assertEquals("Compared against the background before the rise", 0.0, d.alertReference!!, 0.0)
        assertFalse("One rise, one alert", d.onSpan(20.0, 3_000L, 1_000L))
    }
}
