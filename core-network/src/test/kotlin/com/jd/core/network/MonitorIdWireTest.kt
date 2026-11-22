package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Chapter 240 (ISSUE-NOV22.7-O135): each message a Monitor sends its Sensor carries the Monitor's own ID as `from`, inside
 * the relay's existing settings event, so the relay needs no change; the Sensor keeps the ID it heard last.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorIdWireTest {

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.relay(payload: JSONObject) =
        listeners["settings_relay"]!!.forEach { it.call(JSONObject(payload.toString())) }

    private fun FakeSocket.lastSettings(): JSONObject = emittedEvents.last { it.first == "settings_update" }.second as JSONObject

    private fun idOf(payload: JSONObject): String? {
        val data = JSONObject(payload.toString())
        return SettingsWire.monitorIdOf(data, SettingsWire.decode(data))
    }

    @Test
    fun `every Monitor message names its Monitor, a Sensor message and an older Monitor name none`() {
        val monitorMessages = listOf(
            GeofenceWire.encode("T", GeofenceConfig()),
            SensorBehaviourWire.encode("T", SensorBehaviour()),
            EventsRequestWire.encode("T", 0L),
            InfoRequestWire.encode("T", withSatellites = false),
            GraphRequestWire.encode("T", 0L)
        )
        monitorMessages.forEach { message ->
            assertEquals(message.getString("type"), "M1", idOf(SettingsWire.fromMonitor(JSONObject(message.toString()), "M1")))
            assertNull("A Monitor before Nov22.7 sends no ID", idOf(message))
        }
        val stamped = SettingsWire.fromMonitor(SensorBehaviourWire.encode("T", SensorBehaviour()), "M1")
        assertEquals("The relay still routes by the Sensor's ID", "T", stamped.getString("id"))
        assertEquals("The Sensor still reads the settings", SensorBehaviour(), SensorBehaviourWire.decode(JSONObject(stamped.toString())))

        val sensorInfo = SensorInfoWire.encode("T", SensorInfo(sensorId = "T")).put(SettingsWire.FROM, "X")
        assertNull("A Sensor's message names no Monitor", idOf(sensorInfo))
        assertNull("A blank ID is none", idOf(SettingsWire.fromMonitor(InfoRequestWire.encode("T", true), " ")))
        assertFalse("Not connected: no ID to add", SettingsWire.fromMonitor(GraphRequestWire.encode("T", 0L), null).has(SettingsWire.FROM))
    }

    @Test
    fun `the Sensor hears the Monitor's ID, keeps it, and ignores its own echoes`() = runTest {
        val monitorSocket = FakeSocket()
        val monitor = client(monitorSocket)
        val sensorSocket = FakeSocket()
        val sensor = client(sensorSocket)
        try {
            monitor.connect("http://localhost", "M1", "T")
            monitorSocket.simulateConnect()
            sensor.connect("http://localhost", "T", "T")
            sensorSocket.simulateConnect()
            runCurrent()
            assertNull("None heard yet", sensor.monitorIdFlow.value)

            assertTrue(monitor.sendSensorBehaviour("T", SensorBehaviour()))
            val behaviour = monitorSocket.lastSettings()
            sensorSocket.relay(behaviour)
            assertEquals("M1", sensor.monitorIdFlow.value)
            monitorSocket.relay(behaviour)
            assertNull("A Monitor's own echo is not another Monitor", monitor.monitorIdFlow.value)

            assertTrue(sensor.sendSensorInfo(SensorInfo(sensorId = "T")))
            sensorSocket.relay(sensorSocket.lastSettings())
            assertEquals("The Sensor's own echo changes nothing", "M1", sensor.monitorIdFlow.value)

            sensorSocket.relay(InfoRequestWire.encode("T", withSatellites = false))
            assertEquals("A Monitor before Nov22.7 changes nothing", "M1", sensor.monitorIdFlow.value)

            assertTrue(monitor.requestSensorInfo("T", withSatellites = false))
            assertEquals("M1", monitorSocket.lastSettings().getString(SettingsWire.FROM))
            sensorSocket.relay(SettingsWire.fromMonitor(GeofenceWire.encode("T", GeofenceConfig()), "M2"))
            assertEquals("The Monitor heard last", "M2", sensor.monitorIdFlow.value)

            sensor.disconnect()
            assertEquals("Kept across reconnections", "M2", sensor.monitorIdFlow.value)
        } finally {
            monitor.disconnect()
            sensor.disconnect()
        }
    }
}
