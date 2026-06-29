package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * v8.9.52:
 * - Issue #461: Forensic Parity. Updated GtoNode to preserve maxAccuracy context 
 *   during trajectory promotion. (Formerly #435)
 * v8.9.34:
 * - Issue #264: Consolidated magic numbers into EngineConstants.kt.
 */
class GtoEngine {

    private val window = mutableListOf<GtoNode>()
    private val maxWindowSize = HINDSIGHT_BUFFER_SIZE

    data class GtoNode(
        val lat: Double,
        val lng: Double,
        val alt: Double,
        val accuracy: Float,
        val maxAccuracy: Float, // Added for forensic parity
        val bearing: Float,
        val speedMps: Double,
        val ts: Long,
        val vibrationIndex: Float
    )

    fun addPoint(
        lat: Double, lng: Double, alt: Double, accuracy: Float, maxAccuracy: Float,
        bearing: Float, speedMps: Double, ts: Long, vibrationIndex: Float
    ) {
        window.removeAll { (ts - it.ts) > HINDSIGHT_MAX_AGE_MS }
        if (window.size >= maxWindowSize) {
            window.removeAt(0)
        }
        window.add(GtoNode(lat, lng, alt, accuracy, maxAccuracy, bearing, speedMps, ts, vibrationIndex))
    }

    fun evaluateTrajectory(newLat: Double, newLng: Double, newBearing: Float, newSpeedMps: Double, timestamp: Long): Boolean {
        if (window.isEmpty()) return false

        val last = window.last()
        
        val angleDiff = abs(newBearing - last.bearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(last.lat, last.lng, newLat, newLng)
        val timeFromLast = (timestamp - last.ts) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (timestamp <= last.ts || (timestamp - last.ts) > HINDSIGHT_MAX_AGE_MS) return false
        
        val avgVibration = window.map { it.vibrationIndex }.average().toFloat()
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > GTO_TOW_SPEED_THRESHOLD
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - last.speedMps) < GTO_KINEMATIC_SPEED_DELTA
        
        if (!isKinematicallyConsistent) return false

        if (window.size >= 2) {
            val start = window.first()
            val totalDisplacement = PhysicsUtils.calculateDistance(start.lat, start.lng, newLat, newLng)
            var totalPathLength = 0.0
            var prevNode = start
            for (i in 1 until window.size) {
                val node = window[i]
                totalPathLength += PhysicsUtils.calculateDistance(prevNode.lat, prevNode.lng, node.lat, node.lng)
                prevNode = node
            }
            totalPathLength += distFromLast
            
            val efficiency = totalDisplacement / max(1.0, totalPathLength)
            if (efficiency < PATH_EFFICIENCY_THRESHOLD && totalPathLength > EFFICIENCY_MIN_TOTAL_DIST) {
                return false 
            }
        }

        val isWorkSignature = avgVibration > VIBRATION_STATIONARY_THRESHOLD && newSpeedMps < GTO_WORK_SPEED_THRESHOLD
        if (isWorkSignature && distFromLast < JUMP_CHECK_MIN_DIST) {
            return false
        }
        
        return true
    }

    fun getWindow(): List<GtoNode> = window.toList()

    fun clear() {
        window.clear()
    }
}
