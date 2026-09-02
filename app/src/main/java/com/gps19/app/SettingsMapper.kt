package com.gps19.app

import com.gps19.core.engine.*

/**
 * SettingsMapper: Conversion logic between DataStore Protos and Domain Models.
 * Sep.02.60:
 * - Issue #180: Proto-Mirror Parity Verification. Completed mapping for all 
 *   TrackerStatus fields including RT, forensic indices, and behavior flags (R180).
 * Aug.14.04:
 * - Issue #172: Viewer-Side State Audit. Restored SIT forensic fields (lastSitTs, 
 *   sitVz, sitDz, sitBaro, sitTilt, sitShock, verticalVelocity) to TrackerStatus 
 *   mapping to ensure mirror parity during viewer restarts (R172).
 */
object SettingsMapper {

    fun protoToAlertSettings(s: AlertSettingsProto): AlertSettings {
        return AlertSettings(
            localInternet = s.localInternet,
            serverConnection = s.serverConnection,
            relayConnection = s.relayConnection,
            jammerDetection = s.jammerDetection,
            signalLoss = s.signalLoss,
            gpsStalling = s.gpsStalling,
            distance = s.distance,
            power = s.power,
            lowBattery = s.lowBattery,
            batteryHealth = s.batteryHealth,
            longTimeGap = s.longTimeGap,
            highTemperature = s.highTemperature,
            overrideSilence = s.overrideSilence,
            useMaxVolume = s.useMaxVolume,
            vibrationEnabled = s.vibrationEnabled,
            alarmVolume = s.alarmVolume,
            useCustomVolume = s.useCustomVolume,
            tiltAlert = s.tiltAlert,
            acousticAlert = s.acousticAlert,
            liftAlert = s.liftAlert,
            tamperAlert = s.tamperAlert,
            globalMute = s.globalMute,
            systemStorageLow = s.systemStorageLow
        )
    }

    fun alertSettingsToProto(s: AlertSettings): AlertSettingsProto {
        return AlertSettingsProto.newBuilder()
            .setLocalInternet(s.localInternet)
            .setServerConnection(s.serverConnection)
            .setRelayConnection(s.relayConnection)
            .setJammerDetection(s.jammerDetection)
            .setSignalLoss(s.signalLoss)
            .setGpsStalling(s.gpsStalling)
            .setDistance(s.distance)
            .setPower(s.power)
            .setLowBattery(s.lowBattery)
            .setBatteryHealth(s.batteryHealth)
            .setLongTimeGap(s.longTimeGap)
            .setHighTemperature(s.highTemperature)
            .setOverrideSilence(s.overrideSilence)
            .setUseMaxVolume(s.useMaxVolume)
            .setVibrationEnabled(s.vibrationEnabled)
            .setAlarmVolume(s.alarmVolume)
            .setUseCustomVolume(s.useCustomVolume)
            .setTiltAlert(s.tiltAlert)
            .setAcousticAlert(s.acousticAlert)
            .setLiftAlert(s.liftAlert)
            .setTamperAlert(s.tamperAlert)
            .setGlobalMute(s.globalMute)
            .setSystemStorageLow(s.systemStorageLow)
            .build()
    }

