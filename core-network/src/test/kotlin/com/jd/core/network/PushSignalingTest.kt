package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 180 (ISSUE-NOV10.8-O86): the client's side of alarms by push. The Sensor marks its alarm rows for the
 * relay, the Monitor keeps its push token registered with the relay, and a pushed row reads back as the same row.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushSignalingTest {

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.sent(event: String) = emittedEvents.filter { it.first == event }.map { it.second }

    private fun FakeSocket.tokens(event: String) = sent(event).map { (it as JSONObject).getString("token") }

    @Test
    fun testOnlyACriticalRowIsMarkedForPush() = runTest {
        val socket = FakeSocket()
        val client = client(socket)

        try {
            client.connect("http://localhost", "T", "T")
            socket.simulateConnect()
            runCurrent()

            client.sendTelemetryBatch(
                listOf(
                    LocationUpdate(sequenceId = 1L),
                    LocationUpdate(sequenceId = 2L, status = "JUMP", isCritical = true)
                )
            )

            val rows = socket.sent("location_batch").single() as JSONArray
            assertFalse("An ordinary row must not be pushed", rows.getJSONObject(0).has("crit"))
            assertTrue("An alarm row must be pushed", rows.getJSONObject(1).getBoolean("crit"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testTokenFollowsEveryJoin() = runTest {
        val socket = FakeSocket()
        val client = client(socket)

        try {
            client.registerPushToken("token-1")
            client.connect("http://localhost", "V", "T")
            runCurrent()
            assertTrue("Nothing can be sent before the relay is reached", socket.emittedEvents.isEmpty())

            socket.simulateConnect()
            runCurrent()
            assertEquals("The relay files the token under the joined room", listOf("join", "push_token"), socket.emittedEvents.map { it.first })
            assertEquals(listOf("token-1"), socket.tokens("push_token"))

            socket.disconnect() // The connection is lost; the relay restarts or forgets.
            socket.simulateConnect()
            runCurrent()
            assertEquals(listOf("join", "push_token", "join", "push_token"), socket.emittedEvents.map { it.first })
            assertEquals(listOf("token-1", "token-1"), socket.tokens("push_token"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testTokenRegisteredWhileConnectedIsSentAtOnce() = runTest {
        val socket = FakeSocket()
        val client = client(socket)

        try {
            client.connect("http://localhost", "V", "T")
            socket.simulateConnect()
            runCurrent()
            assertTrue("No token is known yet", socket.sent("push_token").isEmpty())

            client.registerPushToken("token-1")
            assertEquals(listOf("token-1"), socket.tokens("push_token"))

            client.registerPushToken("token-2") // Firebase replaced the token.
            assertEquals(listOf("token-1", "token-2"), socket.tokens("push_token"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testOnlyLeavingOnPurposeTakesTheTokenBack() = runTest {
        val socket = FakeSocket()
        val client = client(socket)

        client.registerPushToken("token-1")
        client.connect("http://localhost", "V", "T")
        socket.simulateConnect()
        runCurrent()

        socket.disconnect() // A lost connection: the pushes must go on.
        runCurrent()
        assertTrue("A lost connection must keep the pushes coming", socket.sent("push_token_remove").isEmpty())

        socket.simulateConnect()
        runCurrent()
        client.disconnect()
        assertEquals("Leaving on purpose must end the pushes", listOf("token-1"), socket.tokens("push_token_remove"))

        client.disconnect()
        assertEquals("An unconnected client has nothing to take back", 1, socket.sent("push_token_remove").size)
    }

    @Test
    fun testPushedRowReadsBackAsTheSentRow() = runTest {
        val socket = FakeSocket()
        val client = client(socket)

        try {
            client.connect("http://localhost", "T", "T")
            socket.simulateConnect()
            runCurrent()

            val alarm = LocationUpdate(
                lat = 32.5, lng = 35.25, timestamp = 1_700_000_000_123L, sequenceId = 41L, status = "JUMP",
                accuracy = 5f, speed = 2f, batteryLevel = 79, isCharging = true, tempCelsius = 31f, isCritical = true
            )
            client.sendTelemetryBatch(listOf(alarm))

            // The relay pushes the row as the JSON text of what the Sensor sent.
            val pushedText = (socket.sent("location_batch").single() as JSONArray).getJSONObject(0).toString()
            val row = LocationRowWire.decodeText(pushedText)!!

            assertEquals("The Monitor checks the row against its paired tracker", "T", row.deviceId)
            assertEquals("JUMP", row.status)
            assertEquals(41L, row.sequenceId)
            assertEquals(1_700_000_000_123L, row.timestamp)
            assertEquals(32.5, row.lat, 0.0)
            assertEquals(35.25, row.lng, 0.0)
            assertEquals(79, row.batteryLevel)
            assertTrue(row.isCharging)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testUnreadablePushIsRejected() {
        assertNull(LocationRowWire.decodeText(""))
        assertNull(LocationRowWire.decodeText("not a row"))
        assertNull(LocationRowWire.decodeText("[1, 2]"))
    }
}
