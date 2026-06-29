package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * v8.9.52:
 * - Issue #431: Added trackerLastValidFixTs to AlarmEvaluationState for Bayesian expansion.
 * v8.9.42:
 * - Issue #334: Added ts to EngineGeoPoint for hindsight rubber-banding.
 * - Issue #326: Added LocationPendingReason and locationPendingReason to AlarmEvaluationState 
 *   for forensic parity.
 * - Issue #325: Authoritative Spatial Anchoring (Dual-Metric). Added accuracy and 
 *   maxAccuracy to EngineGeoPoint for forensic parity in interpolated segments. 
 *   Added maxAccuracy to EngineConnectionPoint for forensic ribbon uncertainty tracking.
 * - Issue #327: Added promotedPoints to SentinelResult for hindsight transition smoothing.
 * - Issue #329: Added tiltIdx and baroIdx for forensic ribbon expansion.
 * - Issue #332: Added isAdaptiveJump to JumpConfidence.
 * v8.9.7:
 * - Plunge Matching: Added sitVzTs to EngineConnectionPoint for forensic parity.
 */

@Serializable
data class EngineGeoPoint(
    val lat: Double, 
    val lng: Double, 
    val ts: Long = 0L,
    val accuracy: Float = 0f,
    val maxAccuracy: Float = 0f
)

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
 * v8.9.22 (Issue #326)
 */
enum class LocationPendingReason {
    NONE,
    GPS_STALL,
    ACOUSTIC_VIOLATION,
    SIGNAL_LOSS,
    JAMMER_SUSPICION
}

/**
 * EngineConnectionPoint: Pure Kotlin representation of a forensic telemetry slice.
 */
@Serializable
data class EngineConnectionPoint(
    val ts: Long,
    val rtt: Int,
    val remoteSig: Int,
    val isConnected: Boolean,
    val isGap: Boolean = false,
    val hasGps: Boolean = false,
    val gpsIndex: Float = 0f,
    val accuracy: Float = 0f,
    val maxAccuracy: Float = 0f,
    val noiseIdx: Float = 0f,
    val luxIdx: Float = 0f,
    val vibeIdx: Float = 0f,
    val proxIdx: Float = 1f,
    val liftIdx: Float = 0f,
    val snrIdx: Float = 0f,
    val tiltIdx: Float = 0f,
    val baroIdx: Float = 0f,
    val verticalVelocity: Float = 0f,
    val sitVz: Float = 0f,
    val sitVzTs: Long = 0L,
    val sitDz: Float = 0f,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val isTick: Boolean = false,
    val currentMa: Int = 0,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
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
data class EngineSnrSample(val ts: Long, val snr: Float)
data class EngineSensorSnapshot(
    val ts: Long,
    val acoustic: Double,
    val lux: Float,
    val vibe: Float,
    val proxIdx: Float,
    val lift: Float,
    val tilt: Float,
    val isSitDetected: Boolean
)

@Serializable
data class SentinelResult(
    val status: SentinelStatus,
    val reason: String = "",
    val optimizedPoint: EngineGeoPoint? = null,
    val jumpConfidence: JumpConfidence? = null,
    val promotedPoints: List<EngineGeoPoint>? = null
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
    val cn0: Float,
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
    val accuracy: Float,
    val bearing: Float,
    val speedMps: Double,
    val ts: Long
)

/**
 * SpatialAnchor: Minimal interface for position recovery.
 */
interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long
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
    val now: Long, 
    val serviceStartTime: Long, 
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
    val trackerGpsAccuracy: Float,
    val maxTrackerAccuracy: Float,
    val lastGpsPacketTs: Long,
    val trackerLastValidFixTs: Long = 0L, // Added for Issue #431
    val trackerSpeed: Float = 0f,
    val isTrackerVisualJump: Boolean = false,
    val isTrajectoryPromoted: Boolean = false,
    val jumpTier: Int = 0,
    val isAdaptiveJump: Boolean = false,
    val trackerBattery: Int, 
    val trackerTemp: Float,
    var wasDistanceViolated: Boolean, 
    var distanceViolationCounter: Int, 
    var firstViolationTs: Long, 
    var firstViolationWasJump: Boolean,
    val homePoints: List<EngineGeoPoint> = emptyList(),
    val maxDistance: Double = 60.0,
    val distToHomeAuthority: Double? = null,
    val isGpsGap: Boolean = false,
    val isSuspicious: Boolean = false,
    val isTamperDetected: Boolean = false,
    val trackerTiltDegrees: Float = 0f,
    val trackerAcousticDb: Double = 0.0,
    val trackerBaroAlt: Float = 0f,
    val trackerLux: Float = 0f,
    val isNear: Boolean = true,
    val luxBaseline: Float = 0f,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Float = 0.12f,
    val peakVibrationShock: Float = 0f,
    val trackerCurrentMa: Int = 0,
    val isPowerTamper: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Float = 0f,
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
    val isXiaomiManualOverride: Boolean = false
)