    fun mapTrackerStatusFromProto(s: TrackerStatusProto): TrackerStatus {
        return TrackerStatus(
            lat = s.lat, lng = s.lng, alt = s.alt,
            speed = s.speed, bearing = s.bearing, accuracy = s.accuracy, maxAccuracy = s.maxAccuracy,
            gpsTs = s.gpsTs, ts = s.ts, rt = s.rt, battery = s.battery, temp = s.temp, maxTemp = s.maxTemp, isCharging = s.isCharging,
            satsView = s.satsView, satsUsed = s.satsUsed,
            lastConnTs = s.lastConnTs, lastDiscTs = s.lastDiscTs,
            uptimeMs = s.uptimeMs,
            totalConnectedMs = s.totalConnectedMs, sessionConnectedMs = s.sessionConnectedMs,
            totalDropMs = s.totalDropMs, maxDropMs = s.maxDropMs,
            maxDropTs = s.maxDropTs, violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
            isPowerTamper = s.isPowerTamper,
            vibration = s.vibration,
            heading = s.heading,
            baroAlt = s.baroAlt,
            lux = s.lux,
            isNear = s.isNear,
            tiltDegrees = s.tiltDegrees,
            acousticDb = s.acousticDb,
            peakVibrationShock = s.peakShock,
            peakVibrationShockTs = s.peakShockTs,
            luxBaseline = s.luxBaseline,
            acousticFloorDb = s.acousticFloor,
            adaptiveVibrationFloor = s.adaptiveVibrationFloor,
            proxIdx = s.proxIdx,
            isTamperDetected = s.isTamperDetected,
            isPowerSaveMode = s.isPowerSaveMode,
            standbyBucket = s.standbyBucket,
            netInterface = s.netInterface,
            isStorageLow = s.isStorageLow,
            isStorageCritical = s.isStorageCritical,
            isBatterySteepDischarge = s.isBatterySteepDischarge,
            isCoolingModeActive = s.isCoolingModeActive,
            currentMa = s.currentMa,
            trackerState = try { if (s.trackerState.isNullOrBlank()) TrackerState.UNKNOWN else TrackerState.valueOf(s.trackerState) } catch (e: Exception) { TrackerState.UNKNOWN },
            status = try { if (s.status.isNullOrBlank()) SentinelStatus.VALID else SentinelStatus.valueOf(s.status) } catch (e: Exception) { SentinelStatus.VALID },
            
            // Issue #172/180: Forensic Parity
            lastSitTs = s.lastSitTs,
            sitVz = s.sitVz,
            sitDz = s.sitDz,
            sitBaro = s.sitBaro,
            sitTilt = s.sitTilt,
            sitShock = s.sitShock,
            verticalVelocity = s.verticalVelocity,
            kineticEnergy = s.kineticEnergy,
            isAdaptiveJump = s.isAdaptiveJump,
            isBatteryLow = s.isBatteryLow,
            isBatteryCritical = s.isBatteryCritical,
            isSilentFailure = s.isSilentFailure,
            
            // Issue #180: Completion
            isJammer = s.isJammer,
            isStalled = s.isStalled,
            isClockRegression = s.isClockRegression,
            jumpTier = s.jumpTier,
            isLocationPending = s.isLocationPending,
            locationPendingReason = try { LocationPendingReason.valueOf(s.locationPendingReason.name.removePrefix("LPR_")) } catch (e: Exception) { LocationPendingReason.NONE },
            lastValidFixRt = s.lastValidFixRt,
            snrIdx = s.snrIdx,
            noiseIdx = s.noiseIdx,
            luxIdx = s.luxIdx,
            vibeIdx = s.vibeIdx,
            liftIdx = s.liftIdx,
            tiltIdx = s.tiltIdx,
            baroIdx = s.baroIdx,
            isSitDetected = s.isSitDetected,
            isSitActive = s.isSitActive,
            isUltraLongStationary = s.isUltraLongStationary,
            isJump = s.isJump,
            micPending = s.micPending
        )
    }

