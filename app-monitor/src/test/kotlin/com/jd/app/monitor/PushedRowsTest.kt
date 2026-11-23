package com.jd.app.monitor

import com.jd.core.network.PushMessage
import com.jd.core.network.SensorSilence
import com.jd.core.network.isPairedTracker
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
 * only the paired tracker's rows are taken. Chapter 181: the relay's silence notices follow the same rules.
 */
class PushedRowsTest {

    private fun row(sequence: Long, tracker: String = "T") =
        LocationUpdate(deviceId = tracker, sequenceId = sequence, timestamp = 5_000L, status = ACOUSTIC_ALERT_STATUS)

    @Test
    fun `a row pushed before the engine started waits for it`() = runBlocking {
        val rows = PushedRows()
        rows.offer(row(1), receivedAtMs = 1_000L)

        val engineStartsAt = 1_000L + PUSHED_ROW_FRESH_MS
        val taken = withTimeout(5.seconds) { rows.fresh { engineStartsAt }.first() }

        assertEquals(1L, taken.sequenceId)
    }

    @Test
    fun `a row that waited too long is dropped`() = runBlocking {
        val rows = PushedRows()
        val engineStartsAt = 1_000L + PUSHED_ROW_FRESH_MS + 1
        rows.offer(row(1), receivedAtMs = 1_000L)
        rows.offer(row(2), receivedAtMs = engineStartsAt)

        val taken = withTimeout(5.seconds) { rows.fresh { engineStartsAt }.first() }

        assertEquals("The stale row must not be raised late", 2L, taken.sequenceId)
    }

    @Test
    fun `only the paired tracker's rows are taken`() = runBlocking {
        // Since SIMP-NOV18.12-S1 the push service checks the pairing once, with isPairedTracker, before PushedRows.
        val rows = PushedRows()
        fun pushed(row: LocationUpdate, trackerId: String?) {
            if (isPairedTracker(PushMessage.Row(row).trackerId, trackerId)) rows.offer(row, 1_000L)
        }

        assertFalse("Another tracker's row", isPairedTracker(PushMessage.Row(row(1, tracker = "X")).trackerId, "T"))
        assertFalse("An unpaired Monitor must stay asleep", isPairedTracker(PushMessage.Row(row(2)).trackerId, null))
        pushed(row(1, tracker = "X"), "T")
        pushed(row(2), null)
        pushed(row(3), "T")

        assertEquals(3L, withTimeout(5.seconds) { rows.fresh { 1_000L }.first() }.sequenceId)
    }

    @Test
    fun `a flood keeps the newest rows`() = runBlocking {
        val rows = PushedRows()
        (1L..20L).forEach { rows.offer(row(it), 1_000L) }

        val taken = withTimeout(5.seconds) { rows.fresh { 1_000L }.take(16).toList() }

        assertEquals((5L..20L).toList(), taken.map { it.sequenceId })
    }

    private fun silence(lastSequence: Long, tracker: String = "T") = SensorSilence(tracker, 5_000L, lastSequence, 35_400L)

    @Test
    fun `a silence notice waits for the engine, but not too long, and only the paired tracker's is taken`() = runBlocking {
        val rows = PushedRows()
        val engineStartsAt = 1_000L + PUSHED_ROW_FRESH_MS + 1

        assertFalse("Another tracker's notice", isPairedTracker(PushMessage.Silence(silence(1, tracker = "X")).trackerId, "T"))
        assertFalse("An unpaired Monitor must stay asleep", isPairedTracker(PushMessage.Silence(silence(2)).trackerId, null))
        assertTrue(isPairedTracker(PushMessage.Silence(silence(3)).trackerId, "T"))
        rows.offerSilence(silence(3), receivedAtMs = 1_000L)
        rows.offerSilence(silence(4), receivedAtMs = engineStartsAt)

        val taken = withTimeout(5.seconds) { rows.freshSilences { engineStartsAt }.first() }

        assertEquals("The stale notice must not be raised late", 4L, taken.lastSequenceId)
    }

    @Test
    fun `the silence notice line tells how long, the last row and the cause`() {
        assertEquals(
            "Relay notice by push: tracker 'T' sent nothing for 35 s (last row seq 9, silent)",
            silenceArrivalLine(silence(9), isHighPriority = true)
        )
        assertEquals(
            "Relay notice by push: tracker 'T' sent nothing for 35 s (last row seq 9, silent) (delivered at normal priority)",
            silenceArrivalLine(silence(9), isHighPriority = false)
        )
        assertEquals( // ISSUE-NOV22.10-O137
            "Relay notice by push: tracker 'T' sent nothing for 35 s (last row seq 9, shutdown)",
            silenceArrivalLine(silence(9).copy(cause = com.jd.core.shared.SilenceCause.SHUTDOWN), isHighPriority = true)
        )
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
