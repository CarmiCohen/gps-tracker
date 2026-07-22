package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * LocationUpdate: Core engine model for position and sensor telemetry.
 * July.21.00:
 * - Forensic Hardening: Added missing sit-detection and forensic index fields.
 * - Issue #102: Temporal Forensic Integrity. Standardized 'rt' and 'ts'.
 */
@Serializable
data class LocationUpdate(
    val lat: Double = 0.0, val lng: Double = 0.0, val alt: Double = 0.0,
    val speed: Double = 0.0, val accuracy: Double = 0.0, val bearing: Double = 0.0,
    val battery: Int = -1, val temp: Double = 0.0, val maxTemp: Double = 0.0,
    val isCharging: Boolean = false, val gpsTs: Long = 0L, val isMe: Boolean = true,
    val ts: Long = 0L, // Wall-clock
    val rt: Long = 0L, // Monotonic (Issue #102)
    val status: SentinelStatus = SentinelStatus.VALID,
    val isJump: Boolean = false, 
    val isTrajectoryPromoted: Boolean = false,
    val jumpTier: Int = 0,
    val distToTracker: Double? = null, val distToHome: Double? = null,
    val totalConnectedMs: Long? = null, val sessionConnectedMs: Long? = null,
    val lastConnTs: Long? = null, val lastDiscTs: Long? = null,
    val satsView: Int = 0, val satsUsed: Int = 0, val maxAccuracy: Double = 0.0,
    val uptimeMs: Long? = null, val totalDropMs: Long? = null, val maxDropMs: Long? = null, val maxDropTs: Long? = null,
    val vibration: Double? = null, val heading: Double? = null, val baroAlt: Double? = null,
    val icon: String? = null, 
    val lux: Double? = null, val isNear: Boolean? = null, val tiltDegrees: Double? = null,
    val acousticDb: Double? = null,
    val luxBaseline: Double? = null,
    val acousticFloorDb: Double? = null,
    val peakVibrationShock: Double? = null,
    val peakVibrationShockTs: Long? = null,
    val adaptiveVibrationFloor: Double? = null,
    val isTamperDetected: Boolean = false,
    val proxIdx: Double? = null,
    val proximityCm: Double? = null,
    val proximityDebounceMs: Long? = null,
    val vibrationRollingSum: Double? = null,
    val currentMa: Int = 0,
    val signal: Int? = null,
    val micPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long? = null,
    val violationPercentage: Double? = null,
    val isClockRegression: Boolean = false,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val lastValidFixRt: Long = 0L,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val gnssDetail: GnssDetail? = null,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val isJammer: Boolean = false,
    val isStalled: Boolean = false,
    val isSuspicious: Boolean = false,
    val isAnchorLocked: Boolean = false,
    
    // Forensic Fields
    val snrIdx: Double = 0.0,
    val tiltIdx: Double = 0.0,
    val baroIdx: Double = 0.0,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Double = 0.0,
    val sitVz: Double = 0.0,
    val sitDz: Double = 0.0,
    val sitBaro: Double = 0.0,
    val sitTilt: Double = 0.0,
    val sitShock: Double = 0.0
)
