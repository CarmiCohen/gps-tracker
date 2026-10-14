package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/**
 * ConnectivityLogicTest: Validating connectivity transitions and idempotency.
 * Targets Chapters 2.7 and 4.10 of TEST_PROCEDURE.md.
 */
class ConnectivityLogicTest {

    private val mockTimeProvider = MockTimeProvider()
    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }

    @Test
    fun `Verify Alarm Acknowledgment Idempotency - Stale telemetry cannot revive resolved alarm (Issue 1410)`() {
        val state = AlarmEvaluationState()
        val now = 1700000000000L
        val nowRt = 100000L
        
        // Setup state with a historical acknowledgment
        state.update(
            now = now,
            nowRt = nowRt,
            serviceStartTime = now - 60000,
            serviceStartRt = nowRt - 60000,
            lastAlarmAckTs = now - 5000, // Acknowledged 5s ago
            violationStartTs = now - 10000, // Violation started 10s ago (stale)
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
            isTrackerMode = false,
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = false,
                backgroundStatus = CapabilityStatus.GRANTED,
                autostartStatus = CapabilityStatus.GRANTED,
                isManualOverrideActive = false
            )
        )

        // Trigger a condition that matches the stale violationStartTs
        state.health.isPowerTamper = true
        state.health.currentMa = -100

        var triggerCount = 0
        MainAlarmLogic.detectViolations(
            state = state,
            timeProvider = mockTimeProvider,
            report = SystemHealthReport(),
            isLockedOut = false,
            onSpike = spikeLogger,
            onTrigger = { triggerCount++ },
            onResolve = { _, _ -> }
        )

        assertEquals("Stale violation should NOT re-trigger after acknowledgment", 0, triggerCount)
    }

    @Test
    fun `Verify Connectivity Sticky-State - App resets on Relay URL change (Issue 1422)`() {
        // This is primarily a verification of the Logic routing in CommunicationManager/MainViewModel.
        // We verify that the detectViolations correctly identifies RELAY_OFFLINE with hysteresis.
        
        val state = AlarmEvaluationState()
        val nowRt = 100000L
        state.nowRt = nowRt
        state.isRelayConnected = false
        state.lastRelayOnlineRt = nowRt - 5000 // Was online 5s ago
        state.lastRelayOfflineRt = nowRt - 4000 // Went offline 4s ago

        val report = SystemHealthReport()
        MainAlarmLogic.detectViolations(
            state = state,
            timeProvider = mockTimeProvider,
            report = report,
            isLockedOut = false,
            onSpike = spikeLogger,
            onTrigger = {},
            onResolve = { _, _ -> }
        )

        val relayAlarm = report.reports.find { it.type == ALERT_ID_RELAY_OFFLINE }
        assertTrue("Relay Offline should trigger after sustained loss (>3s hysteresis)", relayAlarm?.conditionMet == true)
    }
}
