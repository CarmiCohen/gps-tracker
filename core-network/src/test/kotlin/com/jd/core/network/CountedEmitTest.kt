package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 245 (SIMP-NOV22.9-S1): every data event leaves through one counted sender, so the data use is exactly the
 * sum of the data payloads sent; the join and push-token control messages are not counted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CountedEmitTest {

    private val dataEvents = setOf(
        "ping_cmd", "location_batch", "backfill_request", "backfill_response", "settings_update"
    )

    @Test
    fun `the data use is the sum of every data payload and nothing else`() = runTest {
        val socket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))
        try {
            client.registerPushToken("token")
            client.connect("http://localhost", "D1", "T")
            socket.simulateConnect()
            runCurrent()

            val row = LocationUpdate(lat = 32.5, lng = 35.25, timestamp = 5L, sequenceId = 7L)
            assertTrue(client.sendTelemetryBatch(listOf(row)))
            assertTrue(client.requestBackfill(BackfillRequest("T", 1L, 7L, "req")))
            assertTrue(client.sendBackfill("req", listOf(row)))
            assertTrue(client.sendGeofence("T", GeofenceConfig()))
            client.registerPushToken("token2")
            runCurrent()

            val sentEvents = socket.emittedEvents.map { it.first }.toSet()
            assertTrue("Every data event was sent: $sentEvents", sentEvents.containsAll(dataEvents))
            assertTrue("The control messages were sent too", sentEvents.containsAll(setOf("join", "push_token")))

            val dataBytes = socket.emittedEvents.filter { it.first in dataEvents }
                .sumOf { it.second.toString().length.toLong() }
            assertEquals("Each data payload counted once, no control message", dataBytes, client.getMonthlyDataUsageBytes())
        } finally {
            client.disconnect()
        }
    }
}
