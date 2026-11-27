package com.jd.core.shared

import kotlin.math.abs

/** One stored trail row as the trail line and its thinning see it (ISSUE-NOV27.1-O168). */
data class TrailPoint(val timestampMs: Long, val lat: Double, val lng: Double, val accuracy: Float, val status: String)

/** Coarser fixes than this are left out of the trail line (ISSUE-NOV25.22-O153). */
const val TRAIL_MAX_ACCURACY_M = 100f

/** A pause longer than this between two line points ... */
const val TRAIL_GAP_MS = 5 * 60_000L

/** ... breaks the line when they are also farther apart than this, so no straight line crosses an outage. */
const val TRAIL_GAP_DISTANCE_M = 200.0

fun TrailPoint.isJump(): Boolean = status == "JUMP" || status == "OUTLIER"

/** Drawn in the trail line: a known position, not a jump, not coarser than [TRAIL_MAX_ACCURACY_M]. */
fun TrailPoint.isLinePoint(): Boolean = (lat != 0.0 || lng != 0.0) && !isJump() && accuracy <= TRAIL_MAX_ACCURACY_M

/** Whether the line breaks between two successive line points (either time order). */
fun breaksTrailLine(a: TrailPoint, b: TrailPoint): Boolean =
    abs(a.timestampMs - b.timestampMs) > TRAIL_GAP_MS &&
        SpatialMath.calculateDistance(a.lat, a.lng, b.lat, b.lng) > TRAIL_GAP_DISTANCE_M

/** Rows older than [olderThanMs] keep one row [spacingM] from the last kept line point or [spacingMs] after the last kept row. */
data class TrailThinTier(val olderThanMs: Long, val spacingM: Double, val spacingMs: Long)

private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 24 * HOUR_MS

/** Every row of the last day is kept. */
const val TRAIL_KEEP_ALL_MS = DAY_MS

/** Rows older than this are deleted; only a trail's newest row stays, so its last position and sequence survive. */
const val TRAIL_MAX_AGE_MS = 31 * DAY_MS

/** Rows this close in time to an alarm row are all kept, so the line around it stays exact. */
const val TRAIL_ALARM_KEEP_MS = 60_000L

/** Oldest last: 1 to 7 days, then 7 to 31 days. */
val TRAIL_THIN_TIERS = listOf(
    TrailThinTier(7 * DAY_MS, spacingM = 50.0, spacingMs = 10 * 60_000L),
    TrailThinTier(DAY_MS, spacingM = 20.0, spacingMs = 60_000L)
)

private val ORDINARY_STATUSES = setOf("VALID", "NO_FIX")

/**
 * Thins one trail by age (ISSUE-NOV27.1-O168) and returns the rows to keep, oldest first. Rows from [thinBeforeMs]
 * on (at most [TRAIL_KEEP_ALL_MS] before [nowMs]) are all kept; older rows keep the spacing of their [TrailThinTier];
 * rows older than [TRAIL_MAX_AGE_MS] are dropped. Always kept: the newest row given, every alarm or jump row (a status
 * other than VALID or NO_FIX) and the rows within [TRAIL_ALARM_KEEP_MS] of one, the first and last row of each status
 * run (NO_FIX to fix) and both ends of every line break, so a drive's start and end and an outage stay as they were
 * and the kept rows draw the same line pieces. Idempotent: thinning the result again with the same times keeps it all.
 */
fun <T> thinTrail(
    rows: List<T>,
    nowMs: Long,
    thinBeforeMs: Long = nowMs - TRAIL_KEEP_ALL_MS,
    point: (T) -> TrailPoint
): List<T> {
    if (rows.isEmpty()) return rows
    val sorted = rows.sortedBy { point(it).timestampMs }
    val points = sorted.map(point)
    val last = points.lastIndex
    val keepAllFromMs = minOf(thinBeforeMs, nowMs - TRAIL_KEEP_ALL_MS)
    val forced = forcedRows(points)

    val kept = BooleanArray(points.size)
    var lastKept: TrailPoint? = null
    var lastLine: TrailPoint? = null
    for ((i, p) in points.withIndex()) {
        val age = nowMs - p.timestampMs
        if (i != last && age > TRAIL_MAX_AGE_MS) continue
        val isLine = p.isLinePoint()
        val tier = if (p.timestampMs >= keepAllFromMs) null else TRAIL_THIN_TIERS.firstOrNull { age > it.olderThanMs }
        val keep = i == last || forced[i] || tier == null || lastKept == null || (isLine && lastLine == null) ||
            p.timestampMs - lastKept.timestampMs >= tier.spacingMs ||
            (isLine && SpatialMath.calculateDistance(lastLine!!.lat, lastLine.lng, p.lat, p.lng) >= tier.spacingM)
        if (!keep) continue
        kept[i] = true
        lastKept = p
        if (isLine) lastLine = p
    }
    return sorted.filterIndexed { i, _ -> kept[i] }
}

/** The rows [thinTrail] keeps whatever their spacing; each rule still holds among the kept rows, which keeps it idempotent. */
private fun forcedRows(points: List<TrailPoint>): BooleanArray {
    val forced = BooleanArray(points.size)
    val alarmTimes = mutableListOf<Long>()
    var previousLine = -1
    for ((i, p) in points.withIndex()) {
        if (p.status !in ORDINARY_STATUSES) {
            forced[i] = true
            alarmTimes += p.timestampMs
        }
        if (i > 0 && points[i - 1].status != p.status) {
            forced[i - 1] = true
            forced[i] = true
        }
        if (p.isLinePoint()) {
            if (previousLine >= 0 && breaksTrailLine(points[previousLine], p)) {
                forced[previousLine] = true
                forced[i] = true
            }
            previousLine = i
        }
    }
    if (alarmTimes.isNotEmpty()) {
        var a = 0
        for ((i, p) in points.withIndex()) {
            while (a < alarmTimes.lastIndex && alarmTimes[a + 1] <= p.timestampMs) a++
            val near = abs(p.timestampMs - alarmTimes[a]) <= TRAIL_ALARM_KEEP_MS ||
                (a < alarmTimes.lastIndex && abs(alarmTimes[a + 1] - p.timestampMs) <= TRAIL_ALARM_KEEP_MS)
            if (near) forced[i] = true
        }
    }
    return forced
}
