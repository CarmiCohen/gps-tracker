package com.jd.app.monitor

import androidx.compose.ui.graphics.Color
import org.osmdroid.util.GeoPoint

val BrandJd = Color(0xFF06B6D4)
val ViewerCyan = Color(0xFF00E5FF)
val Slate500 = Color(0xFF64748B)
val Indigo500 = Color(0xFF6366F1)
val Rose500 = Color(0xFFF43F5E)

const val DEFAULT_LAT = 37.7749
const val DEFAULT_LNG = -122.4194
const val MARKER_POOL_PRUNE_THRESHOLD = 50

const val ALERT_ID_JUMP_ALERT = "jump_alert"
const val ALERT_ID_VISUAL_JUMP = "visual_jump"
const val ALERT_ID_TRACKER_GEOFENCE = "geofence_violation"

enum class MapFollowMode { TRACKER, VIEWER, AUTO, NONE }
enum class GeofenceMode { IDLE, ADD, REMOVE }

sealed class CameraAction {
    object CenterTracker : CameraAction()
    object CenterViewer : CameraAction()
    object ZoomIn : CameraAction()
    object ZoomOut : CameraAction()
}

sealed class UiEvent {
    data class MapTap(val point: GeoPoint) : UiEvent()
    data class SetMapLocked(val locked: Boolean) : UiEvent()
    object MapZoomIn : UiEvent()
    object MapZoomOut : UiEvent()
    object CenterTracker : UiEvent()
    object CenterViewer : UiEvent()
    data class SetGeofenceViolationsVisible(val visible: Boolean) : UiEvent()
    data class SetViolationsVisible(val visible: Boolean) : UiEvent()
    data class SetFenceVisible(val visible: Boolean) : UiEvent()
    data class SetGeofenceMode(val mode: GeofenceMode) : UiEvent()
    data class RemoveHomePoint(val index: Int) : UiEvent()
    object ClearTrails : UiEvent()
    object ExportTrails : UiEvent()
    object ImportTrails : UiEvent()
}

data class MapTrailSegment(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val color: Int,
    val checksum: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapTrailSegment) return false
        if (color != other.color) return false
        if (checksum != other.checksum) return false
        if (!lats.contentEquals(other.lats)) return false
        if (!lngs.contentEquals(other.lngs)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lats.contentHashCode()
        result = 31 * result + lngs.contentHashCode()
        result = 31 * result + color
        result = 31 * result + checksum
        return result
    }
}

class ViolationPoint(
    var localId: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var type: String = "",
    var ts: Long = 0,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var isJammingCandidate: Boolean = false
) {
    private var _cachedGeoPoint: GeoPoint? = null
    fun toGeoPoint(): GeoPoint {
        val cached = _cachedGeoPoint
        if (cached != null && cached.latitude == lat && cached.longitude == lng) return cached
        return GeoPoint(lat, lng).also { _cachedGeoPoint = it }
    }
    fun contentEquals(other: ViolationPoint): Boolean {
        return lat == other.lat && lng == other.lng && type == other.type &&
               ts == other.ts && accuracy == other.accuracy && maxAccuracy == other.maxAccuracy &&
               isJammingCandidate == other.isJammingCandidate
    }
}

data class MapViewState(
    val appMode: String? = null,
    val hydrationLevel: Int = 8,
    val isMapButtonsVisible: Boolean = false,
    val isFenceVisible: Boolean = true,
    val geofenceMode: GeofenceMode = GeofenceMode.IDLE,
    val isViolationsVisible: Boolean = true,
    val isGeofenceViolationsVisible: Boolean = true,
    val maxDistance: Double = 50.0,
    val isMapLocked: Boolean = true,
    val mapFollowMode: MapFollowMode = MapFollowMode.TRACKER,
    val isAnchorLocked: Boolean = false,
    val homePoints: List<GeoPoint> = emptyList(),
    val trackerLat: Double = 0.0,
    val trackerLng: Double = 0.0,
    val trackerSpeed: Double = 0.0,
    val trackerAccuracy: Double = 5.0,
    val trackerMaxAccuracy: Double = 10.0,
    val trackerGpsTs: Long = 0L,
    val trackerTelemetryTs: Long = 0L,
    val trackerLocPending: Boolean = false,
    val trackerLastValidFixRt: Long = 0L,
    val viewerLat: Double = 0.0,
    val viewerLng: Double = 0.0,
    val viewerSpeed: Double = 0.0,
    val viewerAccuracy: Double = 5.0,
    val viewerMaxAcc: Double = 10.0,
    val viewerGpsTs: Long = 0L,
    val viewerTelemetryTs: Long = 0L,
    val viewerLocPending: Boolean = false,
    val viewerLastValidFixRt: Long = 0L,
    val replayCursorPos: GeoPoint? = null,
    val systemPulse: Long = 0L,
    val systemPulseRt: Long = 0L,
    val trackerSegments: List<MapTrailSegment> = emptyList(),
    val viewerSegments: List<MapTrailSegment> = emptyList(),
    val violations: List<ViolationPoint> = emptyList(),
    val showAccuracyBadge: Boolean = true,
    val showSettingsButton: Boolean = true,
    val showToolsOverlay: Boolean = true,
    val isTrackerFresh: Boolean = true,
    val isViewerFresh: Boolean = true,
    val isTrackerValid: Boolean = true,
    val isViewerValid: Boolean = false,
    val smoothedTrackerLat: Double = 0.0,
    val smoothedTrackerLng: Double = 0.0,
    val smoothedViewerLat: Double = 0.0,
    val smoothedViewerLng: Double = 0.0,
    val initialCenter: GeoPoint = GeoPoint(0.0, 0.0)
)
