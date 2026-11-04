package com.jd.app.monitor

import android.content.Context
import android.location.LocationManager
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
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    val hudState: StateFlow<HudState> = combine(
        selfId,
        targetId.filterNotNull(),
        signalingClient.isConnected,
        _latestTelemetry,
        _ticker
    ) { self, target, isConnected, latest, now ->
        val lastTs = latest?.timestamp ?: 0L
        val ageSec = if (lastTs > 0) (now - lastTs) / 1000 else -1L
        val isPeerFresh = lastTs > 0 && (now - lastTs) < 35000L
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = isLocationProviderEnabled(context)
        val isDataHealthy = isInternet && isConnected && isPeerFresh
        val speedKmh = (latest?.speed ?: 0f) * 3.6f
        val trackerState = if (speedKmh > 1.5f) "MOVING" else "STATIONARY"

        HudState(
            appMode = "monitor",
            selfId = self,
            targetId = target,
            isInternet = isInternet,
            isRelayConnected = isConnected,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "monitor"))

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
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }
}
