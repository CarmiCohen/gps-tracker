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
import kotlin.math.*

/**
 * TrackerService: The "Black Box" background process.
 * July.25.11:
 * - Issue #590: Updated MbrainHardwareManager calls to pass timeProvider for 
 *   unified latency monitoring.
 * July.25.02:
 * - Issue #543: Integrated MbrainHardwareManager JNI bridge. Service now attempts 
 *   to initialize libmbrainSDK on supported hardware to harden stay-alive budget.
 * - Issue #113: Refined A15 Poke. JNI punchHardware() now preferred over 
 *   generic WakeLock cycling if the native library is available.
 */
@AndroidEntryPoint
class TrackerService : BaseMonitorService() {

    private var gpsCollectionJob: Job? = null
    private var gnssDetailJob: Job? = null
    private var settingsJob: Job? = null
    private var alarmEvalJob: Job? = null
    
    private var lastKnownLocation: Location? = null
    private var lastProcessedLocation: LocationProcessor.ProcessedLocation? = null
    private var latestGnssDetail: GnssDetail? = null

    private var lastGpsSpeed = 0.0
    private var lastGpsAccuracy = 0.0
    private var lastGpsBearing = 0.0
    
    private var lastHardwareRecoveryTs = 0L
    private var capabilities = HardwareCapabilities()

    private var lastGpsFixRealtime = 0L
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L
    
    private var lastFastPathAcousticSpikeTs = 0L

    private var isPowerSaveActive = false
    private var lastPowerSaveCheckRt = 0L

    // Issue #113: Samsung A15 Poke Logic
    private var lastA15PokeRt = 0L
    private val A15_POKE_INTERVAL_MS = 10_000L

    // Issue #535: FGS Update Throttling
    private var lastFgsUpdateRt = 0L
    private val FGS_TYPE_UPDATE_THROTTLE_MS = 10_000L

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
        override fun onChairBaselineChanged(baseline: Double) {
            val proc = lastProcessedLocation
            logManager.logServiceEvent("Passive Zeroing: Chair baseline calibrated to ${String.format(Locale.getDefault(), "%.1f", baseline)}°",
                lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
            lifecycleScope.launch { repository.saveDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, baseline) }
        }
        override fun onGpsStallDetected(rt: Long) {
            if (systemMonitor.gpsStallStartTs == 0L) systemMonitor.gpsStallStartTs = rt
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Stealth Enforcement: Force silent notification channels
        notificationManager.setTrackerMode(true)
        
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, timeProvider.currentTimeMillis())

        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            configManager.deviceId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            configManager.viewerId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            configManager.relayUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
            configManager.isTrackerMode = true
            
            refreshCapabilitiesInternal()

