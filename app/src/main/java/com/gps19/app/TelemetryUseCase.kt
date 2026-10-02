package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject

/**
 * TelemetryUseCase: Logic for processing and mapping raw telemetry updates to UI states.
 * Oct.2.9:
 * - Issue #1314: TrackerStatus Convergence. Fixed property routing for 
 *   liftIdx and tamperNote.
 * Sep.10.40:
 * - Issue #946 Visibility RESOLVED: Added tamperNote mapping to mapHealth, 
 *   mapTrackerLocation, and mapHealthFromStatus for header forensic parity (R-ID 288).
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
            currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
            currentLoc.kinetic.bearing = update.kinetic.bearing
        }
        if (update.kinetic.maxAccuracy > 0.0) currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
        if (newTimestamp > 0) currentLoc.kinetic.gpsTs = newTimestamp
        
        currentLoc.ts = effectiveTelemetryTs
        currentLoc.rt = update.rt 
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.integrity.gnssDetail?.let { currentLoc.integrity.gnssDetail = it }
        currentLoc.integrity.isGnssThrottled = update.integrity.isGnssThrottled
        currentLoc.integrity.tamperNote = update.integrity.tamperNote
        
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
            isGnssThrottled = update.integrity.isGnssThrottled,
            tamperNote = update.integrity.tamperNote
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
            currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
            currentLoc.kinetic.bearing = update.kinetic.bearing
        }
        if (update.kinetic.maxAccuracy > 0.0) currentLoc.kinetic.maxAccuracy = update.kinetic.maxAccuracy
        if (newTimestamp > 0) currentLoc.kinetic.gpsTs = newTimestamp
        
        currentLoc.ts = if (update.ts > 0) update.ts else nowMs
        currentLoc.rt = update.rt
        currentLoc.status = update.status
        currentLoc.trackerState = update.trackerState
        update.integrity.gnssDetail?.let { currentLoc.integrity.gnssDetail = it }
        currentLoc.integrity.isGnssThrottled = update.integrity.isGnssThrottled
        currentLoc.integrity.tamperNote = update.integrity.tamperNote

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
        currentStats.violationUptimeMs = update.integrity.violationUptimeMs
        currentStats.violationPercentage = update.integrity.violationPercentage
        return currentStats
    }
}
