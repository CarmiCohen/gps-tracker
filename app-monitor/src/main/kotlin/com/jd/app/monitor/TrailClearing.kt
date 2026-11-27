package com.jd.app.monitor

import java.util.Locale

/** How much of the trails the map's CLEAR keeps (ISSUE-NOV27.1-O167); [spanMs] null keeps nothing. */
enum class TrailKeep(val label: String, val spanMs: Long?) {
    HOUR_1("1 h", HOUR_MS),
    HOURS_6("6 h", 6 * HOUR_MS),
    HOURS_24("24 h", 24 * HOUR_MS),
    DAYS_7("7 d", 7 * 24 * HOUR_MS),
    NOTHING("Nothing, clear everything", null)
}

/** Whose trail the map's CLEAR deletes (ISSUE-NOV27.1-O167): the Sensor's rows, this phone's own, or both. */
enum class TrailScope(val label: String, val sensor: Boolean, val own: Boolean) {
    BOTH("Both trails", sensor = true, own = true),
    SENSOR("The Sensor's trail only", sensor = true, own = false),
    OWN("This phone's trail only", sensor = false, own = true)
}

/** The map CLEAR dialog's choice (ISSUE-NOV27.1-O167); the default keeps the last 24 hours of both trails. */
data class TrailClearChoice(val keep: TrailKeep = TrailKeep.HOURS_24, val scope: TrailScope = TrailScope.BOTH) {

    /** Rows older than this are deleted; everything, a row stamped ahead of this phone's clock too, when nothing is kept. */
    fun deleteBeforeMs(nowMs: Long): Long = keep.spanMs?.let { nowMs - it } ?: Long.MAX_VALUE

    /** The time stored in [TrailClear], so backfill brings no deleted row back: the cutoff, or now when nothing is kept. */
    fun clearAtMs(nowMs: Long): Long = keep.spanMs?.let { nowMs - it } ?: nowMs

    /** The Events line, such as `Trails older than 6 h cleared (12,340 rows)`. */
    fun eventText(deletedRows: Int): String {
        val what = when (scope) {
            TrailScope.BOTH -> "Trails"
            TrailScope.SENSOR -> "Sensor trail"
            TrailScope.OWN -> "This phone's trail"
        }
        val older = if (keep.spanMs == null) "" else " older than ${keep.label}"
        return "$what$older cleared (${rowCount(deletedRows)} rows)"
    }

    companion object {
        /** The plain clear of a statistics reset and a full re-initialization: every row of both trails. */
        val EVERYTHING = TrailClearChoice(TrailKeep.NOTHING, TrailScope.BOTH)
    }
}

/** What a [TrailClearChoice] would delete, shown in the dialog before it is confirmed (ISSUE-NOV27.1-O167). */
data class TrailClearCount(val deleted: Int, val total: Int) {
    val text: String get() = if (total == 0) "No rows stored" else "Deletes ${rowCount(deleted)} of ${rowCount(total)} rows"
}

/**
 * The clear mark after a new clear of [trackerId] at [atMs]: a kept span's cutoff can lie before an earlier clear of the
 * same tracker, whose rows must stay away (ISSUE-NOV27.1-O167), so the later time wins.
 */
fun TrailClear?.raisedTo(trackerId: String, atMs: Long): TrailClear =
    if (this != null && this.trackerId == trackerId && this.atMs > atMs) this else TrailClear(trackerId, atMs)

private fun rowCount(rows: Int): String = String.format(Locale.US, "%,d", rows)

private const val HOUR_MS = 3_600_000L
