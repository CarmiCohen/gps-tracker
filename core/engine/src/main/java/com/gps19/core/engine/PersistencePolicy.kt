package com.gps19.core.engine

/**
 * PersistencePolicy: Rules for determining if data should be written to disk.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Use SystemHealthState.
 */
object PersistencePolicy {

    /**
     * Determines if a trail point should be saved based on storage state and point priority.
     */
    fun shouldSaveTrailPoint(
        health: SystemHealthState,
        status: SentinelStatus
    ): Boolean {
        if (health.isStorageCritical) return false
        
        // On low storage, we only save high-priority forensic points (JUMP or TAMPER).
        if (health.isStorageLow && status == SentinelStatus.VALID) return false
        
        return true
    }

    /**
     * Determines if history/telemetry points should be saved.
     */
    fun shouldSaveHistoryPoint(health: SystemHealthState): Boolean {
        return !health.isStorageCritical
    }
}
