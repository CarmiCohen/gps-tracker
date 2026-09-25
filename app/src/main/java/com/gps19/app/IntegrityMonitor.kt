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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntegrityMonitor: Tracks hardware and network health.
 * Sep.25.03:
 * - Issue #1322 Cleanup: Fixed unresolved references to revivalEvents and LocationStatus.
 *   Migrated revival event observation to the unified DomainEventBus.
 * Sep.25.01:
 * - Issue #1322: Converged IntegrityEvent emission into DomainEventBus.
 */
@Singleton
class IntegrityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider,
    private val hardwareSuite: HardwareSuite,
    private val domainEventBus: DomainEventBus,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var lastFullPollTs = 0L
    private val POLL_TTL_MS = 10_000L

    private val sustainedViolations = mutableMapOf<String, Long>()
    private val batterySamples = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private var lastBatteryCheckTs = 0L
    private var lastPowerDisconnectTs = 0L

    private val isStorageSimulated = AtomicBoolean(false)
    private val isStorageCriticalSimulated = AtomicBoolean(false)
    private val isMaliAnomalySimulated = AtomicBoolean(false)

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
                .onEach { lastInternetUpdateRt = timeProvider.elapsedRealtime() }
                .distinctUntilChanged()
                .onEach { online -> 
                    updateHealth { it.isHardwareOnline = online } 
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observeBatteryStatus()
                .onEach { lastBatteryUpdateRt = timeProvider.elapsedRealtime() }
                .distinctUntilChanged()
                .onEach { status -> 
                    handleBatteryUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observeStorageStatus()
                .onEach { lastStorageUpdateRt = timeProvider.elapsedRealtime() }
                .distinctUntilChanged()
                .onEach { status -> 
                    if (!isStorageSimulated.get()) {
                        handleStorageUpdate(status)
                    }
                }
                .collect()
        }

        scope.launch {
            systemStatusProvider.observePowerStatus()
                .onEach { lastPowerUpdateRt = timeProvider.elapsedRealtime() }
                .distinctUntilChanged()
                .onEach { status -> 
                    handlePowerUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            hardwareSuite.locationStatusFlow
                .onEach { lastLocationStatusUpdateRt = timeProvider.elapsedRealtime() }
                .distinctUntilChanged()
                .onEach { status -> 
                    handleLocationStatusUpdate(status) 
                }
                .collect()
        }

        scope.launch {
            domainEventBus.events
                .filterIsInstance<DomainEvent.Revival>()
                .onEach { event -> handleRevivalEvent(event.event) }
                .collect()
        }

        // Issue #762: Local transparency for [ULTRA] relaxation state
        scope.launch {
            hardwareSuite.isUltraLongStationaryFlow
                .distinctUntilChanged()
                .onEach { isUltra -> updateHealth { it.isUltraLongStationary = isUltra } }
                .collect()
        }

        // Issue #924: Local transparency for GNSS Throttling (A15 Hysteresis)
        scope.launch {
            hardwareSuite.isGnssThrottledFlow
                .distinctUntilChanged()
                .onEach { throttled -> updateHealth { it.isGnssThrottled = throttled } }
                .collect()
        }

        startHeartbeat()
    }

    private fun handleRevivalEvent(event: RevivalEvent) {
        when (event) {
            is RevivalEvent.Attempt -> {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("GPS REVIVAL: Hardware restart attempt ${event.count} on this device.", false)))
            }
            is RevivalEvent.HardwareLock -> {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("CRITICAL: GPS_HARDWARE_LOCK - All revival attempts failed on this device. Manual intervention required.", true)))
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_GPS_HARDWARE_LOCK)))
                updateHealth { it.gpsHardwareLock = true }
            }
            is RevivalEvent.Success -> {
                if (currentHealth.gpsHardwareLock) {
                    domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("GPS REVIVAL: Hardware fix restored on this device.", false)))
                    domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_GPS_HARDWARE_LOCK)))
                    updateHealth { it.gpsHardwareLock = false }
                }
            }
            is RevivalEvent.RawBurstStarted -> {
                val h = currentHealth
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("AUDIT: Raw GNSS Burst STARTED. [Batt: ${h.batteryLevel}%, Current: ${h.currentMa}mA, Temp: ${h.batteryTemp}°C]", false)))
            }
            is RevivalEvent.RawBurstEnded -> {
                val h = currentHealth
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("AUDIT: Raw GNSS Burst ENDED. [Batt: ${h.batteryLevel}%, Current: ${h.currentMa}mA, Temp: ${h.batteryTemp}°C]", false)))
            }
            is RevivalEvent.Footprint -> {
                val msg = "ENERGY AUDIT: Revival Footprint (R-ID 259) - Delta: ${event.deltaMa}mA, Temp Rise: ${event.deltaTemp}°C, Duration: ${event.durationMs}ms"
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, true)))
                Timber.i("IntegrityMonitor: $msg")
                
                updateHealth { h ->
                    h.lastEnergyDeltaMa = event.deltaMa
                    h.lastEnergyDeltaTemp = event.deltaTemp
                    h.lastEnergyDurationMs = event.durationMs
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
        val stallThreshold = INTEGRITY_HEARTBEAT_INTERVAL_MS * 3
        
        val storageStalled = lastStorageUpdateRt > 0 && (nowRt - lastStorageUpdateRt) > stallThreshold
        val powerStalled = lastPowerUpdateRt > 0 && (nowRt - lastPowerUpdateRt) > stallThreshold
        val locationStalled = lastLocationStatusUpdateRt > 0 && (nowRt - lastLocationStatusUpdateRt) > stallThreshold

        if (storageStalled || powerStalled || locationStalled) {
            val stalls = mutableListOf<String>()
            if (storageStalled) stalls.add("Storage")
            if (powerStalled) stalls.add("Power")
            if (locationStalled) stalls.add("Location")
            
            val msg = "INTEGRITY WARNING: Reactive flow stall detected (${stalls.joinToString(", ")}). Monitoring vitality compromised on this device."
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, true)))
        }

        var cpu = systemStatusProvider.getCpuLoad()
        var iow = systemStatusProvider.getIoWait()
        var maxIo = LatencyMonitor.consumeMaxIoLatency()

        if (isMaliAnomalySimulated.get()) {
            cpu = 7.5
            maxIo = 1200L
            iow = 2.0
        }

        var maliAnomaly = false
        if (systemStatusProvider.isStaggeredPerformanceTier() || isMaliAnomalySimulated.get()) {
            if (maxIo > LATENCY_THRESHOLD_DB_WRITE_MS) {
                val msg = "PERFORMANCE WARNING: Critical I/O Spike detected on budget hardware (%dms). System stress: [CPU: %.1f, IOW: %.1f]".format(maxIo, cpu, iow)
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, true)))
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_PERFORMANCE_SPIKE)))
            }
            
            maliAnomaly = checkMaliDriverAnomaly(maxIo, cpu, iow)
        }

        hardwareSuite.setMaliAnomaly(maliAnomaly)

        updateHealth { h ->
            h.lastIntegrityHeartbeatRt = nowRt
            h.cpuLoad = cpu
            h.ioWait = iow
            h.maxIoLatency = maxIo
            h.isMaliAnomaly = maliAnomaly
            
            val isSilent = SentinelValidator.isSilentFailure(
                gpsStalled = h.gpsStalled,
                isTamperDetected = h.isTamperDetected,
                cpuLoad = cpu,
                ioWait = iow,
                maxIoLatency = maxIo,
                isThermalThrottling = h.isThermalThrottling
            )
            
            if (isSilent && !h.isSilentFailure) {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("FORENSIC ALERT: Silent Failure detected on this device. Location stall correlated with high resource load.", true)))
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_SILENT_FAILURE)))
            } else if (!isSilent && h.isSilentFailure) {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_SILENT_FAILURE)))
            }
            h.isSilentFailure = isSilent
        }
    }

    private fun checkMaliDriverAnomaly(maxIo: Long, cpu: Double, iow: Double): Boolean {
        if (maxIo > 500 && cpu > 6.0) {
            if (!currentHealth.isMaliAnomaly) {
                Timber.w("Forensic Audit (R266): Potential Mali driver configuration failure suspected. [IO: %dms, CPU: %.1f]", maxIo, cpu)
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(
                    "STRESS AUDIT: Mali Driver Anomaly detected on this device (High I/O correlation). UI throttling engaged.",
                    isImportant = true
                )))
            }
            return true
        }
        return false
    }

    private fun updateHealth(mutator: (SystemHealthState) -> Unit) {
        _health.update { current ->
            mutator(current)
            repository.updateHealth(current)
            current
        }
    }

    private fun handleLocationStatusUpdate(status: LocationStatus) {
        val workingHealth = currentHealth
        if (status.isPending && !workingHealth.isLocationPending) {
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Location fix pending: ${status.reason.name.replace("_", " ")} on this device", false)))
        } 
        else if (!status.isPending && workingHealth.isLocationPending && status.recoveryConfirmed) {
            val durationSec = status.lastPendingDurationMs / 1000.0
            val reasonStr = workingHealth.locationPendingReason.name.replace("_", " ")
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Location fix restored after ${"%.1f".format(durationSec)}s gap ($reasonStr resolved) on this device", false)))
        }

        val isStalled = status.reason == LocationPendingReason.GPS_STALL
        if (isStalled && !workingHealth.gpsStalled) {
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("GPS STALL: Hardware fix on this device has not updated despite satellite visibility.", true)))
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
        var coolingEnteredTimestamp = workingHealth.coolingEnteredRt
        
        if (!isCooling && batteryTemp >= MAX_SAFE_TEMPERATURE_CELSIUS) {
            isCooling = true
            coolingEnteredTimestamp = nowRt
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Thermal limit reached (${batteryTemp}°C). Entering forced COOLING MODE on this device.", true)))
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_TRACKER_TEMP)))
        } else if (isCooling && batteryTemp < MAX_SAFE_TEMPERATURE_RECOVERY) {
            isCooling = false
            coolingEnteredTimestamp = 0L
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("System Info: Thermal limit recovered (${batteryTemp}°C) on this device.", false)))
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
            h.coolingEnteredRt = coolingEnteredTimestamp
            h.isThermalThrottling = isCooling 
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
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("SYSTEM EMERGENCY: Internal storage is CRITICAL (${megabytesAvailable}MB) on this device.", true)))
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL)))
            }
        }

        if (low != workingHealth.isStorageLow) {
            if (low && !critical) {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("SYSTEM WARNING: Internal storage is low (${megabytesAvailable}MB) on this device.", true)))
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW)))
            } else if (!low) {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("System Info: Storage space restored (${megabytesAvailable}MB) on this device.", false)))
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
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("SYSTEM WARNING: Power Save Mode active on this device. Sensors and GPS may be throttled.", true)))
            } else {
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("System Info: Power Save Mode deactivated on this device.", false)))
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
                    domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, isCritical)))
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
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Network switched to $newNet on this device", false)))
        }

        var isPowerTamper = workingHealth.isPowerTamper
        if (lastPowerDisconnectTs > 0 && !isPowerTamper) {
            if (checkViolationSustained(ALERT_ID_TRACKER_POWER, lastPowerDisconnectTs, POWER_DISCONNECT_DEBOUNCE_MS)) {
                isPowerTamper = true
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Device power tamper confirmed (debounce met) on this device", true)))
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
        
        val isHighLoad = currentHealth.cpuLoad > 0.7 || currentHealth.isThermalThrottling
        val threshold = if (isHighLoad) {
            BATTERY_STEEP_DISCHARGE_THRESHOLD_HIGH_LOAD
        } else {
            BATTERY_STEEP_DISCHARGE_THRESHOLD_NORMAL
        }
        
        val isSteep = drop >= threshold
        
        if (isSteep && !currentHealth.isBatterySteepDischarge) {
            val elapsedMin = (nowRt - earliest.first) / 60000
            val loadContext = if (isHighLoad) "(High Load: CPU %.1f)".format(currentHealth.cpuLoad) else "(Normal Load)"
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("CRITICAL BATTERY HEALTH: Steep discharge detected on this device $loadContext ($drop% in ${elapsedMin}m).", true)))
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_BATTERY_STEEP_DISCHARGE)))
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
        domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, active)))
        
        val nowRt = timeProvider.elapsedRealtime()
        val coolingEnteredTimestamp = if (active) nowRt else 0L

        if (active) domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_TRACKER_TEMP)))
        else domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_TEMP)))

        updateHealth { h ->
            h.isCoolingModeActive = active
            h.coolingEnteredRt = coolingEnteredTimestamp
            h.isThermalThrottling = active
        }
    }

    /**
     * simulateStoragePressure: Simulation hook for Chapter 12.3 audit (R197).
     */
    fun simulateStoragePressure(active: Boolean, critical: Boolean) {
        isStorageSimulated.set(active)
        isStorageCriticalSimulated.set(critical)
        
        val msg = when {
            !active -> "System Info: Simulated Storage pressure recovered."
            critical -> "SYSTEM EMERGENCY: Simulated Internal storage is CRITICAL."
            else -> "SYSTEM WARNING: Simulated Internal storage is low."
        }
        
        domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, active)))
        
        if (active) {
            if (critical) domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_CRITICAL)))
            else domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_SYSTEM_STORAGE_LOW)))
        } else {
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_SYSTEM_STORAGE_CRITICAL)))
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_SYSTEM_STORAGE_LOW)))
        }

        updateHealth { h ->
            h.isStorageLow = active
            h.isStorageCritical = active && critical
        }
    }

    /**
     * simulateMaliAnomaly: Simulation hook for Chapter 13 audit (R266).
     */
    fun simulateMaliAnomaly(active: Boolean) {
        isMaliAnomalySimulated.set(active)
        val msg = if (active) "System Info: Mali Driver Anomaly simulation ENABLED." 
                  else "System Info: Mali Driver Anomaly simulation DISABLED."
        domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent(msg, false)))
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
                domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(ALERT_ID_LOCAL_INTERNET)))
                updateHealth { it.localInternetLoss = true }
                return false
            }
        } else {
            sustainedViolations.remove(ALERT_ID_LOCAL_INTERNET)
            updateHealth { it.localInternetLoss = false }
        }
        return true
    }

    /**
     * checkSignalIntegrity: Enhanced Signal Loss auditing with forensic grace periods.
     * Issue #1060: Transitioned to unified PerformanceTier inspection via provider (R-ID 348).
     * Issue #247: Mitigates false positives on budget hardware (A15) by injecting 
     * a hardware-specific grace period (5s) for telemetry gaps.
     */
    fun checkSignalIntegrity(nowRt: Long, silenceDelta: Long, isTracker: Boolean): Boolean {
        var threshold = if (isTracker) {
            VIEWER_SIGNAL_LOSS_THRESHOLD_MS
        } else {
            TRACKER_SIGNAL_LOSS_THRESHOLD_MS
        }

        if (systemStatusProvider.isStaggeredPerformanceTier()) {
            threshold += BUDGET_HARDWARE_SIGNAL_GRACE_MS
        }

        val loss = silenceDelta > threshold
        
        if (loss != currentHealth.signalLoss) {
            updateHealth { it.signalLoss = loss }
        }
        return !loss
    }

    fun checkViolationSustained(type: String, startTs: Long, threshold: Long): Boolean {
        if (startTs > 0 && (timeProvider.elapsedRealtime() - startTs) > threshold) {
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationSustained(type)))
            return true
        }
        return false
    }

    fun onPowerDisconnected() {
        if (!currentHealth.isPowerTamper && lastPowerDisconnectTs == 0L) {
            lastPowerDisconnectTs = timeProvider.elapsedRealtime()
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Device power unplugged, starting debounce... on this device", false)))
        }
    }

    fun onPowerConnected() {
        lastPowerDisconnectTs = 0L
        if (currentHealth.isPowerTamper) {
            updateHealth { it.isPowerTamper = false }
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER)))
            domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.LogEvent("Device power restored on this device", false)))
        }
    }

    fun onPowerRestored() { onPowerConnected() }

    fun clearPowerTamper() {
        updateHealth { it.isPowerTamper = false }
        domainEventBus.emit(DomainEvent.Integrity(IntegrityEvent.ViolationResolved(ALERT_ID_TRACKER_POWER)))
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
        
        // Issue #1142: Reset vitality timestamps to prevent spurious "Flow Stall" alerts.
        lastInternetUpdateRt = 0L
        lastBatteryUpdateRt = 0L
        lastStorageUpdateRt = 0L
        lastPowerUpdateRt = 0L
        lastLocationStatusUpdateRt = 0L

        isStorageSimulated.set(false)
        isStorageCriticalSimulated.set(false)
        isMaliAnomalySimulated.set(false)
    }

    fun getBatteryLevel(): Int = currentHealth.batteryLevel
    fun getBatteryCurrent(): Int = currentHealth.currentMa
}
