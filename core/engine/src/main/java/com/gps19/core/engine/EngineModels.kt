package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * July.16.22:
 * - Issue #516: De-duplicate "Status" Logic. Use SystemHealthState in AlarmEvaluationState.
 * - Issue #517: Refactor AppAlarmManager. Added AlarmHistory to AlarmEvaluationState.
 */

@Serializable
data class EngineGeoPoint(
    val lat: Double, 
    val lng: Double, 
    val alt: Double = 0.0,
    val ts: Long = 0L,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0
)

@Serializable
enum class TrackerState { MOVING, PARKING, JUMPING, OFFLINE, UNKNOWN }

enum class DiscoveryPhase {
    BOOTSTRAP, DISCOVERING, MONITORING
}

enum class SentinelStatus {
    VALID, // Point is usable
    JUMP,  // Point rejected due to physical impossibility or jitter
    TAMPER // Point accompanied by hardware violation (Tilt, Shock, etc.)
}

/**
 * CapabilityStatus: Status of a specific hardware or OS capability/permission.
 */
enum class CapabilityStatus {
    GRANTED, DENIED, UNKNOWN
}

/**
 * HardwareCapabilities: Abstract representation of device-specific behaviors and restrictions.
 */
@Serializable
data class HardwareCapabilities(
    val hasBackgroundRestriction: Boolean = false,
    val backgroundStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val autostartStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val requiresWakeLockRenewal: Boolean = false,
    val requiresExtraTopPadding: Boolean = false,
    val isManualOverrideActive: Boolean = false
)

/**
 * LocationPendingReason: Contextual cause for Bayesian uncertainty expansion.
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
    val ts: Long,
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
 * Sensor snapshots for gap filling.
 */
data class EngineSensorSnapshot(
    val ts: Long,
    val acoustic: Double,
    val lux: Double,
    val vibe: Double
)

@Serializable
data class SentinelResult(
    val status: SentinelStatus,
    val reason: String = "",
    val optimizedPoint: EngineGeoPoint? = null,
    val jumpConfidence: JumpConfidence? = null,
    val suppressionNote: String? = null
)

@Serializable
data class JumpConfidence(
    val score: Int = 0, 
    val isJump: Boolean = false,
    val isOutlier: Boolean = false,
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
    val serviceStartTime: Long, 
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
    val trackerLastValidFixTs: Long = 0L, 
    val trackerSpeed: Double = 0.0,
    val jumpTier: Int = 0,
    val history: AlarmHistory = AlarmHistory(),
    val homePoints: List<EngineGeoPoint> = emptyList(),
    val maxDistance: Double = 60.0,
    val distToHomeAuthority: Double? = null,
    val isGpsGap: Boolean = false,
    val trackerBaroAltEma: Double = 0.0,
    val isTrackerMode: Boolean = true,
    val health: SystemHealthState = SystemHealthState(),
    val capabilities: HardwareCapabilities = HardwareCapabilities()
)
