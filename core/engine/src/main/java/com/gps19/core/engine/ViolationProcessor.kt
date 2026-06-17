package com.gps19.core.engine

/**
 * ViolationProcessor: Pure logic for evaluating and deduplicating violations.
 * v8.8.21: Extracted from MainRepository to decouple logic from persistence.
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
        adaptiveRadius: Double
    ): Boolean {
        val nowRt = timeProvider.elapsedRealtime()
        val fuzzyThreshold = maxOf(10.0, adaptiveRadius)

        if (type == lastViolationType && (nowRt - lastViolationRealtime < 5000L)) {
            val dist = PhysicsUtils.calculateDistance(lat, lng, lastViolationLat, lastViolationLng)
            if (dist < fuzzyThreshold) return false
        }

        // Update local state if it's a new unique violation
        lastViolationLat = lat
        lastViolationLng = lng
        lastViolationRealtime = nowRt
        lastViolationType = type
        return true
    }
}
