package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * July.30.49:
 * - Issue #653: Performance: Zero-Churn Refactoring. Converted SentinelResult, 
 *   JumpConfidence, and ProcessedLocation into mutable flyweights to eliminate 
 *   heap churn in GPS/Telemetry hot-paths (R-HARDWARE-01).
 * July.30.45:
 * - Issue #654: Performance Hardening. Added isMicrophoneGranted to HardwareCapabilities 
 *   to support centralized throttled auditing of hardware-bound permissions (R650).
 */

@Serializable
data class EngineGeoPoint(
    val lat: Double, 
    val lng: Double, 
    val alt: Double = 0.0,
    val ts: Long = 0L,
    val rt: Long = 0L,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0
)

@Serializable
enum class TrackerState { MOVING, PARKING, JUMPING, OFFLINE, UNKNOWN }

enum class DiscoveryPhase {
    BOOTSTRAP, DISCOVERING, MONITORING
}

enum class SentinelStatus {
    VALID, JUMP, TAMPER, TRAJECTORY_PROMOTED, OUTLIER, JITTER, JAMMER_SUSPICION
}

enum class CapabilityStatus {
    GRANTED, DENIED, UNKNOWN
}

@Serializable
data class HardwareCapabilities(
    val hasBackgroundRestriction: Boolean = false,
    val backgroundStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val autostartStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val requiresWakeLockRenewal: Boolean = false,
    val requiresExtraTopPadding: Boolean = false,
    val isManualOverrideActive: Boolean = false,
    val isA15Device: Boolean = false,
    val isMicrophoneGranted: Boolean = false
)

enum class LocationPendingReason {
    NONE,
    GPS_STALL,
    GPS_GAP,
    ACOUSTIC_VIOLATION,
    SIGNAL_LOSS,
    JAMMER_SUSPICION
}

/**
 * Issue #570: Refactored to mutable class for zero-churn forensics.
 */
@Serializable
class EngineConnectionPoint(
    var ts: Long = 0L,
    var rt: Long = 0L,
    var rtt: Int = 0,
    var remoteSig: Int = 0,
    var isConnected: Boolean = false,
    var isGap: Boolean = false,
    var isRecoveryEvent: Boolean = false,
    var hasGps: Boolean = false,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var isBatterySteepDischarge: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var speed: Double = 0.0,
    var bearing: Double = 0.0,
    var isTick: Boolean = false,
    var currentMa: Int = 0,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    
    // Forensic Indices
    var gpsIndex: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var proxIdx: Double = 1.0,
    var liftIdx: Double = 0.0,
    var snrIdx: Double = 0.0,
    var tiltIdx: Double = 0.0,
    var baroIdx: Double = 0.0,
    var isSitDetected: Boolean = false,
    var isSitActive: Boolean = false,
    var verticalVelocity: Double = 0.0,
    var sitVz: Double = 0.0,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,
    var kineticEnergy: Double = 0.0
) {
    fun copyFrom(other: EngineConnectionPoint) {
        this.ts = other.ts
        this.rt = other.rt
        this.rtt = other.rtt
        this.remoteSig = other.remoteSig
        this.isConnected = other.isConnected
        this.isGap = other.isGap
        this.isRecoveryEvent = other.isRecoveryEvent
        this.hasGps = other.hasGps
        this.accuracy = other.accuracy
        this.maxAccuracy = other.maxAccuracy
        this.isBatterySteepDischarge = other.isBatterySteepDischarge
        this.isCoolingModeActive = other.isCoolingModeActive
        this.speed = other.speed
        this.bearing = other.bearing
        this.isTick = other.isTick
        this.currentMa = other.currentMa
        this.locationPendingReason = other.locationPendingReason
        this.gpsIndex = other.gpsIndex
        this.noiseIdx = other.noiseIdx
        this.luxIdx = other.luxIdx
        this.vibeIdx = other.vibeIdx
        this.proxIdx = other.proxIdx
        this.liftIdx = other.liftIdx
        this.snrIdx = other.snrIdx
        this.tiltIdx = other.tiltIdx
        this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected
        this.isSitActive = other.isSitActive
        this.verticalVelocity = other.verticalVelocity
        this.sitVz = other.sitVz
        this.sitVzTs = other.sitVzTs
        this.sitVzRt = other.sitVzRt
        this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro
        this.sitTilt = other.sitTilt
        this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy
    }
}

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1),
    SIXTEEN_MIN("16M", 4),
    ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60),
    TWENTY_FOUR_HOUR("24H", 360),
    SEVEN_DAY("7D", 2700)
}

