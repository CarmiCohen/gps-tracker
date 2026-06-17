package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * v8.8.21: Migrated to consistent test-time to remove dependency on System clock.
 */
class MainAlarmLogicTest {

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        return AlarmEvaluationState(
            now = now,
            serviceStartTime = now - 10000,
            lastAlarmAckTs = 0L,
            appStartTime = now - 10000,
            isRelayConnected = true,
            isTrackerConnected = true,
            discoveryPhase = DiscoveryPhase.MONITORING,
            isHardwareOnline = true,
            isLocalInternetLoss = false,
            isJammerSuspicion = false,
            isSignalLoss = false,
            isGpsStalling = false,
            powerAlarmPending = false,
            trackerLat = 10.0,
            trackerLng = 10.0,
            homePoints = listOf(EngineGeoPoint(10.0, 10.0)),
            maxDistance = 100.0,
            trackerGpsAccuracy = 5f,
            maxTrackerAccuracy = 5f,
            lastGpsPacketTs = now,
            trackerBattery = 100,
            trackerTemp = 30f,
            wasDistanceViolated = false,
            distanceViolationCounter = 0,
            firstViolationTs = 0L,
            firstViolationWasJump = false,
            isTrackerMode = true
        )
    }

    @Test
    fun `R872 Verify that we do not have any alerts in healthy tracker mode`() {
        val state = createDefaultState()
        val report = MainAlarmLogic.detectViolations(state)
        
        val activeViolations = report.reports.filter { it.conditionMet }
        if (activeViolations.isNotEmpty()) {
            println("Active violations in supposedly healthy state:")
            activeViolations.forEach { println(" - ${it.type}: ${it.title} (${it.subtitle})") }
        }
        
        assertTrue("Expected no active violations, but found: ${activeViolations.map { it.type }}", report.reports.none { it.conditionMet })
    }

    @Test
    fun `R872 Verify violation titles in Tracker mode`() {
        val now = 1700000000000L
        val state = createDefaultState(now).copy(
            isTrackerMode = true,
            trackerLat = 11.0, 
            trackerLng = 11.0,
            maxDistance = 10.0
        )

        val report = MainAlarmLogic.detectViolations(state)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertTrue("Geofence alert should be met", geofence?.conditionMet == true)
        assertEquals("Geofence", geofence?.title)
    }

    @Test
    fun `Verify Geofence Predictive Exit trigger`() {
        val now = 1700000000000L
        val trackerLat = 10.0011 
        
        val state = createDefaultState(now).copy(
            isTrackerMode = false,
            trackerLat = trackerLat,
            trackerLng = 10.0,
            trackerSpeed = 5.0f, 
            maxDistance = 100.0,
            maxTrackerAccuracy = 5f
        )

        val report = MainAlarmLogic.detectViolations(state)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertTrue("Geofence alert should be present", geofence != null)
        assertTrue("Predictive exit should trigger conditionMet", geofence?.conditionMet == true)
        assertTrue("Technical details should mention PREDICTIVE EXIT", geofence?.technicalDetails?.contains("PREDICTIVE EXIT") == true)
    }

    @Test
    fun `Verify Tamper priority and extreme values`() {
        val now = 1700000000000L
        val state = createDefaultState(now).copy(
            isTrackerMode = false,
            trackerTiltDegrees = 45.0f,
            peakVibrationShock = 0.5f,
            adaptiveVibrationFloor = 0.12f,
            isNear = true
        )

        val report = MainAlarmLogic.detectViolations(state)
        val tamper = report.reports.find { it.type == ALERT_ID_TRACKER_TAMPER }
        val tilt = report.reports.find { it.type == ALERT_ID_TRACKER_TILT }

        assertTrue("Tamper should be active", tamper?.conditionMet == true)
        assertTrue("Tilt should be active", tilt?.conditionMet == true)
        assertTrue("Extreme value should capture the 45 degree tilt", tamper?.extremeValue == 45.0)
        assertTrue("Subtitle should describe the tilt", tamper?.subtitle?.contains("45.0° tilt") == true)
    }
}
