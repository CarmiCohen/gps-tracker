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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * v8.8.27: Modularization Phase 2 - Delegated Forensic Marking and Behavioral Logic to UseCases.
 * v8.8.31: Escalated GPS Revival - Implemented retry loop and critical hardware failure logging.
 * v8.8.31 (Identity Propagation): Now passing identity to LocationProcessor for forensic marking.
 * v8.8.32: Fixed forensic marking for VISUAL_JUMP to ensure persistent map markers.
 * v8.8.35: Updated to latest baseline following database schema cleanup (Issue 159).
 * v8.8.35: Issue 163 - Restored power tamper detection by reconnecting callbacks to IntegrityMonitor.
 * v8.8.35: Issue 148 - Implemented A15 stable polling (1000ms).
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    @Inject lateinit var gpsManager: GpsManager
    @Inject lateinit var sensorManager: AppSensorManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var systemStatusProvider: SystemStatusProvider
    @Inject lateinit var forensicUseCase: ServiceForensicUseCase
    @Inject lateinit var behaviorUseCase: ServiceBehaviorUseCase
    
    private lateinit var integrityMonitor: IntegrityMonitor
    private lateinit var alarmManager: AppAlarmManager
    private lateinit var historyManager: HistoryManager
    private lateinit var locationProcessor: LocationProcessor
    private lateinit var syncManager: SyncManager
    private lateinit var commandRouter: CommandRouter
    private lateinit var remoteHandler: RemoteHandler
    
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var settingsJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null
    
    private var isSuspiciousMode = false
    private var lastSitDetected = false
    private var pendingAcousticViolation = false
    private var isMuzzled = false
    
    private var isRevivalTriggered = false
    private var revivalAttemptCount = 0
    private var lastRevivalAttemptTs = 0L
    
    private var isXiaomiManualOverride = false
    private var isS21FE = false
    private var isXiaomi = false
    private var isA15 = false
    private var currentGpsInterval = -1L

    private val localProcessorListener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean) {
            repository.saveTrailPoint(lat, lng, isViewerTrail, isJump)
        }
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean) {
            val specialColor = if (isSpecial || message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
            logManager.logServiceEvent(message, isImportant, isSpecial = isSpecial || message.contains("Merge-on-Stale"), specialColor = specialColor)
        }
        override fun onMaxAccuracyChanged(accuracy: Float) {
            repository.saveFloatSync(MainRepository.MAX_ACCURACY_KEY, accuracy)
        }
        override fun onChairBaselineChanged(baseline: Float) {
            logManager.logServiceEvent("Passive Zeroing: Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", baseline)}°")
            lifecycleScope.launch { repository.saveFloat(MainRepository.CHAIR_BASELINE_TILT_KEY, baseline) }
        }
        override fun onGpsStallDetected(ts: Long) {
            if (systemMonitor.gpsStallStartTs == 0L) systemMonitor.gpsStallStartTs = ts
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch(serviceExceptionHandler) {
            configManager.deviceId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            configManager.viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, DEFAULT_RELAY_URL)
            configManager.isTrackerMode = true
            
            isS21FE = isS21FEDevice()
            isXiaomi = isXiaomiDevice()
            isA15 = isA15Device()

            alarmManager = AppAlarmManager(this@TrackerService, repository, sessionManager, notificationManager, timeProvider) { type, msg, important, extreme, logId, durationMs, special, color -> 
                logManager.submitToLogSink(msg, type, important, extreme, logId, durationMs, special, color)
            }
            
            integrityMonitor = IntegrityMonitor(this@TrackerService, repository, timeProvider, onViolationSustained = { type ->
                // Issue 163: Reconnect power tamper latch
                if (type == ALERT_ID_TRACKER_POWER) {
                    alarmManager.setPowerAlarmPending(true)
                }
            }, onLogEvent = { msg, important ->
                val isSpecial = msg.contains("tamper", ignoreCase = true) || 
                               msg.contains("confirmed", ignoreCase = true) ||
                               msg.contains("EMERGENCY", ignoreCase = true)
                logManager.logServiceEvent(msg, important, isSpecial = isSpecial, specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null)
            })
            
            locationProcessor = LocationProcessor(localProcessorListener, timeProvider)
            
            // Load Engine State
            val savedMaxAcc = repository.getFloat(MainRepository.MAX_ACCURACY_KEY, 0f)
            val savedLastSit = repository.getLong(MainRepository.LAST_SIT_TS_KEY, 0L)
            val savedBaseline = repository.getFloat(MainRepository.CHAIR_BASELINE_TILT_KEY, -1000f)
            val trackerState = repository.loadTrackerState()
            val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
            val maxDist = repository.getFloat(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60f).toDouble()
            locationProcessor.loadState(savedMaxAcc, savedLastSit, savedBaseline, trackerState, homePoints, maxDist)

            sensorManager.setHardwareFailureCallback { reason ->
                logManager.logServiceEvent("CRITICAL: SENSOR_HARDWARE_FAILURE - $reason", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
            }

            historyManager = HistoryManager(this@TrackerService, repository, gpsManager, sensorManager, locationProcessor, timeProvider, lifecycleScope) { msg, important -> logManager.logServiceEvent(msg, important) }

            sensorManager.start()
            networkManager.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, true)
            
            syncManager = SyncManager(applicationContext, networkManager, sessionManager, gpsManager, sensorManager, locationProcessor, telemetryRepository, offlineRepository, logManager, timeProvider, lifecycleScope)
            syncManager.setOnSyncStartedListener {
                isMuzzled = true
                lifecycleScope.launch {
                    delay(MUZZLE_WINDOW_DURATION_MS)
                    isMuzzled = false
                }
            }
            syncManager.startSyncLoop(configManager.deviceId, configManager.viewerId, true)

            remoteHandler = RemoteHandler(this@TrackerService, repository, locationProcessor, alarmManager, sessionManager, timeProvider, lifecycleScope) { id -> handleViewerPulse(id) }

            commandRouter = CommandRouter(
                this@TrackerService, configManager, logManager, networkManager, alarmManager, notificationManager, 
                sessionManager, locationProcessor, 
                remoteHandler, 
                repository, syncManager, integrityMonitor, timeProvider,
                { handleViewerPulse(it) }, 
                { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }, 
                { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }, 
                { onUiVisibilityChangedInternal(it) }, 
                { transientDropDetected.set(it) }, 
                { resetServiceTimers() }, 
                { sensorManager.start() }
            )
            commandRouter.register()
            commandRouter.startObservingCommands(lifecycleScope)

            EntryPointAccessors.fromApplication(applicationContext, GpsApplication.GpsApplicationEntryPoint::class.java)
                .networkManagerWrapper().setCallback { data -> 
                    remoteHandler.handleRemoteUpdate(data, true)
                }

            gpsCollectionJob = lifecycleScope.launch { gpsManager.getLocationFlow().collectLatest { onLocationChanged(it) } }
            gnssDetailJob = lifecycleScope.launch { gpsManager.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

            // R916: Dynamic settings and geofence observation
            settingsJob = lifecycleScope.launch {
                launch {
                    repository.alertSettingsFlow.collect { settings ->
                        alarmManager.updateSettings(settings)
                    }
                }
                launch {
                    repository.homePointsFlow.collect { points ->
                        locationProcessor.setHomePoints(points.map { EngineGeoPoint(it.latitude, it.longitude) })
                    }
                }
                launch {
                    repository.maxDistanceFlow.collect { dist ->
                        locationProcessor.setMaxDistanceAuthority(dist.toDouble())
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
            locationProcessor.setLastValidFixTs(timeProvider.elapsedRealtime()) 
            
            setupPhysicalFastPaths()
            startTickLoop()
            
            logManager.logServiceEvent("Tracker Engine Online", important = true)
        }
    }

    private fun handleViewerPulse(id: String) {
        if ((configManager.viewerId == MainRepository.DEFAULT_VIEWER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer" && id != MainRepository.DEFAULT_VIEWER_ID) {
            configManager.viewerId = id
            networkManager.updateIdentity(configManager.deviceId, id, true)
            lifecycleScope.launch { repository.saveString(MainRepository.VIEWER_ID_KEY, id) } 
        }
        if (sessionManager.onViewerPulse(id, timeProvider.currentTimeMillis(), true)) { 
            logManager.logServiceEvent("Viewer connected: $id")
            startTickLoop() 
        }
    }

    private fun resetServiceTimers() { 
        serviceStartRealtime = timeProvider.elapsedRealtime()
        alarmManager.resetEvaluation()
        locationProcessor.resetStats()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        pendingAcousticViolation = false
        isRevivalTriggered = false
        revivalAttemptCount = 0
        lastRevivalAttemptTs = 0L
        logManager.logServiceEvent("Session Terminated", false)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        val msg = "Tracking system active."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { 
            startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type) 
        } else { 
            startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg)) 
        }
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
                delay(200)
                val type = getAvailableForegroundServiceType()
                val msg = if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0) "Acoustic monitoring active." else "Tracking system active."
                startForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun getAvailableForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasMicPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission && (isUiVisible() || sensorManager.isAcousticMonitoringActive())) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
            }
        }
        return type
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)
        
        val isSocketConnected = networkManager.isConnected() && !transientDropDetected.getAndSet(false)
        networkManager.updateRelayStatus(isSocketConnected)
        
        sessionManager.updateTick(nowRealtime, lastServiceTickRealtime, isPeerAvailable = isSocketConnected, isInViolation = isSuspiciousMode)
        
        val silenceDelta = if (remoteHandler.lastPeerActivityTs > 0) nowRealtime - remoteHandler.lastPeerActivityTs else 0L
        val isSignalLoss = integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, true) 
        val isJammerSuspicionActive = (systemMonitor.jumpStateStartTs != 0L && (nowRealtime - systemMonitor.jumpStateStartTs) > JAMMER_DETECTION_THRESHOLD_MS)
        val isGpsStalledActive = (systemMonitor.gpsStallStartTs > 0L && (nowRealtime - systemMonitor.gpsStallStartTs) > GPS_STALL_THRESHOLD_MS)
        val isGpsGapActive = (nowRealtime - locationProcessor.getLastValidFixTs() > GPS_GAP_THRESHOLD_MS)
        
        if (isGpsStalledActive) {
            val timeSinceLastRevival = nowRealtime - lastRevivalAttemptTs
            if (revivalAttemptCount < MAX_REVIVAL_ATTEMPTS && timeSinceLastRevival > GPS_REVIVAL_RETRY_INTERVAL_MS) {
                revivalAttemptCount++
                lastRevivalAttemptTs = nowRealtime
                gpsManager.reviveGps()
                logManager.logServiceEvent("GPS Stall: Revival attempt $revivalAttemptCount/$MAX_REVIVAL_ATTEMPTS triggered", important = true)
            } else if (revivalAttemptCount >= MAX_REVIVAL_ATTEMPTS && timeSinceLastRevival > GPS_REVIVAL_RETRY_INTERVAL_MS) {
                lastRevivalAttemptTs = nowRealtime
                logManager.logServiceEvent("CRITICAL: GPS_HARDWARE_LOCK - All revival attempts failed. Manual intervention required.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
            }
        }

        // Forensic Marking - Delegated to UseCase
        lastProcessedLocation?.let { proc ->
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes()
            val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isJammerSuspicionActive) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isGpsStalledActive) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isGpsGapActive) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (proc.status == SentinelStatus.JUMP) activeViolations.add(ALERT_ID_VISUAL_JUMP)

            forensicUseCase.recordViolationMarkers(
                now = now,
                lat = proc.optimizedPoint.lat,
                lng = proc.optimizedPoint.lng,
                accuracy = proc.maxAccuracy.toDouble(),
                activeViolations = activeViolations,
                unresolvedAlarms = unresolvedAlarms
            )
        }

        // Sensor Processing
        val peakDb = sensorManager.consumeAcousticPeak()
        val minDb = sensorManager.consumeAcousticMin()
        val peakShock = sensorManager.consumePeakVibration()
        
        locationProcessor.updateSensorData(
            vibration = sensorManager.currentVibrationIndex, heading = sensorManager.currentCompassHeading, baroAlt = sensorManager.relativeAltitude,
            lux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, powerTamper = integrityMonitor.isPowerTamperDetected,
            tiltDegrees = sensorManager.currentTiltDegrees, acousticDb = peakDb, peakShock = peakShock, acousticMinDb = minDb,
            peakVerticalVelocity = sensorManager.currentVerticalVelocity, plungeMatched = sensorManager.consumePlungeMatched(), 
            peakVerticalVelocityTs = 0L, 
            isSirenActive = false, isWarming = sensorManager.isWarming, manualAdaptiveFloor = sensorManager.adaptiveVibrationFloor,
            isMuzzled = isMuzzled,
            nowRealtime = nowRealtime, nowWall = now
        )

        val rawSitDetected = locationProcessor.consumeSitDetected()
        lastSitDetected = rawSitDetected || (locationProcessor.getLastSitRealtime() > 0 && (nowRealtime - locationProcessor.getLastSitRealtime() < SUSPICIOUS_STATE_COOLDOWN_MS))
        
        if (rawSitDetected) {
            logManager.logServiceEvent("Sit Detected (Engine Pulse)", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
            lifecycleScope.launch { repository.saveLong(MainRepository.LAST_SIT_TS_KEY, locationProcessor.getLastSitTs()) }
        }

        val isTamperSiren = integrityMonitor.isPowerTamperDetected || !sensorManager.isProximityNear || SentinelValidator.isLightViolated(sensorManager.currentLux, locationProcessor.getLuxBaseline())
        
        // Behavioral State Management - Delegated to UseCase
        val nextSuspicious = behaviorUseCase.updateSuspiciousMode(
            currentSuspicious = isSuspiciousMode,
            isPhysicalViolation = isTamperSiren,
            isSitDetected = lastSitDetected,
            nowRealtime = nowRealtime
        )
        if (nextSuspicious && !isSuspiciousMode) {
             logManager.logServiceEvent("Suspicious mode: Physical Tamper", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        }
        isSuspiciousMode = nextSuspicious

        // GPS Polling Adaptation - Delegated to UseCase
        val gpsInterval = behaviorUseCase.calculateGpsInterval(
            isCoolingMode = integrityMonitor.isCoolingModeActive,
            isSuspiciousMode = isSuspiciousMode,
            isStationary = sensorManager.isStationary(),
            nowRealtime = nowRealtime,
            deviceSpecialFlags = ServiceBehaviorUseCase.DeviceSpecialFlags(isS21FE = isS21FE, isXiaomi = isXiaomi, isA15 = isA15)
        )
        
        if (gpsInterval != currentGpsInterval) {
            currentGpsInterval = gpsInterval
            gpsManager.setPollingInterval(gpsInterval)
        }

        // Push Status
        val proc = lastProcessedLocation
        syncManager.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = lastKnownLocation, filtered = proc?.optimizedPoint,
            distToTracker = locationProcessor.getDistanceToTracker(), distToHome = locationProcessor.getNearestHomeDistance(),
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), filteredSpeed = proc?.filteredSpeed ?: 0.0,
            vibration = sensorManager.currentVibrationIndex, heading = sensorManager.currentCompassHeading, baroAlt = sensorManager.relativeAltitude,
            lux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, isSuspicious = isSuspiciousMode, tiltDegrees = sensorManager.currentTiltDegrees,
            acousticDb = sensorManager.currentAcousticDb, isJump = (proc?.status == SentinelStatus.JUMP), 
            isTrajectoryPromoted = proc?.isTrajectoryPromoted ?: false, jumpTier = proc?.jumpTier ?: 0, isJammer = isJammerSuspicionActive,
            isStalledRaw = proc?.isStalled ?: false, isStalledActive = isGpsStalledActive, peakShock = locationProcessor.getPeakVibrationShock(),
            peakShockTs = locationProcessor.sentinel.peakVibrationShockTs, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
            adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = sensorManager.proximityIdx, proximityCm = sensorManager.debouncedProximityCm,
            micPending = false, isTamperDetected = isTamperSiren, isPowerTamper = integrityMonitor.isPowerTamperDetected, 
            isSitDetected = rawSitDetected, isSitActive = lastSitDetected, lastSitTs = locationProcessor.getLastSitTs(), receiptRealtime = proc?.receiptRealtime ?: 0L,
            violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(),
            verticalVelocity = sensorManager.currentVerticalVelocity, sitVz = locationProcessor.sentinel.lastSitVz, sitDz = locationProcessor.sentinel.lastSitDz, 
            sitBaro = locationProcessor.sentinel.lastSitBaro, sitTilt = locationProcessor.sentinel.lastSitTilt, sitShock = locationProcessor.sentinel.lastSitShock, 
            isClockRegression = proc?.isClockRegression ?: false, isLocationPending = pendingAcousticViolation, gnssDetail = latestGnssDetail, 
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0f, 1f), isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive
        )

        // Forensic Ribbon Update (1Hz loop)
        val gpsTs = proc?.timestamp ?: 0L
        val gpsAge = if (gpsTs > 0) (now - gpsTs) else 3600000L
        historyManager.updateRibbons(
            now = now,
            lastTickTs = lastServiceTickTs,
            serviceTickCounter = serviceTickCounter,
            rtt = networkManager.getRtt(),
            peerSignal = remoteHandler.peerSignal,
            peerAvail = isSocketConnected && (remoteHandler.lastPeerActivityTs > 0 && (nowRealtime - remoteHandler.lastPeerActivityTs < WATCH_TIMEOUT_MS)),
            hasGps = gpsTs > 0,
            isTrackerMode = true,
            gpsIndex = TelemetryUtils.calculateGpsIndex(gpsAge, proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), lastKnownLocation?.extras?.getInt("satellites") ?: gpsManager.satellitesUsed).totalIndex,
            noiseIdx = ((sensorManager.currentAcousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB).toFloat(),
            luxIdx = (log10(sensorManager.currentLux.toDouble() + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0).toFloat(),
            vibeIdx = (sensorManager.currentVibrationIndex.toDouble() / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0).toFloat(),
            proxIdx = sensorManager.proximityIdx,
            liftIdx = (abs(sensorManager.relativeAltitude) / RIBBON_LIFT_SCALE_METERS).coerceIn(0f, 1f),
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0f, 1f),
            sitVz = locationProcessor.sentinel.lastSitVz,
            sitDz = locationProcessor.sentinel.lastSitDz,
            isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge,
            isCoolingModeActive = integrityMonitor.isCoolingModeActive,
            speed = ((proc?.filteredSpeed ?: 0.0) / 3.6).toFloat(),
            bearing = (lastKnownLocation?.bearing ?: 0f),
            isSitDetected = rawSitDetected,
            isSitActive = lastSitDetected
        )

        evaluateAlarmsInternal(nowRealtime, isSignalLoss, isJammerSuspicionActive, isGpsStalledActive, isTamperSiren)

        if (serviceTickCounter % 60 == 0) { notificationManager.updatePulse(sats = gpsManager.satellitesUsed, battery = integrityMonitor.getBatteryLevel(), isSecure = !alarmManager.hasUnresolvedAlarms(), isPowerSave = integrityMonitor.isPowerSaveModeActive) }
        
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_REALTIME_KEY, nowRealtime)
        lastServiceTickTs = now
        lastServiceTickRealtime = nowRealtime
        serviceTickCounter++
    }

    private fun evaluateAlarmsInternal(
        nowRealtime: Long, 
        isSignalLoss: Boolean, 
        isJammerSuspicion: Boolean, 
        isGpsStalling: Boolean, 
        isTamperDetected: Boolean
    ) {
        val proc = lastProcessedLocation
        val isSocketConnected = networkManager.isConnected()
        
        val xiaomiStatus = when(systemStatusProvider.isXiaomiSpecialPermissionGranted()) {
            XiaomiPermissionStatus.GRANTED -> EngineXiaomiStatus.GRANTED
            XiaomiPermissionStatus.DENIED -> EngineXiaomiStatus.DENIED
            XiaomiPermissionStatus.UNKNOWN -> EngineXiaomiStatus.UNKNOWN
        }

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = nowRealtime, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = true,
                isRelayConnected = isSocketConnected, isTrackerConnected = true, isTrackerVisualJump = (proc?.status == SentinelStatus.JUMP),
                isTrajectoryPromoted = proc?.isTrajectoryPromoted ?: false, jumpTier = proc?.jumpTier ?: 0, trackerLat = proc?.optimizedPoint?.lat ?: 0.0, 
                trackerLng = proc?.optimizedPoint?.lng ?: 0.0, trackerAccuracy = proc?.currentAccuracy ?: 0f, 
                maxTrackerAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), trackerLastGpsTs = proc?.timestamp ?: 0L, 
                trackerSpeed = ((proc?.filteredSpeed ?: 0.0) / 3.6).toFloat(), trackerBattery = integrityMonitor.getBatteryLevel(), trackerTemp = integrityMonitor.batteryTemp,
                isHardwareOnline = true, isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()),
                isJammerSuspicion = isJammerSuspicion, isSignalLoss = isSignalLoss, isGpsStalling = isGpsStalling, isUiVisible = isUiVisible(),
                distToHomeAuthority = locationProcessor.getNearestHomeDistance(), maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), 
                isGpsGap = (nowRealtime - locationProcessor.getLastValidFixTs() > GPS_GAP_THRESHOLD_MS), isSuspicious = isSuspiciousMode, isTamperDetected = isTamperDetected, isPowerTamper = integrityMonitor.isPowerTamperDetected,
                trackerTiltDegrees = sensorManager.currentTiltDegrees, trackerAcousticDb = sensorManager.currentAcousticDb, trackerBaroAlt = sensorManager.relativeAltitude,
                trackerLux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = locationProcessor.getPeakVibrationShock(), trackerCurrentMa = integrityMonitor.getBatteryCurrent(),
                isSitActive = lastSitDetected, isLocationPending = pendingAcousticViolation, isPowerSaveMode = integrityMonitor.isPowerSaveModeActive,
                standbyBucket = integrityMonitor.currentStandbyBucket, netInterface = integrityMonitor.getActiveNetworkInterface(), isStorageLow = integrityMonitor.isStorageLow,
                isStorageCritical = integrityMonitor.isStorageCritical, isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive,
                discoveryPhase = null,
                xiaomiStatus = xiaomiStatus,
                isXiaomiManualOverride = isXiaomiManualOverride,
                isXiaomiAutostartGranted = isXiaomiAutostartGranted(this@TrackerService)
            )
        }
    }

    override fun getRequiredTickInterval(): Long {
        return if (isUiVisible() || isSuspiciousMode) TICK_INTERVAL_MS else TICK_INTERVAL_SLOW_MS
    }

    private fun onLocationChanged(location: Location) {
        val processed = locationProcessor.processGpsPoint(
            lat = location.latitude, lng = location.longitude, alt = location.altitude, androidSpeedKph = location.speed.toDouble() * 3.6, 
            gpsTs = location.time, accuracy = location.accuracy, bearing = location.bearing, snr = gpsManager.averageSnr, 
            satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed, isViewerTrail = false, lastGpsTs = sessionManager.lastGpsTs, 
            isLocal = true, providedAdaptiveVibrationFloor = sensorManager.adaptiveVibrationFloor, providedAcousticFloorDb = locationProcessor.getAcousticFloorDb(), 
            isSuspicious = isSuspiciousMode,
            isMuzzled = isMuzzled
        )
        
        if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
        if (!processed.isClockRegression && !processed.isStalled) { 
            systemMonitor.gpsStallStartTs = 0L; isRevivalTriggered = false 
            revivalAttemptCount = 0; lastRevivalAttemptTs = 0L
        }
        
        if (processed.status == SentinelStatus.JUMP || processed.status == SentinelStatus.OUTLIER) {
            if (systemMonitor.jumpStateStartTs == 0L) systemMonitor.jumpStateStartTs = timeProvider.elapsedRealtime()
        } else if (processed.status == SentinelStatus.VALID) {
            systemMonitor.jumpStateStartTs = 0L
            pendingAcousticViolation = false
        }
        
        lastKnownLocation = location; lastProcessedLocation = processed

        if (processed.status != SentinelStatus.VALID) {
            val nowRealtime = timeProvider.elapsedRealtime()
            val nowWall = timeProvider.currentTimeMillis()
            val silenceDelta = if (remoteHandler.lastPeerActivityTs > 0) nowRealtime - remoteHandler.lastPeerActivityTs else 0L
            val isSignalLoss = integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, true)
            val isJammerSuspicion = (systemMonitor.jumpStateStartTs != 0L && (nowRealtime - systemMonitor.jumpStateStartTs) > JAMMER_DETECTION_THRESHOLD_MS)
            val isGpsStalling = (systemMonitor.gpsStallStartTs > 0L && (nowRealtime - systemMonitor.gpsStallStartTs) > GPS_STALL_THRESHOLD_MS)
            val isTamperSiren = integrityMonitor.isPowerTamperDetected || !sensorManager.isProximityNear || SentinelValidator.isLightViolated(sensorManager.currentLux, locationProcessor.getLuxBaseline())
            evaluateAlarmsInternal(nowRealtime, isSignalLoss, isJammerSuspicion, isGpsStalling, isTamperSiren)
        }
    }

    override fun onDestroy() {
        if (this::sensorManager.isInitialized) sensorManager.stop()
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        alarmEvalJob?.cancel()
        settingsJob?.cancel()
        if (this::commandRouter.isInitialized) commandRouter.unregister()
        super.onDestroy()
    }

    private fun setupPhysicalFastPaths() {
        sensorManager.setAcousticFastPath(locationProcessor.getAcousticFloorDb(), ACOUSTIC_THRESHOLD_DB_JUMP, ACOUSTIC_MIN_THRESHOLD_DB) {
            triggerSuspiciousMode("Acoustic") 
            pendingAcousticViolation = true
        }
        sensorManager.setLightFastPath(locationProcessor.getLuxBaseline(), LIGHT_THRESHOLD_LUX_JUMP) {
            triggerSuspiciousMode("Light")
        }
    }

    private fun triggerSuspiciousMode(source: String) {
        if (!isSuspiciousMode) { 
             logManager.logServiceEvent("Suspicious mode: Physical Tamper ($source)", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        }
        isSuspiciousMode = behaviorUseCase.updateSuspiciousMode(isSuspiciousMode, true, false, timeProvider.elapsedRealtime())
    }
}
