package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Models: UI and Persistence data structures for GPS Tracker.
 * Aug.13.08:
 * - Issue #157: Violation Path Allocations. Refactored ViolationPoint to a 
 *   mutable class with primitive coordinates to eliminate UUID and GeoPoint 
 *   churn in the detection and mapping hot-paths (R157).
 * Aug.13.06:
 * - Issue #152: Excessive GC Pressure. Refactored ConnectionPoint to a mutable 
 *   class with pooling support to eliminate object churn in the telemetry 
 *   hot-path (R152).
 */

sealed class AppSensorEvent {
    data class HardwareFailure(val reason: String) : AppSensorEvent()
    data class LogEvent(val message: String, val isImportant: Boolean) : AppSensorEvent()
}

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
}

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
    val tiltAlert: Boolean = true,
    val acousticAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tamperAlert: Boolean = true,
    val globalMute: Boolean = false,
    val systemStorageLow: Boolean = true
)

/**
 * Issue #152: Refactored to mutable class for zero-churn telemetry.
 */
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
    
    // Forensic Indices
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
    var sitVz: Double = 0.0,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,
    var kineticEnergy: Double = 0.0,

    // Performance & Load Correlation
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,

    // Anomaly Correlation
    var isSilentFailure: Boolean = false
) {
    fun copyFrom(other: ConnectionPoint) {
        this.localId = other.localId
        this.ts = other.ts
        this.rt = other.rt
        this.rtt = other.rtt
        this.localSig = other.localSig
        this.remoteSig = other.remoteSig
        this.isConnected = other.isConnected
        this.isGap = other.isGap
        this.isRecoveryEvent = other.isRecoveryEvent
        this.gpsAccuracy = other.gpsAccuracy
        this.maxAccuracy = other.maxAccuracy
        this.isTick = other.isTick
        this.hasGps = other.hasGps
        this.isBatterySteepDischarge = other.isBatterySteepDischarge
        this.isCoolingModeActive = other.isCoolingModeActive
        this.isBatteryLow = other.isBatteryLow
        this.isBatteryCritical = other.isBatteryCritical
        this.speed = other.speed
        this.bearing = other.bearing
        this.currentMa = other.currentMa
        this.status = other.status
        this.locationPendingReason = other.locationPendingReason
        this.gpsIndex = other.gpsIndex
        this.snrIdx = other.snrIdx
        this.noiseIdx = other.noiseIdx
        this.luxIdx = other.luxIdx
        this.vibeIdx = other.vibeIdx
        this.proxIdx = other.proxIdx
        this.liftIdx = other.liftIdx
        this.tiltIdx = other.tiltIdx
        this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected
        this.isSitActive = other.isSitActive
        this.sitVz = other.sitVz
        this.sitVzTs = other.sitVzTs
        this.sitVzRt = other.sitVzRt
        this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro
        this.sitTilt = other.sitTilt
        this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy
        this.cpuLoad = other.cpuLoad
        this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency
        this.isSilentFailure = other.isSilentFailure
    }

    fun reset() {
        localId = ""
        ts = 0; rt = 0; rtt = 0; localSig = 10; remoteSig = 0
        isConnected = false; isGap = false; isRecoveryEvent = false
        gpsAccuracy = 0.0; maxAccuracy = 0.0; isTick = false; hasGps = false
        isBatterySteepDischarge = false; isCoolingModeActive = false
        isBatteryLow = false; isBatteryCritical = false
        speed = 0.0; bearing = 0.0; currentMa = 0
        status = SentinelStatus.VALID; locationPendingReason = LocationPendingReason.NONE
        gpsIndex = 0.0; snrIdx = 0.0; noiseIdx = 0.0; luxIdx = 0.0; vibeIdx = 0.0
        proxIdx = 1.0; liftIdx = 0.0; tiltIdx = 0.0; baroIdx = 0.0
        isSitDetected = false; isSitActive = false; sitVz = 0.0; sitVzTs = 0L; sitVzRt = 0L
        sitDz = 0.0; sitBaro = 0.0; sitTilt = 0.0; sitShock = 0.0; kineticEnergy = 0.0
        cpuLoad = 0.0; ioWait = 0.0; maxIoLatency = 0L; isSilentFailure = false
    }
}

