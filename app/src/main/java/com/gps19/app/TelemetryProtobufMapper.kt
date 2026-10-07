package com.gps19.app

import com.gps19.core.engine.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * TelemetryProtobufMapper: Centralized authority for telemetry serialization.
 * Oct.7.5:
 * - Issue #SIMP-1007-15: Unified Snapshot Container. Ensured snr and vibe 
 *   snapshots are included in ConnectionPoint persistence mapping.
 * Oct.7.3:
 * - Issue #QA-1007-1: Forensic Expansion. Promoted internal engine flags 
 *   (muzzled, siren, hardware, snapshots, acoustic/light environmental states) 
 *   to Protobuf for remote diagnostics.
 * Oct.6.21:
 * - Issue #QA-1006-12: Forensic Hardening. Wrapped all floating-point fields 
 *   in PhysicsUtils.safeDouble during mapping to prevent SQLiteConstraintExceptions 
 *   (NaN/Infinity) in binary persistence blobs.
 */
object TelemetryProtobufMapper {

    /**
     * mapStatusToBinary: Direct serialization for offline buffering.
     * Always uses absolute coordinates to ensure independent restoration.
     */
    fun mapStatusToBinary(status: LocationUpdate): ByteArray {
        val builder = RealtimeStatus.newBuilder()
        mapToRealtime(status, builder, fromViewer = false, deltaState = null)
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
     * If deltaState is null, absolute coordinates are used.
     */
    fun mapToRealtime(
        status: LocationUpdate, 
        builder: RealtimeStatus.Builder, 
        fromViewer: Boolean,
        deltaState: SignalingDeltaState? = null
    ) {
        builder.setId(SignalingConstants.getTransmissionId(status.deviceId))
        builder.setViewerId(SignalingConstants.getTransmissionId(status.viewerId))
        builder.setFromViewer(fromViewer)
        
        builder.setAlt(PhysicsUtils.safeDouble(status.alt))
        builder.setSpeed(PhysicsUtils.safeDouble(status.speed))
        builder.setBearing(PhysicsUtils.safeDouble(status.bearing))
        builder.setAccuracy(PhysicsUtils.safeDouble(status.accuracy))
        builder.setMaxAccuracy(PhysicsUtils.safeDouble(status.maxAccuracy))
        
        if (deltaState != null) {
            val currentLatE7 = (status.lat * 1e7).toInt()
            val currentLngE7 = (status.lng * 1e7).toInt()
            
            val prevLat = deltaState.getAndSetLat(currentLatE7)
            val prevLng = deltaState.getAndSetLng(currentLngE7)
            
            if (prevLat == 0 || Math.abs(currentLatE7 - prevLat) > 10000000) {
                builder.setLat(PhysicsUtils.safeDouble(status.lat))
                builder.setLng(PhysicsUtils.safeDouble(status.lng))
                builder.setLatE7(currentLatE7)
                builder.setLngE7(currentLngE7)
                builder.setIsDelta(false)
            } else {
                builder.setLat(0.0)
                builder.setLng(0.0)
                builder.setLatE7(currentLatE7 - prevLat)
                builder.setLngE7(currentLngE7 - prevLng)
                builder.setIsDelta(true)
            }
        } else {
            builder.setLat(PhysicsUtils.safeDouble(status.lat))
            builder.setLng(PhysicsUtils.safeDouble(status.lng))
            builder.setLatE7((status.lat * 1e7).toInt())
            builder.setLngE7((status.lng * 1e7).toInt())
            builder.setIsDelta(false)
        }
        
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
        
        builder.setBattery(status.battery)
        builder.setTemp(PhysicsUtils.safeDouble(status.temp))
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        
        builder.setIsJammer(status.integrity.isJammer)
        builder.setIsStalled(status.integrity.isStalled)
        builder.setIsTamperDetected(status.integrity.isTamperDetected)
        builder.setJumpTier(status.jumpTier)
        builder.setIsLocationPending(status.isLocationPending)
        builder.setLastValidFixRt(status.lastValidFixRt)
        builder.setIsBatterySteepDischarge(status.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(status.isCoolingModeActive)
        builder.setIsPowerTamper(status.isPowerTamper)
        
        builder.setSnrIdx(PhysicsUtils.safeDouble(status.snrIdx))
        builder.setNoiseIdx(PhysicsUtils.safeDouble(status.noiseIdx))
        builder.setLuxIdx(PhysicsUtils.safeDouble(status.luxIdx))
        builder.setVibeIdx(PhysicsUtils.safeDouble(status.vibeIdx))
        builder.setLiftIdx(PhysicsUtils.safeDouble(status.atmospheric.liftIdx))
        builder.setTiltIdx(PhysicsUtils.safeDouble(status.tiltIdx))
        builder.setBaroIdx(PhysicsUtils.safeDouble(status.baroIdx))
        builder.setProxIdx(PhysicsUtils.safeDouble(status.proxIdx))
        
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(PhysicsUtils.safeDouble(status.sitVz))
        builder.setSitDz(PhysicsUtils.safeDouble(status.sitDz))
        builder.setSitBaro(PhysicsUtils.safeDouble(status.sitBaro))
        builder.setSitTilt(PhysicsUtils.safeDouble(status.sitTilt))
        builder.setSitShock(PhysicsUtils.safeDouble(status.sitShock))
        builder.setVerticalVelocity(PhysicsUtils.safeDouble(status.verticalVelocity))
        
        builder.setIsClockRegression(status.isClockRegression)
        builder.setKineticEnergy(PhysicsUtils.safeDouble(status.kineticEnergy))
        builder.setSitVzTs(status.integrity.sitVzTs)
        builder.setSitVzRt(status.integrity.sitVzRt)
        builder.setIsAdaptiveJump(status.isAdaptiveJump)
        builder.setIsBatteryLow(status.isBatteryLow)
        builder.setIsBatteryCritical(status.isBatteryCritical)
        builder.setIsSilentFailure(status.isSilentFailure)
        builder.setViolationUptimeMs(status.violationUptimeMs)
        builder.setIsUltraLongStationary(status.isUltraLongStationary)
        builder.setGpsHardwareLock(status.gpsHardwareLock)

        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(PhysicsUtils.safeDouble(status.integrity.lastEnergyDeltaTemp))
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        status.tamperNote?.let { builder.setTamperNote(it) }
        builder.setActivityType(status.activityType.name)
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

        builder.setCurrentMa(status.currentMa)
        builder.setThermalHeadroom(PhysicsUtils.safeDouble(status.integrity.thermalHeadroom))
        builder.setHeapAllocatedMb(PhysicsUtils.safeDouble(status.integrity.heapAllocatedMb))
        builder.setIsAnchorLocked(status.integrity.isAnchorLocked)
        builder.setIsBatteryWhitelisted(status.isBatteryWhitelisted)
        builder.setIsStorageLow(status.isStorageLow)
        builder.setIsStorageCritical(status.isStorageCritical)
        builder.setIsPowerSaveMode(status.isPowerSaveMode)
        builder.setStandbyBucket(status.standbyBucket)
        builder.setNetInterface(status.netInterface)

        status.thermalSnapshot?.let { builder.setThermalSnapshot(PhysicsUtils.safeDouble(it)) }
        status.heapSnapshot?.let { builder.setHeapSnapshot(PhysicsUtils.safeDouble(it)) }

        builder.setState(TrackerStateProto.valueOf("TS_" + status.trackerState.name))
        builder.setPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))

        // Issue #QA-1007-1: Forensic Expansion
        builder.setIsMuzzled(status.isMuzzled)
        builder.setIsSirenActive(status.isSirenActive)
        builder.setIsHardwareOnline(status.isHardwareOnline)
        builder.setLocalInternetLoss(status.localInternetLoss)
        status.suppressionNote?.let { builder.setSuppressionNote(it) }
        builder.setIsWarming(status.isWarming)
        status.snrSnapshot?.let { builder.setSnrSnapshot(PhysicsUtils.safeDouble(it)) }
        status.vibeSnapshot?.let { builder.setVibeSnapshot(PhysicsUtils.safeDouble(it)) }
        builder.setAcousticLockoutRt(status.acousticLockoutRt)
        builder.setLightSpikeRt(status.lightSpikeRt)
        builder.setProvidedAdaptiveFloor(PhysicsUtils.safeDouble(status.providedAdaptiveFloor))
        builder.setAcousticMinDb(PhysicsUtils.safeDouble(status.acousticMinDb))
    }

