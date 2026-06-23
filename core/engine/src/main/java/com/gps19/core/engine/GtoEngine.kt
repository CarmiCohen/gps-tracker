package com.gps19.core.engine

import kotlin.math.*

/**
 * GtoEngine: Graph Trajectory Optimization.
 * v8.9.28:
 * - Issue #15: Implementation of the sliding-window factor graph logic as per GTO_ENGINE_SPEC.md.
 * 
 * The GtoEngine evaluates the consistency of a trajectory by analyzing a sequence of points
 * against physical constraints (Acceleration, Vibration, Path Efficiency).
 */
class GtoEngine {

    private val window = mutableListOf<GtoNode>()
    private val maxWindowSize = HINDSIGHT_BUFFER_SIZE

    data class GtoNode(
        val lat: Double,
        val lng: Double,
        val alt: Double,
        val accuracy: Float,
        val bearing: Float,
        val speedMps: Double,
        val ts: Long,
        val vibrationIndex: Float
    )

    /**
     * addPoint: Adds a new node to the sliding window and performs local optimization checks.
     */
    fun addPoint(
        lat: Double, lng: Double, alt: Double, accuracy: Float, bearing: Float,
        speedMps: Double, ts: Long, vibrationIndex: Float
    ) {
        // Maintain sliding window
        window.removeAll { (ts - it.ts) > HINDSIGHT_MAX_AGE_MS }
        if (window.size >= maxWindowSize) {
            window.removeAt(0)
        }
        window.add(GtoNode(lat, lng, alt, accuracy, bearing, speedMps, ts, vibrationIndex))
    }

    /**
     * evaluateTrajectory: Analyzes the current window for "Least-Energy" path consistency.
     * Returns true if the window represents a high-confidence promoted trajectory.
     */
    fun evaluateTrajectory(newLat: Double, newLng: Double, newBearing: Float, newSpeedMps: Double, timestamp: Long): Boolean {
        if (window.isEmpty()) return false

        val last = window.last()
        
        // 1. Basic Temporal/Angular Consistency (Hindsight Logic)
        val angleDiff = abs(newBearing - last.bearing).let { if (it > 180) 360 - it else it }
        val distFromLast = PhysicsUtils.calculateDistance(last.lat, last.lng, newLat, newLng)
        val timeFromLast = (timestamp - last.ts) / 1000.0
        val impliedSpeed = distFromLast / max(0.1, timeFromLast)
        
        if (timestamp <= last.ts || (timestamp - last.ts) > HINDSIGHT_MAX_AGE_MS) return false
        
        // Tighten angular tolerance if moving fast but no vibration (Potential Towing - Issue #15 alignment)
        val avgVibration = window.map { it.vibrationIndex }.average().toFloat()
        val isTowSignature = avgVibration < VIBRATION_STATIONARY_THRESHOLD && newSpeedMps > 10.0
        val angularTolerance = if (isTowSignature) PROMOTION_ANGLE_TOLERANCE / 2.0 else PROMOTION_ANGLE_TOLERANCE

        val isKinematicallyConsistent = angleDiff < angularTolerance && abs(impliedSpeed - last.speedMps) < 10.0
        
        if (!isKinematicallyConsistent) return false

        // 2. Path Efficiency Optimization (Graph Constraint)
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
                return false // Jittery/Low-energy path
            }
        }

        // 3. Vibration Signature (Mechanical Constraint)
        val isWorkSignature = avgVibration > VIBRATION_STATIONARY_THRESHOLD && newSpeedMps < 5.0
        if (isWorkSignature && distFromLast < JUMP_CHECK_MIN_DIST) {
            // Mechanical jitter during work - suppress promotion to avoid false alarms
            return false
        }
        
        return true
    }

    fun getWindow(): List<GtoNode> = window.toList()

    fun clear() {
        window.clear()
    }
}
