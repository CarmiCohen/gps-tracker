package com.gps19.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.concurrent.ConcurrentHashMap

/**
 * EngineModels: Data structures for the core tracking engine.
 * Oct.8.16:
 * - Issue #BUILD-FIX: Restored correct Kotlin syntax for data class constructors 
 *   (added missing commas).
 * - Standardized DomainEvent hierarchies for multi-module binary compatibility.
 * - Migrated interfaces to 'val' to allow flexible implementation (mutable/immutable).
 * - Fixed trackerLastValidFixTs assignment in AlarmEvaluationState.
 */

@Serializable
data class EngineGeoPoint(
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

@Serializable
enum class DiscoveryPhase { BOOTSTRAP, DISCOVERING, MONITORING }
@Serializable
enum class SentinelStatus { VALID, JUMP, TAMPER, TRAJECTORY_PROMOTED, OUTLIER, JITTER, JAMMER_SUSPICION }
@Serializable
enum class SignalingPriority { HIGH, NORMAL }
@Serializable
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

@Serializable
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
class EngineConnectionPoint {
    var ts: Long = 0L
    var rt: Long = 0L
    var rtt: Int = 0
    var remoteSig: Int = 0
    var isConnected: Boolean = false
    var isGap: Boolean = false
    var isRecoveryEvent: Boolean = false
    var hasGps: Boolean = false
    var accuracy: Double = 0.0
    var maxAccuracy: Double = 0.0
    var isBatteryLow: Boolean = false
    var isBatteryCritical: Boolean = false
    var isBatterySteepDischarge: Boolean = false
    var isCoolingModeActive: Boolean = false
    var speed: Double = 0.0
    var bearing: Double = 0.0
    var isTick: Boolean = false
    var currentMa: Int = 0
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
    var gpsIndex: Double = 0.0
    var noiseIdx: Double = 0.0
    var luxIdx: Double = 0.0
    var vibeIdx: Double = 0.0
    var proxIdx: Double = 0.0
    var liftIdx: Double = 0.0
    var snrIdx: Double = 0.0
    var tiltIdx: Double = 0.0
    var baroIdx: Double = 0.0
    var isSitDetected: Boolean = false
    var isSitActive: Boolean = false
    var verticalVelocity: Double = 0.0
    var sitVz: Double = 0.0
    var sitVzTs: Long = 0L
    var sitVzRt: Long = 0L
    var sitDz: Double = 0.0
    var sitBaro: Double = 0.0
    var sitTilt: Double = 0.0
    var sitShock: Double = 0.0
    var kineticEnergy: Double = 0.0
    var gpsHardwareLock: Boolean = false
    var cpuLoad: Double = 0.0
    var ioWait: Double = 0.0
    var maxIoLatency: Long = 0L
    var isSilentFailure: Boolean = false
    var isUltraLongStationary: Boolean = false
    var violationUptimeMs: Long = 0L
    var thermalHeadroom: Double = 0.0
    var heapAllocatedMb: Double = 0.0
    var activityType: ActivityType = ActivityType.UNKNOWN
    val forensic: ForensicSnapshot = ForensicSnapshot()

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

@Serializable
class LocationProcessingState {
    val accuracy: AccuracyState = AccuracyState()
    val forensic: SentinelForensicState = SentinelForensicState()
    val trajectory: TrajectoryBuffer = TrajectoryBuffer()
    val anchor: AnchorState = AnchorState()
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
    
    // Issue #1417: Connectivity Hysteresis
    var lastRelayOnlineRt: Long = 0L
    var lastRelayOfflineRt: Long = 0L

    // Issue #SIMP-1201-1: Serialization Parity
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
    var currentMemLevel: Int = 0
    var needsMemFlush: Boolean = false
    var currentStorageLevel: Int = 0
    var needsStoragePrune: Boolean = false
}

interface NativeFastPathProvider {
    fun isStationary(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean
    fun updateVibrationFloor(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double
    fun computeNextHpf(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double
    fun computeNextEnergy(currentEnergy: Double, hpfValue: Double): Double
    fun calculateVibrationDelta(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double
    fun isShockViolated(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean
    fun isVibrationSuspicious(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean
    fun computeAdaptiveAcousticAlpha(baseAlpha: Double, vibrationRollingSum: Double): Double
    
    fun processVibrationBatch(batch: VibrationBatch): Boolean
    fun processGnssBatch(batch: GnssHealthBatch): Boolean
    fun processAcousticBatch(batch: AcousticBatch, buffer: ShortArray): Boolean
    fun processProximityBatch(batch: ProximityBatch): Boolean
    fun processSystemPressure(batch: SystemPressureBatch): Boolean
}

@Serializable
data class AccuracyState(
    var lastProcessedAccuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var windowBuffer: DoubleArray = DoubleArray(64),
    var windowSize: Int = 0,
    var windowHead: Int = 0,
    var lastUpdateRt: Long = 0L
)

@Serializable
data class SentinelForensicState(
    var lastValidLat: Double = 0.0,
    var lastValidLng: Double = 0.0,
    var lastValidAlt: Double = 0.0,
    var lastValidTs: Long = 0L,
    var lastValidRt: Long = 0L,
    var lastValidSpeedMps: Double = 0.0,
    var lastValidBearing: Double = 0.0,
    var lastValidAccuracy: Double = 0.0,
    var estimatedSpeedMps: Double = 0.0,
    var estimatedBearing: Double = 0.0,
    var stationaryProb: Double = 1.0,
    var currentVibrationIndex: Double = 0.0,
    var peakVibrationShock: Double = 0.0,
    var peakVibrationShockRt: Long = 0L,
    var currentCompassHeading: Double = 0.0,
    var lastCompassHeading: Double = 0.0,
    var currentBaroAlt: Double = 0.0,
    var currentLux: Double = 0.0,
    var isNear: Boolean = true,
    var isPowerTamper: Boolean = false,
    var currentTiltDegrees: Double = 0.0,
    var currentAcousticDb: Double = 0.0,
    var lastFastPathAcousticSpikeRt: Long = 0L,
    var lastFastPathLightSpikeRt: Long = 0L,
    var isSitDetected: Boolean = false,
    var lastSitTs: Long = 0L,
    var lastSitRt: Long = 0L,
    var baselineSitTilt: Double = -1.0,
    var lastSitVz: Double = 0.0,
    var lastSitVzTs: Long = 0L,
    var lastSitVzRt: Long = 0L,
    var lastSitDz: Double = 0.0,
    var lastSitBaro: Double = 0.0,
    var lastSitTilt: Double = 0.0,
    var lastSitShock: Double = 0.0,
    var sitDetectionCooldownRt: Long = 0L,
    var stationaryDurationMs: Long = 0L,
    var gpsMotionStartRt: Long = 0L,
    var luxBaseline: Double = -1.0,
    var baroBaseline: Double = -1000.0,
    var acousticFloorDb: Double = -1.0,
    var adaptiveVibrationFloor: Double = 0.1,
    var lastAcousticContractionRt: Long = 0L,
    var lastSnr: Double = 0.0,
    var lastSatsUsed: Int = 0,
    var isSuspiciousNoise: Boolean = false,
    var isMemoryPressureThrottled: Boolean = false,
    var isJammingCandidate: Boolean = false
)

@Serializable
data class TrajectoryBuffer(
    var latBuffer: DoubleArray = DoubleArray(128),
    var lngBuffer: DoubleArray = DoubleArray(128),
    var altBuffer: DoubleArray = DoubleArray(128),
    var accBuffer: DoubleArray = DoubleArray(128),
    var maxAccBuffer: DoubleArray = DoubleArray(128),
    var bearingBuffer: DoubleArray = DoubleArray(128),
    var speedBuffer: DoubleArray = DoubleArray(128),
    var tsBuffer: LongArray = LongArray(128),
    var rtBuffer: LongArray = LongArray(128),
    var vibeBuffer: DoubleArray = DoubleArray(128),
    var head: Int = 0,
    var size: Int = 0
)

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
class ForensicSample {
    var ts: Long = 0L
    var rt: Long = 0L
    var snr: Double = 0.0
    var acoustic: Double = 0.0
    var lux: Double = 0.0
    var vibe: Double = 0.0
    var proxIdx: Double = 0.0
    var lift: Double = 0.0
    var tilt: Double = 0.0
    var isSitDetected: Boolean = false
    var sitVzTs: Long = 0L
    var sitVzRt: Long = 0L
    var sitShock: Double = 0.0
    var kineticEnergy: Double = 0.0
    var activityType: ActivityType = ActivityType.UNKNOWN

    fun reset() {
        ts = 0L; rt = 0L; snr = 0.0; acoustic = 0.0; lux = 0.0; vibe = 0.0
        proxIdx = 0.0; lift = 0.0; tilt = 0.0; isSitDetected = false
        sitVzTs = 0L; sitVzRt = 0L; sitShock = 0.0; kineticEnergy = 0.0
        activityType = ActivityType.UNKNOWN
    }
}

@Serializable
data class VibrationBatch(
    var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0,
    var lx: Double = 0.0, var ly: Double = 0.0, var lz: Double = 0.0,
    var adaptiveFloor: Double = 0.0,
    var isWarming: Boolean = false,
    var cpuLoad: Double = 0.0,
    var lastRawVibe: Double = 0.0,
    var lastHpfValue: Double = 0.0,
    var currentEnergy: Double = 0.0,
    var snr: Double = -1.0,
    var thermal: Double = -1.0,
    var heap: Double = -1.0,
    var nowRt: Long = 0L,
    var delta: Double = 0.0,
    var nextFloor: Double = 0.0,
    var nextHpf: Double = 0.0,
    var nextEnergy: Double = 0.0,
    var isStationary: Boolean = false,
    var isSuspiciousNoise: Boolean = false,
    var isMemoryPressureThrottled: Boolean = false,
    var stationaryDuration: Long = 0L,
    var muzzleResetTriggered: Boolean = false,
    var isJammingCandidate: Boolean = false
)

@Serializable
data class GnssHealthBatch(
    var count: Int = 0,
    var svid: IntArray = IntArray(64),
    var cn0: FloatArray = FloatArray(64),
    var usedInFix: BooleanArray = BooleanArray(64),
    var constellation: IntArray = IntArray(64),
    var satellitesInView: Int = 0,
    var satellitesUsed: Int = 0,
    var averageSnr: Double = 0.0
)

@Serializable
data class AcousticBatch(
    var readCount: Int = 0,
    var baseAlpha: Double = 0.0,
    var vibrationRollingSum: Double = 0.0,
    var nowRt: Long = 0L,
    var isWarming: Boolean = false,
    var maxAmp: Int = 0,
    var db: Double = 0.0,
    var isSpike: Boolean = false,
    var lastSpikeRt: Long = 0L
)

@Serializable
data class ProximityBatch(
    var distance: Double = 0.0,
    var maxRange: Double = 0.0,
    var nowRt: Long = 0L,
    var isStationary: Boolean = false,
    var stationaryDurationMs: Long = 0L,
    var isHighLoad: Boolean = false,
    var currentIdx: Double = 0.0,
    var rawNear: Boolean = false,
    var isFlickering: Boolean = false,
    var nextIdx: Double = 0.0,
    var nextRawNear: Boolean = false,
    var debounceMs: Long = 0L
)

interface Locatable {
    val isLocationPending: Boolean
    val locationPendingReason: LocationPendingReason
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
    val ts: Long
    val rt: Long
}

interface SpatialAnchor {
    val lat: Double
    val lng: Double
    val alt: Double
    val gpsTs: Long
}

interface BatteryProvider {
    val battery: Int
    val isCharging: Boolean
}

interface DeviceIdentity {
    val trackerId: String
    val viewerId: String
}

@Serializable
sealed class DomainEvent {
    abstract val priority: EventPriority
    
    @Serializable
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
        val recoveryFlagged: Boolean,
        val gnssDetail: GnssDetail?,
        val isSuspiciousMode: Boolean,
        val lastSitTs: Long,
        val lastTickTs: Long,
        val lastTickRt: Long
    ) : DomainEvent() {
        override val priority = EventPriority.NORMAL
    }
    @Serializable
    data class ServiceStatus(val message: String, val isImportant: Boolean = false) : DomainEvent() {
        override val priority = if (isImportant) EventPriority.HIGH else EventPriority.LOW
    }
    @Serializable
    data class HeuristicRecovery(val message: String, val gapMs: Long, val lat: Double = 0.0, val lng: Double = 0.0, val accuracy: Double = 0.0) : DomainEvent() {
        override val priority = EventPriority.HIGH
    }
    @Serializable
    data class StabilityViolation(val message: String, val isJitter: Boolean, val lat: Double = 0.0, val lng: Double = 0.0, val accuracy: Double = 0.0) : DomainEvent() {
        override val priority = EventPriority.HIGH
    }
    @Serializable
    data class PowerSaveTransition(val isEngaged: Boolean) : DomainEvent() {
        override val priority = EventPriority.NORMAL
    }
    @Serializable
    data class PeerConnectionChanged(val isConnected: Boolean, val peerId: String) : DomainEvent() {
        override val priority = EventPriority.HIGH
    }
    @Serializable
    data class PeerStatusReceived(val status: LocationUpdate) : DomainEvent() {
        override val priority = EventPriority.NORMAL
    }
}

@Serializable
enum class EventPriority { LOW, NORMAL, HIGH }

@Serializable
data class GnssDetail(
    val satellites: List<SatelliteInfo> = emptyList()
)

@Serializable
data class SatelliteInfo(
    val svid: Int,
    val cn0: Double,
    val usedInFix: Boolean,
    val constellation: Int
)

@Serializable
sealed class ProcessorEvent : DomainEvent() {
    override val priority = EventPriority.NORMAL
    abstract val isPrimary: Boolean

    @Serializable
    data class LocationProcessed(val update: LocationUpdate) : ProcessorEvent() {
        override val isPrimary: Boolean = true
    }
    @Serializable
    data class LogAdded(val message: String, val type: String, val isImportant: Boolean, val isSpecial: Boolean, val lat: Double, val lng: Double, val accuracy: Double, val forensic: ForensicSnapshot, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class VibrationFloorChanged(val floor: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class LuxBaselineChanged(val baseline: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class AcousticFloorChanged(val floor: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class ChairBaselineChanged(val baseline: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class MaxAccuracyChanged(val accuracy: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class TrailPointSaved(val lat: Double, val lng: Double, val isViewerTrail: Boolean, val status: SentinelStatus, val timestamp: Long, val accuracy: Double, val maxAccuracy: Double, override val isPrimary: Boolean) : ProcessorEvent()
    @Serializable
    data class GpsStallDetected(val rt: Long, override val isPrimary: Boolean) : ProcessorEvent()
}

@Serializable
sealed class AlarmEvent : DomainEvent() {
    override val priority = EventPriority.HIGH
    
    @Serializable
    data class LogEvent(val message: String, val type: String, val isImportant: Boolean, val extremeValue: Double?, val logId: String?, val durationMs: Long, val isSpecial: Boolean, val specialColor: Int?, val lat: Double, val lng: Double, val accuracy: Double, val maxAccuracy: Double, val forensic: ForensicSnapshot) : AlarmEvent()
}

@Serializable
sealed class IntegrityEvent : DomainEvent() {
    override val priority = EventPriority.HIGH
    
    @Serializable
    data class ViolationSustained(val type: String) : IntegrityEvent()
    @Serializable
    data class ViolationResolved(val type: String) : IntegrityEvent()
    @Serializable
    data class LogEvent(val message: String, val isImportant: Boolean = false) : IntegrityEvent()
    @Serializable
    data class LocationStatusChanged(val status: LocationStatus) : IntegrityEvent()
    @Serializable
    data class GnssThrottledChanged(val isThrottled: Boolean) : IntegrityEvent()
    @Serializable
    data class MemoryPressureChanged(val level: MemoryPressureLevel, val heapMb: Double) : IntegrityEvent()
    @Serializable
    data class StoragePressureChanged(val isLow: Boolean, val isCritical: Boolean, val availableMb: Long) : IntegrityEvent()
}

@Serializable
sealed class ConnectivityEvent : DomainEvent() {
    override val priority = EventPriority.NORMAL
    
    @Serializable
    data class PeerPulse(val peerId: String) : ConnectivityEvent()
}

@Serializable
sealed class HistoryEvent : DomainEvent() {
    override val priority = EventPriority.LOW
    
    @Serializable
    data class LogEvent(val message: String, val isImportant: Boolean = false) : HistoryEvent()
}

@Serializable
sealed class AppSensorEvent : DomainEvent() {
    override val priority = EventPriority.HIGH
    
    @Serializable
    data class HardwareFailure(val reason: String) : AppSensorEvent()
    @Serializable
    data class LogEvent(val message: String, val isImportant: Boolean = false) : AppSensorEvent()
}

@Serializable
sealed class CommandEvent : DomainEvent() {
    override val priority = EventPriority.HIGH
    
    @Serializable
    data object ResetTimers : CommandEvent()
    @Serializable
    data object TriggerMemoryFlush : CommandEvent()
    @Serializable
    data object TriggerStoragePrune : CommandEvent()
    @Serializable
    data object WatchdogTrigger : CommandEvent()
    @Serializable
    data object UiPulse : CommandEvent()
    @Serializable
    data object SyncSensors : CommandEvent()
    @Serializable
    data class UiVisibilityChanged(val isVisible: Boolean) : CommandEvent()
    
    @Serializable
    data object ExecuteStressTest : CommandEvent()
    @Serializable
    data object ExecuteLogPressureTest : CommandEvent()
    @Serializable
    data object ExecuteNetworkStressTest : CommandEvent()
    @Serializable
    data class SimulateStoragePressure(val active: Boolean, val isCritical: Boolean) : CommandEvent()
    @Serializable
    data class SimulateMemoryPressure(val active: Boolean, val level: MemoryPressureLevel) : CommandEvent()
}

@Serializable
sealed class RevivalEvent : DomainEvent() {
    override val priority = EventPriority.HIGH
    
    @Serializable
    data class Footprint(val deltaMa: Int, val deltaTemp: Double, val durationMs: Long) : RevivalEvent()
    @Serializable
    data object HardwareLock : RevivalEvent()
    @Serializable
    data class Attempt(val count: Int) : RevivalEvent()
    @Serializable
    data object Success : RevivalEvent()
    @Serializable
    data object RawBurstStarted : RevivalEvent()
    @Serializable
    data object RawBurstEnded : RevivalEvent()
}

@Serializable
class SystemHealthReport(
    val reports: MutableList<ViolationReport> = MutableList(32) { ViolationReport() }
) {
    var size: Int = 0
    fun getOrCreate(index: Int): ViolationReport {
        while (reports.size <= index) { reports.add(ViolationReport()) }
        if (index >= size) size = index + 1
        return reports[index]
    }
    fun truncate(newSize: Int) {
        size = newSize
    }
}

@Serializable
class ViolationReport {
    var type: String = ""
    var title: String = ""
    var subtitle: String = ""
    var conditionMet: Boolean = false
    var technicalDetails: String? = null
    var extremeValue: Double = 0.0
    
    fun update(type: String, title: String, subtitle: String, conditionMet: Boolean, technicalDetails: String? = null, extremeValue: Double = 0.0) {
        this.type = type; this.title = title; this.subtitle = subtitle; this.conditionMet = conditionMet
        this.technicalDetails = technicalDetails; this.extremeValue = extremeValue
    }
}

@Serializable
class JumpConfidence {
    var tier: Int = 0
    var isJump: Boolean = false
    var isOutlier: Boolean = false
    var isAdaptiveJump: Boolean = false
    var score: Int = 0
    var reason: String? = null
    
    fun reset() {
        tier = 0; isJump = false; isOutlier = false; isAdaptiveJump = false; score = 0; reason = null
    }
}

@Serializable
enum class RibbonScale(val intervalSeconds: Int) { 
    FOUR_MIN(60), 
    SIXTEEN_MIN(240), 
    ONE_HOUR(900), 
    FOUR_HOUR(3600), 
    TWENTY_FOUR_HOUR(21600), 
    SEVEN_DAY(86400) 
}

@Serializable
class ProcessedLocation {
    var rawPoint: EngineGeoPoint? = null
    var optimizedPoint: EngineGeoPoint? = null
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
        rawPoint = null; optimizedPoint = null; status = SentinelStatus.VALID
        maxAccuracy = 0.0; currentAccuracy = 0.0; filteredSpeed = 0.0
        timestamp = 0L; rt = 0L; isStalled = false; isClockRegression = false
        receiptRt = 0L; isTrajectoryPromoted = false; jumpTier = 0
        isAdaptiveJump = false; distToHome = null; isSpatiallyValid = false
        geofenceViolationDetected = false; tamperDetected = false; jammerDetected = false
        isAnchorLocked = false; suppressionNote = null; kineticEnergy = 0.0
        locationPendingReason = LocationPendingReason.NONE
    }
}

@Serializable
class SentinelResult {
    var status: SentinelStatus = SentinelStatus.VALID
    var reason: String? = null
    var jumpConfidence: JumpConfidence? = null
    var optimizedPoint: EngineGeoPoint? = null
    var promotedPoints: List<EngineGeoPoint>? = null
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
    var suppressionNote: String? = null
    
    fun reset(status: SentinelStatus = SentinelStatus.VALID) {
        this.status = status
        reason = null
        jumpConfidence?.reset()
        optimizedPoint = null
        promotedPoints = null
        locationPendingReason = LocationPendingReason.NONE
        suppressionNote = null
    }
}

@Serializable
class TrajectoryNode {
    var lat: Double = 0.0
    var lng: Double = 0.0
    var alt: Double = 0.0
    var accuracy: Double = 0.0
    var maxAccuracy: Double = 0.0
    var bearing: Double = 0.0
    var speedMps: Double = 0.0
    var ts: Long = 0L
    var rt: Long = 0L
    var vibe: Double = 0.0

    fun update(lat: Double, lng: Double, alt: Double, acc: Double, maxAcc: Double, bearing: Double, speed: Double, ts: Long, rt: Long, vibe: Double) {
        this.lat = lat; this.lng = lng; this.alt = alt; this.accuracy = acc; this.maxAccuracy = maxAcc
        this.bearing = bearing; this.speedMps = speed; this.ts = ts; this.rt = rt; this.vibe = vibe
    }

    fun reset() {
        lat = 0.0; lng = 0.0; alt = 0.0; accuracy = 0.0; maxAccuracy = 0.0
        bearing = 0.0; speedMps = 0.0; ts = 0L; rt = 0L; vibe = 0.0
    }
}

@Serializable
class RejectedPoint(
    val lat: Double,
    val lng: Double,
    val alt: Double,
    val accuracy: Double,
    val bearing: Double,
    val speedMps: Double,
    val ts: Long,
    val rt: Long
)
