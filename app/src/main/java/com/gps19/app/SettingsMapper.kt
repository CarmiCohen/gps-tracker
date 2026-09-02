package com.gps19.app

import com.gps19.core.engine.*

/**
 * SettingsMapper: Conversion logic between DataStore Protos and Domain Models.
 * Sep.02.70:
 * - Idea #241: Protobuf Mapping Unification. Integrated TelemetryProtobufMapper 
 *   to handle TrackerStatusProto serialization, ensuring field parity (R-ID 245).
 * - Issue #180: Proto-Mirror Parity Verification. Completed mapping for all 
 *   TrackerStatus fields including RT, forensic indices, and behavior flags.
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
            isBatteryWhitelisted = s.isBatteryWhitelisted,
            isBatterySteepDischarge = s.isBatterySteepDischarge,
            isCoolingModeActive = s.isCoolingModeActive,
            currentMa = s.currentMa,
            trackerState = try { if (s.trackerState.isNullOrBlank()) TrackerState.UNKNOWN else TrackerState.valueOf(s.trackerState) } catch (e: Exception) { TrackerState.UNKNOWN },
            status = try { if (s.status.isNullOrBlank()) SentinelStatus.VALID else SentinelStatus.valueOf(s.status) } catch (e: Exception) { SentinelStatus.VALID },
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
            micPending = s.micPending,
            deviceId = s.deviceId,
            viewerId = s.viewerId,
            currentProximityCm = s.proximityCm,
            proximityDebounceMs = s.proximityDebounceMs,
            vibrationRollingSum = s.vibrationRollingSum,
            isTrajectoryPromoted = s.isTrajectoryPromoted,
            isSuspicious = s.isSuspicious,
            isAnchorLocked = s.isAnchorLocked
        )
    }

    fun mapTrackerStatusToProto(status: TrackerStatus): TrackerStatusProto {
        val builder = TrackerStatusProto.newBuilder()
        TelemetryProtobufMapper.mapToPersistence(status, builder)
        return builder.build()
    }
}
