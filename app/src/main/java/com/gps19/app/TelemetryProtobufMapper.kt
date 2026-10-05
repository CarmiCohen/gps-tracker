package com.gps19.app

import com.gps19.core.engine.*

/**
 * TelemetryProtobufMapper: Centralized authority for telemetry serialization.
 * Oct.5.1:
 * - Issue #1173: Protobuf-First Persistence (Phase 2). Expanded parity fields 
 *   for RealtimeStatus and TrackerStatusProto. Added binary entry points 
 *   for persistence BLOBs.
 * Oct.2.9:
 * - Issue #1314: TrackerStatus Convergence. Migrated to unified LocationUpdate 
 *   monolith DTO. Fixed liftIdx and battery flag references.
 */
object TelemetryProtobufMapper {

    /**
     * mapStatusToBinary: Direct serialization of LocationUpdate for offline buffering.
     */
    fun mapStatusToBinary(status: LocationUpdate): ByteArray {
        val builder = RealtimeStatus.newBuilder()
        mapToRealtime(status, builder, false)
        return builder.build().toByteArray()
    }

    /**
     * mapAppToBinary: Direct serialization of ConnectionPoint for ribbon history.
     */
    fun mapAppToBinary(p: ConnectionPoint): ByteArray {
        val builder = TrackerStatusProto.newBuilder()
        mapAppToPersistence(p, builder)
        return builder.build().toByteArray()
    }

    /**
     * mapToRealtime: Maps LocationUpdate to RealtimeStatus (Signaling/Relay).
     */
    fun mapToRealtime(status: LocationUpdate, builder: RealtimeStatus.Builder, fromViewer: Boolean) {
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
        builder.setUptimeMs(status.integrity.uptimeMs)
        builder.setTotalConnectedMs(status.integrity.totalConnectedMs)
        builder.setSessionConnectedMs(status.integrity.sessionConnectedMs)
        builder.setTotalDropMs(status.integrity.totalDropMs)
        builder.setMaxDropMs(status.integrity.maxDropMs)
        builder.setLastConnTs(status.integrity.lastConnTs)
        builder.setLastDiscTs(status.integrity.lastDiscTs)
        
        // Common Health
        builder.setBattery(status.battery)
        builder.setTemp(status.temp)
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        
        // Behavioral Flags
        builder.setIsJammer(status.integrity.isJammer)
        builder.setIsStalled(status.integrity.isStalled)
        builder.setIsTamperDetected(status.integrity.isTamperDetected)
        builder.setJumpTier(status.jumpTier)
        builder.setIsLocationPending(status.isLocationPending)
        builder.setLastValidFixRt(status.lastValidFixRt)
        builder.setIsBatterySteepDischarge(status.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(status.isCoolingModeActive)
        builder.setIsPowerTamper(status.isPowerTamper)
        
        // Forensic Indices
        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.atmospheric.liftIdx)
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
        builder.setSitVzTs(status.integrity.sitVzTs)
        builder.setSitVzRt(status.integrity.sitVzRt)
        builder.setIsAdaptiveJump(status.isAdaptiveJump)
        builder.setIsBatteryLow(status.isBatteryLow)
        builder.setIsBatteryCritical(status.isBatteryCritical)
        builder.setIsSilentFailure(status.isSilentFailure)
        builder.setViolationUptimeMs(status.violationUptimeMs)
        builder.setIsUltraLongStationary(status.isUltraLongStationary)
        builder.setGpsHardwareLock(status.gpsHardwareLock)

        // Issue #924 & R-ID 259
        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.integrity.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        // Issue #946: Forensic Reason propagation
        status.tamperNote?.let { builder.setTamperNote(it) }

        // Issue #1205: Activity context
        builder.setActivityType(status.activityType.name)

        // Issue #1410: Viewer Persistence
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

        // Issue #1173: Persistence Parity
        builder.setCurrentMa(status.currentMa)
        builder.setThermalHeadroom(status.integrity.thermalHeadroom)
        builder.setHeapAllocatedMb(status.integrity.heapAllocatedMb)
        builder.setIsAnchorLocked(status.integrity.isAnchorLocked)
        builder.setIsBatteryWhitelisted(status.isBatteryWhitelisted)
        builder.setIsStorageLow(status.isStorageLow)
        builder.setIsStorageCritical(status.isStorageCritical)
        builder.setIsPowerSaveMode(status.isPowerSaveMode)
        builder.setStandbyBucket(status.standbyBucket)
        builder.setNetInterface(status.netInterface)

        // Enums
        builder.setState(TrackerStateProto.valueOf("TS_" + status.trackerState.name))
        builder.setPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }

