package com.gps19.app

import com.gps19.core.engine.*
import com.gps19.core.engine.PhysicsUtils.safeDouble
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Models: UI and Persistence data structures for GPS Tracker.
 * Oct.11.1:
 * - Issue #SIMP-1011-7: Thermal Forensic Audit. Added coolingSnapshot to 
 *   LogEntry to authoritatively audit sampling decay during transients.
 * Oct.7.7:
 * - Issue #SIMP-1007-16: Flag Propagation. Added isSuspiciousNoise and 
 *   isMemoryPressureThrottled delegates to ConnectionPoint for HUD visibility.
 * Oct.7.5:
 * - Issue #SIMP-1007-15: Unified Snapshot Container. Completed migration of 
 *   ConnectionPoint and LogEntry to use unified ForensicSnapshot for all probes (snr, vibe, thermal, heap).
 */

@Serializable
data class SerializableGeoPoint(val lat: Double, val lng: Double) {
    fun toGeoPoint() = GeoPoint(lat, lng)
}

fun GeoPoint.toSerializable() = SerializableGeoPoint(latitude, longitude)

@Serializable
data class TrailPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long = 0L,
    val status: SentinelStatus = SentinelStatus.VALID,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0
) {
    @Transient
    private var _cachedGeoPoint: GeoPoint? = null

    fun toGeoPoint(): GeoPoint {
        val cached = _cachedGeoPoint
        if (cached != null && cached.latitude == lat && cached.longitude == lng) {
            return cached
        }
        val newPoint = GeoPoint(lat, lng)
        _cachedGeoPoint = newPoint
        return newPoint
    }

    /**
     * contentEquals: Deep parity check to prevent redundant UI snapshot updates (R312).
     */
    fun contentEquals(other: TrailPoint): Boolean {
        return lat == other.lat && lng == other.lng && status == other.status && 
               accuracy == other.accuracy && maxAccuracy == other.maxAccuracy
    }
}

/**
 * MapTrailSegment: Used for optimized map rendering of trail chunks.
 */
data class MapTrailSegment(
    val points: List<GeoPoint>,
    val color: Int,
    val checksum: Int = 0 
)

@Serializable
data class AlertSettings(
    val localInternet: Boolean = true,
    val serverConnection: Boolean = true,
    val relayConnection: Boolean = true,
    val jammerDetection: Boolean = true,
    val signalLoss: Boolean = true,
    val gpsStalling: Boolean = true,
    val distance: Boolean = true,
    val power: Boolean = true,
    val lowBattery: Boolean = true,
    val batteryHealth: Boolean = true, 
    val longTimeGap: Boolean = true,
    val highTemperature: Boolean = true,
    val overrideSilence: Boolean = true,
    val useMaxVolume: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val alarmVolume: Float = 0.8f,
    val useCustomVolume: Boolean = false,
    val vibrationEnabledV2: Boolean = true, // Placeholders for future expansion
    val alarmVolumeV2: Float = 0.8f,
    val tiltAlert: Boolean = true,
    val acousticAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tamperAlert: Boolean = true,
    val globalMute: Boolean = false,
    val systemStorageLow: Boolean = true,
    val vibrationSensitivity: Float = 0.5f,
    val tiltSensitivity: Float = 0.5f
)

