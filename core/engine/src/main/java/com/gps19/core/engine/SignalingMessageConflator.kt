package com.gps19.core.engine

/**
 * SignalingMessageConflator: Logic for merging partial signaling updates.
 * v8.8.21: Extracted from CommunicationManager to ensure logic purity.
 */
object SignalingMessageConflator {

    /**
     * Merges an incoming partial update into the pending update.
     * If the new update contains core coordinates ("lat"), it typically replaces the pending one
     * to avoid jumping between old coordinates and new secondary data.
     * If the new update is purely secondary (no "lat"), it is merged into the existing pending update.
     */
    fun conflate(
        pending: Map<String, Any>?,
        incoming: Map<String, Any>
    ): Map<String, Any> {
        if (pending == null) return incoming
        
        val incomingHasLat = incoming.containsKey("lat")
        val pendingHasLat = pending.containsKey("lat")

        return if (pendingHasLat && !incomingHasLat) {
            // Merge secondary data into existing coordinate-rich packet
            val merged = pending.toMutableMap()
            merged.putAll(incoming)
            merged
        } else {
            // New coordinates or no previous pending: use incoming as base
            incoming
        }
    }
}
