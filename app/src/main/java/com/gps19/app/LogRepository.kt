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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * LogRepository: Dedicated repository for application logs.
 * v9.4.01:
 * - Issue #104: Unified pruning logic using proactivePruning for both startup and reactive maintenance.
 */
@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    private val telemetry: TelemetryRepository
) {
    private val logMutex = Mutex()
    private var logWriteCount = 0
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val DB_PRUNE_THRESHOLD = 50
    }

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
        val integrity = telemetry.integrityState.value
        if (integrity.isStorageCritical && !entry.isSpecial) {
            return
        }

        scope.launch {
            logMutex.withLock {
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
                        return@withLock
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
                            return@withLock
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
                            // Issue #104: Use authoritative deep prune logic reactively.
                            logDao.deepPruneLogs()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error adding log to database")
                }
            }
        }
    }

    /**
     * Issue #104: Startup ANR Hardening.
     * Executes a deep prune of the log table.
     */
    suspend fun proactivePruning() {
        logMutex.withLock {
            try {
                logDao.deepPruneLogs()
                Timber.d("Proactive pruning completed.")
            } catch (e: Exception) {
                Timber.e(e, "Error during proactive pruning")
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
        scope.launch { 
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
