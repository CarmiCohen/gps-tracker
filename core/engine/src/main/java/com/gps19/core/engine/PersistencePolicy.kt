package com.gps19.core.engine

/**
 * PersistencePolicy: Rules for determining if data should be written to disk.
 * Oct.11.1:
 * - Issue #SIMP-1011-8: I/O Pressure Test. Integrated isMaliAnomaly into 
 *   persistence gating to inhibit IO during driver-level stalls.
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
        // Issue #SIMP-1011-8: Block trail IO during Mali anomalies to mitigate contention.
        if (health.isStorageCritical || health.isMaliAnomaly) return false
        
        // On low storage, we only save high-priority forensic points (JUMP or TAMPER).
        if (health.isStorageLow && status == SentinelStatus.VALID) return false
        
        return true
    }

    /**
     * Determines if history/telemetry points should be saved.
     */
    fun shouldSaveHistoryPoint(health: SystemHealthState): Boolean {
        // Issue #SIMP-1011-8: Block history IO during Mali anomalies.
        return !health.isStorageCritical && !health.isMaliAnomaly
    }
}
