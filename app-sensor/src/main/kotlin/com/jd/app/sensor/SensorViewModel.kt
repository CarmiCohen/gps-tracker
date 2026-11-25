package com.jd.app.sensor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.FILE_SAVED_LEVEL
import com.jd.core.shared.SettingsFileException
import com.jd.core.shared.settingsFileName
import com.jd.core.shared.EventLog
import com.jd.core.shared.ViewPeriod
import com.jd.core.shared.GraphHistory
import com.jd.core.shared.GraphScreenState
import com.jd.core.shared.graphScreenClock
import com.jd.core.shared.sensorGraphScreen
import com.jd.core.shared.InfoScreenState
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.atUiRate
import com.jd.core.ui.HudState
import com.jd.core.ui.gpsLed
import com.jd.core.ui.ownLeds
import com.jd.core.ui.peerLed
import com.jd.core.ui.relayLinkUp
import com.jd.core.ui.isLocationProviderEnabled
import com.jd.core.ui.isNetworkAvailable
import com.jd.core.ui.readBattery
import com.jd.core.ui.withLocalReadings
import com.jd.core.ui.FIX_CURRENT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val identityManager: ForensicIdentityManager,
    private val signalingClient: SignalingClient,
    private val sensorDataStore: SensorDataStore,
    private val fixStore: SensorFixStore,
    private val serviceStatus: SensorServiceStatus,
    private val eventLog: EventLog,
    private val infoSource: SensorInfoSource,
    private val graphHistory: GraphHistory,
    private val settingsTransfer: SensorSettingsTransfer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selfId = identityManager.selfId

    /** The ID of the Monitor heard last, null while none was (ISSUE-NOV22.7-O135). */
    val lastMonitorId: StateFlow<String?> = signalingClient.monitorIdFlow

    private val _transferMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** The outcome of each settings file export or import, shown once (ISSUE-NOV10.8-O80). */
    val transferMessages: SharedFlow<String> = _transferMessages.asSharedFlow()

    /** The name the file picker suggests for an export. */
    fun exportFileName(): String = settingsFileName("Sensor", System.currentTimeMillis())

    /**
     * [savedTo] is the shown path when the file went straight to the GPS-Tracker folder (Nov19.3). The event is a
     * DETAIL one, listed under More and kept on this phone (ISSUE-NOV25.12-O142).
     */
    fun exportSettings(uri: Uri, savedTo: String? = null) {
        viewModelScope.launch {
            val message = try {
                val file = settingsTransfer.export(uri)
                ("Settings exported: ID ${file.sensorId}, next sequence ${file.nextSequenceId ?: "unknown"}" +
                    (savedTo?.let { " to $it" } ?: ""))
                    .also { eventLog.record(FILE_SAVED_LEVEL, it) }
            } catch (e: Exception) {
                "Export failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                val file = settingsTransfer.import(uri)
                "Settings imported: ID ${file.sensorId}, ${file.geofence.points.size} home points"
                    .also { eventLog.record(EventLevel.INFO, it) }
            } catch (e: Exception) {
                "Import failed: ${e.userMessage()}".also { eventLog.record(EventLevel.WARNING, it) }
            }
            _transferMessages.emit(message)
        }
    }

    private fun Exception.userMessage(): String =
        if (this is SettingsFileException) message.orEmpty() else (message ?: javaClass.simpleName)

    /** This phone's events, oldest first (ISSUE-NOV10.8-O84). */
    val events: StateFlow<List<EventEntry>> = eventLog.events

    fun clearEvents() = eventLog.clear()

    /**
     * The Info screen, built from the running service every second while the screen is open (ISSUE-NOV10.8-O84 part 2).
     */
    val infoScreen: StateFlow<InfoScreenState> = flow {
        while (true) {
            emit(sensorInfoScreen(infoSource.current()))
            delay(INFO_REFRESH_MS)
        }
    }
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
     * The Graphs screen, rebuilt at each new sample, event, period and minute while it is open (ISSUE-NOV10.8-O84 part 3);
     * the events give the service's stops (ISSUE-NOV15.6-O100 part 3).
     */
    val graphScreen: StateFlow<GraphScreenState> = combine(
        graphHistory.samples,
        eventLog.events,
        graphPeriod,
        graphFocus,
        graphScreenClock()
    ) { samples, events, period, focus, now ->
        sensorGraphScreen(samples, period, now, events, focus)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), GraphScreenState())

    /** Reported by [SensorService] itself; drives the console's service card (ISSUE-NOV6.4-O34). */
    val isServiceRunning: StateFlow<Boolean> = serviceStatus.isRunning

    val serviceStartTimeMillis: StateFlow<Long> = serviceStatus.startTimeMillis

    /** What a background start was refused; opening the app restores it (ISSUE-NOV8.103-O60). */
    val refusedAccess: StateFlow<RefusedAccess> = serviceStatus.refusedAccess

    // The microphone and motion states are shown on the Info screen only (SIMP-NOV15.5-S1), from SensorService.buildInfo.

    private val _ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val serviceUptimeText: StateFlow<String> = combine(
        isServiceRunning,
        serviceStartTimeMillis,
        _ticker
    ) { running, startMs, _ ->
        if (running && startMs > 0L) {
            val nowMs = SensorServiceStatus.currentElapsedRealtime()
            formatUptime(startMs, nowMs)
        } else {
            "Stopped"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stopped")

    val relayUrl: StateFlow<String> = sensorDataStore.relayUrl
        .map { RelayConfig.resolve(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RelayConfig.DEFAULT_URL)

    /** Set on the Monitor and shown here read-only (ISSUE-NOV10.8-O83). */
    val sensorBehaviour: StateFlow<SensorBehaviour> = sensorDataStore.sensorBehaviour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SensorBehaviour())

    fun updateRelayUrl(url: String) {
        viewModelScope.launch {
            val formattedUrl = url.trim()
            sensorDataStore.saveRelayUrl(formattedUrl)
        }
    }

    /** Resets operational statistics and service uptime timer on Sensor (ISSUE-NOV7.1-O35). */
    fun resetOperationalStatistics() {
        serviceStatus.resetUptime()
    }

    /**
     * Triggers full system re-initialization: resets the relay URL to its default and the uptime timer (ISSUE-NOV7.1-O35).
     * The detection settings and the geofence belong to the Monitor and are kept (ISSUE-NOV10.8-O83).
     */
    fun triggerFullSystemReinitialization() {
        viewModelScope.launch {
            sensorDataStore.saveRelayUrl(RelayConfig.DEFAULT_URL)
            serviceStatus.resetUptime()
        }
    }

    private val uiFix = fixStore.fix.atUiRate()

    val hudState: StateFlow<HudState> = combine(
        selfId,
        signalingClient.isConnected,
        signalingClient.viewerStatusFlow,
        combine(uiFix, fixStore.satelliteFixTimeMs) { fix, satelliteFixMs -> fix to satelliteFixMs },
        _ticker
    ) { id, isConnected, isViewerConnected, (fix, satelliteFixMs), now ->
        val satelliteFixAgeMs = satelliteFixMs?.let { (now - it).coerceAtLeast(0L) }
        val isInternet = isNetworkAvailable(context)
        val isLinkUp = relayLinkUp(isInternet, isConnected)

        HudState(
            selfId = id,
            isRelayConnected = isConnected,
            isPeerActive = isViewerConnected,
            // INT, SRV, GPS and VWR in green, red or gray (Nov19.5); DAT (a copy of INT) and WDG are gone.
            leds = ownLeds(
                isInternet = isInternet,
                isRelayConnected = isLinkUp,
                gps = gpsLed(isLocationProviderEnabled(context), satelliteFixAgeMs),
                peerLabel = "VWR",
                peer = peerLed(isPaired = true, isRelayConnected = isLinkUp, isPeerHeard = isViewerConnected)
            )
        ).withLocalReadings(fix, readBattery(context), now, speedValidMs = FIX_CURRENT_MS) // As the telemetry (SIMP-NOV7.21-S1).
    }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState())
}
