package com.gps19.app

import android.content.Context
import android.util.Log
import com.gps19.core.engine.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*

/**
 * Socket.io implementation of the SignalingProvider.
 * July.17.03:
 * - Fixed #R997: markTraffic() now called on emit/emitBinary to prevent keep-alive logic
 *   from falsely detecting silence during active outgoing traffic.
 * v9.5.0:
 * - Issue #503: Hilt Removal. Manual dependency injection.
 */
class CommunicationManager(
    private val context: Context,
    private val configManager: ConfigManager,
    private val logManager: LogManager,
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
                // R997: Log join as non-important to prevent recursive emission loop if keep-alive triggered it
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
            if (deviceId.isNotEmpty()) {
                s.emit("join", createJoinPayload())
            }
            pushSettings()
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
            if (reason != "io client disconnect") {
                onConnectionLost?.invoke()
            }
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args?.getOrNull(0)?.toString() ?: "Transport Error"
            logToApp("Relay Connect Error: $error", true)
            onConnectionLost?.invoke()
        }

        s.on("location_relay") { markTraffic(); /* handleLocationRelay delegated */ }
        s.on("location_relay_bin") { markTraffic(); /* handleLocationRelayBinary delegated */ }
        s.on("log_relay") { markTraffic(); /* handleLogRelay delegated */ }
        s.on("settings_relay") { markTraffic(); /* handleSettingsRelay delegated */ }
        s.on("viewer_status_relay") { markTraffic(); /* handleViewerStatusRelay delegated */ }
        s.on("ping_relay") { markTraffic(); /* handlePingRelay delegated */ }
        s.on("pong_relay") { markTraffic(); /* handlePongRelay delegated */ }
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
    }

    override fun disconnect() { 
        isStopped = true
        scope.cancel()
        socket?.disconnect() 
    }
}
