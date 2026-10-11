package com.gps19.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.concurrent.ConcurrentHashMap

/**
 * EngineModels: Data structures for the core tracking engine.
 * Oct.11.1:
 * - Issue #SIMP-1011-5: Acoustic Profiling. Updated NativeFastPathProvider 
 *   to accept TimeProvider for standardized latency auditing.
 * Oct.10.1 (Restoration Path):
 * - Issue #SIMP-1014-2: Unified Pressure Path. Added SystemPressureBatch 
 *   DTO and updated events/interfaces for consolidated JNI pressure evaluation.
 * - Issue #SIMP-1012-2: Native Proximity Scaling. Added ProximityBatch for JNI debouncing.
 * - Issue #SIMP-1011-3: Forensic Buffer Consolidation. Unified ForensicSample.
 * - Issue #SIMP-1011-2: Acoustic JNI Offloading. JNI audio processing.
 * - Issue #SIMP-1011-1: Native GNSS Batching. Native SV evaluation.
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

@Serializable
enum class ActivityType { STILL, WALKING, RUNNING, BICYCLING, IN_VEHICLE, TILTING, UNKNOWN }

@Serializable
enum class AppRole(val prefix: String) {
    TRACKER("T_"),
    VIEWER_REMOTE("VR_"),
    VIEWER_SELF("V_");

    companion object {
        fun fromKey(key: String): Pair<AppRole, String>? {
            val role = entries.sortedByDescending { it.prefix.length }
                .find { key.startsWith(it.prefix) } ?: return null
            return role to key.removePrefix(role.prefix)
        }
    }
}

enum class DiscoveryPhase { BOOTSTRAP, DISCOVERING, MONITORING }
enum class SentinelStatus { VALID, JUMP, TAMPER, TRAJECTORY_PROMOTED, OUTLIER, JITTER, JAMMER_SUSPICION }
enum class SignalingPriority { HIGH, NORMAL }
enum class CapabilityStatus { GRANTED, DENIED, UNKNOWN }

@Serializable
enum class PerformanceTier { STANDARD, STAGGERED }

@Serializable
enum class MemoryPressureLevel { NORMAL, HIGH, CRITICAL }

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

enum class LocationPendingReason { NONE, GPS_STALL, GPS_GAP, ACOUSTIC_VIOLATION, SIGNAL_LOSS, JAMMER_SUSPICION }

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
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,
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
    var isUltraLongStationary: Boolean = false,
    var violationUptimeMs: Long = 0L,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    var activityType: ActivityType = ActivityType.UNKNOWN,
    val forensic: ForensicSnapshot = ForensicSnapshot()
) {
    var snrSnapshot: Double?
        get() = forensic.snr
        set(value) { forensic.snr = value }
    var vibeSnapshot: Double?
        get() = forensic.vibe
        set(value) { forensic.vibe = value }
    var thermalSnapshot: Double?
        get() = forensic.thermal
        set(value) { forensic.thermal = value }
    var heapSnapshot: Double?
        get() = forensic.heap
        set(value) { forensic.heap = value }

    fun copyFrom(other: EngineConnectionPoint) {
        this.ts = other.ts; this.rt = other.rt; this.rtt = other.rtt; this.remoteSig = other.remoteSig
        this.isConnected = other.isConnected; this.isGap = other.isGap; this.isRecoveryEvent = other.isRecoveryEvent
        this.hasGps = other.hasGps; this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy
        this.isBatteryLow = other.isBatteryLow; this.isBatteryCritical = other.isBatteryCritical
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
        this.isSilentFailure = other.isSilentFailure
        this.isUltraLongStationary = other.isUltraLongStationary; this.violationUptimeMs = other.violationUptimeMs
        this.thermalHeadroom = other.thermalHeadroom; this.heapAllocatedMb = other.heapAllocatedMb
        this.activityType = other.activityType
        this.forensic.copyFrom(other.forensic)
    }
}

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
    val role: AppRole = AppRole.TRACKER
)

/**
 * EventPriority: Defines the criticality of domain events for backpressure handling.
 */
enum class EventPriority { LOW, NORMAL, HIGH, CRITICAL }

