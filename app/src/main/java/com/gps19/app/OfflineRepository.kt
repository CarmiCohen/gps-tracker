package com.gps19.app

import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Use systemHealth.
 */
@Singleton
class OfflineRepository @Inject constructor(
    private val pendingStatusDao: PendingStatusDao,
    private val telemetry: TelemetryRepository
) {
    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        if (telemetry.systemHealth.value.isStorageCritical) return

        pendingStatusDao.insert(update)
        pendingStatusDao.prune()
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
