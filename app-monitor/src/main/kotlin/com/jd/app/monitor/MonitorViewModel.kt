package com.jd.app.monitor

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventLog
import com.jd.core.shared.ViewPeriod
import com.jd.core.shared.GRAPH_REQUEST_INTERVAL_MS
import com.jd.core.shared.GraphScreenState
import com.jd.core.shared.graphScreenClock
import com.jd.core.shared.monitorGraphScreen
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.shared.InfoScreenState
import com.jd.core.shared.SENSOR_INFO_REQUEST_INTERVAL_MS
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.SensorSetupReport
import com.jd.core.shared.SettingsFileException
import com.jd.core.shared.settingsFileName
import com.jd.core.shared.atUiRate
import com.jd.core.ui.HudState
import com.jd.core.ui.getAppVersionName
import com.jd.core.ui.metersPerSecondToKmh
import com.jd.core.ui.motionState
import com.jd.core.ui.readBattery
import com.jd.core.ui.readPhoneState
import com.jd.core.ui.withLocalReadings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Returns this update only if it came from [deviceId], so another device's data is never displayed. */
fun LocationUpdate?.ifFrom(deviceId: String?): LocationUpdate? = this?.takeIf { it.deviceId == deviceId }

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val repository: MonitorRepository,
    private val identityManager: ForensicIdentityManager,
    private val signalingClient: SignalingClient,
    private val dataStore: MonitorDataStore,
    private val locationSource: MonitorLocationSource,
    private val alarmEngine: MonitorAlarmEngine,
    private val eventLog: EventLog,
    private val graphs: MonitorGraphs,
    private val settingsTransfer: MonitorSettingsTransfer,
    private val trailTransfer: MonitorTrailTransfer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _transferMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** The outcome of each settings file export or import, shown once (ISSUE-NOV10.8-O80). */
    val transferMessages: SharedFlow<String> = _transferMessages.asSharedFlow()

    /** The name the file picker suggests for an export. */
    fun exportFileName(): String = settingsFileName("Monitor", System.currentTimeMillis())

    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                val file = settingsTransfer.export(uri)
                "Settings exported: ${file.geofence.points.size} home points, ${file.trails.size} trail points"
                    .also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Export failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            val pairedBefore = pairing.value?.trackerId
            val message = try {
                val file = settingsTransfer.import(uri)
                // Another Sensor's samples must not mix with the imported one's (ISSUE-NOV10.8-O84 part 3).
                if (file.trackerId != pairedBefore) graphs.sensor.clear()
                "Settings imported: tracker ${file.trackerId ?: "none"}, ${file.geofence.points.size} home points, ${file.trails.size} trail points"
                    .also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Import failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    /** The name the file picker suggests for the map's SAVE (ISSUE-NOV17.7-O107). */
    fun suggestedTrailFileName(): String = trailFileName(System.currentTimeMillis())

    fun saveTrails(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                "Trails saved: ${trailTransfer.save(uri).trails.size} points".also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Trail save failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    fun loadTrails(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                val file = trailTransfer.load(uri)
                val other = file.trackerId?.takeIf { it != pairing.value?.trackerId }
                ("Trails loaded: ${file.trails.size} points" + (other?.let { ", of tracker $it, not shown on this map" } ?: ""))
                    .also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Trail load failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    /** The map's CLEAR: the stored trails go, and the Sensor's older rows stay away (ISSUE-NOV17.7-O107). */
    fun clearTrails() {
        viewModelScope.launch { repository.clearTrail(pairing.value?.trackerId) }
        eventLog.record(EventLevel.INFO, "Trails cleared")
    }

    private fun Exception.userMessage(): String =
        if (this is SettingsFileException) message.orEmpty() else (message ?: javaClass.simpleName)

    /** This phone's events and the paired Sensor's, oldest first (ISSUE-NOV10.8-O84). */
    val events: StateFlow<List<EventEntry>> = eventLog.events

    fun clearEvents() = eventLog.clear()

    /**
     * The stored pairing; null only until it has been read. The ViewModel outlives Activity recreation,
     * so a recreated screen sees the pairing at once instead of a placeholder (ISSUE-NOV8.5-O50).
     */
    val pairing: StateFlow<Pairing?> = identityManager.targetId
        .map { Pairing(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val selfId = identityManager.selfId

    val relayUrl: StateFlow<String> = dataStore.relayUrl
        .map { RelayConfig.resolve(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RelayConfig.DEFAULT_URL)

    val alertSettings: StateFlow<AlertSettings> = dataStore.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertSettings())

    val sirenType: StateFlow<String> = dataStore.sirenType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Siren")

    /** True while the siren really plays, for an alarm or a test. */
    val isSirenPlaying: StateFlow<Boolean> = alarmEngine.isSirenOn

    val alarms: StateFlow<AlarmBook> = alarmEngine.alarms

    /**
     * The paired Sensor's newest setup report, or null while none has arrived; a report of another tracker is never
     * shown (ISSUE-NOV10.8-O83 part 2).
     */
    val sensorSetup: StateFlow<SensorSetupReport?> = combine(signalingClient.sensorSetupFlow, identityManager.targetId) { report, target ->
        report?.takeIf { it.trackerId == target }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun acknowledgeAlarms() {
        alarmEngine.acknowledge()
    }

    fun updateAlertSettings(settings: AlertSettings) {
        viewModelScope.launch {
            dataStore.saveAlertSettings(settings)
        }
    }

    fun updateSirenType(type: String) {
        viewModelScope.launch {
            dataStore.saveSirenType(type)
        }
    }

    fun updateAlarmVolume(volume: Float) {
        viewModelScope.launch {
            val current = alertSettings.value
            dataStore.saveAlertSettings(current.copy(alarmVolume = volume, useCustomVolume = true))
        }
    }

    fun testSirenAudio() {
        alarmEngine.toggleSirenTest()
    }

    fun updateRelayUrl(url: String) {
        viewModelScope.launch {
            dataStore.saveRelayUrl(url.trim())
        }
    }

    /** The user's Home Points and radius, enforced by the tracker (ISSUE-NOV8.13-O51). */
    val geofence: StateFlow<GeofenceConfig> = dataStore.geofence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeofenceConfig())

    fun addHomePoint(lat: Double, lng: Double) {
        viewModelScope.launch { dataStore.editGeofence { it.withPoint(HomePoint(lat, lng)) } }
        eventLog.record(EventLevel.INFO, "Home point added")
    }

    fun removeHomePoint(index: Int) {
        viewModelScope.launch { dataStore.editGeofence { it.withoutPoint(index) } }
        eventLog.record(EventLevel.INFO, "Home point ${index + 1} removed")
    }

    fun setGeofenceRadius(meters: Double) {
        viewModelScope.launch { dataStore.editGeofence { it.withRadius(meters) } }
    }

    /**
     * Removes every Home Point, which the tracker receives as an empty geofence (ISSUE-NOV7.1-O35, ISSUE-NOV8.13-O51).
     * Alarm settings stay as they are, so Home Points added later raise breach alarms again (user decision 2026-11-08).
     */
    fun clearHomePoints() {
        viewModelScope.launch { dataStore.editGeofence { it.cleared() } }
        eventLog.record(EventLevel.INFO, "All home points removed")
    }

    /** Resets operational statistics and clears stored trail history (ISSUE-NOV7.1-O35). */
    fun resetOperationalStatistics() {
        viewModelScope.launch {
            repository.clearTrail(pairing.value?.trackerId)
            _latestTelemetry.value = null
            alarmEngine.acknowledge()
        }
        eventLog.record(EventLevel.INFO, "Trail history cleared")
    }

    /** Triggers full system re-initialization: unpairs target, resets settings & URL to defaults, clears trail (ISSUE-NOV7.1-O35). */
    fun triggerFullSystemReinitialization() {
        viewModelScope.launch {
            identityManager.clearTargetId()
            dataStore.saveAlertSettings(AlertSettings())
            dataStore.saveSirenType("Siren")
            dataStore.saveRelayUrl(RelayConfig.DEFAULT_URL)
            dataStore.editGeofence { GeofenceConfig() }
            repository.clearTrail(trackerId = null) // Unpaired: a new pairing gets the Sensor's whole history.
            _latestTelemetry.value = null
            alarmEngine.acknowledge()
        }
        graphs.sensor.clear()
        graphs.own.clear()
        eventLog.record(EventLevel.WARNING, "Full re-initialization: unpaired from ${pairing.value?.trackerId}, settings reset")
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val trail: StateFlow<List<TrailEntity>> = identityManager.targetId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.getTrail(id)
        }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val localTrail: StateFlow<List<TrailEntity>> = selfId
        .filter { it != "LOADING..." && it.isNotBlank() }
        .flatMapLatest { id ->
            repository.getTrail(id)
        }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val hasLocationPermission = MutableStateFlow(locationSource.hasPermission())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val localFix: StateFlow<LocalFix?> = hasLocationPermission
        .flatMapLatest { granted -> if (granted) locationSource.fixes() else emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val localPosition: StateFlow<MapPosition?> = localFix
        .map { it?.position }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refreshLocationPermission() {
        hasLocationPermission.value = locationSource.hasPermission()
    }

    private val _latestTelemetry = MutableStateFlow<LocationUpdate?>(null)
    private val pairedTelemetry = combine(_latestTelemetry, identityManager.targetId) { latest, target -> latest.ifFrom(target) }
    private val uiTelemetry = pairedTelemetry.atUiRate()

    init {
        signalingClient.telemetryFlow
            .onEach { update ->
                _latestTelemetry.value = update
            }
            .launchIn(viewModelScope)

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            selfId
                .filter { it != "LOADING..." && it.isNotBlank() }
                .flatMapLatest { id ->
                    localFix.filterNotNull().map { fix -> Pair(id, fix) }
                }
                .collect { (id, fix) ->
                    repository.insertLocalTrail(id, fix)
                }
        }
    }

    private val _ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val localHudState: StateFlow<HudState> = combine(
        selfId,
        signalingClient.isConnected,
        uiTelemetry,
        localFix,
        _ticker
    ) { self, isConnected, latest, fix, now ->
        val isInternet = isNetworkAvailable(context)
        val isLocalGps = isLocationProviderEnabled(context)
        val lastTs = latest?.timestamp ?: 0L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < TRACKER_PRESENT_MS

        HudState(
            appMode = "monitor", // Cyan local GPS LED
            selfId = self,
            isInternet = isInternet,
            isRelayConnected = isConnected,       // SRV: Green ONLY if connected to Relay
            isLocalGpsActive = isLocalGps,
            isPeerActive = isTrackerPresent,       // TRK: Green ONLY if Tracker T is connected & active
            isDataHealthy = isInternet && isConnected,
            watchdogOk = true
        ).withLocalReadings(fix?.reading, readBattery(context), now)
    }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "monitor"))

    val remoteHudState: StateFlow<HudState> = combine(
        identityManager.targetId.filterNotNull(),
        signalingClient.isConnected,
        uiTelemetry,
        _ticker
    ) { target, isConnected, latest, now ->
        val lastTs = latest?.timestamp ?: 0L
        val ageSec = if (lastTs > 0) (now - lastTs) / 1000 else -1L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < TRACKER_PRESENT_MS
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = latest != null && (latest.lat != 0.0 || latest.lng != 0.0)
        val speedMps = latest?.takeIf { isGpsActive && it.speed >= 0f }?.speed
        val speedKmh = speedMps?.let(::metersPerSecondToKmh)

        HudState(
            appMode = "sensor", // Green remote sensor GPS LED
            selfId = target, // this HUD describes the paired Sensor (ISSUE-NOV17.7-O105)
            isInternet = isInternet,
            isRelayConnected = isConnected,          // SRV: Green ONLY if connected to Relay
            isLocalGpsActive = isGpsActive,
            isPeerActive = isTrackerPresent,          // Green ONLY if remote Tracker T is present and active
            isDataHealthy = isInternet && isTrackerPresent,
            watchdogOk = true,
            trackerState = motionState(speedKmh),
            speedKmh = speedKmh,
            batteryLevel = latest?.takeIf { it.batteryLevel >= 0 }?.batteryLevel,
            isCharging = latest?.isCharging ?: false,
            tempCelsius = latest?.takeIf { it.tempCelsius > 0f }?.tempCelsius,
            accuracyMeters = latest?.accuracy ?: 0f,
            telemetryAgeSec = ageSec,
            sequenceId = latest?.sequenceId ?: 0L
        )
    }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "sensor"))

    val hudState: StateFlow<HudState> = remoteHudState

    /** The paired Sensor's newest Info; another tracker's is never shown (ISSUE-NOV10.8-O84 part 2). */
    private val pairedSensorInfo = combine(signalingClient.sensorInfoFlow, identityManager.targetId) { received, target ->
        received?.takeIf { it.info.sensorId == target }
    }

    private val satelliteListOpen = MutableStateFlow(false)

    /** The Info screen's satellite list opened or closed: only an open list asks for it (ISSUE-NOV15.5-O99 part 1). */
    fun setSatelliteListOpen(open: Boolean) {
        satelliteListOpen.value = open
    }

    /**
     * Asks the paired Sensor for its Info every [SENSOR_INFO_REQUEST_INTERVAL_MS] while collected, with the satellite
     * list while it is open; opening the list asks at once. Emits nothing.
     */
    private val sensorInfoRequests = flow<InfoScreenState> {
        satelliteListOpen.collectLatest { withSatellites ->
            while (true) {
                identityManager.targetId.first()?.let { signalingClient.requestSensorInfo(it, withSatellites) }
                delay(SENSOR_INFO_REQUEST_INTERVAL_MS)
            }
        }
    }

    /**
     * The Info screen (ISSUE-NOV10.8-O84 part 2), rebuilt every second while it is open; only then is the Sensor asked
     * for its Info, so a closed screen costs no data.
     */
    val infoScreen: StateFlow<InfoScreenState> = merge(
        sensorInfoRequests,
        combine(_ticker, pairedSensorInfo, pairedTelemetry, signalingClient.linkStats, localFix) { now, received, row, link, fix ->
            monitorInfoScreen(
                received = received,
                newestRow = row,
                link = link,
                localFix = fix,
                phone = readPhoneState(context),
                monitorId = selfId.first(),
                version = getAppVersionName(context),
                nowMs = now
            )
        }
    )
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), InfoScreenState())

    private val _graphPeriod = MutableStateFlow(ViewPeriod.DAY)

    /** The period the Graphs screen shows; kept here so it survives a rotation (ISSUE-NOV10.8-O84 part 3). */
    val graphPeriod: StateFlow<ViewPeriod> = _graphPeriod.asStateFlow()

    fun setGraphPeriod(period: ViewPeriod) {
        _graphPeriod.value = period
    }

    private val graphFocus = MutableStateFlow<Long?>(null)

    /**
     * Opens the Graphs around an event's [timeMs], over the hour around it, or with null over the period ending now
     * (ISSUE-NOV15.6-O100 part 4).
     */
    fun showGraphsAt(timeMs: Long?) {
        graphFocus.value = timeMs
        if (timeMs != null) _graphPeriod.value = ViewPeriod.HOUR
    }

    /**
     * Asks the paired Sensor for the samples newer than the newest held, at once and every [GRAPH_REQUEST_INTERVAL_MS]
     * while collected. Emits nothing.
     */
    private val graphRequests = flow<GraphScreenState> {
        while (true) {
            identityManager.targetId.first()?.let { signalingClient.requestGraphSamples(it, graphs.sensor.newestTimeMs) }
            delay(GRAPH_REQUEST_INTERVAL_MS)
        }
    }

    /**
     * The Graphs screen (ISSUE-NOV10.8-O84 part 3): the Sensor's samples and this phone's own, rebuilt at each new sample,
     * period and minute while it is open; only then is the Sensor asked for its samples, so a closed screen costs no data.
     * The paired Sensor's events in this phone's log give its service stops (ISSUE-NOV15.6-O100 parts 3 and 7).
     */
    val graphScreen: StateFlow<GraphScreenState> = merge(
        graphRequests,
        combine(
            graphs.sensor.samples,
            graphs.own.samples,
            combine(eventLog.events, identityManager.targetId, ::Pair),
            combine(graphPeriod, graphFocus, ::Pair),
            graphScreenClock()
        ) { sensorSamples, ownSamples, (events, trackerId), (period, focus), now ->
            monitorGraphScreen(sensorSamples, ownSamples, period, now, events, focus, trackerId)
        }
    )
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), GraphScreenState())

    fun setTargetId(id: String) {
        viewModelScope.launch {
            identityManager.setTargetId(id)
        }
        eventLog.record(EventLevel.INFO, "Paired with tracker $id")
    }

    fun clearTargetId() {
        eventLog.record(EventLevel.WARNING, "Unpaired from tracker ${pairing.value?.trackerId}")
        graphs.sensor.clear() // The next Sensor's samples must not mix with this one's (ISSUE-NOV10.8-O84 part 3).
        viewModelScope.launch {
            identityManager.clearTargetId()
        }
    }

    private fun isNetworkAvailable(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun isLocationProviderEnabled(ctx: Context): Boolean {
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                    lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }
}