sealed class DomainEvent(open val priority: EventPriority = EventPriority.NORMAL) {
    data class TickEvaluated(
        val now: Long,
        val nowRt: Long,
        val isTrackerMode: Boolean,
        val snapshot: LocationUpdate,
        val processed: ProcessedLocation?,
        val health: SystemHealthState,
        val isSocketConnected: Boolean,
        val isPeerActive: Boolean,
        val serviceTickCounter: Long,
        val rtt: Int,
        val recoveryFlagged: Boolean = false,
        val gnssDetail: GnssDetail? = null,
        val isSuspiciousMode: Boolean = false,
        val lastSitTs: Long = 0L,
        val lastTickTs: Long = 0L,
        val lastTickRt: Long = 0L
    ) : DomainEvent(EventPriority.NORMAL)

    data class PowerSaveTransition(val isEngaged: Boolean) : DomainEvent(EventPriority.HIGH)
    data class PeerStatusReceived(val status: LocationUpdate) : DomainEvent(EventPriority.NORMAL)
    data class PeerConnectionChanged(val isConnected: Boolean, val peerId: String) : DomainEvent(EventPriority.HIGH)
    data class HeuristicRecovery(val message: String, val gapMs: Long, val lat: Double, val lng: Double, val accuracy: Double) : DomainEvent(EventPriority.HIGH)
    class StabilityViolation(val message: String, val isJitter: Boolean, val lat: Double, val lng: Double, val accuracy: Double) : DomainEvent(EventPriority.HIGH)
    data class ServiceStatus(val message: String, val isImportant: Boolean = false) : DomainEvent(if (isImportant) EventPriority.NORMAL else EventPriority.LOW)
}

sealed class AlarmEvent(override val priority: EventPriority = EventPriority.CRITICAL) : DomainEvent(priority) {
    data class LogEvent(
        val type: String, val message: String, val isImportant: Boolean, 
        val extremeValue: Double?, val logId: String?, val durationMs: Long, 
        val isSpecial: Boolean, val specialColor: Int?, 
        val lat: Double, val lng: Double, val accuracy: Double, 
        val maxAccuracy: Double, val forensic: ForensicSnapshot = ForensicSnapshot()
    ) : AlarmEvent(if (isImportant) EventPriority.CRITICAL else EventPriority.HIGH)
}

sealed class IntegrityEvent(override val priority: EventPriority = EventPriority.HIGH) : DomainEvent(priority) {
    data class ViolationSustained(val type: String) : IntegrityEvent(EventPriority.CRITICAL)
    data class ViolationResolved(val type: String) : IntegrityEvent(EventPriority.CRITICAL)
    data class LogEvent(val message: String, val isImportant: Boolean) : IntegrityEvent(if (isImportant) EventPriority.HIGH else EventPriority.NORMAL)
    data class LocationStatusChanged(val status: LocationStatus) : IntegrityEvent(EventPriority.HIGH)
    data class GnssThrottledChanged(val throttled: Boolean) : IntegrityEvent(EventPriority.NORMAL)
    data class MemoryPressureChanged(val level: MemoryPressureLevel, val heapMb: Double) : IntegrityEvent(EventPriority.HIGH)
    data class StoragePressureChanged(val isLow: Boolean, val isCritical: Boolean, val availableMb: Long) : IntegrityEvent(EventPriority.HIGH)
}

sealed class ProcessorEvent(open val isPrimary: Boolean, override val priority: EventPriority = EventPriority.NORMAL) : DomainEvent(priority) {
    data class TrailPointSaved(val lat: Double, val lng: Double, val isViewerTrail: Boolean, val status: SentinelStatus, val timestamp: Long, val accuracy: Double, val maxAccuracy: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.NORMAL)
    data class LogAdded(val message: String, val type: String, val isImportant: Boolean, val isSpecial: Boolean, val lat: Double, val lng: Double, val accuracy: Double, val forensic: ForensicSnapshot = ForensicSnapshot(), override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, if (isImportant) EventPriority.HIGH else EventPriority.LOW)
    data class MaxAccuracyChanged(val accuracy: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.LOW)
    data class ChairBaselineChanged(val baseline: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.LOW)
    data class VibrationFloorChanged(val floor: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.LOW)
    data class LuxBaselineChanged(val baseline: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.LOW)
    data class AcousticFloorChanged(val floor: Double, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.LOW)
    data class GpsStallDetected(val rt: Long, override val isPrimary: Boolean = true) : ProcessorEvent(isPrimary, EventPriority.HIGH)
}

