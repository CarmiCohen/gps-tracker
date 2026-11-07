package com.jd.app.sensor

import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 137 (ISSUE-NOV6.4-O33): a failed backfill chunk ends the answer. */
class BackfillResponderTest {

    private fun rows(count: Int) = (1..count).map { LocationUpdate(deviceId = "S1", sequenceId = it.toLong()) }

    /** Records every attempt; attempt number [failAt] (1-based) and all later ones fail. */
    private class FakeSender(private val failAt: Int = Int.MAX_VALUE) {
        val attempts = mutableListOf<BackfillResponse>()
        suspend fun send(response: BackfillResponse): Boolean {
            attempts += response
            return attempts.size < failAt
        }
    }

    @Test
    fun allChunksSent_onlyLastIsComplete() = runTest {
        val sender = FakeSender()

        val sent = answerBackfill("req", rows(120), sender::send)

        assertTrue(sent)
        assertEquals(listOf(50, 50, 20), sender.attempts.map { it.updates.size })
        assertEquals(listOf(false, false, true), sender.attempts.map { it.isComplete })
        assertEquals((1L..120L).toList(), sender.attempts.flatMap { it.updates }.map { it.sequenceId })
    }

    @Test
    fun failedMiddleChunk_stopsAndNeverSendsComplete() = runTest {
        val sender = FakeSender(failAt = 2)

        val sent = answerBackfill("req", rows(120), sender::send)

        assertFalse(sent)
        assertEquals(2, sender.attempts.size)
        assertTrue(sender.attempts.none { it.isComplete })
    }

    @Test
    fun failedFirstChunk_sendsNothingElse() = runTest {
        val sender = FakeSender(failAt = 1)

        assertFalse(answerBackfill("req", rows(120), sender::send))
        assertEquals(1, sender.attempts.size)
    }

    @Test
    fun failedLastChunk_reportsFailure() = runTest {
        val sender = FakeSender(failAt = 3)

        assertFalse(answerBackfill("req", rows(120), sender::send))
        assertEquals(3, sender.attempts.size)
    }

    @Test
    fun singleChunk_isComplete() = runTest {
        val sender = FakeSender()

        assertTrue(answerBackfill("req", rows(50), sender::send))
        assertEquals(1, sender.attempts.size)
        assertTrue(sender.attempts.single().isComplete)
    }

    @Test
    fun emptyRange_sendsOneCompleteResponse() = runTest {
        val sender = FakeSender()

        assertTrue(answerBackfill("req", emptyList(), sender::send))
        val only = sender.attempts.single()
        assertTrue(only.isComplete)
        assertTrue(only.updates.isEmpty())
        assertEquals("req", only.requestId)
    }

    @Test
    fun emptyRange_failedSendReported() = runTest {
        assertFalse(answerBackfill("req", emptyList(), FakeSender(failAt = 1)::send))
    }
}
