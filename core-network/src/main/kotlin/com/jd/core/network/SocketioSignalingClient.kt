package com.jd.core.network

import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.DEFAULT_LIGHT_SENSITIVITY
import com.jd.core.shared.EventEntry
import com.jd.core.shared.GraphRequest
import com.jd.core.shared.GraphSample
import com.jd.core.shared.SensorGraphBatch
import com.jd.core.shared.SensorEventBatch
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.shared.LATENCY_PING_INTERVAL_MS
import com.jd.core.shared.LinkStats
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ReceivedSensorInfo
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorSetupReport
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * Updated Nov1.2: Forensic Signaling Batching (ISSUE-REVOL33.0-F1).
 * Updated Nov1.5: Resolved data loss during batch flush transitions.
 */
@Singleton
class SocketioSignalingClient @Inject constructor(
    private val socketFactory: SocketFactory,
    @com.jd.core.network.IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SignalingClient {

    private var socket: Socket? = null
    private val clientScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val queue = SignalingQueue(maxCapacity = BURST_CAPACITY)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _viewerStatusFlow = MutableStateFlow(false)
    override val viewerStatusFlow: StateFlow<Boolean> = _viewerStatusFlow.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = BURST_CAPACITY)
    override val telemetryFlow: SharedFlow<LocationUpdate> = _telemetryFlow.asSharedFlow()

    private val _violationFlow = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
    override val violationFlow: SharedFlow<ViolationEvent> = _violationFlow.asSharedFlow()

    private val _backfillRequestFlow = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = BURST_CAPACITY)
    override val backfillRequestFlow: SharedFlow<BackfillRequest> = _backfillRequestFlow.asSharedFlow()

    private val _backfillResponseFlow = MutableSharedFlow<BackfillResponse>(extraBufferCapacity = BURST_CAPACITY)
    override val backfillResponseFlow: SharedFlow<BackfillResponse> = _backfillResponseFlow.asSharedFlow()

    private val _geofenceFlow = MutableSharedFlow<GeofenceConfig>(extraBufferCapacity = 16)
    override val geofenceFlow: SharedFlow<GeofenceConfig> = _geofenceFlow.asSharedFlow()

    private val _sensorBehaviourFlow = MutableSharedFlow<SensorBehaviour>(extraBufferCapacity = 16)
    override val sensorBehaviourFlow: SharedFlow<SensorBehaviour> = _sensorBehaviourFlow.asSharedFlow()

    private val _sensorSetupFlow = MutableStateFlow<SensorSetupReport?>(null)
    override val sensorSetupFlow: StateFlow<SensorSetupReport?> = _sensorSetupFlow.asStateFlow()

    private val _sensorEventsFlow = MutableSharedFlow<SensorEventBatch>(extraBufferCapacity = 16)
    override val sensorEventsFlow: SharedFlow<SensorEventBatch> = _sensorEventsFlow.asSharedFlow()

    private val _linkStats = MutableStateFlow(LinkStats(countingSinceMs = System.currentTimeMillis())) // ISSUE-NOV15.5-O99 part 3
    override val linkStats: StateFlow<LinkStats> = _linkStats.asStateFlow()

    private val _infoRequestFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    override val infoRequestFlow: SharedFlow<Boolean> = _infoRequestFlow.asSharedFlow()

    private val _sensorInfoFlow = MutableStateFlow<ReceivedSensorInfo?>(null)
    override val sensorInfoFlow: StateFlow<ReceivedSensorInfo?> = _sensorInfoFlow.asStateFlow()

    private val _graphRequestFlow = MutableSharedFlow<GraphRequest>(extraBufferCapacity = 4)
    override val graphRequestFlow: SharedFlow<GraphRequest> = _graphRequestFlow.asSharedFlow()

    private val _eventsRequestFlow = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    override val eventsRequestFlow: SharedFlow<Long> = _eventsRequestFlow.asSharedFlow()

    // A week's first sync arrives as 14 messages at once; tryEmit drops what does not fit, which the next request asks for again.
    private val _graphSamplesFlow = MutableSharedFlow<SensorGraphBatch>(extraBufferCapacity = 32)
    override val graphSamplesFlow: SharedFlow<SensorGraphBatch> = _graphSamplesFlow.asSharedFlow()

    private val _monitorIdFlow = MutableStateFlow<String?>(null) // ISSUE-NOV22.7-O135; disconnect keeps it.
    override val monitorIdFlow: StateFlow<String?> = _monitorIdFlow.asStateFlow()

    private var currentRoom: String? = null // The room joined as `secret`, where the latency pings go.
    private var pingJob: Job? = null

    private var bytesSent: Long = 0
    private val arrivalLag = ArrivalLagMeter() // Used only on the socket's event thread (ISSUE-NOV10.7-O73).
    private var currentDeviceId: String? = null
    private var currentUrl: String? = null
    @Volatile private var pushToken: String? = null // Monitor only; kept across reconnections (ISSUE-NOV10.8-O86).

    // Batching logic (ISSUE-REVOL33.0-F1)
    private val pendingBatch = mutableListOf<LocationUpdate>()
    private val batchMutex = Mutex()
    private var batchFlushJob: Job? = null
    private val BATCH_TIME_MS = 500L
    private val BATCH_SIZE_LIMIT = 10

    override suspend fun connect(url: String, deviceId: String, secret: String, lastSequenceId: Long) {
        if (socket != null && currentDeviceId == deviceId && currentUrl == url) return

        // A changed relay or identity replaces the session; a kept socket would retry forever.
        disconnect()
        currentDeviceId = deviceId
        currentUrl = url
        currentRoom = secret
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
                pushToken?.let { emit("push_token", pushTokenPayload(it)) } // After join: the relay files it under the joined room.
                _linkStats.update { it.afterLink(true, System.currentTimeMillis()) }

                clientScope.launch {
                    flushQueue()
                }
                startBatchTimer()
                startPinging()
            }

            on(Socket.EVENT_DISCONNECT) {
                _isConnected.value = false
                _viewerStatusFlow.value = false
                SystemLogger.w("Signaling", "Disconnected from relay")
                _linkStats.update { it.afterLink(false, System.currentTimeMillis()) }
                stopBatchTimer()
                stopPinging()
                clientScope.launch { movePendingToQueue() }
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                SystemLogger.e("Signaling", "Connection error: ${args.getOrNull(0)}")
            }

            on("location_relay") { args ->
                val data = args[0] ?: return@on
                try {
                    when (data) {
                        is JSONObject -> {
                            val update = LocationRowWire.decode(data)
                            measureArrival(update)
                            _telemetryFlow.tryEmit(update)
                        }
                        is JSONArray -> {
                            for (i in 0 until data.length()) {
                                val update = LocationRowWire.decode(data.getJSONObject(i))
                                measureArrival(update)
                                _telemetryFlow.tryEmit(update)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse location_relay")
                }
            }

            on("viewer_status_relay") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                _viewerStatusFlow.value = data.optBoolean("active", false)
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
                        updates.add(LocationRowWire.decode(updatesArr.getJSONObject(i)))
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

            // The relay forwards settings_update to the room as settings_relay; the sender's echo is harmless.
            on("settings_relay") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                // One decoder reads the type once (SIMP-NOV15.3-S1).
                val message = SettingsWire.decode(data)
                SettingsWire.monitorIdOf(data, message)?.takeIf { it != currentDeviceId }?.let { _monitorIdFlow.value = it }
                when (message) {
                    is SettingsMessage.Geofence -> _geofenceFlow.tryEmit(message.config)
                    is SettingsMessage.Behaviour -> _sensorBehaviourFlow.tryEmit(message.behaviour)
                    is SettingsMessage.Setup -> _sensorSetupFlow.value = message.report
                    is SettingsMessage.Events -> _sensorEventsFlow.tryEmit(message.batch)
                    is SettingsMessage.EventsRequest -> _eventsRequestFlow.tryEmit(message.afterMs)
                    is SettingsMessage.Info -> _sensorInfoFlow.value = ReceivedSensorInfo(message.info, System.currentTimeMillis())
                    is SettingsMessage.InfoRequest -> _infoRequestFlow.tryEmit(message.withSatellites)
                    is SettingsMessage.GraphRequest -> _graphRequestFlow.tryEmit(message.request)
                    is SettingsMessage.GraphSamples -> _graphSamplesFlow.tryEmit(message.batch)
                    null -> Unit
                }
            }

            // Every ping of the room comes here; only this device's own echo is its latency (ISSUE-NOV10.8-O84 part 2).
            on("ping_relay") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                val id = currentDeviceId ?: return@on
                LatencyPingWire.roundTripMs(data, id, monotonicMs())?.let { rtt -> _linkStats.update { it.copy(latencyMs = rtt) } }
            }

            connect()
        }
    }

    private fun measureArrival(update: LocationUpdate) {
        arrivalLag.record(update.status, update.sequenceId, update.timestamp, System.currentTimeMillis())
            ?.let { SystemLogger.i("Signaling", it) }
    }

    private suspend fun flushQueue() {
        while (_isConnected.value) {
            val batch = queue.pollBatch(50) 
            if (batch.isEmpty()) break
            if (!sendTelemetryBatch(batch)) {
                // If send failed (disconnected), put them back at the front in original order
                queue.pushFront(batch)
                break
            }
            yield()
        }
    }

    private fun startBatchTimer() {
        batchFlushJob?.cancel()
        batchFlushJob = clientScope.launch {
            while (isActive) {
                delay(BATCH_TIME_MS)
                flushPendingBatch()
            }
        }
    }

    private fun stopBatchTimer() {
        batchFlushJob?.cancel()
        batchFlushJob = null
    }

    /** Measures the latency to the relay at once and then every [LATENCY_PING_INTERVAL_MS] while connected. */
    private fun startPinging() {
        pingJob?.cancel()
        pingJob = clientScope.launch {
            while (isActive) {
                val room = currentRoom
                val id = currentDeviceId
                if (room != null && id != null && _isConnected.value) {
                    val payload = LatencyPingWire.encode(room, id, monotonicMs())
                    emitCounted("ping_cmd", payload)
                }
                delay(LATENCY_PING_INTERVAL_MS)
            }
        }
    }

    private fun stopPinging() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun monotonicMs(): Long = System.nanoTime() / 1_000_000L

    private suspend fun flushPendingBatch() {
        val toSend = batchMutex.withLock {
            if (pendingBatch.isEmpty()) return@withLock emptyList<LocationUpdate>()
            val items = ArrayList(pendingBatch)
            pendingBatch.clear()
            items
        }
        if (toSend.isNotEmpty()) {
            if (!sendTelemetryBatch(toSend)) {
                // Re-queue on failure
                batchMutex.withLock {
                    pendingBatch.addAll(0, toSend)
                }
            }
        }
    }

    private suspend fun movePendingToQueue() {
        batchMutex.withLock {
            if (pendingBatch.isNotEmpty()) {
                pendingBatch.forEach { queue.offer(it) }
                pendingBatch.clear()
            }
        }
    }

    override fun registerPushToken(token: String) {
        pushToken = token
        if (_isConnected.value) socket?.emit("push_token", pushTokenPayload(token))
    }

    private fun pushTokenPayload(token: String) = JSONObject().put("token", token)

    override fun disconnect() {
        // Leaving on purpose ends the pushes to this phone; a lost connection keeps them (ISSUE-NOV10.8-O86).
        pushToken?.let { if (_isConnected.value) socket?.emit("push_token_remove", pushTokenPayload(it)) }
        socket?.disconnect()
        socket = null
        _isConnected.value = false
        _viewerStatusFlow.value = false
        _linkStats.update { it.afterLink(false, System.currentTimeMillis()) }
        _sensorSetupFlow.value = null // The next session may watch another Sensor.
        _sensorInfoFlow.value = null
        currentDeviceId = null
        currentRoom = null
        stopBatchTimer()
        stopPinging()
    }

    /** The rows held while offline; the half-second batch in transit is no backlog (ISSUE-NOV10.8-O84 part 2). */
    override fun queuedRowCount(): Int = queue.size()

    override fun getMonthlyDataUsageBytes(): Long = bytesSent

    override suspend fun sendTelemetry(update: LocationUpdate): Boolean {
        currentDeviceId ?: return false

        // Callers recycle pooled updates on return, so anything held past this call must be a copy.
        val owned = update.snapshot()

        if (!_isConnected.value) {
            queue.offer(owned)
            return false
        }

        // Critical updates (violations, geofence transitions) bypass batching; an ordinary status such as NO_FIX does not
        if (owned.isCritical) {
            flushPendingBatch() // Flush pending first to maintain order
            return sendTelemetryBatch(listOf(owned))
        }

        batchMutex.withLock {
            pendingBatch.add(owned)
            if (pendingBatch.size >= BATCH_SIZE_LIMIT) {
                clientScope.launch { flushPendingBatch() }
            }
        }
        
        return true
    }

    override suspend fun sendTelemetryBatch(updates: List<LocationUpdate>): Boolean {
        if (updates.isEmpty()) return true
        val deviceId = currentDeviceId ?: return false
        if (!_isConnected.value) return false

        val batchArray = JSONArray()
        updates.forEach { update ->
            batchArray.put(LocationRowWire.encode(update, deviceId, live = true))
        }

        emitCounted("location_batch", batchArray)
        return true
    }

    override suspend fun sendViolation(event: ViolationEvent): Boolean {
        val deviceId = currentDeviceId ?: return false
        
        // Critical event: flush pending telemetry to ensure context arrives first
        flushPendingBatch()

        val payload = JSONObject().apply {
            put("id", deviceId)
            put("type", event.type)
            put("ts", event.timestamp)
        }
        
        emitCounted("violation_event", payload)
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
        emitCounted("backfill_request", payload)
        return true
    }

    override suspend fun sendBackfillResponse(response: BackfillResponse): Boolean {
        val deviceId = currentDeviceId ?: return false
        if (!_isConnected.value) return false
        val payload = JSONObject().apply {
            put("id", deviceId)
            put("reqId", response.requestId)
            put("done", response.isComplete)
            val updatesArr = JSONArray()
            response.updates.forEach { update ->
                updatesArr.put(LocationRowWire.encode(update, deviceId, live = false))
            }
            put("updates", updatesArr)
        }
        emitCounted("backfill_response", payload)
        return true
    }

    override suspend fun sendGeofence(targetId: String, config: GeofenceConfig): Boolean =
        emitFromMonitor { GeofenceWire.encode(targetId, config) }

    override suspend fun sendSensorBehaviour(targetId: String, behaviour: SensorBehaviour): Boolean =
        emitFromMonitor { SensorBehaviourWire.encode(targetId, behaviour) }

    override suspend fun sendSensorSetup(report: SensorSetupReport): Boolean =
        emitFromSensor { deviceId ->
            SensorSetupWire.encode(report.trackerId.ifEmpty { deviceId }, report, sentAtMs = System.currentTimeMillis())
        }

    override suspend fun sendSensorEvents(events: List<EventEntry>, completeAfterMs: Long?): Boolean =
        emitFromSensor { deviceId -> SensorEventsWire.encode(deviceId, events, completeAfterMs) }

    override suspend fun requestSensorEvents(targetId: String, afterMs: Long): Boolean =
        emitFromMonitor { EventsRequestWire.encode(targetId, afterMs) }

    override suspend fun requestSensorInfo(targetId: String, withSatellites: Boolean): Boolean =
        emitFromMonitor { InfoRequestWire.encode(targetId, withSatellites) }

    override suspend fun sendSensorInfo(info: SensorInfo): Boolean =
        emitFromSensor { deviceId -> SensorInfoWire.encode(info.sensorId.ifEmpty { deviceId }, info) }

    override suspend fun requestGraphSamples(targetId: String, afterMs: Long): Boolean =
        emitFromMonitor { GraphRequestWire.encode(targetId, afterMs) }

    override suspend fun sendGraphSamples(samples: List<GraphSample>, compressed: Boolean): Boolean =
        emitFromSensor { deviceId -> GraphSamplesWire.encode(deviceId, samples, compressed) }

    /**
     * The one sender of every data event (SIMP-NOV22.9-S1): the payload leaves and its size counts towards
     * [bytesSent], so no event can be sent uncounted. Only the push-token control messages bypass it.
     */
    private fun emitCounted(event: String, payload: Any) {
        socket?.emit(event, payload)
        bytesSent += payload.toString().length.toLong()
    }

    /**
     * The one sender of the relay's settings_update event (SIMP-NOV22.3-S1): nothing is encoded or sent while
     * disconnected; a sent payload is counted by [emitCounted].
     */
    private inline fun emitSettings(encode: () -> JSONObject): Boolean {
        if (!_isConnected.value) return false
        emitCounted("settings_update", encode())
        return true
    }

    /** A Monitor -> Sensor message, stamped with this Monitor's ID by [SettingsWire.fromMonitor]. */
    private inline fun emitFromMonitor(body: () -> JSONObject): Boolean =
        emitSettings { SettingsWire.fromMonitor(body(), currentDeviceId) }

    /** A Sensor -> Monitor message, which needs this Sensor's own ID; false before [connect]. */
    private inline fun emitFromSensor(encode: (deviceId: String) -> JSONObject): Boolean {
        val deviceId = currentDeviceId ?: return false
        return emitSettings { encode(deviceId) }
    }

    private companion object {
        /**
         * The most a peer can flush at once. Socket callbacks use tryEmit, which drops
         * silently when a flow buffer is full (ISSUE-NOV6.3-O32).
         */
        const val BURST_CAPACITY = 5000
    }
}

