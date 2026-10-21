package com.gps19.app

import com.gps19.core.engine.*
import org.osmdroid.util.GeoPoint

/**
 * MapStateMapper: Pure mapping logic for Map state transitions and trail rendering.
 * Oct.21.01:
 * - Settings Gear Restoration (#OCT20.33-F3): Explicitly enabled showSettingsButton 
 *   and showToolsOverlay for all modes (including Tracker) to allow geofence 
 *   verification, reversing partial R872 purge.
 * Oct.15.3:
 * - SIMP-OCT15.1-S2: Map Rendering Primitive Integration. Transitioned 
 *   computeTrailSegments to use DoubleArrays, avoiding GeoPoint creation in mapper.
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
            initialCenter = initialCenter,
            showSettingsButton = true, // Restore Gear for all roles (ISSUE-OCT20.33-F3)
            showToolsOverlay = true
        )

        return state to nextSmoothed
    }

    /**
     * computeTrailSegments: Optimized trail segmentation using TrailBuffer (SIMP-OCT15.3).
     * Uses primitive arrays to avoid GeoPoint overhead in the mapping logic.
     */
    fun computeTrailSegments(trailBuffer: TrailBuffer, color: Int, nowMs: Long): List<MapTrailSegment> {
        if (trailBuffer.size == 0) return emptyList()
        val segments = mutableListOf<MapTrailSegment>()
        val slateGray = 0xFF64748B.toInt()
        
        var startIdx = 0
        var currentIsStale = (nowMs - trailBuffer.timestamps[0]) > 35000L

        for (i in 1 until trailBuffer.size) {
            val isStale = (nowMs - trailBuffer.timestamps[i]) > 35000L
            if (isStale != currentIsStale) {
                // Segment boundary found.
                val size = i - startIdx
                val lats = DoubleArray(size)
                val lngs = DoubleArray(size)
                System.arraycopy(trailBuffer.lats, startIdx, lats, 0, size)
                System.arraycopy(trailBuffer.lngs, startIdx, lngs, 0, size)
                
                segments.add(MapTrailSegment(
                    lats = lats,
                    lngs = lngs,
                    color = if (currentIsStale) slateGray else color,
                    checksum = lats.contentHashCode() xor lngs.contentHashCode()
                ))
                
                startIdx = i - 1 // Connect to next segment
                currentIsStale = isStale
            }
        }
        
        // Final segment
        val size = trailBuffer.size - startIdx
        if (size > 0) {
            val lats = DoubleArray(size)
            val lngs = DoubleArray(size)
            System.arraycopy(trailBuffer.lats, startIdx, lats, 0, size)
            System.arraycopy(trailBuffer.lngs, startIdx, lngs, 0, size)
            segments.add(MapTrailSegment(
                lats = lats,
                lngs = lngs,
                color = if (currentIsStale) slateGray else color,
                checksum = lats.contentHashCode() xor lngs.contentHashCode()
            ))
        }

        return segments
    }
}
