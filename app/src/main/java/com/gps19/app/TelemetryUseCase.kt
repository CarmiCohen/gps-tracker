package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * Sep.09.11:
 * - Forensic Audit Hardening: Enhanced maxTemp propagation using maxOf to ensure peak 
 *   values are captured across partitioned updates (R-ID 284).
 * - Health Mapping: Ensured isHardwareOnline considers signal non-nullability from 
 *   ConnectivitySuite fix to resolve HUD "Offline" regressions.
 * Sep.09.00:
 * - Legacy Field Cleanup (Part A): Migrated to direct partitioned state access 
 *   (.kinetic, .atmospheric, .integrity) to bypass aggregate bridges (R-ID 284).
 * Sep.08.12:
 * - Issue #924 Visibility: Added isGnssThrottled mapping for A15 Hysteresis transparency.
 * Sep.06.59:
 * - Issue #935 FIX: Monotonic Propagation. Added missing rt field mapping 
 *   in mapTrackerLocation and mapLocalLocation to resolve HUD GPS red-lock.
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
        val isLocationValid = PhysicsUtils.isValidLocation(update.kinetic.lat, update.kinetic.lng)
        val newTimestamp = update.kinetic.gpsTs
        
        val effectiveTelemetryTs = if (!update.isMe) nowMs else (if (update.ts > 0) update.ts else nowMs)
        
        if (isLocationValid) {
            currentLoc.kinetic.lat = update.kinetic.lat
            currentLoc.kinetic.lng = update.kinetic.lng
            currentLoc.kinetic.speed = update.kinetic.speed
            currentLoc.kinetic.accuracy = update.kinetic.accuracy
            currentLoc.kinetic.bearing = update.kinetic.bearing
        }
        if (update.kinetic.maxAccuracy > 0.0) currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
        if (newTimestamp > 0) currentLoc.kinetic.gpsTs = newTimestamp
        
        currentLoc.ts = effectiveTelemetryTs
        currentLoc.kinetic.rt = update.kinetic.rt // Fix #935: Propagate monotonic timestamp
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.integrity.gnssDetail?.let { currentLoc.integrity.gnssDetail = it }
        currentLoc.integrity.isGnssThrottled = update.integrity.isGnssThrottled
        
        return currentLoc
    }

    fun mapHealthFromUpdate(update: LocationUpdate, current: SystemHealthState): SystemHealthState {
        current.update(
            signalLoss = update.integrity.signal?.let { it < 2 } ?: current.signalLoss,
            gpsStalled = update.integrity.locationPendingReason == LocationPendingReason.GPS_STALL,
            gpsHardwareLock = update.integrity.gpsHardwareLock,
            localInternetLoss = current.localInternetLoss, 
            isHardwareOnline = update.integrity.signal != null,
            batteryLevel = if (update.integrity.battery >= 0) update.integrity.battery else current.batteryLevel,
            batteryTemp = update.atmospheric.temp,
            isCharging = update.integrity.isCharging,
            currentMa = update.integrity.currentMa,
            status = update.status,
            isJammer = update.integrity.locationPendingReason == LocationPendingReason.JAMMER_SUSPICION,
            isTamperDetected = update.integrity.isTamperDetected,
            tiltDegrees = update.atmospheric.tiltDegrees,
            acousticDb = update.atmospheric.acousticDb,
            baroAlt = update.atmospheric.baroAlt,
            lux = update.atmospheric.lux,
            isNear = update.atmospheric.isNear,
            luxBaseline = update.atmospheric.luxBaseline,
            acousticFloorDb = update.atmospheric.acousticFloorDb,
            adaptiveVibrationFloor = update.atmospheric.adaptiveVibrationFloor,
            peakVibrationShock = update.atmospheric.peakVibrationShock,
            isPowerTamper = update.integrity.isPowerTamper,
            isLocationPending = update.integrity.isLocationPending,
            locationPendingReason = update.integrity.locationPendingReason,
            isPowerSaveMode = update.integrity.isPowerSaveMode,
            standbyBucket = update.integrity.standbyBucket,
            netInterface = update.integrity.netInterface,
            isStorageLow = update.integrity.isStorageLow,
            isStorageCritical = update.integrity.isStorageCritical,
            isBatterySteepDischarge = update.integrity.isBatterySteepDischarge,
            isCoolingModeActive = update.integrity.isCoolingModeActive,
            isBatteryLow = update.integrity.isBatteryLow,
            isBatteryCritical = update.integrity.isBatteryCritical,
            isUltraLongStationary = update.integrity.isUltraLongStationary,
            isGnssThrottled = update.integrity.isGnssThrottled
        )
        
        current.maxTemp = maxOf(current.maxTemp, update.atmospheric.maxTemp)
        current.trackerState = update.trackerState
        if (update.lastValidFixRt > 0L) current.lastValidFixRt = update.lastValidFixRt
        update.integrity.gnssDetail?.let { current.gnssDetail = it }
        current.snrIdx = update.integrity.snrIdx
        current.noiseIdx = update.atmospheric.noiseIdx
        current.luxIdx = update.atmospheric.luxIdx
        current.vibeIdx = update.atmospheric.vibeIdx
        current.liftIdx = update.atmospheric.liftIdx
        current.tiltIdx = update.atmospheric.tiltIdx
        current.baroIdx = update.atmospheric.baroIdx
        current.uptimeMs = update.integrity.uptimeMs
        current.lastConnTs = update.integrity.lastConnTs
        current.lastDiscTs = update.integrity.lastDiscTs
        current.totalDropMs = update.integrity.totalDropMs
        current.maxDropMs = update.integrity.maxDropMs
        current.maxDropTs = update.integrity.maxDropTs
        current.totalConnectedMs = update.integrity.totalConnectedMs
        current.sessionConnectedMs = update.integrity.sessionConnectedMs
        current.violationUptimeMs = update.integrity.violationUptimeMs
        current.violationPercentage = update.integrity.violationPercentage
        current.vibration = update.atmospheric.vibration
        current.heading = update.atmospheric.heading
        current.peakVibrationShockTs = update.atmospheric.peakVibrationShockTs
        current.proxIdx = update.atmospheric.proxIdx
        current.proximityCm = update.atmospheric.proximityCm
        current.proximityDebounceMs = update.atmospheric.proximityDebounceMs
        current.vibrationRollingSum = update.atmospheric.vibrationRollingSum
        current.isSitDetected = update.integrity.isSitDetected
        current.isSitActive = update.integrity.isSitActive
        current.lastSitTs = update.integrity.lastSitTs
        current.verticalVelocity = update.kinetic.verticalVelocity
        current.sitVz = update.integrity.sitVz
        current.sitDz = update.integrity.sitDz
        current.sitBaro = update.integrity.sitBaro
        current.sitTilt = update.integrity.sitTilt
        current.sitShock = update.integrity.sitShock
        current.kineticEnergy = update.kinetic.kineticEnergy

        return current
    }

    fun mapHealthFromStatus(status: TrackerStatus, current: SystemHealthState): SystemHealthState {
        current.batteryLevel = status.battery
        current.batteryTemp = status.temp
        current.maxTemp = maxOf(current.maxTemp, status.maxTemp)
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
        current.isGnssThrottled = status.isGnssThrottled
        return current
    }

    fun mapTrackerLocationFromStatus(status: TrackerStatus, currentLoc: LocationUpdate): LocationUpdate {
        currentLoc.kinetic.lat = status.lat
        currentLoc.kinetic.lng = status.lng
        currentLoc.kinetic.speed = status.speed
        currentLoc.kinetic.bearing = status.bearing
        currentLoc.kinetic.accuracy = status.accuracy
        currentLoc.kinetic.maxAccuracy = status.maxAccuracy
        currentLoc.kinetic.gpsTs = status.gpsTs
        currentLoc.ts = status.ts
        currentLoc.kinetic.rt = status.ts - (timeProvider.currentTimeMillis() - timeProvider.elapsedRealtime()) // Heuristic rt for remote status
        currentLoc.status = status.status
        currentLoc.trackerState = status.trackerState
        currentLoc.integrity.gnssDetail = status.gnssDetail
        currentLoc.integrity.isGnssThrottled = status.isGnssThrottled
        return currentLoc
    }

    fun mapLocalLocation(
        update: LocationUpdate, 
        currentLoc: LocationUpdate, 
        nowMs: Long, 
        appStartTime: Long
    ): LocationUpdate {
        val isLocationValid = PhysicsUtils.isValidLocation(update.kinetic.lat, update.kinetic.lng)
        val newTimestamp = update.kinetic.gpsTs
        
        if (isLocationValid) {
            currentLoc.kinetic.lat = update.kinetic.lat
            currentLoc.kinetic.lng = update.kinetic.lng
            currentLoc.kinetic.speed = update.kinetic.speed
            currentLoc.kinetic.accuracy = update.kinetic.accuracy
            currentLoc.kinetic.bearing = update.kinetic.bearing
        }
        if (update.kinetic.maxAccuracy > 0.0) currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
        if (newTimestamp > 0) currentLoc.kinetic.gpsTs = newTimestamp
        
        currentLoc.ts = if (update.ts > 0) update.ts else nowMs
        currentLoc.kinetic.rt = update.kinetic.rt // Fix #935: Propagate monotonic timestamp
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.integrity.gnssDetail?.let { currentLoc.integrity.gnssDetail = it }
        currentLoc.integrity.isGnssThrottled = update.integrity.isGnssThrottled

        return currentLoc
    }

    fun mapStats(update: LocationUpdate, currentStats: StatsState): StatsState {
        currentStats.update(
            totalConnectedMs = update.integrity.totalConnectedMs, 
            sessionConnectedMs = update.integrity.sessionConnectedMs, 
            maxDropMs = update.integrity.maxDropMs,
            maxDropTs = update.integrity.maxDropTs,
            totalDropMs = update.integrity.totalDropMs, 
            uptimeMs = update.integrity.uptimeMs, 
            lastConnTs = update.integrity.lastConnTs, 
            lastDiscTs = update.integrity.lastDiscTs
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
