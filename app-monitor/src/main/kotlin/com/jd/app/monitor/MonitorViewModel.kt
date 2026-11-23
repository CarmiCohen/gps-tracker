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
import com.jd.core.shared.ForensicIdentity
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
import com.jd.core.ui.HudLed
import com.jd.core.ui.HudState
import com.jd.core.ui.gpsLed
import com.jd.core.ui.isSatelliteFix
import com.jd.core.ui.ownLeds
import com.jd.core.ui.peerLed
import com.jd.core.ui.relayLinkUp
import com.jd.core.ui.remoteGpsLed
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
    private val setupReports: SensorSetupReports,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _transferMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** The outcome of each settings file export or import, shown once (ISSUE-NOV10.8-O80). */
    val transferMessages: SharedFlow<String> = _transferMessages.asSharedFlow()

    /** The name the file picker suggests for an export. */
    fun exportFileName(): String = settingsFileName("Monitor", System.currentTimeMillis())

    /** [savedTo] is the shown path when the file went straight to the GPS-Tracker folder (Nov19.3). */
    fun exportSettings(uri: Uri, savedTo: String? = null) {
        viewModelScope.launch {
            val message = try {
                val file = settingsTransfer.export(uri)
                ("Settings exported: ${file.geofence.points.size} home points, ${file.trails.size} trail points" + savedTo.asSuffix())
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

    fun saveTrails(uri: Uri, savedTo: String? = null) {
        viewModelScope.launch {
            val message = try {
                ("Trails saved: ${trailTransfer.save(uri).trails.size} points" + savedTo.asSuffix()).also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Trail save failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    private fun String?.asSuffix(): String = if (this == null) "" else " to $this"

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
     * shown (ISSUE-NOV10.8-O83 part 2). It came by the relay connection or by push (ISSUE-NOV10.8-O86).
     */
    val sensorSetup: StateFlow<SensorSetupReport?> = combine(setupReports.latest, identityManager.targetId) { report, target ->
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

    /**
     * Sets this Monitor's own ID, set in its Setup dialog (ISSUE-NOV22.7-O135): the session rejoins the relay under it
     * and the Sensor shows it. The ID is set before the trail moves, so no new fix is left under the old one.
     */
    fun updateMonitorId(id: String) {
        val newId = id.trim()
        viewModelScope.launch {
            val oldId = identityManager.selfId.first()
            if (newId == oldId || ForensicIdentity.monitorIdProblem(newId, identityManager.targetId.first()) != null) return@launch
            identityManager.setSelfId(newId)
            repository.moveLocalTrail(oldId, newId)
            eventLog.record(EventLevel.INFO, "Monitor ID changed from $oldId to $newId")
        }
    }

    /** The user's Home Points and radius, enforced by the tracker (ISSUE-NOV8.13-O51). */
    val geofence: StateFlow<GeofenceConfig> = dataStore.geofence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeofenceConfig())

    fun addHomePoint(lat: Double, lng: Double) {
        viewModelScope.launch { dataStore.editGeofence { it.withPoint(HomePoint(lat, lng)) } }
        eventLog.record(EventLevel.DETAIL, "Home point added") // Home Point changes are details (ISSUE-NOV23.4-O139).
    }

    fun removeHomePoint(index: Int) {
        viewModelScope.launch { dataStore.editGeofence { it.withoutPoint(index) } }
        eventLog.record(EventLevel.DETAIL, "Home point ${index + 1} removed")
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
        eventLog.record(EventLevel.DETAIL, "All home points removed")
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
            setupReports.forget()
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

    /** When this Monitor's GNSS chip last reported a satellite fix (`isSatelliteFix`), for its GPS LED (Nov19.5). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastSatelliteFixMs: StateFlow<Long?> = hasLocationPermission
        .flatMapLatest { granted -> if (granted) locationSource.satellitesUsed() else emptyFlow() }
        .filter(::isSatelliteFix)
        .map { System.currentTimeMillis() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refreshLocationPermission() {
        hasLocationPermission.value = locationSource.hasPermission()
    }

    private val _latestTelemetry = MutableStateFlow<LocationUpdate?>(null)
    private val pairedTelemetry = combine(_latestTelemetry, identityManager.targetId) { latest, target -> latest.ifFrom(target) }
    private val uiTelemetry = pairedTelemetry.atUiRate()

    /** The Sensor and time (its clock) of its newest row with a satellite fix, for the gray of its GPS LED (Nov19.5). */
    private val lastSensorFix = MutableStateFlow<Pair<String, Long>?>(null)

    init {
        signalingClient.telemetryFlow
            .onEach { update ->
                _latestTelemetry.value = update
                // The newest time only: a late backfilled row must not move it back.
                if (isSatelliteFix(update.satsUsed)) lastSensorFix.update { last ->
                    if (last == null || last.first != update.deviceId || update.timestamp > last.second) update.deviceId to update.timestamp else last
                }
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
        combine(selfId, identityManager.targetId) { self, target -> self to target },
        signalingClient.isConnected,
        uiTelemetry,
        combine(localFix, lastSatelliteFixMs) { fix, satelliteFixMs -> fix to satelliteFixMs },
        _ticker
    ) { (self, target), isConnected, latest, (fix, satelliteFixMs), now ->
        val lastTs = latest?.timestamp ?: 0L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < TRACKER_PRESENT_MS
        val satelliteFixAgeMs = satelliteFixMs?.let { (now - it).coerceAtLeast(0L) }
        val isInternet = isNetworkAvailable(context)
        val isLinkUp = relayLinkUp(isInternet, isConnected)

        HudState(
            appMode = "monitor",
            selfId = self,
            isRelayConnected = isConnected,
            isPeerActive = isTrackerPresent,
            // INT, SRV, GPS and TRK in green, red or gray (Nov19.5); DAT (INT and SRV again) and WDG are gone.
            leds = ownLeds(
                isInternet = isInternet,
                isRelayConnected = isLinkUp,
                gps = gpsLed(isLocationProviderEnabled(context), satelliteFixAgeMs),
                peerLabel = "TRK",
                peer = peerLed(isPaired = !target.isNullOrBlank(), isRelayConnected = isLinkUp, isPeerHeard = isTrackerPresent)
            )
        ).withLocalReadings(fix?.reading, readBattery(context), now)
    }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "monitor"))

    val remoteHudState: StateFlow<HudState> = combine(
        identityManager.targetId.filterNotNull(),
        signalingClient.isConnected,
        uiTelemetry,
        lastSensorFix,
        _ticker
    ) { target, isConnected, latest, lastFix, now ->
        val lastTs = latest?.timestamp ?: 0L
        val ageSec = if (lastTs > 0) (now - lastTs) / 1000 else -1L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < TRACKER_PRESENT_MS
        val isGpsActive = latest != null && (latest.lat != 0.0 || latest.lng != 0.0)
        val speedMps = latest?.takeIf { isGpsActive && it.speed >= 0f }?.speed
        val speedKmh = speedMps?.let(::metersPerSecondToKmh)
        val lastFixAgeMs = lastFix?.takeIf { it.first == target }?.let { (now - it.second).coerceAtLeast(0L) }

        HudState(
            appMode = "sensor",
            selfId = target, // this HUD describes the paired Sensor (ISSUE-NOV17.7-O105)
            isRelayConnected = isConnected,
            isPeerActive = isTrackerPresent,
            // GPS alone (Nov19.5): the Monitor cannot see the Sensor's internet or relay link, and TRK already tells it is heard.
            leds = listOf(
                HudLed("GPS", remoteGpsLed(isTrackerPresent, latest?.satsUsed ?: -1, lastFixAgeMs))
            ),
            trackerState = motionState(speedKmh),
            speedKmh = speedKmh,
            batteryLevel = latest?.batteryLevel,
            isCharging = latest?.isCharging ?: false,
            tempCelsius = latest?.tempCelsius,
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
            setupReports.forget() // The Sensor phone warnings were about the unpaired tracker.
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
