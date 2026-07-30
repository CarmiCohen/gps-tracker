package com.gps19.app

import android.Manifest
import android.app.AlarmManager
import android.app.usage.UsageStatsManager
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
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.gps19.core.engine.CapabilityStatus
import com.gps19.core.engine.SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB
import com.gps19.core.engine.SYSTEM_STORAGE_LOW_THRESHOLD_MB
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SystemStatusProvider: Centralizes observation of OS-level states and hardware capabilities.
 * July.30.45:
 * - Issue #655: Performance Hardening (Fix). Implemented FORCED_REFRESH_COOLDOWN_MS (2s) 
 *   to prevent IPC bursts during reactive setup phases. Eliminated getPackageName spam 
 *   on Samsung A15 by ensuring forceRefresh respects a hardware-level throttle.
 * - Issue #654: Performance Hardening. Centralized ACCESS_FINE_LOCATION and 
 *   NetworkInterface checks into throttled IPC audit to eliminate main-thread stalls.
 */
interface SystemStatusProvider {
    suspend fun isBatteryWhitelisted(): Boolean
    suspend fun isAutoStartGranted(): Boolean
    suspend fun isOverlayGranted(): Boolean
    suspend fun isMicrophoneGranted(): Boolean
    suspend fun isExactAlarmGranted(): Boolean
    suspend fun isPostNotificationsGranted(): Boolean
    suspend fun isBackgroundLocationGranted(): Boolean
    suspend fun isBackgroundLocationState(): Boolean
    suspend fun isActivityRecognitionGranted(): Boolean
    suspend fun isFineLocationGranted(): Boolean
    suspend fun isLocalOnline(): Boolean
    suspend fun getNetworkInterface(): String
    fun isA15Hardware(): Boolean
    
    suspend fun getPermissionState(forceRefresh: Boolean = false): PermissionState
    
    fun observeInternetStatus(): Flow<Boolean>
    fun observeBatteryStatus(): Flow<BatteryStatus>
    fun observeStorageStatus(): Flow<StorageStatus>
    fun observePowerStatus(): Flow<PowerStatus>
    
    fun getStorageStatus(): StorageStatus
    fun getPowerStatus(): PowerStatus
}

