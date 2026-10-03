package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * KineticState: Spatial and motion telemetry.
 */
@Serializable
data class KineticState(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var alt: Double = 0.0,
    var speed: Double = 0.0,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var bearing: Double = 0.0,
    var gpsTs: Long = 0L,
    var rt: Long = 0L,
    var isJump: Boolean = false,
    var isTrajectoryPromoted: Boolean = false,
    var jumpTier: Int = 0,
    var isAdaptiveJump: Boolean = false,
    var verticalVelocity: Double = 0.0,
    var kineticEnergy: Double = 0.0,
    var distToTracker: Double? = null,
    var distToHome: Double? = null,
    var activityType: ActivityType = ActivityType.UNKNOWN
) {
    fun copyFrom(other: KineticState) {
        this.lat = other.lat; this.lng = other.lng; this.alt = other.alt; this.speed = other.speed
        this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy; this.bearing = other.bearing
        this.gpsTs = other.gpsTs; this.rt = other.rt; this.isJump = other.isJump
        this.isTrajectoryPromoted = other.isTrajectoryPromoted; this.jumpTier = other.jumpTier
        this.isAdaptiveJump = other.isAdaptiveJump; this.verticalVelocity = other.verticalVelocity
        this.kineticEnergy = other.kineticEnergy; this.distToTracker = other.distToTracker
        this.distToHome = other.distToHome
        this.activityType = other.activityType
    }

    fun reset() {
        lat = 0.0; lng = 0.0; alt = 0.0; speed = 0.0; accuracy = 0.0; maxAccuracy = 0.0; bearing = 0.0
        gpsTs = 0L; rt = 0L; isJump = false; isTrajectoryPromoted = false; jumpTier = 0
        isAdaptiveJump = false; verticalVelocity = 0.0; kineticEnergy = 0.0
        distToTracker = null; distToHome = null; activityType = ActivityType.UNKNOWN
    }
}

/**
 * AtmosphericState: Environmental and IMU sensor telemetry.
 */
@Serializable
data class AtmosphericState(
    var temp: Double = 0.0,
    var maxTemp: Double = 0.0,
    var baroAlt: Double = 0.0,
    var baroIdx: Double = 0.0,
    var lux: Double = 0.0,
    var luxBaseline: Double = 0.0,
    var luxIdx: Double = 0.0,
    var acousticDb: Double = 0.0,
    var acousticFloorDb: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var tiltDegrees: Double = 0.0,
    var heading: Double = 0.0,
    var vibration: Double = 0.0,
    var vibrationRollingSum: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var peakVibrationShock: Double = 0.0,
    var peakVibrationShockTs: Long = 0L,
    var adaptiveVibrationFloor: Double = 0.0,
    var proxIdx: Double = 0.0,
    var proximityCm: Double = -1.0,
    var proximityDebounceMs: Long = 0L,
    var isNear: Boolean = true,
    var liftIdx: Double = 0.0,
    var tiltIdx: Double = 0.0
) {
    fun copyFrom(other: AtmosphericState) {
        this.temp = other.temp; this.maxTemp = other.maxTemp; this.baroAlt = other.baroAlt
        this.baroIdx = other.baroIdx; this.lux = other.lux; this.luxBaseline = other.luxBaseline
        this.luxIdx = other.luxIdx; this.acousticDb = other.acousticDb; this.acousticFloorDb = other.acousticFloorDb
        this.noiseIdx = other.noiseIdx; this.tiltDegrees = other.tiltDegrees; this.heading = other.heading
        this.vibration = other.vibration; this.vibrationRollingSum = other.vibrationRollingSum
        this.vibeIdx = other.vibeIdx; this.peakVibrationShock = other.peakVibrationShock
        this.peakVibrationShockTs = other.peakVibrationShockTs; this.adaptiveVibrationFloor = other.adaptiveVibrationFloor
        this.proxIdx = other.proxIdx; this.proximityCm = other.proximityCm
        this.proximityDebounceMs = other.proximityDebounceMs; this.isNear = other.isNear
        this.liftIdx = other.liftIdx; this.tiltIdx = other.tiltIdx
    }

    fun reset() {
        temp = 0.0; maxTemp = 0.0; baroAlt = 0.0; baroIdx = 0.0; lux = 0.0; luxBaseline = 0.0; luxIdx = 0.0
        acousticDb = 0.0; acousticFloorDb = 0.0; noiseIdx = 0.0; tiltDegrees = 0.0; heading = 0.0
        vibration = 0.0; vibrationRollingSum = 0.0; vibeIdx = 0.0; peakVibrationShock = 0.0
        peakVibrationShockTs = 0L; adaptiveVibrationFloor = 0.0; proxIdx = 0.0; proximityCm = -1.0
        proximityDebounceMs = 0L; isNear = true; liftIdx = 0.0; tiltIdx = 0.0
    }
}

