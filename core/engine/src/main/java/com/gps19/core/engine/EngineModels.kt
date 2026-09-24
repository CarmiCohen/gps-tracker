package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * Sep.24.94:
 * - Issue #1311: Expanded AlarmEvaluationState to include active alarms and siren 
 *   metadata for stateless evaluation.
 * Sep.23.70:
 * - Issue #1230 REMEDIATION: Added rolePrefix to AlarmServiceContext to support 
 *   role-based namespace isolation during logic state persistence (R-ID 453).
 */

@Serializable
class EngineGeoPoint(
    var lat: Double = 0.0, 
    var lng: Double = 0.0, 
    var alt: Double = 0.0,
    var ts: Long = 0L,
    var rt: Long = 0L,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0
) {
    fun update(lat: Double, lng: Double, alt: Double = 0.0, ts: Long = 0L, rt: Long = 0L, accuracy: Double = 0.0, maxAccuracy: Double = 0.0) {
        this.lat = lat; this.lng = lng; this.alt = alt; this.ts = ts; this.rt = rt; this.accuracy = accuracy; this.maxAccuracy = maxAccuracy
    }
}

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

/**
 * PerformanceTier: Defines hardware performance characteristics for remediation gating.
 */
@Serializable
enum class PerformanceTier {
    STANDARD,
    STAGGERED
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
    val isSamsungDevice: Boolean = false,
    val isHuaweiDevice: Boolean = false,
    val isMicrophoneGranted: Boolean = false,
    val performanceTier: PerformanceTier = PerformanceTier.STANDARD
)

enum class LocationPendingReason {
    NONE,
    GPS_STALL,
    GPS_GAP,
    ACOUSTIC_VIOLATION, SIGNAL_LOSS,
    JAMMER_SUSPICION
}

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
    var gpsIndex: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var proxIdx: Double = 0.0,
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
    var kineticEnergy: Double = 0.0,
    var gpsHardwareLock: Boolean = false,
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,
    var isUltraLongStationary: Boolean = false,
    var violationUptimeMs: Long = 0L
) {
    fun copyFrom(other: EngineConnectionPoint) {
        this.ts = other.ts; this.rt = other.rt; this.rtt = other.rtt; this.remoteSig = other.remoteSig
        this.isConnected = other.isConnected; this.isGap = other.isGap; this.isRecoveryEvent = other.isRecoveryEvent
        this.hasGps = other.hasGps; this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy
        this.isBatterySteepDischarge = other.isBatterySteepDischarge; this.isCoolingModeActive = other.isCoolingModeActive
        this.speed = other.speed; this.bearing = other.bearing; this.isTick = other.isTick
        this.currentMa = other.currentMa; this.locationPendingReason = other.locationPendingReason
        this.gpsIndex = other.gpsIndex; this.noiseIdx = other.noiseIdx; this.luxIdx = other.luxIdx
        this.vibeIdx = other.vibeIdx; this.proxIdx = other.proxIdx; this.liftIdx = other.liftIdx
        this.snrIdx = other.snrIdx; this.tiltIdx = other.tiltIdx; this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected; this.isSitActive = other.isSitActive
        this.verticalVelocity = other.verticalVelocity; this.sitVz = other.sitVz
        this.sitVzTs = other.sitVzTs; this.sitVzRt = other.sitVzRt; this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro; this.sitTilt = other.sitTilt; this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy; this.gpsHardwareLock = other.gpsHardwareLock
        this.cpuLoad = other.cpuLoad; this.ioWait = other.ioWait; this.maxIoLatency = other.maxIoLatency
        this.isSilentFailure = other.isSilentFailure; this.isBatteryLow = other.isBatteryLow; this.isBatteryCritical = other.isBatteryCritical
        this.isUltraLongStationary = other.isUltraLongStationary; this.violationUptimeMs = other.violationUptimeMs
    }
}

/**
 * AlarmTelemetrySnapshot: Unified DTO for telemetry propagation to the alarm engine.
 */
