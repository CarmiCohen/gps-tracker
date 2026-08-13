package com.gps19.app

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * LogRepository: Dedicated repository for application logs.
 * Aug.13.12:
 * - Issue #164: Forensic Log Buffer Audit. Implemented deterministic 
 *   composite IDs (F-timestamp-idx) for forensic traces to eliminate UUID 
 *   churn. Utilizing new snapshot columns to avoid string concatenation 
 *   in high-frequency drainage (R164).
 * Aug.13.02:
 * - Build Fix: Resolved type inference failures on budget hardware toolchains by 
 *   explicitly typing LatencyMonitor and withLock calls.
 */
@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    private val forensicSpillBufferProvider: Provider<ForensicSpillBuffer>,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val telemetry: TelemetryRepository
) {
    private val logMutex = Mutex()
    private var logWriteCount = 0
    private val isPruning = AtomicBoolean(false)

    private val forensicSuccessCount = AtomicInteger(0)
    private val forensicFailureCount = AtomicInteger(0)
    private var liveReliability = 1.0

    private val logBuffer = Channel<BufferedLog>(LOG_BUFFER_CAPACITY)

    private data class BufferedLog(val entry: LogEntry, val initiallySynced: Boolean)

    companion object {
        private val BRACKET_REGEX = Regex("""\[.*?\]""")
        private val PAREN_REGEX = Regex("""\(.*?\)""")
        private val INTERRUPTION_REGEX = Regex("""\s*after an interruption of[^.]+""", RegexOption.IGNORE_CASE)
        private val COLON_VALUE_REGEX = Regex(""":\s*-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        private val SPACE_VALUE_REGEX = Regex("""\s+-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        
        private const val FORENSIC_FILL_THRESHOLD = FORENSIC_SPILL_CAPACITY / 2
        private const val FORENSIC_CONVERGENCE_STALL_LIMIT = 3
        private const val FORENSIC_EMERGENCY_FILL_LEVEL = 0.9
        private const val RELIABILITY_EMA_ALPHA = 0.1 
    }

    init {
        startBatchProcessor()
        recoverAbandonedTraces()
        startForensicDrainer()
    }

    private fun startBatchProcessor() {
        scope.launch(Dispatchers.IO) {
            val batch = mutableListOf<BufferedLog>()
            var lastFlushTime = timeProvider.currentTimeMillis()

            while (isActive) {
                try {
                    val log = withTimeoutOrNull(LOG_BATCH_DELAY_MS) {
                        logBuffer.receive()
                    }

                    if (log != null) {
                        batch.add(log)
                    }

                    val now = timeProvider.currentTimeMillis()
                    if (batch.size >= LOG_BATCH_SIZE || (batch.isNotEmpty() && now - lastFlushTime >= LOG_BATCH_DELAY_MS)) {
                        flushBatch(batch)
                        batch.clear()
                        lastFlushTime = now
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error in log batch processor")
                }
            }
        }
    }

    private fun recoverAbandonedTraces() {
        scope.launch(Dispatchers.IO) {
            val buffer = forensicSpillBufferProvider.get()
            val pending = buffer.getPendingCount()
            if (pending > 0) {
                Timber.i("Forensic Recovery: Replaying $pending abandoned traces.")
                performForensicDrain(limit = FORENSIC_SPILL_CAPACITY, isRecovery = true)
            }
        }
    }

    private fun startForensicDrainer() {
        scope.launch(Dispatchers.IO) {
            var lastDrainTime = timeProvider.currentTimeMillis()
            
            while (isActive) {
                try {
                    val health = telemetry.systemHealth.value
                    
                    val loadFactor = (health.cpuLoad / 8.0).coerceIn(0.0, 1.0)
                    val dynamicDelay = FORENSIC_DRAIN_THROTTLE_MIN_MS + 
                                     ((FORENSIC_DRAIN_THROTTLE_MAX_MS - FORENSIC_DRAIN_THROTTLE_MIN_MS) * loadFactor).toLong()
                    
                    delay(dynamicDelay)
                    
                    val now = timeProvider.currentTimeMillis()
                    val buffer = forensicSpillBufferProvider.get()
                    val pendingAtStart = buffer.getPendingCount()
                    val fillLevel = pendingAtStart.toDouble() / FORENSIC_SPILL_CAPACITY
                    val isEmergency = fillLevel >= FORENSIC_EMERGENCY_FILL_LEVEL
                    
                    val shouldDrain = isEmergency || 
                                    (pendingAtStart >= FORENSIC_FILL_THRESHOLD && loadFactor < 0.8) || 
                                    (pendingAtStart > 0 && now - lastDrainTime >= FORENSIC_DRAIN_INTERVAL_MS)
                    
                    if (shouldDrain) {
                        val baseBatchSize = FORENSIC_BATCH_SIZE_MIN + 
                                          ((FORENSIC_BATCH_SIZE_MAX - FORENSIC_BATCH_SIZE_MIN) * fillLevel).toInt()
                        
                        val dynamicBatchSize = (baseBatchSize * (1.0 - (loadFactor * 0.4))).toInt()
                            .coerceIn(FORENSIC_BATCH_SIZE_MIN, FORENSIC_BATCH_SIZE_MAX)

                        if (performForensicDrain(limit = dynamicBatchSize, isRecovery = false)) {
                            lastDrainTime = now
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Timber.e(e, "Error in forensic drainer loop")
                }
            }
        }
    }

    /**
     * performForensicDrain: Optimized drain logic for telemetry persistence.
     * R164: Utilizing deterministic composite IDs for forensic traces. Allocated 
     * objects only for traces passing the deduplication gate.
     */
    private suspend fun performForensicDrain(limit: Int, isRecovery: Boolean): Boolean {
        val buffer = forensicSpillBufferProvider.get()
        val pendingAtStart = buffer.getPendingCount()
        val traces = buffer.peek(limit)
        if (traces.isEmpty()) {
            if (pendingAtStart > 0) {
                buffer.commitDrain(pendingAtStart)
            }
            return false
        }

        return try {
            var minTs = Long.MAX_VALUE
            for (t in traces) { if (t.timestamp < minTs) minTs = t.timestamp }
            if (minTs == Long.MAX_VALUE) minTs = 0L

            val existingSignatures = logDao.getExistingForensicSignatures(minTs).toSet()

            val toInsert = ArrayList<LogEntity>()
            for (trace in traces) {
                val signature = ForensicSignature(trace.timestamp, trace.spillIdx)
                if (!existingSignatures.contains(signature)) {
                    // R164: Deterministic ID generation to replace UUID.randomUUID()
                    val compositeId = if (trace.localId.isNotEmpty()) trace.localId 
                                     else "F-${trace.timestamp}-${trace.spillIdx}"
                    
                    toInsert.add(LogEntity(
                        localId = compositeId,
                        timestamp = trace.timestamp, message = trace.message, type = trace.type,
                        isImportant = trace.isImportant, deviceId = trace.id, viewerId = trace.viewerId,
                        isSpecial = trace.isSpecial, role = trace.role,
                        lat = trace.lat, lng = trace.lng, accuracy = trace.accuracy,
                        maxAccuracy = trace.maxAccuracy, snrSnapshot = trace.snrSnapshot,
                        vibeSnapshot = trace.vibeSnapshot, synced = false,
                        spillIdx = trace.spillIdx,
                        gpsHardwareLock = trace.gpsHardwareLock,
                        tempSnapshot = trace.tempSnapshot,
                        battSnapshot = trace.battSnapshot,
                        chargingSnapshot = trace.chargingSnapshot
                    ))
                }
            }
            
            if (toInsert.isNotEmpty()) {
                logDao.insertAll(toInsert)
            }
            
            buffer.commitDrain(traces.size)
            forensicSuccessCount.incrementAndGet()
            updateReliability(true)
            
            if (isRecovery && toInsert.isNotEmpty()) {
                val msg = "Forensic Recovery Successful: ${toInsert.size} traces replayed."
                addLog(LogEntry(
                    localId = "RECOVERY-${timeProvider.currentTimeMillis()}",
                    timestamp = timeProvider.currentTimeMillis(),
                    message = msg, type = "SYSTEM", isImportant = true,
                    id = "SYSTEM", viewerId = "SYSTEM", isSpecial = true,
                    specialColor = FORENSIC_PINK_COLOR
                ), initiallySynced = true)
            }
            
            if (!isRecovery) {
                checkDrainConvergence(pendingAtStart)
            }

            logMutex.withLock<Unit> {
                logWriteCount += toInsert.size
                if (logWriteCount >= DB_PRUNE_THRESHOLD) {
                    logWriteCount = 0
                    triggerAsyncPruning()
                }
            }
            true
        } catch (e: Exception) {
            forensicFailureCount.incrementAndGet()
            updateReliability(false)
            Timber.e(e, "Forensic drain failed")
            false
        }
    }

    private var consecutiveStalls = 0
    private fun checkDrainConvergence(pendingAtStart: Int) {
        val buffer = forensicSpillBufferProvider.get()
        val pendingAfter = buffer.getPendingCount()
        if (pendingAfter > 0) {
            if (pendingAfter >= pendingAtStart) {
                consecutiveStalls++
                if (consecutiveStalls >= FORENSIC_CONVERGENCE_STALL_LIMIT) {
                    val h = telemetry.systemHealth.value
                    // R164: Formatted message only created on actual failure path.
                    val msg = "Forensic Stall Correlated: Backfill not converging ($pendingAfter pending). System: [CPU: ${h.cpuLoad}, IOW: ${h.ioWait}, Temp: ${h.batteryTemp}C, Batt: ${h.batteryLevel}%]"
                    Timber.w(msg)
                    addLog(LogEntry(
                        localId = "STALL-${timeProvider.currentTimeMillis()}",
                        timestamp = timeProvider.currentTimeMillis(),
                        message = msg, type = "SYSTEM", isImportant = true,
                        id = "SYSTEM", viewerId = "SYSTEM", isSpecial = true,
                        specialColor = FORENSIC_PINK_COLOR
                    ), initiallySynced = true)
                }
            } else {
                consecutiveStalls = 0
            }
        } else {
            consecutiveStalls = 0
        }
    }

    private fun updateReliability(success: Boolean) {
        val currentSample = if (success) 1.0 else 0.0
        liveReliability = (RELIABILITY_EMA_ALPHA * currentSample) + ((1.0 - RELIABILITY_EMA_ALPHA) * liveReliability)
        
        val health = telemetry.systemHealth.value
        health.forensicReliability = liveReliability
        telemetry.updateHealth(health)
    }

    private suspend fun flushBatch(batch: List<BufferedLog>) {
        if (batch.isEmpty()) return

        logMutex.withLock<Unit> {
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LOG_LATENCY_THRESHOLD_MS,
                operation = "Log batch write [size: ${batch.size}]",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                try {
                    val toInsert = mutableListOf<LogEntity>()
                    
                    for (buffered in batch) {
                        val entry = buffered.entry
                        val initiallySynced = buffered.initiallySynced
                        val currentBase = stripLogVariableParts(entry.message)

                        val existing = if (entry.localId.isNotBlank()) logDao.getLogByLocalId(entry.localId) else null
                        if (existing != null) {
                            logDao.update(existing.copy(
                                timestamp = entry.timestamp, message = entry.message, type = entry.type,
                                isImportant = entry.isImportant, extremeValue = entry.extremeValue,
                                count = entry.count, durationMs = entry.durationMs, isSpecial = entry.isSpecial,
                                specialColor = entry.specialColor, role = entry.role, synced = initiallySynced,
                                lat = entry.lat, lng = entry.lng, accuracy = entry.accuracy,
                                maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot,
                                vibeSnapshot = entry.vibeSnapshot,
                                gpsHardwareLock = entry.gpsHardwareLock,
                                tempSnapshot = entry.tempSnapshot,
                                battSnapshot = entry.battSnapshot,
                                chargingSnapshot = entry.chargingSnapshot
                            ))
                            continue
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
                                    maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot,
                                    gpsHardwareLock = entry.gpsHardwareLock,
                                    tempSnapshot = entry.tempSnapshot,
                                    battSnapshot = entry.battSnapshot,
                                    chargingSnapshot = entry.chargingSnapshot
                                ))
                                continue
                            }
                        }

                        toInsert.add(LogEntity(
                            localId = if (entry.localId.isBlank()) "L-${entry.timestamp}-${UUID.randomUUID().toString().take(4)}" else entry.localId, 
                            timestamp = entry.timestamp, message = entry.message, type = entry.type, 
                            isImportant = entry.isImportant, deviceId = entry.id, viewerId = entry.viewerId, 
                            count = entry.count, extremeValue = entry.extremeValue, durationMs = durationMs, 
                            isSpecial = entry.isSpecial, specialColor = specialColor,
                            firstSeenTs = if (entry.firstSeenTs == 0L) (entry.timestamp - entry.durationMs) else entry.firstSeenTs,
                            role = entry.role, synced = initiallySynced, lat = entry.lat, lng = entry.lng, 
                            accuracy = entry.accuracy, maxAccuracy = entry.maxAccuracy, 
                            snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot,
                            spillIdx = entry.spillIdx,
                            gpsHardwareLock = entry.gpsHardwareLock,
                            tempSnapshot = entry.tempSnapshot,
                            battSnapshot = entry.battSnapshot,
                            chargingSnapshot = entry.chargingSnapshot
                        ))
                        logWriteCount++
                    }

                    if (toInsert.isNotEmpty()) {
                        logDao.insertAll(toInsert)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error flushing log batch to database")
                }
            }
        }

        if (logWriteCount >= DB_PRUNE_THRESHOLD) {
            logWriteCount = 0
            triggerAsyncPruning()
        }
    }

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logDao.getAllLogs(limit)
        .onEach { 
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LOG_RETRIEVAL_THRESHOLD_MS,
                operation = "Log retrieval [limit: $limit]",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> Timber.w(message) }
            ) { }
        }
        .map { entities ->
            entities.map { 
                LogEntry(
                    localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
                    isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count, 
                    extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial, 
                    specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
                    lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                    snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot,
                    spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                    tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                    chargingSnapshot = it.chargingSnapshot
                ) 
            }
        }.flowOn(Dispatchers.Default)

    /**
     * addLog: Entry point for all logging.
     * Issue #151: Forensic traces are offloaded to Dispatchers.Default to ensure 
     * that MappedByteBuffer I/O stalls on the persistence lock never block 
     * the caller (which may be the Main thread during UI events).
     */
    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        if (entry.type == "FORENSIC_TRACE") {
            scope.launch(Dispatchers.Default) {
                forensicSpillBufferProvider.get().writeTrace(entry)
            }
            return
        }

        val result = logBuffer.trySend(BufferedLog(entry, initiallySynced))
        if (result.isFailure) {
            Timber.w("Log buffer full, dropping log: ${entry.message}")
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
        LatencyMonitor.measureAndAudit<Unit>(
            timeProvider = timeProvider,
            thresholdMs = LOG_LATENCY_THRESHOLD_MS,
            operation = "Proactive pruning",
            type = LatencyMonitor.AuditType.IO,
            onSpike = { message, _ -> Timber.w(message) }
        ) {
            try {
                val health = telemetry.systemHealth.value
                val count = logDao.getCount()
                
                val threshold = when {
                    health.isStorageCritical -> ADAPTIVE_PRUNE_THRESHOLD_CRITICAL
                    health.isBatteryCritical -> ADAPTIVE_PRUNE_THRESHOLD_CHARGING 
                    health.isStorageLow -> ADAPTIVE_PRUNE_THRESHOLD_LOW
                    health.isBatteryLow -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL
                    health.isCharging && !health.isCoolingModeActive -> ADAPTIVE_PRUNE_THRESHOLD_CHARGING
                    else -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL
                }

                if (count > threshold) {
                    val heartbeatTarget = if (health.isStorageLow) 50 else 100
                    val generalTarget = if (health.isStorageLow) 300 else 500
                    
                    val maxChunks = when {
                        health.isBatteryCritical -> 1
                        health.isBatteryLow -> 2
                        health.isStorageCritical -> 10
                        else -> 5
                    }
                    
                    var totalPruned = 0
                    repeat(maxChunks) { 
                        val p1 = logDao.pruneRoutineHeartbeatsChunk(heartbeatTarget, PRUNE_CHUNK_SIZE)
                        val p2 = logDao.pruneNonForensicLogsChunk(generalTarget, PRUNE_CHUNK_SIZE)
                        
                        val p3 = if (count > LOG_LIMIT_STRICT) {
                            logDao.pruneSpecialLogsChunk(LOG_LIMIT_STANDARD, PRUNE_CHUNK_SIZE)
                        } else 0
                        
                        val chunkPruned = p1 + p2 + p3
                        totalPruned += chunkPruned
                        
                        if (chunkPruned < PRUNE_CHUNK_SIZE) return@repeat 
                        delay(if (health.isBatteryLow) 200 else 50)
                    }
                    
                    if (totalPruned > 0) {
                        Timber.d("Proactive pruning [Threshold: $threshold, Count: $count, Pruned: $totalPruned].")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during proactive pruning")
            }
        }
    }

    suspend fun getUnsyncedLogs(limit: Int): List<LogEntry> = LatencyMonitor.measureAndAudit<List<LogEntry>>(
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
                snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot,
                spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                chargingSnapshot = it.chargingSnapshot
            )
        }
    }

    suspend fun markLogsAsSynced(localIds: List<String>) = LatencyMonitor.measureAndAudit<Unit>(
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

    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = LatencyMonitor.measureAndAudit<List<LogEntry>>(
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
                snrSnapshot = it.snrSnapshot, vibeSnapshot = it.vibeSnapshot,
                spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                chargingSnapshot = it.chargingSnapshot
            ) 
        }
    }
}
