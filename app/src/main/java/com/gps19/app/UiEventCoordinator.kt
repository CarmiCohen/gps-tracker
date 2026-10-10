package com.gps19.app

import android.Manifest
import android.os.Build
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UiEventCoordinator: Central authority for routing UI events to domain logic.
 * Oct.10.2:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API.
 * Oct.6.2:
 * - Issue #AUDIT-1006-5: Added ExecuteLogPressureTest routing.
 */
@Singleton
class UiEventCoordinator @Inject constructor(
    private val repository: MainRepository,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val sessionUseCase: SessionUseCase,
    private val alertUseCase: AlertUseCase,
    private val spatialLogicUseCase: SpatialLogicUseCase,
    private val telemetryUseCase: TelemetryUseCase,
    private val timeProvider: TimeProvider,
    private val audioSynthesizer: AudioSynthesizer,
    private val sirenLockoutUseCase: SirenLockoutUseCase,
    private val systemStatusProvider: SystemStatusProvider,
    private val sessionManager: SessionManager
) {

    private var autoSaveJob: Job? = null

    fun handleEvent(
        event: UiEvent,
        currentState: MainUiState,
        scope: CoroutineScope,
        onStateUpdate: ( (MainUiState) -> MainUiState ) -> Unit,
        onKinematicUpdate: ( (KinematicState) -> KinematicState ) -> Unit,
        onDiagnosticUpdate: ( (DiagnosticState) -> DiagnosticState ) -> Unit,
        onReplayRequest: (Long?) -> Unit,
        onCameraAction: (CameraAction) -> Unit,
        onUiEffect: (UiEffect) -> Unit
    ) {
        when (event) {
            // --- Navigation & Visibility ---
            is UiEvent.ToggleMap, is UiEvent.ToggleLog, is UiEvent.ToggleSettings,
            is UiEvent.TogglePhoneSetup, is UiEvent.ToggleRibbons, is UiEvent.SetDashboardExpanded,
            is UiEvent.ToggleGnssDetail, is UiEvent.SetSubSettings, is UiEvent.ShowStopTrackingConfirmation,
            is UiEvent.NavigateToDiagnostics, is UiEvent.SetPendingMode -> {
                if (event is UiEvent.ToggleSettings) {
                    if (event.visible) {
                        onStateUpdate { it.copy(settings = it.settings.copy(draftSettings = settingsUseCase.prepareDraft(it))) }
                    } else {
                        commitSettings(currentState.settings.draftSettings, scope, onStateUpdate)
                    }
                }
                onStateUpdate { it.copy(navigation = navigationUseCase.handleNavigationEvent(event, it)) }
            }

            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && currentState.navigation.isSettingsOpen) {
                    commitSettings(currentState.settings.draftSettings, scope, onStateUpdate)
                }
            }

            // --- Session Control ---
            is UiEvent.SetSystemActive -> {
                onStateUpdate { it.copy(session = it.session.copy(isSystemActive = event.active)) }
                scope.launch(Dispatchers.IO) { sessionUseCase.setSystemActive(event.active) }
            }

            is UiEvent.SetAppMode -> {
                scope.launch(Dispatchers.Main.immediate) {
                    onKinematicUpdate { it.apply { reset() } }
                    onDiagnosticUpdate { it.apply { reset() } }
                    val newStartTime = sessionUseCase.setAppMode(event.mode)
                    onStateUpdate { it.copy(
                        session = it.session.copy(
                            appMode = event.mode,
                            appStartTime = newStartTime ?: it.session.appStartTime,
                            isSystemActive = if (event.mode != null) true else it.session.isSystemActive
                        )
                    )}
                }
            }

            is UiEvent.InitiateMode -> handleInitiateMode(event.mode, currentState, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, onUiEffect)
            is UiEvent.RequestProceedToMode -> handleProceedToMode(event.mode, currentState, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, onUiEffect)
            is UiEvent.ConfirmBackgroundDisclosure -> {
                if (event.confirmed) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        onUiEffect(UiEffect.RequestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), event.mode))
                    }
                } else {
                    onStateUpdate { it.copy(spatial = it.spatial.copy(isManualSelectionInProgress = false)) }
                    handleProceedToMode(event.mode, currentState, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, onUiEffect)
                }
            }

            is UiEvent.ConfirmStopTracking, UiEvent.ManualExit -> {
                onKinematicUpdate { it.apply { reset() } }
                onDiagnosticUpdate { it.apply { reset() } }
                onStateUpdate { it.copy(
                    session = it.session.copy(isSystemActive = false, appMode = null),
                    settings = it.settings.copy(isSafeMode = false)
                ) }
                repository.setSafeMode(false)
                scope.launch(Dispatchers.IO) { 
                    sessionUseCase.stopTrackingSession() 
                    withContext(Dispatchers.Main) {
                        if (event is UiEvent.ManualExit) onUiEffect(UiEffect.CleanupAndExit)
                        else onUiEffect(UiEffect.StopTracking)
                    }
                }
            }

            // --- Alarms & Safety ---
            is UiEvent.DismissAlarms -> {
                onDiagnosticUpdate { it.apply { isRedScreenVisible = false } }
                scope.launch(Dispatchers.IO) { alertUseCase.dismissAlarms() }
            }

            is UiEvent.StopSiren -> {
                scope.launch(Dispatchers.IO) { alertUseCase.stopSiren(event.causes) }
            }

            is UiEvent.SetRedScreenVisible -> {
                onDiagnosticUpdate { it.apply { isRedScreenVisible = event.visible } }
            }

            is UiEvent.ToggleTestSiren -> {
                val isPlaying = audioSynthesizer.isPlaying()
                if (isPlaying) {
                    audioSynthesizer.stopSiren(timeProvider = timeProvider)
                } else {
                    val s = currentState.settings.draftSettings.alertSettings
                    val volume = if (s.useMaxVolume) 1.0f else if (s.useCustomVolume) s.alarmVolume else 1.0f
                    audioSynthesizer.playSiren(
                        currentState.settings.selectedSirenType, force = true, volume = volume,
                        overrideSilence = s.overrideSilence,
                        loop = true, vibrate = s.vibrationEnabled,
                        timeProvider = timeProvider
                    )
                }
            }

            // --- Settings & Drafts (Debounced) ---
            is UiEvent.UpdateDraftDeviceId -> updateDraft(scope, onStateUpdate) { it.copy(deviceId = event.id) }
            is UiEvent.UpdateDraftViewerId -> updateDraft(scope, onStateUpdate) { it.copy(viewerId = event.id) }
            is UiEvent.UpdateDraftRelayUrl -> updateDraft(scope, onStateUpdate) { it.copy(relayUrl = event.url) }
            is UiEvent.UpdateDraftMaxDistance -> updateDraft(scope, onStateUpdate) { it.copy(maxDistance = event.distance) }
            is UiEvent.UpdateDraftAlertSettings -> updateDraft(scope, onStateUpdate) { it.copy(alertSettings = event.settings) }
            is UiEvent.UpdateDraftAlarmVolume -> updateDraft(scope, onStateUpdate) { 
                val newAlerts = it.alertSettings.copy(alarmVolume = event.volume)
                it.copy(alertSettings = newAlerts)
            }
            is UiEvent.CommitSettings -> commitSettings(currentState.settings.draftSettings, scope, onStateUpdate)

            is UiEvent.BulkUpdateSettings -> {
                scope.launch(Dispatchers.IO) {
                    settingsUseCase.bulkUpdateSettings(
                        deviceId = event.deviceId,
                        viewerId = event.viewerId,
                        relayUrl = event.relayUrl,
                        maxDistance = event.maxDistance,
                        alertSettings = event.alertSettings,
                        homePoints = event.homePoints
                    )
                }
            }

            is UiEvent.DismissIdentitySanitization -> {
                onStateUpdate { it.copy(settings = it.settings.copy(isIdentitySanitized = false)) }
                scope.launch(Dispatchers.IO) { repository.saveBoolean(IDENTITY_SANITIZED_KEY, false) }
            }

            // --- Spatial & Geofencing ---
            is UiEvent.SetFenceVisible, is UiEvent.SetViolationsVisible, is UiEvent.SetGeofenceViolationsVisible,
            is UiEvent.SetMapButtonsVisible, is UiEvent.SetMapLocked, is UiEvent.SetGeofenceMode -> {
                onStateUpdate { spatialLogicUseCase.handleMapEvent(event, it) }
            }

            is UiEvent.MapZoomIn -> onCameraAction(CameraAction.ZoomIn)
            is UiEvent.MapZoomOut -> onCameraAction(CameraAction.ZoomOut)
            is UiEvent.CenterTracker -> {
                onStateUpdate { spatialLogicUseCase.handleMapEvent(event, it) }
                onCameraAction(CameraAction.CenterTracker)
            }
            is UiEvent.CenterViewer -> {
                onStateUpdate { spatialLogicUseCase.handleMapEvent(event, it) }
                onCameraAction(CameraAction.CenterViewer)
            }

            is UiEvent.MapTap -> handleMapTap(event.point, currentState, scope, onStateUpdate, onCameraAction, onUiEffect)

            is UiEvent.AddHomePoint -> {
                scope.launch(Dispatchers.IO) {
                    val newPoints = spatialLogicUseCase.addHomePoint(event.point)
                    withContext(Dispatchers.Main.immediate) {
                        onStateUpdate { it.copy(spatial = it.spatial.copy(homePoints = newPoints, isFenceVisible = true)) }
                    }
                }
            }

            is UiEvent.RemoveHomePoint -> {
                scope.launch(Dispatchers.IO) {
                    val newPoints = spatialLogicUseCase.removeHomePoint(event.index)
                    withContext(Dispatchers.Main.immediate) {
                        onStateUpdate { it.copy(spatial = it.spatial.copy(homePoints = newPoints)) }
                    }
                }
            }

            is UiEvent.ClearHomePoints -> scope.launch(Dispatchers.IO) {
                val newPoints = spatialLogicUseCase.clearHomePoints(currentState.spatial.maxDistance)
                withContext(Dispatchers.Main.immediate) {
                    onStateUpdate { it.copy(spatial = it.spatial.copy(homePoints = newPoints)) }
                }
            }

            // --- Simulation & Recovery ---
            is UiEvent.TriggerRecovery -> {
                if (currentState.simulation.isRecoveryPending) {
                    onStateUpdate { it.copy(
                        navigation = navigationUseCase.handleNavigationEvent(event, it),
                        simulation = it.simulation.copy(isRecoveryPending = false)
                    )}
                }
            }
            is UiEvent.SetRecoveryPending -> onStateUpdate { it.copy(simulation = it.simulation.copy(isRecoveryPending = event.pending)) }
            is UiEvent.SetForensicSimulation -> {
                onStateUpdate { it.copy(simulation = it.simulation.copy(isForensicStallSimulated = event.active)) }
                repository.setForensicStallSimulation(event.active)
            }
            is UiEvent.SetStorageSimulation -> {
                onStateUpdate { it.copy(simulation = it.simulation.copy(isStorageSimulated = event.active, isStorageCriticalSimulated = event.isCritical)) }
                repository.sendCommand(UiCommand.SimulateStoragePressure(event.active, event.isCritical))
            }
            is UiEvent.SetMemoryPressureSimulation -> {
                onStateUpdate { it.copy(simulation = it.simulation.copy(isMemorySimulated = event.active, simulatedMemoryLevel = event.level)) }
                repository.sendCommand(UiCommand.SimulateMemoryPressure(event.active, event.level))
            }
            is UiEvent.ExecuteStressTest -> repository.sendCommand(UiCommand.ExecuteStressTest)
            is UiEvent.ExecuteLogPressureTest -> repository.sendCommand(UiCommand.ExecuteLogPressureTest)
            is UiEvent.ExecuteNetworkStressTest -> repository.sendCommand(UiCommand.ExecuteNetworkStressTest)

            // --- Logging & Stats ---
            is UiEvent.LogAction -> {
                repository.addLog(LogEntry(
                    localId = UUID.randomUUID().toString(),
                    timestamp = timeProvider.currentTimeMillis,
                    message = event.message,
                    type = event.type.uppercase(),
                    isImportant = event.isImportant,
                    id = currentState.settings.deviceId,
                    viewerId = currentState.settings.viewerId,
                    isSpecial = event.isSpecial,
                    specialColor = event.specialColor
                ))
            }
            is UiEvent.ClearLogs -> repository.clearLogs()
            is UiEvent.ResetStats -> scope.launch(Dispatchers.IO) { repository.resetStats() }
            is UiEvent.SetLogFilterShowDetails -> repository.updateLogFilters(details = event.show)
            is UiEvent.SetLogFilterShowRecovered -> repository.updateLogFilters(recovered = event.show)

            // --- UI Transient States ---
            is UiEvent.SetManualSelection -> onStateUpdate { it.copy(spatial = it.spatial.copy(isManualSelectionInProgress = event.active)) }
            is UiEvent.SetSettlingActive -> onStateUpdate { it.copy(session = it.session.copy(isSettlingActive = event.active)) }
            is UiEvent.ToggleSetupBypass -> onStateUpdate { it.copy(session = it.session.copy(isSetupBypassActive = event.active)) }
            is UiEvent.ToggleStrictMode -> onStateUpdate { it.copy(navigation = navigationUseCase.handleNavigationEvent(event, it)) }
            is UiEvent.SetReplayCursor -> {
                onStateUpdate { it.copy(navigation = it.navigation.copy(replayCursorTs = event.ts)) }
                onReplayRequest(event.ts)
            }

            // --- System & Hardware ---
            is UiEvent.RefreshPermissionStatus -> {
                scope.launch(Dispatchers.IO) {
                    val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                    withContext(Dispatchers.Main.immediate) {
                        onStateUpdate { it.copy(session = it.session.copy(permissions = newState)) }
                    }
                }
            }
            is UiEvent.ToggleXiaomiManualOverride -> {
                scope.launch(Dispatchers.IO) {
                    val current = currentState.session.permissions.isManualOverride
                    repository.saveBoolean(IS_XIAOMI_MANUAL_OVERRIDE_KEY, !current)
                }
            }
            is UiEvent.RequestTestAlarm -> repository.sendCommand(UiCommand.ExecuteTestAlarm)

            else -> {}
        }
    }

    private fun handleInitiateMode(
        mode: String,
        state: MainUiState,
        scope: CoroutineScope,
        onStateUpdate: ((MainUiState) -> MainUiState) -> Unit,
        onKinematicUpdate: ((KinematicState) -> KinematicState) -> Unit,
        onDiagnosticUpdate: ((DiagnosticState) -> DiagnosticState) -> Unit,
        onUiEffect: (UiEffect) -> Unit
    ) {
        val perms = state.session.permissions
        val hasFine = perms.isFineLocationGranted
        val hasAudio = if (mode == "tracker") perms.isMicrophoneGranted else true
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.isPostNotificationsGranted else true
        val hasActivity = if (mode == "tracker" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.isActivityRecognitionGranted else true
        
        if (hasFine && hasAudio && hasNotification && hasActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !perms.isBackgroundLocationGranted) {
                onStateUpdate { it.copy(navigation = it.navigation.copy(pendingMode = mode)) }
                onUiEffect(UiEffect.ShowBackgroundDisclosure(mode))
            } else {
                handleProceedToMode(mode, state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, onUiEffect)
            }
        } else {
            val list = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (mode == "tracker") {
                list.add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) list.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) list.add(Manifest.permission.POST_NOTIFICATIONS)
            
            onStateUpdate { it.copy(navigation = it.navigation.copy(pendingMode = mode)) }
            onUiEffect(UiEffect.RequestPermissions(list, mode))
        }
    }

    private fun handleProceedToMode(
        mode: String,
        state: MainUiState,
        scope: CoroutineScope,
        onStateUpdate: ((MainUiState) -> MainUiState) -> Unit,
        onKinematicUpdate: ((KinematicState) -> KinematicState) -> Unit,
        onDiagnosticUpdate: ((DiagnosticState) -> DiagnosticState) -> Unit,
        onUiEffect: (UiEffect) -> Unit
    ) {
        // Procedure: Reset -> Save Mode -> Start Service -> Check Setup
        handleEvent(UiEvent.SetManualSelection(true), state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, {}, {}, onUiEffect)
        handleEvent(UiEvent.SetSettlingActive(false), state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, {}, {}, onUiEffect)
        handleEvent(UiEvent.SetAppMode(mode), state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, {}, {}, onUiEffect)
        handleEvent(UiEvent.SetSystemActive(true), state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, {}, {}, onUiEffect)

        val appStartRt = sessionManager.appStartRt
        val nowRt = timeProvider.elapsedRealtime
        val elapsed = nowRt - appStartRt
        
        if (elapsed < STARTUP_SETTLING_DELAY_MS && appStartRt > 0) {
            val remaining = STARTUP_SETTLING_DELAY_MS - elapsed
            scope.launch {
                delay(remaining)
                onUiEffect(UiEffect.StartService(mode))
            }
        } else {
            onUiEffect(UiEffect.StartService(mode))
        }

        if (!state.isSystemReady) {
            handleEvent(UiEvent.TogglePhoneSetup(true), state, scope, onStateUpdate, onKinematicUpdate, onDiagnosticUpdate, {}, {}, onUiEffect)
        }
    }

    private fun updateDraft(
        scope: CoroutineScope, 
        onStateUpdate: ((MainUiState) -> MainUiState) -> Unit, 
        update: (DraftSettings) -> DraftSettings
    ) {
        onStateUpdate { it.copy(settings = it.settings.copy(draftSettings = update(it.settings.draftSettings))) }
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch(Dispatchers.IO) {
            delay(300L)
        }
    }

    private fun commitSettings(draft: DraftSettings, scope: CoroutineScope, onStateUpdate: ( (MainUiState) -> MainUiState ) -> Unit) {
        scope.launch(Dispatchers.IO) {
            settingsUseCase.saveDraftToRepo(draft)
            settingsUseCase.commitDraft()
            withContext(Dispatchers.Main.immediate) {
                onStateUpdate { it.copy(settings = it.settings.copy(draftSettings = DraftSettings())) }
            }
        }
    }

    private fun handleMapTap(point: GeoPoint, state: MainUiState, scope: CoroutineScope, onStateUpdate: ( (MainUiState) -> MainUiState ) -> Unit, onCameraAction: (CameraAction) -> Unit, onUiEffect: (UiEffect) -> Unit) {
        val mode = state.spatial.geofenceMode
        if (mode == GeofenceMode.ADD) {
            handleEvent(UiEvent.AddHomePoint(point), state, scope, onStateUpdate, {}, {}, {}, onCameraAction, onUiEffect)
        } else if (mode == GeofenceMode.REMOVE) {
            val idx = spatialLogicUseCase.findNearestPointIndex(state.spatial.homePoints, point)
            if (idx != -1) handleEvent(UiEvent.RemoveHomePoint(idx), state, scope, onStateUpdate, {}, {}, {}, onCameraAction, onUiEffect)
        }
    }
}
