package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * July.23.07:
 * - Issue #113: Hardened HardwareCapabilities for Samsung A15 stabilization.
 * July.21.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized 'rt' and 'ts'.
 * - Forensic Hardening: Expanded SentinelStatus and JumpConfidence for deep auditing.
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
    val isA15Device: Boolean = false
)

enum class LocationPendingReason {
    NONE,
    GPS_STALL,
    GPS_GAP,
    ACOUSTIC_VIOLATION,
    SIGNAL_LOSS,
    JAMMER_SUSPICION
}

@Serializable
data class EngineConnectionPoint(
    val ts: Long,
    val rt: Long = 0L,
    val rtt: Int,
    val remoteSig: Int,
    val isConnected: Boolean,
    val isGap: Boolean = false,
    val hasGps: Boolean = false,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val isTick: Boolean = false,
    val currentMa: Int = 0,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    
    // Forensic Indices (Issue #102)
    val gpsIndex: Double = 0.0,
    val noiseIdx: Double = 0.0,
    val luxIdx: Double = 0.0,
    val vibeIdx: Double = 0.0,
    val proxIdx: Double = 1.0,
    val liftIdx: Double = 0.0,
    val snrIdx: Double = 0.0,
    val tiltIdx: Double = 0.0,
    val baroIdx: Double = 0.0,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val verticalVelocity: Double = 0.0,
    val sitVz: Double = 0.0,
    val sitDz: Double = 0.0,
    val sitBaro: Double = 0.0,
    val sitTilt: Double = 0.0,
    val sitShock: Double = 0.0
)

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1),
    SIXTEEN_MIN("16M", 4),
    ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60),
    TWENTY_FOUR_HOUR("24H", 360),
    SEVEN_DAY("7D", 2700)
}

data class EngineSnrSample(val ts: Long, val rt: Long = 0L, val snr: Double)

data class EngineSensorSnapshot(
    val ts: Long,
    val rt: Long = 0L,
    val acoustic: Double,
    val lux: Double,
    val vibe: Double,
    val proxIdx: Double = 1.0,
    val lift: Double = 0.0,
    val tilt: Double = 0.0,
    val isSitDetected: Boolean = false
)

@Serializable
data class SentinelResult(
    val status: SentinelStatus,
    val reason: String = "",
    val optimizedPoint: EngineGeoPoint? = null,
    val jumpConfidence: JumpConfidence? = null,
    val suppressionNote: String? = null,
    val promotedPoints: List<EngineGeoPoint>? = null
)

@Serializable
data class JumpConfidence(
    val score: Int = 0, 
    val isJump: Boolean = false,
    val isOutlier: Boolean = false,
    val tier: Int = 0, 
    val reason: String = "",
    val isAdaptiveJump: Boolean = false
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
