package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 137 (ISSUE-NOV6.4-O33): a failed backfill chunk ends the answer. Moved from the Sensor's
 * `BackfillResponderTest` with the chunking itself (SIMP-NOV7.15-S1).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackfillChunksTest {

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

        val sent = sendBackfillChunks("req", rows(120), sender::send)

        assertTrue(sent)
        assertEquals(listOf(50, 50, 20), sender.attempts.map { it.updates.size })
        assertEquals(listOf(false, false, true), sender.attempts.map { it.isComplete })
        assertEquals((1L..120L).toList(), sender.attempts.flatMap { it.updates }.map { it.sequenceId })
    }

    @Test
    fun failedMiddleChunk_stopsAndNeverSendsComplete() = runTest {
        val sender = FakeSender(failAt = 2)

        val sent = sendBackfillChunks("req", rows(120), sender::send)

        assertFalse(sent)
        assertEquals(2, sender.attempts.size)
        assertTrue(sender.attempts.none { it.isComplete })
    }

    @Test
    fun failedFirstChunk_sendsNothingElse() = runTest {
        val sender = FakeSender(failAt = 1)

        assertFalse(sendBackfillChunks("req", rows(120), sender::send))
        assertEquals(1, sender.attempts.size)
    }

    @Test
    fun failedLastChunk_reportsFailure() = runTest {
        val sender = FakeSender(failAt = 3)

        assertFalse(sendBackfillChunks("req", rows(120), sender::send))
        assertEquals(3, sender.attempts.size)
    }

    @Test
    fun singleChunk_isComplete() = runTest {
        val sender = FakeSender()

        assertTrue(sendBackfillChunks("req", rows(50), sender::send))
        assertEquals(1, sender.attempts.size)
        assertTrue(sender.attempts.single().isComplete)
    }

    @Test
    fun emptyRange_sendsOneCompleteResponse() = runTest {
        val sender = FakeSender()

        assertTrue(sendBackfillChunks("req", emptyList(), sender::send))
        val only = sender.attempts.single()
        assertTrue(only.isComplete)
        assertTrue(only.updates.isEmpty())
        assertEquals("req", only.requestId)
    }

    @Test
    fun emptyRange_failedSendReported() = runTest {
        assertFalse(sendBackfillChunks("req", emptyList(), FakeSender(failAt = 1)::send))
    }

    @Test
    fun client_sendsChunkedMessages_onlyLastDone() = runTest {
        val socket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))
        try {
            client.connect("http://localhost", "T", "T")
            socket.simulateConnect()
            runCurrent()

            assertTrue(client.sendBackfill("req", rows(120)))

            val messages = socket.emittedEvents.filter { it.first == "backfill_response" }.map { it.second as JSONObject }
            assertEquals(listOf(50, 50, 20), messages.map { it.getJSONArray("updates").length() })
            assertEquals(listOf(false, false, true), messages.map { it.getBoolean("done") })
            assertTrue(messages.all { it.getString("reqId") == "req" })
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun client_notConnected_sendsNothing() = runTest {
        val socket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

        assertFalse(client.sendBackfill("req", rows(120)))
        assertTrue(socket.emittedEvents.none { it.first == "backfill_response" })
    }
}
