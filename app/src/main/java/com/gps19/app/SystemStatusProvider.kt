package com.gps19.app

import android.Manifest
import android.app.AlarmManager
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
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface SystemStatusProvider {
    suspend fun isBatteryWhitelisted(): Boolean
    suspend fun isAutoStartGranted(): Boolean
    suspend fun isOverlayGranted(): Boolean
    suspend fun isMicrophoneGranted(): Boolean
    suspend fun isExactAlarmGranted(): Boolean
    suspend fun isPostNotificationsGranted(): Boolean
    suspend fun isBackgroundLocationGranted(): Boolean
    fun isLocalOnline(): Boolean
    suspend fun isXiaomiSpecialPermissionGranted(): XiaomiPermissionStatus
    
    /**
     * v9.3.45: Hardened for ANR prevention. Implements non-blocking polling 
     * to avoid IPC congestion on low-end hardware (Samsung A15).
     */
    suspend fun getPermissionState(forceRefresh: Boolean = false): PermissionState
    
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
    
    private val cachedPackageName = context.packageName
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastFullRefreshTime: Long = 0
    private val cachedState = AtomicReference<PermissionState>(PermissionState())
    private val isRefreshing = AtomicBoolean(false)
    
    private val PERMISSION_TTL_MS = 15_000L

    override suspend fun isBatteryWhitelisted(): Boolean = getPermissionState().isBatteryWhitelisted
    override suspend fun isAutoStartGranted(): Boolean = getPermissionState().isAutoStartGranted
    override suspend fun isOverlayGranted(): Boolean = getPermissionState().isOverlayGranted
    override suspend fun isMicrophoneGranted(): Boolean = getPermissionState().isMicrophoneGranted
    override suspend fun isExactAlarmGranted(): Boolean = getPermissionState().isExactAlarmGranted
    override suspend fun isPostNotificationsGranted(): Boolean = getPermissionState().isPostNotificationsGranted
    override suspend fun isBackgroundLocationGranted(): Boolean = getPermissionState().isBackgroundLocationGranted

    override fun isLocalOnline(): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override suspend fun isXiaomiSpecialPermissionGranted(): XiaomiPermissionStatus = getPermissionState().xiaomiStatus

    override suspend fun getPermissionState(forceRefresh: Boolean): PermissionState {
        val now = SystemClock.elapsedRealtime()
        val current = cachedState.get()
        
        val isStale = now - lastFullRefreshTime > PERMISSION_TTL_MS
        if ((isStale || forceRefresh) && isRefreshing.compareAndSet(false, true)) {
            scope.launch {
                try {
                    val isXiaomi = isXiaomiDevice()
                    val batteryWhitelisted = if (current.isBatteryWhitelisted && !forceRefresh) true 
                                           else powerManager.isIgnoringBatteryOptimizations(cachedPackageName)
                    
                    val overlayGranted = if (current.isOverlayGranted && !forceRefresh) true 
                                         else Settings.canDrawOverlays(context)

                    val newState = PermissionState(
                        isBatteryWhitelisted = batteryWhitelisted,
                        isAutoStartGranted = if (isXiaomi) isXiaomiAutostartGranted(context, cachedPackageName) else batteryWhitelisted,
                        isOverlayGranted = overlayGranted,
                        isMicrophoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                        isExactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true,
                        isPostNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true,
                        isBackgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED else true,
                        xiaomiStatus = if (isXiaomi) com.gps19.app.isXiaomiSpecialPermissionGranted(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN,
                        xiaomiAutostartStatus = if (isXiaomi) com.gps19.app.getXiaomiAutostartStatus(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN
                    )
                    cachedState.set(newState)
                    lastFullRefreshTime = now
                } finally {
                    isRefreshing.set(false)
                }
            }
        }
        return cachedState.get()
    }

    override fun observeInternetStatus(): Flow<Boolean> = callbackFlow<Boolean> {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, callback)
        trySend(isLocalOnline())
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    override fun observeBatteryStatus(): Flow<BatteryStatus> = callbackFlow<BatteryStatus> {
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
