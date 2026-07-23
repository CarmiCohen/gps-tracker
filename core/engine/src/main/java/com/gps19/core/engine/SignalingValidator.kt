package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * July.22.12:
 * - Issue #521: Deep Purge of Remote Settings Leftovers. Removed shouldProcessSettingsUpdate.
 * v9.3.22:
 * - Legacy Compatibility: Support both "viewer" and "client" labels.
 * - Alias-Aware Matching: Integrated SignalingConstants.isTrackerMatch and 
 *   isViewerMatch to support cross-version identity mapping (e.g. T vs Trk).
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
