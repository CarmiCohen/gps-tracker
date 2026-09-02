package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * Sep.02.68:
 * - Idea #243: Flattened StatusBar indicator chain. Added isSystemActive 
 *   to HudConnectivityState to support unified state propagation (R243).
 * Sep.02.01:
 * - Issue #897: Added vibrationSensitivity and tiltSensitivity to 
 *   AlarmEvaluationState for dynamic thresholding (R2.3).
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
        tiltSensitivity: Float = 0.5f
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
    }
}

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1), SIXTEEN_MIN("16M", 4), ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60), TWENTY_FOUR_HOUR("24H", 360), SEVEN_DAY("7D", 2700)
}

class EngineSnrSample(var ts: Long = 0L, var rt: Long = 0L, var snr: Double = 0.0)

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

/**
 * HUD Component States: Segmented for performance on budget hardware.
 */
@Serializable
data class HudConnectivityState(
    val appMode: String? = null,
    val isInternet: Boolean = false,
    val isRelayConnected: Boolean = false,
    val isTelemetryFresh: Boolean = false,
    val isDataHealthy: Boolean = false,
    val commIndex: Int = 0,
    val remoteCommIndex: Int = 0,
    val trackerId: String = "TRK",
    val viewerId: String = "VIEW",
    val watchdogOk: Boolean = true,
    val rtt: Int = 0,
    val remoteSignal: Int = 0,
    val isSystemActive: Boolean = false
)

@Serializable
data class HudTelemetryState(
    val isLocalGpsActive: Boolean = false,
    val isGpsFresh: Boolean = false,
    val speedMps: Float = 0f,
    val trackerAccuracy: Float = 0f,
    val maxTrackerAccuracy: Float = 0f,
    val viewerAccuracy: Float = 0f,
    val maxViewerAccuracy: Float = 0f,
    val satsUsed: Int = 0,
    val satsView: Int = 0,
    val viewerSatsUsed: Int = 0,
    val viewerSatsView: Int = 0,
    val distToHome: Double? = null,
    val distToViewer: Double? = null,
    val lastGpsTs: Long = 0L,
    val viewerGpsTs: Long = 0L,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val isTrackerLocPending: Boolean = false,
    val trackerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isViewerLocPending: Boolean = false,
    val viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isUltraLongStationary: Boolean = false
)

@Serializable
data class HudHealthState(
    val battery: Int = 100,
    val remoteBattery: Int = -1,
    val isCharging: Boolean = false,
    val remoteCharging: Boolean = false,
    val trackerTemp: Float = 0f,
    val viewerTemp: Float = 0f,
    val hasActiveAlarms: Boolean = false,
    val isRedScreenSuppressed: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val activeAlarms: List<AlarmInfo> = emptyList(),
    val progressPulse: Float = 0f,
    val systemPulse: Long = 0L
)

/**
 * Monolithic HudState retained as a facade for legacy Compose compatibility 
 * while aggregator transitions to segmented emissions.
 */
@Serializable
data class HudState(
    val connectivity: HudConnectivityState = HudConnectivityState(),
    val telemetry: HudTelemetryState = HudTelemetryState(),
    val health: HudHealthState = HudHealthState()
) {
    // Convenience properties for legacy UI code
    val appMode get() = connectivity.appMode
    val isInternet get() = connectivity.isInternet
    val isRelayConnected get() = connectivity.isRelayConnected
    val isTelemetryFresh get() = connectivity.isTelemetryFresh
    val isDataHealthy get() = connectivity.isDataHealthy
    val commIndex get() = connectivity.commIndex
    val remoteCommIndex get() = connectivity.remoteCommIndex
    val trackerId get() = connectivity.trackerId
    val viewerId get() = connectivity.viewerId
    val watchdogOk get() = connectivity.watchdogOk
    val isSystemActive get() = connectivity.isSystemActive
    
    val isLocalGpsActive get() = telemetry.isLocalGpsActive
    val isGpsFresh get() = telemetry.isGpsFresh
    val speedMps get() = telemetry.speedMps
    val trackerAccuracy get() = telemetry.trackerAccuracy
    val maxTrackerAccuracy get() = telemetry.maxTrackerAccuracy
    val viewerAccuracy get() = telemetry.viewerAccuracy
    val maxViewerAccuracy get() = telemetry.maxViewerAccuracy
    val satsUsed get() = telemetry.satsUsed
    val satsView get() = telemetry.satsView
    val viewerSatsUsed get() = telemetry.viewerSatsUsed
    val viewerSatsView get() = telemetry.viewerSatsView
    val distToHome get() = telemetry.distToHome
    val distToViewer get() = telemetry.distToViewer
    val lastGpsTs get() = telemetry.lastGpsTs
    val viewerGpsTs get() = telemetry.viewerGpsTs
    val trackerState get() = telemetry.trackerState
    val isTrackerLocPending get() = telemetry.isTrackerLocPending
    val trackerLocPendingReason get() = telemetry.trackerLocPendingReason
    val isViewerLocPending get() = telemetry.isViewerLocPending
    val viewerLocPendingReason get() = telemetry.viewerLocPendingReason
    val isUltraLongStationary get() = telemetry.isUltraLongStationary

    val battery get() = health.battery
    val remoteBattery get() = health.remoteBattery
    val isCharging get() = health.isCharging
    val remoteCharging get() = health.remoteCharging
    val trackerTemp get() = health.trackerTemp
    val viewerTemp get() = health.viewerTemp
    val hasActiveAlarms get() = health.hasActiveAlarms
    val isRedScreenSuppressed get() = health.isRedScreenSuppressed
    val isSirenPlaying get() = health.isSirenPlaying
    val activeAlarms get() = health.activeAlarms
    val progressPulse get() = health.progressPulse
    val systemPulse get() = health.systemPulse
}
