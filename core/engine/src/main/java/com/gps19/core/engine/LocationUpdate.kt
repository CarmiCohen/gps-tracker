package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * KineticState: Spatial and motion telemetry.
 * Sep.27.18:
 * - Issue #1205: Context-Aware Power Optimization. Added activityType field.
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
    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L,
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
 * LocationUpdate: Aggregated telemetry container.
 * Oct.2.8:
 * - Issue #1330: Snap-to-Update Monolith. Merged SystemEvaluationSnapshot 
 *   into LocationUpdate to eliminate bridge layer. Added engine evaluation 
 *   fields (nowRt, isMuzzled, isWarming, etc.) to support unified tick evaluation.
 */
@Serializable
data class LocationUpdate(
    val kinetic: KineticState = KineticState(),
    val atmospheric: AtmosphericState = AtmosphericState(),
    val integrity: IntegrityState = IntegrityState(),
    var status: SentinelStatus = SentinelStatus.VALID,
    var ts: Long = 0L,
    var isMe: Boolean = true,
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var isClockRegression: Boolean = false,
    var lastValidFixRt: Long = 0L,

    // --- Merged Engine Evaluation Fields (Issue #1330) ---
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

    // Evaluation Scratchpad Fields (Temporary redundancy for engine logic)
    var snrSnapshot: Double? = null,
    var vibeSnapshot: Double? = null,
    
    // Engine-Direct Flags (Legacy redundancy maintained for logic safety)
    var isStalled: Boolean = false,
    var isJammer: Boolean = false,
    var jumpTier: Int = 0,
    var isAdaptiveJump: Boolean = false,
    var tamperDetected: Boolean = false,
    var jammerDetected: Boolean = false,
    var isAnchorLocked: Boolean = false,
    var suppressionNote: String? = null,
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isMaliAnomaly: Boolean = false,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    var activityType: ActivityType = ActivityType.UNKNOWN
) {
    fun copyFrom(other: LocationUpdate) {
        this.kinetic.copyFrom(other.kinetic)
        this.atmospheric.copyFrom(other.atmospheric)
        this.integrity.copyFrom(other.integrity)
        this.status = other.status
        this.ts = other.ts
        this.isMe = other.isMe
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
        this.isStalled = other.isStalled
        this.isJammer = other.isJammer
        this.jumpTier = other.jumpTier
        this.isAdaptiveJump = other.isAdaptiveJump
        this.tamperDetected = other.tamperDetected
        this.jammerDetected = other.jammerDetected
        this.isAnchorLocked = other.isAnchorLocked
        this.suppressionNote = other.suppressionNote
        this.cpuLoad = other.cpuLoad
        this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency
        this.isSilentFailure = other.isSilentFailure
        this.isMaliAnomaly = other.isMaliAnomaly
        this.thermalHeadroom = other.thermalHeadroom
        this.heapAllocatedMb = other.heapAllocatedMb
        this.activityType = other.activityType
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
        isMe = true
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
        isStalled = false
        isJammer = false
        jumpTier = 0
        isAdaptiveJump = false
        tamperDetected = false
        jammerDetected = false
        isAnchorLocked = false
        suppressionNote = null
        cpuLoad = 0.0
        ioWait = 0.0
        maxIoLatency = 0L
        isSilentFailure = false
        isMaliAnomaly = false
        thermalHeadroom = 0.0
        heapAllocatedMb = 0.0
        activityType = ActivityType.UNKNOWN
    }
}
