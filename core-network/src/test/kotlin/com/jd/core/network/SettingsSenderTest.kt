package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorSetupReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 242 (SIMP-NOV22.3-S1): the ten settings messages leave through one sender, so each one is sent only while
 * connected, as one `settings_update` counted in the data use, a Monitor's stamped with its own ID and a Sensor's
 * carrying the Sensor's ID. The Sensor's shared geofence came with ISSUE-NOV22.7-O134.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSenderTest {

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    /** The ten settings senders, each named by the wire type it must send; the Monitor's come first. */
    private val senders: List<Pair<String, suspend SocketioSignalingClient.() -> Boolean>> = listOf(
        GeofenceWire.TYPE to { sendGeofence("T", GeofenceConfig()) },
        SensorBehaviourWire.TYPE to { sendSensorBehaviour("T", SensorBehaviour()) },
        EventsRequestWire.TYPE to { requestSensorEvents("T", 0L) },
        InfoRequestWire.TYPE to { requestSensorInfo("T", withSatellites = false) },
        GraphRequestWire.TYPE to { requestGraphSamples("T", 0L) },
        SensorSetupWire.TYPE to { sendSensorSetup(SensorSetupReport()) },
        SensorEventsWire.TYPE to { sendSensorEvents(emptyList(), null) },
        SensorInfoWire.TYPE to { sendSensorInfo(SensorInfo()) },
        GraphSamplesWire.TYPE to { sendGraphSamples(emptyList(), compressed = false) },
        GeofenceWire.TYPE to { shareGeofence(GeofenceConfig()) }
    )
    private val monitorSenderCount = 5

    @Test
    fun `nothing is sent or counted before a connection`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            senders.forEach { (type, send) -> assertFalse("$type before connect()", client.send()) }
            client.connect("http://localhost", "D1", "T")
            runCurrent()
            senders.forEach { (type, send) -> assertFalse("$type while disconnected", client.send()) }
            assertTrue(socket.emittedEvents.none { it.first == "settings_update" })
            assertEquals(0L, client.getMonthlyDataUsageBytes())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `each message is one counted settings_update with the right IDs`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            client.connect("http://localhost", "D1", "T")
            socket.simulateConnect()
            runCurrent()
            val bytesBefore = client.getMonthlyDataUsageBytes()

            senders.forEachIndexed { index, (type, send) ->
                val before = socket.emittedEvents.size
                assertTrue(type, client.send())
                val sent = socket.emittedEvents.drop(before)
                assertEquals("$type: one event", listOf("settings_update"), sent.map { it.first })
                val payload = sent.single().second as JSONObject
                assertEquals(type, payload.getString("type"))
                if (index < monitorSenderCount) {
                    assertEquals("$type is stamped with the Monitor's ID", "D1", payload.getString(SettingsWire.FROM))
                    assertEquals("$type is routed to its Sensor", "T", payload.getString("id"))
                } else {
                    assertFalse("$type names no Monitor", payload.has(SettingsWire.FROM))
                    assertEquals("$type carries the Sensor's own ID", "D1", payload.getString("id"))
                }
            }

            val settingsBytes = socket.emittedEvents.filter { it.first == "settings_update" }
                .sumOf { it.second.toString().length.toLong() }
            assertEquals("Every payload is counted once", settingsBytes, client.getMonthlyDataUsageBytes() - bytesBefore)
        } finally {
            client.disconnect()
        }
    }
}
