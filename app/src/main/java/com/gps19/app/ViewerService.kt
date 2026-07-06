package com.gps19.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
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
 * ViewerService: Background monitoring for the Viewer role.
 * v9.0.3:
 * - Issue #029: Fixed grayed-out viewer line by propagating local telemetry to repository.
 * v9.0.2:
 * - R951: Implemented GPS Stability Audit for Viewer local GPS.
 * - R951: Set VIEWER_GPS_POLLING_MS to 1000L (Temporary).
 * v8.9.99:
 * - Issue #041: Identity Sanitization. Added strict validation to handleTrackerPulse 
 *   to prevent command injection or corruption from malformed peer pulses.
 */
@AndroidEntryPoint
class ViewerService : BaseMonitorService() {

    @Inject lateinit var gpsManager: GpsManager
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
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    
    private var isXiaomiManualOverride = false
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    // R951: Stability Audit State
    private var lastGpsFixRealtime = 0L
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L

    private val localProcessorListener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean, timestamp: Long, isHindsightCorrected: Boolean, accuracy: Double, maxAccuracy: Double) {
            repository.saveTrailPoint(lat, lng, isViewerTrail, isJump, timestamp, isHindsightCorrected = isHindsightCorrected, accuracy = accuracy, maxAccuracy = maxAccuracy)
        }
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double?, vibe: Double?) {
            val specialColor = if (isSpecial || message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
            logManager.logServiceEvent(message, isImportant, isSpecial = isSpecial || message.contains("Merge-on-Stale"), specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy, snr = snr, vibe = vibe)
        }
        override fun onMaxAccuracyChanged(accuracy: Double) {
            repository.saveDoubleSync(MainRepository.MAX_ACCURACY_KEY, accuracy)
        }
        override fun onChairBaselineChanged(baseline: Double) {
            val proc = lastProcessedLocation
            logManager.logServiceEvent("Tracker: Passive Zeroing - Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", baseline)}°",
                lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
        }
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

            alarmManager = AppAlarmManager(this@ViewerService, repository, sessionManager, notificationManager, timeProvider) { type, msg, important, extreme, logId, durationMs, special, color, lat, lng, acc, maxAcc, snr, vibe -> 
                logManager.submitToLogSink(msg, type, important, extreme, logId, durationMs, special, color, lat, lng, acc, maxAcc, snr, vibe)
            }

            integrityMonitor = IntegrityMonitor(this@ViewerService, repository, timeProvider, onViolationSustained = { }, onLogEvent = { msg, important ->
                logManager.logServiceEvent(msg, important)
            })
            
            locationProcessor = LocationProcessor(localProcessorListener, timeProvider)
            
            val savedMaxAcc = repository.getDouble(MainRepository.MAX_ACCURACY_KEY, 0.0)
            val savedLastSit = repository.getLong(MainRepository.LAST_SIT_TS_KEY, 0L)
            val savedBaseline = repository.getDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, -1000.0)
            val trackerState = repository.loadTrackerState()
            val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
            val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60.0)
            locationProcessor.loadState(savedMaxAcc, savedLastSit, savedBaseline, trackerState, homePoints, maxDist)

            networkManager.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
            
            syncManager = SyncManager(this@ViewerService, networkManager, sessionManager, gpsManager, null, locationProcessor, telemetryRepository, offlineRepository, logManager, timeProvider, repository, lifecycleScope)
            syncManager.startSyncLoop(configManager.deviceId, configManager.viewerId, false)

            remoteHandler = RemoteHandler(this@ViewerService, repository, locationProcessor, alarmManager, sessionManager, forensicUseCase, timeProvider, lifecycleScope) { id -> handleTrackerPulse(id) }

            historyManager = HistoryManager(this@ViewerService, repository, gpsManager, null, locationProcessor, timeProvider, lifecycleScope) { msg, important -> 
                logManager.logServiceEvent(msg, important) 
            }

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
                { }
            )
            commandRouter.register()
            commandRouter.startObservingCommands(lifecycleScope)

            EntryPointAccessors.fromApplication(applicationContext, GpsApplication.GpsApplicationEntryPoint::class.java)
                .networkManagerWrapper().setCallback { data -> 
                    if (data.optString("type") == "remote_log") {
                        remoteHandler.handleRemoteLog(LogEntry.fromJSONObject(data))
                    } else {
                        remoteHandler.handleRemoteUpdate(data, false)
                    }
                }
            
            gpsManager.setPollingInterval(VIEWER_GPS_POLLING_MS)
            gpsCollectionJob = lifecycleScope.launch { gpsManager.getLocationFlow().collectLatest { onLocationChanged(it) } }
            gnssDetailJob = lifecycleScope.launch { gpsManager.gnssDetailFlow.collectLatest { latestGnssDetail = it } }

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
                        locationProcessor.setMaxDistanceAuthority(dist)
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
            
            serviceStartRealtime = timeProvider.elapsedRealtime()

            startTickLoop()
            logManager.logServiceEvent("Viewer Engine Online", important = true)
        }
    }

    private fun onLocationChanged(location: Location) {
        val nowRealtime = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()

        // R951: Stability Gap Detection (at 1Hz polling)
        if (VIEWER_GPS_POLLING_MS == 1000L && lastGpsFixRealtime > 0) {
            val gap = nowRealtime - lastGpsFixRealtime
            stabilityAuditFixCount++
            // Threshold for 1Hz is 1200ms (standard jitter allowance)
            if (gap > 1200L) {
                stabilityAuditViolationCount++
                val proc = lastProcessedLocation
                logManager.logServiceEvent("STABILITY GAP (V): ${gap}ms detected during 1Hz polling.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }
        }
        lastGpsFixRealtime = nowRealtime

        val processed = locationProcessor.processGpsPoint(
            lat = location.latitude,
            lng = location.longitude,
            alt = location.altitude,
            androidSpeedKph = location.speed.toDouble() * 3.6,
            gpsTs = location.time,
            accuracy = location.accuracy.toDouble(),
            bearing = location.bearing.toDouble(),
            snr = gpsManager.averageSnr,
            satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed,
            isViewerTrail = true,
            lastGpsTs = sessionManager.lastGpsTs,
            isLocal = true,
            nowRealtime = nowRealtime,
            nowWall = nowWall
        )

        if (!processed.isClockRegression) {
            sessionManager.lastGpsTs = location.time
        }
        
        lastKnownLocation = location
        lastProcessedLocation = processed

        repository.updateLocation(LocationUpdate(
            lat = location.latitude,
            lng = location.longitude,
            alt = location.altitude,
            speed = location.speed.toDouble(),
            accuracy = location.accuracy.toDouble(),
            bearing = location.bearing.toDouble(),
            battery = integrityMonitor.getBatteryLevel(),
            temp = integrityMonitor.batteryTemp,
            isCharging = integrityMonitor.isCharging,
            gpsTs = location.time,
            ts = nowWall,
            isMe = true,
            satsView = gpsManager.satellitesInView,
            satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed,
            maxAccuracy = processed.maxAccuracy,
            currentMa = integrityMonitor.getBatteryCurrent(),
            lastValidFixRealtime = locationProcessor.getLastValidFixTs(),
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
        ))

        val trackerAnchor = object : SpatialAnchor {
            override val lat = remoteHandler.trackerLat
            override val lng = remoteHandler.trackerLng
            override val alt = remoteHandler.trackerBaroAlt
            override val gpsTs = remoteHandler.trackerLastGpsTs
            override val ts = 0L 
        }
        
        locationProcessor.updateCalculatedDistances(location.latitude, location.longitude, true, trackerAnchor)
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) {
            Timber.w("Rejecting invalid Tracker ID from pulse: $id")
            return
        }

        // Fix: Save peer identity to TRACKER_ID_KEY, not VIEWER_ID_KEY.
        if ((configManager.deviceId == MainRepository.DEFAULT_TRACKER_ID || configManager.deviceId.isEmpty()) && id.isNotEmpty() && id != "Active Tracker") {
            configManager.deviceId = id
            networkManager.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch { repository.saveString(MainRepository.TRACKER_ID_KEY, id) }
        }
        if (sessionManager.onTrackerPulse(id, timeProvider.currentTimeMillis(), false)) {
            val proc = lastProcessedLocation
            logManager.logServiceEvent("Tracker connected: $id",
                lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            startTickLoop()
        }
    }

    private fun resetServiceTimers() {
        val proc = lastProcessedLocation
        serviceStartRealtime = timeProvider.elapsedRealtime()
        alarmManager.resetEvaluation()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        logManager.logServiceEvent("Session Terminated", false,
            lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
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
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
    }

    @SuppressLint("InlinedApi")
    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
                try {
                    delay(200)
                    val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    val msg = "Monitoring system active."
                    safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Failed to update foreground service type")
                }
            }
        }
    }
    
    override fun getRequiredTickInterval(): Long {
        return if (isUiVisible()) TICK_INTERVAL_MS else TICK_INTERVAL_SLOW_MS
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)

        // R951: Stability Audit Report
        if (nowRealtime - lastStabilityAuditTs > GPS_STABILITY_AUDIT_INTERVAL_MS) {
            if (stabilityAuditFixCount > 0) {
                val reliability = 100.0 * (stabilityAuditFixCount - stabilityAuditViolationCount) / stabilityAuditFixCount
                if (reliability < GPS_STABILITY_RELIABILITY_THRESHOLD) {
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY AUDIT (V): Reliability ${String.format(Locale.getDefault(), "%.1f", reliability)}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes)", important = true,
                        lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                }
                stabilityAuditFixCount = 0
                stabilityAuditViolationCount = 0
            }
            lastStabilityAuditTs = nowRealtime
        }

        val isSocketConnected = networkManager.isConnected() && !transientDropDetected.getAndSet(false)
        networkManager.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = remoteHandler.lastPeerActivityTs > 0 && (nowRealtime - remoteHandler.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRealtime, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isTrackerActive, isInViolation = false)

        val silenceDelta = if (remoteHandler.lastPeerActivityTs > 0) nowRealtime - remoteHandler.lastPeerActivityTs else 0L
        val isSignalLoss = integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, false)
        val isTrackerJammerSuspicion = remoteHandler.isTrackerJammerSuspicion
        val isTrackerStalled = remoteHandler.trackerGpsStallStartTs > 0L && (nowRealtime - remoteHandler.trackerGpsStallStartTs > GPS_STALL_THRESHOLD_MS)
        val isTrackerGap = remoteHandler.trackerLastValidFixRealtime > 0L && (nowRealtime - remoteHandler.trackerLastValidFixRealtime > GPS_GAP_THRESHOLD_MS)

        if (remoteHandler.trackerLat != 0.0 && remoteHandler.trackerLng != 0.0) {
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes()
            val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isTrackerJammerSuspicion) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isTrackerStalled) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isTrackerGap) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (remoteHandler.isTrackerVisualJump) activeViolations.add(ALERT_ID_VISUAL_JUMP)
            if (remoteHandler.isTrackerSitDetected) activeViolations.add(ALERT_ID_TRACKER_CHAIR)

            forensicUseCase.recordViolationMarkers(
                now = now,
                lat = remoteHandler.trackerLat,
                lng = remoteHandler.trackerLng,
                accuracy = remoteHandler.trackerAccuracy,
                maxAccuracy = remoteHandler.trackerMaxAccuracy,
                activeViolations = activeViolations,
                unresolvedAlarms = unresolvedAlarms
            )
        }

        val distToTracker = locationProcessor.getDistanceToTracker() ?: 0.0
        val distToHome = locationProcessor.getNearestHomeDistance() ?: 0.0
        val proc = lastProcessedLocation

        syncManager.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = false, 
            loc = lastKnownLocation, filtered = proc?.optimizedPoint,
            distToTracker = distToTracker, distToHome = distToHome, 
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), 
            filteredSpeed = proc?.filteredSpeed ?: 0.0,
            vibration = 0.0, heading = 0.0, baroAlt = 0.0,
            lux = 0.0, isNear = true, isSuspicious = false, tiltDegrees = 0.0, acousticDb = 0.0, isJump = false, isTrajectoryPromoted = false,
            jumpTier = 0, isJammer = false, isStalledRaw = false, isStalledActive = false,
            peakShock = 0.0, peakShockTs = 0L, luxBaseline = 0.0, acousticFloorDb = 0.0, adaptiveVibrationFloor = 0.12,
            proxIdx = 1.0, proximityCm = -1.0, 
            proximityDebounceMs = 0L, vibrationRollingSum = 0.0,
            micPending = false, isTamperDetected = false, isPowerTamper = false,
            isSitDetected = false, isSitActive = false, lastSitTs = 0L, receiptRealtime = proc?.receiptRealtime ?: 0L,
            violationUptimeMs = 0L, violationPercentage = 0.0, verticalVelocity = 0.0, sitVz = 0.0, sitDz = 0.0,
            sitBaro = 0.0, sitTilt = 0.0, sitShock = 0.0, isClockRegression = proc?.isClockRegression ?: false,
            isLocationPending = false, locationPendingReason = LocationPendingReason.NONE, lastValidFixRealtime = locationProcessor.getLastValidFixTs(),
            gnssDetail = latestGnssDetail,
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0),
            tiltIdx = 0.0, baroIdx = 0.0, isBatterySteepDischarge = false, isCoolingModeActive = false,
            batteryLevel = integrityMonitor.getBatteryLevel(), batteryTemp = integrityMonitor.batteryTemp, isCharging = integrityMonitor.isCharging
        )

        val gpsTs = proc?.timestamp ?: 0L
        val gpsAge = if (gpsTs > 0) (now - gpsTs) else 3600000L
        historyManager.updateRibbons(
            now = now,
            lastTickTs = lastServiceTickTs,
            serviceTickCounter = serviceTickCounter,
            rtt = networkManager.getRtt(),
            peerSignal = remoteHandler.peerSignal,
            peerAvail = isSocketConnected && isTrackerActive,
            hasGps = gpsTs > 0,
            isTrackerMode = false,
            gpsIndex = TelemetryUtils.calculateGpsIndex(gpsAge, proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), lastKnownLocation?.extras?.getInt("satellites") ?: gpsManager.satellitesUsed).totalIndex,
            accuracy = proc?.currentAccuracy ?: locationProcessor.getLastProcessedAccuracy(),
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(),
            noiseIdx = 0.0, luxIdx = 0.0, vibeIdx = 0.0, proxIdx = 1.0, liftIdx = 0.0,
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0),
            tiltIdx = 0.0, baroIdx = 0.0,
            verticalVelocity = 0.0, sitVz = 0.0, sitDz = 0.0, sitBaro = 0.0, sitTilt = 0.0, sitShock = 0.0,
            isBatterySteepDischarge = false, isCoolingModeActive = false,
            speed = (proc?.filteredSpeed ?: 0.0) / 3.6,
            bearing = (lastKnownLocation?.bearing?.toDouble() ?: 0.0),
            isSitDetected = false, isSitActive = false, currentMa = integrityMonitor.getBatteryCurrent(),
            locationPendingReason = LocationPendingReason.NONE
        )

        evaluateAlarmsInternal(nowRealtime, isSignalLoss, isTrackerJammerSuspicion, isTrackerStalled, isTrackerGap, isTrackerActive)

        if (serviceTickCounter % 60 == 0) {
            notificationManager.updatePulse(
                sats = gpsManager.satellitesUsed,
                battery = integrityMonitor.getBatteryLevel(),
                isSecure = !alarmManager.hasUnresolvedAlarms(),
                isPowerSave = integrityMonitor.isPowerSaveModeActive
            )
        }

        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_REALTIME_KEY, nowRealtime)
        lastServiceTickTs = now
        lastServiceTickRealtime = nowRealtime
        serviceTickCounter++
    }

    private fun evaluateAlarmsInternal(
        nowRealtime: Long,
        isSignalLoss: Boolean,
        isTrackerJammerSuspicion: Boolean,
        isTrackerStalled: Boolean,
        isTrackerGap: Boolean,
        isTrackerConnected: Boolean
    ) {
        val proc = lastProcessedLocation
        val isSocketConnected = networkManager.isConnected()
        
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = nowRealtime, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = false,
                isRelayConnected = isSocketConnected, isTrackerConnected = isTrackerConnected,
                isTrackerVisualJump = remoteHandler.isTrackerVisualJump, 
                isTrajectoryPromoted = remoteHandler.isTrackerTrajectoryPromoted, jumpTier = remoteHandler.trackerJumpTier,
                trackerLat = remoteHandler.trackerLat, trackerLng = remoteHandler.trackerLng, trackerAccuracy = remoteHandler.trackerAccuracy,
                maxTrackerAccuracy = remoteHandler.trackerMaxAccuracy, trackerLastGpsTs = remoteHandler.trackerLastGpsTs,
                trackerLastValidFixTs = remoteHandler.trackerLastValidFixRealtime,
                trackerSpeed = remoteHandler.trackerSpeed, trackerBattery = remoteHandler.trackerBattery, trackerTemp = remoteHandler.trackerTemp,
                isHardwareOnline = remoteHandler.isTrackerConnected, isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()),
                isJammerSuspicion = isTrackerJammerSuspicion, isSignalLoss = isSignalLoss, isGpsStalling = isTrackerStalled, isUiVisible = isUiVisible(),
                distToHomeAuthority = remoteHandler.trackerDistToHome, maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(),
                isGpsGap = isTrackerGap, isSuspicious = remoteHandler.isTrackerSuspicious, isTamperDetected = remoteHandler.isTrackerTamperDetected,
                isPowerTamper = remoteHandler.isTrackerPowerTamper, trackerTiltDegrees = remoteHandler.trackerTiltDegrees, 
                trackerAcousticDb = remoteHandler.trackerAcousticDb, trackerBaroAlt = remoteHandler.trackerBaroAlt, trackerLux = remoteHandler.trackerLux,
                isNear = remoteHandler.isTrackerNear, luxBaseline = remoteHandler.trackerLuxBaseline, acousticFloorDb = remoteHandler.trackerAcousticFloorDb,
                adaptiveVibrationFloor = remoteHandler.trackerAdaptiveVibrationFloor, peakVibrationShock = remoteHandler.trackerPeakVibrationShock,
                trackerCurrentMa = remoteHandler.trackerCurrentMa, isSitActive = remoteHandler.isTrackerSitActive, isLocationPending = remoteHandler.isTrackerLocationPending,
                locationPendingReason = remoteHandler.trackerLocationPendingReason, isPowerSaveMode = remoteHandler.isTrackerPowerSaveMode,
                standbyBucket = remoteHandler.trackerStandbyBucket, netInterface = remoteHandler.trackerNetInterface, 
                isStorageLow = remoteHandler.isTrackerStorageLow,
                isStorageCritical = remoteHandler.isTrackerStorageCritical, 
                isBatterySteepDischarge = remoteHandler.isTrackerBatterySteepDischarge,
                isCoolingModeActive = remoteHandler.isTrackerCoolingModeActive,
                discoveryPhase = null, isXiaomiDevice = false, xiaomiStatus = EngineXiaomiStatus.UNKNOWN, xiaomiAutostartStatus = EngineXiaomiStatus.UNKNOWN, isXiaomiManualOverride = false,
                snrSnapshot = gpsManager.averageSnr, vibeSnapshot = 0.0
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        alarmEvalJob?.cancel()
        settingsJob?.cancel()
        if (this::commandRouter.isInitialized) commandRouter.unregister()
        super.onDestroy()
    }
}
