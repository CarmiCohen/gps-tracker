package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * Sep.09.10:
 * - Legacy Field Cleanup: Migrated to partitioned states (.kinetic, .integrity) 
 *   to support bridge removal in LocationUpdate (R-ID 284).
 */
object TelemetryMerger {

    /**
     * mergeInto: Updates the [target] flyweight with data from [incoming].
     */
    fun mergeInto(target: LocationUpdate, incoming: LocationUpdate) {
        val incomingLat = incoming.kinetic.lat
        val incomingLng = incoming.kinetic.lng
        val hasIncomingGps = incomingLat != 0.0 && incomingLng != 0.0
        
        val isStale = TelemetryProcessor.isUpdateStale(incoming.kinetic.gpsTs, target.kinetic.gpsTs)
        val isMassiveRegression = TelemetryProcessor.isMassiveRegression(incoming.kinetic.gpsTs, target.kinetic.gpsTs)
        
        val preferCached = TelemetryProcessor.shouldPreferCachedCoordinates(
            hasIncomingGps = hasIncomingGps,
            isStale = isStale,
            isMassiveRegression = isMassiveRegression
        )

        if (preferCached) {
            // Keep target's spatial data, only update metadata from incoming
            target.ts = if (incoming.ts > 0) incoming.ts else target.ts
            if (incoming.kinetic.maxAccuracy > 0) {
                target.kinetic.maxAccuracy = incoming.kinetic.maxAccuracy
            }
            target.integrity.gnssDetail = incoming.integrity.gnssDetail ?: target.integrity.gnssDetail
        } else {
            // Overwrite target with incoming data
            target.copyFrom(incoming)
        }
    }
}
