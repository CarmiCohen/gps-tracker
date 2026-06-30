package com.gps19.core.engine

/**
 * SignalingConstants: Core rules for relay communication and role enforcement.
 * v8.9.63:
 * - Issue #461: Hardened uniqueness enforcement feedback.
 * v8.9.61:
 * - R924: Added VID_NOTES for UI labeling.
 * v8.9.50:
 * - R182 Relaxation: IDs are now free-form strings.
 * - Minimum length: 1 character.
 * - Enforces uniqueness concept (Tracker ID != Viewer ID).
 */
object SignalingConstants {
    const val DEFAULT_TRACKER_ID = "T"
    const val DEFAULT_VIEWER_ID = "V"
    
    // R924-A: Hard-coded note for UI display.
    const val VID_NOTES = "renumb"

    /**
     * R182: Validation relaxed to non-empty (min 1 char).
     */
    fun isValidTrackerId(id: String) = id.trim().isNotEmpty()
    fun isValidViewerId(id: String) = id.trim().isNotEmpty()
    
    /**
     * R182: IDs must be unique to prevent signal echoes.
     */
    fun areIdsUnique(trackerId: String, viewerId: String): Boolean {
        return trackerId.trim().lowercase() != viewerId.trim().lowercase()
    }
    
    fun getPeerTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer" else "tracker"
    fun getOwnTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "tracker" else "viewer"
    
    fun getPulseType(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer_pulse" else "tracker_pulse"
}
