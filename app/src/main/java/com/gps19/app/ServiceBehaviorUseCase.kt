package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceBehaviorUseCase: Encapsulates high-level logic for service-level state transitions.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 * July.21.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized 'nowRt'.
 * - Maintained Hilt compatibility as per hardened Golden Master architecture.
 * - Restored dynamic GPS polling interval calculations and suspicious mode gates.
 */
@Singleton
class ServiceBehaviorUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private var lastSuspiciousTriggerTs = 0L
    private var lastMotionDetectedTs = 0L

    /**
     * Calculates the target GPS polling interval based on device state and forensic triggers.
     */
    fun calculateGpsInterval(
        isCoolingMode: Boolean,
        isSuspiciousMode: Boolean,
        isStationary: Boolean,
        isScreenOn: Boolean,
        nowRt: Long,
        deviceSpecialFlags: DeviceSpecialFlags
    ): Long {
        if (!isStationary) {
            lastMotionDetectedTs = nowRt
        }
        val isStationaryState = isStationary && (nowRt - lastMotionDetectedTs > MOVING_HOLD_DURATION_MS)

        return when {
            isCoolingMode -> COOLING_GPS_POLLING_MS
            isSuspiciousMode -> SUSPICIOUS_GPS_POLLING_MS
            isStationaryState -> STATIONARY_GPS_POLLING_MS
            !isScreenOn -> SCREEN_OFF_GPS_POLLING_MS 
            deviceSpecialFlags.isS21FE || deviceSpecialFlags.isXiaomi -> HIGH_FREQUENCY_GPS_POLLING_MS
            else -> MOVING_GPS_POLLING_MS
        }
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

    data class DeviceSpecialFlags(
        val isS21FE: Boolean,
        val isXiaomi: Boolean
    )
}
