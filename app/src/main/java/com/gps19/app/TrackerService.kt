package com.gps19.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * Sep.24.30:
 * - Issue #1307 REMEDIATION: Decoupled spike-triggered forensic captures from the sampling 
 *   rate delay by transitioning to a buffered channel with non-blocking timeout polling.
 * Sep.24.20:
 * - Issue #1256: Monotonic Latch Staleness Across Reboots (R-ID 462).
 * - Issue #1260: Boot-ID Validation for Persistent Monotonic Latches.
 * - Issue #1234 / #1244 REMEDIATION: Corrected Thermal Recovery Latency logic to measure 
 *   total duration from entry to exit of cooling mode.
 * Sep.24.10:
 * - Issue #1301 REMEDIATION: Loaded and persisted Lux and Acoustic baselines across service
 *   restarts to eliminate the startup baseline learning period (R-ID 461).
 * Sep.24.04:
 * - Issue #1271 REMEDIATION: Restored hardwareSuite's adaptive vibration floor during 
 *   initialization using the persisted value to prevent sensitivity resets.
 * Sep.24.02:
 * - Issue #1255 REMEDIATION: Implemented reboot-aware monotonic clock recovery via 
 *   HistoryManager.recoverLastRealtime using role-isolated clock drift reference.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    @Inject lateinit var sessionCoordinator: SessionLifecycleCoordinator
    @Inject lateinit var deviceProfileManager: DeviceProfileManager

    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var revivalEventsJob: Job? = null
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var forensicSamplingJob: Job? = null
    
    private val forensicTriggerChannel = kotlinx.coroutines.channels.Channel<Boolean>(kotlinx.coroutines.channels.Channel.BUFFERED)

    private fun triggerForensicSample(isSpike: Boolean = false) {
        forensicTriggerChannel.trySend(isSpike)
    }

    private val locationBuffer = ConcurrentLinkedQueue<Location>()
    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    private var lastGpsSpeed = 0.0
    private var lastGpsAccuracy = 0.0
    private var lastGpsBearing = 0.0
    
    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var lastFastPathAcousticSpikeTs = 0L
    private var lastFastPathLightSpikeTs = 0L

    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L
    
    private var isSuspiciousMode = false
    private var currentIntervalMs = TICK_INTERVAL_MS

    private var lastForensicLat = 0.0
    private var lastForensicLng = 0.0
    private var lastForensicVibe = 0.0
    private var lastForensicTilt = 0.0

    private var lastWasCooling = false
    private var coolingEnteredRt = 0L
    private val forensicCaptureMutex = Mutex()

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    override fun onServicePreInit() {
        notificationManager.setTrackerMode(true)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync("T_" + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync("T_" + LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

        configManager.deviceId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        configManager.viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = true
        
        refreshCapabilitiesInternal()

        deviceProfileManager.initializeHardwareProfile(capabilities, configManager.deviceId)

        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeSensorEvents()
        observeCommandEvents()
        observeRevivalEvents()

        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, true)
        
        val settingsSnapshot = repository.getSettingsSnapshot()
        
        val savedMaxAcc = repository.getDouble("T_" + MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong("T_" + LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble("T_" + CHAIR_BASELINE_TILT_KEY, -1000.0)
        val savedVibeFloor = repository.getDouble("T_" + ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0)
        val savedLuxBaseline = repository.getDouble("T_" + TRACKER_LUX_BASELINE_KEY, -1.0)
        val savedAcousticFloor = repository.getDouble("T_" + TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
        val trackerState = repository.loadTrackerState("T_")
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)
        
        locationProcessor.loadState(
            savedMaxAccuracy = savedMaxAcc,
            savedLastSitTs = savedLastSitTs,
            savedBaseline = savedBaseline,
            trackerState = trackerState,
            homePoints = homePoints,
            maxDistance = maxDist,
            savedVibrationFloor = savedVibeFloor,
            savedLuxBaseline = savedLuxBaseline,
            savedAcousticFloor = savedAcousticFloor
        )

        if (savedVibeFloor >= 0.0) {
            hardwareSuite.setAdaptiveVibrationFloor(savedVibeFloor)
        }

        val savedAlarms = repository.getLastAlarmsJson("T_")
        alarmManager.restoreState(savedAlarms)
        alarmManager.restoreLogicState(settingsSnapshot, "T_")

        historyManager.initialize(lifecycleScope, "T_")
        
        hardwareSuite.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        settingsJob = lifecycleScope.launch(Dispatchers.Default) {
            launch { repository.alertSettingsFlow.collectLatest { settings -> alarmManager.updateSettings(settings) } }
            launch { repository.homePointsFlow.collectLatest { points -> locationProcessor.setHomePoints(points.map { EngineGeoPoint(it.latitude, it.longitude) }) } }
            launch { repository.maxDistanceFlow.collectLatest { dist -> locationProcessor.setMaxDistanceAuthority(dist) } }
            launch { repository.isSafeMode.collectLatest { safe -> hardwareSuite.setSafeMode(safe) } }
        }

        val recoveredTs = repository.getLong("T_" + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong("T_" + CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = historyManager.recoverLastRealtime(recoveredTs, recoveredDrift)
        locationProcessor.setLastValidFixRt(timeProvider.elapsedRealtime()) 
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()

        systemMonitor.setSessionStart(serviceStartRealtime)

        setupPhysicalFastPaths()
        startTickLoop()
        startHeartbeatLoop()
        startForensicSamplingLoop()
        
        logManager.logServiceEvent("Tracker Engine Online (Coordinated)", isImportant = true)
    }

    private fun observeAlarmEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.alarmEvents.collect { event ->
                when (event) {
                    is AlarmEvent.LogEvent -> {
                        logManager.submitToLogSink(
                            message = event.message,
                            type = event.type,
                            isImportant = event.isImportant,
                            extremeValue = event.extremeValue,
                            localId = event.logId,
                            durationMs = event.durationMs,
                            isSpecial = event.isSpecial,
                            specialColor = event.specialColor,
                            lat = event.lat,
                            lng = event.lng,
                            accuracy = event.accuracy,
                            maxAccuracy = event.maxAccuracy,
                            snr = event.snr,
                            vibe = event.vibe
                        )
                    }
                }
            }
        }
    }

    private fun observeIntegrityEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            integrityMonitor.integrityEvents.collect { event ->
                when (event) {
                    is IntegrityEvent.ViolationSustained -> {
                        when (event.type) {
                            ALERT_ID_TRACKER_POWER -> alarmManager.setPowerAlarmPending(true, "T_")
                            ALERT_ID_GPS_HARDWARE_LOCK, ALERT_ID_SILENT_FAILURE, 
                            ALERT_ID_PERFORMANCE_SPIKE, ALERT_ID_SYSTEM_STORAGE_LOW, 
                            ALERT_ID_SYSTEM_STORAGE_CRITICAL, ALERT_ID_BATTERY_STEEP_DISCHARGE -> {
                            }
                        }
                    }
                    is IntegrityEvent.ViolationResolved -> {
                        if (event.type == ALERT_ID_TRACKER_POWER) alarmManager.setPowerAlarmPending(false, "T_")
                    }
                    is IntegrityEvent.LogEvent -> {
                        val isSpecial = event.message.contains("tamper", ignoreCase = true) || 
                                       event.message.contains("confirmed", ignoreCase = true) || 
                                       event.message.contains("EMERGENCY", ignoreCase = true) || 
                                       event.message.contains("PRIORITY", ignoreCase = true) || 
                                       event.message.contains("BUCKET", ignoreCase = true) ||
                                       event.message.contains("ENERGY AUDIT", ignoreCase = true)
                        logManager.logServiceEvent(m = event.message, isImportant = event.isImportant, isSpecial = isSpecial, specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null)
                    }
                }
            }
        }
    }

    private fun observeRevivalEvents() {
        revivalEventsJob?.cancel()
        revivalEventsJob = lifecycleScope.launch(Dispatchers.Default) {
            hardwareSuite.revivalEvents.collect { event ->
                when (event) {
                    is HardwareSuite.RevivalEvent.Footprint -> {
                        val msg = "ENERGY AUDIT: Revival Footprint (R-ID 259) - Delta: ${event.deltaMa}mA, Temp Rise: ${event.deltaTemp}°C, Duration: ${event.durationMs}ms"
                        val proc = lastProcessedLocation
                        logManager.submitToLogSink(msg, "system", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is HardwareSuite.RevivalEvent.HardwareLock -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent(m = "CRITICAL: GPS_HARDWARE_LOCK - All revival attempts failed. Hardware stall confirmed.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is HardwareSuite.RevivalEvent.Attempt -> {
                        logManager.logServiceEvent(m = "GPS REVIVAL: Hardware restart attempt ${event.count} triggered.", isImportant = false)
                    }
                    is HardwareSuite.RevivalEvent.Success -> {
                        logManager.logServiceEvent(m = "GPS REVIVAL: Hardware fix restored successfully.", isImportant = true)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeProcessorEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            locationProcessor.processorEvents.collect { event ->
                when (event) {
                    is ProcessorEvent.TrailPointSaved -> {
                        repository.saveTrailPoint(event.lat, event.lng, event.isViewerTrail, event.status, event.timestamp, accuracy = event.accuracy, maxAccuracy = event.maxAccuracy)
                    }
                    is ProcessorEvent.LogAdded -> {
                        val isMergeStale = event.message.contains("Merge-on-Stale")
                        val specialColor = if (event.isSpecial || isMergeStale) FORENSIC_PINK_COLOR else null
                        logManager.submitToLogSink(
                            message = event.message,
                            type = event.type,
                            isImportant = event.isImportant,
                            isSpecial = event.isSpecial || isMergeStale,
                            specialColor = specialColor,
                            lat = event.lat,
                            lng = event.lng,
                            accuracy = event.accuracy,
                            snr = event.snr,
                            vibe = event.vibe
                        )
                    }
                    is ProcessorEvent.MaxAccuracyChanged -> {
                        repository.saveDoubleSync("T_" + MAX_ACCURACY_KEY, event.accuracy)
                    }
                    is ProcessorEvent.ChairBaselineChanged -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent(m = "Passive Zeroing: Chair baseline calibrated to ${event.baseline.roundToOneDecimal()}°",
                            lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                        repository.saveDouble("T_" + CHAIR_BASELINE_TILT_KEY, event.baseline)
                    }
                    is ProcessorEvent.VibrationFloorChanged -> {
                        repository.saveDoubleSync("T_" + ADAPTIVE_VIBRATION_FLOOR_KEY, event.floor)
                    }
                    is ProcessorEvent.LuxBaselineChanged -> {
                        repository.saveDoubleSync("T_" + TRACKER_LUX_BASELINE_KEY, event.baseline)
                    }
                    is ProcessorEvent.AcousticFloorChanged -> {
                        repository.saveDoubleSync("T_" + TRACKER_ACOUSTIC_FLOOR_KEY, event.floor)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeConnectivityEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            connectivitySuite.connectivityEvents.collect { event ->
                when (event) {
                    is ConnectivityEvent.PeerPulse -> handleViewerPulse(event.id)
                }
            }
        }
    }

    private fun observeHistoryEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            historyManager.historyEvents.collect { event ->
                when (event) {
                    is HistoryEvent.LogEvent -> logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
                }
            }
        }
    }

    private fun observeSensorEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            hardwareSuite.sensorEvents.collect { event ->
                when (event) {
                    is AppSensorEvent.HardwareFailure -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent(m = "CRITICAL: SENSOR_HARDWARE_FAILURE - ${event.reason}", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is AppSensorEvent.LogEvent -> {
                        logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
                    }
                }
            }
        }
    }

    private fun observeCommandEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            commandRouter.commandEvents.collect { event ->
                when (event) {
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { 
                        refreshCapabilitiesInternal()
                        launch { hardwareSuite.start() }
                    }
                    is CommandEvent.ExecuteStressTest -> executeAutomatedStressTest()
                    is CommandEvent.SimulateStoragePressure -> {} 
                }
            }
        }
    }

    private suspend fun refreshCapabilitiesInternal() {
        val perms = systemStatusProvider.getPermissionState(forceRefresh = true)
        capabilities = HardwareCapabilities(
            hasBackgroundRestriction = perms.hasBackgroundRestriction,
            backgroundStatus = perms.backgroundStatus,
            autostartStatus = perms.autostartStatus,
            requiresWakeLockRenewal = perms.requiresWakeLockRenewal,
            requiresExtraTopPadding = perms.requiresExtraTopPadding,
            isManualOverrideActive = perms.isManualOverride,
            isA15Device = perms.isA15Device,
            isSamsungDevice = perms.isSamsungDevice,
            isHuaweiDevice = perms.isHuaweiDevice,
            isMicrophoneGranted = perms.isMicrophoneGranted,
            performanceTier = perms.performanceTier
        )
    }

    private fun setupPhysicalFastPaths() {
        hardwareSuite.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent(m = "Acoustic Spike Detected (FastPath)", isImportant = false)
                lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime()
                triggerForensicSample(isSpike = true)
            }
        )
        hardwareSuite.setLightFastPath(
            baseline = locationProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP,
            onSpike = {
                logManager.logServiceEvent(m = "Light Spike Detected (FastPath)", isImportant = false)
                lastFastPathLightSpikeTs = timeProvider.elapsedRealtime()
                triggerForensicSample(isSpike = true)
            }
        )
    }

    private fun onLocationChanged(location: Location) {
        locationBuffer.add(location)
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) return
        repository.updateRemoteActivity(timeProvider.elapsedRealtime())

        if ((configManager.viewerId == SettingsRepository.DEFAULT_VIEWER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
            configManager.viewerId = id
            connectivitySuite.updateIdentity(configManager.deviceId, id, true)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(VIEWER_ID_KEY, id) } 
        }
        
        val isNew = sessionManager.onViewerPulse(id, timeProvider.elapsedRealtime())
        if (isNew || tickJob?.isActive != true) {
            if (isNew) {
                logManager.logServiceEvent(m = "Viewer connected: $id")
            }
            startTickLoop() 
        }
    }

    private fun resetServiceTimers() { 
        sessionCoordinator.resetSession(
            roleTag = "T",
            processors = listOf(locationProcessor),
            onReset = {
                serviceStartRealtime = timeProvider.elapsedRealtime()
                serviceStartWall = timeProvider.currentTimeMillis()
                
                lastForensicLat = 0.0
                lastForensicLng = 0.0
                lastForensicVibe = 0.0
                lastForensicTilt = 0.0
                lastWasCooling = false
                coolingEnteredRt = 0L
                
                lastHardwareRecoveryTs = 0L
                lastFastPathAcousticSpikeTs = 0L
                lastFastPathLightSpikeTs = 0L
                setupPhysicalFastPaths()
                
                locationBuffer.clear()
            }
        )
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        val health = integrityMonitor.currentHealth
        val battery = if (health.batteryLevel > 0) health.batteryLevel else integrityMonitor.getBatteryLevel()
        val msg = notificationManager.getPulseMessage(
            sats = 0,
            battery = battery,
            isSecure = !alarmManager.hasUnresolvedAlarms(),
            isPowerSave = health.isPowerSaveMode
        )
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type, force = true)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(200)
                val type = getAvailableForegroundServiceType()
                val health = integrityMonitor.currentHealth
                val msg = notificationManager.getPulseMessage(
                    hardwareSuite.satellitesUsed,
                    health.batteryLevel,
                    isSecure = !alarmManager.hasUnresolvedAlarms(),
                    isPowerSave = isPowerSaveActive || health.isPowerSaveMode
                )
                safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun getAvailableForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && capabilities.isA15Device) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isMicEnabled = hardwareSuite.isAcousticMonitoringEnabled()
            val hasPermission = capabilities.isMicrophoneGranted
            if (hasPermission && (isMicEnabled || isRecentUiPulse())) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
            }
        }
        return type
    }

    override fun getRequiredTickInterval(): Long { return if (isPowerSaveActive) POWER_SAVE_TICK_INTERVAL_MS else TICK_INTERVAL_MS }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt)
        integrityMonitor.checkInternetIntegrity(nowRt)
        
        val health = integrityMonitor.currentHealth
        val snapshot = hardwareSuite.consumeLogicSnapshot()

        val sensorSnapshot = SensorStateSnapshot(
            vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, powerTamper = health.isPowerTamper,
            tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, peakShock = snapshot.peakShock, acousticMinDb = snapshot.acousticPeakMin,
            peakVerticalVelocity = snapshot.peakVerticalVelocity, peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs, peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt,
            plungeMatched = snapshot.plungeMatched, peakVerticalDisplacement = snapshot.peakVerticalDisplacement, isSirenActive = false, isWarming = hardwareSuite.isWarming,
            manualAdaptiveFloor = -1.0, acousticLockoutRt = lastFastPathAcousticSpikeTs, lightSpikeRt = lastFastPathLightSpikeTs, isMuzzled = false,
            kineticEnergy = snapshot.kineticEnergy, providedAdaptiveFloor = snapshot.adaptiveVibrationFloor, nowRt = nowRt, nowTs = now
        )

        val evalSnapshot = EvaluationSnapshot(health = health, sensor = sensorSnapshot)

        // Issue #1233: Omit onSpike callback to prevent high allocation churn during tick updates.
        hardwareSuite.setLightFastPath(
            baseline = locationProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP
        )

        hardwareSuite.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0
        )

        hardwareSuite.setHighLoad(evalSnapshot.health.isCoolingModeActive)
        
        isSuspiciousMode = serviceBehaviorUseCase.updateSuspiciousMode(
            currentSuspicious = isSuspiciousMode,
            isPhysicalViolation = locationProcessor.sentinel.checkPhysicalTamper(nowRt, false) == SentinelStatus.TAMPER,
            isSitDetected = locationProcessor.consumeSitDetected(),
            nowRt = nowRt
        )
        
        val targetGpsInterval = serviceBehaviorUseCase.calculateGpsInterval(
            isCoolingMode = evalSnapshot.health.isCoolingModeActive,
            isSuspiciousMode = isSuspiciousMode,
            isStationary = hardwareSuite.isStationary(),
            isScreenOn = hardwareSuite.isScreenOn(),
            isGeofenceActive = locationProcessor.getMaxDistanceAuthority() > 0.0,
            nowRt = nowRt,
            capabilities = capabilities
        )
        
        if (targetGpsInterval != currentIntervalMs) {
            currentIntervalMs = targetGpsInterval
            forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, "T")
            locationProcessor.updateExpectedInterval(nowRt, targetGpsInterval)
            hardwareSuite.setPollingInterval(targetGpsInterval)
        }
        
        val isSocketConnected = connectivitySuite.isConnected()
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isViewerActive = sessionManager.getViewerCount() > 0 || isRecentUiPulse()
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isSocketConnected && isViewerActive, isInViolation = alarmManager.hasUnresolvedAlarms())

        deviceProfileManager.executeContinuityTweaks(
            capabilities = capabilities,
            nowRt = nowRt,
            serviceTickCounter = serviceTickCounter,
            lastValidFixRt = locationProcessor.getLastValidFixRt(),
            isPowerSaveMode = isPowerSaveActive || evalSnapshot.health.isPowerSaveMode,
            localInternetLoss = evalSnapshot.health.localInternetLoss,
            isSocketConnected = isSocketConnected,
            isPeerActive = isViewerActive
        )

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            val isStaggered = capabilities.performanceTier == PerformanceTier.STAGGERED
            val recoveryThreshold = if (isStaggered) 10000L else HARDWARE_SUPPRESSION_THRESHOLD_MS
            
            if (tickGap > recoveryThreshold && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                recoveryFlagged = true
                val proc = lastProcessedLocation
                logManager.logServiceEvent(m = "HEURISTIC RECOVERY: Heartbeat gap detected (${tickGap}ms). Reviving connection.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                systemMonitor.acquireWakeLock()
                connectivitySuite.connect(configManager.relayUrl)
            }
        }

        forensicAuditor.evaluateStability(nowRt, "T")?.let { verdict ->
            val proc = lastProcessedLocation
            logManager.logServiceEvent(
                m = verdict.message,
                isImportant = true,
                isSpecial = verdict.isJitterViolation,
                specialColor = if (verdict.isJitterViolation) FORENSIC_PINK_COLOR else null,
                lat = proc?.optimizedPoint?.lat ?: 0.0,
                lng = proc?.optimizedPoint?.lng ?: 0.0,
                accuracy = lastGpsAccuracy
            )
        }
        
        locationProcessor.updateSensorData(evalSnapshot.sensor)

        val noiseIdx = (evalSnapshot.sensor.acousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val luxIdx = log10(evalSnapshot.sensor.lux + 1.0) / RIBBON_LUX_LOG_SCALE
        val vibeIdx = evalSnapshot.sensor.vibration / RIBBON_VIBRATION_SCALE_G
        val liftIdx = (evalSnapshot.sensor.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
        
        val satellites = latestGnssDetail?.satellites ?: emptyList()
        val avgCn0 = satellites.map { it.cn0 }.safeAverage()
        val snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB
        
        val tiltIdx = abs(evalSnapshot.sensor.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
        val baroIdx = (evalSnapshot.sensor.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val hasUnresolved = alarmManager.hasUnresolvedAlarms()
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(isStationary = hardwareSuite.isStationary(), isGpsStalled = evalSnapshot.health.gpsStalled, hasUnresolvedAlarms = hasUnresolved, isUiVisible = isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; hardwareSuite.setPowerSaveMode(shouldBePowerSave); logManager.logServiceEvent(m = "POWER SAVER: ${if (shouldBePowerSave) "ENGAGED" else "DISABLED"}", isImportant = false)
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        var lastLocInBatch: Location? = null
        while (locationBuffer.isNotEmpty()) {
            val loc = locationBuffer.poll() ?: break
            lastLocInBatch = loc
            val processed = locationProcessor.processGpsPoint(
                lat = loc.latitude, lng = loc.longitude, alt = loc.altitude, androidSpeedMps = loc.speed.toDouble(), gpsTs = loc.time, accuracy = loc.accuracy.toDouble(), bearing = loc.bearing.toDouble(), snr = avgCn0, satsUsed = latestGnssDetail?.satellites?.count { it.usedInFix } ?: 0, isViewerTrail = false, lastGpsTs = forensicAuditor.getLastGpsFixRealtime("T"), isLocal = true, providedAcousticLockoutRt = lastFastPathAcousticSpikeTs, providedLightSpikeRt = lastFastPathLightSpikeTs, nowWall = now, nowRt = nowRt,
                providedIsStalled = evalSnapshot.health.gpsStalled,
                isSuspicious = isSuspiciousMode,
                providedAdaptiveVibrationFloor = evalSnapshot.sensor.providedAdaptiveFloor
            )
            lastProcessedLocation = processed
        }

        val proc = lastProcessedLocation
        if (proc != null && lastLocInBatch != null) {
            lastGpsSpeed = lastLocInBatch.speed.toDouble()
            lastGpsAccuracy = lastLocInBatch.accuracy.toDouble()
            lastGpsBearing = lastLocInBatch.bearing.toDouble()
            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isViewerActive, proc, snapshot, lastLocInBatch.time)
        }

        repository.updateLocation(LocationUpdate().apply {
            this.kinetic.lat = proc?.optimizedPoint?.lat ?: 0.0
            this.kinetic.lng = proc?.optimizedPoint?.lng ?: 0.0
            this.kinetic.alt = proc?.optimizedPoint?.alt ?: 0.0
            this.kinetic.speed = proc?.filteredSpeed ?: 0.0
            this.kinetic.accuracy = proc?.currentAccuracy ?: 0.0
            this.kinetic.bearing = lastGpsBearing
            this.kinetic.gpsTs = proc?.timestamp ?: 0L
            this.kinetic.rt = nowRt
            this.kinetic.maxAccuracy = proc?.maxAccuracy ?: 0.0
            this.kinetic.kineticEnergy = evalSnapshot.sensor.kineticEnergy
            this.kinetic.verticalVelocity = evalSnapshot.sensor.peakVerticalVelocity

            this.atmospheric.temp = evalSnapshot.health.batteryTemp
            this.atmospheric.maxTemp = evalSnapshot.health.maxTemp
            this.atmospheric.vibration = evalSnapshot.sensor.vibration
            this.atmospheric.heading = evalSnapshot.sensor.heading
            this.atmospheric.baroAlt = evalSnapshot.sensor.baroAlt
            this.atmospheric.lux = evalSnapshot.sensor.lux
            this.atmospheric.isNear = evalSnapshot.sensor.isNear
            this.atmospheric.tiltDegrees = evalSnapshot.sensor.tiltDegrees
            this.atmospheric.acousticDb = evalSnapshot.sensor.acousticDb
            this.atmospheric.peakVibrationShock = evalSnapshot.sensor.peakShock
            this.atmospheric.peakVibrationShockTs = now
            this.atmospheric.noiseIdx = noiseIdx
            this.atmospheric.luxIdx = luxIdx
            this.atmospheric.vibeIdx = vibeIdx
            this.atmospheric.liftIdx = liftIdx
            this.atmospheric.tiltIdx = tiltIdx
            this.atmospheric.baroIdx = baroIdx
            this.atmospheric.luxBaseline = locationProcessor.getLuxBaseline()
            this.atmospheric.acousticFloorDb = locationProcessor.getAcousticFloorDb()
            this.atmospheric.adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor()
            this.atmospheric.proxIdx = snapshot.proximityIdx
            this.atmospheric.proximityCm = snapshot.proximityCm
            this.atmospheric.proximityDebounceMs = snapshot.proximityDebounceMs
            this.atmospheric.vibrationRollingSum = snapshot.vibrationRollingSum

            this.integrity.battery = evalSnapshot.health.batteryLevel
            this.integrity.isCharging = evalSnapshot.health.isCharging
            this.integrity.currentMa = evalSnapshot.health.currentMa
            this.integrity.satsView = hardwareSuite.satellitesInView
            this.integrity.satsUsed = hardwareSuite.satellitesUsed
            this.integrity.snrIdx = snrIdx
            this.integrity.isPowerTamper = evalSnapshot.health.isPowerTamper
            this.integrity.isSitDetected = isSuspiciousMode
            this.integrity.lastSitTs = locationProcessor.getLastSitTs()
            this.integrity.sitVz = evalSnapshot.sensor.peakVerticalVelocity
            this.integrity.sitVzTs = evalSnapshot.sensor.peakVerticalVelocityTs
            this.integrity.sitVzRt = evalSnapshot.sensor.peakVerticalVelocityRt
            this.integrity.sitDz = evalSnapshot.sensor.peakVerticalDisplacement
            this.integrity.sitBaro = evalSnapshot.sensor.peakVerticalDisplacement
            this.integrity.sitTilt = evalSnapshot.sensor.tiltDegrees
            this.integrity.sitShock = evalSnapshot.sensor.peakShock
            this.integrity.isBatteryLow = evalSnapshot.health.isBatteryLow
            this.integrity.isBatteryCritical = evalSnapshot.health.isBatteryCritical
            this.integrity.locationPendingReason = evalSnapshot.health.locationPendingReason
            this.integrity.isPowerSaveMode = isPowerSaveActive || evalSnapshot.health.isPowerSaveMode
            this.integrity.standbyBucket = evalSnapshot.health.standbyBucket
            this.integrity.netInterface = evalSnapshot.health.netInterface
            this.integrity.isStorageLow = evalSnapshot.health.isStorageLow
            this.integrity.isStorageCritical = evalSnapshot.health.isStorageCritical
            this.integrity.isBatterySteepDischarge = evalSnapshot.health.isBatterySteepDischarge
            this.integrity.isCoolingModeActive = evalSnapshot.health.isCoolingModeActive
            this.integrity.gpsHardwareLock = evalSnapshot.health.gpsHardwareLock
            this.integrity.isUltraLongStationary = evalSnapshot.health.isUltraLongStationary
            this.integrity.isTamperDetected = proc?.tamperDetected ?: false
            this.integrity.tamperNote = proc?.suppressionNote

            this.ts = now
            this.isMe = true
            this.status = proc?.status ?: SentinelStatus.VALID
            this.lastValidFixRt = locationProcessor.getLastValidFixRt()
            this.trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING
        })

        if (isViewerActive) {
            val procViewer = lastProcessedLocation
            connectivitySuite.pushCurrentStatus(
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = lastLocInBatch, filtered = procViewer?.optimizedPoint, distToTracker = null, distToHome = procViewer?.distToHome, maxAccuracy = procViewer?.maxAccuracy ?: 0.0, filteredSpeed = procViewer?.filteredSpeed ?: 0.0, vibration = evalSnapshot.sensor.vibration, heading = evalSnapshot.sensor.heading, baroAlt = evalSnapshot.sensor.baroAlt, lux = evalSnapshot.sensor.lux, isNear = evalSnapshot.sensor.isNear, tiltDegrees = evalSnapshot.sensor.tiltDegrees, acousticDb = evalSnapshot.sensor.acousticDb, jumpTier = procViewer?.jumpTier ?: 0, isJammer = procViewer?.jammerDetected ?: false, isStalled = evalSnapshot.health.gpsStalled, peakShock = evalSnapshot.sensor.peakShock, peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx, proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs, vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false, isTamperDetected = procViewer?.tamperDetected ?: false, isPowerTamper = evalSnapshot.health.isPowerTamper, isSitDetected = isSuspiciousMode, isSitActive = false, lastSitTs = locationProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = evalSnapshot.sensor.peakVerticalVelocity, sitVz = evalSnapshot.sensor.peakVerticalVelocity, sitVzTs = evalSnapshot.sensor.peakVerticalVelocityTs, sitVzRt = evalSnapshot.sensor.peakVerticalVelocityRt, sitDz = evalSnapshot.sensor.peakVerticalDisplacement, sitBaro = evalSnapshot.sensor.peakVerticalDisplacement, sitTilt = evalSnapshot.sensor.tiltDegrees, sitShock = evalSnapshot.sensor.peakShock, isClockRegression = procViewer?.isClockRegression ?: false, isLocationPending = evalSnapshot.health.isLocationPending, locationPendingReason = evalSnapshot.health.locationPendingReason, lastValidFixRt = evalSnapshot.health.lastValidFixRt, gnssDetail = latestGnssDetail, snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = evalSnapshot.sensor.vibration / RIBBON_VIBRATION_SCALE_G, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, isBatterySteepDischarge = procViewer?.isClockRegression ?: false, isCoolingModeActive = evalSnapshot.health.isCoolingModeActive, batteryLevel = evalSnapshot.health.batteryLevel, temp = evalSnapshot.health.batteryTemp, isCharging = evalSnapshot.health.isCharging, trackerState = if ((procViewer?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, status = procViewer?.status ?: SentinelStatus.VALID, isStorageLow = evalSnapshot.health.isStorageLow, isStorageCritical = evalSnapshot.health.isStorageCritical, isPowerSaveMode = isPowerSaveActive || evalSnapshot.health.isPowerSaveMode, standbyBucket = evalSnapshot.health.standbyBucket, netInterface = evalSnapshot.health.netInterface, kineticEnergy = evalSnapshot.sensor.kineticEnergy, isAdaptiveJump = procViewer?.isAdaptiveJump ?: false, isBatteryLow = evalSnapshot.health.isBatteryLow, isBatteryCritical = evalSnapshot.health.isBatteryCritical, isUltraLongStationary = evalSnapshot.health.isUltraLongStationary, gpsHardwareLock = evalSnapshot.health.gpsHardwareLock,
                tamperNote = procViewer?.suppressionNote
            )
        }

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = if (isViewerActive && lastLocInBatch != null) 10 else 0, peerAvail = isSocketConnected && isViewerActive, hasGps = lastLocInBatch != null, isTrackerMode = true, accuracy = lastProcessedLocation?.currentAccuracy ?: 0.0, maxAccuracy = lastProcessedLocation?.maxAccuracy ?: 0.0, noiseIdx = noiseIdx, luxIdx = log10(evalSnapshot.sensor.lux + 1.0) / RIBBON_LUX_LOG_SCALE, vibeIdx = evalSnapshot.sensor.vibration / RIBBON_VIBRATION_SCALE_G, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB, tiltIdx = abs(evalSnapshot.sensor.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG, baroIdx = (evalSnapshot.sensor.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS, verticalVelocity = evalSnapshot.sensor.peakVerticalVelocity, sitVz = evalSnapshot.sensor.peakVerticalVelocity, sitVzTs = evalSnapshot.sensor.peakVerticalVelocityTs, sitVzRt = evalSnapshot.sensor.peakVerticalVelocityRt, sitDz = evalSnapshot.sensor.peakVerticalDisplacement, sitBaro = evalSnapshot.sensor.peakVerticalDisplacement, sitTilt = evalSnapshot.sensor.tiltDegrees, sitShock = evalSnapshot.sensor.peakShock, isBatterySteepDischarge = evalSnapshot.health.isBatterySteepDischarge, isCoolingModeActive = evalSnapshot.health.isCoolingModeActive, speed = lastProcessedLocation?.filteredSpeed ?: 0.0, bearing = lastLocInBatch?.bearing?.toDouble() ?: 0.0, isSitDetected = isSuspiciousMode, isSitActive = false, currentMa = evalSnapshot.health.currentMa, locationPendingReason = evalSnapshot.health.locationPendingReason, kineticEnergy = evalSnapshot.sensor.kineticEnergy, isRecoveryEvent = recoveryFlagged, cpuLoad = evalSnapshot.health.cpuLoad, ioWait = evalSnapshot.health.ioWait, maxIoLatency = evalSnapshot.health.maxIoLatency, isSilentFailure = evalSnapshot.health.isSilentFailure, isBatteryLow = evalSnapshot.health.isBatteryLow, isBatteryCritical = evalSnapshot.health.isBatteryCritical, isUltraLongStationary = evalSnapshot.health.isUltraLongStationary
        )

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync("T_" + LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync("T_" + LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        serviceTickCounter++
        triggerForensicSample()
    }

    override suspend fun onHeartbeat(now: Long, nowRt: Long) {
        if (isSystemActive) {
            val health = integrityMonitor.currentHealth
            notificationManager.updatePulse(
                sats = hardwareSuite.satellitesUsed, 
                battery = health.batteryLevel, 
                isSecure = !alarmManager.hasUnresolvedAlarms(), 
                isPowerSave = isPowerSaveActive || health.isPowerSaveMode
            )
        }
    }

    private suspend fun performForensicCapture(isSpike: Boolean) = forensicCaptureMutex.withLock {
        val health = integrityMonitor.currentHealth
        val proc = lastProcessedLocation
        val snapshot = hardwareSuite.consumeForensicSnapshot()
        
        val lat = proc?.optimizedPoint?.lat ?: 0.0
        val lng = proc?.optimizedPoint?.lng ?: 0.0
        val vibe = snapshot.vibration
        val tilt = snapshot.tiltDegrees

        // Issue #1307: Decouple spikes from sampling rate gates
        val dist = if (lastForensicLat != 0.0) PhysicsUtils.calculateDistance(lastForensicLat, lastForensicLng, lat, lng) else Double.MAX_VALUE
        val vibeDelta = abs(vibe - lastForensicVibe)
        val tiltDelta = abs(tilt - lastForensicTilt)
        
        val shouldLog = isSpike || dist > FORENSIC_SPATIAL_GATE_METERS || 
                       vibeDelta > FORENSIC_IMU_VIBRATION_THRESHOLD || 
                       tiltDelta > FORENSIC_IMU_TILT_THRESHOLD
        
        if (shouldLog) {
            lastForensicLat = lat; lastForensicLng = lng; lastForensicVibe = vibe; lastForensicTilt = tilt
            
            logManager.logForensicTraceOptimized(
                timestamp = timeProvider.currentTimeMillis(),
                lat = lat,
                lng = lng,
                accuracy = proc?.currentAccuracy ?: 0.0,
                maxAccuracy = proc?.maxAccuracy ?: 0.0,
                vibe = vibe,
                snr = snapshot.acousticDb,
                batteryLevel = health.batteryLevel,
                isCharging = health.isCharging,
                batteryTemp = health.batteryTemp
            )
        }
    }

    private fun startForensicSamplingLoop() {
        forensicSamplingJob?.cancel()
        forensicSamplingJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            initializationDeferred.await()
            delay(STARTUP_SETTLING_DELAY_MS)

            // Initial trigger to capture baseline state on service startup
            triggerForensicSample()

            while (isActive) {
                val health = integrityMonitor.currentHealth

                // Issue #1244 Fix: Thermal Recovery Latency Audit
                if (lastWasCooling && !health.isCoolingModeActive) {
                    val latency = timeProvider.elapsedRealtime() - coolingEnteredRt
                    logManager.logServiceEvent(m = "Forensic Performance Audit: Thermal Recovery Latency: ${latency}ms", isImportant = true)
                    coolingEnteredRt = 0L
                }
                if (health.isCoolingModeActive && !lastWasCooling) {
                    coolingEnteredRt = timeProvider.elapsedRealtime()
                }
                lastWasCooling = health.isCoolingModeActive

                val delayMs = when {
                    health.isCoolingModeActive -> FORENSIC_SAMPLING_INTERVAL_COOLING_MS
                    logManager.isForensicBufferUnderPressure() -> FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS
                    health.isCharging -> FORENSIC_SAMPLING_INTERVAL_MIN_MS
                    else -> FORENSIC_SAMPLING_INTERVAL_MAX_MS
                }

                val trigger = withTimeoutOrNull(delayMs) {
                    forensicTriggerChannel.receive()
                }

                performForensicCapture(isSpike = (trigger == true))
            }
        }
    }

    private fun executeAutomatedStressTest() {
        lifecycleScope.launch(Dispatchers.Default) {
            logManager.logServiceEvent(m = "FORENSIC STRESS TEST: Initiating 5s CPU/IO saturation burst.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
            
            val cpuJob = launch(Dispatchers.Default) {
                val end = System.currentTimeMillis() + 5000L
                var count = 0L
                while (System.currentTimeMillis() < end) {
                    sin(count.toDouble()); cos(count.toDouble()); sqrt(count.toDouble()); count++
                }
                logManager.logServiceEvent(m = "STRESS TEST: CPU Saturation complete ($count iterations).", isImportant = false)
            }

            val ioJob = launch(Dispatchers.IO) {
                val end = System.currentTimeMillis() + 5000L
                val data = ByteArray(1024 * 1024) { 0xFF.toByte() } 
                val tempFile = File(cacheDir, "stress_test.tmp")
                var writes = 0
                while (System.currentTimeMillis() < end) {
                    try {
                        FileOutputStream(tempFile).use { fos ->
                            fos.write(data); fos.flush()
                        }
                        writes++
                    } catch (e: Exception) {
                        Timber.e(e, "Stress Test IO failure")
                    }
                }
                tempFile.delete()
                logManager.logServiceEvent(m = "STRESS TEST: IO Saturation complete ($writes MB written).", isImportant = false)
            }

            val forensicJob = launch(Dispatchers.Default) {
                repeat(500) { i ->
                    logManager.logForensicTrace("STRESS_BURST: Forensic sample #$i injection.")
                    if (i % 100 == 0) delay(1) 
                }
                logManager.logServiceEvent(m = "STRESS TEST: Forensic Saturation burst complete.", isImportant = false)
            }

            joinAll(cpuJob, ioJob, forensicJob)
            logManager.logServiceEvent(m = "FORENSIC STRESS TEST: Saturation routine COMPLETED.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); revivalEventsJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel(); forensicSamplingJob?.cancel()
        deviceProfileManager.teardownHardwareProfile(capabilities)
        super.onDestroy()
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSocketConnected: Boolean, isViewerActive: Boolean, processed: ProcessedLocation, snapshot: HardwareSuite.ForensicSnapshot, rawGpsTs: Long) {
        val health = integrityMonitor.currentHealth
        
        val telemetry = AlarmTelemetrySnapshot(
            status = processed.status,
            isJammer = processed.jammerDetected,
            jumpTier = processed.jumpTier,
            isAdaptiveJump = processed.isAdaptiveJump,
            lat = processed.optimizedPoint.lat,
            lng = processed.optimizedPoint.lng,
            accuracy = processed.currentAccuracy,
            maxAccuracy = processed.maxAccuracy,
            gpsTs = rawGpsTs,
            lastValidFixRt = locationProcessor.getLastValidFixRt(),
            speed = processed.filteredSpeed,
            battery = health.batteryLevel,
            temp = health.batteryTemp,
            currentMa = health.currentMa,
            isLocationPending = health.isLocationPending,
            locationPendingReason = health.locationPendingReason,
            isTamperDetected = processed.tamperDetected,
            isPowerTamper = health.isPowerTamper,
            tiltDegrees = snapshot.tiltDegrees,
            acousticDb = snapshot.acousticDb,
            baroAlt = snapshot.baroAlt,
            baroAltEma = locationProcessor.getBaroBaseline(),
            lux = snapshot.lux,
            isNear = snapshot.isNear,
            luxBaseline = locationProcessor.getLuxBaseline(),
            acousticFloorDb = locationProcessor.getAcousticFloorDb(),
            adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(),
            peakVibrationShock = snapshot.peakShock,
            isPowerSaveMode = health.isPowerSaveMode,
            standbyBucket = health.standbyBucket,
            netInterface = health.netInterface,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            snrSnapshot = hardwareSuite.averageSnr,
            vibeSnapshot = snapshot.vibration,
            isGpsHardwareLock = health.gpsHardwareLock,
            cpuLoad = health.cpuLoad,
            ioWait = health.ioWait,
            maxIoLatency = health.maxIoLatency,
            isSilentFailure = health.isSilentFailure,
            isMaliAnomaly = health.isMaliAnomaly,
            isUltraLongStationary = health.isUltraLongStationary,
            isBatteryLow = health.isBatteryLow,
            isBatteryCritical = health.isBatteryCritical,
            tamperNote = processed.suppressionNote,
            isSignalLoss = health.signalLoss,
            isGpsStalling = health.gpsStalled,
            isGpsGap = health.locationPendingReason == LocationPendingReason.GPS_GAP,
            localInternetLoss = health.localInternetLoss,
            isHardwareOnline = health.isHardwareOnline
        )

        val serviceContext = AlarmServiceContext(
            now = now,
            nowRt = nowRt,
            serviceStartTs = serviceStartWall,
            serviceStartRt = serviceStartRealtime,
            appStartTime = sessionManager.appStartTime,
            isTrackerMode = true,
            isRelayConnected = isSocketConnected,
            isTrackerConnected = true,
            isUiVisible = isUiVisible(),
            distToHomeAuthority = processed.distToHome,
            maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(),
            capabilities = capabilities,
            rolePrefix = "T_"
        )

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(telemetry, serviceContext)
        }
    }
}
