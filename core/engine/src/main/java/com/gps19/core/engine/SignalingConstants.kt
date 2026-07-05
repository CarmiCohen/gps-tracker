package com.gps19.core.engine

/**
 * SignalingConstants: Core rules for relay communication and role enforcement.
 * v8.9.99:
 * - Issue #041: ID Sanitization. Implemented strict alphanumeric validation 
 *   (Regex: ^[a-zA-Z0-9_-]{1,32}$) to prevent command injection and identity corruption.
 */
object SignalingConstants {
    const val DEFAULT_TRACKER_ID = "T"
    const val DEFAULT_VIEWER_ID = "V"
    
    // Strict alphanumeric/underscore/hyphen pattern. Rejects spaces and special shell characters.
    private val ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,32}$")

    /**
     * R182: Validation enforced: 1-32 alphanumeric/underscore/hyphen chars.
     */
    fun isValidTrackerId(id: String): Boolean = ID_REGEX.matches(id.trim())
    fun isValidViewerId(id: String): Boolean = ID_REGEX.matches(id.trim())
    
    /**
     * R182: IDs must be unique to prevent signal echoes.
     */
    fun areIdsUnique(trackerId: String, viewerId: String): Boolean {
        if (!isValidTrackerId(trackerId) || !isValidViewerId(viewerId)) return false
        return trackerId.trim().lowercase() != viewerId.trim().lowercase()
    }
    
    fun getPeerTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer" else "tracker"
    fun getOwnTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "tracker" else "viewer"
    
    fun getPulseType(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer_pulse" else "tracker_pulse"
}
