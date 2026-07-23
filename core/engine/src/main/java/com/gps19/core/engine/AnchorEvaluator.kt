package com.gps19.core.engine

import java.util.Locale
import kotlin.math.max

/**
 * AnchorEvaluator: Manages stationary anchor state and breakout logic.
 * July.23.09:
 * - Fix: Coordinate averaging now only includes points within the breakout threshold
 *   to prevent the anchor from "chasing" a breakout drift (R990c hardening).
 * July.23.08:
 * - Extracted from LocationProcessor for architectural purity.
 * - Added Safety Valve: Accelerates breakout if displacement is consistently high 
 *   despite stationary IMU status (Risk mitigation for faulty accelerometers).
 */
class AnchorEvaluator(
    private val onLog: (String, Double, Double, Double, Double?) -> Unit
) {
    private var parkingAnchorPoint: EngineGeoPoint? = null
    private var anchorEscapeScore = 0.0
    private val anchorTrendPoints = mutableListOf<EngineGeoPoint>()
    private val anchorAveragingBuffer = mutableListOf<EngineGeoPoint>()
    private var isAnchorLockedState = false

    fun isLocked() = isAnchorLockedState
    fun getAnchorPoint() = parkingAnchorPoint

    fun reset() {
        parkingAnchorPoint = null
        anchorEscapeScore = 0.0
        anchorTrendPoints.clear()
        anchorAveragingBuffer.clear()
        isAnchorLockedState = false
    }

    data class AnchorResult(
        val isLocked: Boolean,
        val optimizedPoint: EngineGeoPoint,
        val shouldSkipPersistence: Boolean
    )

    fun evaluate(
        point: EngineGeoPoint,
        isPhysicallyStationary: Boolean,
        stationaryProb: Double,
        estimatedSpeed: Double,
        maxAccuracy: Double,
        isSuspicious: Boolean,
        isAdaptationMuzzled: Boolean,
        isAccuracySnap: Boolean,
        vibeIndex: Double?
    ): AnchorResult {
        var skipPersistence = false
        var isLockedNow = false
        var finalPoint = point

        if (!isSuspicious && !isAdaptationMuzzled && stationaryProb > ANCHOR_ENGAGEMENT_PROBABILITY) {
            // 1. Engagement Logic
            if (parkingAnchorPoint == null && isPhysicallyStationary) {
                parkingAnchorPoint = point
                anchorEscapeScore = 0.0
                anchorTrendPoints.clear()
                anchorAveragingBuffer.clear()
                anchorAveragingBuffer.add(point)
                onLog(
                    "Stationary Anchor engaged at ${String.format(Locale.getDefault(), "%.5f, %.5f", point.lat, point.lng)} (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})",
                    point.lat, point.lng, point.accuracy, vibeIndex
                )
            }

            if (parkingAnchorPoint != null) {
                // 2. Score Calculation & Breakout Threshold
                val breakoutThreshold = max(PARKING_ANCHOR_MIN_DIST, maxAccuracy * PARKING_ANCHOR_FACTOR)
                val distFromAnchor = PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, point.lat, point.lng)

                // 3. Coordinate-averaging convergence (R990c Hardening)
                // Only include points that are NOT currently causing a major breakout score accumulation
                if (distFromAnchor < breakoutThreshold) {
                    anchorAveragingBuffer.add(point)
                    if (anchorAveragingBuffer.size > ANCHOR_AVERAGING_WINDOW_SIZE) anchorAveragingBuffer.removeAt(0)

                    val avgLat = anchorAveragingBuffer.map { it.lat }.average()
                    val avgLng = anchorAveragingBuffer.map { it.lng }.average()
                    parkingAnchorPoint = parkingAnchorPoint!!.copy(lat = avgLat, lng = avgLng)
                }

                if (!isPhysicallyStationary) {
                    anchorEscapeScore = ANCHOR_ESCAPE_SCORE_THRESHOLD
                } else {
                    val transitionZoneStart = breakoutThreshold * ANCHOR_TRANSITION_ZONE_START
                    if (distFromAnchor > transitionZoneStart) {
                        // Accuracy-weighted penalty (Issue #530)
                        val accuracyPenalty = if (point.accuracy > ANCHOR_ACCURACY_PENALTY_LIMIT) {
                            (ANCHOR_ACCURACY_PENALTY_LIMIT / point.accuracy).coerceIn(0.2, 1.0)
                        } else 1.0

                        // IMU damping
                        val imuDamping = if (isPhysicallyStationary) ANCHOR_IMU_DAMPING_FACTOR else 1.0

                        val zoneProgress = (distFromAnchor - transitionZoneStart) / (breakoutThreshold - transitionZoneStart)
                        var increment = (zoneProgress * 25.0).coerceIn(0.0, 50.0)
                        increment += (distFromAnchor - transitionZoneStart) * ANCHOR_DISPLACEMENT_WEIGHT

                        // Safety Valve: If displacement is very high (> 2x threshold), reduce damping effect
                        val safetyValveFactor = if (distFromAnchor > breakoutThreshold * 2.0) 2.0 else 1.0

                        anchorEscapeScore += (increment * accuracyPenalty * imuDamping * safetyValveFactor)
                    } else {
                        anchorEscapeScore = (anchorEscapeScore * 0.8).coerceAtLeast(0.0)
                    }

                    anchorEscapeScore += estimatedSpeed * ANCHOR_VELOCITY_WEIGHT_MPS

                    // Trend analysis
                    anchorTrendPoints.add(point)
                    if (anchorTrendPoints.size > ANCHOR_TREND_WINDOW_SIZE) anchorTrendPoints.removeAt(0)
                    if (anchorTrendPoints.size >= ANCHOR_TREND_WINDOW_SIZE) {
                        val d1 = PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, anchorTrendPoints[0].lat, anchorTrendPoints[0].lng)
                        val d2 = PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, anchorTrendPoints[1].lat, anchorTrendPoints[1].lng)
                        val d3 = PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, anchorTrendPoints[2].lat, anchorTrendPoints[2].lng)
                        if (d3 > d2 && d2 > d1 && d3 > transitionZoneStart) {
                            anchorEscapeScore += 30.0
                        }
                    }
                }

                // Accuracy Snap suppression (Issue #529)
                if (isAccuracySnap) {
                    anchorEscapeScore = (anchorEscapeScore * 0.5).coerceAtLeast(0.0)
                }

                // 4. Decision Logic
                if (anchorEscapeScore < ANCHOR_ESCAPE_SCORE_THRESHOLD && distFromAnchor < breakoutThreshold) {
                    skipPersistence = true
                    isLockedNow = true
                    finalPoint = finalPoint.copy(lat = parkingAnchorPoint!!.lat, lng = parkingAnchorPoint!!.lng)
                } else {
                    val reason = when {
                        !isPhysicallyStationary -> "Physical Motion"
                        anchorEscapeScore >= ANCHOR_ESCAPE_SCORE_THRESHOLD -> "Displacement Trend (Score: ${anchorEscapeScore.toInt()})"
                        else -> "Distance Threshold"
                    }
                    onLog(
                        "Stationary Anchor breakout ($reason): Distance ${String.format(Locale.getDefault(), "%.1f", distFromAnchor)}m",
                        point.lat, point.lng, point.accuracy, vibeIndex
                    )
                    release()
                }
            }
        } else {
            if (parkingAnchorPoint != null) {
                onLog(
                    "Stationary Anchor released (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})",
                    point.lat, point.lng, point.accuracy, vibeIndex
                )
                release()
            }
        }

        isAnchorLockedState = isLockedNow
        return AnchorResult(isLockedNow, finalPoint, skipPersistence)
    }

    private fun release() {
        parkingAnchorPoint = null
        anchorEscapeScore = 0.0
        anchorTrendPoints.clear()
        anchorAveragingBuffer.clear()
    }
}
