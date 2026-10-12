package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * Oct.12.3:
 * - Issue #SIMP-1012-4: Batch Persistence. Implemented a memory buffer and 
 *   bulk-insertion loop to reduce SQLite write-lock duration and IOPS overhead.
 * Oct.12.1:
 * - Issue #SIMP-1012-1: Memory Pressure Hardening. Implemented aggressive 
 *   pruning (500/1000 limit) when MemoryPressureLevel is HIGH or CRITICAL.
 */
@Singleton
class OfflineRepository @Inject constructor(
    private val pendingStatusDao: PendingStatusDao,
    private val telemetry: TelemetryRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private companion object {
        private const val OFFLINE_PRUNE_LIMIT = 2000
        private const val PRUNE_CHUNK_SIZE = 500
        private const val BATCH_SIZE_THRESHOLD = 5
        private const val BATCH_FLUSH_INTERVAL_MS = 10000L
    }

    private var memoryPressureLevel = MemoryPressureLevel.NORMAL
    private val buffer = mutableListOf<PendingStatusEntity>()
    private val bufferMutex = Mutex()
    private var lastFlushRt = 0L

    init {
        startBatchFlushLoop()
    }

    /**
     * startBatchFlushLoop: Ensures buffered updates are periodically committed 
     * to disk even if the threshold isn't met.
     */
    private fun startBatchFlushLoop() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(BATCH_FLUSH_INTERVAL_MS)
                val nowRt = SystemClock.elapsedRealtime()
                if (nowRt - lastFlushRt >= BATCH_FLUSH_INTERVAL_MS) {
                    flushBuffer()
                }
            }
        }
    }

    /**
     * setMemoryPressureLevel: Updates the internal pressure state for adaptive pruning.
     */
    fun setMemoryPressureLevel(level: MemoryPressureLevel) {
        this.memoryPressureLevel = level
    }

    private fun getEffectivePruneLimit(): Int {
        return when (memoryPressureLevel) {
            MemoryPressureLevel.CRITICAL -> 500
            MemoryPressureLevel.HIGH -> 1000
            else -> OFFLINE_PRUNE_LIMIT
        }
    }

    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        val health = telemetry.systemHealth.value
        if (health.isStorageCritical) return

        bufferMutex.withLock {
            buffer.add(update)
            if (buffer.size >= BATCH_SIZE_THRESHOLD) {
                scope.launch(Dispatchers.IO) { flushBuffer() }
            }
        }
    }

    private suspend fun flushBuffer() {
        val toInsert = bufferMutex.withLock {
            if (buffer.isEmpty()) return
            val copy = ArrayList(buffer)
            buffer.clear()
            lastFlushRt = SystemClock.elapsedRealtime()
            copy
        }

        try {
            pendingStatusDao.insertAll(toInsert)
            
            // R197: Chunked Pruning Implementation
            val limit = getEffectivePruneLimit()
            val count = pendingStatusDao.getCount()
            if (count > limit) {
                val threshold = pendingStatusDao.getPruneThreshold(limit)
                threshold?.let {
                    pendingStatusDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to flush pending status buffer or prune")
            // Note: On failure, we don't re-add to buffer to prevent infinite loops, 
            // relying on the next successful batch or live signaling.
        }
    }

    /**
     * addPendingStatusUpdate: Zero-allocation path for LocationUpdate buffering (R1173).
     */
    suspend fun addPendingStatusUpdate(update: LocationUpdate) {
        val health = telemetry.systemHealth.value
        if (health.isStorageCritical) return
        
        try {
            val entity = TelemetryMapper.mapStatusToPending(update)
            addPendingStatusUpdate(entity)
        } catch (e: Exception) {
            Timber.e(e, "Binary pending status insertion failed")
        }
    }

    /**
     * getPendingStatusUpdates: Retrieves oldest updates with on-the-fly binary migration (R1173).
     */
    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> {
        // Ensure buffer is flushed before retrieval to maintain temporal consistency
        flushBuffer()
        
        val entities = pendingStatusDao.getOldestPending(limit)
        
        // Migrate legacy entries without payloads to binary format to ensure parity
        entities.filter { it.payload.isEmpty() }.forEach { legacy ->
            try {
                val flyweight = LocationUpdate()
                TelemetryMapper.mapPendingToStatus(legacy, "", "", flyweight)
                val updated = legacy.copy(payload = TelemetryProtobufMapper.mapStatusToBinary(flyweight))
                pendingStatusDao.insert(updated)
            } catch (e: Exception) {
                Timber.e(e, "Failed to migrate legacy pending update ${legacy.id}")
            }
        }
        
        return entities
    }

    suspend fun deletePendingStatusUpdate(id: Long) = 
        pendingStatusDao.deletePending(longArrayOf(id))

    suspend fun clear() {
        bufferMutex.withLock { buffer.clear() }
        pendingStatusDao.clearAll()
    }

    /**
     * trimMemory: Aggressive cleanup of pending updates under memory pressure.
     */
    suspend fun trimMemory() {
        bufferMutex.withLock { buffer.clear() }
        val limit = getEffectivePruneLimit()
        val threshold = pendingStatusDao.getPruneThreshold(limit)
        threshold?.let {
            pendingStatusDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE * 2)
            Timber.i("OfflineRepository: Memory trim completed (Limit: $limit)")
        }
    }
}