@Serializable
data class AlarmTelemetrySnapshot(
    val status: SentinelStatus = SentinelStatus.VALID,
    val isJammer: Boolean = false,
    val jumpTier: Int = 0,
    val isAdaptiveJump: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val gpsTs: Long = 0L,
    val lastValidFixRt: Long = 0L,
    val speed: Double = 0.0,
    val battery: Int = 100,
    val temp: Double = 0.0,
    val currentMa: Int = 0,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isTamperDetected: Boolean = false,
    val isPowerTamper: Boolean = false,
    val tiltDegrees: Double = 0.0,
    val acousticDb: Double = 0.0,
    val baroAlt: Double = 0.0,
    val baroAltEma: Double = -1000.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Double = 0.12,
    val peakVibrationShock: Double = 0.0,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val snrSnapshot: Double? = null,
    val vibeSnapshot: Double? = null,
    val isGpsHardwareLock: Boolean = false,
    val cpuLoad: Double = 0.0,
    val ioWait: Double = 0.0,
    val maxIoLatency: Long = 0L,
    val isSilentFailure: Boolean = false,
    val isMaliAnomaly: Boolean = false,
    val isUltraLongStationary: Boolean = false,
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    val tamperNote: String? = null,
    val isSignalLoss: Boolean = false,
    val isGpsStalling: Boolean = false,
    val isGpsGap: Boolean = false,
    val localInternetLoss: Boolean = false,
    val isHardwareOnline: Boolean = true
)

/**
 * AlarmServiceContext: Unified DTO for service-level context in alarm evaluation.
 */
@Serializable
data class AlarmServiceContext(
    val now: Long,
    val nowRt: Long,
    val serviceStartTs: Long,
    val serviceStartRt: Long,
    val appStartTime: Long,
    val isTrackerMode: Boolean,
    val isRelayConnected: Boolean,
    val isTrackerConnected: Boolean,
    val isUiVisible: Boolean,
    val distToHomeAuthority: Double?,
    val maxDistanceAuthority: Double,
    val discoveryPhase: DiscoveryPhase? = null,
    val capabilities: HardwareCapabilities = HardwareCapabilities(),
    val rolePrefix: String = ""
)

/**
 * SpatialAnchor: Polymorphic base for coordinate-aware telemetry.
 */
interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long
    val rt: Long
}

@Serializable
data class RejectedPoint(
    val lat: Double,
    val lng: Double,
    val alt: Double,
    val accuracy: Double,
    val bearing: Double,
    val speedMps: Double,
    val ts: Long,
    val rt: Long
)

@Serializable
class ProcessedLocation {
    var rawPoint: EngineGeoPoint = EngineGeoPoint()
    var optimizedPoint: EngineGeoPoint = EngineGeoPoint()
    var status: SentinelStatus = SentinelStatus.VALID
    var maxAccuracy: Double = 0.0
    var currentAccuracy: Double = 0.0
    var filteredSpeed: Double = 0.0
    var timestamp: Long = 0L
    var rt: Long = 0L
    var isStalled: Boolean = false
    var isClockRegression: Boolean = false
    var receiptRt: Long = 0L
    var isTrajectoryPromoted: Boolean = false
    var jumpTier: Int = 0
    var isAdaptiveJump: Boolean = false
    var distToHome: Double? = null
    var isSpatiallyValid: Boolean = false
    var geofenceViolationDetected: Boolean = false
    var tamperDetected: Boolean = false
    var jammerDetected: Boolean = false
    var isAnchorLocked: Boolean = false
    var suppressionNote: String? = null
    var kineticEnergy: Double = 0.0

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
        jumpTier = 0
        isAdaptiveJump = false
        distToHome = null
        isSpatiallyValid = false
        geofenceViolationDetected = false
        tamperDetected = false
        jammerDetected = false
        isAnchorLocked = false
        suppressionNote = null
        kineticEnergy = 0.0
    }
}

