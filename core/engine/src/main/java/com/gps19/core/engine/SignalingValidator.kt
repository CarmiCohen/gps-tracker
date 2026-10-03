package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * Oct.3.6:
 * - Bug Fix: Corrected inverted logic in shouldProcessLogRelay which was 
 *   dropping legitimate tracker logs on the viewer.
 * - Forensic Alignment: Aligned getDropReason with new Log Relay rules (R1422).
 */
object SignalingValidator {

    private fun isDefault(id: String) = id == SignalingConstants.DEFAULT_TRACKER_ID || 
                                       id == SignalingConstants.DEFAULT_VIEWER_ID || 
                                       id.isEmpty()

    /**
     * R182: Helper to handle legacy role labels.
     */
    fun isViewerRole(role: String?): Boolean {
        return role == "viewer" || role == "client"
    }

    /**
     * Determines if a location update should be processed by the current device.
     */
    fun shouldProcessLocationUpdate(
        incomingId: String,
        ownDeviceId: String,
        isFromViewer: Boolean,
        viewerId: String, 
        ownViewerId: String, 
        isTrackerMode: Boolean
    ): Boolean {
        // R182: Strict Tracker ID match is always required.
        if (!SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) return false

        // Tracker Mode: Only accept from our authorized Viewer or if we are in "Discovery"
        if (isTrackerMode) {
            return isFromViewer && (SignalingConstants.isViewerMatch(viewerId, ownViewerId) || isDefault(ownViewerId))
        }
        
        // Viewer Mode: 
        // 1. Suppress self-echoes.
        if (!isTrackerMode && isFromViewer && SignalingConstants.isViewerMatch(viewerId, ownViewerId)) return false
        
        // 2. Accept all packets from the target Tracker ID (even if coordinates are 0,0 heartbeat).
        return true
    }

    /**
     * R-ID 320: Forensic drop analysis.
     */
    fun getDropReason(
        incomingId: String,
        ownDeviceId: String,
        isFromViewer: Boolean,
        viewerId: String,
        ownViewerId: String,
        isTrackerMode: Boolean
    ): String? {
        if (!SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) {
            return "ID mismatch: PacketID=$incomingId (Expected=$ownDeviceId)"
        }

        if (isTrackerMode) {
            if (!isFromViewer) {
                return "Tracker dropped packet from another Tracker"
            }
            if (!SignalingConstants.isViewerMatch(viewerId, ownViewerId) && !isDefault(ownViewerId)) {
                return "Unauthorized Viewer: Incoming=$viewerId (Authorized=$ownViewerId)"
            }
        } else {
            if (isFromViewer && SignalingConstants.isViewerMatch(viewerId, ownViewerId)) {
                return "Echo suppression: Ignored own reflected command"
            }
        }
        return null
    }
    
    fun shouldProcessLogRelay(
        incomingId: String,
        ownDeviceId: String,
        incomingViewerId: String,
        ownViewerId: String,
        isTrackerMode: Boolean,
        isFromViewer: Boolean
    ): Boolean {
        if (!SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) return false
        
        if (isTrackerMode) {
            // Tracker only processes logs from its authorized viewer
            return isFromViewer && (SignalingConstants.isViewerMatch(incomingViewerId, ownViewerId) || isDefault(ownViewerId))
        } else {
            // Viewer processes all logs for its tracker, except self-echoes
            if (isFromViewer && SignalingConstants.isViewerMatch(incomingViewerId, ownViewerId)) return false
            return true
        }
    }
}
