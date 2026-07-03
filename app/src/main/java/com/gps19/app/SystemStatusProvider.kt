package com.gps19.app

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

interface SystemStatusProvider {
    fun isBatteryWhitelisted(): Boolean
    fun isAutoStartGranted(): Boolean
    fun isOverlayGranted(): Boolean
    fun isMicrophoneGranted(): Boolean
    fun isExactAlarmGranted(): Boolean
    fun isPostNotificationsGranted(): Boolean
    fun isBackgroundLocationGranted(): Boolean
    fun isLocalOnline(): Boolean
    fun isXiaomiSpecialPermissionGranted(): XiaomiPermissionStatus
    
    // v8.8.27: Unified permission state retrieval
    fun getPermissionState(): PermissionState
    
    // R945: Reactive Flows for system states
    fun observeInternetStatus(): Flow<Boolean>
    fun observeBatteryStatus(): Flow<BatteryStatus>
}

@Singleton
class SystemStatusProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemStatusProvider {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    // Rationale: Cache packageName to prevent repetitive getPackageName() log spam on Samsung devices.
    private val cachedPackageName = context.packageName

    override fun isBatteryWhitelisted(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(cachedPackageName)
    }

    override fun isAutoStartGranted(): Boolean {
        return if (isXiaomiDevice()) {
            isXiaomiAutostartGranted(context)
        } else {
            isBatteryWhitelisted()
        }
    }

    override fun isOverlayGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun isMicrophoneGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun isExactAlarmGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun isPostNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun isBackgroundLocationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun isLocalOnline(): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun isXiaomiSpecialPermissionGranted(): XiaomiPermissionStatus {
        return com.gps19.app.isXiaomiSpecialPermissionGranted(context)
    }

    override fun getPermissionState(): PermissionState {
        return PermissionState(
            isBatteryWhitelisted = isBatteryWhitelisted(),
            isAutoStartGranted = isAutoStartGranted(),
            isOverlayGranted = isOverlayGranted(),
            isMicrophoneGranted = isMicrophoneGranted(),
            isExactAlarmGranted = isExactAlarmGranted(),
            isPostNotificationsGranted = isPostNotificationsGranted(),
            isBackgroundLocationGranted = isBackgroundLocationGranted(),
            xiaomiStatus = isXiaomiSpecialPermissionGranted()
        )
    }

    override fun observeInternetStatus(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager.registerNetworkCallback(request, callback)
        trySend(isLocalOnline())
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    override fun observeBatteryStatus(): Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isCharging = plugged > 0
                val currentMa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
                trySend(BatteryStatus(pct, temp, isCharging, currentMa))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged().conflate()
}

data class BatteryStatus(
    val level: Int,
    val temp: Double,
    val isCharging: Boolean,
    val currentMa: Int = 0
)
