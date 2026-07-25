package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * July.25.07:
 * - Issue #547b: Kernel I/O Optimization. Refactored internal window to use 
 *   primitive circular buffers, achieving Zero-Churn for high-frequency 
 *   kinematic evaluation.
 * - Removed GtoNode object allocations in hot path.
 */
class GtoEngine {

    private val MAX_WINDOW_SIZE = 5
    private val HINDSIGHT_MAX_AGE_MS = 60000L

    // Zero-Churn primitive circular buffers
    private val latBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val lngBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val altBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val accBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val maxAccBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val bearingBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val speedBuffer = DoubleArray(MAX_WINDOW_SIZE)
    private val tsBuffer = LongArray(MAX_WINDOW_SIZE)
    private val rtBuffer = LongArray(MAX_WINDOW_SIZE)
    private val vibeBuffer = DoubleArray(MAX_WINDOW_SIZE)
    
    private var head = 0
    private var size = 0

    data class GtoNode(
        val lat: Double,
        val lng: Double,
        val alt: Double,
        val accuracy: Double,
        val maxAccuracy: Double, 
        val bearing: Double,
        val speedMps: Double,
        val ts: Long,
        val rt: Long,
        val vibrationIndex: Double
    )

    fun addPoint(
        lat: Double, lng: Double, alt: Double, accuracy: Double, maxAccuracy: Double,
        bearing: Double, speedMps: Double, ts: Long, rt: Long, vibrationIndex: Double
    ) {
        // Prune aged points
        while (size > 0) {
            val tailIdx = (head - size + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            if ((rt - rtBuffer[tailIdx]) > HINDSIGHT_MAX_AGE_MS) {
                size--
            } else {
                break
            }
        }

        // Add new point
        latBuffer[head] = lat
        lngBuffer[head] = lng
        altBuffer[head] = alt
        accBuffer[head] = accuracy
        maxAccBuffer[head] = maxAccuracy
        bearingBuffer[head] = bearing
        speedBuffer[head] = speedMps
        tsBuffer[head] = ts
        rtBuffer[head] = rt
        vibeBuffer[head] = vibrationIndex
        
        head = (head + 1) % MAX_WINDOW_SIZE
        if (size < MAX_WINDOW_SIZE) size++
    }

    fun evaluateTrajectory(newLat: Double, newLng: Double, newBearing: Double, newSpeedMps: Double, timestamp: Long, rt: Long): Boolean {
        if (size == 0) return false

        val lastIdx = (head - 1 + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
        val lastRt = rtBuffer[lastIdx]
        val lastLat = latBuffer[lastIdx]
        val lastLng = lngBuffer[lastIdx]
        val lastBearing = bearingBuffer[lastIdx]
        val lastSpeed = speedBuffer[lastIdx]

        val angleDiff = abs(newBearing - lastBearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(lastLat, lastLng, newLat, newLng)
        
        val timeFromLast = (rt - lastRt) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (rt <= lastRt || (rt - lastRt) > HINDSIGHT_MAX_AGE_MS) return false
        
        // Zero-Churn average calculation
        var vibrationSum = 0.0
        for (i in 0 until size) {
            val idx = (head - size + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            vibrationSum += vibeBuffer[idx]
        }
        val avgVibration = vibrationSum / size
        
        val GTO_TOW_SPEED_THRESHOLD = 15.0
        val PROMOTION_ANGLE_TOLERANCE = 30.0
        val GTO_KINEMATIC_SPEED_DELTA = 10.0
        
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > GTO_TOW_SPEED_THRESHOLD
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - lastSpeed) < GTO_KINEMATIC_SPEED_DELTA
        
        if (!isKinematicallyConsistent) return false

        if (size >= 2) {
            val startIdx = (head - size + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            val startLat = latBuffer[startIdx]
            val startLng = lngBuffer[startIdx]
            
            val totalDisplacement = PhysicsUtils.calculateDistance(startLat, startLng, newLat, newLng)
            var totalPathLength = 0.0
            
            var prevIdx = startIdx
            for (i in 1 until size) {
                val currIdx = (startIdx + i) % MAX_WINDOW_SIZE
                totalPathLength += PhysicsUtils.calculateDistance(
                    latBuffer[prevIdx], lngBuffer[prevIdx],
                    latBuffer[currIdx], lngBuffer[currIdx]
                )
                prevIdx = currIdx
            }
            totalPathLength += distFromLast
            
            if (totalPathLength > EFFICIENCY_MIN_TOTAL_DIST) {
                val efficiency = totalDisplacement / max(1.0, totalPathLength)
                if (efficiency < PATH_EFFICIENCY_THRESHOLD) {
                    return false 
                }
            }
        }

        val GTO_WORK_SPEED_THRESHOLD = 1.0
        val isWorkSignature = avgVibration > VIBRATION_STATIONARY_THRESHOLD && newSpeedMps < GTO_WORK_SPEED_THRESHOLD
        if (isWorkSignature && distFromLast < JUMP_CHECK_MIN_DIST) {
            return false
        }
        
        return true
    }

    /**
     * getWindow: Only called for telemetry/UI updates, so allocation here 
     * is acceptable as it is not part of the 1Hz/10Hz tick hot-path.
     */
    fun getWindow(): List<GtoNode> {
        val result = mutableListOf<GtoNode>()
        for (i in 0 until size) {
            val idx = (head - size + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            result.add(GtoNode(
                latBuffer[idx], lngBuffer[idx], altBuffer[idx],
                accBuffer[idx], maxAccBuffer[idx], bearingBuffer[idx],
                speedBuffer[idx], tsBuffer[idx], rtBuffer[idx], vibeBuffer[idx]
            ))
        }
        return result
    }

    fun clear() {
        head = 0
        size = 0
    }
}