sealed class ConnectivityEvent(override val priority: EventPriority = EventPriority.NORMAL) : DomainEvent(priority) {
    data class PeerPulse(val id: String) : ConnectivityEvent(EventPriority.NORMAL)
}

sealed class HistoryEvent(override val priority: EventPriority = EventPriority.LOW) : DomainEvent(priority) {
    data class LogEvent(val message: String, val isImportant: Boolean) : HistoryEvent(if (isImportant) EventPriority.NORMAL else EventPriority.LOW)
}

sealed class AppSensorEvent(override val priority: EventPriority = EventPriority.NORMAL) : DomainEvent(priority) {
    data class HardwareFailure(val reason: String) : AppSensorEvent(EventPriority.HIGH)
    data class LogEvent(val message: String, val isImportant: Boolean) : AppSensorEvent(if (isImportant) EventPriority.NORMAL else EventPriority.LOW)
}

sealed class CommandEvent(override val priority: EventPriority = EventPriority.HIGH) : DomainEvent(priority) {
    object WatchdogTrigger : CommandEvent(EventPriority.CRITICAL)
    object UiPulse : CommandEvent(EventPriority.NORMAL)
    data class UiVisibilityChanged(val visible: Boolean) : CommandEvent(EventPriority.NORMAL)
    object ResetTimers : CommandEvent(EventPriority.HIGH)
    object SyncSensors : CommandEvent(EventPriority.NORMAL)
    object ExecuteStressTest : CommandEvent(EventPriority.NORMAL)
    object ExecuteLogPressureTest : CommandEvent(EventPriority.NORMAL)
    object ExecuteNetworkStressTest : CommandEvent(EventPriority.NORMAL)
    data class SimulateStoragePressure(val active: Boolean, val isCritical: Boolean) : CommandEvent(EventPriority.HIGH)
    object TriggerMemoryFlush : CommandEvent(EventPriority.HIGH)
    object TriggerStoragePrune : CommandEvent(EventPriority.HIGH)
}

sealed class RevivalEvent(override val priority: EventPriority = EventPriority.NORMAL) : DomainEvent(priority) {
    data class Attempt(val count: Int) : RevivalEvent(EventPriority.HIGH)
    object HardwareLock : RevivalEvent(EventPriority.CRITICAL)
    object Success : RevivalEvent(EventPriority.HIGH)
    object RawBurstStarted : RevivalEvent(EventPriority.NORMAL)
    object RawBurstEnded : RevivalEvent(EventPriority.NORMAL)
    data class Footprint(val deltaMa: Int, val deltaTemp: Double, val durationMs: Long) : RevivalEvent(EventPriority.LOW)
}

interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long
    val rt: Long
}

interface Locatable {
    val isLocationPending: Boolean
    val locationPendingReason: LocationPendingReason
}

interface BatteryProvider {
    val battery: Int
    val isCharging: Boolean
}

interface DeviceIdentity {
    val trackerId: String
    val viewerId: String
}

/**
 * VibrationBatch: Data transfer object for JNI batching (Issue #1450).
 */
@Serializable
class VibrationBatch {
    // Inputs
    var x: Double = 0.0; var y: Double = 0.0; var z: Double = 0.0
    var lx: Double = 0.0; var ly: Double = 0.0; var lz: Double = 0.0
    var adaptiveFloor: Double = 0.0
    var isWarming: Boolean = false
    var cpuLoad: Double = 0.0
    var lastRawVibe: Double = 0.0
    var lastHpfValue: Double = 0.0
    var currentEnergy: Double = 0.0
    var snr: Double = -1.0
    var thermal: Double = -1.0
    var heap: Double = -1.0
    var nowRt: Long = 0L
    