            // Issue #543: Initialize Mbrain Native SDK if available
            if (capabilities.isA15Device && MbrainHardwareManager.isAvailable()) {
                val res = MbrainHardwareManager.initMbrain(timeProvider, configManager.deviceId, 0)
                logManager.logServiceEvent("HARDWARE: libmbrainSDK initialized (Result: $res)", important = true)
            }

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
                override fun onViolationResolved(type: String) {
                    if (type == ALERT_ID_TRACKER_POWER) {
                        alarmManager.setPowerAlarmPending(false)
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
            val savedLastSitTs = repository.getLong(MainRepository.LAST_SIT_TS_KEY, 0L)
            val savedBaseline = repository.getDouble(MainRepository.CHAIR_BASELINE_TILT_KEY, -1000.0)
            val trackerState = repository.loadTrackerState()
            val homePoints = repository.loadHomePoints().map { EngineGeoPoint(it.latitude, it.longitude) }
            val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, 60.0)
            locationProcessor.loadState(savedMaxAcc, savedLastSitTs, savedBaseline, trackerState, homePoints, maxDist)

            // Issue #527: Restore persisted alarm state
            val savedAlarms = repository.getLastAlarmsJson()
            alarmManager.restoreState(savedAlarms)

            appSensorManager.setHardwareFailureCallback { reason ->
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

            appSensorManager.start()

            delay(1000)
            connectivitySuite.start(configManager.relayUrl, configManager.deviceId, configManager.viewerId, true)
            
            connectivitySuite.setPeerListener(object : ConnectivitySuite.PeerListener {
                override fun onPeerPulse(id: String) {
                    handleViewerPulse(id)
                }
            })

            commandRouter.setListener(object : CommandRouter.Listener {
                override fun onViewerPulse(id: String) = handleViewerPulse(id)
                override fun onWatchdogTrigger() { systemMonitor.acquireWakeLock(); systemMonitor.scheduleWatchdogAlarm(force = true) }
                override fun onUiPulse() { lastUiPulseTs = timeProvider.currentTimeMillis(); updateForegroundServiceType() }
                override fun onUiVisibilityChanged(visible: Boolean) { onUiVisibilityChangedInternal(visible) }
                override fun onTransientDrop(drop: Boolean) { transientDropDetected.set(drop) }
                override fun onResetTimers() { resetServiceTimers() }
                override fun onSyncSensors() {
                    lifecycleScope.launch(Dispatchers.Default) {
                        refreshCapabilitiesInternal()
                        appSensorManager.start()
                    }
                }
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
            val recoveredDrift = repository.getLong(MainRepository.CLOCK_DRIFT_REF_KEY, 0L)
            
            lastServiceTickTs = recoveredTs
            lastServiceTickRealtime = if (recoveredDrift != 0L) recoveredTs - recoveredDrift else timeProvider.elapsedRealtime()

            locationProcessor.setLastValidFixRt(timeProvider.elapsedRealtime()) 
            
            serviceStartRealtime = timeProvider.elapsedRealtime()
            serviceStartWall = timeProvider.currentTimeMillis()

            setupPhysicalFastPaths()
            startTickLoop()
            
            withContext(Dispatchers.Main) {
                updateForegroundServiceType()
            }
            
            logManager.logServiceEvent("Tracker Engine Online (Staggered)", important = true)
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

    private fun setupPhysicalFastPaths() {
        appSensorManager.setAcousticFastPath(
            floor = locationProcessor.getAcousticFloorDb(),
            spikeThreshold = 15.0,
            minDb = 40.0,
            onSpike = {
                logManager.logServiceEvent("Acoustic Spike Detected (FastPath)", false)
                lastFastPathAcousticSpikeTs = timeProvider.elapsedRealtime()
            }
        )
    }

    private fun handleViewerPulse(id: String) {
        if (!SignalingConstants.isValidViewerId(id)) return
        repository.updateRemoteActivity(timeProvider.currentTimeMillis())

        if ((configManager.viewerId == MainRepository.DEFAULT_TRACKER_ID || configManager.viewerId.isEmpty()) && id.isNotEmpty() && id != "Active Viewer") {
            configManager.viewerId = id
            connectivitySuite.updateIdentity(configManager.deviceId, id, true)
            lifecycleScope.launch { repository.saveString(MainRepository.VIEWER_ID_KEY, id) } 
        }
        if (sessionManager.onViewerPulse(id, timeProvider.currentTimeMillis(), true)) { 
            logManager.logServiceEvent("Viewer connected: $id")
            startTickLoop() 
        }
    }

    private fun resetServiceTimers() { 
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()
        alarmManager.resetEvaluation()
        locationProcessor.resetStats()
        sessionManager.reset()
        integrityMonitor.resetStats()
        forensicUseCase.resetLatches()
        lastHardwareRecoveryTs = 0L
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        logManager.logServiceEvent("Session Terminated", false)
    }

    private fun onUiVisibilityChangedInternal(visible: Boolean) {
        isUiForeground.set(visible)
        updateForegroundServiceType()
        if (visible) startTickLoop()
    }

    override fun startServiceForeground() {
        val type = getAvailableForegroundServiceType()
        // Issue #113: Force = true for initial startup notification
        safeStartForeground(notificationManager.getNotificationId(), notificationManager.buildForegroundNotification("Tracking system active."), type, force = true)
    }

    override fun updateForegroundServiceType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nowRt = timeProvider.elapsedRealtime()

            // Issue #534/535: Suppress FGS updates during startup and apply strict throttling
            val startupWindowActive = nowRt - serviceStartRealtime < 10_000L
            if (startupWindowActive && lastFgsUpdateRt != 0L) {
                return
            }

            if (nowRt - lastFgsUpdateRt < FGS_TYPE_UPDATE_THROTTLE_MS) {
                return
            }

            lastFgsUpdateRt = nowRt
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
        
        // Issue #113: Promote A15 to Special Use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && capabilities.isA15Device) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isMicEnabled = appSensorManager.isAcousticMonitoringEnabled()
            val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPermission && (isMicEnabled || isRecentUiPulse())) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
            }
        }
        return type
    }