class ConnectionPoint(
    var localId: String = "",
    var ts: Long = 0, 
    var rt: Long = 0, 
    var rtt: Int = 0, 
    var localSig: Int = 10, 
    var remoteSig: Int = 0,
    var isConnected: Boolean = false, 
    var isGap: Boolean = false, 
    var isRecoveryEvent: Boolean = false,
    var gpsAccuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var isTick: Boolean = false, 
    var hasGps: Boolean = false,
    var isBatterySteepDischarge: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,
    var speed: Double = 0.0, 
    var bearing: Double = 0.0,
    var currentMa: Int = 0,
    var status: SentinelStatus = SentinelStatus.VALID,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var gpsIndex: Double = 0.0,
    var snrIdx: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var proxIdx: Double = 1.0,
    var liftIdx: Double = 0.0,
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
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isUltraLongStationary: Boolean = false,
    var violationUptimeMs: Long = 0L,
    var gpsHardwareLock: Boolean = false,
    var isAnchorLocked: Boolean = false,
    var isGnssThrottled: Boolean = false,
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

    // Oct.7.7 Anomaly Flags
    var isSuspiciousNoise: Boolean
        get() = forensic.isSuspiciousNoise
        set(value) { forensic.isSuspiciousNoise = value }
    var isMemoryPressureThrottled: Boolean
        get() = forensic.isMemoryPressureThrottled
        set(value) { forensic.isMemoryPressureThrottled = value }

    fun copyFrom(other: ConnectionPoint) {
        this.localId = other.localId; this.ts = other.ts; this.rt = other.rt; this.rtt = other.rtt
        this.localSig = other.localSig; this.remoteSig = other.remoteSig; this.isConnected = other.isConnected
        this.isGap = other.isGap; this.isRecoveryEvent = other.isRecoveryEvent; this.gpsAccuracy = other.gpsAccuracy
        this.maxAccuracy = other.maxAccuracy; this.isTick = other.isTick; this.hasGps = other.hasGps
        this.isBatterySteepDischarge = other.isBatterySteepDischarge; this.isCoolingModeActive = other.isCoolingModeActive
        this.isBatteryLow = other.isBatteryLow; this.isBatteryCritical = other.isBatteryCritical
        this.speed = other.speed; this.bearing = other.bearing; this.currentMa = other.currentMa
        this.status = other.status; this.locationPendingReason = other.locationPendingReason
        this.gpsIndex = other.gpsIndex; this.snrIdx = other.snrIdx; this.noiseIdx = other.noiseIdx
        this.luxIdx = other.luxIdx; this.vibeIdx = other.vibeIdx; this.proxIdx = other.proxIdx
        this.liftIdx = other.liftIdx; this.tiltIdx = other.tiltIdx; this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected; this.isSitActive = other.isSitActive
        this.verticalVelocity = other.verticalVelocity; this.sitVz = other.sitVz
        this.sitVzTs = other.sitVzTs; this.sitVzRt = other.sitVzRt; this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro; this.sitTilt = other.sitTilt; this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy; this.cpuLoad = other.cpuLoad; this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency; this.isSilentFailure = other.isSilentFailure
        this.isUltraLongStationary = other.isUltraLongStationary; this.violationUptimeMs = other.violationUptimeMs
        this.gpsHardwareLock = other.gpsHardwareLock; this.isAnchorLocked = other.isAnchorLocked
        this.isGnssThrottled = other.isGnssThrottled
        this.thermalHeadroom = other.thermalHeadroom
        this.heapAllocatedMb = other.heapAllocatedMb
        this.activityType = other.activityType
        this.forensic.copyFrom(other.forensic)
    }

    /**
     * contentEquals: Deep parity check for history ribbon points (R312).
     */
    fun contentEquals(other: ConnectionPoint): Boolean {
        return ts == other.ts && rtt == other.rtt && remoteSig == other.remoteSig && 
               isConnected == other.isConnected && isGap == other.isGap && 
               isRecoveryEvent == other.isRecoveryEvent && hasGps == other.hasGps &&
               gpsIndex == other.gpsIndex && snrIdx == other.snrIdx && vibeIdx == other.vibeIdx &&
               luxIdx == other.luxIdx && noiseIdx == other.noiseIdx && liftIdx == other.liftIdx &&
               tiltIdx == other.tiltIdx && baroIdx == other.baroIdx && isSitActive == other.isSitActive &&
               isUltraLongStationary == other.isUltraLongStationary && violationUptimeMs == other.violationUptimeMs &&
               gpsHardwareLock == other.gpsHardwareLock && isAnchorLocked == other.isAnchorLocked &&
               isGnssThrottled == other.isGnssThrottled && thermalHeadroom == other.thermalHeadroom && 
               heapAllocatedMb == other.heapAllocatedMb && activityType == other.activityType &&
               forensic.snr == other.forensic.snr && forensic.vibe == other.forensic.vibe &&
               forensic.thermal == other.forensic.thermal && forensic.heap == other.forensic.heap &&
               forensic.isSuspiciousNoise == other.forensic.isSuspiciousNoise &&
               forensic.isMemoryPressureThrottled == other.forensic.isMemoryPressureThrottled
    }

    fun reset() {
        localId = ""; ts = 0; rt = 0; rtt = 0; localSig = 10; remoteSig = 0
        isConnected = false; isGap = false; isRecoveryEvent = false; gpsAccuracy = 0.0
        maxAccuracy = 0.0; isTick = false; hasGps = false; isBatterySteepDischarge = false
        isCoolingModeActive = false; isBatteryLow = false; isBatteryCritical = false
        speed = 0.0; bearing = 0.0; currentMa = 0; status = SentinelStatus.VALID
        locationPendingReason = LocationPendingReason.NONE; gpsIndex = 0.0
        snrIdx = 0.0; noiseIdx = 0.0; luxIdx = 0.0; vibeIdx = 0.0; proxIdx = 1.0; liftIdx = 0.0
        tiltIdx = 0.0; baroIdx = 0.0; isSitDetected = false; isSitActive = false
        sitVz = 0.0; sitVzTs = 0L; sitVzRt = 0L; sitDz = 0.0; sitBaro = 0.0; sitTilt = 0.0
        sitShock = 0.0; kineticEnergy = 0.0; cpuLoad = 0.0; ioWait = 0.0; maxIoLatency = 0L
        isSilentFailure = false; isUltraLongStationary = false; violationUptimeMs = 0L
        gpsHardwareLock = false; isAnchorLocked = false; isGnssThrottled = false
        thermalHeadroom = 0.0; heapAllocatedMb = 0.0; activityType = ActivityType.UNKNOWN
        forensic.reset()
    }
}

