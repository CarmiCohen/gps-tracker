package com.gps19.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps19.core.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * MainViewModel: Orchestrates top-level application state and global navigation.
 * Sep.23.04:
 * - Shared Overlay Scope: Centralized shared overlays (Settings, Log, Ribbons, 
 *   GNSS Detail) by exposing global flows and handling action events (R-ID 419).
 * Sep.23.03:
 * - Issue #1192 RESOLVED: Unified draft settings state flow. Moved draft handling 
 *   logic from feature ViewModels to MainViewModel to ensure visual consistency 
 *   during configuration updates (R-ID 419).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository,
    private val systemStatusProvider: SystemStatusProvider,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val spatialLogicUseCase: SpatialLogicUseCase,
    private val stateSubscriptionUseCase: StateSubscriptionUseCase,
    private val sessionUseCase: SessionUseCase,
    val timeProvider: TimeProvider,
    val audioSynthesizer: AudioSynthesizer,
    private val hydrationManager: LifecycleHydrationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "MainViewModel Coroutine Exception")
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val sessionUiState: StateFlow<SessionUiState> = _uiState
        .map { it.session }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState())

    val settingsUiState: StateFlow<SettingsUiState> = _uiState
        .map { it.settings }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val spatialUiState: StateFlow<SpatialUiState> = _uiState
        .map { it.spatial }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpatialUiState())

    val navigationState: StateFlow<NavigationState> = _uiState
        .map { it.navigation }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationState())

    val simulationUiState: StateFlow<SimulationUiState> = _uiState
        .map { it.simulation }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SimulationUiState())

    private val _kinematicState = MutableStateFlow(KinematicState())
    val kinematicState: StateFlow<KinematicState> = _kinematicState.asStateFlow()

    private val _diagnosticState = MutableStateFlow(DiagnosticState())
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState.asStateFlow()

    val eventLogsFlow: StateFlow<List<LogEntry>> = combine(
        _uiState.map { it.session.appMode }.distinctUntilChanged(),
        _uiState.map { it.navigation.isStrictMode }.distinctUntilChanged(),
        _uiState.map { it.navigation.isLogVisible }.distinctUntilChanged()
    ) { mode, strict, visible -> Triple(mode ?: "tracker", strict, visible) }
        .flatMapLatest { (_, strict, visible) -> 
            if (visible) repository.eventLogsFlow(if (strict) LOG_LIMIT_STRICT else LOG_LIMIT_STANDARD) 
            else flowOf(emptyList()) 
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGnssDetail: StateFlow<GnssDetail?> = repository.gnssDetail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history4MFlow = repository.getHistoryFlow("4M").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history16MFlow = repository.getHistoryFlow("16M").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history1HFlow = repository.getHistoryFlow("1H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history4HFlow = repository.getHistoryFlow("4H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history24HFlow = repository.getHistoryFlow("24H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history7DFlow = repository.getHistoryFlow("7D").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerTrailFlow: StateFlow<List<TrailPoint>> = repository.trackerTrailFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val replayCursorRequest = MutableStateFlow<Long?>(null)
    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default + uiExceptionHandler) {
            val initialSettings = settingsUseCase.loadAllSettings()
            val initialPerms = systemStatusProvider.getPermissionState()
            
            withContext(Dispatchers.Main.immediate) {
                applyInitialSettings(initialSettings)
                
                hydrationManager.hydrationLevel.onEach { level ->
                    updateState { it.copy(session = it.session.copy(hydrationLevel = level)) }
                }.launchIn(viewModelScope)

                hydrationManager.startHydration(viewModelScope, initialPerms.performanceTier == PerformanceTier.STAGGERED) {
                    updateState { it.copy(session = it.session.copy(isInitialized = true)) }
                }
            }
            
            launch(Dispatchers.IO) { 
                delay(STAGGERED_IO_PRUNING_DELAY_MS + 1000)
                repository.proactivePruning() 
            }
            
            withContext(Dispatchers.Main.immediate) {
                startBaseObservations()
                startGlobalTimer()
            }

            launch(Dispatchers.Default) {
                replayCursorRequest.collectLatest { ts ->
                    if (ts == null) {
                        updateKinematicState { it.apply { replayCursorPos = null } }
                        return@collectLatest
                    }
                    val trail = trackerTrailFlow.value
                    stateSubscriptionUseCase.findClosestTrailPoint(trail, ts)?.let { bp ->
                        updateKinematicState { it.apply { 
                            replayCursorPos = bp.toGeoPoint() 
                            pulse = timeProvider.elapsedRealtime()
                        }}
                    }
                }
            }
        }
    }

    private fun startBaseObservations() {
        stateSubscriptionUseCase.observeRepositorySettings()
            .onEach { update ->
                updateState { it.copy(
                    settings = it.settings.copy(
                        deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl,
                        lastAlarmAckTs = update.lastAlarmAckTs, isIdentitySanitized = update.identitySanitized,
                        alertSettings = update.alertSettings
                    ),
                    spatial = it.spatial.copy(
                        maxDistance = update.maxDistance, homePoints = update.homePoints
                    ),
                    session = it.session.copy(
                        appMode = update.appMode, isSystemActive = update.isSystemActive,
                        permissions = it.permissions.copy(isManualOverride = update.isXiaomiManualOverride)
                    )
                )}
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeInternetStatus()
            .onEach { online -> 
                updateDiagnosticState { current ->
                    current.apply {
                        connectivity.isLocalOnline = online
                        pulse = timeProvider.elapsedRealtime()
                    }
                }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeIntegrityUpdates()
            .onEach { update ->
                updateDiagnosticState { current -> 
                    current.activeAlarms = update.activeAlarms
                    current.pulse = timeProvider.elapsedRealtime()
                    current
                }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        audioSynthesizer.isSirenPlaying
            .onEach { playing ->
                updateDiagnosticState { it.apply { isSirenPlaying = playing } }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)
        
        viewModelScope.launch(Dispatchers.IO) { 
            while(true) { 
                val refreshFast = _uiState.value.navigation.isPhoneSetupVisible || _uiState.value.navigation.isDiagnosticsVisible
                val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                withContext(Dispatchers.Main.immediate) { 
                    updateState { it.copy(session = it.session.copy(permissions = newState)) } 
                }
                delay(if (refreshFast) 5000L else 30000L) 
            } 
        }
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ToggleMap, is UiEvent.ToggleLog, is UiEvent.ToggleSettings, 
            is UiEvent.TogglePhoneSetup, is UiEvent.ToggleRibbons, is UiEvent.SetDashboardExpanded,
            is UiEvent.ToggleGnssDetail, is UiEvent.SetSubSettings, is UiEvent.ShowStopTrackingConfirmation,
            is UiEvent.NavigateToDiagnostics, is UiEvent.SetPendingMode -> {
                if (event is UiEvent.ToggleSettings) {
                    if (event.visible) updateState { it.copy(settings = it.settings.copy(draftSettings = settingsUseCase.prepareDraft(it))) }
                    else commitDraft()
                }
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && _uiState.value.navigation.isSettingsOpen) commitDraft()
            }
            is UiEvent.SetSystemActive -> { 
                updateState { it.copy(session = it.session.copy(isSystemActive = event.active)) } 
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { sessionUseCase.setSystemActive(event.active) }
            }
            is UiEvent.SetAppMode -> {
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    val newStartTime = sessionUseCase.setAppMode(event.mode)
                    updateState { it.copy(
                        session = it.session.copy(
                            appMode = event.mode, 
                            appStartTime = newStartTime ?: it.session.appStartTime,
                            isSystemActive = if (event.mode != null) true else it.session.isSystemActive
                        )
                    )}
                }
            }
            is UiEvent.ConfirmStopTracking, UiEvent.ManualExit -> {
                updateState { it.copy(
                    session = it.session.copy(isSystemActive = false, appMode = null),
                    settings = it.settings.copy(isSafeMode = false)
                ) }
                repository.setSafeMode(false)
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    sessionUseCase.stopTrackingSession()
                }
            }
            is UiEvent.TriggerRecovery -> {
                if (_uiState.value.isRecoveryPending) {
                    updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
                    updateState { it.copy(simulation = it.simulation.copy(isRecoveryPending = false)) }
                }
            }
            is UiEvent.SetRecoveryPending -> updateState { it.copy(simulation = it.simulation.copy(isRecoveryPending = event.pending)) }
            is UiEvent.SetForensicSimulation -> {
                updateState { it.copy(simulation = it.simulation.copy(isForensicStallSimulated = event.active)) }
                repository.setForensicStallSimulation(event.active)
            }
            is UiEvent.SetStorageSimulation -> {
                updateState { it.copy(simulation = it.simulation.copy(isStorageSimulated = event.active, isStorageCriticalSimulated = event.isCritical)) }
                repository.sendCommand(UiCommand.SimulateStoragePressure(event.active, event.isCritical))
            }
            is UiEvent.SetManualSelection -> updateState { it.copy(spatial = it.spatial.copy(isManualSelectionInProgress = event.active)) }
            is UiEvent.SetSettlingActive -> updateState { it.copy(session = it.session.copy(isSettlingActive = event.active)) }
            is UiEvent.ToggleSetupBypass -> updateState { it.copy(session = it.session.copy(isSetupBypassActive = event.active)) }
            is UiEvent.DismissIdentitySanitization -> {
                updateState { it.copy(settings = it.settings.copy(isIdentitySanitized = false)) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    repository.saveBoolean(IDENTITY_SANITIZED_KEY, false)
                }
            }
            is UiEvent.DismissAlarms -> {
                updateDiagnosticState { it.apply { isRedScreenVisible = false } }
            }
            is UiEvent.SetRedScreenVisible -> {
                updateDiagnosticState { it.apply { isRedScreenVisible = event.visible } }
            }
            is UiEvent.StopSiren -> {
                repository.sendCommand(UiCommand.StopSiren(event.causes))
            }
            is UiEvent.ToggleStrictMode -> {
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.RefreshPermissionStatus -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                    withContext(Dispatchers.Main.immediate) {
                        updateState { it.copy(session = it.session.copy(permissions = newState)) }
                    }
                }
            }
            is UiEvent.ToggleXiaomiManualOverride -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val current = _uiState.value.session.permissions.isManualOverride
                    repository.saveBoolean(IS_XIAOMI_MANUAL_OVERRIDE_KEY, !current)
                }
            }
            is UiEvent.RequestTestAlarm -> {
                repository.sendCommand(UiCommand.ExecuteTestAlarm)
            }
            is UiEvent.UpdateDraftDeviceId, is UiEvent.UpdateDraftViewerId, is UiEvent.UpdateDraftRelayUrl, 
            is UiEvent.UpdateDraftMaxDistance, is UiEvent.UpdateDraftAlertSettings, is UiEvent.UpdateDraftAlarmVolume, 
            is UiEvent.CommitSettings -> handleDraftEvent(event)
            is UiEvent.BulkUpdateSettings -> {
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
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
            is UiEvent.LogAction -> {
                repository.addLog(
                    LogEntry(
                        localId = UUID.randomUUID().toString(),
                        timestamp = timeProvider.currentTimeMillis(),
                        message = event.message,
                        type = event.type.uppercase(),
                        isImportant = event.isImportant,
                        id = _uiState.value.settings.deviceId,
                        viewerId = _uiState.value.settings.viewerId,
                        isSpecial = event.isSpecial,
                        specialColor = event.specialColor
                    )
                )
            }
            is UiEvent.ClearLogs -> repository.clearLogs()
            is UiEvent.ClearHomePoints -> viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                val newPoints = spatialLogicUseCase.clearHomePoints(_uiState.value.maxDistance)
                withContext(Dispatchers.Main.immediate) {
                    updateState { it.copy(spatial = it.spatial.copy(homePoints = newPoints)) }
                }
            }
            is UiEvent.ResetStats -> viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                repository.resetStats()
            }
            is UiEvent.SetLogFilterShowDetails -> repository.updateLogFilters(details = event.show)
            is UiEvent.SetLogFilterShowRecovered -> repository.updateLogFilters(recovered = event.show)
            is UiEvent.SetReplayCursor -> {
                updateNavigation { it.copy(replayCursorTs = event.ts) }
                replayCursorRequest.value = event.ts
            }
            is UiEvent.ToggleTestSiren -> {
                if (_diagnosticState.value.isSirenPlaying) {
                    audioSynthesizer.stopSiren(timeProvider = timeProvider)
                } else {
                    val s = _uiState.value.settings.draftSettings.alertSettings
                    val volume = if (s.useMaxVolume) 1.0f else if (s.useCustomVolume) s.alarmVolume else 1.0f
                    audioSynthesizer.playSiren(
                        _uiState.value.settings.selectedSirenType, force = true, volume = volume, 
                        overrideSilence = s.overrideSilence, 
                        loop = true, vibrate = s.vibrationEnabled,
                        timeProvider = timeProvider
                    )
                }
            }
            else -> {}
        }
    }

    private fun handleDraftEvent(event: UiEvent) {
        when (event) {
            is UiEvent.UpdateDraftDeviceId -> updateDraft { it.copy(deviceId = event.id) }
            is UiEvent.UpdateDraftViewerId -> updateDraft { it.copy(viewerId = event.id) }
            is UiEvent.UpdateDraftRelayUrl -> updateDraft { it.copy(relayUrl = event.url) }
            is UiEvent.UpdateDraftMaxDistance -> updateDraft { it.copy(maxDistance = event.distance) }
            is UiEvent.UpdateDraftAlertSettings -> updateDraft { it.copy(alertSettings = event.settings) }
            is UiEvent.UpdateDraftAlarmVolume -> updateDraft { it.alertSettings.copy(alarmVolume = event.volume).let { s -> it.copy(alertSettings = s) } }
            is UiEvent.CommitSettings -> commitDraft()
            else -> {}
        }
    }

    private fun commitDraft() {
        val finalDraft = _uiState.value.settings.draftSettings
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            settingsUseCase.saveDraftToRepo(finalDraft)
            settingsUseCase.commitDraft()
            updateState { it.copy(settings = it.settings.copy(draftSettings = DraftSettings())) }
        }
    }

    private fun updateDraft(update: (DraftSettings) -> DraftSettings) {
        updateState { it.copy(settings = it.settings.copy(draftSettings = update(it.settings.draftSettings))) }
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { delay(300L); settingsUseCase.saveDraftToRepo(_uiState.value.settings.draftSettings) }
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
    private fun updateKinematicState(update: (KinematicState) -> KinematicState) { _kinematicState.update { current -> update(current) } }
    private fun updateDiagnosticState(update: (DiagnosticState) -> DiagnosticState) { _diagnosticState.update { current -> update(current) } }
    private fun updateNavigation(update: (NavigationState) -> NavigationState) { updateState { it.copy(navigation = update(it.navigation)) } }

    private fun startGlobalTimer() {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            while (true) {
                val nowRt = timeProvider.elapsedRealtime()

                if (_uiState.value.isInitialized && _uiState.value.appMode != null) {
                    repository.sendCommand(UiCommand.SyncRequest)
                }
                
                val lastActivity = repository.lastRemoteActivityTs.value
                val isPeerActive = lastActivity > 0 && (nowRt - lastActivity) < TELEMETRY_UI_STALE_THRESHOLD_MS
                if (_uiState.value.isPeerActive != isPeerActive) {
                    updateState { it.copy(session = it.session.copy(isPeerActive = isPeerActive)) }
                }

                delay(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 2000L)
            }
        }
    }

    private fun applyInitialSettings(initial: InitialSettings) {
        updateState { it.copy(
            settings = it.settings.copy(
                deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl,
                isIdentitySanitized = initial.identitySanitized, alertSettings = initial.alertSettings,
                draftSettings = initial.draftSettings ?: it.settings.draftSettings
            ),
            session = it.session.copy(
                appMode = initial.appMode, isSystemActive = initial.isSystemActive,
                appStartTime = initial.appStartTime
            )
        )}
    }

    fun fullInitialization(context: Context) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val nextStartTime = settingsUseCase.fullInitialization(context)
            updateState { it.copy(session = it.session.copy(appStartTime = nextStartTime)) }
        }
    }
}
