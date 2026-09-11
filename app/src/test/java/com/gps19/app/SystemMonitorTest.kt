package com.gps19.app

import com.gps19.core.engine.SYSTEM_WATCHDOG_INTERVAL_MS
import com.gps19.core.engine.WATCHDOG_DANGER_WINDOW_MS
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SystemMonitorTest: Validating Fixed Grid Scheduling and Danger Window protection.
 * Sep.10.39:
 * - Issue #945 Audit: Validated `calculateNextGridPoint` to ensure 
 *   deterministic grid alignment and 20s danger window suppression (R-ID 302).
 *   Logic moved to companion object for Context-free testing.
 */
class SystemMonitorTest {

    @Test
    fun `Grid Scheduling - Default fallback when no session start`() {
        val now = 100_000L
        val expected = now + SYSTEM_WATCHDOG_INTERVAL_MS
        val result = SystemMonitor.calculateNextGridPoint(now, 0L)
        assertEquals("Should fallback to simple interval if session not set", expected, result)
    }

    @Test
    fun `Grid Scheduling - Aligns to fixed grid when outside danger window`() {
        val sessionStart = 100_000L
        val now = 150_000L 
        // Interval = 90s. Next grid = 190,000. 
        // Gap = 40s (> 20s danger window).
        val expected = 190_000L
        val result = SystemMonitor.calculateNextGridPoint(now, sessionStart)
        assertEquals("Should align to the next 90s grid point", expected, result)
    }

    @Test
    fun `Grid Scheduling - Pushes to next slot when inside danger window`() {
        val sessionStart = 100_000L
        val now = 180_000L
        // Interval = 90s. Candidate = 190,000.
        // Gap = 10s (< 20s danger window).
        // Should push to 100,000 + (2 * 90,000) = 280,000.
        val expected = 280_000L
        val result = SystemMonitor.calculateNextGridPoint(now, sessionStart)
        assertEquals("Should push to the subsequent grid point if within 20s danger window", expected, result)
    }

    @Test
    fun `Grid Scheduling - Exact boundary of danger window`() {
        val sessionStart = 100_000L
        val now = 170_000L
        // Gap = 190,000 - 170,000 = 20,000.
        // Boundary condition (candidate - now < 20000) is false if gap == 20000.
        // So it should stay at 190,000.
        val expected = 190_000L
        val result = SystemMonitor.calculateNextGridPoint(now, sessionStart)
        assertEquals("Should allow scheduling exactly at the danger window boundary", expected, result)
    }

    @Test
    fun `Grid Scheduling - Long running session drift protection`() {
        val sessionStart = 100_000L
        val twelveHoursMs = 12 * 60 * 60 * 1000L
        val now = sessionStart + twelveHoursMs + 5_000L 
        
        // 12 hours = 43,200,000 ms. 
        // 43,200,000 / 90,000 = 480 intervals exactly.
        // Elapsed = 43,205,000.
        // nextIntervalIndex = 480 + 1 = 481.
        val expected = sessionStart + (481 * SYSTEM_WATCHDOG_INTERVAL_MS)
        val result = SystemMonitor.calculateNextGridPoint(now, sessionStart)
        
        assertEquals("Should maintain grid alignment after 12 hours of uptime", expected, result)
        assertEquals(0L, (result - sessionStart) % SYSTEM_WATCHDOG_INTERVAL_MS)
    }
}