/** The wire form of a telemetry row as the receiver reads it, from the relay socket or from an alarm push (ISSUE-NOV10.8-O86). */
object LocationRowWire {

    /**
     * One row as the Sensor sends it, live or as backfill. [live] adds what the forensic buffer does not keep:
     * `noiseOff`, and `crit`, which tells the relay to push the row to the Monitors as well (ISSUE-NOV10.8-O86).
     * Satellites, signal and noise travel on both since Nov17.3 (ISSUE-NOV15.5-O99 part 2); a value not known is left out.
     * Backfilled battery, charging and temperature are real since Nov17.7 (ISSUE-NOV17.3-O102), -1 when not known.
     * The light level travels on both since Nov22.11 as `lux` (SIMP-NOV22.4-S1), left out while not known.
     */
    fun encode(update: LocationUpdate, deviceId: String, live: Boolean): JSONObject = JSONObject().apply {
        put("id", update.deviceId.ifEmpty { deviceId })
        put("lat", update.lat)
        put("lng", update.lng)
        put("ts", update.timestamp)
        put("seq", update.sequenceId)
        put("status", update.status)
        put("vib", update.vibrationG)
        put("acc", update.accuracy)
        put("spd", update.speed)
        put("batt", update.batteryLevel)
        put("chg", update.isCharging)
        update.isPluggedIn?.let { put("plg", it) } // ISSUE-NOV17.15-O114; left out while not known.
        put("temp", update.tempCelsius)
        if (update.satsUsed >= 0) put("sats", update.satsUsed)
        if (update.satsInView >= 0) put("satsView", update.satsInView)
        update.meanCn0DbHz?.let { put("snr", it.toDouble()) }
        update.noiseDbfs?.let { put("noise", it.toDouble()) }
        update.lightLux?.let { put("lux", it.toDouble()) }
        if (live) {
            put("noiseOff", update.isNoiseDetectionOff)
            if (update.isCritical) put("crit", true)
        }
    }

