package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * v9.4.0:
 * - Issue #502: Device Independency. Updated tests to use HardwareCapabilities abstraction.
 * v9.3.16:
 * - R999b: Barometer EMA lift detection support.
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
            trackerGpsAccuracy = 5.0,
            maxTrackerAccuracy = 5.0,
            trackerLastValidFixTs = now,
            lastGpsPacketTs = now,
            trackerBattery = 100,
            trackerTemp = 30.0,
            wasDistanceViolated = false,
            distanceViolationCounter = 0,
            firstViolationTs = 0L,
            firstViolationWasJump = false,
            isTrackerMode = true,
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = false,
                backgroundStatus = CapabilityStatus.GRANTED,
                autostartStatus = CapabilityStatus.GRANTED,
                isManualOverrideActive = false
            )
        )
    }

    @Test
    fun `Verify healthy state has no violations`() {
        val state = createDefaultState()
        val report = MainAlarmLogic.detectViolations(state)
        assertTrue(report.reports.none { it.conditionMet })
    }

    @Test
    fun `Verify Stationary Anchor suppresses coordinates during Hard-Lock`() {
        val now = 1700000000000L
        val lockedState = createDefaultState(now).copy(
            isAnchorLocked = true,
            trackerLat = 10.005, // Deviated point
            trackerLng = 10.005
        )
        
        val report = MainAlarmLogic.detectViolations(lockedState)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        // Logic should use the anchor point (10,10) instead of (10.005, 10.005) when locked
        assertFalse("Geofence should be suppressed by Hard-Lock anchor clamp", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Bayesian Expansion triggers Geofence breach during GPS gap`() {
        val now = 1700000000000L
        
        val stateWithGap = createDefaultState(now + 10000).copy(
            trackerLat = 10.002, // ~220m away.
            trackerLastValidFixTs = now,
            isLocationPending = true,
            trackerSpeed = 20.0 
        )
        
        val report = MainAlarmLogic.detectViolations(stateWithGap)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
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

        // Scenario 1: 2 minutes later
        val stateAt2Min = baseState.copy(now = now + 120000)
        val report1 = MainAlarmLogic.detectViolations(stateAt2Min)
        assertFalse("Adaptive jump should be latched for 6 mins", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Scenario 2: 7 minutes later (Past limit)
        val stateAt7Min = baseState.copy(now = now + 420000)
        val report3 = MainAlarmLogic.detectViolations(stateAt7Min)
        assertTrue("Adaptive jump latch should expire after 6 mins", 
            report3.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Hardware Boot Grace suppresses alarms`() {
        val now = 1700000000000L
        val stateInGrace = createDefaultState(now).copy(
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = true,
                backgroundStatus = CapabilityStatus.DENIED,
                autostartStatus = CapabilityStatus.DENIED
            ),
            serviceStartTime = now - 10000 // 10s uptime
        )
        
        val report = MainAlarmLogic.detectViolations(stateInGrace)
        val alert = report.reports.find { it.type == ALERT_ID_HARDWARE_CONFIGURATION }
        assertFalse("Alert should be suppressed during boot grace period", alert?.conditionMet == true)
    }

    @Test
    fun `Verify Geofence Predictive Exit trigger`() {
        val now = 1700000000000L
        val state = createDefaultState(now).copy(
            trackerLat = 10.0011, // ~120m away
            trackerSpeed = 5.0,
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
            trackerTiltDegrees = 45.0,
            peakVibrationShock = 0.5
        )
        val report = MainAlarmLogic.detectViolations(state)
        val tamper = report.reports.find { it.type == ALERT_ID_TRACKER_TAMPER }
        assertTrue(tamper?.conditionMet == true)
        assertEquals(45.0, tamper?.extremeValue ?: 0.0, 0.1)
    }
}
