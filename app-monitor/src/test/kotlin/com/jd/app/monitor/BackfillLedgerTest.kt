package com.jd.app.monitor

import com.jd.core.shared.BackfillRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies Chapter 125: no missing range is forgotten (ISSUE-NOV6.3-O32, ISSUE-NOV6.2-O31). */
class BackfillLedgerTest {

    private val ledger = BackfillLedger(maxActive = 10, timeoutMs = 30_000L)

    private fun gaps(count: Int) = (count downTo 1).map { SequenceGap(it * 10L, it * 10L + 1) }

    /** Requests whatever the ledger allows, and returns the requests. */
    private fun send(gaps: List<SequenceGap>, nowMs: Long = 0L, deviceId: String = "T") =
        ledger.gapsToRequest(deviceId, gaps, nowMs).map { gap ->
            BackfillRequest(deviceId, gap.firstMissing, gap.lastMissing, "req-${gap.firstMissing}")
                .also { ledger.onRequestSent(it, nowMs) }
        }

    @Test
    fun gapsOverTheLimitWaitAndAreRequestedWhenASlotFrees() {
        val all = gaps(13) // The field test left 13 holes
        val first = send(all)
        assertEquals("The limit still applies", 10, first.size)
        assertEquals("Newest gaps go first: the Sensor overwrites its oldest rows", 130L, first[0].startSequenceId)

        first.forEach { ledger.onRequestComplete(it.requestId) }

        assertEquals(listOf(30L, 20L, 10L), send(all).map { it.startSequenceId })
    }

    @Test
    fun aRequestInFlightIsNotRepeated() {
        val all = gaps(1)
        send(all)

        assertEquals(emptyList<SequenceGap>(), ledger.gapsToRequest("T", all, 29_999L))
    }

    @Test
    fun anUnansweredRequestIsReleasedAndAskedAgain() {
        val all = gaps(10)
        send(all)

        assertEquals("Held until the timeout", 0, ledger.gapsToRequest("T", all, 29_999L).size)
        assertEquals("Released at the timeout", 10, ledger.gapsToRequest("T", all, 30_000L).size)
    }

    @Test
    fun eachChunkRestartsTheTimeout() {
        val all = gaps(1)
        val request = send(all).single()

        ledger.onChunkReceived(request.requestId, 29_000L)

        assertEquals(0, ledger.gapsToRequest("T", all, 58_999L).size)
        assertEquals(1, ledger.gapsToRequest("T", all, 59_000L).size)
    }

    @Test
    fun anAnsweredHoleIsNotAskedForever() {
        val all = gaps(1)
        val request = send(all).single()
        ledger.onRequestComplete(request.requestId)

        assertEquals(emptyList<SequenceGap>(), ledger.gapsToRequest("T", all, 1_000_000L))
    }

    @Test
    fun whatRemainsOfAPartlyFilledRangeCountsAsAnswered() {
        val request = send(listOf(SequenceGap(1, 100))).single()
        ledger.onRequestComplete(request.requestId)

        assertEquals(emptyList<SequenceGap>(), ledger.gapsToRequest("T", listOf(SequenceGap(1, 40)), 0L))
    }

    @Test
    fun aNewHoleOutsideAnAnsweredRangeIsRequested() {
        val request = send(listOf(SequenceGap(1, 100))).single()
        ledger.onRequestComplete(request.requestId)

        val remaining = listOf(SequenceGap(200, 210), SequenceGap(1, 40))

        assertEquals(listOf(SequenceGap(200, 210)), ledger.gapsToRequest("T", remaining, 0L))
    }

    @Test
    fun aLateCompletionOfAReleasedRequestIsIgnored() {
        val all = gaps(1)
        val request = send(all).single()
        ledger.gapsToRequest("T", all, 30_000L)

        assertNull(ledger.onRequestComplete(request.requestId))
        assertEquals("Not counted as answered", 1, ledger.gapsToRequest("T", all, 30_000L).size)
    }

    @Test
    fun answeredRangesAreForgottenOnceTheirHoleIsGone() {
        val all = gaps(1)
        val request = send(all).single()
        ledger.onRequestComplete(request.requestId)

        ledger.gapsToRequest("T", emptyList(), 0L)

        assertEquals("The record is gone, so the same range counts as new", all, ledger.gapsToRequest("T", all, 0L))
    }

    @Test
    fun anotherTrackerIsNotCoveredByTheseRanges() {
        val all = gaps(1)
        val request = send(all, deviceId = "T").single()
        ledger.onRequestComplete(request.requestId)

        assertEquals(all, ledger.gapsToRequest("U", all, 0L))
    }
}
