package com.gps19.app

import android.content.Context
import androidx.room.withTransaction
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
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
 * Sep.24.93:
 * - Issue #1265 REMEDIATION: Added getLocalLocationSync, getTrackerLocationSync, 
 *   and saveDoubleDebounced to support unified event orchestration.
 * Sep.24.90:
 * - Issue #1306 REMEDIATION: Added support for "VR_" (Viewer-Remote) prefix to 
 *   segregate remote tracker logic state from local viewer telemetry.
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
    private val timeProvider: TimeProvider
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
        const val DEFAULT_TRACKER_ID = SettingsRepository.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SettingsRepository.DEFAULT_VIEWER_ID
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
                TrailPoint(entity.lat, entity.lng, entity.timestamp, SentinelStatus.valueOf(entity.status), entity.accuracy, entity.maxAccuracy)
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
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val lastAlarmAckTsFlow = appModeFlow.flatMapLatest { mode ->
        if (mode == "tracker") trackerAlarmAckTsFlow else viewerAlarmAckTsFlow
    }.distinctUntilChanged()

    val homePointsFlow = settings.homePointsFlow
    val maxDistanceFlow = settings.maxDistanceFlow
    val alertSettingsFlow = settings.alertSettingsFlow
    val identitySanitizedFlow = settings.identitySanitizedFlow
    val isSystemActiveFlow = settings.isSystemActiveFlow
    val lastAlarmsJsonFlow = settings.lastAlarmsJsonFlow

    init {
        scope.launch { lastAlarmAckTsFlow.collect { lastAlarmAckTs = it } }
        scope.launch { trackerAlarmAckTsFlow.collect { trackerAlarmAckTs = it } }
        scope.launch { viewerAlarmAckTsFlow.collect { viewerAlarmAckTs = it } }
        scope.launch { homePointsFlow.collect { cachedHomePoints = it } }
        startUiHistoryEmitter()
    }

    suspend fun saveString(key: String, value: String) = settings.saveString(key, value)
    fun saveStringSync(key: String, value: String) { scope.launch { settings.saveString(key, value) } }
    suspend fun saveLong(key: String, value: Long) {
        when (key) {
            LAST_ALARM_ACK_TS_KEY -> lastAlarmAckTs = value
            "T_$LAST_ALARM_ACK_TS_KEY" -> trackerAlarmAckTs = value
            "V_$LAST_ALARM_ACK_TS_KEY" -> viewerAlarmAckTs = value
            "VR_$LAST_ALARM_ACK_TS_KEY" -> viewerRemoteAlarmAckTs = value
        }
        settings.saveLong(key, value)
    }
    fun saveLongSync(key: String, value: Long) {
        when (key) {
            LAST_ALARM_ACK_TS_KEY -> lastAlarmAckTs = value
            "T_$LAST_ALARM_ACK_TS_KEY" -> trackerAlarmAckTs = value
            "V_$LAST_ALARM_ACK_TS_KEY" -> viewerAlarmAckTs = value
            "VR_$LAST_ALARM_ACK_TS_KEY" -> viewerRemoteAlarmAckTs = value
        }
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

    fun getLastAlarmAckTsSync(rolePrefix: String? = null): Long {
        return when (rolePrefix) {
            "T_" -> trackerAlarmAckTs
            "V_" -> viewerAlarmAckTs
            "VR_" -> viewerRemoteAlarmAckTs
            else -> lastAlarmAckTs
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

    fun addHistoryPoints(ribbonKey: String, points: List<ConnectionPoint>) {
        scope.launch { _liveHistoryFlow.emit(ribbonKey to points.map { p -> ConnectionPoint().apply { copyFrom(p) } }) }
        if (!PersistencePolicy.shouldSaveHistoryPoint(telemetry.systemHealth.value)) return
        points.forEach { historyBuffer.add(TelemetryMapper.mapAppToEntity(it, ribbonKey)) }
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

    fun saveTrackerState(status: TrackerStatus, rolePrefix: String? = null) = settings.saveTrackerState(status, rolePrefix)
    suspend fun loadTrackerState(rolePrefix: String? = null) = settings.loadTrackerState(rolePrefix)
    suspend fun getLastAlarmAckTs(): Long = settings.getLong(LAST_ALARM_ACK_TS_KEY, 0L)
    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) { offlineRepository.addPendingStatusUpdate(update) }
    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> = offlineRepository.getPendingStatusUpdates(limit)
    suspend fun deletePendingStatusUpdate(id: Long) = offlineRepository.deletePendingStatusUpdate(id)
    suspend fun getLastAlarmsJson(rolePrefix: String? = null): String = settings.getString((rolePrefix ?: "") + LAST_ALARMS_JSON_KEY, "[]")
    fun saveAlarmsJsonSync(json: String, rolePrefix: String? = null) { scope.launch { settings.saveString((rolePrefix ?: "") + LAST_ALARMS_JSON_KEY, json) } }

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

    suspend fun saveLogicState(
        firstViolationTs: Long, firstViolationRt: Long, firstViolationWasJump: Boolean,
        distanceViolationCounter: Int, wasDistanceViolated: Boolean, powerAlarmPending: Boolean,
        lastSirenStopRt: Long, lastGlobalTriggerRt: Long, forensicReliabilityDegradationStartRt: Long,
        rolePrefix: String? = null
    ) {
        val p = rolePrefix ?: ""
        saveLong(p + FIRST_VIOLATION_TS_KEY, firstViolationTs); saveLong(p + FIRST_VIOLATION_RT_KEY, firstViolationRt)
        saveBoolean(p + FIRST_VIOLATION_WAS_JUMP_KEY, firstViolationWasJump); saveInt(p + DISTANCE_VIOLATION_COUNTER_KEY, distanceViolationCounter)
        saveBoolean(p + WAS_DISTANCE_VIOLATED_KEY, wasDistanceViolated); saveBoolean(p + POWER_ALARM_PENDING_KEY, powerAlarmPending)
        saveLong(p + LAST_SIREN_STOP_RT_KEY, lastSirenStopRt); saveLong(p + LAST_GLOBAL_TRIGGER_RT_KEY, lastGlobalTriggerRt)
        saveLong(p + FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY, forensicReliabilityDegradationStartRt)
    }
}