    fun decode(data: JSONObject): LocationUpdate {
        return LocationUpdate(
            deviceId = data.optString("id", ""),
            lat = data.optDouble("lat"),
            lng = data.optDouble("lng"),
            timestamp = data.optLong("ts"),
            sequenceId = data.optLong("seq"),
            status = data.optString("status", "VALID"),
            accuracy = data.optDouble("acc", 0.0).toFloat(),
            speed = data.optDouble("spd", 0.0).toFloat(),
            batteryLevel = data.optInt("batt", 0),
            isCharging = data.optBoolean("chg", false),
            // Absent from Sensors before Nov17.15 and from backfill: the Monitor then reads `chg` (ISSUE-NOV17.15-O114).
            isPluggedIn = if (data.has("plg")) data.optBoolean("plg") else null,
            tempCelsius = data.optDouble("temp", 0.0).toFloat(),
            isNoiseDetectionOff = data.optBoolean("noiseOff", false), // Absent from Sensors before Nov9.1: no warning.
            // Absent from Sensors before Nov15.5, from backfill before Nov17.3, and while not known (O84 part 2, O99 part 2).
            satsUsed = data.optInt("sats", -1),
            satsInView = data.optInt("satsView", -1),
            meanCn0DbHz = if (data.has("snr")) data.optDouble("snr").toFloat() else null,
            noiseDbfs = if (data.has("noise")) data.optDouble("noise").toFloat() else null,
            // Absent from Sensors before Nov22.11 and while not known (SIMP-NOV22.4-S1).
            lightLux = if (data.has("lux")) data.optDouble("lux").toFloat() else null,
            vibrationG = data.optDouble("vib", 0.0).toFloat()
        )
    }

