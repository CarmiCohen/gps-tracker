package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * SystemHealthState: The authoritative model for all device metadata and health status.
 * Oct.8.15:
 * - Issue #SIMP-1015-1: Build Parity. Implemented full Locatable interface 
 *   to resolve compilation errors from model consolidation.
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
    
    override var isLocationPending: Boolean = false,
    override var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    override var lat: Double = 0.0,
    override var lng: Double = 0.0,
    override var alt: Double = 0.0,
    override var gpsTs: Long = 0L,
    override var ts: Long = 0L,
    override var rt: Long = 0L,
    
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
    var coolingEnteredRt: Long = 0L,
    var gnssDetail: GnssDetail? = null,
    var snrIdx: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var liftIdx: Double = 0.0,
    var tiltIdx: Double = 0.0,
    var baroIdx: Double = 0.0,
    
    // Performance & Load Correlation
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L, 
    var isThermalThrottling: Boolean = false,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    val forensic: ForensicSnapshot = ForensicSnapshot(),

    // Forensic Persistence Health
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

    // Forensic Sit Detection
    var isSitDetected: Boolean = false,
    var isSitActive: Boolean = false,
    var lastSitTs: Long = 0L,
    var verticalVelocity: Double = 0.0,
    var sitVz: Double = 0.0,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,

    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,

    var isSilentFailure: Boolean = false,
    var isUltraLongStationary: Boolean = false,
    var isMaliAnomaly: Boolean = false,
    var isGnssThrottled: Boolean = false,

    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L,
    var tamperNote: String? = null,
    var activityType: ActivityType = ActivityType.UNKNOWN
) : Locatable {

    var thermalSnapshot: Double?
        get() = forensic.thermal
        set(value) { forensic.thermal = value }
    var heapSnapshot: Double?
        get() = forensic.heap
        set(value) { forensic.heap = value }

    var isSuspiciousNoise: Boolean
        get() = forensic.isSuspiciousNoise
        set(value) { forensic.isSuspiciousNoise = value }
    var isMemoryPressureThrottled: Boolean
        get() = forensic.isMemoryPressureThrottled
        set(value) { forensic.isMemoryPressureThrottled = value }

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
        this.lat = other.lat
        this.lng = other.lng
        this.alt = other.alt
        this.gpsTs = other.gpsTs
        this.ts = other.ts
        this.rt = other.rt
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
        this.coolingEnteredRt = other.coolingEnteredRt
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
        this.maxIoLatency = other.maxIoLatency
        this.isThermalThrottling = other.isThermalThrottling
        this.thermalHeadroom = other.thermalHeadroom
        this.heapAllocatedMb = other.heapAllocatedMb
        this.forensic.copyFrom(other.forensic)
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
        this.isSilentFailure = other.isSilentFailure
        this.isUltraLongStationary = other.isUltraLongStationary
        this.isMaliAnomaly = other.isMaliAnomaly
        this.isGnssThrottled = other.isGnssThrottled
        this.lastEnergyDeltaMa = other.lastEnergyDeltaMa
        this.lastEnergyDeltaTemp = other.lastEnergyDeltaTemp
        this.lastEnergyDurationMs = other.lastEnergyDurationMs
        this.tamperNote = other.tamperNote
        this.activityType = other.activityType
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
        isBatteryLow: Boolean = false, isBatteryCritical: Boolean = false, maxIoLatency: Long = 0L,
        isSilentFailure: Boolean = false, isThermalThrottling: Boolean = false,
        isUltraLongStationary: Boolean = false, isMaliAnomaly: Boolean = false,
        isGnssThrottled: Boolean = false,
        lastEnergyDeltaMa: Int = 0, lastEnergyDeltaTemp: Double = 0.0, lastEnergyDurationMs: Long = 0L,
        tamperNote: String? = null, coolingEnteredRt: Long = 0L,
        thermalHeadroom: Double = 0.0, heapAllocatedMb: Double = 0.0,
        thermalSnapshot: Double? = null, heapSnapshot: Double? = null,
        isSuspiciousNoise: Boolean = false, isMemoryPressureThrottled: Boolean = false,
        activityType: ActivityType = ActivityType.UNKNOWN,
        lat: Double = 0.0, lng: Double = 0.0, alt: Double = 0.0, gpsTs: Long = 0L, ts: Long = 0L, rt: Long = 0L
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
        this.maxIoLatency = maxIoLatency
        this.isThermalThrottling = isThermalThrottling
        this.thermalHeadroom = thermalHeadroom
        this.heapAllocatedMb = heapAllocatedMb
        this.forensic.thermal = thermalSnapshot
        this.forensic.heap = heapSnapshot
        this.forensic.isSuspiciousNoise = isSuspiciousNoise
        this.forensic.isMemoryPressureThrottled = isMemoryPressureThrottled
        this.forensicReliability = forensicReliability
        this.vibration = vibration
        this.isBatteryLow = isBatteryLow
        this.isBatteryCritical = isBatteryCritical
        this.isSilentFailure = isSilentFailure
        this.isUltraLongStationary = isUltraLongStationary
        this.isMaliAnomaly = isMaliAnomaly
        this.isGnssThrottled = isGnssThrottled
        this.lastEnergyDeltaMa = lastEnergyDeltaMa
        this.lastEnergyDeltaTemp = lastEnergyDeltaTemp
        this.lastEnergyDurationMs = lastEnergyDurationMs
        this.tamperNote = tamperNote
        this.coolingEnteredRt = coolingEnteredRt
        this.activityType = activityType
        this.lat = lat
        this.lng = lng
        this.alt = alt
        this.gpsTs = gpsTs
        this.ts = ts
        this.rt = rt
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
        lat = 0.0; lng = 0.0; alt = 0.0; gpsTs = 0L; ts = 0L; rt = 0L
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
        coolingEnteredRt = 0L
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
        maxIoLatency = 0L
        isThermalThrottling = false
        thermalHeadroom = 0.0
        heapAllocatedMb = 0.0
        forensic.reset()
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
        proximityCm = 0.0
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
        isSilentFailure = false
        isUltraLongStationary = false
        isMaliAnomaly = false
        isGnssThrottled = false
        lastEnergyDeltaMa = 0
        lastEnergyDeltaTemp = 0.0
        lastEnergyDurationMs = 0L
        tamperNote = null
        activityType = ActivityType.UNKNOWN
    }
}
