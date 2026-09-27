package com.gps19.core.engine

import java.util.Locale
import kotlin.math.max

/**
 * AnchorEvaluator: Manages stationary anchor state and breakout logic.
 * Sep.27.5:
 * - Issue #1349: Mutability Reduction. Adapted to access fields via state.anchor.
 */
object AnchorEvaluator {

    data class AnchorResult(
        val isLocked: Boolean,
        val optimizedPoint: EngineGeoPoint,
        val shouldSkipPersistence: Boolean
    )

    fun evaluate(
        state: LocationProcessingState,
        point: EngineGeoPoint,
        isPhysicallyStationary: Boolean,
        stationaryProb: Double,
        estimatedSpeed: Double,
        maxAccuracy: Double,
        isSuspicious: Boolean,
        isAdaptationMuzzled: Boolean,
        isAccuracySnap: Boolean,
        snr: Double = 0.0,
        vibeIndex: Double?,
        onLog: (String, Double, Double, Double, Double?) -> Unit
    ): AnchorResult {
        var skipPersistence = false
        var isLockedNow = false
        var finalPoint = point

        val isLowSnr = snr > 0 && snr < JUMP_GATE_LOW_SNR_THRESHOLD
        val shouldHoldAnchor = state.anchor.isActive && isPhysicallyStationary && isLowSnr
        
        if (!isSuspicious && !isAdaptationMuzzled && (stationaryProb > ANCHOR_ENGAGEMENT_PROBABILITY || shouldHoldAnchor)) {
            // 1. Engagement Logic
            if (!state.anchor.isActive && isPhysicallyStationary) {
                state.anchor.parkingPoint.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                state.anchor.isActive = true
                state.anchor.escapeScore = 0.0
                state.anchor.trendCount = 0
                state.anchor.averageCount = 0
                
                // Add first point to averaging buffer
                val p = state.anchor.averagingBuffer[state.anchor.averageIdx]
                p.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                state.anchor.averageIdx = (state.anchor.averageIdx + 1) % ANCHOR_AVERAGING_WINDOW_SIZE
                state.anchor.averageCount = 1

                onLog(
                    "Stationary Anchor engaged at ${String.format(Locale.getDefault(), "%.5f, %.5f", point.lat, point.lng)} (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})",
                    point.lat, point.lng, point.accuracy, vibeIndex
                )
            }

            if (state.anchor.isActive) {
                // 2. Score Calculation & Breakout Threshold
                val breakoutThreshold = max(PARKING_ANCHOR_MIN_DIST, maxAccuracy * PARKING_ANCHOR_FACTOR)
                val distFromAnchor = PhysicsUtils.calculateDistance(state.anchor.parkingPoint.lat, state.anchor.parkingPoint.lng, point.lat, point.lng)
                val transitionZoneStart = breakoutThreshold * ANCHOR_TRANSITION_ZONE_START

                // 3. Coordinate-averaging convergence
                if (distFromAnchor < transitionZoneStart) {
                    val p = state.anchor.averagingBuffer[state.anchor.averageIdx]
                    p.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                    state.anchor.averageIdx = (state.anchor.averageIdx + 1) % ANCHOR_AVERAGING_WINDOW_SIZE
                    if (state.anchor.averageCount < ANCHOR_AVERAGING_WINDOW_SIZE) state.anchor.averageCount++

                    var sumLat = 0.0
                    var sumLng = 0.0
                    for (i in 0 until state.anchor.averageCount) {
                        sumLat += state.anchor.averagingBuffer[i].lat
                        sumLng += state.anchor.averagingBuffer[i].lng
                    }
                    state.anchor.parkingPoint.lat = sumLat / state.anchor.averageCount
                    state.anchor.parkingPoint.lng = sumLng / state.anchor.averageCount
                }

                if (!isPhysicallyStationary) {
                    state.anchor.escapeScore = ANCHOR_ESCAPE_SCORE_THRESHOLD
                } else {
                    if (distFromAnchor > transitionZoneStart) {
                        val accuracyPenalty = if (point.accuracy > ANCHOR_ACCURACY_PENALTY_LIMIT) {
                            (ANCHOR_ACCURACY_PENALTY_LIMIT / point.accuracy).coerceIn(0.2, 1.0)
                        } else 1.0

                        val imuDamping = if (isPhysicallyStationary) ANCHOR_IMU_DAMPING_FACTOR else 1.0
                        val snrDamping = if (isLowSnr) ANCHOR_SKEPTICISM_LOW_SNR_FACTOR else 1.0

                        val zoneProgress = (distFromAnchor - transitionZoneStart) / (breakoutThreshold - transitionZoneStart)
                        var increment = (zoneProgress * 25.0).coerceIn(0.0, 50.0)
                        increment += (distFromAnchor - transitionZoneStart) * ANCHOR_DISPLACEMENT_WEIGHT

                        val safetyValveFactor = if (distFromAnchor > breakoutThreshold * 2.0) 2.0 else 1.0

                        state.anchor.escapeScore += (increment * accuracyPenalty * imuDamping * snrDamping * safetyValveFactor)
                    } else {
                        state.anchor.escapeScore = (state.anchor.escapeScore * 0.8).coerceAtLeast(0.0)
                    }

                    state.anchor.escapeScore += estimatedSpeed * ANCHOR_VELOCITY_WEIGHT_MPS

                    // Trend analysis
                    val tp = state.anchor.trendPoints[state.anchor.trendIdx]
                    tp.update(point.lat, point.lng, point.alt, point.ts, point.rt, point.accuracy, point.maxAccuracy)
                    state.anchor.trendIdx = (state.anchor.trendIdx + 1) % ANCHOR_TREND_WINDOW_SIZE
                    if (state.anchor.trendCount < ANCHOR_TREND_WINDOW_SIZE) state.anchor.trendCount++

                    if (state.anchor.trendCount >= ANCHOR_TREND_WINDOW_SIZE) {
                        val p0 = state.anchor.trendPoints[(state.anchor.trendIdx - 3 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        val p1 = state.anchor.trendPoints[(state.anchor.trendIdx - 2 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        val p2 = state.anchor.trendPoints[(state.anchor.trendIdx - 1 + ANCHOR_TREND_WINDOW_SIZE) % ANCHOR_TREND_WINDOW_SIZE]
                        
                        val d1 = PhysicsUtils.calculateDistance(state.anchor.parkingPoint.lat, state.anchor.parkingPoint.lng, p0.lat, p0.lng)
                        val d2 = PhysicsUtils.calculateDistance(state.anchor.parkingPoint.lat, state.anchor.parkingPoint.lng, p1.lat, p1.lng)
                        val d3 = PhysicsUtils.calculateDistance(state.anchor.parkingPoint.lat, state.anchor.parkingPoint.lng, p2.lat, p2.lng)
                        
                        if (d3 > d2 && d2 > d1 && d3 > transitionZoneStart) {
                            state.anchor.escapeScore += 30.0
                        }
                    }
                }

                if (isAccuracySnap) {
                    state.anchor.escapeScore = (state.anchor.escapeScore * 0.5).coerceAtLeast(0.0)
                }

                // 4. Decision Logic
                val effectiveBreakoutThreshold = if (isAccuracySnap) breakoutThreshold * 1.5 else breakoutThreshold
                if (state.anchor.escapeScore < ANCHOR_ESCAPE_SCORE_THRESHOLD && distFromAnchor < effectiveBreakoutThreshold) {
                    skipPersistence = true
                    isLockedNow = true
                    
                    state.optimizedPointFlyweight.update(
                        lat = state.anchor.parkingPoint.lat,
                        lng = state.anchor.parkingPoint.lng,
                        alt = point.alt,
                        ts = point.ts,
                        rt = point.rt,
                        accuracy = point.accuracy,
                        maxAccuracy = point.maxAccuracy
                    )
                    finalPoint = state.optimizedPointFlyweight
                } else {
                    val reason = when {
                        !isPhysicallyStationary -> "Physical Motion"
                        state.anchor.escapeScore >= ANCHOR_ESCAPE_SCORE_THRESHOLD -> "Displacement Trend (Score: ${state.anchor.escapeScore.toInt()})"
                        else -> "Distance Threshold"
                    }
                    onLog(
                        "Stationary Anchor breakout ($reason): Distance ${String.format(Locale.getDefault(), "%.1f", distFromAnchor)}m",
                        point.lat, point.lng, point.accuracy, vibeIndex
                    )
                    reset(state)
                }
            }
        } else {
            if (state.anchor.isActive) {
                onLog(
                    "Stationary Anchor released (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})",
                    point.lat, point.lng, point.accuracy, vibeIndex
                )
                reset(state)
            }
        }

        state.anchor.isLocked = isLockedNow
        return AnchorResult(isLockedNow, finalPoint, skipPersistence)
    }

    fun reset(state: LocationProcessingState) {
        state.anchor.isActive = false
        state.anchor.escapeScore = 0.0
        state.anchor.trendCount = 0
        state.anchor.averageCount = 0
        state.anchor.isLocked = false
    }
}