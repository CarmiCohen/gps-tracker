package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
 * Chapter 253 (ISSUE-NOV22.7-O134): the Sensor sends the set it enforces back to its room, inside the relay's existing
 * settings event, so the relay needs no change; a Monitor reads it, and another Monitor's edit, on the same flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedGeofenceTest {

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.relay(payload: JSONObject) =
        listeners["settings_relay"]!!.forEach { it.call(JSONObject(payload.toString())) }

    private fun FakeSocket.lastSettings(): JSONObject = emittedEvents.last { it.first == "settings_update" }.second as JSONObject

    private val shared = GeofenceConfig(listOf(HomePoint(32.1, 34.8), HomePoint(32.2, 34.9)), radiusMeters = 200.0, revision = 9L)

    @Test
    fun `a Monitor receives the Sensor's shared set and another Monitor's edit`() = runTest {
        val sensorSocket = FakeSocket()
        val sensor = client(sensorSocket)
        val monitorSocket = FakeSocket()
        val monitor = client(monitorSocket)
        val otherSocket = FakeSocket()
        val other = client(otherSocket)
        val received = mutableListOf<GeofenceConfig>()
        try {
            sensor.connect("http://localhost", "T", "T")
            sensorSocket.simulateConnect()
            monitor.connect("http://localhost", "M1", "T")
            monitorSocket.simulateConnect()
            other.connect("http://localhost", "M2", "T")
            otherSocket.simulateConnect()
            backgroundScope.launch { monitor.geofenceFlow.toList(received) }
            runCurrent()

            assertTrue(sensor.shareGeofence(shared))
            val fromSensor = sensorSocket.lastSettings()
            assertEquals("Routed to the Sensor's own room", "T", fromSensor.getString("id"))
            assertFalse("A Sensor's message names no Monitor", fromSensor.has(SettingsWire.FROM))
            monitorSocket.relay(fromSensor)
            runCurrent()
            assertEquals(listOf(shared), received)
            assertNull("The Sensor is not taken for a Monitor", monitor.monitorIdFlow.value)

            val edit = shared.withRadius(300.0).copy(revision = 10L)
            assertTrue(other.sendGeofence("T", edit))
            monitorSocket.relay(otherSocket.lastSettings())
            runCurrent()
            assertEquals("Another Monitor's edit arrives too", listOf(shared, edit), received)
        } finally {
            sensor.disconnect()
            monitor.disconnect()
            other.disconnect()
        }
    }
}
