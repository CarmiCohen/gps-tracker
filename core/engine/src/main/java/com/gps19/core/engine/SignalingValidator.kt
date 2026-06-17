package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * v8.8.21: Standardized echo suppression and role guarding to prevent pulse regressions.
 */
object SignalingValidator {

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

        // Echo Suppression logic:
        // 1. If I'm a Tracker, I should only process updates that come FROM a Viewer.
        if (isTrackerMode && !isFromViewer) return false
        
        // 2. If I'm a Viewer, I should NOT process updates that come from MYSELF.
        if (!isTrackerMode && isFromViewer && viewerId == ownViewerId) return false

        return true
    }

    /**
     * Determines if a settings update should be processed.
     * Trackers only process settings updates coming from authorized Viewers.
     */
    fun shouldProcessSettingsUpdate(
        incomingId: String,
        ownDeviceId: String,
        fromViewer: Boolean,
        isTrackerMode: Boolean
    ): Boolean {
        if (incomingId != ownDeviceId || ownDeviceId.isEmpty()) return false
        
        // Only Trackers process incoming settings updates, and they must come from a Viewer.
        return isTrackerMode && fromViewer
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

        // Tracker handles logs from viewers. Viewer handles logs from tracker or other viewers.
        if (isTrackerMode && incomingViewerId.isEmpty()) return false
        if (!isTrackerMode && incomingViewerId == ownViewerId) return false
        
        return true
    }
}
