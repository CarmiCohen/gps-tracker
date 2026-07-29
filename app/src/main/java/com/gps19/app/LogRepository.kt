package com.gps19.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LogRepository: Dedicated repository for application logs.
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized 
 *   forensic audit log messages and migrated to measureAndAudit API.
 * July.27.07:
 * - Issue #605: Forensic Log Latency Audit. Fixed critical 'it' reference error.
 */
@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider
) {
    private val logMutex = Mutex()
    private var logWriteCount = 0
    private val isPruning = AtomicBoolean(false)

    companion object {
        private val BRACKET_REGEX = Regex("""\[.*?\]""")
        private val PAREN_REGEX = Regex("""\(.*?\)""")
        private val INTERRUPTION_REGEX = Regex("""\s*after an interruption of[^.]+""", RegexOption.IGNORE_CASE)
        private val COLON_VALUE_REGEX = Regex(""":\s*-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        private val SPACE_VALUE_REGEX = Regex("""\s+-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
    }

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logDao.getAllLogs(limit)
        .onEach { 
            LatencyMonitor.measureAndAudit(
                timeProvider = timeProvider,
                thresholdMs = LOG_RETRIEVAL_THRESHOLD_MS,
                operation = "Log retrieval [limit: $limit]",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> Timber.w(message) }
            ) { /* measurement only */ }
        }
        .map { entities ->
            entities.map { 
                LogEntry(
                    localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
                    isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count, 
                    extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial, 
                    specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
                    lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                    snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot
                ) 
            }
        }.flowOn(Dispatchers.Default)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        val currentBase = stripLogVariableParts(entry.message)
        
        scope.launch(Dispatchers.IO) {
            logMutex.withLock {
                LatencyMonitor.measureAndAudit(
                    timeProvider = timeProvider,
                    thresholdMs = LOG_LATENCY_THRESHOLD_MS,
                    operation = "Log write [type: ${entry.type}]",
                    type = LatencyMonitor.AuditType.IO,
                    onSpike = { message, _ -> Timber.w(message) }
                ) {
                    try {
                        val existing = if (entry.localId.isNotBlank()) logDao.getLogByLocalId(entry.localId) else null
                        if (existing != null) {
                            logDao.update(existing.copy(
                                timestamp = entry.timestamp, message = entry.message, type = entry.type,
                                isImportant = entry.isImportant, extremeValue = entry.extremeValue,
                                count = entry.count, durationMs = entry.durationMs, isSpecial = entry.isSpecial,
                                specialColor = entry.specialColor, role = entry.role, synced = initiallySynced,
                                lat = entry.lat, lng = entry.lng, accuracy = entry.accuracy,
                                maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot
                            ))
                            return@measureAndAudit
                        }

                        val last = logDao.getLastLogByMetadata(entry.type, entry.role, entry.id)
                        if (last != null) {
                            val lastBase = stripLogVariableParts(last.message)
                            if (lastBase == currentBase && lastBase.isNotEmpty() && last.isSpecial == entry.isSpecial) {
                                logDao.update(last.copy(
                                    localId = if (last.localId.isBlank()) entry.localId else last.localId,
                                    count = last.count + entry.count,
                                    durationMs = last.durationMs + entry.durationMs,
                                    extremeValue = if (entry.extremeValue != null) {
                                        val lastExtreme = last.extremeValue ?: 0.0
                                        if (abs(entry.extremeValue) > abs(lastExtreme)) entry.extremeValue else lastExtreme
                                    } else last.extremeValue,
                                    timestamp = entry.timestamp, message = entry.message, synced = initiallySynced,
                                    lat = entry.lat, lng = entry.lng, accuracy = entry.accuracy,
                                    maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot
                                ))
                                return@measureAndAudit
                            }
                        }
                        
                        logDao.insert(LogEntity(
                            localId = if (entry.localId.isBlank()) UUID.randomUUID().toString() else entry.localId, 
                            timestamp = entry.timestamp, message = entry.message, type = entry.type, 
                            isImportant = entry.isImportant, deviceId = entry.id, viewerId = entry.viewerId, 
                            count = entry.count, extremeValue = entry.extremeValue, durationMs = entry.durationMs, 
                            isSpecial = entry.isSpecial, specialColor = entry.specialColor,
                            firstSeenTs = if (entry.firstSeenTs == 0L) (entry.timestamp - entry.durationMs) else entry.firstSeenTs,
                            role = entry.role, synced = initiallySynced, lat = entry.lat, lng = entry.lng, 
                            accuracy = entry.accuracy, maxAccuracy = entry.maxAccuracy, 
                            snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot
                        ))
                        logWriteCount++
                    } catch (e: Exception) {
                        Timber.e(e, "Error adding log to database")
                    }
                }
            }

            if (logWriteCount >= DB_PRUNE_THRESHOLD) {
                logWriteCount = 0
                triggerAsyncPruning()
            }
        }
    }

    private fun triggerAsyncPruning() {
        if (isPruning.compareAndSet(false, true)) {
            scope.launch(Dispatchers.IO) {
                try { proactivePruning() } finally { isPruning.set(false) }
            }
        }
    }

    suspend fun proactivePruning() {
        LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = LOG_LATENCY_THRESHOLD_MS,
            operation = "Proactive pruning",
            type = LatencyMonitor.AuditType.IO,
            onSpike = { message, _ -> Timber.w(message) }
        ) {
            try {
                val count = logDao.getCount()
                if (count > 1000) {
                    logDao.deepPruneLogs()
                    Timber.d("Proactive pruning completed. Current log count: $count")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during proactive pruning")
            }
        }
    }

    suspend fun getUnsyncedLogs(limit: Int): List<LogEntry> = LatencyMonitor.measureAndAudit(
        timeProvider = timeProvider,
        thresholdMs = LOG_RETRIEVAL_THRESHOLD_MS,
        operation = "Unsynced log retrieval [limit: $limit]",
        type = LatencyMonitor.AuditType.IO,
        onSpike = { message, _ -> Timber.w(message) }
    ) {
        logDao.getUnsyncedLogs(limit).map {
            LogEntry(
                localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type,
                isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count,
                extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial,
                specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
                lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot
            )
        }
    }

    suspend fun markLogsAsSynced(localIds: List<String>) = LatencyMonitor.measureAndAudit(
        timeProvider = timeProvider,
        thresholdMs = LOG_LATENCY_THRESHOLD_MS,
        operation = "Sync status update [count: ${localIds.size}]",
        type = LatencyMonitor.AuditType.IO,
        onSpike = { message, _ -> Timber.w(message) }
    ) {
        logDao.markLogsAsSynced(localIds)
    }

    private fun stripLogVariableParts(message: String): String {
        var m = message
        m = m.replace(BRACKET_REGEX, "")
        m = m.replace(PAREN_REGEX, "")
        m = m.replace(INTERRUPTION_REGEX, "")
        m = m.replace(COLON_VALUE_REGEX, "")
        m = m.replace(SPACE_VALUE_REGEX, "")
        return m.trim().trimEnd('.')
    }

    fun clearLogs() { 
        scope.launch(Dispatchers.IO) {
            try { logDao.clearAll() } catch (e: Exception) { Timber.e(e, "Error clearing logs") }
        } 
    }

    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = LatencyMonitor.measureAndAudit(
        timeProvider = timeProvider,
        thresholdMs = LOG_RETRIEVAL_THRESHOLD_MS,
        operation = "Static log retrieval [limit: $limit]",
        type = LatencyMonitor.AuditType.IO,
        onSpike = { message, _ -> Timber.w(message) }
    ) {
        logDao.getAllLogsStatic(limit).map {
            LogEntry(
                localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
                isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count, 
                extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial, 
                specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
                lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot
            ) 
        }
    }
}
