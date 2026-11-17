package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.shared.SensorEventBatch
import com.jd.core.shared.SensorSetupReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 190 (ISSUE-NOV10.8-O84 Nov15.4): the Sensor's newest events travel to the Monitor inside the relay's existing
 * settings_update / settings_relay event, so the relay needs no change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorEventsWireTest {

    private val events = listOf(
        EventEntry("S1", 1_800_000_000_000L, EventSource.SENSOR, EventLevel.ALARM, "Tilt alert: 40 degrees"),
        EventEntry("S2", 1_800_000_001_000L, EventSource.SENSOR, EventLevel.INFO, "Relay connected")
    )

    /** As the Monitor keeps them: marked with the Sensor they came from (ISSUE-NOV15.6-O100 part 7, Nov17.2). */
    private val received = events.map { it.copy(trackerId = "T") }

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    @Test
    fun `the events survive the round trip under the Sensor's ID`() {
        val wire = SensorEventsWire.encode("T", events)
        assertEquals("The relay routes by id to the Sensor's own room", "T", wire.getString("id"))
        assertEquals("sensor_events", wire.getString("type"))
        assertEquals(SensorEventBatch("T", received), SensorEventsWire.decode(JSONObject(wire.toString())))
    }

    @Test
    fun `the time the events are complete after, and the request for missed events, survive the round trip (O98 part 4)`() {
        val wire = JSONObject(SensorEventsWire.encode("T", events, completeAfterMs = 1_799_999_999_000L).toString())
        assertEquals(SensorEventBatch("T", received, 1_799_999_999_000L), SensorEventsWire.decode(wire))
        assertEquals("0 means complete from the start", 0L, SensorEventsWire.decode(JSONObject(SensorEventsWire.encode("T", events, 0L).toString()))?.completeAfterMs)

        val request = JSONObject(EventsRequestWire.encode("T", 1_800_000_000_000L).toString())
        assertEquals("T", request.getString("id"))
        assertEquals(1_800_000_000_000L, EventsRequestWire.decode(request))
        assertNull("A request is not events", SensorEventsWire.decode(request))
        assertNull("Events are not a request", EventsRequestWire.decode(wire))
        assertNull(GraphRequestWire.decode(request))
    }

    @Test
    fun `other settings types are not read as events, nor events as a setup report`() {
        val eventsWire = JSONObject(SensorEventsWire.encode("T", events).toString())
        assertNull(SensorSetupWire.decode(eventsWire))
        assertNull(GeofenceWire.decode(eventsWire))
        assertNull(SensorBehaviourWire.decode(eventsWire))
        assertNull(SensorEventsWire.decode(JSONObject(SensorSetupWire.encode("T", SensorSetupReport()).toString())))
    }

    @Test
    fun `a level from a newer Sensor is shown as INFO and a malformed payload is ignored`() {
        val newer = JSONObject(SensorEventsWire.encode("T", events.take(1)).toString())
        newer.getJSONArray("events").getJSONObject(0).put("l", "SOMETHING_NEW")
        assertEquals(EventLevel.INFO, SensorEventsWire.decode(newer)!!.events.single().level)
        assertNull(SensorEventsWire.decode(JSONObject().put("type", "sensor_events").put("id", "T")))
    }

    @Test
    fun `the Sensor sends only while connected and the Monitor receives each batch`() = runTest {
        val sensorSocket = FakeSocket()
        val sensor = client(sensorSocket)
        val monitorSocket = FakeSocket()
        val monitor = client(monitorSocket)
        val batches = mutableListOf<SensorEventBatch>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { monitor.sensorEventsFlow.collect { batches.add(it) } }
        try {
            assertFalse("Nothing can be sent before connecting", sensor.sendSensorEvents(events))
            sensor.connect("http://localhost", "T", "T")
            sensorSocket.simulateConnect()
            runCurrent()
            assertTrue(sensor.sendSensorEvents(events))
            val sent = sensorSocket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject

            monitor.connect("http://localhost", "V", "T")
            monitorSocket.simulateConnect()
            runCurrent()
            monitorSocket.listeners["settings_relay"]!!.forEach { it.call(JSONObject(sent.toString())) }
            assertEquals(listOf(SensorEventBatch("T", received)), batches)
        } finally {
            collecting.cancel()
            sensor.disconnect()
            monitor.disconnect()
        }
    }
}
