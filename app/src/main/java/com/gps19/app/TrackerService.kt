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
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * Sep.22.27:
 * - Issue #1186: Acoustic Fast-Path Baseline Dynamic Synchronization. Added periodic
 *   re-synchronization of acoustic fast-path baseline with LocationSentinel's contracting
 *   acoustic floor within processTick loop.
 * Sep.22.11:
 * - Issue #1183: Trigger-Based Forensic Sampling. Transitioned from a fixed-interval
 *   forensic loop to a "Signal-on-Spike" model triggered by hardware fast-paths,
 *   location updates, and periodic ticks.
 * Sep.22.07:
 * - Issue #1168: Vendor Adaptation Centralization. Injected DeviceProfileManager 
 *   to encapsulate and centralize all hardware/vendor-dependent behavioral overrides 
 *   and loop tweaks (R-ID 403).
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
    
    private val forensicTriggerChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)

    private fun triggerForensicSample() {
        forensicTriggerChannel.trySend(Unit)
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
    private var recoveryTriggerRt = 0L

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    override fun onServicePreInit() {
        notificationManager.setTrackerMode(true)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync(LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

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
        
        val savedMaxAcc = repository.getDouble(MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong(LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble(CHAIR_BASELINE_TILT_KEY, -1000.0)
        val trackerState = repository.loadTrackerState()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)
        locationProcessor.loadState(savedMaxAcc, savedLastSitTs, savedBaseline, trackerState, homePoints, maxDist)

        val savedAlarms = repository.getLastAlarmsJson()
        alarmManager.restoreState(savedAlarms)

        historyManager.initialize(lifecycleScope)
        
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

        val recoveredTs = repository.getLong(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = if (recoveredDrift != 0L) recoveredTs - recoveredDrift else timeProvider.elapsedRealtime()
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
                            ALERT_ID_TRACKER_POWER -> alarmManager.setPowerAlarmPending(true)
                            ALERT_ID_GPS_HARDWARE_LOCK, ALERT_ID_SILENT_FAILURE, 
                            ALERT_ID_PERFORMANCE_SPIKE, ALERT_ID_SYSTEM_STORAGE_LOW, 
                            ALERT_ID_SYSTEM_STORAGE_CRITICAL, ALERT_ID_BATTERY_STEEP_DISCHARGE -> {
                            }
                        }
                    }
                    is IntegrityEvent.ViolationResolved -> {
                        if (event.type == ALERT_ID_TRACKER_POWER) alarmManager.setPowerAlarmPending(false)
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
                        repository.saveDoubleSync(MAX_ACCURACY_KEY, event.accuracy)
                    }
                    is ProcessorEvent.ChairBaselineChanged -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent(m = "Passive Zeroing: Chair baseline calibrated to ${event.baseline.roundToOneDecimal()}°",
                            lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                        repository.saveDouble(CHAIR_BASELINE_TILT_KEY, event.baseline)
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
                triggerForensicSample()
            }
        )
        hardwareSuite.setLightFastPath(
            baseline = locationProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP,
            onSpike = {
                logManager.logServiceEvent(m = "Light Spike Detected (FastPath)", isImportant = false)
                lastFastPathLightSpikeTs = timeProvider.elapsedRealtime()
                triggerForensicSample()
            }
        )
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
                recoveryTriggerRt = 0L
                
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

        hardwareSuite.setLightFastPath(
            baseline = locationProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP,
            onSpike = {
                logManager.logServiceEvent(m = "Light Spike Detected (FastPath)", isImportant = false)
                lastFastPathLightSpikeTs = timeProvider.elapsedRealtime()
                triggerForensicSample()
            }
        )

        hardwareSuite.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent(m = "Acoustic Spike Detected (FastPath)", isImportant = false)
                lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime()
                triggerForensicSample()
            }
        )

        hardwareSuite.setHighLoad(health.isCoolingModeActive)
        
        isSuspiciousMode = serviceBehaviorUseCase.updateSuspiciousMode(
            currentSuspicious = isSuspiciousMode,
            isPhysicalViolation = locationProcessor.sentinel.checkPhysicalTamper(nowRt, false) == SentinelStatus.TAMPER,
            isSitDetected = locationProcessor.sentinel.consumeSitDetected(),
            nowRt = nowRt
        )
        
        val targetGpsInterval = serviceBehaviorUseCase.calculateGpsInterval(
            isCoolingMode = health.isCoolingModeActive,
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
            isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode,
            localInternetLoss = health.localInternetLoss,
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
        
        locationProcessor.updateSensorData(
            vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, powerTamper = health.isPowerTamper, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, peakShock = snapshot.peakShock, peakVerticalVelocity = snapshot.peakVerticalVelocity, peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs, peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt, plungeMatched = snapshot.plungeMatched, peakVerticalDisplacement = snapshot.peakVerticalDisplacement, nowRt = nowRt, nowWall = now,
            lightSpikeRt = lastFastPathLightSpikeTs,
            acousticLockoutRt = lastFastPathAcousticSpikeTs,
            providedAdaptiveFloor = snapshot.adaptiveVibrationFloor
        )

        val noiseIdx = (snapshot.acousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val luxIdx = log10(snapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE
        val vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G
        val liftIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
        
        val satellites = latestGnssDetail?.satellites ?: emptyList()
        val avgCn0 = satellites.map { it.cn0 }.safeAverage()
        val snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB
        
        val tiltIdx = abs(snapshot.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
        val baroIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val hasUnresolved = alarmManager.hasUnresolvedAlarms()
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(isStationary = hardwareSuite.isStationary(), isGpsStalled = health.gpsStalled, hasUnresolvedAlarms = hasUnresolved, isUiVisible = isUiVisible())
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
                providedIsStalled = health.gpsStalled,
                isSuspicious = isSuspiciousMode,
                providedAdaptiveVibrationFloor = snapshot.adaptiveVibrationFloor
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
            this.kinetic.kineticEnergy = snapshot.kineticEnergy
            this.kinetic.verticalVelocity = snapshot.peakVerticalVelocity

            this.atmospheric.temp = health.batteryTemp
            this.atmospheric.maxTemp = health.maxTemp
            this.atmospheric.vibration = snapshot.vibration
            this.atmospheric.heading = snapshot.heading
            this.atmospheric.baroAlt = snapshot.baroAlt
            this.atmospheric.lux = snapshot.lux
            this.atmospheric.isNear = snapshot.isNear
            this.atmospheric.tiltDegrees = snapshot.tiltDegrees
            this.atmospheric.acousticDb = snapshot.acousticDb
            this.atmospheric.peakVibrationShock = snapshot.peakShock
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

            this.integrity.battery = health.batteryLevel
            this.integrity.isCharging = health.isCharging
            this.integrity.currentMa = health.currentMa
            this.integrity.satsView = hardwareSuite.satellitesInView
            this.integrity.satsUsed = hardwareSuite.satellitesUsed
            this.integrity.snrIdx = snrIdx
            this.integrity.isPowerTamper = health.isPowerTamper
            this.integrity.isSitDetected = isSuspiciousMode
            this.integrity.lastSitTs = locationProcessor.getLastSitTs()
            this.integrity.sitVz = snapshot.peakVerticalVelocity
            this.integrity.sitVzTs = snapshot.peakVerticalVelocityTs
            this.integrity.sitVzRt = snapshot.peakVerticalVelocityRt
            this.integrity.sitDz = snapshot.peakVerticalDisplacement
            this.integrity.sitBaro = snapshot.peakVerticalDisplacement
            this.integrity.sitTilt = snapshot.tiltDegrees
            this.integrity.sitShock = snapshot.peakShock
            this.integrity.isBatteryLow = health.isBatteryLow
            this.integrity.isBatteryCritical = health.isBatteryCritical
            this.integrity.locationPendingReason = health.locationPendingReason
            this.integrity.isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode
            this.integrity.standbyBucket = health.standbyBucket
            this.integrity.netInterface = health.netInterface
            this.integrity.isStorageLow = health.isStorageLow
            this.integrity.isStorageCritical = health.isStorageCritical
            this.integrity.isBatterySteepDischarge = health.isBatterySteepDischarge
            this.integrity.isCoolingModeActive = health.isCoolingModeActive
            this.integrity.gpsHardwareLock = health.gpsHardwareLock
            this.integrity.isUltraLongStationary = health.isUltraLongStationary
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
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = lastLocInBatch, filtered = procViewer?.optimizedPoint, distToTracker = null, distToHome = procViewer?.distToHome, maxAccuracy = procViewer?.maxAccuracy ?: 0.0, filteredSpeed = procViewer?.filteredSpeed ?: 0.0, vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, jumpTier = procViewer?.jumpTier ?: 0, isJammer = procViewer?.jammerDetected ?: false, isStalled = health.gpsStalled, peakShock = snapshot.peakShock, peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx, proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs, vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false, isTamperDetected = procViewer?.tamperDetected ?: false, isPowerTamper = health.isPowerTamper, isSitDetected = isSuspiciousMode, isSitActive = false, lastSitTs = locationProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isClockRegression = procViewer?.isClockRegression ?: false, isLocationPending = health.isLocationPending, locationPendingReason = health.locationPendingReason, lastValidFixRt = health.lastValidFixRt, gnssDetail = latestGnssDetail, snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, batteryLevel = health.batteryLevel, temp = health.batteryTemp, isCharging = health.isCharging, trackerState = if ((procViewer?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, status = procViewer?.status ?: SentinelStatus.VALID, isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical, isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface, kineticEnergy = snapshot.kineticEnergy, isAdaptiveJump = procViewer?.isAdaptiveJump ?: false, isBatteryLow = health.isBatteryLow, isBatteryCritical = health.isBatteryCritical, isUltraLongStationary = health.isUltraLongStationary, gpsHardwareLock = health.gpsHardwareLock,
                tamperNote = procViewer?.suppressionNote
            )
        }

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = if (isViewerActive && lastLocInBatch != null) 10 else 0, peerAvail = isSocketConnected && isViewerActive, hasGps = lastLocInBatch != null, isTrackerMode = true, accuracy = lastProcessedLocation?.currentAccuracy ?: 0.0, maxAccuracy = lastProcessedLocation?.maxAccuracy ?: 0.0, noiseIdx = noiseIdx, luxIdx = log10(snapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE, vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB, tiltIdx = abs(snapshot.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG, baroIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS, verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, speed = lastProcessedLocation?.filteredSpeed ?: 0.0, bearing = lastLocInBatch?.bearing?.toDouble() ?: 0.0, isSitDetected = isSuspiciousMode, isSitActive = false, currentMa = health.currentMa, locationPendingReason = health.locationPendingReason, kineticEnergy = snapshot.kineticEnergy, isRecoveryEvent = recoveryFlagged, cpuLoad = health.cpuLoad, ioWait = health.ioWait, maxIoLatency = health.maxIoLatency, isSilentFailure = health.isSilentFailure, isBatteryLow = health.isBatteryLow, isBatteryCritical = health.isBatteryCritical, isUltraLongStationary = health.isUltraLongStationary
        )

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        serviceTickCounter++
        triggerForensicSample()
    }

    private fun startForensicSamplingLoop() {
        forensicSamplingJob?.cancel()
        forensicSamplingJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            delay(STARTUP_SETTLING_DELAY_MS)

            // Initial trigger to capture baseline state on service startup
            triggerForensicSample()

            for (unit in forensicTriggerChannel) {
                val health = integrityMonitor.currentHealth
                val proc = lastProcessedLocation
                val snapshot = hardwareSuite.consumeForensicSnapshot()
                
                val lat = proc?.optimizedPoint?.lat ?: 0.0
                val lng = proc?.optimizedPoint?.lng ?: 0.0
                val vibe = snapshot.vibration
                val tilt = snapshot.tiltDegrees

                if (lastWasCooling && !health.isCoolingModeActive) {
                    recoveryTriggerRt = timeProvider.elapsedRealtime()
                }

                val dist = if (lastForensicLat != 0.0) PhysicsUtils.calculateDistance(lastForensicLat, lastForensicLng, lat, lng) else Double.MAX_VALUE
                val vibeDelta = abs(vibe - lastForensicVibe)
                val tiltDelta = abs(tilt - lastForensicTilt)
                
                val shouldLog = dist > FORENSIC_SPATIAL_GATE_METERS || 
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
                
                val delayMs = when {
                    health.isCoolingModeActive -> FORENSIC_SAMPLING_INTERVAL_COOLING_MS
                    logManager.isForensicBufferUnderPressure() -> FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS
                    health.isCharging -> FORENSIC_SAMPLING_INTERVAL_MIN_MS
                    else -> FORENSIC_SAMPLING_INTERVAL_MAX_MS
                }

                if (recoveryTriggerRt > 0 && delayMs < FORENSIC_SAMPLING_INTERVAL_COOLING_MS) {
                    val latency = timeProvider.elapsedRealtime() - recoveryTriggerRt
                    logManager.logServiceEvent(m = "Forensic Performance Audit: Thermal Recovery Latency: ${latency}ms", isImportant = true)
                    recoveryTriggerRt = 0L
                }

                lastWasCooling = health.isCoolingModeActive
                delay(delayMs)
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

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        locationBuffer.add(location)
        
        forensicAuditor.recordGpsFix(nowRt, currentIntervalMs, "T")?.let { gapMsg ->
            val proc = lastProcessedLocation
            logManager.submitToLogSink(
                message = "STABILITY GAP (T): $gapMsg",
                type = "system",
                isImportant = true,
                isSpecial = true,
                specialColor = FORENSIC_PINK_COLOR,
                lat = proc?.optimizedPoint?.lat ?: 0.0,
                lng = proc?.optimizedPoint?.lng ?: 0.0,
                accuracy = location.accuracy.toDouble()
            )
        }
        triggerForensicSample()
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
            capabilities = capabilities
        )

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(telemetry, serviceContext)
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); revivalEventsJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel(); forensicSamplingJob?.cancel()
        deviceProfileManager.teardownHardwareProfile(capabilities)
        super.onDestroy()
    }
}