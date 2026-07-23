package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.gps19.core.engine.*
import com.gps19.core.engine.LocationProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandRouter: Handles incoming UI commands via SharedFlow and system events via broadcasts.
 * July.22.11:
 * - Issue #520: Purge Signaling Command Leftovers. Removed SendSettingsCmd handling.
 * July.22.04:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 * v9.5.0: Issue #513 - Flatten Service Architecture (ConnectivitySuite).
 */
@Singleton
class CommandRouter @Inject constructor(
    @ApplicationContext private val context: Context,
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
    interface Listener {
        fun onViewerPulse(id: String)
        fun onWatchdogTrigger()
        fun onUiPulse()
        fun onUiVisibilityChanged(visible: Boolean)
        fun onTransientDrop(drop: Boolean)
        fun onResetTimers()
        fun onSyncSensors()
        fun onTriggerForensicTest()
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

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
                ACTION_ALARM_WAKEUP -> listener?.onWatchdogTrigger()
                ACTION_RELAY_STATUS -> {
                    if (intent.getBooleanExtra("connected", false) == false) listener?.onTransientDrop(true)
                }
            }
        }
    }

    fun startObservingCommands(scope: CoroutineScope) {
        repository.uiCommands
            .onEach { command ->
                try {
                    when (command) {
                        is UiCommand.SyncRequest -> listener?.onUiPulse()
                        is UiCommand.UiVisibilityChanged -> listener?.onUiVisibilityChanged(command.visible)
                        is UiCommand.StopSiren -> {
                            repository.saveLongSync(MainRepository.LAST_ALARM_ACK_TS_KEY, timeProvider.currentTimeMillis())
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
                            listener?.onResetTimers()
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                        }
                        is UiCommand.SettingsUpdated -> {
                            listener?.onSyncSensors()
                            connectivitySuite.connect(configManager.relayUrl)
                            connectivitySuite.updateIdentity(configManager.deviceId, configManager.viewerId, configManager.isTrackerMode)
                        }
                        is UiCommand.ZoomIn, is UiCommand.ZoomOut, is UiCommand.MapZoomIn, is UiCommand.MapZoomOut -> {}
                        is UiCommand.FullInitializationReset -> {
                            listener?.onResetTimers()
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            connectivitySuite.resetPeerStats()
                            listener?.onSyncSensors()
                        }
                        is UiCommand.TriggerTestAlarm -> {
                            scope.launch {
                                try {
                                    logManager.logServiceEvent("TEST ALARM: Triggering 3s physical siren", true)
                                    val sirenType = repository.getString(MainRepository.SELECTED_SIREN_KEY, "Siren")
                                    AudioSynthesizer.playSiren(
                                        type = sirenType,
                                        force = true,
                                        volume = 1.0f,
                                        overrideSilence = true,
                                        context = context,
                                        loop = true,
                                        vibrate = true,
                                        timeProvider = timeProvider
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
                        is UiCommand.TriggerForensicTest -> {
                            listener?.onTriggerForensicTest()
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
        try { context.unregisterReceiver(legacyReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(powerReceiver) } catch (e: Exception) {}
    }
}
