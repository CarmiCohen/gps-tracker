package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * AppEventCoordinator: Unified domain event orchestrator.
 * Sep.26.2:
 * - Issue #1333: Peer Connection State Caching. Introduced peerConnectionCache 
 *   to suppress redundant lifecycle logging. Added cache clearing to 
 *   ResetTimers command handler to ensure clean state after session resets.
 * - Issue #1332 Remediation: Removed stale reference to ViewerLocationUpdated.
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
    
    // Issue #1333: Cache to suppress redundant peer lifecycle logs.
    private val peerConnectionCache = ConcurrentHashMap<String, Boolean>()

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
        repository.updateLocation(TelemetryMapper.mapSnapshotToUpdate(snapshot, proc, isMe = true, ts = now))
        
        // 2. Peer Signaling (Issue #1314: Convergence)
        if (isTrackerMode && event.isPeerActive) {
            connectivitySuite.sendTelemetry(TelemetryMapper.mapSnapshotToStatus(
                snapshot = snapshot,
                processed = proc,
                deviceId = configManager.deviceId,
                viewerId = configManager.viewerId,
                now = now,
                nowRt = nowRt,
                gnssDetail = event.gnssDetail,
                isSuspiciousMode = event.isSuspiciousMode,
                lastSitTs = event.lastSitTs
            ))
        }

        // 3. Ribbon Updates (Issue #1314: Simplified Event-Driven Mapping)
        historyManager.updateRibbons(event)
    }

    private fun handlePeerConnectionChanged(event: DomainEvent.PeerConnectionChanged) {
        val lastState = peerConnectionCache[event.peerId]
        if (lastState == event.isConnected) return // Suppress redundant logging
        
        peerConnectionCache[event.peerId] = event.isConnected

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
        when (event) {
            is CommandEvent.ResetTimers -> {
                // Issue #1333: Clear peer connection cache on session reset
                peerConnectionCache.clear()
            }
            else -> {
                // Other commands handled in MonitorService
            }
        }
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