/**
 * Issue #157: Refactored to mutable class with primitive coords for zero-churn violations.
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
        if (cached != null && cached.latitude == lat && cached.longitude == lng) {
            return cached
        }
        val newPoint = GeoPoint(lat, lng)
        _cachedGeoPoint = newPoint
        return newPoint
    }

    fun copyFrom(other: ViolationPoint) {
        this.localId = other.localId
        this.lat = other.lat
        this.lng = other.lng
        this.type = other.type
        this.ts = other.ts
        this.accuracy = other.accuracy
        this.maxAccuracy = other.maxAccuracy
    }
}

@Serializable
data class LogEntry(
    val localId: String = UUID.randomUUID().toString(),
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
    val snrSnapshot: Double? = null,
    val vibeSnapshot: Double? = null,
    val spillIdx: Int = -1,
    val gpsHardwareLock: Boolean = false 
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("localId", localId); put("timestamp", timestamp)
            put("localTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            put("message", message); put("type", type); put("isImportant", isImportant)
            put("id", SignalingConstants.getTransmissionId(id))
            put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
            put("count", count)
            put("duration_ms", durationMs)
            put("is_special", isSpecial)
            put("first_seen_ts", if (firstSeenTs == 0L) timestamp else firstSeenTs)
            put("role", role)
            if (lat != 0.0) put("lat", lat)
            if (lng != 0.0) put("lng", lng)
            if (accuracy != 0.0) put("accuracy", accuracy)
            if (maxAccuracy != 0.0) put("max_accuracy", maxAccuracy)
            specialColor?.let { put("special_color", it) }
            extremeValue?.let { if (!it.isNaN() && !it.isInfinite()) put("extreme_value", it) }
            snrSnapshot?.let { put("snr_snapshot", it) }
            vibeSnapshot?.let { put("vibe_snapshot", it) }
            if (spillIdx != -1) put("spill_idx", spillIdx)
            if (gpsHardwareLock) put("gps_hw_lock", true)
        }
    }

    companion object {
        fun fromJSONObject(obj: JSONObject): LogEntry {
            val ts = obj.optLong("timestamp")
            val localId = obj.optString("localId").ifBlank { UUID.randomUUID().toString() }
            
            return LogEntry(
                localId = localId,
                timestamp = ts,
                message = obj.optString("message"),
                type = obj.optString("type"),
                isImportant = obj.optBoolean("isImportant"),
                id = obj.optString("id"),
                viewerId = obj.optString("viewer_id"),
                count = obj.optInt("count", 1),
                durationMs = obj.optLong("duration_ms", 0L),
                isSpecial = obj.optBoolean("is_special", false),
                specialColor = if (obj.has("special_color")) obj.getInt("special_color") else null,
                firstSeenTs = if (obj.has("first_seen_ts")) obj.getLong("first_seen_ts") else ts,
                role = obj.optString("role", "tracker"),
                extremeValue = if (obj.has("extreme_value")) {
                    val ev = obj.optDouble("extreme_value")
                    if (ev.isNaN() || ev.isInfinite()) null else ev
                } else null,
                lat = obj.optDouble("lat", 0.0),
                lng = obj.optDouble("lng", 0.0),
                accuracy = obj.optDouble("accuracy", 0.0),
                maxAccuracy = obj.optDouble("max_accuracy", 0.0),
                snrSnapshot = if (obj.has("snr_snapshot")) obj.optDouble("snr_snapshot") else null,
                vibeSnapshot = if (obj.has("vibe_snapshot")) obj.optDouble("vibe_snapshot") else null,
                spillIdx = obj.optInt("spill_idx", -1),
                gpsHardwareLock = obj.optBoolean("gps_hw_lock", false)
            )
        }
    }
}

@Serializable
data class TrackerStatus(
    val deviceId: String = "",
    val viewerId: String = "",
    override val lat: Double = 0.0,
    override val lng: Double = 0.0,
    override val alt: Double = 0.0,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    override val gpsTs: Long = 0L,
    override val ts: Long = 0L,
    override val rt: Long = 0L, 
    val uptimeMs: Long = 0L,
    val lastConnTs: Long = 0L,
    val lastDiscTs: Long = 0L,
    val totalDropMs: Long = 0L,
    val maxDropMs: Long = 0L,
    val maxDropTs: Long = 0L,
    val totalConnectedMs: Long = 0L,
    val sessionConnectedMs: Long = 0L,
    val battery: Int = 100,
    val temp: Double = 0.0,
    val maxTemp: Double = 0.0,
    val isCharging: Boolean = false,
    val currentMa: Int = 0,
    val satsView: Int = 0,
    val satsUsed: Int = 0,
    val peakVibrationShock: Double = 0.0,
    val peakVibrationShockTs: Long = 0L,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Double = 0.0,
    val status: SentinelStatus = SentinelStatus.VALID,
    val isJammer: Boolean = false,
    val isStalled: Boolean = false,
    val isTamperDetected: Boolean = false,
    val vibration: Double = 0.0,
    val heading: Double = 0.0,
    val tiltDegrees: Double = 0.0,
    val acousticDb: Double = 0.0,
    val baroAlt: Double = 0.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val adaptiveVibrationFloor: Double = 0.12,
    val proxIdx: Double = 1.0,
    val proximityCm: Double = -1.0,
    val proximityDebounceMs: Long = 0L,
    val vibrationRollingSum: Double = 0.0,
    val isClockRegression: Boolean = false,
    val jumpTier: Int = 0,
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val lastValidFixRt: Long = 0L,
    val isPowerSaveMode: Boolean = false, 
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val gnssDetail: GnssDetail? = null,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val snrIdx: Double = 0.0,
    val noiseIdx: Double = 0.0,
    val luxIdx: Double = 0.0,
    val vibeIdx: Double = 0.0,
    val liftIdx: Double = 0.0,
    val isAnchorLocked: Boolean = false,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val lastSitTs: Long = 0L,
    val verticalVelocity: Double = 0.0,
    val sitVz: Double = 0.0,
    val sitVzTs: Long = 0L,
    val sitVzRt: Long = 0L,
    val sitDz: Double = 0.0,
    val sitBaro: Double = 0.0,
    val sitTilt: Double = 0.0,
    val sitShock: Double = 0.0,
    val isJump: Boolean = false,
    val isTrajectoryPromoted: Boolean = false,
    val isSuspicious: Boolean = false,
    val tiltIdx: Double = 0.0,
    val baroIdx: Double = 0.0,
    val micPending: Boolean = false,
    val kineticEnergy: Double = 0.0,
    val isAdaptiveJump: Boolean = false,
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    val isSilentFailure: Boolean = false
) : SpatialAnchor {

    fun toMap(fromViewer: Boolean): Map<String, Any?> {
        return mutableMapOf<String, Any?>().apply {
            put("id", SignalingConstants.getTransmissionId(deviceId))
            put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
            put("from_viewer", fromViewer)
            put("lat", lat); put("lng", lng); put("alt", alt)
            put("speed", speed); put("bearing", bearing); put("accuracy", accuracy); put("max_accuracy", maxAccuracy)
            put("gps_ts", gpsTs); put("ts", ts); put("rt", rt)
            put("uptime_ms", uptimeMs); put("last_conn_ts", lastConnTs); put("last_disc_ts", lastDiscTs)
            put("total_drop_ms", totalDropMs); put("max_drop_ms", maxDropMs); put("max_drop_ts", maxDropTs)
            put("total_connected_ms", totalConnectedMs); put("session_connected_ms", sessionConnectedMs)
            put("battery", battery); put("temp", temp); put("max_temp", maxTemp)
            put("is_charging", isCharging); put("current_ma", currentMa)
            put("sats_view", satsView); put("sats_used", satsUsed)
            put("peak_vibration_shock", peakVibrationShock); put("peak_shock_ts", peakVibrationShockTs)
            put("is_power_tamper", isPowerTamper)
            put("violation_uptime_ms", violationUptimeMs); put("violation_percentage", violationPercentage)
            put("status", status.name); put("is_jammer", isJammer); put("is_stalled", isStalled); put("is_tamper_detected", isTamperDetected)
            put("vibration", vibration); put("heading", heading); put("tilt_degrees", tiltDegrees)
            put("acoustic_db", acousticDb); put("baro_alt", baroAlt); put("lux", lux); put("is_near", isNear)
            put("lux_baseline", luxBaseline); put("acoustic_floor_db", acousticFloorDb); put("adaptiveVibrationFloor", adaptiveVibrationFloor)
            put("prox_idx", proxIdx); put("proximity_cm", proximityCm); put("proximity_debounce_ms", proximityDebounceMs)
            put("vibration_rolling_sum", vibrationRollingSum); put("is_clock_regression", isClockRegression)
            put("jump_tier", jumpTier); put("is_location_pending", isLocationPending); put("location_pending_reason", locationPendingReason.name)
            put("last_valid_fix_rt", lastValidFixRt); put("is_power_save_mode", isPowerSaveMode)
            put("standby_bucket", standbyBucket); put("net_interface", netInterface)
            put("is_storage_low", isStorageLow); put("is_storage_critical", isStorageCritical)
            put("is_battery_steep_discharge", isBatterySteepDischarge); put("is_cooling_mode_active", isCoolingModeActive)
            put("tracker_state", trackerState.name); put("is_sit_detected", isSitDetected); put("last_sit_ts", lastSitTs)
            put("is_jump", isJump); put("mic_pending", micPending)
            put("snr_idx", snrIdx); put("noise_idx", noiseIdx); put("lux_idx", luxIdx); put("vibe_idx", vibeIdx); put("lift_idx", liftIdx)
            put("tilt_idx", tiltIdx); put("baro_idx", baroIdx)
            put("is_sit_active", isSitActive)
            put("sit_vz", sitVz); put("sit_vz_ts", sitVzTs); put("sit_vz_rt", sitVzRt)
            put("sit_dz", sitDz); put("sit_baro", sitBaro); put("sit_tilt", sitTilt); put("sit_shock", sitShock)
            put("vertical_velocity", verticalVelocity)
            put("kinetic_energy", kineticEnergy)
            put("is_adaptive_jump", isAdaptiveJump)
            put("is_battery_low", isBatteryLow)
            put("is_battery_critical", isBatteryCritical)
            put("is_silent_failure", isSilentFailure)
        }
    }

    fun toJSONObject(fromViewer: Boolean): JSONObject {
        return JSONObject(toMap(fromViewer) as Map<*, *>)
    }

    fun writeTo(builder: RealtimeStatus.Builder, fromViewer: Boolean) {
        builder.clear()
            .setId(SignalingConstants.getTransmissionId(deviceId))
            .setViewerId(SignalingConstants.getTransmissionId(viewerId))
            .setFromViewer(fromViewer)
            .setLat(lat)
            .setLng(lng)
            .setAlt(alt)
            .setSpeed(speed)
            .setBearing(bearing)
            .setAccuracy(accuracy)
            .setMaxAccuracy(maxAccuracy)
            .setGpsTs(gpsTs)
            .setTs(ts)
            .setBattery(battery)
            .setTemp(temp)
            .setIsCharging(isCharging)
            .setSatsView(satsView)
            .setSatsUsed(satsUsed)
            .setUptimeMs(uptimeMs)
            .setTotalConnectedMs(totalConnectedMs)
            .setSessionConnectedMs(sessionConnectedMs)
            .setTotalDropMs(totalDropMs)
            .setMaxDropMs(maxDropMs)
            .setLastConnTs(lastConnTs)
            .setLastDiscTs(lastDiscTs)
            .setState(mapTrackerStateToProto(trackerState))
            .setIsLocationPending(isLocationPending)
            .setPendingReason(mapPendingReasonToProto(locationPendingReason))
            .setIsBatterySteepDischarge(isBatterySteepDischarge)
            .setIsCoolingModeActive(isCoolingModeActive)
            .setSnrIdx(snrIdx)
            .setNoiseIdx(noiseIdx)
            .setLuxIdx(luxIdx)
            .setVibeIdx(vibeIdx)
            .setLiftIdx(liftIdx)
            .setProxIdx(proxIdx)
            .setTiltIdx(tiltIdx)
            .setBaroIdx(baroIdx)
            .setIsSitDetected(isSitDetected)
            .setIsSitActive(isSitActive)
            .setLastSitTs(lastSitTs)
            .setSitVz(sitVz)
            .setSitVzTs(sitVzTs)
            .setSitVzRt(sitVzRt)
            .setSitDz(sitDz)
            .setSitBaro(sitBaro)
            .setSitTilt(sitTilt)
            .setSitShock(sitShock)
            .setVerticalVelocity(verticalVelocity)
            .setIsJammer(isJammer)
            .setIsStalled(isStalled)
            .setIsTamperDetected(isTamperDetected)
            .setJumpTier(jumpTier)
            .setSentinelStatus(status.name)
            .setLastValidFixRt(lastValidFixRt)
            .setIsClockRegression(isClockRegression)
            .setKineticEnergy(kineticEnergy)
            .setIsAdaptiveJump(isAdaptiveJump)
            .setIsBatteryLow(isBatteryLow)
            .setIsBatteryCritical(isBatteryCritical)
            .setIsSilentFailure(isSilentFailure)
    }

    fun toProto(fromViewer: Boolean): RealtimeStatus {
        val builder = RealtimeStatus.newBuilder()
        writeTo(builder, fromViewer)
        return builder.build()
    }

    private fun mapTrackerStateToProto(state: TrackerState): TrackerStateProto {
        return when(state) {
            TrackerState.MOVING -> TrackerStateProto.TS_MOVING
            TrackerState.PARKING -> TrackerStateProto.TS_PARKING
            TrackerState.JUMPING -> TrackerStateProto.TS_JUMPING
            TrackerState.OFFLINE -> TrackerStateProto.TS_OFFLINE
            else -> TrackerStateProto.TS_UNKNOWN
        }
    }

    private fun mapPendingReasonToProto(reason: LocationPendingReason): LocationPendingReasonProto {
        return when(reason) {
            LocationPendingReason.GPS_STALL -> LocationPendingReasonProto.LPR_GPS_STALL
            LocationPendingReason.GPS_GAP -> LocationPendingReasonProto.LPR_GPS_GAP
            LocationPendingReason.ACOUSTIC_VIOLATION -> LocationPendingReasonProto.LPR_ACOUSTIC_VIOLATION
            LocationPendingReason.SIGNAL_LOSS -> LocationPendingReasonProto.LPR_SIGNAL_LOSS
            LocationPendingReason.JAMMER_SUSPICION -> LocationPendingReasonProto.LPR_JAMMER_SUSPICION
            else -> LocationPendingReasonProto.LPR_NONE
        }
    }

    companion object {
        fun mapProtoToTrackerState(proto: TrackerStateProto): TrackerState {
            return when(proto) {
                TrackerStateProto.TS_MOVING -> TrackerState.MOVING
                TrackerStateProto.TS_PARKING -> TrackerState.PARKING
                TrackerStateProto.TS_JUMPING -> TrackerState.JUMPING
                TrackerStateProto.TS_OFFLINE -> TrackerState.OFFLINE
                else -> TrackerState.UNKNOWN
            }
        }

        fun mapProtoToPendingReason(proto: LocationPendingReasonProto): LocationPendingReason {
            return when(proto) {
                LocationPendingReasonProto.LPR_GPS_STALL -> LocationPendingReason.GPS_STALL
                LocationPendingReasonProto.LPR_GPS_GAP -> LocationPendingReason.GPS_GAP
                LocationPendingReasonProto.LPR_ACOUSTIC_VIOLATION -> LocationPendingReason.ACOUSTIC_VIOLATION
                LocationPendingReasonProto.LPR_SIGNAL_LOSS -> LocationPendingReason.SIGNAL_LOSS
                LocationPendingReasonProto.LPR_JAMMER_SUSPICION -> LocationPendingReason.JAMMER_SUSPICION
                else -> LocationPendingReason.NONE
            }
        }
    }
}

data class AlarmInfo(val title: String, val subtitle: String, val type: String = "", val isResolved: Boolean = false, val isSirenDisabled: Boolean = false)

/**
 * LocationState: Pure position data. Hardware/Health metadata moved to SystemHealthState.
 */
