package com.gps19.core.engine

/**
 * SignalingMessageConflator: Logic for merging partial signaling updates.
 * Oct.6.5:
 * - Issue #AUDIT-1006-7: Binary Telemetry Conflation. Implemented deep-merge 
 *   for LocationUpdate objects to allow Protobuf conflation before serialization.
 *   This preserves forensic fidelity (Rule 1.122 / R-ID 511).
 * Oct.6.4:
 * - Issue #AUDIT-1006-7: Enhanced Conflation Logic. Implemented full map merge 
 *   for JSON telemetry.
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
     * Deep-merges two LocationUpdate objects.
     * Preserves forensic snapshots (thermal, heap, vibe) from the pending object 
     * if the incoming one has default/null values.
     */
    fun conflateLocationUpdate(
        pending: LocationUpdate?,
        incoming: LocationUpdate
    ): LocationUpdate {
        if (pending == null) return incoming

        // R-ID 511: Preserve fidelity across the burst.
        // If incoming has no thermal snapshot but pending does, keep pending's.
        if (incoming.thermalSnapshot == null && pending.thermalSnapshot != null) {
            incoming.thermalSnapshot = pending.thermalSnapshot
        }
        if (incoming.heapSnapshot == null && pending.heapSnapshot != null) {
            incoming.heapSnapshot = pending.heapSnapshot
        }
        if (incoming.snrSnapshot == null && pending.snrSnapshot != null) {
            incoming.snrSnapshot = pending.snrSnapshot
        }
        if (incoming.vibeSnapshot == null && pending.vibeSnapshot != null) {
            incoming.vibeSnapshot = pending.vibeSnapshot
        }

        // Preserve accumulated integrity stats if incoming hasn't updated them
        if (incoming.integrity.uptimeMs == 0L && pending.integrity.uptimeMs > 0) {
            incoming.integrity.uptimeMs = pending.integrity.uptimeMs
        }

        return incoming
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
