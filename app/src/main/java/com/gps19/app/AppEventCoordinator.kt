package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * AppEventCoordinator: Unified domain event orchestrator.
 * Sep.25.05:
 * - Issue #1330: Snap-to-Update Monolith. Eliminated ~100 lines of manual 
 *   bridge mapping by leveraging unified partitioned states in SystemEvaluationSnapshot.
 * - Performance: Transitioned repository updates to use snapshot partitions directly.
 */
@Singleton
class AppEventCoordinator @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val logManager: LogManager,
    private val audioSynthesizer: AudioSynthesizer,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider,
    private val configManager: ConfigManager,
    private val repository: MainRepository,
    private val alarmManager: AppAlarmManager,
    private val historyManager: HistoryManager,
    private val domainEventBus: com.gps19.core.engine.DomainEventBus
) {
    private var isStarted = false

    fun start(connectivitySuite: ConnectivitySuite) {
        if (isStarted) return
        isStarted = true

        scope.launch(Dispatchers.Default) {
            launch { observeDomainEvents(connectivitySuite) }
            launch { observeSirenRequirement() }
            launch { observePhysicalSirenState() }
        }
    }

    private suspend fun observeDomainEvents(connectivitySuite: ConnectivitySuite) {
        domainEventBus.events.collect { event ->
            when (event) {
                is DomainEvent.TickEvaluated -> handleTickEvaluated(event, connectivitySuite)
                is DomainEvent.ViewerLocationUpdated -> handleViewerLocationUpdated(event)
                is DomainEvent.PeerStatusReceived -> repository.updateLocation(event.status)
                is DomainEvent.PeerConnectionChanged -> handlePeerConnectionChanged(event)
                is DomainEvent.HeuristicRecovery -> handleHeuristicRecovery(event)
                is DomainEvent.StabilityViolation -> handleStabilityViolation(event)
                is DomainEvent.PowerSaveTransition -> handlePowerSaveTransition(event)
                is DomainEvent.ServiceStatus -> logManager.logServiceEvent(event.message, isImportant = event.isImportant)
                is DomainEvent.Alarm -> handleAlarmEvent(event.event)
                is DomainEvent.Integrity -> handleIntegrityEvent(event.event)
                is DomainEvent.Processor -> handleProcessorEvent(event.event, event.isPrimary)
                is DomainEvent.Connectivity -> handleConnectivityEvent(event.event)
                is DomainEvent.History -> handleHistoryEvent(event.event)
                is DomainEvent.Sensor -> handleSensorEvent(event.event)
                is DomainEvent.Command -> handleCommandEvent(event.event)
                is DomainEvent.Revival -> handleRevivalEvent(event.event)
            }
        }
    }

    private suspend fun handleTickEvaluated(event: DomainEvent.TickEvaluated, connectivitySuite: ConnectivitySuite) {
        val proc = event.processed
        val snapshot = event.snapshot
        val now = event.now
        val nowRt = event.nowRt
        val isTrackerMode = event.isTrackerMode

        // 1. Repository Persistence (Snap-to-Update Monolith)
        if (isTrackerMode) {
            repository.updateLocation(LocationUpdate(
                kinetic = snapshot.kinetic.copy(),
                atmospheric = snapshot.atmospheric.copy(),
                integrity = snapshot.integrity.copy(),
                status = snapshot.status,
                ts = now,
                isMe = true,
                trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING,
                isClockRegression = snapshot.isClockRegression,
                lastValidFixRt = snapshot.lastValidFixRt
            ))
            
            // 2. Peer Signaling (Bridge removal in progress)
            if (event.isPeerActive) {
                connectivitySuite.pushCurrentStatus(
                    deviceId = configManager.deviceId, 
                    viewerId = configManager.viewerId, 
                    isTrackerMode = true, 
                    loc = null, 
                    filtered = proc?.optimizedPoint, 
                    distToTracker = null, 
                    distToHome = snapshot.kinetic.distToHome, 
                    maxAccuracy = snapshot.kinetic.maxAccuracy, 
                    filteredSpeed = snapshot.kinetic.speed, 
                    vibration = snapshot.atmospheric.vibration, 
                    heading = snapshot.atmospheric.heading, 
                    baroAlt = snapshot.atmospheric.baroAlt, 
                    lux = snapshot.atmospheric.lux, 
                    isNear = snapshot.atmospheric.isNear, 
                    tiltDegrees = snapshot.atmospheric.tiltDegrees, 
                    acousticDb = snapshot.atmospheric.acousticDb, 
                    jumpTier = snapshot.jumpTier, 
                    isJammer = snapshot.jammerDetected, 
                    isStalled = snapshot.isStalled, 
                    peakShock = snapshot.atmospheric.peakVibrationShock, 
                    peakShockTs = now, 
                    luxBaseline = snapshot.atmospheric.luxBaseline, 
                    acousticFloorDb = snapshot.atmospheric.acousticFloorDb, 
                    adaptiveVibrationFloor = snapshot.atmospheric.adaptiveVibrationFloor, 
                    proxIdx = snapshot.atmospheric.proxIdx,
                    proximityCm = snapshot.atmospheric.proximityCm,
                    proximityDebounceMs = snapshot.atmospheric.proximityDebounceMs,
                    vibrationRollingSum = snapshot.atmospheric.vibrationRollingSum,
                    micPending = false, 
                    isTamperDetected = snapshot.tamperDetected, 
                    isPowerTamper = snapshot.integrity.isPowerTamper, 
                    isSitDetected = event.isSuspiciousMode, 
                    isSitActive = false, 
                    lastSitTs = event.lastSitTs, 
                    receiptRt = nowRt, 
                    violationUptimeMs = snapshot.integrity.violationUptimeMs, 
                    violationPercentage = snapshot.integrity.violationPercentage, 
                    verticalVelocity = snapshot.kinetic.verticalVelocity, 
                    sitVz = snapshot.integrity.sitVz, 
                    sitVzTs = snapshot.integrity.sitVzTs, 
                    sitVzRt = snapshot.integrity.sitVzRt, 
                    sitDz = snapshot.integrity.sitDz, 
                    sitBaro = snapshot.integrity.sitBaro, 
                    sitTilt = snapshot.integrity.sitTilt, 
                    sitShock = snapshot.integrity.sitShock, 
                    isClockRegression = snapshot.isClockRegression, 
                    isLocationPending = snapshot.integrity.isLocationPending, 
                    locationPendingReason = snapshot.integrity.locationPendingReason, 
                    lastValidFixRt = snapshot.lastValidFixRt, 
                    gnssDetail = event.gnssDetail,
                    isBatterySteepDischarge = snapshot.integrity.isBatterySteepDischarge,
                    isCoolingModeActive = snapshot.integrity.isCoolingModeActive, 
                    batteryLevel = snapshot.integrity.battery, 
                    temp = snapshot.atmospheric.temp, 
                    isCharging = snapshot.integrity.isCharging, 
                    trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, 
                    status = snapshot.status, 
                    isStorageLow = snapshot.integrity.isStorageLow, 
                    isStorageCritical = snapshot.integrity.isStorageCritical, 
                    isPowerSaveMode = snapshot.integrity.isPowerSaveMode, 
                    standbyBucket = snapshot.integrity.standbyBucket, 
                    netInterface = snapshot.integrity.netInterface, 
                    snrIdx = event.snrIdx,
                    noiseIdx = event.noiseIdx,
                    luxIdx = event.luxIdx,
                    vibeIdx = event.vibeIdx,
                    liftIdx = event.liftIdx,
                    tiltIdx = event.tiltIdx,
                    baroIdx = event.baroIdx,
                    kineticEnergy = snapshot.kinetic.kineticEnergy,
                    isAdaptiveJump = snapshot.isAdaptiveJump, 
                    isBatteryLow = snapshot.integrity.isBatteryLow, 
                    isBatteryCritical = snapshot.integrity.isBatteryCritical, 
                    isUltraLongStationary = snapshot.integrity.isUltraLongStationary, 
                    gpsHardwareLock = snapshot.integrity.gpsHardwareLock, 
                    tamperNote = snapshot.suppressionNote,
                    satsUsed = snapshot.integrity.satsUsed,
                    satsView = snapshot.integrity.satsView
                )
            }
        }

        // 3. Ribbon Updates
        historyManager.updateRibbons(
            now = now, nowRt = nowRt, lastTickTs = event.lastTickTs, lastTickRt = event.lastTickRt, 
            serviceTickCounter = event.serviceTickCounter, rtt = event.rtt, 
            peerSignal = if (event.isPeerActive) 10 else 0, peerAvail = event.isSocketConnected && event.isPeerActive, 
            hasGps = (proc?.timestamp ?: 0L) > 0, isTrackerMode = isTrackerMode, 
            accuracy = proc?.currentAccuracy ?: 0.0, maxAccuracy = proc?.maxAccuracy ?: 0.0, 
            noiseIdx = event.noiseIdx, luxIdx = event.luxIdx, vibeIdx = event.vibeIdx, proxIdx = snapshot.atmospheric.proxIdx,
            liftIdx = event.liftIdx, snrIdx = event.snrIdx, tiltIdx = event.tiltIdx, baroIdx = event.baroIdx, 
            verticalVelocity = snapshot.kinetic.verticalVelocity, sitVz = snapshot.integrity.sitVz, 
            sitVzTs = snapshot.integrity.sitVzTs, sitVzRt = snapshot.integrity.sitVzRt, 
            sitDz = snapshot.integrity.sitDz, sitBaro = snapshot.integrity.sitBaro, 
            sitTilt = snapshot.integrity.sitTilt, sitShock = snapshot.integrity.sitShock, 
            isBatterySteepDischarge = snapshot.integrity.isBatterySteepDischarge, isCoolingModeActive = snapshot.integrity.isCoolingModeActive, 
            speed = snapshot.kinetic.speed, bearing = snapshot.kinetic.bearing, 
            isSitDetected = if (isTrackerMode) event.isSuspiciousMode else false, isSitActive = false,
            currentMa = snapshot.integrity.currentMa, locationPendingReason = snapshot.integrity.locationPendingReason, 
            kineticEnergy = snapshot.kinetic.kineticEnergy, isRecoveryEvent = event.recoveryFlagged, 
            cpuLoad = snapshot.cpuLoad, ioWait = snapshot.ioWait, maxIoLatency = snapshot.maxIoLatency, 
            isSilentFailure = snapshot.isSilentFailure, isBatteryLow = snapshot.integrity.isBatteryLow, 
            isBatteryCritical = snapshot.integrity.isBatteryCritical, isUltraLongStationary = snapshot.integrity.isUltraLongStationary
        )
    }

    private suspend fun handleViewerLocationUpdated(event: DomainEvent.ViewerLocationUpdated) {
        val snapshot = event.snapshot
        repository.updateLocation(snapshot.toLocationUpdate(isMe = true))
    }

    private fun handlePeerConnectionChanged(event: DomainEvent.PeerConnectionChanged) {
        logManager.logServiceEvent(
            m = "PEER LIFECYCLE: Peer ${event.peerId} ${if (event.isConnected) "Connected" else "Disconnected"}",
            isImportant = true
        )
    }

    private fun handleHeuristicRecovery(event: DomainEvent.HeuristicRecovery) {
        logManager.logServiceEvent(
            m = "HEURISTIC RECOVERY: ${event.message} - Heartbeat gap detected (${event.gapMs}ms). Reviving connection.", 
            isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
            lat = event.lat, lng = event.lng, accuracy = event.accuracy
        )
    }

    private fun handleStabilityViolation(event: DomainEvent.StabilityViolation) {
        logManager.logServiceEvent(
            m = event.message, isImportant = true, isSpecial = event.isJitter, 
            specialColor = if (event.isJitter) FORENSIC_PINK_COLOR else null, 
            lat = event.lat, lng = event.lng, accuracy = event.accuracy
        )
    }

    private fun handlePowerSaveTransition(event: DomainEvent.PowerSaveTransition) {
        logManager.logServiceEvent(m = "POWER SAVER: ${if (event.isEngaged) "ENGAGED" else "DISABLED"}", isImportant = false)
    }

    private fun handleAlarmEvent(event: AlarmEvent) {
        if (event is AlarmEvent.LogEvent) {
            logManager.submitToLogSink(
                message = event.message, type = event.type, isImportant = event.isImportant,
                extremeValue = event.extremeValue, localId = event.logId, durationMs = event.durationMs,
                isSpecial = event.isSpecial, specialColor = event.specialColor,
                lat = event.lat, lng = event.lng, accuracy = event.accuracy,
                maxAccuracy = event.maxAccuracy, snr = event.snr, vibe = event.vibe
            )
        }
    }

    private fun handleIntegrityEvent(event: IntegrityEvent) {
        when (event) {
            is IntegrityEvent.ViolationSustained -> {
                if (configManager.isTrackerMode && event.type == ALERT_ID_TRACKER_POWER) {
                    alarmManager.setPowerAlarmPending(true, "T_")
                }
            }
            is IntegrityEvent.ViolationResolved -> {
                if (configManager.isTrackerMode && event.type == ALERT_ID_TRACKER_POWER) {
                    alarmManager.setPowerAlarmPending(false, "T_")
                }
            }
            is IntegrityEvent.LogEvent -> {
                val isSpecial = event.message.contains("tamper", ignoreCase = true) || 
                               event.message.contains("confirmed", ignoreCase = true) || 
                               event.message.contains("EMERGENCY", ignoreCase = true) || 
                               event.message.contains("ENERGY AUDIT", ignoreCase = true)
                logManager.logServiceEvent(
                    m = event.message, 
                    isImportant = event.isImportant, 
                    isSpecial = isSpecial, 
                    specialColor = if (isSpecial) FORENSIC_PINK_COLOR else null
                )
            }
            is IntegrityEvent.LocationStatusChanged -> {}
            is IntegrityEvent.GnssThrottledChanged -> {}
        }
    }

    private fun handleProcessorEvent(event: ProcessorEvent, isPrimary: Boolean) {
        val isTrackerMode = configManager.isTrackerMode
        val prefix = if (isTrackerMode) "T_" else (if (isPrimary) "V_" else "VR_")
        val logPrefix = if (!isTrackerMode && isPrimary) "[Self] " else ""
        
        when (event) {
            is ProcessorEvent.TrailPointSaved -> {
                repository.saveTrailPoint(event.lat, event.lng, event.isViewerTrail, event.status, event.timestamp, accuracy = event.accuracy, maxAccuracy = event.maxAccuracy)
            }
            is ProcessorEvent.LogAdded -> {
                val specialColor = if (event.isSpecial || event.message.contains("Merge-on-Stale")) FORENSIC_PINK_COLOR else null
                logManager.submitToLogSink(
                    message = logPrefix + event.message, type = event.type, isImportant = event.isImportant,
                    isSpecial = event.isSpecial || event.message.contains("Merge-on-Stale"),
                    specialColor = specialColor, lat = event.lat, lng = event.lng,
                    accuracy = event.accuracy, snr = event.snr, vibe = event.vibe
                )
            }
            is ProcessorEvent.MaxAccuracyChanged -> {
                if (isTrackerMode || !isPrimary) repository.saveDoubleSync(prefix + MAX_ACCURACY_KEY, event.accuracy)
            }
            is ProcessorEvent.ChairBaselineChanged -> {
                val telem = if (isTrackerMode || isPrimary) repository.getLocalLocationSync() else repository.getTrackerLocationSync()
                logManager.logServiceEvent(
                    m = "Passive Zeroing: Chair baseline calibrated to ${event.baseline.roundToOneDecimal()}°", 
                    lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                )
                if (isTrackerMode || !isPrimary) repository.saveDoubleSync(prefix + CHAIR_BASELINE_TILT_KEY, event.baseline)
            }
            is ProcessorEvent.VibrationFloorChanged -> {
                if (isTrackerMode || !isPrimary) {
                    repository.saveDoubleDebounced(prefix + ADAPTIVE_VIBRATION_FLOOR_KEY, event.floor)
                }
            }
            is ProcessorEvent.LuxBaselineChanged -> {
                if (isTrackerMode || !isPrimary) {
                    repository.saveDoubleDebounced(prefix + TRACKER_LUX_BASELINE_KEY, event.baseline)
                }
            }
            is ProcessorEvent.AcousticFloorChanged -> {
                if (isTrackerMode || !isPrimary) {
                    repository.saveDoubleDebounced(prefix + TRACKER_ACOUSTIC_FLOOR_KEY, event.floor)
                }
            }
            is ProcessorEvent.GpsStallDetected -> {
                if (!isTrackerMode && isPrimary) logManager.logServiceEvent(m = "GPS STALL: Fix unchanged for >1s", isImportant = false)
            }
        }
    }

    private fun handleConnectivityEvent(event: ConnectivityEvent) {
        if (event is ConnectivityEvent.PeerPulse) {
            // Orchestration for peer activity if needed
        }
    }

    private fun handleHistoryEvent(event: HistoryEvent) {
        if (event is HistoryEvent.LogEvent) logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
    }

    private fun handleSensorEvent(event: AppSensorEvent) {
        when (event) {
            is AppSensorEvent.HardwareFailure -> {
                val telem = repository.getLocalLocationSync()
                logManager.logServiceEvent(
                    m = "CRITICAL: SENSOR_HARDWARE_FAILURE - ${event.reason}", 
                    isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
                    lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                )
            }
            is AppSensorEvent.LogEvent -> logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
        }
    }

    private fun handleCommandEvent(event: CommandEvent) {
        // Handled in MonitorService
    }

    private fun handleRevivalEvent(event: RevivalEvent) {
        val isTrackerMode = configManager.isTrackerMode
        val roleTag = if (isTrackerMode) "" else "(V) "
        val telem = repository.getLocalLocationSync()
        
        when (event) {
            is RevivalEvent.Footprint -> {
                val msg = "ENERGY AUDIT ${roleTag}: Revival Footprint - Delta: ${event.deltaMa}mA, Temp Rise: ${event.deltaTemp}°C, Duration: ${event.durationMs}ms"
                logManager.submitToLogSink(
                    msg, "system", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
                    lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                )
            }
            is RevivalEvent.HardwareLock -> {
                logManager.logServiceEvent(
                    m = "CRITICAL ${roleTag}: GPS_HARDWARE_LOCK - All revival attempts failed. Hardware stall confirmed.", 
                    isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
                    lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                )
            }
            is RevivalEvent.Attempt -> logManager.logServiceEvent(m = "GPS REVIVAL ${roleTag}: Hardware restart attempt ${event.count} triggered.", isImportant = false)
            is RevivalEvent.Success -> logManager.logServiceEvent(m = "GPS REVIVAL ${roleTag}: Hardware fix restored successfully.", isImportant = true)
            is RevivalEvent.RawBurstStarted -> {}
            is RevivalEvent.RawBurstEnded -> {}
        }
    }

    private suspend fun observeSirenRequirement() {
        alarmManager.isSirenRequired.collectLatest { required ->
            val isCurrentlyPlaying = audioSynthesizer.isPlaying()
            if (required && !isCurrentlyPlaying) {
                audioSynthesizer.playSiren(
                    timeProvider = timeProvider,
                    isTrackerMode = configManager.isTrackerMode,
                    vibrate = true,
                    force = true
                )
            } else if (!required && isCurrentlyPlaying) {
                if (!audioSynthesizer.isForced()) {
                    audioSynthesizer.stopSiren(timeProvider = timeProvider)
                }
            }
        }
    }

    private suspend fun observePhysicalSirenState() {
        audioSynthesizer.isSirenPlaying.collectLatest { playing ->
            if (playing) {
                logManager.logServiceEvent("AUDIO: Siren output engaged", isImportant = false)
            } else {
                logManager.logServiceEvent("AUDIO: Siren output disengaged", isImportant = false)
            }
        }
    }

    private fun Double.roundToOneDecimal(): String = (Math.round(this * 10) / 10.0).toString()
}
