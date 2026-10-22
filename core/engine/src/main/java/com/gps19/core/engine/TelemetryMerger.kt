package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * Oct.22.13:
 * - ISSUE-OCT22.0-F4: Telemetry Sync Hardening. Ensured RTT and forensic sensor 
 *   indices (noise, lux, vibe, etc.) are merged even when coordinates are 
 *   preferred from cache to ensure Viewer HUD parity.
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
            // Keep target's spatial data, only update metadata and forensic indices from incoming
            target.ts = if (incoming.ts > 0) incoming.ts else target.ts
            target.rt = incoming.rt
            target.trackerId = incoming.deviceId
            target.viewerId = incoming.viewerId
            target.trackerState = incoming.trackerState
            target.status = incoming.status
            target.lastValidFixRt = incoming.lastValidFixRt
            
            if (incoming.kinetic.maxAccuracy > 0) {
                target.kinetic.maxAccuracy = incoming.kinetic.maxAccuracy
            }
            
            // Integrity & Forensic Merge
            target.integrity.gnssDetail = incoming.integrity.gnssDetail ?: target.integrity.gnssDetail
            target.integrity.rtt = incoming.integrity.rtt
            target.integrity.battery = incoming.integrity.battery
            target.integrity.isCharging = incoming.integrity.isCharging
            target.integrity.isLocationPending = incoming.integrity.isLocationPending
            target.integrity.locationPendingReason = incoming.integrity.locationPendingReason
            
            // Forensic Sensor Indices
            target.integrity.snrIdx = incoming.integrity.snrIdx
            target.atmospheric.noiseIdx = incoming.atmospheric.noiseIdx
            target.atmospheric.luxIdx = incoming.atmospheric.luxIdx
            target.atmospheric.vibeIdx = incoming.atmospheric.vibeIdx
            target.atmospheric.liftIdx = incoming.atmospheric.liftIdx
            target.atmospheric.tiltIdx = incoming.atmospheric.tiltIdx
            target.atmospheric.baroIdx = incoming.atmospheric.baroIdx
            target.atmospheric.temp = incoming.atmospheric.temp
            target.atmospheric.proximityCm = incoming.atmospheric.proximityCm
            
            target.violationUptimeMs = incoming.violationUptimeMs
        } else {
            // Overwrite target with incoming data
            target.copyFrom(incoming)
        }
    }
}
