package com.gps19.app

import com.gps19.core.engine.*

/**
 * TelemetryProtobufMapper: Centralized authority for telemetry serialization.
 * Sep.10.40:
 * - Issue #946 Visibility RESOLVED: Added tamperNote mapping for forensic 
 *   transparency in both Realtime (binary) and Persistence builders (R-ID 288).
 * Sep.08.12:
 * - Issue #924 Visibility: Mapped isGnssThrottled for A15 Hysteresis 
 *   transparency in remote HUDs (R-ID 267).
 * - R-ID 259: Mapped structured Energy Footprint verdicts (deltaMa, deltaTemp, 
 *   durationMs) for structured auditing parity.
 * Sep.08.10:
 * - Issue #935 RESOLVED: Monotonic Signaling Hardening. Added rt field 
 *   mapping to RealtimeStatus to eliminate heuristic drift in remote HUDs.
 */
object TelemetryProtobufMapper {

    /**
     * mapToRealtime: Maps TrackerStatus to RealtimeStatus (Signaling/Relay).
     */
    fun mapToRealtime(status: TrackerStatus, builder: RealtimeStatus.Builder, fromViewer: Boolean) {
        // R907: Ensure transmission IDs are aliased for relay room compatibility.
        builder.setId(SignalingConstants.getTransmissionId(status.deviceId))
        builder.setViewerId(SignalingConstants.getTransmissionId(status.viewerId))
        builder.setFromViewer(fromViewer)
        
        // Common Geometry & Physics
        builder.setLat(status.lat)
        builder.setLng(status.lng)
        builder.setAlt(status.alt)
        builder.setSpeed(status.speed)
        builder.setBearing(status.bearing)
        builder.setAccuracy(status.accuracy)
        builder.setMaxAccuracy(status.maxAccuracy)
        
        // Common Lifecycle
        builder.setGpsTs(status.gpsTs)
        builder.setTs(status.ts)
        builder.setRt(status.rt)
        builder.setUptimeMs(status.uptimeMs)
        builder.setTotalConnectedMs(status.totalConnectedMs)
        builder.setSessionConnectedMs(status.sessionConnectedMs)
        builder.setTotalDropMs(status.totalDropMs)
        builder.setMaxDropMs(status.maxDropMs)
        builder.setLastConnTs(status.lastConnTs)
        builder.setLastDiscTs(status.lastDiscTs)
        
        // Common Health
        builder.setBattery(status.battery)
        builder.setTemp(status.temp)
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        
        // Behavioral Flags
        builder.setIsJammer(status.isJammer)
        builder.setIsStalled(status.isStalled)
        builder.setIsTamperDetected(status.isTamperDetected)
        builder.setJumpTier(status.jumpTier)
        builder.setIsLocationPending(status.isLocationPending)
        builder.setLastValidFixRt(status.lastValidFixRt)
        builder.setIsBatterySteepDischarge(status.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(status.isCoolingModeActive)
        
        // Forensic Indices
        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.liftIdx)
        builder.setTiltIdx(status.tiltIdx)
        builder.setBaroIdx(status.baroIdx)
        builder.setProxIdx(status.proxIdx)
        
        // SIT States
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(status.sitVz)
        builder.setSitDz(status.sitDz)
        builder.setSitBaro(status.sitBaro)
        builder.setSitTilt(status.sitTilt)
        builder.setSitShock(status.sitShock)
        builder.setVerticalVelocity(status.verticalVelocity)
        
        // Extended Forensic
        builder.setIsClockRegression(status.isClockRegression)
        builder.setKineticEnergy(status.kineticEnergy)
        builder.setSitVzTs(status.sitVzTs)
        builder.setSitVzRt(status.sitVzRt)
        builder.setIsAdaptiveJump(status.isAdaptiveJump)
        builder.setIsBatteryLow(status.isBatteryLow)
        builder.setIsBatteryCritical(status.isBatteryCritical)
        builder.setIsSilentFailure(status.isSilentFailure)
        builder.setViolationUptimeMs(status.violationUptimeMs)
        builder.setIsUltraLongStationary(status.isUltraLongStationary)
        builder.setGpsHardwareLock(status.gpsHardwareLock)

        // Issue #924 & R-ID 259
        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.lastEnergyDurationMs)
        
        // Issue #946: Forensic Reason propagation
        status.tamperNote?.let { builder.setTamperNote(it) }

