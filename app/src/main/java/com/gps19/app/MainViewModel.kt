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
 * Oct.5.6:
 * - Issue #1328: Phase 2 - UI Performance Hardening. Refactored state mapping 
 *   to allow granular binding in MainAppContent. Added isRedScreenVisible 
 *   and isAlarmSilenced to hudHealthState to decouple AlarmOverlay from root 
 *   DiagnosticState collection.
 * Oct.4.5:
 * - Issue #1425: Unified Clock Authority. Migrated HUD and Map freshness 
 *   calculations to monotonic time (systemPulseRt) to prevent UI jitter during 
 *   clock syncs. Refactored mapMapViewState to use monotonic fix age.
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
) : ViewModel() {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "MainViewModel Coroutine Exception")
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _cameraActions = MutableSharedFlow<CameraAction>(extraBufferCapacity = 16)
    val cameraActions: SharedFlow<CameraAction> = _cameraActions.asSharedFlow()

    private val _uiEffects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 64)
    val uiEffects: SharedFlow<UiEffect> = _uiEffects.asSharedFlow()

    // Coordinate Smoothing State (Issue #1290)
    private var sTrkLat = 0.0; private var sTrkLng = 0.0
    private var sVwrLat = 0.0; private var sVwrLng = 0.0

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

    private val _systemPulseRt = MutableStateFlow(timeProvider.elapsedRealtime())
    val systemPulseRt: StateFlow<Long> = _systemPulseRt.asStateFlow()

    private val _rtt = MutableStateFlow(0)
    val rtt: StateFlow<Int> = _rtt.asStateFlow()

    private val _remoteSignal = MutableStateFlow(0)
    val remoteSignal: StateFlow<Int> = _remoteSignal.asStateFlow()

    private val _currentMa = MutableStateFlow(0)
    val currentMa: StateFlow<Int> = _currentMa.asStateFlow()

    private val _trackerState = MutableStateFlow(TrackerState.UNKNOWN)
    val trackerState: StateFlow<TrackerState> = _trackerState.asStateFlow()

    private val _trackerMaxTemp = MutableStateFlow(0.0)
    val trackerMaxTemp: StateFlow<Double> = _trackerMaxTemp.asStateFlow()

    private val _gpsIndexData = MutableStateFlow(GpsIndexData(0.0, 0.0, 0.0, 0.0))
    val gpsIndexData: StateFlow<GpsIndexData> = _gpsIndexData.asStateFlow()

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

    val viewerTrailFlow: StateFlow<List<TrailPoint>> = repository.viewerTrailFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerTrailSegments: StateFlow<List<MapTrailSegment>> = trackerTrailFlow
        .map { trail -> computeTrailSegments(trail, BrandJd.toArgb()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewerTrailSegments: StateFlow<List<MapTrailSegment>> = viewerTrailFlow
        .map { trail -> computeTrailSegments(trail, ViewerCyan.toArgb()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardState: StateFlow<DashboardState> = combine(
        combine(
            _uiState.map { it.session.appMode }.distinctUntilChanged(),
            _kinematicState,
            _diagnosticState
        ) { mode, kin, diag -> Triple(mode ?: "tracker", kin, diag) },
        _systemPulseRt,
        _trackerState,
        _trackerMaxTemp
    ) { (mode, kin, diag), pulseRt, state, tMax ->
        val isUltra = if (mode == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        val conn = mapDashboardConnectivity(mode, diag, pulseRt)
        val tel = mapDashboardTelemetry(mode, kin, pulseRt, state, isUltra)
        val health = mapDashboardHealth(mode, kin, diag, diag.battery.temp, tMax, pulseRt)
        DashboardState(conn, tel, health)
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    val hudConnectivityState: StateFlow<HudConnectivityState> = combine(
        combine(
            _uiState.map { it.session }.distinctUntilChanged(),
            _uiState.map { it.settings }.distinctUntilChanged()
        ) { session, settings -> session to settings },
        _diagnosticState,
        _rtt,
        _remoteSignal,
        _systemPulseRt
    ) { (session, settings), diag, rtt, sig, pulseRt ->
        mapHudConnectivity(
            session.appMode, 
            settings.deviceId, 
            settings.viewerId, 
            session.isSystemActive, 
            settings.isSafeMode, 
            session.permissions.performanceTier == PerformanceTier.STAGGERED, 
            diag, rtt, sig, pulseRt
        )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudConnectivityState())

    val hudTelemetryState: StateFlow<HudTelemetryState> = combine(
        _uiState.map { it.session.appMode }.distinctUntilChanged(),
        _kinematicState,
        _systemPulseRt, 
        _trackerState
    ) { mode, kin, pulseRt, state ->
        val m = mode ?: "tracker"
        val isUltra = if (m == "viewer") kin.trackerHealth.isUltraLongStationary else kin.localHealth.isUltraLongStationary
        mapHudTelemetry(m, kin, pulseRt, state, isUltra)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudTelemetryState())

    val hudHealthState: StateFlow<HudHealthState> = combine(
        _diagnosticState,
        _systemPulseRt, 
        _kinematicState.map { it.localHealth.isMaliAnomaly }.distinctUntilChanged()
    ) { diag, pulseRt, isMali ->
        mapHudHealth(diag, pulseRt, isMali)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudHealthState())

    val mapViewState: StateFlow<MapViewState> = combine(
        combine(
            _uiState.map { it.session.appMode }.distinctUntilChanged(),
            _uiState.map { it.session.hydrationLevel }.distinctUntilChanged(),
            _uiState.map { it.spatial }.distinctUntilChanged(),
            _kinematicState
        ) { mode, hydration, spatial, kin ->
            FourParts(mode, hydration, spatial, kin)
        },
        _systemPulseRt,
        trackerTrailSegments,
        viewerTrailSegments,
        repository.violationsFlow.distinctUntilChanged()
    ) { parts, pulseRt, trkSegs, vwrSegs, vios ->
        mapMapViewState(
            mode = parts.mode, 
            hydration = parts.hydration, 
            spatial = parts.spatial, 
            kin = parts.kin, 
            pulseRt = pulseRt, 
            trkSegs = trkSegs, 
            vwrSegs = vwrSegs, 
            vios = vios
        )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapViewState())

    private data class FourParts(val mode: String?, val hydration: Int, val spatial: SpatialUiState, val kin: KinematicState)

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
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            // 1. Settings & Session
            launch {
                stateSubscriptionUseCase.observeRepositorySettings().collect { update ->
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
            }

            // 2. Connectivity & Diagnostics
            launch {
                stateSubscriptionUseCase.observeInternetStatus().collect { online -> 
                    updateDiagnosticState { current ->
                        current.apply {
                            connectivity.isLocalOnline = online
                            pulse = timeProvider.elapsedRealtime()
                        }
                    }
                }
            }

            launch {
                stateSubscriptionUseCase.observeConnectivityBasics().collect { update ->
                    _rtt.value = update.lastRtt
                    updateDiagnosticState { current -> 
                        current.apply {
                            connectivity.isRelayConnected = update.isRelayConnected
                            connectivity.lastRemoteActivityTs = update.lastRemoteActivityTs
                            recoveryCount = update.recoveryCount
                            cumulativeRecoveryBlackoutMs = update.cumulativeRecoveryBlackoutMs
                            pulse = timeProvider.elapsedRealtime()
                        }
                    }
                }
            }

            launch {
                stateSubscriptionUseCase.observeIntegrityUpdates().collect { update ->
                    updateDiagnosticState { current -> 
                        current.activeAlarms = update.activeAlarms
                        
                        if (update.activeAlarms.any { !it.isResolved && !it.isSirenDisabled } && 
                            _uiState.value.session.isSystemActive && 
                            _uiState.value.session.appMode == "viewer") {
                            if (!current.isRedScreenVisible) {
                                current.isRedScreenVisible = true
                            }
                        }

                        current.pulse = timeProvider.elapsedRealtime()
                        current
                    }
                }
            }

            launch {
                stateSubscriptionUseCase.observeBatteryStatus().collect { status -> 
                    updateDiagnosticState { current -> 
                        current.battery.level = status.level
                        current.battery.temp = status.temp
                        current.apply { pulse = timeProvider.elapsedRealtime() }
                    } 
                    _currentMa.value = status.level
                }
            }

            // 3. Telemetry (Local & Remote)
            launch {
                repository.localLocation.collect { update ->
                    val nowMs = timeProvider.currentTimeMillis()
                    val nowRt = timeProvider.elapsedRealtime()
                    updateKinematicState { current ->
                        telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, _uiState.value.session.appStartTime)
                        telemetryUseCase.mapHealthFromUpdate(update, current.localHealth)
                        current.apply { pulse = nowRt }
                    }
                    _gpsIndexData.value = GpsIndexData(update.integrity.snrIdx, update.integrity.satsUsed.toDouble(), update.integrity.satsView.toDouble(), 0.0)
                    
                    // Issue #1414: Update local tracker state for local dashboard.
                    if (_uiState.value.session.appMode == "tracker") {
                        _trackerState.value = update.trackerState
                    }
                }
            }

            launch {
                remoteStatusRepository.remoteStatus.collect { status ->
                    val nowMs = timeProvider.currentTimeMillis()
                    val appStartTime = _uiState.value.session.appStartTime

                    _remoteSignal.value = remoteStatusRepository.peerSignal.value
                    
                    // Only update from remote if we are NOT the primary tracker.
                    if (_uiState.value.session.appMode != "tracker") {
                        _trackerState.value = status.trackerState
                    }
                    
                    _trackerMaxTemp.value = status.maxTemp
                    updateKinematicState { current ->
                        telemetryUseCase.mapTrackerLocation(status, current.trackerLocation, nowMs, appStartTime)
                        telemetryUseCase.mapHealthFromUpdate(status, current.trackerHealth)
                        current.apply { pulse = timeProvider.elapsedRealtime() }
                    }
                    updateDiagnosticState { current ->
                        current.trackerBattery.level = status.battery
                        current.trackerBattery.temp = status.temp
                        current.trackerIsGnssThrottled = status.isGnssThrottled
                        current.pulse = timeProvider.elapsedRealtime()
                        current
                    }
                }
            }

            // 4. Audio & Alarms
            launch {
                audioSynthesizer.isSirenPlaying.collect { playing ->
                    updateDiagnosticState { it.apply { isSirenPlaying = playing } }
                    
                    if (playing && 
                        _uiState.value.session.isSystemActive && 
                        _uiState.value.session.appMode == "viewer") {
                        updateDiagnosticState { it.apply { isRedScreenVisible = true } }
                    }
                }
            }

            launch {
                sirenLockoutUseCase.silencedUntilRt.collect { ts -> 
                    updateDiagnosticState { it.apply { 
                        silencedUntilRt = ts
                        isAlarmSilenced = sirenLockoutUseCase.isLockedOut()
                        pulse = timeProvider.elapsedRealtime()
                    } }
                }
            }
            
            // 5. Background Polling (Permissions)
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

    /**
     * Issue #1202: Unified UI event routing.
     * Delegates to UiEventCoordinator for procedural logic and domain orchestration.
     */
    fun onEvent(event: UiEvent) {
        uiEventCoordinator.handleEvent(
            event = event,
            currentState = _uiState.value,
            scope = viewModelScope,
            onStateUpdate = { update: (MainUiState) -> MainUiState -> updateState(update) },
            onKinematicUpdate = { update: (KinematicState) -> KinematicState -> updateKinematicState(update) },
            onDiagnosticUpdate = { update: (DiagnosticState) -> DiagnosticState -> updateDiagnosticState(update) },
            onReplayRequest = { ts: Long? -> replayCursorRequest.value = ts },
            onCameraAction = { action: CameraAction -> 
                viewModelScope.launch { _cameraActions.emit(action) }
            },
            onUiEffect = { effect: UiEffect ->
                viewModelScope.launch { _uiEffects.emit(effect) }
            }
        )
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
    private fun updateKinematicState(update: (KinematicState) -> KinematicState) { _kinematicState.update { current -> update(current) } }
    private fun updateDiagnosticState(update: (DiagnosticState) -> DiagnosticState) { _diagnosticState.update { current -> update(current) } }
    private fun updateNavigation(update: (NavigationState) -> NavigationState) { updateState { it.copy(navigation = update(it.navigation)) } }

    private fun startGlobalTimer() {
        viewModelScope.launch(Dispatchers.Main.immediate + uiExceptionHandler) {
            while (true) {
                val nowRt = timeProvider.elapsedRealtime()

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
        updateState { it.copy(
            settings = it.settings.copy(
                deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl,
                isIdentitySanitized = initial.identitySanitized, alertSettings = initial.alertSettings,
                draftSettings = initial.draftSettings ?: it.settings.draftSettings,
                lastAlarmAckTs = initial.lastAlarmAckTs
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

    fun clearTrails() {
        viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
            repository.clearTrails()
            addPersistentLog("system", "Trails cleared by user", isImportant = true)
        }
    }

    fun addPersistentLog(type: String, message: String, isImportant: Boolean = false, isSpecial: Boolean = false, specialColor: Int? = null) {
        val entry = LogEntry(
            localId = UUID.randomUUID().toString(), timestamp = timeProvider.currentTimeMillis(),
            message = message, type = type.uppercase(), isImportant = isImportant,
            isSpecial = isSpecial, specialColor = specialColor, role = _uiState.value.session.appMode ?: "system"
        )
        repository.addLog(entry)
    }

    // --- MAPPING LOGIC (Issue #1290) ---

    private fun mapDashboardConnectivity(
        appMode: String?,
        diag: DiagnosticState,
        nowRt: Long
    ): DashboardConnectivityState {
        val isViewer = appMode == "viewer"
        val activeStats = if (isViewer) diag.trackerStats else diag.stats
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs

        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS
        
        val watchdogSec = if (isViewer && lastSeenTs > 0) {
            val remaining = (WATCH_TIMEOUT_MS - (nowRt - lastSeenTs)) / 1000
            maxOf(0L, remaining)
        } else 0L

        return DashboardConnectivityState(
            lastSeenTs = lastSeenTs,
            watchdogOk = isLocalServiceAlive,
            watchdogCountdownSec = if (isViewer) watchdogSec else 0L,
            totalUptimeMs = activeStats.uptimeMs,
            sessionMs = if (activeStats.lastConnTs > 0) activeStats.sessionConnectedMs else 0L,
            sinceConnMs = if (activeStats.lastConnTs > 0) (nowRt - activeStats.lastConnTs) else 0L,
            sinceDiscoMs = if (activeStats.lastDiscTs > 0) (nowRt - activeStats.lastDiscTs) else 0L,
            totalDropMs = activeStats.totalDropMs,
            maxDropMs = activeStats.maxDropMs,
            engineVersion = BuildConfig.VERSION_NAME,
            netInterface = diag.connectivity.netInterface,
            systemPulse = nowRt,
            isLocalOnline = diag.connectivity.isLocalOnline,
            isRelayConnected = diag.connectivity.isRelayConnected
        )
    }

    private fun mapDashboardTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): DashboardTelemetryState {
        val isViewer = appMode == "viewer"
        val loc = if (isViewer) kinematicState.trackerLocation else kinematicState.localLocation
        
        val fixAgeRt = if (loc.kinetic.rt > 0) nowRt - loc.kinetic.rt else Long.MAX_VALUE
        val isGpsActive = fixAgeRt < GPS_UI_FAIL_THRESHOLD_MS && loc.kinetic.gpsTs > 0
        
        val telemetryAge = if (kinematicState.pulse > 0) nowRt - kinematicState.pulse else Long.MAX_VALUE
        val isTelemetryFresh = telemetryAge < TELEMETRY_UI_STALE_THRESHOLD_MS

        val gnss = loc.integrity.gnssDetail
        val avgCn0 = gnss?.satellites?.map { it.cn0 }?.safeAverage() ?: 0.0

        return DashboardTelemetryState(
            lat = if (isGpsActive) loc.kinetic.lat else 0.0,
            lng = if (isGpsActive) loc.kinetic.lng else 0.0,
            gpsSpeedMps = loc.kinetic.speed,
            trackerAccuracy = loc.kinetic.accuracy,
            trackerMaxAcc = if (loc.kinetic.maxAccuracy > 0) loc.kinetic.maxAccuracy else loc.kinetic.accuracy,
            viewerAccuracy = if (appMode == "tracker") 0.0 else kinematicState.localLocation.kinetic.accuracy,
            viewerMaxAcc = if (appMode == "tracker") 0.0 else (if(kinematicState.localLocation.kinetic.maxAccuracy > 0) kinematicState.localLocation.kinetic.maxAccuracy else kinematicState.localLocation.kinetic.accuracy),
            satsUsed = loc.integrity.satsUsed,
            satsView = loc.integrity.satsView,
            snr = avgCn0,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            isGpsFresh = isGpsActive,
            isTelemetryFresh = isTelemetryFresh,
            isLocationPending = if (isViewer) kinematicState.trackerHealth.isLocationPending else kinematicState.localHealth.isLocationPending,
            locationPendingReason = if (isViewer) kinematicState.trackerHealth.locationPendingReason else kinematicState.localHealth.locationPendingReason,
            trackerState = trackerState,
            status = loc.status,
            tamperReason = if (isViewer) kinematicState.trackerHealth.tamperNote else kinematicState.localHealth.tamperNote,
            isUltraLongStationary = isUltra,
            systemPulse = nowRt,
            gpsTs = loc.kinetic.gpsTs
        )
    }

    private fun mapDashboardHealth(
        appMode: String?,
        kinematicState: KinematicState,
        diag: DiagnosticState,
        localMaxTemp: Double,
        trackerMaxTemp: Double,
        nowRt: Long
    ): DashboardHealthState {
        val isViewer = appMode == "viewer"
        val health = if (isViewer) kinematicState.trackerHealth else kinematicState.localHealth

        return DashboardHealthState(
            batteryLevel = if (isViewer) diag.trackerBattery.level else diag.battery.level,
            trackerTemp = diag.trackerBattery.temp,
            trackerMaxTemp = trackerMaxTemp,
            viewerTemp = diag.battery.temp,
            viewerMaxTemp = localMaxTemp,
            vibration = health.vibration,
            heading = health.heading,
            tilt = health.tiltDegrees,
            acousticDb = health.acousticDb,
            baroAlt = health.baroAlt,
            lux = health.lux,
            isNear = health.isNear,
            proximityCm = health.proximityCm,
            proximityDebounceMs = health.proximityDebounceMs,
            rollingVibration = health.vibrationRollingSum,
            kineticEnergy = health.kineticEnergy,
            peakShock = health.peakVibrationShock,
            luxBaseline = health.luxBaseline,
            acousticFloorDb = health.acousticFloorDb,
            vibrationFloor = health.adaptiveVibrationFloor,
            isMicPending = health.micPending,
            isPowerTamper = health.isPowerTamper,
            violationUptimeMs = health.violationUptimeMs,
            violationPercentage = health.violationPercentage,
            isPowerSaveMode = health.isPowerSaveMode,
            standbyBucket = health.standbyBucket,
            netInterface = health.netInterface,
            isStorageLow = health.isStorageLow,
            isStorageCritical = health.isStorageCritical,
            isBatterySteepDischarge = health.isBatterySteepDischarge,
            isCoolingModeActive = health.isCoolingModeActive,
            trackerCurrentMa = health.currentMa,
            isBatteryLow = health.isBatteryLow,
            isBatteryCritical = health.isBatteryCritical,
            cpuLoad = health.cpuLoad,
            ioWait = health.ioWait,
            maxIoLatency = health.maxIoLatency,
            isSilentFailure = health.isSilentFailure,
            isMaliAnomaly = health.isMaliAnomaly,
            isGnssThrottled = health.isGnssThrottled,
            lastEnergyDeltaMa = health.lastEnergyDeltaMa,
            lastEnergyDeltaTemp = health.lastEnergyDeltaTemp,
            lastEnergyDurationMs = health.lastEnergyDurationMs,
            systemPulse = nowRt,
            thermalHeadroom = health.thermalHeadroom,
            heapAllocatedMb = health.heapAllocatedMb
        )
    }

    private fun mapHudConnectivity(
        appMode: String?,
        deviceId: String,
        viewerId: String,
        isSystemActive: Boolean,
        isSafeMode: Boolean,
        isStaggered: Boolean,
        diag: DiagnosticState,
        rtt: Int,
        remoteSignal: Int,
        nowRt: Long
    ): HudConnectivityState {
        val lastSeenTs = diag.connectivity.lastRemoteActivityTs
        
        val isTelemetryFresh = if (lastSeenTs > 0) {
            (nowRt - lastSeenTs) < TELEMETRY_UI_STALE_THRESHOLD_MS
        } else false

        val commIndex = if (isSystemActive && diag.connectivity.isRelayConnected) {
            TelemetryUtils.calculateCommIndex(rtt, 10, 10)
        } else 0

        val remoteCommIndex = if (appMode == "viewer" && isTelemetryFresh) {
            TelemetryUtils.calculateCommIndex(rtt, remoteSignal, 10)
        } else 0

        val isDataHealthy = (appMode == "viewer") && 
                            isTelemetryFresh &&
                            diag.connectivity.isLocalOnline && 
                            diag.connectivity.isRelayConnected

        val isLocalServiceAlive = (nowRt - diag.pulse) < TELEMETRY_UI_STALE_THRESHOLD_MS

        val throttled = if (appMode == "viewer") diag.trackerIsGnssThrottled else diag.isGnssThrottled

        return HudConnectivityState(
            appMode = appMode,
            isInternet = diag.connectivity.isLocalOnline,
            isRelayConnected = diag.connectivity.isRelayConnected,
            isTelemetryFresh = isTelemetryFresh,
            isDataHealthy = isDataHealthy,
            commIndex = commIndex,
            remoteCommIndex = remoteCommIndex,
            trackerId = deviceId,
            viewerId = viewerId,
            watchdogOk = isLocalServiceAlive,
            rtt = rtt,
            remoteSignal = remoteSignal,
            isSystemActive = isSystemActive,
            isSafeMode = isSafeMode,
            isStaggered = isStaggered,
            isGnssThrottled = throttled,
            systemPulse = nowRt
        )
    }

    private fun mapHudTelemetry(
        appMode: String?,
        kinematicState: KinematicState,
        nowRt: Long,
        trackerState: TrackerState,
        isUltra: Boolean
    ): HudTelemetryState {
        val loc = if (appMode == "viewer") kinematicState.trackerLocation else kinematicState.localLocation
        val isGpsFresh = (nowRt - loc.kinetic.rt) < GPS_UI_FAIL_THRESHOLD_MS && loc.kinetic.gpsTs > 0

        return HudTelemetryState(
            isLocalGpsActive = if (appMode == "tracker") isGpsFresh else (nowRt - kinematicState.localLocation.kinetic.rt < GPS_UI_FAIL_THRESHOLD_MS && kinematicState.localLocation.kinetic.gpsTs > 0),
            isGpsFresh = isGpsFresh,
            speedMps = (if (appMode == "viewer") kinematicState.trackerLocation.kinetic.speed else 0.0).toFloat(),
            trackerAccuracy = kinematicState.trackerLocation.kinetic.accuracy.toFloat(),
            maxTrackerAccuracy = kinematicState.trackerLocation.kinetic.maxAccuracy.toFloat(),
            viewerAccuracy = (if (kinematicState.localLocation.kinetic.lat != 0.0) kinematicState.localLocation.kinetic.accuracy.toFloat() else 0f),
            maxViewerAccuracy = kinematicState.localLocation.kinetic.maxAccuracy.toFloat(),
            satsUsed = kinematicState.trackerLocation.integrity.satsUsed,
            satsView = kinematicState.trackerLocation.integrity.satsView,
            viewerSatsUsed = kinematicState.localLocation.integrity.satsUsed,
            viewerSatsView = kinematicState.localLocation.integrity.satsView,
            distToHome = kinematicState.distanceTrackerToHome,
            distToViewer = kinematicState.distanceTrackerToViewer,
            lastGpsTs = if (loc.kinetic.gpsTs > 0) loc.kinetic.rt else 0L,
            viewerGpsTs = if (kinematicState.localLocation.kinetic.gpsTs > 0) kinematicState.localLocation.kinetic.rt else 0L,
            trackerState = trackerState,
            isTrackerLocPending = kinematicState.trackerHealth.isLocationPending,
            locationPendingReason = kinematicState.trackerHealth.locationPendingReason,
            isViewerLocPending = kinematicState.localHealth.isLocationPending,
            viewerLocPendingReason = kinematicState.localHealth.locationPendingReason,
            isUltraLongStationary = isUltra,
            systemPulse = nowRt
        )
    }

    private fun mapHudHealth(
        diag: DiagnosticState,
        nowRt: Long,
        isMaliAnomaly: Boolean
    ): HudHealthState {
        val rawPulse = diag.connectivity.lastRemoteActivityTs
        val age = if (rawPulse > 0) nowRt - rawPulse else Long.MAX_VALUE
        val progressValue = if (rawPulse > 0) {
            maxOf(0f, minOf(1f, (TELEMETRY_UI_STALE_THRESHOLD_MS - age).toFloat() / TELEMETRY_UI_STALE_THRESHOLD_MS))
        } else 0f

        return HudHealthState(
            battery = diag.battery.level,
            remoteBattery = diag.trackerBattery.level,
            isCharging = diag.battery.isChargingStable,
            remoteCharging = diag.trackerBattery.isChargingStable,
            trackerTemp = diag.trackerBattery.temp.toFloat(),
            viewerTemp = diag.battery.temp.toFloat(),
            hasActiveAlarms = diag.activeAlarms.any { !it.isResolved },
            isRedScreenSuppressed = (diag.activeAlarms.any { !it.isResolved } && !diag.isRedScreenVisible),
            isRedScreenVisible = diag.isRedScreenVisible,
            isAlarmSilenced = diag.isAlarmSilenced,
            isSirenPlaying = diag.isSirenPlaying,
            activeAlarms = diag.activeAlarms,
            progressPulse = progressValue,
            systemPulse = nowRt,
            isMaliAnomaly = isMaliAnomaly
        )
    }

    private fun mapMapViewState(
        mode: String?,
        hydration: Int,
        spatial: SpatialUiState,
        kin: KinematicState,
        pulseRt: Long,
        trkSegs: List<MapTrailSegment>,
        vwrSegs: List<MapTrailSegment>,
        vios: List<ViolationPoint>
    ): MapViewState {
        val m = mode ?: "tracker"
        val pulse = timeProvider.currentTimeMillis()
        val loc = if (m == "tracker") kin.localLocation else kin.trackerLocation
        val tLat = loc.kinetic.lat
        val tLng = loc.kinetic.lng
        val tTs = loc.kinetic.gpsTs
        val tTel = loc.ts
        val vLat = if (m == "viewer") kin.localLocation.kinetic.lat else 0.0
        val vLng = if (m == "viewer") kin.localLocation.kinetic.lng else 0.0
        
        if (PhysicsUtils.isValidLocation(tLat, tLng)) {
            val alpha = if (kin.localLocation.kinetic.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            if (sTrkLat == 0.0 || PhysicsUtils.calculateDistance(sTrkLat, sTrkLng, tLat, tLng) > 100.0) { 
                sTrkLat = tLat; sTrkLng = tLng 
            } else { 
                sTrkLat = PhysicsUtils.smoothCoordinate(sTrkLat, tLat, alpha)
                sTrkLng = PhysicsUtils.smoothCoordinate(sTrkLng, tLng, alpha) 
            }
        }
        
        val fixAgeRt = if (loc.kinetic.rt > 0) pulseRt - loc.kinetic.rt else Long.MAX_VALUE
        val isTrackerFresh = fixAgeRt < GPS_UI_FAIL_THRESHOLD_MS && tTs > 0

        return MapViewState(
            appMode = m, hydrationLevel = hydration, isMapButtonsVisible = spatial.isMapButtonsVisible, isFenceVisible = spatial.isFenceVisible, 
            geofenceMode = spatial.geofenceMode, isViolationsVisible = spatial.isViolationsVisible, isGeofenceViolationsVisible = spatial.isGeofenceViolationsVisible, 
            maxDistance = spatial.maxDistance, isMapLocked = spatial.isMapLocked, mapFollowMode = spatial.mapFollowMode,
            isAnchorLocked = loc.integrity.isAnchorLocked,
            homePoints = spatial.homePoints,
            trackerLat = tLat, trackerLng = tLng, trackerGpsTs = tTs, trackerTelemetryTs = tTel,
            viewerLat = vLat, viewerLng = vLng, systemPulse = pulse, systemPulseRt = pulseRt,
            trackerSegments = trkSegs, viewerSegments = vwrSegs, violations = vios,
            isTrackerFresh = isTrackerFresh,
            isTrackerValid = PhysicsUtils.isValidLocation(tLat, tLng),
            smoothedTrackerLat = sTrkLat, smoothedTrackerLng = sTrkLng
        )
    }

    private fun computeTrailSegments(trailPoints: List<TrailPoint>, color: Int): List<MapTrailSegment> {
        if (trailPoints.isEmpty()) return emptyList()
        val now = timeProvider.currentTimeMillis()
        val segments = mutableListOf<MapTrailSegment>()
        var currentPoints = mutableListOf<org.osmdroid.util.GeoPoint>()
        var currentIsStale: Boolean? = null
        val slateGray = 0xFF64748B.toInt() // Slate500 equivalent

        trailPoints.forEach { pt ->
            val age = now - pt.timestamp
            val isStale = age > 35000L // R338 requirement
            
            if (currentIsStale != null && isStale != currentIsStale) {
                if (currentPoints.isNotEmpty()) {
                    segments.add(MapTrailSegment(currentPoints.toList(), if (currentIsStale == true) slateGray else color, currentPoints.hashCode()))
                    // Connect segments by starting new one with last point of previous
                    val last = currentPoints.last()
                    currentPoints = mutableListOf(last)
                }
            }
            
            currentPoints.add(pt.toGeoPoint())
            currentIsStale = isStale
        }
        
        if (currentPoints.isNotEmpty()) {
            segments.add(MapTrailSegment(currentPoints.toList(), if (currentIsStale == true) slateGray else color, currentPoints.hashCode()))
        }
        
        return segments
    }
}