    fun mapTrackerStatusToProto(status: TrackerStatus): TrackerStatusProto {
        return TrackerStatusProto.newBuilder()
            .setLat(status.lat)
            .setLng(status.lng)
            .setAlt(status.alt)
            .setSpeed(status.speed)
            .setBearing(status.bearing)
            .setAccuracy(status.accuracy)
            .setMaxAccuracy(status.maxAccuracy)
            .setGpsTs(status.gpsTs)
            .setTs(status.ts)
            .setRt(status.rt)
            .setBattery(status.battery)
            .setTemp(status.temp)
            .setMaxTemp(status.maxTemp)
            .setIsCharging(status.isCharging)
            .setSatsView(status.satsView)
            .setSatsUsed(status.satsUsed)
            .setLastConnTs(status.lastConnTs)
            .setLastDiscTs(status.lastDiscTs)
            .setUptimeMs(status.uptimeMs)
            .setTotalConnectedMs(status.totalConnectedMs)
            .setSessionConnectedMs(status.sessionConnectedMs)
            .setTotalDropMs(status.totalDropMs)
            .setMaxDropMs(status.maxDropMs)
            .setMaxDropTs(status.maxDropTs)
            .setViolationUptimeMs(status.violationUptimeMs)
            .setViolationPercentage(status.violationPercentage)
            .setIsPowerTamper(status.isPowerTamper)
            .setVibration(status.vibration)
            .setHeading(status.heading)
            .setBaroAlt(status.baroAlt)
            .setLux(status.lux)
            .setIsNear(status.isNear)
            .setTiltDegrees(status.tiltDegrees)
            .setAcousticDb(status.acousticDb)
            .setPeakShock(status.peakVibrationShock)
            .setPeakShockTs(status.peakVibrationShockTs)
            .setLuxBaseline(status.luxBaseline)
            .setAcousticFloor(status.acousticFloorDb)
            .setAdaptiveVibrationFloor(status.adaptiveVibrationFloor)
            .setProxIdx(status.proxIdx)
            .setIsTamperDetected(status.isTamperDetected)
            .setIsPowerSaveMode(status.isPowerSaveMode)
            .setStandbyBucket(status.standbyBucket)
            .setNetInterface(status.netInterface)
            .setIsStorageLow(status.isStorageLow)
            .setIsBatterySteepDischarge(status.isBatterySteepDischarge)
            .setIsCoolingModeActive(status.isCoolingModeActive)
            .setIsStorageCritical(status.isStorageCritical)
            .setCurrentMa(status.currentMa)
            .setTrackerState(status.trackerState.name)
            .setStatus(status.status.name)
            
            // Issue #172/180: Forensic Parity
            .setLastSitTs(status.lastSitTs)
            .setSitVz(status.sitVz)
            .setSitDz(status.sitDz)
            .setSitBaro(status.sitBaro)
            .setSitTilt(status.sitTilt)
            .setSitShock(status.sitShock)
            .setVerticalVelocity(status.verticalVelocity)
            .setKineticEnergy(status.kineticEnergy)
            .setIsAdaptiveJump(status.isAdaptiveJump)
            .setIsBatteryLow(status.isBatteryLow)
            .setIsBatteryCritical(status.isBatteryCritical)
            .setIsSilentFailure(status.isSilentFailure)
            
            // Issue #180: Completion
            .setIsJammer(status.isJammer)
            .setIsStalled(status.isStalled)
            .setIsClockRegression(status.isClockRegression)
            .setJumpTier(status.jumpTier)
            .setIsLocationPending(status.isLocationPending)
            .setLocationPendingReason(try { LocationPendingReasonProto.valueOf("LPR_" + status.locationPendingReason.name) } catch (e: Exception) { LocationPendingReasonProto.LPR_NONE })
            .setLastValidFixRt(status.lastValidFixRt)
            .setSnrIdx(status.snrIdx)
            .setNoiseIdx(status.noiseIdx)
            .setLuxIdx(status.luxIdx)
            .setVibeIdx(status.vibeIdx)
            .setLiftIdx(status.liftIdx)
            .setTiltIdx(status.tiltIdx)
            .setBaroIdx(status.baroIdx)
            .setIsSitDetected(status.isSitDetected)
            .setIsSitActive(status.isSitActive)
            .setIsUltraLongStationary(status.isUltraLongStationary)
            .setIsJump(status.isJump)
            .setMicPending(status.micPending)
            .build()
    }
}
