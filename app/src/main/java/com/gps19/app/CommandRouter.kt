package com.gps19.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandRouter: Handles incoming UI commands via SharedFlow and system events via broadcasts.
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
@Singleton
class CommandRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val configManager: ConfigManager,
    private val logManager: LogManager,
    private val connectivitySuite: ConnectivitySuite,
    private val alarmManager: AppAlarmManager,
    private val notificationManager: AppNotificationManager,
    private val sessionManager: SessionManager,
    private val locationProcessor: LocationProcessor,
    private val repository: MainRepository,
    private val integrityMonitor: IntegrityMonitor,
    private val timeProvider: TimeProvider,
    private val audioSynthesizer: AudioSynthesizer,
    private val historyManager: HistoryManager,
    private val domainEventBus: DomainEventBus,
    private val sirenLockoutUseCase: SirenLockoutUseCase
) {
    private val isRegistered = AtomicBoolean(false)
    private val isObserving = AtomicBoolean(false)

    private val routerExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        Timber.e(throwable, "CRITICAL: Command Router Failure")
        logManager.logServiceEvent("CRITICAL: Command Router Failure: ${throwable.message}", true)
    }

    private val powerReceiver = object : ManagedBroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (configManager.isTrackerMode) {
                val action = intent.action
                logManager.logServiceEvent("POWER CHANGE: $action")
                
                when (action) {
                    Intent.ACTION_POWER_DISCONNECTED -> integrityMonitor.onPowerDisconnected()
                    Intent.ACTION_POWER_CONNECTED -> integrityMonitor.onPowerConnected()
                }
            }
        }
    }

    private val legacyReceiver = object : ManagedBroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ALARM_WAKEUP -> domainEventBus.emit(CommandEvent.WatchdogTrigger)
            }
        }
    }

    fun startObservingCommands(scope: CoroutineScope) {
        if (isObserving.getAndSet(true)) return

        repository.uiCommands
            .onEach { command ->
                try {
                    when (command) {
                        is UiCommand.SyncRequest -> domainEventBus.emit(CommandEvent.UiPulse)
                        is UiCommand.UiVisibilityChanged -> domainEventBus.emit(CommandEvent.UiVisibilityChanged(command.visible))
                        is UiCommand.StopSiren -> {
                            val now = timeProvider.currentTimeMillis
                            val role = if (configManager.isTrackerMode) AppRole.TRACKER else AppRole.VIEWER_REMOTE
                            
                            repository.saveLongSync(role, LAST_ALARM_ACK_TS_KEY, now)
                            
                            // Issue #1410: Remote synchronization
                            if (!configManager.isTrackerMode) {
                                connectivitySuite.emit("acknowledge_alarm", JSONObject().apply {
                                    put("id", configManager.deviceId)
                                    put("viewer_id", configManager.viewerId)
                                    put("ack_ts", now)
                                    put("from", "viewer")
                                }, SignalingPriority.HIGH)
                            }

                            alarmManager.setPowerAlarmPending(false, role)
                            alarmManager.notifySirenManualStop() 
                            alarmManager.dismissResolvedAlarms()
                            integrityMonitor.clearPowerTamper()
                            sessionManager.notifyTamperCleared() 
                            sirenLockoutUseCase.setSilence(SILENCE_TIMEOUT_MS)
                            audioSynthesizer.stopSiren(timeProvider = timeProvider)
                            notificationManager.cancelAlarm()
                        }
                        is UiCommand.ClearTrails -> repository.clearTrails()
                        is UiCommand.StatsReset -> {
                            domainEventBus.emit(CommandEvent.ResetTimers)
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                            historyManager.reset()
                            integrityMonitor.resetStats()
                        }
                        is UiCommand.SettingsUpdated -> {
                            domainEventBus.emit(CommandEvent.SyncSensors)
                            connectivitySuite.connect(configManager.relayUrl)
                            connectivitySuite.updateIdentity(configManager.deviceId, configManager.viewerId, configManager.isTrackerMode)
                        }
                        is UiCommand.ZoomIn, is UiCommand.ZoomOut, is UiCommand.MapZoomIn, is UiCommand.MapZoomOut -> {}
                        is UiCommand.FullInitializationReset -> {
                            domainEventBus.emit(CommandEvent.ResetTimers)
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                            historyManager.reset()
                            integrityMonitor.resetStats()
                            domainEventBus.emit(CommandEvent.SyncSensors)
                        }
                        is UiCommand.ExecuteTestAlarm -> {
                            if (configManager.isTrackerMode) {
                                logManager.logServiceEvent("TEST ALARM: Suppressed in Tracker Mode (Stealth)", false)
                                return@onEach
                            }
                            scope.launch {
                                try {
                                    logManager.logServiceEvent("TEST ALARM: Triggering 3s physical siren", true)
                                    val sirenType = repository.getString(SELECTED_SIREN_KEY, "Siren")
                                    audioSynthesizer.playSiren(
                                        type = sirenType,
                                        force = true,
                                        volume = 1.0f,
                                        overrideSilence = true,
                                        loop = true,
                                        vibrate = true,
                                        timeProvider = timeProvider,
                                        isTrackerMode = false 
                                    )
                                    delay(3000)
                                    audioSynthesizer.stopSiren(0, timeProvider = timeProvider)
                                    logManager.logServiceEvent("TEST ALARM: Siren stopped")
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Timber.e(e, "Error during test alarm")
                                }
                            }
                        }
                        is UiCommand.ExecuteStressTest -> {
                            Timber.i("CommandRouter: Routing ExecuteStressTest to domain event bus.")
                            domainEventBus.emit(CommandEvent.ExecuteStressTest)
                        }
                        is UiCommand.ExecuteLogPressureTest -> {
                            Timber.i("CommandRouter: Routing ExecuteLogPressureTest to domain event bus.")
                            domainEventBus.emit(CommandEvent.ExecuteLogPressureTest)
                        }
                        is UiCommand.ExecuteNetworkStressTest -> {
                            domainEventBus.emit(CommandEvent.ExecuteNetworkStressTest)
                        }
                        is UiCommand.SimulateStoragePressure -> {
                            integrityMonitor.simulateStoragePressure(command.active, command.isCritical)
                        }
                        is UiCommand.SimulateMemoryPressure -> {
                            integrityMonitor.simulateMemoryPressure(command.active, command.level)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error processing UI command: ${command::class.java.simpleName}")
                    logManager.logServiceEvent("ERROR: Failed to process command ${command::class.java.simpleName}: ${e.message}", false)
                }
            }
            .launchIn(CoroutineScope(scope.coroutineContext + routerExceptionHandler))
    }

    fun register() {
        if (isRegistered.getAndSet(true)) return

        val legacyFilter = IntentFilter().apply {
            addAction(ACTION_ALARM_WAKEUP)
        }
        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(legacyReceiver, legacyFilter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(powerReceiver, powerFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(legacyReceiver, legacyFilter)
            context.registerReceiver(powerReceiver, powerFilter)
        }
    }

    fun unregister() {
        if (!isRegistered.getAndSet(false)) return
        legacyReceiver.unregister(context)
        powerReceiver.unregister(context)
    }
}
