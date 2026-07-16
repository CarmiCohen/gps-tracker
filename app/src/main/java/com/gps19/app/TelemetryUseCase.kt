package com.gps19.app

import com.gps19.core.engine.*

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Added mapping for SystemHealthState.
 */
class TelemetryUseCase(
    private val timeProvider: TimeProvider
) {
    fun mapTrackerLocation(
        update: LocationUpdate, 
        currentLoc: LocationState, 
        nowMs: Long, 
        appStartTime: Long
    ): LocationState {
        val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
        val newTimestamp = update.gpsTs
        
        // For remote data, telemetryTs MUST be the local receipt time (nowMs)
        // to prevent clock skew from turning the HUD gray.
        val effectiveTelemetryTs = if (!update.isMe) nowMs else (if (update.ts > 0) update.ts else nowMs)
        
        return currentLoc.copy(
            lat = if (isLocationValid) update.lat else currentLoc.lat, 
            lng = if (isLocationValid) update.lng else currentLoc.lng, 
            speed = if (isLocationValid) update.speed else currentLoc.speed, 
            accuracy = if (isLocationValid) update.accuracy else currentLoc.accuracy, 
            maxAccuracy = if (update.maxAccuracy > 0.0) update.maxAccuracy else currentLoc.maxAccuracy,
            bearing = if (isLocationValid) update.bearing else currentLoc.bearing, 
            timestamp = if (newTimestamp > 0) newTimestamp else currentLoc.timestamp,
            telemetryTs = effectiveTelemetryTs,
            status = update.status,
            trackerState = update.trackerState,
            gnssDetail = update.gnssDetail
        )
    }

    fun mapHealthFromUpdate(update: LocationUpdate, current: SystemHealthState): SystemHealthState {
        return current.copy(
            signalLoss = update.signal?.let { it < 2 } ?: current.signalLoss,
            gpsStalled = update.locationPendingReason == LocationPendingReason.GPS_STALL,
            isHardwareOnline = update.signal != null,
            batteryLevel = if (update.battery >= 0) update.battery else current.batteryLevel,
            batteryTemp = update.temp,
            maxTemp = update.maxTemp,
            isCharging = update.isCharging,
            currentMa = update.currentMa,
            status = update.status,
            trackerState = update.trackerState,
            isJammer = update.locationPendingReason == LocationPendingReason.JAMMER_SUSPICION,
            isTamperDetected = update.isTamperDetected,
            micPending = update.micPending,
            isPowerTamper = update.isPowerTamper,
            isClockRegression = update.isClockRegression,
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            lastValidFixRealtime = update.lastValidFixRealtime,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            isBatterySteepDischarge = update.isBatterySteepDischarge,
            isCoolingModeActive = update.isCoolingModeActive,
            gnssDetail = update.gnssDetail,
            uptimeMs = update.uptimeMs ?: current.uptimeMs,
            lastConnTs = update.lastConnTs ?: current.lastConnTs,
            lastDiscTs = update.lastDiscTs ?: current.lastDiscTs,
            totalDropMs = update.totalDropMs ?: current.totalDropMs,
            maxDropMs = update.maxDropMs ?: current.maxDropMs,
            maxDropTs = update.maxDropTs ?: current.maxDropTs,
            totalConnectedMs = update.totalConnectedMs ?: current.totalConnectedMs,
            sessionConnectedMs = update.sessionConnectedMs ?: current.sessionConnectedMs,
            violationUptimeMs = update.violationUptimeMs ?: current.violationUptimeMs,
            violationPercentage = update.violationPercentage ?: current.violationPercentage,
            vibration = update.vibration ?: current.vibration,
            heading = update.heading ?: current.heading,
            tiltDegrees = update.tiltDegrees ?: current.tiltDegrees,
            acousticDb = update.acousticDb ?: current.acousticDb,
            baroAlt = update.baroAlt ?: current.baroAlt,
            lux = update.lux ?: current.lux,
            isNear = update.isNear ?: current.isNear,
            peakVibrationShock = update.peakVibrationShock ?: current.peakVibrationShock,
            peakVibrationShockTs = update.peakVibrationShockTs ?: current.peakVibrationShockTs,
            luxBaseline = update.luxBaseline ?: current.luxBaseline,
            acousticFloorDb = update.acousticFloorDb ?: current.acousticFloorDb,
            adaptiveVibrationFloor = update.adaptiveVibrationFloor ?: current.adaptiveVibrationFloor,
            proxIdx = update.proxIdx ?: current.proxIdx,
            proximityCm = update.proximityCm ?: current.proximityCm,
            proximityDebounceMs = update.proximityDebounceMs ?: current.proximityDebounceMs,
            vibrationRollingSum = update.vibrationRollingSum ?: current.vibrationRollingSum
        )
    }

    fun mapHealthFromStatus(status: TrackerStatus, current: SystemHealthState): SystemHealthState {
        return current.copy(
            batteryLevel = status.battery,
            batteryTemp = status.temp,
            maxTemp = status.maxTemp,
            isCharging = status.isCharging,
            currentMa = status.currentMa,
            status = status.status,
            trackerState = status.trackerState,
            isJammer = status.isJammer,
            isTamperDetected = status.isTamperDetected,
            isPowerTamper = status.isPowerTamper,
            isLocationPending = status.isLocationPending,
            locationPendingReason = status.locationPendingReason,
            lastValidFixRealtime = status.lastValidFixRealtime,
            isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket,
            netInterface = status.netInterface,
            isStorageLow = status.isStorageLow,
            isStorageCritical = status.isStorageCritical,
            isBatterySteepDischarge = status.isBatterySteepDischarge,
            isCoolingModeActive = status.isCoolingModeActive,
            gnssDetail = status.gnssDetail,
            uptimeMs = status.uptimeMs,
            lastConnTs = status.lastConnTs,
            lastDiscTs = status.lastDiscTs,
            totalDropMs = status.totalDropMs,
            maxDropMs = status.maxDropMs,
            maxDropTs = status.maxDropTs,
            totalConnectedMs = status.totalConnectedMs,
            sessionConnectedMs = status.sessionConnectedMs,
            violationUptimeMs = status.violationUptimeMs,
            violationPercentage = status.violationPercentage,
            vibration = status.vibration,
            heading = status.heading,
            tiltDegrees = status.tiltDegrees,
            acousticDb = status.acousticDb,
            baroAlt = status.baroAlt,
            lux = status.lux,
            isNear = status.isNear,
            peakVibrationShock = status.peakVibrationShock,
            peakVibrationShockTs = status.peakVibrationShockTs,
            luxBaseline = status.luxBaseline,
            acousticFloorDb = status.acousticFloorDb,
            adaptiveVibrationFloor = status.adaptiveVibrationFloor,
            proxIdx = status.proxIdx,
            proximityCm = status.proximityCm,
            proximityDebounceMs = status.proximityDebounceMs,
            vibrationRollingSum = status.vibrationRollingSum
        )
    }

    fun mapTrackerLocationFromStatus(status: TrackerStatus, currentLoc: LocationState): LocationState {
        return currentLoc.copy(
            lat = status.lat, lng = status.lng, speed = status.speed, bearing = status.bearing, accuracy = status.accuracy, 
            maxAccuracy = status.maxAccuracy,
            timestamp = status.gpsTs, 
            telemetryTs = status.ts, 
            status = status.status,
            trackerState = status.trackerState,
            gnssDetail = status.gnssDetail
        )
    }

    fun mapLocalLocation(
        update: LocationUpdate, 
        currentLoc: LocationState, 
        nowMs: Long, 
        appStartTime: Long
    ): LocationState {
        val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
        val newTimestamp = update.gpsTs
        
        return currentLoc.copy(
            lat = if (isLocationValid) update.lat else currentLoc.lat, 
            lng = if (isLocationValid) update.lng else currentLoc.lng, 
            speed = if (isLocationValid) update.speed else currentLoc.speed, 
            accuracy = if (isLocationValid) update.accuracy else currentLoc.accuracy, 
            maxAccuracy = if (update.maxAccuracy > 0.0) update.maxAccuracy else currentLoc.maxAccuracy,
            bearing = if (isLocationValid) update.bearing else currentLoc.bearing, 
            timestamp = if (newTimestamp > 0) newTimestamp else currentLoc.timestamp,
            telemetryTs = if (update.ts > 0) update.ts else nowMs,
            status = update.status,
            trackerState = update.trackerState,
            gnssDetail = update.gnssDetail
        )
    }

    fun mapStats(update: LocationUpdate, currentStats: StatsState): StatsState {
        return currentStats.copy(
            totalConnectedMs = update.totalConnectedMs ?: currentStats.totalConnectedMs, 
            sessionConnectedMs = update.sessionConnectedMs ?: currentStats.sessionConnectedMs, 
            lastConnTs = update.lastConnTs ?: currentStats.lastConnTs, 
            lastDiscTs = update.lastDiscTs ?: currentStats.lastDiscTs, 
            uptimeMs = update.uptimeMs ?: currentStats.uptimeMs, 
            totalDropMs = update.totalDropMs ?: currentStats.totalDropMs, 
            maxDropMs = update.maxDropMs ?: currentStats.maxDropMs,
            maxDropTs = update.maxDropTs ?: currentStats.maxDropTs
        )
    }

    fun mapStatsFromStatus(status: TrackerStatus, currentStats: StatsState): StatsState {
        return currentStats.copy(
            totalConnectedMs = status.totalConnectedMs,
            sessionConnectedMs = status.sessionConnectedMs,
            lastConnTs = status.lastConnTs,
            lastDiscTs = status.lastDiscTs,
            uptimeMs = status.uptimeMs,
            totalDropMs = status.totalDropMs,
            maxDropMs = status.maxDropMs,
            maxDropTs = status.maxDropTs,
            violationUptimeMs = status.violationUptimeMs,
            violationPercentage = status.violationPercentage
        )
    }

    fun calculateViolationPercentage(violationMs: Long?, totalMs: Long): Double {
        if (violationMs == null || totalMs <= 0) return 0.0
        return (violationMs.toDouble() / totalMs.toDouble()).coerceIn(0.0, 1.0)
    }
}
