package com.gps19.app

import android.content.Context
import androidx.room.withTransaction
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlin.math.abs

/**
 * MainRepository: Centralized data hub for the application.
 * v8.9.38:
 * - Issue #245: Mapping locationPendingReason for HistoryEntity parity.
 * v8.9.19:
 * - Issue #222: Added isHindsightCorrected propagation to trail flows and save logic.
 * v8.9.10:
 * - Issue 208: Synchronized versioning and forensic logging baseline for release verification.
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

    companion object {
        const val APP_MODE_KEY = SettingsRepository.APP_MODE_KEY
        const val TRACKER_ID_KEY = SettingsRepository.TRACKER_ID_KEY
        const val VIEWER_ID_KEY = SettingsRepository.VIEWER_ID_KEY
        const val RELAY_URL_KEY = SettingsRepository.RELAY_URL_KEY
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
        const val DEFAULT_TRACKER_ID = SettingsRepository.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SettingsRepository.DEFAULT_VIEWER_ID
        const val MAX_DISTANCE_STORAGE_KEY = SettingsRepository.MAX_DISTANCE_STORAGE_KEY
        const val DEFAULT_MAX_DISTANCE = SettingsRepository.DEFAULT_MAX_DISTANCE
        const val MAX_ACCURACY_KEY = SettingsRepository.MAX_ACCURACY_KEY
        const val MAX_TEMP_KEY = SettingsRepository.MAX_TEMP_KEY
        const val LAST_ALARM_ACK_TS_KEY = SettingsRepository.LAST_ALARM_ACK_TS_KEY
        const val HOME_POINTS_TS_KEY = SettingsRepository.HOME_POINTS_TS_KEY
        const val IS_MANUAL_EXIT_KEY = SettingsRepository.IS_MANUAL_EXIT_KEY
        const val APP_START_TIME_KEY = SettingsRepository.APP_START_TIME_KEY
        
        const val TOTAL_CONNECTED_KEY = SettingsRepository.TOTAL_CONNECTED_KEY
        const val UPTIME_KEY = SettingsRepository.UPTIME_KEY
        const val LAST_CONNECTION_TS_KEY = SettingsRepository.LAST_CONNECTION_TS_KEY
        const val LAST_DISCONNECTION_TS_KEY = SettingsRepository.LAST_DISCONNECTION_TS_KEY
        const val TOTAL_DROP_KEY = SettingsRepository.TOTAL_DROP_KEY
        const val MAX_DROP_KEY = SettingsRepository.MAX_DROP_KEY
        const val MAX_DROP_TS_KEY = SettingsRepository.MAX_DROP_TS_KEY
        const val LAST_GPS_TS_KEY = SettingsRepository.LAST_GPS_TS_KEY
        const val VIOLATION_UPTIME_MS_KEY = SettingsRepository.VIOLATION_UPTIME_MS_KEY

        const val TRACKER_LUX_BASELINE_KEY = SettingsRepository.TRACKER_LUX_BASELINE_KEY
        const val TRACKER_ACOUSTIC_FLOOR_KEY = SettingsRepository.TRACKER_ACOUSTIC_FLOOR_KEY

        const val LAST_SIT_TS_KEY = SettingsRepository.LAST_SIT_TS_KEY
        const val CHAIR_BASELINE_TILT_KEY = SettingsRepository.CHAIR_BASELINE_TILT_KEY
        
        const val SELECTED_SIREN_KEY = SettingsRepository.SELECTED_SIREN_KEY
        const val LAST_SERVICE_TICK_TS_KEY = SettingsRepository.LAST_SERVICE_TICK_TS_KEY
        const val LAST_SERVICE_TICK_REALTIME_KEY = SettingsRepository.LAST_SERVICE_TICK_REALTIME_KEY
        const val LAST_AUTO_SAVE_HOUR_KEY = SettingsRepository.LAST_AUTO_SAVE_HOUR_KEY
        
        const val LAST_DAILY_ARCHIVE_DATE_KEY = SettingsRepository.LAST_DAILY_ARCHIVE_DATE_KEY
        const val LAST_DAILY_CLEANUP_DATE_KEY = SettingsRepository.LAST_DAILY_CLEANUP_DATE_KEY

        const val DRAFT_TRACKER_ID = SettingsRepository.DRAFT_TRACKER_ID
        const val DRAFT_VIEWER_ID = SettingsRepository.DRAFT_VIEWER_ID
        const val DRAFT_RELAY_URL = SettingsRepository.DRAFT_RELAY_URL
        const val DRAFT_MAX_DISTANCE = SettingsRepository.DRAFT_MAX_DISTANCE

        const val IS_XIAOMI_MANUAL_OVERRIDE_KEY = SettingsRepository.IS_XIAOMI_MANUAL_OVERRIDE_KEY
        const val LAST_HISTORY_SIT_TS_KEY = SettingsRepository.LAST_HISTORY_SIT_TS_KEY
    }

    val isRelayConnected = telemetry.isRelayConnected
    val lastRtt = telemetry.lastRtt
    val integrityState = telemetry.integrityState
    val localLocation = telemetry.localLocation
    val trackerLocation = telemetry.trackerLocation
    val connectedViewers = telemetry.connectedViewers
    val lastRemoteActivityTs = telemetry.lastRemoteActivityTs
    val gnssDetail = telemetry.gnssDetail

    val eventLogsFlow: Flow<List<LogEntry>> = logRepository.eventLogsFlow

    val trackerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(false).map { entities -> 
        entities.map { TrailPoint(it.lat, it.lng, it.timestamp, it.isJump, it.isHindsightCorrected) } 
    }
    val viewerTrailFlow: Flow<List<TrailPoint>> = trailDao.getTrail(true).map { entities -> 
        entities.map { TrailPoint(it.lat, it.lng, it.timestamp, it.isJump, it.isHindsightCorrected) } 
    }
    val violationsFlow: Flow<List<ViolationPoint>> = violationDao.getAllFlow().map { entities -> entities.map { ViolationPoint(point = GeoPoint(it.lat, it.lng), type = it.type, ts = it.ts) } }

    private val _uiCommands = MutableSharedFlow<UiCommand>(extraBufferCapacity = 10)
    val uiCommands: SharedFlow<UiCommand> = _uiCommands.asSharedFlow()

    fun sendCommand(command: UiCommand) { scope.launch { _uiCommands.emit(command) } }

    fun updateRelayStatus(connected: Boolean) { telemetry.updateRelayStatus(connected) }
    fun updateLastRtt(rtt: Int) { telemetry.updateLastRtt(rtt) }
    fun updateIntegrity(state: IntegrityState) { telemetry.updateIntegrity(state) }
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
    val lastSitTsFlow = settings.lastSitTsFlow
    val homePointsFlow = settings.homePointsFlow
    val maxDistanceFlow = settings.maxDistanceFlow
    val alertSettingsFlow = settings.alertSettingsFlow
    val isXiaomiManualOverrideFlow = settings.isXiaomiManualOverrideFlow

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
    suspend fun saveFloat(key: String, value: Float) = settings.saveFloat(key, value)
    fun saveFloatSync(key: String, value: Float) { scope.launch { settings.saveFloat(key, value) } }
    suspend fun saveDouble(key: String, value: Double) = settings.saveDouble(key, value)
    fun saveDoubleSync(key: String, value: Double) { scope.launch { settings.saveDouble(key, value) } }
    suspend fun saveBoolean(key: String, value: Boolean) = settings.saveBoolean(key, value)
    fun saveBooleanSync(key: String, value: Boolean) { scope.launch { settings.saveBoolean(key, value) } }
    suspend fun saveInt(key: String, value: Int) = settings.saveInt(key, value)
    fun saveIntSync(key: String, value: Int) { scope.launch { settings.saveInt(key, value) } }

    suspend fun getString(key: String, default: String) = settings.getString(key, default)
    suspend fun getLong(keyName: String, default: Long) = settings.getLong(keyName, default)
    suspend fun getFloat(keyName: String, default: Float) = settings.getFloat(keyName, default)
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
    suspend fun saveDraftAlertSettings(s: AlertSettings) = settings.saveDraftAlertSettings(s)
    suspend fun loadDraftAlertSettings() = settings.loadDraftAlertSettings()
    fun clearDraftSettings() { scope.launch { settings.clearDraftSettings() } }
    
    suspend fun saveDraftSettings(deviceId: String, viewerId: String, relayUrl: String, maxDistance: Float, alertSettings: AlertSettings) = settings.saveDraftSettings(deviceId, viewerId, relayUrl, maxDistance, alertSettings)
    suspend fun commitDraftSettings() = settings.commitDraftSettings()
    suspend fun hasPendingDrafts(): Boolean = settings.hasPendingDrafts()

    suspend fun saveSettingsBulk(deviceId: String? = null, viewerId: String? = null, relayUrl: String? = null, maxDistance: Float? = null, alertSettings: AlertSettings? = null, homePoints: List<GeoPoint>? = null) = settings.saveSettingsBulk(deviceId, viewerId, relayUrl, maxDistance, alertSettings, homePoints)

    suspend fun saveSessionMetricsBulk(
        totalConnected: Long, uptime: Long, totalDrop: Long, 
        maxDrop: Long, maxDropTs: Long, lastGpsTs: Long, violationUptimeMs: Long
    ) = settings.saveSessionMetricsBulk(totalConnected, uptime, totalDrop, maxDrop, maxDropTs, lastGpsTs, violationUptimeMs)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        logRepository.addLog(entry, initiallySynced)
    }

    fun clearLogs() { logRepository.clearLogs() }
    suspend fun loadAllLogsStatic(): List<LogEntry> = logRepository.loadAllLogsStatic()

    fun saveTrailPoint(lat: Double, lng: Double, isViewer: Boolean, isJump: Boolean = false, timestamp: Long? = null, force: Boolean = false, isHindsightCorrected: Boolean = false) {
        if (lat == 0.0 || lng == 0.0) return
        
        val integrity = telemetry.integrityState.value
        if (!PersistencePolicy.shouldSaveTrailPoint(
            isStorageCritical = integrity.isStorageCritical,
            isStorageLow = integrity.isStorageLow,
            isJump = isJump,
            isSuspicious = integrity.isSuspicious
        )) return

        scope.launch {
            val wallTs = timestamp ?: timeProvider.currentTimeMillis()
            trailDao.insert(TrailEntity(
                lat = lat, lng = lng, timestamp = wallTs, 
                isViewerTrail = isViewer, isJump = isJump, 
                isHindsightCorrected = isHindsightCorrected
            ))
            
            trailWriteCount++
            if (force || trailWriteCount >= DB_PRUNE_THRESHOLD) {
                trailWriteCount = 0
                trailDao.pruneTrail(isViewer)
            }
        }
    }

    suspend fun clearTrails() = withContext(Dispatchers.IO) {
        trailDao.clearTrail(false)
        trailDao.clearTrail(true)
        violationDao.clearAll()
    }

    suspend fun loadTrailStatic(isViewer: Boolean): List<TrailPoint> = trailDao.getTrailStatic(isViewer).map { 
        TrailPoint(it.lat, it.lng, it.timestamp, it.isJump, it.isHindsightCorrected) 
    }

    suspend fun resetStats() = withContext(Dispatchers.IO) {
        settings.resetStatsBulk()
        clearTrails()
        historyDao.clearAll()
        logRepository.clearLogs()
        offlineRepository.clear()
    }

    fun addViolation(lat: Double, lng: Double, type: String, adaptiveRadius: Double = 0.0, timestamp: Long? = null) {
        if (!violationProcessor.shouldRecordViolation(lat, lng, type, adaptiveRadius)) return

        val wallTs = timestamp ?: timeProvider.currentTimeMillis()
        scope.launch { 
            violationDao.insert(ViolationEntity(lat = lat, lng = lng, type = type, ts = wallTs))
            
            violationWriteCount++
            if (violationWriteCount >= DB_PRUNE_THRESHOLD) {
                violationWriteCount = 0
                violationDao.prune()
            }
        }
    }

    fun getHistoryFlow(ribbonKey: String): Flow<List<ConnectionPoint>> = historyDao.getHistoryFlow(ribbonKey).map { l: List<HistoryEntity> -> 
        l.map { entity ->
            ConnectionPoint(
                localId = UUID.randomUUID().toString(), ts = entity.ts, rtt = entity.rtt, localSig = 10, remoteSig = entity.remoteSig,
                isConnected = entity.isConnected, isGap = entity.isGap, gpsAccuracy = 0f, isTick = entity.isTick, 
                hasGps = entity.hasGps, gpsIndex = entity.gpsIndex,
                noiseIdx = entity.noiseIdx, luxIdx = entity.luxIdx, vibeIdx = entity.vibeIdx, proxIdx = entity.proxIdx,
                liftIdx = entity.liftIdx, snrIdx = entity.snrIdx,
                verticalVelocity = entity.verticalVelocity,
                sitVz = entity.sitVz, sitDz = entity.sitDz,
                isBatterySteepDischarge = entity.isBatterySteepDischarge,
                isCoolingModeActive = entity.isCoolingModeActive,
                speed = entity.speed, bearing = entity.bearing,
                isSitDetected = entity.isSitDetected,
                isSitActive = entity.isSitActive,
                sitBaro = entity.sitBaro,
                sitTilt = entity.sitTilt,
                sitShock = entity.sitShock,
                currentMa = entity.currentMa,
                locationPendingReason = try { LocationPendingReason.valueOf(entity.locationPendingReason) } catch(e: Exception) { LocationPendingReason.NONE }
            ) 
        }
    }

    private var lastBatchWriteRealtime = 0L
    private val historyBuffer = ConcurrentLinkedQueue<HistoryEntity>()

    private val _liveHistoryFlow = MutableSharedFlow<Pair<String, List<ConnectionPoint>>>(extraBufferCapacity = 64)
    val liveHistoryFlow = _liveHistoryFlow.asSharedFlow()

    fun addHistoryPoint(ribbonKey: String, point: ConnectionPoint) {
        addHistoryPoints(ribbonKey, listOf(point))
    }

    fun addHistoryPoints(ribbonKey: String, points: List<ConnectionPoint>) {
        scope.launch { _liveHistoryFlow.emit(ribbonKey to points) }
        
        val integrity = telemetry.integrityState.value
        if (!PersistencePolicy.shouldSaveHistoryPoint(integrity.isStorageCritical)) return

        points.forEach { point ->
            historyBuffer.add(HistoryEntity(
                ts = point.ts, rtt = point.rtt, isConnected = point.isConnected, isGap = point.isGap, 
                hasGps = point.hasGps, isTick = point.isTick, ribbonKey = ribbonKey, gpsIndex = point.gpsIndex,
                noiseIdx = point.noiseIdx, luxIdx = point.luxIdx, vibeIdx = point.vibeIdx, proxIdx = point.proxIdx,
                liftIdx = point.liftIdx, snrIdx = point.snrIdx,
                verticalVelocity = point.verticalVelocity,
                sitVz = point.sitVz, sitDz = point.sitDz,
                isBatterySteepDischarge = point.isBatterySteepDischarge,
                remoteSig = point.remoteSig,
                isCoolingModeActive = point.isCoolingModeActive,
                speed = point.speed, bearing = point.bearing,
                isSitDetected = point.isSitDetected,
                isSitActive = point.isSitActive,
                sitBaro = point.sitBaro,
                sitTilt = point.sitTilt,
                sitShock = point.sitShock,
                currentMa = point.currentMa,
                locationPendingReason = point.locationPendingReason.name
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
            val start = timeProvider.elapsedRealtime()
            database.withTransaction {
                historyDao.insertAll(dbPoints)
                
                historyWriteCount += dbPoints.size
                if (historyWriteCount >= DB_PRUNE_THRESHOLD) {
                    historyWriteCount = 0
                    listOf("4M", "16M", "1H", "4H", "24H", "7D").forEach { key ->
                        historyDao.pruneHistory(key)
                    }
                }
            }
            val duration = timeProvider.elapsedRealtime() - start
            if (duration > 500) {
                Timber.w("Database I/O Audit: Slow history batch write detected: ${duration}ms for ${dbPoints.size} points")
                addLog(LogEntry(
                    localId = UUID.randomUUID().toString(),
                    timestamp = timeProvider.currentTimeMillis(),
                    message = "Forensic I/O Audit: Slow history write (${duration}ms)",
                    type = "SYSTEM",
                    isImportant = false,
                    id = "SYSTEM",
                    viewerId = "SYSTEM",
                    isSpecial = true,
                    specialColor = 0xFFFFD700.toInt()
                ), initiallySynced = true) // I/O Audit logs are local and don't need real-time sync unless recovered
            }
        }
    }

    fun saveTrackerState(status: TrackerStatus) = settings.saveTrackerState(status)
    suspend fun loadTrackerState() = settings.loadTrackerState()
    suspend fun getLastAlarmAckTs(): Long = settings.getLong(LAST_ALARM_ACK_TS_KEY, 0L)

    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        offlineRepository.addPendingStatusUpdate(update)
    }

    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> = offlineRepository.getPendingStatusUpdates(limit)
    suspend fun deletePendingStatusUpdate(id: Long) = offlineRepository.deletePendingStatusUpdate(id)
    
    private val _logFilterDetails = MutableStateFlow(false)
    val logFilterDetails = _logFilterDetails.asStateFlow()
    
    private val _logFilterRecovered = MutableStateFlow(false)
    val logFilterRecovered = _logFilterRecovered.asStateFlow()
    
    fun updateLogFilters(details: Boolean? = null, recovered: Boolean? = null) {
        details?.let { _logFilterDetails.value = it } 
        recovered?.let { _logFilterRecovered.value = it }
    }
}
