package com.gps19.core.engine

import kotlin.math.*

/**
 * PhysicsUtils: High-performance geospatial and kinematic calculations.
 * v9.3.20:
 * - R405: Samsung A15 Hardening. Unified jump gates and removed isA15 branching.
 */
object PhysicsUtils {

    /**
     * Calculates the distance between two points in meters using the Haversine formula.
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1.isNaN() || lng1.isNaN() || lat2.isNaN() || lng2.isNaN()) return 0.0
        if (lat1 == lat2 && lng1 == lng2) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val clampedA = a.coerceIn(0.0, 1.0)
        val dist = 6371000.0 * 2 * atan2(sqrt(clampedA), sqrt(1.0 - clampedA))

        return if (dist.isNaN()) 0.0 else dist
    }

    fun isValidLocation(lat: Double, lng: Double): Boolean {
        return lat != 0.0 && lng != 0.0 && lat in -90.0..90.0 && lng in -180.0..180.0
    }

    fun smoothCoordinate(last: Double, current: Double, alpha: Double = 0.3): Double {
        if (last == 0.0) return current
        return last + alpha * (current - last)
    }

    fun smoothBearing(last: Double, current: Double, alpha: Double = 0.2): Double {
        var delta = current - last
        while (delta < -180) delta += 360
        while (delta > 180) delta -= 360
        return (last + delta * alpha + 360) % 360
    }

    fun interpolateSegment(
        startLat: Double, startLng: Double, startTs: Long,
        endLat: Double, endLng: Double, endTs: Long,
        startAcc: Double, startMaxAcc: Double,
        endAcc: Double, endMaxAcc: Double
    ): List<EngineGeoPoint> {
        val durationMs = endTs - startTs
        if (durationMs <= 1000L) return emptyList()

        val steps = (durationMs / 1000L).toInt()
        val points = mutableListOf<EngineGeoPoint>()

        for (i in 1..steps) {
            val fraction = i.toDouble() / (steps + 1)
            val lat = startLat + (endLat - startLat) * fraction
            val lng = startLng + (endLng - startLng) * fraction
            val ts = startTs + (durationMs * fraction).toLong()
            val acc = startAcc + (endAcc - startAcc) * fraction
            val maxAcc = startMaxAcc + (endMaxAcc - startMaxAcc) * fraction
            
            points.add(EngineGeoPoint(lat, lng, ts = ts, accuracy = acc, maxAccuracy = maxAcc))
        }
        return points
    }

    /**
     * Multi-Factor Jump Engine logic. Standardized to Double.
     * R405: Removed device-specific (A15) threshold branching.
     */
    fun isVisualJump(
        lastLat: Double, lastLng: Double, 
        newLat: Double, newLng: Double, 
        timeDeltaMs: Long, accuracy: Double,
        snr: Double = 0.0,
        lastSpeedMps: Double = 0.0,
        isParking: Boolean = false,
        altitudeDelta: Double = 0.0,
        hasPhysicalMotion: Boolean = true
    ): JumpConfidence {
        if (lastLat == 0.0 || timeDeltaMs < 100) return JumpConfidence()
        
        val dist = calculateDistance(lastLat, lastLng, newLat, newLng)
        val timeDeltaSec = timeDeltaMs / 1000.0
        val speedMps = dist / max(0.1, timeDeltaSec)
        
        // Tier 1: Outlier Filter
        if (dist > OUTLIER_DISTANCE_THRESHOLD || speedMps > OUTLIER_SPEED_CAP_MPS) {
            return JumpConfidence(score = 100, isJump = true, isOutlier = true, tier = 1, reason = "Hardware/Cold-Start Outlier")
        }
        
        var score = 0
        var isAdaptiveJump = false
        
        // Sensor Fusion check
        val mismatchGate = JUMP_GATE_SENSOR_MISMATCH_MPS
        if (!hasPhysicalMotion && speedMps > mismatchGate) { 
            score += JUMP_WEIGHT_SENSOR_MISMATCH
            
            if (snr >= ADAPTIVE_JUMP_SNR_THRESHOLD) {
                isAdaptiveJump = true
                score += 20
            }
        }
        
        // Velocity Inertia
        val accel = abs(speedMps - lastSpeedMps) / max(0.1, timeDeltaSec)
        val accelLimit = if (isParking) PARKING_ACCEL_LIMIT else MAX_TRACTOR_ACCEL
        if (accel > accelLimit && dist > ACCEL_CHECK_MIN_DIST) { 
            score += JUMP_WEIGHT_ACCEL_CHECK
        }
        
        // 3D Validation
        if (abs(altitudeDelta) / max(0.1, timeDeltaSec) > ALTITUDE_VELOCITY_CAP) { 
            score += JUMP_WEIGHT_ALTITUDE_DELTA
        }

        if (speedMps > MAX_PHYSICAL_SPEED_MPS) score += JUMP_WEIGHT_TRADITIONAL_SPEED
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_LOW_MPS && accuracy > JUMP_GATE_ACCURACY_LOW_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_LOW
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_HIGH_MPS && accuracy > JUMP_GATE_ACCURACY_HIGH_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_HIGH
        
        val isTier2 = dist >= JUMP_POINT_DISTANCE_THRESHOLD && (speedMps > MAX_PHYSICAL_SPEED_MPS || score >= 40)
        
        val jitterThreshold = JUMP_GATE_VISUAL_JITTER_METERS
        val isTier3 = dist >= jitterThreshold && dist < JUMP_POINT_DISTANCE_THRESHOLD && score >= 30
        
        val isJump = isTier2 || isTier3 || score >= 50
        
        val reason = when {
            isAdaptiveJump -> "Signal Reflection Suspicion (High SNR)"
            !hasPhysicalMotion && speedMps > mismatchGate -> "Sensor Mismatch Jump (Urban Canyon)"
            isTier2 -> "Security Jump"
            isTier3 -> "Visual Jitter"
            score >= 50 -> "High Confidence Jump"
            else -> ""
        }
        
        return JumpConfidence(
            score = score.coerceIn(0, 100),
            isJump = isJump,
            isAdaptiveJump = isAdaptiveJump,
            tier = if (isTier2) 2 else if (isTier3) 3 else 0,
            reason = reason
        )
    }
}
