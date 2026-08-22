package com.gps19.core.engine

import kotlinx.serialization.Serializable

/**
 * EngineModels: Data structures for the core tracking engine.
 * Aug.21.09:
 * - Issue #248 Performance Optimization: Segmented HudState into Connectivity, 
 *   Telemetry, and Health components to eliminate UI thread stalls during 
 *   initial hydration on budget hardware (Samsung A15).
 * Aug.20.09:
 * - Issue #226: HUD State Centralization.
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
    var isBatteryCritical: Boolean = false
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
    val remoteSignal: Int = 0
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
    val viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE
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