    /** Reads a row that the relay pushed as JSON text; null when the text is not a JSON object. */
    fun decodeText(text: String): LocationUpdate? = try {
        decode(JSONObject(text))
    } catch (e: org.json.JSONException) {
        null
    }
}

/**
 * Wire form of [GeofenceConfig] inside the relay's generic settings_update / settings_relay event
 * (ISSUE-NOV8.13-O51); `type` keeps other settings kinds possible on the same event.
 */
internal object GeofenceWire {
    const val TYPE = "geofence"

    fun encode(targetId: String, config: GeofenceConfig): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
        put("rev", config.revision)
        put("radius", config.radiusMeters)
        put("points", JSONArray().apply {
            config.points.forEach { p -> put(JSONObject().apply { put("lat", p.lat); put("lng", p.lng) }) }
        })
    }

    /** Returns null for another settings type or a malformed payload. */
    fun decode(data: JSONObject): GeofenceConfig? {
        if (data.optString("type") != TYPE) return null
        return try {
            val arr = data.getJSONArray("points")
            GeofenceConfig(
                points = (0 until arr.length()).map { i ->
                    val p = arr.getJSONObject(i)
                    HomePoint(p.getDouble("lat"), p.getDouble("lng"))
                },
                radiusMeters = data.getDouble("radius"),
                revision = data.getLong("rev")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse geofence settings_relay")
            null
        }
    }
}

