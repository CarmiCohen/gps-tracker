package com.gps19.core.engine

import kotlin.math.*

/**
 * PhysicsUtils: Unified physics and geodesic calculations for the Pure Logic Engine.
 * v8.9.48:
 * - Issue #387: Logic Alignment - Jump Threshold. (Formerly #360-J)
 * v8.9.42:
 * - Issue #332: Adaptive Jump Confidence. Refined SNR-IMU correlation for Urban Canyons. 
 *   High SNR with zero vibration now triggers a higher score penalty. (Formerly #219)
 * - Issue #334: Hindsight Trajectory Correction. Implemented interpolateSegment 
 *   for hindsight rubber-banding. (Formerly #220)
 * - Issue #325: Authoritative Spatial Anchoring (Dual-Metric). Updated interpolateSegment 
 *   to propagate accuracy/maxAccuracy context through hindsight segments.
 * - Alignment: Replaced hardcoded 5.0m/s with JUMP_GATE_SENSOR_MISMATCH_MPS.
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
     * Checks if a location is the default coordinate.
     */
    fun isDefaultLocation(lat: Double, lng: Double): Boolean {
        return abs(lat - DEFAULT_LAT) < 0.0001 && abs(lng - DEFAULT_LNG) < 0.0001
    }

    fun smoothCoordinate(last: Double, current: Double, alpha: Double = 0.3): Double {
        if (last == 0.0) return current
        return last + alpha * (current - last)
    }

    fun smoothBearing(last: Float, current: Float, alpha: Float = 0.2f): Float {
        var delta = current - last
        while (delta < -180) delta += 360
        while (delta > 180) delta -= 360
        return (last + delta * alpha + 360) % 360
    }

    /**
     * Interpolates a segment between two points to prevent visual "teleporting".
     * Part of Issue #334 / #327: Rubber-band smoothing for hindsight promotion.
     * v8.9.42: Updated to propagate accuracy/maxAccuracy (Issue #325).
     */
    fun interpolateSegment(
        startLat: Double, startLng: Double, startTs: Long,
        endLat: Double, endLng: Double, endTs: Long,
        startAcc: Float = 0f, startMaxAcc: Float = 0f,
        endAcc: Float = 0f, endMaxAcc: Float = 0f,
        maxGapMeters: Double = 5.0
    ): List<EngineGeoPoint> {
        val dist = calculateDistance(startLat, startLng, endLat, endLng)
        if (dist <= maxGapMeters || startTs >= endTs) return emptyList()

        val steps = (dist / maxGapMeters).toInt().coerceIn(1, 10)
        val result = mutableListOf<EngineGeoPoint>()
        
        for (i in 1..steps) {
            val fraction = i.toDouble() / (steps + 1)
            val interpLat = startLat + (endLat - startLat) * fraction
            val interpLng = startLng + (endLng - startLng) * fraction
            val interpTs = startTs + ((endTs - startTs) * fraction).toLong()
            val interpAcc = startAcc + (endAcc - startAcc) * fraction.toFloat()
            val interpMaxAcc = startMaxAcc + (endMaxAcc - startMaxAcc) * fraction.toFloat()
            
            result.add(EngineGeoPoint(
                interpLat, interpLng, ts = interpTs, 
                accuracy = interpAcc, maxAccuracy = interpMaxAcc
            ))
        }
        return result
    }

    /**
     * Adaptive Multi-Factor Jump Engine logic.
     */
    fun isVisualJump(
        lastLat: Double, lastLng: Double, 
        newLat: Double, newLng: Double, 
        timeDeltaMs: Long, accuracy: Float,
        snr: Float = 0f,
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
        var isAdaptiveJump = false
        
        // Issue #332: Enhanced Sensor Fusion for Urban Canyons
        if (!hasPhysicalMotion && speedMps > JUMP_GATE_SENSOR_MISMATCH_MPS) { 
            score += JUMP_WEIGHT_SENSOR_MISMATCH
            
            if (snr >= ADAPTIVE_JUMP_SNR_THRESHOLD) {
                isAdaptiveJump = true
                score += 20
            }
        }
        
        // Velocity Inertia (Acceleration Check)
        val accel = abs(speedMps - lastSpeedMps) / timeDeltaSec
        val accelLimit = if (isParking) PARKING_ACCEL_LIMIT else MAX_TRACTOR_ACCEL
        if (accel > accelLimit && dist > ACCEL_CHECK_MIN_DIST) { 
            score += JUMP_WEIGHT_ACCEL_CHECK
        }
        
        // 3D Jump Validation (Altitude Delta)
        if (abs(altitudeDelta) / timeDeltaSec > ALTITUDE_VELOCITY_CAP) { 
            score += JUMP_WEIGHT_ALTITUDE_DELTA
        }

        if (speedMps > MAX_PHYSICAL_SPEED_MPS) score += JUMP_WEIGHT_TRADITIONAL_SPEED
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_LOW_MPS && accuracy > JUMP_GATE_ACCURACY_LOW_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_LOW
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_HIGH_MPS && accuracy > JUMP_GATE_ACCURACY_HIGH_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_HIGH
        
        val isTier2 = dist >= JUMP_POINT_DISTANCE_THRESHOLD && (speedMps > MAX_PHYSICAL_SPEED_MPS || score >= 40)
        val isTier3 = dist >= JUMP_GATE_VISUAL_JITTER_METERS && dist < JUMP_POINT_DISTANCE_THRESHOLD && score >= 30
        
        val isJump = isTier2 || isTier3 || score >= 50
        
        var reason = when {
            isAdaptiveJump -> "Signal Reflection Suspicion (High SNR)"
            !hasPhysicalMotion && speedMps > JUMP_GATE_SENSOR_MISMATCH_MPS -> "Sensor Mismatch Jump (Urban Canyon)"
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
