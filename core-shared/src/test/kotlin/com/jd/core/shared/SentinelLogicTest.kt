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
