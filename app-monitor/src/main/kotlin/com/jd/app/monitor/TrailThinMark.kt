package com.jd.app.monitor

/**
 * The Sensor [trackerId]'s rows before [upToMs] may have been thinned (ISSUE-NOV27.1-O168), so a hole with both sides
 * older than it is no gap to request; late backfill rows older than it are kept and thinned on the next run.
 */
data class TrailThinned(val trackerId: String, val upToMs: Long)

/** The thinned-up-to time of [trackerId], or null when its trail was never thinned. */
fun TrailThinned?.upToMsOf(trackerId: String): Long? = this?.takeIf { it.trackerId == trackerId }?.upToMs

/** The mark of [trackerId] moved on to [upToMs], never back; another tracker's mark is replaced. */
fun TrailThinned?.thinnedTo(trackerId: String, upToMs: Long): TrailThinned =
    if (this != null && this.trackerId == trackerId && this.upToMs > upToMs) this else TrailThinned(trackerId, upToMs)

/** A live row this recent means the Monitor has contact with the Sensor. */
const val THIN_CONTACT_MS = 60_000L

/**
 * Whether the mark may move on: only while the Sensor's rows arrive live ([lastLiveMs]), no backfill for it is active
 * and the last gap scan found no row it may still send ([openRows], null before the first scan). So a real hole from
 * an outage longer than a day is still asked for before thinning reaches it.
 */
fun mayAdvanceThinning(lastLiveMs: Long?, nowMs: Long, isBackfilling: Boolean, openRows: Long?): Boolean =
    lastLiveMs != null && nowMs - lastLiveMs <= THIN_CONTACT_MS && !isBackfilling && openRows == 0L
