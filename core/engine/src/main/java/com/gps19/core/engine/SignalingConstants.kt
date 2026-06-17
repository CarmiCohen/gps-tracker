package com.gps19.core.engine

/**
 * SignalingConstants: Core rules for relay communication and role enforcement.
 * v8.8.21:
 * - Role-Based Standardization: Explicitly enforcing Tracker ID ("T") vs Viewer ID ("C").
 */
object SignalingConstants {
    const val TRACKER_PREFIX = "T"
    const val VIEWER_PREFIX = "C"

    fun isValidTrackerId(id: String) = id.startsWith(TRACKER_PREFIX)
    fun isValidViewerId(id: String) = id.startsWith(VIEWER_PREFIX)
    
    fun getPeerTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer" else "tracker"
    fun getOwnTypeLabel(isTrackerMode: Boolean): String = if (isTrackerMode) "tracker" else "viewer"
    
    fun getPulseType(isTrackerMode: Boolean): String = if (isTrackerMode) "viewer_pulse" else "tracker_pulse"
}
