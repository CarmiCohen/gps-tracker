package com.gps19.app

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import androidx.room.withTransaction
import androidx.annotation.VisibleForTesting
import kotlin.coroutines.coroutineContext

/**
 * LogRepository: Dedicated repository for application logs.
 * Oct.20.24:
 * - ISSUE-OCT20.23-F2: Finalized remediation. Made clearLogs() suspendable and 
 *   resolved type mismatches in mapping functions to ensure deterministic test setup.
 * Oct.20.2:
 * - ISSUE-OCT18.22-F3: Added flushLogs() and flushForensicTraces() for deterministic testing.
 */
@OptIn(FlowPreview::class)
@Singleton
class LogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logDao: LogDao,
    private val db: AppDatabase,
    private val forensicSpillBufferProvider: Provider<ForensicSpillBuffer>,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val telemetry: TelemetryRepository,
    private val testModeProvider: TestModeProvider
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
        Log.i("LogRepository", "Initialized. Awaiting explicit start().")
    }

    fun start() {
        if (testModeProvider.isTestMode()) {
            Log.i("LogRepository", "Start inhibited by Test Mode.")
            return
        }
        startBackgroundTasks()
        Log.i("LogRepository", "Background tasks started explicitly.")
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
        while (logBuffer.tryReceive().isSuccess) { /* drain */ }
        Log.i("LogRepository", "Internal state reset for test.")
    }

    @VisibleForTesting
    fun triggerManualRecovery() {
        recoveryJob = scope.launch(Dispatchers.IO) { recoverAbandonedTraces() }
    }

    @VisibleForTesting
    fun triggerManualDrainer() {
        if (drainerJob?.isActive == true) return
        drainerJob = scope.launch(Dispatchers.IO) { runForensicDrainer() }
        Log.i("LogRepository", "Manual drainer triggered.")
    }

    @VisibleForTesting
    fun triggerManualBatchProcessor() {
        if (batchJob?.isActive == true) return
        batchJob = scope.launch(Dispatchers.IO) { runBatchProcessor() }
        Log.i("LogRepository", "Manual batch processor triggered.")
    }

    @VisibleForTesting
    suspend fun flushLogs() {
        val batch = mutableListOf<BufferedLog>()
        while (true) {
            val result = logBuffer.tryReceive()
            if (result.isSuccess) {
                batch.add(result.getOrThrow())
            } else {
                break
            }
        }
        if (batch.isNotEmpty()) {
            flushBatch(batch)
        }
    }

    @VisibleForTesting
    suspend fun flushForensicTraces() {
        performForensicDrain(limit = FORENSIC_SPILL_CAPACITY, isRecovery = false)
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
                                (pendingAtStart > 0 && (nowRt - lastDrainRt >= FORENSIC_DRAIN_INTERVAL_MS || testModeProvider.isTestMode())) ||
                                isForensicStallSimulated.get()
                
                if (shouldDrain) {
                    val baseBatchSize = if (testModeProvider.isTestMode()) FORENSIC_BATCH_SIZE_MAX else 
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
                buffer.commitDrain(processedCount)
                return@withLock true
            }
            return@withLock false
        }

        val minTs = entities.minOf { it.timestamp }
        val maxTs = entities.maxOf { it.timestamp }
        val signaturesSet = logDao.getExistingForensicSignaturesInRange(minTs, maxTs).mapTo(HashSet()) { it.timestamp to it.spillIdx }
        val filteredEntities = if (signaturesSet.isEmpty()) entities else entities.filter { (it.timestamp to it.spillIdx) !in signaturesSet }

        return@withLock try {
            if (filteredEntities.isNotEmpty()) logDao.insertAll(filteredEntities)
            buffer.commitDrain(processedCount)
            forensicSuccessCount.incrementAndGet()
            updateReliability(true)
            
            if (isRecovery && filteredEntities.isNotEmpty()) {
                val recoveryTs = timeProvider.currentTimeMillis
                addLog(LogEntry(
                    localId = "RECOVERY-$recoveryTs", timestamp = recoveryTs,
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
            false
        }
    }

    private fun updateReliability(success: Boolean) {
        val currentSample = if (success) BigDecimal.ONE else BigDecimal.ZERO
        liveReliability = RELIABILITY_EMA_ALPHA.multiply(currentSample)
            .add(BigDecimal.ONE.subtract(RELIABILITY_EMA_ALPHA).multiply(liveReliability))
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
                val stallTs = timeProvider.currentTimeMillis
                addLog(LogEntry(
                    localId = "STALL-$stallTs", timestamp = stallTs,
                    message = "Forensic Stall Correlated.", type = "SYSTEM", isImportant = true,
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
            try {
                db.withTransaction {
                    val toInsert = mutableListOf<LogEntity>()
                    for (buffered in batch) {
                        val entry = buffered.entry
                        val existing = if (entry.localId.isNotBlank()) logDao.getLogByLocalId(entry.localId) else null
                        if (existing != null) {
                            logDao.update(existing.copy(
                                timestamp = entry.timestamp, 
                                message = entry.message, 
                                synced = buffered.initiallySynced,
                                type = entry.type,
                                isImportant = entry.isImportant,
                                deviceId = entry.id,
                                viewerId = entry.viewerId,
                                count = entry.count,
                                extremeValue = entry.extremeValue,
                                durationMs = entry.durationMs,
                                isSpecial = entry.isSpecial,
                                specialColor = entry.specialColor,
                                firstSeenTs = entry.firstSeenTs,
                                role = entry.role,
                                lat = entry.lat,
                                lng = entry.lng,
                                accuracy = entry.accuracy,
                                maxAccuracy = entry.maxAccuracy,
                                snrSnapshot = entry.snrSnapshot,
                                vibeSnapshot = entry.vibeSnapshot,
                                spillIdx = entry.spillIdx,
                                gpsHardwareLock = entry.gpsHardwareLock,
                                tempSnapshot = entry.tempSnapshot,
                                battSnapshot = entry.battSnapshot,
                                chargingSnapshot = entry.chargingSnapshot,
                                thermalSnapshot = entry.thermalSnapshot,
                                heapSnapshot = entry.heapSnapshot,
                                coolingSnapshot = entry.coolingSnapshot,
                                checksum = entry.checksum
                            ))
                            continue
                        }
                        toInsert.add(LogEntity(
                            localId = if (entry.localId.isBlank()) "L-${entry.timestamp}-${UUID.randomUUID().toString().take(4)}" else entry.localId, 
                            timestamp = entry.timestamp, message = entry.message, type = entry.type, 
                            isImportant = entry.isImportant, deviceId = entry.id, viewerId = entry.viewerId,
                            count = entry.count, extremeValue = entry.extremeValue, durationMs = entry.durationMs,
                            isSpecial = entry.isSpecial, specialColor = entry.specialColor,
                            firstSeenTs = entry.firstSeenTs, role = entry.role,
                            synced = buffered.initiallySynced, lat = entry.lat, lng = entry.lng,
                            accuracy = entry.accuracy, maxAccuracy = entry.maxAccuracy,
                            snrSnapshot = entry.snrSnapshot, vibeSnapshot = entry.vibeSnapshot,
                            spillIdx = entry.spillIdx, gpsHardwareLock = entry.gpsHardwareLock,
                            tempSnapshot = entry.tempSnapshot, battSnapshot = entry.battSnapshot,
                            chargingSnapshot = entry.chargingSnapshot, thermalSnapshot = entry.thermalSnapshot,
                            heapSnapshot = entry.heapSnapshot, coolingSnapshot = entry.coolingSnapshot,
                            checksum = entry.checksum
                        ))
                    }
                    if (toInsert.isNotEmpty()) logDao.insertAll(toInsert)
                }
            } catch (e: Exception) { Timber.e(e, "Error flushing log batch") }
        }
    }

    fun eventLogsFlow(limit: Int): Flow<List<LogEntry>> = logDao.getAllLogs(limit).sample(UI_LOG_UPDATE_SAMPLE_MS).map { entities ->
        entities.map { 
            LogEntry(
                localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
                isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, role = it.role,
                lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
                count = it.count, extremeValue = it.extremeValue, durationMs = it.durationMs,
                isSpecial = it.isSpecial, specialColor = it.specialColor, firstSeenTs = it.firstSeenTs,
                forensic = ForensicSnapshot(snr = it.snrSnapshot, vibe = it.vibeSnapshot, thermal = it.thermalSnapshot, heap = it.heapSnapshot),
                spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock, tempSnapshot = it.tempSnapshot,
                battSnapshot = it.battSnapshot, chargingSnapshot = it.chargingSnapshot,
                coolingSnapshot = it.coolingSnapshot, checksum = it.checksum
            ) 
        }
    }

    fun addLog(entry: LogEntry, initiallySynced: Boolean = false) {
        if (entry.type == "FORENSIC_TRACE") {
            scope.launch(Dispatchers.Default) { forensicSpillBufferProvider.get().writeTrace(entry) }
            return
        }
        val buffered = BufferedLog(entry, initiallySynced, stripLogVariableParts(entry.message))
        if (!logBuffer.trySend(buffered).isSuccess && entry.isImportant) {
            scope.launch(Dispatchers.IO) { logBuffer.send(buffered) }
        }
    }

    private fun stripLogVariableParts(message: String): String {
        var m = message
        m = m.replace(BRACKET_REGEX, "").replace(PAREN_REGEX, "").replace(INTERRUPTION_REGEX, "").replace(COLON_VALUE_REGEX, "").replace(SPACE_VALUE_REGEX, "")
        return m.trim().trimEnd('.')
    }

    private fun triggerAsyncPruning() {
        val nowRt = timeProvider.elapsedRealtime
        if (nowRt - lastPruneTime.get() < PRUNE_COOLDOWN_MS) return
        if (isPruning.compareAndSet(false, true)) {
            lastPruneTime.set(nowRt)
            PruningWorker.triggerNow(context)
            isPruning.set(false)
        }
    }

    suspend fun proactivePruning() {
        try {
            val count = logDao.getCount()
            if (count > ADAPTIVE_PRUNE_THRESHOLD_NORMAL) {
                db.withTransaction {
                    logDao.pruneHeartbeatsByThreshold(logDao.getHeartbeatPruneThreshold(500) ?: 0L, REFINED_PRUNE_CHUNK_SIZE)
                    logDao.pruneGeneralByThreshold(logDao.getGeneralPruneThreshold(1000) ?: 0L, REFINED_PRUNE_CHUNK_SIZE)
                }
            }
        } catch (e: Exception) { Timber.e(e, "Error during proactive pruning") }
    }

    suspend fun getUnsyncedLogs(limit: Int): List<LogEntry> = logDao.getUnsyncedLogs(limit).map { 
        LogEntry(
            localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
            isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, role = it.role,
            lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
            count = it.count, extremeValue = it.extremeValue, durationMs = it.durationMs,
            isSpecial = it.isSpecial, specialColor = it.specialColor, firstSeenTs = it.firstSeenTs,
            forensic = ForensicSnapshot(snr = it.snrSnapshot, vibe = it.vibeSnapshot, thermal = it.thermalSnapshot, heap = it.heapSnapshot),
            spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock, tempSnapshot = it.tempSnapshot,
            battSnapshot = it.battSnapshot, chargingSnapshot = it.chargingSnapshot,
            coolingSnapshot = it.coolingSnapshot, checksum = it.checksum
        ) 
    }
    suspend fun markLogsAsSynced(localIds: List<String>) = logDao.markLogsAsSynced(localIds)

    /**
     * clearLogs: Now suspendable for deterministic testing.
     * Oct.20.24: Part of ISSUE-OCT20.23-F2 remediation.
     */
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        try { logDao.clearAll() } catch (e: Exception) { Log.e("LogRepository", "Error clearing logs", e) }
    }

    suspend fun loadAllLogsStatic(limit: Int = LOG_LIMIT_STANDARD): List<LogEntry> = logDao.getAllLogsStatic(limit).map { 
        LogEntry(
            localId = it.localId, timestamp = it.timestamp, message = it.message, type = it.type, 
            isImportant = it.isImportant, id = it.deviceId, viewerId = it.viewerId, role = it.role,
            lat = it.lat, lng = it.lng, accuracy = it.accuracy, maxAccuracy = it.maxAccuracy,
            count = it.count, extremeValue = it.extremeValue, durationMs = it.durationMs,
            isSpecial = it.isSpecial, specialColor = it.specialColor, firstSeenTs = it.firstSeenTs,
            forensic = ForensicSnapshot(snr = it.snrSnapshot, vibe = it.vibeSnapshot, thermal = it.thermalSnapshot, heap = it.heapSnapshot),
            spillIdx = it.spillIdx, gpsHardwareLock = it.gpsHardwareLock, tempSnapshot = it.tempSnapshot,
            battSnapshot = it.battSnapshot, chargingSnapshot = it.chargingSnapshot,
            coolingSnapshot = it.coolingSnapshot, checksum = it.checksum
        )
    }
}
