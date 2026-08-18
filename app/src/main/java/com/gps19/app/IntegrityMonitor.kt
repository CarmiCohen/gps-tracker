package com.gps19.app

import android.app.usage.UsageStatsManager
import android.content.Context
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
 * Aug.17.08:
 * - Issue #191 Validation: Implemented simulateCoolingMode() to support dynamic 
 *   polling throttle verification (R191).
 * Aug.11.08:
 * - Issue #143: Forensic Integrity Verification. Mapped Thermal Throttling to 
 *   Silent Failure correlation engine (R133). Corrected NetworkCapabilities import.
 * Aug.11.00:
 * - Issue #137: UI Davey/ANR Remediation.
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

    val currentHealth: SystemHealthState get() = _health.value

    init {
        scope.launch {
            systemStatusProvider.observeInternetStatus()
                .onEach { online -> 
                    lastInternetUpdateRt = timeProvider.elapsedRealtime()
                    updateHealth { it.isHardwareOnline = online } 
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

        scope.launch {
            gpsManager.revivalEvents
                .onEach { event -> handleRevivalEvent(event) }
                .collect()
        }

        startHeartbeat()
    }

    private fun handleRevivalEvent(event: GpsManager.RevivalEvent) {
        when (event) {
            is GpsManager.RevivalEvent.Attempt -> {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("GPS REVIVAL: Hardware restart attempt ${event.count} on this device.", false))
            }
            is GpsManager.RevivalEvent.HardwareLock -> {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("CRITICAL: GPS_HARDWARE_LOCK - All revival attempts failed on this device. Manual intervention required.", true))
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_GPS_HARDWARE_LOCK))
                updateHealth { it.gpsHardwareLock = true }
            }
            is GpsManager.RevivalEvent.Success -> {
                if (currentHealth.gpsHardwareLock) {
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent("GPS REVIVAL: Hardware fix restored on this device.", false))
                    _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_GPS_HARDWARE_LOCK))
                    updateHealth { it.gpsHardwareLock = false }
                }
            }
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            delay(BOOTSTRAP_PHASE_MS)
            while (isActive) {
                performIntegrityHeartbeat()
                delay(FORENSIC_PULSE_INTERVAL_MS)
            }
        }
    }

    private suspend fun performIntegrityHeartbeat() {
        val nowRt = timeProvider.elapsedRealtime()
        val storageStalled = lastStorageUpdateRt > 0 && (nowRt - lastStorageUpdateRt) > INTEGRITY_HEARTBEAT_INTERVAL_MS * 3
        val powerStalled = lastPowerUpdateRt > 0 && (nowRt - lastPowerUpdateRt) > INTEGRITY_HEARTBEAT_INTERVAL_MS * 3
        val locationStalled = lastLocationStatusUpdateRt > 0 && (nowRt - lastLocationStatusUpdateRt) > 30000L

        if (storageStalled || powerStalled || locationStalled) {
            val stalls = mutableListOf<String>()
            if (storageStalled) stalls.add("Storage")
            if (powerStalled) stalls.add("Power")
            if (locationStalled) stalls.add("Location")
            
            val msg = "INTEGRITY WARNING: Reactive flow stall detected (${stalls.joinToString(", ")}). Monitoring vitality compromised on this device."
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, true))
        }

        val cpu = systemStatusProvider.getCpuLoad()
        val iow = systemStatusProvider.getIoWait()
        val maxIo = LatencyMonitor.consumeMaxIoLatency()

        if (maxIo > LATENCY_THRESHOLD_DB_WRITE_MS && systemStatusProvider.isA15Hardware()) {
            val msg = "PERFORMANCE WARNING: Critical I/O Spike detected on budget hardware (%dms). System stress: [CPU: %.1f, IOW: %.1f]".format(maxIo, cpu, iow)
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, true))
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_PERFORMANCE_SPIKE))
        }

        updateHealth { h ->
            h.lastIntegrityHeartbeatRt = nowRt
            h.cpuLoad = cpu
            h.ioWait = iow
            h.maxIoLatency = maxIo
            
            val isSilent = SentinelValidator.isSilentFailure(
                gpsStalled = h.gpsStalled,
                isTamperDetected = h.isTamperDetected,
                cpuLoad = cpu,
                ioWait = iow,
                maxIoLatency = maxIo,
                isThermalThrottling = h.isThermalThrottling
            )
            
            if (isSilent && !h.isSilentFailure) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("FORENSIC ALERT: Silent Failure detected on this device. Location stall correlated with high resource load.", true))
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_SILENT_FAILURE))
            } else if (!isSilent && h.isSilentFailure) {
                _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_SILENT_FAILURE))
            }
            h.isSilentFailure = isSilent
        }
    }

    private fun updateHealth(mutator: (SystemHealthState) -> Unit) {
        _health.update { current ->
            mutator(current)
            repository.updateHealth(current)
            current
        }
    }

    private fun handleLocationStatusUpdate(status: GpsManager.LocationStatus) {
        val workingHealth = currentHealth
        if (status.isPending && !workingHealth.isLocationPending) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Location fix pending: ${status.reason.name.replace("_", " ")} on this device", false))
        } 
        else if (!status.isPending && workingHealth.isLocationPending && status.recoveryConfirmed) {
            val durationSec = status.lastPendingDurationMs / 1000.0
            val reasonStr = workingHealth.locationPendingReason.name.replace("_", " ")
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Location fix restored after ${"%.1f".format(durationSec)}s gap ($reasonStr resolved) on this device", false))
        }

        val isStalled = status.reason == LocationPendingReason.GPS_STALL
        if (isStalled && !workingHealth.gpsStalled) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("GPS STALL: Hardware fix on this device has not updated despite satellite visibility.", true))
        }

        updateHealth { h ->
            h.isLocationPending = status.isPending
            h.locationPendingReason = status.reason
            h.lastValidFixRt = status.lastFixRt
            h.lastLocationPendingDurationMs = status.lastPendingDurationMs
            h.gpsStalled = isStalled
        }
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
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Thermal limit reached (${batteryTemp}°C). Entering forced COOLING MODE on this device.", true))
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_TRACKER_TEMP))
        } else if (isCooling && batteryTemp < MAX_SAFE_TEMPERATURE_RECOVERY) {
            isCooling = false
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Thermal limit recovered (${batteryTemp}°C) on this device.", false))
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

        updateHealth { h ->
            h.batteryLevel = status.level
            h.batteryTemp = batteryTemp
            h.maxTemp = maxTemp
            h.isCharging = isCharging
            h.isCoolingModeActive = isCooling
            h.isThermalThrottling = isCooling // R133: Link thermal state to throttling flag
            h.isBatterySteepDischarge = isSteepDischarge
            h.currentMa = status.currentMa
            h.isBatteryLow = status.isLow
            h.isBatteryCritical = status.isCritical
        }
    }

    private fun handleStorageUpdate(status: StorageStatus) {
        val workingHealth = currentHealth
        val megabytesAvailable = status.availableMb
        val critical = status.isCritical
        val low = status.isLow
        
        if (critical != workingHealth.isStorageCritical) {
            if (critical) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Internal storage is CRITICAL (${megabytesAvailable}MB) on this device.", true))
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL))
            }
        }

        if (low != workingHealth.isStorageLow) {
            if (low && !critical) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM WARNING: Internal storage is low (${megabytesAvailable}MB) on this device.", true))
                _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW))
            } else if (!low) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Storage space restored (${megabytesAvailable}MB) on this device.", false))
            }
        }

        updateHealth { h ->
            h.isStorageLow = low
            h.isStorageCritical = critical
            h.storageAvailableMb = status.availableMb
            h.storageTotalMb = status.totalMb
        }
    }

    private fun handlePowerUpdate(status: PowerStatus) {
        val workingHealth = currentHealth
        val powerSave = status.isPowerSaveMode
        val bucket = status.standbyBucket

        if (powerSave != workingHealth.isPowerSaveMode) {
            if (powerSave) {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("SYSTEM WARNING: Power Save Mode active on this device. Sensors and GPS may be throttled.", true))
            } else {
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("System Info: Power Save Mode deactivated on this device.", false))
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
                    val msg = "SYSTEM PRIORITY: Standby bucket on this device changed to $bucketName. ${if (isCritical) "Background tracking may be severely limited." else ""}"
                    _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, isCritical))
                }
            }
        }

        updateHealth { h ->
            h.isPowerSaveMode = powerSave
            h.standbyBucket = bucket
        }
    }

    suspend fun pollSystemStatus(nowWall: Long, nowRt: Long) {
        val delta = nowRt - lastFullPollTs
        if (delta < POLL_TTL_MS && lastFullPollTs != 0L) return
        lastFullPollTs = nowRt

        val workingHealth = currentHealth
        
        val newNet = systemStatusProvider.getNetworkInterface()
        if (newNet != workingHealth.netInterface) {
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Network switched to $newNet on this device", false))
        }

        var isPowerTamper = workingHealth.isPowerTamper
        if (lastPowerDisconnectTs > 0 && !isPowerTamper) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamper = true
                _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Device power tamper confirmed (debounce met) on this device", true))
            }
        }

        updateHealth { h ->
            h.netInterface = newNet
            h.isPowerTamper = isPowerTamper
        }
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
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected on this device ($drop% in ${elapsedMin}m).", true))
            _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE))
        }
        return isSteep
    }

    fun setMaxTemperature(temp: Double) {
        updateHealth { it.maxTemp = temp }
    }

    /**
     * simulateCoolingMode: External simulation trigger for heat mitigation (Issue #191).
     */
    fun simulateCoolingMode(active: Boolean) {
        val msg = if (active) "SYSTEM EMERGENCY: Simulated Thermal limit reached. Entering forced COOLING MODE." 
                  else "System Info: Simulated Thermal limit recovered."
        _integrityEvents.tryEmit(IntegrityEvent.LogEvent(msg, active))
        if (active) _integrityEvents.tryEmit(IntegrityEvent.ViolationSustained(ALERT_ID_TRACKER_TEMP))
        else _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_TEMP))

        updateHealth { h ->
            h.isCoolingModeActive = active
            h.isThermalThrottling = active
        }
    }

    suspend fun isInternetHardwarePresent(): Boolean {
        return systemStatusProvider.isLocalOnline()
    }

    suspend fun checkInternetIntegrity(now: Long): Boolean {
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
                updateHealth { it.localInternetLoss = true }
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
            updateHealth { it.localInternetLoss = false }
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
        updateHealth { it.signalLoss = loss }
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
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Device power unplugged, starting debounce... on this device", false))
        }
    }

    fun onPowerConnected() {
        lastPowerDisconnectTs = 0L
        if (currentHealth.isPowerTamper) {
            updateHealth { it.isPowerTamper = false }
            _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
            _integrityEvents.tryEmit(IntegrityEvent.LogEvent("Device power restored on this device", false))
        }
    }

    fun clearPowerTamper() {
        updateHealth { it.isPowerTamper = false }
        _integrityEvents.tryEmit(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER))
        lastPowerDisconnectTs = 0L
    }

    fun resetStats() {
        sustainedViolations.clear()
        _health.update { h ->
            h.copyFrom(SystemHealthState())
            repository.updateHealth(h)
            h
        }
        lastPowerDisconnectTs = 0L
        batterySamples.clear()
        lastFullPollTs = 0L
        lastInternetCheckRt = 0L
    }

    fun getBatteryLevel(): Int = currentHealth.batteryLevel
    fun getBatteryCurrent(): Int = currentHealth.currentMa
}