/**
 * ViolationPoint: Represents a geofence or sensor violation on the map.
 */
class ViolationPoint(
    var localId: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var type: String = "", 
    var ts: Long = 0,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0
) {
    private var _cachedGeoPoint: GeoPoint? = null
    fun toGeoPoint(): GeoPoint {
        val cached = _cachedGeoPoint
        if (cached != null && cached.latitude == lat && cached.longitude == lng) return cached
        return GeoPoint(lat, lng).also { _cachedGeoPoint = it }
    }
    fun copyFrom(other: ViolationPoint) {
        this.localId = other.localId; this.lat = other.lat; this.lng = other.lng; this.type = other.type
        this.ts = other.ts; this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy
    }

    /**
     * contentEquals: Deep parity check for map violation points (R312).
     */
    fun contentEquals(other: ViolationPoint): Boolean {
        return lat == other.lat && lng == other.lng && type == other.type && 
               ts == other.ts && accuracy == other.accuracy && maxAccuracy == other.maxAccuracy
    }
}

@Serializable
data class LogEntry(
    val localId: String = "", 
    val timestamp: Long, val message: String, val type: String,
    val isImportant: Boolean, val id: String = "", val viewerId: String = "",
    val count: Int = 1, val extremeValue: Double? = null,
    val durationMs: Long = 0L,
    val isSpecial: Boolean = false, 
    val specialColor: Int? = null,    
    val firstSeenTs: Long = 0L,
    val role: String = "tracker",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val forensic: ForensicSnapshot = ForensicSnapshot(),
    val spillIdx: Int = -1,
    val gpsHardwareLock: Boolean = false,
    val tempSnapshot: Double? = null,
    val battSnapshot: Int? = null,
    val chargingSnapshot: Boolean? = null,
    val coolingSnapshot: Boolean? = null
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

    /**
     * contentEquals: Deep parity check to suppress redundant Logcat/UI noise (R312).
     */
    fun contentEquals(other: LogEntry): Boolean {
        return timestamp == other.timestamp && message == other.message && 
               count == other.count && durationMs == other.durationMs &&
               lat == other.lat && lng == other.lng && accuracy == other.accuracy &&
               forensic.snr == other.forensic.snr && forensic.vibe == other.forensic.vibe &&
               forensic.thermal == other.forensic.thermal && forensic.heap == other.forensic.heap &&
               forensic.isSuspiciousNoise == other.forensic.isSuspiciousNoise &&
               forensic.isMemoryPressureThrottled == other.forensic.isMemoryPressureThrottled &&
               coolingSnapshot == other.coolingSnapshot
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("localId", localId); put("timestamp", timestamp)
            put("localTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            
            // R779: Forensic scrubbing of paths and hardware identifiers in messages.
            val sanitizedMsg = ForensicSanitizer.sanitizeMessage(message)
            val finalMsg = ForensicSanitizer.scrubHardwareInfo(sanitizedMsg, isSpecial)
            put("message", finalMsg)
            
            put("type", type); put("isImportant", isImportant)
            put("id", SignalingConstants.getTransmissionId(id))
            put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
            put("count", count); put("duration_ms", durationMs); put("is_special", isSpecial)
            put("first_seen_ts", if (firstSeenTs == 0L) timestamp else firstSeenTs); put("role", role)
            
            // Oct.6.21: Hardened numeric puts to prevent JSONExceptions (Rule 1.125)
            if (lat != 0.0) put("lat", safeDouble(lat))
            if (lng != 0.0) put("lng", safeDouble(lng))
            if (accuracy != 0.0) put("accuracy", safeDouble(accuracy))
            if (maxAccuracy != 0.0) put("max_accuracy", safeDouble(maxAccuracy))
            
            specialColor?.let { put("special_color", it) }
            extremeValue?.let { put("extreme_value", safeDouble(it)) }
            snrSnapshot?.let { put("snr_snapshot", safeDouble(it)) }
            vibeSnapshot?.let { put("vibe_snapshot", safeDouble(it)) }
            if (spillIdx != -1) put("spill_idx", spillIdx)
            if (gpsHardwareLock) put("gps_hw_lock", true)
            tempSnapshot?.let { put("temp_snapshot", safeDouble(it)) }
            battSnapshot?.let { put("batt_snapshot", it) }
            chargingSnapshot?.let { put("charging_snapshot", it) }
            coolingSnapshot?.let { put("cooling_snapshot", it) }
            thermalSnapshot?.let { put("thermal_snapshot", safeDouble(it)) }
            heapSnapshot?.let { put("heap_snapshot", safeDouble(it)) }
            
            // Oct.7.7 Anomaly Flags
            put("is_suspicious_noise", forensic.isSuspiciousNoise)
            put("is_memory_pressure_throttled", forensic.isMemoryPressureThrottled)
        }
    }

    companion object {
        fun fromJSONObject(obj: JSONObject): LogEntry {
            val ts = obj.optLong("timestamp")
            val f = ForensicSnapshot(
                snr = if (obj.has("snr_snapshot")) obj.optDouble("snr_snapshot") else null,
                vibe = if (obj.has("vibe_snapshot")) obj.optDouble("vibe_snapshot") else null,
                thermal = if (obj.has("thermal_snapshot")) obj.optDouble("thermal_snapshot") else null,
                heap = if (obj.has("heap_snapshot")) obj.optDouble("heap_snapshot") else null,
                isSuspiciousNoise = obj.optBoolean("is_suspicious_noise", false),
                isMemoryPressureThrottled = obj.optBoolean("is_memory_pressure_throttled", false)
            )
            return LogEntry(
                localId = obj.optString("localId"), timestamp = ts, message = obj.optString("message"),
                type = obj.optString("type"), isImportant = obj.optBoolean("isImportant"),
                id = obj.optString("id"), viewerId = obj.optString("viewer_id"),
                count = obj.optInt("count", 1), durationMs = obj.optLong("duration_ms", 0L),
                isSpecial = obj.optBoolean("is_special", false),
                specialColor = if (obj.has("special_color")) obj.getInt("special_color") else null,
                firstSeenTs = if (obj.has("first_seen_ts")) obj.getLong("first_seen_ts") else ts,
                role = obj.optString("role", "tracker"),
                extremeValue = obj.optDouble("extreme_value").let { if (it.isNaN() || it.isInfinite()) null else it },
                lat = obj.optDouble("lat", 0.0), lng = obj.optDouble("lng", 0.0),
                accuracy = obj.optDouble("accuracy", 0.0), maxAccuracy = obj.optDouble("max_accuracy", 0.0),
                forensic = f,
                spillIdx = obj.optInt("spill_idx", -1), gpsHardwareLock = obj.optBoolean("gps_hw_lock", false),
                tempSnapshot = if (obj.has("temp_snapshot")) obj.optDouble("temp_snapshot") else null,
                battSnapshot = if (obj.has("batt_snapshot")) obj.optInt("batt_snapshot") else null,
                chargingSnapshot = if (obj.has("charging_snapshot")) obj.optBoolean("charging_snapshot") else null,
                coolingSnapshot = if (obj.has("cooling_snapshot")) obj.optBoolean("cooling_snapshot") else null
            )
        }
    }
}

