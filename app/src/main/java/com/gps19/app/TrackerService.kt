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
import java.util.*
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * Aug.21.09:
 * - Issue #265 Remediation: Migrated to JdHardwareManager.initialize() suspend 
 *   pattern to eliminate UI thread stalls during service bootstrap (R265).
 * - Issue #249/262 Remediation: Added JdHardwareManager.releaseHardware() to 
 *   onDestroy to ensure native global references and hardware handles are 
 *   disposed, preventing memory leaks and BaseEventQueue failures.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var forensicSamplingJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    private var lastGpsSpeed = 0.0
    private var lastGpsAccuracy = 0.0
    private var lastGpsBearing = 0.0
    
    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var lastGpsFixRealtime = 0L
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L
    
    private var lastFastPathAcousticSpikeTs = 0L

    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L
    
    private var isSuspiciousMode = false
    private var currentIntervalMs = TICK_INTERVAL_MS
    private var lastIntervalChangeRt = 0L

    private var lastA15PokeRt = 0L
    private val A15_POKE_INTERVAL_MS = 60_000L

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

        configManager.deviceId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        configManager.viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = true
        
        refreshCapabilitiesInternal()

        if (capabilities.isA15Device) {
             lifecycleScope.launch(Dispatchers.Default) {
                 val success = JdHardwareManager.initialize(timeProvider, configManager.deviceId)
                 if (success) {
                    logManager.logServiceEvent("HARDWARE: libjdHardware initialized successfully.", isImportant = true)
                 } else {
                    logManager.logServiceEvent("HARDWARE: libjdHardware initialization failed.", isImportant = true)
                 }
             }
        }

        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeSensorEvents()
        observeCommandEvents()

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
        appSensorManager.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { gpsManager.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { gpsManager.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        settingsJob = lifecycleScope.launch(Dispatchers.Default) {
            launch { repository.alertSettingsFlow.collectLatest { settings -> alarmManager.updateSettings(settings) } }
            launch { repository.homePointsFlow.collectLatest { points -> locationProcessor.setHomePoints(points.map { EngineGeoPoint(it.latitude, it.longitude) }) } }
            launch { repository.maxDistanceFlow.collectLatest { dist -> locationProcessor.setMaxDistanceAuthority(dist) } }
        }

        val recoveredTs = repository.getLong(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = if (recoveredDrift != 0L) recoveredTs - recoveredDrift else timeProvider.elapsedRealtime()
        locationProcessor.setLastValidFixRt(timeProvider.elapsedRealtime()) 
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()

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
                    is IntegrityEvent.ViolationSustained -> if (event.type == ALERT_ID_TRACKER_POWER) alarmManager.setPowerAlarmPending(true)
                    is IntegrityEvent.ViolationResolved -> if (event.type == ALERT_ID_TRACKER_POWER) alarmManager.setPowerAlarmPending(false)
                    is IntegrityEvent.LogEvent -> {
                        val isSpecial = event.message.contains("tamper", ignoreCase = true) || 
                                       event.message.contains("confirmed", ignoreCase = true) || 
                                       event.message.contains("EMERGENCY", ignoreCase = true) || 
                                       event.message.contains("PRIORITY", ignoreCase = true) || 
                                       event.message.contains("BUCKET", ignoreCase = true)
                        logManager.logServiceEvent(event.message, isImportant = event.isImportant, isSpecial = isSpecial, specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null)
                    }
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
                        logManager.logServiceEvent("Passive Zeroing: Chair baseline calibrated to ${event.baseline.roundToOneDecimal()}°",
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
                    is HistoryEvent.LogEvent -> logManager.logServiceEvent(event.message, isImportant = event.isImportant)
                }
            }
        }
    }

    private fun observeSensorEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            appSensorManager.sensorEvents.collect { event ->
                when (event) {
                    is AppSensorEvent.HardwareFailure -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent("CRITICAL: SENSOR_HARDWARE_FAILURE - ${event.reason}", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is AppSensorEvent.LogEvent -> {
                        logManager.logServiceEvent(event.message, isImportant = event.isImportant)
                    }
                }
            }
        }
    }

    private fun observeCommandEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            commandRouter.commandEvents.collect { event ->
                when (event) {
                    is CommandEvent.ViewerPulse -> handleViewerPulse(event.id)
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.TransientDrop -> transientDropDetected.set(event.drop)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { refreshCapabilitiesInternal(); appSensorManager.start() }
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
            requiresAdaptationMuzzle = perms.requiresAdaptationMuzzle
        )
    }

    private fun setupPhysicalFastPaths() {
        appSensorManager.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent("Acoustic Spike Detected (FastPath)", isImportant = false)
                lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime()
            }
        )
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) return
        repository.updateRemoteActivity(timeProvider.currentTimeMillis())

        if ((configManager.viewerId == SettingsRepository.DEFAULT_TRACKER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
            configManager.viewerId = id
            connectivitySuite.updateIdentity(configManager.deviceId, id, true)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(VIEWER_ID_KEY, id) } 
        }
        if (sessionManager.onViewerPulse(id, timeProvider.currentTimeMillis(), true)) { 
            logManager.logServiceEvent("Viewer connected: $id")
            startTickLoop() 
        }
    }

    private fun resetServiceTimers() { 
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()
        alarmManager.resetEvaluation()
        locationProcessor.resetStats()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        systemMonitor.resetSimulatedAnomalies()
        serviceBehaviorUseCase.reset()
        lastHardwareRecoveryTs = 0L
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        gpsManager.resetGnssJitter()
        logManager.logServiceEvent("Session Terminated", isImportant = false)
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
                    sats = gpsManager.satellitesUsed,
                    battery = health.batteryLevel,
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
            val isMicEnabled = appSensorManager.isAcousticMonitoringEnabled()
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
        val snapshot = appSensorManager.consumeLogicSnapshot()

        appSensorManager.setHighLoad(health.isCoolingModeActive)
        
        // Issue #141: Dynamic GPS Polling Adjustment (R406a)
        isSuspiciousMode = serviceBehaviorUseCase.updateSuspiciousMode(
            currentSuspicious = isSuspiciousMode,
            isPhysicalViolation = locationProcessor.sentinel.checkPhysicalTamper(nowRt, false) == SentinelStatus.TAMPER,
            isSitDetected = locationProcessor.sentinel.consumeSitDetected(),
            nowRt = nowRt
        )
        
        // Issue #169: Pass isGeofenceActive to polling logic.
        val targetGpsInterval = serviceBehaviorUseCase.calculateGpsInterval(
            isCoolingMode = health.isCoolingModeActive,
            isSuspiciousMode = isSuspiciousMode,
            isStationary = appSensorManager.isStationary(),
            isScreenOn = appSensorManager.isScreenOn(),
            isGeofenceActive = locationProcessor.getMaxDistanceAuthority() > 0.0,
            nowRt = nowRt,
            deviceSpecialFlags = ServiceBehaviorUseCase.DeviceSpecialFlags(
                isS21FE = capabilities.requiresAdaptationMuzzle,
                isXiaomi = capabilities.requiresExtraTopPadding
            )
        )
        
        if (targetGpsInterval != currentIntervalMs) {
            currentIntervalMs = targetGpsInterval
            lastIntervalChangeRt = nowRt
            gpsManager.setPollingInterval(targetGpsInterval)
        }
        
        val isAdaptationMuzzled = nowRt - lastIntervalChangeRt < ADAPTATION_SETTLING_MS

        if (capabilities.requiresWakeLockRenewal) systemMonitor.renewWakeLock()

        if (capabilities.isA15Device) {
            if (JdHardwareManager.isAvailable()) {
                val flags = if (health.isPowerSaveMode) 0x01 else 0x00
                JdHardwareManager.syncState(timeProvider, serviceTickCounter, flags)
            } else if (nowRt - lastA15PokeRt > A15_POKE_INTERVAL_MS) {
                lastA15PokeRt = nowRt
                systemMonitor.acquireWakeLock(force = true)
            }
        }

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            if (tickGap > HARDWARE_SUPPRESSION_THRESHOLD_MS && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                recoveryFlagged = true
                val proc = lastProcessedLocation
                logManager.logServiceEvent("HEURISTIC RECOVERY: Heartbeat gap detected (${tickGap}ms). Reviving connection.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                systemMonitor.acquireWakeLock()
                connectivitySuite.connect(configManager.relayUrl)
            }
        }

        if (nowRt - lastStabilityAuditTs > GPS_STABILITY_AUDIT_INTERVAL_MS) {
            val maxJitter = gpsManager.maxGnssJitterMs
            if (stabilityAuditFixCount > 0 || maxJitter > 0) {
                val reliability = if (stabilityAuditFixCount > 0) 100.0 * (stabilityAuditFixCount - stabilityAuditViolationCount) / stabilityAuditFixCount else 100.0
                val jitterViolation = maxJitter > GNSS_JITTER_THRESHOLD_MS
                val reliabilityViolation = reliability < GPS_STABILITY_RELIABILITY_THRESHOLD
                
                if (reliabilityViolation || jitterViolation) {
                    val proc = lastProcessedLocation
                    val msg = StringBuilder("STABILITY AUDIT (T): ")
                    if (reliabilityViolation) msg.append("Reliability ${reliability.roundToOneDecimal()}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes). ")
                    if (jitterViolation) msg.append("GNSS Jitter: ${maxJitter}ms (Hardware Instability).")
                    
                    logManager.logServiceEvent(msg.toString().trim(), isImportant = true, isSpecial = jitterViolation, specialColor = if (jitterViolation) FORENSIC_PINK_COLOR else null, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
                }
                stabilityAuditFixCount = 0; stabilityAuditViolationCount = 0
                gpsManager.resetGnssJitter()
            }
            lastStabilityAuditTs = nowRt
        }
        
        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isViewerActive = sessionManager.getViewerCount() > 0 || isRecentUiPulse()
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isViewerActive, isInViolation = alarmManager.hasUnresolvedAlarms())

        locationProcessor.updateSensorData(
            vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, powerTamper = health.isPowerTamper, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, peakShock = snapshot.peakShock, peakVerticalVelocity = snapshot.peakVerticalVelocity, peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs, peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt, plungeMatched = snapshot.plungeMatched, peakVerticalDisplacement = snapshot.peakVerticalDisplacement, nowRt = nowRt, nowWall = now
        )

        val noiseIdx = (snapshot.acousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val luxIdx = log10(snapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE
        val vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G
        val liftIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
        val avgCn0 = latestGnssDetail?.satellites?.map { it.cn0 }?.average() ?: 0.0
        val snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB
        val tiltIdx = abs(snapshot.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
        val baroIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val hasUnresolved = alarmManager.hasUnresolvedAlarms()
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(isStationary = appSensorManager.isStationary(), isGpsStalled = health.gpsStalled, hasUnresolvedAlarms = hasUnresolved, isUiVisible = isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; appSensorManager.setPowerSaveMode(shouldBePowerSave); logManager.logServiceEvent("POWER SAVER: ${if (shouldBePowerSave) "ENGAGED" else "DISABLED"}", isImportant = false)
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        val location = lastKnownLocation
        if (location != null) {
            val processed = locationProcessor.processGpsPoint(
                lat = location.latitude, lng = location.longitude, alt = location.altitude, androidSpeedMps = lastGpsSpeed, gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing, snr = avgCn0, satsUsed = latestGnssDetail?.satellites?.count { it.usedInFix } ?: 0, isViewerTrail = false, lastGpsTs = lastGpsFixRealtime, isLocal = true, providedAcousticLockoutRt = lastFastPathAcousticSpikeTs, nowWall = now, nowRt = nowRt,
                providedIsStalled = health.gpsStalled,
                isSuspicious = isSuspiciousMode,
                isAdaptationMuzzled = isAdaptationMuzzled
            )
            lastProcessedLocation = processed
            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isViewerActive, processed, snapshot)
        }

        if (isViewerActive) {
            val proc = lastProcessedLocation
            connectivitySuite.pushCurrentStatus(
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = location, filtered = proc?.optimizedPoint, distToTracker = null, distToHome = proc?.distToHome, maxAccuracy = proc?.maxAccuracy ?: 0.0, filteredSpeed = proc?.filteredSpeed ?: 0.0, vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, jumpTier = proc?.jumpTier ?: 0, isJammer = proc?.jammerDetected ?: false, isStalled = health.gpsStalled, peakShock = snapshot.peakShock, peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx, proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs, vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false, isTamperDetected = proc?.tamperDetected ?: false, isPowerTamper = health.isPowerTamper, isSitDetected = isSuspiciousMode, isSitActive = false, lastSitTs = locationProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isClockRegression = proc?.isClockRegression ?: false, isLocationPending = health.isLocationPending, locationPendingReason = health.locationPendingReason, lastValidFixRt = health.lastValidFixRt, gnssDetail = latestGnssDetail, snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, batteryLevel = health.batteryLevel, temp = health.batteryTemp, isCharging = health.isCharging, trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, status = proc?.status ?: SentinelStatus.VALID, isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical, isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface, kineticEnergy = snapshot.kineticEnergy, isAdaptiveJump = proc?.isAdaptiveJump ?: false, isBatteryLow = health.isBatteryLow, isBatteryCritical = health.isBatteryCritical
            )
        }

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = if (isViewerActive && location != null) 10 else 0, peerAvail = isSocketConnected && isViewerActive, hasGps = location != null, isTrackerMode = true, accuracy = lastProcessedLocation?.currentAccuracy ?: 0.0, maxAccuracy = lastProcessedLocation?.maxAccuracy ?: 0.0, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = snrIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, speed = lastProcessedLocation?.filteredSpeed ?: 0.0, bearing = location?.bearing?.toDouble() ?: 0.0, isSitDetected = isSuspiciousMode, isSitActive = false, currentMa = health.currentMa, locationPendingReason = health.locationPendingReason, kineticEnergy = snapshot.kineticEnergy, isRecoveryEvent = recoveryFlagged, cpuLoad = health.cpuLoad, ioWait = health.ioWait, maxIoLatency = health.maxIoLatency, isSilentFailure = health.isSilentFailure
        )

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, now)
        serviceTickCounter++
    }

    private fun startForensicSamplingLoop() {
        forensicSamplingJob?.cancel()
        forensicSamplingJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            delay(STARTUP_SETTLING_DELAY_MS)

            while (isActive) {
                val health = integrityMonitor.currentHealth
                val proc = lastProcessedLocation
                val snapshot = appSensorManager.consumeForensicSnapshot()
                
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
                    logManager.logServiceEvent("Forensic Performance Audit: Thermal Recovery Latency: ${latency}ms", isImportant = true)
                    recoveryTriggerRt = 0L
                }

                lastWasCooling = health.isCoolingModeActive
                delay(delayMs)
            }
        }
    }

    override suspend fun onHeartbeat(now: Long, nowRt: Long) {
        if (isSystemActive) {
            val health = integrityMonitor.currentHealth
            notificationManager.updatePulse(
                sats = gpsManager.satellitesUsed, 
                battery = health.batteryLevel, 
                isSecure = !alarmManager.hasUnresolvedAlarms(), 
                isPowerSave = isPowerSaveActive || health.isPowerSaveMode
            )
        }
    }

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        lastKnownLocation = location; lastGpsSpeed = location.speed.toDouble(); lastGpsAccuracy = location.accuracy.toDouble(); lastGpsBearing = location.bearing.toDouble()
        
        if (lastGpsFixRealtime > 0) {
            val gap = nowRt - lastGpsFixRealtime
            if (HIGH_FREQUENCY_GPS_POLLING_MS == TICK_INTERVAL_MS) {
                stabilityAuditFixCount++
                if (gap > TICK_INTERVAL_MS + GPS_STABILITY_GAP_THRESHOLD_MS) {
                    stabilityAuditViolationCount++
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY GAP (T): ${gap}ms detected during logic pulse.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
                }
            }
        }
        lastGpsFixRealtime = nowRt
        if (lastStabilityAuditTs == 0L) lastStabilityAuditTs = nowRt
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSocketConnected: Boolean, isViewerConnected: Boolean, processed: ProcessedLocation, snapshot: AppSensorManager.ForensicSnapshot) {
        val health = integrityMonitor.currentHealth
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = true, isRelayConnected = isSocketConnected, isTrackerConnected = true, status = processed.status, isJammer = processed.jammerDetected, jumpTier = processed.jumpTier, 
                isAdaptiveJump = processed.isAdaptiveJump,
                trackerLat = processed.optimizedPoint.lat, trackerLng = processed.optimizedPoint.lng, trackerAccuracy = processed.currentAccuracy, maxTrackerAccuracy = processed.maxAccuracy, trackerLastGpsTs = lastKnownLocation?.time ?: 0L, trackerLastGpsRt = lastGpsFixRealtime, trackerLastValidFixTs = 0L, trackerLastValidFixRt = locationProcessor.getLastValidFixRt(), trackerSpeed = processed.filteredSpeed, trackerBattery = health.batteryLevel, trackerTemp = health.batteryTemp, isHardwareOnline = health.isHardwareOnline, isLocalInternetLoss = health.localInternetLoss, isSignalLoss = health.signalLoss, isGpsStalling = health.gpsStalled, isUiVisible = isUiVisible(), distToHomeAuthority = processed.distToHome, maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), isGpsGap = health.locationPendingReason == LocationPendingReason.GPS_GAP, isTamperDetected = processed.tamperDetected, isPowerTamper = health.isPowerTamper, trackerTiltDegrees = snapshot.tiltDegrees, trackerAcousticDb = snapshot.acousticDb, trackerBaroAlt = snapshot.baroAlt, trackerBaroAltEma = locationProcessor.getBaroBaseline(), trackerLux = snapshot.lux, isNear = snapshot.isNear, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = snapshot.peakShock, trackerCurrentMa = health.currentMa, capabilities = capabilities
            )
        }
    }

    override fun onDestroy() {
        // Issue #249/262: Release native hardware resources and stop jobs.
        if (capabilities.isA15Device) {
            JdHardwareManager.releaseHardware(timeProvider)
        }
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel(); forensicSamplingJob?.cancel()
        super.onDestroy()
    }
}
