package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

/**
 * SignalingResilienceTest: Audits telemetry recovery during connection flapping.
 * Part of ISSUE-REVOL30.1-F2.
 * Updated Nov1.2 to support Forensic Signaling Batching (ISSUE-REVOL33.0-F1).
 * Updated: Replaced advanceUntilIdle with runCurrent to prevent infinite loop with timer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignalingResilienceTest {

    // Helper to bypass heavy Socket constructor
    class FakeSocket : Socket(null, null, null) {
        val emittedEvents = mutableListOf<Pair<String, Any>>()
        val listeners = mutableMapOf<String, MutableList<Emitter.Listener>>()
        private var _connected = false

        override fun connect(): Socket {
            return this
        }

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
            if (args.isNotEmpty()) {
                emittedEvents.add(event to args[0])
            } else {
                emittedEvents.add(event to Unit)
            }
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
    fun auditTelemetryLossDuringFlapping() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")
            
            // 1. Initial Disconnected State
            assertFalse("Client should start disconnected", client.isConnected.value)
            
            // 2. Send telemetry while disconnected
            val update1 = LocationUpdate(sequenceId = 1L)
            val sent1 = client.sendTelemetry(update1)
            assertFalse("Should return false when disconnected", sent1)

            // 3. Simulate Connection
            fakeSocket.simulateConnect()
            runCurrent() 
            
            assertTrue("Client should be connected", client.isConnected.value)
            
            // Verify update1 was flushed (using location_batch in Nov1.2)
            val locationBatchEvent = fakeSocket.emittedEvents.find { it.first == "location_batch" }
            assertNotNull("Update should be flushed on connect as batch", locationBatchEvent)
            val batch = locationBatchEvent!!.second as JSONArray
            assertEquals(1L, batch.getJSONObject(0).getLong("seq"))

            // 4. Simulate Disconnect and multiple updates
            fakeSocket.disconnect()
            assertFalse(client.isConnected.value)
            
            for (i in 2..5) {
                client.sendTelemetry(LocationUpdate(sequenceId = i.toLong()))
            }
            
            // 5. Reconnect and verify all 4 updates are flushed
            fakeSocket.emittedEvents.clear()
            fakeSocket.simulateConnect()
            runCurrent()
            
            val flushedSeqs = mutableListOf<Long>()
            fakeSocket.emittedEvents
                .filter { it.first == "location_batch" }
                .forEach { (_, data) ->
                    val arr = data as JSONArray
                    for (i in 0 until arr.length()) {
                        flushedSeqs.add(arr.getJSONObject(i).getLong("seq"))
                    }
                }
            
            assertEquals("All queued updates should be flushed in order", listOf(2L, 3L, 4L, 5L), flushedSeqs)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun testRapidFlappingResilience() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeSocket = FakeSocket()
        val client = SocketioSignalingClient(FakeSocketFactory(fakeSocket), testDispatcher)
        
        try {
            client.connect("http://localhost", "test-device", "secret")

            // Simulate 10 drops and reconnects with updates in between
            for (i in 1..10) {
                fakeSocket.disconnect()
                client.sendTelemetry(LocationUpdate(sequenceId = i.toLong()))
                fakeSocket.simulateConnect()
                runCurrent()
            }

            val flushedSeqs = mutableListOf<Long>()
            fakeSocket.emittedEvents
                .filter { it.first == "location_batch" }
                .forEach { (_, data) ->
                    val arr = data as JSONArray
                    for (i in 0 until arr.length()) {
                        flushedSeqs.add(arr.getJSONObject(i).getLong("seq"))
                    }
                }
            
            assertEquals("Should have all 10 updates despite flapping", (1..10).map { it.toLong() }, flushedSeqs)
        } finally {
            client.disconnect()
        }
    }
}
