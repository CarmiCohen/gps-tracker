package com.gps19.app

import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * MapStateMapper: Pure mapping logic for Map state transitions and trail rendering.
 * Oct.15.1:
 * - SIMP-OCT15.0-S1: Trail Buffer Optimization. Updated computeTrailSegments to 
 *   accept TrailBuffer, reducing object overhead during rendering cycles.
 * Oct.15.0: Extracted from MainViewModel to resolve SIMP-OCT14.21-S1.
 */
object MapStateMapper {

    data class SmoothedCoordinates(
        val trkLat: Double,
        val trkLng: Double,
        val vwrLat: Double,
        val vwrLng: Double
    )

    fun mapMapViewState(
        mode: String?,
        hydration: Int,
        spatial: SpatialUiState,
        kinematic: KinematicState,
        pulseRt: Long,
        nowMs: Long,
        trkSegs: List<MapTrailSegment>,
        vwrSegs: List<MapTrailSegment>,
        vios: List<ViolationPoint>,
        prevSmoothed: SmoothedCoordinates
    ): Pair<MapViewState, SmoothedCoordinates> {
        val m = mode ?: "tracker"
        val loc = if (m == "tracker") kinematic.localLocation else kinematic.trackerLocation
        val tLat = loc.kinetic.lat
        val tLng = loc.kinetic.lng
        val tTs = loc.kinetic.gpsTs
        val tTel = loc.ts
        val vLat = if (m == "viewer") kinematic.localLocation.kinetic.lat else 0.0
        val vLng = if (m == "viewer") kinematic.localLocation.kinetic.lng else 0.0

        var sTrkLat = prevSmoothed.trkLat
        var sTrkLng = prevSmoothed.trkLng
        var sVwrLat = prevSmoothed.vwrLat
        var sVwrLng = prevSmoothed.vwrLng

        if (PhysicsUtils.isValidLocation(tLat, tLng)) {
            val alpha = if (kinematic.localLocation.kinetic.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            if (sTrkLat == 0.0 || PhysicsUtils.calculateDistance(sTrkLat, sTrkLng, tLat, tLng) > 100.0) {
                sTrkLat = tLat; sTrkLng = tLng
            } else {
                sTrkLat = PhysicsUtils.smoothCoordinate(sTrkLat, tLat, alpha)
                sTrkLng = PhysicsUtils.smoothCoordinate(sTrkLng, tLng, alpha)
            }
        }

        if (PhysicsUtils.isValidLocation(vLat, vLng)) {
            val alpha = if (kinematic.localLocation.kinetic.speed < STATIONARY_SPEED_THRESHOLD_MPS) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
            if (sVwrLat == 0.0 || PhysicsUtils.calculateDistance(sVwrLat, sVwrLng, vLat, vLng) > 100.0) {
                sVwrLat = vLat; sVwrLng = vLng
            } else {
                sVwrLat = PhysicsUtils.smoothCoordinate(sVwrLat, vLat, alpha)
                sVwrLng = PhysicsUtils.smoothCoordinate(sVwrLng, vLng, alpha)
            }
        }

        val fixAgeRt = if (loc.kinetic.rt > 0) pulseRt - loc.kinetic.rt else Long.MAX_VALUE
        val isPowerSave = if (m == "viewer") kinematic.trackerHealth.isPowerSaveMode else kinematic.localHealth.isPowerSaveMode
        val threshold = when {
            kinematic.trackerHealth.isUltraLongStationary -> ULTRA_LONG_STATIONARY_GPS_POLLING_MS + 30000L
            loc.trackerState == TrackerState.PARKING -> STATIONARY_GPS_POLLING_MS + 15000L
            isPowerSave -> SCREEN_OFF_GPS_POLLING_MS + 25000L
            else -> GPS_UI_FAIL_THRESHOLD_MS
        }
        val isTrackerFresh = fixAgeRt < threshold && tTs > 0

        val isTrackerValid = PhysicsUtils.isValidLocation(tLat, tLng)
        val isViewerValid = PhysicsUtils.isValidLocation(vLat, vLng)
        val initialCenter = when {
            isTrackerValid -> GeoPoint(tLat, tLng)
            isViewerValid -> GeoPoint(vLat, vLng)
            else -> GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
        }

        val nextSmoothed = SmoothedCoordinates(sTrkLat, sTrkLng, sVwrLat, sVwrLng)

        val state = MapViewState(
            appMode = m, hydrationLevel = hydration, isMapButtonsVisible = spatial.isMapButtonsVisible, isFenceVisible = spatial.isFenceVisible,
            geofenceMode = spatial.geofenceMode, isViolationsVisible = spatial.isViolationsVisible, isGeofenceViolationsVisible = spatial.isGeofenceViolationsVisible,
            maxDistance = spatial.maxDistance, isMapLocked = spatial.isMapLocked, mapFollowMode = spatial.mapFollowMode,
            isAnchorLocked = loc.integrity.isAnchorLocked, homePoints = spatial.homePoints,
            trackerLat = tLat, trackerLng = tLng, trackerGpsTs = tTs, trackerTelemetryTs = tTel,
            viewerLat = vLat, viewerLng = vLng, systemPulse = nowMs, systemPulseRt = pulseRt,
            trackerSegments = trkSegs, viewerSegments = vwrSegs, violations = vios,
            isTrackerFresh = isTrackerFresh, isTrackerValid = isTrackerValid, isViewerValid = isViewerValid,
            smoothedTrackerLat = sTrkLat, smoothedTrackerLng = sTrkLng,
            smoothedViewerLat = sVwrLat, smoothedViewerLng = sVwrLng,
            initialCenter = initialCenter
        )

        return state to nextSmoothed
    }

    /**
     * computeTrailSegments: Optimized trail segmentation using TrailBuffer (SIMP-OCT15.0-S1).
     */
    fun computeTrailSegments(trailBuffer: TrailBuffer, color: Int, nowMs: Long): List<MapTrailSegment> {
        if (trailBuffer.size == 0) return emptyList()
        val segments = mutableListOf<MapTrailSegment>()
        var currentPoints = mutableListOf<GeoPoint>()
        var currentIsStale: Boolean? = null
        val slateGray = 0xFF64748B.toInt()
        
        for (i in 0 until trailBuffer.size) {
            val ts = trailBuffer.timestamps[i]
            val lat = trailBuffer.lats[i]
            val lng = trailBuffer.lngs[i]
            
            val age = nowMs - ts
            val isStale = age > 35000L
            
            if (currentIsStale != null && isStale != currentIsStale) {
                if (currentPoints.isNotEmpty()) {
                    segments.add(MapTrailSegment(currentPoints.toList(), if (currentIsStale == true) slateGray else color, currentPoints.hashCode()))
                    val last = currentPoints.last()
                    currentPoints = mutableListOf(last)
                }
            }
            currentPoints.add(GeoPoint(lat, lng))
            currentIsStale = isStale
        }
        
        if (currentPoints.isNotEmpty()) {
            segments.add(MapTrailSegment(currentPoints.toList(), if (currentIsStale == true) slateGray else color, currentPoints.hashCode()))
        }
        return segments
    }
}
