package com.jd.app.monitor

import com.jd.core.shared.TrailPoint
import com.jd.core.shared.breaksTrailLine
import com.jd.core.shared.isJump
import com.jd.core.shared.isLinePoint

data class MapPosition(val lat: Double, val lng: Double)

/** The three centering modes the Monitor can reach: AUTO at start, TRACKER or VIEWER from the HUD (SIMP-NOV22.5-S1). */
enum class MapFollowMode { TRACKER, VIEWER, AUTO }

/** Trail positions in stored order, without the (0,0) rows sent before the sensor had a fix. */
fun knownPositions(trail: List<TrailEntity>): List<MapPosition> =
    trail.filter { it.lat != 0.0 || it.lng != 0.0 }.map { MapPosition(it.lat, it.lng) }

/** The row as the shared trail rules see it (ISSUE-NOV27.1-O168). */
fun TrailEntity.toTrailPoint() = TrailPoint(timestamp, lat, lng, accuracy, status)

/**
 * The trail line as separate pieces (ISSUE-NOV25.22-O153): only [isLinePoint] rows, so no (0,0) rows, no JUMP or
 * OUTLIER rows (shown as jump markers instead, O149) and no coarse fixes; a new piece starts where [breaksTrailLine].
 * The thinning of older rows keeps both ends of every break (ISSUE-NOV27.1-O168). Works on either time order.
 */
fun trailSegments(trail: List<TrailEntity>): List<List<MapPosition>> {
    val segments = mutableListOf<MutableList<MapPosition>>()
    var previous: TrailPoint? = null
    for (row in trail) {
        val point = row.toTrailPoint()
        if (!point.isLinePoint()) continue
        val last = previous
        if (last == null || breaksTrailLine(last, point)) segments.add(mutableListOf())
        segments.last().add(MapPosition(row.lat, row.lng))
        previous = point
    }
    return segments
}

/** Trail rows the tracker marked as GPS jumps (JUMP, OUTLIER), shown as jump markers (ISSUE-NOV25.22-O149). */
fun gpsJumps(trail: List<TrailEntity>): List<TrailEntity> =
    trail.filter { it.toTrailPoint().isJump() && (it.lat != 0.0 || it.lng != 0.0) }

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
    MapFollowMode.AUTO ->
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
