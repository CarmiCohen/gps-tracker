package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * SyncManager: Handles broadcasting state updates and relay synchronization.
 * v8.9.3:
 * - Issue 188: Preserved historical gps_ts during offline flushing.
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 * - Issue 134: Removed dead code broadcastIntegrityUpdate and pushStatusUpdateOnly.
 * - Forensic Simplification: Removed legacy 'ver' and 'vid' from internal models; 
 *   version is now injected at the emission layer via BuildConfig.VERSION_NAME.
 */
class SyncManager(
    private val context: Context,
    private val networkManager: AppNetworkManager,
    private val sessionManager: SessionManager,
    private val gpsManager: GpsManager?,
    private val sensorManager: AppSensorManager?,
    private val locationProcessor: LocationProcessor,
    private val telemetryRepository: TelemetryRepository,
    private val offlineRepository: OfflineRepository,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var onSyncStarted: (() -> Unit)? = null

    private fun safeDouble(value: Double): Double = if (java.lang.Double.isNaN(value) || java.lang.Double.isInfinite(value)) 0.0 else value
    private fun safeFloat(value: Float): Float = if (java.lang.Float.isNaN(value) || java.lang.Float.isInfinite(value)) 0f else value

    fun setOnSyncStartedListener(listener: () -> Unit) {
        this.onSyncStarted = listener
    }

    fun startSyncLoop(deviceId: String, viewerId: String, isTracker: Boolean) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                if (networkManager.isConnected() && deviceId.isNotEmpty()) {
                    val pingJson = JSONObject().apply {
                        put("ts", timeProvider.currentTimeMillis())
                        put("id", deviceId)
                        put("viewer_id", viewerId)
                        put("from", if (isTracker) "tracker" else "viewer")
                        put("role", if (isTracker) "tracker" else "viewer")
                        put("ver", BuildConfig.VERSION_NAME)
                    }
                    networkManager.emit("ping_cmd", pingJson)

                    if (isTracker) {
                        flushPendingUpdates(deviceId, viewerId)
                    }
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun flushPendingUpdates(deviceId: String, viewerId: String) {
        if (!networkManager.isConnected() || deviceId.isEmpty()) return
        
        scope.launch {
            try {
                onSyncStarted?.invoke() // Notify for muzzling (Issue 99)
                
                val pending = offlineRepository.getPendingStatusUpdates(50)
                if (pending.isEmpty()) return@launch
                
                logManager.logServiceEvent("Sync: Flushing ${pending.size} offline status updates...")
                
                val idsToDelete = mutableListOf<Long>()
                pending.forEach { entity ->
                    val data = JSONObject().apply {
                        put("id", deviceId)
                        put("viewer_id", viewerId)
                        put("from_viewer", false)
                        put("role", "tracker")
                        put("lat", entity.lat)
                        put("lng", entity.lng)
                        put("speed", entity.speed)
                        put("accuracy", entity.accuracy)
                        put("bearing", entity.bearing)
                        put("battery", entity.battery)
                        put("temp", entity.temp)
                        put("is_charging", entity.isCharging)
                        put("ts", entity.timestamp)
                        put("gps_ts", if (entity.gpsTs > 0) entity.gpsTs else entity.timestamp)
                        put("sats_view", entity.satsView)
                        put("sats_used", entity.satsUsed)
                        put("max_accuracy", entity.maxAccuracy)
                        put("snr_idx", safeFloat(entity.snrIdx))
                        put("is_battery_steep_discharge", entity.isBatterySteepDischarge)
                        put("is_cooling_mode_active", entity.isCoolingModeActive)
                        put("is_historical", true)
                        put("is_sit_detected", entity.isSitDetected)
                        put("is_sit_active", entity.isSitActive)
                        put("sit_vz", safeFloat(entity.sitVz))
                        put("sit_dz", safeFloat(entity.sitDz))
                        put("vertical_velocity", safeFloat(entity.verticalVelocity))
                        put("sit_baro", safeFloat(entity.sitBaro))
                        put("sit_tilt", safeFloat(entity.sitTilt))
                        put("sit_shock", safeFloat(entity.sitShock))
                        put("is_storage_low", entity.isStorageLow)
                        put("is_storage_critical", entity.isStorageCritical)
                        put("is_power_save_mode", entity.isPowerSaveMode)
                        put("standby_bucket", entity.standbyBucket)
                        put("net_interface", entity.netInterface)
                        put("ver", BuildConfig.VERSION_NAME)
                    }
                    
                    if (networkManager.isConnected()) {
                        networkManager.emit("location_update", data)
                        idsToDelete.add(entity.id)
                    }
                }
                
                if (idsToDelete.isNotEmpty()) {
                    idsToDelete.forEach { offlineRepository.deletePendingStatusUpdate(it) }
                }
            } catch (e: Exception) {
                logManager.logServiceEvent("Sync: Flush failed: ${e.message}")
            }
        }
    }

    fun stopSyncLoop() { syncJob?.cancel() }

    fun broadcastRttUpdate(isPeerAvailable: Boolean, lastPeerTs: Long, isHardwareOnline: Boolean) {
        telemetryRepository.updateLastRtt(networkManager.getRtt())
        telemetryRepository.updateRemoteActivity(lastPeerTs)
        telemetryRepository.updateIntegrity(telemetryRepository.integrityState.value.copy(isHardwareOnline = isHardwareOnline))
    }

    fun pushCurrentStatus(
        deviceId: String, viewerId: String, isTrackerMode: Boolean, 
        loc: android.location.Location? = null, filtered: EngineGeoPoint? = null, 
        distToTracker: Double? = null, distToHome: Double? = null, 
        maxAccuracy: Float = 0f, filteredSpeed: Double = 0.0,
        vibration: Float = 0f, heading: Float = 0f, baroAlt: Float = 0f,
        lux: Float = 0f, isNear: Boolean = true, isSuspicious: Boolean = false,
        tiltDegrees: Float = 0f, acousticDb: Double = 0.0,
        isJump: Boolean = false, 
        isTrajectoryPromoted: Boolean = false,
        jumpTier: Int = 0,
        isJammer: Boolean = false, isStalledRaw: Boolean = false, isStalledActive: Boolean = false,
        peakShock: Float = 0f, peakShockTs: Long = 0L, luxBaseline: Float = 0f, acousticFloorDb: Double = 0.0, adaptiveVibrationFloor: Float = 0.12f,
        proxIdx: Float = 1.0f, proximityCm: Float = -1.0f, micPending: Boolean = false,
        isTamperDetected: Boolean = false,
        isPowerTamper: Boolean = false,
        isSitDetected: Boolean = false,
        isSitActive: Boolean = false,
        lastSitTs: Long = 0L,
        receiptRealtime: Long = 0L,
        violationUptimeMs: Long = 0L,
        violationPercentage: Float = 0f,
        verticalVelocity: Float = 0f,
        sitVz: Float = 0f,
        sitDz: Float = 0f,
        sitBaro: Float = 0f,
        sitTilt: Float = 0f,
        sitShock: Float = 0f,
        isClockRegression: Boolean = false,
        isLocationPending: Boolean = false,
        gpsTsOverride: Long? = null,
        gnssDetail: GnssDetail? = null,
        snrIdx: Float = 0f,
        isBatterySteepDischarge: Boolean = false,
        isCoolingModeActive: Boolean = false
    ) {
        if (deviceId.isEmpty()) return
        val now = timeProvider.currentTimeMillis()
        val signalIndex = TelemetryUtils.calculateCommIndex(networkManager.getRtt(), DEFAULT_SIGNAL_STRENGTH, DEFAULT_SIGNAL_STRENGTH)
        val integrity = telemetryRepository.integrityState.value
        val effectiveGpsTs = gpsTsOverride ?: loc?.time ?: 0L
        
        val data = JSONObject().apply {
            put("id", deviceId); put("viewer_id", viewerId); put("from_viewer", !isTrackerMode)
            put("role", if (isTrackerMode) "tracker" else "viewer")
            put("ver", BuildConfig.VERSION_NAME)
            
            val lat = safeDouble(filtered?.lat ?: loc?.latitude ?: 0.0)
            val lng = safeDouble(filtered?.lng ?: loc?.longitude ?: 0.0)
            
            if (lat != 0.0 && lng != 0.0) {
                put("lat", lat)
                put("lng", lng)
            }
            
            put("alt", safeDouble(loc?.altitude ?: 0.0))
            put("speed", safeFloat((filteredSpeed / 3.6).toFloat()))
            put("accuracy", safeFloat(loc?.accuracy ?: 0f))
            put("max_accuracy", safeFloat(maxAccuracy))
            put("bearing", safeFloat(loc?.bearing ?: 0f))
            put("vibration", safeFloat(vibration)); put("heading", safeFloat(heading)); put("baro_alt", safeFloat(baroAlt))
            put("lux", safeFloat(lux)); put("is_near", isNear); put("is_suspicious", isSuspicious); put("prox_idx", safeFloat(proxIdx))
            put("proximity_cm", safeFloat(proximityCm))
            put("tilt_degrees", safeFloat(tiltDegrees)); put("acoustic_db", safeDouble(acousticDb))
            put("peak_vibration_shock", safeFloat(peakShock))
            put("peak_shock_ts", peakShockTs)
            put("lux_baseline", safeFloat(luxBaseline))
            put("acoustic_floor_db", safeDouble(acousticFloorDb))
            put("adaptive_vibration_floor", safeFloat(adaptiveVibrationFloor))
            put("dist_to_home", safeDouble(distToHome ?: 0.0))
            put("battery", integrity.batteryLevel); put("temp", integrity.batteryTemp); put("max_temp", integrity.maxTemp); put("is_charging", integrity.isCharging); put("current_ma", integrity.currentMa)
            put("is_tamper_detected", isTamperDetected)
            put("is_power_tamper", isPowerTamper)
            put("is_sit_detected", isSitDetected)
            put("is_sit_active", isSitActive)
            put("last_sit_ts", lastSitTs)
            put("vertical_velocity", safeFloat(verticalVelocity))
            put("is_clock_regression", isClockRegression)
            put("is_trajectory_promoted", isTrajectoryPromoted)
            put("jump_tier", jumpTier)
            put("is_location_pending", isLocationPending)
            put("snr_idx", safeFloat(snrIdx))
            put("is_battery_steep_discharge", isBatterySteepDischarge)
            put("is_cooling_mode_active", isCoolingModeActive)
            
            put("is_power_save_mode", integrity.isPowerSaveMode)
            put("standby_bucket", integrity.standbyBucket)
            put("net_interface", integrity.netInterface)
            put("is_storage_low", integrity.isStorageLow)
            put("is_storage_critical", integrity.isStorageCritical)
            
            put("sit_vz", safeFloat(sitVz))
            put("sit_dz", safeFloat(sitDz))
            put("sit_baro", safeFloat(sitBaro))
            put("sit_tilt", safeFloat(sitTilt))
            put("sit_shock", safeFloat(sitShock))

            gnssDetail?.let { gd ->
                val satsArray = JSONArray()
                gd.satellites.forEach { sat ->
                    satsArray.put(JSONObject().apply {
                        put("svid", sat.svid)
                        put("cn0", safeFloat(sat.cn0))
                        put("used_in_fix", sat.usedInFix)
                        put("constellation", sat.constellation)
                    })
                }
                put("gnss_detail", satsArray) 
            }

            put("ts", now); put("gps_ts", effectiveGpsTs)
            put("mic_pending", micPending)
            
            val age = if (receiptRealtime > 0) {
                timeProvider.elapsedRealtime() - receiptRealtime
            } else {
                if (effectiveGpsTs > 0) now - effectiveGpsTs else 0L
            }
            put("gps_age_ms", if (age < 0) 0L else age)

            put("sats_view", gpsManager?.satellitesInView ?: 0); put("sats_used", gpsManager?.satellitesUsed ?: 0)
            put("signal", signalIndex); put("is_jump", isJump); put("is_jammer", isJammer); put("is_stalled", isStalledRaw)
            
            put("uptime_ms", sessionManager.uptimeMs)
            put("total_connected_ms", sessionManager.totalConnectedMs); put("session_connected_ms", sessionManager.sessionConnectedMs)
            put("total_drop_ms", sessionManager.getTotalDropWithActive(now)); put("max_drop_ms", sessionManager.getMaxDropWithActive(now))
            put("max_drop_ts", sessionManager.getMaxDropTsWithActive(now))
            put("last_conn_ts", sessionManager.lastConnectionTs)
            put("last_disc_ts", sessionManager.lastDisconnectionTs)
            
            put("violation_uptime_ms", sessionManager.violationUptimeMs)
            put("violation_percentage", sessionManager.getViolationPercentage())
        }
        
        if (networkManager.isConnected()) {
            networkManager.emit("location_update", data)
        } else if (isTrackerMode) {
            scope.launch {
                offlineRepository.addPendingStatusUpdate(PendingStatusEntity(
                    lat = data.optDouble("lat", 0.0),
                    lng = data.optDouble("lng", 0.0),
                    speed = data.optDouble("speed", 0.0).toFloat(),
                    accuracy = data.optDouble("accuracy", 0.0).toFloat(),
                    bearing = data.optDouble("bearing", 0.0).toFloat(),
                    battery = integrity.batteryLevel, temp = integrity.batteryTemp,
                    isCharging = integrity.isCharging, timestamp = now,
                    gpsTs = effectiveGpsTs,
                    satsView = gpsManager?.satellitesInView ?: 0, satsUsed = gpsManager?.satellitesUsed ?: 0,
                    maxAccuracy = maxAccuracy, distToTracker = distToTracker, distToHome = distToHome,
                    snrIdx = safeFloat(snrIdx), isBatterySteepDischarge = integrity.isBatterySteepDischarge,
                    isCoolingModeActive = isCoolingModeActive,
                    isSitDetected = isSitDetected, isSitActive = isSitActive,
                    sitVz = sitVz, sitDz = sitDz,
                    sitBaro = sitBaro, sitTilt = sitTilt, sitShock = sitShock,
                    isStorageLow = integrity.isStorageLow, isStorageCritical = integrity.isStorageCritical,
                    isPowerSaveMode = integrity.isPowerSaveMode, standbyBucket = integrity.standbyBucket,
                    netInterface = integrity.netInterface,
                    verticalVelocity = verticalVelocity
                ))
            }
        }
        
        telemetryRepository.updateLocation(LocationUpdate(
            lat = data.optDouble("lat", 0.0), 
            lng = data.optDouble("lng", 0.0),
            speed = data.optDouble("speed", 0.0).toFloat(),
            accuracy = data.optDouble("accuracy", 0.0).toFloat(), 
            bearing = data.optDouble("bearing", 0.0).toFloat(),
            battery = integrity.batteryLevel, temp = integrity.batteryTemp, maxTemp = integrity.maxTemp, isCharging = integrity.isCharging, currentMa = integrity.currentMa,
            gpsTs = effectiveGpsTs, isMe = true, distToTracker = distToTracker, distToHome = distToHome,
            satsView = gpsManager?.satellitesInView ?: 0, satsUsed = gpsManager?.satellitesUsed ?: 0, 
            isJump = isJump, isJammer = isJammer, isStalled = isStalledActive,
            maxAccuracy = maxAccuracy, signal = signalIndex,
            vibration = vibration, heading = heading, baroAlt = baroAlt,
            lux = lux, isNear = isNear, tiltDegrees = tiltDegrees, acousticDb = acousticDb,
            peakVibrationShock = peakShock, peakVibrationShockTs = peakShockTs,
            luxBaseline = luxBaseline, acousticFloorDb = acousticFloorDb,
            adaptiveVibrationFloor = adaptiveVibrationFloor, isSuspicious = isSuspicious,
            isTamperDetected = isTamperDetected, isPowerTamper = integrity.isPowerTamper,
            isSitDetected = isSitDetected, isSitActive = isSitActive, lastSitTs = lastSitTs,
            proxIdx = proxIdx, proximityCm = proximityCm, micPending = micPending,
            uptimeMs = sessionManager.uptimeMs, totalDropMs = sessionManager.getTotalDropWithActive(now), 
            maxDropMs = sessionManager.getMaxDropWithActive(now),
            maxDropTs = sessionManager.getMaxDropTsWithActive(now),
            totalConnectedMs = sessionManager.totalConnectedMs,
            sessionConnectedMs = sessionManager.sessionConnectedMs,
            lastConnTs = sessionManager.lastConnectionTs, 
            lastDiscTs = sessionManager.lastDisconnectionTs,
            violationUptimeMs = sessionManager.violationUptimeMs,
            violationPercentage = sessionManager.getViolationPercentage(),
            verticalVelocity = verticalVelocity,
            sitVz = sitVz, sitDz = sitDz, sitBaro = sitBaro, sitTilt = sitTilt, sitShock = sitShock,
            isClockRegression = isClockRegression,
            isLocationPending = integrity.isLocationPending,
            isPowerSaveMode = integrity.isPowerSaveMode,
            standbyBucket = integrity.standbyBucket,
            netInterface = integrity.netInterface,
            isStorageLow = integrity.isStorageLow,
            isStorageCritical = integrity.isStorageCritical,
            gnssDetail = gnssDetail,
            snrIdx = snrIdx,
            isBatterySteepDischarge = integrity.isBatterySteepDischarge,
            isCoolingModeActive = integrity.isCoolingModeActive
        ))
    }
}
