package com.gps19.core.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AlarmStealthTest: Validating R872 Absolute Stealth requirements.
 * Targets TEST_PROCEDURE.md Chapters 3.2, 4.2, and 4.9.
 */
class AlarmStealthTest {

    private val mockTimeProvider = MockTimeProvider()
    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }

    private fun createTrackerState(): AlarmEvaluationState {
        val now = 1700000000000L
        val baseNowRt = 100000L
        return AlarmEvaluationState().apply {
            update(
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
                isTrackerMode = true, // TRACKER MODE
                capabilities = HardwareCapabilities(
                    hasBackgroundRestriction = false,
                    backgroundStatus = CapabilityStatus.GRANTED,
                    autostartStatus = CapabilityStatus.GRANTED,
                    isManualOverrideActive = false
                )
            )
            health.apply {
                isHardwareOnline = true
                status = SentinelStatus.VALID
            }
        }
    }

    private fun createViewerState(): AlarmEvaluationState {
        return createTrackerState().apply {
            isTrackerMode = false // VIEW MODE
        }
    }

    @Test
    fun `Verify Absolute Stealth - Tracker Mode logs but never triggers siren (R872)`() {
        val state = createTrackerState()
        state.health.isPowerTamper = true
        state.health.currentMa = -100 // Trigger Power Alarm

        var sirenTriggered = false
        var logTriggered = false

        val onTrigger: (AlarmEvaluationState.ActiveAlarm) -> Unit = {
            sirenTriggered = true
        }
        val onTriggerMuted: (AlarmEvaluationState.ActiveAlarm) -> Unit = {
            logTriggered = true
        }

        // Act: Run logic with isLockedOut = true to simulate Stealth/Mute restriction
        MainAlarmLogic.detectViolations(
            state = state,
            timeProvider = mockTimeProvider,
            report = SystemHealthReport(),
            isLockedOut = true, // Tracker mode must always be effectively 'locked out' for UI/Audio
            onSpike = spikeLogger,
            onTrigger = onTrigger,
            onResolve = { _, _ -> },
            onTriggerMuted = onTriggerMuted
        )

        assertFalse("Tracker MUST NOT trigger audible/visual alarm (R872)", sirenTriggered)
        assertTrue("Tracker SHOULD log the violation internally", logTriggered)
    }

    @Test
    fun `Verify Alarm Authority - Viewer Mode triggers full alert`() {
        val state = createViewerState()
        state.health.isPowerTamper = true
        state.health.currentMa = -100

        var sirenTriggered = false

        val onTrigger: (AlarmEvaluationState.ActiveAlarm) -> Unit = {
            sirenTriggered = true
        }

        // Act: Viewer mode is NOT locked out
        MainAlarmLogic.detectViolations(
            state = state,
            timeProvider = mockTimeProvider,
            report = SystemHealthReport(),
            isLockedOut = false,
            onSpike = spikeLogger,
            onTrigger = onTrigger,
            onResolve = { _, _ -> }
        )

        assertTrue("Viewer MUST trigger audible/visual alarm for remote violations", sirenTriggered)
    }

    @Test
    fun `Verify Alarm Titles are Mode-Aware (Issue 1413)`() {
        val trackerState = createTrackerState()
        val viewerState = createViewerState()

        trackerState.isTrackerConnected = false
        viewerState.isTrackerConnected = false

        val trackerReport = SystemHealthReport()
        val viewerReport = SystemHealthReport()

        MainAlarmLogic.detectViolations(trackerState, mockTimeProvider, trackerReport, true, spikeLogger, {}, { _, _ -> })
        MainAlarmLogic.detectViolations(viewerState, mockTimeProvider, viewerReport, false, spikeLogger, {}, { _, _ -> })

        val trackerAlarm = trackerReport.reports.find { it.type == ALERT_ID_TRACKER_OFFLINE }
        val viewerAlarm = viewerReport.reports.find { it.type == ALERT_ID_TRACKER_OFFLINE }

        // ALERT_TITLE_VIEWER_OFFLINE = "Viewer: Offline"
        // ALERT_TITLE_TRACKER_OFFLINE = "Tracker: Offline"
        // getTrackerTitle strips the prefix based on mode.
        
        assertTrue("Tracker mode should see 'Viewer' as the offline entity", trackerAlarm?.title?.contains("Viewer") == true)
        assertTrue("Viewer mode should see 'Tracker' as the offline entity", viewerAlarm?.title?.contains("Tracker") == true)
    }
}
