package com.gps19.app

import com.gps19.core.engine.*

/**
 * SettingsMapper: Conversion logic between DataStore Protos and Domain Models.
 * Sep.26.0:
 * - Issue #1314: TrackerStatus & Evaluation Snapshot Convergence. Aligned Proto 
 *   mapping with partitioned TrackerStatus structure.
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
        val kinetic = KineticState(
            lat = s.lat, lng = s.lng, alt = s.alt,
            speed = s.speed, accuracy = s.accuracy, maxAccuracy = s.maxAccuracy,
            bearing = s.bearing, gpsTs = s.gpsTs, rt = s.rt,
            isJump = s.isJump, isTrajectoryPromoted = s.isTrajectoryPromoted,
            jumpTier = s.jumpTier, isAdaptiveJump = s.isAdaptiveJump,
            verticalVelocity = s.verticalVelocity, kineticEnergy = s.kineticEnergy
        )

        val atmospheric = AtmosphericState(
            temp = s.temp, maxTemp = s.maxTemp, baroAlt = s.baroAlt,
            lux = s.lux, luxBaseline = s.luxBaseline, 
            acousticDb = s.acousticDb, acousticFloorDb = s.acousticFloor,
            tiltDegrees = s.tiltDegrees, heading = s.heading,
            vibration = s.vibration, vibrationRollingSum = s.vibrationRollingSum,
            peakVibrationShock = s.peakShock, peakVibrationShockTs = s.peakShockTs,
            adaptiveVibrationFloor = s.adaptiveVibrationFloor, proxIdx = s.proxIdx,
            proximityCm = s.proximityCm, proximityDebounceMs = s.proximityDebounceMs,
            isNear = s.isNear, noiseIdx = s.noiseIdx, luxIdx = s.luxIdx,
            vibeIdx = s.vibeIdx, liftIdx = s.liftIdx, tiltIdx = s.tiltIdx,
            baroIdx = s.baroIdx
        )

        val integrity = IntegrityState(
            battery = s.battery, isCharging = s.isCharging, currentMa = s.currentMa,
            satsView = s.satsView, satsUsed = s.satsUsed, snrIdx = s.snrIdx,
            isTamperDetected = s.isTamperDetected, isPowerTamper = s.isPowerTamper,
            isJammer = s.isJammer, isStalled = s.isStalled, isSuspicious = s.isSuspicious,
            isAnchorLocked = s.isAnchorLocked, gpsHardwareLock = s.gpsHardwareLock,
            isBatteryLow = s.isBatteryLow, isBatteryCritical = s.isBatteryCritical,
            isCoolingModeActive = s.isCoolingModeActive, 
            isUltraLongStationary = s.isUltraLongStationary,
            isGnssThrottled = s.isGnssThrottled, 
            isBatterySteepDischarge = s.isBatterySteepDischarge,
            isPowerSaveMode = s.isPowerSaveMode, standbyBucket = s.standbyBucket,
            netInterface = s.netInterface, isStorageLow = s.isStorageLow,
            isStorageCritical = s.isStorageCritical, micPending = s.micPending,
            violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
            uptimeMs = s.uptimeMs, totalConnectedMs = s.totalConnectedMs,
            sessionConnectedMs = s.sessionConnectedMs, lastConnTs = s.lastConnTs,
            lastDiscTs = s.lastDiscTs, totalDropMs = s.totalDropMs,
            maxDropMs = s.maxDropMs, maxDropTs = s.maxDropTs,
            lastSitTs = s.lastSitTs, sitVz = s.sitVz, sitDz = s.sitDz,
            sitBaro = s.sitBaro, sitTilt = s.sitTilt, sitShock = s.sitShock,
            isSitDetected = s.isSitDetected, isSitActive = s.isSitActive,
            lastValidFixRt = s.lastValidFixRt
        )

        return TrackerStatus(
            deviceId = s.deviceId,
            viewerId = s.viewerId,
            kinetic = kinetic,
            atmospheric = atmospheric,
            integrity = integrity,
            status = try { if (s.status.isNullOrBlank()) SentinelStatus.VALID else SentinelStatus.valueOf(s.status) } catch (e: Exception) { SentinelStatus.VALID },
            ts = s.ts,
            rt = s.rt,
            trackerState = try { if (s.trackerState.isNullOrBlank()) TrackerState.UNKNOWN else TrackerState.valueOf(s.trackerState) } catch (e: Exception) { TrackerState.UNKNOWN },
            isClockRegression = s.isClockRegression,
            lastValidFixRt = s.lastValidFixRt,
            isSilentFailure = s.isSilentFailure,
            isBatteryWhitelisted = s.isBatteryWhitelisted
        )
    }

    fun mapTrackerStatusToProto(status: TrackerStatus): TrackerStatusProto {
        val builder = TrackerStatusProto.newBuilder()
        TelemetryProtobufMapper.mapToPersistence(status, builder)
        return builder.build()
    }
}
