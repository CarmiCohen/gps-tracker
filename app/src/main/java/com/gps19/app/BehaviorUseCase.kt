package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * BehaviorUseCase: Logic for determining high-level behavioral states and UI visibility gates.
 * July.24.08:
 * - Issue #547: State Decomposition. Refactored to accept TelemetryState 
 *   to reduce heap churn and mitigate kernel performance issues.
 */
class BehaviorUseCase @Inject constructor() {

    fun computeTrackerState(
        uiState: MainUiState,
        telemetryState: TelemetryState,
        systemTimePulse: Long
    ): TrackerState {
        val appMode = uiState.appMode ?: return TrackerState.UNKNOWN
        
        // Issue #046 Fix: If we are a Viewer, the Tracker is the authority on its own state.
        if (appMode == "viewer") {
            return telemetryState.trackerLocation.trackerState
        }

        val isConnected = if (appMode == "tracker") {
            telemetryState.connectivity.isRelayConnected
        } else {
            telemetryState.connectivity.isTrackerConnected
        }

        val effectiveLocation = if (appMode == "tracker") {
            telemetryState.localLocation
        } else {
            telemetryState.trackerLocation
        }

        val effectiveHealth = if (appMode == "tracker") {
            telemetryState.localHealth
        } else {
            telemetryState.trackerHealth
        }

        return TrackerStateManager.updateState(
            status = effectiveLocation.status,
            speed = effectiveLocation.speed,
            vibration = effectiveHealth.vibration,
            vibrationFloor = effectiveHealth.adaptiveVibrationFloor,
            isTrackerConnected = isConnected,
            systemTimePulse = systemTimePulse
        )
    }

    fun shouldShowRedScreen(
        uiState: MainUiState,
        telemetryState: TelemetryState,
        nowRt: Long,
        lastAckRt: Long,
        currentVisible: Boolean
    ): Boolean {
        // R872: No visual alerts (red screen) in tracker mode.
        if (uiState.appMode == "tracker") return false

        val hasAlertToDisplay = telemetryState.activeAlarms.any { !it.isResolved }
        if (!hasAlertToDisplay) return false

        // Use monotonic time for lockout to survive system clock jumps
        val lockoutActive = (nowRt - lastAckRt < ALARM_OVERLAY_THROTTLE_MS)
        
        return if (!lockoutActive || telemetryState.isNewViolationDetected) {
            true
        } else {
            currentVisible
        }
    }

    fun isAlarmSilenced(lastAckTs: Long, now: Long): Boolean {
        return (now - lastAckTs < SILENCE_AUTO_RECOVERY_MS)
    }
}
