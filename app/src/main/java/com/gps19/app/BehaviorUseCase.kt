package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * BehaviorUseCase: Logic for determining high-level behavioral states and UI visibility gates.
 * July.28.24:
 * - Issue #620: State Partitioning Audit. Refactored to accept partitioned 
 *   KinematicState and DiagnosticState to isolation high-frequency motion from 
 *   scalar diagnostics, reducing re-computation churn.
 */
class BehaviorUseCase @Inject constructor() {

    /**
     * computeTrackerStateDecomposed: Determines the tracker state using partitioned data.
     * KinematicState provides motion authority; DiagnosticState provides connectivity context.
     */
    fun computeTrackerStateDecomposed(
        uiState: MainUiState,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        systemTimePulse: Long
    ): TrackerState {
        val appMode = uiState.appMode ?: return TrackerState.UNKNOWN
        
        if (appMode == "viewer") {
            return kinematicState.trackerLocation.trackerState
        }

        val isConnected = if (appMode == "tracker") {
            diagnosticState.connectivity.isRelayConnected
        } else {
            diagnosticState.connectivity.isTrackerConnected
        }

        val effectiveLocation = if (appMode == "tracker") {
            kinematicState.localLocation
        } else {
            kinematicState.trackerLocation
        }

        val effectiveHealth = if (appMode == "tracker") {
            kinematicState.localHealth
        } else {
            kinematicState.trackerHealth
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

    /**
     * shouldShowRedScreenDecomposed: Gates the red alert screen using partitioned data.
     */
    fun shouldShowRedScreenDecomposed(
        uiState: MainUiState,
        kinematicState: KinematicState,
        diagnosticState: DiagnosticState,
        nowRt: Long,
        lastAckRt: Long,
        currentVisible: Boolean
    ): Boolean {
        if (uiState.appMode == "tracker") return false

        val hasAlertToDisplay = diagnosticState.activeAlarms.any { !it.isResolved }
        if (!hasAlertToDisplay) return false

        val lockoutActive = (nowRt - lastAckRt < ALARM_OVERLAY_THROTTLE_MS)
        
        return if (!lockoutActive || diagnosticState.isNewViolationDetected) {
            true
        } else {
            currentVisible
        }
    }

    fun isAlarmSilenced(lastAckTs: Long, now: Long): Boolean {
        return (now - lastAckTs < SILENCE_AUTO_RECOVERY_MS)
    }
}
