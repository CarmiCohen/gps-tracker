package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * LocationUpdate: Core engine model for position and sensor telemetry.
 * Aug.10.24:
 * - Issue #130: Proto Health Parity. Added isBatteryLow and isBatteryCritical 
 *   to maintain forensic parity in viewer mode (R130).
 * Aug.07.129:
 * - Issue #124: GPS Hardware Revival Hardening (R124). Added gpsHardwareLock 
 *   to ensure critical hardware status is carried in telemetry.
 * Aug.03.37:
 * - Issue #669: Forensic Audit: Database I/O Contention. Added isAdaptiveJump 
 *   to maintain forensic parity across signaling roles (R-HARDWARE-01).
 */
@Serializable
class LocationUpdate(
    var lat: Double = 0.0, var lng: Double = 0.0, var alt: Double = 0.0,
    var speed: Double = 0.0, var accuracy: Double = 0.0, var bearing: Double = 0.0,
    var battery: Int = -1, var temp: Double = 0.0, var maxTemp: Double = 0.0,
    var isCharging: Boolean = false, var gpsTs: Long = 0L, var isMe: Boolean = true,
    var ts: Long = 0L, 
    var rt: Long = 0L,
    var status: SentinelStatus = SentinelStatus.VALID,
    var isJump: Boolean = false, 
    var isTrajectoryPromoted: Boolean = false,
    var jumpTier: Int = 0,
    var distToTracker: Double? = null, var distToHome: Double? = null,
    var totalConnectedMs: Long? = null, var sessionConnectedMs: Long? = null,
    var lastConnTs: Long? = null, var lastDiscTs: Long? = null,
    var satsView: Int = 0, var satsUsed: Int = 0, var maxAccuracy: Double = 0.0,
    var uptimeMs: Long? = null, var totalDropMs: Long? = null, var maxDropMs: Long? = null, var maxDropTs: Long? = null,
    var vibration: Double? = null, var heading: Double? = null, var baroAlt: Double? = null,
    var icon: String? = null, 
    var lux: Double? = null, var isNear: Boolean? = null, var tiltDegrees: Double? = null,
    var acousticDb: Double? = null,
    var luxBaseline: Double? = null,
    var acousticFloorDb: Double? = null,
    var peakVibrationShock: Double? = null,
    var peakVibrationShockTs: Long? = null,
    var adaptiveVibrationFloor: Double? = null,
    var isTamperDetected: Boolean = false,
    var proxIdx: Double? = null,
    var proximityCm: Double? = null,
    var proximityDebounceMs: Long? = null,
    var vibrationRollingSum: Double? = null,
    var currentMa: Int = 0,
    var signal: Int? = null,
    var micPending: Boolean = false,
    var isPowerTamper: Boolean = false,
    var violationUptimeMs: Long? = null,
    var violationPercentage: Double? = null,
    var isClockRegression: Boolean = false,
    var isLocationPending: Boolean = false,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var lastValidFixRt: Long = 0L,
    var isPowerSaveMode: Boolean = false,
    var standbyBucket: Int = -1,
    var netInterface: String = "UNKNOWN",
    var isStorageLow: Boolean = false,
    var isStorageCritical: Boolean = false,
    var gnssDetail: GnssDetail? = null,
    var isBatterySteepDischarge: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var isJammer: Boolean = false,
    var isStalled: Boolean = false,
    var gpsHardwareLock: Boolean = false,
    var isSuspicious: Boolean = false,
    var isAnchorLocked: Boolean = false,
    
    // Forensic Fields
    var snrIdx: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var liftIdx: Double = 0.0,
    var tiltIdx: Double = 0.0,
    var baroIdx: Double = 0.0,
    var isSitDetected: Boolean = false,
    var isSitActive: Boolean = false,
    var lastSitTs: Long = 0L,
    var verticalVelocity: Double = 0.0,
    var sitVz: Double = 0.0,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,
    var kineticEnergy: Double = 0.0,
    var isAdaptiveJump: Boolean = false,
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false
) {
    fun copyFrom(other: LocationUpdate) {
        this.lat = other.lat; this.lng = other.lng; this.alt = other.alt
        this.speed = other.speed; this.accuracy = other.accuracy; this.bearing = other.bearing
        this.battery = other.battery; this.temp = other.temp; this.maxTemp = other.maxTemp
        this.isCharging = other.isCharging; this.gpsTs = other.gpsTs; this.isMe = other.isMe
        this.ts = other.ts; this.rt = other.rt; this.status = other.status
        this.isJump = other.isJump; this.isTrajectoryPromoted = other.isTrajectoryPromoted
        this.jumpTier = other.jumpTier; this.distToTracker = other.distToTracker; this.distToHome = other.distToHome
        this.totalConnectedMs = other.totalConnectedMs; this.sessionConnectedMs = other.sessionConnectedMs
        this.lastConnTs = other.lastConnTs; this.lastDiscTs = other.lastDiscTs
        this.satsView = other.satsView; this.satsUsed = other.satsUsed; this.maxAccuracy = other.maxAccuracy
        this.uptimeMs = other.uptimeMs; this.totalDropMs = other.totalDropMs; this.maxDropMs = other.maxDropMs; this.maxDropTs = other.maxDropTs
        this.vibration = other.vibration; this.heading = other.heading; this.baroAlt = other.baroAlt
        this.icon = other.icon; this.lux = other.lux; this.isNear = other.isNear; this.tiltDegrees = other.tiltDegrees
        this.acousticDb = other.acousticDb; this.luxBaseline = other.luxBaseline; this.acousticFloorDb = other.acousticFloorDb
        this.peakVibrationShock = other.peakVibrationShock; this.peakVibrationShockTs = other.peakVibrationShockTs
        this.adaptiveVibrationFloor = other.adaptiveVibrationFloor; this.isTamperDetected = other.isTamperDetected
        this.proxIdx = other.proxIdx; this.proximityCm = other.proximityCm; this.proximityDebounceMs = other.proximityDebounceMs
        this.vibrationRollingSum = other.vibrationRollingSum; this.currentMa = other.currentMa; this.signal = other.signal
        this.micPending = other.micPending; this.isPowerTamper = other.isPowerTamper
        this.violationUptimeMs = other.violationUptimeMs; this.violationPercentage = other.violationPercentage
        this.isClockRegression = other.isClockRegression; this.isLocationPending = other.isLocationPending
        this.locationPendingReason = other.locationPendingReason; this.lastValidFixRt = other.lastValidFixRt
        this.isPowerSaveMode = other.isPowerSaveMode; this.standbyBucket = other.standbyBucket; this.netInterface = other.netInterface
        this.isStorageLow = other.isStorageLow; this.isStorageCritical = other.isStorageCritical; this.gnssDetail = other.gnssDetail
        this.isBatterySteepDischarge = other.isBatterySteepDischarge; this.isCoolingModeActive = other.isCoolingModeActive
        this.trackerState = other.trackerState; this.isJammer = other.isJammer; this.isStalled = other.isStalled
        this.gpsHardwareLock = other.gpsHardwareLock
        this.isSuspicious = other.isSuspicious; this.isAnchorLocked = other.isAnchorLocked
        this.snrIdx = other.snrIdx; this.noiseIdx = other.noiseIdx; this.luxIdx = other.luxIdx; this.vibeIdx = other.vibeIdx
        this.liftIdx = other.liftIdx; this.tiltIdx = other.tiltIdx; this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected; this.isSitActive = other.isSitActive; this.lastSitTs = other.lastSitTs
        this.verticalVelocity = other.verticalVelocity; this.sitVz = other.sitVz; this.sitVzTs = other.sitVzTs
        this.sitVzRt = other.sitVzRt; this.sitDz = other.sitDz; this.sitBaro = other.sitBaro
        this.sitTilt = other.sitTilt; this.sitShock = other.sitShock; this.kineticEnergy = other.kineticEnergy
        this.isAdaptiveJump = other.isAdaptiveJump
        this.isBatteryLow = other.isBatteryLow
        this.isBatteryCritical = other.isBatteryCritical
    }
}
