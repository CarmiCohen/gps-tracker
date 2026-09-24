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
 * Sep.24.97:
 * - Issue #1291: Integrated DomainEventBus to centralize side-effect reactions 
 *   (forensics, ribbon updates, repository persistence) and decouple them from 
 *   the background evaluation loop. Fixed mapping errors and suspend contexts.
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
    private val integrityMonitor: IntegrityMonitor,
    private val sessionManager: SessionManager,
    private val historyManager: HistoryManager,
    private val domainEventBus: DomainEventBus
) {
    private var isStarted = false

    fun start(
        alarmManager: AppAlarmManager,
        hardwareSuite: HardwareSuite,
        connectivitySuite: ConnectivitySuite,
        commandRouter: CommandRouter,
        primaryProcessor: LocationProcessor,
        remoteProcessor: LocationProcessor?
    ) {
        if (isStarted) return
        isStarted = true

        scope.launch(Dispatchers.Default) {
            launch { observeDomainEvents(connectivitySuite) }
            launch { observeAlarmEvents(alarmManager) }
            launch { observeIntegrityEvents(alarmManager) }
            launch { observeProcessorEvents(primaryProcessor, remoteProcessor) }
            launch { observeConnectivityEvents(connectivitySuite) }
            launch { observeHistoryEvents() }
            launch { observeSensorEvents(hardwareSuite) }
            launch { observeCommandEvents(commandRouter) }
            launch { observeRevivalEvents(hardwareSuite) }
            launch { observeSirenRequirement(alarmManager) }
            launch { observePhysicalSirenState() }
        }
    }

    private suspend fun observeDomainEvents(connectivitySuite: ConnectivitySuite) {
        domainEventBus.events.collect { event ->
            when (event) {
                is DomainEvent.TickEvaluated -> handleTickEvaluated(event, connectivitySuite)
                is DomainEvent.HeuristicRecovery -> handleHeuristicRecovery(event)
                is DomainEvent.StabilityViolation -> handleStabilityViolation(event)
                is DomainEvent.PowerSaveTransition -> handlePowerSaveTransition(event)
                is DomainEvent.ServiceStatus -> logManager.logServiceEvent(event.message, isImportant = event.isImportant)
            }
        }
    }

    private suspend fun handleTickEvaluated(event: DomainEvent.TickEvaluated, connectivitySuite: ConnectivitySuite) {
        val proc = event.processed
        val snapshot = event.snapshot
        val health = event.health
        val now = event.now
        val nowRt = event.nowRt
        val isTrackerMode = event.isTrackerMode

        // 1. Repository Persistence
        if (isTrackerMode) {
            repository.updateLocation(LocationUpdate().apply {
                this.kinetic.lat = proc?.optimizedPoint?.lat ?: 0.0; this.kinetic.lng = proc?.optimizedPoint?.lng ?: 0.0; this.kinetic.alt = proc?.optimizedPoint?.alt ?: 0.0; this.kinetic.speed = proc?.filteredSpeed ?: 0.0; this.kinetic.accuracy = proc?.currentAccuracy ?: 0.0; this.kinetic.bearing = snapshot.bearing; this.kinetic.gpsTs = proc?.timestamp ?: 0L; this.kinetic.rt = nowRt; this.kinetic.maxAccuracy = proc?.maxAccuracy ?: 0.0; this.kinetic.kineticEnergy = snapshot.kineticEnergy; this.kinetic.verticalVelocity = snapshot.peakVerticalVelocity
                this.atmospheric.temp = snapshot.batteryTemp; this.atmospheric.maxTemp = health.maxTemp; this.atmospheric.vibration = snapshot.vibration; this.atmospheric.heading = snapshot.heading; this.atmospheric.baroAlt = snapshot.baroAlt; this.atmospheric.lux = snapshot.lux; this.atmospheric.isNear = snapshot.isNear; this.atmospheric.tiltDegrees = snapshot.tiltDegrees; this.atmospheric.acousticDb = snapshot.acousticDb; this.atmospheric.peakVibrationShock = snapshot.peakShock; this.atmospheric.peakVibrationShockTs = now; this.atmospheric.noiseIdx = event.noiseIdx; this.atmospheric.luxIdx = event.luxIdx; this.atmospheric.vibeIdx = event.vibeIdx; this.atmospheric.liftIdx = event.liftIdx; this.atmospheric.tiltIdx = event.tiltIdx; this.atmospheric.baroIdx = event.baroIdx; this.atmospheric.luxBaseline = snapshot.luxBaseline; this.atmospheric.acousticFloorDb = snapshot.acousticFloorDb; this.atmospheric.adaptiveVibrationFloor = snapshot.adaptiveVibrationFloor; this.atmospheric.proxIdx = event.proxIdx; this.atmospheric.proximityCm = event.proximityCm; this.atmospheric.proximityDebounceMs = event.proximityDebounceMs; this.atmospheric.vibrationRollingSum = event.vibrationRollingSum
                this.integrity.battery = snapshot.batteryLevel; this.integrity.isCharging = snapshot.isCharging; this.integrity.currentMa = snapshot.currentMa; this.integrity.satsView = event.satsView; this.integrity.satsUsed = event.satsUsed; this.integrity.snrIdx = event.snrIdx; this.integrity.isPowerTamper = snapshot.isPowerTamper; this.integrity.isSitDetected = event.isSuspiciousMode; this.integrity.lastSitTs = event.lastSitTs; this.integrity.sitVz = snapshot.peakVerticalVelocity; this.integrity.sitVzTs = snapshot.peakVerticalVelocityTs; this.integrity.sitVzRt = snapshot.peakVerticalVelocityRt; this.integrity.sitDz = snapshot.peakVerticalDisplacement; this.integrity.sitBaro = snapshot.peakVerticalDisplacement; this.integrity.sitTilt = snapshot.tiltDegrees; this.integrity.sitShock = snapshot.peakShock; this.integrity.isBatteryLow = snapshot.isBatteryLow; this.integrity.isBatteryCritical = snapshot.isBatteryCritical; this.integrity.locationPendingReason = snapshot.locationPendingReason; this.integrity.isPowerSaveMode = snapshot.isPowerSaveMode; this.integrity.standbyBucket = snapshot.standbyBucket; this.integrity.netInterface = snapshot.netInterface; this.integrity.isStorageLow = snapshot.isStorageLow; this.integrity.isStorageCritical = snapshot.isStorageCritical; this.integrity.isBatterySteepDischarge = snapshot.isBatterySteepDischarge; this.integrity.isCoolingModeActive = snapshot.isCoolingModeActive; this.integrity.gpsHardwareLock = snapshot.isGpsHardwareLock; this.integrity.isUltraLongStationary = snapshot.isUltraLongStationary; this.integrity.isTamperDetected = snapshot.tamperDetected; this.integrity.tamperNote = snapshot.suppressionNote
                this.ts = now; this.isMe = true; this.status = snapshot.status; this.lastValidFixRt = snapshot.lastValidFixRt; this.trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING
            })
            
            // 2. Peer Signaling
            if (event.isPeerActive) {
                connectivitySuite.pushCurrentStatus(
                    deviceId = configManager.deviceId, 
                    viewerId = configManager.viewerId, 
                    isTrackerMode = true, 
                    loc = null, 
                    filtered = proc?.optimizedPoint, 
                    distToTracker = null, 
                    distToHome = proc?.distToHome, 
                    maxAccuracy = proc?.maxAccuracy ?: 0.0, 
                    filteredSpeed = proc?.filteredSpeed ?: 0.0, 
                    vibration = snapshot.vibration, 
                    heading = snapshot.heading, 
                    baroAlt = snapshot.baroAlt, 
                    lux = snapshot.lux, 
                    isNear = snapshot.isNear, 
                    tiltDegrees = snapshot.tiltDegrees, 
                    acousticDb = snapshot.acousticDb, 
                    jumpTier = proc?.jumpTier ?: 0, 
                    isJammer = proc?.jammerDetected ?: false, 
                    isStalled = snapshot.isStalled, 
                    peakShock = snapshot.peakShock, 
                    peakShockTs = now, 
                    luxBaseline = snapshot.luxBaseline, 
                    acousticFloorDb = snapshot.acousticFloorDb, 
                    adaptiveVibrationFloor = snapshot.adaptiveVibrationFloor, 
                    proxIdx = event.proxIdx,
                    proximityCm = event.proximityCm,
                    proximityDebounceMs = event.proximityDebounceMs,
                    vibrationRollingSum = event.vibrationRollingSum,
                    micPending = false, 
                    isTamperDetected = snapshot.tamperDetected, 
                    isPowerTamper = snapshot.isPowerTamper, 
                    isSitDetected = event.isSuspiciousMode, 
                    isSitActive = false, 
                    lastSitTs = event.lastSitTs, 
                    receiptRt = nowRt, 
                    violationUptimeMs = event.violationUptimeMs, 
                    violationPercentage = event.violationPercentage, 
                    verticalVelocity = snapshot.peakVerticalVelocity, 
                    sitVz = snapshot.peakVerticalVelocity, 
                    sitVzTs = snapshot.peakVerticalVelocityTs, 
                    sitVzRt = snapshot.peakVerticalVelocityRt, 
                    sitDz = snapshot.peakVerticalDisplacement, 
                    sitBaro = snapshot.peakVerticalDisplacement, 
                    sitTilt = snapshot.tiltDegrees, 
                    sitShock = snapshot.peakShock, 
                    isClockRegression = snapshot.isClockRegression, 
                    isLocationPending = snapshot.isLocationPending, 
                    locationPendingReason = snapshot.locationPendingReason, 
                    lastValidFixRt = snapshot.lastValidFixRt, 
                    gnssDetail = event.gnssDetail,
                    isBatterySteepDischarge = snapshot.isBatterySteepDischarge,
                    isCoolingModeActive = snapshot.isCoolingModeActive, 
                    batteryLevel = snapshot.batteryLevel, 
                    temp = snapshot.batteryTemp, 
                    isCharging = snapshot.isCharging, 
                    trackerState = if ((proc?.filteredSpeed ?: 0.0) > 0.5) TrackerState.MOVING else TrackerState.PARKING, 
                    status = snapshot.status, 
                    isStorageLow = snapshot.isStorageLow, 
                    isStorageCritical = snapshot.isStorageCritical, 
                    isPowerSaveMode = snapshot.isPowerSaveMode, 
                    standbyBucket = snapshot.standbyBucket, 
                    netInterface = snapshot.netInterface, 
                    snrIdx = event.snrIdx,
                    noiseIdx = event.noiseIdx,
                    luxIdx = event.luxIdx,
                    vibeIdx = event.vibeIdx,
                    liftIdx = event.liftIdx,
                    tiltIdx = event.tiltIdx,
                    baroIdx = event.baroIdx,
                    kineticEnergy = snapshot.kineticEnergy,
                    isAdaptiveJump = snapshot.isAdaptiveJump, 
                    isBatteryLow = snapshot.isBatteryLow, 
                    isBatteryCritical = snapshot.isBatteryCritical, 
                    isUltraLongStationary = snapshot.isUltraLongStationary, 
                    gpsHardwareLock = snapshot.isGpsHardwareLock, 
                    tamperNote = snapshot.suppressionNote
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
            noiseIdx = event.noiseIdx, luxIdx = event.luxIdx, vibeIdx = event.vibeIdx, proxIdx = event.proxIdx, 
            liftIdx = event.liftIdx, snrIdx = event.snrIdx, tiltIdx = event.tiltIdx, baroIdx = event.baroIdx, 
            verticalVelocity = snapshot.peakVerticalVelocity, sitVz = snapshot.peakVerticalVelocity, 
            sitVzTs = snapshot.peakVerticalVelocityTs, sitVzRt = snapshot.peakVerticalVelocityRt, 
            sitDz = snapshot.peakVerticalDisplacement, sitBaro = snapshot.peakVerticalDisplacement, 
            sitTilt = snapshot.tiltDegrees, sitShock = snapshot.peakShock, 
            isBatterySteepDischarge = snapshot.isBatterySteepDischarge, isCoolingModeActive = snapshot.isCoolingModeActive, 
            speed = proc?.filteredSpeed ?: 0.0, bearing = snapshot.bearing, 
            isSitDetected = if (isTrackerMode) event.isSuspiciousMode else false, isSitActive = false,
            currentMa = snapshot.currentMa, locationPendingReason = snapshot.locationPendingReason, 
            kineticEnergy = snapshot.kineticEnergy, isRecoveryEvent = event.recoveryFlagged, 
            cpuLoad = snapshot.cpuLoad, ioWait = snapshot.ioWait, maxIoLatency = snapshot.maxIoLatency, 
            isSilentFailure = snapshot.isSilentFailure, isBatteryLow = snapshot.isBatteryLow, 
            isBatteryCritical = snapshot.isBatteryCritical, isUltraLongStationary = snapshot.isUltraLongStationary
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

    private suspend fun observeAlarmEvents(alarmManager: AppAlarmManager) {
        alarmManager.alarmEvents.collect { event ->
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
    }

    private suspend fun observeIntegrityEvents(alarmManager: AppAlarmManager) {
        integrityMonitor.integrityEvents.collect { event ->
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
            }
        }
    }

    private suspend fun observeProcessorEvents(primary: LocationProcessor, remote: LocationProcessor?) {
        coroutineScope {
            launch { primary.processorEvents.collect { handleProcessorEvent(it, true) } }
            remote?.let { launch { it.processorEvents.collect { handleProcessorEvent(it, false) } } }
        }
    }

    private suspend fun handleProcessorEvent(event: ProcessorEvent, isPrimary: Boolean) {
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
                if (isTrackerMode || !isPrimary) repository.saveDouble(prefix + CHAIR_BASELINE_TILT_KEY, event.baseline)
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

    private suspend fun observeConnectivityEvents(connectivitySuite: ConnectivitySuite) {
        connectivitySuite.connectivityEvents.collect { event ->
            if (event is ConnectivityEvent.PeerPulse) {
                // Future orchestration for peer activity
            }
        }
    }

    private suspend fun observeHistoryEvents() {
        historyManager.historyEvents.collect { event ->
            if (event is HistoryEvent.LogEvent) logManager.logServiceEvent(m = event.message, isImportant = event.isImportant)
        }
    }

    private suspend fun observeSensorEvents(hardwareSuite: HardwareSuite) {
        hardwareSuite.sensorEvents.collect { event ->
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
    }

    private suspend fun observeCommandEvents(commandRouter: CommandRouter) {
        commandRouter.commandEvents.collect { event ->
            when (event) {
                is CommandEvent.WatchdogTrigger -> { /* Service-only reaction */ }
                is CommandEvent.UiPulse -> { /* Service-only reaction */ }
                is CommandEvent.UiVisibilityChanged -> { /* Service-only reaction */ }
                is CommandEvent.ResetTimers -> { /* Service-only reaction */ }
                is CommandEvent.SyncSensors -> { /* Service-only reaction */ }
                is CommandEvent.ExecuteStressTest -> { /* Service-only reaction */ }
                is CommandEvent.SimulateStoragePressure -> { /* Service-only reaction */ }
            }
        }
    }

    private suspend fun observeRevivalEvents(hardwareSuite: HardwareSuite) {
        hardwareSuite.revivalEvents.collect { event ->
            val isTrackerMode = configManager.isTrackerMode
            val roleTag = if (isTrackerMode) "" else "(V) "
            val telem = repository.getLocalLocationSync()
            
            when (event) {
                is HardwareSuite.RevivalEvent.Footprint -> {
                    val msg = "ENERGY AUDIT ${roleTag}: Revival Footprint - Delta: ${event.deltaMa}mA, Temp Rise: ${event.deltaTemp}°C, Duration: ${event.durationMs}ms"
                    logManager.submitToLogSink(
                        msg, "system", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
                        lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                    )
                }
                is HardwareSuite.RevivalEvent.HardwareLock -> {
                    logManager.logServiceEvent(
                        m = "CRITICAL ${roleTag}: GPS_HARDWARE_LOCK - All revival attempts failed. Hardware stall confirmed.", 
                        isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR, 
                        lat = telem.kinetic.lat, lng = telem.kinetic.lng, accuracy = telem.kinetic.maxAccuracy
                    )
                }
                is HardwareSuite.RevivalEvent.Attempt -> logManager.logServiceEvent(m = "GPS REVIVAL ${roleTag}: Hardware restart attempt ${event.count} triggered.", isImportant = false)
                is HardwareSuite.RevivalEvent.Success -> logManager.logServiceEvent(m = "GPS REVIVAL ${roleTag}: Hardware fix restored successfully.", isImportant = true)
                is HardwareSuite.RevivalEvent.RawBurstStarted -> {}
                is HardwareSuite.RevivalEvent.RawBurstEnded -> {}
            }
        }
    }

    private suspend fun observeSirenRequirement(alarmManager: AppAlarmManager) {
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
