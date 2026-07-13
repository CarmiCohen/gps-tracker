package com.gps19.core.engine

/**
 * SignalingConstants: Core rules for relay communication and role enforcement.
 * v9.3.23:
 * - Robust Aliasing: Broadened matchers to support bidirectional cross-version 
 *   identity mapping (e.g. T vs Trk) to ensure connectivity between old and new apps.
 * v9.3.24:
 * - Alias-Aware Uniqueness: Enhanced areIdsUnique to prevent cross-role reserved ID 
 *   contamination (e.g., Tracker cannot be named 'V' or 'viewer').
 * - Transmission Mapping: Added getTransmissionId to ensure defaults map to 
 *   legacy strings (T -> Trk) for backward compatibility with older viewers.
 */
object SignalingConstants {
    const val DEFAULT_TRACKER_ID = "T"
    const val DEFAULT_VIEWER_ID = "V"
    
    // Legacy Aliases
    const val LEGACY_TRACKER_ID = "Trk"
    const val LEGACY_VIEWER_ID = "viewer"
    
    private val ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,32}$")

    fun isValidTrackerId(id: String): Boolean {
        val trimmed = id.trim()
        return ID_REGEX.matches(trimmed) || trimmed == LEGACY_TRACKER_ID
    }
    
    fun isValidViewerId(id: String): Boolean {
        val trimmed = id.trim()
        return ID_REGEX.matches(trimmed) || trimmed == LEGACY_VIEWER_ID
    }
    
    /**
     * R182: Ensures IDs are not only string-unique but also do not cross-contaminate
     * the reserved alias sets of the opposite role.
     */
    fun areIdsUnique(trackerId: String, viewerId: String): Boolean {
        val t = trackerId.trim()
        val v = viewerId.trim()
        if (!isValidTrackerId(t) || !isValidViewerId(v)) return false
        
        // 1. Strict string equality
        if (t.lowercase() == v.lowercase()) return false
        
        // 2. Reserved Set Cross-Contamination
        // Tracker cannot use Viewer reserved IDs
        if (t == DEFAULT_VIEWER_ID || t == LEGACY_VIEWER_ID) return false
        // Viewer cannot use Tracker reserved IDs
        if (v == DEFAULT_TRACKER_ID || v == LEGACY_TRACKER_ID) return false
        
        return true
    }
    
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
