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
 * Sep.22.40:
 * - Issue #1170 RESOLVED: Decomposed monolithic ViewModel. Moved feature-specific 
 *   logic to TrackerViewModel, ViewerViewModel, and SetupViewModel. MainViewModel 
 *   now acts as a lightweight coordinator for app-level state and shared overlays (R-ID 408).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository,
    private val systemStatusProvider: SystemStatusProvider,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
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
        }
    }

    private fun startBaseObservations() {
        stateSubscriptionUseCase.observeRepositorySettings()
            .onEach { update ->
                updateState { it.copy(
                    settings = it.settings.copy(
                        deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl,
                        lastAlarmAckTs = update.lastAlarmAckTs, isIdentitySanitized = update.identitySanitized
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
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
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
                    updateNavigation { it.copy(serviceRecoveryTrigger = it.serviceRecoveryTrigger + 1) }
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
                updateNavigation { it.copy(isStrictMode = event.visible) }
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
            else -> {}
        }
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
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
                isIdentitySanitized = initial.identitySanitized
            ),
            session = it.session.copy(
                appMode = initial.appMode, isSystemActive = initial.isSystemActive,
                appStartTime = initial.appStartTime
            )
        )}
    }
}
