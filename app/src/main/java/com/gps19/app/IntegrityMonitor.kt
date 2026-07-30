package com.gps19.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
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
    data class LogEvent(val message: String, val isImportant: Boolean) : IntegrityEvent()
}

/**
 * IntegrityMonitor: Tracks hardware and network health.
 * July.30.42:
 * - Issue #648: Performance: Persistent "Kumiho" Log Spam & UI Jank. Added 
 *   lastInternetCheckRt throttle (5s) to checkInternetIntegrity() to prevent 
 *   redundant calls and align with SystemStatusProvider IPC throttling.
 * July.30.23:
 * - Issue #624: Forensic: System Integrity Periodic Check.
 */
@Singleton
class IntegrityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider,
    private val gpsManager: GpsManager,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var lastFullPollTs = 0L
    private val POLL_TTL_MS = 10_000L

    private val _integrityEvents = MutableSharedFlow<IntegrityEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val integrityEvents: SharedFlow<IntegrityEvent> = _integrityEvents.asSharedFlow()

    private val sustainedViolations = mutableMapOf<String, Long>()
    private val batterySamples = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private var lastBatteryCheckTs = 0L
    private var lastPowerDisconnectTs = 0L

    // Vitality Tracking
    private var lastInternetUpdateRt = 0L
    private var lastBatteryUpdateRt = 0L
    private var lastStorageUpdateRt = 0L
    private var lastPowerUpdateRt = 0L
    private var lastLocationStatusUpdateRt = 0L

    private var lastInternetCheckRt = 0L
    private val INTERNET_CHECK_TTL_MS = 5000L

    private val _health = MutableStateFlow(SystemHealthState())
    val healthFlow: StateFlow<SystemHealthState> = _health.asStateFlow()

    /**
     * The single source of truth for health data - thread safe access via StateFlow.
     */
    val currentHealth: SystemHealthState get() = _health.value

    init {
        // Observe reactive OS status
        scope.launch {
            systemStatusProvider.observeInternetStatus()
                .onEach { online -> 
                    lastInternetUpdateRt = timeProvider.elapsedRealtime()
                    updateHealth { it.copy(isHardwareOnline = online) } 
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observeBatteryStatus()
                .onEach { status -> 
                    lastBatteryUpdateRt = timeProvider.elapsedRealtime()
                    handleBatteryUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observeStorageStatus()
                .onEach { status -> 
                    lastStorageUpdateRt = timeProvider.elapsedRealtime()
                    handleStorageUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observePowerStatus()
                .onEach { status -> 
                    lastPowerUpdateRt = timeProvider.elapsedRealtime()
                    handlePowerUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            gpsManager.locationStatusFlow
                .onEach { status -> 
                    lastLocationStatusUpdateRt = timeProvider.elapsedRealtime()
                    handleLocationStatusUpdate(status) 
                }
                .collect()
        }

        startHeartbeat()
    }

    private fun startHeartbeat() {
        scope.launch {
            // Initial grace period to allow flows to stabilize
            delay(BOOTSTRAP_PHASE_MS)
            while (isActive) {
                performIntegrityHeartbeat()
                delay(INTEGRITY_HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun performIntegrityHeartbeat() {
        val nowRt = timeProvider.elapsedRealtime()
        
        // Audit flow vitality: Storage and Power are polled at 60s, Location at 2s.
        // We warn if a flow has been silent for 3x its expected interval.
        val storageStalled = lastStorageUpdateRt > 0 && (nowRt - lastStorageUpdateRt) > INTEGRITY_HEARTBEAT_INTERVAL_MS * 3
        val powerStalled = lastPowerUpdateRt > 0 && (nowRt - lastPowerUpdateRt) > INTEGRITY_HEARTBEAT_INTERVAL_MS * 3
        val locationStalled = lastLocationStatusUpdateRt > 0 && (nowRt - lastLocationStatusUpdateRt) > 30000L

        if (storageStalled || powerStalled || locationStalled) {
            val stalls = mutableListOf<String>()
            if (storageStalled) stalls.add("Storage")
            if (powerStalled) stalls.add("Power")
            if (locationStalled) stalls.add("Location")
            
            val msg = "INTEGRITY WARNING: Reactive flow stall detected (${stalls.joinToString(", ")}). Monitoring vitality compromised."
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, true))
        }

        updateHealth { it.copy(lastIntegrityHeartbeatRt = nowRt) }
    }

    private fun updateHealth(transform: (SystemHealthState) -> SystemHealthState) {
        _health.update { current ->
            val next = transform(current)
            if (next != current) {
                repository.updateHealth(next)
            }
            next
        }
    }

    private fun handleLocationStatusUpdate(status: GpsManager.LocationStatus) {
        val workingHealth = currentHealth
        
        // Transition: OK -> Pending
        if (status.isPending && !workingHealth.isLocationPending) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Location fix pending: ${status.reason.name.replace("_", " ")}", false))
        } 
        // Transition: Pending -> OK (Recovery)
        else if (!status.isPending && workingHealth.isLocationPending && status.recoveryConfirmed) {
            val durationSec = status.lastPendingDurationMs / 1000.0
            val reasonStr = workingHealth.locationPendingReason.name.replace("_", " ")
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Location fix restored after ${"%.1f".format(durationSec)}s gap ($reasonStr resolved)", false))
        }

        val isStalled = status.reason == LocationPendingReason.GPS_STALL
        if (isStalled && !workingHealth.gpsStalled) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("GPS STALL: Hardware fix has not updated despite satellite visibility.", true))
        }

        updateHealth { it.copy(
            isLocationPending = status.isPending,
            locationPendingReason = status.reason,
            lastValidFixRt = status.lastFixRt,
            lastLocationPendingDurationMs = status.lastPendingDurationMs,
            gpsStalled = isStalled
        ) }
    }

    private fun handleBatteryUpdate(status: BatteryStatus) {
        val nowRt = timeProvider.elapsedRealtime()
        val batteryTemp = status.temp
        val isCharging = status.isCharging
        
        var workingHealth = currentHealth
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

        if (isCharging) onPowerConnected() else onPowerDisconnected()

        var isSteepDischarge = workingHealth.isBatterySteepDischarge
        if (status.level != -1 && !isCharging) {
            if (nowRt - lastBatteryCheckTs > 60000L) {
                batterySamples.add(nowRt to status.level)
                lastBatteryCheckTs = nowRt
                isSteepDischarge = checkBatteryDischarge(nowRt)
            }
        } else if (isCharging) {
            batterySamples.clear()
            isSteepDischarge = false
        }

        updateHealth { it.copy(
            batteryLevel = status.level,
            batteryTemp = batteryTemp,
            maxTemp = maxTemp,
            isCharging = isCharging,
            isCoolingModeActive = isCooling,
            isBatterySteepDischarge = isSteepDischarge,
            currentMa = status.currentMa
        ) }
    }

    private fun handleStorageUpdate(status: StorageStatus) {
        val workingHealth = currentHealth
        val megabytesAvailable = status.availableMb
        val critical = status.isCritical
        val low = status.isLow
        
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
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Storage space restored (${megabytesAvailable}MB).", false))
            }
        }

        updateHealth { it.copy(
            isStorageLow = low,
            isStorageCritical = critical
        ) }
    }

    private fun handlePowerUpdate(status: PowerStatus) {
        val workingHealth = currentHealth
        val powerSave = status.isPowerSaveMode
        val bucket = status.standbyBucket

        if (powerSave != workingHealth.isPowerSaveMode) {
            if (powerSave) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM WARNING: Power Save Mode active. Sensors and GPS may be throttled by OS.", true))
            } else {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Power Save Mode deactivated. Normal tracking resumed.", false))
            }
        }

        if (bucket != workingHealth.standbyBucket) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val bucketName = when (bucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
                    UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
                    else -> "UNKNOWN ($bucket)"
                }
                
                if (workingHealth.standbyBucket != -1) {
                    val isCritical = bucket >= UsageStatsManager.STANDBY_BUCKET_RARE
                    val msg = "SYSTEM PRIORITY: Standby bucket changed to $bucketName. ${if (isCritical) "Background tracking may be severely limited." else ""}"
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, isCritical))
                }
            }
        }

        updateHealth { it.copy(
            isPowerSaveMode = powerSave,
            standbyBucket = bucket
        ) }
    }

    fun pollSystemStatus(nowWall: Long, nowRt: Long) {
        val delta = nowRt - lastFullPollTs
        if (delta < POLL_TTL_MS && lastFullPollTs != 0L) return
        lastFullPollTs = nowRt

        val workingHealth = currentHealth
        
        val newNet = getActiveNetworkInterface()
        if (newNet != workingHealth.netInterface) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Network switched to $newNet", false))
        }

        var isPowerTamper = workingHealth.isPowerTamper
        if (lastPowerDisconnectTs > 0 && !isPowerTamper) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamper = true
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Tracker power tamper confirmed (debounce met)", true))
            }
        }

        updateHealth { it.copy(
            netInterface = newNet,
            isPowerTamper = isPowerTamper
        ) }
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
            val elapsedMin = (nowRt - earliest.first) / 60000
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected ($drop% in ${elapsedMin}m). System shutdown likely imminent.", true))
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE))
        }
        return isSteep
    }

    fun setMaxTemperature(temp: Double) {
        updateHealth { it.copy(maxTemp = temp) }
    }

    fun getActiveNetworkInterface(): String {
        val activeNetwork = connectivityManager.activeNetwork ?: return "OFFLINE"
        val caps = try { connectivityManager.getNetworkCapabilities(activeNetwork) } catch(e: Exception) { null } ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    fun isInternetHardwarePresent(): Boolean {
        return systemStatusProvider.isLocalOnline()
    }

    fun checkInternetIntegrity(now: Long): Boolean {
        val nowRt = timeProvider.elapsedRealtime()
        if (nowRt - lastInternetCheckRt < INTERNET_CHECK_TTL_MS && lastInternetCheckRt != 0L) {
            return !currentHealth.localInternetLoss
        }
        lastInternetCheckRt = nowRt

        val online = isInternetHardwarePresent()
        if (!online) {
            val firstDetected = sustainedViolations.getOrPut(ALERT_ID_LOCAL_INTERNET) { now }
            if (now - firstDetected > INTERNET_LOSS_THRESHOLD_MS) {
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_LOCAL_INTERNET))
                updateHealth { it.copy(localInternetLoss = true) }
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
            updateHealth { it.copy(localInternetLoss = false) }
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
        updateHealth { it.copy(signalLoss = loss) }
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
            updateHealth { it.copy(isPowerTamper = false) }
            _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Tracker power restored", false))
        }
    }

    fun clearPowerTamper() {
        updateHealth { it.copy(isPowerTamper = false) }
        _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
        lastPowerDisconnectTs = 0L
    }

    fun resetStats() {
        sustainedViolations.clear()
        _health.value = SystemHealthState()
        lastPowerDisconnectTs = 0L
        batterySamples.clear()
        lastFullPollTs = 0L
        lastInternetCheckRt = 0L
    }

    fun getBatteryLevel(): Int = currentHealth.batteryLevel
    fun getBatteryCurrent(): Int = currentHealth.currentMa
}
