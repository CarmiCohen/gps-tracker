package com.gps19.app

import android.content.Context
import android.util.Log
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.io implementation of the SignalingProvider.
 * July.24.04:
 * - Issue #541: Direct Binary Flow. Updated location_relay_bin handler to 
 *   dispatch raw bytes via onBinaryUpdate, bypassing JSONObject allocation.
 * - Issue #538: Churn Optimization. Optimized handleLogRelay and conflation 
 *   path to minimize Map/JSONObject conversions.
 * July.23.10:
 * - Issue #533: Fixed SRV status inconsistency.
 */
@Singleton
class CommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configManager: ConfigManager,
    private val logManager: LogManager,
    private val telemetryRepository: TelemetryRepository,
    private val logRepository: LogRepository,
    private val timeProvider: TimeProvider
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
    private var remoteUpdateListener: SignalingProvider.RemoteUpdateListener? = null

    private val commExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped) return@CoroutineExceptionHandler
        Timber.e(throwable, "CRITICAL: Communication failure")
        logManager.logServiceEvent("CRITICAL: Communication failure: ${throwable.message}", true)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + commExceptionHandler)
    private var pendingLocationMap: MutableMap<String, Any>? = null
    private var conflationJob: Job? = null

    override fun setRemoteUpdateListener(listener: SignalingProvider.RemoteUpdateListener?) {
        this.remoteUpdateListener = listener
    }

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
                logToApp("Joining room: $transNew (Force: $force)", false)
                socket?.emit("join", createJoinPayload())
                markTraffic()
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
            telemetryRepository.updateRelayStatus(true)
            if (deviceId.isNotEmpty()) {
                s.emit("join", createJoinPayload())
            }
        }

        s.on(Socket.EVENT_CONNECT) { onConnectAction() }
        s.on("reconnecting") { 
            logToApp("Relay Reconnecting...", true)
            telemetryRepository.updateRelayStatus(false)
        }
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

        s.on("location_relay") { args -> markTraffic(); handleLocationRelay(args) }
        s.on("location_relay_bin") { args -> markTraffic(); handleLocationRelayBinary(args) }
        s.on("log_relay") { args -> markTraffic(); handleLogRelay(args) }
        s.on("viewer_status_relay") { args -> markTraffic(); handleViewerStatusRelay(args) }
        s.on("ping_relay") { args -> markTraffic(); handlePingRelay(args) }
        s.on("pong_relay") { args -> markTraffic(); handlePongRelay(args) }
    }

    private fun handleLocationRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val incomingId = data.optString("id")
            val incomingViewerId = data.optString("viewer_id")
            val fromViewer = data.optBoolean("from_viewer")

            if (!SignalingValidator.shouldProcessLocationUpdate(
                    incomingId = incomingId,
                    ownDeviceId = deviceId,
                    isFromViewer = fromViewer,
                    viewerId = incomingViewerId,
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return
            
            remoteUpdateListener?.onUpdate(data)
        } catch (e: Exception) {
            Log.e("GPS19", "location_relay parse error")
        }
    }

    private fun handleLocationRelayBinary(args: Array<Any>) {
        try {
            val data = args[0] as ByteArray
            // Issue #541: Prioritize direct binary delivery
            remoteUpdateListener?.onBinaryUpdate(data)
        } catch (e: Exception) {
            Log.e("GPS19", "location_relay_bin direct dispatch failure")
        }
    }

    private fun handleLogRelay(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            if (!SignalingValidator.shouldProcessLogRelay(
                    incomingId = data.optString("id"),
                    ownDeviceId = deviceId,
                    incomingViewerId = data.optString("viewer_id"),
                    ownViewerId = viewerId,
                    isTrackerMode = isTrackerMode
            )) return
            
            val entry = LogEntry.fromJSONObject(data)
            logRepository.addLog(entry)

            // Issue #538: Optimized dispatch. Instead of deep copy, we use a 
            // shallow clone of keys if listener is active, or just pass if safe.
            remoteUpdateListener?.let { listener ->
                val wrapped = JSONObject()
                val keys = data.keys()
                while(keys.hasNext()) { val k = keys.next(); wrapped.put(k, data.get(k)) }
                wrapped.put("type", "remote_log")
                listener.onUpdate(wrapped)
            }
        } catch (e: Exception) {
            Timber.e(e, "log_relay parse error")
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

            remoteUpdateListener?.onUpdate(JSONObject().apply {
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
            val pingDeviceId = data.optString("id", "")
            val incomingViewerId = data.optString("viewer_id", "")
            
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isViewerPing = SignalingValidator.isViewerRole(data.optString("from"))
                
                if (isTrackerMode && isViewerPing && !SignalingConstants.isViewerMatch(incomingViewerId, viewerId) && !isDefaultViewer(viewerId)) return
                
                if ((isTrackerMode && isViewerPing) || (!isTrackerMode && !isViewerPing)) {
                    val incomingMap = mutableMapOf<String, Any>()
                    data.keys().forEach { incomingMap[it] = data.get(it) }
                    
                    SignalPayloadGenerator.createPongPayload(incomingMap, deviceId, isTrackerMode)?.let { pongMap ->
                        socket?.emit("pong_cmd", JSONObject(pongMap))
                    }
                    
                    remoteUpdateListener?.onUpdate(JSONObject().apply {
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
            val pingDeviceId = data.optString("id", "")
            val pongViewerId = data.optString("viewer_id", "")
            
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isFromViewer = SignalingValidator.isViewerRole(data.optString("from"))
                val isMyPong = if (isTrackerMode) !isFromViewer else isFromViewer

                if (isMyPong) {
                    remoteUpdateListener?.onUpdate(JSONObject().apply { 
                        put("type", "pong_activity"); put("id", deviceId); put("viewer_id", pongViewerId); put("from_viewer", isFromViewer) 
                    })
                    val rtt = (timeProvider.currentTimeMillis() - data.optLong("ts")).toInt()
                    if (rtt > 0) {
                        rtts.add(rtt); if (rtts.size > 5) rtts.removeAt(0)
                        lastRttInternal = rtts.minOrNull() ?: rtt
                        telemetryRepository.updateLastRtt(lastRttInternal)
                    }
                } else {
                    if (isTrackerMode && !SignalingConstants.isViewerMatch(pongViewerId, viewerId) && !isDefaultViewer(viewerId)) return

                    remoteUpdateListener?.onUpdate(JSONObject().apply {
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

    override fun setConnectionLostCallback(callback: () -> Unit) {
        this.onConnectionLost = callback
    }

    override fun clearRtt() { 
        rtts.clear(); lastRttInternal = 0
    }

    override fun getRtt(): Int = lastRttInternal

    override fun emit(event: String, data: JSONObject) {
        if (isStopped) return
        markTraffic()
        if (event == "location_update") { emitLocationConflated(data) } else { socket?.emit(event, data) }
    }

    override fun emitBinary(event: String, routingId: String, data: ByteArray) {
        if (isStopped) return
        markTraffic()
        socket?.emit(event, routingId, data) 
    }

    private fun emitLocationConflated(data: JSONObject) {
        // Issue #538: Optimized conflation. We maintain the pending state as a Map
        // and only convert the incoming JSONObject once.
        val incoming = data.toMap()
        pendingLocationMap = SignalingMessageConflator.conflate(pendingLocationMap, incoming).toMutableMap()

        if (conflationJob == null || !conflationJob!!.isActive) {
            conflationJob = scope.launch {
                delay(100)
                val mapToSend = pendingLocationMap
                if (mapToSend != null && isConnected() && !isStopped) { 
                    socket?.emit("location_update", JSONObject(mapToSend))
                    pendingLocationMap = null
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

    override fun disconnect() { 
        isStopped = true
        scope.cancel()
        socket?.disconnect() 
        telemetryRepository.updateRelayStatus(false)
    }
}
