package com.jd.core.shared

/**
 * The periods the Events list and the Graphs can show (user decision 2026-11-15). Named `EventPeriod` until
 * Nov16.5, when the Graphs made it no longer the Events list's own (ISSUE-NOV15.6-O100 part 5).
 */
enum class ViewPeriod(val label: String, val durationMs: Long) {
    HOUR("1 h", 3_600_000L),
    SIX_HOURS("6 h", 6 * 3_600_000L),
    DAY("24 h", 24 * 3_600_000L),
    WEEK("7 d", EventLog.RETENTION_MS)
}
