package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * July.22.01:
 * - Forensic Parity: Added mapping for tiltIdx, baroIdx, noiseIdx, luxIdx, vibeIdx, and liftIdx.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor.
 * July.21.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized monotonic timestamps to 'Rt'.
 * - Issue #516: De-duplicate "Status" Logic. Using SystemHealthState for all health/sensor metadata.
 */
class TelemetryUseCase @Inject constructor(
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
            lastValidFixRt = if (update.lastValidFixRt > 0L) update.lastValidFixRt else current.lastValidFixRt,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            isBatterySteepDischarge = update.isBatterySteepDischarge,
            isCoolingModeActive = update.isCoolingModeActive,
            gnssDetail = update.gnssDetail ?: current.gnssDetail,
            snrIdx = update.snrIdx,
            noiseIdx = update.noiseIdx,
            luxIdx = update.luxIdx,
            vibeIdx = update.vibeIdx,
            liftIdx = update.liftIdx,
            tiltIdx = update.tiltIdx,
            baroIdx = update.baroIdx,
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
            vibrationRollingSum = update.vibrationRollingSum ?: current.vibrationRollingSum,
            isSitDetected = update.isSitDetected,
            isSitActive = update.isSitActive,
            lastSitTs = update.lastSitTs,
            verticalVelocity = update.verticalVelocity ?: current.verticalVelocity,
            sitVz = update.sitVz ?: current.sitVz,
            sitDz = update.sitDz ?: current.sitDz,
            sitBaro = update.sitBaro ?: current.sitBaro,
            sitTilt = update.sitTilt ?: current.sitTilt,
            sitShock = update.sitShock ?: current.sitShock
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
            lastValidFixRt = status.lastValidFixRt,
            isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket,
            netInterface = status.netInterface,
            isStorageLow = status.isStorageLow,
            isStorageCritical = status.isStorageCritical,
            isBatterySteepDischarge = status.isBatterySteepDischarge,
            isCoolingModeActive = status.isCoolingModeActive,
            gnssDetail = status.gnssDetail,
            snrIdx = status.snrIdx,
            noiseIdx = status.noiseIdx,
            luxIdx = status.luxIdx,
            vibeIdx = status.vibeIdx,
            liftIdx = status.liftIdx,
            tiltIdx = status.tiltIdx,
            baroIdx = status.baroIdx,
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
            vibrationRollingSum = status.vibrationRollingSum,
            isSitDetected = status.isSitDetected,
            isSitActive = status.isSitActive,
            lastSitTs = status.lastSitTs,
            verticalVelocity = status.verticalVelocity,
            sitVz = status.sitVz,
            sitDz = status.sitDz,
            sitBaro = status.sitBaro,
            sitTilt = status.sitTilt,
            sitShock = status.sitShock
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
            gnssDetail = update.gnssDetail ?: currentLoc.gnssDetail
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
}
