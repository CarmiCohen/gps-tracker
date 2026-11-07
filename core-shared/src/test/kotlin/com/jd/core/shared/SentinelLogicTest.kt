package com.jd.core.shared

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * SentinelLogicTest: Ported from OLD MainAlarmLogicTest.
 * Validates core movement validation, jump detection, and geofence transitions.
 */
class SentinelLogicTest {

    private val homeGeofence = SentinelLogic.Geofence(
        id = "HOME",
        lat = 32.7940,
        lng = 34.9896,
        radiusMeters = 100.0
    )

    @Test
    fun `Verify movement validation logic`() {
        val last = LocationUpdate(lat = 32.7940, lng = 34.9896, timestamp = 1000L, speed = 0f)
        
        // 1. Valid slow movement
        val currentValid = LocationUpdate(lat = 32.7941, lng = 34.9897, timestamp = 2000L)
        val res1 = SentinelLogic.validateMovement(last, currentValid, hasPhysicalMotion = true)
        assertEquals(SentinelLogic.SentinelStatus.VALID, res1.status)

        // 2. Outlier movement (Speed > 300km/h)
        val currentOutlier = LocationUpdate(lat = 33.0, lng = 35.0, timestamp = 2000L)
        val res2 = SentinelLogic.validateMovement(last, currentOutlier, hasPhysicalMotion = true)
        assertEquals(SentinelLogic.SentinelStatus.OUTLIER, res2.status)
        assertTrue(res2.reason.contains("Speed Outlier"))
    }

    // Chapter 139.3, ISSUE-NOV7.17-O42: drift inside the GPS error radius is not a movement.
    @Test
    fun `Drift inside the GPS error radius of a still phone is valid`() {
        // The A15 lying still, 2026-11-07 20:10:54: 23 m in 1.6 s between fixes of 68 m and 59 m accuracy.
        val last = LocationUpdate(lat = 32.653092, lng = 35.301082, accuracy = 68f, timestamp = 10_000L)
        val current = LocationUpdate(lat = 32.653299, lng = 35.301082, accuracy = 59f, timestamp = 11_600L)

        val result = SentinelLogic.validateMovement(last, current, hasPhysicalMotion = false)

        assertEquals(SentinelLogic.SentinelStatus.VALID, result.status)
        assertEquals("Within GPS error", result.reason)
    }

    @Test
    fun `A move beyond the GPS error radius is still judged and reported with its measurements`() {
        // About 111 m in 5 s without vibration, with 14 m fixes.
        val last = LocationUpdate(lat = 32.653092, lng = 35.301082, accuracy = 14f, timestamp = 10_000L)
        val current = LocationUpdate(lat = 32.654092, lng = 35.301082, accuracy = 14f, timestamp = 15_000L)

        val result = SentinelLogic.validateMovement(last, current, hasPhysicalMotion = false)

        assertEquals(SentinelLogic.SentinelStatus.JUMP, result.status)
        assertTrue(result.reason, result.reason.startsWith("Kinematic Violation: 22.2 m/s, 111 m in 5.0 s, accuracy 14 to 14 m"))
    }

    @Test
    fun `Verify Geofence transition detection`() {
        val insideUpdate = LocationUpdate(lat = 32.7940, lng = 34.9896)
        val outsideUpdate = LocationUpdate(lat = 32.8000, lng = 35.0000)

        // Transition OUT
        val exitRes = SentinelLogic.evaluateGeofenceTransition(
            outsideUpdate, homeGeofence, SentinelLogic.GeofenceState.INSIDE
        )
        assertEquals(SentinelLogic.GeofenceTransition.EXIT, exitRes.transition)

        // Transition IN
        val enterRes = SentinelLogic.evaluateGeofenceTransition(
            insideUpdate, homeGeofence, SentinelLogic.GeofenceState.OUTSIDE
        )
        assertEquals(SentinelLogic.GeofenceTransition.ENTER, enterRes.transition)
    }

    @Test
    fun `Verify Silent Failure Anomaly Correlation R133`() {
        // High load + GPS Stall = Silent Failure
        val isSilent = SentinelLogic.isSilentFailure(
            gpsStalled = true,
            isTamperDetected = false,
            cpuLoad = 0.9,
            ioWait = 0.1,
            maxIoLatency = 100,
            isThermalThrottling = false
        )
        assertTrue("High CPU load MUST trigger silent failure if GPS stalled", isSilent)

        // Low load = Not Silent
        val isNotSilent = SentinelLogic.isSilentFailure(
            gpsStalled = true,
            isTamperDetected = false,
            cpuLoad = 0.1,
            ioWait = 0.1,
            maxIoLatency = 100,
            isThermalThrottling = false
        )
        assertFalse(isNotSilent)
    }
}
