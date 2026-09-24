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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.*

/**
 * ViewerService: Background monitoring for the Viewer role.
 * Sep.24.60:
 * - Issue #1308 REMEDIATION: Implemented Forensic Sampling Loop in ViewerService.
 *   Ensures local environment forensic parity with Tracker role (R-ID 466).
 * Sep.24.40:
 * - Issue #1241 REMEDIATION: Restored functional history sync streams by implementing 
 *   legitimate observation of HistoryManager events (R-ID 464).
 * Sep.24.10:
 * - Issue #1301 REMEDIATION: Loaded and persisted remote tracker Lux and Acoustic baselines 
 *   within ViewerService to eliminate baseline learning lag upon restart (R-ID 461).
 * - Issue #1302 REMEDIATION: Aligned selfProcessor with sensor updates in processTick to 
 *   ensure correct local motion awareness.
 * - Issue #1303 REMEDIATION: Removed cross-role HardwareSuite sensitivity contamination by 
 *   decoupling local hardware settings from remote tracker anchors.
 * - Issue #1304 REMEDIATION: Fixed peer stat reset logic to prevent local state corruption.
 * Sep.24.04:
 * - Issue #1271 REMEDIATION: Restored hardwareSuite's adaptive vibration floor during 
 *   initialization using the persisted value to prevent sensitivity resets.
 * Sep.24.03:
 * - Issue #1271: Implemented persistence for Adaptive Vibration Floor. Restored floor anchor 
 *   during remoteProcessor initialization and registered persistent observer for floor updates.
 * Sep.24.02:
 * - Issue #1255 REMEDIATION: Implemented reboot-aware monotonic clock recovery via 
 *   HistoryManager.recoverLastRealtime using role-isolated clock drift reference.
 */
@AndroidEntryPoint
class ViewerService : BaseMonitorService() {

    @Inject lateinit var sessionCoordinator: SessionLifecycleCoordinator
    @Inject lateinit var deviceProfileManager: DeviceProfileManager

    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var revivalEventsJob: Job? = null
    private var forensicSamplingJob: Job? = null
    
    private val forensicTriggerChannel = Channel<Boolean>(Channel.BUFFERED)

    private fun triggerForensicSample(isSpike: Boolean = false) {
        forensicTriggerChannel.trySend(isSpike)
    }
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    private var lastGpsSpeed = 0.0
    private var lastGpsAccuracy = 0.0
    private var lastGpsBearing = 0.0

    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L

    private var currentIntervalMs = TICK_INTERVAL_MS

    private var lastForensicLat = 0.0
    private var lastForensicLng = 0.0
    private var lastForensicVibe = 0.0
    private var lastForensicTilt = 0.0

    private var lastWasCooling = false
    private var coolingEnteredRt = 0L
    private val forensicCaptureMutex = Mutex()

    private lateinit var selfProcessor: LocationProcessor
    private lateinit var remoteProcessor: LocationProcessor

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    override fun onServicePreInit() {
        notificationManager.setTrackerMode(false)
        selfProcessor = LocationProcessor(timeProvider)
        remoteProcessor = LocationProcessor(timeProvider)
    }