/**
 * IntegrityState: Hardware, system health, and session audit telemetry.
 */
@Serializable
data class IntegrityState(
    var battery: Int = -1,
    var isCharging: Boolean = false,
    var currentMa: Int = 0,
    var satsView: Int = -1,
    var satsUsed: Int = -1,
    var snrIdx: Double = 0.0,
    var isTamperDetected: Boolean = false,
    var isPowerTamper: Boolean = false,
    var isJammer: Boolean = false,
    var isStalled: Boolean = false,
    var isSuspicious: Boolean = false,
    var isAnchorLocked: Boolean = false,
    var gpsHardwareLock: Boolean = false,
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var isUltraLongStationary: Boolean = false,
    var isGnssThrottled: Boolean = false,
    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L,
    var isBatterySteepDischarge: Boolean = false,
    var isPowerSaveMode: Boolean = false,
    var standbyBucket: Int = -1,
    var netInterface: String = "UNKNOWN",
    var isStorageLow: Boolean = false,
    var isStorageCritical: Boolean = false,
    var micPending: Boolean = false,
    var violationUptimeMs: Long = 0L,
    var violationPercentage: Double = 0.0,
    var uptimeMs: Long = 0L,
    var totalConnectedMs: Long = 0L,
    var sessionConnectedMs: Long = 0L,
    var lastConnTs: Long = 0L,
    var lastDiscTs: Long = 0L,
    var totalDropMs: Long = 0L,
    var maxDropMs: Long = 0L,
    var maxDropTs: Long = 0L,
    var gnssDetail: GnssDetail? = null,
    var isSitDetected: Boolean = false,
    var lastSitTs: Long = 0L,
    var sitVz: Double = 0.0,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,
    var isSitActive: Boolean = false,
    var isLocationPending: Boolean = false,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var signal: Int? = null,
    var tamperNote: String? = null,
    var lastValidFixRt: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isMaliAnomaly: Boolean = false,
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,
    var isBatteryWhitelisted: Boolean = false,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0
) {
    fun copyFrom(other: IntegrityState) {
        this.battery = other.battery; this.isCharging = other.isCharging; this.currentMa = other.currentMa
        this.satsView = other.satsView; this.satsUsed = other.satsUsed; this.snrIdx = other.snrIdx
        this.isTamperDetected = other.isTamperDetected; this.isPowerTamper = other.isPowerTamper
        this.isJammer = other.isJammer; this.isStalled = other.isStalled; this.isSuspicious = other.isSuspicious
        this.isAnchorLocked = other.isAnchorLocked; this.gpsHardwareLock = other.gpsHardwareLock
        this.isBatteryLow = other.isBatteryLow; this.isBatteryCritical = other.isBatteryCritical
        this.isCoolingModeActive = other.isCoolingModeActive; this.isUltraLongStationary = other.isUltraLongStationary
        this.isGnssThrottled = other.isGnssThrottled; this.isBatterySteepDischarge = other.isBatterySteepDischarge
        this.isPowerSaveMode = other.isPowerSaveMode; this.standbyBucket = other.standbyBucket
        this.netInterface = other.netInterface; this.isStorageLow = other.isStorageLow
        this.isStorageCritical = other.isStorageCritical; this.micPending = other.micPending
        this.violationUptimeMs = other.violationUptimeMs; this.violationPercentage = other.violationPercentage
        this.uptimeMs = other.uptimeMs; this.totalConnectedMs = other.totalConnectedMs
        this.sessionConnectedMs = other.sessionConnectedMs; this.lastConnTs = other.lastConnTs
        this.lastDiscTs = other.lastDiscTs; this.totalDropMs = other.totalDropMs
        this.maxDropMs = other.maxDropMs; this.maxDropTs = other.maxDropTs
        this.lastEnergyDeltaMa = other.lastEnergyDeltaMa; this.lastEnergyDeltaTemp = other.lastEnergyDeltaTemp
        this.lastEnergyDurationMs = other.lastEnergyDurationMs; this.gnssDetail = other.gnssDetail
        this.isSitDetected = other.isSitDetected; this.lastSitTs = other.lastSitTs
        this.sitVz = other.sitVz; this.sitVzTs = other.sitVzTs; this.sitVzRt = other.sitVzRt
        this.sitDz = other.sitDz; this.sitBaro = other.sitBaro; this.sitTilt = other.sitTilt
        this.sitShock = other.sitShock; this.isSitActive = other.isSitActive
        this.isLocationPending = other.isLocationPending; this.locationPendingReason = other.locationPendingReason
        this.signal = other.signal; this.tamperNote = other.tamperNote
        this.lastValidFixRt = other.lastValidFixRt
        this.isSilentFailure = other.isSilentFailure
        this.isMaliAnomaly = other.isMaliAnomaly
        this.cpuLoad = other.cpuLoad
        this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency
        this.isBatteryWhitelisted = other.isBatteryWhitelisted
        this.thermalHeadroom = other.thermalHeadroom
        this.heapAllocatedMb = other.heapAllocatedMb
    }

    fun reset() {
        battery = -1; isCharging = false; currentMa = 0; satsView = -1; satsUsed = -1; snrIdx = 0.0
        isTamperDetected = false; isPowerTamper = false; isJammer = false; isStalled = false
        isSuspicious = false; isAnchorLocked = false; gpsHardwareLock = false; isBatteryLow = false
        isBatteryCritical = false; isCoolingModeActive = false; isUltraLongStationary = false
        isGnssThrottled = false; isBatterySteepDischarge = false; isPowerSaveMode = false
        standbyBucket = -1; netInterface = "UNKNOWN"; isStorageLow = false; isStorageCritical = false
        micPending = false; violationUptimeMs = 0L; violationPercentage = 0.0; uptimeMs = 0L
        totalConnectedMs = 0L; sessionConnectedMs = 0L; lastConnTs = 0L; lastDiscTs = 0L
        totalDropMs = 0L; maxDropMs = 0L; maxDropTs = 0L; lastEnergyDeltaMa = 0; lastEnergyDeltaTemp = 0.0
        lastEnergyDurationMs = 0L; gnssDetail = null; isSitDetected = false; lastSitTs = 0L
        sitVz = 0.0; sitVzTs = 0L; sitVzRt = 0L; sitDz = 0.0; sitBaro = 0.0; sitTilt = 0.0
        sitShock = 0.0; isSitActive = false; isLocationPending = false
        locationPendingReason = LocationPendingReason.NONE; signal = null; tamperNote = null
        lastValidFixRt = 0L; isSilentFailure = false; isMaliAnomaly = false; cpuLoad = 0.0
        ioWait = 0.0; maxIoLatency = 0L; isBatteryWhitelisted = false
        thermalHeadroom = 0.0; heapAllocatedMb = 0.0
    }
}

