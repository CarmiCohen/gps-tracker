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
)

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
)

/**
 * IntegrityState: Hardware, system health, and session audit telemetry.
 */
@Serializable
data class IntegrityState(
    var battery: Int = -1,
    var isCharging: Boolean = false,
    var currentMa: Int = 0,
    var satsView: Int = 0,
    var satsUsed: Int = 0,
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
    var isSitActive: Boolean = false
)

/**
 * LocationUpdate: Aggregated telemetry container.
 * Sep.08.20:
 * - Idea #2 RESOLVED: Telemetry Model Flattening. Partitioned monolithic fields 
 *   into Kinetic, Atmospheric, and Integrity sub-states (R-ID 284).
 * - Legacy Mapping Helpers: Added accessors to simplify build restoration.
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
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var isClockRegression: Boolean = false,
    var lastValidFixRt: Long = 0L
) {
    // --- Legacy Mapping Helpers (Bridge for R-ID 284) ---
    var lat: Double get() = kinetic.lat; set(value) { kinetic.lat = value }
    var lng: Double get() = kinetic.lng; set(value) { kinetic.lng = value }
    var alt: Double get() = kinetic.alt; set(value) { kinetic.alt = value }
    var speed: Double get() = kinetic.speed; set(value) { kinetic.speed = value }
    var accuracy: Double get() = kinetic.accuracy; set(value) { kinetic.accuracy = value }
    var maxAccuracy: Double get() = kinetic.maxAccuracy; set(value) { kinetic.maxAccuracy = value }
    var bearing: Double get() = kinetic.bearing; set(value) { kinetic.bearing = value }
    var gpsTs: Long get() = kinetic.gpsTs; set(value) { kinetic.gpsTs = value }
    var rt: Long get() = kinetic.rt; set(value) { kinetic.rt = value }
    
    var temp: Double get() = atmospheric.temp; set(value) { atmospheric.temp = value }
    var maxTemp: Double get() = atmospheric.maxTemp; set(value) { atmospheric.maxTemp = value }
    var vibration: Double get() = atmospheric.vibration; set(value) { atmospheric.vibration = value }
    var heading: Double get() = atmospheric.heading; set(value) { atmospheric.heading = value }
    var baroAlt: Double get() = atmospheric.baroAlt; set(value) { atmospheric.baroAlt = value }
    var baroIdx: Double get() = atmospheric.baroIdx; set(value) { atmospheric.baroIdx = value }
    var lux: Double get() = atmospheric.lux; set(value) { atmospheric.lux = value }
    var luxIdx: Double get() = atmospheric.luxIdx; set(value) { atmospheric.luxIdx = value }
    var luxBaseline: Double get() = atmospheric.luxBaseline; set(value) { atmospheric.luxBaseline = value }
    var noiseIdx: Double get() = atmospheric.noiseIdx; set(value) { atmospheric.noiseIdx = value }
    var isNear: Boolean get() = atmospheric.isNear; set(value) { atmospheric.isNear = value }
    var tiltDegrees: Double get() = atmospheric.tiltDegrees; set(value) { atmospheric.tiltDegrees = value }
    var acousticDb: Double get() = atmospheric.acousticDb; set(value) { atmospheric.acousticDb = value }
    var acousticFloorDb: Double get() = atmospheric.acousticFloorDb; set(value) { atmospheric.acousticFloorDb = value }
    var adaptiveVibrationFloor: Double get() = atmospheric.adaptiveVibrationFloor; set(value) { atmospheric.adaptiveVibrationFloor = value }
    var peakVibrationShock: Double get() = atmospheric.peakVibrationShock; set(value) { atmospheric.peakVibrationShock = value }
    var peakVibrationShockTs: Long get() = atmospheric.peakVibrationShockTs; set(value) { atmospheric.peakVibrationShockTs = value }
    var liftIdx: Double get() = atmospheric.liftIdx; set(value) { atmospheric.liftIdx = value }
    var tiltIdx: Double get() = atmospheric.tiltIdx; set(value) { atmospheric.tiltIdx = value }
    var vibeIdx: Double get() = atmospheric.vibeIdx; set(value) { atmospheric.vibeIdx = value }
    var proxIdx: Double get() = atmospheric.proxIdx; set(value) { atmospheric.proxIdx = value }
    var proximityCm: Double get() = atmospheric.proximityCm; set(value) { atmospheric.proximityCm = value }
    var proximityDebounceMs: Long get() = atmospheric.proximityDebounceMs; set(value) { atmospheric.proximityDebounceMs = value }
    var vibrationRollingSum: Double get() = atmospheric.vibrationRollingSum; set(value) { atmospheric.vibrationRollingSum = value }
    
    var battery: Int get() = integrity.battery; set(value) { integrity.battery = value }
    var isCharging: Boolean get() = integrity.isCharging; set(value) { integrity.isCharging = value }
    var currentMa: Int get() = integrity.currentMa; set(value) { integrity.currentMa = value }
    var satsView: Int get() = integrity.satsView; set(value) { integrity.satsView = value }
    var satsUsed: Int get() = integrity.satsUsed; set(value) { integrity.satsUsed = value }
    var snrIdx: Double get() = integrity.snrIdx; set(value) { integrity.snrIdx = value }
    var isTamperDetected: Boolean get() = integrity.isTamperDetected; set(value) { integrity.isTamperDetected = value }
    var isPowerTamper: Boolean get() = integrity.isPowerTamper; set(value) { integrity.isPowerTamper = value }
    var isSitDetected: Boolean get() = integrity.isSitDetected; set(value) { integrity.isSitDetected = value }
    var lastSitTs: Long get() = integrity.lastSitTs; set(value) { integrity.lastSitTs = value }
    var sitVz: Double get() = integrity.sitVz; set(value) { integrity.sitVz = value }
    var sitVzTs: Long get() = integrity.sitVzTs; set(value) { integrity.sitVzTs = value }
    var sitVzRt: Long get() = integrity.sitVzRt; set(value) { integrity.sitVzRt = value }
    var sitDz: Double get() = integrity.sitDz; set(value) { integrity.sitDz = value }
    var sitBaro: Double get() = integrity.sitBaro; set(value) { integrity.sitBaro = value }
    var sitTilt: Double get() = integrity.sitTilt; set(value) { integrity.sitTilt = value }
    var sitShock: Double get() = integrity.sitShock; set(value) { integrity.sitShock = value }
    var kineticEnergy: Double get() = kinetic.kineticEnergy; set(value) { kinetic.kineticEnergy = value }
    var isBatteryLow: Boolean get() = integrity.isBatteryLow; set(value) { integrity.isBatteryLow = value }
    var isBatteryCritical: Boolean get() = integrity.isBatteryCritical; set(value) { integrity.isBatteryCritical = value }
    var isStorageLow: Boolean get() = integrity.isStorageLow; set(value) { integrity.isStorageLow = value }
    var isStorageCritical: Boolean get() = integrity.isStorageCritical; set(value) { integrity.isStorageCritical = value }
    var isBatterySteepDischarge: Boolean get() = integrity.isBatterySteepDischarge; set(value) { integrity.isBatterySteepDischarge = value }
    var isCoolingModeActive: Boolean get() = integrity.isCoolingModeActive; set(value) { integrity.isCoolingModeActive = value }
    var isUltraLongStationary: Boolean get() = integrity.isUltraLongStationary; set(value) { integrity.isUltraLongStationary = value }
    var gpsHardwareLock: Boolean get() = integrity.gpsHardwareLock; set(value) { integrity.gpsHardwareLock = value }

    fun copyFrom(other: LocationUpdate) {
        this.kinetic = other.kinetic.copy()
        this.atmospheric = other.atmospheric.copy()
        this.integrity = other.integrity.copy()
        this.status = other.status
        this.ts = other.ts
        this.isMe = other.isMe
        this.trackerState = other.trackerState
        this.locationPendingReason = other.locationPendingReason
        this.isClockRegression = other.isClockRegression
        this.lastValidFixRt = other.lastValidFixRt
    }
}
