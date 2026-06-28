package com.gps19.core.engine

import kotlin.math.max

/**
 * ViolationProcessor: Pure logic for evaluating and deduplicating violations.
 * v8.9.42:
 * - Issue #325: Authoritative Spatial Anchoring. Refactored shouldRecordViolation 
 *   to utilize authoritative maxAccuracy for spatial deduplication.
 * v8.8.21: Extracted from MainRepository to decouple logic from persistence.
 */
class ViolationProcessor(private val timeProvider: TimeProvider) {

    private var lastViolationLat = 0.0
    private var lastViolationLng = 0.0
    private var lastViolationRealtime = 0L
    private var lastViolationType = ""

    /**
     * Checks if a new violation should be recorded based on proximity and time thresholds.
     * R325: Using maxAccuracy (uncertainty) as the spatial gate for deduplication.
     */
    fun shouldRecordViolation(
        lat: Double,
        lng: Double,
        type: String,
        accuracy: Float,
        maxAccuracy: Float
    ): Boolean {
        val nowRt = timeProvider.elapsedRealtime()
        
        // Logical anchor: Deduplication threshold is the greater of 10m or the authoritative uncertainty.
        val gate = max(10.0, maxAccuracy.toDouble())

        if (type == lastViolationType && (nowRt - lastViolationRealtime < 5000L)) {
            val dist = PhysicsUtils.calculateDistance(lat, lng, lastViolationLat, lastViolationLng)
            if (dist < gate) return false
        }

        // Update local state if it's a new unique violation
        lastViolationLat = lat
        lastViolationLng = lng
        lastViolationRealtime = nowRt
        lastViolationType = type
        return true
    }
}
