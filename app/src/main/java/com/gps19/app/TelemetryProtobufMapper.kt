package com.gps19.app

import com.gps19.core.engine.*

/**
 * TelemetryProtobufMapper: Centralized authority for telemetry serialization.
 * Sep.04.20:
 * - Issue #907 RESOLVED: System-Wide Interconnectivity Failure. Hardened ID 
 *   aliasing by using SignalingConstants.getTransmissionId() during serialization. 
 *   Ensures Protobuf parity with JSON path (T -> Trk) to prevent handshake 
 *   rejections on budget hardware (R907).
 * Sep.02.70:
 * - Idea #241: Protobuf Mapping Unification. Consolidated mapping logic for 
 *   RealtimeStatus (Signaling) and TrackerStatusProto (Persistence) to ensure 
 *   field parity and reduce Technical Debt (R-ID 241).
 */
object TelemetryProtobufMapper {

    /**
     * mapToRealtime: Maps TrackerStatus to RealtimeStatus (Signaling/Relay).
     */
    fun mapToRealtime(status: TrackerStatus, builder: RealtimeStatus.Builder, fromViewer: Boolean) {
        builder.apply {
            // R907: Ensure transmission IDs are aliased for relay room compatibility.
            setId(SignalingConstants.getTransmissionId(status.deviceId))
            setViewerId(SignalingConstants.getTransmissionId(status.viewerId))
            setFromViewer(fromViewer)
            
            // Common Geometry & Physics
            setLat(status.lat)
            setLng(status.lng)
            setAlt(status.alt)
            setSpeed(status.speed)
            setBearing(status.bearing)
            setAccuracy(status.accuracy)
            setMaxAccuracy(status.maxAccuracy)
            
            // Common Lifecycle
            setGpsTs(status.gpsTs)
            setTs(status.ts)
            setUptimeMs(status.uptimeMs)
            setTotalConnectedMs(status.totalConnectedMs)
            setSessionConnectedMs(status.sessionConnectedMs)
            setTotalDropMs(status.totalDropMs)
            setMaxDropMs(status.maxDropMs)
            setLastConnTs(status.lastConnTs)
            setLastDiscTs(status.lastDiscTs)
            
            // Common Health
            setBattery(status.battery)
            setTemp(status.temp)
            setIsCharging(status.isCharging)
            setSatsView(status.satsView)
            setSatsUsed(status.satsUsed)
            
            // Behavioral Flags
            setIsJammer(status.isJammer)
            setIsStalled(status.isStalled)
            setIsTamperDetected(status.isTamperDetected)
            setJumpTier(status.jumpTier)
            setIsLocationPending(status.isLocationPending)
            setLastValidFixRt(status.lastValidFixRt)
            setIsBatterySteepDischarge(status.isBatterySteepDischarge)
            setIsCoolingModeActive(status.isCoolingModeActive)
            
            // Forensic Indices
            setSnrIdx(status.snrIdx)
            setNoiseIdx(status.noiseIdx)
            setLuxIdx(status.luxIdx)
            setVibeIdx(status.vibeIdx)
            setLiftIdx(status.liftIdx)
            setTiltIdx(status.tiltIdx)
            setBaroIdx(status.baroIdx)
            setProxIdx(status.proxIdx)
            
            // SIT States
            setIsSitDetected(status.isSitDetected)
            setIsSitActive(status.isSitActive)
            setLastSitTs(status.lastSitTs)
            setSitVz(status.sitVz)
            setSitDz(status.sitDz)
            setSitBaro(status.sitBaro)
            setSitTilt(status.sitTilt)
            setSitShock(status.sitShock)
            setVerticalVelocity(status.verticalVelocity)
            
            // Extended Forensic
            setIsClockRegression(status.isClockRegression)
            setKineticEnergy(status.kineticEnergy)
            setSitVzTs(status.sitVzTs)
            setSitVzRt(status.sitVzRt)
            setIsAdaptiveJump(status.isAdaptiveJump)
            setIsBatteryLow(status.isBatteryLow)
            setIsBatteryCritical(status.isBatteryCritical)
            setIsSilentFailure(status.isSilentFailure)
            setViolationUptimeMs(status.violationUptimeMs)
            setIsUltraLongStationary(status.isUltraLongStationary)

            // Enums
            setState(TrackerStateProto.valueOf("TS_" + status.trackerState.name))
            setPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
        }
    }

