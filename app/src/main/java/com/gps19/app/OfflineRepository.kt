package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * Aug.22.03:
 * - Issue #197 Hardening: Aligned with R197 chunked pruning standards. Implemented 
 *   staggered deletion for pending_status_updates to prevent I/O stalls (R197).
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
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
