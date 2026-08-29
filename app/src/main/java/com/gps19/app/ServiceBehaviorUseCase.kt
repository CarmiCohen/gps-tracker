package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceBehaviorUseCase: Encapsulates high-level logic for service-level state transitions.
 * Aug.29.09:
 * - Concern #764 Simplification: Refactored calculateGpsInterval to use 
 *   HardwareCapabilities directly, removing redundant DeviceSpecialFlags.
 * Aug.29.08:
 * - Concern #763: GNSS Relaxation. Added ultra-long stationary check to relax 
 *   polling to 5 minutes after 4 hours of confirmed immobility (R763).
 * Aug.14.01:
 * - Issue #169: Geofence Accuracy vs. Battery Audit. Added isGeofenceActive to 
 *   calculateGpsInterval to prevent 45s polling blind-spots when moving with 
 *   screen off (R406a).
 */
@Singleton
class ServiceBehaviorUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private var lastSuspiciousTriggerTs = 0L
    private var lastMotionDetectedTs = 0L

    /**
     * reset: Clears all behavioral latches (R141).
     */
    fun reset() {
        lastSuspiciousTriggerTs = 0L
        lastMotionDetectedTs = 0L
    }

    /**
     * Calculates the target GPS polling interval based on device state and forensic triggers.
     * R406a: Dynamic Polling.
     * R763: Ultra-long Stationary Relaxation.
     */
    fun calculateGpsInterval(
        isCoolingMode: Boolean,
        isSuspiciousMode: Boolean,
        isStationary: Boolean,
        isScreenOn: Boolean,
        isGeofenceActive: Boolean,
        nowRt: Long,
        capabilities: HardwareCapabilities
    ): Long {
        if (!isStationary) {
            lastMotionDetectedTs = nowRt
        }
        val stationaryDuration = nowRt - lastMotionDetectedTs
        val isStationaryState = isStationary && (stationaryDuration > MOVING_HOLD_DURATION_MS)
        val isUltraLongStationary = isStationaryState && (stationaryDuration > ULTRA_LONG_STATIONARY_DURATION_MS)

        return when {
            isCoolingMode -> COOLING_GPS_POLLING_MS
            isSuspiciousMode -> SUSPICIOUS_GPS_POLLING_MS
            isUltraLongStationary -> ULTRA_LONG_STATIONARY_GPS_POLLING_MS
            isStationaryState -> STATIONARY_GPS_POLLING_MS
            // Issue #169: If Geofence is active and we are moving, do NOT drop to 45s even if screen is off.
            !isScreenOn && !isGeofenceActive -> SCREEN_OFF_GPS_POLLING_MS
            capabilities.requiresAdaptationMuzzle || capabilities.requiresExtraTopPadding -> HIGH_FREQUENCY_GPS_POLLING_MS
            else -> MOVING_GPS_POLLING_MS
        }
    }

    /**
     * Issue #526: Evaluates if the system should enter logic-level power save.
     * Criteria: Physically stationary, GPS stalled, no pending alarms, and UI not visible.
     */
    fun evaluatePowerSaveMode(
        isStationary: Boolean,
        isGpsStalled: Boolean,
        hasUnresolvedAlarms: Boolean,
        isUiVisible: Boolean
    ): Boolean {
        return isStationary && isGpsStalled && !hasUnresolvedAlarms && !isUiVisible
    }

    /**
     * Manages the 'Suspicious' state latch, triggered by physical violations or sit detection.
     */
    fun updateSuspiciousMode(
        currentSuspicious: Boolean,
        isPhysicalViolation: Boolean,
        isSitDetected: Boolean,
        nowRt: Long
    ): Boolean {
        if (isPhysicalViolation || isSitDetected) {
            lastSuspiciousTriggerTs = nowRt
            return true
        }

        if (currentSuspicious && nowRt - lastSuspiciousTriggerTs > SUSPICIOUS_STATE_COOLDOWN_MS) {
            return false
        }

        return currentSuspicious
    }
}
