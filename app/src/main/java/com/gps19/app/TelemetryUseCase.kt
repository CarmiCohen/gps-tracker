package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * Sep.03.05:
 * - Issue #238: Location Model Unification. Refactored all mapping methods 
 *   to use LocationUpdate as the unified model, eliminating LocationState 
 *   to reduce allocation churn (R-ID 238).
 * Aug.31.02:
 * - Issue #762 Validation: Hardened isUltraLongStationary mapping in 
 *   mapHealthFromUpdate and mapHealthFromStatus to ensure state parity 
 *   across all ingestion paths (R765, R778).
 */
class TelemetryUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {
    fun mapTrackerLocation(
        update: LocationUpdate, 
        currentLoc: LocationUpdate, 
        nowMs: Long, 
        appStartTime: Long
    ): LocationUpdate {
        val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
        val newTimestamp = update.gpsTs
        
        val effectiveTelemetryTs = if (!update.isMe) nowMs else (if (update.ts > 0) update.ts else nowMs)
        
        if (isLocationValid) {
            currentLoc.lat = update.lat
            currentLoc.lng = update.lng
            currentLoc.speed = update.speed
            currentLoc.accuracy = update.accuracy
            currentLoc.bearing = update.bearing
        }
        if (update.maxAccuracy > 0.0) currentLoc.maxAccuracy = update.maxAccuracy
        if (newTimestamp > 0) currentLoc.gpsTs = newTimestamp
        
        currentLoc.ts = effectiveTelemetryTs
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.gnssDetail?.let { currentLoc.gnssDetail = it }
        
        return currentLoc
    }

    fun mapHealthFromUpdate(update: LocationUpdate, current: SystemHealthState): SystemHealthState {
        current.update(
            signalLoss = update.signal?.let { it < 2 } ?: current.signalLoss,
            gpsStalled = update.locationPendingReason == LocationPendingReason.GPS_STALL,
            gpsHardwareLock = update.gpsHardwareLock,
            localInternetLoss = current.localInternetLoss, 
            isHardwareOnline = update.signal != null,
            batteryLevel = if (update.battery >= 0) update.battery else current.batteryLevel,
            batteryTemp = update.temp,
            isCharging = update.isCharging,
            currentMa = update.currentMa,
            status = update.status,
            isJammer = update.locationPendingReason == LocationPendingReason.JAMMER_SUSPICION,
            isTamperDetected = update.isTamperDetected,
            tiltDegrees = update.tiltDegrees ?: current.tiltDegrees,
            acousticDb = update.acousticDb ?: current.acousticDb,
            baroAlt = update.baroAlt ?: current.baroAlt,
            lux = update.lux ?: current.lux,
            isNear = update.isNear ?: current.isNear,
            luxBaseline = update.luxBaseline ?: current.luxBaseline,
            acousticFloorDb = update.acousticFloorDb ?: current.acousticFloorDb,
            adaptiveVibrationFloor = update.adaptiveVibrationFloor ?: current.adaptiveVibrationFloor,
            peakVibrationShock = update.peakVibrationShock ?: current.peakVibrationShock,
            isPowerTamper = update.isPowerTamper,
            isLocationPending = update.isLocationPending,
            locationPendingReason = update.locationPendingReason,
            isPowerSaveMode = update.isPowerSaveMode,
            standbyBucket = update.standbyBucket,
            netInterface = update.netInterface,
            isStorageLow = update.isStorageLow,
            isStorageCritical = update.isStorageCritical,
            isBatterySteepDischarge = update.isBatterySteepDischarge,
            isCoolingModeActive = update.isCoolingModeActive,
            isBatteryLow = update.isBatteryLow,
            isBatteryCritical = update.isBatteryCritical,
            isUltraLongStationary = update.isUltraLongStationary
        )
        
        if (update.maxTemp > 0.0) current.maxTemp = update.maxTemp
        current.trackerState = update.trackerState
        if (update.lastValidFixRt > 0L) current.lastValidFixRt = update.lastValidFixRt
        update.gnssDetail?.let { current.gnssDetail = it }
        current.snrIdx = update.snrIdx
        current.noiseIdx = update.noiseIdx
        current.luxIdx = update.luxIdx
        current.vibeIdx = update.vibeIdx
        current.liftIdx = update.liftIdx
        current.tiltIdx = update.tiltIdx
        current.baroIdx = update.baroIdx
        update.uptimeMs?.let { current.uptimeMs = it }
        update.lastConnTs?.let { current.lastConnTs = it }
        update.lastDiscTs?.let { current.lastDiscTs = it }
        update.totalDropMs?.let { current.totalDropMs = it }
        update.maxDropMs?.let { current.maxDropMs = it }
        update.maxDropTs?.let { current.maxDropTs = it }
        update.totalConnectedMs?.let { current.totalConnectedMs = it }
        update.sessionConnectedMs?.let { current.sessionConnectedMs = it }
        update.violationUptimeMs?.let { current.violationUptimeMs = it }
        update.violationPercentage?.let { current.violationPercentage = it }
        update.vibration?.let { current.vibration = it }
        update.heading?.let { current.heading = it }
        update.peakVibrationShockTs?.let { current.peakVibrationShockTs = it }
        update.proxIdx?.let { current.proxIdx = it }
        update.proximityCm?.let { current.proximityCm = it }
        update.proximityDebounceMs?.let { current.proximityDebounceMs = it }
        update.vibrationRollingSum?.let { current.vibrationRollingSum = it }
        current.isSitDetected = update.isSitDetected
        current.isSitActive = update.isSitActive
        current.lastSitTs = update.lastSitTs
        update.verticalVelocity?.let { current.verticalVelocity = it }
        update.sitVz?.let { current.sitVz = it }
        update.sitDz?.let { current.sitDz = it }
        update.sitBaro?.let { current.sitBaro = it }
        update.sitTilt?.let { current.sitTilt = it }
        update.sitShock?.let { current.sitShock = it }
        current.kineticEnergy = update.kineticEnergy

        return current
    }

