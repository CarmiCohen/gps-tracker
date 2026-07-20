package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * v9.4.00:
 * - Issue #102: Temporal Forensic Integrity. Added monotonic 'rt' (realtime) 
 *   timestamps to Geo and Connection points to ensure logic immunity to clock drifts.
 * v9.3.16:
 * - Requirement R999b: Added trackerBaroAltEma to AlarmEvaluationState to support 
 *   synchronized barometer violation detection.
 */

@Serializable
data class EngineGeoPoint(
    val lat: Double, 
    val lng: Double, 
    val alt: Double = 0.0,
    val ts: Long = 0L, // Wall-clock for forensics
    val rt: Long = 0L, // Monotonic for engine logic
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0
)

@Serializable
enum class TrackerState { MOVING, PARKING, JUMPING, OFFLINE, UNKNOWN }

enum class DiscoveryPhase {
    BOOTSTRAP, DISCOVERING, MONITORING
}

enum class SentinelStatus {
    VALID,
    JUMP,
    OUTLIER,
    JITTER,
    JAMMER_SUSPICION,
    TAMPER_ALERT,
    ACOUSTIC_WARNING,
    SENSOR_SUSPICIOUS,
    TRAJECTORY_PROMOTED
}

enum class EngineXiaomiStatus {
    GRANTED, DENIED, UNKNOWN
}

/**
 * LocationPendingReason: Contextual cause for Bayesian uncertainty expansion.
 * v9.2.2 (Issue #326)
 */
enum class LocationPendingReason {
    NONE,
    GPS_STALL,
    GPS_GAP,
    ACOUSTIC_VIOLATION,
    SIGNAL_LOSS,
    JAMMER_SUSPICION
}

/**
 * EngineConnectionPoint: Pure Kotlin representation of a forensic telemetry slice.
 */
@Serializable
data class EngineConnectionPoint(
    val ts: Long, // Wall-clock
    val rt: Long = 0L, // Monotonic (Issue #102)
    val rtt: Int,
    val remoteSig: Int,
    val isConnected: Boolean,
    val isGap: Boolean = false,
    val hasGps: Boolean = false,
    val gpsIndex: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val noiseIdx: Double = 0.0,
    val luxIdx: Double = 0.0,
    val vibeIdx: Double = 0.0,
    val proxIdx: Double = 1.0,
    val liftIdx: Double = 0.0,
    val snrIdx: Double = 0.0,
    val tiltIdx: Double = 0.0,
    val baroIdx: Double = 0.0,
    val verticalVelocity: Double = 0.0,
    val sitVz: Double = 0.0,
    val sitVzTs: Long = 0L,
    val sitDz: Double = 0.0,
    val sitBaro: Double = 0.0,
    val sitTilt: Double = 0.0,
    val sitShock: Double = 0.0,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val isTick: Boolean = false,
    val currentMa: Int = 0,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isAnchorLocked: Boolean = false
)

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1),
    SIXTEEN_MIN("16M", 4),
    ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60),
    TWENTY_FOUR_HOUR("24H", 360),
    SEVEN_DAY("7D", 2700)
}

/**
 * Sensor and SNR snapshots for gap filling.
 */
data class EngineSnrSample(val ts: Long, val rt: Long = 0L, val snr: Double)
data class EngineSensorSnapshot(
    val ts: Long,
    val rt: Long = 0L,
    val acoustic: Double,
    val lux: Double,
    val vibe: Double,
    val proxIdx: Double,
    val lift: Double,
    val tilt: Double,
    val isSitDetected: Boolean
)

@Serializable
data class SentinelResult(
    val status: SentinelStatus,
    val reason: String = "",
    val optimizedPoint: EngineGeoPoint? = null,
    val jumpConfidence: JumpConfidence? = null,
    val promotedPoints: List<EngineGeoPoint>? = null,
    val suppressionNote: String? = null
)

@Serializable
data class JumpConfidence(
    val score: Int = 0, 
    val isJump: Boolean = false,
    val isOutlier: Boolean = false,
    val isAdaptiveJump: Boolean = false,
    val tier: Int = 0, 
    val reason: String = ""
)

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

/**
 * RejectedPoint: Used for hindsight correction.
 */
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

/**
 * SpatialAnchor: Minimal interface for position recovery.
 */
interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long // Wall-clock
    val rt: Long // Monotonic
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

data class AlarmEvaluationState(
    val now: Long, // Wall-clock for server sync
    val nowRt: Long, // Monotonic for alarm logic (Issue #102)
    val serviceStartTime: Long, 
    val serviceStartRt: Long, // Monotonic
    val lastAlarmAckTs: Long, 
    val appStartTime: Long,
    val isRelayConnected: Boolean, 
    val isTrackerConnected: Boolean, 
    val discoveryPhase: DiscoveryPhase?,
    val isHardwareOnline: Boolean, 
    val isLocalInternetLoss: Boolean, 
    val isJammerSuspicion: Boolean,
    val isSignalLoss: Boolean, 
    val isGpsStalling: Boolean, 
    var powerAlarmPending: Boolean,
    val trackerLat: Double, 
    val trackerLng: Double, 
    val trackerGpsAccuracy: Double,
    val maxTrackerAccuracy: Double,
    val lastGpsPacketTs: Long,
    val lastGpsPacketRt: Long = 0L,
    val trackerLastValidFixTs: Long = 0L, 
    val trackerLastValidFixRt: Long = 0L,
    val trackerSpeed: Double = 0.0,
    val isTrackerVisualJump: Boolean = false,
    val isTrajectoryPromoted: Boolean = false,
    val jumpTier: Int = 0,
    val isAdaptiveJump: Boolean = false,
    val trackerBattery: Int, 
    val trackerTemp: Double,
    var wasDistanceViolated: Boolean, 
    var distanceViolationCounter: Int, 
    var firstViolationTs: Long, 
    var firstViolationRt: Long = 0L,
    var firstViolationWasJump: Boolean,
    val homePoints: List<EngineGeoPoint> = emptyList(),
    val maxDistance: Double = 60.0,
    val distToHomeAuthority: Double? = null,
    val isGpsGap: Boolean = false,
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val trackerTiltDegrees: Double = 0.0,
    val trackerAcousticDb: Double = 0.0,
    val trackerBaroAlt: Double = 0.0,
    val trackerBaroAltEma: Double = 0.0,
    val trackerLux: Double = 0.0,
    val isNear: Boolean = true,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Double = 0.12,
    val peakVibrationShock: Double = 0.0,
    val trackerCurrentMa: Int = 0,
    val isPowerTamper: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Double = 0.0,
    val sitVzTs: Long = 0L,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isPowerSaveMode: Boolean = false, 
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isTrackerMode: Boolean = true,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val isXiaomiDevice: Boolean = false,
    val xiaomiStatus: EngineXiaomiStatus = EngineXiaomiStatus.UNKNOWN,
    val xiaomiAutostartStatus: EngineXiaomiStatus = EngineXiaomiStatus.UNKNOWN,
    val isXiaomiManualOverride: Boolean = false,
    val isAnchorLocked: Boolean = false
)