/**
 * Dashboard Component States: Segmented for performance.
 */
@Serializable
data class DashboardConnectivityState(
    val lastSeenTs: Long = 0L,
    val watchdogOk: Boolean = true,
    val watchdogCountdownSec: Long = 0L,
    val totalUptimeMs: Long = 0L,
    val sessionMs: Long = 0L,
    val sinceConnMs: Long = 0L,
    val sinceDiscoMs: Long = 0L,
    val totalDropMs: Long = 0L,
    val maxDropMs: Long = 0L,
    val engineVersion: String = "--",
    val trackerConnIndex: Int = 0,
    val viewerConnIndex: Int = 0,
    val netInterface: String = "UNKNOWN",
    val systemPulse: Long = 0L,
    val isLocalOnline: Boolean = false,
    val isRelayConnected: Boolean = false
)

@Serializable
data class DashboardTelemetryState(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val gpsSpeedMps: Double = 0.0,
    val trackerAccuracy: Double = 0.0,
    val trackerMaxAcc: Double = 0.0,
    val viewerAccuracy: Double = 0.0,
    val viewerMaxAcc: Double = 0.0,
    val satsUsed: Int = -1,
    val satsView: Int = -1,
    val isSatsIndexWarning: Boolean = false,
    val snr: Double = 0.0,
    val distToHome: Double? = null,
    val distToViewer: Double? = null,
    val isGpsFresh: Boolean = true,
    val isTelemetryFresh: Boolean = true,
    val isLinkFresh: Boolean = true,
    override val isLocationPending: Boolean = false,
    override val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val status: SentinelStatus = SentinelStatus.VALID,
    val tamperReason: String? = null,
    val isTamperDetected: Boolean = false,
    val isUltraLongStationary: Boolean = false,
    val systemPulse: Long = 0L,
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val gpsTs: Long = 0L
) : Locatable