class LocationState(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var speed: Double = 0.0,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var bearing: Double = 0.0,
    var timestamp: Long = 0L,
    var telemetryTs: Long = 0L, 
    var status: SentinelStatus = SentinelStatus.VALID,
    var trackerState: TrackerState = TrackerState.UNKNOWN,
    var gnssDetail: GnssDetail? = null
) {
    fun copyFrom(other: LocationState) {
        this.lat = other.lat
        this.lng = other.lng
        this.speed = other.speed
        this.accuracy = other.accuracy
        this.maxAccuracy = other.maxAccuracy
        this.bearing = other.bearing
        this.timestamp = other.timestamp
        this.telemetryTs = other.telemetryTs
        this.status = other.status
        this.trackerState = other.trackerState
        this.gnssDetail = other.gnssDetail
    }

    fun update(
        lat: Double, lng: Double, speed: Double, accuracy: Double, maxAccuracy: Double,
        bearing: Double, timestamp: Long, telemetryTs: Long, status: SentinelStatus,
        trackerState: TrackerState, gnssDetail: GnssDetail?
    ) {
        this.lat = lat
        this.lng = lng
        this.speed = speed
        this.accuracy = accuracy
        this.maxAccuracy = maxAccuracy
        this.bearing = bearing
        this.timestamp = timestamp
        this.telemetryTs = telemetryTs
        this.status = status
        this.trackerState = trackerState
        this.gnssDetail = gnssDetail
    }

    fun reset() {
        update(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, SentinelStatus.VALID, TrackerState.UNKNOWN, null)
    }
}

