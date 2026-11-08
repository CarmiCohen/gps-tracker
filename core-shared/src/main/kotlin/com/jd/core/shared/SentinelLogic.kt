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

        // 0. GPS error: a displacement inside the fixes' own error radius is noise, not movement (ISSUE-NOV7.17-O42).
        // A still A15 reported 23 m in 1.6 s between fixes of 68 m and 59 m accuracy as a jump.
        if (dist < max(last.accuracy, current.accuracy)) {
            return ValidationResult(SentinelStatus.VALID, "Within GPS error")
        }

        // 1. Hard Outlier Check
        if (speedMps > 138.8) { // > 500 km/h (Bullet train / Extreme outliers)
            return ValidationResult(SentinelStatus.OUTLIER, "Speed Outlier: ${speedMps.toInt()} m/s")
        }

        var jumpScore = 0
        
        // 2. Sensor Fusion: Speed without vibration (e.g. GPS drift while stationary)
        if (!hasPhysicalMotion && speedMps > 3.0) {
            jumpScore += 60
        }

        // 3. Kinematic Consistency (Acceleration)
        // Use the calculated speed of the current segment for future comparisons
        val accel = abs(speedMps - last.speed) / timeDeltaSec
        if (accel > ACCEL_LIMIT && dist > 15.0) {
            jumpScore += 40
        }

        // The measured movement, kept in the reason for the forensic log.
        val measured = "%.1f m/s, %.0f m in %.1f s, accuracy %.0f to %.0f m"
            .format(java.util.Locale.ROOT, speedMps, dist, timeDeltaSec, last.accuracy, current.accuracy)
        return when {
            jumpScore >= 50 -> ValidationResult(SentinelStatus.JUMP, "Kinematic Violation: $measured", jumpScore)
            speedMps > MAX_PHYSICAL_SPEED_MPS -> ValidationResult(SentinelStatus.JUMP, "Speed Threshold Exceeded: $measured", jumpScore)
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

    /**
     * Checks a fix against the user's Home Points (ISSUE-NOV8.13-O51). The tracker is INSIDE when it is within
     * the radius of any point, and OUTSIDE only when even the near edge of its GPS error circle is beyond the
     * radius of every point; in between, [previousState] is kept, so GPS noise at the boundary raises no alarm.
     * With [previousState] UNKNOWN (a new start or a changed geofence) the state is set without a transition:
     * only leaving home is a breach, not being away when the check begins.
     * [GeofenceResult.distanceToCenter] is the distance to the nearest point; an inactive config yields UNKNOWN.
     */
    fun evaluateHomeZone(
        lat: Double,
        lng: Double,
        accuracyMeters: Float,
        config: GeofenceConfig,
        previousState: GeofenceState
    ): GeofenceResult {
        if (!config.isActive) return GeofenceResult(GeofenceTransition.NONE, GeofenceState.UNKNOWN, -1.0)

        val nearest = config.points.minOf { SpatialMath.calculateDistance(lat, lng, it.lat, it.lng) }
        val measured = when {
            nearest <= config.radiusMeters -> GeofenceState.INSIDE
            nearest - max(0f, accuracyMeters) > config.radiusMeters -> GeofenceState.OUTSIDE
            else -> previousState
        }
        val transition = when {
            previousState == GeofenceState.INSIDE && measured == GeofenceState.OUTSIDE -> GeofenceTransition.EXIT
            previousState == GeofenceState.OUTSIDE && measured == GeofenceState.INSIDE -> GeofenceTransition.ENTER
            else -> GeofenceTransition.NONE
        }
        return GeofenceResult(transition, measured, nearest)
    }
}
