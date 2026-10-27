package com.gps19.app

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.toArgb
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
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * MainViewModel: Orchestrates top-level application state and global navigation.
 * Oct.26.5:
 * - Fix: Resolved HUD Stale State Bug. Implemented double-buffering for 
 *   KinematicState and DiagnosticState to ensure StateFlow emission on mutation.
 * Oct.24.20:
 * - ISSUE-OCT24.14-F3: Davey Optimization.
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
    private val telemetryUseCase: TelemetryUseCase,
    private val remoteStatusRepository: RemoteStatusRepository,
    private val alertUseCase: AlertUseCase,
    val timeProvider: TimeProvider,
    val audioSynthesizer: AudioSynthesizer,
    private val hydrationManager: LifecycleHydrationManager,
    private val sirenLockoutUseCase: SirenLockoutUseCase,
    private val uiEventCoordinator: UiEventCoordinator,
    @ApplicationContext private val context: Context
) : ViewModel(), UiStateProvider {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "MainViewModel Coroutine Exception")
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _cameraActions = MutableSharedFlow<CameraAction>(extraBufferCapacity = 16)
    val cameraActions: SharedFlow<CameraAction> = _cameraActions.asSharedFlow()

    private val _uiEffects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 64)
    val uiEffects: SharedFlow<UiEffect> = _uiEffects.asSharedFlow()

    private var sTrkLat = 0.0; private var sTrkLng = 0.0
    private var sVwrLat = 0.0; private var sVwrLng = 0.0

    override val session: StateFlow<SessionUiState> = _uiState
        .map { it.session }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState())

    override val settings: StateFlow<SettingsUiState> = _uiState
        .map { it.settings }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    override val spatial: StateFlow<SpatialUiState> = _uiState
        .map { it.spatial }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpatialUiState())

    override val navigation: StateFlow<NavigationState> = _uiState
        .map { it.navigation }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationState())

    override val simulation: StateFlow<SimulationUiState> = _uiState
        .map { it.simulation }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SimulationUiState())

    // Oct.26.5: State Buffers to force emission
    private val kinBuffers = listOf(KinematicState(), KinematicState())
    private var kinBufIdx = 0
    private val _kinematicState = MutableStateFlow(kinBuffers[0])
    override val kinematic: StateFlow<KinematicState> = _kinematicState.asStateFlow()

    private val diagBuffers = listOf(DiagnosticState(), DiagnosticState())
    private var diagBufIdx = 0
    private val _diagnosticState = MutableStateFlow(diagBuffers[0])
    override val diagnostic: StateFlow<DiagnosticState> = _diagnosticState.asStateFlow()

    override val isSirenPlaying: StateFlow<Boolean> = _diagnosticState
        .map { it.isSirenPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _systemPulseRt = MutableStateFlow(timeProvider.elapsedRealtime)
    override val systemPulseRt: StateFlow<Long> = _systemPulseRt.asStateFlow()

    private val _rtt = MutableStateFlow(0)
    override val rtt: StateFlow<Int> = _rtt.asStateFlow()

    private val _remoteSignal = MutableStateFlow(0)
    override val remoteSignal: StateFlow<Int> = _remoteSignal.asStateFlow()

    private val _currentMa = MutableStateFlow(0)
    override val currentMa: StateFlow<Int> = _currentMa.asStateFlow()

    private val _trackerState = MutableStateFlow(TrackerState.UNKNOWN)
    val trackerState: StateFlow<TrackerState> = _trackerState.asStateFlow()

    private val _trackerMaxTemp = MutableStateFlow(0.0)
    val trackerMaxTemp: StateFlow<Double> = _trackerMaxTemp.asStateFlow()

    private val _gpsIndexData = MutableStateFlow(GpsIndexData(0.0, 0.0, 0.0, 0.0))
    override val gpsIndexData: StateFlow<GpsIndexData> = _gpsIndexData.asStateFlow()

    override val activeGnssDetail: StateFlow<GnssDetail?> = combine(
        session.map { it.appMode }.distinctUntilChanged(),
        kinematic
    ) { mode, kin ->
        if (mode == "tracker") kin.localLocation.integrity.gnssDetail 
        else kin.trackerLocation.integrity.gnssDetail
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val eventLogs: StateFlow<List<LogEntry>> = combine(
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

    override val logFilterDetails: StateFlow<Boolean> = repository.logFilterDetails
    override val logFilterRecovered: StateFlow<Boolean> = repository.logFilterRecovered

    override val history4M: StateFlow<HistoryBuffer> = repository.getHistoryFlow("4M").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)
    override val history16M: StateFlow<HistoryBuffer> = repository.getHistoryFlow("16M").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)
    override val history1H: StateFlow<HistoryBuffer> = repository.getHistoryFlow("1H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)
    override val history4H: StateFlow<HistoryBuffer> = repository.getHistoryFlow("4H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)
    override val history24H: StateFlow<HistoryBuffer> = repository.getHistoryFlow("24H").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)
    override val history7D: StateFlow<HistoryBuffer> = repository.getHistoryFlow("7D").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryBuffer.EMPTY)

    private val trackerTrailFlow: StateFlow<TrailBuffer> = repository.trackerTrailFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrailBuffer.EMPTY)

    private val viewerTrailFlow: StateFlow<TrailBuffer> = repository.viewerTrailFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrailBuffer.EMPTY)

    private val trackerTrailSegments: StateFlow<List<MapTrailSegment>> = trackerTrailFlow
        .map { buffer -> MapStateMapper.computeTrailSegments(buffer, BrandJd.toArgb(), timeProvider.currentTimeMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val viewerTrailSegments: StateFlow<List<MapTrailSegment>> = viewerTrailFlow
        .map { buffer -> MapStateMapper.computeTrailSegments(buffer, ViewerCyan.toArgb(), timeProvider.currentTimeMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val dashboardState: StateFlow<DashboardState> = combine(
        combine(
            _uiState.map { it.session.appMode }.distinctUntilChanged(),
            kinematic,
            diagnostic
        ) { mode, kin, diag -> Triple(mode ?: "tracker", kin, diag) },
        systemPulseRt,
        _trackerState,
        _trackerMaxTemp
    ) { (mode, kin, diag), pulseRt, state, tMax ->
        val isUltra = if (mode == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        DashboardState(
            DashboardMapper.mapDashboardConnectivity(mode, diag, pulseRt),
            DashboardMapper.mapDashboardTelemetry(mode, kinematic.value, pulseRt, state, isUltra),
            DashboardMapper.mapDashboardHealth(mode, kinematic.value, diag, diag.battery.temp, tMax, pulseRt)
        )
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    override val hudState: StateFlow<HudState> = combine(
        combine(session, settings, diagnostic) { s, set, d -> Triple(s, set, d) },
        combine(_rtt, _remoteSignal, systemPulseRt) { r, sig, p -> Triple(r, sig, p) },
        combine(kinematic, _trackerState) { k, ts -> k to ts }
    ) { (session, settings, diag), (rtt, remoteSignal, pulseRt), (kin, trackerState) ->
        HudMapper.mapHudState(session, settings, diag, rtt, remoteSignal, pulseRt, kin, trackerState)
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState())

    override val mapViewState: StateFlow<MapViewState> = combine(
        combine(
            _uiState.map { it.session.appMode }.distinctUntilChanged(),
            _uiState.map { it.session.hydrationLevel }.distinctUntilChanged(),
            spatial,
            kinematic
        ) { mode, hydration, spatial, kin -> 
            FourParts(mode, hydration, spatial, kin) 
        },
        systemPulseRt,
        trackerTrailSegments,
        viewerTrailSegments,
        repository.violationsFlow.distinctUntilChanged()
    ) { parts, pulseRt, trkSegs, vwrSegs, vios ->
        val (state, nextSmoothed) = MapStateMapper.mapMapViewState(
            parts.mode, parts.hydration, parts.spatial, parts.kin, 
            pulseRt, timeProvider.currentTimeMillis, trkSegs, vwrSegs, vios,
            MapStateMapper.SmoothedCoordinates(sTrkLat, sTrkLng, sVwrLat, sVwrLng)
        )
        sTrkLat = nextSmoothed.trkLat; sTrkLng = nextSmoothed.trkLng
        sVwrLat = nextSmoothed.vwrLat; sVwrLng = nextSmoothed.vwrLng
        state
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapViewState())

    private data class FourParts(val mode: String?, val hydration: Int, val spatial: SpatialUiState, val kin: KinematicState)

    private val replayCursorRequest = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch(Dispatchers.Default + uiExceptionHandler) {
            val initialSettings = settingsUseCase.loadAllSettings()
            val initialPerms = systemStatusProvider.getPermissionState()
            
            withContext(Dispatchers.Main.immediate) {
                applyInitialSettings(initialSettings)
                
                // R-ID 242: Last Mode Restoration. Automatically resume service if mode is present.
                if (!initialSettings.isRestorationDisabled) {
                    initialSettings.appMode?.let { mode ->
                        Timber.d("MODE_RESTORE: Resuming service for mode: $mode")
                        _uiEffects.emit(UiEffect.StartService(mode))
                    }
                } else {
                    Timber.d("MODE_RESTORE: Bypassing R-ID 242 as restoration is disabled.")
                }

                hydrationManager.hydrationLevel.onEach { level -> 
                    updateState { it.copy(session = it.session.copy(hydrationLevel = level)) } 
                    
                    // ISSUE-OCT24.14-F3: Defer heavy Main-thread observations until 
                    // after Phase 1 (Core Surface) stabilization to avoid Davey violation.
                    if (level == 1) {
                        launch(Dispatchers.Main.immediate) {
                            startBaseObservations()
                            startGlobalTimer() 
                        }
                    }
                }.launchIn(viewModelScope)

                hydrationManager.startHydration(viewModelScope, initialPerms.performanceTier == PerformanceTier.STAGGERED) { 
                    updateState { it.copy(session = it.session.copy(isInitialized = true)) } 
                }
            }
            
            launch(Dispatchers.IO) { 
                delay(STAGGERED_IO_PRUNING_DELAY_MS + 1000)
                PruningWorker.triggerNow(context)
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
                            pulse = timeProvider.elapsedRealtime 
                        } } 
                    }
                }
            }
        }
    }

    private fun startBaseObservations() {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            launch { 
                stateSubscriptionUseCase.observeRepositorySettings().collect { update -> 
                    updateState { it.copy(
                        settings = it.settings.copy(deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl, lastAlarmAckTs = update.lastAlarmAckTs, isIdentitySanitized = update.identitySanitized, alertSettings = update.alertSettings), 
                        spatial = it.spatial.copy(maxDistance = update.maxDistance, homePoints = update.homePoints), 
                        session = it.session.copy(appMode = update.appMode, isSystemActive = update.isSystemActive, permissions = it.permissions.copy(isManualOverride = update.isXiaomiManualOverride))
                    ) } 
                } 
            }
            launch {
                repository.isRestorationDisabledFlow.collect { disabled ->
                    updateState { it.copy(simulation = it.simulation.copy(isRestorationDisabled = disabled)) }
                }
            }
            launch { 
                stateSubscriptionUseCase.observeInternetStatus().collect { online -> 
                    updateDiagnosticState { it.apply { connectivity.isLocalOnline = online; pulse = timeProvider.elapsedRealtime } } 
                } 
            }
            launch { 
                stateSubscriptionUseCase.observeConnectivityBasics().collect { update -> 
                    _rtt.value = update.lastRtt
                    updateDiagnosticState { it.apply { connectivity.isRelayConnected = update.isRelayConnected; connectivity.lastRemoteActivityTs = update.lastRemoteActivityTs; recoveryCount = update.recoveryCount; cumulativeRecoveryBlackoutMs = update.cumulativeRecoveryBlackoutMs; pulse = timeProvider.elapsedRealtime } } 
                } 
            }
            launch { 
                stateSubscriptionUseCase.observeIntegrityUpdates().collect { update -> 
                    updateDiagnosticState { current -> 
                        current.activeAlarms = update.activeAlarms
                        if (update.activeAlarms.any { !it.isResolved && !it.isSirenDisabled } && _uiState.value.session.isSystemActive && _uiState.value.session.appMode == "viewer") { 
                            if (!current.isRedScreenVisible) current.isRedScreenVisible = true 
                        }
                        current.pulse = timeProvider.elapsedRealtime
                        current 
                    } 
                } 
            }
            launch { 
                stateSubscriptionUseCase.observeBatteryStatus().collect { status -> 
                    updateDiagnosticState { current -> 
                        current.battery.level = status.level
                        current.battery.temp = status.temp
                        current.apply { pulse = timeProvider.elapsedRealtime } 
                    }
                    _currentMa.value = status.level 
                } 
            }
            
            val isMaliAnomalyFlow = kinematic.map { it.localHealth.isMaliAnomaly }.distinctUntilChanged()
            
            launch { 
                isMaliAnomalyFlow.flatMapLatest { isAnomaly ->
                    val interval = if (isAnomaly) HUD_STATE_SAMPLE_THROTTLED_MS else HUD_STATE_SAMPLE_MS
                    repository.localLocation.sample(interval)
                }.collect { update -> 
                    val nowMs = timeProvider.currentTimeMillis
                    val mode = _uiState.value.session.appMode
                    updateKinematicState { current -> 
                        telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, _uiState.value.session.appStartTime)
                        telemetryUseCase.mapHealthFromUpdate(update, current.localHealth)
                        current.apply { pulse = timeProvider.elapsedRealtime } 
                    }
                    if (mode == "tracker") {
                        _trackerState.value = update.trackerState
                    }
                    _gpsIndexData.value = GpsIndexData(update.integrity.snrIdx, update.integrity.satsUsed.toDouble(), update.integrity.satsView.toDouble(), 0.0)
                } 
            }

            launch { 
                isMaliAnomalyFlow.flatMapLatest { isAnomaly ->
                    val interval = if (isAnomaly) HUD_STATE_SAMPLE_THROTTLED_MS else HUD_STATE_SAMPLE_MS
                    remoteStatusRepository.remoteStatus.sample(interval)
                }.collect { status -> 
                    val nowMs = timeProvider.currentTimeMillis
                    val mode = _uiState.value.session.appMode
                    _remoteSignal.value = remoteStatusRepository.peerSignal.value
                    if (mode != "tracker") _trackerState.value = status.trackerState
                    _trackerMaxTemp.value = status.maxTemp
                    updateKinematicState { current -> 
                        telemetryUseCase.mapTrackerLocation(status, current.trackerLocation, nowMs, _uiState.value.session.appStartTime)
                        telemetryUseCase.mapHealthFromUpdate(status, current.trackerHealth)
                        current.apply { pulse = timeProvider.elapsedRealtime } 
                    }
                    updateDiagnosticState { it.apply { 
                        trackerBattery.level = status.battery
                        trackerBattery.temp = status.temp
                        trackerIsGnssThrottled = status.isGnssThrottled
                        pulse = timeProvider.elapsedRealtime 
                    } } 
                } 
            }

            launch { 
                audioSynthesizer.isSirenPlaying.collect { playing -> 
                    updateDiagnosticState { it.apply { isSirenPlaying = playing } }
                    if (playing && _uiState.value.session.isSystemActive && _uiState.value.session.appMode == "viewer") {
                        updateDiagnosticState { it.apply { isRedScreenVisible = true } } 
                    }
                } 
            }
            launch { 
                sirenLockoutUseCase.silencedUntilRt.collect { ts -> 
                    updateDiagnosticState { it.apply { silencedUntilRt = ts; isAlarmSilenced = sirenLockoutUseCase.isLockedOut(); pulse = timeProvider.elapsedRealtime } } 
                } 
            }
            launch {
                isMaliAnomalyFlow.flatMapLatest { isAnomaly ->
                    val interval = if (isAnomaly) HUD_STATE_SAMPLE_THROTTLED_MS else HUD_STATE_SAMPLE_MS
                    repository.signalingMetrics.sample(interval)
                }.collect { metrics ->
                    updateDiagnosticState { it.apply { 
                        signalingMetrics = metrics
                        pulse = timeProvider.elapsedRealtime 
                    } }
                }
            }
            launch(Dispatchers.IO) { 
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
    }

    fun onEvent(event: UiEvent) { 
        uiEventCoordinator.handleEvent(event, _uiState.value, viewModelScope, { updateState(it) }, { updateKinematicState(it) }, { updateDiagnosticState(it) }, { replayCursorRequest.value = it }, { viewModelScope.launch { _cameraActions.emit(it) } }, { viewModelScope.launch { _uiEffects.emit(it) } }) 
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { 
        _uiState.update { current -> 
            val next = update(current)
            if (next.session.hydrationLevel != current.session.hydrationLevel) {
                Timber.d("STATE_TRACE: Hydration level transitioned: ${current.session.hydrationLevel} -> ${next.session.hydrationLevel}")
            }
            next
        } 
    }

    private fun updateKinematicState(update: (KinematicState) -> KinematicState) { 
        _kinematicState.update { current -> 
            val nextIdx = (kinBufIdx + 1) % 2
            val next = kinBuffers[nextIdx]
            next.copyFrom(current)
            update(next)
            kinBufIdx = nextIdx
            next
        } 
    }

    private fun updateDiagnosticState(update: (DiagnosticState) -> DiagnosticState) { 
        _diagnosticState.update { current -> 
            val nextIdx = (diagBufIdx + 1) % 2
            val next = diagBuffers[nextIdx]
            next.copyFrom(current)
            update(next)
            diagBufIdx = nextIdx
            next
        } 
    }

    private fun startGlobalTimer() { 
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) { 
            while (true) { 
                val nowRt = timeProvider.elapsedRealtime
                if (_uiState.value.isInitialized && _uiState.value.session.appMode != null) {
                    repository.sendCommand(UiCommand.SyncRequest)
                }
                val lastActivity = repository.lastRemoteActivityTs.value
                val isPeerActive = lastActivity > 0 && (nowRt - lastActivity) < TELEMETRY_UI_STALE_THRESHOLD_MS
                if (_uiState.value.session.isPeerActive != isPeerActive) {
                    updateState { it.copy(session = it.session.copy(isPeerActive = isPeerActive)) }
                }
                _systemPulseRt.value = nowRt
                delay(if (_uiState.value.session.permissions.performanceTier == PerformanceTier.STAGGERED) 5000L else 2000L) 
            } 
        } 
    }

    private fun applyInitialSettings(initial: InitialSettings) {
        val restoredMode = if (initial.isRestorationDisabled) null else initial.appMode
        val restoredActive = if (initial.isRestorationDisabled) false else initial.isSystemActive
        updateState { it.copy(
            settings = it.settings.copy(
                deviceId = initial.deviceId,
                viewerId = initial.viewerId,
                relayUrl = initial.relayUrl,
                isIdentitySanitized = initial.identitySanitized,
                alertSettings = initial.alertSettings,
                draftSettings = initial.draftSettings ?: it.settings.draftSettings,
                lastAlarmAckTs = initial.lastAlarmAckTs
            ),
            session = it.session.copy(
                appMode = restoredMode,
                isSystemActive = restoredActive,
                appStartTime = initial.appStartTime
            ),
            simulation = it.simulation.copy(isRestorationDisabled = initial.isRestorationDisabled)
        ) }
    }

    fun fullInitialization(context: Context) { 
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { 
            val nextStartTime = settingsUseCase.fullInitialization(context)
            updateState { it.copy(session = it.session.copy(appStartTime = nextStartTime)) } 
        } 
    }

    fun clearTrails() { 
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { 
            repository.clearTrails()
            addPersistentLog("system", "Trails cleared by user", isImportant = true) 
        } 
    }

    fun addPersistentLog(type: String, message: String, isImportant: Boolean = false, isSpecial: Boolean = false, specialColor: Int? = null) { 
        repository.addLog(LogEntry(localId = UUID.randomUUID().toString(), timestamp = timeProvider.currentTimeMillis, message = message, type = type.uppercase(), isImportant = isImportant, isSpecial = isSpecial, specialColor = specialColor, role = _uiState.value.session.appMode ?: "system")) 
    }
}