    /**
     * mapToPersistence: Maps LocationUpdate to TrackerStatusProto (Local DataStore).
     */
    fun mapToPersistence(status: LocationUpdate, builder: TrackerStatusProto.Builder) {
        builder.setLat(PhysicsUtils.safeDouble(status.lat))
        builder.setLng(PhysicsUtils.safeDouble(status.lng))
        builder.setAlt(PhysicsUtils.safeDouble(status.alt))
        builder.setSpeed(PhysicsUtils.safeDouble(status.speed))
        builder.setBearing(PhysicsUtils.safeDouble(status.bearing))
        builder.setAccuracy(PhysicsUtils.safeDouble(status.accuracy))
        builder.setMaxAccuracy(PhysicsUtils.safeDouble(status.maxAccuracy))
        
        builder.setLatE7((status.lat * 1e7).toInt())
        builder.setLngE7((status.lng * 1e7).toInt())
        
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
        
        builder.setBattery(status.battery)
        builder.setTemp(PhysicsUtils.safeDouble(status.temp))
        builder.setMaxTemp(PhysicsUtils.safeDouble(status.maxTemp))
        builder.setIsCharging(status.isCharging)
        builder.setSatsView(status.satsView)
        builder.setSatsUsed(status.satsUsed)
        builder.setCurrentMa(status.currentMa)
        
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

        builder.setSnrIdx(PhysicsUtils.safeDouble(status.snrIdx))
        builder.setNoiseIdx(PhysicsUtils.safeDouble(status.noiseIdx))
        builder.setLuxIdx(PhysicsUtils.safeDouble(status.luxIdx))
        builder.setVibeIdx(PhysicsUtils.safeDouble(status.vibeIdx))
        builder.setLiftIdx(PhysicsUtils.safeDouble(status.atmospheric.liftIdx))
        builder.setTiltIdx(PhysicsUtils.safeDouble(status.tiltIdx))
        builder.setBaroIdx(PhysicsUtils.safeDouble(status.baroIdx))
        builder.setProxIdx(PhysicsUtils.safeDouble(status.proxIdx))
        
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(PhysicsUtils.safeDouble(status.sitVz))
        builder.setSitDz(PhysicsUtils.safeDouble(status.sitDz))
        builder.setSitBaro(PhysicsUtils.safeDouble(status.sitBaro))
        builder.setSitTilt(PhysicsUtils.safeDouble(status.sitTilt))
        builder.setSitShock(PhysicsUtils.safeDouble(status.sitShock))
        builder.setVerticalVelocity(PhysicsUtils.safeDouble(status.verticalVelocity))
        
        builder.setIsClockRegression(status.isClockRegression)
        builder.setKineticEnergy(PhysicsUtils.safeDouble(status.kineticEnergy))
        builder.setSitVzTs(status.integrity.sitVzTs)
        builder.setSitVzRt(status.integrity.sitVzRt)
        builder.setIsAdaptiveJump(status.isAdaptiveJump)
        builder.setIsBatteryLow(status.isBatteryLow)
        builder.setIsBatteryCritical(status.isBatteryCritical)
        builder.setIsSilentFailure(status.isSilentFailure)
        builder.setViolationUptimeMs(status.violationUptimeMs)
        builder.setViolationPercentage(PhysicsUtils.safeDouble(status.violationPercentage))
        builder.setIsUltraLongStationary(status.isUltraLongStationary)
        builder.setIsJump(status.isJump)

        builder.setVibration(PhysicsUtils.safeDouble(status.vibration))
        builder.setHeading(PhysicsUtils.safeDouble(status.heading))
        builder.setBaroAlt(PhysicsUtils.safeDouble(status.baroAlt))
        builder.setLux(PhysicsUtils.safeDouble(status.lux))
        builder.setIsNear(status.isNear)
        builder.setTiltDegrees(PhysicsUtils.safeDouble(status.tiltDegrees))
        builder.setAcousticDb(PhysicsUtils.safeDouble(status.acousticDb))
        builder.setPeakShock(PhysicsUtils.safeDouble(status.peakVibrationShock))
        builder.setPeakShockTs(status.peakVibrationShockTs)
        builder.setLuxBaseline(PhysicsUtils.safeDouble(status.luxBaseline))
        builder.setAcousticFloor(PhysicsUtils.safeDouble(status.acousticFloorDb))
        builder.setAdaptiveVibrationFloor(PhysicsUtils.safeDouble(status.adaptiveVibrationFloor))
        builder.setNetInterface(status.netInterface)
        builder.setVer("Oct7.3")
        
        builder.setDeviceId(status.deviceId)
        builder.setViewerId(status.viewerId)
        builder.setProximityCm(PhysicsUtils.safeDouble(status.currentProximityCm))
        builder.setProximityDebounceMs(status.proximityDebounceMs)
        builder.setVibrationRollingSum(PhysicsUtils.safeDouble(status.vibrationRollingSum))
        builder.setIsTrajectoryPromoted(status.isTrajectoryPromoted)
        builder.setIsSuspicious(status.isSuspicious)
        builder.setIsAnchorLocked(status.isAnchorLocked)
        builder.setIsBatteryWhitelisted(status.isBatteryWhitelisted)
        builder.setGpsHardwareLock(status.gpsHardwareLock)

        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(PhysicsUtils.safeDouble(status.integrity.lastEnergyDeltaTemp))
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        status.tamperNote?.let { builder.setTamperNote(it) }
        builder.setActivityType(status.activityType.name)
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

        builder.setThermalHeadroom(PhysicsUtils.safeDouble(status.integrity.thermalHeadroom))
        builder.setHeapAllocatedMb(PhysicsUtils.safeDouble(status.integrity.heapAllocatedMb))

        status.thermalSnapshot?.let { builder.setThermalSnapshot(PhysicsUtils.safeDouble(it)) }
        status.heapSnapshot?.let { builder.setHeapSnapshot(PhysicsUtils.safeDouble(it)) }

        builder.setTrackerState(status.trackerState.name)
        builder.setStatus(status.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))

