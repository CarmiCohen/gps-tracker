package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * July.16.18:
 * - Issue #514: Simplified GpsManager. Removed EngineSnrSample.
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
    val isSignalLoss: Boolean, 
    val isGpsStalling: Boolean, 
    var powerAlarmPending: Boolean,
    val trackerLat: Double, 
    val trackerLng: Double, 
    val trackerGpsAccuracy: Double,
    val maxTrackerAccuracy: Double,
    val lastGpsPacketTs: Long,
    val trackerLastValidFixTs: Long = 0L, 
    val trackerSpeed: Double = 0.0,
    val status: SentinelStatus = SentinelStatus.VALID,
    val isJammer: Boolean = false,
    val jumpTier: Int = 0,
    val trackerBattery: Int, 
    val trackerTemp: Double,
    var wasDistanceViolated: Boolean, 
    var distanceViolationCounter: Int, 
    var firstViolationTs: Long, 
    var firstViolationWasJump: Boolean,
    val homePoints: List<EngineGeoPoint> = emptyList(),
    val maxDistance: Double = 60.0,
    val distToHomeAuthority: Double? = null,
    val isGpsGap: Boolean = false,
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
    val capabilities: HardwareCapabilities = HardwareCapabilities()
)