@Serializable
data class DashboardHealthState(
    val batteryLevel: Int = 100,
    val trackerTemp: Double = 0.0,
    val trackerMaxTemp: Double = 0.0,
    val viewerTemp: Double = 0.0,
    val viewerMaxTemp: Double = 0.0,
    val vibration: Double = 0.0,
    val heading: Double = 0.0,
    val tilt: Double = 0.0,
    val acousticDb: Double = 0.0,
    val baroAlt: Double = 0.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val proximityCm: Double = -1.0,
    val proximityDebounceMs: Long = 0L,
    val rollingVibration: Double = 0.0,
    val kineticEnergy: Double = 0.0,
    val peakShock: Double = 0.0,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val vibrationFloor: Double = 0.0,
    val isMicPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Double = 0.0,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val trackerCurrentMa: Int = 0,
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    val cpuLoad: Double = 0.0,
    val ioWait: Double = 0.0,
    val maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isMaliAnomaly: Boolean = false,
    var isGnssThrottled: Boolean = false,
    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L,
    var systemPulse: Long = 0L,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    var isSuspiciousNoise: Boolean = false,
    var isMemoryPressureThrottled: Boolean = false
) : BatteryProvider {
    override val battery: Int get() = batteryLevel
    override val isCharging: Boolean get() = trackerCurrentMa < 0
}