    /**
     * mapToPersistence: Maps TrackerStatus to TrackerStatusProto (Local DataStore).
     */
    fun mapToPersistence(status: TrackerStatus, builder: TrackerStatusProto.Builder) {
        builder.apply {
            // Common Geometry & Physics
            setLat(status.lat)
            setLng(status.lng)
            setAlt(status.alt)
            setSpeed(status.speed)
            setBearing(status.bearing)
            setAccuracy(status.accuracy)
            setMaxAccuracy(status.maxAccuracy)
            
            // Common Lifecycle
            setGpsTs(status.gpsTs)
            setTs(status.ts)
            setRt(status.rt)
            setUptimeMs(status.uptimeMs)
            setTotalConnectedMs(status.totalConnectedMs)
            setSessionConnectedMs(status.sessionConnectedMs)
            setTotalDropMs(status.totalDropMs)
            setMaxDropMs(status.maxDropMs)
            setMaxDropTs(status.maxDropTs)
            setLastConnTs(status.lastConnTs)
            setLastDiscTs(status.lastDiscTs)
            
            // Common Health
            setBattery(status.battery)
            setTemp(status.temp)
            setMaxTemp(status.maxTemp)
            setIsCharging(status.isCharging)
            setSatsView(status.satsView)
            setSatsUsed(status.satsUsed)
            setCurrentMa(status.currentMa)
            
            // Behavioral Flags
            setIsJammer(status.isJammer)
            setIsStalled(status.isStalled)
            setIsTamperDetected(status.isTamperDetected)
            setJumpTier(status.jumpTier)
            setIsLocationPending(status.isLocationPending)
            setLastValidFixRt(status.lastValidFixRt)
            setIsBatterySteepDischarge(status.isBatterySteepDischarge)
            setIsCoolingModeActive(status.isCoolingModeActive)
            setIsPowerSaveMode(status.isPowerSaveMode)
            setStandbyBucket(status.standbyBucket)
            setIsStorageLow(status.isStorageLow)
            setIsStorageCritical(status.isStorageCritical)
            setIsPowerTamper(status.isPowerTamper)
            setMicPending(status.micPending)

            // Forensic Indices
            setSnrIdx(status.snrIdx)
            setNoiseIdx(status.noiseIdx)
            setLuxIdx(status.luxIdx)
            setVibeIdx(status.vibeIdx)
            setLiftIdx(status.liftIdx)
            setTiltIdx(status.tiltIdx)
            setBaroIdx(status.baroIdx)
            setProxIdx(status.proxIdx)
            
            // SIT States
            setIsSitDetected(status.isSitDetected)
            setIsSitActive(status.isSitActive)
            setLastSitTs(status.lastSitTs)
            setSitVz(status.sitVz)
            setSitDz(status.sitDz)
            setSitBaro(status.sitBaro)
            setSitTilt(status.sitTilt)
            setSitShock(status.sitShock)
            setVerticalVelocity(status.verticalVelocity)
            
            // Extended Forensic
            setIsClockRegression(status.isClockRegression)
            setKineticEnergy(status.kineticEnergy)
            setSitVzTs(status.sitVzTs)
            setSitVzRt(status.sitVzRt)
            setIsAdaptiveJump(status.isAdaptiveJump)
            setIsBatteryLow(status.isBatteryLow)
            setIsBatteryCritical(status.isBatteryCritical)
            setIsSilentFailure(status.isSilentFailure)
            setViolationUptimeMs(status.violationUptimeMs)
            setViolationPercentage(status.violationPercentage)
            setIsUltraLongStationary(status.isUltraLongStationary)
            setIsJump(status.isJump)

            // High-res Raw Sensors (Persistence Only)
            setVibration(status.vibration)
            setHeading(status.heading)
            setBaroAlt(status.baroAlt)
            setLux(status.lux)
            setIsNear(status.isNear)
            setTiltDegrees(status.tiltDegrees)
            setAcousticDb(status.acousticDb)
            setPeakShock(status.peakVibrationShock)
            setPeakShockTs(status.peakVibrationShockTs)
            setLuxBaseline(status.luxBaseline)
            setAcousticFloor(status.acousticFloorDb)
            setAdaptiveVibrationFloor(status.adaptiveVibrationFloor)
            setNetInterface(status.netInterface)
            setVer(BuildConfig.VERSION_NAME)
            
            // Idea #241: Persistence Parity Completion
            setDeviceId(status.deviceId)
            setViewerId(status.viewerId)
            setProximityCm(status.currentProximityCm)
            setProximityDebounceMs(status.proximityDebounceMs)
            setVibrationRollingSum(status.vibrationRollingSum)
            setIsTrajectoryPromoted(status.isTrajectoryPromoted)
            setIsSuspicious(status.isSuspicious)
            setIsAnchorLocked(status.isAnchorLocked)
            setIsBatteryWhitelisted(status.isBatteryWhitelisted)

            // Enums
            setTrackerState(status.trackerState.name)
            setStatus(status.status.name)
            setLocationPendingReason(LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name))
        }
    }
}
