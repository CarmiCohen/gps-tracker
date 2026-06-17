package com.gps19.core.engine

/**
 * TelemetryProcessor: Pure logic for merging and validating telemetry updates.
 * v8.8.21: Extracted from TelemetryRepository to ensure logic purity.
 */
object TelemetryProcessor {

    /**
     * Determines if an incoming update is stale or a massive clock regression.
     */
    fun isUpdateStale(incomingGpsTs: Long, currentGpsTs: Long): Boolean {
        return incomingGpsTs > 0 && currentGpsTs > 0 && incomingGpsTs < currentGpsTs
    }

    fun isMassiveRegression(incomingGpsTs: Long, currentGpsTs: Long): Boolean {
        return isUpdateStale(incomingGpsTs, currentGpsTs) && (currentGpsTs - incomingGpsTs) > 86400000L
    }

    /**
     * Logic to determine if we should favor the current (cached) coordinates 
     * over the incoming ones (e.g., if the incoming packet has no GPS or is stale).
     */
    fun shouldPreferCachedCoordinates(
        hasIncomingGps: Boolean,
        isStale: Boolean,
        isMassiveRegression: Boolean
    ): Boolean {
        return (!hasIncomingGps || isStale) && !isMassiveRegression
    }
}