/**
 * Monolithic DashboardState facade for legacy compatibility.
 */
@Serializable
data class DashboardState(
    val connectivity: DashboardConnectivityState = DashboardConnectivityState(),
    val telemetry: DashboardTelemetryState = DashboardTelemetryState(),
    val health: DashboardHealthState = DashboardHealthState()
) {
    val maxDropMs get() = connectivity.maxDropMs
    val lastSeenTs get() = connectivity.lastSeenTs
    val totalDropMs get() = connectivity.totalDropMs
    val watchdogOk get() = connectivity.watchdogOk
    val watchdogCountdownSec get() = connectivity.watchdogCountdownSec
    val totalUptimeMs get() = connectivity.totalUptimeMs
    val sessionMs get() = connectivity.sessionMs
    val sinceConnMs get() = connectivity.sinceConnMs
    val sinceDiscoMs get() = connectivity.sinceDiscoMs
    val engineVersion get() = connectivity.engineVersion
    val netInterface get() = connectivity.netInterface
    
    val lat get() = telemetry.lat
    val lng get() = telemetry.lng
    val gpsSpeedMps get() = telemetry.gpsSpeedMps
    val trackerAccuracy get() = telemetry.trackerAccuracy
    val trackerMaxAcc get() = telemetry.trackerMaxAcc
    val viewerAccuracy get() = telemetry.viewerAccuracy
    val viewerMaxAcc get() = telemetry.viewerMaxAcc
    val satsUsed get() = telemetry.satsUsed
    val satsView get() = telemetry.satsView
    val isSatsIndexWarning get() = telemetry.isSatsIndexWarning
    val snr get() = telemetry.snr
    val distToHome get() = telemetry.distToHome
    val distToViewer get() = telemetry.distToViewer
    val isGpsFresh get() = telemetry.isGpsFresh
    val isTelemetryFresh get() = telemetry.isTelemetryFresh
    val isLinkFresh get() = telemetry.isLinkFresh
    val isLocationPending get() = telemetry.isLocationPending
    val locationPendingReason get() = telemetry.locationPendingReason
    val trackerState get() = telemetry.trackerState
    val status get() = telemetry.status
    val tamperReason get() = telemetry.tamperReason
    val isTamperDetected get() = telemetry.isTamperDetected
    val isUltraLongStationary get() = telemetry.isUltraLongStationary
    val activityType get() = telemetry.activityType
    val gpsTs get() = telemetry.gpsTs

    val vibration get() = health.vibration
    val heading get() = health.heading
    val tilt get() = health.tilt
    val acousticDb get() = health.acousticDb
    val baroAlt get() = health.baroAlt
    val lux get() = health.lux
    val isNear get() = health.isNear
    val proximityCm get() = health.proximityCm
    val proximityDebounceMs get() = health.proximityDebounceMs
    val rollingVibration get() = health.rollingVibration
    val kineticEnergy get() = health.kineticEnergy
    val trackerMaxTemp get() = health.trackerMaxTemp
    val viewerMaxTemp get() = health.viewerMaxTemp
    val peakShock get() = health.peakShock
    val vibrationFloor get() = health.vibrationFloor
    val luxBaseline get() = health.luxBaseline
    val acousticFloorDb get() = health.acousticFloorDb
    val isMicPending get() = health.isMicPending
    val isPowerTamper get() = health.isPowerTamper
    val violationUptimeMs get() = health.violationUptimeMs
    val violationPercentage get() = health.violationPercentage
    val isPowerSaveMode get() = health.isPowerSaveMode
    val standbyBucket get() = health.standbyBucket
    val isStorageLow get() = health.isStorageLow
    val isStorageCritical get() = health.isStorageCritical
    val isBatterySteepDischarge get() = health.isBatterySteepDischarge
    val isCoolingModeActive get() = health.isCoolingModeActive
    val trackerCurrentMa get() = health.trackerCurrentMa
    val isBatteryLow get() = health.isBatteryLow
    val isBatteryCritical get() = health.isBatteryCritical
    val cpuLoad get() = health.cpuLoad
    val ioWait get() = health.ioWait
    val maxIoLatency get() = health.maxIoLatency
    val isSilentFailure get() = health.isSilentFailure
    val isMaliAnomaly get() = health.isMaliAnomaly
    val isGnssThrottled get() = health.isGnssThrottled
    val lastEnergyDeltaMa get() = health.lastEnergyDeltaMa
    val lastEnergyDeltaTemp get() = health.lastEnergyDeltaTemp
    val lastEnergyDurationMs get() = health.lastEnergyDurationMs
    val systemPulse get() = health.systemPulse
    val thermalHeadroom get() = health.thermalHeadroom
    val heapAllocatedMb get() = health.heapAllocatedMb
    val isSuspiciousNoise get() = health.isSuspiciousNoise
    val isMemoryPressureThrottled get() = health.isMemoryPressureThrottled
}