/**
 * Mutable flyweight for zero-churn SNR forensics.
 */
class EngineSnrSample(
    var ts: Long = 0L, 
    var rt: Long = 0L, 
    var snr: Double = 0.0
)

/**
 * Mutable flyweight for zero-churn sensor forensics.
 */
class EngineSensorSnapshot(
    var ts: Long = 0L,
    var rt: Long = 0L,
    var acoustic: Double = 0.0,
    var lux: Double = 0.0,
    var vibe: Double = 0.0,
    var proxIdx: Double = 1.0,
    var lift: Double = 0.0,
    var tilt: Double = 0.0,
    var isSitDetected: Boolean = false,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitShock: Double = 0.0,
    var kineticEnergy: Double = 0.0
) {
    fun copyFrom(other: EngineSensorSnapshot) {
        this.ts = other.ts
        this.rt = other.rt
        this.acoustic = other.acoustic
        this.lux = other.lux
        this.vibe = other.vibe
        this.proxIdx = other.proxIdx
        this.lift = other.lift
        this.tilt = other.tilt
        this.isSitDetected = other.isSitDetected
        this.sitVzTs = other.sitVzTs
        this.sitVzRt = other.sitVzRt
        this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy
    }
}

/**
 * Issue #653: Refactored to mutable class for zero-churn results.
 */
@Serializable
class SentinelResult(
    var status: SentinelStatus = SentinelStatus.VALID,
    var reason: String = "",
    var optimizedPoint: EngineGeoPoint? = null,
    var jumpConfidence: JumpConfidence? = null,
    var suppressionNote: String? = null,
    var promotedPoints: List<EngineGeoPoint>? = null
) {
    fun reset(status: SentinelStatus = SentinelStatus.VALID) {
        this.status = status
        this.reason = ""
        this.optimizedPoint = null
        this.jumpConfidence?.reset()
        this.suppressionNote = null
        this.promotedPoints = null
    }
}

/**
 * Issue #653: Refactored to mutable class for zero-churn results.
 */
@Serializable
class JumpConfidence(
    var score: Int = 0, 
    var isJump: Boolean = false,
    var isOutlier: Boolean = false,
    var tier: Int = 0, 
    var reason: String = "",
    var isAdaptiveJump: Boolean = false
) {
    fun reset() {
        score = 0
        isJump = false
        isOutlier = false
        tier = 0
        reason = ""
        isAdaptiveJump = false
    }

    fun copyFrom(other: JumpConfidence) {
        this.score = other.score
        this.isJump = other.isJump
        this.isOutlier = other.isOutlier
        this.tier = other.tier
        this.reason = other.reason
        this.isAdaptiveJump = other.isAdaptiveJump
    }
}

@Serializable
data class SatelliteInfo(
    val svid: Int,
    val cn0: Double,
    val usedInFix: Boolean,
    val constellation: Int
)

@Serializable
data class GnssDetail(
    val satellites: List<SatelliteInfo> = emptyList()
)

data class RejectedPoint(
    val lat: Double,
    val lng: Double,
    val alt: Double,
    val accuracy: Double,
    val bearing: Double,
    val speedMps: Double,
    val ts: Long,
    val rt: Long = 0L
)

interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long
    val rt: Long
}

