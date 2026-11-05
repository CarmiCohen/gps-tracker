package com.jd.app.monitor

data class MapPosition(val lat: Double, val lng: Double)

/** Trail positions in stored order, without the (0,0) rows sent before the sensor had a fix. */
fun knownPositions(trail: List<TrailEntity>): List<MapPosition> =
    trail.filter { it.lat != 0.0 || it.lng != 0.0 }.map { MapPosition(it.lat, it.lng) }

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
