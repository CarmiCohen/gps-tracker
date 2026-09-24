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
 * Sep.24.93:
 * - Issue #1265 REMEDIATION: Centralized alert triggers, audio synthesis, and 
 *   forensic logging into a high-cohesion coordinator. Decoupled domain 
 *   reactions from background service lifecycles.
 * - Issue #1292 REMEDIATION: Implemented reactive siren state binding between 
 *   AppAlarmManager and AudioSynthesizer (R-ID 472).
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
    private val historyManager: HistoryManager
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