    override fun getRequiredTickInterval(): Long {
        return if (isPowerSaveActive) POWER_SAVE_TICK_INTERVAL_MS else TICK_INTERVAL_MS
    }

    override suspend fun processTick(now: Long, nowRt: Long): Unit = withContext(Dispatchers.Default) {
        integrityMonitor.pollSystemStatus(now, nowRt)
        integrityMonitor.checkInternetIntegrity(nowRt)
        val health = integrityMonitor.currentHealth
        repository.updateHealth(health)

        appSensorManager.setHighLoad(health.isCoolingModeActive)

        if (capabilities.requiresWakeLockRenewal) systemMonitor.renewWakeLock()

        // Issue #113: Hardware Poke for A15 budget stabilization
        if (capabilities.isA15Device && nowRt - lastA15PokeRt > A15_POKE_INTERVAL_MS) {
            lastA15PokeRt = nowRt
            
            // Issue #543: Prefer JNI punch over generic WakeLock if library is available
            if (MbrainHardwareManager.isAvailable()) {
                MbrainHardwareManager.punchHardware(timeProvider)
            } else {
                systemMonitor.acquireWakeLock(force = true)
            }
            
            // Trigger minor hardware event via accelerometer request (handled by renewing registration)
            if (appSensorManager.isStationary()) {
                Timber.d("Issue #113: A15 Hardware Poke - Refreshing Budget")
            }
        }

        if (lastServiceTickRealtime > 0) {
            val tickGap = nowRt - lastServiceTickRealtime
            if (tickGap > HARDWARE_SUPPRESSION_THRESHOLD_MS && nowRt - lastHardwareRecoveryTs > HARDWARE_RECOVERY_COOLDOWN_MS) {
                lastHardwareRecoveryTs = nowRt
                val proc = lastProcessedLocation
                logManager.logServiceEvent("HEURISTIC RECOVERY: Heartbeat gap detected (${tickGap}ms). Reviving connection.", true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                    lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                
                systemMonitor.acquireWakeLock()
                connectivitySuite.connect(configManager.relayUrl)
            }
        }

        if (nowRt - lastStabilityAuditTs > GPS_STABILITY_AUDIT_INTERVAL_MS) {
            if (stabilityAuditFixCount > 0) {
                val reliability = 100.0 * (stabilityAuditFixCount - stabilityAuditViolationCount) / stabilityAuditFixCount
                if (reliability < GPS_STABILITY_RELIABILITY_THRESHOLD) {
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY AUDIT (T): Reliability ${String.format(Locale.getDefault(), "%.1f", reliability)}% ($stabilityAuditViolationCount gaps in $stabilityAuditFixCount fixes)", important = true,
                        lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = proc?.maxAccuracy ?: 0.0)
                }
                stabilityAuditFixCount = 0
                stabilityAuditViolationCount = 0
            }
            lastStabilityAuditTs = nowRt
        }
        
        val isSocketConnected = connectivitySuite.isConnected() && !transientDropDetected.getAndSet(false)
        connectivitySuite.updateRelayStatus(isSocketConnected)
        
        val isViewerActive = sessionManager.getViewerCount() > 0 || isRecentUiPulse()
        sessionManager.updateTick(nowRt, lastServiceTickRealtime, isPeerAvailable = isSocketConnected && isViewerActive, isInViolation = alarmManager.hasUnresolvedAlarms())

        val snapshot = appSensorManager.consumeForensicSnapshot()

        locationProcessor.updateSensorData(
            vibration = snapshot.vibration,
            heading = snapshot.heading,
            baroAlt = snapshot.baroAlt,
            lux = snapshot.lux,
            isNear = snapshot.isNear,
            powerTamper = health.isPowerTamper,
            tiltDegrees = snapshot.tiltDegrees,
            acousticDb = snapshot.acousticDb,
            peakShock = snapshot.peakShock,
            peakVerticalVelocity = snapshot.peakVerticalVelocity,
            peakVerticalVelocityTs = snapshot.peakVerticalVelocityTs,
            peakVerticalVelocityRt = snapshot.peakVerticalVelocityRt,
            plungeMatched = snapshot.plungeMatched,
            peakVerticalDisplacement = snapshot.peakVerticalDisplacement,
            nowRt = nowRt,
            nowWall = now
        )

        // Unified Forensic Index Computation (Issue #525)
        val noiseIdx = (snapshot.acousticDb - locationProcessor.getAcousticFloorDb()).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB
        val luxIdx = log10(snapshot.lux + 1.0) / RIBBON_LUX_LOG_SCALE
        val vibeIdx = snapshot.vibration / RIBBON_VIBRATION_SCALE_G
        val liftIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_LIFT_SCALE_METERS) / RIBBON_LIFT_SCALE_METERS
        val avgCn0 = latestGnssDetail?.satellites?.map { it.cn0 }?.average() ?: 0.0
        val snrIdx = avgCn0 / RIBBON_SNR_SCALE_DB
        val tiltIdx = abs(snapshot.tiltDegrees - locationProcessor.getChairBaselineTilt()).coerceIn(0.0, RIBBON_SIT_TILT_SCALE_DEG) / RIBBON_SIT_TILT_SCALE_DEG
        val baroIdx = (snapshot.baroAlt - locationProcessor.getBaroBaseline()).coerceIn(0.0, RIBBON_SIT_BARO_SCALE_METERS) / RIBBON_SIT_BARO_SCALE_METERS
        val sitDetected = locationProcessor.consumeSitDetected()

