package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ViolationEvent
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

/**
 * SignalingBatchTest: Verifies forensic signaling batching logic.
 * Part of ISSUE-REVOL33.0-F1.
 * Updated: Accounted for "join" event in assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignalingBatchTest {

    class FakeSocket : Socket(null, null, null) {
        val emittedEvents = mutableListOf<Pair<String, Any>>()
        val listeners = mutableMapOf<String, MutableList<Emitter.Listener>>()
        private var _connected = false

        override fun connect(): Socket = this
        override fun disconnect(): Socket {
            _connected = false
            listeners[EVENT_DISCONNECT]?.forEach { it.call() }
            return this
        }
        override fun connected(): Boolean = _connected
        override fun on(event: String, fn: Emitter.Listener): Emitter {
            listeners.getOrPut(event) { mutableListOf() }.add(fn)
            return this
        }
        override fun emit(event: String, vararg args: Any): Emitter {
            if (args.isNotEmpty()) emittedEvents.add(event to args[0])
            else emittedEvents.add(event to Unit)
            return this
        }
        fun simulateConnect() {
            _connected = true
            listeners[EVENT_CONNECT]?.forEach { it.call() }
        }
        fun relayViewerStatus(active: Boolean) {
            val payload = JSONObject().put("id", "secret").put("active", active)
            listeners["viewer_status_relay"]?.forEach { it.call(payload) }
        }
    }

    class FakeSocketFactory(val socket: FakeSocket) : SocketFactory {
        override fun createSocket(url: String, options: IO.Options): Socket = socket
    }

    private fun pooledUpdate(sequence: Long) = LocationUpdate(
        lat = 32.5, lng = 35.25, timestamp = 1_000L + sequence, sequenceId = sequence,
        accuracy = 5f, speed = 2f, batteryLevel = 79, isCharging = true, tempCelsius = 31f
    )

    @Test
    fun testRecycledUpdateIsSentIntactAndRoundTrips() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            repeat(10) {
                val update = pooledUpdate(it.toLong())
                client.sendTelemetry(update)
                update.reset() // What ObjectPool.release does immediately after sending
            }
            runCurrent()

            val batch = fakeSocket.emittedEvents.first { it.first == "location_batch" }.second as JSONArray
            val sent = batch.getJSONObject(9)
            assertEquals(9L, sent.getLong("seq"))
            assertEquals(1_009L, sent.getLong("ts"))
            assertEquals(32.5, sent.getDouble("lat"), 0.0)

            val received = async(start = CoroutineStart.UNDISPATCHED) { client.telemetryFlow.first() }
            fakeSocket.listeners["location_relay"]!!.forEach { it.call(sent) }
            val update = received.await()
            assertEquals(35.25, update.lng, 0.0)
            assertEquals(79, update.batteryLevel)
            assertTrue(update.isCharging)
            assertEquals(31f, update.tempCelsius, 0f)
            assertEquals(5f, update.accuracy, 0f)
            assertEquals(2f, update.speed, 0f)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testRecycledUpdateSurvivesOfflineQueue() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "test-device", "secret")
            runCurrent()

            val update = pooledUpdate(7L)
            client.sendTelemetry(update)
            update.reset()

            fakeSocket.simulateConnect()
            runCurrent()

            val batch = fakeSocket.emittedEvents.first { it.first == "location_batch" }.second as JSONArray
            assertEquals(7L, batch.getJSONObject(0).getLong("seq"))
            assertEquals(32.5, batch.getJSONObject(0).getDouble("lat"), 0.0)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testBackfillResponseIsRoutableAndKeepsItsOwner() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "T", "T")
            fakeSocket.simulateConnect()
            runCurrent()

            val stored = LocationUpdate(lat = 32.5, lng = 35.25, timestamp = 5L, sequenceId = 7L)
            client.sendBackfillResponse(com.jd.core.shared.BackfillResponse("req", listOf(stored), true))

            val payload = fakeSocket.emittedEvents.first { it.first == "backfill_response" }.second as JSONObject
            assertEquals("The relay routes on id", "T", payload.getString("id"))
            assertEquals("T", payload.getJSONArray("updates").getJSONObject(0).getString("id"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testReconnectBurstReachesABusyCollector() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "T", "T")
            fakeSocket.simulateConnect()
            val received = mutableListOf<Long>()
            backgroundScope.launch { client.telemetryFlow.collect { received += it.sequenceId } }
            runCurrent()

            // The socket thread delivers a flushed queue faster than the database stores it.
            val burst = 1L..600L
            burst.forEach { sequence ->
                val payload = JSONObject().put("id", "T").put("seq", sequence)
                fakeSocket.listeners["location_relay"]!!.forEach { it.call(payload) }
            }
            runCurrent()

            assertEquals(burst.toList(), received)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testBackfillBurstReachesABusyCollector() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "T", "T")
            fakeSocket.simulateConnect()
            val received = mutableListOf<String>()
            backgroundScope.launch { client.backfillResponseFlow.collect { received += it.requestId } }
            runCurrent()

            // A full Sensor buffer is 10000 rows, sent as 200 chunks of 50.
            val chunks = (1..200).map { "chunk-$it" }
            chunks.forEach { requestId ->
                val payload = JSONObject().put("reqId", requestId).put("done", false).put("updates", JSONArray())
                fakeSocket.listeners["backfill_response"]!!.forEach { it.call(payload) }
            }
            runCurrent()

            assertEquals(chunks, received)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testRelayUrlChangeReplacesTheSocket() = runTest {
        val sockets = mutableListOf<FakeSocket>()
        val factory = object : SocketFactory {
            override fun createSocket(url: String, options: IO.Options): Socket = FakeSocket().also { sockets += it }
        }
        val client = SocketioSignalingClient(factory, StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://relay-a", "test-device", "secret")
            sockets[0].simulateConnect()
            runCurrent()

            client.connect("http://relay-a", "test-device", "secret")
            assertEquals("The same relay must keep its socket", 1, sockets.size)

            client.connect("http://relay-b", "test-device", "secret")
            runCurrent()
            assertEquals("A new relay must open a new socket", 2, sockets.size)
            assertFalse("The old socket must be closed", sockets[0].connected())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testViewerStatusFollowsRelayPresence() = runTest {
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()
            assertFalse(client.viewerStatusFlow.value)

            fakeSocket.relayViewerStatus(active = true)
            assertTrue("VWR must activate when the relay reports a viewer", client.viewerStatusFlow.value)

            fakeSocket.relayViewerStatus(active = false)
            assertFalse("VWR must clear when the viewer leaves", client.viewerStatusFlow.value)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testBatchingBySize() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            // Send 9 updates (below limit of 10)
            repeat(9) {
                client.sendTelemetry(LocationUpdate(sequenceId = it.toLong()))
            }
            runCurrent()
            
            val batchesBefore = fakeSocket.emittedEvents.filter { it.first == "location_batch" }
            assertTrue("Batch should not be sent before limit", batchesBefore.isEmpty())

            // Send 10th update
            client.sendTelemetry(LocationUpdate(sequenceId = 9L))
            runCurrent()

            val batchesAfter = fakeSocket.emittedEvents.filter { it.first == "location_batch" }
            assertEquals("Batch should be sent after reaching limit", 1, batchesAfter.size)
            val array = batchesAfter[0].second as JSONArray
            assertEquals(10, array.length())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testBatchingByTime() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            // Send 5 updates
            repeat(5) {
                client.sendTelemetry(LocationUpdate(sequenceId = it.toLong()))
            }
            runCurrent()
            
            advanceTimeBy(400)
            runCurrent()
            assertTrue(fakeSocket.emittedEvents.none { it.first == "location_batch" })

            advanceTimeBy(101)
            runCurrent()

            val batches = fakeSocket.emittedEvents.filter { it.first == "location_batch" }
            assertEquals("Batch should be sent after timer expires", 1, batches.size)
            assertEquals(5, (batches[0].second as JSONArray).length())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testCriticalEventFlushesBatch() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            // Send 2 updates
            client.sendTelemetry(LocationUpdate(sequenceId = 1L))
            client.sendTelemetry(LocationUpdate(sequenceId = 2L))
            
            // Send critical update
            client.sendTelemetry(LocationUpdate(sequenceId = 3L, status = "JUMP", isCritical = true))
            runCurrent()

            // Filter for relevant signaling events (ignore "join")
            val sigEvents = fakeSocket.emittedEvents.filter { it.first != "join" }
            assertEquals("Should have 2 signaling events: batch and then critical update", 2, sigEvents.size)
            assertEquals("location_batch", sigEvents[0].first)
            assertEquals("location_batch", sigEvents[1].first)
            
            val batch1 = sigEvents[0].second as JSONArray
            assertEquals(2, batch1.length())
            
            val batch2 = sigEvents[1].second as JSONArray
            assertEquals(1, batch2.length())
            assertEquals("JUMP", batch2.getJSONObject(0).getString("status"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testNoFixStatusIsBatchedNotSentAlone() = runTest {
        // ISSUE-NOV7.19-O43: the Sensor sends NO_FIX on every tick while it has no fix; at 10 Hz it must not bypass batching.
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), StandardTestDispatcher(testScheduler))

        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            repeat(3) { client.sendTelemetry(LocationUpdate(sequenceId = it.toLong(), status = "NO_FIX")) }
            runCurrent()
            assertTrue("NO_FIX must wait for the batch", fakeSocket.emittedEvents.none { it.first == "location_batch" })

            advanceTimeBy(501)
            runCurrent()
            val batches = fakeSocket.emittedEvents.filter { it.first == "location_batch" }
            assertEquals(1, batches.size)
            val array = batches[0].second as JSONArray
            assertEquals(3, array.length())
            assertEquals("NO_FIX", array.getJSONObject(0).getString("status"))
            assertFalse("The flag stays local to the sender", array.getJSONObject(0).has("isCritical"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testViolationFlushesBatch() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")
            fakeSocket.simulateConnect()
            runCurrent()

            client.sendTelemetry(LocationUpdate(sequenceId = 1L))
            
            client.sendViolation(ViolationEvent("SPEED", LocationUpdate(), "nonce"))
            runCurrent()

            val sigEvents = fakeSocket.emittedEvents.filter { it.first != "join" }
            assertEquals(2, sigEvents.size)
            assertEquals("location_batch", sigEvents[0].first)
            assertEquals("violation_event", sigEvents[1].first)
        } finally {
            client.disconnect()
        }
    }
}
