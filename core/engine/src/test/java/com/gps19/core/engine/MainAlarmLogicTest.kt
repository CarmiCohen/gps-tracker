package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * v8.9.26: Updated Xiaomi gating and boot grace verification (Issue #190).
 */
class MainAlarmLogicTest {

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        return AlarmEvaluationState(
            now = now,
            serviceStartTime = now - 60000, // 60s uptime (past grace)
            lastAlarmAckTs = 0L,
            appStartTime = now - 60000,
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
            isTrackerMode = true,
            isXiaomiDevice = false,
            xiaomiStatus = EngineXiaomiStatus.GRANTED,
            xiaomiAutostartStatus = EngineXiaomiStatus.GRANTED,
            isXiaomiManualOverride = false
        )
    }

    @Test
    fun `Verify healthy state has no violations`() {
        val state = createDefaultState()
        val report = MainAlarmLogic.detectViolations(state)
        assertTrue(report.reports.none { it.conditionMet })
    }

    @Test
    fun `Verify Xiaomi Boot Grace suppresses alarms`() {
        val now = 1700000000000L
        // Device is Xiaomi, permissions are DENIED, but uptime is only 10s (< 30s grace)
        val stateInGrace = createDefaultState(now).copy(
            isXiaomiDevice = true,
            xiaomiStatus = EngineXiaomiStatus.DENIED,
            xiaomiAutostartStatus = EngineXiaomiStatus.DENIED,
            serviceStartTime = now - 10000 // 10s uptime
        )
        
        val report = MainAlarmLogic.detectViolations(stateInGrace)
        val alert = report.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }
        assertFalse("Alert should be suppressed during boot grace period", alert?.conditionMet == true)
    }

    @Test
    fun `Verify Xiaomi System Missing alert logic after grace`() {
        val now = 1700000000000L
        
        // Scenario 1: Xiaomi device with Autostart DENIED (past grace)
        val stateDenied = createDefaultState(now).copy(
            isXiaomiDevice = true,
            xiaomiAutostartStatus = EngineXiaomiStatus.DENIED,
            serviceStartTime = now - 40000 // 40s uptime
        )
        val report1 = MainAlarmLogic.detectViolations(stateDenied)
        val alert1 = report1.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }
        assertTrue("Alert should trigger when Autostart is denied", alert1?.conditionMet == true)
        assertTrue(alert1?.subtitle?.contains("DENIED") == true)

        // Scenario 2: Status UNKNOWN, no override
        val stateUnknown = createDefaultState(now).copy(
            isXiaomiDevice = true,
            xiaomiStatus = EngineXiaomiStatus.UNKNOWN,
            isXiaomiManualOverride = false,
            serviceStartTime = now - 40000
        )
        val report2 = MainAlarmLogic.detectViolations(stateUnknown)
        assertTrue("Alert should trigger when UNKNOWN and no override", report2.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }?.conditionMet == true)

        // Scenario 3: Status UNKNOWN, WITH override
        val stateOverride = stateUnknown.copy(isXiaomiManualOverride = true)
        val report3 = MainAlarmLogic.detectViolations(stateOverride)
        assertFalse("Alert should NOT trigger when UNKNOWN but override is active", report3.reports.find { it.type == ALERT_ID_XIAOMI_SYSTEM_MISSING }?.conditionMet == true)
    }

    @Test
    fun `Verify Geofence Predictive Exit trigger`() {
        val now = 1700000000000L
        val state = createDefaultState(now).copy(
            trackerLat = 10.0011, // ~120m away
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
