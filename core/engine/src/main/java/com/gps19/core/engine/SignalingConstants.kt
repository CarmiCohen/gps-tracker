package com.gps19.core.engine

/**
 * SignalingConstants: Core rules for relay communication and role enforcement.
 * Oct24.16:
 * - SIMP-OCT22.0-S1: Delegated identity validation to ConfigValidator.
 * Oct23.107:
 * - ISSUE-OCT23.102-F7: Added DELTA_REFRESH_INTERVAL for periodic absolute 
 *   coordinate refresh to prevent delta drift.
 * v9.3.23:
 * - Robust Aliasing: Broadened matchers to support bidirectional cross-version 
 *   identity mapping (e.g. T vs Trk) to ensure connectivity between old and new apps.
 */
object SignalingConstants {
    const val DEFAULT_TRACKER_ID = "T"
    const val DEFAULT_VIEWER_ID = "V"
    
    // Legacy Aliases
    const val LEGACY_TRACKER_ID = "Trk"
    const val LEGACY_VIEWER_ID = "viewer"
    
    /**
     * R183: Periodic absolute refresh interval (packets) to prevent delta drift.
     */
    const val DELTA_REFRESH_INTERVAL = 50
    
    // Delegated to ConfigValidator (SIMP-OCT22.0-S1)
    fun isValidTrackerId(id: String): Boolean = ConfigValidator.isValidTrackerId(id)
    fun isValidViewerId(id: String): Boolean = ConfigValidator.isValidViewerId(id)
    fun areIdsUnique(trackerId: String, viewerId: String): Boolean = ConfigValidator.areIdsUnique(trackerId, viewerId)
    
    /**
     * R182: Returns the ID to be used for room joining and payload labeling.
     * Maps 'T' -> 'Trk' and 'V' -> 'viewer' for backward compatibility.
     */
    fun getTransmissionId(id: String): String {
        return when (id) {
            DEFAULT_TRACKER_ID -> LEGACY_TRACKER_ID
            DEFAULT_VIEWER_ID -> LEGACY_VIEWER_ID
            else -> id
        }
    }
    
    /**
     * R182: Matches IDs if they are identical OR if they are both in the 
     * permitted default/legacy set for their respective role.
     */
    fun isTrackerMatch(idA: String, idB: String): Boolean {
        if (idA == idB) return true
        val isALegacySet = (idA == DEFAULT_TRACKER_ID || idA == LEGACY_TRACKER_ID)
        val isBLegacySet = (idB == DEFAULT_TRACKER_ID || idB == LEGACY_TRACKER_ID)
        return isALegacySet && isBLegacySet
    }

    fun isViewerMatch(idA: String, idB: String): Boolean {
        if (idA == idB) return true
        val isALegacySet = (idA == DEFAULT_VIEWER_ID || idA == LEGACY_VIEWER_ID)
        val isBLegacySet = (idB == DEFAULT_VIEWER_ID || idB == LEGACY_VIEWER_ID)
        return isALegacySet && isBLegacySet
    }

    fun getPeerTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer" else "tracker"
    fun getOwnTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "tracker" else "viewer"
    fun getPulseType(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer_pulse" else "tracker_pulse"
}