    fun mapHealthFromStatus(status: TrackerStatus, current: SystemHealthState): SystemHealthState {
        current.batteryLevel = status.battery
        current.batteryTemp = status.temp
        current.maxTemp = status.maxTemp
        current.isCharging = status.isCharging
        current.currentMa = status.currentMa
        current.status = status.status
        current.trackerState = status.trackerState
        current.isJammer = status.isJammer
        current.isTamperDetected = status.isTamperDetected
        current.isPowerTamper = status.isPowerTamper
        current.isLocationPending = status.isLocationPending
        current.locationPendingReason = status.locationPendingReason
        current.lastValidFixRt = status.lastValidFixRt
        current.isPowerSaveMode = status.isPowerSaveMode
        current.standbyBucket = status.standbyBucket
        current.netInterface = status.netInterface
        current.isStorageLow = status.isStorageLow
        current.isStorageCritical = status.isStorageCritical
        current.isBatterySteepDischarge = status.isBatterySteepDischarge
        current.isCoolingModeActive = status.isCoolingModeActive
        current.isBatteryLow = status.isBatteryLow
        current.isBatteryCritical = status.isBatteryCritical
        current.isUltraLongStationary = status.isUltraLongStationary
        current.gpsHardwareLock = status.locationPendingReason == LocationPendingReason.GPS_STALL
        current.gnssDetail = status.gnssDetail
        current.snrIdx = status.snrIdx
        current.noiseIdx = status.noiseIdx
        current.luxIdx = status.luxIdx
        current.vibeIdx = status.vibeIdx
        current.liftIdx = status.liftIdx
        current.tiltIdx = status.tiltIdx
        current.baroIdx = status.baroIdx
        current.uptimeMs = status.uptimeMs
        current.lastConnTs = status.lastConnTs
        current.lastDiscTs = status.lastDiscTs
        current.totalDropMs = status.totalDropMs
        current.maxDropMs = status.maxDropMs
        current.maxDropTs = status.maxDropTs
        current.totalConnectedMs = status.totalConnectedMs
        current.sessionConnectedMs = status.sessionConnectedMs
        current.violationUptimeMs = status.violationUptimeMs
        current.violationPercentage = status.violationPercentage
        current.vibration = status.vibration
        current.heading = status.heading
        current.tiltDegrees = status.tiltDegrees
        current.acousticDb = status.acousticDb
        current.baroAlt = status.baroAlt
        current.lux = status.lux
        current.isNear = status.isNear
        current.peakVibrationShock = status.peakVibrationShock
        current.peakVibrationShockTs = status.peakVibrationShockTs
        current.luxBaseline = status.luxBaseline
        current.acousticFloorDb = status.acousticFloorDb
        current.adaptiveVibrationFloor = status.adaptiveVibrationFloor
        current.proxIdx = status.proxIdx
        current.proximityCm = status.proximityCm
        current.proximityDebounceMs = status.proximityDebounceMs
        current.vibrationRollingSum = status.vibrationRollingSum
        current.isSitDetected = status.isSitDetected
        current.isSitActive = status.isSitActive
        current.lastSitTs = status.lastSitTs
        current.verticalVelocity = status.verticalVelocity
        current.sitVz = status.sitVz
        current.sitDz = status.sitDz
        current.sitBaro = status.sitBaro
        current.sitTilt = status.sitTilt
        current.sitShock = status.sitShock
        current.kineticEnergy = status.kineticEnergy
        return current
    }

