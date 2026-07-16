package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * SyncManager: Handles the telemetry synchronization loop.
 * v9.5.0: Hilt removed. Manual DI transition.
 */
class SyncManager(
    private val context: Context,
    private val networkManager: AppNetworkManager,
    private val sessionManager: SessionManager,
    private val gpsManager: GpsManager,
    private val locationProcessor: LocationProcessor,
    private val telemetryRepository: TelemetryRepository,
    private val offlineRepository: OfflineRepository,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider,
    private val repository: MainRepository
) {
    private var syncJob: Job? = null
    private var scope: CoroutineScope? = null
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var onSyncStarted: (() -> Unit)? = null
    private var onSyncFinished: (() -> Unit)? = null

    fun setOnSyncStartedListener(listener: () -> Unit) { onSyncStarted = listener }
    fun setOnSyncFinishedListener(listener: () -> Unit) { onSyncFinished = listener }

    /**
     * startSyncLoop: Implements the PING_INTERVAL_MS loop with RTT-aware scaling.
     */
    fun startSyncLoop(scope: CoroutineScope, deviceId: String, viewerId: String, isTracker: Boolean) {
        this.scope = scope
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentRtt = networkManager.getRtt()
                if (networkManager.isConnected()) {
                    _isSyncing.value = true
                    onSyncStarted?.invoke()
                    
                    try {
                        flushPendingUpdates(deviceId, viewerId, isTracker)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Timber.e(e, "Failed to sync telemetry")
                    } finally {
                        _isSyncing.value = false
                        onSyncFinished?.invoke()
                    }
                }

                val dynamicDelay = when {
                    currentRtt > MAX_ALLOWED_RTT_MS -> PING_INTERVAL_MS * 3
                    currentRtt > MAX_ALLOWED_RTT_MS / 2 -> {
                        val scale = 1.0 + (currentRtt.toDouble() / MAX_ALLOWED_RTT_MS)
                        (PING_INTERVAL_MS * scale).toLong()
                    }
                    else -> PING_INTERVAL_MS
                }
                delay(dynamicDelay)
            }
        }
    }

    suspend fun pushCurrentStatus(
        deviceId: String, viewerId: String, isTrackerMode: Boolean, loc: android.location.Location?, filtered: EngineGeoPoint?,
        distToTracker: Double?, distToHome: Double?, maxAccuracy: Double, filteredSpeed: Double,
        vibration: Double, heading: Double, baroAlt: Double, lux: Double, isNear: Boolean,
        tiltDegrees: Double, acousticDb: Double, jumpTier: Int,
        isJammer: Boolean, isStalled: Boolean, peakShock: Double, peakShockTs: Long,
        luxBaseline: Double, acousticFloorDb: Double, adaptiveVibrationFloor: Double, proxIdx: Double, proximityCm: Double,
        proximityDebounceMs: Long, vibrationRollingSum: Double,
        isTamperDetected: Boolean, isPowerTamper: Boolean,
        receiptRealtime: Long, violationUptimeMs: Long, violationPercentage: Double,
        isClockRegression: Boolean, isLocationPending: Boolean, locationPendingReason: LocationPendingReason,
        lastValidFixRealtime: Long, gnssDetail: GnssDetail?,
        isBatterySteepDischarge: Boolean, isCoolingModeActive: Boolean,
        batteryLevel: Int, batteryTemp: Double, isCharging: Boolean,
        trackerState: TrackerState = TrackerState.UNKNOWN,
        status: SentinelStatus = SentinelStatus.VALID
    ) {
        val trackerStatus = TrackerStatus(
            deviceId = deviceId,
            viewerId = viewerId,
            ts = timeProvider.currentTimeMillis(),
            lat = filtered?.lat ?: loc?.latitude ?: 0.0,
            lng = filtered?.lng ?: loc?.longitude ?: 0.0,
            alt = loc?.altitude ?: 0.0,
            accuracy = loc?.accuracy?.toDouble() ?: 0.0,
            maxAccuracy = maxAccuracy,
            speed = filteredSpeed,
            bearing = loc?.bearing?.toDouble() ?: 0.0,
            vibration = vibration,
            heading = heading,
            baroAlt = baroAlt,
            lux = lux,
            isNear = isNear,
            tiltDegrees = tiltDegrees,
            acousticDb = acousticDb,
            jumpTier = jumpTier,
            isJammer = isJammer,
            isStalled = isStalled,
            peakVibrationShock = peakShock,
            peakVibrationShockTs = peakShockTs,
            luxBaseline = luxBaseline,
            acousticFloorDb = acousticFloorDb,
            adaptiveVibrationFloor = adaptiveVibrationFloor,
            proxIdx = proxIdx,
            proximityCm = proximityCm,
            proximityDebounceMs = proximityDebounceMs,
            vibrationRollingSum = vibrationRollingSum,
            isTamperDetected = isTamperDetected,
            isPowerTamper = isPowerTamper,
            violationUptimeMs = violationUptimeMs,
            violationPercentage = violationPercentage,
            status = status,
            isClockRegression = isClockRegression,
            isLocationPending = isLocationPending,
            locationPendingReason = locationPendingReason,
            lastValidFixRealtime = lastValidFixRealtime,
            isBatterySteepDischarge = isBatterySteepDischarge,
            isCoolingModeActive = isCoolingModeActive,
            gnssDetail = gnssDetail,
            battery = batteryLevel,
            temp = batteryTemp,
            isCharging = isCharging,
            trackerState = trackerState
        )

        val success = networkManager.sendTelemetry(trackerStatus)
        
        if (isTrackerMode) {
            repository.saveTrackerState(trackerStatus)
            
            if (!success) {
                offlineRepository.addPendingStatusUpdate(PendingStatusEntity(
                    lat = trackerStatus.lat,
                    lng = trackerStatus.lng,
                    speed = trackerStatus.speed,
                    accuracy = trackerStatus.accuracy,
                    bearing = trackerStatus.bearing,
                    battery = trackerStatus.battery,
                    temp = trackerStatus.temp,
                    isCharging = trackerStatus.isCharging,
                    timestamp = trackerStatus.ts,
                    gpsTs = loc?.time ?: 0L,
                    satsView = trackerStatus.gnssDetail?.satellites?.size ?: 0,
                    satsUsed = trackerStatus.gnssDetail?.satellites?.count { it.usedInFix } ?: 0,
                    maxAccuracy = trackerStatus.maxAccuracy,
                    distToTracker = distToTracker,
                    distToHome = distToHome,
                    isBatterySteepDischarge = trackerStatus.isBatterySteepDischarge,
                    isCoolingModeActive = trackerStatus.isCoolingModeActive,
                    isStorageLow = telemetryRepository.integrityState.value.isStorageLow,
                    isStorageCritical = telemetryRepository.integrityState.value.isStorageCritical,
                    isPowerSaveMode = telemetryRepository.integrityState.value.isPowerSaveMode,
                    standbyBucket = trackerStatus.standbyBucket,
                    netInterface = trackerStatus.netInterface,
                    lastValidFixRealtime = trackerStatus.lastValidFixRealtime,
                    locationPendingReason = trackerStatus.locationPendingReason.name,
                    trackerState = trackerStatus.trackerState.name,
                    status = trackerStatus.status.name
                ))
            }
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
                alt = 0.0, 
                accuracy = entity.accuracy,
                maxAccuracy = entity.maxAccuracy,
                speed = entity.speed,
                bearing = entity.bearing,
                vibration = 0.0, 
                heading = 0.0,
                baroAlt = 0.0,
                lux = 0.0,
                isNear = true,
                tiltDegrees = 0.0,
                acousticDb = 0.0,
                jumpTier = 0,
                isJammer = false,
                isStalled = false,
                peakVibrationShock = 0.0,
                peakVibrationShockTs = 0L,
                isTamperDetected = false,
                isPowerTamper = false,
                status = try { SentinelStatus.valueOf(entity.status) } catch(e: Exception) { SentinelStatus.VALID },
                isLocationPending = false,
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE },
                lastValidFixRealtime = entity.lastValidFixRealtime,
                isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive,
                battery = entity.battery,
                temp = entity.temp,
                isCharging = entity.isCharging,
                trackerState = try { TrackerState.valueOf(entity.trackerState) } catch(e: Exception) { TrackerState.UNKNOWN }
            )

            if (networkManager.sendTelemetry(status)) {
                offlineRepository.deletePendingStatusUpdate(entity.id)
            }
        }
    }

    fun stopSyncLoop() {
        syncJob?.cancel()
    }
}
