package com.gps19.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntegrityEvent: Reactive event container for hardware health changes.
 */
sealed class IntegrityEvent {
    data class ViolationSustained(val type: String) : IntegrityEvent()
    data class ViolationResolved(val type: String) : IntegrityEvent()
    data class LogEvent(val message: String, val important: Boolean) : IntegrityEvent()
}

/**
 * IntegrityMonitor: Tracks hardware and network health.
 * July.27.00:
 * - Architecture Audit: Updated to use centralized PreferenceKeys.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization.
 */
@Singleton
class IntegrityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    private val healthLock = Any()

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    } else null

    private var lastFullPollTs = 0L
    private val POLL_TTL_MS = 10_000L

    private val _integrityEvents = MutableSharedFlow<IntegrityEvent>(extraBufferCapacity = 32)
    val integrityEvents: SharedFlow<IntegrityEvent> = _integrityEvents.asSharedFlow()

    private val sustainedViolations = mutableMapOf<String, Long>()
    private val batterySamples = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private var lastBatteryCheckTs = 0L
    private var lastPowerDisconnectTs = 0L
    private var _batteryLevel = -1

    private var _currentHealth = SystemHealthState()
    
    // The single source of truth for health data - thread safe access
    var currentHealth: SystemHealthState
        get() = synchronized(healthLock) { _currentHealth }
        private set(value) = synchronized(healthLock) { _currentHealth = value }

    fun getBatteryLevel(): Int {
        if (_batteryLevel == -1) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            _batteryLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        }
        return _batteryLevel
    }

    fun getBatteryCurrent(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
    }

    fun pollSystemStatus(nowWall: Long, nowRt: Long) {
        val delta = nowRt - lastFullPollTs
        if (delta < POLL_TTL_MS && lastFullPollTs != 0L) return
        lastFullPollTs = nowRt

        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        var workingHealth = currentHealth

        if (intent != null) {
            val batteryTemp = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10.0
            var maxTemp = workingHealth.maxTemp
            if (batteryTemp > maxTemp) {
                maxTemp = batteryTemp
                repository.saveDoubleSync(MAX_TEMP_KEY, maxTemp)
            }

            var isCooling = workingHealth.isCoolingModeActive
            if (!isCooling && batteryTemp >= MAX_SAFE_TEMPERATURE_CELSIUS) {
                isCooling = true
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Thermal limit reached (${batteryTemp}°C). Entering forced COOLING MODE. Sensors and GPS throttled.", true))
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_TRACKER_TEMP))
            } else if (isCooling && batteryTemp < MAX_SAFE_TEMPERATURE_RECOVERY) {
                isCooling = false
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Thermal limit recovered (${batteryTemp}°C). Normal tracking resumed.", false))
            }

            _batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isCharging = plugged > 0

            if (isCharging) onPowerConnected() else onPowerDisconnected()

            var isSteepDischarge = workingHealth.isBatterySteepDischarge
            if (_batteryLevel != -1 && !isCharging) {
                if (nowRt - lastBatteryCheckTs > 60000L) {
                    batterySamples.add(nowRt to _batteryLevel)
                    lastBatteryCheckTs = nowRt
                    isSteepDischarge = checkBatteryDischarge(nowRt)
                }
            } else if (isCharging) {
                batterySamples.clear()
                isSteepDischarge = false
            }

            workingHealth = workingHealth.copy(
                batteryLevel = _batteryLevel,
                batteryTemp = batteryTemp,
                maxTemp = maxTemp,
                isCharging = isCharging,
                isCoolingModeActive = isCooling,
                isBatterySteepDischarge = isSteepDischarge,
                currentMa = getBatteryCurrent()
            )
        }

        val powerSave = powerManager.isPowerSaveMode
        if (powerSave != workingHealth.isPowerSaveMode) {
            if (powerSave) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM WARNING: Power Save Mode active. Sensors and GPS may be throttled by OS.", true))
            } else {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Power Save Mode deactivated. Normal tracking resumed.", false))
            }
        }
        
        var standbyBucket = workingHealth.standbyBucket
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && usageStatsManager != null) {
            val bucket = usageStatsManager.appStandbyBucket
            if (bucket != standbyBucket) {
                val bucketName = when (bucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
                    UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
                    else -> "UNKNOWN ($bucket)"
                }
                
                if (standbyBucket != -1) {
                    val isCritical = bucket >= UsageStatsManager.STANDBY_BUCKET_RARE
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM PRIORITY: Standby bucket changed to $bucketName. ${if(isCritical) "Background tracking may be severely limited." else ""}", isCritical))
                }
                standbyBucket = bucket
            }
        }

        val newNet = getActiveNetworkInterface()
        if (newNet != workingHealth.netInterface) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Network switched to $newNet", false))
        }

        val storage = checkStorageIntegrity()

        var isPowerTamper = workingHealth.isPowerTamper
        if (lastPowerDisconnectTs > 0 && !isPowerTamper) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamper = true
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Tracker power tamper confirmed (debounce met)", true))
            }
        }

        currentHealth = workingHealth.copy(
            isPowerSaveMode = powerSave,
            standbyBucket = standbyBucket,
            netInterface = newNet,
            isStorageLow = storage.first,
            isStorageCritical = storage.second,
            isPowerTamper = isPowerTamper,
            isHardwareOnline = isInternetHardwarePresent()
        )
    }

    private fun checkBatteryDischarge(nowRt: Long): Boolean {
        while (batterySamples.isNotEmpty() && (nowRt - batterySamples.peek()!!.first) > BATTERY_STEEP_DISCHARGE_WINDOW_MS) {
            batterySamples.poll()
        }

        if (batterySamples.size < 2) return currentHealth.isBatterySteepDischarge

        val earliest = batterySamples.peek()!!
        val latest = batterySamples.last()
        
        val drop = earliest.second - latest.second
        val isSteep = drop >= BATTERY_STEEP_DISCHARGE_THRESHOLD
        
        if (isSteep && !currentHealth.isBatterySteepDischarge) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected ($drop% in ${(nowRt - earliest.first) / 60000}m). System shutdown likely imminent.", true))
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE))
        }
        return isSteep
    }

    private fun checkStorageIntegrity(): Pair<Boolean, Boolean> {
        val workingHealth = currentHealth
        var low = workingHealth.isStorageLow
        var critical = workingHealth.isStorageCritical
        try {
            val stat = StatFs(context.filesDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val megabytesAvailable = bytesAvailable / (1024 * 1024)
            
            critical = megabytesAvailable < SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB
            low = megabytesAvailable < SYSTEM_STORAGE_LOW_THRESHOLD_MB
            
            if (critical != workingHealth.isStorageCritical) {
                if (critical) {
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Internal storage is CRITICAL (${megabytesAvailable}MB). ALL non-essential logging HALTED to prevent corruption.", true))
                    _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL))
                }
            }

            if (low != workingHealth.isStorageLow) {
                if (low && !critical) {
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM WARNING: Internal storage is low (${megabytesAvailable}MB). Throttling logs.", true))
                    _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW))
                } else if (!low) {
                    critical = false
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Storage space restored (${megabytesAvailable}MB).", false))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage integrity")
        }
        return low to critical
    }

    fun setMaxTemperature(temp: Double) {
        currentHealth = currentHealth.copy(maxTemp = temp)
    }

    fun getActiveNetworkInterface(): String {
        val activeNetwork = connectivityManager.activeNetwork ?: return "OFFLINE"
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    fun isInternetHardwarePresent(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun checkInternetIntegrity(now: Long): Boolean {
        val online = isInternetHardwarePresent()
        if (!online) {
            val firstDetected = sustainedViolations.getOrPut(ALERT_ID_LOCAL_INTERNET) { now }
            if (now - firstDetected > INTERNET_LOSS_THRESHOLD_MS) {
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_LOCAL_INTERNET))
                currentHealth = currentHealth.copy(localInternetLoss = true)
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
            currentHealth = currentHealth.copy(localInternetLoss = false)
        }
        return true
    }

    fun checkSignalIntegrity(nowRt: Long, silenceDelta: Long, isTracker: Boolean): Boolean {
        val threshold = if (isTracker) {
            VIEWER_SIGNAL_LOSS_THRESHOLD_MS
        } else {
            TRACKER_SIGNAL_LOSS_THRESHOLD_MS
        }
        val loss = silenceDelta > threshold
        currentHealth = currentHealth.copy(signalLoss = loss)
        return !loss
    }

    fun checkViolationSustained(type: String, startTs: Long, threshold: Long): Boolean {
        if (startTs > 0 && (timeProvider.elapsedRealtime() - startTs) > threshold) {
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(type))
            return true
        }
        return false
    }

    fun onPowerDisconnected() {
        if (!currentHealth.isPowerTamper && lastPowerDisconnectTs == 0L) {
            lastPowerDisconnectTs = timeProvider.elapsedRealtime()
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Tracker power unplugged, starting debounce...", false))
        }
    }

    fun onPowerConnected() {
        lastPowerDisconnectTs = 0L
        if (currentHealth.isPowerTamper) {
            currentHealth = currentHealth.copy(isPowerTamper = false)
            _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Tracker power restored", false))
        }
    }

    fun clearPowerTamper() {
        currentHealth = currentHealth.copy(isPowerTamper = false)
        _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
        lastPowerDisconnectTs = 0L
    }

    fun resetStats() {
        sustainedViolations.clear()
        currentHealth = SystemHealthState()
        lastPowerDisconnectTs = 0L
        batterySamples.clear()
        lastFullPollTs = 0L
        _batteryLevel = -1
    }
}
