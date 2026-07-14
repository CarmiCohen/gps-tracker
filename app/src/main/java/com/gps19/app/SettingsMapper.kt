package com.gps19.app

import com.gps19.core.engine.*

/**
 * SettingsMapper: Conversion logic between DataStore Protos and Domain Models.
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
            chairOccupied = s.chairOccupied,
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
            .setChairOccupied(s.chairOccupied)
            .setGlobalMute(s.globalMute)
            .setSystemStorageLow(s.systemStorageLow)
            .build()
    }

    fun mapTrackerStatusFromProto(s: TrackerStatusProto): TrackerStatus {
        return TrackerStatus(
            lat = s.lat, lng = s.lng, alt = s.alt,
            speed = s.speed, bearing = s.bearing, accuracy = s.accuracy, maxAccuracy = s.maxAccuracy,
            gpsTs = s.gpsTs, ts = s.ts, battery = s.battery, temp = s.temp, maxTemp = s.maxTemp, isCharging = s.isCharging,
            satsView = s.satsView, satsUsed = s.satsUsed,
            lastConnTs = s.lastConnTs, lastDiscTs = s.lastDiscTs,
            uptimeMs = s.uptimeMs,
            totalConnectedMs = s.totalConnectedMs, sessionConnectedMs = s.sessionConnectedMs,
            totalDropMs = s.totalDropMs, maxDropMs = s.maxDropMs,
            maxDropTs = s.maxDropTs, violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
            isSitDetected = s.isSitDetected, lastSitTs = s.lastSitTs, verticalVelocity = s.verticalVelocity,
            sitVz = s.sitVz, sitDz = s.sitDz, sitBaro = s.sitBaro, sitTilt = s.sitTilt, sitShock = s.sitShock,
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
            isSuspicious = s.isSuspicious,
            isTamperDetected = s.isTamperDetected,
            isPowerSaveMode = s.isPowerSaveMode,
            standbyBucket = s.standbyBucket,
            netInterface = s.netInterface,
            isStorageLow = s.isStorageLow,
            isStorageCritical = s.isStorageCritical,
            isBatterySteepDischarge = s.isBatterySteepDischarge,
            isJammer = s.isJammer,
            isCoolingModeActive = s.isCoolingModeActive,
            currentMa = s.currentMa,
            isAnchorLocked = s.isAnchorLocked,
            trackerState = try { TrackerState.valueOf(s.trackerState) } catch (e: Exception) { TrackerState.UNKNOWN }
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
            .setIsSitDetected(status.isSitDetected)
            .setLastSitTs(status.lastSitTs)
            .setVerticalVelocity(status.verticalVelocity)
            .setSitVz(status.sitVz)
            .setSitDz(status.sitDz)
            .setSitBaro(status.sitBaro)
            .setSitTilt(status.sitTilt)
            .setSitShock(status.sitShock)
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
            .setIsSuspicious(status.isSuspicious)
            .setIsTamperDetected(status.isTamperDetected)
            .setIsPowerSaveMode(status.isPowerSaveMode)
            .setStandbyBucket(status.standbyBucket)
            .setNetInterface(status.netInterface)
            .setIsStorageLow(status.isStorageLow)
            .setIsBatterySteepDischarge(status.isBatterySteepDischarge)
            .setIsJammer(status.isJammer)
            .setIsCoolingModeActive(status.isCoolingModeActive)
            .setIsStorageCritical(status.isStorageCritical)
            .setCurrentMa(status.currentMa)
            .setIsAnchorLocked(status.isAnchorLocked)
            .setTrackerState(status.trackerState.name)
            .build()
    }
}
