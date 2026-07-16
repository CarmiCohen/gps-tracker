package com.gps19.app

import com.gps19.core.engine.*

/**
 * BehaviorUseCase: Logic for determining high-level behavioral states and UI visibility gates.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class BehaviorUseCase {

    fun computeTrackerState(
        currentState: MainUiState,
        systemTimePulse: Long
    ): TrackerState {
        val appMode = currentState.appMode ?: return TrackerState.UNKNOWN
        
        // Issue #046 Fix: If we are a Viewer, the Tracker is the authority on its own state.
        if (appMode == "viewer") {
            return currentState.trackerLocation.trackerState
        }

        val isConnected = if (appMode == "tracker") {
            currentState.connectivity.isRelayConnected
        } else {
            currentState.connectivity.isTrackerConnected
        }

        val effectiveLocation = if (appMode == "tracker") {
            currentState.localLocation
        } else {
            currentState.trackerLocation
        }

        val effectiveHealth = if (appMode == "tracker") {
            currentState.localHealth
        } else {
            currentState.trackerHealth
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
        currentState: MainUiState,
        nowRealtime: Long,
        lastAckRealtime: Long,
        currentVisible: Boolean
    ): Boolean {
        // R872: No visual alerts (red screen) in tracker mode.
        if (currentState.appMode == "tracker") return false

        val hasAlertToDisplay = currentState.activeAlarms.any { !it.isResolved }
        if (!hasAlertToDisplay) return false

        // Use monotonic time for lockout to survive system clock jumps
        val lockoutActive = (nowRealtime - lastAckRealtime < ALARM_OVERLAY_THROTTLE_MS)
        
        return if (!lockoutActive || currentState.isNewViolationDetected) {
            true
        } else {
            currentVisible
        }
    }

    fun isAlarmSilenced(lastAckTs: Long, now: Long): Boolean {
        return (now - lastAckTs < SILENCE_AUTO_RECOVERY_MS)
    }
}
