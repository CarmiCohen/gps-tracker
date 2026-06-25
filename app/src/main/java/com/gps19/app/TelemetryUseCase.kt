package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * Extracted from MainViewModel to resolve Issue #322 (Architectural Bloat). (Formerly #385 / #115)
 * v8.9.37:
 * - Issue #325: Mapping maxAccuracy for unified accuracy fallback logic. (Formerly #484 / #214)
 * - Issue #326: Mapping locationPendingReason for intelligent uncertainty UX. (Formerly #496 / #226)
 * - Issue #491: Mapping lastValidFixRealtime for Bayesian uncertainty scaling. (Formerly #221)
 * - Issue #462: Added currentMa mapping for full forensic parity. (Formerly #192)
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
        
        return currentLoc.copy(
            lat = if (isLocationValid) update.lat else currentLoc.lat, 
            lng = if (isLocationValid) update.lng else currentLoc.lng, 
            speed = if (isLocationValid) update.speed else currentLoc.speed, 
            accuracy = if (isLocationValid) update.accuracy else currentLoc.accuracy, 
            maxAccuracy = if (update.maxAccuracy > 0) update.maxAccuracy else currentLoc.maxAccuracy,
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
            lastSitTs = update.lastSitTs ?: currentLoc.lastSitTs,
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
            violationUptimeMs = update.violationUptimeMs ?: currentLoc.violationUptimeMs,
            violationPercentage = update.violationPercentage ?: calculateViolationPercentage(update.violationUptimeMs, nowMs - appStartTime),
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            lastValidFixRealtime = if (update.lastValidFixRealtime > 0) update.lastValidFixRealtime else currentLoc.lastValidFixRealtime,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            gnssDetail = update.gnssDetail ?: currentLoc.gnssDetail,
            snrIdx = update.snrIdx,
            isCoolingModeActive = update.isCoolingModeActive,
            currentMa = update.currentMa
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
            isSitDetected = status.isSitDetected, lastSitTs = status.lastSitTs, verticalVelocity = status.verticalVelocity,
            sitVz = status.sitVz, sitDz = status.sitDz, sitBaro = status.sitBaro, sitTilt = status.sitTilt, sitShock = status.sitShock,
            peakVibrationShock = status.peakVibrationShock, peakVibrationShockTs = status.peakVibrationShockTs,
            luxBaseline = status.luxBaseline, acousticFloorDb = status.acousticFloorDb, adaptiveVibrationFloor = status.adaptiveVibrationFloor,
            proxIdx = status.proxIdx, violationUptimeMs = status.violationUptimeMs, violationPercentage = status.violationPercentage,
            isLocationPending = status.isLocationPending, 
            locationPendingReason = status.locationPendingReason,
            lastValidFixRealtime = status.lastValidFixRealtime,
            isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket, netInterface = status.netInterface,
            isStorageLow = status.isStorageLow, isStorageCritical = status.isStorageCritical,
            gnssDetail = status.gnssDetail, snrIdx = status.snrIdx,
            isCoolingModeActive = status.isCoolingModeActive,
            currentMa = status.currentMa
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
            maxAccuracy = if (update.maxAccuracy > 0) update.maxAccuracy else currentLoc.maxAccuracy,
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
            lastSitTs = update.lastSitTs ?: currentLoc.lastSitTs,
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
            adaptiveVibrationFloor = update.adaptiveVibrationFloor ?: SentinelValidator.updateVibrationFloor(currentLoc.adaptiveVibrationFloor, update.vibration ?: 0f, false),
            proxIdx = update.proxIdx ?: currentLoc.proxIdx,
            violationUptimeMs = update.violationUptimeMs ?: currentLoc.violationUptimeMs,
            violationPercentage = update.violationPercentage ?: calculateViolationPercentage(update.violationUptimeMs, nowMs - appStartTime),
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            lastValidFixRealtime = if (update.lastValidFixRealtime > 0) update.lastValidFixRealtime else currentLoc.lastValidFixRealtime,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            gnssDetail = update.gnssDetail ?: currentLoc.gnssDetail,
            snrIdx = update.snrIdx,
            isCoolingModeActive = update.isCoolingModeActive,
            currentMa = update.currentMa
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

    fun calculateViolationPercentage(violationMs: Long?, totalMs: Long): Float {
        if (violationMs == null || totalMs <= 0) return 0f
        return (violationMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }
}
