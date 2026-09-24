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
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.math.*

/**
 * MonitorService: Unified role-reactive background service for Tracker and Viewer modes.
 * Sep.24.96:
 * - Issue #1312 REMEDIATION: Migrated evaluation pipeline to consume unified 
 *   SystemEvaluationSnapshot, ensuring absolute temporal parity between kinematic, 
 *   environmental, and health logic.
 * - Role-switching Validation: Added reactive appModeFlow listener and handleRoleTransition
 *   to support clean, live role transitions. Made remoteProcessor non-nullable.
 * Sep.24.95:
 * - Issue #1163: Updated LocationProcessor interactions to align with the 
 *   stateless refactor.
 * Sep.24.93:
 * - Issue #1265 REMEDIATION: Integrated AppEventCoordinator to offload domain 
 *   event orchestration. Cleaned up redundant observers for alarms, integrity, 
 *   and forensics (R-ID 472).
 * Sep.24.92:
 * - Issue #1261 REMEDIATION: Consolidated TrackerService and ViewerService into 
 *   a single reactive engine. Merged redundant boilerplate (R-ID 471).
 */
@AndroidEntryPoint
class MonitorService : BaseMonitorService() {

    @Inject lateinit var sessionCoordinator: SessionLifecycleCoordinator
    @Inject lateinit var deviceProfileManager: DeviceProfileManager
    @Inject lateinit var eventCoordinator: AppEventCoordinator

    private var activeMode: String? = null
    private var rolePrefix: String = "T_"
    private var isTrackerMode: Boolean = true

    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var revivalEventsJob: Job? = null
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var forensicSamplingJob: Job? = null
    
    private val forensicTriggerChannel = Channel<Boolean>(Channel.BUFFERED)
    private val forensicCaptureMutex = Mutex()
    private val locationBuffer = ConcurrentLinkedQueue<Location>()

    private lateinit var primaryProcessor: LocationProcessor
    private lateinit var remoteProcessor: LocationProcessor

    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null
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
    
    private var lastGpsBearing = 0.0
    private var lastGpsAccuracy = 0.0