class StatsState(
    var totalConnectedMs: Long = 0L, var sessionConnectedMs: Long = 0L,
    var maxDropMs: Long = 0L, var maxDropTs: Long = 0L, var totalDropMs: Long = 0L,
    var uptimeMs: Long = 0L, var lastConnTs: Long = 0L, var lastDiscTs: Long = 0L,
    var violationUptimeMs: Long = 0L, var violationPercentage: Double = 0.0
) {
    fun copyFrom(other: StatsState) {
        this.totalConnectedMs = other.totalConnectedMs; this.sessionConnectedMs = other.sessionConnectedMs
        this.maxDropMs = other.maxDropMs; this.maxDropTs = other.maxDropTs; this.totalDropMs = other.totalDropMs
        this.uptimeMs = other.uptimeMs; this.lastConnTs = other.lastConnTs; this.lastDiscTs = other.lastDiscTs
        this.violationUptimeMs = other.violationUptimeMs; this.violationPercentage = other.violationPercentage
    }
    fun update(totalConnectedMs: Long, sessionConnectedMs: Long, maxDropMs: Long, maxDropTs: Long, totalDropMs: Long, uptimeMs: Long, lastConnTs: Long, lastDiscTs: Long) {
        this.totalConnectedMs = totalConnectedMs; this.sessionConnectedMs = sessionConnectedMs
        this.maxDropMs = maxDropMs; this.maxDropTs = maxDropTs; this.totalDropMs = totalDropMs
        this.uptimeMs = uptimeMs; this.lastConnTs = lastConnTs; this.lastDiscTs = lastDiscTs
    }
    fun reset() { update(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L); violationUptimeMs = 0L; violationPercentage = 0.0 }
}

