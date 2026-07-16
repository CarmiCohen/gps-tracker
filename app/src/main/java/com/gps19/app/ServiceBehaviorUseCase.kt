package com.gps19.app

import com.gps19.core.engine.*

/**
 * ServiceBehaviorUseCase: Encapsulates high-level logic for service-level state transitions.
 * v9.5.0:
 * - Issue #503: Hilt Removal. Manual DI transition.
 */
class ServiceBehaviorUseCase(
    private val timeProvider: TimeProvider
) {
    private var lastSuspiciousTriggerTs = 0L

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
        val isXiaomi: Boolean
    )
}
