package com.jd.core.network

import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.GraphRequest
import com.jd.core.shared.HomePoint
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorEventBatch
import com.jd.core.shared.SensorGraphBatch
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorSetupReport
import com.jd.core.shared.PlacementExamples
import com.jd.core.shared.SitExample
import com.jd.core.shared.SitExampleBook
import com.jd.core.shared.SitExampleReport
import com.jd.core.shared.SitFeatures
import com.jd.core.shared.SitLabel
import com.jd.core.shared.SitLearnRequest
import com.jd.core.shared.SitVerdict
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 236 (SIMP-NOV15.3-S1): one decoder reads every message of the relay's settings_update / settings_relay
 * event once, by its type, into one sealed [SettingsMessage].
 */
class SettingsWireTest {

    /** The payload as the relay forwards it: JSON text read back, not the sender's own object. */
    private fun relayed(payload: JSONObject): JSONObject = JSONObject(payload.toString())

    @Test
    fun `every settings type reads as its own message`() {
        val config = GeofenceConfig(listOf(HomePoint(32.1, 34.8)), radiusMeters = 40.0, revision = 3L)
        assertEquals(SettingsMessage.Geofence(config), SettingsWire.decode(relayed(GeofenceWire.encode("T", config))))

        val behaviour = SensorBehaviour(tiltAlert = false, revision = 5L)
        assertEquals(SettingsMessage.Behaviour(behaviour), SettingsWire.decode(relayed(SensorBehaviourWire.encode("T", behaviour))))

        val report = SensorSetupReport(trackerId = "T", behaviourRevision = 5L, sentAtMs = 9_000L)
        assertEquals(SettingsMessage.Setup(report), SettingsWire.decode(relayed(SensorSetupWire.encode("T", report))))

        val event = EventEntry("S1", 1_800_000_000_000L, EventSource.SENSOR, EventLevel.ALARM, "Tilt alert", trackerId = "T")
        assertEquals(
            SettingsMessage.Events(SensorEventBatch("T", listOf(event), completeAfterMs = 7L)),
            SettingsWire.decode(relayed(SensorEventsWire.encode("T", listOf(event.copy(trackerId = "")), 7L)))
        )
        assertEquals(SettingsMessage.EventsRequest(7L), SettingsWire.decode(relayed(EventsRequestWire.encode("T", 7L))))

        val info = SensorInfo(capturedAtMs = 1_000L, sensorId = "T", version = "Nov22.3")
        assertEquals(SettingsMessage.Info(info), SettingsWire.decode(relayed(SensorInfoWire.encode("T", info))))
        assertEquals(SettingsMessage.InfoRequest(false), SettingsWire.decode(relayed(InfoRequestWire.encode("T", withSatellites = false))))
        assertEquals(SettingsMessage.InfoRequest(true), SettingsWire.decode(relayed(InfoRequestWire.encode("T", withSatellites = true))))

        assertEquals(
            SettingsMessage.GraphRequest(GraphRequest(7L, compressed = true)),
            SettingsWire.decode(relayed(GraphRequestWire.encode("T", 7L)))
        )
        assertEquals(
            SettingsMessage.GraphSamples(SensorGraphBatch("T", emptyList())),
            SettingsWire.decode(relayed(GraphSamplesWire.encode("T", emptyList())))
        )

        // ISSUE-NOV26.0-O163 part 2.
        val learn = SitLearnRequest(placementId = 7L, label = SitLabel.STAND_UP, keepIds = listOf(1L, 7L))
        assertEquals(SettingsMessage.SitLearn(learn), SettingsWire.decode(relayed(SitLearnWire.encode("T", learn))))
        val example = SitExample(SitLabel.SIT, 1_000L, SitFeatures(joltG = 0.45, firstMoveMs2 = -0.15, tiltDeg = 0.8, settleMs = 600L), SitVerdict.SIT)
        val examples = SitExampleReport("SM-A155F", hasBarometer = false, book = SitExampleBook(listOf(PlacementExamples(7L, "SM-A155F", false, listOf(example)))))
        assertEquals(SettingsMessage.SitExamples("T", examples), SettingsWire.decode(relayed(SitExamplesWire.encode("T", examples))))
    }

    @Test
    fun `an unknown, missing or malformed type reads as nothing`() {
        assertNull("A newer peer's type is ignored", SettingsWire.decode(JSONObject().put("id", "T").put("type", "siren")))
        assertNull(SettingsWire.decode(JSONObject().put("id", "T")))
        assertNull("A malformed geofence", SettingsWire.decode(JSONObject().put("type", GeofenceWire.TYPE).put("rev", 1L)))
        assertNull("A malformed Info", SettingsWire.decode(JSONObject().put("type", SensorInfoWire.TYPE).put("info", "not json")))
        assertNull("A malformed learn request", SettingsWire.decode(JSONObject().put("type", SitLearnWire.TYPE).put("req", "{}")))
        assertNull("Examples without the Sensor's ID", SettingsWire.decode(JSONObject().put("type", SitExamplesWire.TYPE).put("report", "{}")))
    }

    @Test
    fun `the type names are distinct`() {
        val types = listOf(
            GeofenceWire.TYPE, SensorBehaviourWire.TYPE, SensorSetupWire.TYPE, SensorEventsWire.TYPE, EventsRequestWire.TYPE,
            SensorInfoWire.TYPE, InfoRequestWire.TYPE, GraphRequestWire.TYPE, GraphSamplesWire.TYPE,
            SitLearnWire.TYPE, SitExamplesWire.TYPE
        )
        assertEquals(types.size, types.toSet().size)
    }
}
