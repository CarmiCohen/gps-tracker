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
 * Sep.25.04:
 * - Issue #1324: Added PeerStatusReceived handling to offload peer telemetry persistence from signaling.
 * - Issue #1327: Added PeerConnectionChanged handling to log lifecycle-only connection events.
 * Sep.25.03:
 * - Issue #1323: Implemented handleViewerLocationUpdated to converge Viewer 
 *   self-tracking persistence into the bus-centric model.
 * Sep.25.01:
 * - Issue #1322: Converged all component-level flow observations into a single 
 *   DomainEventBus listener. Eliminated flow fragmentation and simplified the 
 *   orchestration layer. Fixed saveDouble suspend context error.
 * Sep.25.00:
 * - Issue #1325: Migrated metadata mapping to use SystemEvaluationSnapshot.
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
        val health = event.health
        val now = event.now
        val nowRt = event.nowRt
        val isTrackerMode = event.isTrackerMode

        // 1. Repository Persistence
        if (isTrackerMode) {
            repository.updateLocation(LocationUpdate().apply {
                this.kinetic.lat = proc?.optimizedPoint?.lat ?: 0.0; this.kinetic.lng = proc?.optimizedPoint?.lng ?: 0.0; this.kinetic.alt = proc?.optimizedPoint?.alt ?: 0.0; this.kinetic.speed = proc?.filteredSpeed ?: 0.0; this.kinetic.accuracy = proc?.currentAccuracy ?: 0.0; this.kinetic.bearing = snapshot.bearing; this.kinetic.gpsTs = proc?.timestamp ?: 0L; this.kinetic.rt = nowRt; this.kinetic.maxAccuracy = proc?.maxAccuracy ?: 0.0; this.kinetic.kineticEnergy = snapshot.kineticEnergy; this.kinetic.verticalVelocity = snapshot.peakVerticalVelocity
                this.atmospheric.temp = snapshot.batteryTemp; this.atmospheric.maxTemp = health.maxTemp; this.atmospheric.vibration = snapshot.vibration; this.atmospheric.heading = snapshot.heading; this.atmospheric.baroAlt = snapshot.baroAlt; this.atmospheric.lux = snapshot.lux; this.atmospheric.isNear = snapshot.isNear; this.atmospheric.tiltDegrees = snapshot.tiltDegrees; this.atmospheric.acousticDb = snapshot.acousticDb; this.atmospheric.peakVibrationShock = snapshot.peakShock; this.atmospheric.peakVibrationShockTs = now; this.atmospheric.noiseIdx = event.noiseIdx; this.atmospheric.luxIdx = event.luxIdx; this.atmospheric.vibeIdx = event.vibeIdx; this.atmospheric.liftIdx = event.liftIdx; this.atmospheric.tiltIdx = event.tiltIdx; this.atmospheric.baroIdx = event.baroIdx; this.atmospheric.luxBaseline = snapshot.luxBaseline; this.atmospheric.acousticFloorDb = snapshot.acousticFloorDb; this.atmospheric.adaptiveVibrationFloor = snapshot.adaptiveVibrationFloor; this.atmospheric.proxIdx = snapshot.proxIdx; this.atmospheric.proximityCm = snapshot.proximityCm; this.atmospheric.proximityDebounceMs = snapshot.proximityDebounceMs; this.atmospheric.vibrationRollingSum = snapshot.vibrationRollingSum
                this.integrity.battery = snapshot.batteryLevel; this.integrity.isCharging = snapshot.isCharging; this.integrity.currentMa = snapshot.currentMa; this.integrity.satsView = snapshot.satsView; this.integrity.satsUsed = snapshot.satsUsed; this.integrity.snrIdx = event.snrIdx; this.integrity.isPowerTamper = snapshot.isPowerTamper; this.integrity.isSitDetected = event.isSuspiciousMode; this.integrity.lastSitTs = event.lastSitTs; this.integrity.sitVz = snapshot.peakVerticalVelocity; this.integrity.sitVzTs = snapshot.peakVerticalVelocityTs; this.integrity.sitVzRt = snapshot.peakVerticalVelocityRt; this.integrity.sitDz = snapshot.peakVerticalDisplacement; this.integrity.sitBaro = snapshot.peakVerticalDisplacement; this.integrity.sitTilt = snapshot.tiltDegrees; this.integrity.sitShock = snapshot.peakShock; this.integrity.isBatteryLow = snapshot.isBatteryLow; this.integrity.isBatteryCritical = snapshot.isBatteryCritical; this.integrity.locationPendingReason = snapshot.locationPendingReason; this.integrity.isPowerSaveMode = snapshot.isPowerSaveMode; this.integrity.standbyBucket = snapshot.standbyBucket; this.integrity.netInterface = snapshot.netInterface; this.integrity.isStorageLow = snapshot.isStorageLow; this.integrity.isStorageCritical = snapshot.isStorageCritical; this.integrity.isBatterySteepDischarge = snapshot.isBatterySteepDischarge; this.integrity.isCoolingModeActive = snapshot.isCoolingModeActive; this.integrity.gpsHardwareLock = snapshot.isGpsHardwareLock; this.integrity.isUltraLongStationary = snapshot.isUltraLongStationary; this.integrity.isTamperDetected = snapshot.tamperDetected; this.integrity.tamperNote = snapshot.suppressionNote
                this.integrity.violationUptimeMs = snapshot.violationUptimeMs
                this.integrity.violationPercentage = snapshot.violationPercentage
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
                    proxIdx = snapshot.proxIdx,
                    proximityCm = snapshot.proximityCm,
                    proximityDebounceMs = snapshot.proximityDebounceMs,
                    vibrationRollingSum = snapshot.vibrationRollingSum,
                    micPending = false, 
                    isTamperDetected = snapshot.tamperDetected, 
                    isPowerTamper = snapshot.isPowerTamper, 
                    isSitDetected = event.isSuspiciousMode, 
                    isSitActive = false, 
                    lastSitTs = event.lastSitTs, 
                    receiptRt = nowRt, 
                    violationUptimeMs = snapshot.violationUptimeMs, 
                    violationPercentage = snapshot.violationPercentage, 
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
                    tamperNote = snapshot.suppressionNote,
                    satsUsed = snapshot.satsUsed,
                    satsView = snapshot.satsView
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
            noiseIdx = event.noiseIdx, luxIdx = event.luxIdx, vibeIdx = event.vibeIdx, proxIdx = snapshot.proxIdx,
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

    private suspend fun handleViewerLocationUpdated(event: DomainEvent.ViewerLocationUpdated) {
        val proc = event.processed
        val snapshot = event.snapshot
        val health = event.health

        repository.updateLocation(LocationUpdate().apply {
            this.kinetic.lat = snapshot.lat; this.kinetic.lng = snapshot.lng; this.kinetic.alt = snapshot.alt; this.kinetic.speed = snapshot.speed; this.kinetic.accuracy = snapshot.accuracy
            this.kinetic.bearing = snapshot.bearing; this.kinetic.gpsTs = snapshot.gpsTs; this.kinetic.rt = event.nowRt; this.kinetic.maxAccuracy = proc.maxAccuracy
            this.atmospheric.temp = health.batteryTemp; this.atmospheric.maxTemp = health.maxTemp
            this.integrity.battery = health.batteryLevel; this.integrity.isCharging = health.isCharging; this.integrity.satsView = snapshot.satsView; this.integrity.satsUsed = snapshot.satsUsed; this.integrity.currentMa = health.currentMa
            this.integrity.snrIdx = ((snapshot.snrSnapshot ?: 0.0) / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            this.ts = event.nowTs; this.isMe = true; this.lastValidFixRt = snapshot.lastValidFixRt; this.status = proc.status; this.isClockRegression = proc.isClockRegression
        })
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
