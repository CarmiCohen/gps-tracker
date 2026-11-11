package com.jd.core.network

/**
 * Measures how late live rows arrive from the relay: the receiver's clock minus the row's `ts`, which the Sensor
 * stamps with its own clock when it sends the row (ISSUE-NOV10.7-O73). The two phones' clocks differ by their
 * network time error, usually well under a second.
 *
 * [record] returns a log line for every alert row at once, and a summary of ordinary rows once per [windowMs].
 */
class ArrivalLagMeter(private val windowMs: Long = WINDOW_MS) {

    private var windowStartMs: Long? = null
    private var rows = 0
    private var maxLagMs = Long.MIN_VALUE
    private var lagSumMs = 0L

    fun record(status: String, sequenceId: Long, sentAtMs: Long, nowMs: Long): String? {
        val lagMs = nowMs - sentAtMs
        if (status !in ORDINARY_STATUSES) return "Alert row $status seq $sequenceId arrived ${lagMs} ms after it was sent"

        val start = windowStartMs ?: nowMs.also { windowStartMs = it }
        rows++
        lagSumMs += lagMs
        if (lagMs > maxLagMs) maxLagMs = lagMs
        if (nowMs - start < windowMs) return null

        val line = "Arrival lag over ${(nowMs - start) / 1000} s: $rows rows, mean ${lagSumMs / rows} ms, max $maxLagMs ms"
        windowStartMs = nowMs
        rows = 0
        lagSumMs = 0L
        maxLagMs = Long.MIN_VALUE
        return line
    }

    private companion object {
        const val WINDOW_MS = 10_000L
        val ORDINARY_STATUSES = setOf("VALID", "NO_FIX")
    }
}
