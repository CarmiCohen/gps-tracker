package com.jd.app.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.AlertSettings
import com.jd.core.ui.HudState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val identityManager: ForensicIdentityManager,
    private val signalingClient: SignalingClient,
    private val sensorDataStore: SensorDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val DEFAULT_RELAY_URL = "https://gps-relay-server.onrender.com"
    }

    val selfId = identityManager.selfId

    val relayUrl: StateFlow<String> = sensorDataStore.relayUrl
        .map { it.takeIf { url -> !url.isNullOrBlank() } ?: DEFAULT_RELAY_URL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_RELAY_URL)

    val alertSettings: StateFlow<AlertSettings> = sensorDataStore.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertSettings())

    val sirenType: StateFlow<String> = sensorDataStore.sirenType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Siren")

    private val _isSirenPlaying = MutableStateFlow(false)
    val isSirenPlaying: StateFlow<Boolean> = _isSirenPlaying.asStateFlow()

    fun updateAlertSettings(settings: AlertSettings) {
        viewModelScope.launch {
            sensorDataStore.saveAlertSettings(settings)
        }
    }

    fun updateSirenType(type: String) {
        viewModelScope.launch {
            sensorDataStore.saveSirenType(type)
        }
    }

    fun updateAlarmVolume(volume: Float) {
        viewModelScope.launch {
            val current = alertSettings.value
            sensorDataStore.saveAlertSettings(current.copy(alarmVolume = volume, useCustomVolume = true))
        }
    }

    fun testSirenAudio() {
        _isSirenPlaying.value = !_isSirenPlaying.value
    }


    fun updateRelayUrl(url: String) {
        viewModelScope.launch {
            val formattedUrl = url.trim()
            sensorDataStore.saveRelayUrl(formattedUrl)
        }
    }

    private val _ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val hudState: StateFlow<HudState> = combine(
        selfId,
        signalingClient.isConnected,
        signalingClient.viewerStatusFlow,
        _ticker
    ) { id, isConnected, isViewerConnected, _ ->
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = isLocationProviderEnabled(context)
        val batteryInfo = getBatteryInfo(context)

        HudState(
            appMode = "sensor",
            selfId = id,
            targetId = "V",
            isInternet = isInternet,
            isRelayConnected = isConnected,
            isLocalGpsActive = isGpsActive,
            isPeerActive = isViewerConnected,
            isDataHealthy = isInternet,
            watchdogOk = true,
            trackerState = "STATIONARY",
            speedKmh = 0f,
            batteryLevel = batteryInfo.level,
            isCharging = batteryInfo.isCharging,
            tempCelsius = batteryInfo.tempCelsius,
            accuracyMeters = 5.0f,
            telemetryAgeSec = 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "sensor"))

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

    private data class BatteryInfo(val level: Int, val isCharging: Boolean, val tempCelsius: Float)

    private fun getBatteryInfo(ctx: Context): BatteryInfo {
        return try {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
            BatteryInfo(pct, isCharging, tempTenths / 10.0f)
        } catch (_: Exception) {
            BatteryInfo(100, false, 30.0f)
        }
    }
}
