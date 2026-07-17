package com.gps19.app

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

/**
 * MainViewModel: Manages UI state and orchestrates data flow.
 * July.17.02:
 * - Persisting isSystemActive state to prevent unintended engine starts on landing page.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Use localHealth and trackerHealth.
 */
class MainViewModel(
    val repository: MainRepository,
    private val logManager: LogManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val homePointUseCase: HomePointUseCase,
    private val dashboardUseCase: DashboardUseCase,
    private val navigationUseCase: NavigationUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val telemetryUseCase: TelemetryUseCase,
    private val stateSubscriptionUseCase: StateSubscriptionUseCase,
    private val sessionUseCase: SessionUseCase,
    private val behaviorUseCase: BehaviorUseCase,
    private val alertUseCase: AlertUseCase,
    private val mapUseCase: MapUseCase,
    val timeProvider: TimeProvider,
    private val context: Context
) : ViewModel() {

    private val uiExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "ViewModel Coroutine Exception")
        addPersistentLog(type = "error", message = "UI ERROR: ${throwable.localizedMessage}", isImportant = true)
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _systemPulse = MutableStateFlow(timeProvider.currentTimeMillis())
    val systemPulse: StateFlow<Long> = _systemPulse.asStateFlow()

    private val _systemPulseRealtime = MutableStateFlow(timeProvider.elapsedRealtime())
    val systemPulseRealtime: StateFlow<Long> = _systemPulseRealtime.asStateFlow()

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

    private val _redScreenVisible = MutableStateFlow(false)
    val redScreenVisible: StateFlow<Boolean> = _redScreenVisible.asStateFlow()

    private val _localMaxTemp = MutableStateFlow(0.0)
    val localMaxTemp: StateFlow<Double> = _localMaxTemp.asStateFlow()

    private val _trackerMaxTemp = MutableStateFlow(0.0)
    val trackerMaxTemp: StateFlow<Double> = _trackerMaxTemp.asStateFlow()

    val activeGnssDetail: StateFlow<GnssDetail?> = _uiState.combine(_gnssDetail) { state, localDetail ->
        if (state.appMode == "viewer") state.trackerLocation.gnssDetail else localDetail
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SHARING_TIMEOUT_MS), null)

    val dashboardState: StateFlow<DashboardState> = combine(
        _uiState, _systemPulse, _trackerState, _localMaxTemp, _trackerMaxTemp
    ) { state, pulse, trkState, lMax, tMax ->
        dashboardUseCase.computeDashboardState(state, pulse, trkState, lMax, tMax)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SHARING_TIMEOUT_MS), DashboardState())

    val eventLogsFlow: StateFlow<List<LogEntry>> = repository.eventLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SHARING_TIMEOUT_MS), emptyList())

    val trackerTrailFlow: StateFlow<List<TrailPoint>> = repository.trackerTrailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SHARING_TIMEOUT_MS), emptyList())

    val viewerTrailFlow: StateFlow<List<TrailPoint>> = repository.viewerTrailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SHARING_TIMEOUT_MS), emptyList())

    val violationPointsFlow: Flow<List<ViolationPoint>> = repository.violationsFlow

    val history4MFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("4M")
    val history16MFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("16M")
    val history1HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("1H")
    val history4HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("4H")
    val history24HFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("24H")
    val history7DFlow: StateFlow<List<ConnectionPoint>> = stateSubscriptionUseCase.getHistoryFlow("7D")

    var appStartTime: Long = 0L
    private var autoSaveJob: Job? = null
    private var lastKnownAlarmTypes: Set<String> = emptySet()

    private var lastAlarmAckRealtime: Long = 0L

    init {
        viewModelScope.launch(uiExceptionHandler) {
            loadInitialData()
            startReactiveObservations()
            startGlobalTimer()
            stateSubscriptionUseCase.startHistoryObservations(viewModelScope)
        }
    }

    private fun startReactiveObservations() {
        stateSubscriptionUseCase.observeRepositorySettings()
            .distinctUntilChanged()
            .onEach { update ->
                updateState { it.copy(
                    deviceId = update.trackerId, viewerId = update.viewerId, relayUrl = update.relayUrl,
                    maxDistance = update.maxDistance, homePoints = update.homePoints, lastAlarmAckTs = update.lastAlarmAckTs,
                    appMode = update.appMode, isSystemActive = update.isSystemActive,
                    permissions = it.permissions.copy(isManualOverride = update.isXiaomiManualOverride)
                )}
            }.launchIn(viewModelScope)

        stateSubscriptionUseCase.observeConnectivityBasics()
            .distinctUntilChanged()
            .onEach { update ->
                _rtt.value = update.lastRtt
                updateState { it.copy(connectivity = it.connectivity.copy(isRelayConnected = update.isRelayConnected, lastRemoteActivityTs = update.lastRemoteActivityTs))}
            }.launchIn(viewModelScope)

        stateSubscriptionUseCase.observeIntegrityUpdates()
            .distinctUntilChanged()
            .onEach { update ->
                updateState { current -> current.copy(
                    localHealth = update.health,
                    connectivity = current.connectivity.copy(isLocalOnline = update.isLocalOnline),
                    trackerBattery = if (current.appMode == "tracker") current.trackerBattery.copy(level = update.batteryLevel, temp = update.batteryTemp, isCharging = update.isCharging, isChargingStable = update.isCharging) else current.trackerBattery,
                    activeAlarms = update.activeAlarms,
                    isNewViolationDetected = update.activeAlarmTypes.any { it !in lastKnownAlarmTypes }
                )}
                lastKnownAlarmTypes = update.activeAlarmTypes
                _localMaxTemp.value = update.maxTemp
                if (_uiState.value.appMode == "tracker") _trackerMaxTemp.value = update.maxTemp
            }.launchIn(viewModelScope)

        stateSubscriptionUseCase.observeInternetStatus()
            .distinctUntilChanged()
            .onEach { online -> updateState { it.copy(connectivity = it.connectivity.copy(isLocalOnline = online)) } }.launchIn(viewModelScope)
            
        stateSubscriptionUseCase.observeBatteryStatus()
            .distinctUntilChanged()
            .onEach { status -> 
                updateState { current -> current.copy(
                    battery = current.battery.copy(level = status.level, temp = status.temp, isCharging = status.isCharging, isChargingStable = status.isCharging),
                    trackerBattery = if (current.appMode == "tracker") current.trackerBattery.copy(level = status.level, temp = status.temp, isCharging = status.isCharging, isChargingStable = status.isCharging) else current.trackerBattery
                ) } 
                _currentMa.value = status.currentMa
            }.launchIn(viewModelScope)

        stateSubscriptionUseCase.observeGnssDetail().distinctUntilChanged().onEach { _gnssDetail.value = it }.launchIn(viewModelScope)
        stateSubscriptionUseCase.observeGpsIndex().distinctUntilChanged().onEach { _gpsIndexData.value = it }.launchIn(viewModelScope)

        viewModelScope.launch { repository.localLocation.collect { update -> update?.let { handleLocationUpdateInternal(update) } } }
        viewModelScope.launch { repository.trackerLocation.collect { update -> update?.let { handleLocationUpdateInternal(update) } } }
        viewModelScope.launch { repository.connectedViewers.collect { viewers -> updateState { it.copy(connectivity = it.connectivity.copy(connectedViewers = viewers)) } } }

        viewModelScope.launch(Dispatchers.IO) { 
            while(true) { 
                val newState = systemStatusProvider.getPermissionState()
                withContext(Dispatchers.Main) {
                    updateState { it.copy(permissions = newState) }
                }
                val refreshFast = _uiState.value.navigation.isPhoneSetupVisible || _uiState.value.navigation.isDiagnosticsVisible
                delay(if (refreshFast) PERMISSION_REFRESH_INTERVAL_FAST_MS else PERMISSION_REFRESH_INTERVAL_SLOW_MS) 
            } 
        }

        repository.identitySanitizedFlow.distinctUntilChanged().onEach { sanitized ->
            updateState { it.copy(isIdentitySanitized = sanitized) }
        }.launchIn(viewModelScope)
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
            is UiEvent.SetPendingMode -> updateNavigation { it.copy(pendingMode = event.mode) }
            is UiEvent.SetRedScreenVisible -> _redScreenVisible.value = event.visible
            is UiEvent.SetUiVisible -> {
                repository.sendCommand(UiCommand.UiVisibilityChanged(event.visible))
                if (!event.visible && _uiState.value.navigation.isSettingsOpen) commitDraft()
            }
            is UiEvent.DismissAlarms, is UiEvent.StopSiren -> handleAlarmEvent(event)
            is UiEvent.SetAppMode, is UiEvent.ConfirmStopTracking -> handleSystemEvent(event)
            is UiEvent.SetSystemActive -> { 
                addPersistentLog("user", "USER ACTION: System ${if (event.active) "ACTIVATED" else "DEACTIVATED"}", true)
                updateState { it.copy(isSystemActive = event.active) } 
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) {
                    sessionUseCase.setSystemActive(event.active)
                }
            }
            is UiEvent.ManualExit -> addPersistentLog("user", "USER ACTION: Manual navigation to background requested", true)
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
            
            is UiEvent.SetJammerSuspicion -> updateState { it.copy(localHealth = it.localHealth.copy(isJammer = event.isJammer)) }
            is UiEvent.SetSignalLoss -> updateState { it.copy(localHealth = it.localHealth.copy(signalLoss = event.isSignalLoss)) }

            is UiEvent.SetAlertSettings -> { 
                viewModelScope.launch(uiExceptionHandler) {
                    settingsUseCase.handleImmediateAlertUpdate(event.settings)
                    updateState { it.copy(alertSettings = event.settings, draftSettings = it.draftSettings.copy(alertSettings = event.settings)) }
                    addPersistentLog("user", "USER ACTION: Alert settings modified", true)
                }
            }
            is UiEvent.SetSirenType -> { 
                addPersistentLog("user", "USER ACTION: Siren type set to ${event.type}", true)
                viewModelScope.launch(uiExceptionHandler) { repository.saveString(MainRepository.SELECTED_SIREN_KEY, event.type); updateState { it.copy(selectedSirenType = event.type) } } 
            }
            is UiEvent.UpdateDraftDeviceId, is UiEvent.UpdateDraftViewerId, is UiEvent.UpdateDraftRelayUrl, 
            is UiEvent.UpdateDraftMaxDistance, is UiEvent.UpdateDraftAlertSettings, is UiEvent.UpdateDraftAlarmVolume, 
            is UiEvent.CommitSettings -> handleDraftEvent(event)
            is UiEvent.SetLogFilterShowDetails -> viewModelScope.launch { repository.updateLogFilters(details = event.show) }
            is UiEvent.SetLogFilterShowRecovered -> viewModelScope.launch { repository.updateLogFilters(recovered = event.show) }
            is UiEvent.RefreshPermissionStatus -> viewModelScope.launch(Dispatchers.IO) { 
                val newState = systemStatusProvider.getPermissionState(forceRefresh = true)
                withContext(Dispatchers.Main) { updateState { it.copy(permissions = newState) } }
            }
            is UiEvent.TriggerTestAlarm -> { addPersistentLog("user", "USER ACTION: Test alarm triggered", true); repository.sendCommand(UiCommand.TriggerTestAlarm) }
            is UiEvent.TriggerForensicTest -> { addPersistentLog("user", "USER ACTION: Forensic stress test triggered", true); repository.sendCommand(UiCommand.TriggerForensicTest) }
            is UiEvent.ToggleXiaomiManualOverride -> {
                val nextValue = !_uiState.value.permissions.isManualOverride
                updateState { it.copy(permissions = it.permissions.copy(isManualOverride = nextValue)) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { repository.saveBoolean(MainRepository.IS_XIAOMI_MANUAL_OVERRIDE_KEY, nextValue); addPersistentLog("user", "USER ACTION: Xiaomi manual override set to $nextValue", true) }
            }
            is UiEvent.DismissIdentitySanitization -> {
                updateState { it.copy(isIdentitySanitized = false) }
                viewModelScope.launch(Dispatchers.IO + uiExceptionHandler) { repository.saveBoolean(MainRepository.IDENTITY_SANITIZED_KEY, false) }
            }
            is UiEvent.BulkUpdateSettings -> {
                viewModelScope.launch(uiExceptionHandler) {
                    try {
                        settingsUseCase.bulkUpdateSettings(
                            deviceId = event.deviceId,
                            viewerId = event.viewerId,
                            relayUrl = event.relayUrl,
                            maxDistance = event.maxDistance,
                            alertSettings = event.alertSettings,
                            homePoints = event.homePoints
                        )
                        addPersistentLog("user", "USER ACTION: Bulk settings update applied", true)
                    } catch (e: IllegalArgumentException) {
                        val errMsg = "Bulk Update Rejected: ${e.message}"
                        Timber.e(e, errMsg)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                        addPersistentLog("error", errMsg, true)
                    } catch (e: Exception) {
                        Timber.e(e, "Bulk update failed")
                    }
                }
            }
            else -> {}
        }
    }

    private fun handleConfigEvent(event: UiEvent) {
        viewModelScope.launch(uiExceptionHandler) {
            when (event) {
                is UiEvent.SetDeviceId -> { 
                    settingsUseCase.updateDeviceId(event.id)
                    updateState { it.copy(deviceId = event.id, trackerLocation = LocationState(), trackerStats = StatsState(), trackerBattery = BatteryState(level = -1), connectivity = it.connectivity.copy(isTrackerConnected = false, lastUpdateTs = 0L)) }
                    _trackerMaxTemp.value = 0.0; _trackerCurrentMa.value = 0; repository.resetStats(); repository.sendCommand(UiCommand.StatsReset); repository.sendCommand(UiCommand.SettingsUpdated)
                }
                is UiEvent.SetViewerId -> {
                    settingsUseCase.updateViewerId(event.id)
                    updateState { it.copy(viewerId = event.id) }; repository.resetStats(); repository.resetStats(); repository.sendCommand(UiCommand.StatsReset); repository.sendCommand(UiCommand.SettingsUpdated)
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
            is UiEvent.UpdateDraftAlarmVolume -> updateDraft { it.copy(alertSettings = it.alertSettings.copy(alarmVolume = event.volume)) }
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Commit Failed: ${result.error}", Toast.LENGTH_LONG).show()
                }
                addPersistentLog("error", "Settings Commit Failed: ${result.error}", true)
                return@launch
            }

            if (result.anyChanged) {
                if (result.trackerIdChanged) addPersistentLog("user", "USER ACTION: Tracker ID changed", true)
                if (result.viewerIdChanged) addPersistentLog("user", "USER ACTION: Viewer ID changed", true)
                if (result.relayUrlChanged) addPersistentLog("user", "USER ACTION: Relay URL changed", true)
                if (result.maxDistanceChanged) addPersistentLog("user", "USER ACTION: Geofence distance updated", true)
                if (result.trackerIdChanged || result.viewerIdChanged) {
                    repository.resetStats(); repository.sendCommand(UiCommand.StatsReset)
                    _trackerMaxTemp.value = 0.0; _remoteSignal.value = 0; _trackerCurrentMa.value = 0; _gnssDetail.value = null; _trackerState.value = TrackerState.UNKNOWN
                    updateState { current -> current.copy(trackerLocation = LocationState(), trackerStats = StatsState(), trackerBattery = BatteryState(level = -1), connectivity = current.connectivity.copy(isTrackerConnected = false, lastUpdateTs = 0L, lastRemoteActivityTs = 0L), distanceTrackerToHome = null, distanceTrackerToViewer = null, distanceViewerToHome = null, maxTrackerAccuracy = 0.0) }
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
        viewModelScope.launch(uiExceptionHandler) {
            val nowRealtime = timeProvider.elapsedRealtime()
            val nowWall = when (event) {
                is UiEvent.DismissAlarms -> alertUseCase.dismissAlarms()
                is UiEvent.StopSiren -> alertUseCase.stopSiren(event.causes)
                else -> 0L
            }
            if (nowWall > 0) {
                lastAlarmAckRealtime = nowRealtime
                updateState { it.copy(isAlarmSilenced = true, lastAlarmAckTs = nowWall) }
                _redScreenVisible.value = false
            }
        }
    }

    private fun handleSystemEvent(event: UiEvent) {
        when (event) {
            is UiEvent.SetAppMode -> { 
                addPersistentLog("user", "USER ACTION: App mode set to ${event.mode ?: "NONE"}", true)
                viewModelScope.launch(uiExceptionHandler) {
                    val newStartTime = sessionUseCase.setAppMode(event.mode)
                    updateState { it.copy(appMode = event.mode, appStartTime = newStartTime ?: it.appStartTime) }
                    if (newStartTime != null) appStartTime = newStartTime
                }
            }
            is UiEvent.ConfirmStopTracking -> {
                addPersistentLog("user", "User-initiated Session Termination", true)
                updateNavigation { it.copy(isStopTrackingConfirmationVisible = false) }
                viewModelScope.launch(uiExceptionHandler) {
                    sessionUseCase.stopTrackingSession()
                    updateState { it.copy(appMode = null, isSystemActive = false, trackerLocation = LocationState(), trackerStats = StatsState(), trackerBattery = BatteryState(level = -1), connectivity = ConnectivityState(isTrackerConnected = false, lastUpdateTs = 0L, lastRemoteActivityTs = 0L), distanceTrackerToHome = null, distanceTrackerToViewer = null, distanceViewerToHome = null, maxTrackerAccuracy = 0.0, maxViewerAccuracy = 0.0, localHealth = SystemHealthState(), trackerHealth = SystemHealthState()) }
                    _remoteSignal.value = 0; _trackerCurrentMa.value = 0; _gnssDetail.value = null; _trackerState.value = TrackerState.UNKNOWN; _localMaxTemp.value = 0.0; _trackerMaxTemp.value = 0.0; _redScreenVisible.value = false; _rtt.value = 0; _gpsIndexData.value = GpsIndexData(0.0, 0.0, 0.0, 0.0); stateSubscriptionUseCase.clearHistory()
                }
            }
            else -> {}
        }
    }

    private fun handleLogAndStatsEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ResetStats -> { 
                viewModelScope.launch(uiExceptionHandler) {
                    val newStartTime = sessionUseCase.resetStats()
                    appStartTime = newStartTime
                    updateState { it.copy(appStartTime = appStartTime) }
                    addPersistentLog("user", "USER ACTION: Connectivity stats reset", true); stateSubscriptionUseCase.clearHistory(); Toast.makeText(context, "Connectivity stats reset", Toast.LENGTH_SHORT).show()
                }
            }
            is UiEvent.ClearLogs -> { repository.clearLogs(); addPersistentLog("user", "USER ACTION: Event logs cleared", true) }
            else -> {}
        }
    }

    private fun handleHomePointEvent(event: UiEvent) {
        viewModelScope.launch(uiExceptionHandler) {
            when (event) {
                is UiEvent.ClearHomePoints -> { val newList = homePointUseCase.clearHomePoints(_uiState.value.maxDistance); updateState { it.copy(homePoints = newList, geofenceMode = GeofenceMode.IDLE) }; addPersistentLog("user", "USER ACTION: All home points cleared", true) }
                is UiEvent.AddHomePoint -> { val newList = homePointUseCase.addHomePoint(_uiState.value.homePoints, event.point, _uiState.value.maxDistance); updateState { it.copy(homePoints = newList) }; addPersistentLog("user", String.format(Locale.getDefault(), "USER ACTION: Home point added at %.4f, %.4f", event.point.latitude, event.point.longitude), true) }
                is UiEvent.RemoveHomePoint -> { val newList = homePointUseCase.removeHomePoint(_uiState.value.homePoints, event.index, _uiState.value.maxDistance); updateState { it.copy(homePoints = newList) }; addPersistentLog("user", "USER ACTION: Home point removed", true) }
                is UiEvent.SetGeofenceMode -> updateState { it.copy(geofenceMode = if (it.geofenceMode == event.mode) GeofenceMode.IDLE else event.mode) }
                is UiEvent.MapTap -> {
                    val mode = _uiState.value.geofenceMode
                    if (mode == GeofenceMode.ADD) onEvent(UiEvent.AddHomePoint(event.point))
                    else if (mode == GeofenceMode.REMOVE) {
                        val nearestIdx = homePointUseCase.findNearestPointIndex(_uiState.value.homePoints, event.point)
                        if (nearestIdx != -1) onEvent(UiEvent.RemoveHomePoint(nearestIdx))
                    }
                }
                is UiEvent.SetMaxDistance -> { addPersistentLog("user", "USER ACTION: Geofence distance updated: ${event.distance.toInt()}m", true); repository.saveHomePoints(_uiState.value.homePoints, event.distance); updateState { it.copy(maxDistance = event.distance) } }
                is UiEvent.SetHomePoints -> { updateState { it.copy(homePoints = event.points) }; repository.saveHomePoints(event.points, _uiState.value.maxDistance); addPersistentLog("user", "USER ACTION: Home points restored", true) }
                is UiEvent.SaveHomePoints -> repository.saveHomePoints(_uiState.value.homePoints, _uiState.value.maxDistance)
                else -> {}
            }
            repository.sendCommand(UiCommand.PushSettings)
        }
    }

    private fun updateState(update: (MainUiState) -> MainUiState) { _uiState.update { current -> update(current) } }
    private fun updateNavigation(update: (NavigationState) -> NavigationState) { updateState { it.copy(navigation = update(it.navigation)) } }

    private fun startGlobalTimer() {
        viewModelScope.launch(Dispatchers.Main + uiExceptionHandler) {
            while (true) {
                val stateSnapshot = _uiState.value
                if (stateSnapshot.isInitialized && stateSnapshot.appMode != null) {
                    val now = timeProvider.currentTimeMillis()
                    val nowRealtime = timeProvider.elapsedRealtime()
                    _systemPulse.value = now
                    _systemPulseRealtime.value = nowRealtime
                    updateState { state -> state.copy(isSirenPlaying = AudioSynthesizer.isPlaying()) }
                    
                    repository.sendCommand(UiCommand.SyncRequest)
                    withContext(Dispatchers.Default) {
                        val currentState = _uiState.value
                        val newState = behaviorUseCase.computeTrackerState(currentState, now)
                        val shouldShowRed = behaviorUseCase.shouldShowRedScreen(currentState, nowRealtime, lastAlarmAckRealtime, _redScreenVisible.value)
                        
                        withContext(Dispatchers.Main) {
                            if (newState != _trackerState.value && newState != TrackerState.UNKNOWN) {
                                addPersistentLog("event", "Tracker is $newState", true)
                            }
                            _trackerState.value = newState
                            _redScreenVisible.value = shouldShowRed
                        }
                    }
                    updateState { state -> state.copy(isAlarmSilenced = behaviorUseCase.isAlarmSilenced(state.lastAlarmAckTs, now)) }
                }

                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun handleLocationUpdateInternal(update: LocationUpdate) {
        val nowMs = timeProvider.currentTimeMillis()
        _localMaxTemp.value = update.maxTemp
        if (_uiState.value.appMode == "tracker") _trackerMaxTemp.value = update.maxTemp

        updateState { current ->
            val home = current.homePoints.firstOrNull()
            val distToHome = if (home != null) PhysicsUtils.calculateDistance(update.lat, update.lng, home.latitude, home.longitude) else null
            
            val mappedHealth = telemetryUseCase.mapHealthFromUpdate(update, if (update.isMe) current.localHealth else current.trackerHealth)

            if (!update.isMe) {
                _trackerCurrentMa.value = update.currentMa
                current.copy(
                    trackerLocation = telemetryUseCase.mapTrackerLocation(update, current.trackerLocation, nowMs, appStartTime),
                    trackerHealth = mappedHealth,
                    connectivity = current.connectivity.copy(isTrackerConnected = true, lastUpdateTs = nowMs, lastRemoteActivityTs = nowMs),
                    trackerStats = telemetryUseCase.mapStats(update, current.trackerStats),
                    trackerBattery = if (current.appMode == "tracker") current.trackerBattery.copy(level = update.battery, temp = update.temp, isCharging = update.isCharging, isChargingStable = update.isCharging) else current.trackerBattery,
                    trackerSatsView = update.satsView,
                    trackerSatsUsed = update.satsUsed,
                    distanceTrackerToHome = if (current.appMode == "viewer" && PhysicsUtils.isValidLocation(update.lat, update.lng)) distToHome else current.distanceTrackerToHome,
                    distanceViewerToHome = if (current.appMode == "tracker" && PhysicsUtils.isValidLocation(update.lat, update.lng)) distToHome else current.distanceViewerToHome,
                    distanceTrackerToViewer = if (PhysicsUtils.isValidLocation(current.localLocation.lat, current.localLocation.lng) && PhysicsUtils.isValidLocation(update.lat, update.lng)) PhysicsUtils.calculateDistance(update.lat, update.lng, current.localLocation.lat, current.localLocation.lng) else current.distanceTrackerToViewer,
                    maxTrackerAccuracy = if (current.appMode == "viewer" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxTrackerAccuracy
                )
            } else {
                val isLocationValid = PhysicsUtils.isValidLocation(update.lat, update.lng)
                val dToOther = if (PhysicsUtils.isValidLocation(current.trackerLocation.lat, current.trackerLocation.lng) && isLocationValid) PhysicsUtils.calculateDistance(current.trackerLocation.lat, current.trackerLocation.lng, update.lat, update.lng) else null
                if (current.localLocation.lat != 0.0 && isLocationValid && PhysicsUtils.calculateDistance(current.localLocation.lat, current.localLocation.lng, update.lat, update.lng) > WILD_JUMP_THRESHOLD_METERS) return@updateState current
                current.copy(
                    localLocation = telemetryUseCase.mapLocalLocation(update, current.localLocation, nowMs, appStartTime),
                    localHealth = mappedHealth,
                    viewerSatsView = if (current.appMode == "viewer") update.satsView else current.viewerSatsView,
                    viewerSatsUsed = if (current.appMode == "viewer") update.satsUsed else current.viewerSatsUsed,
                    trackerSatsView = if (current.appMode == "tracker") update.satsView else current.trackerSatsView,
                    trackerSatsUsed = if (current.appMode == "tracker") update.satsUsed else current.trackerSatsUsed,
                    distanceTrackerToHome = if (current.appMode == "tracker" && isLocationValid) distToHome else current.distanceTrackerToHome,
                    distanceViewerToHome = if (current.appMode == "viewer" && isLocationValid) distToHome else current.distanceViewerToHome,
                    distanceTrackerToViewer = if (isLocationValid) dToOther else current.distanceTrackerToViewer,
                    maxTrackerAccuracy = if (current.appMode == "tracker" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxTrackerAccuracy,
                    maxViewerAccuracy = if (current.appMode == "viewer" && update.maxAccuracy > 0.0) update.maxAccuracy else current.maxViewerAccuracy,
                    stats = telemetryUseCase.mapStats(update, current.stats)
                )
            }
        }
    }

    private suspend fun loadInitialData() {
        val initial = settingsUseCase.loadAllSettings()
        appStartTime = initial.appStartTime
        updateState { it.copy(deviceId = initial.deviceId, viewerId = initial.viewerId, relayUrl = initial.relayUrl, maxDistance = initial.maxDistance, homePoints = initial.homePoints, alertSettings = initial.alertSettings, appMode = initial.appMode, isSystemActive = initial.isSystemActive, selectedSirenType = initial.selectedSirenType, lastAlarmAckTs = initial.lastAlarmAckTs, appStartTime = initial.appStartTime, draftSettings = initial.draftSettings ?: it.draftSettings, isIdentitySanitized = initial.identitySanitized) }
        _localMaxTemp.value = initial.maxTemp; if (initial.appMode == "tracker") _trackerMaxTemp.value = initial.maxTemp
        initial.trackerStatus?.let { status -> 
            updateState { it.copy(
                trackerLocation = telemetryUseCase.mapTrackerLocationFromStatus(status, it.trackerLocation),
                trackerHealth = telemetryUseCase.mapHealthFromStatus(status, it.trackerHealth),
                connectivity = it.connectivity.copy(lastUpdateTs = status.ts), 
                trackerStats = telemetryUseCase.mapStatsFromStatus(status, it.trackerStats), 
                trackerBattery = it.trackerBattery.copy(level = status.battery, temp = status.temp, isCharging = status.isCharging, isChargingStable = status.isCharging), 
                trackerSatsView = status.satsView, trackerSatsUsed = status.satsUsed, 
                maxTrackerAccuracy = if (status.maxAccuracy > 0.0) status.maxAccuracy else it.maxTrackerAccuracy
            ) }
            _trackerMaxTemp.value = status.maxTemp
            _trackerCurrentMa.value = status.currentMa
            if (_uiState.value.appMode == "tracker") _localMaxTemp.value = status.maxTemp 
        }
        
        updateState { it.copy(isInitialized = true) }
    }

    fun addPersistentLog(type: String, message: String, isImportant: Boolean = false, isSpecial: Boolean = false, specialColor: Int? = null) { logManager.submitToLogSink(message, type, important = isImportant, isSpecial = isSpecial, specialColor = specialColor) }

    fun fullInitialization(context: Context) {
        viewModelScope.launch(uiExceptionHandler) {
            appStartTime = settingsUseCase.fullInitialization(context)
            updateState { state -> state.copy(trackerLocation = LocationState(), connectivity = ConnectivityState(), trackerBattery = BatteryState(level = -1), trackerStats = StatsState(), stats = StatsState(), trackerSatsView = 0, trackerSatsUsed = 0, viewerSatsView = 0, viewerSatsUsed = 0, distanceTrackerToHome = null, distanceTrackerToViewer = null, distanceViewerToHome = null, localLocation = LocationState(), battery = BatteryState(level = -1), maxTrackerAccuracy = 0.0, maxViewerAccuracy = 0.0, activeAlarms = emptyList(), appStartTime = appStartTime, geofenceMode = GeofenceMode.IDLE, draftSettings = DraftSettings(), isIdentitySanitized = false, localHealth = SystemHealthState(), trackerHealth = SystemHealthState()) }
            _trackerState.value = TrackerState.UNKNOWN; _redScreenVisible.value = false; _localMaxTemp.value = 0.0; _trackerMaxTemp.value = 0.0; _trackerCurrentMa.value = 0; stateSubscriptionUseCase.clearHistory(); loadInitialData()
        }
    }
    
    fun clearTrails(context: Context) { viewModelScope.launch(uiExceptionHandler) { MainFileHelper.manualExportTrails(context, this@MainViewModel, timeProvider); repository.clearTrails(); addPersistentLog("user", "USER ACTION: Trails cleared", true); Toast.makeText(context, "Trails exported and cleared", Toast.LENGTH_SHORT).show() } }
}
