package com.gps19.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.*

/**
 * ViewerService: Background monitoring for the Viewer role.
 * v8.8.27: Modularization Phase 2 - Delegated Forensic Marking to ServiceForensicUseCase.
 * v8.8.31 (Identity Propagation): Now passing identity to LocationProcessor for forensic marking.
 * v8.8.32: Fixed forensic marking for VISUAL_JUMP to ensure persistent map markers for tracker jumps.
 * v8.8.34: Forensic Simplification - Removed peer version tracking from history manager.
 * v8.8.35: Updated to latest baseline following database schema cleanup (Issue 159).
 */
@AndroidEntryPoint
class ViewerService : BaseMonitorService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var systemStatusProvider: SystemStatusProvider
    @Inject lateinit var forensicUseCase: ServiceForensicUseCase

    private lateinit var integrityMonitor: IntegrityMonitor
    private lateinit var alarmManager: AppAlarmManager
    private lateinit var remoteHandler: RemoteHandler
    private lateinit var syncManager: SyncManager
    private lateinit var commandRouter: CommandRouter
    private lateinit var locationProcessor: LocationProcessor
    private lateinit var historyManager: HistoryManager
    
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    
    private var isXiaomiManualOverride = false

    private val localProcessorListener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean) {}
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean) {}
        override fun onMaxAccuracyChanged(accuracy: Float) {}
        override fun onChairBaselineChanged(baseline: Float) {}
        override fun onGpsStallDetected(ts: Long) {}
    }

    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch(serviceExceptionHandler) {
            val trackerId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            val viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            
            configManager.deviceId = trackerId
            configManager.viewerId = viewerId
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, DEFAULT_RELAY_URL)
            configManager.isTrackerMode = false

            alarmManager = AppAlarmManager(this@ViewerService, repository, sessionManager, notificationManager, timeProvider) { type, msg, important, extreme, logId, durationMs, special, color -> 
                logManager.submitToLogSink(msg, type, important, extreme, logId, durationMs, special, color)
            }

            integrityMonitor = IntegrityMonitor(this@ViewerService, repository, timeProvider, onViolationSustained = { }, onLogEvent = { msg, important ->
                logManager.logServiceEvent(msg, important)
            })
            
            locationProcessor = LocationProcessor(localProcessorListener, timeProvider)

            networkManager.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
            
            syncManager = SyncManager(this@ViewerService, networkManager, sessionManager, null, null, locationProcessor, telemetryRepository, offlineRepository, logManager, timeProvider, lifecycleScope)
            syncManager.startSyncLoop(configManager.deviceId, configManager.viewerId, false)

            remoteHandler = RemoteHandler(this@ViewerService, repository, locationProcessor, alarmManager, sessionManager, timeProvider, lifecycleScope) { id -> handleTrackerPulse(id) }

            historyManager = HistoryManager(this@ViewerService, repository, null, null, locationProcessor, timeProvider, lifecycleScope) { msg, important -> logManager.logServiceEvent(msg, important) }

            commandRouter = CommandRouter(
                this@ViewerService, configManager, logManager, networkManager, alarmManager, notificationManager, 
                sessionManager, locationProcessor, 
                remoteHandler, 
                repository, syncManager, integrityMonitor, timeProvider,
                { handleTrackerPulse(it) }, 
                { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }, 
                { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }, 
                { onUiVisibilityChangedInternal(it) }, 
                { transientDropDetected.set(it) }, 
                { resetServiceTimers() }, 
                { /* sensorManager.start() - N/A for Viewer */ }
            )
            commandRouter.register()
            commandRouter.startObservingCommands(lifecycleScope)

            EntryPointAccessors.fromApplication(applicationContext, GpsApplication.GpsApplicationEntryPoint::class.java)
                .networkManagerWrapper().setCallback { data -> 
                    remoteHandler.handleRemoteUpdate(data, false)
                }
            
            settingsJob = lifecycleScope.launch {
                launch {
                    repository.alertSettingsFlow.collect { settings ->
                        alarmManager.updateSettings(settings)
                    }
                }
                launch {
                    repository.isXiaomiManualOverrideFlow.collect { override ->
                        isXiaomiManualOverride = override
                    }
                }
            }

            val recoveredTs = repository.getLong(MainRepository.LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())
            lastServiceTickTs = recoveredTs
            lastServiceTickRealtime = timeProvider.elapsedRealtime()
            
            startTickLoop()
            logManager.logServiceEvent("Viewer Engine Online", important = true)
        }
    }

    private fun handleTrackerPulse(id: String) {
        if ((configManager.deviceId == MainRepository.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id
            networkManager.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch { repository.saveString(MainRepository.TRACKER_ID_KEY, id) } 
        }
        if (sessionManager.onTrackerPulse(id, timeProvider.currentTimeMillis(), false)) {
            logManager.logServiceEvent("Tracker connected: $id")
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        serviceStartRealtime = timeProvider.elapsedRealtime()
        alarmManager.resetEvaluation()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        logManager.logServiceEvent("Session Terminated", false)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    @SuppressLint("InlinedApi")
    override fun startServiceForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        val msg = "Monitoring system active."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
        } else {
            startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg))
        }
    }

    @SuppressLint("InlinedApi")
    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
                delay(200)
                val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                val msg = "Monitoring system active."
                startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
            }
        }
    }
    
    override fun getRequiredTickInterval(): Long {
        return if (isUiVisible()) TICK_INTERVAL_MS else TICK_INTERVAL_SLOW_MS
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)

        val isSocketConnected = networkManager.isConnected() && !transientDropDetected.getAndSet(false)
        networkManager.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = remoteHandler.lastPeerActivityTs > 0 && (nowRealtime - remoteHandler.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRealtime, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isTrackerActive, isInViolation = false)

        val silenceDelta = if (remoteHandler.lastPeerActivityTs > 0) nowRealtime - remoteHandler.lastPeerActivityTs else 0L
        val isSignalLoss = integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, false)
        val isTrackerJammer = remoteHandler.isTrackerJammerSuspicion
        val isTrackerStalled = remoteHandler.trackerGpsStallStartTs > 0L && (nowRealtime - remoteHandler.trackerGpsStallStartTs > GPS_STALL_THRESHOLD_MS)
        val isTrackerGap = remoteHandler.trackerLastValidFixRealtime > 0L && (nowRealtime - remoteHandler.trackerLastValidFixRealtime > GPS_GAP_THRESHOLD_MS)

        // Forensic Marking for Viewer - Delegated to UseCase
        if (remoteHandler.trackerLat != 0.0 && remoteHandler.trackerLng != 0.0) {
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes()
            val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isTrackerJammer) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isTrackerStalled) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isTrackerGap) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (remoteHandler.isTrackerVisualJump) activeViolations.add(ALERT_ID_VISUAL_JUMP)

            forensicUseCase.recordViolationMarkers(
                now = now,
                lat = remoteHandler.trackerLat,
                lng = remoteHandler.trackerLng,
                accuracy = remoteHandler.trackerMaxAccuracy.toDouble(),
                activeViolations = activeViolations,
                unresolvedAlarms = unresolvedAlarms
            )
        }

        syncManager.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = false, loc = null, filtered = null,
            distToTracker = 0.0, distToHome = 0.0, maxAccuracy = 0f, filteredSpeed = 0.0, vibration = 0f, heading = 0f, baroAlt = 0f,
            lux = 0f, isNear = true, isSuspicious = false, tiltDegrees = 0f, acousticDb = 0.0, isJump = false, isTrajectoryPromoted = false,
            jumpTier = 0, isJammer = false, isStalledRaw = false, isStalledActive = false, peakShock = 0f, peakShockTs = 0L, luxBaseline = 0f,
            acousticFloorDb = 0.0, adaptiveVibrationFloor = 0.12f, proxIdx = 0f, proximityCm = 0f, micPending = false, isTamperDetected = false,
            isPowerTamper = integrityMonitor.isPowerTamperDetected, isSitDetected = false, isSitActive = false, lastSitTs = 0L, receiptRealtime = 0L,
            violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(),
            verticalVelocity = 0f, sitVz = 0f, sitDz = 0f, sitBaro = 0f, sitTilt = 0f, sitShock = 0f, isClockRegression = false, 
            isLocationPending = false, gnssDetail = null, snrIdx = 0f, isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive
        )

        val trackerGpsTs = remoteHandler.trackerLastGpsTs
        val gpsAge = if (trackerGpsTs > 0) (now - trackerGpsTs) else 3600000L
        historyManager.updateRibbons(
            now = now,
            lastTickTs = lastServiceTickTs,
            serviceTickCounter = serviceTickCounter,
            rtt = networkManager.getRtt(),
            peerSignal = remoteHandler.peerSignal,
            peerAvail = isTrackerActive,
            hasGps = trackerGpsTs > 0,
            isTrackerMode = false,
            gpsIndex = TelemetryUtils.calculateGpsIndex(gpsAge, remoteHandler.trackerMaxAccuracy, remoteHandler.trackerSatsUsed).totalIndex,
            noiseIdx = ((remoteHandler.trackerAcousticDb - remoteHandler.trackerAcousticFloorDb).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB).toFloat(),
            luxIdx = (log10(remoteHandler.trackerLux.toDouble() + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0).toFloat(),
            vibeIdx = (remoteHandler.trackerVibration.toDouble() / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0).toFloat(),
            proxIdx = remoteHandler.trackerProxIdx,
            liftIdx = (abs(remoteHandler.trackerBaroAlt) / RIBBON_LIFT_SCALE_METERS).coerceIn(0f, 1f),
            snrIdx = remoteHandler.trackerSnrIdx,
            sitVz = remoteHandler.trackerSitVz,
            sitDz = remoteHandler.trackerSitDz,
            isBatterySteepDischarge = remoteHandler.isTrackerBatterySteepDischarge,
            isCoolingModeActive = remoteHandler.isTrackerCoolingModeActive,
            speed = remoteHandler.trackerSpeed,
            bearing = remoteHandler.trackerBearing,
            isSitDetected = remoteHandler.isTrackerSitDetected,
            isSitActive = remoteHandler.isTrackerSitActive
        )

        val xiaomiStatus = when(systemStatusProvider.isXiaomiSpecialPermissionGranted()) {
            XiaomiPermissionStatus.GRANTED -> EngineXiaomiStatus.GRANTED
            XiaomiPermissionStatus.DENIED -> EngineXiaomiStatus.DENIED
            XiaomiPermissionStatus.UNKNOWN -> EngineXiaomiStatus.UNKNOWN
        }

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = nowRealtime, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = false,
                isRelayConnected = isSocketConnected, isTrackerConnected = isTrackerActive, isTrackerVisualJump = remoteHandler.isTrackerVisualJump,
                isTrajectoryPromoted = remoteHandler.isTrackerTrajectoryPromoted, jumpTier = remoteHandler.trackerJumpTier,
                trackerLat = remoteHandler.trackerLat, trackerLng = remoteHandler.trackerLng, trackerAccuracy = remoteHandler.trackerAccuracy,
                maxTrackerAccuracy = remoteHandler.trackerMaxAccuracy, trackerLastGpsTs = remoteHandler.trackerLastGpsTs,
                trackerSpeed = remoteHandler.trackerSpeed, trackerBattery = remoteHandler.trackerBattery, trackerTemp = remoteHandler.trackerTemp,
                isHardwareOnline = true, isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()),
                isJammerSuspicion = isTrackerJammer, isSignalLoss = isSignalLoss, isGpsStalling = isTrackerStalled,
                isUiVisible = isUiVisible(), distToHomeAuthority = remoteHandler.trackerDistToHome, maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(),
                isGpsGap = isTrackerGap,
                isSuspicious = remoteHandler.isTrackerSuspicious, isTamperDetected = remoteHandler.isTrackerTamperDetected,
                isPowerTamper = remoteHandler.isTrackerPowerTamper, trackerTiltDegrees = remoteHandler.trackerTiltDegrees, trackerAcousticDb = remoteHandler.trackerAcousticDb,
                trackerBaroAlt = remoteHandler.trackerBaroAlt, trackerLux = remoteHandler.trackerLux, isNear = remoteHandler.isTrackerNear,
                luxBaseline = remoteHandler.trackerLuxBaseline, acousticFloorDb = remoteHandler.trackerAcousticFloorDb, adaptiveVibrationFloor = remoteHandler.trackerAdaptiveVibrationFloor,
                peakVibrationShock = remoteHandler.trackerPeakVibrationShock, trackerCurrentMa = 0, isSitActive = remoteHandler.isTrackerSitActive,
                isLocationPending = remoteHandler.isTrackerLocationPending, isPowerSaveMode = integrityMonitor.isPowerSaveModeActive,
                standbyBucket = integrityMonitor.currentStandbyBucket, netInterface = integrityMonitor.getActiveNetworkInterface(),
                isStorageLow = integrityMonitor.isStorageLow, isStorageCritical = integrityMonitor.isStorageCritical,
                isBatterySteepDischarge = remoteHandler.isTrackerBatterySteepDischarge, isCoolingModeActive = remoteHandler.isTrackerCoolingModeActive,
                discoveryPhase = null,
                xiaomiStatus = xiaomiStatus,
                isXiaomiManualOverride = isXiaomiManualOverride,
                isXiaomiAutostartGranted = isXiaomiAutostartGranted(this@ViewerService)
            )
        }

        if (serviceTickCounter % 60 == 0) {
            notificationManager.updatePulse(sats = 0, battery = integrityMonitor.getBatteryLevel(), isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = integrityMonitor.isPowerSaveModeActive)
        }

        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_REALTIME_KEY, nowRealtime)
        lastServiceTickTs = now
        lastServiceTickRealtime = nowRealtime
        serviceTickCounter++
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        alarmEvalJob?.cancel()
        if (this::commandRouter.isInitialized) commandRouter.unregister()
        super.onDestroy()
    }
}
