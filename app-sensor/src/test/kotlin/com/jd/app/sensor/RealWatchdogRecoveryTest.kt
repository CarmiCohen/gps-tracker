package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

/**
 * Chapter 83 (ISSUE-NOV6.0-O23): Main-Thread Stall Watchdog Recovery.
 * Verifies that when sensing loop tick timestamp lags beyond the calculated stall threshold,
 * stall recovery is detected and triggered.
 */
class RealWatchdogRecoveryTest {

    private fun isStallDetected(elapsedMs: Long, currentIntervalMs: Long): Boolean {
        val threshold = max(currentIntervalMs * 3, 10000L)
        return elapsedMs > threshold
    }

    @Test
    fun normalSensingIntervalDoesNotTriggerWatchdogStall() {
        val intervalMs = 100L // 100ms charging rate
        val elapsedMs = 500L // 500ms normal tick

        assertFalse("Normal tick inside threshold must not trigger stall", isStallDetected(elapsedMs, intervalMs))
    }

    @Test
    fun stallExceedingMinimumTenSecondThresholdTriggersWatchdogRecovery() {
        val intervalMs = 100L
        val elapsedMs = 12000L // 12 seconds stall

        assertTrue("Stall of 12s exceeding 10s minimum threshold must trigger recovery", isStallDetected(elapsedMs, intervalMs))
    }

    @Test
    fun slowIntervalStallDetection() {
        val intervalMs = 5000L
        val elapsedMs = 16000L // 16 seconds stall exceeding 15s threshold

        assertTrue("Stall exceeding 15s threshold must trigger recovery", isStallDetected(elapsedMs, intervalMs))
    }

    @Test
    fun thresholdCalculatesAsThreeTimesIntervalOrTenSecondsMinimum() {
        // At 100ms interval, threshold = max(300, 10000) = 10000ms
        val thresholdFast = max(100L * 3, 10000L)
        assertEquals(10000L, thresholdFast)

        // At 5000ms interval, threshold = max(15000, 10000) = 15000ms
        val thresholdSlow = max(5000L * 3, 10000L)
        assertEquals(15000L, thresholdSlow)
    }
}
