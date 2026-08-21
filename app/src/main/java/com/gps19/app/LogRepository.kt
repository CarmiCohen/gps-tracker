package com.gps19.app

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import androidx.room.withTransaction

/**
 * LogRepository: Dedicated repository for application logs.
 * Aug.21.06:
 * - Issue #196 Hardening: Added setForensicStallSimulation for urban multipath 
 *   validation. performForensicDrain now supports simulated failures to verify 
 *   EMA reliability degradation and alarm triggers (R196-V).
 * Aug.21.05:
 * - Issue #196 Hardening: Implemented range-based signature deduplication in 
 *   performForensicDrain to reduce query overhead during 100Hz bursts (R197).
 */
@OptIn(FlowPreview::class)
@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    private val db: AppDatabase,
    private val forensicSpillBufferProvider: Provider<ForensicSpillBuffer>,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val telemetry: TelemetryRepository
) {
    private val logMutex = Mutex()
    private val logWriteCount = AtomicInteger(0)
    private val isPruning = AtomicBoolean(false)
    private val lastPruneTime = AtomicLong(0L)

    private val forensicSuccessCount = AtomicInteger(0)
    private val forensicFailureCount = AtomicInteger(0)
    private var liveReliability = 1.0
    private val isForensicStallSimulated = AtomicBoolean(false)

    private val logBuffer = Channel<BufferedLog>(LOG_BUFFER_CAPACITY)

    private data class BufferedLog(
        val entry: LogEntry, 
        val initiallySynced: Boolean, 
        val strippedMessage: String = ""
    )

    companion object {
        private val BRACKET_REGEX = Regex("""\[.*?\]""")
        private val PAREN_REGEX = Regex("""\(.*?\)""")
        private val INTERRUPTION_REGEX = Regex("""\s*after an interruption of[^.]+""", RegexOption.IGNORE_CASE)
        private val COLON_VALUE_REGEX = Regex(""":\s*-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        private val SPACE_VALUE_REGEX = Regex("""\s+-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        
        private const val FORENSIC_FILL_THRESHOLD = FORENSIC_SPILL_CAPACITY / 4
        private const val FORENSIC_CONVERGENCE_STALL_LIMIT = 3
        private const val FORENSIC_EMERGENCY_FILL_LEVEL = 0.9
        private const val RELIABILITY_EMA_ALPHA = 0.1 
        private const val PRUNE_COOLDOWN_MS = 30000L 
        private const val UI_LOG_UPDATE_SAMPLE_MS = 1000L 
        private const val REFINED_PRUNE_CHUNK_SIZE = 1000 
    }

    init {
        startBatchProcessor()
        recoverAbandonedTraces()
        startForensicDrainer()
    }

    fun setForensicStallSimulation(active: Boolean) {
        isForensicStallSimulated.set(active)
        Timber.i("Forensic Audit: Simulation mode ${if (active) "ENABLED" else "DISABLED"}")
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
                                    buffer.isHighPressure() ||
                                    (pendingAtStart >= FORENSIC_FILL_THRESHOLD && loadFactor < 0.8) || 
                                    (pendingAtStart > 0 && now - lastDrainTime >= FORENSIC_DRAIN_INTERVAL_MS) ||
                                    isForensicStallSimulated.get()
                    
                    if (shouldDrain) {
                        val baseBatchSize = FORENSIC_BATCH_SIZE_MIN + 
                                          ((FORENSIC_BATCH_SIZE_MAX - FORENSIC_BATCH_SIZE_MIN) * fillLevel).toInt()
                        
                        val loadImpact = if (isEmergency) 0.1 else 0.4
                        val dynamicBatchSize = (baseBatchSize * (1.0 - (loadFactor * loadImpact))).toInt()
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

    private suspend fun performForensicDrain(limit: Int, isRecovery: Boolean): Boolean {
        if (isForensicStallSimulated.get() && !isRecovery) {
            forensicFailureCount.incrementAndGet()
            updateReliability(false)
            return false
        }

        val buffer = forensicSpillBufferProvider.get()
        val pendingAtStart = buffer.getPendingCount()
        
        val entities = buffer.peekToEntities(limit)
        if (entities.isEmpty()) {
            if (pendingAtStart > 0) buffer.commitDrain(pendingAtStart)
            return false
        }

        // Issue #196: Optimized range-based deduplication (R197)
        val minTs = entities.minOf { it.timestamp }
        val maxTs = entities.maxOf { it.timestamp }
        
        val signaturesSet = logDao.getExistingForensicSignaturesInRange(minTs, maxTs).mapTo(HashSet()) {
            (it.timestamp shl 32) or (it.spillIdx.toLong() and 0xFFFFFFFFL)
        }
        
        val filteredEntities = if (signaturesSet.isEmpty()) entities else entities.filter { 
            val packed = (it.timestamp shl 32) or (it.spillIdx.toLong() and 0xFFFFFFFFL)
            packed !in signaturesSet 
        }

        return try {
            if (filteredEntities.isNotEmpty()) {
                logDao.insertAll(filteredEntities)
            }
            
            buffer.commitDrain(entities.size)
            forensicSuccessCount.incrementAndGet()
            updateReliability(true)
            
            if (isRecovery && filteredEntities.isNotEmpty()) {
                addLog(LogEntry(
                    localId = "RECOVERY-${timeProvider.currentTimeMillis()}",
                    timestamp = timeProvider.currentTimeMillis(),
                    message = "Forensic Recovery Successful: ${filteredEntities.size} traces replayed.", 
                    type = "SYSTEM", isImportant = true, id = "SYSTEM", viewerId = "SYSTEM", 
                    isSpecial = true, specialColor = FORENSIC_PINK_COLOR
                ), initiallySynced = true)
            }
            
            if (!isRecovery) checkDrainConvergence(pendingAtStart)

            logMutex.withLock {
                if (logWriteCount.addAndGet(filteredEntities.size) >= DB_PRUNE_THRESHOLD) {
                    logWriteCount.set(0)
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

    private fun updateReliability(success: Boolean) {
        val currentSample = if (success) 1.0 else 0.0
        liveReliability = (RELIABILITY_EMA_ALPHA * currentSample) + ((1.0 - RELIABILITY_EMA_ALPHA) * liveReliability)
        val health = telemetry.systemHealth.value
        health.forensicReliability = liveReliability
        telemetry.updateHealth(health)
    }

    private fun checkDrainConvergence(pendingAtStart: Int) {
        val buffer = forensicSpillBufferProvider.get()
        val pendingAfter = buffer.getPendingCount()
        if (pendingAfter > 0 && pendingAfter >= pendingAtStart) {
            consecutiveStalls++
            if (consecutiveStalls >= FORENSIC_CONVERGENCE_STALL_LIMIT) {
                val h = telemetry.systemHealth.value
                val msg = "Forensic Stall Correlated: Backfill not converging ($pendingAfter pending). System: [CPU: ${h.cpuLoad}, IOW: ${h.ioWait}]"
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
    }
    private var consecutiveStalls = 0

    private suspend fun flushBatch(batch: List<BufferedLog>) {
        if (batch.isEmpty()) return

        logMutex.withLock {
            LatencyMonitor.measureAndAudit<Unit>(
                timeProvider = timeProvider,
                thresholdMs = LOG_LATENCY_THRESHOLD_MS,
                operation = "Log batch write [size: ${batch.size}]",
                type = LatencyMonitor.AuditType.IO,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                try {
                    db.withTransaction {
                        val toInsert = mutableListOf<LogEntity>()
                        var lastEntityMetadata = ""
                        var lastEntityStripped = ""
                        var lastEntityCached: LogEntity? = null

                        for (buffered in batch) {
                            val entry = buffered.entry
                            val initiallySynced = buffered.initiallySynced
                            val currentBase = buffered.strippedMessage 

                            val existing = if (entry.localId.isNotBlank()) logDao.getLogByLocalId(entry.localId) else null
                            if (existing != null) {
                                logDao.update(existing.copy(
                                    timestamp = entry.timestamp, message = entry.message, type = entry.type,
                                    isImportant = entry.isImportant, extremeValue = entry.extremeValue,
                                    count = entry.count, durationMs = entry.durationMs, isSpecial = entry.isSpecial,
                                    specialColor = entry.specialColor, role = entry.role, synced = initiallySynced,
                                    lat = entry.lat, lng = entry.lng, accuracy = entry.accuracy,
                                    maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot,
                                    vibeSnapshot = entry.vibeSnapshot, gpsHardwareLock = entry.gpsHardwareLock,
                                    tempSnapshot = entry.tempSnapshot, battSnapshot = entry.battSnapshot,
                                    chargingSnapshot = entry.chargingSnapshot
                                ))
                                continue
                            }

                            // Issue #207: Optimized deduplication cache to minimize redundant queries
                            val metaKey = "${entry.type}|${entry.role}|${entry.id}"
                            val last = if (metaKey == lastEntityMetadata) lastEntityCached else logDao.getLastLogByMetadata(entry.type, entry.role, entry.id)
                            
                            if (last != null) {
                                val lastBase = if (metaKey == lastEntityMetadata) lastEntityStripped else stripLogVariableParts(last.message)
                                if (lastBase == currentBase && lastBase.isNotEmpty() && last.isSpecial == entry.isSpecial) {
                                    val updated = last.copy(
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
                                    )
                                    logDao.update(updated)
                                    lastEntityMetadata = metaKey
                                    lastEntityStripped = lastBase
                                    lastEntityCached = updated
                                    continue
                                }
                            }

                            val newLog = LogEntity(
                                localId = if (entry.localId.isBlank()) "L-${entry.timestamp}-${UUID.randomUUID().toString().take(4)}" else entry.localId, 
                                timestamp = entry.timestamp, message = entry.message, type = entry.type, 
                                isImportant = entry.isImportant, deviceId = entry.id, viewerId = entry.viewerId, 
                                count = entry.count, extremeValue = entry.extremeValue, durationMs = entry.durationMs, 
                                isSpecial = entry.isSpecial, specialColor = entry.specialColor,
                                firstSeenTs = if (entry.firstSeenTs == 0L) (entry.timestamp - entry.durationMs) else entry.firstSeenTs,
                                role = entry.role, synced = initiallySynced, lat = entry.lat, lng = entry.lng, 
                                accuracy = entry.accuracy, maxAccuracy = entry.maxAccuracy, 
                                snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot,
                                spillIdx = entry.spillIdx, gpsHardwareLock = entry.gpsHardwareLock,
                                tempSnapshot = entry.tempSnapshot, battSnapshot = entry.battSnapshot,
                                chargingSnapshot = entry.chargingSnapshot
                            )
                            toInsert.add(newLog)
                            logWriteCount.incrementAndGet()
                            lastEntityMetadata = metaKey
                            lastEntityStripped = currentBase
                            lastEntityCached = newLog
                        }
                        if (toInsert.isNotEmpty()) logDao.insertAll(toInsert)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error flushing log batch to database")
                }
            }
        }
        if (logWriteCount.get() >= DB_PRUNE_THRESHOLD) {
            logWriteCount.set(0)
            triggerAsyncPruning()
        }
    }

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logDao.getAllLogs(limit)
        .sample(UI_LOG_UPDATE_SAMPLE_MS)
        .map { entities ->
            withContext(Dispatchers.Default) {
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
            }
        }.flowOn(Dispatchers.Default)

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        if (entry.type == "FORENSIC_TRACE") {
            scope.launch(Dispatchers.Default) {
                forensicSpillBufferProvider.get().writeTrace(entry)
            }
            return
        }
        // Issue #207: Move regex work to addLog call site (off-transaction)
        val result = logBuffer.trySend(BufferedLog(entry, initiallySynced, stripLogVariableParts(entry.message)))
        if (result.isFailure) Timber.w("Log buffer full, dropping log: ${entry.message}")
    }

    private fun triggerAsyncPruning() {
        val now = timeProvider.elapsedRealtime()
        if (now - lastPruneTime.get() < PRUNE_COOLDOWN_MS) return
        if (isPruning.compareAndSet(false, true)) {
            lastPruneTime.set(now)
            scope.launch(Dispatchers.IO) { try { proactivePruning() } finally { isPruning.set(false) } }
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
                    health.isStorageLow -> ADAPTIVE_PRUNE_THRESHOLD_LOW
                    health.isBatteryCritical -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL 
                    health.isCharging -> ADAPTIVE_PRUNE_THRESHOLD_CHARGING 
                    else -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL
                }

                if (count > threshold) {
                    val heartbeatTarget = if (health.isStorageLow) 100 else 500
                    val generalTarget = if (health.isStorageLow) 1000 else 2000 
                    val forensicTarget = if (health.isStorageCritical) FORENSIC_PRUNE_LIMIT_CRITICAL else if (health.isStorageLow) FORENSIC_PRUNE_LIMIT_LOW else if (health.isCharging) FORENSIC_PRUNE_LIMIT_CHARGING else FORENSIC_PRUNE_LIMIT_NORMAL
                    
                    val maxChunks = if (health.isStorageCritical) 30 else 15
                    val hT = logDao.getHeartbeatPruneThreshold(heartbeatTarget)
                    val gT = logDao.getGeneralPruneThreshold(generalTarget)
                    val iT = logDao.getImportantPruneThreshold(2000)
                    val sT = if (count > LOG_LIMIT_STRICT) logDao.getSpecialPruneThreshold(LOG_LIMIT_STANDARD) else null
                    val fT = logDao.getForensicPruneThreshold(forensicTarget)

                    repeat(maxChunks) { 
                        var chunkPruned = 0
                        db.withTransaction {
                            hT?.let { chunkPruned += logDao.pruneHeartbeatsByThreshold(it, REFINED_PRUNE_CHUNK_SIZE) }
                            gT?.let { chunkPruned += logDao.pruneGeneralByThreshold(it, REFINED_PRUNE_CHUNK_SIZE) }
                            iT?.let { chunkPruned += logDao.pruneImportantByThreshold(it, REFINED_PRUNE_CHUNK_SIZE) }
                            sT?.let { chunkPruned += logDao.pruneSpecialByThreshold(it, REFINED_PRUNE_CHUNK_SIZE) }
                            fT?.let { chunkPruned += logDao.pruneForensicByThreshold(it, REFINED_PRUNE_CHUNK_SIZE) }
                        }
                        if (chunkPruned == 0) return@repeat 
                        delay(if (health.isBatteryLow) 150L else 50L)
                    }
                }
            } catch (e: Exception) { Timber.e(e, "Error during proactive pruning") }
        }
    }

    suspend fun getUnsyncedLogs(limit: Int): List<LogEntry> = withContext(Dispatchers.Default) {
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

    suspend fun markLogsAsSynced(localIds: List<String>) = logDao.markLogsAsSynced(localIds)

    private fun stripLogVariableParts(message: String): String {
        var m = message
        m = m.replace(BRACKET_REGEX, "").replace(PAREN_REGEX, "").replace(INTERRUPTION_REGEX, "").replace(COLON_VALUE_REGEX, "").replace(SPACE_VALUE_REGEX, "")
        return m.trim().trimEnd('.')
    }

    fun clearLogs() { scope.launch(Dispatchers.IO) { try { logDao.clearAll() } catch (e: Exception) { Timber.e(e, "Error clearing logs") } } }

    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = withContext(Dispatchers.Default) {
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
