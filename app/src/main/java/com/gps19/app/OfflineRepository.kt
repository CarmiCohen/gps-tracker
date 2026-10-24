package com.gps19.app

import android.os.SystemClock
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * Oct24.2:
 * - ISSUE-OCT23.101-F6: Offline Violation Event Sync. Added support for 
 *   PendingViolationEntity buffering and flushing.
 * Oct.13.5:
 * - Hardened persistence actor for extreme concurrent burst handling.
 */
@Singleton
class OfflineRepository @Inject constructor(
    private val pendingStatusDao: PendingStatusDao,
    private val pendingViolationDao: PendingViolationDao,
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
    private val updateChannel = Channel<PersistenceTask>(Channel.UNLIMITED)
    private val statusBuffer = mutableListOf<PendingStatusEntity>()
    private val violationBuffer = mutableListOf<PendingViolationEntity>()
    private val bufferMutex = Mutex()
    private var lastFlushRt = 0L

    sealed class PersistenceTask {
        data class Status(val entity: PendingStatusEntity) : PersistenceTask()
        data class Violation(val entity: PendingViolationEntity) : PersistenceTask()
    }

    init {
        startPersistenceActor()
    }

    private fun startPersistenceActor() {
        scope.launch(Dispatchers.IO) {
            val ticker = launch {
                while (isActive) {
                    delay(BATCH_FLUSH_INTERVAL_MS)
                    flushBuffer()
                }
            }

            try {
                updateChannel.consumeEach { task ->
                    bufferMutex.withLock {
                        when (task) {
                            is PersistenceTask.Status -> statusBuffer.add(task.entity)
                            is PersistenceTask.Violation -> violationBuffer.add(task.entity)
                        }
                        if (statusBuffer.size + violationBuffer.size >= BATCH_SIZE_THRESHOLD) {
                            flushInternal()
                        }
                    }
                }
            } finally {
                ticker.cancel()
            }
        }
    }

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
        updateChannel.send(PersistenceTask.Status(update))
    }

    suspend fun addPendingViolation(violation: PendingViolationEntity) {
        val health = telemetry.systemHealth.value
        if (health.isStorageCritical) return
        updateChannel.send(PersistenceTask.Violation(violation))
    }

    suspend fun flushBuffer() {
        bufferMutex.withLock {
            if (statusBuffer.isNotEmpty() || violationBuffer.isNotEmpty()) {
                flushInternal()
            }
        }
    }

    private suspend fun flushInternal() {
        val statusToInsert = ArrayList(statusBuffer)
        val violationsToInsert = ArrayList(violationBuffer)
        statusBuffer.clear()
        violationBuffer.clear()
        lastFlushRt = SystemClock.elapsedRealtime()

        try {
            if (statusToInsert.isNotEmpty()) {
                pendingStatusDao.insertAll(statusToInsert)
                pruneDao(pendingStatusDao)
            }
            if (violationsToInsert.isNotEmpty()) {
                violationsToInsert.forEach { pendingViolationDao.insert(it) }
                pruneDao(pendingViolationDao)
            }
        } catch (e: Exception) {
            Timber.e(e, "OfflineRepository: Batch flush failed")
        }
    }

    private suspend fun pruneDao(dao: Any) {
        val limit = getEffectivePruneLimit()
        when (dao) {
            is PendingStatusDao -> {
                if (dao.getCount() > limit) {
                    dao.getPruneThreshold(limit)?.let { dao.pruneByThreshold(it, PRUNE_CHUNK_SIZE) }
                }
            }
            is PendingViolationDao -> {
                if (dao.getCount() > limit) {
                    dao.getPruneThreshold(limit)?.let { dao.pruneByThreshold(it, PRUNE_CHUNK_SIZE) }
                }
            }
        }
    }

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

    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> {
        flushBuffer()
        val entities = pendingStatusDao.getOldestPending(limit)
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

    suspend fun getPendingViolations(limit: Int): List<PendingViolationEntity> {
        flushBuffer()
        return pendingViolationDao.getOldestPending(limit)
    }

    suspend fun deletePendingViolation(id: Long) = 
        pendingViolationDao.deletePending(longArrayOf(id))

    suspend fun clear() {
        bufferMutex.withLock { 
            statusBuffer.clear()
            violationBuffer.clear()
        }
        pendingStatusDao.clearAll()
        pendingViolationDao.clearAll()
    }

    suspend fun trimMemory() {
        bufferMutex.withLock { 
            statusBuffer.clear()
            violationBuffer.clear()
        }
        val limit = getEffectivePruneLimit()
        pendingStatusDao.getPruneThreshold(limit)?.let { pendingStatusDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE * 2) }
        pendingViolationDao.getPruneThreshold(limit)?.let { pendingViolationDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE * 2) }
    }
}
