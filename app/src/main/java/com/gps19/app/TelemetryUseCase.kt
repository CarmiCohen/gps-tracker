package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * v9.3.8:
 * - Clock Skew Hardening: Forced remote updates to use local receipt time (nowMs) 
 *   for telemetryTs to ensure HUD freshness (Green vs Gray) is immune to device clock drift.
 * v9.1.8:
 * - Issue #046: Shared Behavioral State. Mapped trackerState from incoming telemetry.
 */
@Singleton
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
            isVisualJump = update.isJump,
            isTrajectoryPromoted = update.isTrajectoryPromoted,
            isJammer = update.isJammer, isStalled = update.isStalled,
            vibration = update.vibration ?: currentLoc.vibration,
            heading = update.heading ?: currentLoc.heading,
            tiltDegrees = update.tiltDegrees ?: currentLoc.tiltDegrees,
            acousticDb = update.acousticDb ?: currentLoc.acousticDb,
            baroAlt = update.baroAlt ?: currentLoc.baroAlt,
            lux = update.lux ?: currentLoc.lux,
            isNear = update.isNear ?: currentLoc.isNear,
            isSuspicious = update.isSuspicious,
            isTamperDetected = update.isTamperDetected,
            isPowerTamper = update.isPowerTamper,
            isSitDetected = update.isSitDetected,
            lastSitTs = update.lastSitTs,
            verticalVelocity = update.verticalVelocity ?: currentLoc.verticalVelocity,
            sitVz = update.sitVz ?: currentLoc.sitVz,
            sitDz = update.sitDz ?: currentLoc.sitDz,
            sitBaro = update.sitBaro ?: currentLoc.sitBaro,
            sitTilt = update.sitTilt ?: currentLoc.sitTilt,
            sitShock = update.sitShock ?: currentLoc.sitShock,
            micPending = update.micPending,
            peakVibrationShock = update.peakVibrationShock ?: currentLoc.peakVibrationShock,
            peakVibrationShockTs = update.peakVibrationShockTs ?: currentLoc.peakVibrationShockTs,
            luxBaseline = update.luxBaseline ?: currentLoc.luxBaseline,
            acousticFloorDb = update.acousticFloorDb ?: currentLoc.acousticFloorDb,
            adaptiveVibrationFloor = update.adaptiveVibrationFloor ?: currentLoc.adaptiveVibrationFloor,
            proxIdx = update.proxIdx ?: currentLoc.proxIdx,
            proximityCm = update.proximityCm ?: currentLoc.proximityCm,
            proximityDebounceMs = update.proximityDebounceMs ?: currentLoc.proximityDebounceMs,
            vibrationRollingSum = update.vibrationRollingSum ?: currentLoc.vibrationRollingSum,
            violationUptimeMs = update.violationUptimeMs ?: currentLoc.violationUptimeMs,
            violationPercentage = update.violationPercentage ?: calculateViolationPercentage(update.violationUptimeMs, nowMs - appStartTime),
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            lastValidFixRealtime = if (update.lastValidFixRealtime > 0L) update.lastValidFixRealtime else currentLoc.lastValidFixRealtime,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            gnssDetail = update.gnssDetail ?: currentLoc.gnssDetail,
            snrIdx = update.snrIdx,
            isCoolingModeActive = update.isCoolingModeActive,
            currentMa = update.currentMa,
            isAnchorLocked = update.isAnchorLocked,
            trackerState = update.trackerState
        )
    }

    fun mapTrackerLocationFromStatus(status: TrackerStatus, currentLoc: LocationState): LocationState {
        return currentLoc.copy(
            lat = status.lat, lng = status.lng, speed = status.speed, bearing = status.bearing, accuracy = status.accuracy, 
            maxAccuracy = status.maxAccuracy,
            timestamp = status.gpsTs, 
            telemetryTs = status.ts, 
            isVisualJump = status.isJump, isTrajectoryPromoted = status.isTrajectoryPromoted,
            vibration = status.vibration, heading = status.heading, tiltDegrees = status.tiltDegrees,
            acousticDb = status.acousticDb, baroAlt = status.baroAlt, lux = status.lux, isNear = status.isNear,
            isSuspicious = status.isSuspicious, isTamperDetected = status.isTamperDetected, isPowerTamper = status.isPowerTamper,
            isSitDetected = status.isSitDetected, isSitActive = status.isSitActive, lastSitTs = status.lastSitTs, verticalVelocity = status.verticalVelocity,
            sitVz = status.sitVz, sitDz = status.sitDz, sitBaro = status.sitBaro, sitTilt = status.sitTilt, sitShock = status.sitShock,
            peakVibrationShock = status.peakVibrationShock, peakVibrationShockTs = status.peakVibrationShockTs,
            luxBaseline = status.luxBaseline, acousticFloorDb = status.acousticFloorDb, adaptiveVibrationFloor = status.adaptiveVibrationFloor,
            proxIdx = status.proxIdx, proximityCm = status.proximityCm, 
            proximityDebounceMs = status.proximityDebounceMs, vibrationRollingSum = status.vibrationRollingSum,
            violationUptimeMs = status.violationUptimeMs, violationPercentage = status.violationPercentage,
            isLocationPending = status.isLocationPending, 
            locationPendingReason = status.locationPendingReason,
            lastValidFixRealtime = status.lastValidFixRealtime,
            isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket, netInterface = status.netInterface,
            isStorageLow = status.isStorageLow, isStorageCritical = status.isStorageCritical,
            gnssDetail = status.gnssDetail, snrIdx = status.snrIdx,
            isCoolingModeActive = status.isCoolingModeActive,
            currentMa = status.currentMa,
            isAnchorLocked = status.isAnchorLocked,
            trackerState = status.trackerState
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
            isVisualJump = update.isJump,
            isTrajectoryPromoted = update.isTrajectoryPromoted,
            isJammer = update.isJammer, isStalled = update.isStalled,
            vibration = update.vibration ?: currentLoc.vibration,
            heading = update.heading ?: currentLoc.heading,
            tiltDegrees = update.tiltDegrees ?: currentLoc.tiltDegrees,
            acousticDb = update.acousticDb ?: currentLoc.acousticDb,
            baroAlt = update.baroAlt ?: currentLoc.baroAlt,
            lux = update.lux ?: currentLoc.lux,
            isNear = update.isNear ?: currentLoc.isNear,
            isSuspicious = update.isSuspicious,
            isTamperDetected = update.isTamperDetected,
            isPowerTamper = update.isPowerTamper,
            isSitDetected = update.isSitDetected,
            lastSitTs = update.lastSitTs,
            verticalVelocity = update.verticalVelocity ?: currentLoc.verticalVelocity,
            sitVz = update.sitVz ?: currentLoc.sitVz,
            sitDz = update.sitDz ?: currentLoc.sitDz,
            sitBaro = update.sitBaro ?: currentLoc.sitBaro,
            sitTilt = update.sitTilt ?: currentLoc.sitTilt,
            sitShock = update.sitShock ?: currentLoc.sitShock,
            micPending = update.micPending,
            peakVibrationShock = update.peakVibrationShock ?: currentLoc.peakVibrationShock,
            peakVibrationShockTs = update.peakVibrationShockTs ?: currentLoc.peakVibrationShockTs,
            luxBaseline = update.luxBaseline ?: currentLoc.luxBaseline,
            acousticFloorDb = update.acousticFloorDb ?: currentLoc.acousticFloorDb,
            adaptiveVibrationFloor = update.adaptiveVibrationFloor ?: SentinelValidator.updateVibrationFloor(currentLoc.adaptiveVibrationFloor, update.vibration ?: 0.0, false),
            proxIdx = update.proxIdx ?: currentLoc.proxIdx,
            proximityCm = update.proximityCm ?: currentLoc.proximityCm,
            proximityDebounceMs = update.proximityDebounceMs ?: currentLoc.proximityDebounceMs,
            vibrationRollingSum = update.vibrationRollingSum ?: currentLoc.vibrationRollingSum,
            violationUptimeMs = update.violationUptimeMs ?: currentLoc.violationUptimeMs,
            violationPercentage = update.violationPercentage ?: calculateViolationPercentage(update.violationUptimeMs, nowMs - appStartTime),
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            lastValidFixRealtime = if (update.lastValidFixRealtime > 0L) update.lastValidFixRealtime else currentLoc.lastValidFixRealtime,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket, netInterface = update.netInterface,
            isStorageLow = update.isStorageLow, isStorageCritical = update.isStorageCritical,
            gnssDetail = update.gnssDetail ?: currentLoc.gnssDetail,
            snrIdx = update.snrIdx,
            isCoolingModeActive = update.isCoolingModeActive,
            currentMa = update.currentMa,
            isAnchorLocked = update.isAnchorLocked,
            trackerState = update.trackerState
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
            lastConnTs = status.lastDiscTs, 
            lastDiscTs = status.lastDiscTs, 
            totalConnectedMs = status.totalConnectedMs, 
            sessionConnectedMs = status.sessionConnectedMs, 
            maxDropMs = status.maxDropMs, 
            maxDropTs = status.maxDropTs, 
            totalDropMs = status.totalDropMs, 
            uptimeMs = status.uptimeMs
        )
    }

    fun calculateViolationPercentage(violationMs: Long?, totalMs: Long): Double {
        if (violationMs == null || totalMs <= 0) return 0.0
        return (violationMs.toDouble() / totalMs.toDouble()).coerceIn(0.0, 1.0)
    }
}