class ConnectivityState(
    var isLocalOnline: Boolean = true,
    var isRelayConnected: Boolean = false,
    var isTrackerConnected: Boolean = false,
    var lastUpdateTs: Long = 0L,
    var lastRemoteActivityTs: Long = 0L,
    var connectedViewers: List<String> = emptyList(),
    var netInterface: String = "UNKNOWN"
) {
    fun copyFrom(other: ConnectivityState) {
        this.isLocalOnline = other.isLocalOnline
        this.isRelayConnected = other.isRelayConnected
        this.isTrackerConnected = other.isTrackerConnected
        this.lastUpdateTs = other.lastUpdateTs
        this.lastRemoteActivityTs = other.lastRemoteActivityTs
        this.connectedViewers = other.connectedViewers
        this.netInterface = other.netInterface
    }

    fun update(
        isLocalOnline: Boolean, isRelayConnected: Boolean, isTrackerConnected: Boolean,
        lastUpdateTs: Long, lastRemoteActivityTs: Long, connectedViewers: List<String>,
        netInterface: String
    ) {
        this.isLocalOnline = isLocalOnline
        this.isRelayConnected = isRelayConnected
        this.isTrackerConnected = isTrackerConnected
        this.lastUpdateTs = lastUpdateTs
        this.lastRemoteActivityTs = lastRemoteActivityTs
        this.connectedViewers = connectedViewers
        this.netInterface = netInterface
    }

    fun reset() {
        update(true, false, false, 0L, 0L, emptyList(), "UNKNOWN")
    }
}

class BatteryState(
    var level: Int = 100, var temp: Double = 0.0, var isCharging: Boolean = false, var isChargingStable: Boolean = false
) {
    fun copyFrom(other: BatteryState) {
        this.level = other.level; this.temp = other.temp; this.isCharging = other.isCharging; this.isChargingStable = other.isChargingStable
    }
    fun reset() { level = 100; temp = 0.0; isCharging = false; isChargingStable = false }
}

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
    override val trackerId: String = "TRK",
    override val viewerId: String = "VIEW",
    val watchdogOk: Boolean = true,
    val rtt: Int = 0,
    val remoteSignal: Int = 0,
    val isSystemActive: Boolean = false,
    val isSafeMode: Boolean = false,
    val isStaggered: Boolean = false,
    val isGnssThrottled: Boolean = false,
    val systemPulse: Long = 0L
) : DeviceIdentity

@Serializable
data class HudTelemetryState(
    val isLocalGpsActive: Boolean = false,
    val isGpsFresh: Boolean = false,
    val speedMps: Float = 0f,
    val trackerAccuracy: Float = 0f,
    val maxTrackerAccuracy: Float = 0f,
    val viewerAccuracy: Float = 0f,
    val maxViewerAccuracy: Float = 0f,
    val satsUsed: Int = -1,
    val satsView: Int = -1,
    val viewerSatsUsed: Int = -1,
    val viewerSatsView: Int = -1,
    val distToHome: Double? = null,
    val distToViewer: Double? = null,
    val lastGpsTs: Long = 0L,
    val viewerGpsTs: Long = 0L,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val isTrackerLocPending: Boolean = false,
    override val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isViewerLocPending: Boolean = false,
    val viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isUltraLongStationary: Boolean = false,
    val systemPulse: Long = 0L,
    val activityType: ActivityType = ActivityType.UNKNOWN
) : Locatable {
    override val isLocationPending: Boolean get() = isTrackerLocPending
}

@Serializable
data class HudHealthState(
    override val battery: Int = 100,
    val remoteBattery: Int = -1,
    override val isCharging: Boolean = false,
    val remoteCharging: Boolean = false,
    val trackerTemp: Float = 0f,
    val viewerTemp: Float = 0f,
    val hasActiveAlarms: Boolean = false,
    val isRedScreenSuppressed: Boolean = false,
    val isRedScreenVisible: Boolean = false,
    val isAlarmSilenced: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val activeAlarms: List<AlarmInfo> = emptyList(),
    val progressPulse: Float = 0f,
    val systemPulse: Long = 0L,
    val isMaliAnomaly: Boolean = false,
    val isSuspiciousNoise: Boolean = false,
    val isMemoryPressureThrottled: Boolean = false
) : BatteryProvider
