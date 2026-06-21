package com.gps19.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.gps19.core.engine.*
import com.gps19.core.engine.LocationProcessor // Explicit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import org.osmdroid.util.GeoPoint

/**
 * RemoteHandler: Handles incoming telemetry from the tracker in Viewer mode.
 * v8.9.13:
 * - Issue #212: Finalized accuracy-aware forensic recovery logic.
 * v8.9.11:
 * - Issue #212: Prioritized accuracy from LogEntry in forensic marker reconstruction.
 * v8.9.10:
 * - Issue 209: Fixed inaccurate SIT marker placement by using historical coordinates from recovered logs.
 */
class RemoteHandler(
    private val context: Context,
    private val repository: MainRepository,
    private val locationProcessor: LocationProcessor,
    private val alarmManager: AppAlarmManager,
    private val sessionManager: SessionManager,
    private val forensicUseCase: ServiceForensicUseCase,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope,
    private val onPulse: (String) -> Unit
) {
    var isTrackerConnected = false
    var lastPeerActivityTs = 0L 
    var peerSignal = 0
    var lastPeerGpsTs = 0L
    private var lastRemotePacketTs = 0L

    // Tracker State
    var trackerLat = 0.0
    var trackerLng = 0.0
    var trackerSpeed = 0f
    var trackerBearing = 0f 
    var trackerAccuracy = 0f
    var trackerMaxAccuracy = 0f
    var trackerLastGpsTs = 0L
    var trackerLastValidFixRealtime = 0L 
    var trackerBattery = 0
    var trackerTemp = 0f
    var trackerMaxTemp = 0f
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
    var trackerVerticalVelocity = 0f
    var isTrackerClockRegression = false
    var isTrackerLocationPending = false
    var trackerGnssDetail: GnssDetail? = null
    var trackerSnrIdx = 0f
    var isTrackerBatterySteepDischarge = false 
    var isTrackerCoolingModeActive = false 
    
    var isTrackerPowerSaveMode = false
    var trackerStandbyBucket = -1
    var trackerNetInterface = "UNKNOWN"
    var isTrackerStorageLow = false
    var isTrackerStorageCritical = false

    var trackerSitVz = 0f 
    var trackerSitDz = 0f 
    var trackerSitBaro = 0f 
    var trackerSitTilt = 0f 
    var trackerSitShock = 0f

    var trackerDistToHome: Double? = null
    var trackerDistToViewer: Double? = null

    var trackerVibration = 0f
    var trackerHeading = 0f
    var trackerBaroAlt = 0f
    var trackerLux = 0f
    var isTrackerNear = true
    var trackerTiltDegrees = 0f
    var trackerAcousticDb = 0.0
    var trackerPeakVibrationShock = 0f
    var trackerPeakVibrationShockTs = 0L
    var trackerLuxBaseline = 0f
    var trackerAcousticFloorDb = 0.0
    var trackerAdaptiveVibrationFloor = 0.12f
    var trackerProxIdx = 1.0f

    var trackerUptimeMs = 0L
    var trackerTotalDropMs = 0L
    var trackerMaxDropMs = 0L
    var trackerMaxDropTs = 0L
    var trackerTotalConnectedMs = 0L
    var trackerSessionConnectedMs = 0L
    var trackerLastConnTs = 0L
    var trackerLastDiscTs = 0L
    
    var trackerGpsStallStartTs = 0L 

    init {
        scope.launch {
            trackerLuxBaseline = repository.getFloat(MainRepository.TRACKER_LUX_BASELINE_KEY, 0f)
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
                trackerProxIdx = s.proxIdx; trackerUptimeMs = s.uptimeMs; trackerTotalDropMs = s.totalDropMs
                trackerMaxDropMs = s.maxDropMs; trackerMaxDropTs = s.maxDropTs
                trackerTotalConnectedMs = s.totalConnectedMs
                trackerSessionConnectedMs = s.sessionConnectedMs; trackerLastConnTs = s.lastConnTs
                trackerLastDiscTs = s.lastDiscTs
                isTrackerLocationPending = s.isLocationPending
                trackerGnssDetail = s.gnssDetail
                trackerSnrIdx = s.snrIdx
                isTrackerBatterySteepDischarge = s.isBatterySteepDischarge
                isTrackerCoolingModeActive = s.isCoolingModeActive
                
                isTrackerPowerSaveMode = s.isPowerSaveMode
                trackerStandbyBucket = s.standbyBucket
                trackerNetInterface = s.netInterface
                isTrackerStorageLow = s.isStorageLow
                isTrackerStorageCritical = s.isStorageCritical
                
                trackerLastValidFixRealtime = timeProvider.elapsedRealtime()

                repository.updateLocation(LocationUpdate(
                    lat = trackerLat, lng = trackerLng, speed = trackerSpeed, accuracy = trackerAccuracy, bearing = trackerBearing,
                    battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp, isCharging = isTrackerCharging, currentMa = trackerCurrentMa,
                    gpsTs = trackerLastGpsTs, isMe = false, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                    isJump = false, isJammer = false, isStalled = false,
                    maxAccuracy = trackerMaxAccuracy, signal = 0,
                    vibration = trackerVibration, heading = trackerHeading, baroAlt = trackerBaroAlt,
                    lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb,
                    peakVibrationShock = trackerPeakVibrationShock, peakVibrationShockTs = trackerPeakVibrationShockTs,
                    luxBaseline = trackerLuxBaseline, acousticFloorDb = trackerAcousticFloorDb,
                    adaptiveVibrationFloor = trackerAdaptiveVibrationFloor, isSuspicious = isTrackerSuspicious,
                    isTamperDetected = isTrackerTamperDetected, isPowerTamper = isTrackerPowerTamper,
                    isSitDetected = isTrackerSitDetected, isSitActive = isTrackerSitActive, lastSitTs = trackerLastSitTs,
                    verticalVelocity = trackerVerticalVelocity, sitVz = trackerSitVz, sitDz = trackerSitDz,
                    sitBaro = trackerSitBaro, sitTilt = trackerSitTilt, sitShock = trackerSitShock,
                    proxIdx = trackerProxIdx, uptimeMs = trackerUptimeMs, totalDropMs = trackerTotalDropMs,
                    maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                    totalConnectedMs = trackerTotalConnectedMs,
                    sessionConnectedMs = trackerSessionConnectedMs, lastConnTs = trackerLastConnTs,
                    lastDiscTs = trackerLastDiscTs,
                    violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
                    isLocationPending = isTrackerLocationPending,
                    isPowerSaveMode = isTrackerPowerSaveMode,
                    standbyBucket = trackerStandbyBucket,
                    netInterface = trackerNetInterface,
                    isStorageLow = isTrackerStorageLow,
                    isStorageCritical = isTrackerStorageCritical,
                    gnssDetail = trackerGnssDetail,
                    snrIdx = trackerSnrIdx,
                    isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                    isCoolingModeActive = isTrackerCoolingModeActive
                ))
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
        trackerLat = 0.0; trackerLng = 0.0; trackerSpeed = 0f; trackerBearing = 0f; trackerAccuracy = 0f
        trackerMaxAccuracy = 0f; trackerLastGpsTs = 0L; trackerBattery = 0; trackerTemp = 0f
        trackerMaxTemp = 0f; trackerCurrentMa = 0; trackerSatsView = 0; trackerSatsUsed = 0
        isTrackerCharging = false; isTrackerJammerSuspicion = false; isTrackerVisualJump = false
        isTrackerTrajectoryPromoted = false; trackerJumpTier = 0; isTrackerSuspicious = false
        isTrackerTamperDetected = false; isTrackerPowerTamper = false
        isTrackerSitDetected = false; isTrackerSitActive = false; trackerLastSitTs = 0L; trackerVerticalVelocity = 0f
        trackerSitVz = 0f; trackerSitDz = 0f; trackerSitBaro = 0f; trackerSitTilt = 0f; trackerSitShock = 0f
        trackerDistToHome = null; trackerDistToViewer = null
        trackerVibration = 0f; trackerHeading = 0f; trackerBaroAlt = 0f; trackerLux = 0f
        isTrackerNear = true; trackerTiltDegrees = 0f; trackerAcousticDb = 0.0
        trackerPeakVibrationShock = 0f; trackerPeakVibrationShockTs = 0L
        trackerLuxBaseline = 0f; trackerAcousticFloorDb = 0.0
        trackerAdaptiveVibrationFloor = 0.12f; trackerProxIdx = 1.0f
        trackerUptimeMs = 0L; trackerTotalDropMs = 0L; trackerMaxDropMs = 0L
        trackerMaxDropTs = 0L
        trackerTotalConnectedMs = 0L; trackerSessionConnectedMs = 0L
        trackerLastConnTs = 0L; trackerLastDiscTs = 0L
        trackerGpsStallStartTs = 0L
        trackerLastValidFixRealtime = 0L
        isTrackerClockRegression = false
        isTrackerLocationPending = false
        trackerGnssDetail = null
        trackerSnrIdx = 0f
        isTrackerBatterySteepDischarge = false
        isTrackerCoolingModeActive = false
        
        isTrackerPowerSaveMode = false
        trackerStandbyBucket = -1
        trackerNetInterface = "UNKNOWN"
        isTrackerStorageLow = false
        isTrackerStorageCritical = false
        
        repository.saveFloatSync(MainRepository.TRACKER_LUX_BASELINE_KEY, 0f)
        repository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
    }

    /**
     * Issue 194: Reconstructs forensic state from incoming remote logs.
     * Ensures that recovered "Sit Detected" events trigger the same map markers as real-time flags.
     * Modified in v8.9.10: Issue 209: Using historical coordinates from LogEntry for accurate recovery.
     * Modified in v8.9.13: Issue #212: Finalized accuracy prioritization from LogEntry.
     */
    fun handleRemoteLog(entry: LogEntry) {
        if (entry.message.contains("Sit Detected", ignoreCase = true)) {
            // Rising-edge detection for log-based SIT events to ensure marker placement
            if (!isTrackerSitDetected) {
                isTrackerSitDetected = true
                
                // Issue 209: Immediate forensic marker placement using historical coordinates
                val effectiveLat = if (entry.lat != 0.0) entry.lat else trackerLat
                val effectiveLng = if (entry.lng != 0.0) entry.lng else trackerLng
                
                // Issue #212: Use log-specific accuracy if available
                val effectiveAccuracy = if (entry.accuracy > 0f) entry.accuracy.toDouble() else trackerMaxAccuracy.toDouble()

                if (effectiveLat != 0.0 && effectiveLng != 0.0) {
                    forensicUseCase.recordViolationMarkers(
                        now = entry.timestamp,
                        lat = effectiveLat,
                        lng = effectiveLng,
                        accuracy = effectiveAccuracy,
                        activeViolations = setOf(ALERT_ID_TRACKER_CHAIR),
                        unresolvedAlarms = emptySet()
                    )
                }
            }
        }
    }

    fun handleRemoteUpdate(data: JSONObject, isTrackerMode: Boolean) {
        val fromId = data.optString("id")
        val fromViewer = data.optBoolean("from_viewer", false) 
        val type = data.optString("type", "")
        val now = timeProvider.currentTimeMillis()
        val nowRealtime = timeProvider.elapsedRealtime()

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

            onPulse(fromId)
            lastPeerActivityTs = nowRealtime
            return
        }

        if (isTrackerMode && fromViewer && data.has("home_points")) {
            scope.launch {
                try {
                    val array = data.getJSONArray("home_points")
                    val newList = mutableListOf<GeoPoint>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        newList.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
                    }
                    val maxDist = data.optDouble("max_dist", -1.0)
                    val ts = data.optLong("settings_ts", 0L)
                    
                    repository.saveHomePoints(newList, if (maxDist > 0) maxDist else null, if (ts > 0) ts else null)
                    onPulse(fromId)
                    lastPeerActivityTs = nowRealtime
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing remote settings")
                }
            }
            return
        }

        if (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity") {
            if (isTrackerMode && fromViewer) {
                onPulse(fromId)
                lastPeerActivityTs = nowRealtime
            } else if (!isTrackerMode && !fromViewer) {
                onPulse(fromId)
                lastPeerActivityTs = nowRealtime
                isTrackerConnected = true
            }
            return
        }

        if (isTrackerMode && fromViewer) {
            onPulse(fromId)
            lastPeerActivityTs = nowRealtime
            return
        }

        if (!isTrackerMode && !fromViewer) {
            val remoteTs = data.optLong("ts", 0L)
            if (remoteTs > 0 && remoteTs < lastRemotePacketTs) {
                return
            }
            if (remoteTs > 0) lastRemotePacketTs = remoteTs

            onPulse(fromId)
            lastPeerActivityTs = nowRealtime
            isTrackerConnected = true
            
            peerSignal = data.optInt("signal", 0)
            
            val hasIncomingGps = data.has("lat") || data.has("gps_ts") || data.has("gps_age_ms")
            val prevGpsTs = trackerLastGpsTs
            
            isTrackerSuspicious = data.optBoolean("is_suspicious", isTrackerSuspicious)
            isTrackerTamperDetected = data.optBoolean("is_tamper_detected", isTrackerTamperDetected)
            isTrackerPowerTamper = data.optBoolean("is_power_tamper", isTrackerPowerTamper)
            
            // Issue 194: Rising-edge detection for latched SIT events in real-time telemetry
            val incomingSitDetected = data.optBoolean("is_sit_detected", false)
            if (incomingSitDetected && !isTrackerSitDetected) {
                 repository.addLog(LogEntry(
                    timestamp = now,
                    message = "Tracker: Sit Detected (Remote)",
                    type = "event",
                    isImportant = true,
                    isSpecial = true,
                    specialColor = -0x10000
                ))
            }
            isTrackerSitDetected = incomingSitDetected

            isTrackerSitActive = data.optBoolean("is_sit_active", isTrackerSitActive)
            trackerLastSitTs = data.optLong("last_sit_ts", trackerLastSitTs)
            trackerVerticalVelocity = data.optDouble("vertical_velocity", trackerVerticalVelocity.toDouble()).toFloat()
            isTrackerLocationPending = data.optBoolean("is_location_pending", false)
            trackerSnrIdx = data.optDouble("snr_idx", trackerSnrIdx.toDouble()).toFloat()
            isTrackerBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false)
            isTrackerCoolingModeActive = data.optBoolean("is_cooling_mode_active", false)
            
            isTrackerPowerSaveMode = data.optBoolean("is_power_save_mode", isTrackerPowerSaveMode)
            trackerStandbyBucket = data.optInt("standby_bucket", trackerStandbyBucket)
            trackerNetInterface = data.optString("net_interface", trackerNetInterface)
            isTrackerStorageLow = data.optBoolean("is_storage_low", isTrackerStorageLow)
            isTrackerStorageCritical = data.optBoolean("is_storage_critical", isTrackerStorageCritical)

            if (data.has("gnss_detail")) {
                try {
                    val array = data.getJSONArray("gnss_detail")
                    val satList = mutableListOf<SatelliteInfo>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        satList.add(SatelliteInfo(
                            svid = obj.getInt("svid"),
                            cn0 = obj.optDouble("cn0", 0.0).toFloat(),
                            usedInFix = obj.getBoolean("used_in_fix"),
                            constellation = obj.optInt("constellation", 0)
                        ))
                    }
                    trackerGnssDetail = GnssDetail(satellites = satList)
                } catch (e: Exception) {
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
                
                val rawSpeed = data.optDouble("speed", -1.0)
                val rawBearing = data.optDouble("bearing", 0.0).toFloat()
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
                    androidSpeedKph = if (rawSpeed >= 0.0) rawSpeed * 3.6 else 0.0,
                    gpsTs = candidateTs,
                    accuracy = if (rawAccuracy > 0.0) rawAccuracy.toFloat() else 0f,
                    bearing = rawBearing,
                    snr = 0f,
                    satsUsed = data.optInt("sats_used", trackerSatsUsed),
                    isViewerTrail = false,
                    lastGpsTs = prevGpsTs,
                    providedMaxAccuracy = rawMaxAcc.toFloat(),
                    providedIsJump = isJumpPacket,
                    providedIsTrajectoryPromoted = isTrajectoryPacket,
                    providedJumpTier = jumpTierPacket,
                    providedIsJammer = isJammerPacket,
                    providedIsStalled = isStalledPacket,
                    providedIsTamper = isTrackerTamperDetected || isTrackerLocationPending,
                    providedAcousticFloorDb = trackerAcousticFloorDb,
                    isSuspicious = isTrackerSuspicious,
                    nowWall = now,
                    nowRealtime = nowRealtime
                )

                isTrackerClockRegression = processed.isClockRegression

                if (!processed.isClockRegression) {
                    if (rawLat != 0.0 && rawLng != 0.0) {
                        trackerLat = rawLat
                        trackerLng = rawLng
                        trackerLastGpsTs = candidateTs
                        lastPeerGpsTs = trackerLastGpsTs
                        if (!isStalledPacket) trackerLastValidFixRealtime = nowRealtime
                    }
                    
                    trackerSpeed = (processed.filteredSpeed / 3.6).toFloat()
                    trackerBearing = rawBearing
                    
                    if (rawAccuracy > 0.0) trackerAccuracy = rawAccuracy.toFloat()
                    if (rawMaxAcc > 0.0) trackerMaxAccuracy = rawMaxAcc.toFloat()
                    
                    trackerSatsView = data.optInt("sats_view", trackerSatsView)
                    trackerSatsUsed = data.optInt("sats_used", trackerSatsUsed)
                    isTrackerJammerSuspicion = isJammerPacket
                    isTrackerVisualJump = isJumpPacket
                    isTrackerTrajectoryPromoted = isTrajectoryPacket
                    trackerJumpTier = jumpTierPacket
                }
            }
            
            trackerBattery = data.optInt("battery", trackerBattery)
            trackerTemp = data.optDouble("temp", trackerTemp.toDouble()).toFloat()
            trackerMaxTemp = data.optDouble("max_temp", trackerMaxTemp.toDouble()).toFloat()
            trackerCurrentMa = data.optInt("current_ma", trackerCurrentMa)
            isTrackerCharging = data.optBoolean("is_charging", isTrackerCharging)
            
            trackerSitVz = data.optDouble("sit_vz", trackerSitVz.toDouble()).toFloat()
            trackerSitDz = data.optDouble("sit_dz", trackerSitDz.toDouble()).toFloat()
            trackerSitBaro = data.optDouble("sit_baro", trackerSitBaro.toDouble()).toFloat()
            trackerSitTilt = data.optDouble("sit_tilt", trackerSitTilt.toDouble()).toFloat()
            trackerSitShock = data.optDouble("sit_shock", trackerSitShock.toDouble()).toFloat()
            
            trackerVibration = data.optDouble("vibration", trackerVibration.toDouble()).toFloat()
            trackerHeading = data.optDouble("heading", trackerHeading.toDouble()).toFloat()
            trackerBaroAlt = data.optDouble("baro_alt", trackerBaroAlt.toDouble()).toFloat()
            trackerLux = data.optDouble("lux", trackerLux.toDouble()).toFloat()
            isTrackerNear = data.optBoolean("is_near", isTrackerNear)
            trackerProxIdx = data.optDouble("prox_idx", trackerProxIdx.toDouble()).toFloat()
            trackerTiltDegrees = data.optDouble("tilt_degrees", trackerTiltDegrees.toDouble()).toFloat()
            trackerAcousticDb = data.optDouble("acoustic_db", trackerAcousticDb)
            trackerPeakVibrationShock = data.optDouble("peak_vibration_shock", trackerPeakVibrationShock.toDouble()).toFloat()
            trackerPeakVibrationShockTs = data.optLong("peak_shock_ts", trackerPeakVibrationShockTs)
            
            val newLuxBaseline = data.optDouble("lux_baseline", trackerLuxBaseline.toDouble()).toFloat()
            if (newLuxBaseline != trackerLuxBaseline) {
                trackerLuxBaseline = newLuxBaseline
                repository.saveFloatSync(MainRepository.TRACKER_LUX_BASELINE_KEY, trackerLuxBaseline)
            }
            
            val newAcousticFloor = data.optDouble("acoustic_floor_db", trackerAcousticFloorDb)
            if (newAcousticFloor != trackerAcousticFloorDb) {
                trackerAcousticFloorDb = newAcousticFloor
                repository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, trackerAcousticFloorDb)
            }

            trackerAdaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", trackerAdaptiveVibrationFloor.toDouble()).toFloat()
            
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
                nowRealtime = nowRealtime,
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
            val violationPercentage = data.optDouble("violation_percentage", 0.0).toFloat()

            val isStalled = data.optBoolean("is_stalled", false)
            if (isStalled && trackerGpsStallStartTs == 0L) trackerGpsStallStartTs = nowRealtime
            else if (!isStalled) trackerGpsStallStartTs = 0L

            scope.launch {
                repository.updateLocation(LocationUpdate(
                    lat = trackerLat, lng = trackerLng, speed = trackerSpeed, accuracy = trackerAccuracy, bearing = trackerBearing,
                    battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp, isCharging = isTrackerCharging, currentMa = trackerCurrentMa,
                    gpsTs = trackerLastGpsTs, isMe = false, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                    isJump = isTrackerVisualJump, isTrajectoryPromoted = isTrackerTrajectoryPromoted, jumpTier = trackerJumpTier, 
                    isJammer = isTrackerJammerSuspicion, isStalled = isStalled,
                    maxAccuracy = trackerMaxAccuracy, signal = peerSignal,
                    vibration = trackerVibration, heading = trackerHeading, baroAlt = trackerBaroAlt,
                    lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb,
                    peakVibrationShock = trackerPeakVibrationShock, peakVibrationShockTs = trackerPeakVibrationShockTs,
                    luxBaseline = trackerLuxBaseline, acousticFloorDb = trackerAcousticFloorDb,
                    adaptiveVibrationFloor = trackerAdaptiveVibrationFloor, isSuspicious = isTrackerSuspicious,
                    isTamperDetected = isTrackerTamperDetected,
                    isPowerTamper = isTrackerPowerTamper,
                    isSitDetected = isTrackerSitDetected,
                    isSitActive = isTrackerSitActive,
                    lastSitTs = trackerLastSitTs,
                    verticalVelocity = trackerVerticalVelocity,
                    sitVz = trackerSitVz,
                    sitDz = trackerSitDz,
                    sitBaro = trackerSitBaro,
                    sitTilt = trackerSitTilt,
                    sitShock = trackerSitShock,
                    proxIdx = trackerProxIdx,
                    uptimeMs = trackerUptimeMs, totalDropMs = trackerTotalDropMs, maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                    totalConnectedMs = trackerTotalConnectedMs,
                    sessionConnectedMs = trackerSessionConnectedMs,
                    lastConnTs = trackerLastConnTs, lastDiscTs = trackerLastDiscTs,
                    violationUptimeMs = violationUptimeMs,
                    violationPercentage = violationPercentage,
                    isClockRegression = isTrackerClockRegression,
                    isLocationPending = isTrackerLocationPending,
                    isPowerSaveMode = isTrackerPowerSaveMode,
                    standbyBucket = trackerStandbyBucket,
                    netInterface = trackerNetInterface,
                    isStorageLow = isTrackerStorageLow,
                    isStorageCritical = isTrackerStorageCritical,
                    gnssDetail = trackerGnssDetail,
                    snrIdx = trackerSnrIdx,
                    isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                    isCoolingModeActive = isTrackerCoolingModeActive
                ))
                
                repository.saveTrackerState(TrackerStatus(
                    lat = trackerLat, lng = trackerLng, speed = trackerSpeed, bearing = trackerBearing, accuracy = trackerAccuracy,
                    gpsTs = trackerLastGpsTs, ts = now, battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp,
                    isCharging = isTrackerCharging, currentMa = trackerCurrentMa, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                    lastConnTs = trackerLastConnTs, lastDiscTs = trackerLastDiscTs, uptimeMs = trackerUptimeMs,
                    totalConnectedMs = trackerTotalConnectedMs,
                    sessionConnectedMs = trackerSessionConnectedMs, totalDropMs = trackerTotalDropMs,
                    maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                    violationUptimeMs = violationUptimeMs, violationPercentage = violationPercentage,
                    isSitDetected = isTrackerSitDetected, isSitActive = isTrackerSitActive, lastSitTs = trackerLastSitTs, verticalVelocity = trackerVerticalVelocity,
                    sitVz = trackerSitVz, sitDz = trackerSitDz, sitBaro = trackerSitBaro, sitTilt = trackerSitTilt, sitShock = trackerSitShock,
                    isPowerTamper = isTrackerPowerTamper, vibration = trackerVibration, heading = trackerHeading,
                    baroAlt = trackerBaroAlt, lux = trackerLux, isNear = isTrackerNear, tiltDegrees = trackerTiltDegrees,
                    acousticDb = trackerAcousticDb, peakVibrationShock = trackerPeakVibrationShock,
                    peakVibrationShockTs = trackerPeakVibrationShockTs, luxBaseline = trackerLuxBaseline,
                    acousticFloorDb = trackerAcousticFloorDb, adaptiveVibrationFloor = trackerAdaptiveVibrationFloor,
                    proxIdx = trackerProxIdx, isSuspicious = isTrackerSuspicious, isTamperDetected = isTrackerTamperDetected,
                    isTrajectoryPromoted = isTrackerTrajectoryPromoted, jumpTier = trackerJumpTier,
                    isLocationPending = isTrackerLocationPending,
                    isPowerSaveMode = isTrackerPowerSaveMode,
                    standbyBucket = trackerStandbyBucket,
                    netInterface = trackerNetInterface,
                    isStorageLow = isTrackerStorageLow,
                    isStorageCritical = isTrackerStorageCritical,
                    gnssDetail = trackerGnssDetail,
                    snrIdx = trackerSnrIdx,
                    isBatterySteepDischarge = isTrackerBatterySteepDischarge,
                    isCoolingModeActive = isTrackerCoolingModeActive
                ))
            }
        }
    }
}
