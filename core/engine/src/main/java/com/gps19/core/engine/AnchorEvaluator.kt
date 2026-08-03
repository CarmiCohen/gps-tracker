package com.gps19.core.engine

import java.util.Locale
import kotlin.math.max

/**
 * AnchorEvaluator: Manages stationary anchor state and breakout logic.
 * Aug.03.37:
 * - Issue #669: Refactored to utilize mutable flyweight patterns for EngineGeoPoint 
 *   to resolve build errors and ensure zero-churn compliance (R668).
 * July.23.09:
 * - Fix: Coordinate averaging now only includes points within the breakout threshold.
 */
class AnchorEvaluator(
    private val onLog: (String, Double, Double, Double, Double?) -> Unit
) {
    private val parkingAnchorPoint = EngineGeoPoint()
    private var isAnchorActive = false
    private var anchorEscapeScore = 0.0
    
    // Flyweights for trend and averaging to eliminate per-tick allocation
    private val anchorTrendPoints = Array(ANCHOR_TREND_WINDOW_SIZE) { EngineGeoPoint() }
    private var trendCount = 0
    private var trendIdx = 0

    private val anchorAveragingBuffer = Array(ANCHOR_AVERAGING_WINDOW_SIZE) { EngineGeoPoint() }
    private var averageCount = 0
    private var averageIdx = 0

    private val optimizedPointFlyweight = EngineGeoPoint()
    private var isAnchorLockedState = false

    fun isLocked() = isAnchorLockedState
    fun getAnchorPoint() = if (isAnchorActive) parkingAnchorPoint else null

    fun reset() {
        isAnchorActive = false
        anchorEscapeScore = 0.0
        trendCount = 0
        trendIdx = 0
        averageCount = 0
        averageIdx = 0
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
            if (!isAnchorActive && isPhysicallyStationary) {
                parkingAnchorPoint.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                isAnchorActive = true
                anchorEscapeScore = 0.0
                trendCount = 0
                averageCount = 0
                
                // Add first point to averaging buffer
                val p = anchorAveragingBuffer[averageIdx]
                p.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                averageIdx = (averageIdx + 1) % ANCHOR_AVERAGING_WINDOW_SIZE
                averageCount = 1

                onLog(
                    "Stationary Anchor engaged at ${String.format(Locale.getDefault(), "%.5f, %.5f", point.lat, point.lng)} (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})",
                    point.lat, point.lng, point.accuracy, vibeIndex
                )
            }

            if (isAnchorActive) {
                // 2. Score Calculation & Breakout Threshold
                val breakoutThreshold = max(PARKING_ANCHOR_MIN_DIST, maxAccuracy * PARKING_ANCHOR_FACTOR)
                val distFromAnchor = PhysicsUtils.calculateDistance(parkingAnchorPoint.lat, parkingAnchorPoint.lng, point.lat, point.lng)

                // 3. Coordinate-averaging convergence (R990c Hardening)
                if (distFromAnchor < breakoutThreshold) {
                    val p = anchorAveragingBuffer[averageIdx]
                    p.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                    averageIdx = (averageIdx + 1) % ANCHOR_AVERAGING_WINDOW_SIZE
                    if (averageCount < ANCHOR_AVERAGING_WINDOW_SIZE) averageCount++

                    var sumLat = 0.0
                    var sumLng = 0.0
                    for (i in 0 until averageCount) {
                        sumLat += anchorAveragingBuffer[i].lat
                        sumLng += anchorAveragingBuffer[i].lng
                    }
                    parkingAnchorPoint.lat = sumLat / averageCount
                    parkingAnchorPoint.lng = sumLng / averageCount
                }

                if (!isPhysicallyStationary) {
                    anchorEscapeScore = ANCHOR_ESCAPE_SCORE_THRESHOLD
                } else {
                    val transitionZoneStart = breakoutThreshold * ANCHOR_TRANSITION_ZONE_START
                    if (distFromAnchor > transitionZoneStart) {
                        val accuracyPenalty = if (point.accuracy > ANCHOR_ACCURACY_PENALTY_LIMIT) {
                            (ANCHOR_ACCURACY_PENALTY_LIMIT / point.accuracy).coerceIn(0.2, 1.0)
                        } else 1.0

                        val imuDamping = if (isPhysicallyStationary) ANCHOR_IMU_DAMPING_FACTOR else 1.0

                        val zoneProgress = (distFromAnchor - transitionZoneStart) / (breakoutThreshold - transitionZoneStart)
                        var increment = (zoneProgress * 25.0).coerceIn(0.0, 50.0)
                        increment += (distFromAnchor - transitionZoneStart) * ANCHOR_DISPLACEMENT_WEIGHT

                        val safetyValveFactor = if (distFromAnchor > breakoutThreshold * 2.0) 2.0 else 1.0

                        anchorEscapeScore += (increment * accuracyPenalty * imuDamping * safetyValveFactor)
                    } else {
                        anchorEscapeScore = (anchorEscapeScore * 0.8).coerceAtLeast(0.0)
                    }

                    anchorEscapeScore += estimatedSpeed * ANCHOR_VELOCITY_WEIGHT_MPS

                    // Trend analysis using circular flyweight buffer
                    val tp = anchorTrendPoints[trendIdx]
                    tp.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                    trendIdx = (trendIdx + 1) % ANCHOR_TREND_WINDOW_SIZE
                    if (trendCount < ANCHOR_TREND_WINDOW_SIZE) trendCount++

                    if (trendCount >= ANCHOR_TREND_WINDOW_SIZE) {
                        val p0 = anchorTrendPoints[(trendIdx - 3 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        val p1 = anchorTrendPoints[(trendIdx - 2 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        val p2 = anchorTrendPoints[(trendIdx - 1 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        
                        val d1 = PhysicsUtils.calculateDistance(parkingAnchorPoint.lat, parkingAnchorPoint.lng, p0.lat, p0.lng)
                        val d2 = PhysicsUtils.calculateDistance(parkingAnchorPoint.lat, parkingAnchorPoint.lng, p1.lat, p1.lng)
                        val d3 = PhysicsUtils.calculateDistance(parkingAnchorPoint.lat, parkingAnchorPoint.lng, p2.lat, p2.lng)
                        
                        if (d3 > d2 && d2 > d1 && d3 > transitionZoneStart) {
                            anchorEscapeScore += 30.0
                        }
                    }
                }

                if (isAccuracySnap) {
                    anchorEscapeScore = (anchorEscapeScore * 0.5).coerceAtLeast(0.0)
                }

                // 4. Decision Logic
                if (anchorEscapeScore < ANCHOR_ESCAPE_SCORE_THRESHOLD && distFromAnchor < breakoutThreshold) {
                    skipPersistence = true
                    isLockedNow = true
                    
                    optimizedPointFlyweight.update(
                        lat = parkingAnchorPoint.lat,
                        lng = parkingAnchorPoint.lng,
                        alt = point.alt,
                        ts = point.ts,
                        rt = point.rt,
                        accuracy = point.accuracy,
                        maxAccuracy = point.maxAccuracy
                    )
                    finalPoint = optimizedPointFlyweight
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
            if (isAnchorActive) {
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
        isAnchorActive = false
        anchorEscapeScore = 0.0
        trendCount = 0
        averageCount = 0
    }
}
