package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * Sep.27.6:
 * - Issue #1161: Unified Trajectory & Buffer Management. Refactored to use 
 *   TrajectoryBuffer and TrajectoryNode from EngineModels.kt.
 */
object GtoEngine {

    fun addPoint(
        state: LocationProcessingState,
        lat: Double, lng: Double, alt: Double, accuracy: Double, maxAccuracy: Double,
        bearing: Double, speedMps: Double, ts: Long, rt: Long, vibrationIndex: Double
    ) {
        val buffer = state.trajectory
        
        // Prune aged points
        while (buffer.size > 0) {
            val tailIdx = (buffer.head - buffer.size + TRAJECTORY_BUFFER_MAX_SIZE) % TRAJECTORY_BUFFER_MAX_SIZE
            if ((rt - buffer.rtBuffer[tailIdx]) > TRAJECTORY_HINDSIGHT_MAX_AGE_MS) {
                buffer.size--
            } else {
                break
            }
        }

        // Add new point
        buffer.latBuffer[buffer.head] = lat
        buffer.lngBuffer[buffer.head] = lng
        buffer.altBuffer[buffer.head] = alt
        buffer.accBuffer[buffer.head] = accuracy
        buffer.maxAccBuffer[buffer.head] = maxAccuracy
        buffer.bearingBuffer[buffer.head] = bearing
        buffer.speedBuffer[buffer.head] = speedMps
        buffer.tsBuffer[buffer.head] = ts
        buffer.rtBuffer[buffer.head] = rt
        buffer.vibeBuffer[buffer.head] = vibrationIndex
        
        buffer.head = (buffer.head + 1) % TRAJECTORY_BUFFER_MAX_SIZE
        if (buffer.size < TRAJECTORY_BUFFER_MAX_SIZE) buffer.size++
    }

    fun evaluateTrajectory(state: LocationProcessingState, newLat: Double, newLng: Double, newBearing: Double, newSpeedMps: Double, timestamp: Long, rt: Long): Boolean {
        val buffer = state.trajectory
        if (buffer.size == 0) return false

        val lastIdx = (buffer.head - 1 + TRAJECTORY_BUFFER_MAX_SIZE) % TRAJECTORY_BUFFER_MAX_SIZE
        val lastRt = buffer.rtBuffer[lastIdx]
        val lastLat = buffer.latBuffer[lastIdx]
        val lastLng = buffer.lngBuffer[lastIdx]
        val lastBearing = buffer.bearingBuffer[lastIdx]
        val lastSpeed = buffer.speedBuffer[lastIdx]

        val angleDiff = abs(newBearing - lastBearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(lastLat, lastLng, newLat, newLng)
        
        val timeFromLast = (rt - lastRt) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (rt <= lastRt || (rt - lastRt) > TRAJECTORY_HINDSIGHT_MAX_AGE_MS) return false
        
        // Zero-Churn average calculation
        var vibrationSum = 0.0
        for (i in 0 until buffer.size) {
            val idx = (buffer.head - buffer.size + i + TRAJECTORY_BUFFER_MAX_SIZE) % TRAJECTORY_BUFFER_MAX_SIZE
            vibrationSum += buffer.vibeBuffer[idx]
        }
        val avgVibration = vibrationSum / buffer.size
        
        val GTO_TOW_SPEED_THRESHOLD = 15.0
        val PROMOTION_ANGLE_TOLERANCE = 30.0
        val GTO_KINEMATIC_SPEED_DELTA = 10.0
        
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > GTO_TOW_SPEED_THRESHOLD
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - lastSpeed) < GTO_KINEMATIC_SPEED_DELTA
        
        if (!isKinematicallyConsistent) return false

        if (buffer.size >= 2) {
            val startIdx = (buffer.head - buffer.size + TRAJECTORY_BUFFER_MAX_SIZE) % TRAJECTORY_BUFFER_MAX_SIZE
            val startLat = buffer.latBuffer[startIdx]
            val startLng = buffer.lngBuffer[startIdx]
            
            val totalDisplacement = PhysicsUtils.calculateDistance(startLat, startLng, newLat, newLng)
            var totalPathLength = 0.0
            
            var prevIdx = startIdx
            for (i in 1 until buffer.size) {
                val currIdx = (startIdx + i) % TRAJECTORY_BUFFER_MAX_SIZE
                totalPathLength += PhysicsUtils.calculateDistance(
                    buffer.latBuffer[prevIdx], buffer.lngBuffer[prevIdx],
                    buffer.latBuffer[currIdx], buffer.lngBuffer[currIdx]
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

    fun getWindow(state: LocationProcessingState): List<TrajectoryNode> {
        val buffer = state.trajectory
        val result = mutableListOf<TrajectoryNode>()
        for (i in 0 until buffer.size) {
            val idx = (buffer.head - buffer.size + i + TRAJECTORY_BUFFER_MAX_SIZE) % TRAJECTORY_BUFFER_MAX_SIZE
            result.add(TrajectoryNode(
                buffer.latBuffer[idx], buffer.lngBuffer[idx], buffer.altBuffer[idx],
                buffer.accBuffer[idx], buffer.maxAccBuffer[idx], buffer.bearingBuffer[idx],
                buffer.speedBuffer[idx], buffer.tsBuffer[idx], buffer.rtBuffer[idx], buffer.vibeBuffer[idx]
            ))
        }
        return result
    }

    fun clear(state: LocationProcessingState) {
        state.trajectory.head = 0
        state.trajectory.size = 0
    }
}
