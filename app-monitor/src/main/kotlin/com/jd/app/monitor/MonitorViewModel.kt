package com.jd.app.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.atUiRate
import com.jd.core.ui.HudState
import com.jd.core.ui.readBattery
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    val relayUrl: StateFlow<String> = dataStore.relayUrl
        .map { RelayConfig.resolve(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RelayConfig.DEFAULT_URL)

    val alertSettings: StateFlow<AlertSettings> = dataStore.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertSettings())

    val sirenType: StateFlow<String> = dataStore.sirenType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Siren")

    private val _isSirenPlaying = MutableStateFlow(false)
    val isSirenPlaying: StateFlow<Boolean> = _isSirenPlaying.asStateFlow()

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
        _isSirenPlaying.value = !_isSirenPlaying.value
    }

    fun updateRelayUrl(url: String) {
        viewModelScope.launch {
            dataStore.saveRelayUrl(url.trim())
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val trail: StateFlow<List<TrailEntity>> = identityManager.targetId
        .filterNotNull()
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

    val targetId = identityManager.targetId
    val selfId = identityManager.selfId

    private val _latestTelemetry = MutableStateFlow<LocationUpdate?>(null)
    private val pairedTelemetry = combine(_latestTelemetry, targetId) { latest, target -> latest.ifFrom(target) }

    init {
        signalingClient.telemetryFlow
            .onEach { update ->
                _latestTelemetry.value = update
            }
            .launchIn(viewModelScope)
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
        pairedTelemetry,
        localFix,
        _ticker
    ) { self, isConnected, latest, fix, now ->
        val isInternet = isNetworkAvailable(context)
        val isLocalGps = isLocationProviderEnabled(context)
        val lastTs = latest?.timestamp ?: 0L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < 35000L

        HudState(
            appMode = "monitor", // Cyan local GPS LED
            selfId = self,
            targetId = "LOCAL",
            isInternet = isInternet,
            isRelayConnected = isConnected,       // SRV: Green ONLY if connected to Relay
            isLocalGpsActive = isLocalGps,
            isPeerActive = isTrackerPresent,       // TRK: Green ONLY if Tracker T is connected & active
            isDataHealthy = isInternet && isConnected,
            watchdogOk = true
        ).withLocalReadings(fix, readBattery(context), now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "monitor"))

    val remoteHudState: StateFlow<HudState> = combine(
        selfId,
        targetId.filterNotNull(),
        signalingClient.isConnected,
        pairedTelemetry,
        _ticker
    ) { self, target, isConnected, latest, now ->
        val lastTs = latest?.timestamp ?: 0L
        val ageSec = if (lastTs > 0) (now - lastTs) / 1000 else -1L
        val isTrackerPresent = lastTs > 0 && (now - lastTs) < 35000L
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = latest != null && (latest.lat != 0.0 || latest.lng != 0.0)
        val speedKmh = latest?.takeIf { isGpsActive }?.speed?.let(::metersPerSecondToKmh)

        HudState(
            appMode = "sensor", // Green remote sensor GPS LED
            selfId = target,
            targetId = self,
            isInternet = isInternet,
            isRelayConnected = isConnected,          // SRV: Green ONLY if connected to Relay
            isLocalGpsActive = isGpsActive,
            isPeerActive = isTrackerPresent,          // Green ONLY if remote Tracker T is present and active
            isDataHealthy = isInternet && isTrackerPresent,
            watchdogOk = true,
            trackerState = motionState(speedKmh),
            speedKmh = speedKmh,
            batteryLevel = latest?.batteryLevel,
            isCharging = latest?.isCharging ?: false,
            tempCelsius = latest?.tempCelsius,
            accuracyMeters = latest?.accuracy ?: 0f,
            telemetryAgeSec = ageSec,
            sequenceId = latest?.sequenceId ?: 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "sensor"))

    val hudState: StateFlow<HudState> = remoteHudState

    fun setTargetId(id: String) {
        viewModelScope.launch {
            identityManager.setTargetId(id)
        }
    }

    fun clearTargetId() {
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
