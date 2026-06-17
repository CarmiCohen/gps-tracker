package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.gps19.core.engine.*
import com.gps19.core.engine.LocationProcessor // Explicit import to override local duplicate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

/**
 * CommandRouter: Handles incoming UI commands via SharedFlow and system events via broadcasts.
 * v8.8.21: Migrated to TimeProvider for all timing logic.
 * v8.8.28: Standardized signaling keys to snake_case (viewer_id, from_viewer).
 */
class CommandRouter(
    private val context: Context,
    private val configManager: ConfigManager,
    private val logManager: LogManager,
    private val networkManager: AppNetworkManager,
    private val alarmManager: AppAlarmManager,
    private val notificationManager: AppNotificationManager,
    private val sessionManager: SessionManager,
    private val locationProcessor: LocationProcessor,
    private val remoteHandler: RemoteHandler,
    private val repository: MainRepository,
    private val syncManager: SyncManager,
    private val integrityMonitor: IntegrityMonitor,
    private val timeProvider: TimeProvider,
    private val onViewerPulse: (String) -> Unit,
    private val onWatchdogTrigger: () -> Unit,
    private val onUiPulse: () -> Unit,
    private val onUiVisibilityChanged: (Boolean) -> Unit,
    private val onTransientDrop: (Boolean) -> Unit,
    private val onResetTimers: () -> Unit = {},
    private val onSyncSensors: () -> Unit = {}
) {

    private val routerExceptionHandler = CoroutineExceptionHandler { _, throwable ->
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
                ACTION_ALARM_WAKEUP -> onWatchdogTrigger()
                ACTION_RELAY_STATUS -> {
                    if (intent.getBooleanExtra("connected", false) == false) onTransientDrop(true)
                }
            }
        }
    }

    fun startObservingCommands(scope: CoroutineScope) {
        repository.uiCommands
            .onEach { command ->
                try {
                    when (command) {
                        is UiCommand.SyncRequest -> onUiPulse()
                        is UiCommand.UiVisibilityChanged -> onUiVisibilityChanged(command.visible)
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
                            onResetTimers()
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            remoteHandler.resetStats()
                        }
                        is UiCommand.SendSettingsCmd -> {
                            try {
                                val data = JSONObject(command.data)
                                data.put("viewer_id", configManager.viewerId)
                                data.put("from_viewer", true)
                                networkManager.emit("settings_update", data)
                            } catch (e: Exception) {
                                Timber.e(e, "Error parsing settings command data")
                            }
                        }
                        is UiCommand.SettingsUpdated -> {
                            onSyncSensors()
                            networkManager.connect(configManager.relayUrl)
                            networkManager.updateIdentity(configManager.deviceId, configManager.viewerId, configManager.isTrackerMode)
                            syncManager.startSyncLoop(configManager.deviceId, configManager.viewerId, configManager.isTrackerMode)
                        }
                        is UiCommand.PushSettings -> {
                            networkManager.pushSettings()
                        }
                        is UiCommand.ZoomIn, is UiCommand.ZoomOut, is UiCommand.MapZoomIn, is UiCommand.MapZoomOut -> {}
                        is UiCommand.FullInitializationReset -> {
                            onResetTimers()
                            sessionManager.reset()
                            locationProcessor.resetStats()
                            remoteHandler.resetStats()
                            onSyncSensors()
                        }
                        is UiCommand.CalibrateChair -> {
                            if (configManager.isTrackerMode) {
                                locationProcessor.sentinel.resetChairBaseline()
                                logManager.logServiceEvent("MANUAL CALIBRATION: Chair baseline zeroed", true)
                            } else {
                                val cmd = JSONObject().apply {
                                    put("id", configManager.deviceId)
                                    put("viewer_id", configManager.viewerId)
                                    put("from_viewer", true)
                                    put("type", "calibrate_chair")
                                }
                                networkManager.emit("location_update", cmd)
                            }
                        }
                        is UiCommand.TriggerTestAlarm -> {
                            scope.launch {
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
                            }
                        }
                    }
                } catch (e: Exception) {
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
