package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * Sep.27.5:
 * - Issue #1349: Mutability Reduction. Adapted to access fields via state.gto sub-state.
 */
object GtoEngine {

    private const val MAX_WINDOW_SIZE = 5
    private const val HINDSIGHT_MAX_AGE_MS = 60000L

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
        state: LocationProcessingState,
        lat: Double, lng: Double, alt: Double, accuracy: Double, maxAccuracy: Double,
        bearing: Double, speedMps: Double, ts: Long, rt: Long, vibrationIndex: Double
    ) {
        // Prune aged points
        while (state.gto.size > 0) {
            val tailIdx = (state.gto.head - state.gto.size + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            if ((rt - state.gto.rtBuffer[tailIdx]) > HINDSIGHT_MAX_AGE_MS) {
                state.gto.size--
            } else {
                break
            }
        }

        // Add new point
        state.gto.latBuffer[state.gto.head] = lat
        state.gto.lngBuffer[state.gto.head] = lng
        state.gto.altBuffer[state.gto.head] = alt
        state.gto.accBuffer[state.gto.head] = accuracy
        state.gto.maxAccBuffer[state.gto.head] = maxAccuracy
        state.gto.bearingBuffer[state.gto.head] = bearing
        state.gto.speedBuffer[state.gto.head] = speedMps
        state.gto.tsBuffer[state.gto.head] = ts
        state.gto.rtBuffer[state.gto.head] = rt
        state.gto.vibeBuffer[state.gto.head] = vibrationIndex
        
        state.gto.head = (state.gto.head + 1) % MAX_WINDOW_SIZE
        if (state.gto.size < MAX_WINDOW_SIZE) state.gto.size++
    }

    fun evaluateTrajectory(state: LocationProcessingState, newLat: Double, newLng: Double, newBearing: Double, newSpeedMps: Double, timestamp: Long, rt: Long): Boolean {
        if (state.gto.size == 0) return false

        val lastIdx = (state.gto.head - 1 + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
        val lastRt = state.gto.rtBuffer[lastIdx]
        val lastLat = state.gto.latBuffer[lastIdx]
        val lastLng = state.gto.lngBuffer[lastIdx]
        val lastBearing = state.gto.bearingBuffer[lastIdx]
        val lastSpeed = state.gto.speedBuffer[lastIdx]

        val angleDiff = abs(newBearing - lastBearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(lastLat, lastLng, newLat, newLng)
        
        val timeFromLast = (rt - lastRt) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (rt <= lastRt || (rt - lastRt) > HINDSIGHT_MAX_AGE_MS) return false
        
        // Zero-Churn average calculation
        var vibrationSum = 0.0
        for (i in 0 until state.gto.size) {
            val idx = (state.gto.head - state.gto.size + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            vibrationSum += state.gto.vibeBuffer[idx]
        }
        val avgVibration = vibrationSum / state.gto.size
        
        val GTO_TOW_SPEED_THRESHOLD = 15.0
        val PROMOTION_ANGLE_TOLERANCE = 30.0
        val GTO_KINEMATIC_SPEED_DELTA = 10.0
        
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > GTO_TOW_SPEED_THRESHOLD
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - lastSpeed) < GTO_KINEMATIC_SPEED_DELTA
        
        if (!isKinematicallyConsistent) return false

        if (state.gto.size >= 2) {
            val startIdx = (state.gto.head - state.gto.size + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            val startLat = state.gto.latBuffer[startIdx]
            val startLng = state.gto.lngBuffer[startIdx]
            
            val totalDisplacement = PhysicsUtils.calculateDistance(startLat, startLng, newLat, newLng)
            var totalPathLength = 0.0
            
            var prevIdx = startIdx
            for (i in 1 until state.gto.size) {
                val currIdx = (startIdx + i) % MAX_WINDOW_SIZE
                totalPathLength += PhysicsUtils.calculateDistance(
                    state.gto.latBuffer[prevIdx], state.gto.lngBuffer[prevIdx],
                    state.gto.latBuffer[currIdx], state.gto.lngBuffer[currIdx]
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

    fun getWindow(state: LocationProcessingState): List<GtoNode> {
        val result = mutableListOf<GtoNode>()
        for (i in 0 until state.gto.size) {
            val idx = (state.gto.head - state.gto.size + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            result.add(GtoNode(
                state.gto.latBuffer[idx], state.gto.lngBuffer[idx], state.gto.altBuffer[idx],
                state.gto.accBuffer[idx], state.gto.maxAccBuffer[idx], state.gto.bearingBuffer[idx],
                state.gto.speedBuffer[idx], state.gto.tsBuffer[idx], state.gto.rtBuffer[idx], state.gto.vibeBuffer[idx]
            ))
        }
        return result
    }

    fun clear(state: LocationProcessingState) {
        state.gto.head = 0
        state.gto.size = 0
    }
}