package com.gps19.app

import android.content.Context
import android.util.Log
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Socket.io implementation of the SignalingProvider.
 * v9.3.25:
 * - R988: Implemented routingId-aware emitBinary for optimized relay routing.
 * - Protocol Optimization: Updated handleLocationRelayBinary to map Protobuf 
 *   Enums (state, pending_reason) back to internal strings for app-wide compatibility.
 * - Forensic Audit: Added logging for binary emissions to verify R988 activation.
 */
class CommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val telemetryRepository: TelemetryRepository,
    private val logRepository: LogRepository,
    private val logManager: LogManager,
    private val timeProvider: TimeProvider,
    private val onRemoteUpdateWrapper: RemoteUpdateWrapper
) : SignalingProvider {

    private var socket: Socket? = null
    private var isStopped = false
    
    private var deviceId = ""
    private var viewerId = ""
    private var relayUrl = ""
    private var isTrackerMode = false
    
    private val rtts = mutableListOf<Int>()
    private var lastRttInternal = 0
    private var lastRelayTrafficTs = timeProvider.elapsedRealtime()

    private var onConnectionLost: (() -> Unit)? = null

    private val commExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped) return@CoroutineExceptionHandler
        Timber.e(throwable, "CRITICAL: Communication failure")
        logManager.logServiceEvent("CRITICAL: Communication failure: ${throwable.message}", true)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + commExceptionHandler)
    private var pendingLocationUpdate: JSONObject? = null
    private var conflationJob: Job? = null

    private fun isDefaultViewer(id: String) = id == SignalingConstants.DEFAULT_VIEWER_ID || id.isEmpty()

    private fun logToApp(message: String, important: Boolean = false) {
        if (isStopped) return
        Log.i("GPS19_COMM", message)
        logManager.submitToLogSink(message, "system", important)
    }

    override fun getLastRelayTrafficTs(): Long = lastRelayTrafficTs

    private fun markTraffic() {
        lastRelayTrafficTs = timeProvider.elapsedRealtime()
    }

    private fun createJoinPayload(): JSONObject {
        return JSONObject().apply {
            put("id", SignalingConstants.getTransmissionId(deviceId))
            put("role", if (isTrackerMode) "tracker" else "viewer")
            put("ver", BuildConfig.VERSION_NAME)
        }
    }

    override fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean) {
        val oldId = this.deviceId
        val cleanedDeviceId = deviceId.trim()
        val cleanedViewerId = viewerId.trim()

        if (isTracker && !SignalingConstants.isValidTrackerId(cleanedDeviceId)) {
            logToApp("Invalid Tracker ID: $cleanedDeviceId (Cannot be empty)", true)
            return
        }
        if (!isTracker && !SignalingConstants.isValidViewerId(cleanedViewerId)) {
            logToApp("Invalid Viewer ID: $cleanedViewerId (Cannot be empty)", true)
            return
        }

        val idChanged = oldId.isNotEmpty() && oldId != cleanedDeviceId
        
        this.deviceId = cleanedDeviceId
        this.viewerId = cleanedViewerId
        this.isTrackerMode = isTracker
        
        if ((idChanged || force) && isConnected()) {
            if (idChanged && oldId.isNotEmpty()) {
                val transOld = SignalingConstants.getTransmissionId(oldId)
                logToApp("Identity changed. Leaving $transOld", true)
                socket?.emit("leave", transOld)
            }
            if (this.deviceId.isNotEmpty()) {
                val transNew = SignalingConstants.getTransmissionId(this.deviceId)
                logToApp("Joining room: $transNew (Force: $force)", force)
                socket?.emit("join", createJoinPayload())
            }
        }
    }

    override fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean) {
        this.isStopped = false
        this.relayUrl = url.trim()
        this.deviceId = deviceId.trim()
        this.viewerId = viewerId.trim()
        this.isTrackerMode = isTracker
        
        if (isTracker && !SignalingConstants.isValidTrackerId(this.deviceId)) {
            logToApp("Connect aborted: Invalid Tracker ID", true)
            return
        }
        if (!isTracker && !SignalingConstants.isValidViewerId(this.viewerId)) {
            logToApp("Connect aborted: Invalid Viewer ID", true)
            return
        }

        socket?.disconnect()
        socket?.off()

        if (relayUrl.isEmpty()) {
            logToApp("Connect aborted: URL is empty", false)
            return
        }

        logToApp("Starting connection to $relayUrl", true)
        markTraffic() 

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            timeout = 45000 
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 2000 
            reconnectionDelayMax = 10000
            randomizationFactor = 0.5
        }

        try {
            socket = IO.socket(relayUrl, opts)
            registerSocketListeners()
            socket?.connect()
        } catch (e: Exception) {
            logToApp("Socket creation failed: ${e.message}", true)
        }
    }

    private fun registerSocketListeners() {
        val s = socket ?: return

        val onConnectAction = {
            logToApp("Connected to relay", true)
            markTraffic()
            if (deviceId.isNotEmpty()) {
                s.emit("join", createJoinPayload())
            }
            pushSettings()
            telemetryRepository.updateRelayStatus(true)
        }

        s.on(Socket.EVENT_CONNECT) { onConnectAction() }
        s.on("reconnecting") { logToApp("Relay Reconnecting...", true) }
        s.on("reconnect") {
            logToApp("Relay Reconnected", true)
            onConnectAction() 
        }
        
        s.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args?.getOrNull(0)?.toString() ?: "unknown"
            logToApp("Relay Disconnected ($reason)", true)
            telemetryRepository.updateRelayStatus(false)
            if (reason != "io client disconnect") {
                onConnectionLost?.invoke()
            }
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args?.getOrNull(0)?.toString() ?: "Transport Error"
            logToApp("Relay Connect Error: $error", true)
            telemetryRepository.updateRelayStatus(false)
            onConnectionLost?.invoke()
        }

        s.on("location_relay") { markTraffic(); handleLocationRelay(it) }
        s.on("location_relay_bin") { markTraffic(); handleLocationRelayBinary(it) }
        s.on("log_relay") { markTraffic(); handleLogRelay(it) }
        s.on("settings_relay") { markTraffic(); handleSettingsRelay(it) }
        s.on("viewer_status_relay") { markTraffic(); handleViewerStatusRelay(it) }
        s.on("ping_relay") { markTraffic(); handlePingRelay(it) }
        s.on("pong_relay") { markTraffic(); handlePongRelay(it) }
    }

    override fun setConnectionLostCallback(callback: () -> Unit) {
        this.onConnectionLost = callback
    }

    private fun handleLocationRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val incomingId = data.optString("id")
            val incomingViewerId = data.optString("viewer_id")
            val fromViewer = data.optBoolean("from_viewer", SignalingValidator.isViewerRole(data.optString("from")))

            if (!SignalingValidator.shouldProcessLocationUpdate(
                    incomingId = incomingId,
                    ownDeviceId = deviceId,
                    isFromViewer = fromViewer,
                    viewerId = incomingViewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return
            
            onRemoteUpdateWrapper.onUpdate(data)
        } catch (e: Exception) {
            Log.e("GPS19", "location_relay parse error")
        }
    }

    private fun handleLocationRelayBinary(args: Array<Any>) {
        try {
            val data = args[0] as ByteArray
            val status = RealtimeStatus.parseFrom(data)
            
            if (!SignalingValidator.shouldProcessLocationUpdate(
                    incomingId = status.id,
                    ownDeviceId = deviceId,
                    isFromViewer = status.fromViewer,
                    viewerId = status.viewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return
            
            val json = JSONObject().apply {
                put("id", status.id); put("viewer_id", status.viewerId); put("from_viewer", status.fromViewer)
                put("lat", status.lat); put("lng", status.lng); put("speed", status.speed)
                put("accuracy", status.accuracy); put("max_accuracy", status.maxAccuracy)
                put("bearing", status.bearing); put("battery", status.battery); put("temp", status.temp)
                put("is_charging", status.isCharging); put("ts", status.ts); put("gps_ts", status.gpsTs)
                put("sats_view", status.satsView); put("sats_used", status.satsUsed)
                put("uptime_ms", status.uptimeMs)
                put("total_connected_ms", status.totalConnectedMs); put("session_connected_ms", status.sessionConnectedMs)
                put("total_drop_ms", status.totalDropMs); put("max_drop_ms", status.maxDropMs)
                put("last_conn_ts", status.lastConnTs); put("last_disc_ts", status.lastDiscTs); put("is_historical", status.isHistorical)
                put("alt", status.alt)
                
                // v9.3.25: Reverse map Enums to strings for app compatibility
                put("tracker_state", TrackerStatus.mapProtoToTrackerState(status.state).name)
                put("is_anchor_locked", status.isAnchorLocked)
                put("is_location_pending", status.isLocationPending)
                put("location_pending_reason", TrackerStatus.mapProtoToPendingReason(status.pendingReason).name)
                
                put("last_valid_fixRealtime", status.lastValidFixRealtime)
                put("is_battery_steep_discharge", status.isBatterySteepDischarge)
                put("is_cooling_mode_active", status.isCoolingModeActive)
            }
            onRemoteUpdateWrapper.onUpdate(json)
        } catch (e: Exception) {
            Log.e("GPS19", "location_relay_bin parse error")
        }
    }

    private fun handleLogRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val incomingId = data.optString("id")
            val incomingViewerId = data.optString("viewer_id")

            if (!SignalingValidator.shouldProcessLogRelay(
                    incomingId = incomingId,
                    ownDeviceId = deviceId,
                    incomingViewerId = incomingViewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return
            
            val entry = LogEntry.fromJSONObject(data)
            logRepository.addLog(entry)

            val wrapped = JSONObject(data.toString())
            wrapped.put("type", "remote_log")
            onRemoteUpdateWrapper.onUpdate(wrapped)

        } catch (e: Exception) {
            Timber.e(e, "log_relay parse error")
        }
    }

    private fun handleSettingsRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val incomingId = data.optString("id", "").trim()
            val incomingViewerId = data.optString("viewer_id", "").trim()
            val fromViewer = data.optBoolean("from_viewer", SignalingValidator.isViewerRole(data.optString("from")))

            if (!SignalingValidator.shouldProcessSettingsUpdate(
                    incomingId = incomingId,
                    ownDeviceId = deviceId,
                    incomingViewerId = incomingViewerId,
                    ownViewerId = viewerId,
                    fromViewer = fromViewer,
                    isTrackerMode = isTrackerMode
            )) return
            
            onRemoteUpdateWrapper.onUpdate(data)
        } catch (e: Exception) {
            Timber.e(e, "settings_relay parse error")
        }
    }

    private fun handleViewerStatusRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val incomingViewerId = data.optString("viewer_id")
            
            if (isTrackerMode) {
                if (!SignalingConstants.isViewerMatch(incomingViewerId, viewerId) && !isDefaultViewer(viewerId)) return
            } else {
                if (SignalingConstants.isViewerMatch(incomingViewerId, viewerId)) return
            }

            onRemoteUpdateWrapper.onUpdate(JSONObject().apply {
                put("type", "viewer_pulse")
                put("id", deviceId) 
                put("viewer_id", incomingViewerId)
                put("from_viewer", true)
            })
        } catch (e: Exception) {
            Timber.e(e, "viewer_status_relay parse error")
        }
    }

    private fun handlePingRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val from = data.optString("from")
            val pingDeviceId = data.optString("id", "")
            val incomingViewerId = data.optString("viewer_id", "")
            
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isViewerPing = SignalingValidator.isViewerRole(from)
                val isTrackerPing = from == SignalingConstants.getOwnTypeLabel(true)
                
                if (isTrackerMode && isViewerPing && !SignalingConstants.isViewerMatch(incomingViewerId, viewerId) && !isDefaultViewer(viewerId)) return
                
                if ((isTrackerMode && isViewerPing) || (!isTrackerMode && isTrackerPing)) {
                    val incomingMap = mutableMapOf<String, Any>()
                    data.keys().forEach { incomingMap[it] = data.get(it) }
                    
                    SignalPayloadGenerator.createPongPayload(incomingMap, deviceId, isTrackerMode)?.let { pongMap ->
                        socket?.emit("pong_cmd", JSONObject(pongMap))
                    }
                    
                    onRemoteUpdateWrapper.onUpdate(JSONObject().apply {
                        put("type", SignalingConstants.getPulseType(isTrackerMode))
                        put("id", deviceId)
                        put("viewer_id", incomingViewerId)
                        put("from_viewer", isViewerPing)
                    })
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "ping_relay parse error")
        }
    }

    private fun handlePongRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val fromStr = data.optString("from")
            val pingDeviceId = data.optString("id", "")
            val pongViewerId = data.optString("viewer_id", "")
            
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isFromViewer = SignalingValidator.isViewerRole(fromStr)
                
                val isMyPong = if (isTrackerMode) fromStr == "tracker" else isFromViewer
                val isPeerPong = if (isTrackerMode) isFromViewer else fromStr == "tracker"

                if (isMyPong) {
                    onRemoteUpdateWrapper.onUpdate(JSONObject().apply { 
                        put("type", "pong_activity"); put("id", deviceId); put("viewer_id", pongViewerId); put("from_viewer", isFromViewer) 
                    })
                    val rtt = (timeProvider.currentTimeMillis() - data.optLong("ts")).toInt()
                    if (rtt > 0) {
                        rtts.add(rtt); if (rtts.size > 5) rtts.removeAt(0)
                        lastRttInternal = rtts.minOrNull() ?: rtt
                        telemetryRepository.updateLastRtt(lastRttInternal)
                    }
                } else if (isPeerPong) {
                    if (isTrackerMode && !SignalingConstants.isViewerMatch(pongViewerId, viewerId) && !isDefaultViewer(viewerId)) return

                    onRemoteUpdateWrapper.onUpdate(JSONObject().apply {
                        put("type", SignalingConstants.getPulseType(isTrackerMode))
                        put("id", deviceId)
                        put("viewer_id", pongViewerId)
                        put("from_viewer", isFromViewer)
                    })
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "pong_relay parse error")
        }
    }

    override fun clearRtt() { 
        rtts.clear(); lastRttInternal = 0; telemetryRepository.updateLastRtt(0)
    }

    override fun getRtt(): Int = lastRttInternal

    override fun emit(event: String, data: JSONObject) {
        if (isStopped) return
        if (event == "location_update") { emitLocationConflated(data) } else { socket?.emit(event, data) }
    }

    override fun emitBinary(event: String, routingId: String, data: ByteArray) {
        if (isStopped) return
        // R988: Pass routingId separately to relay for efficient room broadcasting
        Log.d("GPS19_COMM", "EMIT BINARY: $event for $routingId (${data.size} bytes)")
        socket?.emit(event, routingId, data) 
    }

    private fun emitLocationConflated(data: JSONObject) {
        val pending = pendingLocationUpdate?.toMap()
        val incoming = data.toMap()
        
        val result = SignalingMessageConflator.conflate(pending, incoming)
        pendingLocationUpdate = JSONObject(result)

        if (conflationJob == null || !conflationJob!!.isActive) {
            conflationJob = scope.launch {
                delay(100)
                val toSend = pendingLocationUpdate
                if (toSend != null && isConnected() && !isStopped) { 
                    socket?.emit("location_update", toSend)
                    pendingLocationUpdate = null
                }
            }
        }
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = get(key)
        }
        return map
    }

    override fun isConnected() = socket?.connected() ?: false

    override fun pushSettings() {
        if (isTrackerMode || deviceId.isEmpty() || isStopped) return
        scope.launch {
            try {
                val homePoints = settingsRepository.loadHomePoints()
                val array = JSONArray()
                homePoints.forEach { array.put(JSONObject().apply { put("lat", it.latitude); put("lng", it.longitude) }) }
                socket?.emit("settings_update", JSONObject().apply {
                    put("home_points", array); put("settings_ts", settingsRepository.getLong(SettingsRepository.HOME_POINTS_TS_KEY, 0L))
                    put("max_dist", settingsRepository.getDouble(SettingsRepository.MAX_DISTANCE_STORAGE_KEY, 60.0))
                    put("id", SignalingConstants.getTransmissionId(deviceId))
                    put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
                    put("from_viewer", true)
                })
            } catch (e: Exception) { 
                if (e is CancellationException) throw e
                Timber.e(e, "Error pushing settings to socket") 
            }
        }
    }

    override fun disconnect() { 
        isStopped = true
        scope.cancel()
        socket?.disconnect() 
    }
}
