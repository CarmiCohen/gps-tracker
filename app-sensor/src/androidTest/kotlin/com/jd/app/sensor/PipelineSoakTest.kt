package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.network.SocketFactory
import com.jd.core.network.SocketioSignalingClient
import com.jd.core.shared.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * PipelineSoakTest: High-stress reliability simulation for Revol31.9 (ISSUE-REVOL31.9-F1).
 * Exercises SentinelLogic, ForensicBuffer, and SocketioSignalingClient simultaneously.
 * Verifies R-ID 772, 773, and 775 under sustained load.
 * Updated Nov1.2 to support Forensic Signaling Batching (ISSUE-REVOL33.0-F1).
 * Updated Nov1.5: Increased timeout to 120s to accommodate batching intervals (200 batches * 500ms).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PipelineSoakTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var forensicBuffer: ForensicBuffer
    private lateinit var signalingClient: SocketioSignalingClient
    private lateinit var fakeSocket: FakeSocket

    // Mock Socket Implementation for Soak Testing
    class FakeSocket : Socket(null, null, null) {
        val emittedCount = AtomicInteger(0)
        val listeners = mutableMapOf<String, MutableList<Emitter.Listener>>()
        private var _connected = false

        override fun connect(): Socket { return this }
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
            when (event) {
                "location_update" -> {
                    emittedCount.incrementAndGet()
                }
                "location_batch" -> {
                    val batch = args[0] as? JSONArray
                    if (batch != null) {
                        emittedCount.addAndGet(batch.length())
                    }
                }
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

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDir = File(context.filesDir, "soak_forensic")
        testDir.deleteRecursively()
        forensicBuffer = ForensicBuffer(testDir, capacity = 5000)
        forensicBuffer.initialize()

        fakeSocket = FakeSocket()
        // Use a real dispatcher here because SocketioSignalingClient uses it for the batch timer
        signalingClient = SocketioSignalingClient(FakeSocketFactory(fakeSocket), Dispatchers.Default)
    }

    /**
     * Verifies Pipeline Integrity under a 2000-event burst with connection flapping.
     * Objective: Zero forensic loss and zero telemetry loss.
     */
    @Test
    fun testIntegratedPipelineSoak_BurstAndFlap() = runBlocking {
        val burstSize = 2000
        val testDeviceId = "SOAK_DEVICE_001"
        
        try {
            // 1. Start Signaling (Disconnected)
            signalingClient.connect("http://soak.relay", testDeviceId, "secret")
            assertFalse(signalingClient.isConnected.value)

            // 2. Generate Burst: Sentinel Logic -> Forensic Buffer -> Signaling Queue
            val lastUpdate = LocationUpdate().apply { lat = 37.0; lng = -122.0; timestamp = System.currentTimeMillis() }
            
            for (i in 0 until burstSize) {
                val update = LocationUpdate().apply {
                    deviceId = testDeviceId
                    lat = 37.0 + (i * 0.00001)
                    lng = -122.0 + (i * 0.00001)
                    timestamp = System.currentTimeMillis() + (i * 100)
                    monotonicNanos = System.nanoTime()
                    sequenceId = i.toLong()
                }

                // A. Sentinel Validation
                val validation = SentinelLogic.validateMovement(lastUpdate, update)
                update.status = validation.status.name

                // B. Forensic Persistence
                forensicBuffer.writeTrace(update, isCritical = (i % 100 == 0))

                // C. Signaling Transmission
                signalingClient.sendTelemetry(update)
                
                // D. Periodic Connection Flapping
                if (i == 500) {
                    fakeSocket.simulateConnect()
                } else if (i == 1000) {
                    fakeSocket.disconnect()
                } else if (i == 1500) {
                    fakeSocket.simulateConnect()
                }
                
                if (i % 100 == 0) yield() // Allow background flushing
            }

            // 3. Final Reconnect and Flush
            fakeSocket.simulateConnect()
            
            // Wait for queue to drain (max 120 seconds due to batching overhead)
            withTimeout(120000) {
                while (fakeSocket.emittedCount.get() < burstSize) {
                    delay(500)
                }
            }

            // 4. Verification
            val stored = forensicBuffer.readTraces()
            assertEquals("Forensic Buffer MUST contain all $burstSize events", burstSize, stored.size)
            assertEquals("Signaling Relay MUST have received all $burstSize events", burstSize, fakeSocket.emittedCount.get())
            
            // Verify sequence integrity
            stored.forEachIndexed { index, update ->
                assertEquals("Sequence mismatch at $index", index.toLong(), update.sequenceId)
            }
            
            SystemLogger.i("SoakTest", "Completed 2000 event soak audit with zero loss.")
        } finally {
            signalingClient.disconnect()
        }
    }
}
