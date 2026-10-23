package com.gps19.core.engine

/**
 * TelemetryMerger: Pure logic for aggregating and merging telemetry updates.
 * Oct.23.105:
 * - ISSUE-OCT23.101-F1: Remediation. Ensured all critical integrity flags 
 *   (isJammer, isStalled, battery low, etc.), SIT metrics, and raw sensor 
 *   values are merged in preferCached block to prevent Viewer state staleing.
 * Oct.22.16:
 * - ISSUE-OCT22.0-F4: Forensic Sync Hardening. Ensured luxSnapshot and 
 *   acousticSnapshot are merged in preferCached block for full trace parity.
 * Oct.22.15:
 * - ISSUE-OCT22.0-F4: Forensic Sync Hardening. Ensured isJammingCandidate is merged.
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
            
            // Kinetic Metadata
            if (incoming.kinetic.maxAccuracy > 0) {
                target.kinetic.maxAccuracy = incoming.kinetic.maxAccuracy
            }
            target.kinetic.verticalVelocity = incoming.kinetic.verticalVelocity
            target.kinetic.activityType = incoming.kinetic.activityType
            
            // Integrity & Forensic Merge
            target.integrity.gnssDetail = incoming.integrity.gnssDetail ?: target.integrity.gnssDetail
            target.integrity.rtt = incoming.integrity.rtt
            target.integrity.battery = incoming.integrity.battery
            target.integrity.isCharging = incoming.integrity.isCharging
            target.integrity.currentMa = incoming.integrity.currentMa
            target.integrity.isLocationPending = incoming.integrity.isLocationPending
            target.integrity.locationPendingReason = incoming.integrity.locationPendingReason
            
            // Critical Integrity Flags (F1 Remediation)
            target.integrity.isJammer = incoming.integrity.isJammer
            target.integrity.isStalled = incoming.integrity.isStalled
            target.integrity.isBatteryLow = incoming.integrity.isBatteryLow
            target.integrity.isBatteryCritical = incoming.integrity.isBatteryCritical
            target.integrity.isTamperDetected = incoming.integrity.isTamperDetected
            target.integrity.isPowerTamper = incoming.integrity.isPowerTamper
            target.integrity.isSuspicious = incoming.integrity.isSuspicious
            target.integrity.isAnchorLocked = incoming.integrity.isAnchorLocked
            target.integrity.gpsHardwareLock = incoming.integrity.gpsHardwareLock
            target.integrity.isCoolingModeActive = incoming.integrity.isCoolingModeActive
            target.integrity.isUltraLongStationary = incoming.integrity.isUltraLongStationary
            target.integrity.isGnssThrottled = incoming.integrity.isGnssThrottled
            target.integrity.isBatterySteepDischarge = incoming.integrity.isBatterySteepDischarge
            target.integrity.isPowerSaveMode = incoming.integrity.isPowerSaveMode
            target.integrity.standbyBucket = incoming.integrity.standbyBucket
            target.integrity.netInterface = incoming.integrity.netInterface
            target.integrity.isStorageLow = incoming.integrity.isStorageLow
            target.integrity.isStorageCritical = incoming.integrity.isStorageCritical
            target.integrity.micPending = incoming.integrity.micPending
            target.integrity.isSilentFailure = incoming.integrity.isSilentFailure
            target.integrity.isMaliAnomaly = incoming.integrity.isMaliAnomaly
            target.integrity.isBatteryWhitelisted = incoming.integrity.isBatteryWhitelisted
            
            // SIT Metrics (F5 Parity)
            target.integrity.isSitActive = incoming.integrity.isSitActive
            target.integrity.isSitDetected = incoming.integrity.isSitDetected
            target.integrity.lastSitTs = incoming.integrity.lastSitTs
            target.integrity.sitVz = incoming.integrity.sitVz
            target.integrity.sitVzTs = incoming.integrity.sitVzTs
            target.integrity.sitVzRt = incoming.integrity.sitVzRt
            target.integrity.sitDz = incoming.integrity.sitDz
            target.integrity.sitBaro = incoming.integrity.sitBaro
            target.integrity.sitTilt = incoming.integrity.sitTilt
            target.integrity.sitShock = incoming.integrity.sitShock
            
            // Performance & Power Audit
            target.integrity.cpuLoad = incoming.integrity.cpuLoad
            target.integrity.ioWait = incoming.integrity.ioWait
            target.integrity.maxIoLatency = incoming.integrity.maxIoLatency
            target.integrity.thermalHeadroom = incoming.integrity.thermalHeadroom
            target.integrity.heapAllocatedMb = incoming.integrity.heapAllocatedMb
            target.integrity.signal = incoming.integrity.signal ?: target.integrity.signal
            target.integrity.tamperNote = incoming.integrity.tamperNote ?: target.integrity.tamperNote
            target.integrity.lastEnergyDeltaMa = incoming.integrity.lastEnergyDeltaMa
            target.integrity.lastEnergyDeltaTemp = incoming.integrity.lastEnergyDeltaTemp
            target.integrity.lastEnergyDurationMs = incoming.integrity.lastEnergyDurationMs
            
            // Forensic Sensor Indices & Metadata
            target.integrity.snrIdx = incoming.integrity.snrIdx
            target.atmospheric.noiseIdx = incoming.atmospheric.noiseIdx
            target.atmospheric.luxIdx = incoming.atmospheric.luxIdx
            target.atmospheric.vibeIdx = incoming.atmospheric.vibeIdx
            target.atmospheric.liftIdx = incoming.atmospheric.liftIdx
            target.atmospheric.tiltIdx = incoming.atmospheric.tiltIdx
            target.atmospheric.baroIdx = incoming.atmospheric.baroIdx
            target.atmospheric.temp = incoming.atmospheric.temp
            target.atmospheric.maxTemp = incoming.atmospheric.maxTemp
            target.atmospheric.baroAlt = incoming.atmospheric.baroAlt
            target.atmospheric.proximityCm = incoming.atmospheric.proximityCm
            target.atmospheric.proximityDebounceMs = incoming.atmospheric.proximityDebounceMs
            target.atmospheric.isNear = incoming.atmospheric.isNear
            target.atmospheric.proxIdx = incoming.atmospheric.proxIdx
            target.atmospheric.adaptiveVibrationFloor = incoming.atmospheric.adaptiveVibrationFloor
            
            // Raw Sensor Parity (F10)
            target.atmospheric.vibration = incoming.atmospheric.vibration
            target.atmospheric.vibrationRollingSum = incoming.atmospheric.vibrationRollingSum
            target.atmospheric.acousticDb = incoming.atmospheric.acousticDb
            target.atmospheric.acousticFloorDb = incoming.atmospheric.acousticFloorDb
            target.atmospheric.lux = incoming.atmospheric.lux
            target.atmospheric.luxBaseline = incoming.atmospheric.luxBaseline
            target.atmospheric.tiltDegrees = incoming.atmospheric.tiltDegrees
            target.atmospheric.heading = incoming.atmospheric.heading
            target.atmospheric.peakVibrationShock = incoming.atmospheric.peakVibrationShock
            target.atmospheric.peakVibrationShockTs = incoming.atmospheric.peakVibrationShockTs
            
            // Native Hysteresis (F3)
            target.atmospheric.stationaryDurationMs = incoming.atmospheric.stationaryDurationMs
            
            // Session Audit Parity
            target.violationUptimeMs = incoming.violationUptimeMs
            target.integrity.violationPercentage = incoming.integrity.violationPercentage
            target.violationStartTs = incoming.violationStartTs
            target.lastAlarmAckTs = incoming.lastAlarmAckTs
            
            // Anomaly Flags
            target.isSuspiciousNoise = incoming.isSuspiciousNoise
            target.isMemoryPressureThrottled = incoming.isMemoryPressureThrottled
            target.isJammingCandidate = incoming.isJammingCandidate
            
            // Forensic Snapshots
            target.thermalSnapshot = incoming.thermalSnapshot
            target.heapSnapshot = incoming.heapSnapshot
            target.snrSnapshot = incoming.snrSnapshot
            target.vibeSnapshot = incoming.vibeSnapshot
            target.luxSnapshot = incoming.luxSnapshot
            target.acousticSnapshot = incoming.acousticSnapshot

            // Session Stats
            target.integrity.uptimeMs = incoming.integrity.uptimeMs
            target.integrity.totalConnectedMs = incoming.integrity.totalConnectedMs
            target.integrity.sessionConnectedMs = incoming.integrity.sessionConnectedMs
            target.integrity.totalDropMs = incoming.integrity.totalDropMs
            target.integrity.maxDropMs = incoming.integrity.maxDropMs
            target.integrity.maxDropTs = incoming.integrity.maxDropTs
            target.integrity.lastConnTs = incoming.integrity.lastConnTs
            target.integrity.lastDiscTs = incoming.integrity.lastDiscTs
        } else {
            // Overwrite target with incoming data
            target.copyFrom(incoming)
        }
    }
}
