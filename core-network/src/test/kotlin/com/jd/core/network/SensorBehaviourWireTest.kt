package com.jd.core.network

import com.jd.core.shared.SensorBehaviour
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 188 (ISSUE-NOV10.8-O83): the Sensor's detection settings travel from the Monitor inside the relay's existing
 * settings_update / settings_relay event, next to the geofence, so the relay needs no change.
 */
class SensorBehaviourWireTest {

    private val behaviour = SensorBehaviour(
        tiltAlert = false,
        vibrationAlert = true,
        noiseAlert = false,
        liftAlert = true,
        tiltSensitivity = 0.3f,
        vibrationSensitivity = 0.9f,
        noiseSensitivity = 0.7f,
        revision = 1_794_000_000_000L
    )

    @Test
    fun `the settings survive the round trip`() {
        val wire = SensorBehaviourWire.encode("TRK-1", behaviour)
        assertEquals("The relay routes by id to the tracker's room", "TRK-1", wire.getString("id"))
        assertEquals("sensor_behaviour", wire.getString("type"))
        assertEquals(behaviour, SensorBehaviourWire.decode(JSONObject(wire.toString())))
    }

    @Test
    fun `the two settings types do not read each other`() {
        val behaviourWire = JSONObject(SensorBehaviourWire.encode("TRK-1", behaviour).toString())
        assertNull("A Sensor before Nov15.2 ignores the new type", GeofenceWire.decode(behaviourWire))
        val geofenceWire = JSONObject(GeofenceWire.encode("TRK-1", com.jd.core.shared.GeofenceConfig()).toString())
        assertNull(SensorBehaviourWire.decode(geofenceWire))
    }

    @Test
    fun `malformed payloads are ignored`() {
        assertNull(SensorBehaviourWire.decode(JSONObject().put("id", "TRK-1")))
        assertNull(SensorBehaviourWire.decode(JSONObject().put("type", "sensor_behaviour").put("rev", 1L)))
    }
}
