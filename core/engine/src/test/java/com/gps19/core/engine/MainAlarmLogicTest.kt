package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*
import kotlin.math.abs

/**
 * MainAlarmLogicTest: Validating centralized violation logic.
 * Oct.10.4:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
class MainAlarmLogicTest {

    private val mockTimeProvider = object : TimeProvider {
        override val elapsedRealtime: Long get() = 100000L
        override val currentTimeMillis: Long get() = 1700000000000L
    }

    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }
    private val onTrigger: (AlarmEvaluationState.ActiveAlarm) -> Unit = { }
    private val onResolve: (AlarmEvaluationState.ActiveAlarm, Long) -> Unit = { _, _ -> }

    private fun createDefaultState(now: Long = 1700000000000L): AlarmEvaluationState {
        val baseNowRt = 100000L
        val state = AlarmEvaluationState()
        state.update(
            now = now,
            nowRt = baseNowRt,
            serviceStartTime = now - 60000, 
            serviceStartRt = baseNowRt - 60000,
            lastAlarmAckTs = 0L,
            violationStartTs = 0L,
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
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, false, spikeLogger, onTrigger, onResolve)
        assertTrue(report.reports.none { it.conditionMet })
    }

    @Test
    fun `Verify Siren Lockout Duration logic (Issue 1403)`() {
        val lastSirenStopRt = 100000L
        
        // 1. Check at 15s - should still be locked out
        val nowRt1 = 115000L
        assertTrue("Siren should be locked out at 15s", nowRt1 - lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS)
        assertEquals(30000L, SIREN_RESUME_COOLDOWN_MS)
        
        // 2. Check at 31s - should be cleared
        val nowRt2 = 131000L
        assertFalse("Siren lockout should expire after 30s", nowRt2 - lastSirenStopRt < SIREN_RESUME_COOLDOWN_MS)
    }

    @Test
    fun `Verify Sequential Trigger Mute Protection (Issue 1405)`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        val state = createDefaultState(now).apply {
            nowRt = baseNowRt + 5000     // 5s into lockout
        }

        var triggerCount = 0
        var muteCount = 0

        // 1. Trigger Power Alarm while muted. 
        // PowerTamper triggers ALERT_ID_TRACKER_POWER AND ALERT_ID_TRACKER_TAMPER.
        state.health.isPowerTamper = true
        MainAlarmLogic.detectViolations(state, mockTimeProvider, SystemHealthReport(), true, spikeLogger, { triggerCount++ }, onResolve, { muteCount++ })
        assertEquals(0, triggerCount)
        assertEquals(2, muteCount)

        // 2. Trigger Tilt Alarm while muted.
        // Advance time and satisfy ALERT_TRIGGER_GRACE_PERIOD_MS (2000ms)
        state.now += 5000
        state.nowRt += 5000
        state.health.tiltDegrees = 45.0
        MainAlarmLogic.detectViolations(state, mockTimeProvider, SystemHealthReport(), true, spikeLogger, { triggerCount++ }, onResolve, { muteCount++ })
        assertEquals(0, triggerCount)
        assertEquals(3, muteCount)
    }

    @Test
    fun `Verify Geofence breach detection`() {
        val now = 1700000000000L
        val state = createDefaultState(now).apply {
            distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
            firstViolationTs = now - 10000
            trackerLat = 10.005 
            trackerLng = 10.005
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, false, spikeLogger, onTrigger, onResolve)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue("Geofence should be violated", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Bayesian Expansion suppresses Geofence breach during GPS gap`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        
        val state = createDefaultState(now + 10000).apply {
            this.nowRt = baseNowRt + 10000
            trackerLat = 10.002 
            trackerLastValidFixTs = now
            trackerLastValidFixRt = baseNowRt
            trackerSpeed = 20.0
            health.isLocationPending = true
        }
        
        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, false, spikeLogger, onTrigger, onResolve)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        
        assertFalse("Geofence should be suppressed by Bayesian expansion during gap", geofence?.conditionMet == true)
    }

    @Test
    fun `Verify Geofence Recovery Hysteresis`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        
        val state = createDefaultState(now).apply {
            this.nowRt = baseNowRt
            trackerLat = 10.0012 
            wasDistanceViolated = true
            distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
            maxTrackerAccuracy = 5.0
            lastGpsPacketRt = baseNowRt
        }

        state.trackerLat = 10.00114 
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, false, spikeLogger, onTrigger, onResolve)
        assertTrue("Violation should persist in hysteresis zone", state.wasDistanceViolated)

        state.trackerLat = 10.00108 
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, false, spikeLogger, onTrigger, onResolve)
        assertFalse("Violation should clear inside hysteresis zone", state.wasDistanceViolated)
    }

    @Test
    fun `Verify Jump hold duration`() {
        val now = 1700000000000L
        val baseNowRt = 100000L
        val state = createDefaultState(now).apply {
            trackerLat = 10.005 
            jumpTier = 2
            firstViolationTs = now
            firstViolationRt = baseNowRt
            firstViolationWasJump = true
            health.status = SentinelStatus.JUMP
        }

        state.now = now + 120000
        state.nowRt = baseNowRt + 120000
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, false, spikeLogger, onTrigger, onResolve)
        assertFalse("Jump should be held", 
            report1.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)

        state.now = now + 420000
        state.nowRt = baseNowRt + 420000
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, false, spikeLogger, onTrigger, onResolve)
        assertTrue("Jump hold should expire", 
            report2.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }?.conditionMet == true)
    }

    @Test
    fun `Verify Forensic Persistence Reliability Alerting`() {
        val now = 1700000000000L
        val testNowRt = 100000L
        val state = createDefaultState(now).apply {
            this.nowRt = testNowRt
            health.forensicReliability = 0.8 
        }

        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, false, spikeLogger, onTrigger, onResolve)
        assertFalse("Alert should not trigger immediately", report1.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }?.conditionMet == true)

        state.nowRt = testNowRt + 31000
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, false, spikeLogger, onTrigger, onResolve)
        assertTrue("Alert should trigger after 30s", report2.reports.find { it.type == ALERT_ID_PERFORMANCE_SPIKE }?.conditionMet == true)
    }

    @Test
    fun `Verify Silent Failure Correlation Detection`() {
        val state = createDefaultState().apply {
            health.gpsStalled = true
            health.cpuLoad = 0.9    
        }
        
        val report1 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report1, false, spikeLogger, onTrigger, onResolve)
        assertTrue("Silent Failure should trigger due to high CPU", report1.reports.find { it.type == ALERT_ID_SILENT_FAILURE }?.conditionMet == true)

        state.health.isTamperDetected = true
        val report2 = SystemHealthReport()
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report2, false, spikeLogger, onTrigger, onResolve)
        assertFalse("Silent Failure should be suppressed if tamper is detected", report2.reports.find { it.type == ALERT_ID_SILENT_FAILURE }?.conditionMet == true)
    }
}