        // Issue #QA-1007-1: Forensic Expansion
        builder.setIsMuzzled(status.isMuzzled)
        builder.setIsSirenActive(status.isSirenActive)
        builder.setIsHardwareOnline(status.isHardwareOnline)
        builder.setLocalInternetLoss(status.localInternetLoss)
        status.suppressionNote?.let { builder.setSuppressionNote(it) }
        builder.setIsWarming(status.isWarming)
        status.snrSnapshot?.let { builder.setSnrSnapshot(PhysicsUtils.safeDouble(it)) }
        status.vibeSnapshot?.let { builder.setVibeSnapshot(PhysicsUtils.safeDouble(it)) }
        builder.setAcousticLockoutRt(status.acousticLockoutRt)
        builder.setLightSpikeRt(status.lightSpikeRt)
        builder.setProvidedAdaptiveFloor(PhysicsUtils.safeDouble(status.providedAdaptiveFloor))
        builder.setAcousticMinDb(PhysicsUtils.safeDouble(status.acousticMinDb))
    }

    /**
     * mapAppToPersistence: Maps app-level ConnectionPoint to TrackerStatusProto.
     */
    fun mapAppToPersistence(p: ConnectionPoint, builder: TrackerStatusProto.Builder) {
        builder.setTs(p.ts).setRt(p.rt).setRtt(p.rtt).setTotalConnectedMs(0) 
        builder.setBattery(p.isBatteryLow.let { if (it) 15 else 50 }) 
        builder.setAccuracy(PhysicsUtils.safeDouble(p.gpsAccuracy)).setMaxAccuracy(PhysicsUtils.safeDouble(p.maxAccuracy))
        builder.setSpeed(PhysicsUtils.safeDouble(p.speed)).setBearing(PhysicsUtils.safeDouble(p.bearing))
        
        builder.setSnrIdx(PhysicsUtils.safeDouble(p.snrIdx))
        builder.setNoiseIdx(PhysicsUtils.safeDouble(p.noiseIdx))
        builder.setLuxIdx(PhysicsUtils.safeDouble(p.luxIdx))
        builder.setVibeIdx(PhysicsUtils.safeDouble(p.vibeIdx))
        builder.setProxIdx(PhysicsUtils.safeDouble(p.proxIdx))
        builder.setLiftIdx(PhysicsUtils.safeDouble(p.liftIdx))
        builder.setTiltIdx(PhysicsUtils.safeDouble(p.tiltIdx))
        builder.setBaroIdx(PhysicsUtils.safeDouble(p.baroIdx))
        builder.setVerticalVelocity(PhysicsUtils.safeDouble(p.verticalVelocity))
        builder.setIsSitDetected(p.isSitDetected).setIsSitActive(p.isSitActive)
        builder.setSitVz(PhysicsUtils.safeDouble(p.sitVz)).setSitVzTs(p.sitVzTs).setSitVzRt(p.sitVzRt).setSitDz(PhysicsUtils.safeDouble(p.sitDz))
        builder.setSitBaro(PhysicsUtils.safeDouble(p.sitBaro)).setSitTilt(PhysicsUtils.safeDouble(p.sitTilt)).setSitShock(PhysicsUtils.safeDouble(p.sitShock))
        
        builder.setIsBatterySteepDischarge(p.isBatterySteepDischarge)
        builder.setIsCoolingModeActive(p.isCoolingModeActive)
        builder.setIsBatteryLow(p.isBatteryLow).setIsBatteryCritical(p.isBatteryCritical)
        builder.setViolationUptimeMs(p.violationUptimeMs).setIsUltraLongStationary(p.isUltraLongStationary)
        builder.setGpsHardwareLock(p.gpsHardwareLock).setIsAnchorLocked(p.isAnchorLocked)
        builder.setIsGnssThrottled(p.isGnssThrottled)
        builder.setThermalHeadroom(PhysicsUtils.safeDouble(p.thermalHeadroom)).setHeapAllocatedMb(PhysicsUtils.safeDouble(p.heapAllocatedMb))
        builder.setActivityType(p.activityType.name)
        
        p.snrSnapshot?.let { builder.setSnrSnapshot(PhysicsUtils.safeDouble(it)) }
        p.vibeSnapshot?.let { builder.setVibeSnapshot(PhysicsUtils.safeDouble(it)) }
        p.thermalSnapshot?.let { builder.setThermalSnapshot(PhysicsUtils.safeDouble(it)) }
        p.heapSnapshot?.let { builder.setHeapSnapshot(PhysicsUtils.safeDouble(it)) }

        builder.setStatus(p.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + p.locationPendingReason.name))
    }
}
