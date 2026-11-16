package com.jd.core.network

import com.jd.core.network.SignalingBatchTest.FakeSocket
import com.jd.core.network.SignalingBatchTest.FakeSocketFactory
import com.jd.core.shared.LATENCY_PING_INTERVAL_MS
import com.jd.core.shared.LinkStats
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.PhoneState
import com.jd.core.shared.SatelliteReading
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorSetupReport
import com.jd.core.shared.satelliteSummary
import com.jd.core.shared.withoutSatelliteList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 191 (ISSUE-NOV10.8-O84 part 2): the detailed Info crosses the relay inside its existing settings_update /
 * settings_relay event, a Monitor asks for it the same way, the latency comes from the relay's ping echo, and the new
 * row fields cross the wire, so the relay needs no change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorInfoWireTest {

    private val info = SensorInfo(
        capturedAtMs = 1_794_000_000_000L,
        sensorId = "T",
        version = "Nov15.5",
        serviceStartedAtMs = 1_793_999_000_000L,
        motion = "Armed (accelerometer)",
        isMotionArmed = true,
        microphone = "Listening",
        link = LinkStats(isConnected = true, changedAtMs = 1_793_999_500_000L, drops = 2, longestDropMs = 9_000L, totalDropMs = 12_000L, latencyMs = 180L),
        isMonitorOnline = true,
        fixTimeMs = 1_793_999_998_000L,
        lat = 32.1,
        lng = 34.8,
        accuracyM = 6f,
        homeDistanceM = 40.0,
        homeRadiusM = 100.0,
        satellites = listOf(SatelliteReading(12, 1, 38.5f, true), SatelliteReading(70, 3, 21f, false)),
        vibrationG = 0.01f,
        tiltDeg = 2f,
        noiseDbfs = -61f,
        phone = PhoneState(batteryPercent = 80, network = "Mobile", freeStorageMb = 20_000L)
    )

    private fun TestScope.client(socket: FakeSocket) =
        SocketioSignalingClient(FakeSocketFactory(socket), StandardTestDispatcher(testScheduler))

    private fun FakeSocket.relay(event: String, payload: Any) =
        listeners[event]!!.forEach { it.call(if (payload is JSONObject) JSONObject(payload.toString()) else payload) }

    @Test
    fun `the Info survives the round trip and other settings types do not read it`() {
        val wire = JSONObject(SensorInfoWire.encode("T", info).toString())
        assertEquals("The relay routes by id to the Sensor's own room", "T", wire.getString("id"))
        assertEquals("sensor_info", wire.getString("type"))
        assertEquals(info, SensorInfoWire.decode(wire))
        assertNull("A Monitor before Nov15.5 ignores the new type", SensorSetupWire.decode(wire))
        assertNull(GeofenceWire.decode(wire))
        assertFalse(InfoRequestWire.isRequest(wire))
        assertNull(SensorInfoWire.decode(JSONObject(SensorSetupWire.encode("T", SensorSetupReport()).toString())))
    }

    @Test
    fun `a field from a newer Sensor is skipped and a malformed Info is ignored`() {
        val newer = JSONObject(SensorInfoWire.encode("T", info).toString())
        newer.put("info", JSONObject(newer.getString("info")).put("somethingNew", 5).toString())
        assertEquals(info, SensorInfoWire.decode(newer))
        assertNull(SensorInfoWire.decode(JSONObject().put("type", "sensor_info").put("id", "T")))
        assertNull(SensorInfoWire.decode(JSONObject().put("type", "sensor_info").put("info", "not json")))
    }

    @Test
    fun `only the own ping echo is the latency`() {
        val ping = LatencyPingWire.encode("T", "V", 1_000L)
        assertEquals("The relay sends it to the whole room", "T", ping.getString("id"))
        assertEquals(250L, LatencyPingWire.roundTripMs(ping, "V", 1_250L))
        assertNull("Another phone's ping", LatencyPingWire.roundTripMs(ping, "T", 1_250L))
        assertNull("No time", LatencyPingWire.roundTripMs(JSONObject().put("from", "V"), "V", 1_250L))
    }

    @Test
    fun `the Monitor asks, the Sensor hears the request and answers, the Monitor keeps the newest Info`() = runTest {
        val monitorSocket = FakeSocket()
        val monitor = client(monitorSocket)
        val sensorSocket = FakeSocket()
        val sensor = client(sensorSocket)
        try {
            assertFalse("Nothing can be asked before connecting", monitor.requestSensorInfo("T", withSatellites = false))
            monitor.connect("http://localhost", "V", "T")
            monitorSocket.simulateConnect()
            sensor.connect("http://localhost", "T", "T")
            sensorSocket.simulateConnect()
            runCurrent()

            assertTrue(monitor.requestSensorInfo("T", withSatellites = false))
            val request = monitorSocket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject
            assertEquals("T", request.getString("id"))
            val heard = async(start = CoroutineStart.UNDISPATCHED) { sensor.infoRequestFlow.first() }
            sensorSocket.relay("settings_relay", request)
            assertFalse("A closed satellite list does not ask for the list", heard.await())
            assertNull("A request is no Info", sensor.sensorInfoFlow.value)

            assertTrue(sensor.sendSensorInfo(info))
            val answer = sensorSocket.emittedEvents.single { it.first == "settings_update" }.second as JSONObject
            monitorSocket.relay("settings_relay", answer)
            assertEquals(info, monitor.sensorInfoFlow.value!!.info)
        } finally {
            monitor.disconnect()
            sensor.disconnect()
        }
        assertNull("Leaving on purpose forgets the Info: the next session may watch another Sensor", monitor.sensorInfoFlow.value)
    }

    /** Chapter 197 (ISSUE-NOV15.5-O99 part 1): the satellite list travels only while a Monitor's list is open. */
    @Test
    fun `the request tells whether the satellite list is wanted, and a request from an older Monitor wants it`() {
        assertTrue(InfoRequestWire.wantsSatellites(JSONObject(InfoRequestWire.encode("T", withSatellites = true).toString())))
        assertFalse(InfoRequestWire.wantsSatellites(JSONObject(InfoRequestWire.encode("T", withSatellites = false).toString())))
        val older = JSONObject().put("id", "T").put("type", InfoRequestWire.TYPE)
        assertTrue("A Monitor before Nov16.4 reads the summary from the list", InfoRequestWire.wantsSatellites(older))
    }

    /** Chapter 197: without the list the Info keeps the summary and is a fraction of the size. */
    @Test
    fun `the Info without the satellite list keeps its summary and is much smaller`() {
        val many = (1..55).map { SatelliteReading(it, 1 + it % 6, 20f + it % 25, it % 3 == 0) }
        val gnss = com.jd.core.shared.gnssSummary(many)
        val full = info.copy(satellites = many, satellitesUsed = gnss.used, satellitesInView = gnss.inView, meanCn0DbHz = gnss.meanCn0DbHz)
        val lean = full.withoutSatelliteList()
        val fullBytes = SensorInfoWire.encode("T", full).toString().length
        val leanBytes = SensorInfoWire.encode("T", lean).toString().length
        assertTrue("Full $fullBytes, without the list $leanBytes", leanBytes * 3 < fullBytes)
        val received = SensorInfoWire.decode(JSONObject(SensorInfoWire.encode("T", lean).toString()))!!
        assertTrue(received.satellites.isEmpty())
        assertEquals(gnss, received.satelliteSummary())
    }

    @Test
    fun `the link statistics count drops and the latency comes from the ping echo`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            client.connect("http://localhost", "V", "T")
            assertFalse(client.linkStats.value.isConnected)
            socket.simulateConnect()
            runCurrent()
            assertTrue(client.linkStats.value.isConnected)
            assertEquals("The first connect is no drop", 0, client.linkStats.value.drops)

            val ping = socket.emittedEvents.filter { it.first == "ping_cmd" }
            assertEquals("A ping at once on connect", 1, ping.size)
            socket.relay("ping_relay", ping.single().second)
            assertNotNull("The own echo gives the latency", client.linkStats.value.latencyMs)
            socket.relay("ping_relay", LatencyPingWire.encode("T", "T", 0L))
            assertNotNull("The Sensor's ping in the same room changes nothing", client.linkStats.value.latencyMs)

            advanceTimeBy(LATENCY_PING_INTERVAL_MS + 1)
            assertEquals("Then one every interval", 2, socket.emittedEvents.count { it.first == "ping_cmd" })

            socket.disconnect()
            assertEquals(1, client.linkStats.value.drops)
            assertNull("No latency while disconnected", client.linkStats.value.latencyMs)
            advanceTimeBy(LATENCY_PING_INTERVAL_MS * 2)
            assertEquals("No pings while disconnected", 2, socket.emittedEvents.count { it.first == "ping_cmd" })
            socket.simulateConnect()
            runCurrent()
            assertTrue(client.linkStats.value.isConnected)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `only rows held while offline count as waiting, not the batch in transit`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            client.connect("http://localhost", "T", "T")
            runCurrent()
            repeat(3) { client.sendTelemetry(LocationUpdate(sequenceId = it.toLong())) }
            assertEquals("Offline: held", 3, client.queuedRowCount())

            socket.simulateConnect()
            runCurrent()
            assertEquals("Sent on connect", 0, client.queuedRowCount())
            client.sendTelemetry(LocationUpdate(sequenceId = 9L))
            assertEquals("A row in the half-second batch is no backlog", 0, client.queuedRowCount())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `the satellites and the noise level cross the wire, and a row without them means unknown`() = runTest {
        val socket = FakeSocket()
        val client = client(socket)
        try {
            client.connect("http://localhost", "T", "T")
            socket.simulateConnect()
            runCurrent()

            val update = LocationUpdate(lat = 32.0, lng = 34.0, timestamp = 5_000L, sequenceId = 3L).apply {
                satsUsed = 9; satsInView = 21; meanCn0DbHz = 33.5f; noiseDbfs = -58f; isCritical = true
            }
            client.sendTelemetry(update)
            update.reset() // The pool zeroes it at once.
            runCurrent()
            val sent = (socket.emittedEvents.first { it.first == "location_batch" }.second as JSONArray).getJSONObject(0)

            val received = async(start = CoroutineStart.UNDISPATCHED) { client.telemetryFlow.first() }
            socket.relay("location_relay", sent)
            with(received.await()) {
                assertEquals(9, satsUsed)
                assertEquals(21, satsInView)
                assertEquals(33.5f, meanCn0DbHz!!, 0.01f)
                assertEquals(-58f, noiseDbfs!!, 0.01f)
            }

            listOf("sats", "satsView", "snr", "noise").forEach { sent.remove(it) }
            val older = async(start = CoroutineStart.UNDISPATCHED) { client.telemetryFlow.first() }
            socket.relay("location_relay", sent)
            with(older.await()) {
                assertEquals(-1, satsUsed)
                assertEquals(-1, satsInView)
                assertNull(meanCn0DbHz)
                assertNull(noiseDbfs)
            }
        } finally {
            client.disconnect()
        }
    }
}
