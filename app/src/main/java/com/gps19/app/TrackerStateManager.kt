package com.gps19.app

import android.util.Log
import com.gps19.core.engine.*

/**
 * TrackerStateManager: Logic for mapping raw telemetry to high-level behavioral states.
 * v8.9.79:
 * - Issue #016: Standardized telemetry parameters to Double to eliminate casting overhead.
 */
object TrackerStateManager {
    private var currentState = TrackerState.UNKNOWN
    private var pendingState = TrackerState.UNKNOWN
    private var pendingStateStartTs = 0L
    private var lastMovingTs = 0L
    
    private var sustainedSpeedCount = 0

    fun updateState(
        isVisualJump: Boolean,
        isTrajectoryPromoted: Boolean,
        speed: Double,
        vibration: Double,
        vibrationFloor: Double,
        isTrackerConnected: Boolean,
        systemTimePulse: Long
    ): TrackerState {
        // 1. Connectivity check is immediate
        if (!isTrackerConnected) {
            currentState = TrackerState.UNKNOWN
            pendingState = TrackerState.UNKNOWN
            sustainedSpeedCount = 0
            return currentState
        }

        // 2. Multi-Factor Movement Detection
        val hasSpeed = speed >= ACTIVE_MOVE_THRESHOLD
        if (hasSpeed) {
            sustainedSpeedCount++
        } else {
            sustainedSpeedCount = 0
        }

        // Issue #318: Use unified constants from EngineConstants
        val isPhysicalMoving = vibration > (vibrationFloor * STATIONARY_FLOOR_MULT) && vibration > VIBRATION_STATIONARY_THRESHOLD
        
        // R880: Require more evidence to exit PARKING if not physically moving
        val requiredSustainedCount = if (currentState == TrackerState.PARKING && !isPhysicalMoving) {
            SUSTAINED_SPEED_STATIONARY_THRESHOLD
        } else {
            SUSTAINED_SPEED_THRESHOLD
        }

        // Issue #302: Unified high speed promotion threshold
        val isSpeedConfirmed = (sustainedSpeedCount >= requiredSustainedCount) || (speed > HIGH_SPEED_PROMOTION_THRESHOLD) || isTrajectoryPromoted
        
        // Confirmation: Must have sustained speed OR speed confirmed by vibration
        val isMovingNow = isSpeedConfirmed || (hasSpeed && isPhysicalMoving)

        if (isMovingNow) {
            lastMovingTs = systemTimePulse
        }

        val inMovingHold = lastMovingTs > 0L && (systemTimePulse - lastMovingTs < MOVING_HOLD_DURATION_MS)
        
        val targetState = when {
            isVisualJump && !isTrajectoryPromoted -> TrackerState.JUMPING
            isMovingNow || inMovingHold -> TrackerState.MOVING
            else -> TrackerState.PARKING
        }

        // 3. Apply Confidence Buffer (Hysteresis)
        // Issue #302: Centralized confidence buffers
        val requiredBuffer = if (targetState == TrackerState.PARKING) PARKING_CONFIDENCE_BUFFER_MS else STATE_CONFIDENCE_BUFFER_MS

        if (targetState == currentState || targetState == TrackerState.JUMPING) {
            if (targetState != currentState) {
                logStateChange(currentState, targetState, speed, isTrajectoryPromoted)
                currentState = targetState
            }
            pendingState = targetState
            pendingStateStartTs = 0L
        } else {
            if (targetState != pendingState) {
                pendingState = targetState
                pendingStateStartTs = systemTimePulse
            } else if (pendingStateStartTs != 0L && (systemTimePulse - pendingStateStartTs >= requiredBuffer)) {
                logStateChange(currentState, pendingState, speed, isTrajectoryPromoted)
                currentState = pendingState
                pendingStateStartTs = 0L
            }
        }

        return currentState
    }

    private fun logStateChange(old: TrackerState, new: TrackerState, speed: Double, promoted: Boolean) {
        Log.d("GPS19", "Tracker behavior changed: $old -> $new (Speed: ${"%.1f".format(speed)}, Promoted: $promoted)")
    }
}
