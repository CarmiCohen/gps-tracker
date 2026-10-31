package com.jd.core.network

import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ViolationEvent
import com.jd.core.shared.SystemLogger
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SocketioSignalingClient: Hardened implementation for Multi-Peer Resilience (ISSUE-REVOL30.0-F2).
 * Extended for Adaptive Telemetry Backfill (ISSUE-REVOL30.0-F5).
 * Hardened for Revol31.2: Exhaustive queue flushing and connection resilience (ISSUE-REVOL30.1-F2).
 * Updated Revol31.9: Increased queue capacity to 5000 for Soak Audit (ISSUE-REVOL31.9-F1).
 * Refactored for testability via SocketFactory and injected Dispatcher.
 */
@Singleton
class SocketioSignalingClient @Inject constructor(
    private val socketFactory: SocketFactory,
    @com.jd.core.network.IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SignalingClient {

    private var socket: Socket? = null
    private val clientScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    // Capacity increased to 5000 to support extended offline periods (R-ID 772 hardening)
    private val queue = SignalingQueue(maxCapacity = 5000)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 64)
    override val telemetryFlow: SharedFlow<LocationUpdate> = _telemetryFlow.asSharedFlow()

    private val _violationFlow = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
    override val violationFlow: SharedFlow<ViolationEvent> = _violationFlow.asSharedFlow()

    private val _backfillRequestFlow = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = 8)
    override val backfillRequestFlow: SharedFlow<BackfillRequest> = _backfillRequestFlow.asSharedFlow()

    private val _backfillResponseFlow = MutableSharedFlow<BackfillResponse>(extraBufferCapacity = 8)
    override val backfillResponseFlow: SharedFlow<BackfillResponse> = _backfillResponseFlow.asSharedFlow()

    private var bytesSent: Long = 0
    private var currentDeviceId: String? = null

    override suspend fun connect(url: String, deviceId: String, secret: String, lastSequenceId: Long) {
        if (_isConnected.value && currentDeviceId == deviceId) return
        
        currentDeviceId = deviceId
        val opts = IO.Options.builder()
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1000)
            .setTimeout(20000)
            .build()

        socket = socketFactory.createSocket(url, opts).apply {
            on(Socket.EVENT_CONNECT) {
                _isConnected.value = true
                SystemLogger.i("Signaling", "Connected to relay as $deviceId")
                emit("join", JSONObject().apply {
                    put("id", deviceId)
                    put("secret", secret)
                    put("lastSeq", lastSequenceId)
                })
                
                clientScope.launch {
                    flushQueue()
                }
            }

            on(Socket.EVENT_DISCONNECT) {
                _isConnected.value = false
                SystemLogger.w("Signaling", "Disconnected from relay")
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                SystemLogger.e("Signaling", "Connection error: ${args.getOrNull(0)}")
            }

            on("location_relay") { args ->
                val data = args[0] as? JSONObject ?: return@on
                try {
                    val update = parseLocationUpdate(data)
                    _telemetryFlow.tryEmit(update)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse location_relay")
                }
            }

            on("backfill_request") { args ->
                val data = args[0] as? JSONObject ?: return@on
                try {
                    val request = BackfillRequest(
                        deviceId = data.getString("id"),
                        startSequenceId = data.getLong("start"),
                        endSequenceId = data.getLong("end"),
                        requestId = data.getString("reqId")
                    )
                    _backfillRequestFlow.tryEmit(request)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse backfill_request")
                }
            }

            on("backfill_response") { args ->
                val data = args[0] as? JSONObject ?: return@on
                try {
                    val updatesArr = data.getJSONArray("updates")
                    val updates = mutableListOf<LocationUpdate>()
                    for (i in 0 until updatesArr.length()) {
                        updates.add(parseLocationUpdate(updatesArr.getJSONObject(i)))
                    }
                    val response = BackfillResponse(
                        requestId = data.getString("reqId"),
                        updates = updates,
                        isComplete = data.getBoolean("done")
                    )
                    _backfillResponseFlow.tryEmit(response)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse backfill_response")
                }
            }
            
            connect()
        }
    }

    private suspend fun flushQueue() {
        while (_isConnected.value) {
            val batch = queue.pollBatch(50) // Increased batch size for faster recovery
            if (batch.isEmpty()) break
            batch.forEach { sendTelemetry(it) }
            yield()
        }
    }

    private fun parseLocationUpdate(data: JSONObject): LocationUpdate {
        return LocationUpdate(
            deviceId = data.optString("id", ""),
            lat = data.optDouble("lat"),
            lng = data.optDouble("lng"),
            timestamp = data.optLong("ts"),
            sequenceId = data.optLong("seq"),
            status = data.optString("status", "VALID")
        ).apply {
            sensorSnapshot.vibration = data.optDouble("vib", 0.0).toFloat()
        }
    }

    override fun disconnect() {
        socket?.disconnect()
        socket = null
        _isConnected.value = false
        currentDeviceId = null
    }

    override fun getMonthlyDataUsageBytes(): Long = bytesSent

    override suspend fun sendTelemetry(update: LocationUpdate): Boolean {
        val deviceId = currentDeviceId ?: return false
        
        if (!_isConnected.value) {
            queue.offer(update)
            return false
        }
        
        val payload = JSONObject().apply {
            put("id", update.deviceId.ifEmpty { deviceId })
            put("lat", update.lat)
            put("lng", update.lng)
            put("ts", update.timestamp)
            put("seq", update.sequenceId)
            put("status", update.status)
            put("vib", update.sensorSnapshot.vibration)
        }
        
        socket?.emit("location_update", payload)
        bytesSent += payload.toString().length.toLong()
        return true
    }

    override suspend fun sendViolation(event: ViolationEvent): Boolean {
        val deviceId = currentDeviceId ?: return false
        
        val payload = JSONObject().apply {
            put("id", deviceId)
            put("type", event.type)
            put("ts", event.timestamp)
        }
        
        socket?.emit("violation_event", payload)
        bytesSent += payload.toString().length.toLong()
        return true
    }

    override suspend fun requestBackfill(request: BackfillRequest): Boolean {
        if (!_isConnected.value) return false
        val payload = JSONObject().apply {
            put("id", request.deviceId)
            put("start", request.startSequenceId)
            put("end", request.endSequenceId)
            put("reqId", request.requestId)
        }
        socket?.emit("backfill_request", payload)
        bytesSent += payload.toString().length.toLong()
        return true
    }

    override suspend fun sendBackfillResponse(response: BackfillResponse): Boolean {
        if (!_isConnected.value) return false
        val payload = JSONObject().apply {
            put("reqId", response.requestId)
            put("done", response.isComplete)
            val updatesArr = JSONArray()
            response.updates.forEach { update ->
                updatesArr.put(JSONObject().apply {
                    put("id", update.deviceId)
                    put("lat", update.lat)
                    put("lng", update.lng)
                    put("ts", update.timestamp)
                    put("seq", update.sequenceId)
                    put("status", update.status)
                    put("vib", update.sensorSnapshot.vibration)
                })
            }
            put("updates", updatesArr)
        }
        socket?.emit("backfill_response", payload)
        bytesSent += payload.toString().length.toLong()
        return true
    }
}