    override suspend fun onServiceInitialize() {
        // R-ID 453: Use role-based prefix for logic state isolation
        repository.saveLongSync("V_" + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync("V_" + LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        
        configManager.deviceId = trackerId
        configManager.viewerId = viewerId
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = false

        refreshCapabilitiesInternal()
        
        deviceProfileManager.initializeHardwareProfile(capabilities, configManager.deviceId)

        observeAlarmEvents()
        observeIntegrityEvents()
        observeProcessorEvents()
        observeConnectivityEvents()
        observeHistoryEvents()
        observeCommandEvents()
        observeRevivalEvents()
        
        connectivitySuite.updateRemoteProcessor(remoteProcessor)
        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
        
        val settingsSnapshot = repository.getSettingsSnapshot()

        // R-ID 453: Apply namespace prefixes to all role-sensitive logic states
        val savedMaxAcc = repository.getDouble("V_" + MAX_ACCURACY_KEY, 0.0)
        val savedLastSitTs = repository.getLong("V_" + LAST_SIT_TS_KEY, 0L)
        val savedBaseline = repository.getDouble("V_" + CHAIR_BASELINE_TILT_KEY, -1000.0)
        val savedVibeFloor = repository.getDouble("V_" + ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0)
        val savedLuxBaseline = repository.getDouble("V_" + TRACKER_LUX_BASELINE_KEY, -1.0)
        val savedAcousticFloor = repository.getDouble("V_" + TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
        val trackerState = repository.loadTrackerState("V_")
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
            savedSitVzRt = trackerState?.sitVzRt ?: 0L,
            savedVibrationFloor = savedVibeFloor,
            savedLuxBaseline = savedLuxBaseline,
            savedAcousticFloor = savedAcousticFloor
        )
        
        selfProcessor.loadState(0.0, 0L, -1000.0, null, homePoints, maxDist)

        // Issue #1303: Decoupled local hardwareSuite from remote tracker sensitivity anchor.
        // Local floor will re-adapt autonomously for the monitor device.

        val savedAlarms = repository.getLastAlarmsJson("V_")
        alarmManager.restoreState(savedAlarms)
        alarmManager.restoreLogicState(settingsSnapshot, "V_")

        historyManager.initialize(lifecycleScope, "V_")
        
        hardwareSuite.start()
        
        currentIntervalMs = if (isUiVisible()) HIGH_FREQUENCY_GPS_POLLING_MS else VIEWER_GPS_POLLING_MS
        hardwareSuite.setPollingInterval(currentIntervalMs)

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

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

        val recoveredTs = repository.getLong("V_" + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong("V_" + CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = historyManager.recoverLastRealtime(recoveredTs, recoveredDrift)
        
        remoteProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        selfProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()

        systemMonitor.setSessionStart(serviceStartRealtime)

        startTickLoop()
        startHeartbeatLoop()
        startForensicSamplingLoop()
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
                    is IntegrityEvent.LogEvent -> logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
                    else -> {} 
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
                        val msg = "ENERGY AUDIT (V): Revival Footprint - Delta: ${event.deltaMa}mA, Temp Rise: ${event.deltaTemp}°C, Duration: ${event.durationMs}ms"
                        val proc = lastProcessedLocation
                        logManager.submitToLogSink(msg, "system", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is HardwareSuite.RevivalEvent.HardwareLock -> {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent(m = "CRITICAL (V): GPS_HARDWARE_LOCK - All revival attempts failed. Hardware stall confirmed.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    }
                    is HardwareSuite.RevivalEvent.Attempt -> {
                        logManager.logServiceEvent(m = "GPS REVIVAL (V): Hardware restart attempt ${event.count} triggered.", isImportant = false)
                    }
                    is HardwareSuite.RevivalEvent.Success -> {
                        logManager.logServiceEvent(m = "GPS REVIVAL (V): Hardware fix restored successfully.", isImportant = true)
                    }
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
                    m = prefix + event.message, 
                    isImportant = event.isImportant, 
                    isSpecial = event.isSpecial || event.message.contains("Merge-on-Stale"), 
                    specialColor = specialColor, 
                    lat = event.lat, lng = event.lng, 
                    accuracy = event.accuracy, snr = event.snr, vibe = event.vibe
                )
            }
            is ProcessorEvent.MaxAccuracyChanged -> {
                if (!isSelf) repository.saveDoubleSync("V_" + MAX_ACCURACY_KEY, event.accuracy)
            }
            is ProcessorEvent.ChairBaselineChanged -> {
                val (lat, lng, maxAcc) = if (isSelf) {
                    val proc = lastProcessedLocation
                    Triple(proc?.optimizedPoint?.lat ?: 0.0, proc?.optimizedPoint?.lng ?: 0.0, proc?.maxAccuracy ?: 0.0)
                } else {
                    val status = connectivitySuite.trackerStatus
                    Triple(status.lat, status.lng, status.maxAccuracy)
                }
                
                logManager.logServiceEvent(m = "Passive Zeroing - Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", event.baseline)}°",
                    lat = lat, lng = lng, accuracy = maxAcc)
            }
            is ProcessorEvent.VibrationFloorChanged -> {
                if (!isSelf) repository.saveDoubleSync("V_" + ADAPTIVE_VIBRATION_FLOOR_KEY, event.floor)
            }
            is ProcessorEvent.LuxBaselineChanged -> {
                if (!isSelf) repository.saveDoubleSync("V_" + TRACKER_LUX_BASELINE_KEY, event.baseline)
            }
            is ProcessorEvent.AcousticFloorChanged -> {
                if (!isSelf) repository.saveDoubleSync("V_" + TRACKER_ACOUSTIC_FLOOR_KEY, event.floor)
            }
            is ProcessorEvent.GpsStallDetected -> {
                if (isSelf) logManager.logServiceEvent(m = "GPS STALL: Fix unchanged for >1s", isImportant = false)
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
            historyManager.historyEvents.collect { event ->
                when (event) {
                    is HistoryEvent.LogEvent -> logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
                }
            }
        }
    }

    private fun observeCommandEvents() {
        lifecycleScope.launch(Dispatchers.Default) {
            commandRouter.commandEvents.collectLatest { event ->
                when (event) {
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { 
                        refreshCapabilitiesInternal()
                        launch { hardwareSuite.start() }
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
            isSamsungDevice = perms.isSamsungDevice,
            isHuaweiDevice = perms.isHuaweiDevice,
            isMicrophoneGranted = perms.isMicrophoneGranted,
            performanceTier = perms.performanceTier
        )
    }

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val lat = location.latitude; val lng = location.longitude; val alt = location.altitude
        
        lastGpsSpeed = location.speed.toDouble(); lastGpsAccuracy = location.accuracy.toDouble(); lastGpsBearing = location.bearing.toDouble()

        forensicAuditor.recordGpsFix(nowRt, currentIntervalMs, "V")?.let { gapMsg ->
            val proc = lastProcessedLocation
            logManager.submitToLogSink(
                message = "STABILITY GAP (V): $gapMsg",
                type = "system",
                isImportant = true,
                isSpecial = true,
                specialColor = FORENSIC_PINK_COLOR,
                lat = proc?.optimizedPoint?.lat ?: 0.0,
                lng = proc?.optimizedPoint?.lng ?: 0.0,
                accuracy = lastGpsAccuracy
            )
        }

        val processed = selfProcessor.processGpsPoint(
            lat = lat, lng = lng, alt = alt, androidSpeedMps = lastGpsSpeed, 
            gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing,
            snr = hardwareSuite.averageSnr, satsUsed = location.extras?.getInt("satellites") ?: hardwareSuite.satellitesUsed, isViewerTrail = true, lastGpsTs = sessionManager.lastGpsTs, isLocal = true, 
            nowRt = nowRt, nowWall = nowWall
        )

        if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
        
        lastKnownLocation = location; lastProcessedLocation = processed

        val health = integrityMonitor.currentHealth
        repository.updateLocation(LocationUpdate().apply {
            this.kinetic.lat = lat; this.kinetic.lng = lng; this.kinetic.alt = alt; this.kinetic.speed = lastGpsSpeed; this.kinetic.accuracy = lastGpsAccuracy; 
            this.kinetic.bearing = lastGpsBearing; this.kinetic.gpsTs = location.time; 
            this.kinetic.rt = nowRt;
            this.kinetic.maxAccuracy = processed.maxAccuracy;

            this.atmospheric.temp = health.batteryTemp; 
            this.atmospheric.maxTemp = health.maxTemp;

            this.integrity.battery = health.batteryLevel; 
            this.integrity.isCharging = health.isCharging; 
            this.integrity.satsView = hardwareSuite.satellitesInView; 
            this.integrity.satsUsed = location.extras?.getInt("satellites") ?: hardwareSuite.satellitesUsed; 
            this.integrity.currentMa = health.currentMa; 
            this.integrity.snrIdx = (hardwareSuite.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)

            this.ts = nowWall; 
            this.isMe = true; 
            this.lastValidFixRt = selfProcessor.getLastValidFixRt(); 
            this.status = processed.status; 
            this.isClockRegression = processed.isClockRegression
        })
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) return
        val nowRt = timeProvider.elapsedRealtime()
        if ((configManager.deviceId == SettingsRepository.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id; connectivitySuite.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(TRACKER_ID_KEY, id) }
        }
        
        val isNew = sessionManager.onTrackerPulse(id, nowRt)
        if (isNew || tickJob?.isActive != true) {
            if (isNew) {
                val proc = lastProcessedLocation
                logManager.logServiceEvent(m = "Device connected: $id", lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        sessionCoordinator.resetSession(
            roleTag = "V",
            processors = listOf(selfProcessor, remoteProcessor),
            onReset = {
                serviceStartRealtime = timeProvider.elapsedRealtime()
                serviceStartWall = timeProvider.currentTimeMillis()
                lastHardwareRecoveryTs = 0L
                
                lastForensicLat = 0.0
                lastForensicLng = 0.0
                lastForensicVibe = 0.0
                lastForensicTilt = 0.0
                lastWasCooling = false
                coolingEnteredRt = 0L
            }
        )
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible); updateForegroundServiceType()
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
                try {
                    delay(200)
                    val type = getAvailableForegroundServiceType()
                    val health = integrityMonitor.currentHealth
                    val msg = notificationManager.getPulseMessage(
                        hardwareSuite.satellitesUsed,
                        health.batteryLevel,
                        !alarmManager.hasUnresolvedAlarms(),
                        isPowerSaveActive || health.isPowerSaveMode
                    )
                    safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
                } catch (e: Exception) { if (e !is CancellationException) Timber.e(e, "Failed to update FGS type") }
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
        return type
    }
    
    override fun getRequiredTickInterval(): Long { return TICK_INTERVAL_MS }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt); integrityMonitor.checkInternetIntegrity(nowRt)
        val health = integrityMonitor.currentHealth; val snapshot = hardwareSuite.consumeLogicSnapshot()

        val sensorSnapshot = SensorStateSnapshot(
            vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux, isNear = snapshot.isNear, powerTamper = health.isPowerTamper,
            tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, peakShock = snapshot.peakShock, acousticMinDb = snapshot.acousticPeakMin,
            peakVerticalVelocity = snapshot.peakVerticalVelocity, peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs, peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt,
            plungeMatched = snapshot.plungeMatched, peakVerticalDisplacement = snapshot.peakVerticalDisplacement, isSirenActive = false, isWarming = hardwareSuite.isWarming,
            manualAdaptiveFloor = -1.0, acousticLockoutRt = 0L, lightSpikeRt = 0L, isMuzzled = false,
            kineticEnergy = snapshot.kineticEnergy, providedAdaptiveFloor = snapshot.adaptiveVibrationFloor, nowRt = nowRt, nowTs = now
        )

        val evalSnapshot = EvaluationSnapshot(health = health, sensor = sensorSnapshot)
        
        val targetGpsInterval = if (isUiVisible()) HIGH_FREQUENCY_GPS_POLLING_MS else VIEWER_GPS_POLLING_MS
        if (targetGpsInterval != currentIntervalMs) {
            currentIntervalMs = targetGpsInterval
            forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, "V")
            selfProcessor.updateExpectedInterval(nowRt, targetGpsInterval)
            hardwareSuite.setPollingInterval(targetGpsInterval)
        }

        val isSocketConnected = connectivitySuite.isConnected()
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = connectivitySuite.lastPeerActivityTs > 0 && (nowRt - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isSocketConnected && isTrackerActive, isInViolation = alarmManager.hasUnresolvedAlarms())

        deviceProfileManager.executeContinuityTweaks(
            capabilities = capabilities,
            nowRt = nowRt,
            serviceTickCounter = serviceTickCounter,
            lastValidFixRt = selfProcessor.getLastValidFixRt(),
            isPowerSaveMode = isPowerSaveActive || evalSnapshot.health.isPowerSaveMode,
            localInternetLoss = evalSnapshot.health.localInternetLoss,
            isSocketConnected = isSocketConnected,
            isPeerActive = isTrackerActive
        )

        forensicAuditor.evaluateStability(nowRt, "V")?.let { verdict ->
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

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            val isStaggered = capabilities.performanceTier == PerformanceTier.STAGGERED
            val recoveryThreshold = if (isStaggered) 10000L else HARDWARE_SUPPRESSION_THRESHOLD_MS
            
            if (tickGap > recoveryThreshold && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                recoveryFlagged = true
                val proc = lastProcessedLocation
                logManager.logServiceEvent(m = "HEURISTIC RECOVERY (V): Heartbeat gap detected (${tickGap}ms). Reviving connection.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                systemMonitor.acquireWakeLock()
                connectivitySuite.connect(configManager.relayUrl)
            }
        }

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val hasUnresolved = alarmManager.hasUnresolvedAlarms()
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(hardwareSuite.isStationary(), evalSnapshot.health.gpsStalled, hasUnresolved, isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; hardwareSuite.setPowerSaveMode(shouldBePowerSave)
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        // Issue #1302: Corrected local sensor integration for selfProcessor
        selfProcessor.updateSensorData(evalSnapshot.sensor)

        val location = lastKnownLocation
        if (location != null) {
            selfProcessor.processGpsPoint(location.latitude, location.longitude, location.altitude, location.speed.toDouble(), location.time, lastGpsAccuracy, location.bearing.toDouble(), 0.0, 0, true, 0L, true, nowRt = nowRt, nowWall = now)
        }

        val noiseIdx = (evalSnapshot.sensor.acousticDb - selfProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val liftIdx = (evalSnapshot.sensor.baroAlt - selfProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS

        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = 10, peerAvail = isSocketConnected && isTrackerActive, hasGps = (lastProcessedLocation?.timestamp ?: 0L) > 0, isTrackerMode = false, accuracy = lastGpsAccuracy, maxAccuracy = selfProcessor.getMaxTrackerAccuracy(), noiseIdx = noiseIdx, luxIdx = log10(evalSnapshot.sensor.lux + 1.0) / RIBBON_LUX_LOG_SCALE, vibeIdx = evalSnapshot.sensor.vibration / RIBBON_VIBRATION_SCALE_G, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = (hardwareSuite.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0), tiltIdx = abs(evalSnapshot.sensor.tiltDegrees - selfProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG, baroIdx = (evalSnapshot.sensor.baroAlt - selfProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS, verticalVelocity = evalSnapshot.sensor.peakVerticalVelocity, sitVz = evalSnapshot.sensor.peakVerticalVelocity, sitVzTs = evalSnapshot.sensor.peakVerticalVelocityTs, sitVzRt = evalSnapshot.sensor.peakVerticalVelocityRt, sitDz = evalSnapshot.sensor.peakVerticalDisplacement, sitBaro = evalSnapshot.sensor.baroAlt, sitTilt = evalSnapshot.sensor.tiltDegrees, sitShock = evalSnapshot.sensor.peakShock, isBatterySteepDischarge = evalSnapshot.health.isBatterySteepDischarge, isCoolingModeActive = evalSnapshot.health.isCoolingModeActive, speed = lastProcessedLocation?.filteredSpeed ?: 0.0, bearing = lastGpsBearing, isSitDetected = false, isSitActive = false, currentMa = evalSnapshot.health.currentMa, locationPendingReason = evalSnapshot.health.locationPendingReason, kineticEnergy = evalSnapshot.sensor.kineticEnergy, isRecoveryEvent = recoveryFlagged
        )

        evaluateAlarmsInternal(now, nowRt, evalSnapshot, isSocketConnected, isTrackerActive)

        // R-ID 453: Isolated service ticks via prefix
        repository.saveLongSync("V_" + LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync("V_" + LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        
        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
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

        // Decouple spikes from sampling rate gates
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
                    logManager.logServiceEvent(m = "Forensic Performance Audit (V): Thermal Recovery Latency: ${latency}ms", isImportant = true)
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

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, evalSnapshot: EvaluationSnapshot, isSocketConnected: Boolean, isTrackerActive: Boolean) {
        val status = connectivitySuite.trackerStatus
        val home = repository.getCachedHomePoints().firstOrNull()
        val distToHome = if (status.lat != 0.0 && home != null) PhysicsUtils.calculateDistance(status.lat, status.lng, home.latitude, home.longitude) else null

        val telemetry = AlarmTelemetrySnapshot(
            status = status.status,
            isJammer = status.isJammer,
            jumpTier = status.jumpTier,
            isAdaptiveJump = status.isAdaptiveJump,
            lat = status.lat,
            lng = status.lng,
            accuracy = status.accuracy,
            maxAccuracy = status.maxAccuracy,
            gpsTs = status.gpsTs,
            lastValidFixRt = status.lastValidFixRt,
            speed = status.speed,
            battery = status.battery,
            temp = status.temp,
            currentMa = status.currentMa,
            isLocationPending = status.isLocationPending,
            locationPendingReason = status.locationPendingReason,
            isTamperDetected = status.isTamperDetected,
            isPowerTamper = status.isPowerTamper,
            tiltDegrees = status.tiltDegrees,
            acousticDb = status.acousticDb,
            baroAlt = status.baroAlt,
            baroAltEma = status.sitBaro,
            lux = status.lux,
            isNear = status.isNear,
            luxBaseline = status.luxBaseline,
            acousticFloorDb = status.acousticFloorDb,
            adaptiveVibrationFloor = status.adaptiveVibrationFloor,
            peakVibrationShock = status.peakVibrationShock,
            isPowerSaveMode = status.isPowerSaveMode,
            standbyBucket = status.standbyBucket,
            netInterface = status.netInterface,
            isStorageLow = status.isStorageLow,
            isStorageCritical = status.isStorageCritical,
            isBatterySteepDischarge = status.isBatterySteepDischarge,
            isCoolingModeActive = status.isCoolingModeActive,
            snrSnapshot = status.snrIdx * RIBBON_SNR_SCALE_DB,
            vibeSnapshot = status.vibeIdx * RIBBON_VIBRATION_SCALE_G,
            isGpsHardwareLock = status.gpsHardwareLock,
            tamperNote = status.tamperNote,
            isSignalLoss = evalSnapshot.health.signalLoss,
            isGpsStalling = status.isStalled,
            isGpsGap = status.isClockRegression || (nowRt - status.lastValidFixRt > GPS_GAP_THRESHOLD_MS),
            localInternetLoss = evalSnapshot.health.localInternetLoss,
            isHardwareOnline = evalSnapshot.health.isHardwareOnline
        )

        val serviceContext = AlarmServiceContext(
            now = now,
            nowRt = nowRt,
            serviceStartTs = serviceStartWall,
            serviceStartRt = serviceStartRealtime,
            appStartTime = sessionManager.appStartTime,
            isTrackerMode = false,
            isRelayConnected = isSocketConnected,
            isTrackerConnected = isTrackerActive,
            isUiVisible = isUiVisible(),
            distToHomeAuthority = distToHome,
            maxDistanceAuthority = remoteProcessor.getMaxDistanceAuthority(),
            capabilities = capabilities,
            rolePrefix = "V_"
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