        // Enums
        builder.setState(TrackerStateProto.valueOf("TS_" + status.trackerState.name))
        builder.setPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }

    /**
     * mapToPersistence: Maps TrackerStatus to TrackerStatusProto (Local DataStore).
     */
    fun mapToPersistence(status: TrackerStatus, builder: TrackerStatusProto.Builder) {
        // Common Geometry & Physics
        builder.setLat(status.lat)
        builder.setLng(status.lng)
        builder.setAlt(status.alt)
        builder.setSpeed(status.speed)
        builder.setBearing(status.bearing)
        builder.setAccuracy(status.accuracy)
        builder.setMaxAccuracy(status.maxAccuracy)
        
        // Common Lifecycle
        builder.setGpsTs(status.gpsTs)
        builder.setTs(status.ts)
        builder.setRt(status.rt)
        builder.setUptimeMs(status.uptimeMs)
        builder.setTotalConnectedMs(status.totalConnectedMs)
        builder.setSessionConnectedMs(status.sessionConnectedMs)
        builder.setTotalDropMs(status.totalDropMs)
        builder.setMaxDropMs(status.maxDropMs)
        builder.setMaxDropTs(status.maxDropTs)
        builder.setLastConnTs(status.lastConnTs)
        builder.setLastDiscTs(status.lastDiscTs)
        
        // Common Health
        builder.setBattery(status.battery)
        builder.setTemp(status.temp)
        builder.setMaxTemp(status.maxTemp)
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        builder.setCurrentMa(status.currentMa)
        
        // Behavioral Flags
        builder.setIsJammer(status.isJammer)
        builder.setIsStalled(status.isStalled)
        builder.setIsTamperDetected(status.isTamperDetected)
        builder.setJumpTier(status.jumpTier)
        builder.setIsLocationPending(status.isLocationPending)
        builder.setLastValidFixRt(status.lastValidFixRt)
        builder.setIsBatterySteepDischarge(status.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(status.isCoolingModeActive)
        builder.setIsPowerSaveMode(status.isPowerSaveMode)
        builder.setStandbyBucket(status.standbyBucket)
        builder.setIsStorageLow(status.isStorageLow)
        builder.setIsStorageCritical(status.isStorageCritical)
        builder.setIsPowerTamper(status.isPowerTamper)
        builder.setMicPending(status.micPending)

        // Forensic Indices
        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.liftIdx)
        builder.setTiltIdx(status.tiltIdx)
        builder.setBaroIdx(status.baroIdx)
        builder.setProxIdx(status.proxIdx)
        
        // SIT States
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(status.sitVz)
        builder.setSitDz(status.sitDz)
        builder.setSitBaro(status.sitBaro)
        builder.setSitTilt(status.sitTilt)
        builder.setSitShock(status.sitShock)
        builder.setVerticalVelocity(status.verticalVelocity)
        
        // Extended Forensic
        builder.setIsClockRegression(status.isClockRegression)
        builder.setKineticEnergy(status.kineticEnergy)
        builder.setSitVzTs(status.sitVzTs)
        builder.setSitVzRt(status.sitVzRt)
        builder.setIsAdaptiveJump(status.isAdaptiveJump)
        builder.setIsBatteryLow(status.isBatteryLow)
        builder.setIsBatteryCritical(status.isBatteryCritical)
        builder.setIsSilentFailure(status.isSilentFailure)
        builder.setViolationUptimeMs(status.violationUptimeMs)
        builder.setViolationPercentage(status.violationPercentage)
        builder.setIsUltraLongStationary(status.isUltraLongStationary)
        builder.setIsJump(status.isJump)

        // High-res Raw Sensors (Persistence Only)
        builder.setVibration(status.vibration)
        builder.setHeading(status.heading)
        builder.setBaroAlt(status.baroAlt)
        builder.setLux(status.lux)
        builder.setIsNear(status.isNear)
        builder.setTiltDegrees(status.tiltDegrees)
        builder.setAcousticDb(status.acousticDb)
        builder.setPeakShock(status.peakVibrationShock)
        builder.setPeakShockTs(status.peakVibrationShockTs)
        builder.setLuxBaseline(status.luxBaseline)
        builder.setAcousticFloor(status.acousticFloorDb)
        builder.setAdaptiveVibrationFloor(status.adaptiveVibrationFloor)
        builder.setNetInterface(status.netInterface)
        builder.setVer(BuildConfig.VERSION_NAME)
        
        // Idea #241: Persistence Parity Completion
        builder.setDeviceId(status.deviceId)
        builder.setViewerId(status.viewerId)
        builder.setProximityCm(status.currentProximityCm)
        builder.setProximityDebounceMs(status.proximityDebounceMs)
        builder.setVibrationRollingSum(status.vibrationRollingSum)
        builder.setIsTrajectoryPromoted(status.isTrajectoryPromoted)
        builder.setIsSuspicious(status.isSuspicious)
        builder.setIsAnchorLocked(status.isAnchorLocked)
        builder.setIsBatteryWhitelisted(status.isBatteryWhitelisted)
        builder.setGpsHardwareLock(status.gpsHardwareLock)

        // Issue #924 & R-ID 259
        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.lastEnergyDurationMs)
        
        // Issue #946: Forensic Reason propagation
        status.tamperNote?.let { builder.setTamperNote(it) }

        // Enums
        builder.setTrackerState(status.trackerState.name)
        builder.setStatus(status.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }
}
