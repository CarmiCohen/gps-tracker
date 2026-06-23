package com.gps19.app

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * SyncManager: Handles the telemetry synchronization loop.
 * v8.9.33:
 * - Issue #271: Unified sync loop to use PING_INTERVAL_MS (10s) as per code-first standardization. (Formerly #1)
 * v8.9.32:
 * - Issue #244: Propagating locationPendingReason in flushPendingUpdates and offline storage.
 * v8.9.26:
 * - Issue #272: Synchronized version string to v8.9.26 baseline. (Formerly #2)
 * v8.9.21:
 * - Issue #224: Added tilt_idx and baro_idx to telemetry payloads for forensic expansion.
 * v8.9.18:
 * - Issue #221: Propagated lastValidFixRealtime for Bayesian uncertainty scaling.
 */
class SyncManager(
    private val context: Context,
    private val networkManager: AppNetworkManager,
    private val sessionManager: SessionManager,
    private val gpsManager: GpsManager,
    private val sensorManager: AppSensorManager?,
    private val locationProcessor: LocationProcessor,
    private val telemetryRepository: TelemetryRepository,
    private val offlineRepository: OfflineRepository,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider,
    private val repository: MainRepository,
    private val scope: LifecycleCoroutineScope
) {
    private var syncJob: Job? = null
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var onSyncStarted: (() -> Unit)? = null
    private var onSyncFinished: (() -> Unit)? = null

    fun setOnSyncStartedListener(listener: () -> Unit) { onSyncStarted = listener }
    fun setOnSyncFinishedListener(listener: () -> Unit) { onSyncFinished = listener }

    /**
     * startSyncLoop: Implements the 10s PING_INTERVAL_MS loop.
     */
    fun startSyncLoop(deviceId: String, viewerId: String, isTracker: Boolean) {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (networkManager.isConnected()) {
                    _isSyncing.value = true
                    onSyncStarted?.invoke()
                    
                    try {
                        flushPendingUpdates(deviceId, viewerId, isTracker)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to sync telemetry")
                    } finally {
                        _isSyncing.value = false
                        onSyncFinished?.invoke()
                    }
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    suspend fun pushCurrentStatus(
        deviceId: String, viewerId: String, isTrackerMode: Boolean, loc: android.location.Location?, filtered: EngineGeoPoint?,
        distToTracker: Double?, distToHome: Double?, maxAccuracy: Float, filteredSpeed: Double,
        vibration: Float, heading: Float, baroAlt: Float, lux: Float, isNear: Boolean, isSuspicious: Boolean,
        tiltDegrees: Float, acousticDb: Double, isJump: Boolean, isTrajectoryPromoted: Boolean, jumpTier: Int,
        isJammer: Boolean, isStalledRaw: Boolean, isStalledActive: Boolean, peakShock: Float, peakShockTs: Long,
        luxBaseline: Float, acousticFloorDb: Double, adaptiveVibrationFloor: Float, proxIdx: Float, proximityCm: Float,
        micPending: Boolean, isTamperDetected: Boolean, isPowerTamper: Boolean, isSitDetected: Boolean, isSitActive: Boolean,
        lastSitTs: Long, receiptRealtime: Long, violationUptimeMs: Long, violationPercentage: Float,
        verticalVelocity: Float, sitVz: Float, sitDz: Float, sitBaro: Float, sitTilt: Float, sitShock: Float,
        isClockRegression: Boolean, isLocationPending: Boolean, locationPendingReason: LocationPendingReason,
        lastValidFixRealtime: Long, gnssDetail: GnssDetail?, snrIdx: Float, tiltIdx: Float, baroIdx: Float,
        isBatterySteepDischarge: Boolean, isCoolingModeActive: Boolean,
        batteryLevel: Int, batteryTemp: Float, isCharging: Boolean
    ) {
        val status = TrackerStatus(
            deviceId = deviceId,
            viewerId = viewerId,
            ts = timeProvider.currentTimeMillis(),
            lat = filtered?.lat ?: loc?.latitude ?: 0.0,
            lng = filtered?.lng ?: loc?.longitude ?: 0.0,
            alt = loc?.altitude ?: 0.0,
            accuracy = loc?.accuracy ?: 0f,
            maxAccuracy = maxAccuracy,
            speed = filteredSpeed.toFloat(),
            bearing = loc?.bearing ?: 0f,
            vibration = vibration,
            heading = heading,
            baroAlt = baroAlt,
            lux = lux,
            isNear = isNear,
            isSuspicious = isSuspicious,
            tiltDegrees = tiltDegrees,
            acousticDb = acousticDb,
            isJump = isJump,
            isTrajectoryPromoted = isTrajectoryPromoted,
            jumpTier = jumpTier,
            isJammer = isJammer,
            isStalled = isStalledActive,
            peakVibrationShock = peakShock,
            peakVibrationShockTs = peakShockTs,
            luxBaseline = luxBaseline,
            acousticFloorDb = acousticFloorDb,
            adaptiveVibrationFloor = adaptiveVibrationFloor,
            proxIdx = proxIdx,
            proximityCm = proximityCm,
            isTamperDetected = isTamperDetected,
            isPowerTamper = isPowerTamper,
            isSitDetected = isSitDetected,
            isSitActive = isSitActive,
            lastSitTs = lastSitTs,
            violationUptimeMs = violationUptimeMs,
            violationPercentage = violationPercentage,
            verticalVelocity = verticalVelocity,
            sitVz = sitVz,
            sitDz = sitDz,
            sitBaro = sitBaro,
            sitTilt = sitTilt,
            sitShock = sitShock,
            isClockRegression = isClockRegression,
            isLocationPending = isLocationPending,
            locationPendingReason = locationPendingReason,
            lastValidFixRealtime = lastValidFixRealtime,
            snrIdx = snrIdx,
            tiltIdx = tiltIdx,
            baroIdx = baroIdx,
            isBatterySteepDischarge = isBatterySteepDischarge,
            isCoolingModeActive = isCoolingModeActive,
            gnssDetail = gnssDetail,
            battery = batteryLevel,
            temp = batteryTemp,
            isCharging = isCharging
        )

        networkManager.sendTelemetry(status)
        
        if (isTrackerMode) {
            repository.saveTrackerState(status)
        }
    }

    private suspend fun flushPendingUpdates(deviceId: String, viewerId: String, isTracker: Boolean) {
        val pending = offlineRepository.getPendingStatusUpdates(100)
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            val status = TrackerStatus(
                deviceId = deviceId,
                viewerId = viewerId,
                ts = entity.timestamp,
                lat = entity.lat,
                lng = entity.lng,
                alt = 0.0, // Alt not in entity
                accuracy = entity.accuracy,
                maxAccuracy = entity.maxAccuracy,
                speed = entity.speed,
                bearing = entity.bearing,
                // These are missing from entity, using defaults or basic mapping
                vibration = 0f, 
                heading = 0f,
                baroAlt = 0f,
                lux = 0f,
                isNear = true,
                isSuspicious = false,
                tiltDegrees = 0f,
                acousticDb = 0.0,
                isJump = false,
                isTrajectoryPromoted = false,
                jumpTier = 0,
                isJammer = false,
                isStalled = false,
                peakVibrationShock = 0f,
                peakVibrationShockTs = 0L,
                isTamperDetected = false,
                isPowerTamper = false,
                isSitDetected = entity.isSitDetected,
                isSitActive = entity.isSitActive,
                lastSitTs = 0L,
                verticalVelocity = entity.verticalVelocity,
                sitVz = entity.sitVz,
                sitDz = entity.sitDz,
                sitBaro = entity.sitBaro,
                sitTilt = entity.sitTilt,
                sitShock = entity.sitShock,
                isLocationPending = false,
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE },
                lastValidFixRealtime = entity.lastValidFixRealtime,
                snrIdx = entity.snrIdx,
                tiltIdx = entity.tiltIdx,
                baroIdx = entity.baroIdx,
                isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive,
                battery = entity.battery,
                temp = entity.temp,
                isCharging = entity.isCharging
            )

            if (networkManager.sendTelemetry(status)) {
                offlineRepository.deletePendingStatusUpdate(entity.id)
            }
        }
    }

    private fun isGpsStalling(loc: android.location.Location?, lastValidFixRealtime: Long): Boolean {
        if (lastValidFixRealtime == 0L) return false
        return (timeProvider.elapsedRealtime() - lastValidFixRealtime) > GPS_STALL_THRESHOLD_MS
    }

    fun stopSyncLoop() {
        syncJob?.cancel()
    }
}
