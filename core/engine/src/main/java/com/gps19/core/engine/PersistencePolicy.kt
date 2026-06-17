package com.gps19.core.engine

/**
 * PersistencePolicy: Rules for determining if data should be written to disk.
 * v8.8.21: Extracted from MainRepository to decouple storage logic from the persistence layer.
 */
object PersistencePolicy {

    /**
     * Determines if a trail point should be saved based on storage state and point priority.
     */
    fun shouldSaveTrailPoint(
        isStorageCritical: Boolean,
        isStorageLow: Boolean,
        isJump: Boolean,
        isSuspicious: Boolean
    ): Boolean {
        if (isStorageCritical) return false
        
        // On low storage, we only save high-priority forensic points (jumps or suspicious activity).
        if (isStorageLow && !isJump && !isSuspicious) return false
        
        return true
    }

    /**
     * Determines if history/telemetry points should be saved.
     */
    fun shouldSaveHistoryPoint(isStorageCritical: Boolean): Boolean {
        return !isStorageCritical
    }
}
