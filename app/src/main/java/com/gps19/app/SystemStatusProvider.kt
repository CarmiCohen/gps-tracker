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
import com.gps19.core.engine.CapabilityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SystemStatusProvider: Centralizes observation of OS-level states and hardware capabilities.
 * July.24.01:
 * - Issue #098: Hardened permission refresh logic. Added Mutex-protected synchronous 
 *   refresh for getPermissionState(forceRefresh = true) to prevent stale UI alerts.
 * July.21.00:
 * - ANR Hardening (#099): Uses AtomicReference and background refresh to prevent cold-start frame skips.
 */
interface SystemStatusProvider {
    suspend fun isBatteryWhitelisted(): Boolean
    suspend fun isAutoStartGranted(): Boolean
    suspend fun isOverlayGranted(): Boolean
    suspend fun isMicrophoneGranted(): Boolean
    suspend fun isExactAlarmGranted(): Boolean
    suspend fun isPostNotificationsGranted(): Boolean
    suspend fun isBackgroundLocationGranted(): Boolean
    suspend fun isActivityRecognitionGranted(): Boolean
    fun isLocalOnline(): Boolean
    fun isA15Hardware(): Boolean
    
    suspend fun getPermissionState(forceRefresh: Boolean = false): PermissionState
    
    fun observeInternetStatus(): Flow<Boolean>
    fun observeBatteryStatus(): Flow<BatteryStatus>
}

@Singleton
class SystemStatusProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemStatusProvider {

    private val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    
    private val cachedPackageName = context.packageName
    
    private var lastFullRefreshTime: Long = 0
    private val cachedState = AtomicReference<PermissionState>(PermissionState())
    private val refreshMutex = Mutex()
    
    private val isXiaomi by lazy { isXiaomiDevice() }
    private val isSamsung by lazy { isSamsungDevice() }
    private val isS21FE by lazy { isS21FEDevice() }
    private val isA15 by lazy { isA15Device() }
    
    private val PERMISSION_TTL_MS = 15_000L

    override suspend fun isBatteryWhitelisted(): Boolean = getPermissionState().isBatteryWhitelisted
    override suspend fun isAutoStartGranted(): Boolean = getPermissionState().isAutoStartGranted
    override suspend fun isOverlayGranted(): Boolean = getPermissionState().isOverlayGranted
    override suspend fun isMicrophoneGranted(): Boolean = getPermissionState().isMicrophoneGranted
    override suspend fun isExactAlarmGranted(): Boolean = getPermissionState().isExactAlarmGranted
    override suspend fun isPostNotificationsGranted(): Boolean = getPermissionState().isPostNotificationsGranted
    override suspend fun isBackgroundLocationGranted(): Boolean = getPermissionState().isBackgroundLocationGranted
    override suspend fun isActivityRecognitionGranted(): Boolean = getPermissionState().isActivityRecognitionGranted
    override fun isA15Hardware(): Boolean = isA15

    override fun isLocalOnline(): Boolean {
        val caps = try {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } catch (e: Exception) { null }
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override suspend fun getPermissionState(forceRefresh: Boolean): PermissionState {
        val now = SystemClock.elapsedRealtime()
        val isStale = now - lastFullRefreshTime > PERMISSION_TTL_MS
        
        if (isStale || forceRefresh) {
            refreshMutex.withLock {
                // Double-check staleness inside lock
                if (SystemClock.elapsedRealtime() - lastFullRefreshTime > PERMISSION_TTL_MS || forceRefresh) {
                    try {
                        val current = cachedState.get()
                        val batteryWhitelisted = powerManager.isIgnoringBatteryOptimizations(cachedPackageName)
                        val xiaomiStatus = if (isXiaomi) com.gps19.app.isXiaomiSpecialPermissionGranted(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN
                        val xiaomiAutostart = if (isXiaomi) com.gps19.app.getXiaomiAutostartStatus(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN

                        val newState = PermissionState(
                            isBatteryWhitelisted = batteryWhitelisted,
                            isAutoStartGranted = if (isXiaomi) isXiaomiAutostartGranted(context, cachedPackageName) else batteryWhitelisted,
                            isOverlayGranted = Settings.canDrawOverlays(context),
                            isMicrophoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                            isExactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true,
                            isPostNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true,
                            isBackgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED else true,
                            isActivityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED else true,
                            
                            hasBackgroundRestriction = isXiaomi,
                            backgroundStatus = toCapabilityStatus(xiaomiStatus),
                            autostartStatus = toCapabilityStatus(xiaomiAutostart),
                            isManualOverride = current.isManualOverride,
                            requiresWakeLockRenewal = isSamsung,
                            requiresExtraTopPadding = isXiaomi,
                            requiresAdaptationMuzzle = isS21FE,
                            isA15Device = isA15
                        )
                        cachedState.set(newState)
                        lastFullRefreshTime = SystemClock.elapsedRealtime()
                    } catch (e: Exception) {
                        Timber.e(e, "Permission check failed during refresh")
                    }
                }
            }
        }
        return cachedState.get()
    }

    private fun toCapabilityStatus(status: XiaomiPermissionStatus): CapabilityStatus {
        return when (status) {
            XiaomiPermissionStatus.GRANTED -> CapabilityStatus.GRANTED
            XiaomiPermissionStatus.DENIED -> CapabilityStatus.DENIED
            XiaomiPermissionStatus.UNKNOWN -> CapabilityStatus.UNKNOWN
        }
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
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            trySend(isLocalOnline())
        } catch (e: Exception) {
            Timber.e(e, "Connectivity callback registration failed")
            trySend(false)
        }
        awaitClose { try { connectivityManager.unregisterNetworkCallback(callback) } catch(e: Exception) {} }
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
                val currentMa = try {
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
                } catch (e: Exception) { 0 }
                trySend(BatteryStatus(pct, temp, isCharging, currentMa))
            }
        }
        try {
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            Timber.e(e, "Battery receiver registration failed")
        }
        awaitClose { try { context.unregisterReceiver(receiver) } catch(e: Exception) {} }
    }.distinctUntilChanged().conflate()
}

data class BatteryStatus(
    val level: Int,
    val temp: Double,
    val isCharging: Boolean,
    val currentMa: Int = 0
)
