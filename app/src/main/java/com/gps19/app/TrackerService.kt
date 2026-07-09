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
 * v9.3.6:
 * - Issue #058: Hilt Migration. Updated RemoteHandler initialization to use Listener pattern.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    @Inject lateinit var gpsManager: GpsManager
    @Inject lateinit var sensorManager: AppSensorManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var systemStatusProvider: SystemStatusProvider
    @Inject lateinit var forensicUseCase: ServiceForensicUseCase
    @Inject lateinit var behaviorUseCase: ServiceBehaviorUseCase
    
    @Inject lateinit var integrityMonitor: IntegrityMonitor
    @Inject lateinit var alarmManager: AppAlarmManager
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var locationProcessor: LocationProcessor
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var commandRouter: CommandRouter
    @Inject lateinit var remoteHandler: RemoteHandler
    
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var settingsJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null
    
    private var isSuspiciousMode = false
    private var lastSitDetected = false
    private var lastSitSyncLatchTs = 0L
    private var lastSitLogTs = 0L
    private var pendingAcousticViolation = false
    private var isMuzzled = false
    private var muzzleReleaseJob: Job? = null

    // Issue #038: Adaptation Muzzle State
    private var isAdaptationMuzzled = false
    private var adaptationMuzzleJob: Job? = null
    
    private var revivalAttemptCount = 0
    private var lastRevivalAttemptTs = 0L
    private var lastXiaomiRecoveryTs = 0L
    
    private var isXiaomiManualOverride = false
    private var isS21FE = false
    private var isXiaomi = false
    private var isA15 = false
    private var currentGpsInterval = -1L
    private var lastGpsTransitionLogTs = 0L

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
            logManager.logServiceEvent("Passive Zeroing: Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", baseline)}°",
                lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            lifecycleScope.launch { repository.saveDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, baseline) }
        }
        override fun onGpsStallDetected(ts: Long) {
            if (systemMonitor.gpsStallStartTs == 0L) systemMonitor.gpsStallStartTs = ts
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            configManager.deviceId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            configManager.viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, DEFAULT_RELAY_URL)
            configManager.isTrackerMode = true
            
            isS21FE = isS21FEDevice()
            isXiaomi = isXiaomiDevice()
            isA15 = isA15Device()

            alarmManager.setListener(object : AppAlarmManager.Listener {
                override fun onLogEvent(type: String, message: String, important: Boolean, extremeValue: Double?, logId: String?, durationMs: Long, isSpecial: Boolean, specialColor: Int?, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double, snr: Double?, vibe: Double?) {
                    logManager.submitToLogSink(message, type, important, extremeValue, logId, durationMs, isSpecial, specialColor, lat, lng, accuracy, maxAccuracy, snr, vibe)
                }
            })
            
            integrityMonitor.setListener(object : IntegrityMonitor.Listener {
                override fun onViolationSustained(type: String) {
                    if (type == ALERT_ID_TRACKER_POWER) {
                        alarmManager.setPowerAlarmPending(true)
                    }
                }
                override fun onLogEvent(message: String, important: Boolean) {
                    val isSpecial = message.contains("tamper", ignoreCase = true) || 
                                   message.contains("confirmed", ignoreCase = true) ||
                                   message.contains("EMERGENCY", ignoreCase = true) ||
                                   message.contains("PRIORITY", ignoreCase = true) ||
                                   message.contains("BUCKET", ignoreCase = true)
                    logManager.logServiceEvent(message, important, isSpecial = isSpecial, specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null)
                }
            })
            
            locationProcessor.setListener(localProcessorListener)

            delay(1000)

            val savedMaxAcc = repository.getDouble(MainRepository.MAX_ACCURACY_KEY, 0.0)
            val savedLastSit = repository.getLong(MainRepository.LAST_SIT_TS_KEY, 0L)
            val savedBaseline = repository.getDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, -1000.0)
            val trackerState = repository.loadTrackerState()
            val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
            val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60.0)
            locationProcessor.loadState(savedMaxAcc, savedLastSit, savedBaseline, trackerState, homePoints, maxDist)

            sensorManager.setHardwareFailureCallback { reason ->
                val proc = lastProcessedLocation
                logManager.logServiceEvent("CRITICAL: SENSOR_HARDWARE_FAILURE - $reason", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }

            historyManager.setListener(object : HistoryManager.Listener {
                override fun onLogEvent(message: String, important: Boolean) {
                    logManager.logServiceEvent(message, important)
                }
            })
            historyManager.initialize(lifecycleScope)

            sensorManager.start()

            delay(1000)
            networkManager.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, true)
            
            syncManager.setOnSyncStartedListener {
                muzzleReleaseJob?.cancel()
                isMuzzled = true
            }
            syncManager.setOnSyncFinishedListener {
                muzzleReleaseJob?.cancel()
                muzzleReleaseJob = lifecycleScope.launch {
                    val hysteresis = if (isA15) MUZZLE_HYSTERESIS_A15_MS else MUZZLE_HYSTERESIS_MS
                    delay(hysteresis)
                    isMuzzled = false
                }
            }
            
            delay(1000)
            syncManager.startSyncLoop(lifecycleScope, configManager.deviceId, configManager.viewerId, true)

            remoteHandler.setListener(object : RemoteHandler.Listener {
                override fun onPeerPulse(id: String) { handleViewerPulse(id) }
            })
            remoteHandler.initialize(lifecycleScope)

            commandRouter.setListener(object : CommandRouter.Listener {
                override fun onViewerPulse(id: String) = handleViewerPulse(id)
                override fun onWatchdogTrigger() { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                override fun onUiPulse() { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                override fun onUiVisibilityChanged(visible: Boolean) { onUiVisibilityChangedInternal(visible) }
                override fun onTransientDrop(drop: Boolean) { transientDropDetected.set(drop) }
                override fun onResetTimers() { resetServiceTimers() }
                override fun onSyncSensors() { sensorManager.start() }
                override fun onTriggerForensicTest() {
                    lifecycleScope.launch {
                        val proc = lastProcessedLocation
                        logManager.logServiceEvent("FORENSIC TEST: Manually injecting Jammer/Stall markers", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                            lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                        systemMonitor.jumpStateStartTs = timeProvider.elapsedRealtime() - 31000L
                        systemMonitor.gpsStallStartTs = timeProvider.elapsedRealtime() - 61000L
                    }
                }
            })
            commandRouter.register()
            commandRouter.startObservingCommands(lifecycleScope)

            EntryPointAccessors.fromApplication(applicationContext, GpsApplication.GpsApplicationEntryPoint::class.java)
                .networkManagerWrapper().setCallback { data -> 
                    remoteHandler.handleRemoteUpdate(data, true)
                }

            delay(2000)
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

            setupPhysicalFastPaths()
            startTickLoop()
            
            withContext(Dispatchers.Main) {
                updateForegroundServiceType()
            }
            
            logManager.logServiceEvent("Tracker Engine Online (Staggered)", important = true)
        }
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) {
            Timber.w("Rejecting invalid Viewer ID from pulse: $id")
            return
        }

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
        revivalAttemptCount = 0
        lastRevivalAttemptTs = 0L
        lastXiaomiRecoveryTs = 0L
        lastSitSyncLatchTs = 0L
        lastSitLogTs = 0L
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
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsUpdateJob?.cancel()
            fgsUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
                try {
                    delay(200)
                    val type = getAvailableForegroundServiceType()
                    val msg = if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0) "Acoustic monitoring active." else "Tracking system active."
                    safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification(msg), type)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Failed to update foreground service type")
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun getAvailableForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasMicPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission && isRecentUiPulse()) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
            }
        }
        return type
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)
        
        sensorManager.setHighLoad(integrityMonitor.isCoolingModeActive)

        if (isA15) {
            systemMonitor.renewWakeLock()
        }

        if (isXiaomi && lastServiceTickRealtime > 0) {
            val tickGap = nowRealtime - lastServiceTickRealtime
            if (tickGap > XIAOMI_SUPPRESSION_THRESHOLD_MS && nowRealtime - lastXiaomiRecoveryTs > XIAOMI_RECOVERY_COOLDOWN_MS) {
                try {
                    lastXiaomiRecoveryTs = nowRealtime
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("HEURISTIC RECOVERY: Xiaomi suppression detected (Gap: ${tickGap}ms). Triggering pulse.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                        lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                    
                    gpsManager.reviveGps()
                    systemMonitor.renewWakeLock()
                    updateForegroundServiceType()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Xiaomi heuristic recovery pulse failed")
                }
            }
        }

        if (nowRealtime - lastStabilityAuditTs > GPS_STABILITY_AUDIT_INTERVAL_MS) {
            if (stabilityAuditFixCount > 0) {
                val reliability = 100.0 * (stabilityAuditFixCount - stabilityAuditViolationCount) / stabilityAuditFixCount
                if (reliability < GPS_STABILITY_RELIABILITY_THRESHOLD) {
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY AUDIT: Reliability ${String.format(Locale.getDefault(), "%.1f", reliability)}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes)", important = true,
                        lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                }
                stabilityAuditFixCount = 0
                stabilityAuditViolationCount = 0
            }
            lastStabilityAuditTs = nowRealtime
        }

        val flags = ServiceBehaviorUseCase.DeviceSpecialFlags(isS21FE = isS21FE, isXiaomi = isXiaomi, isA15 = isA15)
        val newInterval = behaviorUseCase.calculateGpsInterval(
            isCoolingMode = integrityMonitor.isCoolingModeActive,
            isSuspiciousMode = isSuspiciousMode,
            isStationary = sensorManager.isStationary(),
            isScreenOn = sensorManager.isScreenOn(),
            nowRealtime = nowRealtime,
            deviceSpecialFlags = flags
        )
        if (newInterval != currentGpsInterval) {
            val oldInterval = currentGpsInterval
            currentGpsInterval = newInterval
            gpsManager.setPollingInterval(newInterval)
            
            if (isA15 && oldInterval != -1L) {
                adaptationMuzzleJob?.cancel()
                isAdaptationMuzzled = true
                adaptationMuzzleJob = lifecycleScope.launch {
                    delay(ADAPTATION_SETTLING_MS)
                    isAdaptationMuzzled = false
                }
                
                if (oldInterval >= STATIONARY_GPS_POLLING_MS && newInterval <= A15_STABLE_GPS_POLLING_MS) {
                    gpsManager.kickGps()
                }
            }

            if (nowRealtime - lastGpsTransitionLogTs > GPS_TRANSITION_LOG_MUZZLE_MS) {
                lastGpsTransitionLogTs = nowRealtime
                logManager.logServiceEvent("GPS Frequency adapted to ${newInterval}ms", important = false)
            }
        }

        val isSocketConnected = networkManager.isConnected() && !transientDropDetected.getAndSet(false)
        networkManager.updateRelayStatus(isSocketConnected)
        
        val isViewerActive = remoteHandler.lastPeerActivityTs > 0 && (nowRealtime - remoteHandler.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRealtime, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isViewerActive, isInViolation = isSuspiciousMode)
        
        val silenceDelta = if (remoteHandler.lastPeerActivityTs > 0) nowRealtime - remoteHandler.lastPeerActivityTs else 0L
        val isSignalLoss = integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, true) 
        val isJammerSuspicionActive = (systemMonitor.jumpStateStartTs != 0L && (nowRealtime - systemMonitor.jumpStateStartTs) > JAMMER_DETECTION_THRESHOLD_MS)
        val isGpsStalledActive = (systemMonitor.gpsStallStartTs > 0L && (nowRealtime - systemMonitor.gpsStallStartTs) > GPS_STALL_THRESHOLD_MS)
        val isGpsGapActive = (nowRealtime - locationProcessor.getLastValidFixTs() > GPS_GAP_THRESHOLD_MS)
        
        if (isGpsStalledActive) {
            val proc = lastProcessedLocation
            val timeSinceLastRevival = nowRealtime - lastRevivalAttemptTs
            if (revivalAttemptCount < MAX_REVIVAL_ATTEMPTS && timeSinceLastRevival > GPS_REVIVAL_RETRY_INTERVAL_MS) {
                revivalAttemptCount++
                lastRevivalAttemptTs = nowRealtime
                gpsManager.reviveGps()
                logManager.logServiceEvent("GPS Stall: Revival attempt $revivalAttemptCount/$MAX_REVIVAL_ATTEMPTS triggered", important = true,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            } else if (revivalAttemptCount >= MAX_REVIVAL_ATTEMPTS && timeSinceLastRevival > GPS_REVIVAL_RETRY_INTERVAL_MS) {
                lastRevivalAttemptTs = nowRealtime
                logManager.logServiceEvent("CRITICAL: GPS_HARDWARE_LOCK - All revival attempts failed. Manual intervention required.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }
        }

        lastProcessedLocation?.let { proc ->
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes()
            val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isJammerSuspicionActive) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isGpsStalledActive) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isGpsGapActive) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (proc.status == SentinelStatus.JUMP || proc.status == SentinelStatus.JITTER) activeViolations.add(ALERT_ID_VISUAL_JUMP)
            if (lastSitDetected) activeViolations.add(ALERT_ID_TRACKER_CHAIR) 

            forensicUseCase.recordViolationMarkers(
                now = now,
                lat = proc.optimizedPoint.lat,
                lng = proc.optimizedPoint.lng,
                accuracy = proc.currentAccuracy,
                maxAccuracy = proc.maxAccuracy,
                activeViolations = activeViolations,
                unresolvedAlarms = unresolvedAlarms
            )
        }

        val peakDb = sensorManager.consumeAcousticPeak()
        val minDb = sensorManager.consumeAcousticMin()
        val peakShockVal = sensorManager.consumePeakVibration()
        val peakVz = sensorManager.consumePeakVerticalVelocity()
        val peakVzTs = sensorManager.consumePeakVerticalVelocityTs()
        val peakDz = sensorManager.consumePeakVerticalDisplacement()
        
        locationProcessor.updateSensorData(
            vibration = sensorManager.currentVibrationIndex, heading = sensorManager.currentCompassHeading, baroAlt = sensorManager.absoluteAltitude,
            lux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, powerTamper = integrityMonitor.isPowerTamperDetected,
            tiltDegrees = sensorManager.currentTiltDegrees, acousticDb = peakDb, peakShock = peakShockVal, acousticMinDb = minDb,
            peakVerticalVelocity = peakVz, peakVerticalVelocityTs = peakVzTs, 
            peakVerticalDisplacement = peakDz,
            plungeMatched = sensorManager.consumePlungeMatched(), 
            isSirenActive = false, isWarming = sensorManager.isWarming, manualAdaptiveFloor = sensorManager.adaptiveVibrationFloor,
            isMuzzled = isMuzzled,
            isA15 = isA15,
            nowRealtime = nowRealtime, nowWall = now
        )

        val rawSitDetected = locationProcessor.consumeSitDetected()
        lastSitDetected = rawSitDetected || (locationProcessor.getLastSitRealtime() > 0 && (nowRealtime - locationProcessor.getLastSitRealtime() < SUSPICIOUS_STATE_COOLDOWN_MS))
        
        if (rawSitDetected) {
            val proc = lastProcessedLocation
            lastSitSyncLatchTs = nowRealtime
            
            if (nowRealtime - lastSitLogTs > SIT_DUPLICATE_GUARD_MS) {
                lastSitLogTs = nowRealtime
                logManager.logServiceEvent("Sit Detected (Engine Pulse)", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }
            lifecycleScope.launch { repository.saveLong(MainRepository.LAST_SIT_TS_KEY, locationProcessor.getLastSitTs()) }
        }

        val latchedSitDetected = rawSitDetected || (lastSitSyncLatchTs > 0 && (nowRealtime - lastSitSyncLatchTs < SIT_TRANSMISSION_LATCH_MS))

        val tiltIdx = (sensorManager.currentTiltDegrees / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0)
        val baroIdx = (abs(sensorManager.relativeAltitude) / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0)

        val pendingReason = when {
            pendingAcousticViolation -> LocationPendingReason.ACOUSTIC_VIOLATION
            isJammerSuspicionActive -> LocationPendingReason.JAMMER_SUSPICION
            isGpsStalledActive -> LocationPendingReason.GPS_STALL
            isGpsGapActive -> LocationPendingReason.GPS_GAP
            isSignalLoss -> LocationPendingReason.SIGNAL_LOSS
            else -> LocationPendingReason.NONE
        }

        val proc = lastProcessedLocation
        
        val currentTrackerState = TrackerStateManager.updateState(
            isVisualJump = proc?.status == SentinelStatus.JUMP || proc?.status == SentinelStatus.JITTER,
            isTrajectoryPromoted = proc?.isTrajectoryPromoted ?: false,
            speed = proc?.filteredSpeed ?: 0.0,
            vibration = sensorManager.currentVibrationIndex,
            vibrationFloor = locationProcessor.getAdaptiveVibrationFloor(),
            isTrackerConnected = isSocketConnected,
            systemTimePulse = now
        )

        syncManager.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true, loc = lastKnownLocation, filtered = proc?.optimizedPoint,
            distToTracker = locationProcessor.getDistanceToTracker(), distToHome = locationProcessor.getNearestHomeDistance(),
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), filteredSpeed = proc?.filteredSpeed ?: 0.0,
            vibration = sensorManager.currentVibrationIndex, heading = sensorManager.currentCompassHeading, baroAlt = sensorManager.absoluteAltitude,
            lux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, isSuspicious = isSuspiciousMode, tiltDegrees = sensorManager.currentTiltDegrees,
            acousticDb = sensorManager.currentAcousticDb, isJump = (proc?.status == SentinelStatus.JUMP || proc?.status == SentinelStatus.JITTER), 
            isTrajectoryPromoted = proc?.isTrajectoryPromoted ?: false, jumpTier = proc?.jumpTier ?: 0, isJammer = isJammerSuspicionActive,
            isStalledRaw = proc?.isStalled ?: false, isStalledActive = isGpsStalledActive, 
            peakShock = locationProcessor.getPeakVibrationShock(),
            peakShockTs = locationProcessor.sentinel.peakVibrationShockTs, 
            luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
            adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = sensorManager.proximityIdx, proximityCm = sensorManager.debouncedProximityCm,
            proximityDebounceMs = sensorManager.proximityDebounceMs, vibrationRollingSum = sensorManager.vibrationRollingSum,
            micPending = false, isTamperDetected = false, isPowerTamper = integrityMonitor.isPowerTamperDetected, 
            isSitDetected = latchedSitDetected, isSitActive = lastSitDetected, lastSitTs = locationProcessor.getLastSitTs(), receiptRealtime = proc?.receiptRealtime ?: 0L,
            violationUptimeMs = sessionManager.violationUptimeMs, violationPercentage = sessionManager.getViolationPercentage(),
            verticalVelocity = sensorManager.currentVerticalVelocity, sitVz = locationProcessor.sentinel.lastSitVz, sitDz = locationProcessor.sentinel.lastSitDz, 
            sitBaro = locationProcessor.sentinel.lastSitBaro, sitTilt = locationProcessor.sentinel.lastSitTilt, sitShock = locationProcessor.sentinel.lastSitShock, 
            isClockRegression = proc?.isClockRegression ?: false, isLocationPending = (pendingReason != LocationPendingReason.NONE),
            locationPendingReason = pendingReason,
            lastValidFixRealtime = locationProcessor.getLastValidFixTs(),
            gnssDetail = latestGnssDetail,
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0), 
            tiltIdx = tiltIdx,
            baroIdx = baroIdx,
            isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive,
            batteryLevel = integrityMonitor.getBatteryLevel(), batteryTemp = integrityMonitor.batteryTemp, isCharging = integrityMonitor.isCharging,
            isAnchorLocked = proc?.isAnchorLocked ?: false,
            trackerState = currentTrackerState
        )

        val gpsTs = proc?.timestamp ?: 0L
        val gpsAge = if (gpsTs > 0) (now - gpsTs) else 3600000L
        historyManager.updateRibbons(
            now = now,
            lastTickTs = lastServiceTickTs,
            serviceTickCounter = serviceTickCounter,
            rtt = networkManager.getRtt(),
            peerSignal = remoteHandler.peerSignal,
            peerAvail = isSocketConnected && isViewerActive, 
            hasGps = gpsTs > 0,
            isTrackerMode = true,
            gpsIndex = TelemetryUtils.calculateGpsIndex(gpsAge, proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), lastKnownLocation?.extras?.getInt("satellites") ?: gpsManager.satellitesUsed).totalIndex,
            accuracy = proc?.currentAccuracy ?: locationProcessor.getLastProcessedAccuracy(),
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(),
            noiseIdx = ((sensorManager.currentAcousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB),
            luxIdx = (log10(sensorManager.currentLux + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0),
            vibeIdx = (sensorManager.currentVibrationIndex / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0),
            proxIdx = sensorManager.proximityIdx,
            liftIdx = (abs(sensorManager.relativeAltitude) / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0),
            snrIdx = (gpsManager.averageSnr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0),
            tiltIdx = tiltIdx,
            baroIdx = baroIdx,
            verticalVelocity = sensorManager.currentVerticalVelocity,
            sitVz = locationProcessor.sentinel.lastSitVz,
            sitDz = locationProcessor.sentinel.lastSitDz,
            sitBaro = locationProcessor.sentinel.lastSitBaro,
            sitTilt = locationProcessor.sentinel.lastSitTilt,
            sitShock = locationProcessor.sentinel.lastSitShock,
            isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge,
            isCoolingModeActive = integrityMonitor.isCoolingModeActive,
            speed = proc?.filteredSpeed ?: 0.0,
            bearing = (lastKnownLocation?.bearing?.toDouble() ?: 0.0),
            isSitDetected = latchedSitDetected,
            isSitActive = lastSitDetected,
            currentMa = integrityMonitor.getBatteryCurrent(),
            locationPendingReason = pendingReason,
            isAnchorLocked = proc?.isAnchorLocked ?: false
        )

        evaluateAlarmsInternal(nowRealtime, isSignalLoss, isJammerSuspicionActive, isGpsStalledActive, false, isViewerActive, pendingReason)

        val notificationInterval = if (isUiVisible()) TICK_INTERVAL_MS else NOTIFICATION_THROTTLE_MS
        if (now - lastNotificationUpdateTs >= notificationInterval) {
            lastNotificationUpdateTs = now
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
        isJammerSuspicion: Boolean, 
        isGpsStalling: Boolean, 
        isTamperDetected: Boolean,
        isViewerConnected: Boolean,
        pendingReason: LocationPendingReason
    ) {
        val proc = lastProcessedLocation
        val isSocketConnected = networkManager.isConnected()
        
        val xiaomiStatus = when(systemStatusProvider.isXiaomiSpecialPermissionGranted()) {
            XiaomiPermissionStatus.GRANTED -> EngineXiaomiStatus.GRANTED
            XiaomiPermissionStatus.DENIED -> EngineXiaomiStatus.DENIED
            XiaomiPermissionStatus.UNKNOWN -> EngineXiaomiStatus.UNKNOWN
        }
        
        val xiaomiAutostartStatus = when(getXiaomiAutostartStatus(this, cachedPkgName)) {
            XiaomiPermissionStatus.GRANTED -> EngineXiaomiStatus.GRANTED
            XiaomiPermissionStatus.DENIED -> EngineXiaomiStatus.DENIED
            XiaomiPermissionStatus.UNKNOWN -> EngineXiaomiStatus.UNKNOWN
        }

        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = nowRealtime, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = true,
                isRelayConnected = isSocketConnected, isTrackerConnected = isViewerConnected, 
                isTrackerVisualJump = (proc?.status == SentinelStatus.JUMP || proc?.status == SentinelStatus.JITTER), 
                isTrajectoryPromoted = proc?.isTrajectoryPromoted ?: false, jumpTier = proc?.jumpTier ?: 0, 
                isAdaptiveJump = proc?.isAdaptiveJump ?: false, 
                trackerLat = proc?.optimizedPoint?.lat ?: 0.0,
                trackerLng = proc?.optimizedPoint?.lng ?: 0.0, trackerAccuracy = proc?.currentAccuracy ?: 0.0, 
                maxTrackerAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), trackerLastGpsTs = proc?.timestamp ?: 0L,
                trackerLastValidFixTs = locationProcessor.getLastValidFixTs(),
                trackerSpeed = proc?.filteredSpeed ?: 0.0, trackerBattery = integrityMonitor.getBatteryLevel(), trackerTemp = integrityMonitor.batteryTemp,
                isHardwareOnline = true, isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()),
                isJammerSuspicion = isJammerSuspicion, isSignalLoss = isSignalLoss, isGpsStalling = isGpsStalling, isUiVisible = isUiVisible(),
                distToHomeAuthority = locationProcessor.getNearestHomeDistance(), maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), 
                isGpsGap = (nowRealtime - locationProcessor.getLastValidFixTs() > GPS_GAP_THRESHOLD_MS), isSuspicious = isSuspiciousMode, isTamperDetected = isTamperDetected, isPowerTamper = integrityMonitor.isPowerTamperDetected,
                trackerTiltDegrees = sensorManager.currentTiltDegrees, trackerAcousticDb = sensorManager.currentAcousticDb, trackerBaroAlt = sensorManager.absoluteAltitude,
                trackerLux = sensorManager.currentLux, isNear = sensorManager.isProximityNear, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = locationProcessor.getPeakVibrationShock(), trackerCurrentMa = integrityMonitor.getBatteryCurrent(),
                isSitActive = lastSitDetected, isLocationPending = (pendingReason != LocationPendingReason.NONE), 
                locationPendingReason = pendingReason,
                isPowerSaveMode = integrityMonitor.isPowerSaveModeActive,
                standbyBucket = integrityMonitor.currentStandbyBucket, netInterface = integrityMonitor.getActiveNetworkInterface(), isStorageLow = integrityMonitor.isStorageLow,
                isStorageCritical = integrityMonitor.isStorageCritical, isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive,
                discoveryPhase = null,
                isXiaomiDevice = isXiaomi,
                xiaomiStatus = xiaomiStatus,
                xiaomiAutostartStatus = xiaomiAutostartStatus,
                isXiaomiManualOverride = isXiaomiManualOverride,
                snrSnapshot = gpsManager.averageSnr,
                vibeSnapshot = sensorManager.currentVibrationIndex,
                isAnchorLocked = proc?.isAnchorLocked ?: false
            )
        }
    }

    override fun getRequiredTickInterval(): Long {
        return if (isUiVisible() || isSuspiciousMode) TICK_INTERVAL_MS else TICK_INTERVAL_SLOW_MS
    }

    private fun onLocationChanged(location: Location) {
        val nowRealtime = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()

        if (currentGpsInterval == HIGH_FREQUENCY_GPS_POLLING_MS && lastGpsFixRealtime > 0) {
            val gap = nowRealtime - lastGpsFixRealtime
            stabilityAuditFixCount++
            if (gap > GPS_STABILITY_GAP_THRESHOLD_MS) {
                stabilityAuditViolationCount++
                val proc = lastProcessedLocation
                logManager.logServiceEvent("STABILITY GAP: ${gap}ms detected during 10Hz polling.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            }
        }
        lastGpsFixRealtime = nowRealtime

        val processed = locationProcessor.processGpsPoint(
            lat = location.latitude, lng = location.longitude, alt = location.altitude, androidSpeedMps = location.speed.toDouble(), 
            gpsTs = location.time, accuracy = location.accuracy.toDouble(), bearing = location.bearing.toDouble(), snr = gpsManager.averageSnr, 
            satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed, isViewerTrail = false, lastGpsTs = sessionManager.lastGpsTs, 
            isLocal = true, providedAdaptiveVibrationFloor = sensorManager.adaptiveVibrationFloor, 
            isSuspicious = isSuspiciousMode,
            isMuzzled = isMuzzled,
            isA15 = isA15,
            isAdaptationMuzzled = isAdaptationMuzzled,
            nowRealtime = nowRealtime,
            nowWall = nowWall
        )
        
        processed.suppressionNote?.let { note ->
            logManager.logServiceEvent(note, important = false, lat = location.latitude, lng = location.longitude, accuracy = location.accuracy.toDouble())
        }
        
        if (!processed.isClockRegression) sessionManager.lastGpsTs = location.time
        if (!processed.isClockRegression && !processed.isStalled) { 
            systemMonitor.gpsStallStartTs = 0L 
            revivalAttemptCount = 0; lastRevivalAttemptTs = 0L
        }
        
        if (processed.status == SentinelStatus.JUMP || processed.status == SentinelStatus.OUTLIER || processed.status == SentinelStatus.JITTER) {
            if (systemMonitor.jumpStateStartTs == 0L) systemMonitor.jumpStateStartTs = timeProvider.elapsedRealtime()
        } else if (processed.status == SentinelStatus.VALID) {
            systemMonitor.jumpStateStartTs = 0L
            pendingAcousticViolation = false
        }
        
        lastKnownLocation = location; lastProcessedLocation = processed

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
    }

    override fun onDestroy() {
        if (this::sensorManager.isInitialized) sensorManager.stop()
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        alarmEvalJob?.cancel()
        settingsJob?.cancel()
        muzzleReleaseJob?.cancel()
        adaptationMuzzleJob?.cancel()
        if (this::commandRouter.isInitialized) commandRouter.unregister()
        super.onDestroy()
    }

    private fun setupPhysicalFastPaths() {
        val acousticJump = if (isA15) ACOUSTIC_THRESHOLD_DB_JUMP_A15 else ACOUSTIC_THRESHOLD_DB_JUMP
        sensorManager.setAcousticFastPath(locationProcessor.getAcousticFloorDb(), acousticJump, ACOUSTIC_MIN_THRESHOLD_DB) {
            triggerSuspiciousMode("Acoustic") 
            pendingAcousticViolation = true
        }
        sensorManager.setLightFastPath(locationProcessor.getLuxBaseline(), LIGHT_THRESHOLD_LUX_JUMP) {
            triggerSuspiciousMode("Light")
        }
    }

    private fun triggerSuspiciousMode(source: String) {
        if (!isSuspiciousMode) { 
             val proc = lastProcessedLocation
             logManager.logServiceEvent("Suspicious mode: Physical Tamper ($source)", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                 lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
        }
        isSuspiciousMode = behaviorUseCase.updateSuspiciousMode(isSuspiciousMode, true, false, timeProvider.elapsedRealtime())
    }
}