    override fun onServicePreInit() {
        runBlocking {
            activeMode = repository.getAppMode() ?: "tracker"
        }
        isTrackerMode = activeMode == "tracker"
        rolePrefix = if (isTrackerMode) "T_" else "V_"
        notificationManager.setTrackerMode(isTrackerMode)
        
        primaryProcessor = LocationProcessor(timeProvider)
        remoteProcessor = LocationProcessor(timeProvider)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync(rolePrefix + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync(rolePrefix + LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        
        configManager.deviceId = trackerId
        configManager.viewerId = viewerId
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = isTrackerMode

        refreshCapabilitiesInternal()
        deviceProfileManager.initializeHardwareProfile(capabilities, configManager.deviceId)

        eventCoordinator.start(
            alarmManager = alarmManager,
            hardwareSuite = hardwareSuite,
            connectivitySuite = connectivitySuite,
            commandRouter = commandRouter,
            primaryProcessor = primaryProcessor,
            remoteProcessor = remoteProcessor
        )

        setupServiceObservers()

        connectivitySuite.updateRemoteProcessor(remoteProcessor)
        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, isTrackerMode)
        
        loadLogicState()

        historyManager.initialize(lifecycleScope, rolePrefix)
        hardwareSuite.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        val recoveredTs = repository.getLong(rolePrefix + LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = historyManager.recoverLastRealtime(recoveredTs, recoveredDrift)
        
        primaryProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        remoteProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        
        systemMonitor.setSessionStart(timeProvider.elapsedRealtime())

        if (isTrackerMode) setupPhysicalFastPaths()
        
        startTickLoop()
        startHeartbeatLoop()
        startForensicSamplingLoop()
        
        logManager.logServiceEvent("${if (isTrackerMode) "Tracker" else "Viewer"} Engine Online (Unified)", isImportant = true)
    }

    private suspend fun loadLogicState() {
        val settingsSnapshot = repository.getSettingsSnapshot()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)

        if (isTrackerMode) {
            val trackerState = repository.loadTrackerState("T_")
            primaryProcessor.loadState(
                savedMaxAccuracy = repository.getDouble("T_" + MAX_ACCURACY_KEY, 0.0),
                savedLastSitTs = repository.getLong("T_" + LAST_SIT_TS_KEY, 0L),
                savedBaseline = repository.getDouble("T_" + CHAIR_BASELINE_TILT_KEY, -1000.0),
                trackerState = trackerState,
                homePoints = homePoints,
                maxDistance = maxDist,
                savedVibrationFloor = repository.getDouble("T_" + ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0),
                savedLuxBaseline = repository.getDouble("T_" + TRACKER_LUX_BASELINE_KEY, -1.0),
                savedAcousticFloor = repository.getDouble("T_" + TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
            )
            alarmManager.restoreState(repository.getLastAlarmsJson("T_"))
            alarmManager.restoreLogicState(settingsSnapshot, "T_")
            
            val vibeFloor = repository.getDouble("T_" + ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0)
            if (vibeFloor >= 0.0) hardwareSuite.setAdaptiveVibrationFloor(vibeFloor)
        } else {
            val remoteState = repository.loadTrackerState("VR_")
            remoteProcessor.loadState(
                savedMaxAccuracy = repository.getDouble("VR_" + MAX_ACCURACY_KEY, 0.0),
                savedLastSitTs = repository.getLong("VR_" + LAST_SIT_TS_KEY, 0L),
                savedBaseline = repository.getDouble("VR_" + CHAIR_BASELINE_TILT_KEY, -1000.0),
                trackerState = remoteState,
                homePoints = homePoints,
                maxDistance = maxDist,
                savedSitVz = remoteState?.sitVz ?: 0.0,
                savedSitDz = remoteState?.sitDz ?: 0.0,
                savedSitBaro = remoteState?.sitBaro ?: 0.0,
                savedSitTilt = remoteState?.sitTilt ?: 0.0,
                savedSitShock = remoteState?.sitShock ?: 0.0,
                savedSitVzTs = remoteState?.sitVzTs ?: 0L,
                savedSitVzRt = remoteState?.sitVzRt ?: 0L,
                savedVibrationFloor = repository.getDouble("VR_" + ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0),
                savedLuxBaseline = repository.getDouble("VR_" + TRACKER_LUX_BASELINE_KEY, -1.0),
                savedAcousticFloor = repository.getDouble("VR_" + TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
            )
            primaryProcessor.loadState(0.0, 0L, -1000.0, null, homePoints, maxDist)
            alarmManager.restoreState(repository.getLastAlarmsJson("VR_"))
            alarmManager.restoreLogicState(settingsSnapshot, "VR_")
        }
    }

    private fun setupServiceObservers() {
        lifecycleScope.launch(Dispatchers.Default) {
            launch { observeConnectivityEvents() }
            launch { observeCommandEvents() }
            launch { observeSettingsChanges() }
            launch {
                repository.appModeFlow.collectLatest { mode ->
                    if (mode != null && mode != activeMode) {
                        withContext(Dispatchers.Main) {
                            handleRoleTransition(mode)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleRoleTransition(newMode: String) {
        if (activeMode == newMode) return
        Timber.i("MonitorService: Handling dynamic role transition from $activeMode to $newMode")
        logManager.logServiceEvent("Engine transitioning role: $activeMode -> $newMode", isImportant = true)

        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        alarmEvalJob?.cancel()
        
        val oldTag = if (isTrackerMode) "T" else "V"
        sessionCoordinator.resetSession(roleTag = oldTag, processors = listOf(primaryProcessor, remoteProcessor), onReset = {
            locationBuffer.clear()
        })

        activeMode = newMode
        isTrackerMode = newMode == "tracker"
        rolePrefix = if (isTrackerMode) "T_" else "V_"
        notificationManager.setTrackerMode(isTrackerMode)
        configManager.isTrackerMode = isTrackerMode

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        connectivitySuite.start(configManager.relayUrl, trackerId, viewerId, isTrackerMode)

        loadLogicState()

        lastServiceTickTs = timeProvider.currentTimeMillis()
        lastServiceTickRealtime = timeProvider.elapsedRealtime()
        primaryProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        remoteProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())

        gpsCollectionJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) } }
        gnssDetailJob = lifecycleScope.launch(Dispatchers.Default) { hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

        if (isTrackerMode) {
            setupPhysicalFastPaths()
        } else {
            hardwareSuite.setLightFastPath(0.0, 0.0, null)
            hardwareSuite.setAcousticFastPath(0.0, 0.0, 0.0, null)
        }

        updateForegroundServiceType()
        logManager.logServiceEvent("${if (isTrackerMode) "Tracker" else "Viewer"} Engine Online via Dynamic Switch", isImportant = true)
    }

    private suspend fun observeConnectivityEvents() {
        connectivitySuite.connectivityEvents.collect { event ->
            if (event is ConnectivityEvent.PeerPulse) {
                if (isTrackerMode) handleViewerPulse(event.id) else handleTrackerPulse(event.id)
            }
        }
    }

    private suspend fun observeCommandEvents() {
        commandRouter.commandEvents.collect { event ->
            when (event) {
                is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                is CommandEvent.UiPulse -> { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                is CommandEvent.ResetTimers -> resetServiceTimers()
                is CommandEvent.SyncSensors -> { refreshCapabilitiesInternal(); lifecycleScope.launch { hardwareSuite.start() } }
                is CommandEvent.ExecuteStressTest -> if (isTrackerMode) executeAutomatedStressTest()
                is CommandEvent.SimulateStoragePressure -> {}
            }
        }
    }

    private suspend fun observeSettingsChanges() {
        coroutineScope {
            launch { repository.alertSettingsFlow.collect { settings -> alarmManager.updateSettings(settings) } }
            launch { 
                repository.homePointsFlow.collect { points -> 
                    val enginePoints = points.map { EngineGeoPoint(it.latitude, it.longitude) }
                    primaryProcessor.setHomePoints(enginePoints)
                    remoteProcessor.setHomePoints(enginePoints)
                } 
            }
            launch { 
                repository.maxDistanceFlow.collect { dist -> 
                    primaryProcessor.setMaxDistanceAuthority(dist)
                    remoteProcessor.setMaxDistanceAuthority(dist)
                } 
            }
            if (isTrackerMode) {
                launch { repository.isSafeMode.collect { safe -> hardwareSuite.setSafeMode(safe) } }
            }
        }
    }

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        
        lastGpsBearing = location.bearing.toDouble()
        lastGpsAccuracy = location.accuracy.toDouble()
        
        if (isTrackerMode) {
            locationBuffer.add(location)
        } else {
            val snapshot = SystemEvaluationSnapshot(
                lat = location.latitude, lng = location.longitude, alt = location.altitude, 
                speed = location.speed.toDouble(), gpsTs = location.time, accuracy = lastGpsAccuracy, 
                bearing = location.bearing.toDouble(), snrSnapshot = hardwareSuite.averageSnr,
                nowRt = nowRt, nowTs = nowWall
            )
            val processed = primaryProcessor.processGpsPoint(snapshot, isViewerTrail = true, lastGpsTs = sessionManager.lastGpsTs, isLocal = true)
            if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
            lastProcessedLocation = processed
            updateRepositoryLocation(processed, location, nowRt, nowWall)
        }
    }

    private fun updateRepositoryLocation(processed: ProcessedLocation, location: Location, nowRt: Long, nowWall: Long) {
        val health = integrityMonitor.currentHealth
        repository.updateLocation(LocationUpdate().apply {
            this.kinetic.lat = location.latitude; this.kinetic.lng = location.longitude; this.kinetic.alt = location.altitude; this.kinetic.speed = location.speed.toDouble(); this.kinetic.accuracy = location.accuracy.toDouble()
            this.kinetic.bearing = location.bearing.toDouble(); this.kinetic.gpsTs = location.time; this.kinetic.rt = nowRt; this.kinetic.maxAccuracy = processed.maxAccuracy
            this.atmospheric.temp = health.batteryTemp; this.atmospheric.maxTemp = health.maxTemp
            this.integrity.battery = health.batteryLevel; this.integrity.isCharging = health.isCharging; this.integrity.satsView = hardwareSuite.satellitesInView; this.integrity.satsUsed = location.extras?.getInt("satellites") ?: hardwareSuite.satellitesUsed; this.integrity.currentMa = health.currentMa
            this.integrity.snrIdx = (hardwareSuite.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            this.ts = nowWall; this.isMe = true; this.lastValidFixRt = primaryProcessor.getLastValidFixRt(); this.status = processed.status; this.isClockRegression = processed.isClockRegression
        })
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
            if (isNew) logManager.logServiceEvent(m = "Viewer connected: $id")
            startTickLoop() 
        }
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
        val tag = if (isTrackerMode) "T" else "V"
        val processors = mutableListOf(primaryProcessor, remoteProcessor)
        
        sessionCoordinator.resetSession(roleTag = tag, processors = processors, onReset = {
            serviceStartRealtime = timeProvider.elapsedRealtime()
            serviceStartWall = timeProvider.currentTimeMillis()
            lastHardwareRecoveryTs = 0L; lastForensicLat = 0.0; lastForensicLng = 0.0; lastForensicVibe = 0.0; lastForensicTilt = 0.0; lastWasCooling = false
            if (isTrackerMode) {
                lastFastPathAcousticSpikeTs = 0L; lastFastPathLightSpikeTs = 0L
                setupPhysicalFastPaths()
                locationBuffer.clear()
            }
        })
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible); updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        val health = integrityMonitor.currentHealth
        val msg = notificationManager.getPulseMessage(sats = 0, battery = if (health.batteryLevel > 0) health.batteryLevel else integrityMonitor.getBatteryLevel(), isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = health.isPowerSaveMode)
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type, force = true)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(200)
                val type = getAvailableForegroundServiceType()
                val health = integrityMonitor.currentHealth
                val msg = notificationManager.getPulseMessage(hardwareSuite.satellitesUsed, health.batteryLevel, isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = isPowerSaveActive || health.isPowerSaveMode)
                safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun getAvailableForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && capabilities.isA15Device) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        if (isTrackerMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (capabilities.isMicrophoneGranted && (hardwareSuite.isAcousticMonitoringEnabled() || isRecentUiPulse())) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
        }
        return type
    }

    override fun getRequiredTickInterval(): Long = if (isTrackerMode && isPowerSaveActive) POWER_SAVE_TICK_INTERVAL_MS else TICK_INTERVAL_MS

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt); integrityMonitor.checkInternetIntegrity(nowRt)
        val health = integrityMonitor.currentHealth; val snapshot = hardwareSuite.consumeLogicSnapshot()

        val isSocketConnected = connectivitySuite.isConnected(); connectivitySuite.updateRelayStatus(isSocketConnected)
        val isPeerActive = if (isTrackerMode) (sessionManager.getViewerCount() > 0 || isRecentUiPulse()) else (connectivitySuite.lastPeerActivityTs > 0 && (nowRt - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS))

        val evaluationSnapshot = SystemEvaluationSnapshot(
            // Environmental & Sensor
            vibration = snapshot.vibration, heading = snapshot.heading, baroAlt = snapshot.baroAlt, baroAltEma = primaryProcessor.getBaroBaseline(),
            lux = snapshot.lux, isNear = snapshot.isNear, tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb, peakShock = snapshot.peakShock, 
            acousticMinDb = snapshot.acousticPeakMin, luxBaseline = primaryProcessor.getLuxBaseline(), acousticFloorDb = primaryProcessor.getAcousticFloorDb(),
            adaptiveVibrationFloor = snapshot.adaptiveVibrationFloor, kineticEnergy = snapshot.kineticEnergy,
            peakVerticalVelocity = snapshot.peakVerticalVelocity, peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs, peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt,
            peakVerticalDisplacement = snapshot.peakVerticalDisplacement,
            
            // Health & System
            batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp, currentMa = health.currentMa, isCharging = health.isCharging,
            isPowerTamper = health.isPowerTamper, isLocationPending = health.isLocationPending, locationPendingReason = health.locationPendingReason,
            isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface,
            isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical, isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive, isGpsHardwareLock = health.gpsHardwareLock, cpuLoad = health.cpuLoad, ioWait = health.ioWait,
            maxIoLatency = health.maxIoLatency, isSilentFailure = health.isSilentFailure, isMaliAnomaly = health.isMaliAnomaly, 
            isUltraLongStationary = health.isUltraLongStationary, isBatteryLow = health.isBatteryLow, isBatteryCritical = health.isBatteryCritical,
            isSignalLoss = health.signalLoss, localInternetLoss = health.localInternetLoss, isHardwareOnline = health.isHardwareOnline,
            
            // Temporal & Fast-Paths
            acousticLockoutRt = if (isTrackerMode) lastFastPathAcousticSpikeTs else 0L, lightSpikeRt = if (isTrackerMode) lastFastPathLightSpikeTs else 0L,
            isMuzzled = false, providedAdaptiveFloor = snapshot.adaptiveVibrationFloor, nowRt = nowRt, nowTs = now, snrSnapshot = hardwareSuite.averageSnr
        )
        
        if (isTrackerMode) {
            hardwareSuite.setLightFastPath(baseline = primaryProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP)
            hardwareSuite.setAcousticFastPath(floor = primaryProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0)
            hardwareSuite.setHighLoad(evaluationSnapshot.isCoolingModeActive)
            isSuspiciousMode = serviceBehaviorUseCase.updateSuspiciousMode(isSuspiciousMode, primaryProcessor.checkPhysicalTamper(nowRt, false) == SentinelStatus.TAMPER, primaryProcessor.consumeSitDetected(), nowRt)
            val targetGpsInterval = serviceBehaviorUseCase.calculateGpsInterval(evaluationSnapshot.isCoolingModeActive, isSuspiciousMode, hardwareSuite.isStationary(), hardwareSuite.isScreenOn(), primaryProcessor.getMaxDistanceAuthority() > 0.0, nowRt, capabilities)
            if (targetGpsInterval != currentIntervalMs) {
                currentIntervalMs = targetGpsInterval; forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, "T"); primaryProcessor.updateExpectedInterval(nowRt, targetGpsInterval); hardwareSuite.setPollingInterval(targetGpsInterval)
            }
        } else {
            val targetGpsInterval = if (isUiVisible()) HIGH_FREQUENCY_GPS_POLLING_MS else VIEWER_GPS_POLLING_MS
            if (targetGpsInterval != currentIntervalMs) {
                currentIntervalMs = targetGpsInterval; forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, "V"); primaryProcessor.updateExpectedInterval(nowRt, targetGpsInterval); hardwareSuite.setPollingInterval(targetGpsInterval)
            }
        }

        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isSocketConnected && isPeerActive, isInViolation = alarmManager.hasUnresolvedAlarms())
        deviceProfileManager.executeContinuityTweaks(capabilities, nowRt, serviceTickCounter, primaryProcessor.getLastValidFixRt(), evaluationSnapshot.isPowerSaveMode, evaluationSnapshot.localInternetLoss, isSocketConnected, isPeerActive)

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val recoveryThreshold = if (capabilities.performanceTier == PerformanceTier.STAGGERED) 10000L else HARDWARE_SUPPRESSION_THRESHOLD_MS
            if (nowRt - lastServiceTickRealtime > recoveryThreshold && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt; recoveryFlagged = true
                val proc = lastProcessedLocation; logManager.logServiceEvent(m = "HEURISTIC RECOVERY ${if (isTrackerMode) "" else "(V)"}: Heartbeat gap detected (${nowRt - lastServiceTickRealtime}ms). Reviving connection.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                systemMonitor.acquireWakeLock(); connectivitySuite.connect(configManager.relayUrl)
            }
        }

        forensicAuditor.evaluateStability(nowRt, if (isTrackerMode) "T" else "V")?.let { verdict ->
            val proc = lastProcessedLocation
            logManager.logServiceEvent(m = verdict.message, isImportant = true, isSpecial = verdict.isJitterViolation, specialColor = if (verdict.isJitterViolation) FORENSIC_PINK_COLOR else null, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
        }
        
        primaryProcessor.updateSensorData(evaluationSnapshot)

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(hardwareSuite.isStationary(), evaluationSnapshot.isStalled, alarmManager.hasUnresolvedAlarms(), isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; hardwareSuite.setPowerSaveMode(shouldBePowerSave); logManager.logServiceEvent(m = "POWER SAVER: ${if (shouldBePowerSave) "ENGAGED" else "DISABLED"}", isImportant = false)
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        if (isTrackerMode) {
            var lastLocInBatch: Location? = null
            while (locationBuffer.isNotEmpty()) {
                val loc = locationBuffer.poll() ?: break
                lastLocInBatch = loc
                val pointSnapshot = evaluationSnapshot.copy(
                    lat = loc.latitude, lng = loc.longitude, alt = loc.altitude, speed = loc.speed.toDouble(),
                    gpsTs = loc.time, accuracy = loc.accuracy.toDouble(), bearing = loc.bearing.toDouble(),
                    isStalled = evaluationSnapshot.isStalled, isMuzzled = isSuspiciousMode
                )
                lastProcessedLocation = primaryProcessor.processGpsPoint(pointSnapshot, isViewerTrail = false, lastGpsTs = forensicAuditor.getLastGpsFixRealtime("T"), isLocal = true)
            }
            val proc = lastProcessedLocation
            if (proc != null && lastLocInBatch != null) {
                lastGpsSpeed = lastLocInBatch.speed.toDouble(); lastGpsAccuracy = lastLocInBatch.accuracy.toDouble(); lastGpsBearing = lastLocInBatch.bearing.toDouble()
                evaluateAlarmsInternal(now, nowRt, isSocketConnected, isPeerActive, proc, snapshot, lastLocInBatch.time, evaluationSnapshot)
            }
        } else {
            val location = lastKnownLocation
            if (location != null) {
                val pointSnapshot = evaluationSnapshot.copy(
                    lat = location.latitude, lng = location.longitude, alt = location.altitude, 
                    speed = location.speed.toDouble(), gpsTs = location.time, accuracy = lastGpsAccuracy, 
                    bearing = location.bearing.toDouble(), isViewerTrail = true, isLocal = true
                )
                primaryProcessor.processGpsPoint(pointSnapshot, isViewerTrail = true, lastGpsTs = 0L, isLocal = true)
            }
            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isPeerActive, lastProcessedLocation ?: ProcessedLocation(), snapshot, 0L, evaluationSnapshot)
        }

        val proc = lastProcessedLocation
        val satList = latestGnssDetail?.satellites ?: emptyList()
        val avgCn0 = satList.map { it.cn0 }.safeAverage()
        val noiseIdx = (evaluationSnapshot.acousticDb - primaryProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val luxIdx = log10(evaluationSnapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE
        val vibeIdx = evaluationSnapshot.vibration / RIBBON_VIBRATION_SCALE_G
        val liftIdx = (evaluationSnapshot.baroAlt - primaryProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
        val tiltIdx = abs(evaluationSnapshot.tiltDegrees - primaryProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
        val baroIdx = (evaluationSnapshot.baroAlt - primaryProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS
        val snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB

        if (isTrackerMode) {
            repository.updateLocation(LocationUpdate().apply {
                this.kinetic.lat = proc?.optimizedPoint?.lat ?: 0.0; this.kinetic.lng = proc?.optimizedPoint?.lng ?: 0.0; this.kinetic.alt = proc?.optimizedPoint?.alt ?: 0.0; this.kinetic.speed = proc?.filteredSpeed ?: 0.0; this.kinetic.accuracy = proc?.currentAccuracy ?: 0.0; this.kinetic.bearing = lastGpsBearing; this.kinetic.gpsTs = proc?.timestamp ?: 0L; this.kinetic.rt = nowRt; this.kinetic.maxAccuracy = proc?.maxAccuracy ?: 0.0; this.kinetic.kineticEnergy = evaluationSnapshot.kineticEnergy; this.kinetic.verticalVelocity = evaluationSnapshot.peakVerticalVelocity
                this.atmospheric.temp = evaluationSnapshot.batteryTemp; this.atmospheric.maxTemp = health.maxTemp; this.atmospheric.vibration = evaluationSnapshot.vibration; this.atmospheric.heading = evaluationSnapshot.heading; this.atmospheric.baroAlt = evaluationSnapshot.baroAlt; this.atmospheric.lux = evaluationSnapshot.lux; this.atmospheric.isNear = evaluationSnapshot.isNear; this.atmospheric.tiltDegrees = evaluationSnapshot.tiltDegrees; this.atmospheric.acousticDb = evaluationSnapshot.acousticDb; this.atmospheric.peakVibrationShock = evaluationSnapshot.peakShock; this.atmospheric.peakVibrationShockTs = now; this.atmospheric.noiseIdx = noiseIdx; this.atmospheric.luxIdx = luxIdx; this.atmospheric.vibeIdx = vibeIdx; this.atmospheric.liftIdx = liftIdx; this.atmospheric.tiltIdx = tiltIdx; this.atmospheric.baroIdx = baroIdx; this.atmospheric.luxBaseline = primaryProcessor.getLuxBaseline(); this.atmospheric.acousticFloorDb = primaryProcessor.getAcousticFloorDb(); this.atmospheric.adaptiveVibrationFloor = primaryProcessor.getAdaptiveVibrationFloor(); this.atmospheric.proxIdx = snapshot.proximityIdx; this.atmospheric.proximityCm = snapshot.proximityCm; this.atmospheric.proximityDebounceMs = snapshot.proximityDebounceMs; this.atmospheric.vibrationRollingSum = snapshot.vibrationRollingSum
                this.integrity.battery = evaluationSnapshot.batteryLevel; this.integrity.isCharging = evaluationSnapshot.isCharging; this.integrity.currentMa = evaluationSnapshot.currentMa; this.integrity.satsView = hardwareSuite.satellitesInView; this.integrity.satsUsed = hardwareSuite.satellitesUsed; this.integrity.snrIdx = snrIdx; this.integrity.isPowerTamper = evaluationSnapshot.isPowerTamper; this.integrity.isSitDetected = isSuspiciousMode; this.integrity.lastSitTs = primaryProcessor.getLastSitTs(); this.integrity.sitVz = evaluationSnapshot.peakVerticalVelocity; this.integrity.sitVzTs = evaluationSnapshot.peakVerticalVelocityTs; this.integrity.sitVzRt = evaluationSnapshot.peakVerticalVelocityRt; this.integrity.sitDz = evaluationSnapshot.peakVerticalDisplacement; this.integrity.sitBaro = evaluationSnapshot.peakVerticalDisplacement; this.integrity.sitTilt = evaluationSnapshot.tiltDegrees; this.integrity.sitShock = evaluationSnapshot.peakShock; this.integrity.isBatteryLow = evaluationSnapshot.isBatteryLow; this.integrity.isBatteryCritical = evaluationSnapshot.isBatteryCritical; this.integrity.locationPendingReason = evaluationSnapshot.locationPendingReason; this.integrity.isPowerSaveMode = evaluationSnapshot.isPowerSaveMode; this.integrity.standbyBucket = evaluationSnapshot.standbyBucket; this.integrity.netInterface = evaluationSnapshot.netInterface; this.integrity.isStorageLow = evaluationSnapshot.isStorageLow; this.integrity.isStorageCritical = evaluationSnapshot.isStorageCritical; this.integrity.isBatterySteepDischarge = evaluationSnapshot.isBatterySteepDischarge; this.integrity.isCoolingModeActive = evaluationSnapshot.isCoolingModeActive; this.integrity.gpsHardwareLock = evaluationSnapshot.isGpsHardwareLock; this.integrity.isUltraLongStationary = evaluationSnapshot.isUltraLongStationary; this.integrity.isTamperDetected = proc?.tamperDetected ?: false; this.integrity.tamperNote = proc?.suppressionNote
                this.ts = now; this.isMe = true; this.status = proc?.status ?: SentinelStatus.VALID; this.lastValidFixRt = primaryProcessor.getLastValidFixRt(); this.trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING
            })
            if (isPeerActive) {
                connectivitySuite.pushCurrentStatus(deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = null, filtered = proc?.optimizedPoint, distToTracker = null, distToHome = proc?.distToHome, maxAccuracy = proc?.maxAccuracy ?: 0.0, filteredSpeed = proc?.filteredSpeed ?: 0.0, vibration = evaluationSnapshot.vibration, heading = evaluationSnapshot.heading, baroAlt = evaluationSnapshot.baroAlt, lux = evaluationSnapshot.lux, isNear = evaluationSnapshot.isNear, tiltDegrees = evaluationSnapshot.tiltDegrees, acousticDb = evaluationSnapshot.acousticDb, jumpTier = proc?.jumpTier ?: 0, isJammer = proc?.jammerDetected ?: false, isStalled = evaluationSnapshot.isStalled, peakShock = evaluationSnapshot.peakShock, peakShockTs = now, luxBaseline = primaryProcessor.getLuxBaseline(), acousticFloorDb = primaryProcessor.getAcousticFloorDb(), adaptiveVibrationFloor = primaryProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx, proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs, vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false, isTamperDetected = proc?.tamperDetected ?: false, isPowerTamper = evaluationSnapshot.isPowerTamper, isSitDetected = isSuspiciousMode, isSitActive = false, lastSitTs = primaryProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = evaluationSnapshot.peakVerticalVelocity, sitVz = evaluationSnapshot.peakVerticalVelocity, sitVzTs = evaluationSnapshot.peakVerticalVelocityTs, sitVzRt = evaluationSnapshot.peakVerticalVelocityRt, sitDz = evaluationSnapshot.peakVerticalDisplacement, sitBaro = evaluationSnapshot.peakVerticalDisplacement, sitTilt = evaluationSnapshot.tiltDegrees, sitShock = evaluationSnapshot.peakShock, isClockRegression = proc?.isClockRegression ?: false, isLocationPending = evaluationSnapshot.isLocationPending, locationPendingReason = evaluationSnapshot.locationPendingReason, lastValidFixRt = evaluationSnapshot.lastValidFixRt, gnssDetail = latestGnssDetail, snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, isBatterySteepDischarge = proc?.isClockRegression ?: false, isCoolingModeActive = evaluationSnapshot.isCoolingModeActive, batteryLevel = evaluationSnapshot.batteryLevel, temp = evaluationSnapshot.batteryTemp, isCharging = evaluationSnapshot.isCharging, trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, status = proc?.status ?: SentinelStatus.VALID, isStorageLow = evaluationSnapshot.isStorageLow, isStorageCritical = evaluationSnapshot.isStorageCritical, isPowerSaveMode = evaluationSnapshot.isPowerSaveMode, standbyBucket = evaluationSnapshot.standbyBucket, netInterface = evaluationSnapshot.netInterface, kineticEnergy = evaluationSnapshot.kineticEnergy, isAdaptiveJump = proc?.isAdaptiveJump ?: false, isBatteryLow = evaluationSnapshot.isBatteryLow, isBatteryCritical = evaluationSnapshot.isBatteryCritical, isUltraLongStationary = evaluationSnapshot.isUltraLongStationary, gpsHardwareLock = evaluationSnapshot.isGpsHardwareLock, tamperNote = proc?.suppressionNote)
            }
        }

        historyManager.updateRibbons(now = now, nowRt = nowRt, lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime, serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), peerSignal = if (isPeerActive) 10 else 0, peerAvail = isSocketConnected && isPeerActive, hasGps = (proc?.timestamp ?: 0L) > 0, isTrackerMode = isTrackerMode, accuracy = proc?.currentAccuracy ?: 0.0, maxAccuracy = proc?.maxAccuracy ?: 0.0, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, proxIdx = snapshot.proximityIdx, liftIdx = liftIdx, snrIdx = snrIdx, tiltIdx = tiltIdx, baroIdx = baroIdx, verticalVelocity = evaluationSnapshot.peakVerticalVelocity, sitVz = evaluationSnapshot.peakVerticalVelocity, sitVzTs = evaluationSnapshot.peakVerticalVelocityTs, sitVzRt = evaluationSnapshot.peakVerticalVelocityRt, sitDz = evaluationSnapshot.peakVerticalDisplacement, sitBaro = evaluationSnapshot.peakVerticalDisplacement, sitTilt = evaluationSnapshot.tiltDegrees, sitShock = evaluationSnapshot.peakShock, isBatterySteepDischarge = evaluationSnapshot.isBatterySteepDischarge, isCoolingModeActive = evaluationSnapshot.isCoolingModeActive, speed = proc?.filteredSpeed ?: 0.0, bearing = lastGpsBearing, isSitDetected = if (isTrackerMode) isSuspiciousMode else false, isSitActive = false, currentMa = evaluationSnapshot.currentMa, locationPendingReason = evaluationSnapshot.locationPendingReason, kineticEnergy = evaluationSnapshot.kineticEnergy, isRecoveryEvent = recoveryFlagged, cpuLoad = evaluationSnapshot.cpuLoad, ioWait = evaluationSnapshot.ioWait, maxIoLatency = evaluationSnapshot.maxIoLatency, isSilentFailure = evaluationSnapshot.isSilentFailure, isBatteryLow = evaluationSnapshot.isBatteryLow, isBatteryCritical = evaluationSnapshot.isBatteryCritical, isUltraLongStationary = evaluationSnapshot.isUltraLongStationary)

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(rolePrefix + LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(rolePrefix + LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        serviceTickCounter++
        triggerForensicSample()
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSocketConnected: Boolean, isPeerActive: Boolean, processed: ProcessedLocation, snapshot: HardwareSuite.ForensicSnapshot, rawGpsTs: Long, evaluationSnapshot: SystemEvaluationSnapshot) {
        val finalSnapshot = if (isTrackerMode) {
            evaluationSnapshot.copy(
                status = processed.status, isJammer = processed.jammerDetected, jumpTier = processed.jumpTier, isAdaptiveJump = processed.isAdaptiveJump,
                lat = processed.optimizedPoint.lat, lng = processed.optimizedPoint.lng, accuracy = processed.currentAccuracy, maxAccuracy = processed.maxAccuracy,
                gpsTs = rawGpsTs, lastValidFixRt = primaryProcessor.getLastValidFixRt(), speed = processed.filteredSpeed, tamperDetected = processed.tamperDetected,
                suppressionNote = processed.suppressionNote, vibeSnapshot = snapshot.vibration, snrSnapshot = hardwareSuite.averageSnr
            )
        } else {
            val s = connectivitySuite.trackerStatus
            evaluationSnapshot.copy(
                status = s.status, isJammer = s.isJammer, jumpTier = s.jumpTier, isAdaptiveJump = s.isAdaptiveJump,
                lat = s.lat, lng = s.lng, accuracy = s.accuracy, maxAccuracy = s.maxAccuracy, gpsTs = s.gpsTs, lastValidFixRt = s.lastValidFixRt,
                speed = s.speed, batteryLevel = s.battery, batteryTemp = s.temp, currentMa = s.currentMa, isLocationPending = s.isLocationPending,
                locationPendingReason = s.locationPendingReason, tamperDetected = s.isTamperDetected, isPowerTamper = s.isPowerTamper,
                tiltDegrees = s.tiltDegrees, acousticDb = s.acousticDb, baroAlt = s.baroAlt, baroAltEma = s.sitBaro, lux = s.lux, isNear = s.isNear,
                luxBaseline = s.luxBaseline, acousticFloorDb = s.acousticFloorDb, adaptiveVibrationFloor = s.adaptiveVibrationFloor,
                peakShock = s.peakVibrationShock, isPowerSaveMode = s.isPowerSaveMode, standbyBucket = s.standbyBucket, netInterface = s.netInterface,
                isStorageLow = s.isStorageLow, isStorageCritical = s.isStorageCritical, isBatterySteepDischarge = s.isBatterySteepDischarge,
                isCoolingModeActive = s.isCoolingModeActive, isGpsHardwareLock = s.gpsHardwareLock, suppressionNote = s.tamperNote,
                isGpsStalling = s.isStalled, isGpsGap = s.isClockRegression || (nowRt - s.lastValidFixRt > GPS_GAP_THRESHOLD_MS),
                snrSnapshot = s.snrIdx * RIBBON_SNR_SCALE_DB, vibeSnapshot = s.vibeIdx * RIBBON_VIBRATION_SCALE_G
            )
        }

        val serviceContext = AlarmServiceContext(now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = isTrackerMode, isRelayConnected = isSocketConnected, isTrackerConnected = if (isTrackerMode) true else isPeerActive, isUiVisible = isUiVisible(), distToHomeAuthority = if (isTrackerMode) processed.distToHome else (if (isSocketConnected && isPeerActive) PhysicsUtils.calculateDistance(finalSnapshot.lat, finalSnapshot.lng, (repository.getCachedHomePoints().firstOrNull()?.latitude ?: 0.0), (repository.getCachedHomePoints().firstOrNull()?.longitude ?: 0.0)) else null), maxDistanceAuthority = (if (isTrackerMode) primaryProcessor else remoteProcessor).getMaxDistanceAuthority(), capabilities = capabilities, rolePrefix = if (isTrackerMode) "T_" else "VR_")
        alarmEvalJob?.cancel(); alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) { alarmManager.evaluateAlarms(finalSnapshot, serviceContext) }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel(); gnssDetailJob?.cancel(); revivalEventsJob?.cancel(); settingsJob?.cancel(); alarmEvalJob?.cancel(); forensicSamplingJob?.cancel()
        deviceProfileManager.teardownHardwareProfile(capabilities); super.onDestroy()
    }

    override suspend fun onHeartbeat(now: Long, nowRt: Long) {
        if (isSystemActive) {
            val health = integrityMonitor.currentHealth
            notificationManager.updatePulse(sats = hardwareSuite.satellitesUsed, battery = health.batteryLevel, isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = isPowerSaveActive || health.isPowerSaveMode)
        }
    }

    private suspend fun performForensicCapture(isSpike: Boolean) = forensicCaptureMutex.withLock {
        val health = integrityMonitor.currentHealth
        val proc = lastProcessedLocation
        val snapshot = hardwareSuite.consumeForensicSnapshot()
        val lat = proc?.optimizedPoint?.lat ?: 0.0; val lng = proc?.optimizedPoint?.lng ?: 0.0; val vibe = snapshot.vibration; val tilt = snapshot.tiltDegrees
        val dist = if (lastForensicLat != 0.0) PhysicsUtils.calculateDistance(lastForensicLat, lastForensicLng, lat, lng) else Double.MAX_VALUE
        if (isSpike || dist > FORENSIC_SPATIAL_GATE_METERS || abs(vibe - lastForensicVibe) > FORENSIC_IMU_VIBRATION_THRESHOLD || abs(tilt - lastForensicTilt) > FORENSIC_IMU_TILT_THRESHOLD) {
            lastForensicLat = lat; lastForensicLng = lng; lastForensicVibe = vibe; lastForensicTilt = tilt
            logManager.logForensicTraceOptimized(timestamp = timeProvider.currentTimeMillis(), lat = lat, lng = lng, accuracy = proc?.currentAccuracy ?: 0.0, maxAccuracy = proc?.maxAccuracy ?: 0.0, vibe = vibe, snr = snapshot.acousticDb, batteryLevel = health.batteryLevel, isCharging = health.isCharging, batteryTemp = health.batteryTemp)
        }
    }

    private fun startForensicSamplingLoop() {
        forensicSamplingJob?.cancel(); forensicSamplingJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            initializationDeferred.await(); delay(STARTUP_SETTLING_DELAY_MS); triggerForensicSample(); var cachedCoolingEnteredRt = 0L
            while (isActive) {
                val health = integrityMonitor.currentHealth
                if (lastWasCooling && !health.isCoolingModeActive) {
                    val entryRt = if (cachedCoolingEnteredRt > 0) cachedCoolingEnteredRt else health.coolingEnteredRt
                    if (entryRt > 0) logManager.logServiceEvent(m = "Forensic Performance Audit ${if (isTrackerMode) "" else "(V)"}: Thermal Recovery Latency: ${timeProvider.elapsedRealtime() - entryRt}ms", isImportant = true)
                    cachedCoolingEnteredRt = 0L
                }
                if (health.isCoolingModeActive && !lastWasCooling) cachedCoolingEnteredRt = health.coolingEnteredRt
                lastWasCooling = health.isCoolingModeActive
                val delayMs = when { health.isCoolingModeActive -> FORENSIC_SAMPLING_INTERVAL_COOLING_MS; logManager.isForensicBufferUnderPressure() -> FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS; health.isCharging -> FORENSIC_SAMPLING_INTERVAL_MIN_MS; else -> FORENSIC_SAMPLING_INTERVAL_MAX_MS }
                val trigger = withTimeoutOrNull(delayMs) { forensicTriggerChannel.receive() }
                performForensicCapture(isSpike = (trigger == true))
            }
        }
    }

    private fun triggerForensicSample(isSpike: Boolean = false) { forensicTriggerChannel.trySend(isSpike) }

    private fun setupPhysicalFastPaths() {
        hardwareSuite.setAcousticFastPath(floor = primaryProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0, onSpike = { lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime(); triggerForensicSample(isSpike = true) })
        hardwareSuite.setLightFastPath(baseline = primaryProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP, onSpike = { lastFastPathLightSpikeTs = timeProvider.elapsedRealtime(); triggerForensicSample(isSpike = true) })
    }

    private suspend fun refreshCapabilitiesInternal() {
        val perms = systemStatusProvider.getPermissionState(forceRefresh = true)
        capabilities = HardwareCapabilities(hasBackgroundRestriction = perms.hasBackgroundRestriction, backgroundStatus = perms.backgroundStatus, autostartStatus = perms.autostartStatus, requiresWakeLockRenewal = perms.requiresWakeLockRenewal, requiresExtraTopPadding = perms.requiresExtraTopPadding, isManualOverrideActive = perms.isManualOverride, isA15Device = perms.isA15Device, isSamsungDevice = perms.isSamsungDevice, isHuaweiDevice = perms.isHuaweiDevice, isMicrophoneGranted = perms.isMicrophoneGranted, performanceTier = perms.performanceTier)
    }

    private var lastKnownLocation: Location? = null
    private var lastGpsSpeed = 0.0

    private fun executeAutomatedStressTest() {
        lifecycleScope.launch(Dispatchers.Default) {
            logManager.logServiceEvent(m = "FORENSIC STRESS TEST: Initiating 5s CPU/IO saturation burst.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
            val cpuJob = launch(Dispatchers.Default) { val end = System.currentTimeMillis() + 5000L; var count = 0L; while (System.currentTimeMillis() < end) { sin(count.toDouble()); cos(count.toDouble()); sqrt(count.toDouble()); count++ }; logManager.logServiceEvent(m = "STRESS TEST: CPU Saturation complete ($count iterations).", isImportant = false) }
            val ioJob = launch(Dispatchers.IO) { val end = System.currentTimeMillis() + 5000L; val data = ByteArray(1024 * 1024) { 0xFF.toByte() }; val tempFile = File(cacheDir, "stress_test.tmp"); var writes = 0; while (System.currentTimeMillis() < end) { try { FileOutputStream(tempFile).use { fos -> fos.write(data); fos.flush() }; writes++ } catch (e: Exception) { Timber.e(e, "Stress Test IO failure") } }; tempFile.delete(); logManager.logServiceEvent(m = "STRESS TEST: IO Saturation complete ($writes MB written).", isImportant = false) }
            val forensicJob = launch(Dispatchers.Default) { repeat(500) { i -> logManager.logForensicTrace("STRESS_BURST: Forensic sample #$i injection."); if (i % 100 == 0) delay(1) }; logManager.logServiceEvent(m = "STRESS TEST: Forensic Saturation burst complete.", isImportant = false) }
            joinAll(cpuJob, ioJob, forensicJob); logManager.logServiceEvent(m = "FORENSIC STRESS TEST: Saturation routine COMPLETED.", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        }
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()
}
