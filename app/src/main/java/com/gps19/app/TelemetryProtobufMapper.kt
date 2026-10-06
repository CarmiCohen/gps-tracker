package com.gps19.app

import com.gps19.core.engine.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * TelemetryProtobufMapper: Centralized authority for telemetry serialization.
 * Oct.6.20:
 * - Issue #SIGN-1006-12: Removed static signaling delta state. mapToRealtime now 
 *   requires a SignalingDeltaState instance (Rule 1.125).
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
        
        builder.setAlt(status.alt)
        builder.setSpeed(status.speed)
        builder.setBearing(status.bearing)
        builder.setAccuracy(status.accuracy)
        builder.setMaxAccuracy(status.maxAccuracy)
        
        if (deltaState != null) {
            val currentLatE7 = (status.lat * 1e7).toInt()
            val currentLngE7 = (status.lng * 1e7).toInt()
            
            val prevLat = deltaState.getAndSetLat(currentLatE7)
            val prevLng = deltaState.getAndSetLng(currentLngE7)
            
            // If it's the first update or a large jump (> 1 deg), send absolute
            if (prevLat == 0 || Math.abs(currentLatE7 - prevLat) > 10000000) {
                builder.setLat(status.lat)
                builder.setLng(status.lng)
                builder.setLatE7(currentLatE7)
                builder.setLngE7(currentLngE7)
                builder.setIsDelta(false)
            } else {
                // Optimization: Set doubles to 0.0 so they are omitted from the wire in Proto3
                builder.setLat(0.0)
                builder.setLng(0.0)
                builder.setLatE7(currentLatE7 - prevLat)
                builder.setLngE7(currentLngE7 - prevLng)
                builder.setIsDelta(true)
            }
        } else {
            builder.setLat(status.lat)
            builder.setLng(status.lng)
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
        builder.setTemp(status.temp)
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
        
        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.atmospheric.liftIdx)
        builder.setTiltIdx(status.tiltIdx)
        builder.setBaroIdx(status.baroIdx)
        builder.setProxIdx(status.proxIdx)
        
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(status.sitVz)
        builder.setSitDz(status.sitDz)
        builder.setSitBaro(status.sitBaro)
        builder.setSitTilt(status.sitTilt)
        builder.setSitShock(status.sitShock)
        builder.setVerticalVelocity(status.verticalVelocity)
        
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

        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.integrity.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        status.tamperNote?.let { builder.setTamperNote(it) }
        builder.setActivityType(status.activityType.name)
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

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

        status.thermalSnapshot?.let { builder.setThermalSnapshot(it) }
        status.heapSnapshot?.let { builder.setHeapSnapshot(it) }

        builder.setState(TrackerStateProto.valueOf("TS_" + status.trackerState.name))
        builder.setPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }

    /**
     * mapToPersistence: Maps LocationUpdate to TrackerStatusProto (Local DataStore).
     */
    fun mapToPersistence(status: LocationUpdate, builder: TrackerStatusProto.Builder) {
        builder.setLat(status.lat)
        builder.setLng(status.lng)
        builder.setAlt(status.alt)
        builder.setSpeed(status.speed)
        builder.setBearing(status.bearing)
        builder.setAccuracy(status.accuracy)
        builder.setMaxAccuracy(status.maxAccuracy)
        
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
        builder.setTemp(status.temp)
        builder.setMaxTemp(status.maxTemp)
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

        builder.setSnrIdx(status.snrIdx)
        builder.setNoiseIdx(status.noiseIdx)
        builder.setLuxIdx(status.luxIdx)
        builder.setVibeIdx(status.vibeIdx)
        builder.setLiftIdx(status.atmospheric.liftIdx)
        builder.setTiltIdx(status.tiltIdx)
        builder.setBaroIdx(status.baroIdx)
        builder.setProxIdx(status.proxIdx)
        
        builder.setIsSitDetected(status.isSitDetected)
        builder.setIsSitActive(status.isSitActive)
        builder.setLastSitTs(status.lastSitTs)
        builder.setSitVz(status.sitVz)
        builder.setSitDz(status.sitDz)
        builder.setSitBaro(status.sitBaro)
        builder.setSitTilt(status.sitTilt)
        builder.setSitShock(status.sitShock)
        builder.setVerticalVelocity(status.verticalVelocity)
        
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

        builder.setIsGnssThrottled(status.isGnssThrottled)
        builder.setEnergyDeltaMa(status.integrity.lastEnergyDeltaMa)
        builder.setEnergyDeltaTemp(status.integrity.lastEnergyDeltaTemp)
        builder.setEnergyDurationMs(status.integrity.lastEnergyDurationMs)
        
        status.tamperNote?.let { builder.setTamperNote(it) }
        builder.setActivityType(status.activityType.name)
        builder.setLastAlarmAckTs(status.lastAlarmAckTs)
        builder.setViolationStartTs(status.violationStartTs)

        builder.setThermalHeadroom(status.integrity.thermalHeadroom)
        builder.setHeapAllocatedMb(status.integrity.heapAllocatedMb)

        status.thermalSnapshot?.let { builder.setThermalSnapshot(it) }
        status.heapSnapshot?.let { builder.setHeapSnapshot(it) }

        builder.setTrackerState(status.trackerState.name)
        builder.setStatus(status.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
    }

    /**
     * mapAppToPersistence: Maps app-level ConnectionPoint to TrackerStatusProto.
     */
    fun mapAppToPersistence(p: ConnectionPoint, builder: TrackerStatusProto.Builder) {
        builder.setTs(p.ts).setRt(p.rt).setRtt(p.rtt).setTotalConnectedMs(0) 
        builder.setBattery(p.isBatteryLow.let { if (it) 15 else 50 }) 
        builder.setAccuracy(p.gpsAccuracy).setMaxAccuracy(p.maxAccuracy)
        builder.setSpeed(p.speed).setBearing(p.bearing)
        
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
        
        p.thermalSnapshot?.let { builder.setThermalSnapshot(it) }
        p.heapSnapshot?.let { builder.setHeapSnapshot(it) }

        builder.setStatus(p.status.name)
        builder.setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + p.locationPendingReason.name))
    }
}
