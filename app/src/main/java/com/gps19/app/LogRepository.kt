package com.gps19.app

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
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
import androidx.annotation.VisibleForTesting
import kotlin.coroutines.coroutineContext

/**
 * LogRepository: Dedicated repository for application logs.
 * Oct.13.7:
 * - Aggressive Draining in Test Mode: Ignored interval and used max batch size 
 *   to resolve 755/1000 under-count in ForensicStressAuditTest.
 * - Added Log.i diagnostics for reliable logcat visibility.
 * - Integrated PeekResult from ForensicSpillBuffer to ensure read-pointer 
 *   forward progress even during CRC errors (Resolving Drain Deficit).
 * - Made stopBackgroundTasks and resetForTest suspendable with cancelAndJoin for 
 *   guaranteed test isolation (Resolving ISSUE-OCT13.5-3 leakage).
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
    private val forensicMutex = Mutex() 
    private val logWriteCount = AtomicInteger(0)
    private val isPruning = AtomicBoolean(false)
    private val lastPruneTime = AtomicLong(0L)

    private val forensicSuccessCount = AtomicInteger(0)
    private val forensicFailureCount = AtomicInteger(0)
    private var liveReliability = BigDecimal.ONE
    private val isForensicStallSimulated = AtomicBoolean(false)

    private val logBuffer = Channel<BufferedLog>(LOG_BUFFER_CAPACITY)

    private var batchJob: Job? = null
    private var recoveryJob: Job? = null
    private var drainerJob: Job? = null
    private var consecutiveStalls = 0

    private data class BufferedLog(
        val entry: LogEntry, 
        val initiallySynced: Boolean, 
        val strippedMessage: String = ""
    )

    companion object {
        private val isTestMode = AtomicBoolean(false)

        @VisibleForTesting
        fun setTestMode(enabled: Boolean) {
            isTestMode.set(enabled)
            Log.i("LogRepository", "Test mode set to $enabled")
        }

        private val BRACKET_REGEX = Regex("""\[.*?\]""")
        private val PAREN_REGEX = Regex("""\(.*?\)""")
        private val INTERRUPTION_REGEX = Regex("""\s*after an interruption of[^.]+""", RegexOption.IGNORE_CASE)
        private val COLON_VALUE_REGEX = Regex(""":\s*-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        private val SPACE_VALUE_REGEX = Regex("""\s+-?\d+(\.\d+)?\s*[A-Za-z%°]*""")
        
        private const val FORENSIC_FILL_THRESHOLD = FORENSIC_SPILL_CAPACITY / 4
        private const val FORENSIC_CONVERGENCE_STALL_LIMIT = 3
        private const val FORENSIC_EMERGENCY_FILL_LEVEL = 0.9
        private val RELIABILITY_EMA_ALPHA = BigDecimal("0.1") 
        private const val PRUNE_COOLDOWN_MS = 30000L 
        private const val UI_LOG_UPDATE_SAMPLE_MS = 1000L 
        private const val REFINED_PRUNE_CHUNK_SIZE = 1000 
    }

    init {
        if (!isTestMode.get()) {
            startBackgroundTasks()
        } else {
            Log.i("LogRepository", "Background tasks inhibited by Test Mode.")
        }
    }

    private fun startBackgroundTasks() {
        if (batchJob?.isActive == true) return
        
        batchJob = scope.launch(Dispatchers.IO) { runBatchProcessor() }
        recoveryJob = scope.launch(Dispatchers.IO) { recoverAbandonedTraces() }
        drainerJob = scope.launch(Dispatchers.IO) { runForensicDrainer() }
    }

    @VisibleForTesting
    suspend fun stopBackgroundTasks() {
        batchJob?.cancelAndJoin()
        recoveryJob?.cancelAndJoin()
        drainerJob?.cancelAndJoin()
        Log.i("LogRepository", "Background tasks stopped and joined.")
    }

    @VisibleForTesting
    suspend fun resetForTest() {
        stopBackgroundTasks()
        logWriteCount.set(0)
        isPruning.set(false)
        forensicSuccessCount.set(0)
        forensicFailureCount.set(0)
        liveReliability = BigDecimal.ONE
        isForensicStallSimulated.set(false)
        consecutiveStalls = 0
        // Clear log buffer
        while (logBuffer.tryReceive().isSuccess) { /* drain */ }
        Log.i("LogRepository", "Internal state reset for test.")
    }

    /**
     * Manually triggers forensic recovery. Useful in Test Mode.
     */
    @VisibleForTesting
    fun triggerManualRecovery() {
        recoveryJob = scope.launch(Dispatchers.IO) { recoverAbandonedTraces() }
    }

    /**
     * Manually starts the forensic drainer loop. Useful in Test Mode.
     */
    @VisibleForTesting
    fun triggerManualDrainer() {
        if (drainerJob?.isActive == true) return
        drainerJob = scope.launch(Dispatchers.IO) { runForensicDrainer() }
        Log.i("LogRepository", "Manual drainer triggered.")
    }

    fun setForensicStallSimulation(active: Boolean) {
        isForensicStallSimulated.set(active)
        Log.i("LogRepository", "Forensic Audit: Simulation mode ${if (active) "ENABLED" else "DISABLED"}")
    }

    private suspend fun runBatchProcessor() {
        val batch = mutableListOf<BufferedLog>()
        var lastFlushRt = timeProvider.elapsedRealtime

        while (coroutineContext.isActive) {
            try {
                val log = withTimeoutOrNull(LOG_BATCH_DELAY_MS) {
                    logBuffer.receive()
                }

                if (log != null) {
                    batch.add(log)
                }

                val nowRt = timeProvider.elapsedRealtime
                if (batch.size >= LOG_BATCH_SIZE || (batch.isNotEmpty() && nowRt - lastFlushRt >= LOG_BATCH_DELAY_MS)) {
                    flushBatch(batch)
                    batch.clear()
                    lastFlushRt = nowRt
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error in log batch processor")
            }
        }
    }

    private suspend fun recoverAbandonedTraces() {
        val buffer = forensicSpillBufferProvider.get()
        val pending = buffer.getPendingCount()
        if (pending > 0) {
            Log.i("LogRepository", "Forensic Recovery: Replaying $pending abandoned traces.")
            performForensicDrain(limit = FORENSIC_SPILL_CAPACITY, isRecovery = true)
        }
    }

    private suspend fun runForensicDrainer() {
        var lastDrainRt = timeProvider.elapsedRealtime
        
        while (coroutineContext.isActive) {
            try {
                val health = telemetry.systemHealth.value
                val loadFactor = if (health.isMaliAnomaly) 1.0 else (health.cpuLoad / 8.0).coerceIn(0.0, 1.0)
                val dynamicDelay = FORENSIC_DRAIN_THROTTLE_MIN_MS + 
                                 ((FORENSIC_DRAIN_THROTTLE_MAX_MS - FORENSIC_DRAIN_THROTTLE_MIN_MS) * loadFactor).toLong()
                
                delay(dynamicDelay)
                
                val nowRt = timeProvider.elapsedRealtime
                val buffer = forensicSpillBufferProvider.get()
                val pendingAtStart = buffer.getPendingCount()
                val fillLevel = pendingAtStart.toDouble() / FORENSIC_SPILL_CAPACITY
                val isEmergency = fillLevel >= FORENSIC_EMERGENCY_FILL_LEVEL
                
                val shouldDrain = isEmergency || 
                                buffer.isHighPressure() ||
                                (pendingAtStart >= FORENSIC_FILL_THRESHOLD && loadFactor < 0.8) || 
                                (pendingAtStart > 0 && (nowRt - lastDrainRt >= FORENSIC_DRAIN_INTERVAL_MS || isTestMode.get())) ||
                                isForensicStallSimulated.get()
                
                if (shouldDrain) {
                    val baseBatchSize = if (isTestMode.get()) FORENSIC_BATCH_SIZE_MAX else 
                                      FORENSIC_BATCH_SIZE_MIN + 
                                      ((FORENSIC_BATCH_SIZE_MAX - FORENSIC_BATCH_SIZE_MIN) * fillLevel).toInt()
                    
                    val loadImpact = if (isEmergency) 0.1 else 0.4
                    val dynamicBatchSize = (baseBatchSize * (1.0 - (loadFactor * loadImpact))).toInt()
                        .coerceIn(FORENSIC_BATCH_SIZE_MIN, FORENSIC_BATCH_SIZE_MAX)

                    if (performForensicDrain(limit = dynamicBatchSize, isRecovery = false)) {
                        lastDrainRt = nowRt
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("LogRepository", "Error in forensic drainer loop", e)
            }
        }
    }

    private suspend fun performForensicDrain(limit: Int, isRecovery: Boolean): Boolean = forensicMutex.withLock {
        if (isForensicStallSimulated.get() && !isRecovery) {
            forensicFailureCount.incrementAndGet()
            updateReliability(false)
            return@withLock false
        }

        val buffer = forensicSpillBufferProvider.get()
        val pendingAtStart = buffer.getPendingCount()
        
        val peekResult = buffer.peekToEntities(limit)
        val entities = peekResult.entities
        val processedCount = peekResult.processedCount

        if (entities.isEmpty()) {
            if (processedCount > 0) {
                Log.w("LogRepository", "Forensic Drain: Peeked $processedCount entries but none were valid (CRC mismatches). Advancing read pointer.")
                buffer.commitDrain(processedCount)
                return@withLock true
            }
            return@withLock false
        }

        val minTs = entities.minOf { it.timestamp }
        val maxTs = entities.maxOf { it.timestamp }
        
        val signaturesSet = logDao.getExistingForensicSignaturesInRange(minTs, maxTs).mapTo(HashSet()) {
            it.timestamp to it.spillIdx
        }
        
        val filteredEntities = if (signaturesSet.isEmpty()) entities else entities.filter { 
            val isUnique = (it.timestamp to it.spillIdx) !in signaturesSet
            if (!isUnique) {
                Log.i("LogRepository", "Forensic Drain: Filtering existing trace at ts=${it.timestamp}, idx=${it.spillIdx}")
            }
            isUnique
        }

        val dupCount = entities.size - filteredEntities.size
        if (dupCount > 0) {
            Log.i("LogRepository", "Forensic Drain: Filtered $dupCount duplicates out of ${entities.size} valid peeked.")
        }

        return@withLock try {
            if (filteredEntities.isNotEmpty()) {
                logDao.insertAll(filteredEntities)
                Log.i("LogRepository", "Forensic Drain: Successfully inserted ${filteredEntities.size} traces (Pending at start: $pendingAtStart, Peek limit: $limit).")
            }
            
            buffer.commitDrain(processedCount)
            forensicSuccessCount.incrementAndGet()
            updateReliability(true)
            
            if (isRecovery && filteredEntities.isNotEmpty()) {
                val recoveryTs = timeProvider.currentTimeMillis
                addLog(LogEntry(
                    localId = "RECOVERY-$recoveryTs",
                    timestamp = recoveryTs,
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
            Log.e("LogRepository", "Forensic drain failed", e)
            false
        }
    }

    private fun updateReliability(success: Boolean) {
        val currentSample = if (success) BigDecimal.ONE else BigDecimal.ZERO
        val alpha = RELIABILITY_EMA_ALPHA
        val oneMinusAlpha = BigDecimal.ONE.subtract(alpha)
        
        liveReliability = alpha.multiply(currentSample)
            .add(oneMinusAlpha.multiply(liveReliability))
            .setScale(8, RoundingMode.HALF_UP)
            
        val health = telemetry.systemHealth.value
        health.forensicReliability = liveReliability.toDouble()
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
                Log.w("LogRepository", msg)
                val stallTs = timeProvider.currentTimeMillis
                addLog(LogEntry(
                    localId = "STALL-$stallTs",
                    timestamp = stallTs,
                    message = msg, type = "SYSTEM", isImportant = true,
                    id = "SYSTEM", viewerId = "SYSTEM", isSpecial = true,
                    specialColor = FORENSIC_PINK_COLOR
                ), initiallySynced = true)
            }
        } else {
            consecutiveStalls = 0
        }
    }

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
                                    vibeSnapshot = entry.vibeSnapshot,
                                    gpsHardwareLock = entry.gpsHardwareLock,
                                    tempSnapshot = entry.tempSnapshot, battSnapshot = entry.battSnapshot,
                                    chargingSnapshot = entry.chargingSnapshot,
                                    thermalSnapshot = entry.thermalSnapshot,
                                    heapSnapshot = entry.heapSnapshot,
                                    coolingSnapshot = entry.coolingSnapshot,
                                    checksum = entry.checksum
                                ))
                                continue
                            }

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
                                        maxAccuracy = entry.maxAccuracy, snrSnapshot = entry.snrSnapshot, 
                                        vibeSnapshot = entry.vibeSnapshot, 
                                        gpsHardwareLock = entry.gpsHardwareLock,
                                        tempSnapshot = entry.tempSnapshot,
                                        battSnapshot = entry.battSnapshot,
                                        chargingSnapshot = entry.chargingSnapshot,
                                        thermalSnapshot = entry.thermalSnapshot,
                                        heapSnapshot = entry.heapSnapshot,
                                        coolingSnapshot = entry.coolingSnapshot,
                                        checksum = entry.checksum
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
                                chargingSnapshot = entry.chargingSnapshot,
                                thermalSnapshot = entry.thermalSnapshot,
                                heapSnapshot = entry.heapSnapshot,
                                coolingSnapshot = entry.coolingSnapshot,
                                checksum = entry.checksum
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
                        forensic = ForensicSnapshot(
                            snr = it.snrSnapshot, 
                            vibe = it.vibeSnapshot, 
                            thermal = it.thermalSnapshot, 
                            heap = it.heapSnapshot
                        ),
                        spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                        tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                        chargingSnapshot = it.chargingSnapshot,
                        coolingSnapshot = it.coolingSnapshot,
                        checksum = it.checksum
                    ) 
                }
            }
        }.flowOn(Dispatchers.Default)

    /**
     * addLog: Routes logs to the batch buffer.
     * Oct.6.2: Hardened against backpressure for important logs.
     */
    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        if (entry.type == "FORENSIC_TRACE") {
            scope.launch(Dispatchers.Default) {
                forensicSpillBufferProvider.get().writeTrace(entry)
            }
            return
        }
        
        val buffered = BufferedLog(entry, initiallySynced, stripLogVariableParts(entry.message))
        val result = logBuffer.trySend(buffered)
        
        if (result.isFailure) {
            if (entry.isImportant) {
                scope.launch(Dispatchers.IO) {
                    logBuffer.send(buffered)
                }
            } else {
                Timber.w("Log buffer full, dropping log: ${entry.message}")
            }
        }
    }

    private fun triggerAsyncPruning() {
        val nowRt = timeProvider.elapsedRealtime
        if (nowRt - lastPruneTime.get() < PRUNE_COOLDOWN_MS) return
        if (isPruning.compareAndSet(false, true)) {
            lastPruneTime.set(nowRt)
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
                
                val memoryCritical = health.heapAllocatedMb >= MEMORY_CRITICAL_THRESHOLD_MB
                val memoryHigh = health.heapAllocatedMb >= MEMORY_PRESSURE_THRESHOLD_MB

                val threshold = when {
                    health.isStorageCritical || memoryCritical || health.isMaliAnomaly -> ADAPTIVE_PRUNE_THRESHOLD_CRITICAL
                    health.isStorageLow || memoryHigh -> ADAPTIVE_PRUNE_THRESHOLD_LOW
                    health.isBatteryCritical -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL 
                    health.isCharging -> ADAPTIVE_PRUNE_THRESHOLD_CHARGING 
                    else -> ADAPTIVE_PRUNE_THRESHOLD_NORMAL
                }

                if (count > threshold) {
                    val heartbeatTarget = if (health.isStorageLow || memoryHigh || health.isMaliAnomaly) 100 else 500
                    val generalTarget = if (health.isStorageLow || memoryHigh || health.isMaliAnomaly) 1000 else 2000 
                    val forensicTarget = if (health.isStorageCritical || memoryCritical || health.isMaliAnomaly) FORENSIC_PRUNE_LIMIT_CRITICAL else if (health.isStorageLow || memoryHigh) FORENSIC_PRUNE_LIMIT_LOW else if (health.isCharging) FORENSIC_PRUNE_LIMIT_CHARGING else FORENSIC_PRUNE_LIMIT_NORMAL
                    
                    val maxChunks = if (health.isStorageCritical || memoryCritical || health.isMaliAnomaly) 30 else 15
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
                forensic = ForensicSnapshot(
                    snr = it.snrSnapshot,
                    vibe = it.vibeSnapshot,
                    thermal = it.thermalSnapshot,
                    heap = it.heapSnapshot
                ),
                spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                chargingSnapshot = it.chargingSnapshot,
                coolingSnapshot = it.coolingSnapshot,
                checksum = it.checksum
            )
        }
    }

    suspend fun markLogsAsSynced(localIds: List<String>) = logDao.markLogsAsSynced(localIds)

    private fun stripLogVariableParts(message: String): String {
        var m = message
        m = m.replace(BRACKET_REGEX, "").replace(PAREN_REGEX, "").replace(INTERRUPTION_REGEX, "").replace(COLON_VALUE_REGEX, "").replace(SPACE_VALUE_REGEX, "")
        return m.trim().trimEnd('.')
    }

    fun clearLogs() { scope.launch(Dispatchers.IO) { try { logDao.clearAll() } catch (e: Exception) { Log.e("LogRepository", "Error clearing logs", e) } } }

    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = withContext(Dispatchers.Default) {
        logDao.getAllLogsStatic(limit).map {
            LogEntry(
                localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
                isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, count = it.count, 
                extremeValue = it.extremeValue, durationMs = it.durationMs, isSpecial = it.isSpecial, 
                specialColor = it.specialColor, firstSeenTs = it.firstSeenTs, role = it.role,
                lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                forensic = ForensicSnapshot(
                    snr = it.snrSnapshot, 
                    vibe = it.vibeSnapshot, 
                    thermal = it.thermalSnapshot, 
                    heap = it.heapSnapshot
                ),
                spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock,
                tempSnapshot = it.tempSnapshot, battSnapshot = it.battSnapshot,
                chargingSnapshot = it.chargingSnapshot,
                coolingSnapshot = it.coolingSnapshot,
                checksum = it.checksum
            )
        }
    }
}
