package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * Oct.4.6:
 * - Issue #1173: Protobuf-First Persistence. Integrated binary payload insertion 
 *   to minimize disk I/O and Room overhead (R1173).
 */
@Singleton
class OfflineRepository @Inject constructor(
    private val pendingStatusDao: PendingStatusDao,
    private val telemetry: TelemetryRepository
) {
    private companion object {
        private const val OFFLINE_PRUNE_LIMIT = 2000
        private const val PRUNE_CHUNK_SIZE = 500
    }

    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        val health = telemetry.systemHealth.value
        if (health.isStorageCritical) return

        try {
            pendingStatusDao.insert(update)
            
            // R197: Chunked Pruning Implementation
            val threshold = pendingStatusDao.getPruneThreshold(OFFLINE_PRUNE_LIMIT)
            threshold?.let {
                pendingStatusDao.pruneByThreshold(it, PRUNE_CHUNK_SIZE)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add or prune pending status update")
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

    suspend fun getPendingStatusUpdates(limit: Int): List<PendingStatusEntity> = 
        pendingStatusDao.getOldestPending(limit)

    suspend fun deletePendingStatusUpdate(id: Long) = 
        pendingStatusDao.deletePending(longArrayOf(id))

    /**
     * Issue 51: Purges all buffered telemetry.
     */
    suspend fun clear() {
        pendingStatusDao.clearAll()
    }
}