/**
 * LocationUpdate: Aggregated telemetry container (Unified Monolith).
 * Oct.3.1:
 * - Issue #1420: Granular HUD Binding. Implements Locatable, BatteryProvider, 
 *   and DeviceIdentity interfaces to allow UI slicing and reduce coupling.
 * Oct.2.9:
 * - Issue #1314: TrackerStatus Convergence. Merged TrackerStatus into 
 *   LocationUpdate to eliminate mapping layers. Added deviceId, viewerId, 
 *   rt, and convenience getters for signaling compatibility. Implements SpatialAnchor.
 * - Restored Mutability: Converted convenience properties to var with custom setters 
 *   to allow engine-direct assignment on flyweights.
 */
@Serializable
data class LocationUpdate(
    val kinetic: KineticState = KineticState(),
    val atmospheric: AtmosphericState = AtmosphericState(),
    val integrity: IntegrityState = IntegrityState(),
    var status: SentinelStatus = SentinelStatus.VALID,
    override var ts: Long = 0L,
    override var rt: Long = 0L,
    var isMe: Boolean = true,
    var deviceId: String = "",
    override var viewerId: String = "",
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var isClockRegression: Boolean = false,
    var lastValidFixRt: Long = 0L,

    // --- Merged Engine Evaluation Fields ---
    var nowRt: Long = 0L,
    var nowTs: Long = 0L,
    var isMuzzled: Boolean = false,
    var isWarming: Boolean = false,
    var isSirenActive: Boolean = false,
    var isHardwareOnline: Boolean = true,
    var localInternetLoss: Boolean = false,
    var acousticLockoutRt: Long = 0L,
    var lightSpikeRt: Long = 0L,
    var providedAdaptiveFloor: Double = -1.0,
    var acousticMinDb: Double = -1.0,
    var lastAlarmAckTs: Long = 0L,
    var violationStartTs: Long = 0L,

    // Evaluation Scratchpad Fields
    var snrSnapshot: Double? = null,
    var vibeSnapshot: Double? = null,
    
    // Engine-Direct Flags (Converged)
    var suppressionNote: String? = null
) : SpatialAnchor, Locatable, BatteryProvider, DeviceIdentity {

    override var lat: Double 
        get() = kinetic.lat
        set(value) { kinetic.lat = value }
    override var lng: Double 
        get() = kinetic.lng
        set(value) { kinetic.lng = value }
    override var alt: Double 
        get() = kinetic.alt
        set(value) { kinetic.alt = value }
    override var gpsTs: Long 
        get() = kinetic.gpsTs
        set(value) { kinetic.gpsTs = value }

    var speed: Double 
        get() = kinetic.speed
        set(value) { kinetic.speed = value }
    var bearing: Double 
        get() = kinetic.bearing
        set(value) { kinetic.bearing = value }
    var accuracy: Double 
        get() = kinetic.accuracy
        set(value) { kinetic.accuracy = value }
    var maxAccuracy: Double 
        get() = kinetic.maxAccuracy
        set(value) { kinetic.maxAccuracy = value }

    override var battery: Int 
        get() = integrity.battery
        set(value) { integrity.battery = value }
    var temp: Double 
        get() = atmospheric.temp
        set(value) { atmospheric.temp = value }
    var maxTemp: Double 
        get() = atmospheric.maxTemp
        set(value) { atmospheric.maxTemp = value }
    override var isCharging: Boolean 
        get() = integrity.isCharging
        set(value) { integrity.isCharging = value }
    var currentMa: Int 
        get() = integrity.currentMa
        set(value) { integrity.currentMa = value }
    var satsView: Int 
        get() = integrity.satsView
        set(value) { integrity.satsView = value }
    var satsUsed: Int 
        get() = integrity.satsUsed
        set(value) { integrity.satsUsed = value }

    var peakVibrationShock: Double 
        get() = atmospheric.peakVibrationShock
        set(value) { atmospheric.peakVibrationShock = value }
    var peakVibrationShockTs: Long 
        get() = atmospheric.peakVibrationShockTs
        set(value) { atmospheric.peakVibrationShockTs = value }
    var isPowerTamper: Boolean 
        get() = integrity.isPowerTamper
        set(value) { integrity.isPowerTamper = value }
    var violationUptimeMs: Long 
        get() = integrity.violationUptimeMs
        set(value) { integrity.violationUptimeMs = value }
    
    var vibration: Double 
        get() = atmospheric.vibration
        set(value) { atmospheric.vibration = value }
    var heading: Double 
        get() = atmospheric.heading
        set(value) { atmospheric.heading = value }
    var tiltDegrees: Double 
        get() = atmospheric.tiltDegrees
        set(value) { atmospheric.tiltDegrees = value }
    var acousticDb: Double 
        get() = atmospheric.acousticDb
        set(value) { atmospheric.acousticDb = value }
    var baroAlt: Double 
        get() = atmospheric.baroAlt
        set(value) { atmospheric.baroAlt = value }
    var lux: Double 
        get() = atmospheric.lux
        set(value) { atmospheric.lux = value }
    var isNear: Boolean 
        get() = atmospheric.isNear
        set(value) { atmospheric.isNear = value }
    var luxBaseline: Double 
        get() = atmospheric.luxBaseline
        set(value) { atmospheric.luxBaseline = value }
    var acousticFloorDb: Double 
        get() = atmospheric.acousticFloorDb
        set(value) { atmospheric.acousticFloorDb = value }
    var adaptiveVibrationFloor: Double 
        get() = atmospheric.adaptiveVibrationFloor
        set(value) { atmospheric.adaptiveVibrationFloor = value }
    var proxIdx: Double 
        get() = atmospheric.proxIdx
        set(value) { atmospheric.proxIdx = value }
    var proximityCm: Double 
        get() = atmospheric.proximityCm
        set(value) { atmospheric.proximityCm = value }
    var proximityDebounceMs: Long 
        get() = atmospheric.proximityDebounceMs
        set(value) { atmospheric.proximityDebounceMs = value }
    var vibrationRollingSum: Double 
        get() = atmospheric.vibrationRollingSum
        set(value) { atmospheric.vibrationRollingSum = value }

    override var isLocationPending: Boolean 
        get() = integrity.isLocationPending
        set(value) { integrity.isLocationPending = value }
    override var locationPendingReason: LocationPendingReason 
        get() = integrity.locationPendingReason
        set(value) { integrity.locationPendingReason = value }

    var isPowerSaveMode: Boolean 
        get() = integrity.isPowerSaveMode
        set(value) { integrity.isPowerSaveMode = value }
    var standbyBucket: Int 
        get() = integrity.standbyBucket
        set(value) { integrity.standbyBucket = value }
    var netInterface: String 
        get() = integrity.netInterface
        set(value) { integrity.netInterface = value }
    var isStorageLow: Boolean 
        get() = integrity.isStorageLow
        set(value) { integrity.isStorageLow = value }
    var isStorageCritical: Boolean 
        get() = integrity.isStorageCritical
        set(value) { integrity.isStorageCritical = value }
    var isBatterySteepDischarge: Boolean 
        get() = integrity.isBatterySteepDischarge
        set(value) { integrity.isBatterySteepDischarge = value }
    var isCoolingModeActive: Boolean 
        get() = integrity.isCoolingModeActive
        set(value) { integrity.isCoolingModeActive = value }

    var snrIdx: Double 
        get() = integrity.snrIdx
        set(value) { integrity.snrIdx = value }
    var noiseIdx: Double 
        get() = atmospheric.noiseIdx
        set(value) { atmospheric.noiseIdx = value }
    var luxIdx: Double 
        get() = atmospheric.luxIdx
        set(value) { atmospheric.luxIdx = value }
    var vibeIdx: Double 
        get() = atmospheric.vibeIdx
        set(value) { atmospheric.vibeIdx = value }
    var liftIdx: Double 
        get() = atmospheric.liftIdx
        set(value) { atmospheric.liftIdx = value }
    var tiltIdx: Double 
        get() = atmospheric.tiltIdx
        set(value) { atmospheric.tiltIdx = value }
    var baroIdx: Double 
        get() = atmospheric.baroIdx
        set(value) { atmospheric.baroIdx = value }

    var isSitDetected: Boolean 
        get() = integrity.isSitDetected
        set(value) { integrity.isSitDetected = value }
    var isSitActive: Boolean 
        get() = integrity.isSitActive
        set(value) { integrity.isSitActive = value }
    var lastSitTs: Long 
        get() = integrity.lastSitTs
        set(value) { integrity.lastSitTs = value }
    var verticalVelocity: Double 
        get() = kinetic.verticalVelocity
        set(value) { kinetic.verticalVelocity = value }
    var sitVz: Double 
        get() = integrity.sitVz
        set(value) { integrity.sitVz = value }
    var sitVzTs: Long 
        get() = integrity.sitVzTs
        set(value) { integrity.sitVzTs = value }
    var sitVzRt: Long 
        get() = integrity.sitVzRt
        set(value) { integrity.sitVzRt = value }
    var sitDz: Double 
        get() = integrity.sitDz
        set(value) { integrity.sitDz = value }
    var sitBaro: Double 
        get() = integrity.sitBaro
        set(value) { integrity.sitBaro = value }
    var sitTilt: Double 
        get() = integrity.sitTilt
        set(value) { integrity.sitTilt = value }
    var sitShock: Double 
        get() = integrity.sitShock
        set(value) { integrity.sitShock = value }
    var isJump: Boolean 
        get() = kinetic.isJump
        set(value) { kinetic.isJump = value }
    var isTrajectoryPromoted: Boolean 
        get() = kinetic.isTrajectoryPromoted
        set(value) { kinetic.isTrajectoryPromoted = value }
    var isUltraLongStationary: Boolean 
        get() = integrity.isUltraLongStationary
        set(value) { integrity.isUltraLongStationary = value }
    var gpsHardwareLock: Boolean 
        get() = integrity.gpsHardwareLock
        set(value) { integrity.gpsHardwareLock = value }
    var isGnssThrottled: Boolean 
        get() = integrity.isGnssThrottled
        set(value) { integrity.isGnssThrottled = value }
    var kineticEnergy: Double 
        get() = kinetic.kineticEnergy
        set(value) { kinetic.kineticEnergy = value }

    var isBatteryLow: Boolean 
        get() = integrity.isBatteryLow
        set(value) { integrity.isBatteryLow = value }
    var isBatteryCritical: Boolean 
        get() = integrity.isBatteryCritical
        set(value) { integrity.isBatteryCritical = value }
    var isSuspicious: Boolean 
        get() = integrity.isSuspicious
        set(value) { integrity.isSuspicious = value }
    var micPending: Boolean 
        get() = integrity.micPending
        set(value) { integrity.micPending = value }
    var lastEnergyDeltaMa: Int 
        get() = integrity.lastEnergyDeltaMa
        set(value) { integrity.lastEnergyDeltaMa = value }
    var lastEnergyDeltaTemp: Double 
        get() = integrity.lastEnergyDeltaTemp
        set(value) { integrity.lastEnergyDeltaTemp = value }
    var lastEnergyDurationMs: Long 
        get() = integrity.lastEnergyDurationMs
        set(value) { integrity.lastEnergyDurationMs = value }
    var currentProximityCm: Double 
        get() = atmospheric.proximityCm
        set(value) { atmospheric.proximityCm = value }
    var violationPercentage: Double 
        get() = integrity.violationPercentage
        set(value) { integrity.violationPercentage = value }
    var gnssDetail: GnssDetail? 
        get() = integrity.gnssDetail
        set(value) { integrity.gnssDetail = value }
    var uptimeMs: Long 
        get() = integrity.uptimeMs
        set(value) { integrity.uptimeMs = value }
    var lastConnTs: Long 
        get() = integrity.lastConnTs
        set(value) { integrity.lastConnTs = value }
    var lastDiscTs: Long 
        get() = integrity.lastDiscTs
        set(value) { integrity.lastDiscTs = value }
    var totalDropMs: Long 
        get() = integrity.totalDropMs
        set(value) { integrity.totalDropMs = value }
    var maxDropMs: Long 
        get() = integrity.maxDropMs
        set(value) { integrity.maxDropMs = value }
    var maxDropTs: Long 
        get() = integrity.maxDropTs
        set(value) { integrity.maxDropTs = value }
    var totalConnectedMs: Long 
        get() = integrity.totalConnectedMs
        set(value) { integrity.totalConnectedMs = value }
    var sessionConnectedMs: Long 
        get() = integrity.sessionConnectedMs
        set(value) { integrity.sessionConnectedMs = value }
    var tamperNote: String? 
        get() = integrity.tamperNote
        set(value) { integrity.tamperNote = value }
    
    var isStalled: Boolean 
        get() = integrity.isStalled
        set(value) { integrity.isStalled = value }
    var isJammer: Boolean 
        get() = integrity.isJammer
        set(value) { integrity.isJammer = value }
    var tamperDetected: Boolean 
        get() = integrity.isTamperDetected
        set(value) { integrity.isTamperDetected = value }
    var jammerDetected: Boolean 
        get() = integrity.isJammer
        set(value) { integrity.isJammer = value }
    var isAnchorLocked: Boolean 
        get() = integrity.isAnchorLocked
        set(value) { integrity.isAnchorLocked = value }
    var cpuLoad: Double 
        get() = integrity.cpuLoad
        set(value) { integrity.cpuLoad = value }
    var ioWait: Double 
        get() = integrity.ioWait
        set(value) { integrity.ioWait = value }
    var maxIoLatency: Long 
        get() = integrity.maxIoLatency
        set(value) { integrity.maxIoLatency = value }
    var isSilentFailure: Boolean 
        get() = integrity.isSilentFailure
        set(value) { integrity.isSilentFailure = value }
    var isMaliAnomaly: Boolean 
        get() = integrity.isMaliAnomaly
        set(value) { integrity.isMaliAnomaly = value }
    var thermalHeadroom: Double 
        get() = integrity.thermalHeadroom
        set(value) { integrity.thermalHeadroom = value }
    var heapAllocatedMb: Double 
        get() = integrity.heapAllocatedMb
        set(value) { integrity.heapAllocatedMb = value }
    var activityType: ActivityType 
        get() = kinetic.activityType
        set(value) { kinetic.activityType = value }
    var isBatteryWhitelisted: Boolean 
        get() = integrity.isBatteryWhitelisted
        set(value) { integrity.isBatteryWhitelisted = value }
    var jumpTier: Int 
        get() = kinetic.jumpTier
        set(value) { kinetic.jumpTier = value }
    var isAdaptiveJump: Boolean
        get() = kinetic.isAdaptiveJump
        set(value) { kinetic.isAdaptiveJump = value }

    override var trackerId: String 
        get() = deviceId
        set(value) { deviceId = value }

    fun copyFrom(other: LocationUpdate) {
        this.kinetic.copyFrom(other.kinetic)
        this.atmospheric.copyFrom(other.atmospheric)
        this.integrity.copyFrom(other.integrity)
        this.status = other.status
        this.ts = other.ts
        this.rt = other.rt
        this.isMe = other.isMe
        this.deviceId = other.deviceId
        this.viewerId = other.viewerId
        this.trackerState = other.trackerState
        this.isClockRegression = other.isClockRegression
        this.lastValidFixRt = other.lastValidFixRt
        
        this.nowRt = other.nowRt
        this.nowTs = other.nowTs
        this.isMuzzled = other.isMuzzled
        this.isWarming = other.isWarming
        this.isSirenActive = other.isSirenActive
        this.isHardwareOnline = other.isHardwareOnline
        this.localInternetLoss = other.localInternetLoss
        this.acousticLockoutRt = other.acousticLockoutRt
        this.lightSpikeRt = other.lightSpikeRt
        this.providedAdaptiveFloor = other.providedAdaptiveFloor
        this.acousticMinDb = other.acousticMinDb
        this.lastAlarmAckTs = other.lastAlarmAckTs
        this.violationStartTs = other.violationStartTs
        this.snrSnapshot = other.snrSnapshot
        this.vibeSnapshot = other.vibeSnapshot
        this.suppressionNote = other.suppressionNote
    }

    /**
     * duplicate: Performs a deep copy to ensure thread safety during event emission (R-ID 392).
     */
    fun duplicate(): LocationUpdate = copy(
        kinetic = kinetic.copy(),
        atmospheric = atmospheric.copy(),
        integrity = integrity.copy()
    )

    fun reset() {
        kinetic.reset()
        atmospheric.reset()
        integrity.reset()
        status = SentinelStatus.VALID
        ts = 0L
        rt = 0L
        isMe = true
        deviceId = ""
        viewerId = ""
        trackerState = TrackerState.UNKNOWN
        isClockRegression = false
        lastValidFixRt = 0L
        
        nowRt = 0L
        nowTs = 0L
        isMuzzled = false
        isWarming = false
        isSirenActive = false
        isHardwareOnline = true
        localInternetLoss = false
        acousticLockoutRt = 0L
        lightSpikeRt = 0L
        providedAdaptiveFloor = -1.0
        acousticMinDb = -1.0
        lastAlarmAckTs = 0L
        violationStartTs = 0L
        snrSnapshot = null
        vibeSnapshot = null
        suppressionNote = null
    }

    fun toMap(fromViewer: Boolean): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
        put("id", SignalingConstants.getTransmissionId(deviceId))
        put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
        put("from_viewer", fromViewer)
        put("lat", lat); put("lng", lng); put("alt", alt)
        put("speed", speed); put("bearing", bearing); put("accuracy", accuracy); put("max_accuracy", maxAccuracy)
        put("gps_ts", gpsTs); put("ts", ts); put("rt", rt); put("uptime_ms", uptimeMs)
        put("last_conn_ts", lastConnTs); put("last_disc_ts", lastDiscTs)
        put("total_drop_ms", totalDropMs); put("max_drop_ms", maxDropMs); put("max_drop_ts", maxDropTs)
        put("total_connected_ms", totalConnectedMs); put("session_connected_ms", sessionConnectedMs)
        put("battery", battery); put("temp", temp); put("max_temp", maxTemp); put("is_charging", isCharging); put("current_ma", currentMa)
        put("sats_view", satsView); put("sats_used", satsUsed); put("peak_vibration_shock", peakVibrationShock)
        put("peak_shock_ts", peakVibrationShockTs); put("is_power_tamper", isPowerTamper)
        put("violation_uptime_ms", violationUptimeMs); put("violation_percentage", violationPercentage)
        put("status", status.name); put("is_jammer", isJammer); put("is_stalled", isStalled)
        put("is_tamper_detected", tamperDetected); put("vibration", vibration); put("heading", heading); put("tilt_degrees", tiltDegrees)
        put("acoustic_db", acousticDb); put("baro_alt", baroAlt); put("lux", lux); put("is_near", isNear)
        put("lux_baseline", luxBaseline); put("acoustic_floor_db", acousticFloorDb); put("adaptiveVibrationFloor", adaptiveVibrationFloor)
        put("prox_idx", proxIdx); put("proximity_cm", proximityCm); put("proximity_debounce_ms", proximityDebounceMs)
        put("vibration_rolling_sum", vibrationRollingSum); put("is_clock_regression", isClockRegression)
        put("jump_tier", jumpTier); put("is_location_pending", isLocationPending)
        put("location_pending_reason", locationPendingReason.name)
        put("last_valid_fix_rt", lastValidFixRt); put("is_power_save_mode", isPowerSaveMode)
        put("standby_bucket", standbyBucket)
        put("net_interface", netInterface)
        put("is_storage_low", isStorageLow); put("is_storage_critical", isStorageCritical)
        put("is_battery_steep_discharge", isBatterySteepDischarge); put("is_cooling_mode_active", isCoolingModeActive)
        put("tracker_state", trackerState.name); put("is_sit_detected", isSitDetected); put("last_sit_ts", lastSitTs)
        put("is_jump", isJump); put("mic_pending", micPending); put("snr_idx", snrIdx); put("noise_idx", noiseIdx)
        put("lux_idx", luxIdx); put("vibe_idx", vibeIdx); put("lift_idx", liftIdx)
        put("tilt_idx", tiltIdx); put("baro_idx", baroIdx); put("is_sit_active", isSitActive)
        put("sit_vz", sitVz); put("sit_vz_ts", integrity.sitVzTs); put("sit_vz_rt", integrity.sitVzRt)
        put("sit_dz", sitDz); put("sit_baro", sitBaro); put("sit_tilt", sitTilt); put("sit_shock", sitShock)
        put("vertical_velocity", verticalVelocity); put("kinetic_energy", kineticEnergy)
        put("is_adaptive_jump", isAdaptiveJump); put("is_battery_low", isBatteryLow)
        put("is_battery_critical", isBatteryCritical); put("is_silent_failure", isSilentFailure)
        put("is_battery_whitelisted", isBatteryWhitelisted)
        put("is_ultra_long_stationary", isUltraLongStationary)
        put("gps_hardware_lock", gpsHardwareLock); put("is_gnss_throttled", isGnssThrottled)
        put("activity_type", activityType.name)
        put("last_energy_delta_ma", lastEnergyDeltaMa)
        put("last_energy_delta_temp", lastEnergyDeltaTemp)
        put("last_energy_durationMs", lastEnergyDurationMs)
        put("tamper_note", tamperNote)
        put("thermal_headroom", thermalHeadroom)
        put("heap_allocated_mb", heapAllocatedMb)
        put("last_alarm_ack_ts", lastAlarmAckTs)
        put("violation_start_ts", violationStartTs)
    }

    companion object {
        fun mapProtoToPendingReason(proto: String): LocationPendingReason {
            return try { LocationPendingReason.valueOf(proto) } catch (e: Exception) { LocationPendingReason.NONE }
        }
        fun mapProtoToTrackerState(proto: String): TrackerState {
            return try { TrackerState.valueOf(proto) } catch (e: Exception) { TrackerState.UNKNOWN }
        }
    }
}