    // Outputs
    var delta: Double = 0.0
    var nextFloor: Double = 0.0
    var nextHpf: Double = 0.0
    var nextEnergy: Double = 0.0
    var isStationary: Boolean = false
    var isSuspiciousNoise: Boolean = false
    var isMemoryPressureThrottled: Boolean = false
    var stationaryDuration: Long = 0L
    var muzzleResetTriggered: Boolean = false
    var isJammingCandidate: Boolean = false
}

/**
 * GnssHealthBatch: Data transfer object for JNI GNSS processing (Issue #SIMP-1011-1).
 */
@Serializable
class GnssHealthBatch {
    var count: Int = 0
    var svid: IntArray = IntArray(64)
    var cn0: FloatArray = FloatArray(64)
    var usedInFix: BooleanArray = BooleanArray(64)
    var constellation: IntArray = IntArray(64)
    var satellitesInView: Int = 0
    var satellitesUsed: Int = 0
    var averageSnr: Double = 0.0
}

/**
 * AcousticBatch: Data transfer object for JNI audio processing (Issue #SIMP-1011-2).
 */
@Serializable
class AcousticBatch {
    var readCount: Int = 0
    var baseAlpha: Double = 0.0
    var vibrationRollingSum: Double = 0.0
    var nowRt: Long = 0L
    var isWarming: Boolean = false
    var maxAmp: Int = 0
    var db: Double = 0.0
    var isSpike: Boolean = false
    var lastSpikeRt: Long = 0L
}

/**
 * ProximityBatch: Data transfer object for JNI proximity processing (Issue #SIMP-1012-2).
 */
@Serializable
class ProximityBatch {
    var distance: Double = 0.0
    var maxRange: Double = 0.0
    var nowRt: Long = 0L
    var isStationary: Boolean = false
    var stationaryDurationMs: Long = 0L
    var isHighLoad: Boolean = false
    var currentIdx: Double = 0.0
    var rawNear: Boolean = false
    var isFlickering: Boolean = false
    var nextIdx: Double = 0.0
    var nextRawNear: Boolean = false
    var debounceMs: Long = 0L
}

/**
 * SystemPressureBatch: Unified container for Memory and Storage pressure evaluation.
 * Issue #SIMP-1014-2: Consolidation of pressure gates into a single JNI crossing.
 */
@Serializable
class SystemPressureBatch {
    var heapMb: Double = 0.0
    var memPressureThresholdMb: Double = 0.0
    var memCriticalThresholdMb: Double = 0.0
    var memHysteresisOffsetMb: Double = 0.0
    var storageAvailableMb: Double = 0.0
    var storageLowThresholdMb: Double = 0.0
    var storageCriticalThresholdMb: Double = 0.0
    var storageHysteresisOffsetMb: Double = 0.0
    var currentMemLevel: Int = 0 // 0: Normal, 1: High, 2: Critical
    var needsMemFlush: Boolean = false
    var currentStorageLevel: Int = 0 // 0: Normal, 1: Low, 2: Critical
    var needsStoragePrune: Boolean = false
}

/**
 * ForensicSample: Unified container for forensic telemetry samples (Issue #SIMP-1011-3).
 */
@Serializable
class ForensicSample(
    var ts: Long = 0L,
    var rt: Long = 0L,
    var snr: Double = 0.0,
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
    var kineticEnergy: Double = 0.0,
    var activityType: ActivityType = ActivityType.UNKNOWN
) {
    fun reset() {
        ts = 0L; rt = 0L; snr = 0.0; acoustic = 0.0; lux = 0.0; vibe = 0.0
        proxIdx = 0.0; lift = 0.0; tilt = 0.0; isSitDetected = false
        sitVzTs = 0L; sitVzRt = 0L; sitShock = 0.0; kineticEnergy = 0.0
        activityType = ActivityType.UNKNOWN
    }
    
    fun copyFrom(other: ForensicSample) {
        this.ts = other.ts; this.rt = other.rt; this.snr = other.snr; this.acoustic = other.acoustic
        this.lux = other.lux; this.vibe = other.vibe; this.proxIdx = other.proxIdx; this.lift = other.lift
        this.tilt = other.tilt; this.isSitDetected = other.isSitDetected; this.sitVzTs = other.sitVzTs
        this.sitVzRt = other.sitVzRt; this.sitShock = other.sitShock; this.kineticEnergy = other.kineticEnergy
        this.activityType = other.activityType
    }
}