@Serializable
class AlarmEvaluationState {
    var now: Long = 0L
    var nowRt: Long = 0L
    var health: SystemHealthState = SystemHealthState()
    var discoveryPhase: DiscoveryPhase = DiscoveryPhase.BOOTSTRAP
    var isTrackerMode: Boolean = true
    var isRelayConnected: Boolean = false
    var isTrackerConnected: Boolean = false
    var jumpTier: Int = 0
    var isGpsGap: Boolean = false
    var trackerBaroAltEma: Double = -1000.0
    var trackerLat: Double = 0.0
    var trackerLng: Double = 0.0
    var trackerGpsAccuracy: Double = 0.0
    var maxTrackerAccuracy: Double = 0.0
    var trackerLastValidFixTs: Long = 0L
    var trackerLastValidFixRt: Long = 0L
    var trackerSpeed: Double = 0.0
    var trackerBattery: Int = 0
    var trackerTemp: Double = 0.0
    var firstViolationTs: Long = 0L
    var firstViolationRt: Long = 0L
    var firstViolationWasJump: Boolean = false
    var wasDistanceViolated: Boolean = false
    var distanceViolationCounter: Int = 0
    var isAdaptiveJump: Boolean = false
    var lastGpsPacketTs: Long = 0L
    var lastGpsPacketRt: Long = 0L
    var serviceStartTime: Long = 0L
    var serviceStartRt: Long = 0L
    var lastAlarmAckTs: Long = 0L
    var appStartTime: Long = 0L
    var capabilities: HardwareCapabilities = HardwareCapabilities()
    var forensicReliabilityDegradationStartRt: Long = 0L

    // Issue #1311: Stateless Evaluation consolidation
    var powerAlarmPending: Boolean = false
    var lastSirenStopRt: Long = 0L
    var lastGlobalTriggerRt: Long = 0L
    var activeAlarms: MutableMap<String, ActiveAlarm> = mutableMapOf()

    @Serializable
    data class ActiveAlarm(
        val type: String,
        var title: String,
        var subtitle: String = "",
        var isTriggered: Boolean = false,
        var firstTriggerTs: Long = 0L,
        var firstTriggerRt: Long = 0L,
        var lastLogTs: Long = 0L,
        var lastLogRt: Long = 0L,
        var isResolved: Boolean = true
    )
    
    var homePoints: MutableList<EngineGeoPoint> = mutableListOf()
    var maxDistance: Double = 0.0
    var distToHomeAuthority: Double? = null

    // Issue #897: Sensitivity Propagation
    var vibrationSensitivity: Float = 0.5f
    var tiltSensitivity: Float = 0.5f

    fun getOrCreateHomePoint(index: Int): EngineGeoPoint {
        while (homePoints.size <= index) {
            homePoints.add(EngineGeoPoint())
        }
        return homePoints[index]
    }

    fun truncateHomePoints(size: Int) {
        while (homePoints.size > size) {
            homePoints.removeAt(homePoints.size - 1)
        }
    }

