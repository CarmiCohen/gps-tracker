package com.gps19.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * ConnectivitySuite: Unified connectivity, telemetry sync, and remote peer handling.
 * Merges AppNetworkManager, SyncManager, and RemoteHandler.
 * v9.5.0: Issue #513 - Flatten Service Architecture.
 */
class ConnectivitySuite(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val telemetryRepository: TelemetryRepository,
    private val logManager: () -> LogManager,
    private val timeProvider: TimeProvider,
    private val signalingProvider: SignalingProvider,
    private val sessionManager: SessionManager,
    private val gpsManager: GpsManager,
    private val locationProcessor: LocationProcessor,
    private val offlineRepository: OfflineRepository,
    private val mainRepository: MainRepository,
    private val alarmManager: AppAlarmManager,
    private val forensicUseCase: ServiceForensicUseCase
) {
    interface PeerListener {
        fun onPeerPulse(id: String)
    }

    private var peerListener: PeerListener? = null
    fun setPeerListener(listener: PeerListener) { this.peerListener = listener }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val isStopped = AtomicBoolean(false)
    private val consecutiveHttpFailures = AtomicInteger(0)
    
    private var relayUrl = ""
    private var deviceId = ""
    private var viewerId = ""
    private var isTrackerMode = true
    private var lastReconnectTs = timeProvider.elapsedRealtime()

    private val suiteExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped.get()) return@CoroutineExceptionHandler
        Timber.e(throwable, "ConnectivitySuite CRITICAL error")
        logManager().logServiceEvent("CRITICAL: ConnectivitySuite failure: ${throwable.message}", true)
        restartLoops()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + suiteExceptionHandler)
    private var keepAliveJob: Job? = null
    private var syncJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // Remote Peer State (Merged from RemoteHandler)
    var isTrackerConnected = false
    var lastPeerActivityTs = 0L 
    var peerSignal = 0
    var lastPeerGpsTs = 0L
    private var lastRemotePacketTs = 0L

    var trackerLat = 0.0; var trackerLng = 0.0; var trackerSpeed = 0.0; var trackerBearing = 0.0
    var trackerAccuracy = 0.0; var trackerMaxAccuracy = 0.0; var trackerLastGpsTs = 0L
    var trackerLastValidFixRealtime = 0L; var trackerBattery = 0; var trackerTemp = 0.0
    var trackerMaxTemp = 0.0; var trackerCurrentMa = 0; var trackerSatsView = 0; var trackerSatsUsed = 0
    var isTrackerCharging = false; var isTrackerJammerSuspicion = false; var isTrackerVisualJump = false
    var trackerJumpTier = 0; var trackerStatus = SentinelStatus.VALID
    var isTrackerTamperDetected = false; var isTrackerPowerTamper = false
    var isTrackerClockRegression = false; var isTrackerLocationPending = false
    var trackerLocationPendingReason = LocationPendingReason.NONE
    var trackerLocationDetail: GnssDetail? = null
    var isTrackerBatterySteepDischarge = false; var isTrackerCoolingModeActive = false
    var isTrackerPowerSaveMode = false; var trackerStandbyBucket = -1
    var trackerNetInterface = "UNKNOWN"; var isTrackerStorageLow = false; var isTrackerStorageCritical = false
    var trackerState = TrackerState.UNKNOWN
    var trackerDistToHome: Double? = null; var trackerDistToViewer: Double? = null
    var trackerVibration = 0.0; var trackerHeading = 0.0; var trackerBaroAlt = 0.0; var trackerLux = 0.0
    var isTrackerNear = true; var trackerTiltDegrees = 0.0; var trackerAcousticDb = 0.0
    var trackerPeakVibrationShock = 0.0; var trackerPeakVibrationShockTs = 0L
    var trackerLuxBaseline = 0.0; var trackerAcousticFloorDb = 0.0
    var trackerAdaptiveVibrationFloor = 0.12; var trackerProxIdx = 1.0; var trackerProximityCm = -1.0
    var trackerProximityDebounceMs = 0L; var trackerVibrationRollingSum = 0.0
    var trackerUptimeMs = 0L; var trackerTotalDropMs = 0L; var trackerMaxDropMs = 0L; var trackerMaxDropTs = 0L
    var trackerTotalConnectedMs = 0L; var trackerSessionConnectedMs = 0L; var trackerLastConnTs = 0L; var trackerLastDiscTs = 0L
    var trackerGpsStallStartTs = 0L 

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isStopped.get() || relayUrl.isEmpty()) return
            scope.launch {
                val now = timeProvider.elapsedRealtime()
                if (now - lastReconnectTs < 3000L || signalingProvider.isConnected()) return@launch
                if (!SignalingConstants.isValidTrackerId(deviceId) || !SignalingConstants.isValidViewerId(viewerId)) return@launch

                logManager().logServiceEvent("Network Handover: Interface Available. Reconnecting.", false)
                lastReconnectTs = now
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }
        override fun onLost(network: Network) {
            if (isStopped.get()) return
            logManager().logServiceEvent("Network Handover: Interface Lost.", false)
            telemetryRepository.updateRelayStatus(false)
        }
    }

    fun start(url: String, dId: String, vId: String, isTracker: Boolean) {
        isStopped.set(false)
        this.relayUrl = url; this.deviceId = dId; this.viewerId = vId; this.isTrackerMode = isTracker
        this.lastReconnectTs = timeProvider.elapsedRealtime()
        
        try {
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) { Timber.e("Failed to register network callback") }

        signalingProvider.setConnectionLostCallback {
            if (!isStopped.get() && relayUrl.isNotEmpty()) {
                val now = timeProvider.elapsedRealtime()
                if (now - lastReconnectTs > 10000L) {
                    lastReconnectTs = now
                    wakeUpRelay()
                }
            }
        }

        if (relayUrl.isNotEmpty() && SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
            signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            wakeUpRelay()
        }

        startKeepAliveLoop()
        startSyncLoop()
        initializePeerState()
    }

    private fun restartLoops() {
        if (isStopped.get()) return
        startKeepAliveLoop()
        startSyncLoop()
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
                withContext(Dispatchers.Main) {
                    lastReconnectTs = timeProvider.elapsedRealtime()
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

            val now = timeProvider.elapsedRealtime()
            if (signalingProvider.isConnected()) {
                if (now - signalingProvider.getLastRelayTrafficTs() > NET_REJOIN_THRESHOLD_MS) {
                    withContext(Dispatchers.Main) {
                        signalingProvider.updateIdentity(deviceId, viewerId, isTrackerMode, force = true)
                        wakeUpRelay()
                    }
                }
            } else if (now - lastReconnectTs > NET_REJOIN_THRESHOLD_MS) {
                withContext(Dispatchers.Main) {
                    lastReconnectTs = now
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
            repeat(4) { attempt ->
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
                lastValidFixRealtime = entity.lastValidFixRealtime, isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive, battery = entity.battery, temp = entity.temp, isCharging = entity.isCharging,
                trackerState = try { TrackerState.valueOf(entity.trackerState) } catch(e: Exception) { TrackerState.UNKNOWN }
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
                    isStorageLow = telemetryRepository.integrityState.value.isStorageLow, isStorageCritical = telemetryRepository.integrityState.value.isStorageCritical,
                    isPowerSaveMode = telemetryRepository.integrityState.value.isPowerSaveMode, standbyBucket = status.standbyBucket,
                    netInterface = status.netInterface, lastValidFixRealtime = status.lastValidFixRealtime,
                    locationPendingReason = status.locationPendingReason.name, trackerState = status.trackerState.name, status = status.status.name
                ))
            }
        }
        return success
    }

    private fun sendTelemetryInternal(status: TrackerStatus): Boolean {
        if (!isConnected()) return false
        if (isTrackerMode) {
            signalingProvider.emitBinary("location_update_bin", SignalingConstants.getTransmissionId(deviceId), status.toProto(false).toByteArray())
        } else {
            signalingProvider.emit("location_update", status.toJSONObject(true))
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
            locationPendingReason = locationPendingReason, lastValidFixRealtime = lastValidFixRealtime,
            isBatterySteepDischarge = isBatterySteepDischarge, isCoolingModeActive = isCoolingModeActive,
            gnssDetail = gnssDetail, battery = batteryLevel, temp = batteryTemp, isCharging = isCharging,
            trackerState = trackerState
        )
        sendTelemetry(trackerStatus)
    }

    private fun initializePeerState() {
        scope.launch {
            try {
                trackerLuxBaseline = mainRepository.getDouble(MainRepository.TRACKER_LUX_BASELINE_KEY, 0.0)
                trackerAcousticFloorDb = mainRepository.getDouble(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
                mainRepository.loadTrackerState()?.let { s ->
                    applyPeerStatus(s)
                    mainRepository.updateLocation(LocationUpdate(
                        lat = s.lat, lng = s.lng, speed = s.speed, accuracy = s.accuracy, bearing = s.bearing,
                        battery = s.battery, temp = s.temp, maxTemp = s.maxTemp, isCharging = s.isCharging, currentMa = s.currentMa,
                        gpsTs = s.gpsTs, isMe = false, satsView = s.satsView, satsUsed = s.satsUsed,
                        maxAccuracy = s.maxAccuracy, signal = 0, vibration = s.vibration, heading = s.bearing, baroAlt = s.baroAlt,
                        lux = s.lux, isNear = s.isNear, tiltDegrees = s.tiltDegrees, acousticDb = s.acousticDb,
                        peakVibrationShock = s.peakVibrationShock, peakVibrationShockTs = s.peakVibrationShockTs,
                        luxBaseline = s.luxBaseline, acousticFloorDb = s.acousticFloorDb, adaptiveVibrationFloor = s.adaptiveVibrationFloor, 
                        status = s.status, isTamperDetected = s.isTamperDetected, isPowerTamper = s.isPowerTamper,
                        proxIdx = s.proxIdx, proximityCm = s.proximityCm, proximityDebounceMs = s.proximityDebounceMs,
                        vibrationRollingSum = s.vibrationRollingSum, uptimeMs = s.uptimeMs, totalDropMs = s.totalDropMs,
                        maxDropMs = s.maxDropMs, maxDropTs = s.maxDropTs, totalConnectedMs = s.totalConnectedMs,
                        sessionConnectedMs = s.sessionConnectedMs, lastConnTs = s.lastConnTs, lastDiscTs = s.lastDiscTs,
                        violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
                        isLocationPending = s.isLocationPending, locationPendingReason = s.locationPendingReason,
                        lastValidFixRealtime = s.lastValidFixRealtime, isPowerSaveMode = s.isPowerSaveMode,
                        standbyBucket = s.standbyBucket, netInterface = s.netInterface, isStorageLow = s.isStorageLow,
                        isStorageCritical = s.isStorageCritical, gnssDetail = s.gnssDetail,
                        isBatterySteepDischarge = s.isBatterySteepDischarge, isCoolingModeActive = s.isCoolingModeActive,
                        trackerState = s.trackerState, ts = s.ts 
                    ))
                }
            } catch (e: Exception) { Timber.e(e, "Peer state init failed") }
        }
    }

    private fun applyPeerStatus(s: TrackerStatus) {
        trackerLat = s.lat; trackerLng = s.lng; trackerSpeed = s.speed; trackerBearing = s.bearing
        trackerAccuracy = s.accuracy; trackerMaxAccuracy = s.maxAccuracy; trackerLastGpsTs = s.gpsTs; trackerBattery = s.battery
        trackerTemp = s.temp; trackerMaxTemp = s.maxTemp; trackerCurrentMa = s.currentMa
        isTrackerCharging = s.isCharging; trackerSatsView = s.satsView; trackerSatsUsed = s.satsUsed
        trackerJumpTier = s.jumpTier; trackerStatus = s.status; isTrackerTamperDetected = s.isTamperDetected
        isTrackerPowerTamper = s.isPowerTamper; trackerVibration = s.vibration; trackerHeading = s.heading
        trackerBaroAlt = s.baroAlt; trackerLux = s.lux; isTrackerNear = s.isNear; trackerTiltDegrees = s.tiltDegrees
        trackerAcousticDb = s.acousticDb; trackerPeakVibrationShock = s.peakVibrationShock
        trackerPeakVibrationShockTs = s.peakVibrationShockTs; trackerLuxBaseline = s.luxBaseline
        trackerAcousticFloorDb = s.acousticFloorDb; trackerAdaptiveVibrationFloor = s.adaptiveVibrationFloor
        trackerProxIdx = s.proxIdx; trackerProximityCm = s.proximityCm; trackerProximityDebounceMs = s.proximityDebounceMs
        trackerVibrationRollingSum = s.vibrationRollingSum; trackerUptimeMs = s.uptimeMs; trackerTotalDropMs = s.totalDropMs
        trackerMaxDropMs = s.maxDropMs; trackerMaxDropTs = s.maxDropTs; trackerTotalConnectedMs = s.totalConnectedMs
        trackerSessionConnectedMs = s.sessionConnectedMs; trackerLastConnTs = s.lastConnTs; trackerLastDiscTs = s.lastDiscTs
        isTrackerLocationPending = s.isLocationPending; trackerLocationPendingReason = s.locationPendingReason
        trackerLocationDetail = s.gnssDetail; isTrackerBatterySteepDischarge = s.isBatterySteepDischarge
        isTrackerCoolingModeActive = s.isCoolingModeActive; isTrackerPowerSaveMode = s.isPowerSaveMode
        trackerStandbyBucket = s.standbyBucket; trackerNetInterface = s.netInterface
        isTrackerStorageLow = s.isStorageLow; isTrackerStorageCritical = s.isStorageCritical
        trackerState = s.trackerState; trackerLastValidFixRealtime = s.lastValidFixRealtime
    }

    fun handleRemoteUpdate(data: JSONObject) {
        val type = data.optString("type", "")
        if (type == "remote_log") {
            handleRemoteLog(LogEntry.fromJSONObject(data))
            return
        }

        val fromId = data.optString("id"); val fromViewerId = data.optString("viewer_id"); val fromViewer = data.optBoolean("from_viewer", false)
        val now = timeProvider.currentTimeMillis(); val nowRealtime = timeProvider.elapsedRealtime()
        val peerId = if (isTrackerMode) (if (fromViewerId.isNotEmpty()) fromViewerId else fromId) else fromId

        if (isTrackerMode && fromViewer && data.has("home_points")) {
            scope.launch {
                try {
                    val array = data.getJSONArray("home_points"); val newList = mutableListOf<org.osmdroid.util.GeoPoint>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i); newList.add(org.osmdroid.util.GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
                    }
                    mainRepository.saveHomePoints(newList, data.optDouble("max_dist", -1.0).takeIf { it > 0 }, data.optLong("settings_ts", 0L).takeIf { it > 0 })
                    peerListener?.onPeerPulse(peerId); lastPeerActivityTs = nowRealtime; mainRepository.updateRemoteActivity(now)
                } catch (e: Exception) { Timber.e(e, "Remote settings parse error") }
            }
            return
        }

        if (type == "viewer_pulse" || type == "tracker_pulse" || type == "pong_activity") {
            if ((isTrackerMode && fromViewer) || (!isTrackerMode && !fromViewer)) {
                peerListener?.onPeerPulse(peerId); lastPeerActivityTs = nowRealtime; isTrackerConnected = !isTrackerMode; mainRepository.updateRemoteActivity(now)
            }
            return
        }

        if (isTrackerMode && fromViewer) {
            peerListener?.onPeerPulse(peerId); lastPeerActivityTs = nowRealtime; mainRepository.updateRemoteActivity(now); return
        }

        if (!isTrackerMode && !fromViewer) {
            val remoteTs = data.optLong("ts", 0L)
            if (remoteTs > 0 && remoteTs < lastRemotePacketTs) return
            if (remoteTs > 0) lastRemotePacketTs = remoteTs

            peerListener?.onPeerPulse(peerId); lastPeerActivityTs = nowRealtime; isTrackerConnected = true; mainRepository.updateRemoteActivity(now)
            peerSignal = data.optInt("signal", 0)

            val statusStr = data.optString("status", SentinelStatus.VALID.name)
            trackerStatus = try { SentinelStatus.valueOf(statusStr) } catch(e: Exception) { SentinelStatus.VALID }
            isTrackerTamperDetected = data.optBoolean("is_tamper_detected", isTrackerTamperDetected)
            isTrackerPowerTamper = data.optBoolean("is_power_tamper", isTrackerPowerTamper)
            isTrackerLocationPending = data.optBoolean("is_location_pending", false)
            trackerLocationPendingReason = try { LocationPendingReason.valueOf(data.optString("location_pending_reason", "NONE")) } catch(e: Exception) { LocationPendingReason.NONE }
            trackerLastValidFixRealtime = data.optLong("last_valid_fix_realtime", trackerLastValidFixRealtime)
            isTrackerBatterySteepDischarge = data.optBoolean("is_battery_steep_discharge", false)
            isTrackerCoolingModeActive = data.optBoolean("is_cooling_mode_active", false)
            isTrackerPowerSaveMode = data.optBoolean("is_power_save_mode", isTrackerPowerSaveMode)
            trackerStandbyBucket = data.optInt("standard_bucket", trackerStandbyBucket)
            trackerNetInterface = data.optString("net_interface", trackerNetInterface)
            isTrackerStorageLow = data.optBoolean("is_storage_low", isTrackerStorageLow)
            isTrackerStorageCritical = data.optBoolean("is_storage_critical", isTrackerStorageCritical)
            trackerState = try { TrackerState.valueOf(data.optString("tracker_state", "UNKNOWN")) } catch(e: Exception) { TrackerState.UNKNOWN }

            if (data.has("gnss_detail")) {
                try {
                    val array = data.getJSONArray("gnss_detail"); val satList = mutableListOf<SatelliteInfo>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i); satList.add(SatelliteInfo(svid = obj.getInt("svid"), cn0 = obj.optDouble("cn0", 0.0), usedInFix = obj.getBoolean("used_in_fix"), constellation = obj.optInt("constellation", 0)))
                    }
                    trackerLocationDetail = GnssDetail(satellites = satList)
                } catch (e: Exception) { Timber.e(e, "GNSS detail parse error") }
            }

            if (data.has("lat") || data.has("gps_ts") || data.has("gps_age_ms")) {
                val incomingGpsTs = data.optLong("gps_ts", 0L)
                val gpsAgeMs = if (data.has("gps_age_ms")) data.optLong("gps_age_ms") else (if (incomingGpsTs > 0) maxOf(0L, now - incomingGpsTs) else 0L)
                val candidateTs = if (gpsAgeMs > 0 || incomingGpsTs > 0) now - gpsAgeMs else 0L
                val rawLat = data.optDouble("lat", 0.0); val rawLng = data.optDouble("lng", 0.0)
                val rawSpeed = data.optDouble("speed", -1.0); val rawBearing = data.optDouble("bearing", 0.0)
                val rawAcc = data.optDouble("accuracy", 0.0); val rawMaxAcc = data.optDouble("max_accuracy", 0.0)

                val processed = locationProcessor.processGpsPoint(
                    lat = rawLat, lng = rawLng, alt = data.optDouble("alt", 0.0), androidSpeedMps = if (rawSpeed >= 0.0) rawSpeed else 0.0,
                    gpsTs = candidateTs, accuracy = if (rawAcc > 0.0) rawAcc else 0.0, bearing = rawBearing, snr = 0.0,
                    satsUsed = data.optInt("sats_used", trackerSatsUsed), isViewerTrail = false, lastGpsTs = trackerLastGpsTs,
                    providedMaxAccuracy = rawMaxAcc, providedJumpTier = data.optInt("jump_tier", 0), providedIsJammer = data.optBoolean("is_jammer", false),
                    providedIsStalled = data.optBoolean("is_stalled", false), providedIsTamper = isTrackerTamperDetected || isTrackerLocationPending || trackerStatus == SentinelStatus.TAMPER,
                    nowWall = now, nowRealtime = nowRealtime
                )
                isTrackerClockRegression = processed.isClockRegression
                if (processed.optimizedPoint.lat != 0.0 && processed.optimizedPoint.lng != 0.0) {
                    trackerLat = processed.optimizedPoint.lat; trackerLng = processed.optimizedPoint.lng; trackerLastGpsTs = processed.optimizedPoint.ts; lastPeerGpsTs = trackerLastGpsTs
                    if (!processed.isStalled) trackerLastValidFixRealtime = nowRealtime
                }
                trackerSpeed = processed.filteredSpeed; trackerBearing = rawBearing
                if (rawAcc > 0.0) trackerAccuracy = rawAcc
                if (rawMaxAcc > 0.0) trackerMaxAccuracy = rawMaxAcc
                trackerSatsView = data.optInt("sats_view", trackerSatsView); trackerSatsUsed = data.optInt("sats_used", trackerSatsUsed)
                isTrackerJammerSuspicion = data.optBoolean("is_jammer", false); isTrackerVisualJump = processed.status == SentinelStatus.JUMP; trackerJumpTier = data.optInt("jump_tier", 0)
            }
            
            trackerBattery = data.optInt("battery", trackerBattery); trackerTemp = data.optDouble("temp", trackerTemp); trackerMaxTemp = data.optDouble("max_temp", trackerMaxTemp)
            trackerCurrentMa = data.optInt("current_ma", trackerCurrentMa); isTrackerCharging = data.optBoolean("is_charging", isTrackerCharging)
            trackerVibration = data.optDouble("vibration", trackerVibration); trackerHeading = data.optDouble("heading", trackerHeading)
            trackerBaroAlt = data.optDouble("baro_alt", trackerBaroAlt); trackerLux = data.optDouble("lux", trackerLux); isTrackerNear = data.optBoolean("is_near", isTrackerNear)
            trackerProxIdx = data.optDouble("prox_idx", trackerProxIdx); trackerProximityCm = data.optDouble("proximity_cm", trackerProximityCm)
            trackerProximityDebounceMs = data.optLong("proximity_debounce_ms", trackerProximityDebounceMs); trackerVibrationRollingSum = data.optDouble("vibration_rolling_sum", trackerVibrationRollingSum)
            trackerTiltDegrees = data.optDouble("tilt_degrees", trackerTiltDegrees); trackerAcousticDb = data.optDouble("acoustic_db", trackerAcousticDb)
            trackerPeakVibrationShock = data.optDouble("peak_vibration_shock", trackerPeakVibrationShock); trackerPeakVibrationShockTs = data.optLong("peak_shock_ts", trackerPeakVibrationShockTs)
            
            val newLuxBaseline = data.optDouble("lux_baseline", trackerLuxBaseline)
            if (newLuxBaseline != trackerLuxBaseline) { trackerLuxBaseline = newLuxBaseline; mainRepository.saveDoubleSync(MainRepository.TRACKER_LUX_BASELINE_KEY, trackerLuxBaseline) }
            val newAcousticFloor = data.optDouble("acoustic_floor_db", trackerAcousticFloorDb)
            if (newAcousticFloor != trackerAcousticFloorDb) { trackerAcousticFloorDb = newAcousticFloor; mainRepository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, trackerAcousticFloorDb) }
            trackerAdaptiveVibrationFloor = data.optDouble("adaptive_vibration_floor", trackerAdaptiveVibrationFloor)
            
            locationProcessor.sentinel.updateSensorState(
                vibration = trackerVibration, heading = trackerHeading, baroAlt = trackerBaroAlt, lux = trackerLux, isNear = isTrackerNear,
                powerTamper = isTrackerPowerTamper, tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb, peakShock = trackerPeakVibrationShock,
                acousticMinDb = -1.0, nowRealtime = nowRealtime, nowWall = now
            )

            trackerUptimeMs = data.optLong("uptime_ms", trackerUptimeMs); trackerTotalDropMs = data.optLong("total_drop_ms", trackerTotalDropMs)
            trackerMaxDropMs = data.optLong("max_drop_ms", trackerMaxDropMs); trackerMaxDropTs = data.optLong("max_drop_ts", trackerMaxDropTs)
            trackerTotalConnectedMs = data.optLong("total_connected_ms", trackerTotalConnectedMs); trackerSessionConnectedMs = data.optLong("session_connected_ms", trackerSessionConnectedMs)
            trackerLastConnTs = data.optLong("last_conn_ts", trackerLastConnTs); trackerLastDiscTs = data.optLong("last_disc_ts", trackerLastDiscTs)
            
            val violationUptimeMs = data.optLong("violation_uptime_ms", 0L); val violationPercentage = data.optDouble("violation_percentage", 0.0)
            if (data.optBoolean("is_stalled", false) && trackerGpsStallStartTs == 0L) trackerGpsStallStartTs = nowRealtime else if (!data.optBoolean("is_stalled", false)) trackerGpsStallStartTs = 0L

            scope.launch {
                try {
                    val locationUpdate = LocationUpdate(
                        lat = trackerLat, lng = trackerLng, speed = trackerSpeed, accuracy = trackerAccuracy, bearing = trackerBearing,
                        battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp, isCharging = isTrackerCharging, currentMa = trackerCurrentMa,
                        gpsTs = trackerLastGpsTs, isMe = false, satsView = trackerSatsView, satsUsed = trackerSatsUsed, jumpTier = trackerJumpTier, 
                        status = trackerStatus, isTamperDetected = isTrackerTamperDetected, isPowerTamper = isTrackerPowerTamper,
                        violationUptimeMs = violationUptimeMs, violationPercentage = violationPercentage, isClockRegression = isTrackerClockRegression,
                        isLocationPending = isTrackerLocationPending, locationPendingReason = trackerLocationPendingReason,
                        lastValidFixRealtime = trackerLastValidFixRealtime, isPowerSaveMode = isTrackerPowerSaveMode, standbyBucket = trackerStandbyBucket,
                        netInterface = trackerNetInterface, isStorageLow = isTrackerStorageLow, isStorageCritical = isTrackerStorageCritical,
                        gnssDetail = trackerLocationDetail, isBatterySteepDischarge = isTrackerBatterySteepDischarge, isCoolingModeActive = isTrackerCoolingModeActive,
                        trackerState = trackerState, ts = now
                    )
                    mainRepository.updateLocation(locationUpdate)
                    mainRepository.saveTrackerState(TrackerStatus(
                        lat = trackerLat, lng = trackerLng, speed = trackerSpeed, bearing = trackerBearing, accuracy = trackerAccuracy,
                        gpsTs = trackerLastGpsTs, ts = now, battery = trackerBattery, temp = trackerTemp, maxTemp = trackerMaxTemp,
                        isCharging = isTrackerCharging, currentMa = trackerCurrentMa, satsView = trackerSatsView, satsUsed = trackerSatsUsed,
                        lastConnTs = trackerLastConnTs, lastDiscTs = trackerLastDiscTs, uptimeMs = trackerUptimeMs, totalConnectedMs = trackerTotalConnectedMs,
                        sessionConnectedMs = trackerSessionConnectedMs, totalDropMs = trackerTotalDropMs, maxDropMs = trackerMaxDropMs, maxDropTs = trackerMaxDropTs,
                        violationUptimeMs = violationUptimeMs, violationPercentage = violationPercentage, isPowerTamper = isTrackerPowerTamper, 
                        vibration = trackerVibration, heading = trackerHeading, baroAlt = trackerBaroAlt, lux = trackerLux, isNear = isTrackerNear, 
                        tiltDegrees = trackerTiltDegrees, acousticDb = trackerAcousticDb, peakVibrationShock = trackerPeakVibrationShock,
                        peakVibrationShockTs = trackerPeakVibrationShockTs, luxBaseline = trackerLuxBaseline, acousticFloorDb = trackerAcousticFloorDb, 
                        adaptiveVibrationFloor = trackerAdaptiveVibrationFloor, proxIdx = trackerProxIdx, proximityCm = trackerProximityCm,
                        proximityDebounceMs = trackerProximityDebounceMs, vibrationRollingSum = trackerVibrationRollingSum, status = trackerStatus, 
                        isTamperDetected = isTrackerTamperDetected, jumpTier = trackerJumpTier, isLocationPending = isTrackerLocationPending,
                        locationPendingReason = trackerLocationPendingReason, lastValidFixRealtime = trackerLastValidFixRealtime,
                        isPowerSaveMode = isTrackerPowerSaveMode, standbyBucket = trackerStandbyBucket, netInterface = trackerNetInterface,
                        isStorageLow = isTrackerStorageLow, isStorageCritical = isTrackerStorageCritical, gnssDetail = trackerLocationDetail,
                        isBatterySteepDischarge = isTrackerBatterySteepDischarge, isCoolingModeActive = isTrackerCoolingModeActive, trackerState = trackerState
                    ))
                } catch (e: Exception) { Timber.e(e, "Peer DB update failed") }
            }
        }
    }

    private fun handleRemoteLog(entry: LogEntry) {
        val now = timeProvider.currentTimeMillis(); val nowRealtime = timeProvider.elapsedRealtime()
        lastPeerActivityTs = nowRealtime; mainRepository.updateRemoteActivity(now)
    }

    fun onRelayLost() { isTrackerConnected = false }

    fun resetPeerStats() {
        isTrackerConnected = false; lastPeerActivityTs = 0L; lastRemotePacketTs = 0L; peerSignal = 0; lastPeerGpsTs = 0L
        trackerLat = 0.0; trackerLng = 0.0; trackerSpeed = 0.0; trackerBearing = 0.0; trackerAccuracy = 0.0; trackerMaxAccuracy = 0.0; trackerLastGpsTs = 0L
        trackerBattery = 0; trackerTemp = 0.0; trackerMaxTemp = 0.0; trackerCurrentMa = 0; trackerSatsView = 0; trackerSatsUsed = 0
        isTrackerCharging = false; isTrackerJammerSuspicion = false; isTrackerVisualJump = false; trackerJumpTier = 0; trackerStatus = SentinelStatus.VALID
        isTrackerTamperDetected = false; isTrackerPowerTamper = false; trackerDistToHome = null; trackerDistToViewer = null
        trackerVibration = 0.0; trackerHeading = 0.0; trackerBaroAlt = 0.0; trackerLux = 0.0; isTrackerNear = true; trackerTiltDegrees = 0.0
        trackerAcousticDb = 0.0; trackerPeakVibrationShock = 0.0; trackerPeakVibrationShockTs = 0L; trackerLuxBaseline = 0.0; trackerAcousticFloorDb = 0.0
        trackerAdaptiveVibrationFloor = 0.12; trackerProxIdx = 1.0; trackerProximityCm = -1.0; trackerProximityDebounceMs = 0L; trackerVibrationRollingSum = 0.0
        trackerUptimeMs = 0L; trackerTotalDropMs = 0L; trackerMaxDropMs = 0L; trackerMaxDropTs = 0L; trackerTotalConnectedMs = 0L
        trackerSessionConnectedMs = 0L; trackerLastConnTs = 0L; trackerLastDiscTs = 0L; trackerGpsStallStartTs = 0L; trackerLastValidFixRealtime = 0L
        isTrackerClockRegression = false; isTrackerLocationPending = false; trackerLocationPendingReason = LocationPendingReason.NONE; trackerLocationDetail = null
        isTrackerBatterySteepDischarge = false; isTrackerCoolingModeActive = false; isTrackerPowerSaveMode = false; trackerStandbyBucket = -1
        trackerNetInterface = "UNKNOWN"; isTrackerStorageLow = false; isTrackerStorageCritical = false; trackerState = TrackerState.UNKNOWN
        mainRepository.saveDoubleSync(MainRepository.TRACKER_LUX_BASELINE_KEY, 0.0); mainRepository.saveDoubleSync(MainRepository.TRACKER_ACOUSTIC_FLOOR_KEY, 0.0)
    }

    fun stop() { 
        isStopped.set(true); keepAliveJob?.cancel(); syncJob?.cancel(); scope.cancel()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        signalingProvider.disconnect() 
    }

    fun isConnected() = signalingProvider.isConnected()
    fun getRtt() = signalingProvider.getRtt()
    fun clearRtt() = signalingProvider.clearRtt()
    fun emit(event: String, data: JSONObject) { if (!isStopped.get()) signalingProvider.emit(event, data) }
    fun emitBinary(event: String, routingId: String, data: ByteArray) { if (!isStopped.get()) signalingProvider.emitBinary(event, routingId, data) }
    fun pushSettings() { if (!isStopped.get()) signalingProvider.pushSettings() }
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
            signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            wakeUpRelay()
        }
    }
}