/** Wire form of [SensorBehaviour] inside the relay's generic settings_update / settings_relay event (ISSUE-NOV10.8-O83). */
internal object SensorBehaviourWire {
    const val TYPE = "sensor_behaviour"

    fun encode(targetId: String, behaviour: SensorBehaviour): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
        put("rev", behaviour.revision)
        put("tilt", behaviour.tiltAlert)
        put("vib", behaviour.vibrationAlert)
        put("noise", behaviour.noiseAlert)
        put("lift", behaviour.liftAlert)
        put("tiltSens", behaviour.tiltSensitivity.toDouble())
        put("vibSens", behaviour.vibrationSensitivity.toDouble())
        put("noiseSens", behaviour.noiseSensitivity.toDouble())
        put("light", behaviour.lightAlert)
        put("lightSens", behaviour.lightSensitivity.toDouble())
    }

    /** Returns null for another settings type or a malformed payload. */
    fun decode(data: JSONObject): SensorBehaviour? {
        if (data.optString("type") != TYPE) return null
        return try {
            SensorBehaviour(
                tiltAlert = data.getBoolean("tilt"),
                vibrationAlert = data.getBoolean("vib"),
                noiseAlert = data.getBoolean("noise"),
                liftAlert = data.getBoolean("lift"),
                tiltSensitivity = data.getDouble("tiltSens").toFloat(),
                vibrationSensitivity = data.getDouble("vibSens").toFloat(),
                noiseSensitivity = data.getDouble("noiseSens").toFloat(),
                // Absent from Monitors before Nov22.4 (ISSUE-NOV22.4-O130): light detection on, at the default limit.
                lightAlert = data.optBoolean("light", true),
                lightSensitivity = data.optDouble("lightSens", DEFAULT_LIGHT_SENSITIVITY.toDouble()).toFloat(),
                revision = data.getLong("rev")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_behaviour settings_relay")
            null
        }
    }
}
