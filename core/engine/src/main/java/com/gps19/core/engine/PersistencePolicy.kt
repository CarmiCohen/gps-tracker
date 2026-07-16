package com.gps19.core.engine

/**
 * PersistencePolicy: Rules for determining if data should be written to disk.
 * July.1.15:
 * - Issue #512: Consolidate Sentinel Statuses. Aligned storage gating with SentinelStatus.
 */
object PersistencePolicy {

    /**
     * Determines if a trail point should be saved based on storage state and point priority.
     */
    fun shouldSaveTrailPoint(
        isStorageCritical: Boolean,
        isStorageLow: Boolean,
        status: SentinelStatus
    ): Boolean {
        if (isStorageCritical) return false
        
        // On low storage, we only save high-priority forensic points (JUMP or TAMPER).
        if (isStorageLow && status == SentinelStatus.VALID) return false
        
        return true
    }

    /**
     * Determines if history/telemetry points should be saved.
     */
    fun shouldSaveHistoryPoint(isStorageCritical: Boolean): Boolean {
        return !isStorageCritical
    }
}
