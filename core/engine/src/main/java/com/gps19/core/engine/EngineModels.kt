package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * Aug.11.09:
 * - Issue #141: Stress Recovery Verification. Added requiresAdaptationMuzzle 
 *   to HardwareCapabilities for budget hardware state management (R141).
 * Aug.10.27:
 * - Issue #133: Forensic Anomaly Correlation Engine. Added isSilentFailure 
 *   to EngineConnectionPoint for load-correlated stall tracking (R133).
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

@Serializable
data class HardwareCapabilities(
    val hasBackgroundRestriction: Boolean = false,
    val backgroundStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val autostartStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val requiresWakeLockRenewal: Boolean = false,
    val requiresExtraTopPadding: Boolean = false,
    val isManualOverrideActive: Boolean = false,
    val isA15Device: Boolean = false,
    val isMicrophoneGranted: Boolean = false,
    val requiresAdaptationMuzzle: Boolean = false
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
    var gpsHardwareLock: Boolean = false, // Issue #125

    // Performance & Load Correlation (Issue #132)
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,

    // Anomaly Correlation (Issue #133)
    var isSilentFailure: Boolean = false
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
        this.gpsHardwareLock = other.gpsHardwareLock
        this.cpuLoad = other.cpuLoad
        this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency
        this.isSilentFailure = other.isSilentFailure
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
    var proxIdx: Double = 0.0,
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

/**
 * Issue #668: Converted to mutable class for object pooling.
 */
@Serializable
class ViolationReport(
    var type: String = "", 
    var title: String = "", 
    var subtitle: String = "", 
    var conditionMet: Boolean = false,
    var technicalDetails: String? = null, 
    var extremeValue: Double? = null
) {
    fun reset() {
        type = ""; title = ""; subtitle = ""; conditionMet = false; technicalDetails = null; extremeValue = null
    }

    fun update(
        type: String, title: String, subtitle: String, conditionMet: Boolean,
        technicalDetails: String? = null, extremeValue: Double? = null
    ) {
        this.type = type
        this.title = title
        this.subtitle = subtitle
        this.conditionMet = conditionMet
        this.technicalDetails = technicalDetails
        this.extremeValue = extremeValue
    }
}

/**
 * Issue #668: Converted to mutable class for zero-churn emissions.
 */
@Serializable
class SystemHealthReport(val reports: MutableList<ViolationReport> = mutableListOf()) {
    fun reset() {
        reports.forEach { it.reset() }
    }

    fun getOrCreate(index: Int): ViolationReport {
        while (reports.size <= index) {
            reports.add(ViolationReport())
        }
        return reports[index]
    }

    fun truncate(size: Int) {
        while (reports.size > size) {
            reports.removeAt(reports.size - 1)
        }
    }
}

@Serializable
data class AlarmHistory(
    var powerAlarmPending: Boolean = false,
    var wasDistanceViolated: Boolean = false,
    var distanceViolationCounter: Int = 0,
    var firstViolationTs: Long = 0L,
    var firstViolationWasJump: Boolean = false
)

/**
 * AlarmEvaluationState: Flyweight for zero-churn alarm logic.
 * Aug.01.10: Refactored to mutable class.
 */
class AlarmEvaluationState(
    var now: Long = 0L,
    var nowRt: Long = 0L,
    var serviceStartTime: Long = 0L, 
    var serviceStartRt: Long = 0L,
    var lastAlarmAckTs: Long = 0L, 
    var appStartTime: Long = 0L,
    var isRelayConnected: Boolean = false, 
    var isTrackerConnected: Boolean = false, 
    var discoveryPhase: DiscoveryPhase? = null,
    var trackerLat: Double = 0.0, 
    var trackerLng: Double = 0.0, 
    var trackerGpsAccuracy: Double = 0.0,
    var maxTrackerAccuracy: Double = 0.0,
    var lastGpsPacketTs: Long = 0L,
    var lastGpsPacketRt: Long = 0L,
    var trackerLastValidFixTs: Long = 0L, 
    var trackerLastValidFixRt: Long = 0L,
    var trackerSpeed: Double = 0.0,
    var jumpTier: Int = 0,
    var isAdaptiveJump: Boolean = false,
    var trackerBattery: Int = 100, 
    var trackerTemp: Double = 0.0,
    var wasDistanceViolated: Boolean = false, 
    var distanceViolationCounter: Int = 0, 
    var firstViolationTs: Long = 0L, 
    var firstViolationRt: Long = 0L,
    var firstViolationWasJump: Boolean = false,
    var homePoints: MutableList<EngineGeoPoint> = mutableListOf(),
    var maxDistance: Double = 60.0,
    var distToHomeAuthority: Double? = null,
    var isGpsGap: Boolean = false,
    var trackerBaroAltEma: Double = 0.0,
    var isTrackerMode: Boolean = true,
    val health: SystemHealthState = SystemHealthState(),
    var capabilities: HardwareCapabilities = HardwareCapabilities(),
    
    // Persistence Alerting (Issue #715)
    var forensicReliabilityDegradationStartRt: Long = 0L
) {
    fun update(
        now: Long, nowRt: Long, serviceStartTime: Long, serviceStartRt: Long,
        lastAlarmAckTs: Long, appStartTime: Long, isRelayConnected: Boolean,
        isTrackerConnected: Boolean, discoveryPhase: DiscoveryPhase?,
        trackerLat: Double, trackerLng: Double, trackerGpsAccuracy: Double,
        maxTrackerAccuracy: Double, lastGpsPacketTs: Long, lastGpsPacketRt: Long,
        trackerLastValidFixTs: Long, trackerLastValidFixRt: Long, trackerSpeed: Double,
        jumpTier: Int, isAdaptiveJump: Boolean, trackerBattery: Int, trackerTemp: Double,
        wasDistanceViolated: Boolean, distanceViolationCounter: Int,
        firstViolationTs: Long, firstViolationRt: Long, firstViolationWasJump: Boolean,
        maxDistance: Double,
        distToHomeAuthority: Double?, isGpsGap: Boolean, trackerBaroAltEma: Double,
        isTrackerMode: Boolean, capabilities: HardwareCapabilities
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
    }

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
}

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
