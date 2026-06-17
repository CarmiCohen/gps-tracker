package com.gps19.core.engine

import kotlin.math.*

/**
 * PhysicsUtils: Unified physics and geodesic calculations for the Pure Logic Engine.
 */
object PhysicsUtils {

    /**
     * Calculates the distance between two points in meters using the Haversine formula.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        if (lat1 == lat2 && lon1 == lon2) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val clampedA = a.coerceIn(0.0, 1.0)
        val dist = EARTH_RADIUS_METERS * 2 * atan2(sqrt(clampedA), sqrt(1.0 - clampedA))

        return if (dist.isNaN()) 0.0 else dist
    }

    /**
     * Checks if a location is valid (not (0,0), not NaN, and within global bounds).
     */
    fun isValidLocation(lat: Double, lng: Double): Boolean {
        if (lat.isNaN() || lng.isNaN()) return false
        if (abs(lat) < 0.000001 && abs(lng) < 0.000001) return false
        if (abs(lat) > 90.0 || abs(lng) > 180.0) return false
        return true
    }

    /**
     * Adaptive Multi-Factor Jump Engine logic.
     */
    fun isVisualJump(
        lastLat: Double, lastLng: Double, 
        newLat: Double, newLng: Double, 
        timeDeltaMs: Long, accuracy: Float,
        lastSpeedMps: Double = 0.0,
        isParking: Boolean = false,
        altitudeDelta: Double = 0.0,
        hasPhysicalMotion: Boolean = true
    ): JumpConfidence {
        if (lastLat == 0.0 || timeDeltaMs < 100) return JumpConfidence()
        
        val dist = calculateDistance(lastLat, lastLng, newLat, newLng)
        val timeDeltaSec = timeDeltaMs / 1000.0
        val speedMps = dist / timeDeltaSec
        
        // Tier 1: Outlier Filter
        if (dist > OUTLIER_DISTANCE_THRESHOLD || speedMps > OUTLIER_SPEED_CAP_MPS) {
            return JumpConfidence(score = 100, isJump = true, isOutlier = true, tier = 1, reason = "Hardware/Cold-Start Outlier")
        }
        
        var score = 0
        
        // Sensor Fusion: GPS-IMU Discrepancy
        if (!hasPhysicalMotion && speedMps > 10.0) { // Using 10.0 as gate for sensor mismatch
            score += 60 // JUMP_WEIGHT_SENSOR_MISMATCH
        }
        
        // Velocity Inertia (Acceleration Check)
        val accel = abs(speedMps - lastSpeedMps) / timeDeltaSec
        val accelLimit = if (isParking) 1.0 else 2.0 // MAX_TRACTOR_ACCEL
        if (accel > accelLimit && dist > 10.0) { // ACCEL_CHECK_MIN_DIST
            score += 40 // JUMP_WEIGHT_ACCEL_CHECK
        }
        
        // 3D Jump Validation (Altitude Delta)
        if (abs(altitudeDelta) / timeDeltaSec > 10.0) { // ALTITUDE_VELOCITY_CAP
            score += 30 // JUMP_WEIGHT_ALTITUDE_DELTA
        }

        if (speedMps > MAX_PHYSICAL_SPEED_MPS) score += 50 // JUMP_WEIGHT_TRADITIONAL_SPEED
        if (speedMps > 22.2 && accuracy > 40.0f) score += 30 // JUMP_GATE_SPEED_ACCURACY_LOW
        if (speedMps > 8.3 && accuracy > 150.0f) score += 20 // JUMP_GATE_SPEED_ACCURACY_HIGH
        
        val isTier2 = dist >= JUMP_POINT_DISTANCE_THRESHOLD && (speedMps > MAX_PHYSICAL_SPEED_MPS || score >= 40)
        val isTier3 = dist >= 10.0 && dist < JUMP_POINT_DISTANCE_THRESHOLD && score >= 30
        
        val isJump = isTier2 || isTier3 || score >= 50
        
        val reason = when {
            !hasPhysicalMotion && speedMps > 10.0 -> "Sensor Mismatch Jump"
            isTier2 -> "Security Jump"
            isTier3 -> "Visual Jitter"
            score >= 50 -> "High Confidence Jump"
            else -> ""
        }
        
        return JumpConfidence(
            score = score.coerceIn(0, 100),
            isJump = isJump,
            tier = if (isTier2) 2 else if (isTier3) 3 else 0,
            reason = reason
        )
    }
}
