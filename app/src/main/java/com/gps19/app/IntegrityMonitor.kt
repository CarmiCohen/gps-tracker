package com.gps19.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import com.gps19.core.engine.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * IntegrityMonitor: Tracks hardware and network health.
 * v8.9.42:
 * - Issue #352: Thermal Throttling Logic.
 * - Issue #353: Battery Health Profiling.
 * - Issue #337: Forensic Power Parity. Exposed isCharging and maxTemperature.
 * - Issue #311: Monotonic Timing Integrity. Migrated to TimeProvider for all timing logic.
 * - Issue #163: Hardened power tamper detection and connected violation callbacks.
 */
class IntegrityMonitor(
    private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val onViolationSustained: (String) -> Unit,
    private val onLogEvent: (String, Boolean) -> Unit
) {
    var maxTemperature = 0f
        private set

    private val sustainedViolations = mutableMapOf<String, Long>()
    
    var batteryTemp = 0f
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

    // Issue #353: Battery Health Profiling. Now uses monotonic time (Issue #311).
    private val batterySamples = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private var lastBatteryCheckTs = 0L
    var isBatterySteepDischarge = false
        private set

    // Issue #352: Thermal Throttling
    var isCoolingModeActive = false
        private set

    fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    fun getBatteryCurrent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
    }

    /**
     * pollSystemStatus: Updated to take both wall and monotonic time (Issue #311).
     */
    fun pollSystemStatus(nowWall: Long, nowRealtime: Long) {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        
        if (intent != null) {
            batteryTemp = (intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)) / 10f
            if (batteryTemp > maxTemperature) {
                maxTemperature = batteryTemp
                repository.saveFloatSync(MainRepository.MAX_TEMP_KEY, maxTemperature)
            }

            // Issue #352: Thermal Throttling Logic
            if (!isCoolingModeActive && batteryTemp >= MAX_SAFE_TEMPERATURE_CELSIUS) {
                isCoolingModeActive = true
                onLogEvent("SYSTEM EMERGENCY: Thermal limit reached (${batteryTemp}°C). Entering forced COOLING MODE. Sensors and GPS throttled.", true)
                onViolationSustained(ALERT_ID_TRACKER_TEMP)
            } else if (isCoolingModeActive && batteryTemp < MAX_SAFE_TEMPERATURE_RECOVERY) {
                isCoolingModeActive = false
                onLogEvent("System Info: Thermal limit recovered (${batteryTemp}°C). Normal tracking resumed.", false)
            }

            val batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val plugged = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
            isCharging = plugged > 0

            // v8.8.35: Auto-recovery of power state in polling loop
            if (isCharging) onPowerConnected() else onPowerDisconnected()

            if (batteryLevel != -1 && !isCharging) {
                if (nowRealtime - lastBatteryCheckTs > 60000L) { // Check once per minute using monotonic time
                    batterySamples.add(nowRealtime to batteryLevel)
                    lastBatteryCheckTs = nowRealtime
                    checkBatteryDischarge(nowRealtime)
                }
            } else if (isCharging) {
                batterySamples.clear()
                isBatterySteepDischarge = false
            }
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val currentPowerSave = pm.isPowerSaveMode
        if (currentPowerSave != isPowerSaveModeActive) {
            isPowerSaveModeActive = currentPowerSave
            if (isPowerSaveModeActive) {
                onLogEvent("SYSTEM WARNING: Power Save Mode active. Sensors and GPS may be throttled by OS.", true)
            } else {
                onLogEvent("System Info: Power Save Mode deactivated. Normal tracking resumed.", false)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val bucket = usm.appStandbyBucket
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
                    onLogEvent("SYSTEM PRIORITY: Standby bucket changed to $bucketName. ${if(isCritical) "Background tracking may be severely limited." else ""}", isCritical)
                }
                currentStandbyBucket = bucket
            }
        }

        val newNet = getActiveNetworkInterface()
        if (newNet != currentNetInterface) {
            onLogEvent("Network switched to $newNet", false)
            currentNetInterface = newNet
        }

        checkStorageIntegrity()

        if (lastPowerDisconnectTs > 0 && !isPowerTamperDetected) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamperDetected = true
                onLogEvent("Tracker power tamper confirmed (debounce met)", true)
            }
        }
    }

    private fun checkBatteryDischarge(nowRealtime: Long) {
        // Prune old samples using monotonic time
        while (batterySamples.isNotEmpty() && (nowRealtime - batterySamples.peek()!!.first) > BATTERY_STEEP_DISCHARGE_WINDOW_MS) {
            batterySamples.poll()
        }

        if (batterySamples.size < 2) return

        val earliest = batterySamples.peek()!!
        val latest = batterySamples.last()
        
        val drop = earliest.second - latest.second
        val wasSteep = isBatterySteepDischarge
        isBatterySteepDischarge = drop >= BATTERY_STEEP_DISCHARGE_THRESHOLD
        
        if (isBatterySteepDischarge && !wasSteep) {
            onLogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected ($drop% in ${(nowRealtime - earliest.first) / 60000}m). System shutdown likely imminent.", true)
            onViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE)
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
                    onLogEvent("SYSTEM EMERGENCY: Internal storage is CRITICAL (${megabytesAvailable}MB). ALL non-essential logging HALTED to prevent corruption.", true)
                    onViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL)
                }
            }

            if (currentLow != isStorageLow) {
                isStorageLow = currentLow
                if (isStorageLow && !isStorageCritical) {
                    onLogEvent("SYSTEM WARNING: Internal storage is low (${megabytesAvailable}MB). Throttling logs.", true)
                    onViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW)
                } else if (!isStorageLow) {
                    isStorageCritical = false
                    onLogEvent("System Info: Storage space restored (${megabytesAvailable}MB).", false)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage integrity")
        }
    }

    fun setMaxTemperature(temp: Float) { maxTemperature = temp }

    fun getActiveNetworkInterface(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return "OFFLINE"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    fun isInternetHardwarePresent(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isHardwareOnline(): Boolean = isInternetHardwarePresent()

    /**
     * checkInternetIntegrity: Returns true if internet is available.
     * v8.8.2: Now uses monotonic time ('now' must be from TimeProvider.elapsedRealtime()).
     */
    fun checkInternetIntegrity(now: Long): Boolean {
        val online = isInternetHardwarePresent()
        if (!online) {
            val firstDetected = sustainedViolations.getOrPut(ALERT_ID_LOCAL_INTERNET) { now }
            if (now - firstDetected > INTERNET_LOSS_THRESHOLD_MS) {
                onViolationSustained(ALERT_ID_LOCAL_INTERNET)
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
        }
        return true
    }

    fun checkSignalIntegrity(now: Long, silenceDelta: Long, isTracker: Boolean): Boolean {
        val threshold = if (isTracker) {
            VIEWER_SIGNAL_LOSS_THRESHOLD_MS
        } else {
            TRACKER_SIGNAL_LOSS_THRESHOLD_MS
        }
        return silenceDelta > threshold
    }

    /**
     * checkViolationSustained: Returns true if the violation has persisted beyond the threshold.
     * v8.8.21: Now uses TimeProvider for high-assurance duration checks. (Issue #311)
     */
    fun checkViolationSustained(type: String, startTs: Long, threshold: Long): Boolean {
        if (startTs > 0 && (timeProvider.elapsedRealtime() - startTs) > threshold) {
            onViolationSustained(type)
            return true
        }
        return false
    }

    fun onPowerDisconnected() {
        if (!isPowerTamperDetected && lastPowerDisconnectTs == 0L) {
            lastPowerDisconnectTs = timeProvider.elapsedRealtime()
            onLogEvent("Tracker power unplugged, starting debounce...", false)
        }
    }

    fun onPowerConnected() {
        lastPowerDisconnectTs = 0L
        if (isPowerTamperDetected) {
            isPowerTamperDetected = false
            onLogEvent("Tracker power restored", false)
        }
    }

    fun clearPowerTamper() {
        isPowerTamperDetected = false
        lastPowerDisconnectTs = 0L
    }

    fun resetStats() {
        sustainedViolations.clear()
        maxTemperature = 0f
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
    }
}
