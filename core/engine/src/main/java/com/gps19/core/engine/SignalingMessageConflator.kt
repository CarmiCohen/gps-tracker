package com.gps19.core.engine

/**
 * SignalingMessageConflator: Logic for merging partial signaling updates.
 * Oct.6.4:
 * - Issue #AUDIT-1006-7: Enhanced Conflation Logic. Implemented full map merge 
 *   to ensure telemetry fidelity (e.g., preserving battery/thermal snapshots 
 *   when location updates arrive).
 */
object SignalingMessageConflator {

    /**
     * Merges an incoming partial update into the pending update.
     * Prioritizes incoming values for duplicate keys but preserves 
     * unique keys from the pending map.
     */
    fun conflate(
        pending: Map<String, Any?>?,
        incoming: Map<String, Any?>
    ): Map<String, Any?> {
        if (pending == null) return incoming
        
        val merged = pending.toMutableMap()
        merged.putAll(incoming)
        return merged
    }
    
    /**
     * Conflates log entries to reduce radio usage.
     * If messages are identical or within a specific threshold, 
     * they can be merged or counted to preserve fidelity without 
     * redundant emissions.
     */
    fun conflateLogs(
        pending: Map<String, Any?>?,
        incoming: Map<String, Any?>
    ): Map<String, Any?> {
        if (pending == null) return incoming
        
        val pendingMsg = pending["message"] as? String
        val incomingMsg = incoming["message"] as? String
        
        // If messages are identical, we increment a 'burst' count instead of sending two packets.
        return if (pendingMsg != null && pendingMsg == incomingMsg) {
            val merged = pending.toMutableMap()
            val currentCount = (merged["burst_count"] as? Int) ?: 1
            merged["burst_count"] = currentCount + 1
            merged["timestamp"] = incoming["timestamp"] // Update to latest timestamp
            merged
        } else {
            // For different logs, we currently don't merge them into one packet 
            // to maintain forensic sequence, but the dispatcher can queue them.
            incoming
        }
    }
}