data class DashboardState(
    val maxDrop: String = "00:00:00",
    val lastSeen: String = "--:--:--",
    val totalDrop: String = "00:00:00",
    val watchdogOk: Boolean = true,
    val watchdogCountdown: String = "--",
    val totalUptime: String = "00:00:00",
    val session: String = "00:00:00",
    val sinceConn: String = "00:00:00",
    val sinceDisco: String = "00:00:00",
    val gpsIndex: String = "--",
    val trackerAccuracy: String = "--",
    val satsIndex: String = "--",
    val trackerMaxAcc: String = "--",
    val viewerAccuracy: String = "--",
    val viewerMaxAcc: String = "--",
    val ageIndex: String = "--",
    val accIndex: String = "--",
    val isSatsIndexWarning: Boolean = false,
    val trackerConnIndex: Int = 0,
    val viewerConnIndex: Int = 0,
    val trackerTemp: String = "--",
    val trackerMaxTemp: String = "--",
    val viewerTemp: String = "--",
    val viewerMaxTemp: String = "--",
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val status: SentinelStatus = SentinelStatus.VALID,
    val vibration: String = "--",
    val heading: String = "--",
    val lat: String = "--",
    val lng: String = "--",
    val tilt: String = "--",
    val acoustic: String = "--",
    val lift: String = "--",
    val lux: String = "--",
    val proximity: String = "--",
    val proximityCm: String = "--", 
    val proximityDebounce: String = "--",
    val rollingVibration: String = "--",
    val kineticEnergy: String = "--",
    val gpsSpeed: String = "--",     
    val isTamperDetected: Boolean = false,
    val peakShock: String = "--",
    val luxBaseline: String = "--",
    val acousticFloor: String = "--",
    val vibrationFloor: String = "--",
    val isMicPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptime: String = "00:00:00",
    val violationPercentage: String = "0.0%",
    val engineVersion: String = "--",
    val isLocationPending: Boolean = false,
    val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val snr: String = "--",
    val distToHome: String = "--",    
    val distToViewer: String = "--",  
    val isGpsFresh: Boolean = true,    
    val isLinkFresh: Boolean = true,
    val isTelemetryFresh: Boolean = true,
    val isGpsVisible: Boolean = true,  
    val isLinkVisible: Boolean = true,   
    val isBatterySteepDischarge: Boolean = false, 
    val isCoolingModeActive: Boolean = false,
    val trackerCurrentMa: String = "--",
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    
    // Issue #132: Forensic UI Dashboard Refinement
    val cpuLoad: String = "--",
    val ioWait: String = "--",
    val maxIoLatency: String = "--",

    // Issue #133: Forensic Anomaly Correlation
    val isSilentFailure: Boolean = false
)

