package com.gps19.core.engine

/**
 * SignalingValidator: Pure logic for enforcing role-based message filtering.
 * Oct.24.00:
 * - Discovery Mode Relaxation (ISSUE-OCT24.00-F2): Relaxed isDiscoveryMode to 
 *   allow Trackers with default viewer IDs to accept custom viewer pulses 
 *   during initial pairing.
 * Oct.22.12:
 * - Connection Asymmetry Fix (#ISSUE-OCT22.0-F3): Enforced strict Viewer ID 
 *   symmetry and blocks cross-viewer traffic to prevent erroneous activity 
 *   badges and telemetry leaks.
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
     * Determines if a location update (or generic signaling event) should be processed.
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

        val isDirectMatch = SignalingConstants.isViewerMatch(viewerId, ownViewerId)
        // Oct.24.00: Discovery mode is active if own state is default.
        val isDiscoveryMode = isDefault(ownViewerId)

        if (isTrackerMode) {
            // Tracker accepts packets from a Viewer if it's a direct match or we are in discovery.
            return isFromViewer && (isDirectMatch || isDiscoveryMode)
        } else {
            // Viewer Mode: 
            // 1. Suppress self-echoes.
            if (isFromViewer && isDirectMatch) return false
            
            // 2. Acceptance logic for packets from the Tracker:
            if (!isFromViewer) {
                return isDirectMatch || isDiscoveryMode
            }
            
            // 3. Reject packets from other Viewers to prevent false activity badges.
            return false
        }
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

        val isDirectMatch = SignalingConstants.isViewerMatch(viewerId, ownViewerId)
        val isDiscoveryMode = isDefault(ownViewerId)

        if (isTrackerMode) {
            if (!isFromViewer) {
                return "Echo suppression: Ignored own reflected pulse"
            }
            if (!isDirectMatch && !isDiscoveryMode) {
                return "Unauthorized Viewer: Incoming=$viewerId (Authorized=$ownViewerId)"
            }
        } else {
            if (isFromViewer && isDirectMatch) {
                return "Echo suppression: Ignored own reflected command"
            }
            if (!isFromViewer) {
                if (!isDirectMatch && !isDiscoveryMode) {
                    return "Viewer ID mismatch: Packet target=$viewerId (Own=$ownViewerId)"
                }
            } else {
                return "Unauthorized Peer: Ignored packet from another Viewer ($viewerId)"
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
        return shouldProcessLocationUpdate(incomingId, ownDeviceId, isFromViewer, incomingViewerId, ownViewerId, isTrackerMode)
    }
}
