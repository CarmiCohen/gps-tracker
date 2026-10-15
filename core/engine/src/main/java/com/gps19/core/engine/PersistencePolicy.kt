package com.gps19.core.engine

/**
 * PersistencePolicy: Rules for determining if data should be written to disk.
 * Oct.15.9:
 * - ISSUE-OCT15.9-F1: Integrated log gating into PersistencePolicy to ensure 
 *   consistent storage pressure handling across all repositories.
 * Oct.15.7:
 * - ISSUE-OCT15.7-F1: Expanded policy to include Violation and Maintenance gating.
 *   Ensured Mali anomalies block all IO to prevent driver-level contention.
 * Oct.11.1:
 * - Issue #SIMP-1011-8: I/O Pressure Test. Integrated isMaliAnomaly into 
 *   persistence gating.
 */
object PersistencePolicy {

    /**
     * Determines if a trail point should be saved based on storage state and point priority.
     */
    fun shouldSaveTrailPoint(
        health: SystemHealthState,
        status: SentinelStatus
    ): Boolean {
        // Block trail IO during Mali anomalies or critical storage.
        if (health.isStorageCritical || health.isMaliAnomaly) return false
        
        // On low storage, we only save high-priority forensic points (JUMP or TAMPER).
        if (health.isStorageLow && status == SentinelStatus.VALID) return false
        
        return true
    }

    /**
     * Determines if history/telemetry points should be saved.
     */
    fun shouldSaveHistoryPoint(health: SystemHealthState): Boolean {
        // Block history IO during Mali anomalies or critical storage.
        return !health.isStorageCritical && !health.isMaliAnomaly
    }

    /**
     * Determines if a violation should be saved. 
     * Violations are higher priority than trails but must respect critical failures.
     */
    fun shouldSaveViolation(health: SystemHealthState): Boolean {
        return !health.isStorageCritical && !health.isMaliAnomaly
    }

    /**
     * Determines if a log should be saved based on storage state and log priority.
     */
    fun shouldSaveLog(
        health: SystemHealthState,
        isImportant: Boolean,
        isSpecial: Boolean
    ): Boolean {
        // Mali anomalies block all standard logging to prevent I/O deadlock.
        if (health.isMaliAnomaly && !isSpecial) return false
        
        if (health.isStorageCritical && !isSpecial) return false
        if (health.isStorageLow && !isImportant && !isSpecial) return false
        
        return true
    }

    /**
     * Determines if background maintenance (pruning) should run.
     * Pruning is heavy IO; inhibit during driver stalls or battery stress.
     */
    fun shouldPerformMaintenance(health: SystemHealthState): Boolean {
        // Pruning helps storage but must wait if there's a driver-level stall (Mali).
        return !health.isBatteryCritical && !health.isMaliAnomaly
    }
}
