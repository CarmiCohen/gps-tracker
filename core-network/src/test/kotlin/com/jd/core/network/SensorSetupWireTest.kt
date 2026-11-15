package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
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
 * Chapter 189 (ISSUE-NOV10.8-O83 part 2): the Sensor's setup report travels to the Monitor inside the relay's existing
 * settings_update / settings_relay event, so the relay needs no change, and the client keeps the newest report.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorSetupWireTest {

    private val report = SensorSetupReport(
        missing = listOf(SensorSetupItem.MICROPHONE, SensorSetupItem.BATTERY_UNRESTRICTED),
        faults = listOf(SensorFault.LOCATION_NEEDS_TAP),
        behaviourRevision = 1_794_000_000_000L
    )

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.relaySettings(payload: JSONObject) =
        listeners["settings_relay"]!!.forEach { it.call(JSONObject(payload.toString())) }

    @Test
    fun `the report survives the round trip under the Sensor's ID`() {
        val wire = SensorSetupWire.encode("T", report)
        assertEquals("The relay routes by id to the Sensor's own room", "T", wire.getString("id"))
        assertEquals("sensor_setup", wire.getString("type"))
        assertEquals(report.copy(trackerId = "T"), SensorSetupWire.decode(JSONObject(wire.toString())))
    }

    @Test
    fun `a complete setup is an empty report`() {
        val wire = JSONObject(SensorSetupWire.encode("T", SensorSetupReport(behaviourRevision = 7L)).toString())
        assertEquals(SensorSetupReport(trackerId = "T", behaviourRevision = 7L), SensorSetupWire.decode(wire))
    }

    @Test
    fun `the settings types do not read each other`() {
        val setupWire = JSONObject(SensorSetupWire.encode("T", report).toString())
        assertNull("A Monitor before Nov15.3 ignores the new type", GeofenceWire.decode(setupWire))
        assertNull(SensorBehaviourWire.decode(setupWire))
        assertNull(SensorSetupWire.decode(JSONObject(GeofenceWire.encode("T", GeofenceConfig()).toString())))
        assertNull(SensorSetupWire.decode(JSONObject(SensorBehaviourWire.encode("T", SensorBehaviour()).toString())))
    }

    @Test
    fun `a name from a newer Sensor is skipped and a malformed payload is ignored`() {
        val newer = JSONObject(SensorSetupWire.encode("T", report).toString())
            .put("missing", JSONArray().put("MICROPHONE").put("SOMETHING_NEW"))
            .put("faults", JSONArray().put("ANOTHER_NEW_FAULT"))
        assertEquals(
            SensorSetupReport(trackerId = "T", missing = listOf(SensorSetupItem.MICROPHONE), behaviourRevision = report.behaviourRevision),
            SensorSetupWire.decode(newer)
        )
        assertNull(SensorSetupWire.decode(JSONObject().put("id", "T")))
        assertNull(SensorSetupWire.decode(JSONObject().put("type", "sensor_setup").put("id", "T")))
    }

    @Test
    fun `the Sensor sends its report on the settings event, only while connected`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            assertFalse("Nothing can be sent before connecting", client.sendSensorSetup(report))
            client.connect("http://localhost", "T", "T")
            runCurrent()
            assertFalse("Nothing can be sent before the relay is reached", client.sendSensorSetup(report))

            socket.simulateConnect()
            runCurrent()
            assertTrue(client.sendSensorSetup(report))

            val sent = socket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject
            assertEquals("The client fills in the Sensor's own ID", report.copy(trackerId = "T"), SensorSetupWire.decode(JSONObject(sent.toString())))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `the Monitor keeps the newest report until it leaves`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            client.connect("http://localhost", "V", "T")
            socket.simulateConnect()
            runCurrent()
            assertNull("No report has arrived yet", client.sensorSetupFlow.value)

            socket.relaySettings(SensorSetupWire.encode("T", report))
            assertEquals(report.copy(trackerId = "T"), client.sensorSetupFlow.value)

            socket.relaySettings(GeofenceWire.encode("T", GeofenceConfig()))
            assertEquals("Another settings type leaves the report in place", report.copy(trackerId = "T"), client.sensorSetupFlow.value)

            socket.disconnect() // A lost connection: the last report still describes the Sensor phone.
            assertEquals(report.copy(trackerId = "T"), client.sensorSetupFlow.value)

            socket.simulateConnect()
            runCurrent()
            socket.relaySettings(SensorSetupWire.encode("T", SensorSetupReport(behaviourRevision = report.behaviourRevision)))
            assertEquals("A fixed setup replaces the warnings", emptyList<SensorSetupItem>(), client.sensorSetupFlow.value!!.missing)
        } finally {
            client.disconnect()
        }
        assertNull("Leaving on purpose forgets the report: the next session may watch another Sensor", client.sensorSetupFlow.value)
    }
}
