package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * v8.9.5:
 * - Identity Lock Relaxation: Allows processing if ownViewerId is default ("V") 
 *   to support first-time pairing/adoption logic.
 * v8.9.0:
 * - Identity Lock: Enforced strict viewerId matching for trackers.
 */
object SignalingValidator {

    private fun isDefault(id: String) = id == SignalingConstants.DEFAULT_VIEWER_ID || id.isEmpty()

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
        if (incomingId != ownDeviceId || ownDeviceId.isEmpty()) return false

        // Tracker: Only process updates from the specific authorized viewer or if not yet locked.
        if (isTrackerMode) {
            return isFromViewer && (viewerId == ownViewerId || isDefault(ownViewerId))
        }
        
        // Viewer: Do NOT process updates that come from MYSELF (echo suppression).
        if (!isTrackerMode && isFromViewer && viewerId == ownViewerId) return false

        return true
    }

    /**
     * Determines if a settings update should be processed.
     */
    fun shouldProcessSettingsUpdate(
        incomingId: String,
        ownDeviceId: String,
        incomingViewerId: String,
        ownViewerId: String,
        fromViewer: Boolean,
        isTrackerMode: Boolean
    ): Boolean {
        if (incomingId != ownDeviceId || ownDeviceId.isEmpty()) return false
        
        // Only Trackers process incoming settings updates, and they must come from the AUTHORIZED or UNLOCKED Viewer.
        return isTrackerMode && fromViewer && (incomingViewerId == ownViewerId || isDefault(ownViewerId))
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
        if (incomingId != ownDeviceId || ownDeviceId.isEmpty()) return false

        // Tracker handles logs from its specific authorized viewer or if not yet locked.
        if (isTrackerMode) {
            return incomingViewerId == ownViewerId || isDefault(ownViewerId)
        }

        // Viewer handles logs from tracker or other viewers, but suppresses its own.
        if (incomingViewerId == ownViewerId) return false
        
        return true
    }
}
