package com.gps19.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.protobuf.CodedOutputStream
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * ConnectivityEvent: Reactive event container for peer and network lifecycle changes.
 */
sealed class ConnectivityEvent {
    data class PeerPulse(val id: String) : ConnectivityEvent()
}

/**
 * ConnectivitySuite: Unified connectivity and telemetry sync.
 * July.26.04:
 * - Issue #545c: Flow Architecture Standardization. Replaced legacy PeerListener 
 *   with a SharedFlow (connectivityEvents) for reactive signaling.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization. Migrated from legacy 
 *   RemoteUpdateListener to reactive signalingFlow collection.
 */
@Singleton
class ConnectivitySuite @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val telemetryRepository: TelemetryRepository,
    private val logManagerProvider: Provider<LogManager>,
    private val timeProvider: TimeProvider,
    private val signalingProvider: SignalingProvider,
    private val sessionManager: SessionManager,
    private val gpsManager: GpsManager,
    private val locationProcessor: LocationProcessor,
    private val offlineRepository: OfflineRepository,
    private val mainRepository: MainRepository,
    private val remoteStatusRepository: RemoteStatusRepository
) {
    private val _connectivityEvents = MutableSharedFlow<ConnectivityEvent>(extraBufferCapacity = 16)
    val connectivityEvents: SharedFlow<ConnectivityEvent> = _connectivityEvents.asSharedFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val isStarted = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)
    private val consecutiveHttpFailures = AtomicInteger(0)
    
    private var relayUrl = ""
    private var deviceId = ""
    private var viewerId = ""
    private var isTrackerMode = true
    private var lastReconnectTs = 0L 
    private var lastForceJoinTs = 0L 

    private val statusBuilder = RealtimeStatus.newBuilder()
    private var serializationBuffer = ByteArray(4096) 
    private val MAX_SERIALIZATION_BUFFER_SIZE = 65536 // 64KB Safety Clamp

    private val suiteExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped.get()) return@CoroutineExceptionHandler
        Timber.e(throwable, "ConnectivitySuite CRITICAL error")
        logManagerProvider.get().logServiceEvent("CRITICAL: ConnectivitySuite failure: ${throwable.message}", true)
        restartLoops()
    }

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + suiteExceptionHandler)
    private var keepAliveJob: Job? = null
    private var syncJob: Job? = null
    private var signalingJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val trackerStatus get() = remoteStatusRepository.remoteStatus.value
    val isTrackerConnected get() = remoteStatusRepository.isTrackerConnected.value
    val lastPeerActivityTs get() = remoteStatusRepository.lastPeerActivityTs.value
    val peerSignal get() = remoteStatusRepository.peerSignal.value

    val trackerLat get() = trackerStatus.lat
    val trackerLng get() = trackerStatus.lng
    val trackerSpeed get() = trackerStatus.speed
    val trackerBearing get() = trackerStatus.bearing
    val trackerAccuracy get() = trackerStatus.accuracy
    val trackerMaxAccuracy get() = trackerStatus.maxAccuracy
    val trackerLastGpsTs get() = trackerStatus.gpsTs
    val trackerLastValidFixRt get() = trackerStatus.lastValidFixRt
    val trackerBattery get() = trackerStatus.battery
    val trackerTemp get() = trackerStatus.temp
    val trackerMaxTemp get() = trackerStatus.maxTemp
    val trackerCurrentMa get() = trackerStatus.currentMa
    val trackerSatsView get() = trackerStatus.satsView
    val trackerSatsUsed get() = trackerStatus.satsUsed
    val isTrackerCharging get() = trackerStatus.isCharging
    val isTrackerJammerSuspicion get() = trackerStatus.isJammer
    val isTrackerVisualJump get() = trackerStatus.isJump
    val trackerJumpTier get() = trackerStatus.jumpTier
    val isTrackerTamperDetected get() = trackerStatus.isTamperDetected
    val isTrackerPowerTamper get() = trackerStatus.isPowerTamper
    val isTrackerLocationPending get() = trackerStatus.isLocationPending
    val trackerLocationPendingReason get() = trackerStatus.locationPendingReason
    val trackerLocationDetail get() = trackerStatus.gnssDetail
    val isTrackerBatterySteepDischarge get() = trackerStatus.isBatterySteepDischarge
    val isTrackerCoolingModeActive get() = trackerStatus.isCoolingModeActive
    val isTrackerPowerSaveMode get() = trackerStatus.isPowerSaveMode
    val trackerStandbyBucket get() = trackerStatus.standbyBucket
    val trackerNetInterface get() = trackerStatus.netInterface
    val isTrackerStorageLow get() = trackerStatus.isStorageLow
    val isTrackerStorageCritical get() = trackerStatus.isStorageCritical
    val trackerState get() = trackerStatus.trackerState
    val trackerVibration get() = trackerStatus.vibration
    val trackerHeading get() = trackerStatus.heading
    val trackerBaroAlt get() = trackerStatus.baroAlt
    val trackerLux get() = trackerStatus.lux
    val isTrackerNear get() = trackerStatus.isNear
    val trackerTiltDegrees get() = trackerStatus.tiltDegrees
    val trackerAcousticDb get() = trackerStatus.acousticDb
    val trackerPeakVibrationShock get() = trackerStatus.peakVibrationShock
    val trackerPeakVibrationShockTs get() = trackerStatus.peakVibrationShockTs
    val trackerLuxBaseline get() = trackerStatus.luxBaseline
    val trackerAcousticFloorDb get() = trackerStatus.acousticFloorDb
    val trackerAdaptiveVibrationFloor get() = trackerStatus.adaptiveVibrationFloor
    val trackerProxIdx get() = trackerStatus.proxIdx
    val trackerProximityCm get() = trackerStatus.proximityCm
    val trackerProximityDebounceMs get() = trackerStatus.proximityDebounceMs
    val trackerVibrationRollingSum get() = trackerStatus.vibrationRollingSum
    val trackerUptimeMs get() = trackerStatus.uptimeMs
    val trackerTotalDropMs get() = trackerStatus.totalDropMs
    val trackerMaxDropMs get() = trackerStatus.maxDropMs
    val trackerMaxDropTs get() = trackerStatus.maxDropTs
    val trackerTotalConnectedMs get() = trackerStatus.totalConnectedMs
    val trackerSessionConnectedMs get() = trackerStatus.sessionConnectedMs
    val trackerLastConnTs get() = trackerStatus.lastConnTs
    val trackerLastDiscTs get() = trackerStatus.lastDiscTs
    var trackerGpsStallStartTs = 0L 
    val trackerDistToHome get() = trackerStatus.sitDz 

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isStopped.get() || relayUrl.isEmpty()) return
            scope.launch {
                val nowRt = timeProvider.elapsedRealtime()
                if (lastReconnectTs > 0L && nowRt - lastReconnectTs < 3000L) return@launch
                if (signalingProvider.isConnected() || signalingProvider.isConnecting()) return@launch
                if (!SignalingConstants.isValidTrackerId(deviceId) || !SignalingConstants.isValidViewerId(viewerId)) return@launch

                logManagerProvider.get().logServiceEvent("Network Handover: Available. Reconnecting.", false)
                lastReconnectTs = nowRt
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }
        override fun onLost(network: Network) {
            if (isStopped.get()) return
            logManagerProvider.get().logServiceEvent("Network Handover: Interface Lost.", false)
            telemetryRepository.updateRelayStatus(false)
        }
    }

    fun start(url: String, dId: String, vId: String, isTracker: Boolean) {
        if (isStarted.getAndSet(true)) {
            this.relayUrl = url; this.deviceId = dId; this.viewerId = vId; this.isTrackerMode = isTracker
            if (!signalingProvider.isConnected() && !signalingProvider.isConnecting()) {
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            }
            return
        }

        isStopped.set(false)
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + suiteExceptionHandler)
        }
        
        this.relayUrl = url; this.deviceId = dId; this.viewerId = vId; this.isTrackerMode = isTracker
        this.lastForceJoinTs = 0L
        
        try {
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) { Timber.e("Failed to register network callback") }

        signalingProvider.setConnectionLostCallback {
            if (!isStopped.get() && relayUrl.isNotEmpty()) {
                val nowRt = timeProvider.elapsedRealtime()
                if (nowRt - lastReconnectTs > 10000L) {
                    lastReconnectTs = nowRt
                    wakeUpRelay()
                }
            }
        }

        startSignalingObservation()

        scope.launch {
            if (relayUrl.isNotEmpty() && SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
                if (!signalingProvider.isConnected() && !signalingProvider.isConnecting()) {
                    signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                    wakeUpRelay()
                }
            }
        }

        startKeepAliveLoop()
        startSyncLoop()
        initializePeerState()
    }

    private fun startSignalingObservation() {
        signalingJob?.cancel()
        signalingJob = scope.launch {
            signalingProvider.signalingFlow.collect { event ->
                when (event) {
                    is SignalingEvent.JsonUpdate -> handleJsonUpdate(event.data)
                    is SignalingEvent.BinaryUpdate -> handleBinaryUpdate(event.data)
                }
            }
        }
    }

    private fun restartLoops() {
        if (isStopped.get()) return
        startKeepAliveLoop()
        startSyncLoop()
        startSignalingObservation()
    }

    private fun startKeepAliveLoop() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                if (relayUrl.isNotEmpty()) {
                    try { performKeepAlive() } catch (e: Exception) { if (e is CancellationException) throw e }
                }
                delay(NET_REJOIN_THRESHOLD_MS)
            }
        }
    }

    private suspend fun performKeepAlive() = withContext(Dispatchers.IO) {
        val latestMode = settingsRepository.getAppMode() ?: (if (isTrackerMode) "tracker" else "viewer")
        val latestDeviceId = settingsRepository.getString(SettingsRepository.TRACKER_ID_KEY, deviceId)
        val latestViewerId = settingsRepository.getString(SettingsRepository.VIEWER_ID_KEY, viewerId)
        val latestRelayUrl = settingsRepository.getString(SettingsRepository.RELAY_URL_KEY, relayUrl)
        val latestIsTracker = latestMode == "tracker"

        if (latestDeviceId != deviceId || latestViewerId != viewerId || latestRelayUrl != relayUrl || latestIsTracker != isTrackerMode) {
            if (SignalingConstants.isValidTrackerId(latestDeviceId) && SignalingConstants.isValidViewerId(latestViewerId)) {
                deviceId = latestDeviceId; viewerId = latestViewerId; relayUrl = latestRelayUrl; isTrackerMode = latestIsTracker
                withContext(Dispatchers.Default) {
                    lastReconnectTs = timeProvider.elapsedRealtime()
                    lastForceJoinTs = 0L 
                    signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                    wakeUpRelay()
                }
            }
            return@withContext
        }

        try {
            val conn = (URL(relayUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000; readTimeout = 30000
                setRequestProperty("User-Agent", "GPS19-Monitor")
            }
            conn.responseCode
            conn.disconnect()
            consecutiveHttpFailures.set(0)

            val nowRt = timeProvider.elapsedRealtime()
            if (signalingProvider.isConnected()) {
                val trafficAge = nowRt - signalingProvider.getLastRelayTrafficTs()
                val rejoinCooldownPassed = nowRt - lastForceJoinTs > NET_REJOIN_THRESHOLD_MS * 4
                
                if (trafficAge > NET_REJOIN_THRESHOLD_MS * 2 && rejoinCooldownPassed) {
                    withContext(Dispatchers.Default) {
                        lastForceJoinTs = nowRt
                        signalingProvider.updateIdentity(deviceId, viewerId, isTrackerMode, force = true)
                        wakeUpRelay()
                    }
                }
            } else if (!signalingProvider.isConnecting() && nowRt - lastReconnectTs > NET_REJOIN_THRESHOLD_MS) {
                withContext(Dispatchers.Default) {
                    lastReconnectTs = nowRt
                    signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                    wakeUpRelay()
                }
            }
        } catch (e: Exception) {
            if (consecutiveHttpFailures.incrementAndGet() > 3) wakeUpRelay()
        }
    }

    private fun wakeUpRelay() {
        if (relayUrl.isEmpty() || isStopped.get()) return
        scope.launch(Dispatchers.IO) {
            repeat(4) {
                try {
                    val conn = URL(relayUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 30000; conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent", "GPS19-Wakeup")
                    conn.responseCode
                    conn.disconnect()
                    return@launch
                } catch (e: Exception) { delay(6000) }
            }
        }
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentRtt = signalingProvider.getRtt()
                if (isConnected()) {
                    _isSyncing.value = true
                    try { flushPendingUpdates() } catch (e: Exception) { Timber.e(e, "Sync failure") }
                    finally { _isSyncing.value = false }
                }
                val dynamicDelay = when {
                    currentRtt > MAX_ALLOWED_RTT_MS -> PING_INTERVAL_MS * 3
                    currentRtt > MAX_ALLOWED_RTT_MS / 2 -> (PING_INTERVAL_MS * (1.0 + (currentRtt.toDouble() / MAX_ALLOWED_RTT_MS))).toLong()
                    else -> PING_INTERVAL_MS
                }
                delay(dynamicDelay)
            }
        }
    }

    private suspend fun flushPendingUpdates() {
        val pending = offlineRepository.getPendingStatusUpdates(100)
        if (pending.isEmpty()) return
        pending.forEach { entity ->
            val status = TrackerStatus(
                deviceId = deviceId, viewerId = viewerId, ts = entity.timestamp, lat = entity.lat, lng = entity.lng, alt = 0.0, 
                accuracy = entity.accuracy, maxAccuracy = entity.maxAccuracy, speed = entity.speed, bearing = entity.bearing,
                vibration = 0.0, heading = 0.0, baroAlt = 0.0, lux = 0.0, isNear = true, tiltDegrees = 0.0, acousticDb = 0.0,
                jumpTier = 0, isJammer = false, isStalled = false, peakVibrationShock = 0.0, peakVibrationShockTs = 0L,
                isTamperDetected = false, isPowerTamper = false, status = try { SentinelStatus.valueOf(entity.status) } catch(e: Exception) { SentinelStatus.VALID },
                isLocationPending = false, locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE },
                lastValidFixRt = entity.lastValidFixRt, isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive, battery = entity.battery, temp = entity.temp, isCharging = entity.isCharging,
                trackerState = try { TrackerState.valueOf(entity.trackerState) } catch(e: Exception) { TrackerState.UNKNOWN },
                isStorageLow = entity.isStorageLow, isStorageCritical = entity.isStorageCritical,
                isPowerSaveMode = entity.isPowerSaveMode, standbyBucket = entity.standbyBucket, netInterface = entity.netInterface
            )
            if (sendTelemetryInternal(status)) offlineRepository.deletePendingStatusUpdate(entity.id)
        }
    }

    suspend fun sendTelemetry(status: TrackerStatus): Boolean {
        val success = sendTelemetryInternal(status)
        if (isTrackerMode) {
            mainRepository.saveTrackerState(status)
            if (!success) {
                offlineRepository.addPendingStatusUpdate(PendingStatusEntity(
                    lat = status.lat, lng = status.lng, speed = status.speed, accuracy = status.accuracy, bearing = status.bearing,
                    battery = status.battery, temp = status.temp, isCharging = status.isCharging, timestamp = status.ts,
                    gpsTs = status.gpsTs, satsView = status.gnssDetail?.satellites?.size ?: 0,
                    satsUsed = status.gnssDetail?.satellites?.count { it.usedInFix } ?: 0,
                    maxAccuracy = status.maxAccuracy, distToTracker = null, distToHome = null,
                    isBatterySteepDischarge = status.isBatterySteepDischarge, isCoolingModeActive = status.isCoolingModeActive,
                    isStorageLow = status.isStorageLow, isStorageCritical = status.isStorageCritical,
                    isPowerSaveMode = status.isPowerSaveMode, standbyBucket = status.standbyBucket,
                    netInterface = status.netInterface, lastValidFixRt = status.lastValidFixRt,
                    locationPendingReason = status.locationPendingReason.name, trackerState = status.trackerState.name, status = status.status.name
                ))
            }
        }
        return success
    }

    @Synchronized
    private fun sendTelemetryInternal(status: TrackerStatus): Boolean {
        if (!isConnected()) return false
        if (isTrackerMode) {
            status.writeTo(statusBuilder, false)
            val message = statusBuilder.buildPartial()
            val size = message.serializedSize
            
            if (size > serializationBuffer.size && size <= MAX_SERIALIZATION_BUFFER_SIZE) {
                val nextSize = (serializationBuffer.size * 2).coerceAtLeast(size).coerceAtMost(MAX_SERIALIZATION_BUFFER_SIZE)
                serializationBuffer = ByteArray(nextSize)
            }

            if (size <= serializationBuffer.size) {
                try {
                    val cos = CodedOutputStream.newInstance(serializationBuffer, 0, size)
                    message.writeTo(cos)
                    cos.checkNoSpaceLeft()
                    signalingProvider.emitBinary("location_update_bin", SignalingConstants.getTransmissionId(deviceId), serializationBuffer, size, SignalingPriority.NORMAL)
                    return true
                } catch (e: Exception) {
                    Timber.e(e, "Issue #560b: Pre-allocated serialization failed")
                }
            }
            signalingProvider.emitBinary("location_update_bin", SignalingConstants.getTransmissionId(deviceId), message.toByteArray(), priority = SignalingPriority.NORMAL)
        } else {
            signalingProvider.emitMap("location_update", status.toMap(true), SignalingPriority.NORMAL)
        }
        return true
    }

    suspend fun pushCurrentStatus(
        deviceId: String, viewerId: String, isTrackerMode: Boolean, loc: android.location.Location?, filtered: EngineGeoPoint?,
        distToTracker: Double?, distToHome: Double?, maxAccuracy: Double, filteredSpeed: Double,
        vibration: Double, heading: Double, baroAlt: Double, lux: Double, isNear: Boolean,
        tiltDegrees: Double, acousticDb: Double, jumpTier: Int,
        isJammer: Boolean, isStalled: Boolean, peakShock: Double, peakShockTs: Long,
        luxBaseline: Double, acousticFloorDb: Double, adaptiveVibrationFloor: Double, proxIdx: Double, proximityCm: Double,
        proximityDebounceMs: Long, vibrationRollingSum: Double, micPending: Boolean,
        isTamperDetected: Boolean, isPowerTamper: Boolean,
        isSitDetected: Boolean, isSitActive: Boolean, lastSitTs: Long,
        receiptRt: Long, violationUptimeMs: Long, violationPercentage: Double,
        verticalVelocity: Double, sitVz: Double, sitDz: Double, sitBaro: Double, sitTilt: Double, sitShock: Double,
        isClockRegression: Boolean, isLocationPending: Boolean, locationPendingReason: LocationPendingReason,
        lastValidFixRt: Long, gnssDetail: GnssDetail?,
        isBatterySteepDischarge: Boolean, isCoolingModeActive: Boolean,
        batteryLevel: Int, batteryTemp: Double, isCharging: Boolean,
        trackerState: TrackerState = TrackerState.UNKNOWN,
        status: SentinelStatus = SentinelStatus.VALID,
        isStorageLow: Boolean = false,
        isStorageCritical: Boolean = false,
        isPowerSaveMode: Boolean = false,
        standbyBucket: Int = -1,
        netInterface: String = "UNKNOWN",
        snrIdx: Double = 0.0,
        noiseIdx: Double = 0.0,
        luxIdx: Double = 0.0,
        vibeIdx: Double = 0.0,
        liftIdx: Double = 0.0,
        tiltIdx: Double = 0.0,
        baroIdx: Double = 0.0
    ) {
        val trackerStatus = TrackerStatus(
            deviceId = deviceId, viewerId = viewerId, ts = timeProvider.currentTimeMillis(),
            lat = filtered?.lat ?: loc?.latitude ?: 0.0, lng = filtered?.lng ?: loc?.longitude ?: 0.0,
            alt = loc?.altitude ?: 0.0, accuracy = loc?.accuracy?.toDouble() ?: 0.0,
            maxAccuracy = maxAccuracy, speed = filteredSpeed, bearing = loc?.bearing?.toDouble() ?: 0.0,
            vibration = vibration, heading = heading, baroAlt = baroAlt, lux = lux, isNear = isNear,
            tiltDegrees = tiltDegrees, acousticDb = acousticDb, jumpTier = jumpTier, isJammer = isJammer,
            isStalled = isStalled, peakVibrationShock = peakShock, peakVibrationShockTs = peakShockTs,
            luxBaseline = luxBaseline, acousticFloorDb = acousticFloorDb, adaptiveVibrationFloor = adaptiveVibrationFloor,
            proxIdx = proxIdx, proximityCm = proximityCm, proximityDebounceMs = proximityDebounceMs,
            vibrationRollingSum = vibrationRollingSum, isTamperDetected = isTamperDetected, isPowerTamper = isPowerTamper,
            violationUptimeMs = violationUptimeMs, violationPercentage = violationPercentage, status = status,
            isClockRegression = isClockRegression, isLocationPending = isLocationPending,
            locationPendingReason = locationPendingReason, lastValidFixRt = lastValidFixRt,
            isBatterySteepDischarge = isBatterySteepDischarge, isCoolingModeActive = isCoolingModeActive,
            gnssDetail = gnssDetail, battery = batteryLevel, temp = batteryTemp, isCharging = isCharging,
            trackerState = trackerState,
            isStorageLow = isStorageLow, isStorageCritical = isStorageCritical,
            isPowerSaveMode = isPowerSaveMode, standbyBucket = standbyBucket, netInterface = netInterface,
            snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, liftIdx = liftIdx,
            tiltIdx = tiltIdx, baroIdx = baroIdx,
            micPending = micPending, isSitDetected = isSitDetected, isSitActive = isSitActive, lastSitTs = lastSitTs,
            verticalVelocity = verticalVelocity, sitVz = sitVz, sitDz = sitDz, sitBaro = sitBaro, sitTilt = sitTilt, sitShock = sitShock
        )
        sendTelemetry(trackerStatus)
    }

    private fun initializePeerState() {
        scope.launch {
            remoteStatusRepository.initialize()
        }
    }

    private fun handleBinaryUpdate(data: ByteArray) {
        if (isTrackerMode || isStopped.get()) return
        try {
            val statusProto = RealtimeStatus.parseFrom(data)
            
            if (!SignalingValidator.shouldProcessLocationUpdate(
                    incomingId = statusProto.id,
                    ownDeviceId = deviceId,
                    isFromViewer = statusProto.fromViewer,
                    viewerId = statusProto.viewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return

            val now = timeProvider.currentTimeMillis()
            val nowRt = timeProvider.elapsedRealtime()
            val peerId = statusProto.id

            _connectivityEvents.tryEmit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt)
            remoteStatusRepository.setTrackerConnected(true)
            mainRepository.updateRemoteActivity(now)
            
            remoteStatusRepository.setPeerSignal((statusProto.snrIdx * 10.0).toInt().coerceIn(0, 10))

            remoteStatusRepository.updateStatusAtomic { current ->
                val trackerLocationPendingReason = TrackerStatus.mapProtoToPendingReason(statusProto.pendingReason)
                
                var lat = current.lat; var lng = current.lng; var gpsTs = current.gpsTs; var filteredSpeed = current.speed; var lastFixRt = statusProto.lastValidFixRt
                var isClockReg = current.isClockRegression; var isVisualJump = current.isJump

                val processed = locationProcessor.processGpsPoint(
                    lat = statusProto.lat, lng = statusProto.lng, alt = statusProto.alt, 
                    androidSpeedMps = statusProto.speed.coerceAtLeast(0.0),
                    gpsTs = statusProto.gpsTs, accuracy = statusProto.accuracy.coerceAtLeast(0.0), 
                    bearing = statusProto.bearing, snr = statusProto.snrIdx * 45.0,
                    satsUsed = statusProto.satsUsed, isViewerTrail = false, lastGpsTs = current.gpsTs,
                    providedMaxAccuracy = statusProto.maxAccuracy, 
                    providedJumpTier = statusProto.jumpTier, providedIsJammer = statusProto.isJammer, 
                    providedIsStalled = statusProto.isStalled,
                    providedIsTamper = statusProto.isTamperDetected || statusProto.isLocationPending,
                    nowWall = now, nowRt = nowRt
                )
                
                isClockReg = processed.isClockRegression
                if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0) {
                    lat = processed.optimizedPoint.lat; lng = processed.optimizedPoint.lng; gpsTs = processed.optimizedPoint.ts
                    lastFixRt = nowRt
                }
                filteredSpeed = processed.filteredSpeed
                isVisualJump = processed.status == SentinelStatus.JUMP

                val updatedStatus = current.copy(
                    lat = lat, lng = lng, gpsTs = gpsTs, speed = filteredSpeed, bearing = statusProto.bearing,
                    accuracy = statusProto.accuracy, maxAccuracy = statusProto.maxAccuracy,
                    battery = statusProto.battery, temp = statusProto.temp, 
                    isCharging = statusProto.isCharging,
                    satsView = statusProto.satsView, satsUsed = statusProto.satsUsed,
                    status = processed.status, 
                    isLocationPending = statusProto.isLocationPending, locationPendingReason = trackerLocationPendingReason,
                    lastValidFixRt = lastFixRt, isBatterySteepDischarge = statusProto.isBatterySteepDischarge, isCoolingModeActive = statusProto.isCoolingModeActive,
                    trackerState = TrackerStatus.mapProtoToTrackerState(statusProto.state),
                    ts = now,
                    snrIdx = statusProto.snrIdx, noiseIdx = statusProto.noiseIdx, 
                    luxIdx = statusProto.luxIdx, vibeIdx = statusProto.vibeIdx, 
                    liftIdx = statusProto.liftIdx,
                    tiltIdx = statusProto.tiltIdx, baroIdx = statusProto.baroIdx,
                    isSitDetected = statusProto.isSitDetected, lastSitTs = statusProto.lastSitTs,
                    verticalVelocity = statusProto.verticalVelocity,
                    sitVz = statusProto.sitVz, sitDz = statusProto.sitDz,
                    sitBaro = statusProto.sitBaro, sitTilt = statusProto.sitTilt, sitShock = statusProto.sitShock,
                    isSitActive = statusProto.isSitActive,
                    uptimeMs = statusProto.uptimeMs,
                    totalConnectedMs = statusProto.totalConnectedMs,
                    sessionConnectedMs = statusProto.sessionConnectedMs,
                    totalDropMs = statusProto.totalDropMs,
                    maxDropMs = statusProto.maxDropMs,
                    lastConnTs = statusProto.lastConnTs,
                    lastDiscTs = statusProto.lastDiscTs,
                    isJump = isVisualJump,
                    isClockRegression = statusProto.isClockRegression,
                    isJammer = statusProto.isJammer,
                    isStalled = statusProto.isStalled,
                    isTamperDetected = statusProto.isTamperDetected,
                    jumpTier = statusProto.jumpTier
                )

                scope.launch {
                    mainRepository.updateLocation(LocationUpdate(
                        lat = updatedStatus.lat, lng = updatedStatus.lng, speed = updatedStatus.speed, accuracy = updatedStatus.accuracy, bearing = updatedStatus.bearing,
                        battery = updatedStatus.battery, temp = updatedStatus.temp, isCharging = updatedStatus.isCharging,
                        gpsTs = updatedStatus.gpsTs, isMe = false, satsView = updatedStatus.satsView, satsUsed = updatedStatus.satsUsed, 
                        status = updatedStatus.status, 
                        isClockRegression = updatedStatus.isClockRegression, isLocationPending = updatedStatus.isLocationPending, 
                        locationPendingReason = updatedStatus.locationPendingReason, lastValidFixRt = updatedStatus.lastValidFixRt, 
                        isBatterySteepDischarge = updatedStatus.isBatterySteepDischarge, isCoolingModeActive = updatedStatus.isCoolingModeActive,
                        trackerState = updatedStatus.trackerState, ts = now, 
                        snrIdx = updatedStatus.snrIdx, noiseIdx = updatedStatus.noiseIdx, luxIdx = updatedStatus.luxIdx, vibeIdx = updatedStatus.vibeIdx, liftIdx = updatedStatus.liftIdx,
                        tiltIdx = updatedStatus.tiltIdx, baroIdx = updatedStatus.baroIdx,
                        isSitDetected = updatedStatus.isSitDetected, lastSitTs = updatedStatus.lastSitTs,
                        verticalVelocity = updatedStatus.verticalVelocity, sitVz = updatedStatus.sitVz, sitDz = updatedStatus.sitDz,
                        sitBaro = updatedStatus.sitBaro, sitTilt = updatedStatus.sitTilt, sitShock = updatedStatus.sitShock
                    ))
                }
                updatedStatus
            }
        } catch (e: Exception) {
            Timber.e(e, "Issue #541: Protobuf direct parse error")
        }
    }

    private fun handleJsonUpdate(data: JSONObject) {
        val type = data.optString("type", "")
        if (type == "remote_log") {
            handleRemoteLog(LogEntry.fromJSONObject(data))
            return
        }

        val fromId = data.optString("id"); val fromViewerId = data.optString("viewer_id"); val fromViewer = data.optBoolean("from_viewer", false)
        val now = timeProvider.currentTimeMillis(); val nowRt = timeProvider.elapsedRealtime()
        val peerId = if (isTrackerMode) (if (fromViewerId.isNotEmpty()) fromViewerId else fromId) else fromId

        if (isTrackerMode && fromViewer && type == "calibrate_chair") {
            locationProcessor.resetChairBaseline()
            mainRepository.addLog(LogEntry(
                timestamp = now,
                message = "REMOTE CALIBRATION: Chair baseline zeroed via viewer command",
                type = "event",
                isImportant = true
            ))
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, "REMOTE: Chair Baseline Zeroed", Toast.LENGTH_SHORT).show() }
            _connectivityEvents.tryEmit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(now)
            return
        }

        if (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity") {
            if ((isTrackerMode && fromViewer) || (!isTrackerMode && !fromViewer)) {
                _connectivityEvents.tryEmit(ConnectivityEvent.PeerPulse(peerId))
                remoteStatusRepository.updatePeerActivity(nowRt); remoteStatusRepository.setTrackerConnected(!isTrackerMode); mainRepository.updateRemoteActivity(now)
            }
            return
        }

        if (isTrackerMode && fromViewer) {
            _connectivityEvents.tryEmit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(now); return
        }

        if (!isTrackerMode && !fromViewer) {
            val remoteTs = data.optLong("ts", 0L)
            if (!remoteStatusRepository.shouldProcessPacket(remoteTs)) return

            _connectivityEvents.tryEmit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); remoteStatusRepository.setTrackerConnected(true); mainRepository.updateRemoteActivity(now)
            remoteStatusRepository.setPeerSignal(data.optInt("signal", 0))

            remoteStatusRepository.updateStatusAtomic { current ->
                val statusStr = data.optString("status", current.status.name)
                val trackerStatus = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { current.status }
                val isTrackerTamperDetected = data.optBoolean("is_tamper_detected", current.isTamperDetected)
                val isTrackerPowerTamper = data.optBoolean("is_power_tamper", current.isPowerTamper)
                val isTrackerLocationPending = data.optBoolean("is_location_pending", false)
                val trackerLocationPendingReason = try { LocationPendingReason.valueOf(data.optString("location_pending_reason", "NONE")) } catch(e: Exception) { LocationPendingReason.NONE }
                val trackerLastValidFixRt = data.optLong("last_valid_fix_rt", current.lastValidFixRt)
                
                var gnssDetail = current.gnssDetail
                if (data.has("gnss_detail")) {
                    try {
                        val array = data.getJSONArray("gnss_detail"); val satList = mutableListOf<SatelliteInfo>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i); satList.add(SatelliteInfo(svid = obj.getInt("svid"), cn0 = obj.optDouble("cn0", 0.0), usedInFix = obj.getBoolean("used_in_fix"), constellation = obj.optInt("constellation", 0)))
                        }
                        gnssDetail = GnssDetail(satellites = satList)
                    } catch (e: Exception) { Timber.e(e, "GNSS detail parse error") }
                }

                var lat = current.lat; var lng = current.lng; var gpsTs = current.gpsTs; var filteredSpeed = current.speed; var lastFixRt = trackerLastValidFixRt
                var isClockReg = current.isClockRegression; var isVisualJump = current.isJump

                if (data.has("lat") || data.has("gps_ts") || data.has("gps_age_ms")) {
                    val incomingGpsTs = data.optLong("gps_ts", 0L)
                    val gpsAgeMs = if (data.has("gps_age_ms")) data.optLong("gps_age_ms") else (if (incomingGpsTs > 0) maxOf(0L, now - incomingGpsTs) else 0L)
                    val candidateTs = if (gpsAgeMs > 0 || incomingGpsTs > 0) now - gpsAgeMs else 0L
                    
                    val processed = locationProcessor.processGpsPoint(
                        lat = data.optDouble("lat", 0.0), lng = data.optDouble("lng", 0.0), alt = data.optDouble("alt", 0.0), 
                        androidSpeedMps = data.optDouble("speed", 0.0).coerceAtLeast(0.0),
                        gpsTs = candidateTs, accuracy = data.optDouble("accuracy", 0.0).coerceAtLeast(0.0), 
                        bearing = data.optDouble("bearing", 0.0), snr = 0.0,
                        satsUsed = data.optInt("sats_used", current.satsUsed), isViewerTrail = false, lastGpsTs = current.gpsTs,
                        providedMaxAccuracy = data.optDouble("max_accuracy", 0.0), providedJumpTier = data.optInt("jump_tier", 0), providedIsJammer = data.optBoolean("is_jammer", false),
                        providedIsStalled = data.optBoolean("is_stalled", false), providedIsTamper = isTrackerTamperDetected || isTrackerLocationPending || trackerStatus == SentinelStatus.TAMPER,
                        nowWall = now, nowRt = nowRt
                    )
                    isClockReg = processed.isClockRegression
                    if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0) {
                        lat = processed.optimizedPoint.lat; lng = processed.optimizedPoint.lng; gpsTs = processed.optimizedPoint.ts
                        if (!processed.isStalled) lastFixRt = nowRt
                    }
                    filteredSpeed = processed.filteredSpeed
                    isVisualJump = processed.status == SentinelStatus.JUMP
                }

                val luxBaseline = data.optDouble("lux_baseline", current.luxBaseline)
                val acousticFloor = data.optDouble("acoustic_floor_db", current.acousticFloorDb)

                locationProcessor.sentinel.updateSensorState(
                    vibration = data.optDouble("vibration", current.vibration), heading = data.optDouble("heading", current.heading), 
                    baroAlt = data.optDouble("baro_alt", current.baroAlt), lux = data.optDouble("lux", current.lux), isNear = data.optBoolean("is_near", current.isNear),
                    powerTamper = isTrackerPowerTamper, tiltDegrees = data.optDouble("tilt_degrees", current.tiltDegrees), 
                    acousticDb = data.optDouble("acoustic_db", current.acousticDb), peakShock = data.optDouble("peak_vibration_shock", current.peakVibrationShock),
                    acousticMinDb = -1.0, nowRt = nowRt, nowTs = now
                )

                if (data.optBoolean("is_stalled", false) && trackerGpsStallStartTs == 0L) trackerGpsStallStartTs = nowRt else if (!data.optBoolean("is_stalled", false)) trackerGpsStallStartTs = 0L

                val updatedStatus = current.copy(
                    lat = lat, lng = lng, gpsTs = gpsTs, speed = filteredSpeed, bearing = data.optDouble("bearing", current.bearing),
                    accuracy = data.optDouble("accuracy", current.accuracy), maxAccuracy = data.optDouble("max_accuracy", current.maxAccuracy),
                    battery = data.optInt("battery", current.battery), temp = data.optDouble("temp", current.temp), maxTemp = data.optDouble("max_temp", current.maxTemp),
                    currentMa = data.optInt("current_ma", current.currentMa), isCharging = data.optBoolean("is_charging", current.isCharging),
                    satsView = data.optInt("sats_view", current.satsView), satsUsed = data.optInt("sats_used", current.satsUsed),
                    status = trackerStatus, isTamperDetected = isTrackerTamperDetected, isPowerTamper = isTrackerPowerTamper,
                    isLocationPending = isTrackerLocationPending, locationPendingReason = trackerLocationPendingReason,
                    lastValidFixRt = lastFixRt, isBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false), isCoolingModeActive = data.optBoolean("is_cooling_mode_active", false),
                    isPowerSaveMode = data.optBoolean("is_power_save_mode", current.isPowerSaveMode), standbyBucket = data.optInt("standby_bucket", current.standbyBucket), netInterface = data.optString("net_interface", current.netInterface),
                    isStorageLow = data.optBoolean("is_storage_low", current.isStorageLow), isStorageCritical = data.optBoolean("is_storage_critical", current.isStorageCritical), 
                    trackerState = try { TrackerState.valueOf(data.optString("tracker_state", "UNKNOWN")) } catch(e: Exception) { current.trackerState }, 
                    gnssDetail = gnssDetail, vibration = data.optDouble("vibration", current.vibration), heading = data.optDouble("heading", current.heading),
                    baroAlt = data.optDouble("baro_alt", current.baroAlt), lux = data.optDouble("lux", current.lux), isNear = data.optBoolean("is_near", current.isNear),
                    tiltDegrees = data.optDouble("tilt_degrees", current.tiltDegrees), acousticDb = data.optDouble("acoustic_db", current.acousticDb),
                    peakVibrationShock = data.optDouble("peak_vibration_shock", current.peakVibrationShock), peakVibrationShockTs = data.optLong("peak_shock_ts", current.peakVibrationShockTs),
                    luxBaseline = luxBaseline, acousticFloorDb = acousticFloor, adaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", current.adaptiveVibrationFloor),
                    proxIdx = data.optDouble("prox_idx", current.proxIdx), proximityCm = data.optDouble("proximity_cm", current.proximityCm),
                    proximityDebounceMs = data.optLong("proximity_debounce_ms", current.proximityDebounceMs), vibrationRollingSum = data.optDouble("vibration_rolling_sum", current.vibrationRollingSum),
                    uptimeMs = data.optLong("uptime_ms", current.uptimeMs), totalDropMs = data.optLong("total_drop_ms", current.totalDropMs),
                    maxDropMs = data.optLong("max_drop_ms", current.maxDropMs), maxDropTs = data.optLong("max_drop_ts", current.maxDropTs),
                    totalConnectedMs = data.optLong("total_connected_ms", current.totalConnectedMs), sessionConnectedMs = data.optLong("session_connected_ms", current.sessionConnectedMs),
                    lastConnTs = data.optLong("last_conn_ts", current.lastConnTs), lastDiscTs = data.optLong("last_disc_ts", current.lastDiscTs),
                    isClockRegression = isClockReg, isJump = isVisualJump, ts = now,
                    snrIdx = data.optDouble("snr_idx", current.snrIdx), noiseIdx = data.optDouble("noise_idx", current.noiseIdx), 
                    luxIdx = data.optDouble("lux_idx", current.luxIdx), vibeIdx = data.optDouble("vibe_idx", current.vibeIdx), 
                    liftIdx = data.optDouble("lift_idx", current.liftIdx),
                    tiltIdx = data.optDouble("tilt_idx", current.tiltIdx), baroIdx = data.optDouble("baro_idx", current.baroIdx),
                    isSitDetected = data.optBoolean("is_sit_detected", current.isSitDetected), lastSitTs = data.optLong("last_sit_ts", current.lastSitTs),
                    isSuspicious = data.optBoolean("is_suspicious", current.isSuspicious), isAnchorLocked = data.optBoolean("is_anchor_locked", current.isAnchorLocked),
                    verticalVelocity = data.optDouble("vertical_velocity", current.verticalVelocity),
                    sitVz = data.optDouble("sit_vz", current.sitVz), sitDz = data.optDouble("sit_dz", current.sitDz),
                    sitBaro = data.optDouble("sit_baro", current.sitBaro), sitTilt = data.optDouble("sit_tilt", current.sitTilt), sitShock = data.optDouble("sit_shock", current.sitShock)
                )

                scope.launch {
                    mainRepository.updateLocation(LocationUpdate(
                        lat = updatedStatus.lat, lng = updatedStatus.lng, speed = updatedStatus.speed, accuracy = updatedStatus.accuracy, bearing = updatedStatus.bearing,
                        battery = updatedStatus.battery, temp = updatedStatus.temp, maxTemp = updatedStatus.maxTemp, isCharging = updatedStatus.isCharging, currentMa = updatedStatus.currentMa,
                        gpsTs = updatedStatus.gpsTs, isMe = false, satsView = updatedStatus.satsView, satsUsed = updatedStatus.satsUsed, 
                        status = updatedStatus.status, isTamperDetected = updatedStatus.isTamperDetected, isPowerTamper = updatedStatus.isPowerTamper,
                        isClockRegression = updatedStatus.isClockRegression, isLocationPending = updatedStatus.isLocationPending, 
                        locationPendingReason = updatedStatus.locationPendingReason, lastValidFixRt = updatedStatus.lastValidFixRt, 
                        isPowerSaveMode = updatedStatus.isPowerSaveMode, standbyBucket = updatedStatus.standbyBucket,
                        netInterface = updatedStatus.netInterface, isStorageLow = updatedStatus.isStorageLow, isStorageCritical = updatedStatus.isStorageCritical,
                        gnssDetail = updatedStatus.gnssDetail, isBatterySteepDischarge = updatedStatus.isBatterySteepDischarge, isCoolingModeActive = updatedStatus.isCoolingModeActive,
                        trackerState = updatedStatus.trackerState, ts = now, 
                        snrIdx = updatedStatus.snrIdx, noiseIdx = updatedStatus.noiseIdx, luxIdx = updatedStatus.luxIdx, vibeIdx = updatedStatus.vibeIdx, liftIdx = updatedStatus.liftIdx,
                        tiltIdx = updatedStatus.tiltIdx, baroIdx = updatedStatus.baroIdx,
                        isSitDetected = updatedStatus.isSitDetected, lastSitTs = updatedStatus.lastSitTs,
                        verticalVelocity = updatedStatus.verticalVelocity, sitVz = updatedStatus.sitVz, sitDz = updatedStatus.sitDz,
                        sitBaro = updatedStatus.sitBaro, sitTilt = updatedStatus.sitTilt, sitShock = updatedStatus.sitShock
                    ))
                }
                updatedStatus
            }
        }
    }

    private fun handleRemoteLog(entry: LogEntry) {
        val now = timeProvider.currentTimeMillis(); val nowRt = timeProvider.elapsedRealtime()
        remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(now)
    }

    fun onRelayLost() { remoteStatusRepository.setTrackerConnected(false) }

    fun resetPeerStats() {
        remoteStatusRepository.reset()
        trackerGpsStallStartTs = 0L
        mainRepository.saveDoubleSync(MainRepository.TRACKER_LUX_BASELINE_KEY, 0.0)
        mainRepository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
    }

    fun stop() { 
        isStarted.set(false)
        isStopped.set(true); keepAliveJob?.cancel(); syncJob?.cancel(); signalingJob?.cancel(); scope.cancel()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        signalingProvider.disconnect() 
    }

    fun isConnected() = signalingProvider.isConnected()
    fun getRtt() = signalingProvider.getRtt()
    fun clearRtt() = signalingProvider.clearRtt()
    
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL) { 
        if (!isStopped.get()) signalingProvider.emit(event, data, priority) 
    }
    fun emitBinary(event: String, routingId: String, data: ByteArray, priority: SignalingPriority = SignalingPriority.NORMAL) { 
        if (!isStopped.get()) signalingProvider.emitBinary(event, routingId, data, priority = priority) 
    }

    fun updateRelayStatus(connected: Boolean) { telemetryRepository.updateRelayStatus(connected) }
    fun updateIdentity(dId: String, vId: String, isTracker: Boolean) {
        if (isStopped.get()) return
        if (!SignalingConstants.isValidTrackerId(dId) || !SignalingConstants.isValidViewerId(vId)) return
        this.deviceId = dId; this.viewerId = vId; this.isTrackerMode = isTracker
        signalingProvider.updateIdentity(dId, vId, isTracker)
    }
    fun connect(url: String) {
        if (isStopped.get()) return
        this.relayUrl = url; this.lastReconnectTs = timeProvider.elapsedRealtime()
        if (SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
            if (!signalingProvider.isConnected() && !signalingProvider.isConnecting()) {
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }
    }
}