/**
 * NativeFastPathProvider: Interface for offloading math to JNI (Issue #SIMP-1510-1).
 */
interface NativeFastPathProvider {
    fun isStationary(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean
    fun updateVibrationFloor(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double
    fun computeNextHpf(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double
    fun computeNextEnergy(currentEnergy: Double, hpfValue: Double): Double
    fun calculateVibrationDelta(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double
    fun isShockViolated(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean
    fun isVibrationSuspicious(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean
    fun processVibrationBatch(timeProvider: TimeProvider, batch: VibrationBatch): Boolean
    fun processGnssBatch(timeProvider: TimeProvider, batch: GnssHealthBatch): Boolean
    fun processAcousticBatch(timeProvider: TimeProvider, batch: AcousticBatch, buffer: ShortArray): Boolean
    fun processProximityBatch(timeProvider: TimeProvider, batch: ProximityBatch): Boolean
    fun processSystemPressure(timeProvider: TimeProvider, batch: SystemPressureBatch): Boolean
}

@Serializable
data class RejectedPoint(
    val lat: Double, val lng: Double, val alt: Double, val accuracy: Double, 
    val bearing: Double, val speedMps: Double, val ts: Long, val rt: Long
)

@Serializable
class TrajectoryNode(
    var lat: Double = 0.0, 
    var lng: Double = 0.0, 
    var alt: Double = 0.0, 
    var accuracy: Double = 0.0, 
    var maxAccuracy: Double = 0.0, 
    var bearing: Double = 0.0, 
    var speedMps: Double = 0.0, 
    var ts: Long = 0L, 
    var rt: Long = 0L, 
    var vibrationIndex: Double = 0.0
) {
    fun update(
        lat: Double, lng: Double, alt: Double, accuracy: Double, maxAccuracy: Double, 
        bearing: Double, speedMps: Double, ts: Long, rt: Long, vibrationIndex: Double
    ) {
        this.lat = lat; this.lng = lng; this.alt = alt; this.accuracy = accuracy; this.maxAccuracy = maxAccuracy
        this.bearing = bearing; this.speedMps = speedMps; this.ts = ts; this.rt = rt; this.vibrationIndex = vibrationIndex
    }
    fun reset() { update(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0.0) }
}

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
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE

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
        locationPendingReason = LocationPendingReason.NONE
        rawPoint.update(0.0, 0.0)
        optimizedPoint.update(0.0, 0.0)
    }

    fun copyFrom(other: ProcessedLocation) {
        this.rawPoint.copyFrom(other.rawPoint)
        this.optimizedPoint.copyFrom(other.optimizedPoint)
        this.status = other.status
        this.maxAccuracy = other.maxAccuracy
        this.currentAccuracy = other.currentAccuracy
        this.filteredSpeed = other.filteredSpeed
        this.timestamp = other.timestamp
        this.rt = other.rt
        this.isStalled = other.isStalled
        this.isClockRegression = other.isClockRegression
        this.receiptRt = other.receiptRt
        this.isTrajectoryPromoted = other.isTrajectoryPromoted
        this.jumpTier = other.jumpTier
        this.isAdaptiveJump = other.isAdaptiveJump
        this.distToHome = other.distToHome
        this.isSpatiallyValid = other.isSpatiallyValid
        this.geofenceViolationDetected = other.geofenceViolationDetected
        this.tamperDetected = other.tamperDetected
        this.jammerDetected = other.jammerDetected
        this.isAnchorLocked = other.isAnchorLocked
        this.suppressionNote = other.suppressionNote
        this.kineticEnergy = other.kineticEnergy
        this.locationPendingReason = other.locationPendingReason
    }
}

@Serializable
class AccuracyState {
    var lastProcessedAccuracy: Double = 0.0
    var maxAccuracy: Double = 0.0
    var windowBuffer: DoubleArray = DoubleArray(ACCURACY_WINDOW_MAX_SIZE)
    var windowSize: Int = 0
    var windowHead: Int = 0
    var lastUpdateRt: Long = 0L
}

@Serializable
class SentinelForensicState {
    var lastValidLat: Double = 0.0
    var lastValidLng: Double = 0.0
    var lastValidAlt: Double = 0.0
    var lastValidTs: Long = 0L
    var lastValidRt: Long = 0L
    var lastValidSpeedMps: Double = 0.0
    var lastValidBearing: Double = 0.0
    var lastValidAccuracy: Double = 0.0
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
    var stationaryDurationMs: Long = 0L
    var gpsMotionStartRt: Long = 0L
    var luxBaseline: Double = -1.0
    var baroBaseline: Double = -1000.0
    var acousticFloorDb: Double = -1.0
    var adaptiveVibrationFloor: Double = INITIAL_VIBRATION_FLOOR
    var lastAcousticContractionRt: Long = 0L
    var lastSnr: Double = 0.0
    var lastSatsUsed: Int = 0
    var isSuspiciousNoise: Boolean = false
    var isMemoryPressureThrottled: Boolean = false
    var isJammingCandidate: Boolean = false
}

@Serializable
class TrajectoryBuffer {
    var latBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var lngBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var altBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var accBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var maxAccBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var bearingBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var speedBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var tsBuffer: LongArray = LongArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var rtBuffer: LongArray = LongArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var vibeBuffer: DoubleArray = DoubleArray(TRAJECTORY_BUFFER_MAX_SIZE)
    var head: Int = 0
    var size: Int = 0
}

@Serializable
class AnchorState {
    var parkingPoint: EngineGeoPoint = EngineGeoPoint()
    var isActive: Boolean = false
    var escapeScore: Double = 0.0
    var trendPoints: MutableList<EngineGeoPoint> = MutableList(3) { EngineGeoPoint() }
    var trendCount: Int = 0
    var trendIdx: Int = 0
    var averagingBuffer: MutableList<EngineGeoPoint> = MutableList(8) { EngineGeoPoint() }
    var averageCount: Int = 0
    var averageIdx: Int = 0
    var isLocked: Boolean = false
}

@Serializable
class LocationProcessingState {
    val accuracy = AccuracyState()
    val forensic = SentinelForensicState()
    val trajectory = TrajectoryBuffer()
    val anchor = AnchorState()
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
    var kineticEnergy: Double = 0.0
    val optimizedPointFlyweight: EngineGeoPoint = EngineGeoPoint()
    @Transient var cachedHomePoints: List<EngineGeoPoint>? = null
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
    var violationStartTs: Long = 0L
    var appStartTime: Long = 0L
    var capabilities: HardwareCapabilities = HardwareCapabilities()
    var forensicReliabilityDegradationStartRt: Long = 0L
    var powerAlarmPending: Boolean = false
    var lastGlobalTriggerRt: Long = 0L
    var lastRelayOnlineRt: Long = 0L
    var lastRelayOfflineRt: Long = 0L
    var lastSirenStopRt: Long = 0L
    var bootId: String = ""
    
    @Transient
    val activeAlarms: MutableMap<String, ActiveAlarm> = ConcurrentHashMap()

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
    var vibrationSensitivity: Float = 0.5f
    var tiltSensitivity: Float = 0.5f

    fun getOrCreateHomePoint(index: Int): EngineGeoPoint {
        while (homePoints.size <= index) { homePoints.add(EngineGeoPoint()) }
        return homePoints[index]
    }

    fun truncateHomePoints(size: Int) {
        while (homePoints.size > size) { homePoints.removeAt(homePoints.size - 1) }
    }

    fun update(
        now: Long, nowRt: Long, serviceStartTime: Long, serviceStartRt: Long,
        lastAlarmAckTs: Long, violationStartTs: Long, appStartTime: Long, 
        isRelayConnected: Boolean, isTrackerConnected: Boolean, discoveryPhase: DiscoveryPhase,
        trackerLat: Double, trackerLng: Double, trackerGpsAccuracy: Double,
        maxTrackerAccuracy: Double, lastGpsPacketTs: Long, lastGpsPacketRt: Long,
        trackerLastValidFixTs: Long, trackerLastValidFixRt: Long,
        trackerSpeed: Double, jumpTier: Int, isAdaptiveJump: Boolean,
        trackerBattery: Int, trackerTemp: Double, wasDistanceViolated: Boolean,
        distanceViolationCounter: Int, firstViolationTs: Long, firstViolationRt: Long,
        firstViolationWasJump: Boolean, maxDistance: Double,
        distToHomeAuthority: Double?, isGpsGap: Boolean, trackerBaroAltEma: Double,
        isTrackerMode: Boolean, capabilities: HardwareCapabilities,
        vibrationSensitivity: Float = 0.5f, tiltSensitivity: Float = 0.5f,
        powerAlarmPending: Boolean = false, lastGlobalTriggerRt: Long = 0L
    ) {
        this.now = now; this.nowRt = nowRt; this.serviceStartTime = serviceStartTime
        this.serviceStartRt = serviceStartRt; this.lastAlarmAckTs = lastAlarmAckTs
        this.violationStartTs = violationStartTs
        this.appStartTime = appStartTime; this.isRelayConnected = isRelayConnected
        this.isTrackerConnected = isTrackerConnected; this.discoveryPhase = discoveryPhase
        this.trackerLat = trackerLat; this.trackerLng = trackerLng; this.trackerGpsAccuracy = trackerGpsAccuracy
        this.maxTrackerAccuracy = maxTrackerAccuracy; this.lastGpsPacketTs = lastGpsPacketTs
        this.lastGpsPacketRt = lastGpsPacketRt; this.trackerLastValidFixTs = trackerLastValidFixTs
        this.trackerLastValidFixRt = trackerLastValidFixRt; this.trackerSpeed = trackerSpeed
        this.jumpTier = jumpTier; this.trackerBattery = trackerBattery; this.trackerTemp = trackerTemp
        this.wasDistanceViolated = wasDistanceViolated; this.distanceViolationCounter = distanceViolationCounter
        this.firstViolationTs = firstViolationTs; this.firstViolationRt = firstViolationRt
        this.firstViolationWasJump = firstViolationWasJump; this.maxDistance = maxDistance
        this.distToHomeAuthority = distToHomeAuthority; this.isGpsGap = isGpsGap
        this.trackerBaroAltEma = trackerBaroAltEma; this.isTrackerMode = isTrackerMode
        this.capabilities = capabilities; this.vibrationSensitivity = vibrationSensitivity
        this.tiltSensitivity = tiltSensitivity; this.powerAlarmPending = powerAlarmPending
        this.lastGlobalTriggerRt = lastGlobalTriggerRt
    }
}

enum class RibbonScale(val key: String, val intervalSeconds: Int) {
    FOUR_MIN("4M", 1), SIXTEEN_MIN("16M", 4), ONE_HOUR("1H", 15),
    FOUR_HOUR("4H", 60), TWENTY_FOUR_HOUR("24H", 360), SEVEN_DAY("7D", 2700)
}

@Serializable
class SentinelResult(
    var status: SentinelStatus = SentinelStatus.VALID, var reason: String = "",
    var optimizedPoint: EngineGeoPoint? = null, var jumpConfidence: JumpConfidence? = null,
    var suppressionNote: String? = null, var promotedPoints: List<EngineGeoPoint>? = null,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
) {
    fun reset(status: SentinelStatus = SentinelStatus.VALID) {
        this.status = status; this.reason = ""; this.optimizedPoint = null
        this.jumpConfidence?.reset(); this.suppressionNote = null; this.promotedPoints = null
        this.locationPendingReason = LocationPendingReason.NONE
    }

    fun copyFrom(other: SentinelResult) {
        this.status = other.status
        this.reason = other.reason
        this.optimizedPoint = other.optimizedPoint?.let { 
            (this.optimizedPoint ?: EngineGeoPoint()).apply { copyFrom(it) } 
        }
        this.jumpConfidence = other.jumpConfidence?.let {
            (this.jumpConfidence ?: JumpConfidence()).apply { copyFrom(it) }
        }
        this.suppressionNote = other.suppressionNote
        this.promotedPoints = other.promotedPoints?.toList()
        this.locationPendingReason = other.locationPendingReason
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
