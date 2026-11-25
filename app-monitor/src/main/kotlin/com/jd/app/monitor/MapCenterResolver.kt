package com.jd.app.monitor

data class MapPosition(val lat: Double, val lng: Double)

/** The three centering modes the Monitor can reach: AUTO at start, TRACKER or VIEWER from the HUD (SIMP-NOV22.5-S1). */
enum class MapFollowMode { TRACKER, VIEWER, AUTO }

/** Trail positions in stored order, without the (0,0) rows sent before the sensor had a fix. */
fun knownPositions(trail: List<TrailEntity>): List<MapPosition> =
    trail.filter { it.lat != 0.0 || it.lng != 0.0 }.map { MapPosition(it.lat, it.lng) }

/** Coarser fixes than this are left out of the trail line (ISSUE-NOV25.22-O153). */
const val TRAIL_MAX_ACCURACY_M = 100f

/** A pause longer than this between two kept fixes ... */
const val TRAIL_GAP_MS = 5 * 60_000L

/** ... breaks the line when the fixes are also farther apart than this, so no straight line crosses an outage. */
const val TRAIL_GAP_DISTANCE_M = 200.0

private fun isJumpRow(row: TrailEntity) = row.status == "JUMP" || row.status == "OUTLIER"

/**
 * The trail line as separate pieces (ISSUE-NOV25.22-O153): without (0,0) rows, JUMP and OUTLIER rows (shown as
 * jump markers instead, O149) and fixes coarser than [TRAIL_MAX_ACCURACY_M]; a new piece starts where a pause longer
 * than [TRAIL_GAP_MS] also moved the tracker more than [TRAIL_GAP_DISTANCE_M]. Works on either time order.
 */
fun trailSegments(trail: List<TrailEntity>): List<List<MapPosition>> {
    val segments = mutableListOf<MutableList<MapPosition>>()
    var previous: TrailEntity? = null
    for (row in trail) {
        if ((row.lat == 0.0 && row.lng == 0.0) || isJumpRow(row) || row.accuracy > TRAIL_MAX_ACCURACY_M) continue
        val last = previous
        val isBreak = last == null || (kotlin.math.abs(row.timestamp - last.timestamp) > TRAIL_GAP_MS &&
            distanceMeters(last.lat, last.lng, row.lat, row.lng) > TRAIL_GAP_DISTANCE_M)
        if (isBreak) segments.add(mutableListOf())
        segments.last().add(MapPosition(row.lat, row.lng))
        previous = row
    }
    return segments
}

/** Trail rows the tracker marked as GPS jumps (JUMP, OUTLIER), shown as jump markers (ISSUE-NOV25.22-O149). */
fun gpsJumps(trail: List<TrailEntity>): List<TrailEntity> =
    trail.filter { isJumpRow(it) && (it.lat != 0.0 || it.lng != 0.0) }

/** Great-circle distance in meters (haversine). */
internal fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } + kotlin.math.cos(Math.toRadians(lat1)) *
        kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLng / 2).let { it * it }
    return 2 * 6_371_000.0 * kotlin.math.asin(kotlin.math.sqrt(a))
}

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