    /**
     * mapToPersistence: Maps LocationUpdate to TrackerStatusProto (Local DataStore).
     */
    fun mapToPersistence(status: LocationUpdate, builder: TrackerStatusProto.Builder) {
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
        builder.setUptimeMs(status.integrity.uptimeMs)
        builder.setTotalConnectedMs(status.integrity.totalConnectedMs)
        builder.setSessionConnectedMs(status.integrity.sessionConnectedMs)
        builder.setTotalDropMs(status.integrity.totalDropMs)
        builder.setMaxDropMs(status.integrity.maxDropMs)
        builder.setMaxDropTs(status.integrity.maxDropTs)
        builder.setLastConnTs(status.integrity.lastConnTs)
        builder.setLastDiscTs(status.integrity.lastDiscTs)
        
        // Common Health
        builder.setBattery(status.battery)
        builder.setTemp(status.temp)
        builder.setMaxTemp(status.maxTemp)
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        builder.setCurrentMa(status.currentMa)
        
        // Behavioral Flags
        builder.setIsJammer(status.integrity.isJammer)
        builder.setIsStalled(status.integrity.isStalled)
        builder.setIsTamperDetected(status.integrity.isTamperDetected)
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
        builder.setMicPending(status.integrity.micPending)

        // Forensic Indices
        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.atmospheric.liftIdx)
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
        builder.setSitVzTs(status.integrity.sitVzTs)
        builder.setSitVzRt(status.integrity.sitVzRt)
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
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.integrity.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        // Issue #946: Forensic Reason propagation
        status.tamperNote?.let { builder.setTamperNote(it) }

        // Issue #1205: Activity context
        builder.setActivityType(status.activityType.name)

        // Issue #1410: Viewer Persistence
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

        // Issue #1173: Parity Expansion
        builder.setThermalHeadroom(status.integrity.thermalHeadroom)
        builder.setHeapAllocatedMb(status.integrity.heapAllocatedMb)

        // Enums
        builder.setTrackerState(status.trackerState.name)
        builder.setStatus(status.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }

    /**
     * mapAppToPersistence: Maps app-level ConnectionPoint to TrackerStatusProto.
     */
    fun mapAppToPersistence(p: ConnectionPoint, builder: TrackerStatusProto.Builder) {
        builder.setTs(p.ts).setRt(p.rt).setRtt(p.rtt).setTotalConnectedMs(0) // Dummy for ribbon
        builder.setBattery(p.isBatteryLow.let { if (it) 15 else 50 }) // Rough approximation if needed
        builder.setAccuracy(p.gpsAccuracy).setMaxAccuracy(p.maxAccuracy)
        builder.setSpeed(p.speed).setBearing(p.bearing)
        
        // Forensic
        builder.setSnrIdx(p.snrIdx).setNoiseIdx(p.noiseIdx).setLuxIdx(p.luxIdx).setVibeIdx(p.vibeIdx)
        builder.setProxIdx(p.proxIdx).setLiftIdx(p.liftIdx).setTiltIdx(p.tiltIdx).setBaroIdx(p.baroIdx)
        builder.setVerticalVelocity(p.verticalVelocity)
        builder.setIsSitDetected(p.isSitDetected).setIsSitActive(p.isSitActive)
        builder.setSitVz(p.sitVz).setSitVzTs(p.sitVzTs).setSitVzRt(p.sitVzRt).setSitDz(p.sitDz)
        builder.setSitBaro(p.sitBaro).setSitTilt(p.sitTilt).setSitShock(p.sitShock)
        
        builder.setIsBatterySteepDischarge(p.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(p.isCoolingModeActive)
        builder.setIsBatteryLow(p.isBatteryLow).setIsBatteryCritical(p.isBatteryCritical)
        builder.setViolationUptimeMs(p.violationUptimeMs).setIsUltraLongStationary(p.isUltraLongStationary)
        builder.setGpsHardwareLock(p.gpsHardwareLock).setIsAnchorLocked(p.isAnchorLocked)
        builder.setIsGnssThrottled(p.isGnssThrottled)
        builder.setThermalHeadroom(p.thermalHeadroom).setHeapAllocatedMb(p.heapAllocatedMb)
        builder.setActivityType(p.activityType.name)
        
        builder.setStatus(p.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + p.locationPendingReason.name))
    }
}
