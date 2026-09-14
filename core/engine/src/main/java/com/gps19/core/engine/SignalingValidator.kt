package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * Sep.14.10:
 * - Forensic Visibility (#1020): Refined getDropReason to distinguish between 
 *   self-echoes and packets from other trackers to prevent misleading triage logs.
 * Sep.14.00:
 * - Forensic Visibility (#1019): Added getDropReason to provide detailed 
 *   explanation for rejected signaling packets (R-ID 320).
 */
object SignalingValidator {

    private fun isDefault(id: String) = id == SignalingConstants.DEFAULT_VIEWER_ID || id.isEmpty()

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
        viewerId: String, // The viewer ID of the incoming packet
        ownViewerId: String, // Our own viewer ID
        isTrackerMode: Boolean
    ): Boolean {
        if (!SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) return false

        // Tracker: Only process updates from the specific authorized viewer or if not yet locked.
        if (isTrackerMode) {
            return isFromViewer && (SignalingConstants.isViewerMatch(viewerId, ownViewerId) || isDefault(ownViewerId))
        }
        
        // Viewer: Do NOT process updates that come from MYSELF (echo suppression).
        if (!isTrackerMode && isFromViewer && SignalingConstants.isViewerMatch(viewerId, ownViewerId)) return false

        return true
    }

    /**
     * R-ID 320: Forensic drop analysis. Returns a human-readable reason for rejection.
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
            return "ID mismatch: Incoming=$incomingId, Local=$ownDeviceId"
        }

        if (isTrackerMode) {
            if (!isFromViewer) {
                // Sep.14.10: Distinguish between echo and foreign tracker
                return if (SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) {
                    "Echo suppression: Tracker received its own reflected packet"
                } else {
                    "Tracker dropped packet from another Tracker"
                }
            }
            if (!SignalingConstants.isViewerMatch(viewerId, ownViewerId) && !isDefault(ownViewerId)) {
                return "Unauthorized Viewer: Incoming=$viewerId, Authorized=$ownViewerId"
            }
        } else {
            if (isFromViewer && SignalingConstants.isViewerMatch(viewerId, ownViewerId)) {
                return "Echo suppression: Viewer received its own reflected command"
            }
        }
        return null
    }
    
    /**
     * Determines if a log message should be processed.
     */
    fun shouldProcessLogRelay(
        incomingId: String,
        ownDeviceId: String,
        incomingViewerId: String,
        ownViewerId: String,
        isTrackerMode: Boolean
    ): Boolean {
        if (!SignalingConstants.isTrackerMatch(incomingId, ownDeviceId)) return false

        // Tracker handles logs from its specific authorized viewer or if not yet locked.
        if (isTrackerMode) {
            return SignalingConstants.isViewerMatch(incomingViewerId, ownViewerId) || isDefault(ownViewerId)
        }

        // Viewer handles logs from tracker or other viewers, but suppresses its own.
        if (SignalingConstants.isViewerMatch(incomingViewerId, ownViewerId)) return false
        
        return true
    }
}
