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
    var isSitActive: Boolean = false,
    var isLocationPending: Boolean = false,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var signal: Int? = null,
    var tamperNote: String? = null
)

/**
 * LocationUpdate: Aggregated telemetry container.
 * Sep.10.40:
 * - Issue #946 Visibility: Added tamperNote to IntegrityState for 
 *   role-agnostic forensic transparency (R-ID 288).
 * Sep.09.10:
 * - Legacy Field Cleanup RESOLVED: Removed all bridge properties. 
 *   Consumers now use .kinetic, .atmospheric, and .integrity directly (R-ID 284).
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
        this.kinetic = other.kinetic.copy()
        this.atmospheric = other.atmospheric.copy()
        this.integrity = other.integrity.copy()
        this.status = other.status
        this.ts = other.ts
        this.isMe = other.isMe
        this.trackerState = other.trackerState
        this.isClockRegression = other.isClockRegression
        this.lastValidFixRt = other.lastValidFixRt
    }
}
