package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * July.1.16:
 * - Issue #512: Consolidate Sentinel Statuses. Removed legacy flags (isJump, isJammer).
 * - Issue #511: Simplified telemetry mapping.
 */
object TelemetryMerger {

    fun merge(current: LocationUpdate, incoming: LocationUpdate): LocationUpdate {
        val incomingLat = incoming.lat
        val incomingLng = incoming.lng
        val hasIncomingGps = incomingLat != 0.0 && incomingLng != 0.0
        
        val isStale = TelemetryProcessor.isUpdateStale(incoming.gpsTs, current.gpsTs)
        val isMassiveRegression = TelemetryProcessor.isMassiveRegression(incoming.gpsTs, current.gpsTs)
        
        val preferCached = TelemetryProcessor.shouldPreferCachedCoordinates(
            hasIncomingGps = hasIncomingGps,
            isStale = isStale,
            isMassiveRegression = isMassiveRegression
        )

        return if (preferCached) {
            incoming.copy(
                lat = current.lat,
                lng = current.lng,
                alt = current.alt,
                speed = current.speed,
                accuracy = current.accuracy,
                bearing = current.bearing,
                gpsTs = current.gpsTs,
                ts = if (incoming.ts > 0) incoming.ts else current.ts,
                maxAccuracy = if (incoming.maxAccuracy > 0) incoming.maxAccuracy else current.maxAccuracy,
                status = current.status,
                jumpTier = current.jumpTier,
                distToHome = current.distToHome,
                distToTracker = current.distToTracker,
                gnssDetail = incoming.gnssDetail ?: current.gnssDetail,
                currentMa = current.currentMa
            )
        } else {
            incoming
        }
    }
}