        // Issue #526: Power Save State Evaluation
        if (nowRt - lastPowerSaveCheckRt > 5000L) {
            val shouldBePowerSave = serviceBehaviorUseCase.evaluatePowerSaveMode(
                isStationary = appSensorManager.isStationary(),
                isGpsStalled = lastProcessedLocation?.isStalled ?: true,
                hasUnresolvedAlarms = alarmManager.hasUnresolvedAlarms(),
                isUiVisible = isUiVisible()
            )
            
            if (shouldBePowerSave != isPowerSaveActive) {
                isPowerSaveActive = shouldBePowerSave
                appSensorManager.setPowerSaveMode(shouldBePowerSave)
                logManager.logServiceEvent("POWER SAVER: ${if (shouldBePowerSave) "ENGAGED" else "DISENGAGED"}", false)
                
                // Issue #531: Trigger FGS type update when power-save state changes
                withContext(Dispatchers.Main) {
                    updateForegroundServiceType()
                }
            }
            lastPowerSaveCheckRt = nowRt
        }

        val hasLocation = lastKnownLocation != null
        if (isViewerActive && hasLocation) {
            val location = lastKnownLocation!!
            val processed = locationProcessor.processGpsPoint(
                lat = location.latitude, lng = location.longitude, alt = location.altitude, androidSpeedMps = lastGpsSpeed,
                gpsTs = location.time, accuracy = lastGpsAccuracy, bearing = lastGpsBearing,
                snr = avgCn0,
                satsUsed = latestGnssDetail?.satellites?.count { it.usedInFix } ?: 0,
                isViewerTrail = false, lastGpsTs = lastGpsFixRealtime, isLocal = true,
                providedAcousticLockoutRt = lastFastPathAcousticSpikeTs,
                nowWall = now, nowRt = nowRt
            )
            lastProcessedLocation = processed

            evaluateAlarmsInternal(now, nowRt, isSocketConnected, isViewerActive, processed, snapshot)
            
            connectivitySuite.pushCurrentStatus(
                deviceId = configManager.deviceId, viewerId = configManager.viewerId, isTrackerMode = true,
                loc = location, filtered = processed.optimizedPoint, distToTracker = null, distToHome = processed.distToHome,
                maxAccuracy = processed.maxAccuracy, filteredSpeed = processed.filteredSpeed, vibration = snapshot.vibration,
                heading = snapshot.heading, baroAlt = snapshot.baroAlt, lux = snapshot.lux,
                isNear = snapshot.isNear,
                tiltDegrees = snapshot.tiltDegrees, acousticDb = snapshot.acousticDb,
                jumpTier = processed.jumpTier, isJammer = processed.jammerDetected,
                isStalled = processed.isStalled, peakShock = snapshot.peakShock,
                peakShockTs = now, luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), proxIdx = snapshot.proximityIdx,
                proximityCm = snapshot.proximityCm, proximityDebounceMs = snapshot.proximityDebounceMs,
                vibrationRollingSum = snapshot.vibrationRollingSum, micPending = false,
                isTamperDetected = processed.tamperDetected, isPowerTamper = health.isPowerTamper,
                isSitDetected = sitDetected, isSitActive = false,
                lastSitTs = locationProcessor.getLastSitTs(), receiptRt = nowRt, violationUptimeMs = sessionManager.violationUptimeMs,
                violationPercentage = sessionManager.getViolationPercentage(), verticalVelocity = snapshot.peakVerticalVelocity,
                sitVz = snapshot.peakVerticalVelocity, sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.baroAlt, sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock,
                isClockRegression = processed.isClockRegression, isLocationPending = false, locationPendingReason = LocationPendingReason.NONE,
                lastValidFixRt = locationProcessor.getLastValidFixRt(), gnssDetail = latestGnssDetail, 
                snrIdx = snrIdx, noiseIdx = noiseIdx, luxIdx = luxIdx, vibeIdx = vibeIdx, liftIdx = liftIdx, tiltIdx = tiltIdx, baroIdx = baroIdx,
                isBatterySteepDischarge = health.isBatterySteepDischarge, isCoolingModeActive = health.isCoolingModeActive,
                batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp, isCharging = health.isCharging,
                trackerState = if (processed.filteredSpeed > 0.5) TrackerState.MOVING else TrackerState.PARKING,
                status = processed.status,
                isStorageLow = health.isStorageLow, isStorageCritical = health.isStorageCritical,
                isPowerSaveMode = isPowerSaveActive || health.isPowerSaveMode, standbyBucket = health.standbyBucket, netInterface = health.netInterface
            )
            
