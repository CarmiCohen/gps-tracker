package com.gps19.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs
import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.DB_PRUNE_THRESHOLD
import com.gps19.core.engine.LOG_LATENCY_THRESHOLD_MS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LogRepository: Dedicated repository for application logs.
 * July.27.00:
 * - Architecture Audit: Updated to use centralized EngineConstants and fixed it/entry reference error.
 * July.25.11:
 * - Issue #590: Unified Latency Monitoring. Integrated LatencyMonitor into addLog 
 *   and proactivePruning to detect I/O bottlenecks.
 */
@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider
) {
    private val logMutex = Mutex()
    private var logWriteCount = 0

    val eventLogsFlow: Flow<List<LogEntry>> = logDao.getAllLogs().map { entities ->
        entities.map { 
            LogEntry(
                localId = it.localId, 
                timestamp = it.timestamp, 
                message = it.message, 
                type = it.type, 
                isImportant = it.isImportant, 
                id = it.deviceId, 
                viewerId = it.viewerId, 
                count = it.count, 
                extremeValue = it.extremeValue, 
                durationMs = it.durationMs, 
                isSpecial = it.isSpecial, 
                specialColor = it.specialColor, 
                firstSeenTs = it.firstSeenTs,
                role = it.role,
                lat = it.lat,
                lng = it.lng,
                accuracy = it.accuracy,
                maxAccuracy = it.maxAccuracy,
                snrSnapshot = it.snrSnapshot,
                vibeSnapshot = it.vibeSnapshot
            ) 
        }
    }.flowOn(Dispatchers.Default)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            logMutex.withLock {
                LatencyMonitor.measure(
                    timeProvider = timeProvider,
                    thresholdMs = LOG_LATENCY_THRESHOLD_MS,
                    onSpike = { duration -> 
                        Timber.w("Forensic I/O Audit: Slow log write detected: ${duration}ms for ${entry.type}")
                    }
                ) {
                    try {
                        val existing = if (entry.localId.isNotBlank()) logDao.getLogByLocalId(entry.localId) else null
                        if (existing != null) {
                            logDao.update(existing.copy(
                                timestamp = entry.timestamp,
                                message = entry.message,
                                type = entry.type,
                                isImportant = entry.isImportant,
                                extremeValue = entry.extremeValue,
                                count = entry.count,
                                durationMs = entry.durationMs,
                                isSpecial = entry.isSpecial,
                                specialColor = entry.specialColor,
                                role = entry.role,
                                synced = initiallySynced,
                                lat = entry.lat,
                                lng = entry.lng,
                                accuracy = entry.accuracy,
                                maxAccuracy = entry.maxAccuracy,
                                snrSnapshot = entry.snrSnapshot,
                                vibeSnapshot = entry.vibeSnapshot
                            ))
                            return@measure
                        }

                        val last = logDao.getLastLogByMetadata(entry.type, entry.role, entry.id)
                        if (last != null) {
                            val lastBase = stripLogVariableParts(last.message)
                            val currentBase = stripLogVariableParts(entry.message)
                            
                            if (lastBase == currentBase && lastBase.isNotEmpty() && last.isSpecial == entry.isSpecial) {
                                val newCount = last.count + entry.count
                                val newDuration = last.durationMs + entry.durationMs
                                val newExtreme = if (entry.extremeValue != null) {
                                    val lastExtreme = last.extremeValue ?: 0.0
                                    if (abs(entry.extremeValue) > abs(lastExtreme)) entry.extremeValue else lastExtreme
                                } else last.extremeValue
                                
                                logDao.update(last.copy(
                                    localId = if (last.localId.isBlank()) entry.localId else last.localId,
                                    count = newCount,
                                    durationMs = newDuration,
                                    extremeValue = newExtreme,
                                    timestamp = entry.timestamp,
                                    message = entry.message,
                                    synced = initiallySynced,
                                    lat = entry.lat,
                                    lng = entry.lng,
                                    accuracy = entry.accuracy,
                                    maxAccuracy = entry.maxAccuracy,
                                    snrSnapshot = entry.snrSnapshot,
                                    vibeSnapshot = entry.vibeSnapshot
                                ))
                                return@measure
                            }
                        }
                        
                        logDao.insert(LogEntity(
                            localId = if (entry.localId.isBlank()) UUID.randomUUID().toString() else entry.localId, 
                            timestamp = entry.timestamp, 
                            message = entry.message, 
                            type = entry.type, 
                            isImportant = entry.isImportant, 
                            deviceId = entry.id, 
                            viewerId = entry.viewerId, 
                            count = entry.count, 
                            extremeValue = entry.extremeValue, 
                            durationMs = entry.durationMs, 
                            isSpecial = entry.isSpecial, 
                            specialColor = entry.specialColor,
                            firstSeenTs = if (entry.firstSeenTs == 0L) (entry.timestamp - entry.durationMs) else entry.firstSeenTs,
                            role = entry.role,
                            synced = initiallySynced,
                            lat = entry.lat,
                            lng = entry.lng,
                            accuracy = entry.accuracy,
                            maxAccuracy = entry.maxAccuracy,
                            snrSnapshot = entry.snrSnapshot,
                            vibeSnapshot = entry.vibeSnapshot
                        ))
                        
                        logWriteCount++
                        if (logWriteCount >= DB_PRUNE_THRESHOLD) {
                            logWriteCount = 0
                            if (logDao.getCount() > 1000) {
                                logDao.deepPruneLogs()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error adding log to database")
                    }
                }
            }
        }
    }

    /**
     * Executes a deep prune of the log table.
     */
    suspend fun proactivePruning() {
        logMutex.withLock {
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LOG_LATENCY_THRESHOLD_MS,
                onSpike = { duration -> 
                    Timber.w("Forensic I/O Audit: Slow proactive pruning detected: ${duration}ms")
                }
            ) {
                try {
                    logDao.deepPruneLogs()
                    Timber.d("Proactive pruning completed.")
                } catch (e: Exception) {
                    Timber.e(e, "Error during proactive pruning")
                }
            }
        }
    }

    suspend fun getUnsyncedLogs(limit: Int): List<LogEntry> = logDao.getUnsyncedLogs(limit).map {
        LogEntry(
            localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type,
            isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count,
            extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial,
            specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
            lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
            snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot
        )
    }

    suspend fun markLogsAsSynced(localIds: List<String>) = logDao.markLogsAsSynced(localIds)

    private fun stripLogVariableParts(message: String): String {
        var m = message
        m = m.replace(Regex("""\[.*?\]"""), "")
        m = m.replace(Regex("""\(.*?\)"""), "")
        m = m.replace(Regex("""\s*after an interruption of[^.]+""", RegexOption.IGNORE_CASE), "")
        m = m.replace(Regex(""":\s*-?\d+(\.\d+)?\s*[A-Za-z%°]*"""), "")
        m = m.replace(Regex("""\s+-?\d+(\.\d+)?\s*[A-Za-z%°]*"""), "")
        return m.trim().trimEnd('.')
    }

    fun clearLogs() { 
        scope.launch(Dispatchers.IO) {
            try {
                logDao.clearAll() 
            } catch (e: Exception) {
                Timber.e(e, "Error clearing logs")
            }
        } 
    }

    suspend fun loadAllLogsStatic(): List<LogEntry> = logDao.getAllLogsStatic().map { 
        LogEntry(
            localId = it.localId, 
            timestamp = it.timestamp, 
            message = it.message, 
            type = it.type, 
            isImportant = it.isImportant, 
            id = it.deviceId, 
            viewerId = it.viewerId, 
            count = it.count, 
            extremeValue = it.extremeValue, 
            durationMs = it.durationMs, 
            isSpecial = it.isSpecial, 
            specialColor = it.specialColor, 
            firstSeenTs = it.firstSeenTs,
            role = it.role,
            lat = it.lat,
            lng = it.lng,
            accuracy = it.accuracy,
            maxAccuracy = it.maxAccuracy,
            snrSnapshot = it.snrSnapshot,
            vibeSnapshot = it.vibeSnapshot
        ) 
    }
}
