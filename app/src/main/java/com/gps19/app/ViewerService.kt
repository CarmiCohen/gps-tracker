package com.gps19.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import kotlin.math.*

/**
 * ViewerService: Background monitoring for the Viewer role.
 * July.17.02:
 * - Issue #R993: Increased notification update interval to NOTIFICATION_THROTTLE_MS (30s).
 * July.17.00:
 * - Issue #526: Definitive performance hardening. All dependencies pulled via lazy 
 *   delegates from BaseMonitorService to prevent Main thread hangs on Samsung A15.
 */
class ViewerService : BaseMonitorService() {

    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    // R951: Stability Audit State
    private var lastGpsFixRealtime = 0L
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L

    private var capabilities = HardwareCapabilities()

    private val localProcessorListener = object : LocationProcessorListener {
        override fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, status: SentinelStatus, timestamp: Long, accuracy: Double, maxAccuracy: Double) {
            repository.saveTrailPoint(lat, lng, isViewerTrail, status, timestamp, accuracy = accuracy, maxAccuracy = maxAccuracy)
        }
        override fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double?, vibe: Double?) {
            val specialColor = if (isSpecial || message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
            logManager.logServiceEvent(message, isImportant, isSpecial = isSpecial || message.contains("Merge-on-Stale"), specialColor = specialColor, lat = lat, lng = lng, accuracy = accuracy, snr = snr, vibe = vibe)
        }
        override fun onMaxAccuracyChanged(accuracy: Double) {
            repository.saveDoubleSync(MainRepository.MAX_ACCURACY_KEY, accuracy)
        }
        override fun onGpsStallDetected(ts: Long) {}
    }

    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            val trackerId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            val viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            
            configManager.deviceId = trackerId
            configManager.viewerId = viewerId
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
            configManager.isTrackerMode = false

            val perms = systemStatusProvider.getPermissionState()
            capabilities = HardwareCapabilities(
                hasBackgroundRestriction = perms.hasBackgroundRestriction,
                backgroundStatus = perms.backgroundStatus,
                autostartStatus = perms.autostartStatus,
                requiresWakeLockRenewal = perms.requiresWakeLockRenewal,
                isManualOverrideActive = perms.isManualOverride
            )

            alarmManager.setListener(object : AppAlarmManager.Listener {
                override fun onLogEvent(type: String, message: String, important: Boolean, extremeValue: Double?, logId: String?, durationMs: Long, isSpecial: Boolean, specialColor: Int?, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double, snr: Double?, vibe: Double?) {
                    logManager.submitToLogSink(message, type, important, extremeValue, logId, durationMs, isSpecial, specialColor, lat, lng, accuracy, maxAccuracy, snr, vibe)
                }
            })

            integrityMonitor.setListener(object : IntegrityMonitor.Listener {
                override fun onViolationSustained(type: String) {}
                override fun onViolationResolved(type: String) {}
                override fun onLogEvent(message: String, important: Boolean) {
                    logManager.logServiceEvent(message, important)
                }
            })
            
            locationProcessor.setListener(localProcessorListener)
            
            delay(1000)
            
            val savedMaxAcc = repository.getDouble(MainRepository.MAX_ACCURACY_KEY, 0.0)
            val trackerState = repository.loadTrackerState()
            val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
            val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60.0)
            locationProcessor.loadState(savedMaxAcc, trackerState, homePoints, maxDist)

            delay(1000)
            connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, false)
            
            connectivitySuite.setPeerListener(object : ConnectivitySuite.PeerListener {
                override fun onPeerPulse(id: String) { handleTrackerPulse(id) }
            })

            historyManager.setListener(object : HistoryManager.Listener {
                override fun onLogEvent(message: String, important: Boolean) {
                    logManager.logServiceEvent(message, important)
                }
            })
            historyManager.initialize(lifecycleScope)

            delay(1000)
            commandRouter.setListener(object : CommandRouter.Listener {
                override fun onViewerPulse(id: String) { handleTrackerPulse(id) }
                override fun onWatchdogTrigger() { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                override fun onUiPulse() { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                override fun onUiVisibilityChanged(visible: Boolean) { onUiVisibilityChangedInternal(visible) }
                override fun onTransientDrop(drop: Boolean) { transientDropDetected.set(drop) }
                override fun onResetTimers() { resetServiceTimers() }
                override fun onSyncSensors() { }
                override fun onTriggerForensicTest() { }
            })
            commandRouter.register()
            commandRouter.startObservingCommands(lifecycleScope)

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
                        locationProcessor.setHomePoints(points.map { EngineGeoPoint(it.latitude, it.longitude) } )
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

            startTickLoop()
            
            withContext(Dispatchers.Main) {
                updateForegroundServiceType()
            }
            
            logManager.logServiceEvent("Viewer Engine Online (Staggered)", important = true)
        }
    }

    private fun onLocationChanged(location: Location) {
        val nowRealtime = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()

        val lat = location.latitude
        val lng = location.longitude
        val alt = location.altitude
        val speed = location.speed.toDouble()
        val acc = location.accuracy.toDouble()
        val bearing = location.bearing.toDouble()

        val currentHeartbeat = getRequiredTickInterval()
        if (lastGpsFixRealtime > 0) {
            val gap = nowRealtime - lastGpsFixRealtime
            stabilityAuditFixCount++
            if (gap > currentHeartbeat + GPS_STABILITY_GAP_THRESHOLD_MS) {
                stabilityAuditViolationCount++
                val proc = lastProcessedLocation
                logManager.logServiceEvent("STABILITY GAP (V): ${gap}ms detected during logic pulse.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = acc)
            }
        }
        lastGpsFixRealtime = nowRealtime

        val processed = locationProcessor.processGpsPoint(
            lat = lat,
            lng = lng,
            alt = alt,
            androidSpeedMps = speed,
            gpsTs = location.time,
            accuracy = acc,
            bearing = bearing,
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

        val health = integrityMonitor.currentHealth
        repository.updateLocation(LocationUpdate(
            lat = lat,
            lng = lng,
            alt = alt,
            speed = speed,
            accuracy = acc,
            bearing = bearing,
            battery = health.batteryLevel,
            temp = health.batteryTemp,
            isCharging = health.isCharging,
            gpsTs = location.time,
            ts = nowWall,
            isMe = true,
            satsView = gpsManager.satellitesInView,
            satsUsed = location.extras?.getInt("satellites") ?: gpsManager.satellitesUsed,
            maxAccuracy = processed.maxAccuracy,
            currentMa = health.currentMa,
            lastValidFixRealtime = locationProcessor.getLastValidFixTs(),
            status = processed.status
        ))

        val trackerAnchor = object : SpatialAnchor {
            override val lat = connectivitySuite.trackerLat
            override val lng = connectivitySuite.trackerLng
            override val alt = connectivitySuite.trackerBaroAlt
            override val gpsTs = connectivitySuite.trackerLastGpsTs
            override val ts = 0L 
        }
        
        locationProcessor.updateCalculatedDistances(lat, lng, true, trackerAnchor)
    }

    private fun handleTrackerPulse(id: String) {
        if (!SignalingConstants.isValidTrackerId(id)) {
            Timber.w("Rejecting invalid Tracker ID from pulse: $id")
            return
        }

        if ((configManager.viewerId == MainRepository.DEFAULT_TRACKER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
            configManager.viewerId = id
            connectivitySuite.updateIdentity(id, configManager.viewerId, false)
            lifecycleScope.launch { repository.saveString(MainRepository.VIEWER_ID_KEY, id) }
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
        return TICK_INTERVAL_MS
    }

    override suspend fun processTick(now: Long, nowRealtime: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRealtime)
        val health = integrityMonitor.currentHealth
        repository.updateHealth(health)

        if (timeProvider.elapsedRealtime() > 0) {
             systemMonitor.renewWakeLock()
        }

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

        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isTrackerActive = connectivitySuite.lastPeerActivityTs > 0 && (nowRealtime - connectivitySuite.lastPeerActivityTs < WATCH_TIMEOUT_MS)
        sessionManager.updateTick(nowRealtime, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isTrackerActive, isInViolation = false)

        val silenceDelta = if (connectivitySuite.lastPeerActivityTs > 0) nowRealtime - connectivitySuite.lastPeerActivityTs else 0L
        val isSignalLoss = !integrityMonitor.checkSignalIntegrity(nowRealtime, silenceDelta, false)
        val isTrackerJammerSuspicion = connectivitySuite.isTrackerJammerSuspicion
        val isTrackerStalled = connectivitySuite.trackerGpsStallStartTs > 0L && (nowRealtime - connectivitySuite.trackerGpsStallStartTs > GPS_STALL_THRESHOLD_MS)
        val isTrackerGap = connectivitySuite.trackerLastValidFixRealtime > 0L && (nowRealtime - connectivitySuite.trackerLastValidFixRealtime > GPS_GAP_THRESHOLD_MS)

        if (connectivitySuite.trackerLat != 0.0 && connectivitySuite.trackerLng != 0.0) {
            val unresolvedAlarms = alarmManager.getUnresolvedAlarmTypes()
            val activeViolations = mutableSetOf<String>()
            if (isSignalLoss) activeViolations.add(ALERT_ID_SIGNAL_LOSS)
            if (isTrackerJammerSuspicion) activeViolations.add(ALERT_ID_JUMP_ALERT)
            if (isTrackerStalled) activeViolations.add(ALERT_ID_GPS_STALL)
            if (isTrackerGap) activeViolations.add(ALERT_ID_TRACKER_GAP)
            if (connectivitySuite.isTrackerVisualJump) activeViolations.add(ALERT_ID_VISUAL_JUMP)

            forensicUseCase.recordViolationMarkers(
                now = now,
                lat = connectivitySuite.trackerLat,
                lng = connectivitySuite.trackerLng,
                accuracy = connectivitySuite.trackerAccuracy,
                maxAccuracy = connectivitySuite.trackerMaxAccuracy,
                activeViolations = activeViolations,
                unresolvedAlarms = unresolvedAlarms
            )
        }

        val distToTracker = locationProcessor.getDistanceToTracker() ?: 0.0
        val distToHome = locationProcessor.getNearestHomeDistance() ?: 0.0
        val proc = lastProcessedLocation

        connectivitySuite.pushCurrentStatus(
            deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = false, 
            loc = lastKnownLocation, filtered = proc?.optimizedPoint,
            distToTracker = distToTracker, distToHome = distToHome, 
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(), 
            filteredSpeed = proc?.filteredSpeed ?: 0.0,
            vibration = 0.0, heading = 0.0, baroAlt = 0.0,
            lux = 0.0, isNear = true, tiltDegrees = 0.0, acousticDb = 0.0,
            jumpTier = 0, isJammer = false, isStalled = false,
            peakShock = 0.0, peakShockTs = 0L, luxBaseline = 0.0, acousticFloorDb = 0.0, adaptiveVibrationFloor = 0.12,
            proxIdx = 1.0, proximityCm = -1.0, 
            proximityDebounceMs = 0L, vibrationRollingSum = 0.0,
            isTamperDetected = false, isPowerTamper = health.isPowerTamper,
            receiptRealtime = proc?.receiptRealtime ?: 0L,
            violationUptimeMs = 0L, violationPercentage = 0.0,
            isClockRegression = proc?.isClockRegression ?: false,
            isLocationPending = false, locationPendingReason = LocationPendingReason.NONE, lastValidFixRealtime = locationProcessor.getLastValidFixTs(),
            gnssDetail = latestGnssDetail,
            isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive,
            batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp, isCharging = health.isCharging,
            status = proc?.status ?: SentinelStatus.VALID,
            isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical,
            isPowerSaveMode = health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface
        )

        val gpsTs = proc?.timestamp ?: 0L
        historyManager.updateRibbons(
            now = now,
            lastTickTs = lastServiceTickTs,
            serviceTickCounter = serviceTickCounter,
            rtt = connectivitySuite.getRtt(),
            peerSignal = 10,
            peerAvail = isSocketConnected && isTrackerActive,
            hasGps = gpsTs > 0,
            isTrackerMode = false,
            accuracy = proc?.currentAccuracy ?: locationProcessor.getLastProcessedAccuracy(),
            maxAccuracy = proc?.maxAccuracy ?: locationProcessor.getMaxTrackerAccuracy(),
            isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive,
            speed = proc?.filteredSpeed ?: 0.0,
            bearing = (lastKnownLocation?.bearing?.toDouble() ?: 0.0),
            currentMa = health.currentMa,
            locationPendingReason = LocationPendingReason.NONE
        )

        evaluateAlarmsInternal(nowRealtime, isSignalLoss, isTrackerJammerSuspicion, isTrackerStalled, isTrackerGap, isTrackerActive)

        if (now - lastNotificationUpdateTs >= NOTIFICATION_THROTTLE_MS) {
            lastNotificationUpdateTs = now
            notificationManager.updatePulse(
                sats = gpsManager.satellitesUsed,
                battery = health.batteryLevel,
                isSecure = !alarmManager.hasUnresolvedAlarms(),
                isPowerSave = health.isPowerSaveMode
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
        val isSocketConnected = connectivitySuite.isConnected()
        val localHealth = integrityMonitor.currentHealth
        
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = nowRealtime, serviceStartTs = serviceStartRealtime, appStartTime = sessionManager.appStartTime, isTrackerMode = false,
                isRelayConnected = isSocketConnected, isTrackerConnected = isTrackerConnected,
                status = connectivitySuite.trackerStatus,
                isJammer = isTrackerJammerSuspicion,
                jumpTier = connectivitySuite.trackerJumpTier,
                trackerLat = connectivitySuite.trackerLat, trackerLng = connectivitySuite.trackerLng, trackerAccuracy = connectivitySuite.trackerAccuracy,
                maxTrackerAccuracy = connectivitySuite.trackerMaxAccuracy, trackerLastGpsTs = connectivitySuite.trackerLastGpsTs,
                trackerLastValidFixTs = connectivitySuite.trackerLastValidFixRealtime,
                trackerSpeed = connectivitySuite.trackerSpeed, trackerBattery = connectivitySuite.trackerBattery, trackerTemp = connectivitySuite.trackerTemp,
                isHardwareOnline = localHealth.isHardwareOnline, 
                isLocalInternetLoss = !integrityMonitor.checkInternetIntegrity(timeProvider.elapsedRealtime()),
                isSignalLoss = isSignalLoss, isGpsStalling = isTrackerStalled, isUiVisible = isUiVisible(),
                distToHomeAuthority = connectivitySuite.trackerDistToHome, maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(),
                isGpsGap = isTrackerGap, isTamperDetected = connectivitySuite.isTrackerTamperDetected,
                isPowerTamper = connectivitySuite.isTrackerPowerTamper, trackerTiltDegrees = connectivitySuite.trackerTiltDegrees,
                trackerAcousticDb = connectivitySuite.trackerAcousticDb, trackerBaroAlt = connectivitySuite.trackerBaroAlt, trackerLux = connectivitySuite.trackerLux,
                isNear = connectivitySuite.isTrackerNear, luxBaseline = connectivitySuite.trackerLuxBaseline, acousticFloorDb = connectivitySuite.trackerAcousticFloorDb,
                adaptiveVibrationFloor = connectivitySuite.trackerAdaptiveVibrationFloor, peakVibrationShock = connectivitySuite.trackerPeakVibrationShock,
                trackerCurrentMa = connectivitySuite.trackerCurrentMa, isLocationPending = connectivitySuite.isTrackerLocationPending,
                locationPendingReason = connectivitySuite.trackerLocationPendingReason, 
                isPowerSaveMode = connectivitySuite.isTrackerPowerSaveMode,
                standbyBucket = connectivitySuite.trackerStandbyBucket, 
                netInterface = connectivitySuite.trackerNetInterface, 
                isStorageLow = connectivitySuite.isTrackerStorageLow,
                isStorageCritical = connectivitySuite.isTrackerStorageCritical, 
                isBatterySteepDischarge = connectivitySuite.isTrackerBatterySteepDischarge,
                isCoolingModeActive = connectivitySuite.isTrackerCoolingModeActive,
                discoveryPhase = null, capabilities = capabilities,
                snrSnapshot = gpsManager.averageSnr, vibeSnapshot = 0.0
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        alarmEvalJob?.cancel()
        settingsJob?.cancel()
        super.onDestroy()
    }
}
