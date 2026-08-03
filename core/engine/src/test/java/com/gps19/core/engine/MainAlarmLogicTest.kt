package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * Aug.03.37:
 * - Issue #669: Refactored to eliminate .copy() usage and fix callback signatures 
 *   following zero-churn transition.
 */
class MainAlarmLogicTest {

    private val mockTimeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = 100000L
        override fun currentTimeMillis(): Long = 1700000000000L
    }

    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        val nowRt = 100000L
        val state = AlarmEvaluationState()
        state.update(
            now = now,
            nowRt = nowRt,
            serviceStartTime = now - 60000, 
            serviceStartRt = nowRt - 60000,
            lastAlarmAckTs = 0L,
            appStartTime = now - 60000,
            isRelayConnected = true,
            isTrackerConnected = true,
            discoveryPhase = DiscoveryPhase.MONITORING,
            trackerLat = 10.0,
            trackerLng = 10.0,
            trackerGpsAccuracy = 5.0,
            maxTrackerAccuracy = 5.0,
            lastGpsPacketTs = now,
            lastGpsPacketRt = nowRt,
            trackerLastValidFixTs = now,
            trackerLastValidFixRt = nowRt,
            trackerSpeed = 0.0,
            jumpTier = 0,
            isAdaptiveJump = false,
            trackerBattery = 100,
            trackerTemp = 30.0,
            wasDistanceViolated = false,
            distanceViolationCounter = 0,
            firstViolationTs = 0L,
            firstViolationRt = 0L,
            firstViolationWasJump = false,
            maxDistance = 100.0,
            distToHomeAuthority = null,
            isGpsGap = false,
            trackerBaroAltEma = 0.0,
            isTrackerMode = true,
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = false,
                backgroundStatus = CapabilityStatus.GRANTED,
                autostartStatus = CapabilityStatus.GRANTED,
                isManualOverrideActive = false
            )
        )
        // Initialize health specifically
        state.health.apply {
            isHardwareOnline = true
            localInternetLoss = false
            isJammer = false
            signalLoss = false
            gpsStalled = false
            batteryLevel = 100
            batteryTemp = 30.0
            status = SentinelStatus.VALID
            isTamperDetected = false
            isNear = true
            lux = 0.0
            luxBaseline = 0.0
            acousticDb = 0.0
            acousticFloorDb = 0.0
            peakVibrationShock = 0.0
            adaptiveVibrationFloor = 0.12
            tiltDegrees = 0.0
        }
        state.truncateHomePoints(0)
        state.getOrCreateHomePoint(0).update(10.0, 10.0)
        return state
    }

    @Test
    fun `Verify healthy state has no violations`() {
        val state = createDefaultState()
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        assertTrue(report.reports.none { it.conditionMet })
    }

    @Test
    fun `Verify Geofence breach detection`() {
        val now = 1700000000000L
        val state = createDefaultState(now).apply {
            distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
            firstViolationTs = now - 10000
            trackerLat = 10.005 // ~550m away
            trackerLng = 10.005
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue("Geofence should be violated", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Bayesian Expansion suppresses Geofence breach during GPS gap`() {
        val now = 1700000000000L
        val nowRt = 100000L
        
        val state = createDefaultState(now + 10000).apply {
            nowRt = 100000L + 10000
            trackerLat = 10.002 // ~220m away.
            trackerLastValidFixTs = now
            trackerLastValidFixRt = nowRt
            trackerSpeed = 20.0
            health.isLocationPending = true
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertFalse("Geofence should be suppressed by Bayesian expansion during gap", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Jump hold duration`() {
        val now = 1700000000000L
        val nowRt = 100000L
        val state = createDefaultState(now).apply {
            trackerLat = 10.005 // ~550m away (Violation)
            jumpTier = 2
            firstViolationTs = now
            firstViolationRt = nowRt
            firstViolationWasJump = true
            isAdaptiveJump = true
            health.status = SentinelStatus.JUMP
        }

        // Test at 2 min
        state.now = now + 120000
        state.nowRt = nowRt + 120000
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, spikeLogger)
        assertFalse("Adaptive jump should be latched for 6 mins", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Test at 7 min
        state.now = now + 420000
        state.nowRt = nowRt + 420000
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, spikeLogger)
        assertTrue("Adaptive jump latch should expire after 6 mins", 
            report2.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Hardware Boot Grace suppresses alarms`() {
        val now = 1700000000000L
        val nowRt = 100000L
        val state = createDefaultState(now).apply {
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = true,
                backgroundStatus = CapabilityStatus.DENIED,
                autostartStatus = CapabilityStatus.DENIED
            )
            serviceStartRt = nowRt - 10000
            this.nowRt = nowRt
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val alert = report.reports.find { it.type == ALERT_ID_HARDWARE_CONFIGURATION }
        assertFalse("Alert should be suppressed during boot grace period", alert?.conditionMet == true)
    }

    @Test
    fun `Verify Geofence Predictive Exit trigger`() {
        val now = 1700000000000L
        val state = createDefaultState(now).apply {
            trackerLat = 10.0011 // ~120m away
            trackerSpeed = 5.0
            maxDistance = 100.0
            lastGpsPacketRt = 100000L
        }
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue(geofence?.conditionMet == true)
        assertTrue(geofence?.technicalDetails?.contains("PREDICTIVE EXIT") == true)
    }

    @Test
    fun `Verify Tamper extreme values`() {
        val state = createDefaultState().apply {
            health.tiltDegrees = 45.0
            health.peakVibrationShock = 0.5
        }
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val tamper = report.reports.find { it.type == ALERT_ID_TRACKER_TAMPER }
        assertTrue(tamper?.conditionMet == true)
        assertEquals(45.0, tamper?.extremeValue ?: 0.0, 0.1)
    }
}
