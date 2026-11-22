package com.jd.app.monitor

data class MapPosition(val lat: Double, val lng: Double)

enum class MapFollowMode { TRACKER, VIEWER, AUTO, NONE }

/** Trail positions in stored order, without the (0,0) rows sent before the sensor had a fix. */
fun knownPositions(trail: List<TrailEntity>): List<MapPosition> =
    trail.filter { it.lat != 0.0 || it.lng != 0.0 }.map { MapPosition(it.lat, it.lng) }

/** Trail rows where the tracker reported leaving every Home Point, shown as OUT markers (ISSUE-NOV8.13-O51). */
fun geofenceBreaches(trail: List<TrailEntity>): List<TrailEntity> =
    trail.filter { it.status == com.jd.core.shared.GEOFENCE_BREACH_STATUS && (it.lat != 0.0 || it.lng != 0.0) }

/**
 * Selects the device the map should center on, or null when no real position is known.
 * Explicit follow modes lock to their device. AUTO prefers the sensor while it is connected
 * and the local device otherwise, falling back to whichever position is known.
 */
fun resolveMapCenter(
    followMode: MapFollowMode,
    isPeerActive: Boolean,
    sensorPosition: MapPosition?,
    localPosition: MapPosition?
): MapPosition? = when (followMode) {
    MapFollowMode.TRACKER -> sensorPosition
    MapFollowMode.VIEWER -> localPosition
    MapFollowMode.AUTO, MapFollowMode.NONE ->
        if (isPeerActive) sensorPosition ?: localPosition else localPosition ?: sensorPosition
}

/** One automatic centering of the map: where, and whether to zoom in first. */
data class MapCentering(val center: MapPosition, val zoomIn: Boolean)

/**
 * The automatic centering due now, or null (ISSUE-NOV22.5-O131): the map centers only while a centering is pending
 * (set at each start or return to the foreground) and a position is known, so new positions do not pull a panned
 * map back; only the first centering zooms in, so a later one keeps the user's zoom.
 */
fun pendingCentering(isCenterPending: Boolean, hasZoomed: Boolean, center: MapPosition?): MapCentering? =
    if (isCenterPending && center != null) MapCentering(center, zoomIn = !hasZoomed) else null
