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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * MonitorService: Unified role-reactive background service for Tracker and Viewer modes.
 * Oct.6.3:
 * - Issue #AUDIT-1006-2: Implemented triggerImmediateTick() to allow Fast-Path 
 *   triggers (Acoustic/Light) to preempt relaxed memory-throttled intervals.
 *   Ensures zero-latency alarm detection even during MemoryPressureLevel.HIGH/CRITICAL.
 * Oct.6.2:
 * - Issue #AUDIT-1006-6: Integrated memory pressure throttling in getRequiredTickInterval.
 *   Aggressively relaxes loops during MemoryPressureLevel.CRITICAL to prevent OOM.
 * - Issue #AUDIT-1006-5: Implemented executeLogPressureTest to simulate 100Hz 
 *   telemetry bursts for backpressure verification.
 */
@AndroidEntryPoint
class MonitorService : BaseMonitorService() {

    @Inject lateinit var sessionCoordinator: SessionLifecycleCoordinator
    @Inject lateinit var deviceProfileManager: DeviceProfileManager
    @Inject lateinit var eventCoordinator: AppEventCoordinator
    @Inject lateinit var domainEventBus: DomainEventBus

    private var activeMode: String? = null
    private var currentRole: AppRole = AppRole.TRACKER
    private var isTrackerMode: Boolean = true
    
    private val forensicTriggerChannel = Channel<Boolean>(Channel.BUFFERED)
    private val forensicCaptureMutex = Mutex()
    private val locationBuffer = ConcurrentLinkedQueue<Location>()

    private lateinit var primaryProcessor: LocationProcessor
    private lateinit var remoteProcessor: LocationProcessor

    private var lastProcessedLocation: ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null
    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var lastFastPathAcousticSpikeRt = 0L
    private var lastFastPathLightSpikeRt = 0L
    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L
    private var isSuspiciousMode = false
    private var currentIntervalMs = TICK_INTERVAL_MS
    
    private var memoryPressureLevel = MemoryPressureLevel.NORMAL

    private var lastForensicLat = 0.0
    private var lastForensicLng = 0.0
    private var lastForensicVibe = 0.0
    private var lastForensicTilt = 0.0
    private var lastWasCooling = false
    
    private var lastGpsBearing = 0.0
    private var lastGpsAccuracy = 0.0

    private var isManualJammerActive = false
    private var isManualStallActive = false

