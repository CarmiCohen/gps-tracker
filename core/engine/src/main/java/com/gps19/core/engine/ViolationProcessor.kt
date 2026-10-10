package com.gps19.core.engine

import kotlin.math.max

/**
 * ViolationProcessor: Pure logic for evaluating and deduplicating violations.
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API.
 */
class ViolationProcessor(private val timeProvider: TimeProvider) {

    private var lastViolationLat = 0.0
    private var lastViolationLng = 0.0
    private var lastViolationRealtime = 0L
    private var lastViolationType = ""

    /**
     * Checks if a new violation should be recorded based on proximity and time thresholds.
     */
    fun shouldRecordViolation(
        lat: Double,
        lng: Double,
        type: String,
        accuracy: Double,
        maxAccuracy: Double
    ): Boolean {
        val nowRt = timeProvider.elapsedRealtime
        
        val gate = max(10.0, maxAccuracy)

        if (type == lastViolationType && (nowRt - lastViolationRealtime < 5000L)) {
            val dist = PhysicsUtils.calculateDistance(lat, lng, lastViolationLat, lastViolationLng)
            if (dist < gate) return false
        }

        lastViolationLat = lat
        lastViolationLng = lng
        lastViolationRealtime = nowRt
        lastViolationType = type
        return true
    }
}