    fun update(
        now: Long,
        nowRt: Long,
        serviceStartTime: Long,
        serviceStartRt: Long,
        lastAlarmAckTs: Long,
        appStartTime: Long,
        isRelayConnected: Boolean,
        isTrackerConnected: Boolean,
        discoveryPhase: DiscoveryPhase,
        trackerLat: Double,
        trackerLng: Double,
        trackerGpsAccuracy: Double,
        maxTrackerAccuracy: Double,
        lastGpsPacketTs: Long,
        lastGpsPacketRt: Long,
        trackerLastValidFixTs: Long,
        trackerLastValidFixRt: Long,
        trackerSpeed: Double,
        jumpTier: Int,
        isAdaptiveJump: Boolean,
        trackerBattery: Int,
        trackerTemp: Double,
        wasDistanceViolated: Boolean,
        distanceViolationCounter: Int,
        firstViolationTs: Long,
        firstViolationRt: Long,
        firstViolationWasJump: Boolean,
        maxDistance: Double,
        distToHomeAuthority: Double?,
        isGpsGap: Boolean,
        trackerBaroAltEma: Double,
        isTrackerMode: Boolean,
        capabilities: HardwareCapabilities,
        vibrationSensitivity: Float = 0.5f,
        tiltSensitivity: Float = 0.5f,
        powerAlarmPending: Boolean = false,
        lastSirenStopRt: Long = 0L,
        lastGlobalTriggerRt: Long = 0L
    ) {
        this.now = now
        this.nowRt = nowRt
        this.serviceStartTime = serviceStartTime
        this.serviceStartRt = serviceStartRt
        this.lastAlarmAckTs = lastAlarmAckTs
        this.appStartTime = appStartTime
        this.isRelayConnected = isRelayConnected
        this.isTrackerConnected = isTrackerConnected
        this.discoveryPhase = discoveryPhase
        this.trackerLat = trackerLat
        this.trackerLng = trackerLng
        this.trackerGpsAccuracy = trackerGpsAccuracy
        this.maxTrackerAccuracy = maxTrackerAccuracy
        this.lastGpsPacketTs = lastGpsPacketTs
        this.lastGpsPacketRt = lastGpsPacketRt
        this.trackerLastValidFixTs = trackerLastValidFixTs
        this.trackerLastValidFixRt = trackerLastValidFixRt
        this.trackerSpeed = trackerSpeed
        this.jumpTier = jumpTier
        this.isAdaptiveJump = isAdaptiveJump
        this.trackerBattery = trackerBattery
        this.trackerTemp = trackerTemp
        this.wasDistanceViolated = wasDistanceViolated
        this.distanceViolationCounter = distanceViolationCounter
        this.firstViolationTs = firstViolationTs
        this.firstViolationRt = firstViolationRt
        this.firstViolationWasJump = firstViolationWasJump
        this.maxDistance = maxDistance
        this.distToHomeAuthority = distToHomeAuthority
        this.isGpsGap = isGpsGap
        this.trackerBaroAltEma = trackerBaroAltEma
        this.isTrackerMode = isTrackerMode
        this.capabilities = capabilities
        this.vibrationSensitivity = vibrationSensitivity
        this.tiltSensitivity = tiltSensitivity
        this.powerAlarmPending = powerAlarmPending
        this.lastSirenStopRt = lastSirenStopRt
        this.lastGlobalTriggerRt = lastGlobalTriggerRt
    }
}

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1), SIXTEEN_MIN("16M", 4), ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60), TWENTY_FOUR_HOUR("24H", 360), SEVEN_DAY("7D", 2700)
}

class EngineSnrSample(var ts: Long = 0L, var rt: Long = 0L, var snr: Double = 0.0)

/**
 * EngineAcousticSample: Represents a forensic acoustic measurement.
 * R-ID 393: Decoupled from satellite SNR to prevent telemetry ambiguity.
 */
class EngineAcousticSample(var ts: Long = 0L, var rt: Long = 0L, var db: Double = 0.0)

class EngineSensorSnapshot(
    var ts: Long = 0L, var rt: Long = 0L, var acoustic: Double = 0.0, var lux: Double = 0.0,
    var vibe: Double = 0.0, var proxIdx: Double = 0.0, var lift: Double = 0.0, var tilt: Double = 0.0,
    var isSitDetected: Boolean = false, var sitVzTs: Long = 0L, var sitVzRt: Long = 0L,
    var sitShock: Double = 0.0, var kineticEnergy: Double = 0.0
) {
    fun copyFrom(other: EngineSensorSnapshot) {
        this.ts = other.ts; this.rt = other.rt; this.acoustic = other.acoustic; this.lux = other.lux
        this.vibe = other.vibe; this.proxIdx = other.proxIdx; this.lift = other.lift; this.tilt = other.tilt
        this.isSitDetected = other.isSitDetected; this.sitVzTs = other.sitVzTs; this.sitVzRt = other.sitVzRt
        this.sitShock = other.sitShock; this.kineticEnergy = other.kineticEnergy
    }
}

