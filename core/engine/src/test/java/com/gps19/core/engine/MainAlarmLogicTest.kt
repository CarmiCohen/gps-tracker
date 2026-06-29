package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * v8.9.52:
 * - Issue #431: Added Bayesian expansion verification for geofence breaches.
 * - Issue #452: Added SNR Latch (Adaptive Jump) hold duration verification.
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
            trackerLastValidFixTs = now, // Added for Issue #431
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
    fun `Verify Bayesian Expansion triggers Geofence breach during GPS gap`() {
        val now = 1700000000000L
        
        val stateWithGap = createDefaultState(now + 10000).copy(
            trackerLat = 10.002, // ~220m away. Threshold is 100 + (5*6) = 130m.
            trackerLastValidFixTs = now,
            isLocationPending = true,
            trackerSpeed = 20f // Drift capped at 33.3m/s
        )
        
        val report = MainAlarmLogic.detectViolations(stateWithGap)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        // Effective Accuracy = 5 + (20 * 10) = 205m.
        // Threshold = 100 + (205 * 6) = 1330m. 
        // 220m < 1330m -> SHOULD NOT BREACH.
        assertFalse("Geofence should be suppressed by Bayesian expansion during gap", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify SNR Latch (Adaptive Jump) hold duration`() {
        val now = 1700000000000L
        val baseState = createDefaultState(now).copy(
            trackerLat = 10.005, // ~550m away (Violation)
            isTrackerVisualJump = true,
            isAdaptiveJump = true,
            jumpTier = 2,
            firstViolationTs = now,
            firstViolationWasJump = true
        )

        // Scenario 1: 2 minutes later (Standard hold is 3 mins, Adaptive is 6 mins)
        val stateAt2Min = baseState.copy(now = now + 120000)
        val report1 = MainAlarmLogic.detectViolations(stateAt2Min)
        assertFalse("Adaptive jump should be latched for 6 mins, 2 mins is too early", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Scenario 2: 4 minutes later (Standard hold 3 mins would have triggered, but Adaptive is 6 mins)
        val stateAt4Min = baseState.copy(now = now + 240000)
        val report2 = MainAlarmLogic.detectViolations(stateAt4Min)
        assertFalse("Adaptive jump should still be suppressed at 4 mins", 
            report2.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Scenario 3: 7 minutes later (Past 6 min adaptive limit)
        val stateAt7Min = baseState.copy(now = now + 420000)
        val report3 = MainAlarmLogic.detectViolations(stateAt7Min)
        assertTrue("Adaptive jump latch should expire after 6 mins", 
            report3.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Xiaomi Boot Grace suppresses alarms`() {
        val now = 1700000000000L
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
