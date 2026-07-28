package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandEvent: Reactive event container for system and UI commands.
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _commandEvents with 
 *   BufferOverflow.DROP_OLDEST to ensure non-blocking command routing (R617).
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization. Unified all command 
 *   dispatches into a single SharedFlow stream.
 */
sealed class CommandEvent {
    data class ViewerPulse(val id: String) : CommandEvent()
    object WatchdogTrigger : CommandEvent()
    object UiPulse : CommandEvent()
    data class UiVisibilityChanged(val visible: Boolean) : CommandEvent()
    data class TransientDrop(val drop: Boolean) : CommandEvent()
    object ResetTimers : CommandEvent()
    object SyncSensors : CommandEvent()
    object TriggerForensicTest : CommandEvent()
}

/**
 * CommandRouter: Handles incoming UI commands via SharedFlow and system events via broadcasts.
 * July.27.00:
 * - Architecture Audit: Updated to use centralized PreferenceKeys.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization. Replaced legacy Listener 
 *   with a SharedFlow (commandEvents) for reactive event dispatching.
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
    private val timeProvider: TimeProvider
) {
    private val isRegistered = AtomicBoolean(false)
    private val isObserving = AtomicBoolean(false)

    private val _commandEvents = MutableSharedFlow<CommandEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()

    private val routerExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        Timber.e(throwable, "CRITICAL: Command Router Failure")
        logManager.logServiceEvent("CRITICAL: Command Router Failure: ${throwable.message}", true)
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (configManager.isTrackerMode) {
                val action = intent?.action
                logManager.logServiceEvent("POWER CHANGE: $action")
                
                when (action) {
                    Intent.ACTION_POWER_DISCONNECTED -> integrityMonitor.onPowerDisconnected()
                    Intent.ACTION_POWER_CONNECTED -> integrityMonitor.onPowerConnected()
                }
            }
        }
    }

    private val legacyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ALARM_WAKEUP -> _commandEvents.tryEmit(CommandEvent.WatchdogTrigger)
                ACTION_RELAY_STATUS -> {
                    if (intent.getBooleanExtra("connected", false) == false) {
                        _commandEvents.tryEmit(CommandEvent.TransientDrop(true))
                    }
                }
            }
        }
    }

    fun startObservingCommands(scope: CoroutineScope) {
        if (isObserving.getAndSet(true)) return

        repository.uiCommands
            .onEach { command ->
                try {
                    when (command) {
                        is UiCommand.SyncRequest -> _commandEvents.emit(CommandEvent.UiPulse)
                        is UiCommand.UiVisibilityChanged -> _commandEvents.emit(CommandEvent.UiVisibilityChanged(command.visible))
                        is UiCommand.StopSiren -> {
                            repository.saveLongSync(LAST_ALARM_ACK_TS_KEY, timeProvider.currentTimeMillis())
                            alarmManager.setPowerAlarmPending(false)
                            alarmManager.notifySirenManualStop() 
                            alarmManager.dismissResolvedAlarms()
                            integrityMonitor.clearPowerTamper()
                            sessionManager.notifyTamperCleared() 
                            AudioSynthesizer.stopSiren(timeProvider = timeProvider)
                            notificationManager.cancelAlarm()
                        }
                        is UiCommand.ClearTrails -> repository.clearTrails()
                        is UiCommand.StatsReset -> {
                            _commandEvents.emit(CommandEvent.ResetTimers)
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                        }
                        is UiCommand.SettingsUpdated -> {
                            _commandEvents.emit(CommandEvent.SyncSensors)
                            connectivitySuite.connect(configManager.relayUrl)
                            connectivitySuite.updateIdentity(configManager.deviceId, configManager.viewerId, configManager.isTrackerMode)
                        }
                        is UiCommand.ZoomIn, is UiCommand.ZoomOut, is UiCommand.MapZoomIn, is UiCommand.MapZoomOut -> {}
                        is UiCommand.FullInitializationReset -> {
                            _commandEvents.emit(CommandEvent.ResetTimers)
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                            _commandEvents.emit(CommandEvent.SyncSensors)
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
                                    AudioSynthesizer.playSiren(
                                        type = sirenType,
                                        force = true,
                                        volume = 1.0f,
                                        overrideSilence = true,
                                        context = context,
                                        loop = true,
                                        vibrate = true,
                                        timeProvider = timeProvider,
                                        isTrackerMode = false 
                                    )
                                    delay(3000)
                                    AudioSynthesizer.stopSiren(0, timeProvider = timeProvider)
                                    logManager.logServiceEvent("TEST ALARM: Siren stopped")
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Timber.e(e, "Error during test alarm")
                                }
                            }
                        }
                        is UiCommand.ExecuteForensicTest -> {
                            _commandEvents.emit(CommandEvent.TriggerForensicTest)
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
            addAction(ACTION_ALARM_WAKEUP); addAction(ACTION_RELAY_STATUS)
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

        try { context.unregisterReceiver(legacyReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(powerReceiver) } catch (e: Exception) {}
    }
}
