package com.jd.app.monitor

import com.jd.core.shared.ACOUSTIC_ALERT_STATUS
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 180 (ISSUE-NOV10.8-O86): alarm rows that arrive by push wait for the alarm engine, but not for long, and
 * only the paired tracker's rows are taken.
 */
class PushedRowsTest {

    private fun row(sequence: Long, tracker: String = "T") =
        LocationUpdate(deviceId = tracker, sequenceId = sequence, timestamp = 5_000L, status = ACOUSTIC_ALERT_STATUS)

    @Test
    fun `a row pushed before the engine started waits for it`() = runBlocking {
        val rows = PushedRows()
        assertTrue(rows.offer(row(1), "T", receivedAtMs = 1_000L))

        val engineStartsAt = 1_000L + PUSHED_ROW_FRESH_MS
        val taken = withTimeout(5.seconds) { rows.fresh { engineStartsAt }.first() }

        assertEquals(1L, taken.sequenceId)
    }

    @Test
    fun `a row that waited too long is dropped`() = runBlocking {
        val rows = PushedRows()
        val engineStartsAt = 1_000L + PUSHED_ROW_FRESH_MS + 1
        rows.offer(row(1), "T", receivedAtMs = 1_000L)
        rows.offer(row(2), "T", receivedAtMs = engineStartsAt)

        val taken = withTimeout(5.seconds) { rows.fresh { engineStartsAt }.first() }

        assertEquals("The stale row must not be raised late", 2L, taken.sequenceId)
    }

    @Test
    fun `only the paired tracker's rows are taken`() = runBlocking {
        val rows = PushedRows()

        assertFalse("Another tracker's row", rows.offer(row(1, tracker = "X"), "T", 1_000L))
        assertFalse("An unpaired Monitor must stay asleep", rows.offer(row(2), null, 1_000L))
        assertTrue(rows.offer(row(3), "T", 1_000L))

        assertEquals(3L, withTimeout(5.seconds) { rows.fresh { 1_000L }.first() }.sequenceId)
    }

    @Test
    fun `a flood keeps the newest rows`() = runBlocking {
        val rows = PushedRows()
        (1L..20L).forEach { rows.offer(row(it), "T", 1_000L) }

        val taken = withTimeout(5.seconds) { rows.fresh { 1_000L }.take(16).toList() }

        assertEquals((5L..20L).toList(), taken.map { it.sequenceId })
    }

    @Test
    fun `the arrival line tells the lag and a lowered priority`() {
        val alarm = row(7)

        assertEquals(
            "Alert row $ACOUSTIC_ALERT_STATUS seq 7 arrived by push 350 ms after it was sent",
            pushArrivalLine(alarm, nowMs = 5_350L, isHighPriority = true)
        )
        assertEquals(
            "Alert row $ACOUSTIC_ALERT_STATUS seq 7 arrived by push 350 ms after it was sent (delivered at normal priority)",
            pushArrivalLine(alarm, nowMs = 5_350L, isHighPriority = false)
        )
    }
}
