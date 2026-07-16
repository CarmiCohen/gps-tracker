package com.gps19.app

/**
 * OfflineRepository: Manages persistent buffering of status updates during network loss.
 * v9.5.0: Hilt removed. Manual DI.
 */
class OfflineRepository(
    private val pendingStatusDao: PendingStatusDao,
    private val telemetry: TelemetryRepository
) {
    suspend fun addPendingStatusUpdate(update: PendingStatusEntity) {
        if (telemetry.integrityState.value.isStorageCritical) return

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
