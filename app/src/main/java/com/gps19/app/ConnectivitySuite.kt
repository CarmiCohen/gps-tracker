package com.gps19.app

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * ConnectivitySuite: Unified connectivity and telemetry sync.
 * Oct.2.8:
 * - Issue #1330: Snap-to-Update Monolith. Migrated from SystemEvaluationSnapshot 
 *   to unified LocationUpdate DTO (R-ID 596).
 * Oct.1.8:
 * - Issue #1410: Viewer Persistence. Added global acknowledgment synchronization. 
 *   Handles "acknowledge_alarm" signals and synchronizes lastAlarmAckTs 
 *   from remote telemetry to prevent recurring alarms (R-ID 575).
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
    private val hardwareSuite: HardwareSuite,
    private var locationProcessor: LocationProcessor, 
    private val offlineRepository: OfflineRepository,
    private val mainRepository: MainRepository,
    private val remoteStatusRepository: RemoteStatusRepository,
    private val forensicLogger: SignalingForensicLogger,
    private val networkProvider: NetworkProvider,
    private val signalingTransport: SignalingTransport,
    private val domainEventBus: DomainEventBus
) {
    private val isStarted = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)
    private val consecutiveHttpFailures = AtomicInteger(0)
    
    private var relayUrl = ""
    private var deviceId = ""
    private var viewerId = ""
    private var isTrackerMode = true
    private var lastReconnectTs = 0L 
    private var reconnectAttempt = 0

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
    private var identitySyncJob: Job? = null
    private var heartbeatJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // R-ID 392: Reusable flyweights for zero-allocation packet processing.
    // Issue #1330: snapshotFlyweight now uses unified LocationUpdate DTO.
    private val snapshotFlyweight = LocationUpdate()
    private val updateFlyweight = LocationUpdate()
    private val statusFlyweight = TrackerStatus()
    private val pendingStatusFlyweight = TrackerStatus()
    
    private val localStatusFlyweight = TrackerStatus()

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
    val isTrackerAdaptiveJump get() = trackerStatus.isAdaptiveJump
    val trackerJumpTier get() = trackerStatus.jumpTier
    val isTrackerTamperDetected get() = trackerStatus.isTamperDetected
    val isTrackerPowerTamper get() = trackerStatus.isPowerTamper
    val isTrackerLocationPending get() = trackerStatus.isLocationPending
    val trackerLocationPendingReason get() = trackerStatus.locationPendingReason
    val trackerLocationDetail get() = trackerStatus.gnssDetail
    val isTrackerBatteryWhitelisted get() = trackerStatus.isBatteryWhitelisted
    val isTrackerBatterySteepDischarge get() = trackerStatus.isBatterySteepDischarge
    val isTrackerCoolingModeActive get() = trackerStatus.isCoolingModeActive
    val isTrackerBatteryLow get() = trackerStatus.isBatteryLow
    val isTrackerBatteryCritical get() = trackerStatus.isBatteryCritical
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
    val tiltDegrees get() = trackerStatus.tiltDegrees
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
    val trackerKineticEnergy get() = trackerStatus.kineticEnergy
    val isTrackerGpsHardwareLock get() = trackerStatus.gpsHardwareLock
    val trackerTamperNote get() = trackerStatus.tamperNote

    private val networkListener = object : NetworkListener {
        override fun onNetworkAvailable() {
            if (isStopped.get() || relayUrl.isEmpty()) return
            scope.launch {
                val nowRt = timeProvider.elapsedRealtime()
                if (lastReconnectTs > 0L && nowRt - lastReconnectTs < 3000L) return@launch
                if (signalingProvider.isConnected() || signalingProvider.isConnecting()) return@launch
                if (!SignalingConstants.isValidTrackerId(deviceId) || !SignalingConstants.isValidViewerId(viewerId)) return@launch

                forensicLogger.logHandover("Interface Available. Reconnecting.", "active")
                logManagerProvider.get().logServiceEvent("Network Handover: Available. Reconnecting.", false)
                lastReconnectTs = nowRt
                reconnectAttempt = 0
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }
        override fun onNetworkLost() {
            if (isStopped.get()) return
            forensicLogger.logHandover("Interface Lost.", "none")
            logManagerProvider.get().logServiceEvent("Network Handover: Interface Lost.", false)
            telemetryRepository.updateRelayStatus(false)
        }
    }

    fun updateRemoteProcessor(processor: LocationProcessor) {
        this.locationProcessor = processor
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
        
        networkProvider.registerListener(networkListener)

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
        startIdentitySyncLoop()
        startHeartbeatLoop()
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
        startIdentitySyncLoop()
        startHeartbeatLoop()
    }

    private fun startKeepAliveLoop() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                if (relayUrl.isNotEmpty()) {
                    try { performKeepAlive() } catch (e: Exception) { if (e is CancellationException) throw e }
                }
                
                val delayMs = calculateNextRejoinDelay()
                delay(delayMs)
            }
        }
    }
    
    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30000)
                if (isTrackerMode && isConnected() && !isStopped.get()) {
                    if (localStatusFlyweight.ts > 0 && !hardwareSuite.shouldDeferSignaling(sessionManager.isInViolation)) {
                        Timber.d("ConnectivitySuite: Issuing Bypass Heartbeat to stabilize peer link.")
                        sendTelemetryInternal(localStatusFlyweight, SignalingPriority.NORMAL)
                    }
                }
            }
        }
    }

    private fun calculateNextRejoinDelay(): Long {
        if (signalingProvider.isConnected()) reconnectAttempt = 0
        return hardwareSuite.calculateNextBackoff(reconnectAttempt, signalingProvider.isConnected())
    }

    private fun startIdentitySyncLoop() {
        identitySyncJob?.cancel()
        identitySyncJob = scope.launch {
            while (isActive) {
                delay(60000) 
                if (isConnected() && !isStopped.get()) {
                    if (hardwareSuite.shouldDeferSignaling(sessionManager.isInViolation)) {
                        Timber.d("ConnectivitySuite: Identity sync deferred (Doze active)")
                        continue
                    }
                    Timber.d("ConnectivitySuite: Periodic identity sync (R254)")
                    signalingProvider.updateIdentity(deviceId, viewerId, isTrackerMode, force = true)
                }
            }
        }
    }

    private suspend fun performKeepAlive() = withContext(Dispatchers.IO) {
        if (hardwareSuite.shouldDeferSignaling(sessionManager.isInViolation)) {
            return@withContext
        }

        val latestMode = settingsRepository.getAppMode() ?: (if (isTrackerMode) "tracker" else "viewer")
        val latestDeviceId = settingsRepository.getString(TRACKER_ID_KEY, deviceId)
        val latestViewerId = settingsRepository.getString(VIEWER_ID_KEY, viewerId)
        val latestRelayUrl = settingsRepository.getString(RELAY_URL_KEY, relayUrl)
        val latestIsTracker = latestMode == "tracker"

        if (latestDeviceId != deviceId || latestViewerId != viewerId || latestRelayUrl != relayUrl || latestIsTracker != isTrackerMode) {
            if (SignalingConstants.isValidTrackerId(latestDeviceId) && SignalingConstants.isValidViewerId(latestViewerId)) {
                deviceId = latestDeviceId; viewerId = latestViewerId; relayUrl = latestRelayUrl; isTrackerMode = latestIsTracker
                withContext(Dispatchers.Default) {
                    lastReconnectTs = timeProvider.elapsedRealtime()
                    reconnectAttempt = 0
                    signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                    wakeUpRelay()
                }
            }
            return@withContext
        }

        try {
            val responseCode = signalingTransport.performKeepAlive(relayUrl)
            if (responseCode in 200..299) {
                consecutiveHttpFailures.set(0)
            } else {
                if (consecutiveHttpFailures.incrementAndGet() > 3) wakeUpRelay()
            }

            val nowRt = timeProvider.elapsedRealtime()
            if (!signalingProvider.isConnected() && !signalingProvider.isConnecting()) {
                val delay = calculateNextRejoinDelay()
                if (nowRt - lastReconnectTs > delay) {
                    withContext(Dispatchers.Default) {
                        lastReconnectTs = nowRt
                        reconnectAttempt++
                        signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                        wakeUpRelay()
                    }
                }
            }
        } catch (e: Exception) {
            if (consecutiveHttpFailures.incrementAndGet() > 3) wakeUpRelay()
        }
    }

    private fun wakeUpRelay() {
        if (relayUrl.isEmpty() || isStopped.get()) return
        scope.launch(Dispatchers.IO) {
            signalingTransport.wakeUpRelay(relayUrl)
        }
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            var wasConnected = false
            
            while (isActive) {
                val currentRtt = signalingProvider.getRtt()
                val isCurrentlyConnected = isConnected()
                val inViolation = sessionManager.isInViolation
                
                if (isCurrentlyConnected) {
                    if (!wasConnected) {
                        delay(500) 
                    }
                    
                    if (hardwareSuite.shouldDeferSignaling(inViolation)) {
                        Timber.v("ConnectivitySuite: Telemetry sync deferred (Doze active)")
                    } else {
                        _isSyncing.value = true
                        try { 
                            val batchSize = if (inViolation) SYNC_BATCH_SIZE_VIOLATION else LOG_BATCH_SIZE
                            flushPendingUpdates(batchSize) 
                        } catch (e: Exception) { Timber.e(e, "Sync failure") }
                        finally { _isSyncing.value = false }
                    }

                    if (currentRtt > MAX_ALLOWED_RTT_MS / 2) {
                        forensicLogger.logHighLatency(currentRtt.toLong(), MAX_ALLOWED_RTT_MS / 2)
                    }
                }
                
                wasConnected = isCurrentlyConnected

                val dynamicDelay = when {
                    inViolation -> SYNC_INTERVAL_VIOLATION_MS
                    currentRtt > MAX_ALLOWED_RTT_MS -> PING_INTERVAL_MS * 3
                    currentRtt > MAX_ALLOWED_RTT_MS / 2 -> (PING_INTERVAL_MS * (1.0 + (currentRtt.toDouble() / MAX_ALLOWED_RTT_MS))).toLong()
                    else -> PING_INTERVAL_MS
                }
                delay(dynamicDelay)
            }
        }
    }

    private suspend fun flushPendingUpdates(limit: Int = LOG_BATCH_SIZE) {
        val pending = offlineRepository.getPendingStatusUpdates(limit)
        if (pending.isEmpty()) return
        pending.forEach { entity ->
            val status = TelemetryMapper.mapPendingToStatus(entity, deviceId, viewerId, pendingStatusFlyweight)
            if (sendTelemetryInternal(status, SignalingPriority.NORMAL)) {
                offlineRepository.deletePendingStatusUpdate(entity.id)
            } else {
                forensicLogger.logTransmissionFailure("Pending update sync drop", if (isTrackerMode) "TRK" else "VWR", deviceId, viewerId)
            }
        }
    }

    fun updateLocalTelemetry(status: TrackerStatus) {
        localStatusFlyweight.copyFrom(status)
    }

    suspend fun sendTelemetry(status: TrackerStatus): Boolean {
        val success = sendTelemetryInternal(status, SignalingPriority.HIGH)
        if (isTrackerMode) {
            // R-ID 453/565: Standardized Role Identity Authority
            mainRepository.saveTrackerState(status, AppRole.TRACKER)
            if (!success) {
                val entity = TelemetryMapper.mapStatusToPending(status)
                offlineRepository.addPendingStatusUpdate(entity)
                forensicLogger.logTransmissionFailure("Telemetry high-priority drop", "TRK", deviceId, viewerId)
            }
        } else {
            if (!success) {
                forensicLogger.logTransmissionFailure("Viewer telemetry drop", "VWR", deviceId, viewerId)
            }
        }
        return success
    }

    private fun sendTelemetryInternal(status: TrackerStatus, priority: SignalingPriority): Boolean {
        if (!isConnected()) return false
        if (hardwareSuite.shouldDeferSignaling(sessionManager.isInViolation)) return false
        signalingProvider.transmit(status, priority, fromViewer = !isTrackerMode)
        return true
    }

    private fun initializePeerState() {
        scope.launch {
            remoteStatusRepository.initialize()
        }
    }

    private fun handleBinaryUpdate(data: ByteArray) {
        if (isStopped.get()) return
        try {
            val statusProto = RealtimeStatus.parseFrom(data)
            
            if (!SignalingValidator.shouldProcessLocationUpdate(
                    incomingId = statusProto.id,
                    ownDeviceId = deviceId,
                    isFromViewer = statusProto.fromViewer,
                    viewerId = statusProto.viewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) {
                val reason = SignalingValidator.getDropReason(statusProto.id, deviceId, statusProto.fromViewer, statusProto.viewerId, viewerId, isTrackerMode)
                forensicLogger.logDrop("Binary", reason, statusProto.id, statusProto.viewerId, if (isTrackerMode) "TRK" else "VWR", deviceId, viewerId)
                return
            }

            val now = timeProvider.currentTimeMillis()
            val nowRt = timeProvider.elapsedRealtime()
            val peerId = statusProto.id

            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt)
            remoteStatusRepository.setTrackerConnected(true)
            mainRepository.updateRemoteActivity(nowRt) 
            
            remoteStatusRepository.setPeerSignal((statusProto.snrIdx * 10.0).toInt().coerceIn(0, 10))

            // Issue #1410: Synchronization of global acknowledgment
            if (!isTrackerMode && statusProto.lastAlarmAckTs > 0) {
                val currentAck = mainRepository.getLastAlarmAckTsSync(AppRole.VIEWER_REMOTE)
                if (statusProto.lastAlarmAckTs > currentAck) {
                    mainRepository.saveLongSync(AppRole.VIEWER_REMOTE, LAST_ALARM_ACK_TS_KEY, statusProto.lastAlarmAckTs)
                }
            }

            remoteStatusRepository.updateStatusAtomic { current ->
                TelemetryMapper.mapProtoToSnapshot(statusProto, now, nowRt, snapshotFlyweight)

                val processed = locationProcessor.processGpsPoint(
                    update = snapshotFlyweight, // Unified DTO
                    isViewerTrail = false,
                    lastGpsTs = current.gpsTs,
                    isLocal = false
                )
                
                val lastFixRt = if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0) nowRt else statusProto.lastValidFixRt
                TelemetryMapper.mapProtoToStatus(statusProto, current, processed, now, lastFixRt, statusFlyweight)

                TelemetryMapper.mapStatusToUpdate(statusFlyweight, isMe = false, out = updateFlyweight)
                domainEventBus.emit(DomainEvent.PeerStatusReceived(updateFlyweight.copy()))
                
                statusFlyweight
            }
        } catch (e: Exception) {
            Timber.e(e, "Protobuf direct parse error")
        }
    }

    private fun handleRemoteLog(entry: LogEntry) {
        val nowRt = timeProvider.elapsedRealtime()
        mainRepository.addLog(entry)
        remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(nowRt)
    }

    private fun handleJsonUpdate(data: JSONObject) {
        val type = data.optString("type", "")
        val fromId = data.optString("id"); val fromViewerId = data.optString("viewer_id"); val fromViewer = data.optBoolean("from_viewer", false)
        val now = timeProvider.currentTimeMillis(); val nowRt = timeProvider.elapsedRealtime()
        val peerId = if (isTrackerMode) (if (fromViewerId.isNotEmpty()) fromViewerId else fromId) else fromId

        if (type == "remote_log") {
            if (!SignalingValidator.shouldProcessLogRelay(fromId, deviceId, fromViewerId, viewerId, isTrackerMode)) {
                val reason = SignalingValidator.getDropReason(fromId, deviceId, fromViewer, fromViewerId, viewerId, isTrackerMode) ?: "Unauthorized Log Relay"
                forensicLogger.logDrop("Log", reason, fromId, fromViewerId, if (isTrackerMode) "TRK" else "VWR", deviceId, viewerId)
                return
            }
            handleRemoteLog(LogEntry.fromJSONObject(data))
            return
        }

        // Issue #1410: Handle remote acknowledgment signal
        if (isTrackerMode && fromViewer && type == "acknowledge_alarm") {
            val ackTs = data.optLong("ack_ts", 0L)
            if (ackTs > 0) {
                val currentAck = mainRepository.getLastAlarmAckTsSync(AppRole.TRACKER)
                if (ackTs > currentAck) {
                    mainRepository.saveLongSync(AppRole.TRACKER, LAST_ALARM_ACK_TS_KEY, ackTs)
                    logManagerProvider.get().logServiceEvent("REMOTE ACTION: Alerts acknowledged by Viewer $fromViewerId", true)
                }
            }
            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(nowRt)
            return
        }

        if (!SignalingValidator.shouldProcessLocationUpdate(
                incomingId = fromId,
                ownDeviceId = deviceId,
                isFromViewer = fromViewer,
                viewerId = fromViewerId,
                ownViewerId = viewerId,
                isTrackerMode = isTrackerMode
        )) {
            val isPulse = (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity")
            val reason = SignalingValidator.getDropReason(fromId, deviceId, fromViewer, fromViewerId, viewerId, isTrackerMode)
            
            if (!isPulse || (reason != null && !reason.contains("Echo suppression"))) {
                forensicLogger.logDrop("JSON", reason, fromId, fromViewerId, if (isTrackerMode) "TRK" else "VWR", deviceId, viewerId, extra = type)
            }
            return
        }

        if (isTrackerMode && fromViewer && type == "calibrate_chair") {
            locationProcessor.resetChairBaseline()
            mainRepository.addLog(LogEntry(
                timestamp = now,
                message = "REMOTE CALIBRATION: Chair baseline zeroed via viewer command",
                type = "event",
                isImportant = true
            ))
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, "REMOTE: Chair Baseline Zeroed", Toast.LENGTH_SHORT).show() }
            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); mainRepository.updateRemoteActivity(nowRt)
            return
        }

        if (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity") {
            if (!isTrackerMode && !fromViewer) {
                remoteStatusRepository.setTrackerConnected(true)
            }
            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt)
            mainRepository.updateRemoteActivity(nowRt)
            return
        }

        if (isTrackerMode && fromViewer) {
            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); remoteStatusRepository.setTrackerConnected(true); mainRepository.updateRemoteActivity(nowRt); return
        }

        if (!isTrackerMode && !fromViewer) {
            val remoteTs = data.optLong("ts", 0L)
            if (!remoteStatusRepository.shouldProcessPacket(remoteTs)) return

            domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
            remoteStatusRepository.updatePeerActivity(nowRt); remoteStatusRepository.setTrackerConnected(true); mainRepository.updateRemoteActivity(nowRt)
            remoteStatusRepository.setPeerSignal(data.optInt("signal", 0))

            // Issue #1410: Global acknowledgement synchronization via JSON
            val remoteAck = data.optLong("last_alarm_ack_ts", 0L)
            if (remoteAck > 0) {
                val currentAck = mainRepository.getLastAlarmAckTsSync(AppRole.VIEWER_REMOTE)
                if (remoteAck > currentAck) {
                    mainRepository.saveLongSync(AppRole.VIEWER_REMOTE, LAST_ALARM_ACK_TS_KEY, remoteAck)
                }
            }

            remoteStatusRepository.updateStatusAtomic { current ->
                TelemetryMapper.mapJsonToSnapshot(data, current, now, nowRt, snapshotFlyweight)

                val processed = locationProcessor.processGpsPoint(
                    update = snapshotFlyweight, // Unified DTO
                    isViewerTrail = false,
                    lastGpsTs = current.gpsTs,
                    isLocal = false
                )

                val lastFixRt = if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0 && !processed.isStalled) nowRt else current.lastValidFixRt
                
                var gnssDetail = current.gnssDetail
                if (data.has("gnss_detail")) {
                    try {
                        val array = data.getJSONArray("gnss_detail")
                        val satList = mutableListOf<SatelliteInfo>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            satList.add(SatelliteInfo(svid = obj.getInt("svid"), cn0 = obj.optDouble("cn0", 0.0), usedInFix = obj.getBoolean("used_in_fix"), constellation = obj.optInt("constellation", 0)))
                        }
                        gnssDetail = GnssDetail(satellites = satList)
                    } catch (e: Exception) { Timber.e(e, "GNSS detail parse error") }
                }

                TelemetryMapper.mapJsonToStatus(data, current, processed, now, lastFixRt, gnssDetail, statusFlyweight)

                TelemetryMapper.mapStatusToUpdate(statusFlyweight, isMe = false, out = updateFlyweight)
                domainEventBus.emit(DomainEvent.PeerStatusReceived(updateFlyweight.copy()))

                statusFlyweight
            }
        }
    }

    fun resetPeerStats() {
        remoteStatusRepository.reset()
        mainRepository.updateRemoteActivity(0L) 
        trackerGpsStallStartTs = 0L
        
        // R-ID 453/565: Standardized Role Identity Authority
        val role = if (isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_REMOTE
        mainRepository.saveDoubleSync(role, TRACKER_LUX_BASELINE_KEY, 0.0)
        mainRepository.saveDoubleSync(role, TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
    }

    fun stop() { 
        if (!isStarted.getAndSet(false)) return
        isStopped.set(true)
        val stopStartTime = timeProvider.elapsedRealtime()
        Timber.i("ConnectivitySuite: Starting teardown sequence (R-ID 197).")

        resetPeerStats()

        telemetryRepository.updateRelayStatus(false)
        telemetryRepository.updateLastRtt(0)
        
        keepAliveJob?.cancel(); keepAliveJob = null
        syncJob?.cancel(); syncJob = null
        signalingJob?.cancel(); signalingJob = null
        identitySyncJob?.cancel(); identitySyncJob = null
        heartbeatJob?.cancel(); heartbeatJob = null
        scope.cancel()
        
        networkProvider.registerListener(networkListener)
        
        val sigStart = timeProvider.elapsedRealtime()
        signalingProvider.disconnect() 
        val sigDuration = timeProvider.elapsedRealtime() - sigStart
        
        val totalDuration = timeProvider.elapsedRealtime() - stopStartTime
        Timber.i("""
            ConnectivitySuite: Teardown Summary (Issue #197 Verification):
            - Total Teardown Time: ${totalDuration}ms
            - Signaling Disconnect Duration: ${sigDuration}ms
            - Status: Clean Teardown Completed.
        """.trimIndent())
    }

    fun isConnected() = signalingProvider.isConnected()
    fun getRtt() = signalingProvider.getRtt()
    fun clearRtt() = signalingProvider.clearRtt()
    
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL) { 
        if (!isStopped.get()) signalingProvider.emit(event, data, priority) 
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
                reconnectAttempt = 0
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }
    }

    fun executeFlappingStressTest() {
        scope.launch(Dispatchers.Default) {
            domainEventBus.emit(DomainEvent.ServiceStatus("NETWORK STRESS TEST: Initiating 10s Signaling Flapping Burst.", isImportant = true))
            val start = timeProvider.elapsedRealtime()
            var count = 0
            while (timeProvider.elapsedRealtime() - start < 10000) {
                if (signalingProvider.isConnected()) {
                    signalingProvider.disconnect()
                } else {
                    signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                }
                count++
                delay(200) 
            }
            domainEventBus.emit(DomainEvent.ServiceStatus("NETWORK STRESS TEST: Flapping burst complete ($count transitions).", isImportant = true))
            
            if (!signalingProvider.isConnected()) {
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            }
        }
    }
}
