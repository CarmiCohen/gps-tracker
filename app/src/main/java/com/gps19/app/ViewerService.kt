package com.gps19.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import kotlin.math.*

/**
 * ViewerService: Background monitoring for the Viewer role.
 * Sep.06.31:
 * - Issue #926 RESOLVED: Revival Integration. Mapped isGpsHardwareLock 
 *   to AlarmManager evaluation (R928).
 * Sep.06.30:
 * - Issue #925 RESOLVED: Async Teardown Race Condition. Synchronized HardwareProvider 
 *   initialization by awaiting suspend start() (R925).
 */
@AndroidEntryPoint
class ViewerService : BaseMonitorService() {

    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    private var lastGpsSpeed = 0.0
    private var lastGpsAccuracy = 0.0
    private var lastGpsBearing = 0.0

    private var lastGpsFixRealtime = 0L
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L
    
    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L

    private lateinit var selfProcessor: LocationProcessor
    private lateinit var remoteProcessor: LocationProcessor

    override fun onServicePreInit() {
        notificationManager.setTrackerMode(false)
        selfProcessor = LocationProcessor(timeProvider)
        remoteProcessor = LocationProcessor(timeProvider)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync(LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        
        configManager.deviceId = trackerId
        configManager.viewerId = viewerId
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = false

        refreshCapabilitiesInternal()
        
        if (capabilities.isA15Device) {
            val success = JdHardwareManager.initialize(timeProvider, configManager.deviceId)
            if (success) {
                logManager.logServiceEvent("HARDWARE: libjdHardware initialized successfully.", isImportant = true)
                delay(500)
            } else {
                logManager.logServiceEvent("HARDWARE: libjdHardware initialization failed.", isImportant = true)
            }
        }

        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeCommandEvents()
        
        connectivitySuite.updateRemoteProcessor(remoteProcessor)
        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
        
        val savedMaxAcc = repository.getDouble(MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong(LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble(CHAIR_BASELINE_TILT_KEY, -1000.0)
        val trackerState = repository.loadTrackerState()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)
        
        remoteProcessor.loadState(
            savedMaxAccuracy = savedMaxAcc,
            savedLastSitTs = savedLastSitTs,
            savedBaseline = savedBaseline,
            trackerState = trackerState,
            homePoints = homePoints,
            maxDistance = maxDist,
            savedSitVz = trackerState?.sitVz ?: 0.0,
            savedSitDz = trackerState?.sitDz ?: 0.0,
            savedSitBaro = trackerState?.sitBaro ?: 0.0,
            savedSitTilt = trackerState?.sitTilt ?: 0.0,
            savedSitShock = trackerState?.sitShock ?: 0.0,
            savedSitVzTs = trackerState?.sitVzTs ?: 0L,
            savedSitVzRt = trackerState?.sitVzRt ?: 0L
        )
        
        selfProcessor.loadState(0.0, 0L, -1000.0, null, homePoints, maxDist)

        historyManager.initialize(lifecycleScope)
        
        // Issue #925: Synchronous wait for hardware availability
        hardwareProvider.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { hardwareProvider.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { hardwareProvider.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        settingsJob = lifecycleScope.launch(Dispatchers.Default) {
            launch { repository.alertSettingsFlow.collectLatest { settings -> alarmManager.updateSettings(settings) } }
            launch { 
                repository.homePointsFlow.collectLatest { points -> 
                    val enginePoints = points.map { EngineGeoPoint(it.latitude, it.longitude) }
                    remoteProcessor.setHomePoints(enginePoints)
                    selfProcessor.setHomePoints(enginePoints)
                } 
            }
            launch { 
                repository.maxDistanceFlow.collectLatest { dist -> 
                    remoteProcessor.setMaxDistanceAuthority(dist)
                    selfProcessor.setMaxDistanceAuthority(dist)
                } 
            }
        }

        val recoveredTs = repository.getLong(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = if (recoveredDrift != 0L) recoveredTs - recoveredDrift else timeProvider.elapsedRealtime()
        
        remoteProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        selfProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()

        startTickLoop()
        startHeartbeatLoop()
        logManager.logServiceEvent("Viewer Engine Online (Coordinated)", isImportant = true)
    }

    private fun observeAlarmEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.alarmEvents.collectLatest { event ->
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
            integrityMonitor.integrityEvents.collectLatest { event ->
                when (event) {
                    is IntegrityEvent.LogEvent -> logManager.logServiceEvent(event.message, isImportant = event.isImportant)
                    else -> {} 
                }
            }
        }
    }

    private fun observeProcessorEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            launch { selfProcessor.processorEvents.collectLatest { handleProcessorEvent(it, true) } }
            launch { remoteProcessor.processorEvents.collectLatest { handleProcessorEvent(it, false) } }
        }
    }
    
    private suspend fun handleProcessorEvent(event: ProcessorEvent, isSelf: Boolean) {
        when (event) {
            is ProcessorEvent.TrailPointSaved -> {
                repository.saveTrailPoint(
                    event.lat, event.lng, event.isViewerTrail, event.status, 
                    event.timestamp, accuracy = event.accuracy, maxAccuracy = event.maxAccuracy
                )
            }
            is ProcessorEvent.LogAdded -> {
                val prefix = if (isSelf) "[Self] " else ""
                val specialColor = if (event.isSpecial || event.message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
                logManager.logServiceEvent(
                    prefix + event.message, 
                    isImportant = event.isImportant, 
                    isSpecial = event.isSpecial || event.message.contains("Merge-on-Stale"), 
                    specialColor = specialColor, 
                    lat = event.lat, lng = event.lng, 
                    accuracy = event.accuracy, snr = event.snr, vibe = event.vibe
                )
            }
            is ProcessorEvent.MaxAccuracyChanged -> {
                if (!isSelf) repository.saveDoubleSync(MAX_ACCURACY_KEY, event.accuracy)
            }
            is ProcessorEvent.ChairBaselineChanged -> {
                val (lat, lng, maxAcc) = if (isSelf) {
                    val proc = lastProcessedLocation
                    Triple(proc?.optimizedPoint?.lat ?: 0.0, proc?.optimizedPoint?.lng ?: 0.0, proc?.maxAccuracy ?: 0.0)
                } else {
                    val status = connectivitySuite.trackerStatus
                    Triple(status.lat, status.lng, status.maxAccuracy)
                }
                
                logManager.logServiceEvent("Passive Zeroing - Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", event.baseline)}°",
                    lat = lat, lng = lng, accuracy = maxAcc)
            }
            is ProcessorEvent.GpsStallDetected -> {
                if (isSelf) logManager.logServiceEvent("GPS STALL: Fix unchanged for >1s", isImportant = false)
            }
        }
    }

    private fun observeConnectivityEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            connectivitySuite.connectivityEvents.collectLatest { event ->
                when (event) {
                    is ConnectivityEvent.PeerPulse -> handleTrackerPulse(event.id)
                }
            }
        }
    }

    private fun observeHistoryEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            historyManager.historyEvents.collectLatest { event ->
                when (event) {
                    is HistoryEvent.LogEvent -> logManager.logServiceEvent(event.message, isImportant = event.isImportant)
                }
            }
        }
    }

    private fun observeCommandEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            commandRouter.commandEvents.collectLatest { event ->
                when (event) {
                    is CommandEvent.ViewerPulse -> handleTrackerPulse(event.id)
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.TransientDrop -> transientDropDetected.set(event.drop)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { 
                        refreshCapabilitiesInternal()
                        // Issue #925: Ensure synchronous restart after hardware sync command
                        launch { hardwareProvider.start() }
                    }
                    else -> {}
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

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val lat = location.latitude; val lng = location.longitude; val alt = location.altitude
        
        lastGpsSpeed = location.speed.toDouble(); lastGpsAccuracy = location.accuracy.toDouble(); lastGpsBearing = location.bearing.toDouble()

        if (lastGpsFixRealtime > 0) {
            val gap = nowRt - lastGpsFixRealtime
            if (gap > TICK_INTERVAL_MS + GPS_STABILITY_GAP_THRESHOLD_MS) {
                val proc = lastProcessedLocation
                logManager.logServiceEvent("STABILITY GAP (V): ${gap}ms detected during logic pulse.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
            }
        }
        lastGpsFixRealtime = nowRt

        val processed = selfProcessor.processGpsPoint(
            lat = lat, lng = lng, alt = alt, androidSpeedMps = lastGpsSpeed, 
            gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing, 
            snr = hardwareProvider.averageSnr, satsUsed = location.extras?.getInt("satellites") ?: hardwareProvider.satellitesUsed, isViewerTrail = true, lastGpsTs = sessionManager.lastGpsTs, isLocal = true, 
            nowRt = nowRt, nowWall = nowWall
        )

        if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
        
        lastKnownLocation = location; lastProcessedLocation = processed

        val health = integrityMonitor.currentHealth
        repository.updateLocation(LocationUpdate(
            lat = lat, lng = lng, alt = alt, speed = lastGpsSpeed, accuracy = lastGpsAccuracy, 
            bearing = lastGpsBearing, battery = health.batteryLevel, temp = health.batteryTemp, 
            maxTemp = health.maxTemp, isCharging = health.isCharging, gpsTs = location.time, ts = nowWall, 
            isMe = true, satsView = hardwareProvider.satellitesInView, satsUsed = location.extras?.getInt("satellites") ?: hardwareProvider.satellitesUsed, maxAccuracy = processed.maxAccuracy, currentMa = health.currentMa, 
            lastValidFixRt = selfProcessor.getLastValidFixRt(), status = processed.status, snrIdx = (hardwareProvider.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
        ))
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) return
        val nowRt = timeProvider.elapsedRealtime()
        if ((configManager.deviceId == SettingsRepository.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id; connectivitySuite.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(TRACKER_ID_KEY, id) }
        }
        if (sessionManager.onTrackerPulse(id, nowRt)) {
            val proc = lastProcessedLocation
            logManager.logServiceEvent("Device connected: $id", lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        val proc = lastProcessedLocation
        serviceStartRealtime = timeProvider.elapsedRealtime(); serviceStartWall = timeProvider.currentTimeMillis()
        alarmManager.resetEvaluation(); sessionManager.reset(); integrityMonitor.resetStats(); forensicUseCase.resetLatches(); stabilityAuditFixCount = 0; stabilityAuditViolationCount = 0
        hardwareProvider.resetGnssJitter()
        lastHardwareRecoveryTs = 0L
        
        selfProcessor.resetStats()
        remoteProcessor.resetStats()
        
        logManager.logServiceEvent("Session Terminated", isImportant = false, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible); updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
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
                try {
                    delay(200)
                    val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    val health = integrityMonitor.currentHealth
                    val msg = notificationManager.getPulseMessage(
                        hardwareProvider.satellitesUsed,
                        health.batteryLevel,
                        !alarmManager.hasUnresolvedAlarms(),
                        isPowerSaveActive || health.isPowerSaveMode
                    )
                    safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
                } catch (e: Exception) { if (e !is CancellationException) Timber.e(e, "Failed to update FGS type") }
            }
        }
    }
    
    override fun getRequiredTickInterval(): Long { return TICK_INTERVAL_MS }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt); integrityMonitor.checkInternetIntegrity(nowRt)
        val health = integrityMonitor.currentHealth; val snapshot = hardwareProvider.consumeLogicSnapshot()
        if (capabilities.requiresWakeLockRenewal) systemMonitor.renewWakeLock()

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val hasUnresolved = alarmManager.hasUnresolvedAlarms()
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(hardwareProvider.isStationary(), health.gpsStalled, hasUnresolved, isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; hardwareProvider.setPowerSaveMode(shouldBePowerSave)
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        val location = lastKnownLocation
        if (location != null) {
            selfProcessor.processGpsPoint(location.latitude, location.longitude, location.altitude, location.speed.toDouble(), location.time, lastGpsAccuracy, location.bearing.toDouble(), 0.0, 0, true, 0L, true, nowRt = nowRt, nowWall = now)
        }

        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = connectivitySuite.lastPeerActivityTs > 0 && (nowRt - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isSocketConnected && isTrackerActive, false)

        val silenceDelta = if (connectivitySuite.lastPeerActivityTs > 0) nowRt - connectivitySuite.lastPeerActivityTs else (nowRt - serviceStartRealtime)
        val isSignalLoss = !integrityMonitor.checkSignalIntegrity(nowRt, silenceDelta, false)
        val isTrackerStalled = connectivitySuite.trackerGpsStallStartTs > 0L && (nowRt - connectivitySuite.trackerGpsStallStartTs > GPS_STALL_THRESHOLD_MS)
        val isTrackerGap = connectivitySuite.trackerLastValidFixRt > 0L && (nowRt - connectivitySuite.trackerLastValidFixRt > GPS_GAP_THRESHOLD_MS)

        val status = connectivitySuite.trackerStatus
        val home = repository.getCachedHomePoints().firstOrNull()
        val distToHome = if (status.lat != 0.0 && home != null) PhysicsUtils.calculateDistance(status.lat, status.lng, home.latitude, home.longitude) else null

        evaluateAlarmsInternal(now, nowRt, isSignalLoss, connectivitySuite.isTrackerJammerSuspicion, isTrackerStalled, isTrackerGap, isTrackerActive)

        val noiseIdx = (snapshot.acousticDb - selfProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val liftIdx = (snapshot.baroAlt - selfProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = 10, peerAvail = isSocketConnected && isTrackerActive, hasGps = (lastProcessedLocation?.timestamp ?: 0L) > 0, isTrackerMode = false, accuracy = lastGpsAccuracy, maxAccuracy = selfProcessor.getMaxTrackerAccuracy(), noiseIdx = noiseIdx, luxIdx = log10(snapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE, vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = (hardwareProvider.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0), tiltIdx = abs(snapshot.tiltDegrees - selfProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG, baroIdx = (snapshot.baroAlt - selfProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS, verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, speed = lastProcessedLocation?.filteredSpeed ?: 0.0, bearing = lastGpsBearing, isSitDetected = false, isSitActive = false, currentMa = health.currentMa, locationPendingReason = health.locationPendingReason, kineticEnergy = snapshot.kineticEnergy, isRecoveryEvent = false
        )

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        serviceTickCounter++
    }

    override suspend fun onHeartbeat(now: Long, nowRt: Long) {
        if (isSystemActive) {
            val health = integrityMonitor.currentHealth
            notificationManager.updatePulse(
                sats = hardwareProvider.satellitesUsed, 
                battery = health.batteryLevel, 
                isSecure = !alarmManager.hasUnresolvedAlarms(), 
                isPowerSave = isPowerSaveActive || health.isPowerSaveMode
            )
        }
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSignalLoss: Boolean, isTrackerJammerSuspicion: Boolean, isTrackerStalled: Boolean, isTrackerGap: Boolean, isTrackerActive: Boolean) {
        val isSocketConnected = connectivitySuite.isConnected(); val localHealth = integrityMonitor.currentHealth
        val status = connectivitySuite.trackerStatus
        val home = repository.getCachedHomePoints().firstOrNull()
        val distToHome = if (status.lat != 0.0 && home != null) PhysicsUtils.calculateDistance(status.lat, status.lng, home.latitude, home.longitude) else null

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = false, isRelayConnected = isSocketConnected, isTrackerConnected = isTrackerActive, status = status.status, isJammer = isTrackerJammerSuspicion, jumpTier = status.jumpTier,
                isAdaptiveJump = status.isAdaptiveJump,
                trackerLat = status.lat, trackerLng = status.lng, trackerAccuracy = status.accuracy, maxTrackerAccuracy = status.maxAccuracy, trackerLastGpsTs = status.gpsTs, trackerLastGpsRt = 0L, trackerLastValidFixTs = 0L, trackerLastValidFixRt = status.lastValidFixRt, trackerSpeed = status.speed, trackerBattery = status.battery, trackerTemp = status.temp, isHardwareOnline = localHealth.isHardwareOnline, isLocalInternetLoss = localHealth.localInternetLoss, isSignalLoss = isSignalLoss, isGpsStalling = isTrackerStalled, isUiVisible = isUiVisible(), distToHomeAuthority = distToHome, maxDistanceAuthority = remoteProcessor.getMaxDistanceAuthority(), isGpsGap = distToHome != null && isTrackerGap, isTamperDetected = status.isTamperDetected, isPowerTamper = status.isPowerTamper, trackerTiltDegrees = status.tiltDegrees, trackerAcousticDb = status.acousticDb, trackerBaroAlt = status.baroAlt, trackerBaroAltEma = status.sitBaro, trackerLux = status.lux, isNear = status.isNear, luxBaseline = status.luxBaseline, acousticFloorDb = status.acousticFloorDb, adaptiveVibrationFloor = status.adaptiveVibrationFloor, peakVibrationShock = status.peakVibrationShock, trackerCurrentMa = status.currentMa, isPowerSaveMode = status.isPowerSaveMode, standbyBucket = status.standbyBucket, netInterface = status.netInterface, isStorageLow = status.isStorageLow, isStorageCritical = status.isStorageCritical, isBatterySteepDischarge = status.isBatterySteepDischarge, isCoolingModeActive = status.isCoolingModeActive, capabilities = capabilities, isLocationPending = status.isLocationPending, locationPendingReason = status.locationPendingReason, snrSnapshot = hardwareProvider.averageSnr, vibeSnapshot = 0.0, isGpsHardwareLock = status.gpsHardwareLock
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel()
        // base.onDestroy() now centralizes hardware unregistration and native release.
        super.onDestroy()
    }
}
