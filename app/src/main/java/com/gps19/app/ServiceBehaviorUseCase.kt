package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceBehaviorUseCase: Encapsulates high-level logic for service-level state transitions.
 * Handles suspicious mode gates and dynamic GPS polling interval calculations.
 * Extracted from TrackerService to resolve Issue 115 (God Objects).
 * v8.8.35: Issue 148 - Implemented A15 stable polling (1000ms).
 */
@Singleton
class ServiceBehaviorUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private var lastSuspiciousTriggerTs = 0L
    private var lastMotionDetectedTs = 0L

    fun calculateGpsInterval(
        isCoolingMode: Boolean,
        isSuspiciousMode: Boolean,
        isStationary: Boolean,
        nowRealtime: Long,
        deviceSpecialFlags: DeviceSpecialFlags
    ): Long {
        if (!isStationary) {
            lastMotionDetectedTs = nowRealtime
        }
        val isStationaryState = isStationary && (nowRealtime - lastMotionDetectedTs > MOVING_HOLD_DURATION_MS)

        return when {
            isCoolingMode -> COOLING_GPS_POLLING_MS
            isSuspiciousMode -> SUSPICIOUS_GPS_POLLING_MS
            isStationaryState -> STATIONARY_GPS_POLLING_MS
            deviceSpecialFlags.isA15 -> A15_STABLE_GPS_POLLING_MS
            deviceSpecialFlags.isS21FE || deviceSpecialFlags.isXiaomi -> HIGH_FREQUENCY_GPS_POLLING_MS
            else -> MOVING_GPS_POLLING_MS
        }
    }

    fun updateSuspiciousMode(
        currentSuspicious: Boolean,
        isPhysicalViolation: Boolean,
        isSitDetected: Boolean,
        nowRealtime: Long
    ): Boolean {
        if (isPhysicalViolation || isSitDetected) {
            lastSuspiciousTriggerTs = nowRealtime
            return true
        }

        if (currentSuspicious && nowRealtime - lastSuspiciousTriggerTs > SUSPICIOUS_STATE_COOLDOWN_MS) {
            return false
        }

        return currentSuspicious
    }

    data class DeviceSpecialFlags(
        val isS21FE: Boolean,
        val isXiaomi: Boolean,
        val isA15: Boolean
    )
}
