package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * v8.9.2: Added Xiaomi gating verification (Issue 133).
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
    fun `Verify healthy state has no violations`() {
        val state = createDefaultState()
        val report = MainAlarmLogic.detectViolations(state)
        assertTrue(report.reports.none { it.conditionMet })
    }

    @Test
    fun `Verify Xiaomi System Missing alert logic`() {
        val now = 1700000000000L
        
        // Scenario 1: Xiaomi device with Autostart BLOCKED
        val stateBlocked = createDefaultState(now).copy(
            isXiaomiDevice = true,
            isXiaomiAutostartGranted = false
        )
        val report1 = MainAlarmLogic.detectViolations(stateBlocked)
        val alert1 = report1.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }
        assertTrue("Alert should trigger when Autostart is blocked", alert1?.conditionMet == true)
        assertTrue(alert1?.subtitle?.contains("Autostart blocked") == true)

        // Scenario 2: Xiaomi device, status DENIED
        val stateDenied = createDefaultState(now).copy(
            isXiaomiDevice = true,
            xiaomiStatus = EngineXiaomiStatus.DENIED,
            isXiaomiAutostartGranted = true
        )
        val report2 = MainAlarmLogic.detectViolations(stateDenied)
        val alert2 = report2.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }
        assertTrue("Alert should trigger when status is DENIED", alert2?.conditionMet == true)

        // Scenario 3: Xiaomi device, status UNKNOWN, no override
        val stateUnknown = createDefaultState(now).copy(
            isXiaomiDevice = true,
            xiaomiStatus = EngineXiaomiStatus.UNKNOWN,
            isXiaomiManualOverride = false,
            isXiaomiAutostartGranted = true
        )
        val report3 = MainAlarmLogic.detectViolations(stateUnknown)
        assertTrue("Alert should trigger when UNKNOWN and no override", report3.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }?.conditionMet == true)

        // Scenario 4: Xiaomi device, status UNKNOWN, WITH override
        val stateOverride = stateUnknown.copy(isXiaomiManualOverride = true)
        val report4 = MainAlarmLogic.detectViolations(stateOverride)
        assertTrue("Alert should NOT trigger when UNKNOWN but override is active", report4.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }?.conditionMet == false)
    }

    @Test
    fun `Verify Geofence Predictive Exit trigger`() {
        val now = 1700000000000L
        val state = createDefaultState(now).copy(
            trackerLat = 10.0011,
            trackerSpeed = 5.0f,
            maxDistance = 100.0
        )
        val report = MainAlarmLogic.detectViolations(state)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue(geofence?.conditionMet == true)
        assertTrue(geofence?.technicalDetails?.contains("PREDICTIVE EXIT") == true)
    }

    @Test
    fun `Verify Tamper extreme values`() {
        val state = createDefaultState().copy(
            trackerTiltDegrees = 45.0f,
            peakVibrationShock = 0.5f
        )
        val report = MainAlarmLogic.detectViolations(state)
        val tamper = report.reports.find { it.type == ALERT_ID_TRACKER_TAMPER }
        assertTrue(tamper?.conditionMet == true)
        assertEquals(45.0, tamper?.extremeValue ?: 0.0, 0.1)
    }
}