sealed class UiEvent {
    data class ToggleMap(val visible: Boolean) : UiEvent()
    data class ToggleLog(val visible: Boolean) : UiEvent()
    data class ToggleSettings(val visible: Boolean) : UiEvent()
    data class TogglePhoneSetup(val visible: Boolean) : UiEvent()
    data class ToggleRibbons(val visible: Boolean) : UiEvent()
    data class ToggleStrictMode(val visible: Boolean) : UiEvent()
    data class SetRedScreenVisible(val visible: Boolean) : UiEvent()
    data class SetDashboardExpanded(val expanded: Boolean) : UiEvent()
    data class SetUiVisible(val visible: Boolean) : UiEvent()
    object DismissAlarms : UiEvent()
    data class SetAppMode(val mode: String?) : UiEvent()
    data class SetPendingMode(val mode: String?) : UiEvent()
    data class SetSystemActive(val active: Boolean) : UiEvent()
    data class SetSystemMode(val mode: String) : UiEvent()
    data class StopSiren(val causes: String? = null) : UiEvent()
    object ResetStats : UiEvent()
    object ClearLogs : UiEvent()
    object ClearTrails : UiEvent()
    object ClearHomePoints : UiEvent()
    object ManualExit : UiEvent()
    data class LogAction(val type: String, val message: String, val isImportant: Boolean = false, val isSpecial: Boolean = false, val specialColor: Int? = null) : UiEvent()
    data class AddHomePoint(val point: GeoPoint) : UiEvent()
    data class RemoveHomePoint(val index: Int) : UiEvent()
    data class SetGeofenceMode(val mode: GeofenceMode) : UiEvent()
    data class MapTap(val point: GeoPoint) : UiEvent()
    data class SetMaxDistance(val distance: Double) : UiEvent()
    data class SetHomePoints(val points: List<GeoPoint>) : UiEvent()
    object SaveHomePoints : UiEvent()
    data class SetAlertSettings(val settings: AlertSettings) : UiEvent()
    data class SetSirenType(val type: String) : UiEvent()
    data class SetFenceVisible(val visible: Boolean) : UiEvent()
    data class SetViolationsVisible(val visible: Boolean) : UiEvent()
    data class SetGeofenceViolationsVisible(val visible: Boolean) : UiEvent() 
    data class SetMapButtonsVisible(val visible: Boolean) : UiEvent()
    data class SetMapLocked(val locked: Boolean) : UiEvent()
    object MapZoomIn : UiEvent()
    object MapZoomOut : UiEvent()
    object CenterTracker : UiEvent()
    object CenterViewer : UiEvent()
    data class SetDeviceId(val id: String) : UiEvent()
    data class SetViewerId(val id: String) : UiEvent()
    data class SetRelayUrl(val url: String) : UiEvent()
    data class UpdateDraftDeviceId(val id: String) : UiEvent()
    data class UpdateDraftViewerId(val id: String) : UiEvent()
    data class UpdateDraftRelayUrl(val url: String) : UiEvent()
    data class UpdateDraftMaxDistance(val distance: String) : UiEvent()
    data class UpdateDraftAlertSettings(val settings: AlertSettings) : UiEvent()
    data class UpdateDraftAlarmVolume(val volume: Float) : UiEvent()
    object CommitSettings : UiEvent()
    object RefreshPermissionStatus : UiEvent()
    object RequestTestAlarm : UiEvent()
    object RequestForensicTest : UiEvent()
    data class ToggleAlertsSetup(val visible: Boolean) : UiEvent()
    data class ToggleAlarmSoundSetup(val visible: Boolean) : UiEvent()
    object ToggleTestSiren : UiEvent()
    data class SetJammerSuspicion(val isJammer: Boolean) : UiEvent()
    data class SetSignalLoss(val isSignalLoss: Boolean) : UiEvent()
    data class BulkUpdateSettings(
        val deviceId: String? = null,
        val viewerId: String? = null,
        val relayUrl: String? = null,
        val maxDistance: Double? = null,
        val homePoints: List<GeoPoint>? = null,
        val alertSettings: AlertSettings? = null
    ) : UiEvent()
    data class ShowStopTrackingConfirmation(val show: Boolean) : UiEvent()
    object ConfirmStopTracking : UiEvent()
    data class SetSubSettings(val sub: SubSettings?) : UiEvent()
    data class SetLogFilterShowDetails(val show: Boolean) : UiEvent()
    data class SetLogFilterShowRecovered(val show: Boolean) : UiEvent()
    data class ToggleGnssDetail(val visible: Boolean) : UiEvent()
    object ToggleXiaomiManualOverride : UiEvent()
    object DismissIdentitySanitization : UiEvent()
    data class NavigateToDiagnostics(val visible: Boolean) : UiEvent()
    
