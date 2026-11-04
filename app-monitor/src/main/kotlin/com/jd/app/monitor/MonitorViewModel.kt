package com.jd.app.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.LocationUpdate
import com.jd.core.ui.HudState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val repository: MonitorRepository,
    private val identityManager: ForensicIdentityManager,
    private val signalingClient: SignalingClient,
    private val dataStore: MonitorDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val relayUrl: StateFlow<String> = dataStore.relayUrl
        .map { it.takeIf { url -> !url.isNullOrBlank() } ?: "https://gps-relay-server.onrender.com" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://gps-relay-server.onrender.com")

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetId = identityManager.targetId
    val selfId = identityManager.selfId

    private val _latestTelemetry = MutableStateFlow<LocationUpdate?>(null)

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
        _ticker
    ) { self, _ ->
        val isInternet = isNetworkAvailable(context)
        val isLocalGps = isLocationProviderEnabled(context)
        HudState(
            appMode = "monitor", // Cyan local GPS LED
            selfId = self,
            targetId = "LOCAL",
            isInternet = isInternet,
            isRelayConnected = isInternet,
            isLocalGpsActive = isLocalGps,
            isPeerActive = false,
            isDataHealthy = isInternet,
            watchdogOk = true,
            trackerState = "STATIONARY",
            speedKmh = 0f,
            batteryLevel = 100,
            isCharging = false,
            tempCelsius = 30f,
            accuracyMeters = 5.0f,
            telemetryAgeSec = 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "monitor"))

    val remoteHudState: StateFlow<HudState> = combine(
        selfId,
        targetId.filterNotNull(),
        signalingClient.isConnected,
        _latestTelemetry,
        _ticker
    ) { self, target, isConnected, latest, now ->
        val lastTs = latest?.timestamp ?: 0L
        val ageSec = if (lastTs > 0) (now - lastTs) / 1000 else -1L
        val hasValidCoords = latest != null && (latest.lat != 0.0 || latest.lng != 0.0)
        val isPeerFresh = lastTs > 0 && (now - lastTs) < 35000L && hasValidCoords
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = isPeerFresh
        val isRelayConnected = isInternet || isConnected
        val isDataHealthy = isInternet && isPeerFresh
        val speedKmh = (latest?.speed ?: 0f) * 3.6f
        val trackerState = if (speedKmh > 1.5f) "MOVING" else "STATIONARY"

        HudState(
            appMode = "sensor", // Green remote sensor GPS LED
            selfId = target,
            targetId = self,
            isInternet = isInternet,
            isRelayConnected = isRelayConnected,
            isLocalGpsActive = isGpsActive,
            isPeerActive = isPeerFresh,
            isDataHealthy = isDataHealthy,
            watchdogOk = true,
            trackerState = trackerState,
            speedKmh = speedKmh,
            batteryLevel = latest?.batteryLevel ?: 100,
            isCharging = latest?.isCharging ?: false,
            tempCelsius = latest?.tempCelsius ?: 30f,
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
