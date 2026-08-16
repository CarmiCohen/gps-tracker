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
 * Aug.16.14:
 * - Issue #186 Hardening: Implemented Gated Sensor Start. Deferred sensor 
 *   registration via appSensorManager.start(deferred = true) to stabilize 
 *   startup IPC load (R186).
 * Aug.14.06:
 * - Issue #172: Viewer-Side State Audit. Finalized forensic parity by restoring 
 *   Vz timestamps (sitVzTs, sitVzRt) during initialization (R172).
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

    // Issue #173: Separate processors for remote and self streams
    private lateinit var selfProcessor: LocationProcessor
    private lateinit var remoteProcessor: LocationProcessor

    override fun onServicePreInit() {
        notificationManager.setTrackerMode(false)
        // Manual instantiation to ensure clean state separation (R173)
        selfProcessor = LocationProcessor(timeProvider)
        remoteProcessor = LocationProcessor(timeProvider)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        
        configManager.deviceId = trackerId
        configManager.viewerId = viewerId
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = false

        refreshCapabilitiesInternal()

        // Reactive Event Subscription
        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeCommandEvents()
        
        // Connectivity suite needs to use the remote processor for telemetry
        connectivitySuite.updateRemoteProcessor(remoteProcessor)
        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
        
        val savedMaxAcc = repository.getDouble(MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong(LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble(CHAIR_BASELINE_TILT_KEY, -1000.0)
        val trackerState = repository.loadTrackerState()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)
        
        // Issue #172: Restore full forensic mirror state
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
        
        // Self processor initializes with zero state (pure local filtering)
        selfProcessor.loadState(0.0, 0L, -1000.0, null, homePoints, maxDist)

        historyManager.initialize(lifecycleScope)

        // Issue #186: Deferred sensor start to stabilize startup IPC load.
        appSensorManager.start(deferred = true)

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        // Issue #138: Explicit Default dispatcher to prevent main-thread congestion
        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { gpsManager.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { gpsManager.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

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
                        logManager.submitToLogSink(event.message, event.type, isImportant = event.isImportant, extremeValue = event.extremeValue, localId = event.logId, durationMs = event.durationMs, isSpecial = event.isSpecial, specialColor = event.specialColor, lat = event.lat, lng = event.lng, accuracy = event.accuracy, maxAccuracy = event.maxAccuracy, snr = event.snr, vibe = event.vibe)
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
        // Collect from both processors to ensure full transparency
        lifecycleScope.launch(Dispatchers.Default) {
            val selfEvents = selfProcessor.processorEvents
            val remoteEvents = remoteProcessor.processorEvents
            
            launch {
                selfEvents.collectLatest { event -> handleProcessorEvent(event, true) }
            }
            launch {
                remoteEvents.collectLatest { event -> handleProcessorEvent(event, false) }
            }
        }
    }
    
    private suspend fun handleProcessorEvent(event: ProcessorEvent, isSelf: Boolean) {
        when (event) {
            is ProcessorEvent.TrailPointSaved -> {
                repository.saveTrailPoint(event.lat, event.lng, event.isViewerTrail, event.status, event.timestamp, accuracy = event.accuracy, maxAccuracy = event.maxAccuracy)
            }
            is ProcessorEvent.LogAdded -> {
                val prefix = if (isSelf) "[Self] " else ""
                val specialColor = if (event.isSpecial || event.message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
                logManager.logServiceEvent(prefix + event.message, isImportant = event.isImportant, isSpecial = event.isSpecial || event.message.contains("Merge-on-Stale"), specialColor = specialColor, lat = event.lat, lng = event.lng, accuracy = event.accuracy, snr = event.snr, vibe = event.vibe)
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
            else -> {}
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
                    is CommandEvent.SyncSensors -> { refreshCapabilitiesInternal(); appSensorManager.start(deferred = true) }
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
            isManualOverrideActive = perms.isManualOverride,
            isA15Device = perms.isA15Device
        )
    }

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val lat = location.latitude; val lng = location.longitude; val alt = location.altitude
        
        lastGpsSpeed = location.speed.toDouble(); lastGpsAccuracy = location.accuracy.toDouble(); lastGpsBearing = location.bearing.toDouble()

        if (lastGpsFixRealtime > 0) {
            val gap = nowRt - lastGpsFixRealtime
            if (HIGH_FREQUENCY_GPS_POLLING_MS == TICK_INTERVAL_MS) {
                stabilityAuditFixCount++
                if (gap > TICK_INTERVAL_MS + GPS_STABILITY_GAP_THRESHOLD_MS) {
                    stabilityAuditViolationCount++
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY GAP (V): ${gap}ms detected during logic pulse.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
                }
            }
        }
        lastGpsFixRealtime = nowRt

        // Issue #173: Use selfProcessor for local fixes
        val processed = selfProcessor.processGpsPoint(
            lat = lat, lng = lng, alt = alt, androidSpeedMps = lastGpsSpeed, gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing, snr = gpsManager.averageSnr, satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed, isViewerTrail = true, lastGpsTs = sessionManager.lastGpsTs, isLocal = true, nowRt = nowRt, nowWall = nowWall
        )

        if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
        
        lastKnownLocation = location; lastProcessedLocation = processed

        val health = integrityMonitor.currentHealth
        repository.updateLocation(LocationUpdate(
            lat = lat, lng = lng, alt = alt, speed = lastGpsSpeed, accuracy = lastGpsAccuracy, bearing = lastGpsBearing, battery = health.batteryLevel, temp = health.batteryTemp, maxTemp = health.maxTemp, isCharging = health.isCharging, gpsTs = location.time, ts = nowWall, isMe = true, satsView = gpsManager.satellitesInView, satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed, maxAccuracy = processed.maxAccuracy, currentMa = health.currentMa, lastValidFixRt = selfProcessor.getLastValidFixRt(), status = processed.status, snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
        ))

        val trackerAnchor = object : SpatialAnchor {
            override val lat = connectivitySuite.trackerLat; override val lng = connectivitySuite.trackerLng; override val alt = connectivitySuite.trackerBaroAlt; override val gpsTs = connectivitySuite.trackerLastGpsTs; override val ts = 0L; override val rt = connectivitySuite.trackerLastValidFixRt
        }
        // Distance calculations use separate processor states (R173)
        selfProcessor.updateCalculatedDistances(lat, lng, true, trackerAnchor)
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) return
        if ((configManager.deviceId == SettingsRepository.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id; connectivitySuite.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(VIEWER_ID_KEY, id) }
        }
        if (sessionManager.onTrackerPulse(id, timeProvider.currentTimeMillis(), false)) {
            val proc = lastProcessedLocation
            logManager.logServiceEvent("Device connected: $id", lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        val proc = lastProcessedLocation
        serviceStartRealtime = timeProvider.elapsedRealtime(); serviceStartWall = timeProvider.currentTimeMillis()
        alarmManager.resetEvaluation(); sessionManager.reset(); integrityMonitor.resetStats(); forensicUseCase.resetLatches(); stabilityAuditFixCount = 0; stabilityAuditViolationCount = 0
        gpsManager.resetGnssJitter()
        lastHardwareRecoveryTs = 0L
        
        selfProcessor.resetStats()
        remoteProcessor.resetStats()
        
        logManager.logServiceEvent("Session Terminated", isImportant = false, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible); updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    @SuppressLint("InlinedApi")
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

    @SuppressLint("InlinedApi")
    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
                try {
                    delay(200)
                    val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    val health = integrityMonitor.currentHealth
                    val msg = notificationManager.getPulseMessage(
                        sats = gpsManager.satellitesUsed,
                        battery = health.batteryLevel,
                        isSecure = !alarmManager.hasUnresolvedAlarms(),
                        isPowerSave = health.isPowerSaveMode
                    )
                    safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
                } catch (e: Exception) { if (e !is CancellationException) Timber.e(e, "Failed to update FGS type") }
            }
        }
    }
    
    override fun getRequiredTickInterval(): Long { return TICK_INTERVAL_MS }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt)
        val health = integrityMonitor.currentHealth

        if (timeProvider.elapsedRealtime() > 0) systemMonitor.renewWakeLock()

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            if (tickGap > HARDWARE_SUPPRESSION_THRESHOLD_MS && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                recoveryFlagged = true
                val proc = lastProcessedLocation
                logManager.logServiceEvent("HEURISTIC RECOVERY (V): Heartbeat gap detected (${tickGap}ms). Reviving connection.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
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
                    val msg = StringBuilder("STABILITY AUDIT (V): ")
                    if (reliabilityViolation) msg.append("Reliability ${String.format(Locale.getDefault(), "%.1f", reliability)}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes). ")
                    if (jitterViolation) msg.append("GNSS Jitter: ${maxJitter}ms (Hardware Instability).")
                    
                    logManager.logServiceEvent(msg.toString().trim(), isImportant = true, isSpecial = jitterViolation, specialColor = if (jitterViolation) FORENSIC_PINK_COLOR else null, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                }
                stabilityAuditFixCount = 0; stabilityAuditViolationCount = 0
                gpsManager.resetGnssJitter()
            }
            lastStabilityAuditTs = nowRt
        }

        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = connectivitySuite.lastPeerActivityTs > 0 && (nowRt - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isTrackerActive, isInViolation = false)

        val silenceDelta = if (connectivitySuite.lastPeerActivityTs > 0) nowRt - connectivitySuite.lastPeerActivityTs else 0L
        val isSignalLoss = !integrityMonitor.checkSignalIntegrity(nowRt, silenceDelta, false)
        val isTrackerJammerSuspicion = connectivitySuite.isTrackerJammerSuspicion
        val isTrackerStalled = connectivitySuite.trackerGpsStallStartTs > 0L && (nowRt - connectivitySuite.trackerGpsStallStartTs > GPS_STALL_THRESHOLD_MS)
        val isTrackerGap = connectivitySuite.trackerLastValidFixRt > 0L && (nowRt - connectivitySuite.trackerLastValidFixRt > GPS_GAP_THRESHOLD_MS)

        if (connectivitySuite.trackerLat != 0.0 && connectivitySuite.trackerLng != 0.0) {
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes(); val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isTrackerJammerSuspicion) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isTrackerStalled) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isTrackerGap) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (connectivitySuite.isTrackerVisualJump) activeViolations.add(ALERT_ID_VISUAL_JUMP)

            forensicUseCase.recordViolationMarkers(
                now = now, lat = connectivitySuite.trackerLat, lng = connectivitySuite.trackerLng, accuracy = connectivitySuite.trackerAccuracy, maxTrackerAccuracy = connectivitySuite.trackerMaxAccuracy, activeViolations = activeViolations, unresolvedAlarms = unresolvedAlarms
            )
        }

        val distToTracker = selfProcessor.getDistanceToTracker() ?: 0.0; val distToHome = selfProcessor.getNearestHomeDistance() ?: 0.0; val proc = lastProcessedLocation

        connectivitySuite.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = false, loc = lastKnownLocation, filtered = proc?.optimizedPoint, distToTracker = distToTracker, distToHome = distToHome, maxAccuracy = proc?.maxAccuracy ?: remoteProcessor.getMaxTrackerAccuracy(), filteredSpeed = proc?.filteredSpeed ?: 0.0, vibration = 0.0, heading = 0.0, baroAlt = 0.0, lux = 0.0, isNear = true, tiltDegrees = 0.0, acousticDb = 0.0, jumpTier = 0, isJammer = false, isStalled = false, peakShock = 0.0, peakShockTs = 0L, luxBaseline = 0.0, acousticFloorDb = 0.0, adaptiveVibrationFloor = 0.12, proxIdx = 1.0, proximityCm = -1.0, proximityDebounceMs = 0L, vibrationRollingSum = 0.0, micPending = false, isTamperDetected = false, isPowerTamper = health.isPowerTamper, isSitDetected = false, isSitActive = false, lastSitTs = 0L, receiptRt = proc?.rt ?: 0L, violationUptimeMs = 0L, violationPercentage = 0.0, verticalVelocity = 0.0, sitVz = 0.0, sitVzTs = 0L, sitVzRt = 0L, sitDz = 0.0, sitBaro = 0.0, sitTilt = 0.0, sitShock = 0.0, isClockRegression = proc?.isClockRegression ?: false, isLocationPending = false, locationPendingReason = LocationPendingReason.NONE, lastValidFixRt = selfProcessor.getLastValidFixRt(), gnssDetail = latestGnssDetail, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, batteryLevel = health.batteryLevel, temp = health.batteryTemp, isCharging = health.isCharging, status = proc?.status ?: SentinelStatus.VALID, isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical, isPowerSaveMode = health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface
        )

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = 10, peerAvail = isSocketConnected && isTrackerActive, hasGps = (proc?.timestamp ?: 0L) > 0, isTrackerMode = false, accuracy = proc?.currentAccuracy ?: selfProcessor.getLastProcessedAccuracy(), maxAccuracy = proc?.maxAccuracy ?: remoteProcessor.getMaxTrackerAccuracy(), noiseIdx = (proc?.maxAccuracy ?: 0.0).coerceIn(0.0, 1.0), luxIdx = 0.0, vibeIdx = 0.0, proxIdx = 1.0, liftIdx = 0.0, snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0), tiltIdx = 0.0, baroIdx = 0.0, verticalVelocity = 0.0, sitVz = 0.0, sitVzTs = 0L, sitVzRt = 0L, sitDz = 0.0, sitBaro = 0.0, sitTilt = 0.0, sitShock = 0.0, isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive, speed = proc?.filteredSpeed ?: 0.0, bearing = lastGpsBearing, isSitDetected = false, isSitActive = false, currentMa = health.currentMa, locationPendingReason = health.locationPendingReason, kineticEnergy = 0.0, isRecoveryEvent = recoveryFlagged
        )

        evaluateAlarmsInternal(now, nowRt, isSignalLoss, isTrackerJammerSuspicion, isTrackerStalled, isTrackerGap, isTrackerActive)

        repository.saveLongSync(LAST_SERVICE_TICK_TS_KEY, now)
        lastServiceTickTs = now; lastServiceTickRealtime = nowRt; serviceTickCounter++
    }

    override suspend fun onHeartbeat(now: Long, nowRt: Long) {
        if (isSystemActive) {
            val health = integrityMonitor.currentHealth
            notificationManager.updatePulse(
                sats = gpsManager.satellitesUsed, 
                battery = health.batteryLevel, 
                isSecure = !alarmManager.hasUnresolvedAlarms(), 
                isPowerSave = health.isPowerSaveMode
            )
        }
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSignalLoss: Boolean, isTrackerJammerSuspicion: Boolean, isTrackerStalled: Boolean, isTrackerGap: Boolean, isTrackerActive: Boolean) {
        val isSocketConnected = connectivitySuite.isConnected(); val localHealth = integrityMonitor.currentHealth
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = false, isRelayConnected = isSocketConnected, isTrackerConnected = isTrackerActive, status = connectivitySuite.trackerStatus.status, isJammer = isTrackerJammerSuspicion, jumpTier = connectivitySuite.trackerJumpTier,
                isAdaptiveJump = connectivitySuite.isTrackerAdaptiveJump,
                trackerLat = connectivitySuite.trackerLat, trackerLng = connectivitySuite.trackerLng, trackerAccuracy = connectivitySuite.trackerAccuracy, maxTrackerAccuracy = connectivitySuite.trackerMaxAccuracy, trackerLastGpsTs = connectivitySuite.trackerLastGpsTs, trackerLastGpsRt = 0L, trackerLastValidFixRt = connectivitySuite.trackerLastValidFixRt, trackerSpeed = connectivitySuite.trackerSpeed, trackerBattery = connectivitySuite.trackerBattery, trackerTemp = connectivitySuite.trackerTemp, isHardwareOnline = localHealth.isHardwareOnline, isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()), isSignalLoss = isSignalLoss, isGpsStalling = isTrackerStalled, isUiVisible = isUiVisible(), distToHomeAuthority = connectivitySuite.trackerDistToHome, maxDistanceAuthority = remoteProcessor.getMaxDistanceAuthority(), isGpsGap = isTrackerGap, isTamperDetected = connectivitySuite.isTrackerTamperDetected, isPowerTamper = connectivitySuite.isTrackerPowerTamper, trackerTiltDegrees = connectivitySuite.trackerTiltDegrees, trackerAcousticDb = connectivitySuite.trackerAcousticDb, trackerBaroAlt = connectivitySuite.trackerBaroAlt, trackerBaroAltEma = remoteProcessor.getBaroBaseline(), trackerLux = connectivitySuite.trackerLux, isNear = connectivitySuite.isTrackerNear, luxBaseline = remoteProcessor.getLuxBaseline(), acousticFloorDb = remoteProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = remoteProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = connectivitySuite.trackerPeakVibrationShock, trackerCurrentMa = connectivitySuite.trackerCurrentMa, isPowerSaveMode = connectivitySuite.isTrackerPowerSaveMode, standbyBucket = connectivitySuite.trackerStandbyBucket, netInterface = connectivitySuite.trackerNetInterface, isStorageLow = connectivitySuite.isTrackerStorageLow, isStorageCritical = connectivitySuite.isTrackerStorageCritical, isBatterySteepDischarge = connectivitySuite.isTrackerBatterySteepDischarge, isCoolingModeActive = connectivitySuite.isTrackerCoolingModeActive, discoveryPhase = null, capabilities = capabilities, isLocationPending = connectivitySuite.isTrackerLocationPending, locationPendingReason = connectivitySuite.trackerLocationPendingReason, snrSnapshot = gpsManager.averageSnr, vibeSnapshot = 0.0
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); alarmEvalJob?.cancel(); settingsJob?.cancel()
        super.onDestroy()
    }
}
