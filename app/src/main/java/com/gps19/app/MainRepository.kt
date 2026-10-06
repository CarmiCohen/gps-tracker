package com.gps19.app

import android.content.Context
import androidx.room.withTransaction
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.osmdroid.util.GeoPoint

/**
 * RepositoryMetrics: Consolidated container for repository performance counters.
 */
private class RepositoryMetrics {
    val trailWriteCount = AtomicInteger(0)
    val violationWriteCount = AtomicInteger(0)
    val historyWriteCount = AtomicInteger(0)
    val isPruningActive = AtomicBoolean(false)
}

/**
 * MainRepository: Centralized data hub for the application.
 * Oct.6.7:
 * - Issue #AUDIT-1006-8: Signaling Metrics Audit. Added getDispatcherMetrics() 
 *   to expose SmartSignalingDispatcher telemetry to the ViewModel (Rule 1.123).
 * Oct.5.1:
 * - Issue #SIMP-1201-1: Logic State Serialization. Refactored saveLogicState 
 *   to pass the unified evaluation state object (R1201).
 */
@Singleton
class MainRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trailDao: TrailDao,
    private val historyDao: HistoryDao,
    private val violationDao: ViolationDao,
    private val pendingStatusDao: PendingStatusDao,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val telemetry: TelemetryRepository,
    private val logRepository: LogRepository,
    private val offlineRepository: OfflineRepository,
    private val timeProvider: TimeProvider,
    private val signalingProvider: SignalingProvider
) {
    private val repositoryExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Repository Coroutine Exception")
    }
    private val scope = CoroutineScope(Dispatchers.IO + repositoryExceptionHandler)

    private var cachedHomePoints: List<GeoPoint>? = null
    private var lastHomeRefreshTs = 0L
    private var lastAlarmAckTs: Long = 0L
    private var trackerAlarmAckTs: Long = 0L
    private var viewerAlarmAckTs: Long = 0L
    private var viewerRemoteAlarmAckTs: Long = 0L

    private val violationProcessor = ViolationProcessor(timeProvider)
    private val metrics = RepositoryMetrics()

    private val trackerPointCache = ShadowCache<Long, TrailPoint>(3000)
    private val viewerPointCache = ShadowCache<Long, TrailPoint>(3000)

    private val debounceJobs = ConcurrentHashMap<String, Job>()

    companion object {
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
        const val DEFAULT_TRACKER_ID = SignalingConstants.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SignalingConstants.DEFAULT_VIEWER_ID
        const val DEFAULT_MAX_DISTANCE = SettingsRepository.DEFAULT_MAX_DISTANCE
        
        private const val DB_PRUNE_THRESHOLD_HISTORY = 500
        private const val DB_PRUNE_THRESHOLD_TRAIL = 100
        private const val UI_HISTORY_EMIT_INTERVAL_MS = 500L 

        private const val PRUNE_LIMIT_HISTORY = 300
        private const val PRUNE_LIMIT_TRAIL = 2000
        private const val PRUNE_LIMIT_VIOLATIONS = 1000
        private const val PRUNE_CHUNK_SIZE = 500
        private const val SAVE_DEBOUNCE_MS = 1000L
    }

    val isRelayConnected = telemetry.isRelayConnected
    val lastRtt = telemetry.lastRtt
    val isSafeMode = telemetry.isSafeMode
    val systemHealth = telemetry.systemHealth
    val localLocation = telemetry.localLocation
    val trackerLocation = telemetry.trackerLocation
    val connectedViewers = telemetry.connectedViewers
    val lastRemoteActivityTs = telemetry.lastRemoteActivityTs
    val gnssDetail = telemetry.gnssDetail

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logRepository.eventLogsFlow(limit)

    val trackerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(false).map { entities -> 
        entities.map { entity ->
            trackerPointCache.getOrPut(entity.timestamp) {
                TrailPoint(entity.lat, entity.lng, entity.timestamp, SentinelStatus.valueOf(entity.status), entity.accuracy, entity.maxAccuracy)
            }
        }
    }.flowOn(Dispatchers.Default)

    val viewerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(true).map { entities -> 
        entities.map { entity ->
            viewerPointCache.getOrPut(entity.timestamp) {
                TrailPoint(entity.lat, entity.lng, entity.timestamp, SentinelStatus.valueOf(entity.status), entity.accuracy, maxAccuracy = entity.maxAccuracy)
            }
        }
    }.flowOn(Dispatchers.Default)

    val violationsFlow: Flow<List<ViolationPoint>> = violationDao.getAllFlow().map { entities -> 
        entities.map { ViolationPoint(lat = it.lat, lng = it.lng, type = it.type, ts = it.ts, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy) }
    }.flowOn(Dispatchers.Default)

    private val _uiCommands = MutableSharedFlow<UiCommand>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiCommands: SharedFlow<UiCommand> = _uiCommands.asSharedFlow()

    fun sendCommand(command: UiCommand) { scope.launch { _uiCommands.emit(command) } }

    fun updateRelayStatus(connected: Boolean) { telemetry.updateRelayStatus(connected) }
    fun updateLastRtt(rtt: Int) { telemetry.updateLastRtt(rtt) }
    fun setSafeMode(enabled: Boolean) { telemetry.setSafeMode(enabled) }
    fun updateHealth(state: SystemHealthState) { telemetry.updateHealth(state) }
    fun updateLocation(update: LocationUpdate) { telemetry.updateLocation(update) }
    fun updateConnectedViewers(viewers: List<String>) { telemetry.updateConnectedViewers(viewers) }
    fun updateRemoteActivity(ts: Long) { telemetry.updateRemoteActivity(ts) }
    fun updateGnssDetail(detail: GnssDetail?) { telemetry.updateGnssDetail(detail) }

    fun getLocalLocationSync(): LocationUpdate = telemetry.localLocation.value
    fun getTrackerLocationSync(): LocationUpdate = telemetry.trackerLocation.value

    fun getDispatcherMetrics(): SmartSignalingDispatcher.Metrics = signalingProvider.getDispatcherMetrics()

    fun clear() { telemetry.clear() }

    val appModeFlow = settings.appModeFlow
    val trackerIdFlow = settings.trackerIdFlow
    val viewerIdFlow = settings.viewerIdFlow
    val relayUrlFlow = settings.relayUrlFlow
    val isManualExitFlow = settings.isManualExitFlow
    val isXiaomiManualOverrideFlow = settings.isXiaomiManualOverrideFlow
    val recoveryCountFlow = settings.recoveryCountFlow
    val cumulativeRecoveryBlackoutMsFlow = settings.cumulativeRecoveryBlackoutMsFlow
    
    val trackerAlarmAckTsFlow = settings.trackerAlarmAckTsFlow
    val viewerAlarmAckTsFlow = settings.viewerAlarmAckTsFlow
    val viewerRemoteAlarmAckTsFlow = settings.viewerRemoteAlarmAckTsFlow
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val lastAlarmAckTsFlow = appModeFlow.flatMapLatest { mode ->
        if (mode == "tracker") trackerAlarmAckTsFlow else viewerRemoteAlarmAckTsFlow
    }.distinctUntilChanged()

    val homePointsFlow = settings.homePointsFlow
    val maxDistanceFlow = settings.maxDistanceFlow
    val alertSettingsFlow = settings.alertSettingsFlow
    val identitySanitizedFlow = settings.identitySanitizedFlow
    val isSystemActiveFlow = settings.isSystemActiveFlow

    init {
        scope.launch { lastAlarmAckTsFlow.collect { lastAlarmAckTs = it } }
        scope.launch { trackerAlarmAckTsFlow.collect { trackerAlarmAckTs = it } }
        scope.launch { viewerAlarmAckTsFlow.collect { viewerAlarmAckTs = it } }
        scope.launch { viewerRemoteAlarmAckTsFlow.collect { viewerRemoteAlarmAckTs = it } }
        scope.launch { homePointsFlow.collect { cachedHomePoints = it } }
        startUiHistoryEmitter()
    }

    // --- Unified Role-Based API ---

    suspend fun saveString(role: AppRole, key: String, value: String) = settings.saveString(role, key, value)
    fun saveStringSync(role: AppRole, key: String, value: String) { scope.launch { settings.saveString(role, key, value) } }

    suspend fun saveLong(role: AppRole, key: String, value: Long) {
        if (key == LAST_ALARM_ACK_TS_KEY) {
            when (role) {
                AppRole.TRACKER -> trackerAlarmAckTs = value
                AppRole.VIEWER_SELF -> viewerAlarmAckTs = value
                AppRole.VIEWER_REMOTE -> viewerRemoteAlarmAckTs = value
            }
        }
        settings.saveLong(role, key, value)
    }
    fun saveLongSync(role: AppRole, key: String, value: Long) {
        if (key == LAST_ALARM_ACK_TS_KEY) {
            when (role) {
                AppRole.TRACKER -> trackerAlarmAckTs = value
                AppRole.VIEWER_SELF -> viewerAlarmAckTs = value
                AppRole.VIEWER_REMOTE -> viewerRemoteAlarmAckTs = value
            }
        }
        scope.launch { settings.saveLong(role, key, value) }
    }

    suspend fun saveDouble(role: AppRole, key: String, value: Double) = settings.saveDouble(role, key, value)
    fun saveDoubleSync(role: AppRole, key: String, value: Double) { scope.launch { settings.saveDouble(role, key, value) } }

    fun saveDoubleDebounced(role: AppRole, key: String, value: Double) {
        val compositeKey = role.prefix + key
        debounceJobs[compositeKey]?.cancel()
        debounceJobs[compositeKey] = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            settings.saveDouble(role, key, value)
            debounceJobs.remove(compositeKey)
        }
    }

    suspend fun saveBoolean(role: AppRole, key: String, value: Boolean) = settings.saveBoolean(role, key, value)
    fun saveBooleanSync(role: AppRole, key: String, value: Boolean) { scope.launch { settings.saveBoolean(role, key, value) } }

    suspend fun saveInt(role: AppRole, key: String, value: Int) = settings.saveInt(role, key, value)
    fun saveIntSync(role: AppRole, key: String, value: Int) { scope.launch { settings.saveInt(role, key, value) } }

    suspend fun getString(role: AppRole, key: String, default: String) = settings.getString(role, key, default)
    suspend fun getLong(role: AppRole, key: String, default: Long) = settings.getLong(role, key, default)
    suspend fun getDouble(role: AppRole, key: String, default: Double) = settings.getDouble(role, key, default)
    suspend fun getInt(role: AppRole, key: String, default: Int): Int = settings.getInt(role, key, default)
    suspend fun getBoolean(role: AppRole, key: String, default: Boolean): Boolean = settings.getBoolean(role, key, default)

    // --- Global String-Keyed API ---

    suspend fun saveString(key: String, value: String) = settings.saveString(key, value)
    fun saveStringSync(key: String, value: String) { scope.launch { settings.saveString(key, value) } }

    suspend fun saveLong(key: String, value: Long) {
        if (key == LAST_ALARM_ACK_TS_KEY) lastAlarmAckTs = value
        settings.saveLong(key, value)
    }
    fun saveLongSync(key: String, value: Long) {
        if (key == LAST_ALARM_ACK_TS_KEY) lastAlarmAckTs = value
        scope.launch { settings.saveLong(key, value) }
    }

    suspend fun saveDouble(key: String, value: Double) = settings.saveDouble(key, value)
    fun saveDoubleSync(key: String, value: Double) { scope.launch { settings.saveDouble(key, value) } }
    
    fun saveDoubleDebounced(key: String, value: Double) {
        debounceJobs[key]?.cancel()
        debounceJobs[key] = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            settings.saveDouble(key, value)
            debounceJobs.remove(key)
        }
    }

    suspend fun saveBoolean(key: String, value: Boolean) = settings.saveBoolean(key, value)
    fun saveBooleanSync(key: String, value: Boolean) { scope.launch { settings.saveBoolean(key, value) } }

    suspend fun saveInt(key: String, value: Int) = settings.saveInt(key, value)
    fun saveIntSync(key: String, value: Int) { scope.launch { settings.saveInt(key, value) } }

    suspend fun getString(key: String, default: String) = settings.getString(key, default)
    suspend fun getLong(keyName: String, default: Long) = settings.getLong(keyName, default)
    suspend fun getDouble(key: String, default: Double) = settings.getDouble(key, default)
    suspend fun getInt(key: String, default: Int): Int = settings.getInt(key, default)
    suspend fun getBoolean(key: String, default: Boolean): Boolean = settings.getBoolean(key, default)

    // --- State & Authority ---

    suspend fun getAppMode() = settings.getAppMode()
    suspend fun setAppMode(mode: String?) = settings.setAppMode(mode)
    suspend fun getAppStartTime() = settings.getLong(APP_START_TIME_KEY, 0L)
    fun setAppStartTime(ts: Long) { saveLongSync(APP_START_TIME_KEY, ts) }

    suspend fun loadHomePoints(): List<GeoPoint> {
        val points = settings.loadHomePoints()
        cachedHomePoints = points
        lastHomeRefreshTs = timeProvider.currentTimeMillis()
        return points
    }
    
    fun getCachedHomePoints(): List<GeoPoint> = cachedHomePoints ?: emptyList()

    fun getLastAlarmAckTsSync(role: AppRole? = null): Long {
        return when (role) {
            AppRole.TRACKER -> trackerAlarmAckTs
            AppRole.VIEWER_SELF -> viewerAlarmAckTs
            AppRole.VIEWER_REMOTE -> viewerRemoteAlarmAckTs
            null -> lastAlarmAckTs
        }
    }

    suspend fun saveHomePoints(points: List<GeoPoint>, maxDist: Double? = null, ts: Long? = null) {
        settings.saveHomePoints(points, maxDist, ts)
        cachedHomePoints = points
        lastHomeRefreshTs = timeProvider.currentTimeMillis()
    }

    suspend fun addHomePoint(lat: Double, lng: Double) = settings.addHomePoint(lat, lng)
    suspend fun removeHomePoint(index: Int) = settings.removeHomePoint(index)

    suspend fun loadAlertSettings() = settings.loadAlertSettings()
    suspend fun saveAlertSettings(s: AlertSettings) = settings.saveAlertSettings(s)
    
    suspend fun saveSettingsBulk(
        deviceId: String? = null, viewerId: String? = null, relayUrl: String? = null, 
        maxDistance: Double? = null, alertSettings: AlertSettings? = null, homePoints: List<GeoPoint>? = null
    ) {
        val currentTracker = deviceId ?: settings.getString(TRACKER_ID_KEY, DEFAULT_TRACKER_ID)
        val currentViewer = viewerId ?: settings.getString(VIEWER_ID_KEY, DEFAULT_VIEWER_ID)
        if (!SignalingConstants.areIdsUnique(currentTracker, currentViewer)) {
            throw IllegalArgumentException("IDs must be unique and alphanumeric")
        }
        settings.saveSettingsBulk(deviceId, viewerId, relayUrl, maxDistance, alertSettings, homePoints)
    }

    suspend fun saveSessionMetricsBulk(
        totalConnected: Long, uptime: Long, totalDrop: Long, 
        maxDrop: Long, maxDropTs: Long, lastGpsTs: Long, violationUptimeMs: Long
    ) = settings.saveSessionMetricsBulk(totalConnected, uptime, totalDrop, maxDrop, maxDropTs, lastGpsTs, violationUptimeMs)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) { logRepository.addLog(entry, initiallySynced) }
    fun clearLogs() { logRepository.clearLogs() }
    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = logRepository.loadAllLogsStatic(limit)
    suspend fun proactivePruning() = logRepository.proactivePruning()

    fun saveTrailPoint(lat: Double, lng: Double, isViewer: Boolean, status: SentinelStatus = SentinelStatus.VALID, timestamp: Long? = null, force: Boolean = false, accuracy: Double = 0.0, maxAccuracy: Double = 0.0) {
        if (lat == 0.0 || lng == 0.0) return
        val health = telemetry.systemHealth.value
        if (!PersistencePolicy.shouldSaveTrailPoint(health, status)) return
        scope.launch {
            val wallTs = timestamp ?: timeProvider.currentTimeMillis()
            trailDao.insert(TrailEntity(lat = lat, lng = lng, timestamp = wallTs, isViewerTrail = isViewer, status = status.name, accuracy = accuracy, maxAccuracy = maxAccuracy))
            if (force || metrics.trailWriteCount.incrementAndGet() >= DB_PRUNE_THRESHOLD_TRAIL) {
                metrics.trailWriteCount.set(0); triggerBackgroundPruning()
            }
        }
    }

    suspend fun clearTrails() = withContext(Dispatchers.IO) {
        trailDao.clearTrail(false); trailDao.clearTrail(true); violationDao.clearAll(); trackerPointCache.clear(); viewerPointCache.clear()
    }

    suspend fun loadTrailStatic(isViewer: Boolean): List<TrailPoint> = trailDao.getTrailStatic(isViewer).map { 
        TrailPoint(it.lat, it.lng, it.timestamp, SentinelStatus.valueOf(it.status), it.accuracy, it.maxAccuracy)
    }

    suspend fun resetStats() = withContext(Dispatchers.IO) {
        settings.resetStatsBulk(); clearTrails(); historyDao.clearAll(); logRepository.clearLogs(); offlineRepository.clear()
    }

    fun addViolation(lat: Double, lng: Double, type: String, accuracy: Double = 0.0, maxAccuracy: Double = 0.0, adaptiveRadius: Double = 0.0, timestamp: Long? = null) {
        if (!violationProcessor.shouldRecordViolation(lat, lng, type, accuracy, maxAccuracy)) return
        val wallTs = timestamp ?: timeProvider.currentTimeMillis()
        scope.launch { 
            violationDao.insert(ViolationEntity(lat = lat, lng = lng, type = type, ts = wallTs, accuracy = accuracy, maxAccuracy = maxAccuracy))
            if (metrics.violationWriteCount.incrementAndGet() >= DB_PRUNE_THRESHOLD_TRAIL) {
                metrics.violationWriteCount.set(0); triggerBackgroundPruning()
            }
        }
    }

    fun getHistoryFlow(ribbonKey: String): Flow<List<ConnectionPoint>> = historyDao.getHistoryFlow(ribbonKey).map { l -> 
        l.map { entity ->
            ConnectionPoint().apply {
                ts = entity.ts; rt = entity.rt; rtt = entity.rtt; localSig = 10; remoteSig = entity.remoteSig
                isConnected = entity.isConnected; isGap = entity.isGap; isRecoveryEvent = entity.isRecoveryEvent
                gpsAccuracy = entity.accuracy; maxAccuracy = entity.maxAccuracy; isTick = entity.isTick 
                hasGps = entity.hasGps; speed = entity.speed; bearing = entity.bearing; currentMa = entity.currentMa
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE }
                TelemetryMapper.mapEntityToApp(entity, this)
            }
        }
    }.flowOn(Dispatchers.Default)

    private var lastBatchWriteRealtime = 0L
    private val historyBuffer = ConcurrentLinkedQueue<HistoryEntity>()
    private val _liveHistoryFlow = MutableSharedFlow<Pair<String, List<ConnectionPoint>>>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val liveHistoryFlow = _liveHistoryFlow.asSharedFlow()
    private val liveHistoryBuffer = ConcurrentLinkedQueue<Pair<String, ConnectionPoint>>()

    fun addHistoryPoint(ribbonKey: String, point: ConnectionPoint) {
        val health = telemetry.systemHealth.value
        liveHistoryBuffer.add(ribbonKey to ConnectionPoint().apply { copyFrom(point) })
        if (!PersistencePolicy.shouldSaveHistoryPoint(health)) return
        historyBuffer.add(TelemetryMapper.mapAppToEntity(point, ribbonKey))
        val nowRt = timeProvider.elapsedRealtime()
        if ((nowRt - lastBatchWriteRealtime > HISTORY_BATCH_WRITE_INTERVAL_MS) || (historyBuffer.size >= HISTORY_BUFFER_MAX_SIZE)) {
            scope.launch { flushHistoryBufferInternal(nowRt) }
        }
    }

    private fun startUiHistoryEmitter() {
        scope.launch {
            while (isActive) {
                delay(UI_HISTORY_EMIT_INTERVAL_MS)
                if (liveHistoryBuffer.isEmpty()) continue
                val batches = mutableMapOf<String, MutableList<ConnectionPoint>>()
                while (liveHistoryBuffer.isNotEmpty()) {
                    liveHistoryBuffer.poll()?.let { (key, point) -> batches.getOrPut(key) { mutableListOf() }.add(point) }
                }
                batches.forEach { (key, list) -> _liveHistoryFlow.emit(key to list) }
            }
        }
    }

    suspend fun flushHistory() { flushHistoryBufferInternal(timeProvider.elapsedRealtime()) }

    private suspend fun flushHistoryBufferInternal(nowRt: Long) = withContext(Dispatchers.IO) {
        val dbPoints = mutableListOf<HistoryEntity>()
        while (historyBuffer.isNotEmpty()) historyBuffer.poll()?.let { dbPoints.add(it) }
        if (dbPoints.isNotEmpty()) {
            lastBatchWriteRealtime = nowRt
            database.withTransaction {
                historyDao.insertAll(dbPoints)
                if (metrics.historyWriteCount.addAndGet(dbPoints.size) >= DB_PRUNE_THRESHOLD_HISTORY) {
                    metrics.historyWriteCount.set(0); triggerBackgroundPruning()
                }
            }
        }
    }

    private fun triggerBackgroundPruning() {
        if (metrics.isPruningActive.getAndSet(true)) return
        if (telemetry.systemHealth.value.isBatteryCritical) { metrics.isPruningActive.set(false); return }
        scope.launch {
            try {
                database.withTransaction {
                    listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { key ->
                        historyDao.getPruneThreshold(key, PRUNE_LIMIT_HISTORY)?.let { historyDao.pruneByThreshold(key, it, PRUNE_CHUNK_SIZE) }
                    }
                    listOf(false, true).forEach { isViewer ->
                        trailDao.getPruneThreshold(isViewer, PRUNE_LIMIT_TRAIL)?.let { trailDao.pruneByThreshold(isViewer, it, PRUNE_CHUNK_SIZE) }
                    }
                    violationDao.getPruneThreshold(PRUNE_LIMIT_VIOLATIONS)?.let { violationDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE) }
                }
            } catch (e: Exception) { Timber.e(e, "Background pruning failed") } finally { metrics.isPruningActive.set(false) }
        }
    }

    fun saveLocationUpdate(status: LocationUpdate, role: AppRole? = null) = settings.saveLocationUpdate(status, role)
    suspend fun loadLocationUpdate(role: AppRole? = null) = settings.loadLocationUpdate(role)
    
    suspend fun getLastAlarmAckTs(): Long = settings.getLong(LAST_ALARM_ACK_TS_KEY, 0L)
    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) { offlineRepository.addPendingStatusUpdate(update) }
    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> = offlineRepository.getPendingStatusUpdates(limit)
    suspend fun addPendingStatusUpdateSync(update: PendingStatusEntity) { scope.launch { offlineRepository.addPendingStatusUpdate(update) } }
    suspend fun getTrackerState(role: AppRole? = null): LocationUpdate? = loadLocationUpdate(role)
    suspend fun deletePendingStatusUpdate(id: Long) = offlineRepository.deletePendingStatusUpdate(id)

    suspend fun saveActiveAlarms(alarms: List<AlarmEvaluationState.ActiveAlarm>, role: AppRole? = null) {
        settings.saveActiveAlarms(alarms, role)
    }

    suspend fun loadActiveAlarms(role: AppRole? = null): List<AlarmEvaluationState.ActiveAlarm> {
        return settings.loadActiveAlarms(role)
    }

    private val _logFilterDetails = MutableStateFlow(false)
    val logFilterDetails = _logFilterDetails.asStateFlow()
    private val _logFilterRecovered = MutableStateFlow(false)
    val logFilterRecovered = _logFilterRecovered.asStateFlow()
    fun updateLogFilters(details: Boolean? = null, recovered: Boolean? = null) {
        details?.let { _logFilterDetails.value = it } 
        recovered?.let { _logFilterRecovered.value = it }
    }

    suspend fun incrementRecoveryStats(blackoutMs: Long) = settings.incrementRecoveryStats(blackoutMs)
    suspend fun getSettingsSnapshot() = settings.getSettingsSnapshot()
    suspend fun checkDatabaseIntegrity(): String = withContext(Dispatchers.IO) { database.checkIntegrity() }
    fun setForensicStallSimulation(active: Boolean) { logRepository.setForensicStallSimulation(active) }
    suspend fun saveDraftSettings(deviceId: String, viewerId: String, relayUrl: String, maxDistance: Double, alertSettings: AlertSettings) { settings.saveDraftSettings(deviceId, viewerId, relayUrl, maxDistance, alertSettings) }
    suspend fun commitDraftSettings(): CommitResult { return settings.commitDraftSettings() }
    suspend fun clearDraftSettings() { settings.clearDraftSettings() }

    suspend fun saveLogicState(state: AlarmEvaluationState, role: AppRole) {
        settings.saveLogicState(state, role)
    }
}