    // Issue #626/634: Foreground Service Start Hardening
    object TriggerRecovery : UiEvent()
    data class SetRecoveryPending(val pending: Boolean) : UiEvent()
}

sealed class UiCommand {
    object SyncRequest : UiCommand()
    data class UiVisibilityChanged(val visible: Boolean) : UiCommand()
    data class StopSiren(val causes: String? = null) : UiCommand()
    object ClearTrails : UiCommand()
    object StatsReset : UiCommand()
    object SettingsUpdated : UiCommand()
    object ZoomIn : UiCommand()
    object ZoomOut : UiCommand()
    object FullInitializationReset : UiCommand()
    object ExecuteTestAlarm : UiCommand()
    object ExecuteForensicTest : UiCommand()
    object MapZoomIn : UiCommand()
    object MapZoomOut : UiCommand()
}

/**
 * StatsState: Connectivity statistics.
 */
class StatsState(
    var totalConnectedMs: Long = 0L, var sessionConnectedMs: Long = 0L,
    var maxDropMs: Long = 0L, var maxDropTs: Long = 0L, var totalDropMs: Long = 0L,
    var uptimeMs: Long = 0L, var lastConnTs: Long = 0L, var lastDiscTs: Long = 0L,
    var violationUptimeMs: Long = 0L, var violationPercentage: Double = 0.0
) {
    fun copyFrom(other: StatsState) {
        this.totalConnectedMs = other.totalConnectedMs
        this.sessionConnectedMs = other.sessionConnectedMs
        this.maxDropMs = other.maxDropMs
        this.maxDropTs = other.maxDropTs
        this.totalDropMs = other.totalDropMs
        this.uptimeMs = other.uptimeMs
        this.lastConnTs = other.lastConnTs
        this.lastDiscTs = other.lastDiscTs
        this.violationUptimeMs = other.violationUptimeMs
        this.violationPercentage = other.violationPercentage
    }

    fun update(
        totalConnectedMs: Long, sessionConnectedMs: Long, maxDropMs: Long, maxDropTs: Long,
        totalDropMs: Long, uptimeMs: Long, lastConnTs: Long, lastDiscTs: Long
    ) {
        this.totalConnectedMs = totalConnectedMs
        this.sessionConnectedMs = sessionConnectedMs
        this.maxDropMs = maxDropMs
        this.maxDropTs = maxDropTs
        this.totalDropMs = totalDropMs
        this.uptimeMs = uptimeMs
        this.lastConnTs = lastConnTs
        this.lastDiscTs = lastDiscTs
    }

    fun reset() {
        update(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
        violationUptimeMs = 0L
        violationPercentage = 0.0
    }
}

/**
 * BatteryState: Device battery and thermal status.
 */
class BatteryState(
    var level: Int = 100, var temp: Double = 0.0, var isCharging: Boolean = false, var isChargingStable: Boolean = false
) {
    fun copyFrom(other: BatteryState) {
        this.level = other.level
        this.temp = other.temp
        this.isCharging = other.isCharging
        this.isChargingStable = other.isChargingStable
    }

    fun reset() {
        level = 100
        temp = 0.0
        isCharging = false
        isChargingStable = false
    }
}
