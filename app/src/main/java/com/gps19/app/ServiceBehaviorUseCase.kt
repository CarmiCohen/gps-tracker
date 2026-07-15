package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceBehaviorUseCase: Encapsulates high-level logic for service-level state transitions.
 * v9.4.0:
 * - R406a: Unified Heartbeat (Issue #501). Removed dynamic GPS interval calculation.
 *   The system now uses a standardized 2s heartbeat (TICK_INTERVAL_MS) globally.
 */
@Singleton
class ServiceBehaviorUseCase @Inject constructor(
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
