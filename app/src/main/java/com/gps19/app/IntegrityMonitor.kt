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
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntegrityMonitor: Tracks hardware and network health.
 * v9.4.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized monotonic timestamp 
 *   parameter naming to 'nowRt'.
 */
@Singleton
class IntegrityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) {
    interface Listener {
        fun onViolationSustained(type: String)
        fun onLogEvent(message: String, important: Boolean)
    }

    private var listener: Listener? = null

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    } else null

    private var lastFullPollTs = 0L
    private val POLL_TTL_MS = 10_000L

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    var maxTemperature = 0.0
        private set

    private val sustainedViolations = mutableMapOf<String, Long>()
    
    var batteryTemp = 0.0
    private var _batteryLevel = -1
    var isPowerTamperDetected = false
    private var lastPowerDisconnectTs = 0L

    var isPowerSaveModeActive = false
        private set
        
    var currentStandbyBucket: Int = -1
        private set

    var isStorageLow = false
        private set

    var isStorageCritical = false
        private set

    var currentNetInterface: String = "UNKNOWN"
        private set

    var isCharging = false
        private set

    private val batterySamples = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private var lastBatteryCheckTs = 0L
    var isBatterySteepDischarge = false
        private set

    var isCoolingModeActive = false
        private set

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
        
        if (intent != null) {
            batteryTemp = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10.0
            if (batteryTemp > maxTemperature) {
                maxTemperature = batteryTemp
                repository.saveDoubleSync(MainRepository.MAX_TEMP_KEY, maxTemperature)
            }

            if (!isCoolingModeActive && batteryTemp >= MAX_SAFE_TEMPERATURE_CELSIUS) {
                isCoolingModeActive = true
                listener?.onLogEvent("SYSTEM EMERGENCY: Thermal limit reached (${batteryTemp}°C). Entering forced COOLING MODE. Sensors and GPS throttled.", true)
                listener?.onViolationSustained(ALERT_ID_TRACKER_TEMP)
            } else if (isCoolingModeActive && batteryTemp < MAX_SAFE_TEMPERATURE_RECOVERY) {
                isCoolingModeActive = false
                listener?.onLogEvent("System Info: Thermal limit recovered (${batteryTemp}°C). Normal tracking resumed.", false)
            }

            _batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            isCharging = plugged > 0

            if (isCharging) onPowerConnected() else onPowerDisconnected()

            if (_batteryLevel != -1 && !isCharging) {
                if (nowRt - lastBatteryCheckTs > 60000L) {
                    batterySamples.add(nowRt to _batteryLevel)
                    lastBatteryCheckTs = nowRt
                    checkBatteryDischarge(nowRt)
                }
            } else if (isCharging) {
                batterySamples.clear()
                isBatterySteepDischarge = false
            }
        }

        val currentPowerSave = powerManager.isPowerSaveMode
        if (currentPowerSave != isPowerSaveModeActive) {
            isPowerSaveModeActive = currentPowerSave
            if (isPowerSaveModeActive) {
                listener?.onLogEvent("SYSTEM WARNING: Power Save Mode active. Sensors and GPS may be throttled by OS.", true)
            } else {
                listener?.onLogEvent("System Info: Power Save Mode deactivated. Normal tracking resumed.", false)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && usageStatsManager != null) {
            val bucket = usageStatsManager.appStandbyBucket
            if (bucket != currentStandbyBucket) {
                val bucketName = when (bucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
                    UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
                    else -> "UNKNOWN ($bucket)"
                }
                
                if (currentStandbyBucket != -1) {
                    val isCritical = bucket >= UsageStatsManager.STANDBY_BUCKET_RARE
                    listener?.onLogEvent("SYSTEM PRIORITY: Standby bucket changed to $bucketName. ${if(isCritical) "Background tracking may be severely limited." else ""}", isCritical)
                }
                currentStandbyBucket = bucket
            }
        }

        val newNet = getActiveNetworkInterface()
        if (newNet != currentNetInterface) {
            listener?.onLogEvent("Network switched to $newNet", false)
            currentNetInterface = newNet
        }

        checkStorageIntegrity()

        if (lastPowerDisconnectTs > 0 && !isPowerTamperDetected) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamperDetected = true
                listener?.onLogEvent("Tracker power tamper confirmed (debounce met)", true)
            }
        }
    }

    private fun checkBatteryDischarge(nowRt: Long) {
        while (batterySamples.isNotEmpty() && (nowRt - batterySamples.peek()!!.first) > BATTERY_STEEP_DISCHARGE_WINDOW_MS) {
            batterySamples.poll()
        }

        if (batterySamples.size < 2) return

        val earliest = batterySamples.peek()!!
        val latest = batterySamples.last()
        
        val drop = earliest.second - latest.second
        val wasSteep = isBatterySteepDischarge
        isBatterySteepDischarge = drop >= BATTERY_STEEP_DISCHARGE_THRESHOLD
        
        if (isBatterySteepDischarge && !wasSteep) {
            listener?.onLogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected ($drop% in ${(nowRt - earliest.first) / 60000}m). System shutdown likely imminent.", true)
            listener?.onViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE)
        }
    }

    private fun checkStorageIntegrity() {
        try {
            val stat = StatFs(context.filesDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val megabytesAvailable = bytesAvailable / (1024 * 1024)
            
            val currentCritical = megabytesAvailable < SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB
            val currentLow = megabytesAvailable < SYSTEM_STORAGE_LOW_THRESHOLD_MB
            
            if (currentCritical != isStorageCritical) {
                isStorageCritical = currentCritical
                if (isStorageCritical) {
                    listener?.onLogEvent("SYSTEM EMERGENCY: Internal storage is CRITICAL (${megabytesAvailable}MB). ALL non-essential logging HALTED to prevent corruption.", true)
                    listener?.onViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL)
                }
            }

            if (currentLow != isStorageLow) {
                isStorageLow = currentLow
                if (isStorageLow && !isStorageCritical) {
                    listener?.onLogEvent("SYSTEM WARNING: Internal storage is low (${megabytesAvailable}MB). Throttling logs.", true)
                    listener?.onViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW)
                } else if (!isStorageLow) {
                    isStorageCritical = false
                    listener?.onLogEvent("System Info: Storage space restored (${megabytesAvailable}MB).", false)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage integrity")
        }
    }

    fun setMaxTemperature(temp: Double) { maxTemperature = temp }

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

    fun isHardwareOnline(): Boolean = isInternetHardwarePresent()

    fun checkInternetIntegrity(now: Long): Boolean {
        val online = isInternetHardwarePresent()
        if (!online) {
            val firstDetected = sustainedViolations.getOrPut(ALERT_ID_LOCAL_INTERNET) { now }
            if (now - firstDetected > INTERNET_LOSS_THRESHOLD_MS) {
                listener?.onViolationSustained(ALERT_ID_LOCAL_INTERNET)
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
        }
        return true
    }

    fun checkSignalIntegrity(nowRt: Long, silenceDelta: Long, isTracker: Boolean): Boolean {
        val threshold = if (isTracker) {
            VIEWER_SIGNAL_LOSS_THRESHOLD_MS
        } else {
            TRACKER_SIGNAL_LOSS_THRESHOLD_MS
        }
        return silenceDelta > threshold
    }

    fun checkViolationSustained(type: String, startTs: Long, threshold: Long): Boolean {
        if (startTs > 0 && (timeProvider.elapsedRealtime() - startTs) > threshold) {
            listener?.onViolationSustained(type)
            return true
        }
        return false
    }

    fun onPowerDisconnected() {
        if (!isPowerTamperDetected && lastPowerDisconnectTs == 0L) {
            lastPowerDisconnectTs = timeProvider.elapsedRealtime()
            listener?.onLogEvent("Tracker power unplugged, starting debounce...", false)
        }
    }

    fun onPowerConnected() {
        lastPowerDisconnectTs = 0L
        if (isPowerTamperDetected) {
            isPowerTamperDetected = false
            listener?.onLogEvent("Tracker power restored", false)
        }
    }

    fun clearPowerTamper() {
        isPowerTamperDetected = false
        lastPowerDisconnectTs = 0L
    }

    fun resetStats() {
        sustainedViolations.clear()
        maxTemperature = 0.0
        isPowerTamperDetected = false
        lastPowerDisconnectTs = 0L
        isPowerSaveModeActive = false
        currentStandbyBucket = -1
        isStorageLow = false
        isStorageCritical = false
        currentNetInterface = "UNKNOWN"
        batterySamples.clear()
        isBatterySteepDischarge = false
        isCoolingModeActive = false
        lastFullPollTs = 0L
        _batteryLevel = -1
    }
}
