package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber

/**
 * TrackerStateManager: Logic for mapping raw telemetry to high-level behavioral states.
 * Sep.02.50:
 * - Issue #005 Hardening: Replaced all android.util.Log calls with Timber to 
 *   ensure log spillage protection on Samsung A15/G990 hardware (R759).
 * July.1.16:
 * - Issue #512: Consolidate Sentinel Statuses. Replaced isVisualJump with SentinelStatus.
 */
object TrackerStateManager {
    private var currentState = TrackerState.UNKNOWN
    private var pendingState = TrackerState.UNKNOWN
    private var pendingStateStartTs = 0L
    private var lastMovingTs = 0L
    
    private var sustainedSpeedCount = 0

    fun updateState(
        status: SentinelStatus,
        speed: Double,
        vibration: Double,
        vibrationFloor: Double,
        isTrackerConnected: Boolean,
        systemTimePulse: Long
    ): TrackerState {
        if (!isTrackerConnected) {
            currentState = TrackerState.UNKNOWN
            pendingState = TrackerState.UNKNOWN
            sustainedSpeedCount = 0
            return currentState
        }

        val hasSpeed = speed >= ACTIVE_MOVE_THRESHOLD
        if (hasSpeed) {
            sustainedSpeedCount++
        } else {
            sustainedSpeedCount = 0
        }

        val isPhysicalMoving = vibration > (vibrationFloor * STATIONARY_FLOOR_MULT) && vibration > VIBRATION_STATIONARY_THRESHOLD
        
        val requiredSustainedCount = if (currentState == TrackerState.PARKING && !isPhysicalMoving) {
            SUSTAINED_SPEED_STATIONARY_THRESHOLD
        } else {
            SUSTAINED_SPEED_THRESHOLD
        }

        val isSpeedConfirmed = (sustainedSpeedCount >= requiredSustainedCount) || (speed > HIGH_SPEED_PROMOTION_THRESHOLD)
        
        val isMovingNow = isSpeedConfirmed || (hasSpeed && isPhysicalMoving)

        if (isMovingNow) {
            lastMovingTs = systemTimePulse
        }

        val inMovingHold = lastMovingTs > 0L && (systemTimePulse - lastMovingTs < MOVING_HOLD_DURATION_MS)
        
        val targetState = when {
            status == SentinelStatus.JUMP -> TrackerState.JUMPING
            isMovingNow || inMovingHold -> TrackerState.MOVING
            else -> TrackerState.PARKING
        }

        val requiredBuffer = if (targetState == TrackerState.PARKING) PARKING_CONFIDENCE_BUFFER_MS else STATE_CONFIDENCE_BUFFER_MS

        if (targetState == currentState || targetState == TrackerState.JUMPING) {
            if (targetState != currentState) {
                logStateChange(currentState, targetState, speed)
                currentState = targetState
            }
            pendingState = targetState
            pendingStateStartTs = 0L
        } else {
            if (targetState != pendingState) {
                pendingState = targetState
                pendingStateStartTs = systemTimePulse
            } else if (pendingStateStartTs != 0L && (systemTimePulse - pendingStateStartTs >= requiredBuffer)) {
                logStateChange(currentState, pendingState, speed)
                currentState = pendingState
                pendingStateStartTs = 0L
            }
        }

        return currentState
    }

    private fun logStateChange(old: TrackerState, new: TrackerState, speed: Double) {
        Timber.tag("GPS19").d("Tracker behavior changed: $old -> $new (Speed: ${"%.1f".format(speed)})")
    }
}
