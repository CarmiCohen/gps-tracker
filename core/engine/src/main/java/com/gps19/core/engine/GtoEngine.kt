package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * Sep.24.95:
 * - Issue #1163: Transitioned to a stateless model. All tracking buffers and 
 *   indices are now managed within LocationProcessingState.
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
        while (state.gtoSize > 0) {
            val tailIdx = (state.gtoHead - state.gtoSize + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            if ((rt - state.gtoRtBuffer[tailIdx]) > HINDSIGHT_MAX_AGE_MS) {
                state.gtoSize--
            } else {
                break
            }
        }

        // Add new point
        state.gtoLatBuffer[state.gtoHead] = lat
        state.gtoLngBuffer[state.gtoHead] = lng
        state.gtoAltBuffer[state.gtoHead] = alt
        state.gtoAccBuffer[state.gtoHead] = accuracy
        state.gtoMaxAccBuffer[state.gtoHead] = maxAccuracy
        state.gtoBearingBuffer[state.gtoHead] = bearing
        state.gtoSpeedBuffer[state.gtoHead] = speedMps
        state.gtoTsBuffer[state.gtoHead] = ts
        state.gtoRtBuffer[state.gtoHead] = rt
        state.gtoVibeBuffer[state.gtoHead] = vibrationIndex
        
        state.gtoHead = (state.gtoHead + 1) % MAX_WINDOW_SIZE
        if (state.gtoSize < MAX_WINDOW_SIZE) state.gtoSize++
    }

    fun evaluateTrajectory(state: LocationProcessingState, newLat: Double, newLng: Double, newBearing: Double, newSpeedMps: Double, timestamp: Long, rt: Long): Boolean {
        if (state.gtoSize == 0) return false

        val lastIdx = (state.gtoHead - 1 + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
        val lastRt = state.gtoRtBuffer[lastIdx]
        val lastLat = state.gtoLatBuffer[lastIdx]
        val lastLng = state.gtoLngBuffer[lastIdx]
        val lastBearing = state.gtoBearingBuffer[lastIdx]
        val lastSpeed = state.gtoSpeedBuffer[lastIdx]

        val angleDiff = abs(newBearing - lastBearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(lastLat, lastLng, newLat, newLng)
        
        val timeFromLast = (rt - lastRt) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (rt <= lastRt || (rt - lastRt) > HINDSIGHT_MAX_AGE_MS) return false
        
        // Zero-Churn average calculation
        var vibrationSum = 0.0
        for (i in 0 until state.gtoSize) {
            val idx = (state.gtoHead - state.gtoSize + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            vibrationSum += state.gtoVibeBuffer[idx]
        }
        val avgVibration = vibrationSum / state.gtoSize
        
        val GTO_TOW_SPEED_THRESHOLD = 15.0
        val PROMOTION_ANGLE_TOLERANCE = 30.0
        val GTO_KINEMATIC_SPEED_DELTA = 10.0
        
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > GTO_TOW_SPEED_THRESHOLD
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - lastSpeed) < GTO_KINEMATIC_SPEED_DELTA
        
        if (!isKinematicallyConsistent) return false

        if (state.gtoSize >= 2) {
            val startIdx = (state.gtoHead - state.gtoSize + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            val startLat = state.gtoLatBuffer[startIdx]
            val startLng = state.gtoLngBuffer[startIdx]
            
            val totalDisplacement = PhysicsUtils.calculateDistance(startLat, startLng, newLat, newLng)
            var totalPathLength = 0.0
            
            var prevIdx = startIdx
            for (i in 1 until state.gtoSize) {
                val currIdx = (startIdx + i) % MAX_WINDOW_SIZE
                totalPathLength += PhysicsUtils.calculateDistance(
                    state.gtoLatBuffer[prevIdx], state.gtoLngBuffer[prevIdx],
                    state.gtoLatBuffer[currIdx], state.gtoLngBuffer[currIdx]
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
        for (i in 0 until state.gtoSize) {
            val idx = (state.gtoHead - state.gtoSize + i + MAX_WINDOW_SIZE) % MAX_WINDOW_SIZE
            result.add(GtoNode(
                state.gtoLatBuffer[idx], state.gtoLngBuffer[idx], state.gtoAltBuffer[idx],
                state.gtoAccBuffer[idx], state.gtoMaxAccBuffer[idx], state.gtoBearingBuffer[idx],
                state.gtoSpeedBuffer[idx], state.gtoTsBuffer[idx], state.gtoRtBuffer[idx], state.gtoVibeBuffer[idx]
            ))
        }
        return result
    }

    fun clear(state: LocationProcessingState) {
        state.gtoHead = 0
        state.gtoSize = 0
    }
}
