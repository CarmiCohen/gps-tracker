package com.gps19.app

import android.util.Log
import com.gps19.core.engine.*

/**
 * TrackerStateManager: Logic for mapping raw telemetry to high-level behavioral states.
 * v8.7.5:
 * - Constant Centralization: Inheriting core thresholds from :core:engine.
 */
object TrackerStateManager {
    private var currentState = TrackerState.UNKNOWN
    private var pendingState = TrackerState.UNKNOWN
    private var pendingStateStartTs = 0L
    private var lastMovingTs = 0L
    
    private var sustainedSpeedCount = 0
    private const val SUSTAINED_SPEED_THRESHOLD = 2 // pulses of >2m/s
    private const val SUSTAINED_SPEED_STATIONARY_THRESHOLD = 4 // pulses if no vibration

    private const val STATE_CONFIDENCE_BUFFER_MS = 2000L
    private const val PARKING_CONFIDENCE_BUFFER_MS = 5000L // Harder gate for Parking

    fun updateState(
        isVisualJump: Boolean,
        isTrajectoryPromoted: Boolean,
        speed: Float,
        vibration: Float,
        vibrationFloor: Float,
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
        val hasSpeed = speed >= ACTIVE_MOVE_THRESHOLD.toFloat()
        if (hasSpeed) {
            sustainedSpeedCount++
        } else {
            sustainedSpeedCount = 0
        }

        val isPhysicalMoving = vibration > (vibrationFloor * 2.0f) && vibration > 0.15f
        
        // R880: Require more evidence to exit PARKING if not physically moving
        val requiredSustainedCount = if (currentState == TrackerState.PARKING && !isPhysicalMoving) {
            SUSTAINED_SPEED_STATIONARY_THRESHOLD
        } else {
            SUSTAINED_SPEED_THRESHOLD
        }

        val isSpeedConfirmed = (sustainedSpeedCount >= requiredSustainedCount) || (speed > 5.0f) || isTrajectoryPromoted
        
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

    private fun logStateChange(old: TrackerState, new: TrackerState, speed: Float, promoted: Boolean) {
        Log.d("GPS19", "Tracker behavior changed: $old -> $new (Speed: ${"%.1f".format(speed)}, Promoted: $promoted)")
    }
}
