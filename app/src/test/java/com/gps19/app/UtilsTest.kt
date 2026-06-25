package com.gps19.app

import com.gps19.core.engine.FormatterUtils
import com.gps19.core.engine.PhysicsUtils
import com.gps19.core.engine.TelemetryUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UtilsTest: Tests for migrated utility functions.
 * v8.9.37:
 * - R810 Split: Updated future packet test to R810-N.
 */
class UtilsTest {

    @Test
    fun `calculateDistance returns correct distance for known points`() {
        val lat1 = 51.5074
        val lon1 = -0.1278
        val lat2 = 48.8566
        val lon2 = 2.3522
        val result = PhysicsUtils.calculateDistance(lat1, lon1, lat2, lon2)
        assertEquals(344000.0, result, 1000.0)
    }

    @Test
    fun `calculateDistance returns zero for same point`() {
        val lat = 32.0853
        val lon = 34.7818
        assertEquals(0.0, PhysicsUtils.calculateDistance(lat, lon, lat, lon), 0.001)
    }

    @Test
    fun `isValidLocation identifies valid and invalid coordinates`() {
        assertTrue(PhysicsUtils.isValidLocation(32.0, 34.0))
        assertFalse(PhysicsUtils.isValidLocation(0.0, 0.0))
        assertFalse(PhysicsUtils.isValidLocation(91.0, 0.0))
        assertFalse(PhysicsUtils.isValidLocation(Double.NaN, 34.0))
    }

    @Test
    fun `formatDurationUnified formats correctly`() {
        assertEquals("01:05:00", FormatterUtils.formatDurationUnified(3900000L))
        assertEquals("00:00:00", FormatterUtils.formatDurationUnified(0L))
    }

    @Test
    fun `calculateGpsIndex handles clock drift correctly`() {
        // R810-N: Test negative age (future packet) up to 30s
        val futureAge = -5000L // 5s in the future
        val result = TelemetryUtils.calculateGpsIndex(futureAge, 5.0f, 15)
        
        // Should be treated as 0ms age (index = 1.0)
        assertTrue("Index should be > 0 for minor future drift", result.totalIndex > 0.9f)
        assertEquals(1.0f, result.ageIndex, 0.01f)

        // Test extreme future drift (> 30s)
        val extremeFuture = -40000L
        val failedResult = TelemetryUtils.calculateGpsIndex(extremeFuture, 5.0f, 15)
        assertEquals(0f, failedResult.totalIndex, 0.001f)
    }

    @Test
    fun `calculateCommIndex scales correctly with RTT and signal`() {
        assertEquals(10, TelemetryUtils.calculateCommIndex(rtt = 50, remoteSig = 10, localSig = 10))
        assertEquals(0, TelemetryUtils.calculateCommIndex(rtt = 2500, remoteSig = 10, localSig = 10))
    }
}
