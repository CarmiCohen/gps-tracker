package com.jd.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ISSUE-NOV10.7-O73: each Monitor logs how late live rows reach it, so a late viewer shows where the delay is. */
class ArrivalLagMeterTest {

    @Test
    fun `an alert row is reported at once with its own lag`() {
        val meter = ArrivalLagMeter()
        assertEquals(
            "Alert row ACOUSTIC_ALERT seq 42 arrived 11200 ms after it was sent",
            meter.record("ACOUSTIC_ALERT", 42, sentAtMs = 1_000, nowMs = 12_200)
        )
    }

    @Test
    fun `ordinary rows are summarised once per window`() {
        val meter = ArrivalLagMeter(windowMs = 10_000)
        assertNull(meter.record("VALID", 1, sentAtMs = 0, nowMs = 100))
        assertNull(meter.record("NO_FIX", 2, sentAtMs = 5_000, nowMs = 5_300))
        assertEquals(
            "Arrival lag over 10 s: 3 rows, mean 300 ms, max 500 ms",
            meter.record("VALID", 3, sentAtMs = 9_600, nowMs = 10_100)
        )
        // The next window starts empty.
        assertNull(meter.record("VALID", 4, sentAtMs = 10_150, nowMs = 10_200))
    }

    @Test
    fun `alert rows do not enter the summary`() {
        val meter = ArrivalLagMeter(windowMs = 1_000)
        assertNull(meter.record("VALID", 1, sentAtMs = 0, nowMs = 50))
        meter.record("TILT_ALERT", 2, sentAtMs = 0, nowMs = 90_000)
        assertEquals("Arrival lag over 1 s: 2 rows, mean 50 ms, max 50 ms", meter.record("VALID", 3, sentAtMs = 1_000, nowMs = 1_050))
    }
}