@Serializable
data class ViolationReport(
    val type: String, 
    val title: String, 
    val subtitle: String, 
    val conditionMet: Boolean,
    val technicalDetails: String? = null, 
    val extremeValue: Double? = null
)

@Serializable
data class SystemHealthReport(val reports: List<ViolationReport>)

@Serializable
data class AlarmHistory(
    var powerAlarmPending: Boolean = false,
    var wasDistanceViolated: Boolean = false,
    var distanceViolationCounter: Int = 0,
    var firstViolationTs: Long = 0L,
    var firstViolationWasJump: Boolean = false
)

data class AlarmEvaluationState(
    val now: Long,
    val nowRt: Long,
    val serviceStartTime: Long, 
    val serviceStartRt: Long,
    val lastAlarmAckTs: Long, 
    val appStartTime: Long,
    val isRelayConnected: Boolean, 
    val isTrackerConnected: Boolean, 
    val discoveryPhase: DiscoveryPhase?,
    val trackerLat: Double, 
    val trackerLng: Double, 
    val trackerGpsAccuracy: Double,
    val maxTrackerAccuracy: Double,
    val lastGpsPacketTs: Long,
    val lastGpsPacketRt: Long = 0L,
    val trackerLastValidFixTs: Long = 0L, 
    val trackerLastValidFixRt: Long = 0L,
    val trackerSpeed: Double = 0.0,
    val jumpTier: Int = 0,
    val isAdaptiveJump: Boolean = false,
    val trackerBattery: Int, 
    val trackerTemp: Double,
    var wasDistanceViolated: Boolean = false, 
    var distanceViolationCounter: Int = 0, 
    var firstViolationTs: Long = 0L, 
    var firstViolationRt: Long = 0L,
    var firstViolationWasJump: Boolean = false,
    val homePoints: List<EngineGeoPoint> = emptyList(),
    val maxDistance: Double = 60.0,
    val distToHomeAuthority: Double? = null,
    val isGpsGap: Boolean = false,
    val trackerBaroAltEma: Double = 0.0,
    val isTrackerMode: Boolean = true,
    val health: SystemHealthState = SystemHealthState(),
    val capabilities: HardwareCapabilities = HardwareCapabilities()
)

/**
 * Issue #653: Refactored to mutable class for zero-churn results.
 */
class ProcessedLocation(
    var rawPoint: EngineGeoPoint = EngineGeoPoint(0.0, 0.0),
    var optimizedPoint: EngineGeoPoint = EngineGeoPoint(0.0, 0.0),
    var status: SentinelStatus = SentinelStatus.VALID,
    var maxAccuracy: Double = 0.0,
    var currentAccuracy: Double = 0.0,
    var filteredSpeed: Double = 0.0,
    var timestamp: Long = 0L,
    var rt: Long = 0L,
    var isStalled: Boolean = false,
    var isClockRegression: Boolean = false,
    var receiptRt: Long = 0L,
    var isTrajectoryPromoted: Boolean = false,
    var isAdaptiveJump: Boolean = false,
    var jumpTier: Int = 0,
    var distToHome: Double? = null,
    var isSpatiallyValid: Boolean = true,
    var geofenceViolationDetected: Boolean = false,
    var tamperDetected: Boolean = false,
    var jammerDetected: Boolean = false,
    var isAnchorLocked: Boolean = false,
    var suppressionNote: String? = null,
    var kineticEnergy: Double = 0.0
) {
    fun reset() {
        status = SentinelStatus.VALID
        maxAccuracy = 0.0
        currentAccuracy = 0.0
        filteredSpeed = 0.0
        timestamp = 0L
        rt = 0L
        isStalled = false
        isClockRegression = false
        receiptRt = 0L
        isTrajectoryPromoted = false
        isAdaptiveJump = false
        jumpTier = 0
        distToHome = null
        isSpatiallyValid = true
        geofenceViolationDetected = false
        tamperDetected = false
        jammerDetected = false
        isAnchorLocked = false
        suppressionNote = null
        kineticEnergy = 0.0
    }
}
