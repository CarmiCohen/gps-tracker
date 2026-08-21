package com.gps19.app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps19.core.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * MainViewModel: Manages UI state and orchestrates data flow.
 * Aug.20.09:
 * - Issue #226: HUD State Centralization. Added hudState StateFlow to 
 *   consolidate telemetry for status badges and ribbons (R226). Fixed 
 *   lambda parameter inference for Samsung A15 compiler stability.
 * - Issue #239: Restored addPersistentLog, clearTrails, and fullInitialization.
 * - Issue #241: Fixed combine lambda argument count mismatch for dashboardState/hudState.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository,
    private val logManager: LogManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val homePointUseCase: HomePointUseCase,
    private val dashboardStateProvider: DashboardStateProvider,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val telemetryUseCase: TelemetryUseCase,
    private val stateSubscriptionUseCase: StateSubscriptionUseCase,
    private val sessionUseCase: SessionUseCase,
    private val behaviorUseCase: BehaviorUseCase,
    private val alertUseCase: AlertUseCase,
    private val mapUseCase: MapUseCase,
    val timeProvider: TimeProvider,
    private val remoteStatusRepository: RemoteStatusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "ViewModel Coroutine Exception")
        addPersistentLog(type = "error", message = "UI ERROR: ${throwable.localizedMessage}", isImportant = true)
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _kinematicState = MutableStateFlow(KinematicState())
    val kinematicState: StateFlow<KinematicState> = _kinematicState.asStateFlow()

    private val _diagnosticState = MutableStateFlow(DiagnosticState())
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState.asStateFlow()

    private val _systemPulse = MutableStateFlow(timeProvider.currentTimeMillis())
    val systemPulse: StateFlow<Long> = _systemPulse.asStateFlow()

    private val _systemPulseRt = MutableStateFlow(timeProvider.elapsedRealtime())
    val systemPulseRt: StateFlow<Long> = _systemPulseRt.asStateFlow()

    private val _rtt = MutableStateFlow(0)
    val rtt: StateFlow<Int> = _rtt.asStateFlow()

    private val _remoteSignal = MutableStateFlow(0)
    val remoteSignal: StateFlow<Int> = _remoteSignal.asStateFlow()

    private val _currentMa = MutableStateFlow(0)
    val currentMa: StateFlow<Int> = _currentMa.asStateFlow()

    private val _trackerCurrentMa = MutableStateFlow(0)
    val trackerCurrentMa: StateFlow<Int> = _trackerCurrentMa.asStateFlow()

    private val _gpsIndexData = MutableStateFlow(GpsIndexData(0.0, 0.0, 0.0, 0.0))
    val gpsIndexData: StateFlow<GpsIndexData> = _gpsIndexData.asStateFlow()

    private val _gnssDetail = MutableStateFlow<GnssDetail?>(null)
    val gnssDetail: StateFlow<GnssDetail?> = _gnssDetail.asStateFlow()

    private val _trackerState = MutableStateFlow(TrackerState.UNKNOWN)
    val trackerState: StateFlow<TrackerState> = _trackerState.asStateFlow()

    private val _localMaxTemp = MutableStateFlow(0.0)
    val localMaxTemp: StateFlow<Double> = _localMaxTemp.asStateFlow()

    private val _trackerMaxTemp = MutableStateFlow(0.0)
    val trackerMaxTemp: StateFlow<Double> = _trackerMaxTemp.asStateFlow()

    val history4MFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("4M")
    val history16MFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("16M")
    val history1HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("1H")
    val history4HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("4H")
    val history24HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("24H")
    val history7DFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("7D")

    val activeGnssDetail: StateFlow<GnssDetail?> = combine(_uiState, _kinematicState, _gnssDetail) { ui, kin, localDetail ->
        if (ui.appMode == "viewer") kin.trackerLocation.gnssDetail else localDetail
    }
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboardState: StateFlow<DashboardState> = combine(
        _uiState,
        _kinematicState,
        _diagnosticState,
        _systemPulse,
        _trackerState,
        _localMaxTemp,
        _trackerMaxTemp
    ) { args ->
        val ui = args[0] as MainUiState
        val kin = args[1] as KinematicState
        val diag = args[2] as DiagnosticState
        val pulse = args[3] as Long
        val trkState = args[4] as TrackerState
        val lMax = args[5] as Double
        val tMax = args[6] as Double
        dashboardStateProvider.buildDashboardState(ui.appMode, kin, diag, pulse, trkState, lMax, tMax)
    }
    .flowOn(Dispatchers.Default)
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L) 
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    val hudState: StateFlow<HudState> = combine(
        _uiState,
        _kinematicState,
        _diagnosticState,
        _systemPulse,
        _trackerState,
        _rtt,
        _remoteSignal
    ) { args ->
        val ui = args[0] as MainUiState
        val kin = args[1] as KinematicState
        val diag = args[2] as DiagnosticState
        val pulse = args[3] as Long
        val trkState = args[4] as TrackerState
        val rttVal = args[5] as Int
        val sig = args[6] as Int
        dashboardStateProvider.buildHudState(ui, kin, diag, pulse, trkState, rttVal, sig)
    }
    .flowOn(Dispatchers.Default)
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState())

    val eventLogsFlow: StateFlow<List<LogEntry>> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _uiState.map { it.navigation.isStrictMode }.distinctUntilChanged(),
        _uiState.map { it.navigation.isLogVisible }.distinctUntilChanged()
    ) { mode, isStrict, isVisible -> Triple(mode, isStrict, isVisible) }
    .flatMapLatest { (mode, isStrict, isVisible) -> 
        if (mode != null && isVisible) {
            val limit = if (isStrict) LOG_LIMIT_STRICT else LOG_LIMIT_STANDARD
            repository.eventLogsFlow(limit)
        } else flowOf(emptyList()) 
    }
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerTrailFlow: StateFlow<List<TrailPoint>> = _uiState.map { it.appMode }.distinctUntilChanged()
        .flatMapLatest { mode -> if (mode != null) repository.trackerTrailFlow else flowOf(emptyList()) }
        .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailFlow: StateFlow<List<TrailPoint>> = _uiState.map { it.appMode }.distinctUntilChanged()
        .flatMapLatest { mode -> if (mode != null) repository.viewerTrailFlow else flowOf(emptyList()) }
        .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastTrackerTrailSize = -1
    private var cachedTrackerSegments = emptyList<MapTrailSegment>()
    private var lastViewerTrailSize = -1
    private var cachedViewerSegments = emptyList<MapTrailSegment>()

    val trackerTrailSegments: StateFlow<List<MapTrailSegment>> = trackerTrailFlow
        .map { trail -> 
            if (trail.size == lastTrackerTrailSize) cachedTrackerSegments
            else computeTrailSegments(trail, BrandJd.toArgb()).also { 
                cachedTrackerSegments = it; lastTrackerTrailSize = trail.size 
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailSegments: StateFlow<List<MapTrailSegment>> = viewerTrailFlow
        .map { trail -> 
            if (trail.size == lastViewerTrailSize) cachedViewerSegments
            else computeTrailSegments(trail, ViewerCyan.toArgb()).also { 
                cachedViewerSegments = it; lastViewerTrailSize = trail.size 
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val violationPointsFlow: Flow<List<ViolationPoint>> = _uiState.map { it.appMode }.distinctUntilChanged()
        .flatMapLatest { mode -> if (mode != null) repository.violationsFlow else flowOf(emptyList()) }
        .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)

    var appStartTime: Long = 0L
    private var autoSaveJob: Job? = null
    private var lastKnownAlarmTypes: Set<String> = emptySet()

    private var lastAlarmAckRt: Long = 0L
    private var isHeavyObservationStarted = false

    private val replayCursorRequest = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch(Dispatchers.Default + uiExceptionHandler) {
            val initialSettings = settingsUseCase.loadAllSettings()
            
            withContext(Dispatchers.Main.immediate) {
                applyInitialSettings(initialSettings)
                updateState { it.copy(hydrationLevel = 1) }
                delay(150) 
                updateState { it.copy(hydrationLevel = 2) }
                delay(300) 
                updateState { it.copy(hydrationLevel = 3, isInitialized = true) }
            }
            
            launch(Dispatchers.IO) { 
                delay(15000)
                repository.proactivePruning() 
            }
            
            withContext(Dispatchers.Main.immediate) {
                startBaseObservations()
                startGlobalTimer()
            }
            
            launch(Dispatchers.Main.immediate) {
                _uiState.filter { it.appMode != null }.first()
                startHeavyObservations()
            }

            launch(Dispatchers.Default) {
                replayCursorRequest.collectLatest { ts ->
                    if (ts == null) {
                        withContext(Dispatchers.Main.immediate) {
                            updateKinematicState { it.apply { replayCursorPos = null } }
                        }
                        return@collectLatest
                    }
                    val mode = _uiState.value.appMode
                    val trail = if (mode == "viewer") trackerTrailFlow.value else viewerTrailFlow.value
                    val bestPoint = stateSubscriptionUseCase.findClosestTrailPoint(trail, ts)
                    
                    bestPoint?.let { bp ->
                        withContext(Dispatchers.Main.immediate) {
                            updateKinematicState { it.apply { 
                                replayCursorPos = bp.toGeoPoint() 
                                pulse = timeProvider.elapsedRealtime()
                            }}
                        }
                    }
                }
            }
        }
    }

    private fun startBaseObservations() {
        stateSubscriptionUseCase.observeRepositorySettings()
            .onEach { update ->
                updateState { it.copy(
                    deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl,
                    maxDistance = update.maxDistance, homePoints = update.homePoints, lastAlarmAckTs = update.lastAlarmAckTs,
                    appMode = update.appMode, isSystemActive = update.isSystemActive,
                    permissions = it.permissions.copy(isManualOverride = update.isXiaomiManualOverride)
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
        
        viewModelScope.launch(Dispatchers.IO) { 
            while(true) { 
                val refreshFast = _uiState.value.navigation.isPhoneSetupVisible || _uiState.value.navigation.isDiagnosticsVisible
                val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                val isA15 = systemStatusProvider.isA15Hardware()
                withContext(Dispatchers.Main.immediate) { 
                    val oldState = _uiState.value
                    updateState { it.copy(permissions = newState.copy(isA15Device = isA15)) } 
                    
                    if (isA15 && !newState.isBatteryWhitelisted && !oldState.navigation.isPhoneSetupVisible && oldState.isInitialized) {
                        Timber.i("R405: Samsung A15 detected without battery exemption (Monitoring). Prompting user.")
                        onEvent(UiEvent.TogglePhoneSetup(true))
                    }
                }
                delay(if (refreshFast) 5000L else 30000L) 
            } 
        }

        repository.identitySanitizedFlow
            .onEach { sanitized -> updateState { it.copy(isIdentitySanitized = sanitized) } }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        repository.isRecoveryPendingFlow
            .onEach { pending -> updateState { it.copy(isRecoveryPending = pending) } }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)
    }

    private fun startHeavyObservations() {
        if (isHeavyObservationStarted) return
        isHeavyObservationStarted = true
        
        stateSubscriptionUseCase.observeConnectivityBasics()
            .onEach { update ->
                _rtt.value = update.lastRtt
                updateDiagnosticState { current -> 
                    current.apply {
                        connectivity.isRelayConnected = update.isRelayConnected
                        connectivity.lastRemoteActivityTs = update.lastRemoteActivityTs
                        cumulativeRecoveryBlackoutMs = update.cumulativeRecoveryBlackoutMs
                        recoveryCount = update.recoveryCount
                        pulse = timeProvider.elapsedRealtime()
                    }
                }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeIntegrityUpdates()
            .onEach { update ->
                updateKinematicState { current ->
                    if (_uiState.value.appMode == "tracker") {
                        current.trackerHealth.copyFrom(update.health)
                    }
                    current.localHealth.copyFrom(update.health)
                    current.apply { pulse = timeProvider.elapsedRealtime() }
                }
                updateDiagnosticState { current -> 
                    val isNewViolation = update.activeAlarmTypes.any { it !in lastKnownAlarmTypes }
                    
                    if (_uiState.value.appMode == "tracker") {
                        current.trackerBattery.level = update.batteryLevel
                        current.trackerBattery.temp = update.batteryTemp
                        current.trackerBattery.isCharging = update.isCharging
                        current.trackerBattery.isChargingStable = update.isCharging
                    }
                    
                    current.connectivity.isLocalOnline = update.isLocalOnline
                    current.activeAlarms = update.activeAlarms
                    current.isNewViolationDetected = isNewViolation
                    current.pulse = timeProvider.elapsedRealtime()
                    
                    val shouldShowRedScreen = behaviorUseCase.shouldShowRedScreenDecomposed(
                        _uiState.value, _kinematicState.value, current, timeProvider.elapsedRealtime(), lastAlarmAckRt, current.isRedScreenVisible
                    )
                    current.isRedScreenVisible = shouldShowRedScreen
                    current
                }
                lastKnownAlarmTypes = update.activeAlarmTypes
                _localMaxTemp.value = update.maxTemp
                if (_uiState.value.appMode == "tracker") _trackerMaxTemp.value = update.maxTemp
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeBatteryStatus().onEach { status -> 
            updateDiagnosticState { current -> 
                current.battery.level = status.level
                current.battery.temp = status.temp
                current.battery.isCharging = status.isCharging
                current.battery.isChargingStable = status.isCharging
                
                if (_uiState.value.appMode == "tracker") {
                    current.trackerBattery.level = status.level
                    current.trackerBattery.temp = status.temp
                    current.trackerBattery.isCharging = status.isCharging
                    current.trackerBattery.isChargingStable = status.isCharging
                }
                current.apply { pulse = timeProvider.elapsedRealtime() }
            } 
            _currentMa.value = status.currentMa
        }
        .flowOn(Dispatchers.Main.immediate)
        .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeGnssDetail().onEach { _gnssDetail.value = it }.flowOn(Dispatchers.Main.immediate).launchIn(viewModelScope)
        stateSubscriptionUseCase.observeGpsIndex().onEach { _gpsIndexData.value = it }.flowOn(Dispatchers.Main.immediate).launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Main.immediate) { 
            repository.localLocation
                .sample(100L) 
                .collect { update -> update?.let { handleLocationUpdateInternal(it) } } 
        }
        viewModelScope.launch(Dispatchers.Main.immediate) { 
            repository.trackerLocation
                .sample(100L)
                .collect { update -> update?.let { handleLocationUpdateInternal(it) } } 
        }

        viewModelScope.launch(Dispatchers.Main.immediate) { repository.connectedViewers.collect { viewers -> 
            updateDiagnosticState { current ->
                current.apply {
                    connectivity.connectedViewers = viewers
                    pulse = timeProvider.elapsedRealtime()
                }
            } 
        } }

        remoteStatusRepository.remoteStatus.onEach { status ->
            if (_uiState.value.appMode == "viewer") {
                _remoteSignal.value = remoteStatusRepository.peerSignal.value
                _trackerCurrentMa.value = status.currentMa
                _trackerState.value = status.trackerState
                _trackerMaxTemp.value = status.maxTemp
                
                updateKinematicState { current ->
                    telemetryUseCase.mapTrackerLocationFromStatus(status, current.trackerLocation)
                    telemetryUseCase.mapHealthFromStatus(status, current.trackerHealth)
                    current.apply { pulse = timeProvider.elapsedRealtime() }
                }
                updateDiagnosticState { current ->
                    telemetryUseCase.mapStatsFromStatus(status, current.trackerStats)
                    current.trackerBattery.level = status.battery
                    current.trackerBattery.temp = status.temp
                    current.trackerBattery.isCharging = status.isCharging
                    current.trackerBattery.isChargingStable = status.isCharging
                    
                    current.connectivity.isTrackerConnected = remoteStatusRepository.isTrackerConnected.value
                    current.connectivity.lastUpdateTs = status.ts
                    current.connectivity.lastRemoteActivityTs = remoteStatusRepository.lastPeerActivityTs.value
                    
                    current.trackerSatsView = status.satsView
                    current.trackerSatsUsed = status.satsUsed
                    current.maxTrackerAccuracy = if (status.maxAccuracy > 0.0) status.maxAccuracy else current.maxTrackerAccuracy
                    current.pulse = timeProvider.elapsedRealtime()
                    current
                }
            }
        }
        .flowOn(Dispatchers.Main.immediate)
        .launchIn(viewModelScope)

        stateSubscriptionUseCase.startHistoryObservations(viewModelScope)
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ToggleMap, is UiEvent.ToggleLog, is UiEvent.ToggleSettings, 
            is UiEvent.TogglePhoneSetup, is UiEvent.ToggleRibbons, is UiEvent.SetDashboardExpanded,
            is UiEvent.ToggleGnssDetail, is UiEvent.SetSubSettings, is UiEvent.ShowStopTrackingConfirmation,
            is UiEvent.NavigateToDiagnostics -> {
                if (event is UiEvent.ToggleSettings) {
                    if (event.visible) updateState { it.copy(draftSettings = settingsUseCase.prepareDraft(it)) }
                    else commitDraft()
                }
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.ToggleStrictMode -> {
                updateNavigation { it.copy(isStrictMode = event.visible) }
                addPersistentLog("user", "USER ACTION: Forensic Strict Mode ${if (event.visible) "ENABLED" else "DISABLED"}", isImportant = true)
            }
            is UiEvent.SetReplayCursor -> handleReplayCursor(event.ts)
            is UiEvent.SetPendingMode -> updateNavigation { it.copy(pendingMode = event.mode) }
            is UiEvent.SetRedScreenVisible -> updateDiagnosticState { it.apply { isRedScreenVisible = event.visible; pulse = timeProvider.elapsedRealtime() } }
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && _uiState.value.navigation.isSettingsOpen) commitDraft()
            }
            is UiEvent.DismissAlarms, is UiEvent.StopSiren -> handleAlarmEvent(event)
            is UiEvent.SetAppMode, is UiEvent.ConfirmStopTracking -> handleSystemEvent(event)
            is UiEvent.SetSystemActive -> { 
                addPersistentLog("user", "USER ACTION: System ${if (event.active) "ACTIVATED" else "DEACTIVATED"}", isImportant = true)
                updateState { it.copy(isSystemActive = event.active) } 
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    sessionUseCase.setSystemActive(event.active)
                }
            }
            is UiEvent.ManualExit -> addPersistentLog("user", "USER ACTION: Manual navigation to background requested", isImportant = true)
            is UiEvent.SetDeviceId, is UiEvent.SetViewerId, is UiEvent.SetRelayUrl -> handleConfigEvent(event)
            is UiEvent.ResetStats, is UiEvent.ClearLogs -> handleLogAndStatsEvent(event)
            is UiEvent.LogAction -> addPersistentLog(event.type, event.message, event.isImportant, event.isSpecial, event.specialColor)
            is UiEvent.ClearTrails -> clearTrails(context)
            is UiEvent.SetFenceVisible, is UiEvent.SetViolationsVisible, is UiEvent.SetGeofenceViolationsVisible, 
            is UiEvent.SetMapButtonsVisible, is UiEvent.SetMapLocked, is UiEvent.MapZoomIn, is UiEvent.MapZoomOut,
            is UiEvent.CenterTracker, is UiEvent.CenterViewer -> updateState { mapUseCase.handleMapEvent(event, it) }
            is UiEvent.ClearHomePoints, is UiEvent.AddHomePoint, is UiEvent.RemoveHomePoint,
            is UiEvent.SetGeofenceMode, is UiEvent.SetMaxDistance, is UiEvent.SetHomePoints,
            is UiEvent.SaveHomePoints, is UiEvent.MapTap -> handleHomePointEvent(event)
            
            is UiEvent.SetJammerSuspicion -> updateKinematicState { current -> 
                current.localHealth.isJammer = event.isJammer
                current.apply { pulse = timeProvider.elapsedRealtime() }
            }
            is UiEvent.SetSignalLoss -> updateKinematicState { current -> 
                current.localHealth.signalLoss = event.isSignalLoss
                current.apply { pulse = timeProvider.elapsedRealtime() }
            }

            is UiEvent.SetAlertSettings -> { 
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    settingsUseCase.handleImmediateAlertUpdate(event.settings)
                    updateState { it.copy(alertSettings = event.settings, draftSettings = it.draftSettings.copy(alertSettings = event.settings)) }
                    addPersistentLog("user", "USER ACTION: Alert settings modified", isImportant = true)
                }
            }
            is UiEvent.SetSirenType -> { 
                addPersistentLog("user", "USER ACTION: Siren type set to ${event.type}", isImportant = true)
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) { repository.saveString(SELECTED_SIREN_KEY, event.type); updateState { it.copy(selectedSirenType = event.type) } } 
            }
            is UiEvent.UpdateDraftDeviceId, is UiEvent.UpdateDraftViewerId, is UiEvent.UpdateDraftRelayUrl, 
            is UiEvent.UpdateDraftMaxDistance, is UiEvent.UpdateDraftAlertSettings, is UiEvent.UpdateDraftAlarmVolume, 
            is UiEvent.CommitSettings -> handleDraftEvent(event)
            is UiEvent.SetLogFilterShowDetails -> viewModelScope.launch(Dispatchers.Main.immediate) { repository.updateLogFilters(details = event.show) }
            is UiEvent.SetLogFilterShowRecovered -> viewModelScope.launch(Dispatchers.Main.immediate) { repository.updateLogFilters(recovered = event.show) }
            is UiEvent.ToggleGnssDetail -> updateNavigation { it.copy(isGnssDetailVisible = event.visible) }
            is UiEvent.RefreshPermissionStatus -> viewModelScope.launch(Dispatchers.IO) { 
                repeat(2) { attempt ->
                    val oldState = _uiState.value.permissions
                    val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                    val isA15 = systemStatusProvider.isA15Hardware()
                    withContext(Dispatchers.Main.immediate) { 
                        val currentUi = _uiState.value
                        updateState { it.copy(permissions = newState.copy(isA15Device = isA15)) } 
                        
                        if (isA15 && !newState.isBatteryWhitelisted && !currentUi.navigation.isPhoneSetupVisible && currentUi.isInitialized) {
                            Timber.i("R405: Samsung A15 detected without battery exemption (Refresh). Prompting user.")
                            onEvent(UiEvent.TogglePhoneSetup(true))
                        }
                        
                        if (!oldState.isActivityRecognitionGranted && newState.isActivityRecognitionGranted) {
                            Timber.i("Issue #098: ACTIVITY_RECOGNITION granted. Triggering reactive sensor sync.")
                            repository.sendCommand(UiCommand.SettingsUpdated)
                        }
                    }
                    if (attempt == 0) delay(1200) 
                }
            }
            is UiEvent.RequestTestAlarm -> { addPersistentLog("user", "USER ACTION: Test alarm triggered", isImportant = true); repository.sendCommand(UiCommand.ExecuteTestAlarm) }
            is UiEvent.ToggleXiaomiManualOverride -> {
                val nextValue = !_uiState.value.permissions.isManualOverride
                updateState { it.copy(permissions = it.permissions.copy(isManualOverride = nextValue)) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { repository.saveBoolean(IS_XIAOMI_MANUAL_OVERRIDE_KEY, nextValue); addPersistentLog("user", "USER ACTION: Xiaomi manual override set to $nextValue", isImportant = true) }
            }
            is UiEvent.DismissIdentitySanitization -> {
                updateState { it.copy(isIdentitySanitized = false) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { repository.saveBoolean(IDENTITY_SANITIZED_KEY, false) }
            }
            is UiEvent.SetRecoveryPending -> {
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    repository.saveBoolean(IS_RECOVERY_PENDING_KEY, event.pending)
                    if (event.pending && repository.getLong(RECOVERY_BLOCKED_TS_KEY, 0L) == 0L) {
                        repository.saveLong(RECOVERY_BLOCKED_TS_KEY, timeProvider.currentTimeMillis())
                    }
                    withContext(Dispatchers.Main.immediate) {
                        updateState { it.copy(isRecoveryPending = event.pending) }
                    }
                }
            }
            is UiEvent.TriggerRecovery -> {
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    val appMode = _uiState.value.appMode
                    val isSystemActive = _uiState.value.isSystemActive
                    if (appMode != null && isSystemActive) {
                        val serviceClass = if (appMode == "tracker") TrackerService::class.java else ViewerService::class.java
                        val intent = Intent(context, serviceClass)
                        try {
                            val blockedTs = repository.getLong(RECOVERY_BLOCKED_TS_KEY, 0L)
                            ContextCompat.startForegroundService(context, intent)
                            
                            val now = timeProvider.currentTimeMillis()
                            repository.saveBoolean(IS_RECOVERY_PENDING_KEY, false)
                            repository.saveLong(RECOVERY_BLOCKED_TS_KEY, 0L)
                            updateState { it.copy(isRecoveryPending = false) }

                            if (blockedTs > 0) {
                                val latency = now - blockedTs
                                repository.incrementRecoveryStats(latency)
                                val snapshot = repository.getSettingsSnapshot()
                                val avg = if (snapshot.recoveryCount > 0) snapshot.cumulativeRecoveryBlackoutMs / snapshot.recoveryCount else 0L
                                addPersistentLog("system", "Forensic Performance Audit: Deferred service recovery blackout (${latency}ms) [Avg: ${avg}ms]", isImportant = true)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Issue #626: Deferred recovery failed")
                            addPersistentLog("error", "RECOVERY ERROR: ${e.localizedMessage}", isImportant = true)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun handleReplayCursor(ts: Long?) {
        updateNavigation { it.copy(replayCursorTs = ts) }
        replayCursorRequest.value = ts
    }

    private fun handleConfigEvent(event: UiEvent) {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            when (event) {
                is UiEvent.SetDeviceId -> { 
                    settingsUseCase.updateDeviceId(event.id)
                    updateState { it.copy(deviceId = event.id) }
                    updateKinematicState { it.apply {
                        localLocation.reset(); trackerLocation.reset()
                        localHealth.reset(); trackerHealth.reset()
                        pulse = timeProvider.elapsedRealtime()
                    }}
                    updateDiagnosticState { it.apply {
                        battery.level = 100; stats.uptimeMs = 0; trackerStats.uptimeMs = 0
                        pulse = timeProvider.elapsedRealtime()
                    }}
                    _trackerMaxTemp.value = 0.0; _trackerCurrentMa.value = 0; remoteStatusRepository.reset(); repository.resetStats(); repository.sendCommand(UiCommand.StatsReset); repository.sendCommand(UiCommand.SettingsUpdated)
                }
                is UiEvent.SetViewerId -> {
                    settingsUseCase.updateViewerId(event.id)
                    updateState { it.copy(viewerId = event.id) }; remoteStatusRepository.reset(); repository.resetStats(); repository.resetStats(); repository.sendCommand(UiCommand.StatsReset); repository.sendCommand(UiCommand.SettingsUpdated)
                }
                is UiEvent.SetRelayUrl -> {
                    settingsUseCase.updateRelayUrl(event.url)
                    updateState { it.copy(relayUrl = event.url) }; repository.sendCommand(UiCommand.SettingsUpdated)
                }
                else -> {}
            }
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
        val finalDraft = _uiState.value.draftSettings
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            settingsUseCase.saveDraftToRepo(finalDraft)
            val result = settingsUseCase.commitDraft()
            
            if (result.error != null) {
                withContext(Dispatchers.Main.immediate) {
                    Toast.makeText(context, "Commit Failed: ${result.error}", Toast.LENGTH_LONG).show()
                }
                addPersistentLog("error", "Settings Commit Failed: ${result.error}", isImportant = true)
                return@launch
            }

            if (result.anyChanged) {
                if (result.trackerIdChanged) addPersistentLog("user", "USER ACTION: Tracker ID changed", isImportant = true)
                if (result.viewerIdChanged) addPersistentLog("user", "USER ACTION: Viewer ID changed", isImportant = true)
                if (result.relayUrlChanged) addPersistentLog("user", "USER ACTION: Relay URL changed", isImportant = true)
                if (result.maxDistanceChanged) addPersistentLog("user", "USER ACTION: Geofence distance updated", isImportant = true)
                if (result.trackerIdChanged || result.viewerIdChanged) {
                    remoteStatusRepository.reset(); repository.resetStats(); repository.sendCommand(UiCommand.StatsReset)
                    _trackerMaxTemp.value = 0.0; _remoteSignal.value = 0; _trackerCurrentMa.value = 0; _gnssDetail.value = null; _trackerState.value = TrackerState.UNKNOWN
                    updateKinematicState { it.apply {
                        localLocation.reset(); trackerLocation.reset()
                        localHealth.reset(); trackerHealth.reset()
                        pulse = timeProvider.elapsedRealtime()
                    }}
                    updateDiagnosticState { it.apply {
                        battery.level = 100; stats.uptimeMs = 0; trackerStats.uptimeMs = 0
                        pulse = timeProvider.elapsedRealtime()
                    }}
                }
                repository.sendCommand(UiCommand.SettingsUpdated)
            }
            updateState { it.copy(draftSettings = DraftSettings()) }
        }
    }

    private fun updateDraft(update: (DraftSettings) -> DraftSettings) {
        updateState { it.copy(draftSettings = update(it.draftSettings)) }
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { delay(300L); settingsUseCase.saveDraftToRepo(_uiState.value.draftSettings) }
    }

    private fun handleAlarmEvent(event: UiEvent) {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            val nowRt = timeProvider.elapsedRealtime()
            val nowWall = when (event) {
                is UiEvent.DismissAlarms -> alertUseCase.dismissAlarms()
                is UiEvent.StopSiren -> alertUseCase.stopSiren(event.causes)
                else -> 0L
            }
            if (nowWall > 0) {
                lastAlarmAckRt = nowRt
                updateState { it.copy(lastAlarmAckTs = nowWall) }
                updateDiagnosticState { it.apply {
                    isAlarmSilenced = true
                    isRedScreenVisible = false
                    pulse = timeProvider.elapsedRealtime()
                }}
            }
        }
    }

    private fun handleSystemEvent(event: UiEvent) {
        when (event) {
            is UiEvent.SetAppMode -> { 
                addPersistentLog("user", "USER ACTION: App mode set to ${event.mode ?: "NONE"}", isImportant = true)
                if (event.mode != null) startHeavyObservations()
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    val newStartTime = sessionUseCase.setAppMode(event.mode)
                    updateState { it.copy(appMode = event.mode, appStartTime = newStartTime ?: it.appStartTime) }
                    if (newStartTime != null) appStartTime = newStartTime
                }
            }
            is UiEvent.ConfirmStopTracking -> {
                addPersistentLog("user", "User-initiated Session Termination", isImportant = true)
                updateNavigation { it.copy(isStopTrackingConfirmationVisible = false) }
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    sessionUseCase.stopTrackingSession()
                    remoteStatusRepository.reset()
                    updateState { it.copy(appMode = null, isSystemActive = false) }
                    updateKinematicState { it.apply {
                        localLocation.reset(); trackerLocation.reset()
                        localHealth.reset(); trackerHealth.reset()
                        pulse = timeProvider.elapsedRealtime()
                    }}
                    updateDiagnosticState { it.apply {
                        battery.level = 100; stats.uptimeMs = 0; trackerStats.uptimeMs = 0
                        pulse = timeProvider.elapsedRealtime()
                    }}
                    _remoteSignal.value = 0; _trackerCurrentMa.value = 0; _gnssDetail.value = null; _trackerState.value = TrackerState.UNKNOWN; _localMaxTemp.value = 0.0; _trackerMaxTemp.value = 0.0; _rtt.value = 0; _gpsIndexData.value = GpsIndexData(0.0, 0.0, 0.0, 0.0); stateSubscriptionUseCase.clearHistory()
                }
            }
            else -> {}
        }
    }

    private fun handleLogAndStatsEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ResetStats -> { 
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    val newStartTime = sessionUseCase.resetStats()
                    appStartTime = newStartTime
                    remoteStatusRepository.reset()
                    updateState { it.copy(appStartTime = appStartTime) }
                    addPersistentLog("user", "USER ACTION: Connectivity stats reset", isImportant = true); stateSubscriptionUseCase.clearHistory(); Toast.makeText(context, "Connectivity stats reset", Toast.LENGTH_SHORT).show()
                }
            }
            is UiEvent.ClearLogs -> { repository.clearLogs(); addPersistentLog("user", "USER ACTION: Event logs cleared", isImportant = true) }
            else -> {}
        }
    }

    private fun handleHomePointEvent(event: UiEvent) {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            when (event) {
                is UiEvent.ClearHomePoints -> { val newList = homePointUseCase.clearHomePoints(_uiState.value.maxDistance); updateState { it.copy(homePoints = newList, geofenceMode = GeofenceMode.IDLE) }; addPersistentLog("user", "USER ACTION: All home points cleared", isImportant = true) }
                is UiEvent.AddHomePoint -> { val newList = homePointUseCase.addHomePoint(_uiState.value.homePoints, event.point, _uiState.value.maxDistance); updateState { it.copy(homePoints = newList) }; addPersistentLog("user", String.format(Locale.getDefault(), "USER ACTION: Home point added at %.4f, %.4f", event.point.latitude, event.point.longitude), isImportant = true) }
                is UiEvent.RemoveHomePoint -> { val newList = homePointUseCase.removeHomePoint(_uiState.value.homePoints, event.index, _uiState.value.maxDistance); updateState { it.copy(homePoints = newList) }; addPersistentLog("user", "USER ACTION: Home point removed", isImportant = true) }
                is UiEvent.SetGeofenceMode -> updateState { it.copy(geofenceMode = if (it.geofenceMode == event.mode) GeofenceMode.IDLE else event.mode) }
                is UiEvent.MapTap -> {
                    val mode = _uiState.value.geofenceMode
                    if (mode == GeofenceMode.ADD) onEvent(UiEvent.AddHomePoint(event.point))
                    else if (mode == GeofenceMode.REMOVE) {
                        val nearestIdx = homePointUseCase.findNearestPointIndex(_uiState.value.homePoints, event.point)
                        if (nearestIdx != -1) onEvent(UiEvent.RemoveHomePoint(nearestIdx))
                    }
                }
                is UiEvent.SetMaxDistance -> { addPersistentLog("user", "USER ACTION: Geofence distance updated: ${event.distance.toInt()}m", isImportant = true); repository.saveHomePoints(_uiState.value.homePoints, event.distance); updateState { it.copy(maxDistance = event.distance) } }
                is UiEvent.SetHomePoints -> { updateState { it.copy(homePoints = event.points) }; repository.saveHomePoints(event.points, _uiState.value.maxDistance); addPersistentLog("user", "USER ACTION: Home points restored", isImportant = true) }
                is UiEvent.SaveHomePoints -> repository.saveHomePoints(_uiState.value.homePoints, _uiState.value.maxDistance)
                else -> {}
            }
        }
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
    private fun updateKinematicState(update: (KinematicState) -> KinematicState) { _kinematicState.update { current -> update(current) } }
    private fun updateDiagnosticState(update: (DiagnosticState) -> DiagnosticState) { _diagnosticState.update { current -> update(current) } }
    private fun updateNavigation(update: (NavigationState) -> NavigationState) { updateState { it.copy(navigation = update(it.navigation)) } }

    private fun startGlobalTimer() {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            while (true) {
                val stateSnapshot = _uiState.value
                if (stateSnapshot.isInitialized && stateSnapshot.appMode != null) {
                    val now = timeProvider.currentTimeMillis()
                    val nowRt = timeProvider.elapsedRealtime()
                    _systemPulse.value = now
                    _systemPulseRt.value = nowRt
                    updateDiagnosticState { state -> state.apply { isSirenPlaying = AudioSynthesizer.isPlaying(); pulse = nowRt } }
                    
                    repository.sendCommand(UiCommand.SyncRequest)
                    withContext(Dispatchers.Default) {
                        val currentUi = _uiState.value
                        val currentKin = _kinematicState.value
                        val currentDiag = _diagnosticState.value
                        val newState = behaviorUseCase.computeTrackerStateDecomposed(currentUi, currentKin, currentDiag, now)
                        val shouldShowRedScreen = behaviorUseCase.shouldShowRedScreenDecomposed(currentUi, currentKin, currentDiag, nowRt, lastAlarmAckRt, currentDiag.isRedScreenVisible)
                        
                        withContext(Dispatchers.Main.immediate) {
                            if (newState != _trackerState.value && newState != TrackerState.UNKNOWN) {
                                addPersistentLog("event", "Tracker is $newState", isImportant = true)
                            }
                            _trackerState.value = newState
                            updateDiagnosticState { it.apply { isRedScreenVisible = shouldShowRedScreen; pulse = timeProvider.elapsedRealtime() } }
                        }
                    }
                    updateDiagnosticState { state -> state.apply { isAlarmSilenced = behaviorUseCase.isAlarmSilenced(stateSnapshot.lastAlarmAckTs, now); pulse = timeProvider.elapsedRealtime() } }
                }

                val currentInterval = getActiveHeartbeatInterval(0)
                delay(currentInterval)
            }
        }
    }

    private fun getActiveHeartbeatInterval(idleCount: Int): Long {
        val nav = _uiState.value.navigation
        val isA15 = _uiState.value.permissions.isA15Device
        return if (nav.isSettingsOpen || nav.isLogVisible || nav.isPhoneSetupVisible || nav.isRibbonsVisible) {
            if (isA15) 5000L else 2000L
        } else {
            if (isA15) 5000L else 2000L
        }
    }

    private fun handleLocationUpdateInternal(update: LocationUpdate) {
        val nowMs = timeProvider.currentTimeMillis()
        val nowRt = timeProvider.elapsedRealtime()
        _localMaxTemp.value = update.maxTemp
        if (_uiState.value.appMode == "tracker") _trackerMaxTemp.value = update.maxTemp

        val home = _uiState.value.homePoints.firstOrNull()
        val distToHome = if (home != null) PhysicsUtils.calculateDistance(update.lat, update.lng, home.latitude, home.longitude) else null

        updateKinematicState { current ->
            telemetryUseCase.mapHealthFromUpdate(update, if (update.isMe) current.localHealth else current.trackerHealth)

            if (!update.isMe) {
                _trackerCurrentMa.value = update.currentMa
                telemetryUseCase.mapTrackerLocation(update, current.trackerLocation, nowMs, appStartTime)
                current.apply {
                    distanceTrackerToHome = if (_uiState.value.appMode == "viewer" && PhysicsUtils.isValidLocation(update.lat, update.lng)) distToHome else current.distanceTrackerToHome
                    distanceViewerToHome = if (_uiState.value.appMode == "tracker" && PhysicsUtils.isValidLocation(update.lat, update.lng)) distToHome else current.distanceViewerToHome
                    distanceTrackerToViewer = if (PhysicsUtils.isValidLocation(current.localLocation.lat, current.localLocation.lng) && PhysicsUtils.isValidLocation(update.lat, update.lng)) PhysicsUtils.calculateDistance(update.lat, update.lng, current.localLocation.lat, current.localLocation.lng) else current.distanceTrackerToViewer
                    pulse = nowRt
                }
            } else {
                val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
                val dToOther = if (PhysicsUtils.isValidLocation(current.trackerLocation.lat, current.trackerLocation.lng) && isLocationValid) PhysicsUtils.calculateDistance(current.trackerLocation.lat, current.trackerLocation.lng, update.lat, update.lng) else null
                
                if (current.localLocation.lat != 0.0 && isLocationValid && PhysicsUtils.calculateDistance(current.localLocation.lat, current.localLocation.lng, update.lat, update.lng) > 500000.0) return@updateKinematicState current
                
                telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, appStartTime)
                current.apply {
                    distanceTrackerToHome = if (_uiState.value.appMode == "tracker" && isLocationValid) distToHome else current.distanceTrackerToHome
                    distanceViewerToHome = if (_uiState.value.appMode == "viewer" && isLocationValid) distToHome else current.distanceViewerToHome
                    distanceTrackerToViewer = if (isLocationValid) dToOther else current.distanceTrackerToViewer
                    pulse = nowRt
                }
            }
        }

        updateDiagnosticState { current ->
            if (!update.isMe) {
                telemetryUseCase.mapStats(update, current.trackerStats)
                current.trackerBattery.level = update.battery
                current.trackerBattery.temp = update.temp
                current.trackerBattery.isCharging = update.isCharging
                current.trackerBattery.isChargingStable = update.isCharging
                
                current.apply {
                    connectivity.isRelayConnected = true
                    connectivity.lastUpdateTs = nowMs
                    connectivity.lastRemoteActivityTs = nowMs
                    trackerSatsView = update.satsView
                    trackerSatsUsed = update.satsUsed
                    maxTrackerAccuracy = if (_uiState.value.appMode == "viewer" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxTrackerAccuracy
                    pulse = nowRt
                }
            } else {
                telemetryUseCase.mapStats(update, current.stats)
                current.apply {
                    viewerSatsView = if (_uiState.value.appMode == "viewer") update.satsView else current.viewerSatsView
                    viewerSatsUsed = if (_uiState.value.appMode == "viewer") update.satsUsed else current.viewerSatsUsed
                    trackerSatsView = if (_uiState.value.appMode == "tracker") update.satsView else current.trackerSatsView
                    trackerSatsUsed = if (_uiState.value.appMode == "tracker") update.satsUsed else current.trackerSatsUsed
                    maxTrackerAccuracy = if (_uiState.value.appMode == "tracker" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxTrackerAccuracy
                    maxViewerAccuracy = if (_uiState.value.appMode == "viewer" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxViewerAccuracy
                    pulse = nowRt
                }
            }
        }
    }

    private fun applyInitialSettings(initial: InitialSettings) {
        appStartTime = initial.appStartTime
        updateState { it.copy(
            deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl, 
            maxDistance = initial.maxDistance, homePoints = initial.homePoints, 
            alertSettings = initial.alertSettings, appMode = initial.appMode, 
            isSystemActive = initial.isSystemActive, selectedSirenType = initial.selectedSirenType, 
            lastAlarmAckTs = initial.lastAlarmAckTs, appStartTime = initial.appStartTime, 
            draftSettings = initial.draftSettings ?: it.draftSettings, 
            isIdentitySanitized = initial.identitySanitized
        )}
        _localMaxTemp.value = initial.maxTemp
        if (initial.appMode == "tracker") _trackerMaxTemp.value = initial.maxTemp
        
        initial.trackerStatus?.let { status -> 
            updateKinematicState { current ->
                telemetryUseCase.mapTrackerLocationFromStatus(status, current.trackerLocation)
                telemetryUseCase.mapHealthFromStatus(status, current.trackerHealth)
                current.apply { pulse = timeProvider.elapsedRealtime() }
            }
            updateDiagnosticState { current ->
                telemetryUseCase.mapStatsFromStatus(status, current.trackerStats)
                current.trackerBattery.level = status.battery
                current.trackerBattery.temp = status.temp
                current.trackerBattery.isCharging = status.isCharging
                current.trackerBattery.isChargingStable = status.isCharging
                
                current.apply {
                    connectivity.lastUpdateTs = status.ts
                    trackerSatsView = status.satsView
                    trackerSatsUsed = status.satsUsed
                    maxTrackerAccuracy = if (status.maxAccuracy > 0.0) status.maxAccuracy else current.maxTrackerAccuracy
                    pulse = timeProvider.elapsedRealtime()
                }
            }
            _trackerMaxTemp.value = status.maxTemp
            _trackerCurrentMa.value = status.currentMa
            if (_uiState.value.appMode == "tracker") _localMaxTemp.value = status.maxTemp 
        }
    }

    fun addPersistentLog(
        type: String,
        message: String,
        isImportant: Boolean = false,
        isSpecial: Boolean = false,
        specialColor: Int? = null
    ) {
        val entry = LogEntry(
            localId = UUID.randomUUID().toString(),
            timestamp = timeProvider.currentTimeMillis(),
            message = message,
            type = type.uppercase(),
            isImportant = isImportant,
            isSpecial = isSpecial,
            specialColor = specialColor,
            role = _uiState.value.appMode ?: "system"
        )
        repository.addLog(entry)
    }

    fun clearTrails(context: Context) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            repository.clearTrails()
            withContext(Dispatchers.Main.immediate) {
                Toast.makeText(context, "Trail data cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun fullInitialization(context: Context) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            repository.sendCommand(UiCommand.FullInitializationReset)
            withContext(Dispatchers.Main.immediate) {
                Toast.makeText(context, "System Re-initialized", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun computeTrailSegments(trailPoints: List<TrailPoint>, color: Int): List<MapTrailSegment> {
        if (trailPoints.isEmpty()) return emptyList()
        val segments = mutableListOf<MapTrailSegment>()
        var startIdx = 0
        while (startIdx < trailPoints.size) {
            val segmentPoints = mutableListOf<TrailPoint>()
            var currentIdx = startIdx
            while (currentIdx < trailPoints.size) {
                val pt = trailPoints[currentIdx]
                if (pt.status != SentinelStatus.VALID && currentIdx > startIdx) break
                segmentPoints.add(pt)
                currentIdx++
                if (pt.status != SentinelStatus.VALID) {
                    startIdx = currentIdx
                    break
                }
            }
            if (segmentPoints.size > 1) {
                val simplified = PhysicsUtils.simplifyTrail(segmentPoints, 1.0, { it.lat }, { it.lng })
                if (simplified.size > 1) {
                    val geoPoints = simplified.map { it.toGeoPoint() }
                    segments.add(MapTrailSegment(geoPoints, color, geoPoints.hashCode()))
                }
            }
            if (currentIdx == trailPoints.size) break
            startIdx = if (startIdx < currentIdx) {
                if (trailPoints[currentIdx - 1].status == SentinelStatus.VALID) currentIdx - 1 else currentIdx
            } else startIdx + 1
        }
        return segments
    }
}
