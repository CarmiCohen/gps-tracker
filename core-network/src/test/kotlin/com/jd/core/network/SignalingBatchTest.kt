package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ViolationEvent
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    class FakeSocketFactory(val socket: FakeSocket) : SocketFactory {
        override fun createSocket(url: String, options: IO.Options): Socket = socket
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
            
            // Send critical status update
            client.sendTelemetry(LocationUpdate(sequenceId = 3L, status = "JUMP"))
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
