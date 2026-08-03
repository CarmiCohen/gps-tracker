package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Refactored to mergeInto() to support 
 *   in-place mutation of LocationUpdate flyweights (R-HARDWARE-01).
 * July.1.16:
 * - Issue #512: Consolidate Sentinel Statuses.
 */
object TelemetryMerger {

    /**
     * mergeInto: Updates the [target] flyweight with data from [incoming].
     */
    fun mergeInto(target: LocationUpdate, incoming: LocationUpdate) {
        val incomingLat = incoming.lat
        val incomingLng = incoming.lng
        val hasIncomingGps = incomingLat != 0.0 && incomingLng != 0.0
        
        val isStale = TelemetryProcessor.isUpdateStale(incoming.gpsTs, target.gpsTs)
        val isMassiveRegression = TelemetryProcessor.isMassiveRegression(incoming.gpsTs, target.gpsTs)
        
        val preferCached = TelemetryProcessor.shouldPreferCachedCoordinates(
            hasIncomingGps = hasIncomingGps,
            isStale = isStale,
            isMassiveRegression = isMassiveRegression
        )

        if (preferCached) {
            // Keep target's spatial data, only update metadata from incoming
            target.ts = if (incoming.ts > 0) incoming.ts else target.ts
            if (incoming.maxAccuracy > 0) target.maxAccuracy = incoming.maxAccuracy
            target.gnssDetail = incoming.gnssDetail ?: target.gnssDetail
        } else {
            // Overwrite target with incoming data
            target.copyFrom(incoming)
        }
    }
}
