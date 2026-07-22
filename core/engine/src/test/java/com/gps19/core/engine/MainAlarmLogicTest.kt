package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * July.21.00:
 * - Forensic Alignment: Aligned with flattened AlarmEvaluationState (Issue #102).
 * - Monotonic Rt: Using monotonic timestamps for duration validation.
 */
class MainAlarmLogicTest {

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        val nowRt = 100000L
        return AlarmEvaluationState(
            now = now,
            nowRt = nowRt,
            serviceStartTime = now - 60000, // 60s uptime (past grace)
            serviceStartRt = nowRt - 60000,
            lastAlarmAckTs = 0L,
            appStartTime = now - 60000,
            isRelayConnected = true,
            isTrackerConnected = true,
            discoveryPhase = DiscoveryPhase.MONITORING,
            trackerLat = 10.0,
            trackerLng = 10.0,
            homePoints = listOf(EngineGeoPoint(10.0, 10.0)),
            maxDistance = 100.0,
            trackerGpsAccuracy = 5.0,
            maxTrackerAccuracy = 5.0,
            trackerLastValidFixTs = now,
            trackerLastValidFixRt = nowRt,
            lastGpsPacketTs = now,
            lastGpsPacketRt = nowRt,
            trackerBattery = 100,
            trackerTemp = 30.0,
            wasDistanceViolated = false,
            distanceViolationCounter = 0,
            firstViolationTs = 0L,
            firstViolationRt = 0L,
            firstViolationWasJump = false,
            isTrackerMode = true,
            health = SystemHealthState(
                isHardwareOnline = true,
                localInternetLoss = false,
                isJammer = false,
                signalLoss = false,
                gpsStalled = false,
                batteryLevel = 100,
                batteryTemp = 30.0
            ),
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
    fun `Verify Geofence breach detection`() {
        val now = 1700000000000L
        val breachedState = createDefaultState(now).apply {
            distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
            firstViolationTs = now - 10000
        }.let { 
            it.copy(
                trackerLat = 10.005, // ~550m away
                trackerLng = 10.005
            )
        }
        
        val report = MainAlarmLogic.detectViolations(breachedState)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue("Geofence should be violated", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Bayesian Expansion suppresses Geofence breach during GPS gap`() {
        val now = 1700000000000L
        val nowRt = 100000L
        
        val stateWithGap = createDefaultState(now + 10000).copy(
            nowRt = nowRt + 10000,
            trackerLat = 10.002, // ~220m away.
            trackerLastValidFixTs = now,
            trackerLastValidFixRt = nowRt,
            trackerSpeed = 20.0,
            health = SystemHealthState(isLocationPending = true)
        )
        
        val report = MainAlarmLogic.detectViolations(stateWithGap)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertFalse("Geofence should be suppressed by Bayesian expansion during gap", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Jump hold duration`() {
        val now = 1700000000000L
        val nowRt = 100000L
        val baseState = createDefaultState(now).copy(
            trackerLat = 10.005, // ~550m away (Violation)
            jumpTier = 2,
            firstViolationTs = now,
            firstViolationRt = nowRt,
            firstViolationWasJump = true,
            isAdaptiveJump = true,
            health = SystemHealthState(status = SentinelStatus.JUMP)
        )

        // Scenario 1: 2 minutes later (Adaptive jump hold is 6 mins)
        val stateAt2Min = baseState.copy(now = now + 120000, nowRt = nowRt + 120000)
        val report1 = MainAlarmLogic.detectViolations(stateAt2Min)
        assertFalse("Adaptive jump should be latched for 6 mins", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Scenario 2: 7 minutes later (Past limit)
        val stateAt7Min = baseState.copy(now = now + 420000, nowRt = nowRt + 420000)
        val report3 = MainAlarmLogic.detectViolations(stateAt7Min)
        assertTrue("Adaptive jump latch should expire after 6 mins", 
            report3.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Hardware Boot Grace suppresses alarms`() {
        val now = 1700000000000L
        val nowRt = 100000L
        val stateInGrace = createDefaultState(now).copy(
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = true,
                backgroundStatus = CapabilityStatus.DENIED,
                autostartStatus = CapabilityStatus.DENIED
            ),
            serviceStartRt = nowRt - 10000, // 10s uptime
            nowRt = nowRt
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
            maxDistance = 100.0,
            lastGpsPacketRt = 100000L
        )
        val report = MainAlarmLogic.detectViolations(state)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue(geofence?.conditionMet == true)
        assertTrue(geofence?.technicalDetails?.contains("PREDICTIVE EXIT") == true)
    }

    @Test
    fun `Verify Tamper extreme values`() {
        val state = createDefaultState().copy(
            health = SystemHealthState(
                tiltDegrees = 45.0,
                peakVibrationShock = 0.5
            )
        )
        val report = MainAlarmLogic.detectViolations(state)
        val tamper = report.reports.find { it.type == ALERT_ID_TRACKER_TAMPER }
        assertTrue(tamper?.conditionMet == true)
        assertEquals(45.0, tamper?.extremeValue ?: 0.0, 0.1)
    }
}
