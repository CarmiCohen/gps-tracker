package com.gps19.app

import android.content.Context
import com.google.protobuf.CodedOutputStream
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import timber.log.Timber
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Socket.io implementation of the SignalingProvider.
 * Oct.6.10:
 * - Issue #AUDIT-1006-9: Fixed Protocol Optimization. Integrated resetDeltaState() 
 *   on connection and reconnection events to ensure coordinate synchronization 
 *   (Rule 1.125).
 * Oct.6.9:
 * - Issue #AUDIT-1006-9: Dependency Cycle Remediation. Migrated to Provider<T> for 
 *   LogManager, ConfigManager, and SessionManager to break initialization circularity.
 * - Issue #AUDIT-1006-9 (SIMP-1426-6): Reactive Metrics. Integrated signalingMetrics 
 *   StateFlow from dispatcher (Rule 2.1).
 */
@Singleton
class CommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configManagerProvider: Provider<ConfigManager>,
    private val logManagerProvider: Provider<LogManager>,
    private val telemetryRepository: TelemetryRepository,
    private val timeProvider: TimeProvider,
    private val sessionManagerProvider: Provider<SessionManager>
) : SignalingProvider {

    private var socket: Socket? = null
    private var isStopped = false
    private val isConnectingInternal = AtomicBoolean(false)
    private val currentSessionId = AtomicInteger(0)
    
    private var deviceId = ""
    private var viewerId = ""
    private var relayUrl = ""
    private var isTrackerMode = false
    
    private val rtts = mutableListOf<Int>()
    private var lastRttInternal = 0
    private var lastRelayTrafficTs = timeProvider.elapsedRealtime()

    private var onConnectionLost: (() -> Unit)? = null

    private val statusBuilder = RealtimeStatus.newBuilder()
    private var serializationBuffer = ByteArray(4096)
    private val MAX_SERIALIZATION_BUFFER_SIZE = 65536

    private val _signalingFlow = MutableSharedFlow<SignalingEvent>(
        extraBufferCapacity = 128, 
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val signalingFlow: SharedFlow<SignalingEvent> = _signalingFlow.asSharedFlow()

    private val commExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped) return@CoroutineExceptionHandler
        Timber.e(throwable, "CRITICAL: Communication failure")
        logManagerProvider.get().logServiceEvent("CRITICAL: Communication failure: ${throwable.message}", true)
    }

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + commExceptionHandler)
    
    private val dispatcher = SmartSignalingDispatcher(
        scope = scope,
        isViolationProvider = { sessionManagerProvider.get().isInViolation },
        jsonSink = { event, data -> 
            if (!isStopped) socket?.emit(event, JSONObject(data))
        },
        binarySink = { event, routingId, data ->
            if (!isStopped) socket?.emit(event, routingId, data)
        },
        objectSink = { event, status ->
            serializeAndEmitBinary(event, status)
        },
        isConnectedProvider = { isConnected() }
    )

    override val signalingMetrics: StateFlow<SmartSignalingDispatcher.Metrics> = dispatcher.metricsFlow

    private fun isDefaultViewer(id: String) = id == SignalingConstants.DEFAULT_VIEWER_ID || id.isEmpty()

    private fun logToApp(message: String, important: Boolean = false) {
        if (isStopped) return
        Timber.tag("GPS19_COMM").i(message)
        logManagerProvider.get().submitToLogSink(message, "system", important)
    }

    override fun getLastRelayTrafficTs(): Long = lastRelayTrafficTs

    private fun markTraffic() { lastRelayTrafficTs = timeProvider.elapsedRealtime() }

    private fun createJoinPayload(): Map<String, Any?> {
        return mapOf(
            "id" to SignalingConstants.getTransmissionId(deviceId),
            "role" to if (isTrackerMode) "tracker" else "viewer",
            "ver" to BuildConfig.VERSION_NAME
        )
    }

    override fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean) {
        val oldId = this.deviceId
        val oldIsTracker = this.isTrackerMode
        val cleanedDeviceId = deviceId.trim()
        val cleanedViewerId = viewerId.trim()

        if (isTracker && !SignalingConstants.isValidTrackerId(cleanedDeviceId)) {
            logToApp("Invalid Tracker ID: $cleanedDeviceId", true); return
        }
        if (!isTracker && !SignalingConstants.isValidViewerId(cleanedViewerId)) {
            logToApp("Invalid Viewer ID: $cleanedViewerId", true); return
        }

        val idChanged = oldId.isNotEmpty() && oldId != cleanedDeviceId
        val roleChanged = oldIsTracker != isTracker
        
        this.deviceId = cleanedDeviceId
        this.viewerId = cleanedViewerId
        this.isTrackerMode = isTracker
        
        if (idChanged || roleChanged || force) {
            if (idChanged && oldId.isNotEmpty()) {
                emitInternal("leave", mapOf("id" to SignalingConstants.getTransmissionId(oldId)), SignalingPriority.HIGH)
            }
            if (this.deviceId.isNotEmpty()) {
                emitInternal("join", createJoinPayload(), SignalingPriority.HIGH)
            }
        }
    }

    override fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean) {
        this.isStopped = false
        val oldIsTracker = this.isTrackerMode
        val oldUrl = this.relayUrl
        val oldDeviceId = this.deviceId
        val oldViewerId = this.viewerId

        val newUrl = url.trim()
        val newDeviceId = deviceId.trim()
        val newViewerId = viewerId.trim()
        
        val roleChanged = oldIsTracker != isTracker && oldDeviceId.isNotEmpty()
        val urlChanged = oldUrl != newUrl
        val idChanged = (oldDeviceId != newDeviceId || oldViewerId != newViewerId) && oldDeviceId.isNotEmpty()

        this.relayUrl = newUrl
        this.deviceId = newDeviceId
        this.viewerId = newViewerId
        this.isTrackerMode = isTracker
        
        if (telemetryRepository.isSafeMode.value) {
            logToApp("SAFE MODE: Connection suppressed to prevent signaling loops.", true)
            return
        }

        if (isTracker && !SignalingConstants.isValidTrackerId(this.deviceId)) return
        if (!isTracker && !SignalingConstants.isValidViewerId(this.viewerId)) return
        if (relayUrl.isEmpty()) return
        
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + commExceptionHandler)
        }

        if (!roleChanged && !urlChanged && !idChanged && (isConnectingInternal.get() || isConnected())) return

        val sessionId = currentSessionId.incrementAndGet()
        
        socket?.disconnect(); socket?.off(); socket = null
        isConnectingInternal.set(true)
        
        logToApp("Starting connection session [$sessionId] to $relayUrl (Role: ${if(isTracker) "Tracker" else "Viewer"})", true)
        markTraffic() 

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            timeout = 60000
            reconnection = true; reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 2000; reconnectionDelayMax = 10000; randomizationFactor = 0.5; forceNew = true 
        }

        try {
            socket = IO.socket(relayUrl, opts)
            registerSocketListeners(sessionId)
            socket?.connect()
        } catch (e: Exception) {
            if (sessionId == currentSessionId.get()) isConnectingInternal.set(false)
            logToApp("Socket creation failed: ${e.message}", true)
        }
    }

    private fun registerSocketListeners(sessionId: Int) {
        val s = socket ?: return

        val checkSession = { block: () -> Unit ->
            if (sessionId == currentSessionId.get() && !isStopped) {
                block()
            }
        }

        s.on(Socket.EVENT_CONNECT) {
            checkSession {
                scope.launch {
                    isConnectingInternal.set(false)
                    yield()
                    logToApp("Connected to relay [Session $sessionId]", true)
                    markTraffic()
                    telemetryRepository.updateRelayStatus(true)
                    
                    // Issue #AUDIT-1006-9: Reset delta encoding state for the new session
                    TelemetryProtobufMapper.resetDeltaState()
                    
                    if (deviceId.isNotEmpty()) emitInternal("join", createJoinPayload(), SignalingPriority.HIGH)
                }
            }
        }

        s.on("reconnecting") { 
            checkSession { 
                logToApp("Relay Reconnecting... [Session $sessionId]", true)
                telemetryRepository.updateRelayStatus(false) 
            } 
        }

        s.on("reconnect") {
            checkSession {
                scope.launch {
                    isConnectingInternal.set(false)
                    logToApp("Relay Reconnected [Session $sessionId]", true)
                    markTraffic()
                    telemetryRepository.updateRelayStatus(true)
                    
                    // Issue #AUDIT-1006-9: Reset delta encoding state on reconnection
                    TelemetryProtobufMapper.resetDeltaState()
                    
                    if (deviceId.isNotEmpty()) emitInternal("join", createJoinPayload(), SignalingPriority.HIGH)
                }
            }
        }
        
        s.on(Socket.EVENT_DISCONNECT) { args ->
            checkSession {
                isConnectingInternal.set(false)
                val reason = args?.getOrNull(0)?.toString() ?: "unknown"
                logToApp("Relay Disconnected ($reason) [Session $sessionId]", true)
                telemetryRepository.updateRelayStatus(false)
                if (reason != "io client disconnect") onConnectionLost?.invoke()
            }
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            checkSession {
                isConnectingInternal.set(false)
                logToApp("Relay Connect Error: ${args?.getOrNull(0)} [Session $sessionId]", true)
                telemetryRepository.updateRelayStatus(false)
                onConnectionLost?.invoke()
            }
        }

        s.on("location_relay") { args -> checkSession { markTraffic(); handleLocationRelay(args) } }
        s.on("location_relay_bin") { args -> checkSession { markTraffic(); handleLocationRelayBinary(args) } }
        s.on("log_relay") { args -> checkSession { markTraffic(); handleLogRelay(args) } }
        s.on("viewer_status_relay") { args -> checkSession { markTraffic(); handleViewerStatusRelay(args) } }
        s.on("ping_relay") { args -> checkSession { markTraffic(); handlePingRelay(args) } }
        s.on("pong_relay") { args -> checkSession { markTraffic(); handlePongRelay(args) } }
    }

    private fun handleLocationRelay(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is JSONObject) args[1] as JSONObject 
                       else args[0] as JSONObject
            _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(data))
        } catch (e: Exception) { Timber.e("location_relay parse error") }
    }

    private fun handleLocationRelayBinary(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is ByteArray) args[1] as ByteArray 
                       else args[0] as ByteArray
            _signalingFlow.tryEmit(SignalingEvent.BinaryUpdate(data))
        } catch (e: Exception) { Timber.e("location_relay_bin parse error") }
    }

    private fun handleLogRelay(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is JSONObject) args[1] as JSONObject 
                       else args[0] as JSONObject
            val wrapped = JSONObject()
            val keys = data.keys()
            while(keys.hasNext()) { val k = keys.next(); wrapped.put(k, data.get(k)) }
            wrapped.put("type", "remote_log")
            _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(wrapped))
        } catch (e: Exception) { Timber.e(e, "log_relay parse error") }
    }

    private fun handleViewerStatusRelay(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is JSONObject) args[1] as JSONObject 
                       else args[0] as JSONObject
            val incomingViewerId = data.optString("viewer_id")
            if (isTrackerMode) {
                if (!SignalingConstants.isViewerMatch(incomingViewerId, viewerId) && !isDefaultViewer(viewerId)) return
            } else {
                if (SignalingConstants.isViewerMatch(incomingViewerId, viewerId)) return
            }
            _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(JSONObject().apply {
                put("type", "viewer_pulse"); put("id", deviceId); put("viewer_id", incomingViewerId); put("from_viewer", true)
            }))
        } catch (e: Exception) { Timber.e(e, "viewer_status_relay parse error") }
    }

    private fun handlePingRelay(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is JSONObject) args[1] as JSONObject 
                       else args[0] as JSONObject
            val pingDeviceId = data.optString("id", "")
            val incomingViewerId = data.optString("viewer_id", "")
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isViewerPing = SignalingValidator.isViewerRole(data.optString("from"))
                if (isTrackerMode && isViewerPing && !SignalingConstants.isViewerMatch(incomingViewerId, viewerId) && !isDefaultViewer(viewerId)) return
                if ((isTrackerMode && isViewerPing) || (!isTrackerMode && !isViewerPing)) {
                    val incomingMap = mutableMapOf<String, Any?>()
                    data.keys().forEach { incomingMap[it] = data.get(it) }
                    SignalPayloadGenerator.createPongPayload(incomingMap as Map<String, Any>, deviceId, isTrackerMode)?.let { pongMap ->
                        emitInternal("pong_cmd", pongMap, SignalingPriority.HIGH)
                    }
                    _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(JSONObject().apply {
                        put("type", SignalingConstants.getPulseType(isTrackerMode)); put("id", deviceId); put("viewer_id", incomingViewerId); put("from_viewer", isViewerPing)
                    }))
                }
            }
        } catch (e: Exception) { Timber.e(e, "ping_relay parse error") }
    }

    private fun handlePongRelay(args: Array<Any>) {
        try {
            val data = if (args.size > 1 && args[1] is JSONObject) args[1] as JSONObject 
                       else args[0] as JSONObject
            val pingDeviceId = data.optString("id", "")
            val pongViewerId = data.optString("viewer_id", "")
            if (SignalingConstants.isTrackerMatch(pingDeviceId, deviceId) && deviceId.isNotEmpty()) {
                val isFromViewer = SignalingValidator.isViewerRole(data.optString("from"))
                val isMyPong = if (isTrackerMode) !isFromViewer else isFromViewer
                if (isMyPong) {
                    _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(JSONObject().apply { put("type", "pong_activity"); put("id", deviceId); put("viewer_id", pongViewerId); put("from_viewer", isFromViewer) }))
                    val rtt = (timeProvider.currentTimeMillis() - data.optLong("ts")).toInt()
                    if (rtt > 0) {
                        rtts.add(rtt); if (rtts.size > 5) rtts.removeAt(0)
                        lastRttInternal = rtts.minOrNull() ?: rtt; telemetryRepository.updateLastRtt(lastRttInternal)
                    }
                } else {
                    if (isTrackerMode && !SignalingConstants.isViewerMatch(pongViewerId, viewerId) && !isDefaultViewer(viewerId)) return
                    _signalingFlow.tryEmit(SignalingEvent.JsonUpdate(JSONObject().apply {
                        put("type", SignalingConstants.getPulseType(isTrackerMode)); put("id", deviceId); put("viewer_id", pongViewerId); put("from_viewer", isFromViewer)
                    }))
                }
            }
        } catch (e: Exception) { Timber.e(e, "pong_relay parse error") }
    }

    override fun setConnectionLostCallback(callback: () -> Unit) { this.onConnectionLost = callback }
    override fun clearRtt() { rtts.clear(); lastRttInternal = 0 }
    override fun getRtt(): Int = lastRttInternal
    
    override fun emit(event: String, data: JSONObject, priority: SignalingPriority) { 
        emitInternal(event, data.toMap(), priority) 
    }

    @Synchronized
    override fun transmit(status: LocationUpdate, priority: SignalingPriority, fromViewer: Boolean) {
        if (isStopped || !isConnected()) return
        markTraffic()
        if (isTrackerMode && !fromViewer) {
            dispatcher.dispatch(SmartSignalingDispatcher.Command.Object("location_update_bin", status, priority))
        } else {
            emitInternal("location_update", status.toMap(fromViewer), priority)
        }
    }

    private fun serializeAndEmitBinary(event: String, status: LocationUpdate) {
        if (isStopped || !isConnected()) return
        synchronized(statusBuilder) {
            statusBuilder.clear()
            // live signaling uses delta encoding by default (Oct.6.10 fix)
            TelemetryProtobufMapper.mapToRealtime(status, statusBuilder, fromViewer = false, useDeltaEncoding = true)
            val message = statusBuilder.buildPartial()
            val size = message.serializedSize
            if (size > serializationBuffer.size && size <= MAX_SERIALIZATION_BUFFER_SIZE) {
                serializationBuffer = ByteArray((serializationBuffer.size * 2).coerceAtLeast(size).coerceAtMost(MAX_SERIALIZATION_BUFFER_SIZE))
            }
            if (size <= serializationBuffer.size) {
                try {
                    val cos = CodedOutputStream.newInstance(serializationBuffer, 0, size)
                    message.writeTo(cos); cos.checkNoSpaceLeft()
                    val payload = Arrays.copyOf(serializationBuffer, size)
                    socket?.emit(event, SignalingConstants.getTransmissionId(deviceId), payload)
                    return
                } catch (e: Exception) { Timber.e(e, "Pre-allocated serialization failed") }
            }
            socket?.emit(event, SignalingConstants.getTransmissionId(deviceId), message.toByteArray())
        }
    }

    private fun emitInternal(event: String, data: Map<String, Any?>, priority: SignalingPriority) {
        if (isStopped) return
        markTraffic()
        dispatcher.dispatch(SmartSignalingDispatcher.Command.Json(event, data, priority))
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = keys()
        while (keys.hasNext()) { val key = keys.next(); map[key] = get(key) }
        return map
    }

    override fun isConnected() = socket?.connected() ?: false
    override fun isConnecting(): Boolean = isConnectingInternal.get()
    
    override fun disconnect() { 
        isStopped = true; isConnectingInternal.set(false)
        currentSessionId.incrementAndGet()
        dispatcher.shutdown()
        socket?.disconnect(); socket?.off(); socket = null
        telemetryRepository.updateRelayStatus(false)
        scope.cancel()
    }
}
