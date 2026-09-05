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
 * HUD UI State Subset: Used to prune aggregation triggers (Issue #248).
 */
private data class HudUiParts(
    val appMode: String?,
    val deviceId: String,
    val viewerId: String,
    val isSystemActive: Boolean
)

/**
 * MainViewModel: Manages UI state and orchestrates data flow.
 * Sep.05.28:
 * - Issue #914 RESOLVED: GNSS Detail Sampling. Implemented A15-aware sampling 
 *   for activeGnssDetail flow to reduce UI overhead during satellite monitoring (R-ID 267).
 * Sep.05.25:
 * - Issue #266: Propagated Mali Anomaly status to HUD for UI-throttling.
 * Sep.05.24:
 * - Issue #910 RESOLVED: Implemented Hydration Watchdog.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository,
    private val logManager: LogManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val homePointUseCase: HomePointUseCase,
    private val aggregator: UiStateAggregator,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val telemetryUseCase: TelemetryUseCase,
    private val stateSubscriptionUseCase: StateSubscriptionUseCase,
    private val sessionUseCase: SessionUseCase,
    private val behaviorUseCase: BehaviorUseCase,
    private val alertUseCase: AlertUseCase,
    private val mapUseCase: MapUseCase,
    val timeProvider: TimeProvider,
    val audioSynthesizer: AudioSynthesizer,
    private val remoteStatusRepository: RemoteStatusRepository,
    private val hydrationManager: LifecycleHydrationManager,
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

    // Segmented Dashboard Flows (R248)
    val dashboardConnectivityState: StateFlow<DashboardConnectivityState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _diagnosticState,
        _systemPulse
    ) { mode, diag, pulse ->
        aggregator.aggregateDashboardConnectivity(mode, diag, pulse)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardConnectivityState())

    val dashboardTelemetryState: StateFlow<DashboardTelemetryState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _kinematicState,
        _systemPulse,
        _trackerState
    ) { mode, kin, pulse, state ->
        val isUltra = if (mode == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        aggregator.aggregateDashboardTelemetry(mode, kin, pulse, state, isUltra)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardTelemetryState())

    val dashboardHealthState: StateFlow<DashboardHealthState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _kinematicState,
        _diagnosticState,
        _localMaxTemp,
        _trackerMaxTemp
    ) { mode, kin, diag, lMax, tMax ->
        aggregator.aggregateDashboardHealth(mode, kin, diag, lMax, tMax)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardHealthState())

    val dashboardState: StateFlow<DashboardState> = combine(
        dashboardConnectivityState,
        dashboardTelemetryState,
        dashboardHealthState
    ) { conn, tel, health ->
        DashboardState(conn, tel, health)
    }
    .distinctUntilChanged()
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    // Segmented HUD Flows (R248 Remediation)
    private val hudUiConnectivityFlow = _uiState.map { 
        HudUiParts(it.appMode, it.deviceId, it.viewerId, it.isSystemActive) 
    }.distinctUntilChanged()

    val hudConnectivityState: StateFlow<HudConnectivityState> = combine(
        hudUiConnectivityFlow,
        _diagnosticState,
        _rtt,
        _remoteSignal
    ) { ui, diag, rtt, sig ->
        aggregator.aggregateHudConnectivity(ui.appMode, ui.deviceId, ui.viewerId, ui.isSystemActive, diag, rtt, sig)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudConnectivityState())

    val hudTelemetryState: StateFlow<HudTelemetryState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _kinematicState,
        _systemPulse,
        _trackerState
    ) { mode, kin, pulse, state ->
        val isUltra = if (mode == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        aggregator.aggregateHudTelemetry(mode, kin, pulse, state, isUltra)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudTelemetryState())

    val hudHealthState: StateFlow<HudHealthState> = combine(
        _diagnosticState,
        _systemPulse,
        _kinematicState.map { it.localHealth.isMaliAnomaly }.distinctUntilChanged()
    ) { diag, pulse, isMali ->
        aggregator.aggregateHudHealth(diag, pulse, isMali)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudHealthState())

    val hudState: StateFlow<HudState> = combine(
        hudConnectivityState,
        hudTelemetryState,
        hudHealthState
    ) { conn, tel, health ->
        HudState(conn, tel, health)
    }
    .distinctUntilChanged()
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState())

    // Snap-Isolation: Deep parity check for list-based flows (R312)
    private fun <T> listContentEquals(a: List<T>?, b: List<T>?, itemCompare: (T, T) -> Boolean): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (!itemCompare(a[i], b[i])) return false
        }
        return true
    }

    // Logic and Event Handlers
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
    .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
    .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerTrailFlow: StateFlow<List<TrailPoint>> = _uiState.map { it.appMode }.distinctUntilChanged()
        .flatMapLatest { mode -> if (mode != null) repository.trackerTrailFlow else flowOf(emptyList()) }
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailFlow: StateFlow<List<TrailPoint>> = _uiState.map { it.appMode }.distinctUntilChanged()
        .flatMapLatest { mode -> if (mode != null) repository.viewerTrailFlow else flowOf(emptyList()) }
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val violationPointsFlow: StateFlow<List<ViolationPoint>> = repository.violationsFlow
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Forensic Ribbon Flows with Snap-Isolation Parity & A15 Sampling (R312/R650)
    val history4MFlow = repository.getHistoryFlow("4M")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history16MFlow = repository.getHistoryFlow("16M")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history1HFlow = repository.getHistoryFlow("1H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history4HFlow = repository.getHistoryFlow("4H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history24HFlow = repository.getHistoryFlow("24H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history7DFlow = repository.getHistoryFlow("7D")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Issue #914 Hardening: Sampling GNSS updates to mitigate UI overhead on budget devices.
     */
    val activeGnssDetail: StateFlow<GnssDetail?> = repository.gnssDetail
        .sample(if (_uiState.value.permissions.isA15Device) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    var appStartTime: Long = 0L
    private var autoSaveJob: Job? = null
    private var isHeavyObservationStarted = false
    private val replayCursorRequest = MutableStateFlow<Long?>(null)

    init {
        // Issue #910 Forensic: Log state changes
        viewModelScope.launch {
            _uiState.map { it.hydrationLevel }.distinctUntilChanged().collect {
                Timber.d("Issue #910: Hydration Level: $it")
            }
        }
        viewModelScope.launch {
            _uiState.map { it.isInitialized }.distinctUntilChanged().collect {
                Timber.d("Issue #910: Is Initialized: $it")
            }
        }
        viewModelScope.launch {
            _uiState.map { it.appMode }.distinctUntilChanged().collect {
                Timber.d("Issue #910: App Mode: $it")
            }
        }
        viewModelScope.launch {
            _uiState.map { it.isSystemActive }.distinctUntilChanged().collect {
                Timber.d("Issue #910: Is System Active: $it")
            }
        }

        // Issue #910: Hydration Watchdog
        viewModelScope.launch(Dispatchers.Default) {
            delay(WATCH_DOG_UI_GRACE_MS)
            if (_uiState.value.hydrationLevel < 3) {
                Timber.e("Issue #910: Hydration Watchdog Triggered. Stuck at level ${_uiState.value.hydrationLevel}")
                addPersistentLog(
                    type = "error", 
                    message = "HYDRATION WATCHDOG: Level 2 Hang Detected. Forcing UI shell.", 
                    isImportant = true
                )
                withContext(Dispatchers.Main.immediate) {
                    updateState { it.copy(isInitialized = true) }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default + uiExceptionHandler) {
            val initialSettings = settingsUseCase.loadAllSettings()
            
            withContext(Dispatchers.Main.immediate) {
                applyInitialSettings(initialSettings)
                
                hydrationManager.hydrationLevel.onEach { level ->
                    updateState { it.copy(hydrationLevel = level) }
                }.launchIn(viewModelScope)

                val isA15 = systemStatusProvider.isA15Hardware()
                hydrationManager.startHydration(viewModelScope, isA15) {
                    updateState { it.copy(isInitialized = true) }
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
            
            launch(Dispatchers.Main.immediate) {
                _uiState.filter { it.isFullyHydrated && it.appMode != null }.first()
                if (systemStatusProvider.isA15Hardware()) delay(1000)
                startHeavyObservations()
            }

            launch(Dispatchers.Default) {
                replayCursorRequest.collectLatest { ts ->
                    if (ts == null) {
                        updateKinematicState { it.apply { replayCursorPos = null } }
                        return@collectLatest
                    }
                    val mode = _uiState.value.appMode
                    val trail = if (mode == "viewer") trackerTrailFlow.value else viewerTrailFlow.value
                    val bestPoint = stateSubscriptionUseCase.findClosestTrailPoint(trail, ts)
                    bestPoint?.let { bp ->
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
                    deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl,
                    maxDistance = update.maxDistance, homePoints = update.homePoints, lastAlarmAckTs = update.lastAlarmAckTs,
                    appMode = update.appMode, isSystemActive = update.isSystemActive,
                    permissions = it.permissions.copy(isManualOverride = update.isXiaomiManualOverride),
                    isIdentitySanitized = update.identitySanitized
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
                    updateState { it.copy(permissions = newState.copy(isA15Device = isA15)) } 
                }
                delay(if (refreshFast) 5000L else 30000L) 
            } 
        }
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
                        pulse = timeProvider.elapsedRealtime()
                    }
                }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeIntegrityUpdates()
            .onEach { update ->
                updateKinematicState { current ->
                    if (_uiState.value.appMode == "tracker") current.trackerHealth.copyFrom(update.health)
                    current.localHealth.copyFrom(update.health)
                    current.apply { pulse = timeProvider.elapsedRealtime() }
                }
                updateDiagnosticState { current -> 
                    current.activeAlarms = update.activeAlarms
                    current.isMaliAnomaly = update.health.isMaliAnomaly
                    current.pulse = timeProvider.elapsedRealtime()
                    current
                }
                _localMaxTemp.value = update.maxTemp
                if (_uiState.value.appMode == "tracker") _trackerMaxTemp.value = update.maxTemp
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeBatteryStatus().onEach { status -> 
            updateDiagnosticState { current -> 
                current. battery.level = status.level
                current.battery.temp = status.temp
                current.apply { pulse = timeProvider.elapsedRealtime() }
            } 
            _currentMa.value = status.currentMa
        }
        .flowOn(Dispatchers.Main.immediate)
        .launchIn(viewModelScope)

        remoteStatusRepository.remoteStatus.onEach { status ->
            if (_uiState.value.appMode == "viewer") {
                _remoteSignal.value = remoteStatusRepository.peerSignal.value
                _trackerState.value = status.trackerState
                _trackerMaxTemp.value = status.maxTemp
                updateKinematicState { current ->
                    telemetryUseCase.mapTrackerLocationFromStatus(status, current.trackerLocation)
                    telemetryUseCase.mapHealthFromStatus(status, current.trackerHealth)
                    current.apply { pulse = timeProvider.elapsedRealtime() }
                }
                updateDiagnosticState { current ->
                    current.trackerBattery.level = status.battery
                    current.trackerBattery.temp = status.temp
                    current.pulse = timeProvider.elapsedRealtime()
                    current
                }
            }
        }
        .flowOn(Dispatchers.Main.immediate)
        .launchIn(viewModelScope)

        repository.localLocation.onEach { handleLocationUpdateInternal(it) }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        repository.trackerLocation.onEach { handleLocationUpdateInternal(it) }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ToggleMap, is UiEvent.ToggleLog, is UiEvent.ToggleSettings, 
            is UiEvent.TogglePhoneSetup, is UiEvent.ToggleRibbons, is UiEvent.SetDashboardExpanded,
            is UiEvent.ToggleGnssDetail, is UiEvent.SetSubSettings, is UiEvent.ShowStopTrackingConfirmation,
            is UiEvent.NavigateToDiagnostics, is UiEvent.SetPendingMode -> {
                if (event is UiEvent.ToggleSettings) {
                    if (event.visible) updateState { it.copy(draftSettings = settingsUseCase.prepareDraft(it)) }
                    else commitDraft()
                }
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.SetReplayCursor -> handleReplayCursor(event.ts)
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && _uiState.value.navigation.isSettingsOpen) commitDraft()
            }
            is UiEvent.SetSystemActive -> { 
                updateState { it.copy(isSystemActive = event.active) } 
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { sessionUseCase.setSystemActive(event.active) }
            }
            is UiEvent.SetAppMode -> {
                if (event.mode != null) {
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        if (systemStatusProvider.isA15Hardware()) delay(500)
                        startHeavyObservations()
                    }
                }
                viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
                    val newStartTime = sessionUseCase.setAppMode(event.mode)
                    updateState { it.copy(
                        appMode = event.mode, 
                        appStartTime = newStartTime ?: it.appStartTime,
                        isSystemActive = if (event.mode != null) true else it.isSystemActive
                    )}
                }
            }
            is UiEvent.ConfirmStopTracking, UiEvent.ManualExit -> {
                updateState { it.copy(isSystemActive = false, appMode = null) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    sessionUseCase.stopTrackingSession()
                }
            }
            is UiEvent.TriggerRecovery -> {
                if (_uiState.value.isRecoveryPending) {
                    updateNavigation { it.copy(serviceRecoveryTrigger = it.serviceRecoveryTrigger + 1) }
                    updateState { it.copy(isRecoveryPending = false) }
                }
            }
            is UiEvent.SetRecoveryPending -> updateState { it.copy(isRecoveryPending = event.pending) }
            is UiEvent.LogAction -> addPersistentLog(event.type, message = event.message, isImportant = event.isImportant, isSpecial = event.isSpecial, specialColor = event.specialColor)
            is UiEvent.RefreshPermissionStatus -> viewModelScope.launch(Dispatchers.IO) { 
                systemStatusProvider.getPermissionState(forceRefresh = true)
            }
            is UiEvent.UpdateDraftDeviceId, is UiEvent.UpdateDraftViewerId, is UiEvent.UpdateDraftRelayUrl, 
            is UiEvent.UpdateDraftMaxDistance, is UiEvent.UpdateDraftAlertSettings, is UiEvent.UpdateDraftAlarmVolume, 
            is UiEvent.CommitSettings -> handleDraftEvent(event)
            is UiEvent.SetForensicSimulation -> {
                updateState { it.copy(isForensicStallSimulated = event.active) }
                logManager.setForensicStallSimulation(event.active)
            }
            is UiEvent.ExecuteStressTest -> {
                repository.sendCommand(UiCommand.ExecuteStressTest)
            }
            is UiEvent.SetStorageSimulation -> {
                repository.sendCommand(UiCommand.SimulateStoragePressure(event.active, event.isCritical))
            }
            is UiEvent.SetManualSelection -> updateState { it.copy(isManualSelectionInProgress = event.active) }
            is UiEvent.SetSettlingActive -> updateState { it.copy(isSettlingActive = event.active) }
            is UiEvent.ToggleSetupBypass -> updateState { it.copy(isSetupBypassActive = event.active) }
            is UiEvent.DismissIdentitySanitization -> {
                updateState { it.copy(isIdentitySanitized = false) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    repository.saveBoolean(IDENTITY_SANITIZED_KEY, false)
                }
            }
            is UiEvent.SetFenceVisible, is UiEvent.SetViolationsVisible, is UiEvent.SetGeofenceViolationsVisible,
            is UiEvent.SetMapButtonsVisible, is UiEvent.SetMapLocked, is UiEvent.MapZoomIn, is UiEvent.MapZoomOut,
            is UiEvent.CenterTracker, is UiEvent.CenterViewer, is UiEvent.SetGeofenceMode -> {
                updateState { mapUseCase.handleMapEvent(event, it) }
            }
            is UiEvent.MapTap -> handleMapTap(event.point)
            is UiEvent.AddHomePoint -> handleAddHomePoint(event.point)
            is UiEvent.RemoveHomePoint -> handleRemoveHomePoint(event.index)
            is UiEvent.ClearHomePoints -> handleClearHomePoints()
            else -> {}
        }
    }

    private fun handleReplayCursor(ts: Long?) {
        updateNavigation { it.copy(replayCursorTs = ts) }
        replayCursorRequest.value = ts
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
            settingsUseCase.commitDraft()
            updateState { it.copy(draftSettings = DraftSettings()) }
        }
    }

    private fun updateDraft(update: (DraftSettings) -> DraftSettings) {
        updateState { it.copy(draftSettings = update(it.draftSettings)) }
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { delay(300L); settingsUseCase.saveDraftToRepo(_uiState.value.draftSettings) }
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
    private fun updateKinematicState(update: (KinematicState) -> KinematicState) { _kinematicState.update { current -> update(current) } }
    private fun updateDiagnosticState(update: (DiagnosticState) -> DiagnosticState) { _diagnosticState.update { current -> update(current) } }
    private fun updateNavigation(update: (NavigationState) -> NavigationState) { updateState { it.copy(navigation = update(it.navigation)) } }

    private fun startGlobalTimer() {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            while (true) {
                if (_uiState.value.isInitialized && _uiState.value.appMode != null) {
                    val now = timeProvider.currentTimeMillis()
                    val nowRt = timeProvider.elapsedRealtime()
                    _systemPulse.value = now
                    _systemPulseRt.value = nowRt
                    repository.sendCommand(UiCommand.SyncRequest)
                }
                delay(if (_uiState.value.permissions.isA15Device) 5000L else 2000L)
            }
        }
    }

    private fun handleLocationUpdateInternal(update: LocationUpdate) {
        val nowMs = timeProvider.currentTimeMillis()
        val nowRt = timeProvider.elapsedRealtime()
        updateKinematicState { current ->
            if (update.isMe) {
                telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, appStartTime)
                telemetryUseCase.mapHealthFromUpdate(update, current.localHealth)
                _localMaxTemp.value = update.maxTemp
                _currentMa.value = update.currentMa

                if (_uiState.value.appMode == "tracker") {
                    telemetryUseCase.mapTrackerLocation(update, current.trackerLocation, nowMs, appStartTime)
                    telemetryUseCase.mapHealthFromUpdate(update, current.trackerHealth)
                    _trackerMaxTemp.value = update.maxTemp
                    _trackerState.value = update.trackerState
                    _trackerCurrentMa.value = update.currentMa
                }
            } else {
                telemetryUseCase.mapTrackerLocation(update, current.trackerLocation, nowMs, appStartTime)
                telemetryUseCase.mapHealthFromUpdate(update, current.trackerHealth)
                _trackerMaxTemp.value = update.maxTemp
                _trackerState.value = update.trackerState
                _trackerCurrentMa.value = update.currentMa
            }
            current.apply { pulse = nowRt }
        }
    }

    private fun applyInitialSettings(initial: InitialSettings) {
        appStartTime = initial.appStartTime
        updateState { it.copy(
            deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl, 
            appMode = initial.appMode, isSystemActive = initial.isSystemActive,
            draftSettings = initial.draftSettings ?: it.draftSettings,
            isIdentitySanitized = initial.identitySanitized
        )}
        _localMaxTemp.value = initial.maxTemp
    }

    fun addPersistentLog(type: String, message: String, isImportant: Boolean = false, isSpecial: Boolean = false, specialColor: Int? = null) {
        val entry = LogEntry(
            localId = UUID.randomUUID().toString(), timestamp = timeProvider.currentTimeMillis(),
            message = message, type = type.uppercase(), isImportant = isImportant,
            isSpecial = isSpecial, specialColor = specialColor, role = _uiState.value.appMode ?: "system"
        )
        repository.addLog(entry)
    }

    private fun computeTrailSegments(trailPoints: List<TrailPoint>, color: Int): List<MapTrailSegment> {
        if (trailPoints.isEmpty()) return emptyList()
        val geoPoints = trailPoints.map { it.toGeoPoint() }
        return listOf(MapTrailSegment(geoPoints, color, geoPoints.hashCode()))
    }

    fun clearTrails(context: Context) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            repository.clearTrails()
            addPersistentLog("system", "Trails cleared by user", isImportant = true)
        }
    }

    fun fullInitialization(context: Context) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val nextStartTime = settingsUseCase.fullInitialization(context)
            updateState { it.copy(appStartTime = nextStartTime) }
            addPersistentLog("system", "Full initialization performed", isImportant = true)
        }
    }

    private fun handleMapTap(point: GeoPoint) {
        val mode = _uiState.value.geofenceMode
        if (mode == GeofenceMode.ADD) {
            onEvent(UiEvent.AddHomePoint(point))
        } else if (mode == GeofenceMode.REMOVE) {
            val idx = homePointUseCase.findNearestPointIndex(_uiState.value.homePoints, point)
            if (idx != -1) onEvent(UiEvent.RemoveHomePoint(idx))
        }
    }

    private fun handleAddHomePoint(point: GeoPoint) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = homePointUseCase.addHomePoint(_uiState.value.homePoints, point, _uiState.value.maxDistance)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(homePoints = newPoints, geofenceMode = GeofenceMode.IDLE) }
            }
        }
    }

    private fun handleRemoveHomePoint(index: Int) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = homePointUseCase.removeHomePoint(_uiState.value.homePoints, index, _uiState.value.maxDistance)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(homePoints = newPoints, geofenceMode = GeofenceMode.IDLE) }
            }
        }
    }

    private fun handleClearHomePoints() {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = homePointUseCase.clearHomePoints(_uiState.value.maxDistance)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(homePoints = newPoints) }
            }
        }
    }
}