    fun mapTrackerLocationFromStatus(status: TrackerStatus, currentLoc: LocationUpdate): LocationUpdate {
        currentLoc.lat = status.lat
        currentLoc.lng = status.lng
        currentLoc.speed = status.speed
        currentLoc.bearing = status.bearing
        currentLoc.accuracy = status.accuracy
        currentLoc.maxAccuracy = status.maxAccuracy
        currentLoc.gpsTs = status.gpsTs
        currentLoc.ts = status.ts
        currentLoc.status = status.status
        currentLoc.trackerState = status.trackerState
        currentLoc.gnssDetail = status.gnssDetail
        return currentLoc
    }

    fun mapLocalLocation(
        update: LocationUpdate, 
        currentLoc: LocationUpdate, 
        nowMs: Long, 
        appStartTime: Long
    ): LocationUpdate {
        val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
        val newTimestamp = update.gpsTs
        
        if (isLocationValid) {
            currentLoc.lat = update.lat
            currentLoc.lng = update.lng
            currentLoc.speed = update.speed
            currentLoc.accuracy = update.accuracy
            currentLoc.bearing = update.bearing
        }
        if (update.maxAccuracy > 0.0) currentLoc.maxAccuracy = update.maxAccuracy
        if (newTimestamp > 0) currentLoc.gpsTs = newTimestamp
        
        currentLoc.ts = if (update.ts > 0) update.ts else nowMs
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.gnssDetail?.let { currentLoc.gnssDetail = it }

        return currentLoc
    }

    fun mapStats(update: LocationUpdate, currentStats: StatsState): StatsState {
        currentStats.update(
            totalConnectedMs = update.totalConnectedMs ?: currentStats.totalConnectedMs, 
            sessionConnectedMs = update.sessionConnectedMs ?: currentStats.sessionConnectedMs, 
            maxDropMs = update.maxDropMs ?: currentStats.maxDropMs,
            maxDropTs = update.maxDropTs ?: currentStats.maxDropTs,
            totalDropMs = update.totalDropMs ?: currentStats.totalDropMs, 
            uptimeMs = update.uptimeMs ?: currentStats.uptimeMs, 
            lastConnTs = update.lastConnTs ?: currentStats.lastConnTs, 
            lastDiscTs = update.lastDiscTs ?: currentStats.lastDiscTs
        )
        return currentStats
    }

    fun mapStatsFromStatus(status: TrackerStatus, currentStats: StatsState): StatsState {
        currentStats.update(
            totalConnectedMs = status.totalConnectedMs,
            sessionConnectedMs = status.sessionConnectedMs,
            maxDropMs = status.maxDropMs,
            maxDropTs = status.maxDropTs,
            totalDropMs = status.totalDropMs,
            uptimeMs = status.uptimeMs,
            lastConnTs = status.lastConnTs,
            lastDiscTs = status.lastDiscTs
        )
        currentStats.violationUptimeMs = status.violationUptimeMs
        currentStats.violationPercentage = status.violationPercentage
        return currentStats
    }
}