@Singleton
class SystemStatusProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope
) : SystemStatusProvider {

    private val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    private val usageStatsManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        } else null
    }
    
    private val cachedPackageName = context.packageName
    
    private var lastFullRefreshTime: Long = 0
    private val cachedState = AtomicReference<PermissionState>(PermissionState())
    private val refreshMutex = Mutex()
    private val internetMutex = Mutex()
    
    private val isXiaomi by lazy { isXiaomiDevice() }
    private val isSamsung by lazy { isSamsungDevice() }
    private val isS21FE by lazy { isS21FEDevice() }
    private val isA15 by lazy { isA15Device() }
    
    private val PERMISSION_TTL_MS = 2000L
    private val FORCED_REFRESH_COOLDOWN_MS = 2000L
    private val STORAGE_POLL_INTERVAL_MS = 60_000L
    private val POWER_POLL_INTERVAL_MS = 60_000L
    
    private var lastInternetCheckRt = 0L
    private var cachedInternetStatus = false
    private var cachedNetworkInterface = "UNKNOWN"
    private val INTERNET_CACHE_TTL_MS = 5000L

    private var lastHardwareCheckRt = 0L
    private val HARDWARE_IPC_THROTTLE_MS = 5000L

    override suspend fun isBatteryWhitelisted(): Boolean = getPermissionState().isBatteryWhitelisted
    override suspend fun isAutoStartGranted(): Boolean = getPermissionState().isAutoStartGranted
    override suspend fun isOverlayGranted(): Boolean = getPermissionState().isOverlayGranted
    override suspend fun isMicrophoneGranted(): Boolean = getPermissionState().isMicrophoneGranted
    override suspend fun isExactAlarmGranted(): Boolean = getPermissionState().isExactAlarmGranted
    override suspend fun isPostNotificationsGranted(): Boolean = getPermissionState().isPostNotificationsGranted
    override suspend fun isBackgroundLocationGranted(): Boolean = getPermissionState().isBackgroundLocationGranted
    override suspend fun isBackgroundLocationState(): Boolean = isBackgroundLocationGranted()
    override suspend fun isActivityRecognitionGranted(): Boolean = getPermissionState().isActivityRecognitionGranted
    override suspend fun isFineLocationGranted(): Boolean = getPermissionState().isFineLocationGranted
    override fun isA15Hardware(): Boolean = isA15

    override suspend fun isLocalOnline(): Boolean = internetMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        if (now - lastInternetCheckRt < INTERNET_CACHE_TTL_MS && lastInternetCheckRt != 0L) {
            return cachedInternetStatus
        }

        val caps = try {
            withContext(Dispatchers.IO) {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            }
        } catch (e: Exception) { null }
        
        val status = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netInterface = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
        
        cachedInternetStatus = status
        cachedNetworkInterface = netInterface
        lastInternetCheckRt = now
        return status
    }

    override suspend fun getNetworkInterface(): String {
        isLocalOnline() // Refresh if needed
        return cachedNetworkInterface
    }

    override suspend fun getPermissionState(forceRefresh: Boolean): PermissionState {
        val now = SystemClock.elapsedRealtime()
        val isStale = now - lastFullRefreshTime > PERMISSION_TTL_MS
        
        // Issue #655: Hardware refresh cooldown. Even for "forced" refreshes, 
        // we enforce a minimum interval of 2 seconds to prevent manufacturer IPC auditing stalls.
        val shouldExecute = when {
            forceRefresh -> (now - lastHardwareCheckRt >= FORCED_REFRESH_COOLDOWN_MS) || lastHardwareCheckRt == 0L
            else -> isStale
        }

        if (shouldExecute) {
            refreshMutex.withLock {
                val currentNow = SystemClock.elapsedRealtime()
                val doubleCheckExecute = when {
                    forceRefresh -> (currentNow - lastHardwareCheckRt >= FORCED_REFRESH_COOLDOWN_MS) || lastHardwareCheckRt == 0L
                    else -> (currentNow - lastFullRefreshTime > PERMISSION_TTL_MS)
                }

                if (doubleCheckExecute) {
                    try {
                        val current = cachedState.get()
                        val useCache = currentNow - lastHardwareCheckRt <= HARDWARE_IPC_THROTTLE_MS && lastHardwareCheckRt != 0L && !forceRefresh
                        
                        val newState = if (useCache) {
                            current
                        } else {
                            withContext(Dispatchers.IO) {
                                val fineLocGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val batteryWhitelisted = powerManager.isIgnoringBatteryOptimizations(cachedPackageName)
                                val overlayGranted = Settings.canDrawOverlays(context)
                                val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
                                val notifyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
                                val bgLocGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED else true
                                val actRecogGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED else true

                                val xiaomiStatus = if (isXiaomi) com.gps19.app.isXiaomiSpecialPermissionGranted(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN
                                val xiaomiAutostart = if (isXiaomi) com.gps19.app.getXiaomiAutostartStatus(context, cachedPackageName) else XiaomiPermissionStatus.UNKNOWN

                                lastHardwareCheckRt = currentNow
                                
                                PermissionState(
                                    isFineLocationGranted = fineLocGranted,
                                    isBatteryWhitelisted = batteryWhitelisted,
                                    isAutoStartGranted = if (isXiaomi) isXiaomiAutostartGranted(context, cachedPackageName) else batteryWhitelisted,
                                    isOverlayGranted = overlayGranted,
                                    isMicrophoneGranted = micGranted,
                                    isExactAlarmGranted = alarmGranted,
                                    isPostNotificationsGranted = notifyGranted,
                                    isBackgroundLocationGranted = bgLocGranted,
                                    isActivityRecognitionGranted = actRecogGranted,
                                    
                                    hasBackgroundRestriction = isXiaomi,
                                    backgroundStatus = toCapabilityStatus(xiaomiStatus),
                                    autostartStatus = toCapabilityStatus(xiaomiAutostart),
                                    isManualOverride = current.isManualOverride,
                                    requiresWakeLockRenewal = isSamsung,
                                    requiresExtraTopPadding = isXiaomi,
                                    requiresAdaptationMuzzle = isS21FE,
                                    isA15Device = isA15
                                )
                            }
                        }

                        cachedState.set(newState)
                        lastFullRefreshTime = currentNow
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

    private val sharedInternetStatusFlow = callbackFlow<Boolean> {
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
            launch {
                trySend(isLocalOnline())
            }
        } catch (e: Exception) {
            Timber.e(e, "Connectivity callback registration failed")
            trySend(false)
        }
        awaitClose { try { connectivityManager.unregisterNetworkCallback(callback) } catch(e: Exception) {} }
    }.distinctUntilChanged()
     .conflate()
     .shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
     )

    override fun observeInternetStatus(): Flow<Boolean> = sharedInternetStatusFlow

    private val sharedBatteryStatusFlow = callbackFlow<BatteryStatus> {
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
    }.distinctUntilChanged()
     .conflate()
     .shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
     )

    override fun observeBatteryStatus(): Flow<BatteryStatus> = sharedBatteryStatusFlow

    private val sharedStorageStatusFlow = flow {
        while (true) {
            emit(getStorageStatus())
            delay(STORAGE_POLL_INTERVAL_MS)
        }
    }.distinctUntilChanged()
     .shareIn(
         scope = externalScope,
         started = SharingStarted.WhileSubscribed(5000),
         replay = 1
     )

    override fun observeStorageStatus(): Flow<StorageStatus> = sharedStorageStatusFlow

    override fun getStorageStatus(): StorageStatus {
        return try {
            val stat = StatFs(context.filesDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val megabytesAvailable = bytesAvailable / (1024 * 1024)
            StorageStatus(
                availableMb = megabytesAvailable,
                isLow = megabytesAvailable < SYSTEM_STORAGE_LOW_THRESHOLD_MB,
                isCritical = megabytesAvailable < SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate storage status")
            StorageStatus(0, isLow = true, isCritical = true)
        }
    }

    private val sharedPowerStatusFlow = callbackFlow<PowerStatus> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(getPowerStatus())
            }
        }
        try {
            context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        } catch (e: Exception) {
            Timber.e(e, "Power save receiver registration failed")
        }

        val pollJob = launch {
            while (isActive) {
                trySend(getPowerStatus())
                delay(POWER_POLL_INTERVAL_MS)
            }
        }

        trySend(getPowerStatus())
        
        awaitClose { 
            try { context.unregisterReceiver(receiver) } catch(e: Exception) {}
            pollJob.cancel()
        }
    }.distinctUntilChanged()
     .conflate()
     .shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
     )

    override fun observePowerStatus(): Flow<PowerStatus> = sharedPowerStatusFlow

    override fun getPowerStatus(): PowerStatus {
        val powerSave = try { powerManager.isPowerSaveMode } catch (e: Exception) { false }
        val standbyBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { usageStatsManager?.appStandbyBucket ?: -1 } catch (e: Exception) { -1 }
        } else -1
        return PowerStatus(powerSave, standbyBucket)
    }
}

data class BatteryStatus(
    val level: Int,
    val temp: Double,
    val isCharging: Boolean,
    val currentMa: Int = 0
)

data class StorageStatus(
    val availableMb: Long,
    val isLow: Boolean,
    val isCritical: Boolean
)

data class PowerStatus(
    val isPowerSaveMode: Boolean,
    val standbyBucket: Int
)
