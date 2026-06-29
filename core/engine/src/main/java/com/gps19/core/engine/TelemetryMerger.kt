package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * v8.9.21:
 * - Issue #224: Added tiltIdx and baroIdx to the merge copy list for forensic parity.
 * v8.9.5:
 * - Issue #337: Added currentMa to the merge copy list to ensure power forensic parity.
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
                isStalled = current.isStalled,
                isJump = current.isJump,
                isTrajectoryPromoted = current.isTrajectoryPromoted,
                jumpTier = current.jumpTier,
                isJammer = current.isJammer,
                distToHome = current.distToHome,
                distToTracker = current.distToTracker,
                snrIdx = current.snrIdx,
                tiltIdx = current.tiltIdx,
                baroIdx = current.baroIdx,
                gnssDetail = incoming.gnssDetail ?: current.gnssDetail,
                currentMa = current.currentMa
            )
        } else {
            incoming
        }
    }
}
