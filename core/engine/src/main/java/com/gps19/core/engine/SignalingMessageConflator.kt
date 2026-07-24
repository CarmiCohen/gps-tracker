package com.gps19.core.engine

/**
 * SignalingMessageConflator: Logic for merging partial signaling updates.
 * July.24.05:
 * - Type Safety: Updated to Map<String, Any?> to support null JSON values.
 */
object SignalingMessageConflator {

    /**
     * Merges an incoming partial update into the pending update.
     */
    fun conflate(
        pending: Map<String, Any?>?,
        incoming: Map<String, Any?>
    ): Map<String, Any?> {
        if (pending == null) return incoming
        
        val incomingHasLat = incoming.containsKey("lat")
        val pendingHasLat = pending.containsKey("lat")

        return if (pendingHasLat && !incomingHasLat) {
            val merged = pending.toMutableMap()
            merged.putAll(incoming)
            merged
        } else {
            incoming
        }
    }
}
