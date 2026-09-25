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
    var distToHome: Double? = null
) {
    fun copyFrom(other: KineticState) {
        this.lat = other.lat; this.lng = other.lng; this.alt = other.alt; this.speed = other.speed
        this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy; this.bearing = other.bearing
        this.gpsTs = other.gpsTs; this.rt = other.rt; this.isJump = other.isJump
        this.isTrajectoryPromoted = other.isTrajectoryPromoted; this.jumpTier = other.jumpTier
        this.isAdaptiveJump = other.isAdaptiveJump; this.verticalVelocity = other.verticalVelocity
        this.kineticEnergy = other.kineticEnergy; this.distToTracker = other.distToTracker
        this.distToHome = other.distToHome
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
    var tamperNote: String? = null
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
    }
}

/**
 * LocationUpdate: Aggregated telemetry container.
 * Sep.25.05:
 * - Issue #1330: Snap-to-Update Monolith. Hardened copyFrom methods for 
 *   sub-states to ensure zero-allocation StateFlow propagation.
 */
@Serializable
class LocationUpdate(
    var kinetic: KineticState = KineticState(),
    var atmospheric: AtmosphericState = AtmosphericState(),
    var integrity: IntegrityState = IntegrityState(),
    var status: SentinelStatus = SentinelStatus.VALID,
    var ts: Long = 0L,
    var isMe: Boolean = true,
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var isClockRegression: Boolean = false,
    var lastValidFixRt: Long = 0L
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
    }
}
