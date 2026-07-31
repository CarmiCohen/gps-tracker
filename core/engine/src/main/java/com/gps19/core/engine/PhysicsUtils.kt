package com.gps19.core.engine

import kotlin.math.*

/**
 * PhysicsUtils: High-performance geospatial and kinematic calculations.
 * July.30.52:
 * - Issue #653: Performance: GC Churn Optimization. Refactored isVisualJump 
 *   to accept a mutable flyweight, eliminating per-call allocations (R-HARDWARE-01).
 * July.30.48:
 * - Issue #653: Performance: GC Churn Optimization.
 */
object PhysicsUtils {

    /**
     * Safely handles NaN or Infinite values, defaulting to 0.0.
     */
    fun safeDouble(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

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
        val dist = EARTH_RADIUS_METERS * 2 * atan2(sqrt(clampedA), sqrt(1.0 - clampedA))

        return if (dist.isNaN()) 0.0 else dist
    }
    
    /**
     * Calculates the initial bearing (azimuth) between two points in degrees.
     */
    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1 == lat2 && lng1 == lng2) return 0.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lng2 - lng1)
        
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * Simplifies a list of points by removing points that are closer than [minDistance] 
     * from the previous kept point. Always preserves the first and last points.
     */
    fun <T> simplifyTrail(
        points: List<T>,
        minDistance: Double,
        getLat: (T) -> Double,
        getLng: (T) -> Double
    ): List<T> {
        if (points.size <= 2) return points
        
        val result = mutableListOf<T>()
        result.add(points.first())
        
        var lastLat = getLat(points.first())
        var lastLng = getLng(points.first())
        
        for (i in 1 until points.size - 1) {
            val curr = points[i]
            val currLat = getLat(curr)
            val currLng = getLng(curr)
            
            val dist = calculateDistance(lastLat, lastLng, currLat, currLng)
            if (dist >= minDistance) {
                result.add(curr)
                lastLat = currLat
                lastLng = currLng
            }
        }
        
        result.add(points.last())
        return result
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

    /**
     * interpolateSegmentCallback: Part of Issue #653. Callback-based interpolation 
     * to achieve zero-churn forensics in the high-frequency trajectory path.
     */
    fun interpolateSegmentCallback(
        startLat: Double, startLng: Double, startTs: Long,
        endLat: Double, endLng: Double, endTs: Long,
        startAcc: Double, startMaxAcc: Double,
        endAcc: Double, endMaxAcc: Double,
        onPoint: (Double, Double, Long, Double, Double) -> Unit
    ) {
        val durationMs = endTs - startTs
        if (durationMs <= 1000L) return

        val steps = (durationMs / 1000L).toInt()

        for (i in 1..steps) {
            val fraction = i.toDouble() / (steps + 1)
            val lat = startLat + (endLat - startLat) * fraction
            val lng = startLng + (endLng - startLng) * fraction
            val ts = startTs + (durationMs * fraction).toLong()
            val acc = startAcc + (endAcc - startAcc) * fraction
            val maxAcc = startMaxAcc + (endMaxAcc - startMaxAcc) * fraction
            
            onPoint(lat, lng, ts, acc, maxAcc)
        }
    }

    /**
     * Multi-Factor Jump Engine logic. Zero-Churn optimized.
     */
    fun isVisualJump(
        lastLat: Double, lastLng: Double, 
        newLat: Double, newLng: Double, 
        timeDeltaMs: Long, accuracy: Double,
        lastAccuracy: Double = 0.0,
        snr: Double = 0.0,
        lastSpeedMps: Double = 0.0,
        isParking: Boolean = false,
        altitudeDelta: Double = 0.0,
        hasPhysicalMotion: Boolean = true,
        result: JumpConfidence
    ) {
        result.reset()
        if (lastLat == 0.0) return
        
        val dist = calculateDistance(lastLat, lastLng, newLat, newLng)
        val timeDeltaSec = max(0.1, timeDeltaMs / 1000.0)
        val speedMps = dist / timeDeltaSec
        
        // Tier 1: Outlier Filter
        if (dist > OUTLIER_DISTANCE_THRESHOLD || speedMps > OUTLIER_SPEED_CAP_MPS) {
            result.score = 100
            result.isJump = true
            result.isOutlier = true
            result.tier = 1
            result.reason = "Hardware/Cold-Start Outlier"
            return
        }

        if (timeDeltaMs < 100) return
        
        var score = 0
        
        // Sensor Fusion check
        val mismatchGate = JUMP_GATE_SENSOR_MISMATCH_MPS
        if (!hasPhysicalMotion && speedMps > mismatchGate) { 
            score += JUMP_WEIGHT_SENSOR_MISMATCH
        }
        
        // Velocity Inertia
        val accel = abs(speedMps - lastSpeedMps) / timeDeltaSec
        val accelLimit = if (isParking) PARKING_ACCEL_LIMIT else MAX_TRACTOR_ACCEL
        if (accel > accelLimit && dist > ACCEL_CHECK_MIN_DIST) { 
            score += JUMP_WEIGHT_ACCEL_CHECK
        }
        
        // 3D Validation
        if (abs(altitudeDelta) / timeDeltaSec > ALTITUDE_VELOCITY_CAP) {
            score += JUMP_WEIGHT_ALTITUDE_DELTA
        }

        if (speedMps > MAX_PHYSICAL_SPEED_MPS) score += JUMP_WEIGHT_TRADITIONAL_SPEED
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_LOW_MPS && accuracy > JUMP_GATE_ACCURACY_LOW_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_LOW
        if (speedMps > JUMP_GATE_SPEED_ACCURACY_HIGH_MPS && accuracy > JUMP_GATE_ACCURACY_HIGH_THRESHOLD) score += JUMP_WEIGHT_ACCURACY_HIGH
        
        // Issue #529: Accuracy Recovery Mitigation
        val isAccuracyImproving = lastAccuracy > 0.0 && accuracy < (lastAccuracy * 0.8)
        val isWithinPreviousError = lastAccuracy > 0.0 && dist < lastAccuracy
        
        val isSnap = isAccuracyImproving && isWithinPreviousError
        if (isSnap && score > 0) {
            score = (score * 0.4).toInt()
        }

        val isTier2 = dist >= JUMP_POINT_DISTANCE_THRESHOLD && (speedMps > MAX_PHYSICAL_SPEED_MPS || score >= 40)
        val jitterThreshold = JUMP_GATE_VISUAL_JITTER_METERS
        val isTier3 = dist >= jitterThreshold && dist < JUMP_POINT_DISTANCE_THRESHOLD && score >= 30
        
        var isJump = (isTier2 || isTier3 || score >= 50)
        if (isSnap) isJump = false
        
        result.score = score.coerceIn(0, 100)
        result.isJump = isJump
        result.tier = if (isTier2) 2 else if (isTier3) 3 else 0
        result.reason = when {
            isSnap -> "Suppressed Accuracy Snap"
            !hasPhysicalMotion && speedMps > mismatchGate -> "Sensor Mismatch Jump (Urban Canyon)"
            isTier2 -> "Security Jump"
            isTier3 -> "Visual Jitter"
            score >= 50 -> "High Confidence Jump"
            else -> ""
        }
    }
}