    override fun onServicePreInit() {
        runBlocking {
            activeMode = repository.getAppMode() ?: "tracker"
        }
        isTrackerMode = activeMode == "tracker"
        currentRole = if (isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_SELF
        notificationManager.setTrackerMode(isTrackerMode)
        
        primaryProcessor = LocationProcessor(timeProvider, domainEventBus, isPrimary = true)
        remoteProcessor = LocationProcessor(timeProvider, domainEventBus, isPrimary = false)
    }

    override suspend fun onServiceInitialize() {
        repository.saveLongSync(currentRole, LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        repository.saveLongSync(currentRole, LAST_SERVICE_TICK_REALTIME_KEY, timeProvider.elapsedRealtime())

        val trackerId = repository.getString(TRACKER_ID_KEY, SettingsRepository.DEFAULT_TRACKER_ID)
        val viewerId = repository.getString(VIEWER_ID_KEY, SettingsRepository.DEFAULT_VIEWER_ID)
        
        configManager.deviceId = trackerId
        configManager.viewerId = viewerId
        configManager.relayUrl = repository.getString(RELAY_URL_KEY, SettingsRepository.DEFAULT_RELAY_URL)
        configManager.isTrackerMode = isTrackerMode

        refreshCapabilitiesInternal()
        deviceProfileManager.initializeHardwareProfile(capabilities, configManager.deviceId)

        eventCoordinator.start(connectivitySuite = connectivitySuite)

        setupServiceObservers()

        connectivitySuite.updateRemoteProcessor(remoteProcessor)
        connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, isTrackerMode)
        
        loadLogicState()

        historyManager.initialize(lifecycleScope, currentRole)
        hardwareSuite.start()

        commandRouter.register()
        commandRouter.startObservingCommands(lifecycleScope)

        tickOrchestrator.launchJob("gps_collection", lifecycleScope + Dispatchers.Default) {
            hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) }
        }
        tickOrchestrator.launchJob("gnss_detail", lifecycleScope + Dispatchers.Default) {
            hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it }
        }

        val recoveredTs = repository.getLong(currentRole, LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
        val recoveredDrift = repository.getLong(currentRole, CLOCK_DRIFT_REF_KEY, 0L)
        
        lastServiceTickTs = recoveredTs
        lastServiceTickRealtime = historyManager.recoverLastRealtime(recoveredTs, recoveredDrift)
        
        primaryProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        remoteProcessor.setLastValidFixRt(timeProvider.elapsedRealtime())
        
        systemMonitor.setSessionStart(timeProvider.elapsedRealtime())

        if (isTrackerMode) setupPhysicalFastPaths()
        
        startTickLoop()
        startHeartbeatLoop()
        startForensicSamplingLoop()
        
        domainEventBus.emit(DomainEvent.ServiceStatus("${if (isTrackerMode) "Tracker" else "Viewer"} Engine Online (Unified)", isImportant = true))
    }

    private suspend fun loadLogicState() {
        val settingsSnapshot = repository.getSettingsSnapshot()
        val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
        val maxDist = repository.getDouble(MAX_DISTANCE_STORAGE_KEY, 60.0)

        val primaryState = repository.loadLocationUpdate(currentRole)
        primaryProcessor.loadState(
            savedMaxAccuracy = repository.getDouble(currentRole, MAX_ACCURACY_KEY, 0.0),
            savedLastSitTs = repository.getLong(currentRole, LAST_SIT_TS_KEY, 0L),
            savedBaseline = repository.getDouble(currentRole, CHAIR_BASELINE_TILT_KEY, -1000.0),
            trackerState = primaryState,
            homePoints = homePoints,
            maxDistance = maxDist,
            savedSitVz = primaryState?.integrity?.sitVz ?: 0.0,
            savedSitDz = primaryState?.integrity?.sitDz ?: 0.0,
            savedSitBaro = primaryState?.integrity?.sitBaro ?: 0.0,
            savedSitTilt = primaryState?.integrity?.sitTilt ?: 0.0,
            savedSitShock = primaryState?.integrity?.sitShock ?: 0.0,
            savedSitVzTs = primaryState?.integrity?.sitVzTs ?: 0L,
            savedSitVzRt = primaryState?.integrity?.sitVzRt ?: 0L,
            savedLastValidFixRt = repository.getLong(currentRole, LAST_VALID_FIX_RT_KEY, 0L),
            savedVibrationFloor = repository.getDouble(currentRole, ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0),
            savedLuxBaseline = repository.getDouble(currentRole, TRACKER_LUX_BASELINE_KEY, -1.0),
            savedAcousticFloor = repository.getDouble(currentRole, TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
        )

        val vibeFloor = repository.getDouble(currentRole, ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0)
        if (vibeFloor >= 0.0 && isTrackerMode) {
            hardwareSuite.setAdaptiveVibrationFloor(vibeFloor)
        }

        if (!isTrackerMode) {
            val remoteRole = AppRole.VIEWER_REMOTE
            val remoteState = repository.loadLocationUpdate(remoteRole)
            remoteProcessor.loadState(
                savedMaxAccuracy = repository.getDouble(remoteRole, MAX_ACCURACY_KEY, 0.0),
                savedLastSitTs = repository.getLong(remoteRole, LAST_SIT_TS_KEY, 0L),
                savedBaseline = repository.getDouble(remoteRole, CHAIR_BASELINE_TILT_KEY, -1000.0),
                trackerState = remoteState,
                homePoints = homePoints,
                maxDistance = maxDist,
                savedSitVz = remoteState?.integrity?.sitVz ?: 0.0,
                savedSitDz = remoteState?.integrity?.sitDz ?: 0.0,
                savedSitBaro = remoteState?.integrity?.sitBaro ?: 0.0,
                savedSitTilt = remoteState?.integrity?.sitTilt ?: 0.0,
                savedSitShock = remoteState?.integrity?.sitShock ?: 0.0,
                savedSitVzTs = remoteState?.integrity?.sitVzTs ?: 0L,
                savedSitVzRt = remoteState?.integrity?.sitVzRt ?: 0L,
                savedLastValidFixRt = remoteState?.lastValidFixRt ?: 0L,
                savedVibrationFloor = repository.getDouble(remoteRole, ADAPTIVE_VIBRATION_FLOOR_KEY, -1.0),
                savedLuxBaseline = repository.getDouble(remoteRole, TRACKER_LUX_BASELINE_KEY, -1.0),
                savedAcousticFloor = repository.getDouble(remoteRole, TRACKER_ACOUSTIC_FLOOR_KEY, -1.0)
            )
        }

        val alarmRole = if (isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_REMOTE
        alarmManager.restoreState(repository.loadActiveAlarms(alarmRole))
        alarmManager.restoreLogicState(settingsSnapshot, alarmRole)
    }

    private fun setupServiceObservers() {
        tickOrchestrator.launchJob("service_observers", lifecycleScope + Dispatchers.Default) {
            launch { observeConnectivityEvents() }
            launch { observeCommandEvents() }
            launch { observeSettingsChanges() }
            launch { observeIntegrityEvents() }
            launch {
                repository.appModeFlow.collectLatest { mode ->
                    if (mode != null && mode != activeMode) {
                        withContext(Dispatchers.Main) {
                            handleRoleTransition(mode)
                        }
                    }
                }
            }
            launch {
                combine(
                    repository.relayUrlFlow,
                    repository.trackerIdFlow,
                    repository.viewerIdFlow
                ) { url, tid, vid -> Triple(url, tid, vid) }
                .collectLatest { (url: String, tid: String, vid: String) ->
                    if (url != configManager.relayUrl || tid != configManager.deviceId || vid != configManager.viewerId) {
                        Timber.i("MonitorService: Connection settings changed. Updating engine.")
                        configManager.relayUrl = url
                        configManager.deviceId = tid
                        configManager.viewerId = vid
                        connectivitySuite.start(url, tid, vid, isTrackerMode)
                    }
                }
            }
        }
    }

    private suspend fun handleRoleTransition(newMode: String) {
        if (activeMode == newMode) return
        Timber.i("MonitorService: Handling dynamic role transition from $activeMode to $newMode")
        domainEventBus.emit(DomainEvent.ServiceStatus("Engine transitioning role: $activeMode -> $newMode", isImportant = true))

        tickOrchestrator.cancelJob("gps_collection")
        tickOrchestrator.cancelJob("gnss_detail")
        tickOrchestrator.cancelJob("alarm_evaluation")
        
        sessionCoordinator.resetSession(role = currentRole, processors = listOf(primaryProcessor, remoteProcessor), onReset = {
            locationBuffer.clear()
        })

        activeMode = newMode
        isTrackerMode = newMode == "tracker"
        currentRole = if (isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_SELF
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

        tickOrchestrator.launchJob("gps_collection", lifecycleScope + Dispatchers.Default) {
            hardwareSuite.getLocationFlow().collectLatest { onLocationChanged(it) }
        }
        tickOrchestrator.launchJob("gnss_detail", lifecycleScope + Dispatchers.Default) {
            hardwareSuite.gnssDetailFlow.collectLatest { latestGnssDetail = it }
        }

        if (isTrackerMode) {
            setupPhysicalFastPaths()
        } else {
            hardwareSuite.setLightFastPath(0.0, 0.0, null)
            hardwareSuite.setAcousticFastPath(0.0, 0.0, 0.0, null)
        }

        updateForegroundServiceType()
        domainEventBus.emit(DomainEvent.ServiceStatus("${if (isTrackerMode) "Tracker" else "Viewer"} Engine Online via Dynamic Switch", isImportant = true))
    }

    private suspend fun observeConnectivityEvents() {
        domainEventBus.events
            .filterIsInstance<ConnectivityEvent.PeerPulse>()
            .collect { event ->
                if (isTrackerMode) handleViewerPulse(event.id) else handleTrackerPulse(event.id)
            }
    }

    private suspend fun observeCommandEvents() {
        domainEventBus.events
            .filterIsInstance<CommandEvent>()
            .collect { event ->
                when (event) {
                    is CommandEvent.WatchdogTrigger -> { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                    is CommandEvent.UiPulse -> { lastUiPulseRt = timeProvider.elapsedRealtime(); updateForegroundServiceType() }
                    is CommandEvent.UiVisibilityChanged -> onUiVisibilityChangedInternal(event.visible)
                    is CommandEvent.ResetTimers -> resetServiceTimers()
                    is CommandEvent.SyncSensors -> { refreshCapabilitiesInternal(); lifecycleScope.launch { hardwareSuite.start() } }
                    is CommandEvent.ExecuteStressTest -> executeAutomatedStressTest()
                    is CommandEvent.ExecuteLogPressureTest -> executeLogPressureTest()
                    is CommandEvent.ExecuteNetworkStressTest -> connectivitySuite.executeFlappingStressTest()
                    is CommandEvent.SimulateStoragePressure -> {}
                    is CommandEvent.TriggerMemoryFlush -> performMemoryFlush()
                }
            }
    }
    
    private suspend fun observeIntegrityEvents() {
        domainEventBus.events
            .filterIsInstance<IntegrityEvent.MemoryPressureChanged>()
            .collect { event ->
                memoryPressureLevel = event.level
                if (event.level != MemoryPressureLevel.NORMAL) {
                    performMemoryFlush()
                }
            }
    }
    
    private fun performMemoryFlush() {
        Timber.w("Memory Flush Triggered: Pressure level $memoryPressureLevel. Executing aggressive recovery.")
        lifecycleScope.launch(Dispatchers.IO) {
            historyManager.trimMemory()
        }
        System.gc()
        System.runFinalization()
        System.gc()
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
        lastGpsBearing = location.bearing.toDouble()
        lastGpsAccuracy = location.accuracy.toDouble()
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
        if (isNew || !tickOrchestrator.isLoopActive("tick_loop")) {
            if (isNew) {
                domainEventBus.emit(DomainEvent.PeerConnectionChanged(isConnected = true, peerId = id))
            }
            startTickLoop() 
        }
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) return
        val nowRt = timeProvider.elapsedRealtime()
        if ((configManager.deviceId == SignalingConstants.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id; connectivitySuite.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch(Dispatchers.IO) { repository.saveString(TRACKER_ID_KEY, id) }
        }
        val isNew = sessionManager.onTrackerPulse(id, nowRt)
        if (isNew || !tickOrchestrator.isLoopActive("tick_loop")) {
            if (isNew) {
                domainEventBus.emit(DomainEvent.PeerConnectionChanged(isConnected = true, peerId = id))
            }
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        val processors = mutableListOf(primaryProcessor, remoteProcessor)
        
        sessionCoordinator.resetSession(role = currentRole, processors = processors, onReset = {
            serviceStartRealtime = timeProvider.elapsedRealtime()
            serviceStartWall = timeProvider.currentTimeMillis()
            lastHardwareRecoveryTs = 0L; lastForensicLat = 0.0; lastForensicLng = 0.0; lastForensicVibe = 0.0; lastForensicTilt = 0.0; lastWasCooling = false
            if (isTrackerMode) {
                lastFastPathAcousticSpikeRt = 0L; lastFastPathLightSpikeRt = 0L
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
            tickOrchestrator.launchJob("fgs_update", lifecycleScope + Dispatchers.Main.immediate) {
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

    /**
     * getRequiredTickInterval: Unified interval authority.
     * Issue #AUDIT-1006-6: Aggressive loop throttling during MemoryPressureLevel.CRITICAL.
     * Throttles to 15s when memory is critical to prevent background OOM (R-ID 592).
     */
    override fun getRequiredTickInterval(): Long {
        if (memoryPressureLevel == MemoryPressureLevel.CRITICAL) return 15000L
        if (memoryPressureLevel == MemoryPressureLevel.HIGH) return 5000L

        if (!isTrackerMode) return if (isUiVisible()) HIGH_FREQUENCY_GPS_POLLING_MS else VIEWER_GPS_POLLING_MS
        
        val health = integrityMonitor.currentHealth
        return when {
            health.isUltraLongStationary -> currentIntervalMs.coerceAtLeast(POWER_SAVE_TICK_INTERVAL_MS)
            isPowerSaveActive -> POWER_SAVE_TICK_INTERVAL_MS
            else -> TICK_INTERVAL_MS
        }
    }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt); integrityMonitor.checkInternetIntegrity(nowRt)
        val health = integrityMonitor.currentHealth; val hSnapshot = hardwareSuite.consumeLogicSnapshot()

        val isSocketConnected = connectivitySuite.isConnected(); connectivitySuite.updateRelayStatus(isSocketConnected)
        val isPeerActive = if (isTrackerMode) (sessionManager.getViewerCount() > 0 || isRecentUiPulse()) else (connectivitySuite.lastPeerActivityTs > 0 && (nowRt - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS))

        val evaluationSnapshot = EnginePools.LOCATION_UPDATE.acquire().apply {
            kinetic.apply {
                kineticEnergy = hSnapshot.kineticEnergy
                rt = nowRt
            }
            atmospheric.apply {
                vibration = hSnapshot.vibration; heading = hSnapshot.heading; baroAlt = hSnapshot.baroAlt
                lux = hSnapshot.lux; isNear = hSnapshot.isNear; tiltDegrees = hSnapshot.tiltDegrees 
                acousticDb = hSnapshot.acousticDb; peakVibrationShock = hSnapshot.peakShock 
                luxBaseline = primaryProcessor.getLuxBaseline(); acousticFloorDb = primaryProcessor.getAcousticFloorDb()
                adaptiveVibrationFloor = hSnapshot.adaptiveVibrationFloor
                proxIdx = hSnapshot.proximityIdx; proximityCm = hSnapshot.proximityCm 
                proximityDebounceMs = hSnapshot.proximityDebounceMs
                vibrationRollingSum = hSnapshot.vibrationRollingSum
            }
            integrity.apply {
                battery = health.batteryLevel; isCharging = health.isCharging; currentMa = health.currentMa
                isPowerTamper = health.isPowerTamper; isLocationPending = health.isLocationPending 
                locationPendingReason = health.locationPendingReason
                isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode; standbyBucket = health.standbyBucket 
                netInterface = health.netInterface; isStorageLow = health.isStorageLow 
                isStorageCritical = health.isStorageCritical; isBatterySteepDischarge = health.isBatterySteepDischarge
                isCoolingModeActive = health.isCoolingModeActive; gpsHardwareLock = health.gpsHardwareLock 
                isUltraLongStationary = health.isUltraLongStationary; isBatteryLow = health.isBatteryLow 
                isBatteryCritical = health.isBatteryCritical; satsUsed = hardwareSuite.satellitesUsed 
                satsView = hardwareSuite.satellitesInView; violationUptimeMs = sessionManager.violationUptimeMs
                violationPercentage = sessionManager.getViolationPercentage()
                thermalHeadroom = health.thermalHeadroom; heapAllocatedMb = health.heapAllocatedMb
                if (isManualJammerActive) isJammer = true
                if (isManualStallActive) isStalled = true
            }
            this.nowRt = nowRt; this.nowTs = now; snrSnapshot = hardwareSuite.averageSnr
            acousticLockoutRt = if (isTrackerMode) lastFastPathAcousticSpikeRt else 0L 
            lightSpikeRt = if (isTrackerMode) lastFastPathLightSpikeRt else 0L
            providedAdaptiveFloor = hSnapshot.adaptiveVibrationFloor
            acousticMinDb = hSnapshot.acousticPeakMin
            integrity.isSilentFailure = health.isSilentFailure; integrity.isMaliAnomaly = health.isMaliAnomaly
            localInternetLoss = health.localInternetLoss; isHardwareOnline = health.isHardwareOnline
            cpuLoad = health.cpuLoad; ioWait = health.ioWait; maxIoLatency = health.maxIoLatency
            kinetic.activityType = hSnapshot.activityType
        }
        
        if (isTrackerMode) {
            hardwareSuite.setLightFastPath(baseline = primaryProcessor.getLuxBaseline(), spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP)
            hardwareSuite.setAcousticFastPath(floor = primaryProcessor.getAcousticFloorDb(), spikeThreshold = 15.0, minDb = 40.0)
            hardwareSuite.setHighLoad(evaluationSnapshot.integrity.isCoolingModeActive)
            hardwareSuite.setCpuLoad(health.cpuLoad)
            isSuspiciousMode = serviceBehaviorUseCase.updateSuspiciousMode(isSuspiciousMode, primaryProcessor.checkPhysicalTamper(nowRt, false, health.cpuLoad) == SentinelStatus.TAMPER, primaryProcessor.consumeSitDetected(), nowRt)
            val targetGpsInterval = serviceBehaviorUseCase.calculateGpsInterval(evaluationSnapshot.integrity.isCoolingModeActive, isSuspiciousMode, hardwareSuite.isStationary(), hardwareSuite.isScreenOn(), primaryProcessor.getMaxDistanceAuthority() > 0.0, evaluationSnapshot.kinetic.activityType, nowRt, capabilities)
            if (targetGpsInterval != currentIntervalMs) {
                currentIntervalMs = targetGpsInterval; forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, currentRole); primaryProcessor.updateExpectedInterval(nowRt, targetGpsInterval); hardwareSuite.setPollingInterval(targetGpsInterval)
            }
        } else {
            val targetGpsInterval = if (isUiVisible()) HIGH_FREQUENCY_GPS_POLLING_MS else VIEWER_GPS_POLLING_MS
            if (targetGpsInterval != currentIntervalMs) {
                currentIntervalMs = targetGpsInterval; forensicAuditor.updateExpectedInterval(nowRt, targetGpsInterval, currentRole); primaryProcessor.updateExpectedInterval(nowRt, targetGpsInterval); hardwareSuite.setPollingInterval(targetGpsInterval)
            }
        }

        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isSocketConnected && isPeerActive, isInViolation = alarmManager.hasUnresolvedAlarms())
        deviceProfileManager.executeContinuityTweaks(capabilities, nowRt, serviceTickCounter, primaryProcessor.getLastValidFixRt(), evaluationSnapshot.integrity.isPowerSaveMode, evaluationSnapshot.localInternetLoss, isSocketConnected, isPeerActive)

        var recoveryFlagged = false
        if (lastServiceTickRealtime > 0) {
            val recoveryThreshold = if (capabilities.performanceTier == PerformanceTier.STAGGERED) 10000L else HARDWARE_SUPPRESSION_THRESHOLD_MS
            if (nowRt - lastServiceTickRealtime > recoveryThreshold && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt; recoveryFlagged = true
                val proc = lastProcessedLocation
                domainEventBus.emit(DomainEvent.HeuristicRecovery(message = "Gap Detected", gapMs = nowRt - lastServiceTickRealtime, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0))
                systemMonitor.acquireWakeLock(); connectivitySuite.connect(configManager.relayUrl)
            }
        }

        forensicAuditor.evaluateStability(nowRt, currentRole)?.let { verdict ->
            val proc = lastProcessedLocation
            domainEventBus.emit(DomainEvent.StabilityViolation(message = verdict.message, isJitter = verdict.isJitterViolation, lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy))
        }
        
        primaryProcessor.updateSensorData(evaluationSnapshot)

        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(hardwareSuite.isStationary(), evaluationSnapshot.integrity.isStalled, alarmManager.hasUnresolvedAlarms(), isUiVisible())
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave; hardwareSuite.setPowerSaveMode(shouldBePowerSave); 
                domainEventBus.emit(DomainEvent.PowerSaveTransition(shouldBePowerSave))
                withContext(Dispatchers.Main.immediate) { updateForegroundServiceType() }
            }
            lastPowerSaveCheckRt = nowRt
        }

        while (locationBuffer.isNotEmpty()) {
            val loc = locationBuffer.poll() ?: break
            forensicAuditor.recordGpsFix(nowRt, currentIntervalMs, currentRole)
            
            val pointSnapshot = EnginePools.LOCATION_UPDATE.acquire().apply {
                copyFrom(evaluationSnapshot)
                kinetic.apply {
                    lat = loc.latitude; lng = loc.longitude; alt = loc.altitude 
                    speed = loc.speed.toDouble(); gpsTs = loc.time 
                    accuracy = loc.accuracy.toDouble(); bearing = loc.bearing.toDouble()
                }
                isMuzzled = if (isTrackerMode) isSuspiciousMode else false
            }
            lastProcessedLocation = primaryProcessor.processGpsPoint(update = pointSnapshot, isViewerTrail = !isTrackerMode, lastGpsTs = sessionManager.lastGpsTs, isLocal = true)
            if (lastProcessedLocation?.isClockRegression == false) { sessionManager.lastGpsTs = loc.time }
            lastGpsBearing = loc.bearing.toDouble(); lastGpsAccuracy = loc.accuracy.toDouble()
        }

        val proc = lastProcessedLocation
        if (proc != null) {
            val stateManagerConnected = if (isTrackerMode) true else (isSocketConnected && isPeerActive)
            
            evaluationSnapshot.trackerState = if (isTrackerMode) {
                TrackerStateManager.updateState(status = proc.status, speed = proc.filteredSpeed, vibration = evaluationSnapshot.atmospheric.vibration, vibrationFloor = primaryProcessor.getAdaptiveVibrationFloor(), isTrackerConnected = stateManagerConnected, systemTimePulse = nowRt)
            } else TrackerState.UNKNOWN
            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isPeerActive, proc, hSnapshot, proc.timestamp, evaluationSnapshot)
        }

        evaluationSnapshot.atmospheric.apply {
            noiseIdx = (acousticDb - primaryProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
            luxIdx = log10(lux + 1.0) / RIBBON_LUX_LOG_SCALE
            vibeIdx = vibration / RIBBON_VIBRATION_SCALE_G
            liftIdx = (baroAlt - primaryProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
            tiltIdx = abs(tiltDegrees - primaryProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
            baroIdx = (baroAlt - primaryProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS
        }
        evaluationSnapshot.integrity.snrIdx = (latestGnssDetail?.satellites?.map { it.cn0 }?.safeAverage() ?: 0.0) / RIBBON_SNR_SCALE_DB

        domainEventBus.emit(DomainEvent.TickEvaluated(
            now = now, nowRt = nowRt, isTrackerMode = isTrackerMode, snapshot = evaluationSnapshot,
            processed = lastProcessedLocation, health = health, isSocketConnected = isSocketConnected, isPeerActive = isPeerActive,
            serviceTickCounter = serviceTickCounter, rtt = connectivitySuite.getRtt(), recoveryFlagged = recoveryFlagged,
            gnssDetail = latestGnssDetail, isSuspiciousMode = isSuspiciousMode,
            lastSitTs = primaryProcessor.getLastSitTs(), lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime
        ))

        lastServiceTickTs = now; lastServiceTickRealtime = nowRt
        repository.saveLongSync(currentRole, LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(currentRole, LAST_SERVICE_TICK_REALTIME_KEY, nowRt)
        serviceTickCounter++
        triggerForensicSample()
    }

    private fun evaluateAlarmsInternal(now: Long, nowRt: Long, isSocketConnected: Boolean, isPeerActive: Boolean, processed: ProcessedLocation, hSnapshot: HardwareSuite.ForensicSnapshot, rawGpsTs: Long, evaluationSnapshot: LocationUpdate) {
        val alarmSnapshot = EnginePools.LOCATION_UPDATE.acquire()
        if (isTrackerMode) {
            TelemetryMapper.mapProcessedToSnapshot(snapshot = evaluationSnapshot, processed = processed, rawGpsTs = rawGpsTs, lastValidFixRt = primaryProcessor.getLastValidFixRt(), snrSnapshot = hardwareSuite.averageSnr, out = alarmSnapshot)
        } else {
            TelemetryMapper.mapStatusToSnapshot(s = connectivitySuite.trackerStatus, base = evaluationSnapshot, nowRt = nowRt, out = alarmSnapshot)
        }

        val serviceContext = AlarmServiceContext(now = now, nowRt = nowRt, serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = isTrackerMode, isRelayConnected = isSocketConnected, isTrackerConnected = if (isTrackerMode) true else isPeerActive, isUiVisible = isUiVisible(), distToHomeAuthority = if (isTrackerMode) processed.distToHome else (if (isSocketConnected && isPeerActive) PhysicsUtils.calculateDistance(alarmSnapshot.kinetic.lat, alarmSnapshot.kinetic.lng, (repository.getCachedHomePoints().firstOrNull()?.latitude ?: 0.0), (repository.getCachedHomePoints().firstOrNull()?.longitude ?: 0.0)) else null), maxDistanceAuthority = (if (isTrackerMode) primaryProcessor else remoteProcessor).getMaxDistanceAuthority(), capabilities = capabilities, role = if (isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_REMOTE)
        tickOrchestrator.launchJob("alarm_evaluation", lifecycleScope + Dispatchers.Default) {
            // R1160: Alarms now use a pooled snapshot. Note: evaluateAlarms should NOT 
            // store this reference long-term as it belongs to a circular pool.
            alarmManager.evaluateAlarms(alarmSnapshot, serviceContext)
        }
    }

    override fun onDestroy() {
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
        tickOrchestrator.launchLoop("forensic_sampling_loop", lifecycleScope + serviceExceptionHandler) {
            delay(STARTUP_SETTLING_DELAY_MS); triggerForensicSample(); var cachedCoolingEnteredRt = 0L
            while (isActive) {
                val health = integrityMonitor.currentHealth
                if (lastWasCooling && !health.isCoolingModeActive) {
                    val entryRt = if (cachedCoolingEnteredRt > 0) cachedCoolingEnteredRt else health.coolingEnteredRt
                    if (entryRt > 0) domainEventBus.emit(DomainEvent.ServiceStatus("Forensic Performance Audit ${if (isTrackerMode) "" else "(V)"}: Thermal Recovery Latency: ${timeProvider.elapsedRealtime() - entryRt}ms", isImportant = true))
                    cachedCoolingEnteredRt = 0L
                }
                if (health.isCoolingModeActive && !lastWasCooling) cachedCoolingEnteredRt = health.coolingEnteredRt
                lastWasCooling = health.isCoolingModeActive
                
                val pressureIntervalMult = when (memoryPressureLevel) {
                    MemoryPressureLevel.CRITICAL -> 4.0
                    MemoryPressureLevel.HIGH -> 2.0
                    else -> 1.0
                }
                
                val delayMs = when { 
                    health.isCoolingModeActive -> FORENSIC_SAMPLING_INTERVAL_COOLING_MS 
                    logManager.isForensicBufferUnderPressure() -> FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS 
                    health.isCharging -> FORENSIC_SAMPLING_INTERVAL_MIN_MS 
                    health.isUltraLongStationary -> 5000L
                    else -> FORENSIC_SAMPLING_INTERVAL_MAX_MS 
                }
                
                val finalDelayMs = (delayMs * pressureIntervalMult).toLong()
                
                val trigger = withTimeoutOrNull(finalDelayMs) { forensicTriggerChannel.receive() }
                performForensicCapture(isSpike = (trigger == true))
            }
        }
    }

    private fun triggerForensicSample(isSpike: Boolean = false) { forensicTriggerChannel.trySend(isSpike) }

    /**
     * Issue #AUDIT-1006-2: Preempts the current tick loop delay to process 
     * a tick immediately. Used for safety-critical sensor spikes.
     */
    private fun triggerImmediateTick() {
        tickOrchestrator.preemptLoop("tick_loop")
    }

    private fun setupPhysicalFastPaths() {
        hardwareSuite.setAcousticFastPath(
            floor = primaryProcessor.getAcousticFloorDb(), 
            spikeThreshold = 15.0, 
            minDb = 40.0, 
            onSpike = { 
                lastFastPathAcousticSpikeRt = timeProvider.elapsedRealtime()
                triggerForensicSample(isSpike = true)
                // R-ID 289: Force immediate tick to evaluate potential alarm
                triggerImmediateTick()
            }
        )
        hardwareSuite.setLightFastPath(
            baseline = primaryProcessor.getLuxBaseline(), 
            spikeThreshold = LIGHT_THRESHOLD_LUX_JUMP, 
            onSpike = { 
                lastFastPathLightSpikeRt = timeProvider.elapsedRealtime()
                triggerForensicSample(isSpike = true)
                // R-ID 289: Force immediate tick to evaluate potential alarm
                triggerImmediateTick()
            }
        )
    }

    private suspend fun refreshCapabilitiesInternal() {
        val perms = systemStatusProvider.getPermissionState(forceRefresh = true)
        capabilities = HardwareCapabilities(hasBackgroundRestriction = perms.hasBackgroundRestriction, backgroundStatus = perms.backgroundStatus, autostartStatus = perms.autostartStatus, requiresWakeLockRenewal = perms.requiresWakeLockRenewal, requiresExtraTopPadding = perms.requiresExtraTopPadding, isManualOverrideActive = perms.isManualOverride, isA15Device = perms.isA15Device, isSamsungDevice = perms.isSamsungDevice, isHuaweiDevice = perms.isHuaweiDevice, isMicrophoneGranted = perms.isMicrophoneGranted, performanceTier = perms.performanceTier)
    }

    private var lastKnownLocation: Location? = null
    private var lastGpsSpeed = 0.0

    private fun executeAutomatedStressTest() {
        lifecycleScope.launch(Dispatchers.Default) {
            isManualJammerActive = true; isManualStallActive = true; repository.setForensicStallSimulation(true)
            val cpuOrder = launch(Dispatchers.Default) { val end = System.currentTimeMillis() + 10000L; var count = 0L; while (System.currentTimeMillis() < end) { sin(count.toDouble()); cos(count.toDouble()); sqrt(count.toDouble()); count++ }; domainEventBus.emit(DomainEvent.ServiceStatus("STRESS TEST: CPU Saturation complete ($count iterations).")) }
            val ioJob = launch(Dispatchers.IO) { val end = System.currentTimeMillis() + 10000L; val data = ByteArray(1024 * 1024) { 0xFF.toByte() }; val tempFile = File(cacheDir, "stress_test.tmp"); var writes = 0; while (System.currentTimeMillis() < end) { try { FileOutputStream(tempFile).use { fos -> fos.write(data); fos.flush() }; writes++ } catch (e: Exception) { Timber.e(e, "Stress Test IO failure") } }; tempFile.delete(); domainEventBus.emit(DomainEvent.ServiceStatus("STRESS TEST: IO Saturation complete ($writes MB written).")) }
            val forensicJob = launch(Dispatchers.Default) { repeat(1000) { i -> logManager.logForensicTrace("STRESS_BURST: Forensic sample #$i injection."); if (i % 100 == 0) delay(1) }; domainEventBus.emit(DomainEvent.ServiceStatus("STRESS TEST: Forensic Saturation burst complete.")) }
            joinAll(cpuOrder, ioJob, forensicJob)
            delay(40000); isManualJammerActive = false; isManualStallActive = false; repository.setForensicStallSimulation(false)
        }
    }

    private fun executeLogPressureTest() {
        lifecycleScope.launch(Dispatchers.Default) {
            domainEventBus.emit(DomainEvent.ServiceStatus("STRESS TEST: Starting Log Pressure Burst (100Hz)...", isImportant = true))
            val start = System.currentTimeMillis()
            repeat(1000) { i ->
                logManager.submitToLogSink(
                    message = "STRESS_LOG: High-frequency burst sample #$i",
                    type = "STRESS",
                    isImportant = i % 100 == 0
                )
                delay(10)
            }
            val duration = System.currentTimeMillis() - start
            domainEventBus.emit(DomainEvent.ServiceStatus("STRESS TEST: Log Pressure Burst complete ($duration ms).", isImportant = true))
        }
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()
}
