package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * v9.3.15:
 * - Hardening: Finalized Double standardization. Eliminated redundant 
 *   conversions in trajectory evaluation.
 */
class GtoEngine {

    private val window = mutableListOf<GtoNode>()
    private val maxWindowSize = HINDSIGHT_BUFFER_SIZE

    data class GtoNode(
        val lat: Double,
        val lng: Double,
        val alt: Double,
        val accuracy: Double,
        val maxAccuracy: Double, 
        val bearing: Double,
        val speedMps: Double,
        val ts: Long,
        val vibrationIndex: Double
    )

    fun addPoint(
        lat: Double, lng: Double, alt: Double, accuracy: Double, maxAccuracy: Double,
        bearing: Double, speedMps: Double, ts: Long, vibrationIndex: Double
    ) {
        window.removeAll { (ts - it.ts) > HINDSIGHT_MAX_AGE_MS }
        if (window.size >= maxWindowSize) {
            window.removeAt(0)
        }
        window.add(GtoNode(lat, lng, alt, accuracy, maxAccuracy, bearing, speedMps, ts, vibrationIndex))
    }

    fun evaluateTrajectory(newLat: Double, newLng: Double, newBearing: Double, newSpeedMps: Double, timestamp: Long): Boolean {
        if (window.isEmpty()) return false

        val last = window.last()
        
        val angleDiff = abs(newBearing - last.bearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(last.lat, last.lng, newLat, newLng)
        val timeFromLast = (timestamp - last.ts) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (timestamp <= last.ts || (timestamp - last.ts) > HINDSIGHT_MAX_AGE_MS) return false
        
        val avgVibration = window.map { it.vibrationIndex }.average()
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
