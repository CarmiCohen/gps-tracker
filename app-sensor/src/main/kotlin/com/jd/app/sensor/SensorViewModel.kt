package com.jd.app.sensor

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.RelayConfig
import com.jd.core.shared.atUiRate
import com.jd.core.ui.HudState
import com.jd.core.ui.readBattery
import com.jd.core.ui.withLocalReadings
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
    private val fixStore: SensorFixStore,
    private val serviceStatus: SensorServiceStatus,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selfId = identityManager.selfId

    /** Reported by [SensorService] itself; shown in the Setup dialog (ISSUE-NOV6.4-O34). */
    val isServiceRunning: StateFlow<Boolean> = serviceStatus.isRunning

    val serviceStartTimeMillis: StateFlow<Long> = serviceStatus.startTimeMillis

    /** What the noise detection is doing, reported by [SensorService] (ISSUE-NOV5.2-O11 Group 4). */
    val microphoneState: StateFlow<MicrophoneState> = serviceStatus.microphone

    /** Whether tilt and lift are armed, reported by [SensorService] (ISSUE-NOV5.2-O11 Group 5). */
    val motionState: StateFlow<MotionState> = serviceStatus.motion

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

    val alertSettings: StateFlow<AlertSettings> = sensorDataStore.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertSettings())

    val sirenType: StateFlow<String> = sensorDataStore.sirenType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Siren")

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

    /** Triggers full system re-initialization: resets settings & relay URL to defaults, resets uptime timer (ISSUE-NOV7.1-O35). */
    fun triggerFullSystemReinitialization() {
        viewModelScope.launch {
            sensorDataStore.saveAlertSettings(AlertSettings())
            sensorDataStore.saveSirenType("Siren")
            sensorDataStore.saveRelayUrl(RelayConfig.DEFAULT_URL)
            serviceStatus.resetUptime()
        }
    }

    private val uiFix = fixStore.fix.atUiRate()

    val hudState: StateFlow<HudState> = combine(
        selfId,
        signalingClient.isConnected,
        signalingClient.viewerStatusFlow,
        uiFix,
        _ticker
    ) { id, isConnected, isViewerConnected, fix, now ->
        val isInternet = isNetworkAvailable(context)
        val isGpsActive = isLocationProviderEnabled(context)

        HudState(
            appMode = "sensor",
            selfId = id,
            targetId = "V",
            isInternet = isInternet,
            isRelayConnected = isConnected,
            isLocalGpsActive = isGpsActive,
            isPeerActive = isViewerConnected,
            isDataHealthy = isInternet,
            watchdogOk = true
        ).withLocalReadings(fix, readBattery(context), now)
    }
        .atUiRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState(appMode = "sensor"))

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
