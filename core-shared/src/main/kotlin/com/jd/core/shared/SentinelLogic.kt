package com.jd.core.shared

import kotlin.math.*

/**
 * SentinelLogic: The core validation engine for Project Revolution.
 * Determines if a location update is a "Visual Jump" or valid movement.
 * Updated Revol31.8: Added Geofence Transition logic for Forensic Audit (ISSUE-REVOL30.1-F3).
 * Updated Nov2.1: Integrated Anomaly Correlation (R133) for Silent Failure detection.
 * Updated Nov2.21: Increased MAX_PHYSICAL_SPEED_MPS to 55.0 to support high-speed rail/transport (ISSUE-REVOL38.1-F1).
 */
object SentinelLogic {

    private const val MAX_PHYSICAL_SPEED_MPS = 55.0 // ~200 km/h
    private const val JUMP_GATE_SPEED_ACCURACY_LOW_MPS = 22.2
    private const val JUMP_GATE_ACCURACY_LOW_THRESHOLD = 40.0
    private const val ACCEL_LIMIT = 5.0 // m/s^2 (Raised from 2.0 to allow for rapid acceleration in vehicles)

    // R133 Hardened Thresholds
    const val SILENT_FAILURE_CPU_THRESHOLD = 0.85
    const val SILENT_FAILURE_IOW_THRESHOLD = 0.40
    const val SILENT_FAILURE_LATENCY_THRESHOLD_MS = 800L

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

    data class Geofence(
        val id: String,
        val lat: Double,
        val lng: Double,
        val radiusMeters: Double
    )

    enum class GeofenceState {
        INSIDE, OUTSIDE, UNKNOWN
    }

    enum class GeofenceTransition {
        ENTER, EXIT, NONE
    }

    data class GeofenceResult(
        val transition: GeofenceTransition,
        val currentState: GeofenceState,
        val distanceToCenter: Double
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
        if (speedMps > 138.8) { // > 500 km/h (Bullet train / Extreme outliers)
            return ValidationResult(SentinelStatus.OUTLIER, "Speed Outlier: ${speedMps.toInt()} m/s")
        }

        var jumpScore = 0
        
        // 2. Sensor Fusion: Speed without vibration (e.g. GPS drift while stationary)
        if (!hasPhysicalMotion && speedMps > 3.0) {
            jumpScore += 60
        }

        // 3. Accuracy Recovery: Suppression of "Snaps"
        val isAccuracyImproving = last.accuracy > 0 && current.accuracy < (last.accuracy * 0.8)
        val isWithinPreviousError = dist < last.accuracy
        if (isAccuracyImproving && isWithinPreviousError) {
            return ValidationResult(SentinelStatus.VALID, "Suppressed Accuracy Snap")
        }

        // 4. Kinematic Consistency (Acceleration)
        // Use the calculated speed of the current segment for future comparisons
        val accel = abs(speedMps - last.speed) / timeDeltaSec
        if (accel > ACCEL_LIMIT && dist > 15.0) {
            jumpScore += 40
        }

        return when {
            jumpScore >= 50 -> ValidationResult(SentinelStatus.JUMP, "Kinematic Violation", jumpScore)
            speedMps > MAX_PHYSICAL_SPEED_MPS -> ValidationResult(SentinelStatus.JUMP, "Speed Threshold Exceeded", jumpScore)
            else -> ValidationResult(SentinelStatus.VALID)
        }
    }

    /**
     * isSilentFailure: Detects Anomaly Correlation (R133).
     * Returns true if a GPS stall is correlated with high hardware resource load.
     */
    fun isSilentFailure(
        gpsStalled: Boolean,
        isTamperDetected: Boolean,
        cpuLoad: Double,
        ioWait: Double,
        maxIoLatency: Long,
        isThermalThrottling: Boolean
    ): Boolean {
        if (!gpsStalled || isTamperDetected) return false

        return cpuLoad >= SILENT_FAILURE_CPU_THRESHOLD ||
               ioWait >= SILENT_FAILURE_IOW_THRESHOLD ||
               maxIoLatency >= SILENT_FAILURE_LATENCY_THRESHOLD_MS ||
               isThermalThrottling
    }

    /**
     * Evaluates a location update against a geofence to detect transitions.
     * Uses nanosecond precision from monotonicNanos for forensic logging.
     */
    fun evaluateGeofenceTransition(
        update: LocationUpdate,
        geofence: Geofence,
        previousState: GeofenceState
    ): GeofenceResult {
        val dist = SpatialMath.calculateDistance(update.lat, update.lng, geofence.lat, geofence.lng)
        val currentState = if (dist <= geofence.radiusMeters) GeofenceState.INSIDE else GeofenceState.OUTSIDE
        
        val transition = when {
            previousState == GeofenceState.INSIDE && currentState == GeofenceState.OUTSIDE -> GeofenceTransition.EXIT
            previousState == GeofenceState.OUTSIDE && currentState == GeofenceState.INSIDE -> GeofenceTransition.ENTER
            else -> GeofenceTransition.NONE
        }

        return GeofenceResult(transition, currentState, dist)
    }
}
