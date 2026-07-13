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
import javax.inject.Inject
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * v9.3.18:
 * - R404: Legacy Relay URL Fallback Remediation. Synchronized with MainRepository.DEFAULT_RELAY_URL.
 * - R403: Startup ANR Remediation. Integrated centralized getActiveHeartbeatInterval 
 *   to ensure dynamic recovery from 2s (startup) to 1s (standard) heartbeat.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    @Inject lateinit var sensorManager: AppSensorManager
    @Inject lateinit var behaviorUseCase: ServiceBehaviorUseCase
    
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var settingsJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null
    
    private var isMuzzled = false
    private var muzzleReleaseJob: Job? = null

    // Issue #038: Adaptation Muzzle State
    private var isAdaptationMuzzled = false
    private var adaptationMuzzleJob: Job? = null
    
    private var lastXiaomiRecoveryTs = 0L
    
    private var isS21FE = false
    private var isXiaomi = false
    private var isA15 = false

    private var lastGpsFixRealtime = 0L
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
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
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
                override fun onPeerPulse(id: String) {
                    handleViewerPulse(id)
                }
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

            networkManagerWrapper.setCallback { data -> 
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

    private fun setupPhysicalFastPaths() {
        sensorManager.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(),
            spikeThreshold = 15.0,
            minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent("Acoustic Spike Detected (FastPath)", false)
            }
        )
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) return
        repository.updateRemoteActivity(timeProvider.currentTimeMillis())

        if ((configManager.viewerId == MainRepository.DEFAULT_VIEWER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
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
        lastXiaomiRecoveryTs = 0L
        logManager.logServiceEvent("Session Terminated", false)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification("Tracking system active."), type)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && isRecentUiPulse()) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
            }
        }
        return type
    }

    override fun getRequiredTickInterval(): Long {
        val elapsed = timeProvider.elapsedRealtime() - serviceStartRealtime
        return getActiveHeartbeatInterval(elapsed, isHighResDevice = isA15)
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)
        sensorManager.setHighLoad(integrityMonitor.isCoolingModeActive)

        if (isA15) systemMonitor.renewWakeLock()

        if (isXiaomi && lastServiceTickRealtime > 0) {
            val tickGap = nowRealtime - lastServiceTickRealtime
            if (tickGap > XIAOMI_SUPPRESSION_THRESHOLD_MS && nowRealtime - lastXiaomiRecoveryTs > XIAOMI_RECOVERY_COOLDOWN_MS) {
                lastXiaomiRecoveryTs = nowRealtime
                val proc = lastProcessedLocation
                logManager.logServiceEvent("HEURISTIC RECOVERY: Xiaomi suppression detected.", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                networkManager.connect(configManager.relayUrl)
            }
        }
        
        lastServiceTickTs = now
        lastServiceTickRealtime = nowRealtime
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        
        if (isAdaptationMuzzled) return@withContext

        val hasLocation = lastKnownLocation != null
        val sessionActive = sessionManager.getViewerCount() > 0 || isRecentUiPulse()

        if (sessionActive && hasLocation) {
            val location = lastKnownLocation!!
            val lat = location.latitude
            val lng = location.longitude
            val acc = location.accuracy.toDouble()
            
            val processed = locationProcessor.processGpsPoint(
                lat = lat, lng = lng, alt = location.altitude, androidSpeedMps = location.speed.toDouble(),
                gpsTs = location.time, accuracy = acc, bearing = location.bearing.toDouble(),
                snr = latestGnssDetail?.satellites?.map { it.cn0 }?.average() ?: 0.0,
                satsUsed = latestGnssDetail?.satellites?.count { it.usedInFix } ?: 0,
                isViewerTrail = false, lastGpsTs = lastGpsFixRealtime, isLocal = true,
                isA15 = isA15, isAdaptationMuzzled = isAdaptationMuzzled,
                nowWall = now, nowRealtime = nowRealtime
            )
            lastProcessedLocation = processed

            alarmManager.evaluateAlarms(
                now = now, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime,
                isTrackerMode = true, isRelayConnected = networkManager.isConnected(), isTrackerConnected = true,
                isTrackerVisualJump = processed.status == SentinelStatus.JUMP,
                isTrajectoryPromoted = processed.isTrajectoryPromoted, jumpTier = processed.jumpTier,
                isAdaptiveJump = processed.isAdaptiveJump, trackerLat = processed.optimizedPoint.lat,
                trackerLng = processed.optimizedPoint.lng, trackerAccuracy = processed.currentAccuracy,
                maxTrackerAccuracy = processed.maxAccuracy, trackerLastGpsTs = location.time,
                trackerLastValidFixTs = locationProcessor.getLastValidFixTs(), trackerSpeed = processed.filteredSpeed,
                trackerBattery = integrityMonitor.getBatteryLevel(), trackerTemp = integrityMonitor.batteryTemp,
                isHardwareOnline = integrityMonitor.isHardwareOnline(), isLocalInternetLoss = false,
                isJammerSuspicion = processed.jammerDetected, isSignalLoss = false, isGpsStalling = processed.isStalled,
                isUiVisible = isUiVisible(), distToHomeAuthority = processed.distToHome,
                maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), isGpsGap = false,
                isSuspicious = processed.status == SentinelStatus.SENSOR_SUSPICIOUS, isTamperDetected = processed.tamperDetected,
                isPowerTamper = integrityMonitor.isPowerTamperDetected, trackerTiltDegrees = sensorManager.currentTiltDegrees,
                trackerAcousticDb = sensorManager.currentAcousticDb, trackerBaroAlt = sensorManager.absoluteAltitude,
                trackerBaroAltEma = locationProcessor.getBaroBaseline(),
                trackerLux = sensorManager.currentLux, isNear = sensorManager.isProximityNear,
                luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = locationProcessor.getPeakVibrationShock(),
                trackerCurrentMa = integrityMonitor.getBatteryCurrent(), isAnchorLocked = processed.isAnchorLocked
            )
            
            val isUrgent = alarmManager.hasUnresolvedAlarms() || processed.status == SentinelStatus.JUMP
            
            syncManager.pushCurrentStatus(
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true,
                loc = location, filtered = processed.optimizedPoint, distToTracker = null, distToHome = processed.distToHome,
                maxAccuracy = processed.maxAccuracy, filteredSpeed = processed.filteredSpeed, vibration = sensorManager.currentVibrationIndex,
                heading = sensorManager.currentCompassHeading, baroAlt = sensorManager.absoluteAltitude, lux = sensorManager.currentLux,
                isNear = sensorManager.isProximityNear, isSuspicious = processed.status == SentinelStatus.SENSOR_SUSPICIOUS,
                tiltDegrees = sensorManager.currentTiltDegrees, acousticDb = sensorManager.currentAcousticDb,
                isJump = processed.status == SentinelStatus.JUMP, isTrajectoryPromoted = processed.isTrajectoryPromoted,
                jumpTier = processed.jumpTier, isJammer = processed.jammerDetected, isStalledRaw = false,
                isStalledActive = processed.isStalled, peakShock = sensorManager.consumePeakVibration(),
                peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = sensorManager.proximityIdx,
                proximityCm = sensorManager.currentProximityCm, proximityDebounceMs = sensorManager.proximityDebounceMs,
                vibrationRollingSum = sensorManager.vibrationRollingSum, micPending = false,
                isTamperDetected = processed.tamperDetected, isPowerTamper = integrityMonitor.isPowerTamperDetected,
                isSitDetected = sensorManager.consumePlungeMatched(), isSitActive = false,
                lastSitTs = locationProcessor.getLastSitTs(), receiptRealtime = nowRealtime, violationUptimeMs = sessionManager.violationUptimeMs,
                violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = sensorManager.currentVerticalVelocity,
                sitVz = sensorManager.consumePeakVerticalVelocity(), sitDz = sensorManager.consumePeakVerticalDisplacement(),
                sitBaro = sensorManager.absoluteAltitude, sitTilt = sensorManager.currentTiltDegrees, sitShock = sensorManager.consumePeakVibration(),
                isClockRegression = processed.isClockRegression, isLocationPending = false, locationPendingReason = LocationPendingReason.NONE,
                lastValidFixRealtime = locationProcessor.getLastValidFixTs(), gnssDetail = latestGnssDetail, snrIdx = 0.0, tiltIdx = 0.0, baroIdx = 0.0,
                isBatterySteepDischarge = integrityMonitor.isBatterySteepDischarge, isCoolingModeActive = integrityMonitor.isCoolingModeActive,
                batteryLevel = integrityMonitor.getBatteryLevel(), batteryTemp = integrityMonitor.batteryTemp, isCharging = integrityMonitor.isCharging,
                isAnchorLocked = processed.isAnchorLocked, trackerState = if (processed.filteredSpeed > 0.5) TrackerState.MOVING else TrackerState.PARKING
            )
            
            if (isUrgent && isS21FE) {
                isAdaptationMuzzled = true
                adaptationMuzzleJob?.cancel()
                adaptationMuzzleJob = lifecycleScope.launch { delay(3000L); isAdaptationMuzzled = false }
            }
        }
    }

    private fun onLocationChanged(location: Location) {
        lastKnownLocation = location
        lastGpsFixRealtime = timeProvider.elapsedRealtime()
        systemMonitor.gpsStallStartTs = 0L
        
        if (lastStabilityAuditTs == 0L) lastStabilityAuditTs = timeProvider.elapsedRealtime()
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        settingsJob?.cancel()
        adaptationMuzzleJob?.cancel()
        commandRouter.unregister()
        sensorManager.stop()
        networkManager.stop()
        syncManager.stopSyncLoop()
        super.onDestroy()
    }

    companion object {
        private const val MUZZLE_HYSTERESIS_MS = 2000L
        private const val MUZZLE_HYSTERESIS_A15_MS = 5000L
        private const val XIAOMI_SUPPRESSION_THRESHOLD_MS = 45000L
        private const val XIAOMI_RECOVERY_COOLDOWN_MS = 60000L
        private const val ALERT_ID_TRACKER_POWER = "TRACKER_POWER"
    }
}
