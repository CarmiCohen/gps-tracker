package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * Oct.22.14:
 * - ISSUE-OCT22.0-F4: Forensic Sync Hardening. Expanded preferCached block 
 *   to ensure all forensic metadata (adaptive thresholds, anomaly flags, 
 *   audit metrics) are merged even when coordinates are stale.
 * Oct.22.13:
 * - ISSUE-OCT22.0-F4: Telemetry Sync Hardening. Ensured RTT and forensic sensor 
 *   indices (noise, lux, vibe, etc.) are merged even when coordinates are 
 *   preferred from cache to ensure Viewer HUD parity.
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
            target.isClockRegression = incoming.isClockRegression
            
            if (incoming.kinetic.maxAccuracy > 0) {
                target.kinetic.maxAccuracy = incoming.kinetic.maxAccuracy
            }
            
            // Integrity & Forensic Merge
            target.integrity.gnssDetail = incoming.integrity.gnssDetail ?: target.integrity.gnssDetail
            target.integrity.rtt = incoming.integrity.rtt
            target.integrity.battery = incoming.integrity.battery
            target.integrity.isCharging = incoming.integrity.isCharging
            target.integrity.currentMa = incoming.integrity.currentMa
            target.integrity.isLocationPending = incoming.integrity.isLocationPending
            target.integrity.locationPendingReason = incoming.integrity.locationPendingReason
            
            // Forensic Sensor Indices & Metadata
            target.integrity.snrIdx = incoming.integrity.snrIdx
            target.atmospheric.noiseIdx = incoming.atmospheric.noiseIdx
            target.atmospheric.luxIdx = incoming.atmospheric.luxIdx
            target.atmospheric.vibeIdx = incoming.atmospheric.vibeIdx
            target.atmospheric.liftIdx = incoming.atmospheric.liftIdx
            target.atmospheric.tiltIdx = incoming.atmospheric.tiltIdx
            target.atmospheric.baroIdx = incoming.atmospheric.baroIdx
            target.atmospheric.temp = incoming.atmospheric.temp
            target.atmospheric.proximityCm = incoming.atmospheric.proximityCm
            target.atmospheric.proximityDebounceMs = incoming.atmospheric.proximityDebounceMs
            target.atmospheric.adaptiveVibrationFloor = incoming.atmospheric.adaptiveVibrationFloor
            
            // Session Audit Parity
            target.violationUptimeMs = incoming.violationUptimeMs
            target.integrity.violationPercentage = incoming.integrity.violationPercentage
            target.violationStartTs = incoming.violationStartTs
            target.lastAlarmAckTs = incoming.lastAlarmAckTs
            
            // Anomaly Flags
            target.isSuspiciousNoise = incoming.isSuspiciousNoise
            target.isMemoryPressureThrottled = incoming.isMemoryPressureThrottled
            
            // Forensic Snapshots
            target.thermalSnapshot = incoming.thermalSnapshot
            target.heapSnapshot = incoming.heapSnapshot
            target.snrSnapshot = incoming.snrSnapshot
            target.vibeSnapshot = incoming.vibeSnapshot

            // Session Stats
            target.integrity.uptimeMs = incoming.integrity.uptimeMs
            target.integrity.totalConnectedMs = incoming.integrity.totalConnectedMs
            target.integrity.sessionConnectedMs = incoming.integrity.sessionConnectedMs
            target.integrity.totalDropMs = incoming.integrity.totalDropMs
            target.integrity.maxDropMs = incoming.integrity.maxDropMs
            target.integrity.lastConnTs = incoming.integrity.lastConnTs
            target.integrity.lastDiscTs = incoming.integrity.lastDiscTs
        } else {
            // Overwrite target with incoming data
            target.copyFrom(incoming)
        }
    }
}
