package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * SystemHealthState: The authoritative model for all device metadata and health status.
 * July.22.01:
 * - Forensic Parity: Added missing indices (noiseIdx, luxIdx, vibeIdx, liftIdx).
 * July.20.07:
 * - Renamed lastValidFixRealtime to lastValidFixRt (Issue #102).
 * - Added Forensic Sit Detection fields.
 * - Added snrIdx for GNSS health tracking.
 */
@Serializable
data class SystemHealthState(
    val signalLoss: Boolean = false,
    val gpsStalled: Boolean = false,
    val localInternetLoss: Boolean = false,
    val isHardwareOnline: Boolean = true,
    val batteryLevel: Int = 100,
    val batteryTemp: Double = 0.0,
    val maxTemp: Double = 0.0,
    val isCharging: Boolean = false,
    val currentMa: Int = 0,
    val status: SentinelStatus = SentinelStatus.VALID,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val isJammer: Boolean = false,
    val isTamperDetected: Boolean = false,
    val micPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val isClockRegression: Boolean = false,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val lastValidFixRt: Long = 0L,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val gnssDetail: GnssDetail? = null,
    val snrIdx: Double = 0.0,
    val noiseIdx: Double = 0.0,
    val luxIdx: Double = 0.0,
    val vibeIdx: Double = 0.0,
    val liftIdx: Double = 0.0,
    
    // Connectivity Stats
    val uptimeMs: Long = 0L,
    val lastConnTs: Long = 0L,
    val lastDiscTs: Long = 0L,
    val totalDropMs: Long = 0L,
    val maxDropMs: Long = 0L,
    val maxDropTs: Long = 0L,
    val totalConnectedMs: Long = 0L,
    val sessionConnectedMs: Long = 0L,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Double = 0.0,

    // Sensor Metadata
    val vibration: Double = 0.0,
    val heading: Double = 0.0,
    val tiltDegrees: Double = 0.0,
    val acousticDb: Double = 0.0,
    val baroAlt: Double = 0.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val peakVibrationShock: Double = 0.0,
    val peakVibrationShockTs: Long = 0L,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Double = 0.12,
    val proxIdx: Double = 1.0,
    val proximityCm: Double = -1.0,
    val proximityDebounceMs: Long = 0L,
    val vibrationRollingSum: Double = 0.0,

    // Forensic Sit Detection (Issue #102/R990)
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