@Serializable
class SentinelResult(
    var status: SentinelStatus = SentinelStatus.VALID, var reason: String = "",
    var optimizedPoint: EngineGeoPoint? = null, var jumpConfidence: JumpConfidence? = null,
    var suppressionNote: String? = null, var promotedPoints: List<EngineGeoPoint>? = null
) {
    fun reset(status: SentinelStatus = SentinelStatus.VALID) {
        this.status = status; this.reason = ""; this.optimizedPoint = null
        this.jumpConfidence?.reset(); this.suppressionNote = null; this.promotedPoints = null
    }
}

@Serializable
class JumpConfidence(
    var score: Int = 0, var isJump: Boolean = false, var isOutlier: Boolean = false,
    var tier: Int = 0, var reason: String = "", var isAdaptiveJump: Boolean = false
) {
    fun reset() { score = 0; isJump = false; isOutlier = false; tier = 0; reason = ""; isAdaptiveJump = false }
    fun copyFrom(other: JumpConfidence) {
        this.score = other.score; this.isJump = other.isJump; this.isOutlier = other.isOutlier
        this.tier = other.tier; this.reason = other.reason; this.isAdaptiveJump = other.isAdaptiveJump
    }
}

@Serializable
data class SatelliteInfo(val svid: Int, val cn0: Double, val usedInFix: Boolean, val constellation: Int)

@Serializable
data class GnssDetail(val satellites: List<SatelliteInfo> = emptyList())

@Serializable
class ViolationReport(
    var type: String = "", var title: String = "", var subtitle: String = "",
    var conditionMet: Boolean = false, var technicalDetails: String? = null, var extremeValue: Double? = null
) {
    fun reset() { type = ""; title = ""; subtitle = ""; conditionMet = false; technicalDetails = null; extremeValue = null }
    fun update(type: String, title: String, subtitle: String, conditionMet: Boolean, technicalDetails: String? = null, extremeValue: Double? = null) {
        this.type = type; this.title = title; this.subtitle = subtitle; this.conditionMet = conditionMet; this.technicalDetails = technicalDetails; this.extremeValue = extremeValue
    }
}

@Serializable
class SystemHealthReport(val reports: MutableList<ViolationReport> = mutableListOf()) {
    fun reset() { reports.forEach { it.reset() } }
    fun getOrCreate(index: Int): ViolationReport {
        while (reports.size <= index) { reports.add(ViolationReport()) }
        return reports[index]
    }
    fun truncate(size: Int) { while (reports.size > size) { reports.removeAt(reports.size - 1) } }
}

@Serializable
data class AlarmInfo(val title: String, val subtitle: String, val type: String = "", val isResolved: Boolean = false, val isSirenDisabled: Boolean = false)

@Serializable
data class SensorStateSnapshot(
    val vibration: Double = -1.0,
    val heading: Double = -1.0,
    val baroAlt: Double = -1000.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val powerTamper: Boolean = false,
    val tiltDegrees: Double = 0.0,
    val acousticDb: Double = 0.0,
    val peakShock: Double = 0.0,
    val acousticMinDb: Double = -1.0,
    val peakVerticalVelocity: Double = 0.0,
    val peakVerticalVelocityTs: Long = 0L,
    val peakVerticalVelocityRt: Long = 0L,
    val plungeMatched: Boolean = false,
    val peakVerticalDisplacement: Double = 0.0,
    val isSirenActive: Boolean = false,
    val isWarming: Boolean = false,
    val manualAdaptiveFloor: Double = -1.0,
    val acousticLockoutRt: Long = 0L,
    val lightSpikeRt: Long = 0L,
    val isMuzzled: Boolean = false,
    val kineticEnergy: Double = 0.0,
    val providedAdaptiveFloor: Double = -1.0,
    val nowRt: Long = 0L,
    val nowTs: Long = 0L
)

/**
 * EvaluationSnapshot: Atomic container for all telemetry and health metrics 
 * consumed during a background tick. (Issue #1162)
 */
@Serializable
data class EvaluationSnapshot(
    val health: SystemHealthState,
    val sensor: SensorStateSnapshot
)
