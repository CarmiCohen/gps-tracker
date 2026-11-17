package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.GraphField
import com.jd.core.shared.GraphRequest
import com.jd.core.shared.GraphSample
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorGraphBatch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 192 (ISSUE-NOV10.8-O84 part 3): a Monitor's Graphs screen asks the Sensor for its newer samples, and the
 * samples come back, both inside the relay's existing settings_update / settings_relay event, so the relay needs no change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GraphWireTest {

    private val samples = listOf(
        GraphSample.of(1_800_000_060_000L, mapOf(GraphField.VIBRATION_G to 0.25, GraphField.BATTERY_PERCENT to 80)),
        GraphSample.of(1_800_000_120_000L, mapOf(GraphField.TILT_DEG to 3.5, GraphField.LINK_UP_PERCENT to 100))
    )

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.relay(event: String, payload: JSONObject) =
        listeners[event]!!.forEach { it.call(JSONObject(payload.toString())) }

    @Test
    fun `requests and samples survive the round trip and other settings types do not read them`() {
        val request = JSONObject(GraphRequestWire.encode("T", 1_800_000_000_000L).toString())
        assertEquals("The relay routes by id to the Sensor's own room", "T", request.getString("id"))
        assertEquals(GraphRequest(1_800_000_000_000L, compressed = true), GraphRequestWire.decode(request))
        assertNull(GraphSamplesWire.decode(request))

        val batch = JSONObject(GraphSamplesWire.encode("T", samples).toString())
        assertEquals("graph_samples", batch.getString("type"))
        with(GraphSamplesWire.decode(batch)!!) {
            assertEquals("T", trackerId)
            assertEquals(samples, this.samples)
        }
        assertNull(GraphRequestWire.decode(batch))
        assertNull("A Sensor before Nov15.6 ignores the new types", SensorBehaviourWire.decode(request))
        assertFalse(InfoRequestWire.isRequest(request))
        assertNull(GraphSamplesWire.decode(JSONObject(SensorBehaviourWire.encode("T", SensorBehaviour()).toString())))
        assertNull("A malformed batch is ignored", GraphSamplesWire.decode(JSONObject().put("type", "graph_samples")))
    }

    @Test
    fun `the Monitor asks, the Sensor hears after which time, and the Monitor receives the samples`() = runTest {
        val monitorSocket = FakeSocket()
        val monitor = client(monitorSocket)
        val sensorSocket = FakeSocket()
        val sensor = client(sensorSocket)
        try {
            assertFalse("Nothing can be asked before connecting", monitor.requestGraphSamples("T", 0L))
            assertFalse("Nothing can be sent before connecting", sensor.sendGraphSamples(samples, compressed = true))
            monitor.connect("http://localhost", "V", "T")
            monitorSocket.simulateConnect()
            sensor.connect("http://localhost", "T", "T")
            sensorSocket.simulateConnect()
            runCurrent()

            assertTrue(monitor.requestGraphSamples("T", 1_800_000_000_000L))
            val request = monitorSocket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject
            val heard = async(start = CoroutineStart.UNDISPATCHED) { sensor.graphRequestFlow.first() }
            sensorSocket.relay("settings_relay", request)
            assertEquals(GraphRequest(1_800_000_000_000L, compressed = true), heard.await())

            assertTrue(sensor.sendGraphSamples(samples, compressed = true))
            val answer = sensorSocket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject
            val received = async(start = CoroutineStart.UNDISPATCHED) { monitor.graphSamplesFlow.first() }
            monitorSocket.relay("settings_relay", answer)
            with(received.await()) {
                assertEquals("T", trackerId)
                assertEquals(samples, this.samples)
            }
        } finally {
            monitor.disconnect()
            sensor.disconnect()
        }
    }

    // Chapter 202 (ISSUE-NOV15.6-O100 part 1, Nov17.2): the answer is compressed for the Monitors that ask for it.

    @Test
    fun `a compressed answer carries the same samples, and the older forms still work both ways`() {
        val compressed = JSONObject(GraphSamplesWire.encode("T", samples, compressed = true).toString())
        assertFalse("Not also sent plain", compressed.has("samples"))
        assertEquals(SensorGraphBatch("T", samples), GraphSamplesWire.decode(compressed))

        val plain = JSONObject(GraphSamplesWire.encode("T", samples).toString())
        assertFalse(plain.has("gz"))
        assertEquals("A Sensor before Nov17.2 answers plain", SensorGraphBatch("T", samples), GraphSamplesWire.decode(plain))

        val older = JSONObject().put("id", "T").put("type", "graph_request").put("after", 5L)
        assertEquals("A Monitor before Nov17.2 gets the plain form", GraphRequest(5L, compressed = false), GraphRequestWire.decode(older))

        val broken = JSONObject().put("id", "T").put("type", "graph_samples").put("gz", "not gzip")
        assertTrue("A malformed compressed text gives no samples", GraphSamplesWire.decode(broken)!!.samples.isEmpty())
    }
}