            historyManager.updateRibbons(
                now = now, nowRt = nowRt,
                lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime,
                serviceTickCounter = serviceTickCounter,
                rtt = connectivitySuite.getRtt(),
                peerSignal = 10,
                peerAvail = isSocketConnected && isViewerActive,
                hasGps = true,
                isTrackerMode = true,
                accuracy = processed.currentAccuracy,
                maxAccuracy = processed.maxAccuracy,
                noiseIdx = noiseIdx,
                luxIdx = luxIdx,
                vibeIdx = vibeIdx,
                proxIdx = snapshot.proximityIdx,
                liftIdx = liftIdx,
                snrIdx = snrIdx,
                tiltIdx = tiltIdx,
                baroIdx = baroIdx,
                verticalVelocity = snapshot.peakVerticalVelocity,
                sitVz = snapshot.peakVerticalVelocity,
                sitDz = snapshot.peakVerticalDisplacement,
                sitBaro = snapshot.baroAlt,
                sitTilt = snapshot.tiltDegrees,
                sitShock = snapshot.peakShock,
                isBatterySteepDischarge = health.isBatterySteepDischarge,
                isCoolingModeActive = health.isCoolingModeActive,
                speed = processed.filteredSpeed,
                bearing = location.bearing.toDouble(),
                isSitDetected = sitDetected,
                isSitActive = false,
                currentMa = health.currentMa,
                locationPendingReason = LocationPendingReason.NONE
            )
        } else {
            historyManager.updateRibbons(
                now = now, nowRt = nowRt,
                lastTickTs = lastServiceTickTs, lastTickRt = lastServiceTickRealtime,
                serviceTickCounter = serviceTickCounter,
                rtt = connectivitySuite.getRtt(),
                peerSignal = 0,
                peerAvail = isSocketConnected && isViewerActive,
                hasGps = false,
                isTrackerMode = true,
                currentMa = health.currentMa
            )
        }

        // Issue #113: Throttled Pulse - Suppress noise if system is not Active
        if (isSystemActive && (now - lastNotificationUpdateTs >= NOTIFICATION_THROTTLE_MS)) {
            lastNotificationUpdateTs = now
            notificationManager.updatePulse(
                sats = gpsManager.satellitesUsed,
                battery = health.batteryLevel,
                isSecure = !alarmManager.hasUnresolvedAlarms(),
                isPowerSave = isPowerSaveActive || health.isPowerSaveMode
            )
        }

        lastServiceTickTs = now
        lastServiceTickRealtime = nowRt
        repository.saveLongSync(MainRepository.LAST_SERVICE_TICK_TS_KEY, now)
        serviceTickCounter++
    }

    private fun onLocationChanged(location: Location) {
        val nowRt = timeProvider.elapsedRealtime()
        lastKnownLocation = location
        lastGpsSpeed = location.speed.toDouble()
        lastGpsAccuracy = location.accuracy.toDouble()
        lastGpsBearing = location.bearing.toDouble()
        
        if (lastGpsFixRealtime > 0) {
            val gap = nowRt - lastGpsFixRealtime
            if (HIGH_FREQUENCY_GPS_POLLING_MS == TICK_INTERVAL_MS) {
                stabilityAuditFixCount++
                if (gap > TICK_INTERVAL_MS + GPS_STABILITY_GAP_THRESHOLD_MS) {
                    stabilityAuditViolationCount++
                    val proc = lastProcessedLocation
                    logManager.logServiceEvent("STABILITY GAP (T): ${gap}ms detected during logic pulse.", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR,
                        lat = proc?.optimizedPoint?.lat ?: 0.0, lng = proc?.optimizedPoint?.lng ?: 0.0, accuracy = lastGpsAccuracy)
                }
            }
        }

        lastGpsFixRealtime = nowRt
        systemMonitor.gpsStallStartTs = 0L
        if (lastStabilityAuditTs == 0L) lastStabilityAuditTs = nowRt
    }

    private fun evaluateAlarmsInternal(
        now: Long,
        nowRt: Long,
        isSocketConnected: Boolean,
        isViewerConnected: Boolean,
        processed: LocationProcessor.ProcessedLocation,
        snapshot: AppSensorManager.ForensicSnapshot
    ) {
        val health = integrityMonitor.currentHealth
        alarmEvalJob?.cancel()
        alarmEvalJob = lifecycleScope.launch(Dispatchers.Default) {
            alarmManager.evaluateAlarms(
                now = now, nowRt = nowRt, 
                serviceStartTs = serviceStartWall, serviceStartRt = serviceStartRealtime,
                appStartTime = sessionManager.appStartTime,
                isTrackerMode = true, isRelayConnected = isSocketConnected, isTrackerConnected = true,
                status = processed.status,
                isJammer = processed.jammerDetected,
                jumpTier = processed.jumpTier,
                trackerLat = processed.optimizedPoint.lat,
                trackerLng = processed.optimizedPoint.lng, trackerAccuracy = processed.currentAccuracy,
                maxTrackerAccuracy = processed.maxAccuracy, trackerLastGpsTs = lastKnownLocation?.time ?: 0L,
                trackerLastGpsRt = lastGpsFixRealtime,
                trackerLastValidFixTs = 0L,
                trackerLastValidFixRt = locationProcessor.getLastValidFixRt(), trackerSpeed = processed.filteredSpeed,
                trackerBattery = health.batteryLevel, trackerTemp = health.batteryTemp,
                isHardwareOnline = health.isHardwareOnline, isLocalInternetLoss = health.localInternetLoss,
                isSignalLoss = health.signalLoss, isGpsStalling = processed.isStalled,
                isUiVisible = isUiVisible(), distToHomeAuthority = processed.distToHome,
                maxDistanceAuthority = locationProcessor.getMaxDistanceAuthority(), 
                isGpsGap = false, // Authority logic handles gaps via trackerLastValidFixRt
                isTamperDetected = processed.tamperDetected,
                isPowerTamper = health.isPowerTamper, trackerTiltDegrees = snapshot.tiltDegrees,
                trackerAcousticDb = snapshot.acousticDb, trackerBaroAlt = snapshot.baroAlt,
                trackerBaroAltEma = locationProcessor.getBaroBaseline(),
                trackerLux = snapshot.lux, isNear = snapshot.isNear,
                luxBaseline = locationProcessor.getLuxBaseline(), acousticFloorDb = locationProcessor.getAcousticFloorDb(),
                adaptiveVibrationFloor = locationProcessor.getAdaptiveVibrationFloor(), peakVibrationShock = snapshot.peakShock,
                trackerCurrentMa = health.currentMa,
                capabilities = capabilities
            )
        }
    }

    override fun onDestroy() {
        gpsCollectionJob?.cancel()
        gnssDetailJob?.cancel()
        settingsJob?.cancel()
        alarmEvalJob?.cancel()
        commandRouter.unregister()
        appSensorManager.stop()
        connectivitySuite.stop()
        super.onDestroy()
    }
}
