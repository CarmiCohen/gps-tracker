package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * Aug.11.14:
 * - Issue #144: Geofence Uncertainty Growth Validation. Added test case for 
 *   Bayesian Drift protection during GPS gaps.
 * Aug.10.31:
 * - Issue #133: Forensic Anomaly Correlation Engine. Added unit test for 
 *   Silent Failure detection (R133).
 * Aug.04.55:
 * - Issue #716: Forensic Audit: Critical Battery Sentinel. Added unit test 
 *   for correlated steep discharge alerting (R716).
 * Aug.04.50:
 * - Issue #715: Forensic Audit: Persistence Health Alerting. Added unit test 
 *   for forensic reliability degradation alerting (R715). Renamed local nowRt 
 *   to avoid reassignment conflict.
 */
class MainAlarmLogicTest {

    private val mockTimeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = 100000L
        override fun currentTimeMillis(): Long = 1700000000000L
    }

    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        val baseNowRt = 100000L
        val state = AlarmEvaluationState()
        state.update(
            now = now,
            nowRt = baseNowRt,
            serviceStartTime = now - 60000, 
            serviceStartRt = baseNowRt - 60000,
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
            lastGpsPacketRt = baseNowRt,
            trackerLastValidFixTs = now,
            trackerLastValidFixRt = baseNowRt,
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
            forensicReliability = 1.0
            vibration = 0.0
            cpuLoad = 0.0
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
        val baseNowRt = 100000L
        
        val state = createDefaultState(now + 10000).apply {
            this.nowRt = baseNowRt + 10000
            trackerLat = 10.002 // ~220m away.
            trackerLastValidFixTs = now
            trackerLastValidFixRt = baseNowRt
            trackerSpeed = 20.0
            health.isLocationPending = true
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertFalse("Geofence should be suppressed by Bayesian expansion during gap", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Geofence breach does NOT clear during GPS gap despite uncertainty growth`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        
        // 1. Establish violation
        val state = createDefaultState(now).apply {
            this.nowRt = baseNowRt
            trackerLat = 10.005 // ~550m away (Violation > 100m + buffer)
            wasDistanceViolated = true
            distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
            trackerLastValidFixRt = baseNowRt
            maxTrackerAccuracy = 5.0
        }
        
        // 2. Simulate GPS gap with high drift
        // After 100 seconds at 15m/s (conservative) drift, acc = 5 + 1500 = 1505m.
        // threshold = 100 + (1505 * 6) = 9130m.
        // 550m is now well within the threshold (Inside range = 550 <= 9130 - 5).
        state.apply {
            this.now = now + 100000
            this.nowRt = baseNowRt + 100000
            trackerSpeed = 20.0
            health.isLocationPending = true
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        
        assertTrue("Violation should persist during GPS gap uncertainty expansion", state.wasDistanceViolated)
    }

    @Test
    fun `Verify Jump hold duration`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        val state = createDefaultState(now).apply {
            trackerLat = 10.005 // ~550m away (Violation)
            jumpTier = 2
            firstViolationTs = now
            firstViolationRt = baseNowRt
            firstViolationWasJump = true
            isAdaptiveJump = true
            health.status = SentinelStatus.JUMP
        }

        // Test at 2 min
        state.now = now + 120000
        state.nowRt = baseNowRt + 120000
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, spikeLogger)
        assertFalse("Adaptive jump should be latched for 6 mins", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        // Test at 7 min
        state.now = now + 420000
        state.nowRt = baseNowRt + 420000
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, spikeLogger)
        assertTrue("Adaptive jump latch should expire after 6 mins", 
            report2.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Hardware Boot Grace suppresses alarms`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        val state = createDefaultState(now).apply {
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = true,
                backgroundStatus = CapabilityStatus.DENIED,
                autostartStatus = CapabilityStatus.DENIED
            )
            serviceStartRt = baseNowRt - 10000
            this.nowRt = baseNowRt
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

    @Test
    fun `Verify Forensic Persistence Reliability Alerting`() {
        val now = 1700000000000L
        val testNowRt = 100000L
        val state = createDefaultState(now).apply {
            this.nowRt = testNowRt
            health.forensicReliability = 0.8 // Below 0.85 threshold
        }

        // 1. Initial detection of degradation - should NOT trigger yet
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, spikeLogger)
        val alert1 = report1.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }
        assertFalse("Alert should not trigger immediately upon degradation", alert1?.conditionMet == true)
        assertTrue("Degradation start time should be recorded", state.forensicReliabilityDegradationStartRt == testNowRt)

        // 2. 15 seconds later - still below duration threshold (30s)
        state.nowRt = testNowRt + 15000
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, spikeLogger)
        val alert2 = report2.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }
        assertFalse("Alert should not trigger after only 15 seconds", alert2?.conditionMet == true)

        // 3. 31 seconds later - should trigger alert
        state.nowRt = testNowRt + 31000
        val report3 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report3, spikeLogger)
        val alert3 = report3.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }
        assertTrue("Alert should trigger after 31 seconds of degradation", alert3?.conditionMet == true)
        assertEquals(0.2, alert3?.extremeValue ?: 0.0, 0.01)

        // 4. Recovery - alert should clear
        state.health.forensicReliability = 0.9
        state.nowRt = testNowRt + 40000
        val report4 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report4, spikeLogger)
        val alert4 = report4.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }
        assertFalse("Alert should clear upon recovery", alert4?.conditionMet == true)
        assertEquals(0L, state.forensicReliabilityDegradationStartRt)
    }

    @Test
    fun `Verify Critical Battery Sentinel Enhanced Correlation`() {
        val state = createDefaultState().apply {
            health.isBatterySteepDischarge = true
            health.vibration = 0.5 // High activity (>0.25G)
            health.cpuLoad = 0.8    // High load (>0.7)
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val alert = report.reports.find { it.type == ALERT_ID_BATTERY_STEEP_DISCHARGE }
        
        assertTrue("Battery alert should trigger", alert?.conditionMet == true)
        assertTrue("Subtitle should indicate imminent shutdown", alert?.subtitle?.contains("IMMINENT SHUTDOWN") == true)
        assertTrue("Technical details should contain metrics", alert?.technicalDetails?.contains("Vibe: 0.50G") == true)
    }

    @Test
    fun `Verify Silent Failure Correlation Detection`() {
        val state = createDefaultState().apply {
            health.gpsStalled = true
            health.isTamperDetected = false
            health.cpuLoad = 0.9    // Above 0.85 threshold
            health.ioWait = 0.1
        }
        
        // 1. Triggered by CPU Load
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, spikeLogger)
        val alert1 = report1.reports.find { it.type == ALERT_ID_SILENT_FAILURE }
        assertTrue("Silent Failure should trigger due to high CPU", alert1?.conditionMet == true)
        assertTrue(alert1?.subtitle?.contains("CPU: 0.9") == true)

        // 2. Triggered by IO Wait
        state.health.cpuLoad = 0.5
        state.health.ioWait = 0.5 // Above 0.40 threshold
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, spikeLogger)
        val alert2 = report2.reports.find { it.type == ALERT_ID_SILENT_FAILURE }
        assertTrue("Silent Failure should trigger due to high IOWait", alert2?.conditionMet == true)
        assertTrue(alert2?.subtitle?.contains("IOW: 0.5") == true)

        // 3. Triggered by IO Latency
        state.health.ioWait = 0.1
        state.health.maxIoLatency = 1000L // Above 800ms threshold
        val report3 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report3, spikeLogger)
        val alert3 = report3.reports.find { it.type == ALERT_ID_SILENT_FAILURE }
        assertTrue("Silent Failure should trigger due to high IO Latency", alert3?.conditionMet == true)
        assertTrue(alert3?.technicalDetails?.contains("1000ms") == true)

        // 4. Suppressed by Tamper (Tamper takes precedence)
        state.health.isTamperDetected = true
        val report4 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report4, spikeLogger)
        val alert4 = report4.reports.find { it.type == ALERT_ID_SILENT_FAILURE }
        assertFalse("Silent Failure should be suppressed if tamper is detected", alert4?.conditionMet == true)
    }
}
