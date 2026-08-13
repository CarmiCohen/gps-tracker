package com.gps19.app

import android.content.Context
import androidx.room.withTransaction
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MainRepository: Centralized data hub for the application.
 * Aug.13.02:
 * - Build Fix: Explicitly typed LatencyMonitor.measureAndAudit calls to resolve 
 *   type inference failures (R146/R151).
 * Aug.10.24:
 * - Issue #130: Proto Health Parity. Synchronized HistoryEntity mapping with 
 *   isBatteryLow and isBatteryCritical flags (R130).
 * - Issue #129: Forensic Storage Pruning Sensitivity. Refactored background 
 *   pruning to be battery-aware, deferring maintenance during critical battery 
 *   states to preserve power and reduce I/O spikes (R129).
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

    private val violationProcessor = ViolationProcessor(timeProvider)

    private var trailWriteCount = 0
    private var violationWriteCount = 0
    private var historyWriteCount = 0
    
    private val isPruningActive = AtomicBoolean(false)

    companion object {
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
        const val DEFAULT_TRACKER_ID = SettingsRepository.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SettingsRepository.DEFAULT_VIEWER_ID
        const val DEFAULT_MAX_DISTANCE = SettingsRepository.DEFAULT_MAX_DISTANCE
        
        private const val DB_PRUNE_THRESHOLD_HISTORY = 500
        private const val DB_PRUNE_THRESHOLD_TRAIL = 100
    }

    val isRelayConnected = telemetry.isRelayConnected
    val lastRtt = telemetry.lastRtt
    val systemHealth = telemetry.systemHealth
    val localLocation = telemetry.localLocation
    val trackerLocation = telemetry.trackerLocation
    val connectedViewers = telemetry.connectedViewers
    val lastRemoteActivityTs = telemetry.lastRemoteActivityTs
    val gnssDetail = telemetry.gnssDetail

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logRepository.eventLogsFlow(limit)

    val trackerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(false).map { entities -> 
        entities.map { TrailPoint(it.lat, it.lng, it.timestamp, SentinelStatus.valueOf(it.status), it.accuracy, it.maxAccuracy) } 
    }.flowOn(Dispatchers.Default)

    val viewerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(true).map { entities -> 
        entities.map { TrailPoint(it.lat, it.lng, it.timestamp, SentinelStatus.valueOf(it.status), it.accuracy, it.maxAccuracy) } 
    }.flowOn(Dispatchers.Default)

    val violationsFlow: Flow<List<ViolationPoint>> = violationDao.getAllFlow().map { entities -> 
        entities.map { ViolationPoint(point = GeoPoint(it.lat, it.lng), type = it.type, ts = it.ts, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy) } 
    }.flowOn(Dispatchers.Default)

    private val _uiCommands = MutableSharedFlow<UiCommand>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiCommands: SharedFlow<UiCommand> = _uiCommands.asSharedFlow()

    fun sendCommand(command: UiCommand) { scope.launch { _uiCommands.emit(command) } }

    fun updateRelayStatus(connected: Boolean) { telemetry.updateRelayStatus(connected) }
    fun updateLastRtt(rtt: Int) { telemetry.updateLastRtt(rtt) }
    fun updateHealth(state: SystemHealthState) { telemetry.updateHealth(state) }
    fun updateLocation(update: LocationUpdate) { telemetry.updateLocation(update) }
    fun updateConnectedViewers(viewers: List<String>) { telemetry.updateConnectedViewers(viewers) }
    fun updateRemoteActivity(ts: Long) { telemetry.updateRemoteActivity(ts) }
    fun updateGnssDetail(detail: GnssDetail?) { telemetry.updateGnssDetail(detail) }

    fun clear() { telemetry.clear() }

    val appModeFlow = settings.appModeFlow
    val trackerIdFlow = settings.trackerIdFlow
    val viewerIdFlow = settings.viewerIdFlow
    val relayUrlFlow = settings.relayUrlFlow
    val isManualExitFlow = settings.isManualExitFlow
    val lastAlarmAckTsFlow = settings.lastAlarmAckTsFlow
    val homePointsFlow = settings.homePointsFlow
    val maxDistanceFlow = settings.maxDistanceFlow
    val alertSettingsFlow = settings.alertSettingsFlow
    val isXiaomiManualOverrideFlow = settings.isXiaomiManualOverrideFlow
    val identitySanitizedFlow = settings.identitySanitizedFlow
    val isSystemActiveFlow = settings.isSystemActiveFlow
    val lastAlarmsJsonFlow = settings.lastAlarmsJsonFlow
    val isRecoveryPendingFlow = settings.isRecoveryPendingFlow
    val cumulativeRecoveryBlackoutMsFlow = settings.cumulativeRecoveryBlackoutMsFlow
    val recoveryCountFlow = settings.recoveryCountFlow

    init {
        scope.launch {
            lastAlarmAckTsFlow.collect { lastAlarmAckTs = it }
        }
        scope.launch {
            homePointsFlow.collect { cachedHomePoints = it }
        }
    }

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
    fun setAppMode(mode: String?) = settings.setAppMode(mode)
    suspend fun getAppStartTime() = settings.getLong(APP_START_TIME_KEY, 0L)
    fun setAppStartTime(ts: Long) { saveLongSync(APP_START_TIME_KEY, ts) }

    suspend fun loadHomePoints(): List<GeoPoint> {
        val points = settings.loadHomePoints()
        cachedHomePoints = points
        lastHomeRefreshTs = timeProvider.currentTimeMillis()
        return points
    }
    
    fun getCachedHomePoints(): List<GeoPoint> = cachedHomePoints ?: emptyList()
    fun getLastAlarmAckTsSync(): Long = lastAlarmAckTs

    suspend fun saveHomePoints(points: List<GeoPoint>, maxDist: Double? = null, ts: Long? = null) {
        settings.saveHomePoints(points, maxDist, ts)
        cachedHomePoints = points
        lastHomeRefreshTs = timeProvider.currentTimeMillis()
    }

    suspend fun loadAlertSettings() = settings.loadAlertSettings()
    suspend fun saveAlertSettings(s: AlertSettings) = settings.saveAlertSettings(s)
    suspend fun saveDraftAlertSettings(s: AlertSettings) = settings.saveAlertSettings(s)
    suspend fun loadDraftAlertSettings() = settings.loadDraftAlertSettings()
    fun clearDraftSettings() { scope.launch { settings.clearDraftSettings() } }
    
    suspend fun saveDraftSettings(deviceId: String, viewerId: String, relayUrl: String, maxDistance: Double, alertSettings: AlertSettings) = settings.saveDraftSettings(deviceId, viewerId, relayUrl, maxDistance, alertSettings)
    suspend fun commitDraftSettings() = settings.commitDraftSettings()
    suspend fun hasPendingDrafts(): Boolean = settings.hasPendingDrafts()

    suspend fun saveSettingsBulk(
        deviceId: String? = null, 
        viewerId: String? = null, 
        relayUrl: String? = null, 
        maxDistance: Double? = null, 
        alertSettings: AlertSettings? = null, 
        homePoints: List<GeoPoint>? = null
    ) {
        val currentTracker = deviceId ?: settings.getString(TRACKER_ID_KEY, DEFAULT_TRACKER_ID)
        val currentViewer = viewerId ?: settings.getString(VIEWER_ID_KEY, DEFAULT_VIEWER_ID)
        
        if (!SignalingConstants.areIdsUnique(currentTracker, currentViewer)) {
            val err = "Identity Collision: IDs must be unique and alphanumeric (T:$currentTracker, V:$currentViewer)"
            Timber.e(err)
            throw IllegalArgumentException("IDs must be unique and alphanumeric")
        }
        
        settings.saveSettingsBulk(deviceId, viewerId, relayUrl, maxDistance, alertSettings, homePoints)
    }

    suspend fun saveSessionMetricsBulk(
        totalConnected: Long, uptime: Long, totalDrop: Long, 
        maxDrop: Long, maxDropTs: Long, lastGpsTs: Long, violationUptimeMs: Long
    ) = settings.saveSessionMetricsBulk(totalConnected, uptime, totalDrop, maxDrop, maxDropTs, lastGpsTs, violationUptimeMs)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        logRepository.addLog(entry, initiallySynced)
    }

    fun clearLogs() { logRepository.clearLogs() }
    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = logRepository.loadAllLogsStatic(limit)

    suspend fun proactivePruning() = logRepository.proactivePruning()

    fun saveTrailPoint(lat: Double, lng: Double, isViewer: Boolean, status: SentinelStatus = SentinelStatus.VALID, timestamp: Long? = null, force: Boolean = false, accuracy: Double = 0.0, maxAccuracy: Double = 0.0) {
        if (lat == 0.0 || lng == 0.0) return
        
        val health = telemetry.systemHealth.value
        if (!PersistencePolicy.shouldSaveTrailPoint(
            health = health,
            status = status
        )) return

        scope.launch {
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_DB_WRITE_MS,
                operation = "Trail Write",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> logLatencySpike(message) }
            ) {
                val wallTs = timestamp ?: timeProvider.currentTimeMillis()
                trailDao.insert(TrailEntity(
                    lat = lat, lng = lng, timestamp = wallTs, 
                    isViewerTrail = isViewer, status = status.name, 
                    accuracy = accuracy,
                    maxAccuracy = maxAccuracy
                ))
                
                trailWriteCount++
                if (force || trailWriteCount >= DB_PRUNE_THRESHOLD_TRAIL) {
                    trailWriteCount = 0
                    triggerBackgroundPruning()
                }
            }
        }
    }

    suspend fun clearTrails() = withContext(Dispatchers.IO) {
        trailDao.clearTrail(false)
        trailDao.clearTrail(true)
        violationDao.clearAll()
    }

    suspend fun loadTrailStatic(isViewer: Boolean): List<TrailPoint> = trailDao.getTrailStatic(isViewer).map { 
        TrailPoint(it.lat, it.lng, it.timestamp, SentinelStatus.valueOf(it.status), it.accuracy, it.maxAccuracy)
    }

    suspend fun resetStats() = withContext(Dispatchers.IO) {
        settings.resetStatsBulk()
        clearTrails()
        historyDao.clearAll()
        logRepository.clearLogs()
        offlineRepository.clear()
    }

    fun addViolation(lat: Double, lng: Double, type: String, accuracy: Double = 0.0, maxAccuracy: Double = 0.0, adaptiveRadius: Double = 0.0, timestamp: Long? = null) {
        if (!violationProcessor.shouldRecordViolation(lat, lng, type, accuracy, maxAccuracy)) return

        val wallTs = timestamp ?: timeProvider.currentTimeMillis()
        scope.launch { 
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_DB_WRITE_MS,
                operation = "Violation Write",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> logLatencySpike(message) }
            ) {
                violationDao.insert(ViolationEntity(lat = lat, lng = lng, type = type, ts = wallTs, accuracy = accuracy, maxAccuracy = maxAccuracy))
                
                violationWriteCount++
                if (violationWriteCount >= DB_PRUNE_THRESHOLD_TRAIL) {
                    violationWriteCount = 0
                    triggerBackgroundPruning()
                }
            }
        }
    }

    fun getHistoryFlow(ribbonKey: String): Flow<List<ConnectionPoint>> = historyDao.getHistoryFlow(ribbonKey).map { l: List<HistoryEntity> -> 
        l.map { entity ->
            ConnectionPoint(
                localId = UUID.randomUUID().toString(), ts = entity.ts, rt = entity.rt, rtt = entity.rtt, localSig = 10, remoteSig = entity.remoteSig,
                isConnected = entity.isConnected, isGap = entity.isGap, isRecoveryEvent = entity.isRecoveryEvent,
                gpsAccuracy = entity.accuracy, maxAccuracy = entity.maxAccuracy, isTick = entity.isTick, 
                hasGps = entity.hasGps,
                isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive,
                isBatteryLow = entity.isBatteryLow,
                isBatteryCritical = entity.isBatteryCritical,
                speed = entity.speed, bearing = entity.bearing,
                currentMa = entity.currentMa,
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE },
                
                gpsIndex = entity.gpsIndex,
                noiseIdx = entity.noiseIdx,
                luxIdx = entity.luxIdx,
                vibeIdx = entity.vibeIdx,
                proxIdx = entity.proxIdx,
                liftIdx = entity.liftIdx,
                snrIdx = entity.snrIdx,
                tiltIdx = entity.tiltIdx,
                baroIdx = entity.baroIdx,
                isSitDetected = entity.isSitDetected,
                isSitActive = entity.isSitActive,
                sitVz = entity.sitVz,
                sitDz = entity.sitDz,
                sitBaro = entity.sitBaro,
                sitTilt = entity.sitTilt,
                sitShock = entity.sitShock
            ) 
        }
    }.flowOn(Dispatchers.Default)

    private var lastBatchWriteRealtime = 0L
    private val historyBuffer = ConcurrentLinkedQueue<HistoryEntity>()

    private val _liveHistoryFlow = MutableSharedFlow<Pair<String, List<ConnectionPoint>>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val liveHistoryFlow = _liveHistoryFlow.asSharedFlow()

    fun addHistoryPoint(ribbonKey: String, point: ConnectionPoint) {
        addHistoryPoints(ribbonKey, listOf(point))
    }

    fun addHistoryPoints(ribbonKey: String, points: List<ConnectionPoint>) {
        scope.launch { _liveHistoryFlow.emit(ribbonKey to points) }
        
        val health = telemetry.systemHealth.value
        if (!PersistencePolicy.shouldSaveHistoryPoint(health)) return

        points.forEach { point ->
            historyBuffer.add(HistoryEntity(
                ts = point.ts, rt = point.rt, rtt = point.rtt, isConnected = point.isConnected, isGap = point.isGap, 
                isRecoveryEvent = point.isRecoveryEvent,
                hasGps = point.hasGps, isTick = point.isTick, ribbonKey = ribbonKey,
                isBatterySteepDischarge = point.isBatterySteepDischarge,
                remoteSig = point.remoteSig,
                isCoolingModeActive = point.isCoolingModeActive,
                isBatteryLow = point.isBatteryLow,
                isBatteryCritical = point.isBatteryCritical,
                speed = point.speed, bearing = point.bearing,
                currentMa = point.currentMa,
                locationPendingReason = point.locationPendingReason.name,
                accuracy = point.gpsAccuracy,
                maxAccuracy = point.maxAccuracy,
                
                gpsIndex = point.gpsIndex,
                noiseIdx = point.noiseIdx,
                luxIdx = point.luxIdx,
                vibeIdx = point.vibeIdx,
                proxIdx = point.proxIdx,
                liftIdx = point.liftIdx,
                snrIdx = point.snrIdx,
                tiltIdx = point.tiltIdx,
                baroIdx = point.baroIdx,
                isSitDetected = point.isSitDetected,
                isSitActive = point.isSitActive,
                sitBaro = point.sitBaro,
                sitTilt = point.sitTilt,
                sitShock = point.sitShock
            ))
        }

        val nowRt = timeProvider.elapsedRealtime()
        val shouldWrite = (nowRt - lastBatchWriteRealtime > HISTORY_BATCH_WRITE_INTERVAL_MS) || (historyBuffer.size >= HISTORY_BUFFER_MAX_SIZE)
        
        if (shouldWrite) {
            scope.launch { flushHistoryBufferInternal(nowRt) }
        }
    }

    suspend fun flushHistory() {
        flushHistoryBufferInternal(timeProvider.elapsedRealtime())
    }

    private suspend fun flushHistoryBufferInternal(nowRt: Long) = withContext(Dispatchers.IO) {
        val dbPoints = mutableListOf<HistoryEntity>()
        while (historyBuffer.isNotEmpty()) historyBuffer.poll()?.let { dbPoints.add(it) }
        
        if (dbPoints.isNotEmpty()) {
            lastBatchWriteRealtime = nowRt
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_DB_WRITE_MS,
                operation = "History Batch Write (${dbPoints.size} pts)",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> logLatencySpike(message) }
            ) {
                database.withTransaction {
                    historyDao.insertAll(dbPoints)
                    historyWriteCount += dbPoints.size
                }
                
                if (historyWriteCount >= DB_PRUNE_THRESHOLD_HISTORY) {
                    historyWriteCount = 0
                    triggerBackgroundPruning()
                }
            }
        }
    }

    private fun triggerBackgroundPruning() {
        if (isPruningActive.getAndSet(true)) return
        
        val health = telemetry.systemHealth.value
        // Issue #129: Defer background maintenance during critical battery states
        if (health.isBatteryCritical) {
            isPruningActive.set(false)
            return
        }

        scope.launch {
            try {
                LatencyMonitor.measureAndAudit<Unit>(
                    timeProvider = timeProvider,
                    thresholdMs = LATENCY_THRESHOLD_DB_WRITE_MS * 4,
                    operation = "Background Pruning",
                    type = LatencyMonitor.AuditType.IO,
                    onSpike = { message, _ -> logLatencySpike(message) }
                ) {
                    database.withTransaction {
                        listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { key ->
                            historyDao.pruneHistory(key)
                        }
                        trailDao.pruneTrail(false)
                        trailDao.pruneTrail(true)
                        violationDao.prune()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Issue #585: Background pruning failed")
            } finally {
                isPruningActive.set(false)
            }
        }
    }

    private fun logLatencySpike(message: String) {
        Timber.w(message)
        addLog(LogEntry(
            localId = UUID.randomUUID().toString(),
            timestamp = timeProvider.currentTimeMillis(),
            message = message,
            type = "SYSTEM",
            isImportant = false,
            id = "SYSTEM",
            viewerId = "SYSTEM",
            isSpecial = true,
            specialColor = FORENSIC_PINK_COLOR
        ), initiallySynced = true)
    }

    fun saveTrackerState(status: TrackerStatus) = settings.saveTrackerState(status)
    suspend fun loadTrackerState() = settings.loadTrackerState()
    suspend fun getLastAlarmAckTs(): Long = settings.getLong(LAST_ALARM_ACK_TS_KEY, 0L)

    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        offlineRepository.addPendingStatusUpdate(update)
    }

    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> = offlineRepository.getPendingStatusUpdates(limit)
    suspend fun deletePendingStatusUpdate(id: Long) = offlineRepository.deletePendingStatusUpdate(id)
    
    suspend fun getLastAlarmsJson(): String = settings.getString(LAST_ALARMS_JSON_KEY, "[]")
    fun saveAlarmsJsonSync(json: String) { scope.launch { settings.saveString(LAST_ALARMS_JSON_KEY, json) } }

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

    /**
     * Issue #729: Runs database integrity check and returns status.
     */
    suspend fun checkDatabaseIntegrity(): String = withContext(Dispatchers.IO) {
        database.checkIntegrity()
    }
}
