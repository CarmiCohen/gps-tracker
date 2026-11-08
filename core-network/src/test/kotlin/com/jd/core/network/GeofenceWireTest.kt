package com.jd.core.network

import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 156 (ISSUE-NOV8.13-O51): the geofence travels from the Monitor to the Sensor inside the relay's
 * existing settings_update / settings_relay event, so the relay needs no change.
 */
class GeofenceWireTest {

    private val config = GeofenceConfig(
        points = listOf(HomePoint(32.0853, 34.7818), HomePoint(31.7683, 35.2137)),
        radiusMeters = 150.0,
        revision = 1_794_000_000_000L
    )

    @Test
    fun `a geofence survives the round trip`() {
        val wire = GeofenceWire.encode("TRK-1", config)
        assertEquals("The relay routes by id to the tracker's room", "TRK-1", wire.getString("id"))
        assertEquals("geofence", wire.getString("type"))
        // The relay forwards the same object; parse it from text as the socket would.
        assertEquals(config, GeofenceWire.decode(JSONObject(wire.toString())))
    }

    @Test
    fun `clearing all home points is sent as an empty list`() {
        val cleared = config.cleared().copy(revision = config.revision + 1)
        assertEquals(cleared, GeofenceWire.decode(JSONObject(GeofenceWire.encode("TRK-1", cleared).toString())))
    }

    @Test
    fun `other settings and malformed payloads are ignored`() {
        assertNull(GeofenceWire.decode(JSONObject().put("id", "TRK-1").put("type", "siren")))
        assertNull(GeofenceWire.decode(JSONObject().put("id", "TRK-1")))
        assertNull(GeofenceWire.decode(JSONObject().put("type", "geofence").put("rev", 1L)))
    }
}
