package com.gps19.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * EngineModels: Data structures for the core tracking engine.
 * Sep.25.04:
 * - Issue #1324: Added PeerStatusReceived to DomainEvent for bus-driven peer persistence.
 * Sep.25.03:
 * - Issue #1323: Added ViewerLocationUpdated to DomainEvent for bus-driven persistence.
 * Sep.25.01:
 * - Issue #1322: Unified all component-level events (Alarm, Integrity, Processor, 
 *   Connectivity, History, Sensor, Command, Revival) into DomainEvent hierarchy.
 *   Migrated LocationStatus to core engine to support bus-driven health updates.
 * Sep.25.00:
 * - Issue #1325: Fully unified SystemEvaluationSnapshot with all metadata 
 *   required for parity (sats, proximity, vibration, violation stats).
 * - Issue #1326: Corrected satellite count mapping to prevent telemetry corruption.
 * - Refactor: Decommissioned redundant fields in DomainEvent.TickEvaluated to 
 *   enforce snapshot-centric state propagation.
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
    
    fun copyFrom(other: EngineGeoPoint) {
        this.lat = other.lat; this.lng = other.lng; this.alt = other.alt; this.ts = other.ts
        this.rt = other.rt; this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy
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
data class LocationStatus(
    val isPending: Boolean = false,
    val reason: LocationPendingReason = LocationPendingReason.NONE,
    val lastFixRt: Long = 0L,
    val lastPendingDurationMs: Long = 0L,
    val recoveryConfirmed: Boolean = false
)

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
 * SystemEvaluationSnapshot: Unified DTO for all telemetry, health metrics, and sensor data 
 * consumed during a background tick or alarm evaluation. (Issue #1312)
 */
@Serializable
data class SystemEvaluationSnapshot(
    // Kinematic & Location State
    val status: SentinelStatus = SentinelStatus.VALID,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val alt: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val gpsTs: Long = 0L,
    val lastValidFixRt: Long = 0L,
    val distToHome: Double? = null,
    val isStalled: Boolean = false,
    val isClockRegression: Boolean = false,

    // Sentinel & Anomaly State
    val isJammer: Boolean = false,
    val jumpTier: Int = 0,
    val isAdaptiveJump: Boolean = false,
    val tamperDetected: Boolean = false,
    val jammerDetected: Boolean = false,
    val isAnchorLocked: Boolean = false,
    val suppressionNote: String? = null,

    // Environmental & Sensor State
    val vibration: Double = -1.0,
    val heading: Double = -1.0,
    val baroAlt: Double = -1000.0,
    val baroAltEma: Double = -1000.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val tiltDegrees: Double = 0.0,
    val acousticDb: Double = 0.0,
    val peakShock: Double = 0.0,
    val acousticMinDb: Double = -1.0,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Double = 0.12,
    val kineticEnergy: Double = 0.0,
    val peakVerticalVelocity: Double = 0.0,
    val peakVerticalVelocityTs: Long = 0L,
    val peakVerticalVelocityRt: Long = 0L,
    val peakVerticalDisplacement: Double = 0.0,

    // Health & System State
    val batteryLevel: Int = 100,
    val batteryTemp: Double = 0.0,
    val currentMa: Int = 0,
    val isCharging: Boolean = false,
    val isPowerTamper: Boolean = false,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val isGpsHardwareLock: Boolean = false,
    val cpuLoad: Double = 0.0,
    val ioWait: Double = 0.0,
    val maxIoLatency: Long = 0L,
    val isSilentFailure: Boolean = false,
    val isMaliAnomaly: Boolean = false,
    val isUltraLongStationary: Boolean = false,
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    var isSignalLoss: Boolean = false,
    var isGpsStalling: Boolean = false,
    val isGpsGap: Boolean = false,
    val localInternetLoss: Boolean = false,
    val isHardwareOnline: Boolean = true,
    
    // Temporal Gating & Fast-Paths
    val acousticLockoutRt: Long = 0L,
    val lightSpikeRt: Long = 0L,
    val isMuzzled: Boolean = false,
    val providedAdaptiveFloor: Double = -1.0,
    val nowRt: Long = 0L,
    val nowTs: Long = 0L,
    val snrSnapshot: Double? = null,
    val vibeSnapshot: Double? = null,

    // Metadata for Signaling & Persistence (Issue #1325)
    val satsUsed: Int = -1,
    val satsView: Int = -1,
    val proxIdx: Double = 0.0,
    val proximityCm: Double = -1.0,
    val proximityDebounceMs: Long = 0L,
    val vibrationRollingSum: Double = 0.0,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Double = 0.0,
    
    // Warm-up & Audio State
    val isWarming: Boolean = false,
    val isSirenActive: Boolean = false
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
 * Component-level event containers. (Issue #1322: DomainEventBus convergence)
 */

sealed class AlarmEvent {
    data class LogEvent(
        val type: String, val message: String, val isImportant: Boolean, 
        val extremeValue: Double?, val logId: String?, val durationMs: Long, 
        val isSpecial: Boolean, val specialColor: Int?, 
        val lat: Double, val lng: Double, val accuracy: Double, 
        val maxAccuracy: Double, val snr: Double?, val vibe: Double?
    ) : AlarmEvent()
}

sealed class IntegrityEvent {
    data class ViolationSustained(val type: String) : IntegrityEvent()
    data class ViolationResolved(val type: String) : IntegrityEvent()
    data class LogEvent(val message: String, val isImportant: Boolean) : IntegrityEvent()
    data class LocationStatusChanged(val status: LocationStatus) : IntegrityEvent()
    data class GnssThrottledChanged(val throttled: Boolean) : IntegrityEvent()
}

sealed class ProcessorEvent {
    data class TrailPointSaved(val lat: Double, val lng: Double, val isViewerTrail: Boolean, val status: SentinelStatus, val timestamp: Long, val accuracy: Double, val maxAccuracy: Double) : ProcessorEvent()
    data class LogAdded(val message: String, val type: String, val isImportant: Boolean, val isSpecial: Boolean, val lat: Double, val lng: Double, val accuracy: Double, val snr: Double?, val vibe: Double?) : ProcessorEvent()
    data class MaxAccuracyChanged(val accuracy: Double) : ProcessorEvent()
    data class ChairBaselineChanged(val baseline: Double) : ProcessorEvent()
    data class VibrationFloorChanged(val floor: Double) : ProcessorEvent()
    data class LuxBaselineChanged(val baseline: Double) : ProcessorEvent()
    data class AcousticFloorChanged(val floor: Double) : ProcessorEvent()
    data class GpsStallDetected(val rt: Long) : ProcessorEvent()
}

sealed class ConnectivityEvent {
    data class PeerPulse(val id: String) : ConnectivityEvent()
}

sealed class HistoryEvent {
    data class LogEvent(val message: String, val isImportant: Boolean) : HistoryEvent()
}

sealed class AppSensorEvent {
    data class HardwareFailure(val reason: String) : AppSensorEvent()
    data class LogEvent(val message: String, val isImportant: Boolean) : AppSensorEvent()
}

sealed class CommandEvent {
    object WatchdogTrigger : CommandEvent()
    object UiPulse : CommandEvent()
    data class UiVisibilityChanged(val visible: Boolean) : CommandEvent()
    object ResetTimers : CommandEvent()
    object SyncSensors : CommandEvent()
    object ExecuteStressTest : CommandEvent()
    data class SimulateStoragePressure(val active: Boolean, val isCritical: Boolean) : CommandEvent()
}

sealed class RevivalEvent {
    data class Attempt(val count: Int) : RevivalEvent()
    object HardwareLock : RevivalEvent()
    object Success : RevivalEvent()
    object RawBurstStarted : RevivalEvent()
    object RawBurstEnded : RevivalEvent()
    data class Footprint(val deltaMa: Int, val deltaTemp: Double, val durationMs: Long) : RevivalEvent()
}

/**
 * DomainEvent: Unified event hierarchy for cross-component orchestration. (Issue #1291, #1322)
 */
sealed class DomainEvent {
    data class TickEvaluated(
        val now: Long,
        val nowRt: Long,
        val isTrackerMode: Boolean,
        val snapshot: SystemEvaluationSnapshot,
        val processed: ProcessedLocation?,
        val health: SystemHealthState,
        val isSocketConnected: Boolean,
        val isPeerActive: Boolean,
        val serviceTickCounter: Long,
        val rtt: Int,
        val recoveryFlagged: Boolean = false,
        
        // Metadata for Signaling & Ribbons
        val gnssDetail: GnssDetail? = null,
        val isSuspiciousMode: Boolean = false,
        val lastSitTs: Long = 0L,
        val lastTickTs: Long = 0L,
        val lastTickRt: Long = 0L,
        val noiseIdx: Double = 0.0,
        val luxIdx: Double = 0.0,
        val vibeIdx: Double = 0.0,
        val liftIdx: Double = 0.0,
        val snrIdx: Double = 0.0,
        val tiltIdx: Double = 0.0,
        val baroIdx: Double = 0.0
    ) : DomainEvent()

    data class PowerSaveTransition(val isEngaged: Boolean) : DomainEvent()

    data class ViewerLocationUpdated(
        val processed: ProcessedLocation,
        val snapshot: SystemEvaluationSnapshot,
        val health: SystemHealthState,
        val nowRt: Long,
        val nowTs: Long
    ) : DomainEvent()
    
    data class PeerStatusReceived(
        val status: LocationUpdate
    ) : DomainEvent()

    data class HeuristicRecovery(
        val message: String,
        val gapMs: Long,
        val lat: Double,
        val lng: Double,
        val accuracy: Double
    ) : DomainEvent()

    data class StabilityViolation(
        val message: String,
        val isJitter: Boolean,
        val lat: Double,
        val lng: Double,
        val accuracy: Double
    ) : DomainEvent()
    
    data class ServiceStatus(val message: String, val isImportant: Boolean = false) : DomainEvent()

    // Issue #1322: Component wrappers
    data class Alarm(val event: AlarmEvent) : DomainEvent()
    data class Integrity(val event: IntegrityEvent) : DomainEvent()
    data class Processor(val event: ProcessorEvent, val isPrimary: Boolean) : DomainEvent()
    data class Connectivity(val event: ConnectivityEvent) : DomainEvent()
    data class History(val event: HistoryEvent) : DomainEvent()
    data class Sensor(val event: AppSensorEvent) : DomainEvent()
    data class Command(val event: CommandEvent) : DomainEvent()
    data class Revival(val event: RevivalEvent) : DomainEvent()
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

/**
 * LocationProcessingState: Consolidated operational state for LocationProcessor, 
 * LocationSentinel, and GtoEngine.
 */
@Serializable
class LocationProcessingState {
    // LocationProcessor State
    var lastProcessedAccuracy: Double = 0.0
    var maxAccuracy: Double = 0.0
    var accuracyWindowBuffer: DoubleArray = DoubleArray(ACCURACY_WINDOW_MAX_SIZE)
    var accuracyWindowSize: Int = 0
    var accuracyWindowHead: Int = 0
    var lastWindowUpdateRt: Long = 0L
    var lastValidFixRt: Long = 0L
    var lastLat: Double = 0.0
    var lastLng: Double = 0.0
    var lastTs: Long = 0L
    var lastRt: Long = 0L
    var lastAcc: Double = 0.0
    var lastMaxAcc: Double = 0.0
    var lastSavedLat: Double = 0.0
    var lastSavedLng: Double = 0.0
    var lastSavedTs: Long = 0L
    var lastSavedRt: Long = 0L
    var lastSavedGpsTs: Long = 0L
    var lastHighAccLat: Double = 0.0
    var lastHighAccLng: Double = 0.0
    var lastHighAccTs: Long = 0L
    var lastHighAccRt: Long = 0L
    var lastExpectedIntervalMs: Long = 0L
    var lastIntervalChangeRt: Long = 0L
    var lastNearestHomeDistance: Double? = null
    var lastDistanceToTracker: Double? = null
    var maxDistanceAuthority: Double = 60.0

    // LocationSentinel State
    var sentinelLastValidLat: Double = 0.0
    var sentinelLastValidLng: Double = 0.0
    var sentinelLastValidAlt: Double = 0.0
    var sentinelLastValidTs: Long = 0L
    var sentinelLastValidRt: Long = 0L
    var sentinelLastValidSpeedMps: Double = 0.0
    var sentinelLastValidBearing: Double = 0.0
    var sentinelLastValidAccuracy: Double = 0.0
    var estimatedSpeedMps: Double = 0.0
    var estimatedBearing: Double = 0.0
    var stationaryProb: Double = 1.0
    var currentVibrationIndex: Double = 0.0
    var peakVibrationShock: Double = 0.0
    var peakVibrationShockRt: Long = 0L
    var currentCompassHeading: Double = 0.0
    var lastCompassHeading: Double = 0.0
    var currentBaroAlt: Double = 0.0
    var currentLux: Double = 0.0
    var isNear: Boolean = true
    var isPowerTamper: Boolean = false
    var currentTiltDegrees: Double = 0.0
    var currentAcousticDb: Double = 0.0
    var lastFastPathAcousticSpikeRt: Long = 0L
    var lastFastPathLightSpikeRt: Long = 0L
    var kineticEnergy: Double = 0.0
    var isSitDetected: Boolean = false
    var lastSitTs: Long = 0L
    var lastSitRt: Long = 0L
    var baselineSitTilt: Double = -1.0
    var lastSitVz: Double = 0.0
    var lastSitVzTs: Long = 0L
    var lastSitVzRt: Long = 0L
    var lastSitDz: Double = 0.0
    var lastSitBaro: Double = 0.0
    var lastSitTilt: Double = 0.0
    var lastSitShock: Double = 0.0
    var sitDetectionCooldownRt: Long = 0L
    var stationaryStartRt: Long = 0L
    var gpsMotionStartRt: Long = 0L
    var luxBaseline: Double = -1.0
    var baroBaseline: Double = -1000.0
    var acousticFloorDb: Double = -1.0
    var adaptiveVibrationFloor: Double = INITIAL_VIBRATION_FLOOR
    var lastAcousticContractionRt: Long = 0L
    var lastSnr: Double = 0.0
    var lastSatsUsed: Int = 0

    // GtoEngine State (Window Size 5)
    var gtoLatBuffer: DoubleArray = DoubleArray(5)
    var gtoLngBuffer: DoubleArray = DoubleArray(5)
    var gtoAltBuffer: DoubleArray = DoubleArray(5)
    var gtoAccBuffer: DoubleArray = DoubleArray(5)
    var gtoMaxAccBuffer: DoubleArray = DoubleArray(5)
    var gtoBearingBuffer: DoubleArray = DoubleArray(5)
    var gtoSpeedBuffer: DoubleArray = DoubleArray(5)
    var gtoTsBuffer: LongArray = LongArray(5)
    var gtoRtBuffer: LongArray = LongArray(5)
    var gtoVibeBuffer: DoubleArray = DoubleArray(5)
    var gtoHead: Int = 0
    var gtoSize: Int = 0
    
    // AnchorEvaluator State
    var parkingAnchorPoint: EngineGeoPoint = EngineGeoPoint()
    var isAnchorActive: Boolean = false
    var anchorEscapeScore: Double = 0.0
    var anchorTrendPoints: MutableList<EngineGeoPoint> = MutableList(3) { EngineGeoPoint() }
    var trendCount: Int = 0
    var trendIdx: Int = 0
    var anchorAveragingBuffer: MutableList<EngineGeoPoint> = MutableList(8) { EngineGeoPoint() }
    var averageCount: Int = 0
    var averageIdx: Int = 0
    var isAnchorLockedState: Boolean = false
    var optimizedPointFlyweight: EngineGeoPoint = EngineGeoPoint()

    @Transient
    var cachedHomePoints: List<EngineGeoPoint>? = null
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
    val isAdaptiveJump: Boolean = false
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
        // this.isAdaptiveJump = isAdaptiveJump (Fix for val property)
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
