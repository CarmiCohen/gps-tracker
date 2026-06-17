package com.gps19.core.engine

/**
 * SignalPayloadGenerator: Pure Kotlin logic for constructing relay messages.
 * v8.8.21:
 * - Decentralized signaling: Moved payload construction from :app to :core:engine.
 * v8.8.28: Standardized signaling keys to snake_case (viewer_id).
 * v8.8.32: Removed vid from signaling.
 */
object SignalPayloadGenerator {

    fun createPingPayload(
        deviceId: String,
        viewerId: String,
        isTracker: Boolean,
        timestamp: Long,
        version: String
    ): Map<String, Any> {
        return mapOf(
            "id" to deviceId,
            "viewer_id" to viewerId,
            "from" to SignalingConstants.getOwnTypeLabel(isTracker),
            "ts" to timestamp,
            "ver" to version
        )
    }

    /**
     * Creates a pong response from an incoming ping.
     * v8.8.21: Ensures the response maintains role integrity.
     */
    fun createPongPayload(
        incomingPing: Map<String, Any>,
        ownDeviceId: String,
        isTracker: Boolean
    ): Map<String, Any>? {
        val pingId = incomingPing["id"] as? String ?: return null
        if (pingId != ownDeviceId) return null

        // We essentially return the ping data back as a pong, 
        // but it's now wrapped in the pong_cmd event.
        return incomingPing
    }

    fun createViewerPulseUpdate(viewerId: String, isFromViewer: Boolean): Map<String, Any> {
        return mapOf(
            "type" to "viewer_pulse",
            "viewer_id" to viewerId,
            "from_viewer" to isFromViewer
        )
    }

    fun createTrackerPulseUpdate(viewerId: String, isFromViewer: Boolean): Map<String, Any> {
        return mapOf(
            "type" to "tracker_pulse",
            "viewer_id" to viewerId,
            "from_viewer" to isFromViewer
        )
    }
}
