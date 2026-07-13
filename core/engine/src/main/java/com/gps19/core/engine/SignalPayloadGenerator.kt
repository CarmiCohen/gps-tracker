package com.gps19.core.engine

/**
 * SignalPayloadGenerator: Pure Kotlin logic for constructing relay messages.
 * v9.3.24:
 * - Protocol Optimization: Integrated SignalingConstants.getTransmissionId() 
 *   to ensure legacy version compatibility (T -> Trk, V -> viewer).
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
            "id" to SignalingConstants.getTransmissionId(deviceId),
            "viewer_id" to SignalingConstants.getTransmissionId(viewerId),
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
        // We match against the raw ownDeviceId (alias-aware match happened in CommManager)
        val pingId = incomingPing["id"] as? String ?: return null
        if (!SignalingConstants.isTrackerMatch(pingId, ownDeviceId)) return null

        // We return the incoming ping data as the basis for the pong.
        // It already has the transmission IDs that the requester used.
        return incomingPing
    }

    fun createViewerPulseUpdate(viewerId: String, isFromViewer: Boolean): Map<String, Any> {
        return mapOf(
            "type" to "viewer_pulse",
            "viewer_id" to SignalingConstants.getTransmissionId(viewerId),
            "from_viewer" to isFromViewer
        )
    }

    fun createTrackerPulseUpdate(viewerId: String, isFromViewer: Boolean): Map<String, Any> {
        return mapOf(
            "type" to "tracker_pulse",
            "viewer_id" to SignalingConstants.getTransmissionId(viewerId),
            "from_viewer" to isFromViewer
        )
    }
}
