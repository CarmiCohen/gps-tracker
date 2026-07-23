package com.gps19.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.gps19.core.engine.*
import com.gps19.core.engine.LocationProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RemoteHandler: Handles incoming telemetry from the tracker in Viewer mode.
 * July.22.12:
 * - Issue #521: Deep Purge of Remote Settings Leftovers. Removed remote home_points sync.
 * v9.4.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized all monotonic 
 *   timestamps to use 'Rt' suffix.
 */
@Singleton
class RemoteHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val locationProcessor: LocationProcessor,
    private val alarmManager: AppAlarmManager,
    private val sessionManager: SessionManager,
    private val forensicUseCase: ServiceForensicUseCase,
    private val timeProvider: TimeProvider
) {
    interface Listener {
        fun onPeerPulse(id: String)
    }

    private var scope: CoroutineScope? = null
    private var listener: Listener? = null

    var isTrackerConnected = false
    var lastPeerActivityTs = 0L 
    var peerSignal = 0
    var lastPeerGpsTs = 0L
    private var lastRemotePacketTs = 0L

    // Tracker State
    var trackerLat = 0.0
    var trackerLng = 0.0
    var trackerSpeed = 0.0
    var trackerBearing = 0.0 
    var trackerAccuracy = 0.0
    var trackerMaxAccuracy = 0.0
    var trackerLastGpsTs = 0L
    var trackerLastValidFixRt = 0L 
    var trackerBattery = 0
    var trackerTemp = 0.0
    var trackerMaxTemp = 0.0
    var trackerCurrentMa = 0
    var trackerSatsView = 0
    var trackerSatsUsed = 0
    var isTrackerCharging = false
    var isTrackerJammerSuspicion = false
    var isTrackerVisualJump = false
    var isTrackerTrajectoryPromoted = false
    var trackerJumpTier = 0
    var isTrackerSuspicious = false
    var isTrackerTamperDetected = false
    var isTrackerPowerTamper = false
    var isTrackerSitDetected = false
    var isTrackerSitActive = false
    var trackerLastSitTs = 0L
    var trackerVerticalVelocity = 0.0
    var isTrackerClockRegression = false
    var isTrackerLocationPending = false
    var trackerLocationPendingReason = LocationPendingReason.NONE
    var trackerLocationDetail: GnssDetail? = null
    var trackerSnrIdx = 0.0
    var trackerTiltIdx = 0.0
    var trackerBaroIdx = 0.0
    var isTrackerBatterySteepDischarge = false 
    var isTrackerCoolingModeActive = false 
    
    var isTrackerPowerSaveMode = false
    var trackerStandbyBucket = -1
    var trackerNetInterface = "UNKNOWN"
    var isTrackerStorageLow = false
    var isTrackerStorageCritical = false
    var isTrackerAnchorLocked = false
    var trackerState = TrackerState.UNKNOWN

    var trackerSitVz = 0.0 
    var trackerSitDz = 0.0 
    var trackerSitBaro = 0.0 
    var trackerSitTilt = 0.0 
    var trackerSitShock = 0.0

    var trackerDistToHome: Double? = null
    var trackerDistToViewer: Double? = null

    var trackerVibration = 0.0
    var trackerHeading = 0.0
    var trackerBaroAlt = 0.0
    var trackerLux = 0.0
    var isTrackerNear = true
    var trackerTiltDegrees = 0.0
    var trackerAcousticDb = 0.0
    var trackerPeakVibrationShock = 0.0
    var trackerPeakVibrationShockTs = 0L
    var trackerLuxBaseline = 0.0
    var trackerAcousticFloorDb = 0.0
    var trackerAdaptiveVibrationFloor = 0.12
    var trackerProxIdx = 1.0
    var trackerProximityCm = -1.0
    var trackerProximityDebounceMs = 0L
    var trackerVibrationRollingSum = 0.0

    var trackerUptimeMs = 0L
    var trackerTotalDropMs = 0L
    var trackerMaxDropMs = 0L
    var trackerMaxDropTs = 0L
    var trackerTotalConnectedMs = 0L
    var trackerSessionConnectedMs = 0L
    var trackerLastConnTs = 0L
    var trackerLastDiscTs = 0L
    
    var trackerGpsStallStartTs = 0L 

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        
        scope.launch {
            try {
                trackerLuxBaseline = repository.getDouble(MainRepository.TRACKER_LUX_BASELINE_KEY, 0.0)
                trackerAcousticFloorDb = repository.getDouble(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
                
                repository.loadTrackerState()?.let { s ->
                    trackerLat = s.lat; trackerLng = s.lng; trackerSpeed = s.speed; trackerBearing = s.bearing
                    trackerAccuracy = s.accuracy; trackerMaxAccuracy = s.maxAccuracy; trackerLastGpsTs = s.gpsTs; trackerBattery = s.battery
                    trackerTemp = s.temp; trackerMaxTemp = s.maxTemp; trackerCurrentMa = s.currentMa
                    isTrackerCharging = s.isCharging; trackerSatsView = s.satsView; trackerSatsUsed = s.satsUsed
                    isTrackerJammerSuspicion = false; isTrackerVisualJump = false; isTrackerTrajectoryPromoted = false
                    trackerJumpTier = 0
                    isTrackerSuspicious = s.isSuspicious; isTrackerTamperDetected = s.isTamperDetected
                    isTrackerPowerTamper = s.isPowerTamper; isTrackerSitDetected = s.isSitDetected
                    isTrackerSitActive = s.isSitActive
                    trackerLastSitTs = s.lastSitTs; trackerVerticalVelocity = s.verticalVelocity
                    trackerSitVz = s.sitVz; trackerSitDz = s.sitDz; trackerSitBaro = s.sitBaro
                    trackerSitTilt = s.sitTilt; trackerSitShock = s.sitShock
                    trackerVibration = s.vibration; trackerHeading = s.heading; trackerBaroAlt = s.baroAlt
                    trackerLux = s.lux; isTrackerNear = s.isNear; trackerTiltDegrees = s.tiltDegrees
                    trackerAcousticDb = s.acousticDb; trackerPeakVibrationShock = s.peakVibrationShock
                    trackerPeakVibrationShockTs = s.peakVibrationShockTs; trackerLuxBaseline = s.luxBaseline
                    trackerAcousticFloorDb = s.acousticFloorDb; trackerAdaptiveVibrationFloor = s.adaptiveVibrationFloor
                    trackerProxIdx = s.proxIdx; trackerProximityCm = s.proximityCm
                    trackerProximityDebounceMs = s.proximityDebounceMs; trackerVibrationRollingSum = s.vibrationRollingSum
                    trackerUptimeMs = s.uptimeMs; trackerTotalDropMs = s.totalDropMs
                    trackerMaxDropMs = s.maxDropMs; trackerMaxDropTs = s.maxDropTs
                    trackerTotalConnectedMs = s.totalConnectedMs
                    trackerSessionConnectedMs = s.sessionConnectedMs; trackerLastConnTs = s.lastConnTs
                    trackerLastDiscTs = s.lastDiscTs
                    isTrackerLocationPending = s.isLocationPending
                    trackerLocationPendingReason = s.locationPendingReason
                    trackerLocationDetail = s.gnssDetail
                    trackerSnrIdx = s.snrIdx
                    trackerTiltIdx = s.tiltIdx
                    trackerBaroIdx = s.baroIdx
                    isTrackerBatterySteepDischarge = s.isBatterySteepDischarge
                    isTrackerCoolingModeActive = s.isCoolingModeActive
                    
                    isTrackerPowerSaveMode = s.isPowerSaveMode
                    trackerStandbyBucket = s.standbyBucket
                    trackerNetInterface = s.netInterface
                    isTrackerStorageLow = s.isStorageLow
                    isTrackerStorageCritical = s.isStorageCritical
                    isTrackerAnchorLocked = s.isAnchorLocked
                    trackerState = s.trackerState
                    
                    trackerLastValidFixRt = s.lastValidFixRt

                    repository.updateLocation(LocationUpdate(
                        lat = trackerLat, lng = trackerLng, speed = trackerSpeed, accuracy = trackerAccuracy, bearing = trackerBearing,
                        battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp, isCharging = isTrackerCharging, currentMa = trackerCurrentMa,
                        gpsTs = trackerLastGpsTs, isMe = false, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                        isJump = false, isJammer = false, isStalled = false,
                        maxAccuracy = trackerMaxAccuracy, signal = 0,
                        vibration = trackerVibration, heading = trackerBearing, baroAlt = trackerBaroAlt,
                        lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb,
                        peakVibrationShock = trackerPeakVibrationShock, peakVibrationShockTs = trackerPeakVibrationShockTs,
                        luxBaseline = trackerLuxBaseline, acousticFloorDb = trackerAcousticFloorDb,
                        adaptiveVibrationFloor = trackerAdaptiveVibrationFloor, isSuspicious = isTrackerSuspicious,
                        isTamperDetected = isTrackerTamperDetected, isPowerTamper = isTrackerPowerTamper,
                        isSitDetected = trackerLastSitTs > 0, isSitActive = isTrackerSitActive, lastSitTs = trackerLastSitTs,
                        verticalVelocity = trackerVerticalVelocity, sitVz = trackerSitVz, sitDz = trackerSitDz,
                        sitBaro = trackerSitBaro, sitTilt = trackerSitTilt, sitShock = trackerSitShock,
                        proxIdx = trackerProxIdx, proximityCm = trackerProximityCm,
                        proximityDebounceMs = trackerProximityDebounceMs, vibrationRollingSum = trackerVibrationRollingSum,
                        uptimeMs = trackerUptimeMs, totalDropMs = trackerTotalDropMs,
                        maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                        totalConnectedMs = trackerTotalConnectedMs,
                        sessionConnectedMs = trackerSessionConnectedMs, lastConnTs = trackerLastConnTs,
                        lastDiscTs = trackerLastDiscTs,
                        violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
                        isLocationPending = isTrackerLocationPending,
                        locationPendingReason = trackerLocationPendingReason,
                        lastValidFixRt = trackerLastValidFixRt,
                        isPowerSaveMode = isTrackerPowerSaveMode,
                        standbyBucket = trackerStandbyBucket,
                        netInterface = trackerNetInterface,
                        isStorageLow = isTrackerStorageLow,
                        isStorageCritical = isTrackerStorageCritical,
                        gnssDetail = trackerLocationDetail,
                        snrIdx = trackerSnrIdx,
                        tiltIdx = trackerTiltIdx,
                        baroIdx = trackerBaroIdx,
                        isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                        isCoolingModeActive = isTrackerCoolingModeActive,
                        isAnchorLocked = isTrackerAnchorLocked,
                        trackerState = trackerState,
                        ts = s.ts // Initial load from stored status
                    ))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading initial tracker state")
            }
        }
    }

    fun onRelayLost() {
        isTrackerConnected = false
    }

    fun resetStats() {
        isTrackerConnected = false
        lastPeerActivityTs = 0L
        lastRemotePacketTs = 0L
        peerSignal = 0
        lastPeerGpsTs = 0L
        trackerLat = 0.0; trackerLng = 0.0; trackerSpeed = 0.0; trackerBearing = 0.0; trackerAccuracy = 0.0
        trackerMaxAccuracy = 0.0; trackerLastGpsTs = 0L; trackerBattery = 0; trackerTemp = 0.0
        trackerMaxTemp = 0.0; trackerCurrentMa = 0; trackerSatsView = 0; trackerSatsUsed = 0
        isTrackerCharging = false; isTrackerJammerSuspicion = false; isTrackerVisualJump = false
        isTrackerTrajectoryPromoted = false; trackerJumpTier = 0; isTrackerSuspicious = false
        isTrackerTamperDetected = false; isTrackerPowerTamper = false
        isTrackerSitDetected = false; isTrackerSitActive = false; trackerLastSitTs = 0L; trackerVerticalVelocity = 0.0
        trackerSitVz = 0.0; trackerSitDz = 0.0; trackerSitBaro = 0.0; trackerSitTilt = 0.0; trackerSitShock = 0.0
        trackerDistToHome = null; trackerDistToViewer = null
        trackerVibration = 0.0; trackerHeading = 0.0; trackerBaroAlt = 0.0; trackerLux = 0.0
        isTrackerNear = true; trackerTiltDegrees = 0.0; trackerAcousticDb = 0.0
        trackerPeakVibrationShock = 0.0; trackerPeakVibrationShockTs = 0L
        trackerLuxBaseline = 0.0; trackerAcousticFloorDb = 0.0
        trackerAdaptiveVibrationFloor = 0.12; trackerProxIdx = 1.0; trackerProximityCm = -1.0
        trackerProximityDebounceMs = 0L; trackerVibrationRollingSum = 0.0
        trackerUptimeMs = 0L; trackerTotalDropMs = 0L; trackerMaxDropMs = 0L
        trackerMaxDropTs = 0L
        trackerTotalConnectedMs = 0L; trackerSessionConnectedMs = 0L
        trackerLastConnTs = 0L; trackerLastDiscTs = 0L
        trackerGpsStallStartTs = 0L
        trackerLastValidFixRt = 0L
        isTrackerClockRegression = false
        isTrackerLocationPending = false
        trackerLocationPendingReason = LocationPendingReason.NONE
        trackerLocationDetail = null
        trackerSnrIdx = 0.0
        trackerTiltIdx = 0.0
        trackerBaroIdx = 0.0
        isTrackerBatterySteepDischarge = false
        isTrackerCoolingModeActive = false
        
        isTrackerPowerSaveMode = false
        trackerStandbyBucket = -1
        trackerNetInterface = "UNKNOWN"
        isTrackerStorageLow = false
        isTrackerStorageCritical = false
        isTrackerAnchorLocked = false
        trackerState = TrackerState.UNKNOWN
        
        repository.saveDoubleSync(MainRepository.TRACKER_LUX_BASELINE_KEY, 0.0)
        repository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
    }

    fun handleRemoteLog(entry: LogEntry) {
        val now = timeProvider.currentTimeMillis()
        val nowRt = timeProvider.elapsedRealtime()
        
        lastPeerActivityTs = nowRt
        repository.updateRemoteActivity(now)

        if (entry.message.contains("Sit Detected", ignoreCase = true)) {
            if (!isTrackerSitDetected) {
                isTrackerSitDetected = true
            }
        }
    }

    fun handleRemoteUpdate(data: JSONObject, isTrackerMode: Boolean) {
        val fromId = data.optString("id")
        val fromViewerId = data.optString("viewer_id") // R182: Explicitly extract viewer ID
        val fromViewer = data.optBoolean("from_viewer", false) 
        val type = data.optString("type", "")
        val now = timeProvider.currentTimeMillis()
        val nowRt = timeProvider.elapsedRealtime()

        // R182: Correct peer ID determination based on mode.
        val peerId = if (isTrackerMode) {
            if (fromViewerId.isNotEmpty()) fromViewerId else fromId
        } else {
            fromId
        }

        if (isTrackerMode && fromViewer && type == "calibrate_chair") {
            locationProcessor.resetChairBaseline()
            repository.addLog(LogEntry(
                timestamp = now,
                message = "REMOTE CALIBRATION: Chair baseline zeroed via viewer command",
                type = "event",
                isImportant = true
            ))
            
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "REMOTE: Chair Baseline Zeroed", Toast.LENGTH_SHORT).show()
            }

            listener?.onPeerPulse(peerId)
            lastPeerActivityTs = nowRt
            repository.updateRemoteActivity(now)
            return
        }

        if (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity") {
            if (isTrackerMode && fromViewer) {
                listener?.onPeerPulse(peerId)
                lastPeerActivityTs = nowRt
                repository.updateRemoteActivity(now)
            } else if (!isTrackerMode && !fromViewer) {
                listener?.onPeerPulse(peerId)
                lastPeerActivityTs = nowRt
                isTrackerConnected = true
                repository.updateRemoteActivity(now)
            }
            return
        }

        if (isTrackerMode && fromViewer) {
            listener?.onPeerPulse(peerId)
            lastPeerActivityTs = nowRt
            repository.updateRemoteActivity(now)
            return
        }

        if (!isTrackerMode && !fromViewer) {
            val remoteTs = data.optLong("ts", 0L)
            if (remoteTs > 0 && remoteTs < lastRemotePacketTs) {
                return
            }
            if (remoteTs > 0) lastRemotePacketTs = remoteTs

            listener?.onPeerPulse(peerId)
            lastPeerActivityTs = nowRt
            isTrackerConnected = true
            repository.updateRemoteActivity(now)
            
            peerSignal = data.optInt("signal", 0)
            
            val hasIncomingGps = data.has("lat") || data.has("gps_ts") || data.has("gps_age_ms")
            val prevGpsTs = trackerLastGpsTs
            
            isTrackerSuspicious = data.optBoolean("is_suspicious", isTrackerSuspicious)
            isTrackerTamperDetected = data.optBoolean("is_tamper_detected", isTrackerTamperDetected)
            isTrackerPowerTamper = data.optBoolean("is_power_tamper", isTrackerPowerTamper)
            
            val incomingSitDetected = data.optBoolean("is_sit_detected", false)
            isTrackerSitDetected = incomingSitDetected

            isTrackerSitActive = data.optBoolean("is_sit_active", isTrackerSitActive)
            trackerLastSitTs = data.optLong("last_sit_ts", trackerLastSitTs)
            trackerVerticalVelocity = data.optDouble("vertical_velocity", trackerVerticalVelocity)
            isTrackerLocationPending = data.optBoolean("is_location_pending", false)
            
            val reasonStr = data.optString("location_pending_reason", "NONE")
            trackerLocationPendingReason = try { LocationPendingReason.valueOf(reasonStr) } catch(e: Exception) { LocationPendingReason.NONE }

            trackerLastValidFixRt = data.optLong("last_valid_fix_rt", trackerLastValidFixRt)
            trackerSnrIdx = data.optDouble("snr_idx", trackerSnrIdx)
            trackerTiltIdx = data.optDouble("tilt_idx", trackerTiltIdx)
            trackerBaroIdx = data.optDouble("baro_idx", trackerBaroIdx)
            isTrackerBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false)
            isTrackerCoolingModeActive = data.optBoolean("is_cooling_mode_active", false)
            
            isTrackerPowerSaveMode = data.optBoolean("is_power_save_mode", isTrackerPowerSaveMode)
            trackerStandbyBucket = data.optInt("standby_bucket", trackerStandbyBucket)
            trackerNetInterface = data.optString("net_interface", trackerNetInterface)
            isTrackerStorageLow = data.optBoolean("is_storage_low", isTrackerStorageLow)
            isTrackerStorageCritical = data.optBoolean("is_storage_critical", isTrackerStorageCritical)
            isTrackerAnchorLocked = data.optBoolean("is_anchor_locked", false)

            val stateStr = data.optString("tracker_state", "UNKNOWN")
            trackerState = try { TrackerState.valueOf(stateStr) } catch(e: Exception) { TrackerState.UNKNOWN }

            if (data.has("gnss_detail")) {
                try {
                    val array = data.getJSONArray("gnss_detail")
                    val satList = mutableListOf<SatelliteInfo>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        satList.add(SatelliteInfo(
                            svid = obj.getInt("svid"),
                            cn0 = obj.optDouble("cn0", 0.0),
                            usedInFix = obj.getBoolean("used_in_fix"),
                            constellation = obj.optInt("constellation", 0)
                        ))
                    }
                    trackerLocationDetail = GnssDetail(satellites = satList)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error parsing gnss_detail from remote")
                }
            }

            if (hasIncomingGps) {
                val incomingGpsTs = data.optLong("gps_ts", 0L)
                val gpsAgeMs = if (data.has("gps_age_ms")) {
                    data.optLong("gps_age_ms")
                } else {
                    if (incomingGpsTs > 0) maxOf(0L, now - incomingGpsTs) else 0L
                }
                
                val candidateTs = if (gpsAgeMs > 0 || incomingGpsTs > 0) now - gpsAgeMs else 0L
                val rawLat = data.optDouble("lat", 0.0)
                val rawLng = data.optDouble("lng", 0.0)
                
                val rawSpeedMps = data.optDouble("speed", -1.0)
                val rawBearing = data.optDouble("bearing", 0.0)
                val rawAccuracy = data.optDouble("accuracy", 0.0)
                val rawMaxAcc = data.optDouble("max_accuracy", 0.0)
                
                val isStalledPacket = data.optBoolean("is_stalled", false)
                val isJumpPacket = data.optBoolean("is_jump", false)
                val isJammerPacket = data.optBoolean("is_jammer", false)
                val isTrajectoryPacket = data.optBoolean("is_trajectory_promoted", false)
                val jumpTierPacket = data.optInt("jump_tier", 0)

                val processed = locationProcessor.processGpsPoint(
                    lat = rawLat,
                    lng = rawLng,
                    alt = data.optDouble("alt", 0.0),
                    androidSpeedMps = if (rawSpeedMps >= 0.0) rawSpeedMps else 0.0,
                    gpsTs = candidateTs,
                    accuracy = if (rawAccuracy > 0.0) rawAccuracy else 0.0,
                    bearing = rawBearing,
                    snr = 0.0,
                    satsUsed = data.optInt("sats_used", trackerSatsUsed),
                    isViewerTrail = false,
                    lastGpsTs = prevGpsTs,
                    providedMaxAccuracy = rawMaxAcc,
                    providedIsJump = isJumpPacket,
                    providedIsTrajectoryPromoted = isTrajectoryPacket,
                    providedJumpTier = jumpTierPacket,
                    providedIsAdaptiveJump = data.optBoolean("is_adaptive_jump", false),
                    providedIsJammer = isJammerPacket,
                    providedIsStalled = isStalledPacket,
                    providedIsTamper = isTrackerTamperDetected || isTrackerLocationPending,
                    isSuspicious = isTrackerSuspicious,
                    nowWall = now,
                    nowRt = nowRt
                )

                isTrackerClockRegression = processed.isClockRegression

                if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0) {
                    trackerLat = processed.optimizedPoint.lat
                    trackerLng = processed.optimizedPoint.lng
                    trackerLastGpsTs = processed.optimizedPoint.ts
                    lastPeerGpsTs = trackerLastGpsTs
                    if (!processed.isStalled) trackerLastValidFixRt = nowRt
                }
                
                trackerSpeed = processed.filteredSpeed
                trackerBearing = rawBearing
                
                if (rawAccuracy > 0.0) trackerAccuracy = rawAccuracy
                if (rawMaxAcc > 0.0) trackerMaxAccuracy = rawMaxAcc
                
                trackerSatsView = data.optInt("sats_view", trackerSatsView)
                trackerSatsUsed = data.optInt("sats_used", trackerSatsUsed)
                isTrackerJammerSuspicion = isJammerPacket
                isTrackerVisualJump = isJumpPacket
                isTrackerTrajectoryPromoted = isTrajectoryPacket
                trackerJumpTier = jumpTierPacket
            }
            
            trackerBattery = data.optInt("battery", trackerBattery)
            trackerTemp = data.optDouble("temp", trackerTemp)
            trackerMaxTemp = data.optDouble("max_temp", trackerMaxTemp)
            trackerCurrentMa = data.optInt("current_ma", trackerCurrentMa)
            isTrackerCharging = data.optBoolean("is_charging", isTrackerCharging)
            
            trackerSitVz = data.optDouble("sit_vz", trackerSitVz)
            trackerSitDz = data.optDouble("sit_dz", trackerSitDz)
            trackerSitBaro = data.optDouble("sit_baro", trackerSitBaro)
            trackerSitTilt = data.optDouble("sit_tilt", trackerSitTilt)
            trackerSitShock = data.optDouble("sit_shock", trackerSitShock)
            
            trackerVibration = data.optDouble("vibration", trackerVibration)
            trackerHeading = data.optDouble("heading", trackerHeading)
            trackerBaroAlt = data.optDouble("baro_alt", trackerBaroAlt)
            trackerLux = data.optDouble("lux", trackerLux)
            isTrackerNear = data.optBoolean("is_near", isTrackerNear)
            trackerProxIdx = data.optDouble("prox_idx", trackerProxIdx)
            trackerProximityCm = data.optDouble("proximity_cm", trackerProximityCm)
            trackerProximityDebounceMs = data.optLong("proximity_debounce_ms", trackerProximityDebounceMs)
            trackerVibrationRollingSum = data.optDouble("vibration_rolling_sum", trackerVibrationRollingSum)
            trackerTiltDegrees = data.optDouble("tilt_degrees", trackerTiltDegrees)
            trackerAcousticDb = data.optDouble("acoustic_db", trackerAcousticDb)
            trackerPeakVibrationShock = data.optDouble("peak_vibration_shock", trackerPeakVibrationShock)
            trackerPeakVibrationShockTs = data.optLong("peak_shock_ts", trackerPeakVibrationShockTs)
            
            val newLuxBaseline = data.optDouble("lux_baseline", trackerLuxBaseline)
            if (newLuxBaseline != trackerLuxBaseline) {
                trackerLuxBaseline = newLuxBaseline
                repository.saveDoubleSync(MainRepository.TRACKER_LUX_BASELINE_KEY, trackerLuxBaseline)
            }
            
            val newAcousticFloor = data.optDouble("acoustic_floor_db", trackerAcousticFloorDb)
            if (newAcousticFloor != trackerAcousticFloorDb) {
                trackerAcousticFloorDb = newAcousticFloor
                repository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, trackerAcousticFloorDb)
            }

            trackerAdaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", trackerAdaptiveVibrationFloor)
            
            locationProcessor.updateSensorData(
                vibration = trackerVibration,
                heading = trackerHeading,
                baroAlt = trackerBaroAlt,
                lux = trackerLux,
                isNear = isTrackerNear,
                powerTamper = isTrackerPowerTamper,
                tiltDegrees = trackerTiltDegrees,
                acousticDb = trackerAcousticDb,
                peakShock = trackerPeakVibrationShock,
                acousticMinDb = -1.0,
                peakVerticalVelocity = trackerVerticalVelocity,
                plungeMatched = false, 
                peakVerticalVelocityTs = 0L,
                isSirenActive = false,
                isWarming = false,
                manualAdaptiveFloor = trackerAdaptiveVibrationFloor,
                nowRt = nowRt,
                nowWall = now
            )

            trackerUptimeMs = data.optLong("uptime_ms", trackerUptimeMs)
            trackerTotalDropMs = data.optLong("total_drop_ms", trackerTotalDropMs)
            trackerMaxDropMs = data.optLong("max_drop_ms", trackerMaxDropMs)
            trackerMaxDropTs = data.optLong("max_drop_ts", trackerMaxDropTs)
            trackerTotalConnectedMs = data.optLong("total_connected_ms", trackerTotalConnectedMs)
            trackerSessionConnectedMs = data.optLong("session_connected_ms", trackerSessionConnectedMs)
            trackerLastConnTs = data.optLong("last_conn_ts", trackerLastConnTs)
            trackerLastDiscTs = data.optLong("last_disc_ts", trackerLastDiscTs)
            
            val violationUptimeMs = data.optLong("violation_uptime_ms", 0L)
            val violationPercentage = data.optDouble("violation_percentage", 0.0)

            val isStalled = data.optBoolean("is_stalled", false)
            if (isStalled && trackerGpsStallStartTs == 0L) trackerGpsStallStartTs = nowRt
            else if (!isStalled) trackerGpsStallStartTs = 0L

            scope?.launch {
                try {
                    repository.updateLocation(LocationUpdate(
                        lat = trackerLat, lng = trackerLng, speed = trackerSpeed, accuracy = trackerAccuracy, bearing = trackerBearing,
                        battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp, isCharging = isTrackerCharging, currentMa = trackerCurrentMa,
                        gpsTs = trackerLastGpsTs, isMe = false, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                        isJump = isTrackerVisualJump, isTrajectoryPromoted = isTrackerTrajectoryPromoted, jumpTier = trackerJumpTier, 
                        isJammer = isTrackerJammerSuspicion, isStalled = isStalled,
                        maxAccuracy = trackerMaxAccuracy, signal = peerSignal,
                        vibration = trackerVibration, heading = trackerBearing, baroAlt = trackerBaroAlt,
                        lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb,
                        peakVibrationShock = trackerPeakVibrationShock, peakVibrationShockTs = trackerPeakVibrationShockTs,
                        luxBaseline = trackerLuxBaseline, acousticFloorDb = trackerAcousticFloorDb,
                        adaptiveVibrationFloor = trackerAdaptiveVibrationFloor, isSuspicious = isTrackerSuspicious,
                        isTamperDetected = isTrackerTamperDetected,
                        isPowerTamper = isTrackerPowerTamper,
                        isSitDetected = incomingSitDetected,
                        isSitActive = isTrackerSitActive,
                        lastSitTs = trackerLastSitTs,
                        verticalVelocity = trackerVerticalVelocity,
                        sitVz = trackerSitVz,
                        sitDz = trackerSitDz,
                        sitBaro = trackerSitBaro,
                        sitTilt = trackerSitTilt,
                        sitShock = trackerSitShock,
                        proxIdx = trackerProxIdx,
                        proximityCm = trackerProximityCm,
                        proximityDebounceMs = trackerProximityDebounceMs,
                        vibrationRollingSum = trackerVibrationRollingSum,
                        uptimeMs = trackerUptimeMs, totalDropMs = trackerTotalDropMs, maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                        totalConnectedMs = trackerTotalConnectedMs,
                        sessionConnectedMs = trackerSessionConnectedMs,
                        lastConnTs = trackerLastConnTs, lastDiscTs = trackerLastDiscTs,
                        violationUptimeMs = violationUptimeMs,
                        violationPercentage = violationPercentage,
                        isClockRegression = isTrackerClockRegression,
                        isLocationPending = isTrackerLocationPending,
                        locationPendingReason = trackerLocationPendingReason,
                        lastValidFixRt = trackerLastValidFixRt,
                        isPowerSaveMode = isTrackerPowerSaveMode,
                        standbyBucket = trackerStandbyBucket,
                        netInterface = trackerNetInterface,
                        isStorageLow = isTrackerStorageLow,
                        isStorageCritical = isTrackerStorageCritical,
                        gnssDetail = trackerLocationDetail,
                        snrIdx = trackerSnrIdx,
                        tiltIdx = trackerTiltIdx,
                        baroIdx = trackerBaroIdx,
                        isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                        isCoolingModeActive = isTrackerCoolingModeActive,
                        isAnchorLocked = isTrackerAnchorLocked,
                        trackerState = trackerState,
                        ts = now
                    ))
                    
                    repository.saveTrackerState(TrackerStatus(
                        lat = trackerLat, lng = trackerLng, speed = trackerSpeed, bearing = trackerBearing, accuracy = trackerAccuracy,
                        gpsTs = trackerLastGpsTs, ts = now, rt = nowRt, battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp,
                        isCharging = isTrackerCharging, currentMa = trackerCurrentMa, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                        lastConnTs = trackerLastConnTs, lastDiscTs = trackerLastDiscTs, uptimeMs = trackerUptimeMs,
                        totalConnectedMs = trackerTotalConnectedMs,
                        sessionConnectedMs = trackerSessionConnectedMs, totalDropMs = trackerTotalDropMs,
                        maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                        violationUptimeMs = violationUptimeMs, violationPercentage = violationPercentage,
                        isSitDetected = incomingSitDetected, isSitActive = isTrackerSitActive, lastSitTs = trackerLastSitTs, verticalVelocity = trackerVerticalVelocity,
                        sitVz = trackerSitVz, sitDz = trackerSitDz, sitBaro = trackerSitBaro, sitTilt = trackerSitTilt, sitShock = trackerSitShock,
                        isPowerTamper = isTrackerPowerTamper, vibration = trackerVibration, heading = trackerHeading,
                        baroAlt = trackerBaroAlt, lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees,
                        acousticDb = trackerAcousticDb, peakVibrationShock = trackerPeakVibrationShock,
                        peakVibrationShockTs = trackerPeakVibrationShockTs, luxBaseline = trackerLuxBaseline,
                        acousticFloorDb = trackerAcousticFloorDb, adaptiveVibrationFloor = trackerAdaptiveVibrationFloor,
                        proxIdx = trackerProxIdx, proximityCm = trackerProximityCm,
                        proximityDebounceMs = trackerProximityDebounceMs, vibrationRollingSum = trackerVibrationRollingSum,
                        isSuspicious = isTrackerSuspicious, isTamperDetected = isTrackerTamperDetected,
                        isTrajectoryPromoted = isTrackerTrajectoryPromoted, jumpTier = trackerJumpTier,
                        isLocationPending = isTrackerLocationPending,
                        locationPendingReason = trackerLocationPendingReason,
                        lastValidFixRt = trackerLastValidFixRt,
                        isPowerSaveMode = isTrackerPowerSaveMode,
                        standbyBucket = trackerStandbyBucket,
                        netInterface = trackerNetInterface,
                        isStorageLow = isTrackerStorageLow,
                        isStorageCritical = isTrackerStorageCritical,
                        gnssDetail = trackerLocationDetail,
                        snrIdx = trackerSnrIdx,
                        tiltIdx = trackerTiltIdx,
                        baroIdx = trackerBaroIdx,
                        isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                        isCoolingModeActive = isTrackerCoolingModeActive,
                        isAnchorLocked = isTrackerAnchorLocked,
                        trackerState = trackerState
                    ))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error updating remote tracker state in DB")
                }
            }
        }
    }
}
