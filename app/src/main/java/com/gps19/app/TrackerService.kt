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
import java.util.*
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * July.26.04:
 * - Issue #595: Forensic Playback Hardening.
 * - Issue #589: Performance Audit. Fixed type mismatch in forensic log routing and 
 *   ensured exhaustive sensor event handling.
 * - Issue #545c: Service Reactive Migration. Refactored to collect from standardized 
 *   SharedFlow event streams.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
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

    private var lastA15PokeRt = 0L
    private val A15_POKE_INTERVAL_MS = 10_000L

    private var lastFgsUpdateRt = 0L
    private val FGS_TYPE_UPDATE_THROTTLE_MS = 10_000L

    override suspend fun onServiceInitialize() {
        notificationManager.setTrackerMode(true)
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())

        configManager.deviceId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
        configManager.viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
        configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = true
        
        refreshCapabilitiesInternal()

        if (capabilities.isA15Device && MbrainHardwareManager.isAvailable()) {
            val res = MbrainHardwareManager.initMbrain(timeProvider, configManager.deviceId, 0)
            logManager.logServiceEvent("HARDWARE: libmbrainSDK initialized (Result: $res)", important = true)
        }

        // Reactive Event Subscription
        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeSensorEvents()
        observeCommandEvents()

        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, true)
        
        // Ensure state is restored before starting high-frequency collectors
        val savedMaxAcc = repository.getDouble(MainRepository.MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong(MainRepository.LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, -1000.0)
        val trackerState = repository.loadTrackerState()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60.0)
        locationProcessor.loadState(savedMaxAcc, savedLastSitTs, savedBaseline, trackerState, homePoints, maxDist)

        val savedAlarms = repository.getLastAlarmsJson()
        alarmManager.restoreState(savedAlarms)

        historyManager.initialize(lifecycleScope)
        appSensorManager.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch { gpsManager.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch { gpsManager.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        settingsJob = lifecycleScope.launch {
            launch { repository.alertSettingsFlow.collectLatest { settings -> alarmManager.updateSettings(settings) } }
            launch { repository.homePointsFlow.collectLatest { points -> locationProcessor.setHomePoints(points.map { EngineGeoPoint(it.latitude, it.longitude) }) } }
            launch { repository.maxDistanceFlow.collectLatest { dist -> locationProcessor.setMaxDistanceAuthority(dist) } }
        }

        val recoveredTs = repository.getLong(MainRepository.LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(MainRepository.CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = if (recoveredDrift != 0L) recoveredTs - recoveredDrift else timeProvider.elapsedRealtime()
        locationProcessor.setLastValidFixRt(timeProvider.elapsedRealtime()) 
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()

        setupPhysicalFastPaths()
        startTickLoop()
        
        withContext(Dispatchers.Main) { updateForegroundServiceType() }
        logManager.logServiceEvent("Tracker Engine Online (Coordinated)", important = true)
    }

    private fun observeAlarmEvents() {
        lifecycleScope.launch {
            alarmManager.alarmEvents.collect { event ->
                when (event) {
                    is AlarmEvent.LogEvent -> {
                        logManager.submitToLogSink(
                            message = event.message,
                            type = event.type,
                            important = event.important,
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
        lifecycleScope.launch {
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
                        logManager.logServiceEvent(event.message, event.important, isSpecial = isSpecial, specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null)
                    }
                }
            }
        }
    }

    private fun observeProcessorEvents() {
        lifecycleScope.launch {
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
                            important = event.isImportant,
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
                        repository.saveDoubleSync(MainRepository.MAX_ACCURACY_KEY, event.accuracy)
                    }
                    is ProcessorEvent.ChairBaselineChanged -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent("Passive Zeroing: Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", event.baseline)}°",
                            lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                        lifecycleScope.launch { repository.saveDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, event.baseline) }
                    }
                    is ProcessorEvent.GpsStallDetected -> {
                        if (systemMonitor.gpsStallStartTs == 0L) systemMonitor.gpsStallStartTs = event.rt
                    }
                }
            }
        }
    }

    private fun observeConnectivityEvents() {
        lifecycleScope.launch {
            connectivitySuite.connectivityEvents.collect { event ->
                when (event) {
                    is ConnectivityEvent.PeerPulse -> handleViewerPulse(event.id)
                }
            }
        }
    }

    private fun observeHistoryEvents() {
        lifecycleScope.launch {
            historyManager.historyEvents.collect { event ->
                when (event) {
                    is HistoryEvent.LogEvent -> logManager.logServiceEvent(event.message, event.important)
                }
            }
        }
    }

    private fun observeSensorEvents() {
        lifecycleScope.launch {
            appSensorManager.sensorEvents.collect { event ->
                when (event) {
                    is AppSensorEvent.HardwareFailure -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent("CRITICAL: SENSOR_HARDWARE_FAILURE - ${event.reason}", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is AppSensorEvent.LogEvent -> {
                        logManager.logServiceEvent(event.message, event.important)
                    }
                }
            }
        }
    }

    private fun observeCommandEvents() {
        lifecycleScope.launch {
            commandRouter.commandEvents.collect { event ->
                when (event) {
                    is CommandEvent.ViewerPulse -> handleViewerPulse(event.id)
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.TransientDrop -> transientDropDetected.set(event.drop)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { lifecycleScope.launch(Dispatchers.Default) { refreshCapabilitiesInternal(); appSensorManager.start() } }
                    is CommandEvent.TriggerForensicTest -> {
                        lifecycleScope.launch {
                            val proc = lastProcessedLocation
                            logManager.logServiceEvent("FORENSIC TEST: Manually injecting Jammer/Stall markers", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                            systemMonitor.jumpStateStartTs = timeProvider.elapsedRealtime() - 31000L
                            systemMonitor.gpsStallStartTs = timeProvider.elapsedRealtime() - 61000L
                        }
                    }
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
            isManualOverrideActive = perms.isManualOverride,
            isA15Device = perms.isA15Device
        )
    }

    private fun setupPhysicalFastPaths() {
        appSensorManager.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent("Acoustic Spike Detected (FastPath)", false)
                lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime()
            }
        )
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) return
        repository.updateRemoteActivity(timeProvider.currentTimeMillis())

        if ((configManager.viewerId == MainRepository.DEFAULT_TRACKER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
            configManager.viewerId = id
            connectivitySuite.updateIdentity(configManager.deviceId, id, true)
            lifecycleScope.launch { repository.saveString(MainRepository.VIEWER_ID_KEY, id) } 
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
        lastHardwareRecoveryTs = 0L
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        logManager.logServiceEvent("Session Terminated", false)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification("Tracking system active."), type, force = true)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nowRt = timeProvider.elapsedRealtime()
            val startupWindowActive = nowRt - serviceStartRealtime < 10_000L
            if (startupWindowActive && lastFgsUpdateRt != 0L) return
            if (nowRt - lastFgsUpdateRt < FGS_TYPE_UPDATE_THROTTLE_MS) return

            lastFgsUpdateRt = nowRt
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
                delay(200)
                val type = getAvailableForegroundServiceType()
                val msg = if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0) "Acoustic monitoring active." else "Tracking system active."
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
            val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
        repository.updateHealth(health)

        appSensorManager.setHighLoad(health.isCoolingModeActive)
        if (capabilities.requiresWakeLockRenewal) systemMonitor.renewWakeLock()

        if (capabilities.isA15Device && nowRt - lastA15PokeRt > A15_POKE_INTERVAL_MS) {
            lastA15PokeRt = nowRt
            if (MbrainHardwareManager.isAvailable()) MbrainHardwareManager.punchHardware(timeProvider) else systemMonitor.acquireWakeLock(force = true)
            if (appSensorManager.isStationary()) Timber.d("Issue #113: A15 Hardware Poke - Refreshing Budget")
        }

        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            if (tickGap > HARDWARE_SUPPRESSION_THRESHOLD_MS && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                val proc = lastProcessedLocation
                logManager.logServiceEvent("HEURISTIC RECOVERY: Heartbeat gap detected (${tickGap}ms). Reviving connection.", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                systemMonitor.acquireWakeLock()
                connectivitySuite.connect(configManager.relayUrl)
            }
        }

        if (nowRt - lastStabilityAuditTs > GPS_STABILITY_AUDIT_INTERVAL_MS) {
            if (stabilityAuditFixCount > 0) {
                val reliability = 100.0 * (stabilityAuditFixCount - stabilityAuditViolationCount) / stabilityAuditFixCount
                if (reliability < GPS_STABILITY_RELIABILITY_THRESHOLD) {
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY AUDIT (T): Reliability ${String.format(Locale.getDefault(), "%.1f", reliability)}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes)", important = true, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                }
                stabilityAuditFixCount = 0; stabilityAuditViolationCount = 0
            }
            lastStabilityAuditTs = nowRt
        }
        
        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isViewerActive = sessionManager.getViewerCount() > 0 || isRecentUiPulse()
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isViewerActive, isInViolation = alarmManager.hasUnresolvedAlarms())

        val snapshot = appSensorManager.consumeForensicSnapshot()

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
        val sitDetected = locationProcessor.consumeSitDetected()

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(isStationary = appSensorManager.isStationary(), isGpsStalled = lastProcessedLocation?.isStalled ?: true, hasUnresolvedAlarms = alarmManager.hasUnresolvedAlarms(), isUiVisible = isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; appSensorManager.setPowerSaveMode(shouldBePowerSave); logManager.logServiceEvent("POWER SAVER: ${if (shouldBePowerSave) "ENGAGED" else "DISABLED"}", false)
                withContext(Dispatchers.Main) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        val hasLocation = lastKnownLocation != null
        if (isViewerActive && hasLocation) {
            val location = lastKnownLocation!!
            val processed = locationProcessor.processGpsPoint(
                lat = location.latitude, lng = location.longitude, alt = location.altitude, androidSpeedMps = lastGpsSpeed, gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing, snr = avgCn0, satsUsed = latestGnssDetail?.satellites?.count { it.usedInFix } ?: 0, isViewerTrail = false, lastGpsTs = lastGpsFixRealtime, isLocal = true, providedAcousticLockoutRt = lastFastPathAcousticSpikeTs, nowWall = now, nowRt = nowRt
            )
            lastProcessedLocation = processed
            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isViewerActive, processed, snapshot)
            
            connectivitySuite.pushCurrentStatus(
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = location, filtered = processed.optimizedPoint, distToTracker = null, distToHome = processed.distToHome, maxAccuracy = processed.maxAccuracy, filteredSpeed = processed.filteredSpeed, vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, jumpTier = processed.jumpTier, isJammer = processed.jammerDetected, isStalled = processed.isStalled, peakShock = snapshot.peakShock, peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx, proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs, vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false, isTamperDetected = processed.tamperDetected, isPowerTamper = health.isPowerTamper, isSitDetected = sitDetected, isSitActive = false, lastSitTs = locationProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isClockRegression = processed.isClockRegression, isLocationPending = false, locationPendingReason = LocationPendingReason.NONE, lastValidFixRt = locationProcessor.getLastValidFixRt(), gnssDetail = latestGnssDetail, snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp, isCharging = health.isCharging, trackerState = if (processed.filteredSpeed > 0.5) TrackerState.MOVING else TrackerState.PARKING, status = processed.status, isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical, isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface
            )
            
            historyManager.updateRibbons(
                now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = 10, peerAvail = isSocketConnected && isViewerActive, hasGps = true, isTrackerMode = true, accuracy = processed.currentAccuracy, maxAccuracy = processed.maxAccuracy, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = snrIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, speed = processed.filteredSpeed, bearing = location.bearing.toDouble(), isSitDetected = sitDetected, isSitActive = false, currentMa = health.currentMa, locationPendingReason = LocationPendingReason.NONE
            )
        } else {
            historyManager.updateRibbons(
                now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = 0, peerAvail = isSocketConnected && isViewerActive, hasGps = false, isTrackerMode = true, currentMa = health.currentMa
            )
        }

        if (isSystemActive && (now - lastNotificationUpdateTs >= NOTIFICATION_THROTTLE_MS)) {
            lastNotificationUpdateTs = now
            notificationManager.updatePulse(sats = gpsManager.satellitesUsed, battery = health.batteryLevel, isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = isPowerSaveActive || health.isPowerSaveMode)
        }

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        serviceTickCounter++
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
                    logManager.logServiceEvent("STABILITY GAP (T): ${gap}ms detected during logic pulse.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
                }
            }
        }
        lastGpsFixRealtime = nowRt; systemMonitor.gpsStallStartTs = 0L
        if (lastStabilityAuditTs == 0L) lastStabilityAuditTs = nowRt
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSocketConnected: Boolean, isViewerConnected: Boolean, processed: LocationProcessor.ProcessedLocation, snapshot: AppSensorManager.ForensicSnapshot) {
        val health = integrityMonitor.currentHealth
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = true, isRelayConnected = isSocketConnected, isTrackerConnected = true, status = processed.status, isJammer = processed.jammerDetected, jumpTier = processed.jumpTier, trackerLat = processed.optimizedPoint.lat, trackerLng = processed.optimizedPoint.lng, trackerAccuracy = processed.currentAccuracy, maxTrackerAccuracy = processed.maxAccuracy, trackerLastGpsTs = lastKnownLocation?.time ?: 0L, trackerLastGpsRt = lastGpsFixRealtime, trackerLastValidFixTs = 0L, trackerLastValidFixRt = locationProcessor.getLastValidFixRt(), trackerSpeed = processed.filteredSpeed, trackerBattery = health.batteryLevel, trackerTemp = health.batteryTemp, isHardwareOnline = health.isHardwareOnline, isLocalInternetLoss = health.localInternetLoss, isSignalLoss = health.signalLoss, isGpsStalling = processed.isStalled, isUiVisible = isUiVisible(), distToHomeAuthority = processed.distToHome, maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), isGpsGap = false, isTamperDetected = processed.tamperDetected, isPowerTamper = health.isPowerTamper, trackerTiltDegrees = snapshot.tiltDegrees, trackerAcousticDb = snapshot.acousticDb, trackerBaroAlt = snapshot.baroAlt, trackerBaroAltEma = locationProcessor.getBaroBaseline(), trackerLux = snapshot.lux, isNear = snapshot.isNear, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = snapshot.peakShock, trackerCurrentMa = health.currentMa, capabilities = capabilities
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel()
        super.onDestroy()
    }
}
