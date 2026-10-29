package com.gps19.core.shared

import kotlin.math.*

/**
 * SentinelLogic: The core validation engine for Project Revolution.
 * Determines if a location update is a "Visual Jump" or valid movement.
 */
object SentinelLogic {

    private const val MAX_PHYSICAL_SPEED_MPS = 33.33 // ~120 km/h
    private const val JUMP_GATE_SPEED_ACCURACY_LOW_MPS = 22.2
    private const val JUMP_GATE_ACCURACY_LOW_THRESHOLD = 40.0
    private const val ACCEL_LIMIT = 2.0 // m/s^2

    enum class SentinelStatus {
        VALID,
        JUMP,
        STALLED,
        TAMPER,
        OUTLIER
    }

    data class ValidationResult(
        val status: SentinelStatus,
        val reason: String = "",
        val score: Int = 0
    )

    /**
     * Validates if the movement from [last] to [current] is physically possible.
     */
    fun validateMovement(
        last: LocationUpdate,
        current: LocationUpdate,
        hasPhysicalMotion: Boolean = true
    ): ValidationResult {
        if (last.lat == 0.0) return ValidationResult(SentinelStatus.VALID)

        val dist = SpatialMath.calculateDistance(last.lat, last.lng, current.lat, current.lng)
        val timeDeltaSec = max(0.1, (current.timestamp - last.timestamp) / 1000.0)
        val speedMps = dist / timeDeltaSec

        // 1. Hard Outlier Check
        if (speedMps > 83.33) { // > 300 km/h
            return ValidationResult(SentinelStatus.OUTLIER, "Speed Outlier: ${speedMps.toInt()} m/s")
        }

        var jumpScore = 0
        
        // 2. Sensor Fusion: Speed without vibration (e.g. GPS drift while stationary)
        if (!hasPhysicalMotion && speedMps > 2.0) {
            jumpScore += 60
        }

        // 3. Accuracy Recovery: Suppression of "Snaps"
        val isAccuracyImproving = last.accuracy > 0 && current.accuracy < (last.accuracy * 0.8)
        val isWithinPreviousError = dist < last.accuracy
        if (isAccuracyImproving && isWithinPreviousError) {
            return ValidationResult(SentinelStatus.VALID, "Suppressed Accuracy Snap")
        }

        // 4. Kinematic Consistency (Acceleration)
        val accel = abs(speedMps - last.speed) / timeDeltaSec
        if (accel > ACCEL_LIMIT && dist > 10.0) {
            jumpScore += 40
        }

        return when {
            jumpScore >= 50 -> ValidationResult(SentinelStatus.JUMP, "Kinematic Violation", jumpScore)
            speedMps > MAX_PHYSICAL_SPEED_MPS -> ValidationResult(SentinelStatus.JUMP, "Speed Threshold Exceeded", jumpScore)
            else -> ValidationResult(SentinelStatus.VALID)
        }
    }
}
