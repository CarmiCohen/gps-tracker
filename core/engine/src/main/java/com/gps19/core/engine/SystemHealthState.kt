package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * SystemHealthState: The authoritative model for all device metadata and health status.
 * Aug.10.24:
 * - Issue #129: Forensic Storage Pruning Sensitivity. Added isBatteryLow and 
 *   isBatteryCritical to support battery-aware adaptive pruning (R129).
 * Aug.07.127:
 * - Issue #124: GPS Hardware Revival Hardening (R124). Added gpsHardwareLock 
 *   to track critical hardware stall escalations.
 * Aug.04.114:
 * - Issue #728: Forensic Audit: Storage-Aware Adaptive Pruning. Added 
 *   storageAvailableMb and storageTotalMb for granular pressure diagnostics (R728).
 */
@Serializable
class SystemHealthState(
    var signalLoss: Boolean = false,
    var gpsStalled: Boolean = false,
    var gpsHardwareLock: Boolean = false,
    var localInternetLoss: Boolean = false,
    var isHardwareOnline: Boolean = true,
    var batteryLevel: Int = 100,
    var batteryTemp: Double = 0.0,
    var maxTemp: Double = 0.0,
    var isCharging: Boolean = false,
    var currentMa: Int = 0,
    var status: SentinelStatus = SentinelStatus.VALID,
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var isJammer: Boolean = false,
    var isTamperDetected: Boolean = false,
    var micPending: Boolean = false,
    var isPowerTamper: Boolean = false,
    var isClockRegression: Boolean = false,
    var isLocationPending: Boolean = false,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var lastValidFixRt: Long = 0L,
    var lastLocationPendingDurationMs: Long = 0L,
    var isPowerSaveMode: Boolean = false,
    var standbyBucket: Int = -1,
    var netInterface: String = "UNKNOWN",
    var isStorageLow: Boolean = false,
    var isStorageCritical: Boolean = false,
    var storageAvailableMb: Long = 0L,
    var storageTotalMb: Long = 0L,
    var isBatterySteepDischarge: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var gnssDetail: GnssDetail? = null,
    var snrIdx: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var liftIdx: Double = 0.0,
    var tiltIdx: Double = 0.0,
    var baroIdx: Double = 0.0,
    
    // Performance & Load Correlation (Issue #711/R711)
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,

    // Forensic Persistence Health (Issue #714/R714)
    var forensicReliability: Double = 1.0,

    // Connectivity Stats
    var uptimeMs: Long = 0L,
    var lastConnTs: Long = 0L,
    var lastDiscTs: Long = 0L,
    var totalDropMs: Long = 0L,
    var maxDropMs: Long = 0L,
    var maxDropTs: Long = 0L,
    var totalConnectedMs: Long = 0L,
    var sessionConnectedMs: Long = 0L,
    var violationUptimeMs: Long = 0L,
    var violationPercentage: Double = 0.0,
    var lastIntegrityHeartbeatRt: Long = 0L,

    // Sensor Metadata
    var vibration: Double = 0.0,
    var heading: Double = 0.0,
    var tiltDegrees: Double = 0.0,
    var acousticDb: Double = 0.0,
    var baroAlt: Double = 0.0,
    var lux: Double = 0.0,
    var isNear: Boolean = true,
    var peakVibrationShock: Double = 0.0,
    var peakVibrationShockTs: Long = 0L,
    var luxBaseline: Double = 0.0,
    var acousticFloorDb: Double = 0.0,
    var adaptiveVibrationFloor: Double = 0.12,
    var proxIdx: Double = 1.0,
    var proximityCm: Double = -1.0,
    var proximityDebounceMs: Long = 0L,
    var vibrationRollingSum: Double = 0.0,
    var kineticEnergy: Double = 0.0,

    // Forensic Sit Detection (Issue #102/R990)
    var isSitDetected: Boolean = false,
    var isSitActive: Boolean = false,
    var lastSitTs: Long = 0L,
    var verticalVelocity: Double = 0.0,
    var sitVz: Double = 0.0,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,

    // Issue #129: Adaptive Pruning Sensitivity
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false
) {
    fun copyFrom(other: SystemHealthState) {
        this.signalLoss = other.signalLoss
        this.gpsStalled = other.gpsStalled
        this.gpsHardwareLock = other.gpsHardwareLock
        this.localInternetLoss = other.localInternetLoss
        this.isHardwareOnline = other.isHardwareOnline
        this.batteryLevel = other.batteryLevel
        this.batteryTemp = other.batteryTemp
        this.maxTemp = other.maxTemp
        this.isCharging = other.isCharging
        this.currentMa = other.currentMa
        this.status = other.status
        this.trackerState = other.trackerState
        this.isJammer = other.isJammer
        this.isTamperDetected = other.isTamperDetected
        this.micPending = other.micPending
        this.isPowerTamper = other.isPowerTamper
        this.isClockRegression = other.isClockRegression
        this.isLocationPending = other.isLocationPending
        this.locationPendingReason = other.locationPendingReason
        this.lastValidFixRt = other.lastValidFixRt
        this.lastLocationPendingDurationMs = other.lastLocationPendingDurationMs
        this.isPowerSaveMode = other.isPowerSaveMode
        this.standbyBucket = other.standbyBucket
        this.netInterface = other.netInterface
        this.isStorageLow = other.isStorageLow
        this.isStorageCritical = other.isStorageCritical
        this.storageAvailableMb = other.storageAvailableMb
        this.storageTotalMb = other.storageTotalMb
        this.isBatterySteepDischarge = other.isBatterySteepDischarge
        this.isCoolingModeActive = other.isCoolingModeActive
        this.gnssDetail = other.gnssDetail
        this.snrIdx = other.snrIdx
        this.noiseIdx = other.noiseIdx
        this.luxIdx = other.luxIdx
        this.vibeIdx = other.vibeIdx
        this.liftIdx = other.liftIdx
        this.tiltIdx = other.tiltIdx
        this.baroIdx = other.baroIdx
        this.cpuLoad = other.cpuLoad
        this.ioWait = other.ioWait
        this.forensicReliability = other.forensicReliability
        this.uptimeMs = other.uptimeMs
        this.lastConnTs = other.lastConnTs
        this.lastDiscTs = other.lastDiscTs
        this.totalDropMs = other.totalDropMs
        this.maxDropMs = other.maxDropMs
        this.maxDropTs = other.maxDropTs
        this.totalConnectedMs = other.totalConnectedMs
        this.sessionConnectedMs = other.sessionConnectedMs
        this.violationUptimeMs = other.violationUptimeMs
        this.violationPercentage = other.violationPercentage
        this.lastIntegrityHeartbeatRt = other.lastIntegrityHeartbeatRt
        this.vibration = other.vibration
        this.heading = other.heading
        this.tiltDegrees = other.tiltDegrees
        this.acousticDb = other.acousticDb
        this.baroAlt = other.baroAlt
        this.lux = other.lux
        this.isNear = other.isNear
        this.peakVibrationShock = other.peakVibrationShock
        this.peakVibrationShockTs = other.peakVibrationShockTs
        this.luxBaseline = other.luxBaseline
        this.acousticFloorDb = other.acousticFloorDb
        this.adaptiveVibrationFloor = other.adaptiveVibrationFloor
        this.proxIdx = other.proxIdx
        this.proximityCm = other.proximityCm
        this.proximityDebounceMs = other.proximityDebounceMs
        this.vibrationRollingSum = other.vibrationRollingSum
        this.kineticEnergy = other.kineticEnergy
        this.isSitDetected = other.isSitDetected
        this.isSitActive = other.isSitActive
        this.lastSitTs = other.lastSitTs
        this.verticalVelocity = other.verticalVelocity
        this.sitVz = other.sitVz
        this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro
        this.sitTilt = other.sitTilt
        this.sitShock = other.sitShock
        this.isBatteryLow = other.isBatteryLow
        this.isBatteryCritical = other.isBatteryCritical
    }
    
    fun update(
        signalLoss: Boolean, gpsStalled: Boolean, gpsHardwareLock: Boolean, localInternetLoss: Boolean, isHardwareOnline: Boolean,
        batteryLevel: Int, batteryTemp: Double, isCharging: Boolean, currentMa: Int,
        status: SentinelStatus, isJammer: Boolean, isTamperDetected: Boolean, tiltDegrees: Double,
        acousticDb: Double, baroAlt: Double, lux: Double, isNear: Boolean,
        luxBaseline: Double, acousticFloorDb: Double, adaptiveVibrationFloor: Double,
        peakVibrationShock: Double, isPowerTamper: Boolean, isLocationPending: Boolean,
        locationPendingReason: LocationPendingReason, isPowerSaveMode: Boolean, standbyBucket: Int,
        netInterface: String, isStorageLow: Boolean, isStorageCritical: Boolean,
        isBatterySteepDischarge: Boolean, isCoolingModeActive: Boolean,
        cpuLoad: Double = 0.0, ioWait: Double = 0.0, forensicReliability: Double = 1.0,
        vibration: Double = 0.0, storageAvailableMb: Long = 0L, storageTotalMb: Long = 0L,
        isBatteryLow: Boolean = false, isBatteryCritical: Boolean = false
    ) {
        this.signalLoss = signalLoss
        this.gpsStalled = gpsStalled
        this.gpsHardwareLock = gpsHardwareLock
        this.localInternetLoss = localInternetLoss
        this.isHardwareOnline = isHardwareOnline
        this.batteryLevel = batteryLevel
        this.batteryTemp = batteryTemp
        this.isCharging = isCharging
        this.currentMa = currentMa
        this.status = status
        this.isJammer = isJammer
        this.isTamperDetected = isTamperDetected
        this.tiltDegrees = tiltDegrees
        this.acousticDb = acousticDb
        this.baroAlt = baroAlt
        this.lux = lux
        this.isNear = isNear
        this.luxBaseline = luxBaseline
        this.acousticFloorDb = acousticFloorDb
        this.adaptiveVibrationFloor = adaptiveVibrationFloor
        this.peakVibrationShock = peakVibrationShock
        this.isPowerTamper = isPowerTamper
        this.isLocationPending = isLocationPending
        this.locationPendingReason = locationPendingReason
        this.isPowerSaveMode = isPowerSaveMode
        this.standbyBucket = standbyBucket
        this.netInterface = netInterface
        this.isStorageLow = isStorageLow
        this.isStorageCritical = isStorageCritical
        this.storageAvailableMb = storageAvailableMb
        this.storageTotalMb = storageTotalMb
        this.isBatterySteepDischarge = isBatterySteepDischarge
        this.isCoolingModeActive = isCoolingModeActive
        this.cpuLoad = cpuLoad
        this.ioWait = ioWait
        this.forensicReliability = forensicReliability
        this.vibration = vibration
        this.isBatteryLow = isBatteryLow
        this.isBatteryCritical = isBatteryCritical
    }

    fun reset() {
        signalLoss = false
        gpsStalled = false
        gpsHardwareLock = false
        localInternetLoss = false
        isHardwareOnline = true
        batteryLevel = 100
        batteryTemp = 0.0
        maxTemp = 0.0
        isCharging = false
        currentMa = 0
        status = SentinelStatus.VALID
        trackerState = TrackerState.UNKNOWN
        isJammer = false
        isTamperDetected = false
        micPending = false
        isPowerTamper = false
        isClockRegression = false
        isLocationPending = false
        locationPendingReason = LocationPendingReason.NONE
        lastValidFixRt = 0L
        lastLocationPendingDurationMs = 0L
        isPowerSaveMode = false
        standbyBucket = -1
        netInterface = "UNKNOWN"
        isStorageLow = false
        isStorageCritical = false
        storageAvailableMb = 0L
        storageTotalMb = 0L
        isBatterySteepDischarge = false
        isCoolingModeActive = false
        gnssDetail = null
        snrIdx = 0.0
        noiseIdx = 0.0
        luxIdx = 0.0
        vibeIdx = 0.0
        liftIdx = 0.0
        tiltIdx = 0.0
        baroIdx = 0.0
        cpuLoad = 0.0
        ioWait = 0.0
        forensicReliability = 1.0
        uptimeMs = 0L
        lastConnTs = 0L
        lastDiscTs = 0L
        totalDropMs = 0L
        maxDropMs = 0L
        maxDropTs = 0L
        totalConnectedMs = 0L
        sessionConnectedMs = 0L
        violationUptimeMs = 0L
        violationPercentage = 0.0
        lastIntegrityHeartbeatRt = 0L
        vibration = 0.0
        heading = 0.0
        tiltDegrees = 0.0
        acousticDb = 0.0
        baroAlt = 0.0
        lux = 0.0
        isNear = true
        peakVibrationShock = 0.0
        peakVibrationShockTs = 0L
        luxBaseline = 0.0
        acousticFloorDb = 0.0
        adaptiveVibrationFloor = 0.12
        proxIdx = 1.0
        proximityCm = -1.0
        proximityDebounceMs = 0L
        vibrationRollingSum = 0.0
        kineticEnergy = 0.0
        isSitDetected = false
        isSitActive = false
        lastSitTs = 0L
        verticalVelocity = 0.0
        sitVz = 0.0
        sitDz = 0.0
        sitBaro = 0.0
        sitTilt = 0.0
        sitShock = 0.0
        isBatteryLow = false
        isBatteryCritical = false
    }
}
