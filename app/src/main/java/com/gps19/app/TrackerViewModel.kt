package com.gps19.app

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps19.core.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

private data class TrackerHudUiParts(
    val appMode: String?,
    val deviceId: String,
    val viewerId: String,
    val isSystemActive: Boolean,
    val isSafeMode: Boolean,
    val performanceTier: PerformanceTier
)

private data class TrackerMapUiParts(
    val appMode: String?,
    val hydrationLevel: Int,
    val isMapButtonsVisible: Boolean,
    val isFenceVisible: Boolean,
    val geofenceMode: GeofenceMode,
    val isViolationsVisible: Boolean,
    val isGeofenceViolationsVisible: Boolean,
    val maxDistance: Double,
    val isMapLocked: Boolean,
    val mapFollowMode: MapFollowMode,
    val centeringTrackerTrigger: Int,
    val centeringViewerTrigger: Int,
    val zoomInTrigger: Int,
    val zoomOutTrigger: Int,
    val homePoints: List<GeoPoint>
)

private data class TrackerMapBase(val ui: TrackerMapUiParts, val kinematic: KinematicState, val p: Long, val prt: Long)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrackerViewModel @Inject constructor(
    val repository: MainRepository,
    private val logManager: LogManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val spatialLogicUseCase: SpatialLogicUseCase,
    private val uiStateMapper: UiStateMapper,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val telemetryUseCase: TelemetryUseCase,
    private val stateSubscriptionUseCase: StateSubscriptionUseCase,
    private val sessionUseCase: SessionUseCase,
    val timeProvider: TimeProvider,
    val audioSynthesizer: AudioSynthesizer,
    private val hydrationManager: LifecycleHydrationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "TrackerViewModel Coroutine Exception")
        addPersistentLog(type = "error", message = "TRACKER UI ERROR: ${throwable.localizedMessage}", isImportant = true)
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

    private val _gpsIndexData = MutableStateFlow(GpsIndexData(0.0, 0.0, 0.0, 0.0))
    val gpsIndexData: StateFlow<GpsIndexData> = _gpsIndexData.asStateFlow()

    private val _trackerState = MutableStateFlow(TrackerState.UNKNOWN)
    val trackerState: StateFlow<TrackerState> = _trackerState.asStateFlow()

    private val _localMaxTemp = MutableStateFlow(0.0)
    val localMaxTemp: StateFlow<Double> = _localMaxTemp.asStateFlow()

    private val _trackerMaxTemp = MutableStateFlow(0.0)
    val trackerMaxTemp: StateFlow<Double> = _trackerMaxTemp.asStateFlow()

    val dashboardConnectivityState: StateFlow<DashboardConnectivityState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _diagnosticState,
        _systemPulseRt 
    ) { mode, diag, pulseRt ->
        uiStateMapper.mapDashboardConnectivity(mode ?: "tracker", diag, pulseRt)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardConnectivityState())

    val dashboardTelemetryState: StateFlow<DashboardTelemetryState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _kinematicState,
        _systemPulseRt, 
        _trackerState
    ) { mode, kin, pulseRt, state ->
        val m = mode ?: "tracker"
        val isUltra = if (m == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        uiStateMapper.mapDashboardTelemetry(m, kin, pulseRt, state, isUltra)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardTelemetryState())

    val dashboardHealthState: StateFlow<DashboardHealthState> = combine(
        combine(
            _uiState.map { it.appMode }.distinctUntilChanged(),
            _kinematicState,
            _diagnosticState
        ) { mode, kin, diag -> Triple(mode ?: "tracker", kin, diag) },
        _localMaxTemp,
        _trackerMaxTemp,
        _systemPulseRt
    ) { (mode, kin, diag), lMax, tMax, pulseRt ->
        uiStateMapper.mapDashboardHealth(mode, kin, diag, lMax, tMax, pulseRt)
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
    .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 1000L)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    private val hudUiConnectivityFlow = _uiState.map { 
        TrackerHudUiParts(it.appMode ?: "tracker", it.deviceId, it.viewerId, it.isSystemActive, it.isSafeMode, it.permissions.performanceTier) 
    }.distinctUntilChanged()

    val hudConnectivityState: StateFlow<HudConnectivityState> = combine(
        hudUiConnectivityFlow,
        _diagnosticState,
        _rtt,
        _remoteSignal,
        _systemPulseRt
    ) { ui, diag, rtt, sig, pulseRt ->
        uiStateMapper.mapHudConnectivity(ui.appMode, ui.deviceId, ui.viewerId, ui.isSystemActive, ui.isSafeMode, ui.performanceTier == PerformanceTier.STAGGERED, diag, rtt, sig, pulseRt)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudConnectivityState())

    val hudTelemetryState: StateFlow<HudTelemetryState> = combine(
        _uiState.map { it.appMode }.distinctUntilChanged(),
        _kinematicState,
        _systemPulseRt, 
        _trackerState
    ) { mode, kin, pulseRt, state ->
        val m = mode ?: "tracker"
        val isUltra = if (m == "viewer") kinematicState.value.trackerHealth.isUltraLongStationary else kinematicState.value.localHealth.isUltraLongStationary
        uiStateMapper.mapHudTelemetry(m, kin, pulseRt, state, isUltra)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudTelemetryState())

    val hudHealthState: StateFlow<HudHealthState> = combine(
        _diagnosticState,
        _systemPulseRt, 
        _kinematicState.map { it.localHealth.isMaliAnomaly }.distinctUntilChanged()
    ) { diag, pulseRt, isMali ->
        uiStateMapper.mapHudHealth(diag, pulseRt, isMali)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudHealthState())

    private fun <T> listContentEquals(a: List<T>?, b: List<T>?, itemCompare: (T, T) -> Boolean): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (!itemCompare(a[i], b[i])) return false
        }
        return true
    }

    val trackerTrailFlow: StateFlow<List<TrailPoint>> = repository.trackerTrailFlow
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailFlow: StateFlow<List<TrailPoint>> = repository.viewerTrailFlow
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerTrailSegments: StateFlow<List<MapTrailSegment>> = trackerTrailFlow
        .map { trail -> computeTrailSegments(trail, BrandJd.toArgb()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailSegments: StateFlow<List<MapTrailSegment>> = viewerTrailFlow
        .map { trail -> computeTrailSegments(trail, ViewerCyan.toArgb()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history4MFlow = repository.getHistoryFlow("4M")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history16MFlow = repository.getHistoryFlow("16M")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history1HFlow = repository.getHistoryFlow("1H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history4HFlow = repository.getHistoryFlow("4H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history24HFlow = repository.getHistoryFlow("24H")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val history7DFlow = repository.getHistoryFlow("7D")
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
        .sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 3000L else 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var sTrkLat = 0.0; private var sTrkLng = 0.0; private var sVwrLat = 0.0; private var sVwrLng = 0.0

    val mapViewState: StateFlow<MapViewState> = combine(
        combine(_uiState.map { TrackerMapUiParts(it.appMode ?: "tracker", it.hydrationLevel, it.isMapButtonsVisible, it.isFenceVisible, it.geofenceMode, it.isViolationsVisible, it.isGeofenceViolationsVisible, it.maxDistance, it.isMapLocked, it.mapFollowMode, it.triggers.centeringTrackerTrigger, it.triggers.centeringViewerTrigger, it.triggers.zoomInTrigger, it.triggers.zoomOutTrigger, it.homePoints) }.distinctUntilChanged(), _kinematicState, _systemPulse, _systemPulseRt) { ui, kin, p, prt -> TrackerMapBase(ui, kin, p, prt) },
        trackerTrailSegments,
        viewerTrailSegments,
        repository.violationsFlow.distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }
    ) { base, trkSegs, vwrSegs, vios ->
        val ui = base.ui; val kin = base.kinematic; val pulse = base.p; val pulseRt = base.prt; val isTracker = true
        val tLat = kin.localLocation.kinetic.lat
        val tLng = kin.localLocation.kinetic.lng
        val tTs = kin.localLocation.kinetic.gpsTs
        val tTel = kin.localLocation.ts
        val vLat = 0.0
        val vLng = 0.0
        val vTs = 0L
        val vTel = 0L
        
        val isTrkFresh = tTs > 0 && (pulse - tTel + maxOf(0L, tTel - tTs)) < GPS_UI_FAIL_THRESHOLD_MS
        val isVwrFresh = false
        val isTrkValid = PhysicsUtils.isValidLocation(tLat, tLng)
        val isVwrValid = false

        if (isTrkValid) {
            val alpha = if (kin.localLocation.kinetic.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            if (sTrkLat == 0.0 || PhysicsUtils.calculateDistance(sTrkLat, sTrkLng, tLat, tLng) > 100.0) { sTrkLat = tLat; sTrkLng = tLng }
            else { sTrkLat = PhysicsUtils.smoothCoordinate(sTrkLat, tLat, alpha); sTrkLng = PhysicsUtils.smoothCoordinate(sTrkLng, tLng, alpha) }
        }

        MapViewState(
            appMode = "tracker", hydrationLevel = ui.hydrationLevel, isMapButtonsVisible = ui.isMapButtonsVisible, isFenceVisible = ui.isFenceVisible, geofenceMode = ui.geofenceMode,
            isViolationsVisible = ui.isViolationsVisible, isGeofenceViolationsVisible = ui.isGeofenceViolationsVisible, maxDistance = ui.maxDistance, isMapLocked = ui.isMapLocked, mapFollowMode = ui.mapFollowMode,
            centeringTrackerTrigger = ui.centeringTrackerTrigger, centeringViewerTrigger = ui.centeringViewerTrigger, zoomInTrigger = ui.zoomInTrigger, zoomOutTrigger = ui.zoomOutTrigger, homePoints = ui.homePoints,
            trackerLat = tLat, trackerLng = tLng, trackerSpeed = kin.localLocation.kinetic.speed, trackerAccuracy = kin.localLocation.kinetic.accuracy,
            trackerMaxAccuracy = kin.localLocation.kinetic.maxAccuracy, trackerGpsTs = tTs, trackerTelemetryTs = tTel,
            trackerLocPending = kin.localHealth.isLocationPending, trackerLocPendingReason = kin.localHealth.locationPendingReason,
            trackerLastValidFixRt = kin.localHealth.lastValidFixRt, viewerLat = vLat, viewerLng = vLng, viewerSpeed = 0.0,
            viewerAccuracy = 0.0, viewerMaxAcc = 0.0, viewerGpsTs = vTs, viewerTelemetryTs = vTel,
            viewerLocPending = false, viewerLocPendingReason = LocationPendingReason.NONE,
            viewerLastValidFixRt = 0L, replayCursorPos = kin.replayCursorPos, systemPulse = pulse, systemPulseRt = pulseRt,
            trackerSegments = trkSegs, viewerSegments = vwrSegs, violations = vios, showAccuracyBadge = true, showSettingsButton = true, showToolsOverlay = true,
            isTrackerFresh = isTrkFresh, isViewerFresh = isVwrFresh, isTrackerValid = isTrkValid, isViewerValid = isVwrValid,
            smoothedTrackerLat = sTrkLat, smoothedTrackerLng = sTrkLng, smoothedViewerLat = sVwrLat, smoothedViewerLng = sVwrLng
        )
    }.flowOn(Dispatchers.Default).sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 1000L).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapViewState())

    val eventLogsFlow: StateFlow<List<LogEntry>> = combine(_uiState.map { it.appMode }.distinctUntilChanged(), _uiState.map { it.navigation.isStrictMode }.distinctUntilChanged(), _uiState.map { it.navigation.isLogVisible }.distinctUntilChanged()) { m, s, v -> Triple(m ?: "tracker", s, v) }
        .flatMapLatest { (_, s, v) -> if (v) repository.eventLogsFlow(if (s) LOG_LIMIT_STRICT else LOG_LIMIT_STANDARD) else flowOf(emptyList()) }
        .distinctUntilChanged { old, new -> listContentEquals(old, new) { a, b -> a.contentEquals(b) } }.sample(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 1000L).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGnssDetail: StateFlow<GnssDetail?> = repository.gnssDetail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var appStartTime: Long = 0L
    private var autoSaveJob: Job? = null
    private val replayCursorRequest = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch(Dispatchers.Default + uiExceptionHandler) {
            val initialSettings = settingsUseCase.loadAllSettings()
            appStartTime = initialSettings.appStartTime
            
            withContext(Dispatchers.Main.immediate) {
                applyInitialSettings(initialSettings)
                hydrationManager.hydrationLevel.onEach { level ->
                    updateState { it.copy(session = it.session.copy(hydrationLevel = level)) }
                }.launchIn(viewModelScope)
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
                        lastAlarmAckTs = update.lastAlarmAckTs, isIdentitySanitized = update.identitySanitized
                    ),
                    spatial = it.spatial.copy(
                        maxDistance = update.maxDistance, homePoints = update.homePoints
                    ),
                    session = it.session.copy(
                        appMode = update.appMode ?: "tracker", isSystemActive = update.isSystemActive,
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
                    current.localHealth.copyFrom(update.health)
                    current.trackerHealth.copyFrom(update.health)
                    current.apply { pulse = timeProvider.elapsedRealtime() }
                }
                updateDiagnosticState { current -> 
                    current.activeAlarms = update.activeAlarms
                    current.isMaliAnomaly = update.health.isMaliAnomaly
                    current.isGnssThrottled = update.health.isGnssThrottled
                    current.lastEnergyDeltaMa = update.health.lastEnergyDeltaMa
                    current.lastEnergyDeltaTemp = update.health.lastEnergyDeltaTemp
                    current.lastEnergyDurationMs = update.health.lastEnergyDurationMs
                    current.pulse = timeProvider.elapsedRealtime()
                    current
                }
                _localMaxTemp.value = update.maxTemp
                _trackerMaxTemp.value = update.maxTemp
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(viewModelScope)

        stateSubscriptionUseCase.observeBatteryStatus().onEach { status -> 
            updateDiagnosticState { current -> 
                current.battery.level = status.level
                current.battery.temp = status.temp
                current.apply { pulse = timeProvider.elapsedRealtime() }
            } 
            _currentMa.value = status.level 
        }
        .flowOn(Dispatchers.Main.immediate)
        .launchIn(viewModelScope)

        repository.localLocation.onEach { handleLocationUpdateInternal(it) }
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
                    if (event.visible) updateState { it.copy(settings = it.settings.copy(draftSettings = settingsUseCase.prepareDraft(it))) }
                    else commitDraft()
                }
                updateNavigation { navigationUseCase.handleNavigationEvent(event, _uiState.value) }
            }
            is UiEvent.SetReplayCursor -> {
                updateNavigation { it.copy(replayCursorTs = event.ts) }
                replayCursorRequest.value = event.ts
            }
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && _uiState.value.navigation.isSettingsOpen) commitDraft()
            }
            is UiEvent.UpdateDraftDeviceId, is UiEvent.UpdateDraftViewerId, is UiEvent.UpdateDraftRelayUrl, 
            is UiEvent.UpdateDraftMaxDistance, is UiEvent.UpdateDraftAlertSettings, is UiEvent.UpdateDraftAlarmVolume, 
            is UiEvent.CommitSettings -> handleDraftEvent(event)
            is UiEvent.SetManualSelection -> updateState { it.copy(spatial = it.spatial.copy(isManualSelectionInProgress = event.active)) }
            is UiEvent.DismissIdentitySanitization -> {
                updateState { it.copy(settings = it.settings.copy(isIdentitySanitized = false)) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    repository.saveBoolean(IDENTITY_SANITIZED_KEY, false)
                }
            }
            is UiEvent.SetFenceVisible, is UiEvent.SetViolationsVisible, is UiEvent.SetGeofenceViolationsVisible,
            is UiEvent.SetMapButtonsVisible, is UiEvent.SetMapLocked, is UiEvent.MapZoomIn, is UiEvent.MapZoomOut,
            is UiEvent.CenterTracker, is UiEvent.CenterViewer, is UiEvent.SetGeofenceMode -> {
                updateState { spatialLogicUseCase.handleMapEvent(event, it) }
            }
            is UiEvent.MapTap -> handleMapTap(event.point)
            is UiEvent.AddHomePoint -> handleAddHomePoint(event.point)
            is UiEvent.RemoveHomePoint -> handleRemoveHomePoint(index = event.index)
            is UiEvent.ClearHomePoints -> handleClearHomePoints()
            is UiEvent.ClearLogs -> repository.clearLogs()
            is UiEvent.SetLogFilterShowDetails -> repository.updateLogFilters(details = event.show)
            is UiEvent.SetLogFilterShowRecovered -> repository.updateLogFilters(recovered = event.show)
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
                val now = timeProvider.currentTimeMillis()
                val nowRt = timeProvider.elapsedRealtime()

                if (_uiState.value.isInitialized) {
                    _systemPulse.value = now
                    _systemPulseRt.value = nowRt
                    repository.sendCommand(UiCommand.SyncRequest)
                }
                delay(if (_uiState.value.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 2000L)
            }
        }
    }

    private fun handleLocationUpdateInternal(update: LocationUpdate) {
        val nowMs = timeProvider.currentTimeMillis()
        val nowRt = timeProvider.elapsedRealtime()
        updateKinematicState { current ->
            telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, appStartTime)
            telemetryUseCase.mapHealthFromUpdate(update, current.localHealth)
            _localMaxTemp.value = update.atmospheric.maxTemp
            _currentMa.value = update.integrity.currentMa
            current.apply { pulse = nowRt }
        }
    }

    private fun applyInitialSettings(initial: InitialSettings) {
        appStartTime = initial.appStartTime
        updateState { it.copy(
            settings = it.settings.copy(
                deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl,
                isIdentitySanitized = initial.identitySanitized,
                draftSettings = initial.draftSettings ?: it.settings.draftSettings
            ),
            session = it.session.copy(
                appMode = "tracker", isSystemActive = initial.isSystemActive
            )
        )}
        _localMaxTemp.value = initial.maxTemp
    }

    fun addPersistentLog(type: String, message: String, isImportant: Boolean = false, isSpecial: Boolean = false, specialColor: Int? = null) {
        val entry = LogEntry(
            localId = UUID.randomUUID().toString(), timestamp = timeProvider.currentTimeMillis(),
            message = message, type = type.uppercase(), isImportant = isImportant,
            isSpecial = isSpecial, specialColor = specialColor, role = "tracker"
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
            updateState { it.copy(session = it.session.copy(appStartTime = nextStartTime)) }
            addPersistentLog("system", "Full initialization performed", isImportant = true)
        }
    }

    private fun handleMapTap(point: GeoPoint) {
        val mode = _uiState.value.geofenceMode
        if (mode == GeofenceMode.ADD) {
            onEvent(UiEvent.AddHomePoint(point))
        } else if (mode == GeofenceMode.REMOVE) {
            val idx = spatialLogicUseCase.findNearestPointIndex(_uiState.value.homePoints, point)
            if (idx != -1) onEvent(UiEvent.RemoveHomePoint(idx))
        }
    }

    private fun handleAddHomePoint(point: GeoPoint) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = spatialLogicUseCase.addHomePoint(point)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(spatial = it.spatial.copy(homePoints = newPoints, isFenceVisible = true)) }
            }
        }
    }

    private fun handleRemoveHomePoint(index: Int) {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = spatialLogicUseCase.removeHomePoint(index)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(spatial = it.spatial.copy(homePoints = newPoints)) }
            }
        }
    }

    private fun handleClearHomePoints() {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            val newPoints = spatialLogicUseCase.clearHomePoints(_uiState.value.maxDistance)
            withContext(Dispatchers.Main.immediate) {
                updateState { it.copy(spatial = it.spatial.copy(homePoints = newPoints)) }
            }
        }
    }
}
