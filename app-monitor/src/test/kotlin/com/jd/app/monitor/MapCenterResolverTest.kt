package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies Chapter 118.2: the map centers only on real device positions. */
class MapCenterResolverTest {

    private val sensor = MapPosition(32.08, 34.78)
    private val local = MapPosition(31.77, 35.21)

    private fun trailPoint(sequence: Long, lat: Double, lng: Double) =
        TrailEntity("T", sequence, sequence, lat, lng, 5f, "VALID")

    @Test
    fun knownPositionsDropsNoFixRowsAndKeepsOrder() {
        val trail = listOf(
            trailPoint(3, sensor.lat, sensor.lng),
            trailPoint(2, 0.0, 0.0),
            trailPoint(1, local.lat, local.lng)
        )

        assertEquals(listOf(sensor, local), knownPositions(trail))
    }

    @Test
    fun knownPositionsKeepsEquatorAndMeridianPoints() {
        val trail = listOf(trailPoint(2, 0.0, 34.78), trailPoint(1, 32.08, 0.0))

        assertEquals(2, knownPositions(trail).size)
    }

    @Test
    fun autoDisconnectedCentersOnLocalDevice() {
        assertEquals(local, resolveMapCenter(MapFollowMode.AUTO, false, sensor, local))
    }

    @Test
    fun autoConnectedCentersOnSensor() {
        assertEquals(sensor, resolveMapCenter(MapFollowMode.AUTO, true, sensor, local))
    }

    @Test
    fun autoFallsBackToWhicheverPositionIsKnown() {
        assertEquals(local, resolveMapCenter(MapFollowMode.AUTO, true, null, local))
        assertEquals(sensor, resolveMapCenter(MapFollowMode.AUTO, false, sensor, null))
    }

    @Test
    fun sensorOverrideStaysOnSensorWhenPeerIsInactive() {
        assertEquals(sensor, resolveMapCenter(MapFollowMode.TRACKER, false, sensor, local))
    }

    @Test
    fun localOverrideStaysOnLocalWhenPeerIsActive() {
        assertEquals(local, resolveMapCenter(MapFollowMode.VIEWER, true, sensor, local))
    }

    @Test
    fun explicitModesDoNotSubstituteTheOtherDevice() {
        assertNull(resolveMapCenter(MapFollowMode.TRACKER, true, null, local))
        assertNull(resolveMapCenter(MapFollowMode.VIEWER, true, sensor, null))
    }

    @Test
    fun noKnownPositionLeavesTheMapUncentered() {
        MapFollowMode.values().forEach { mode ->
            assertNull(resolveMapCenter(mode, false, null, null))
            assertNull(resolveMapCenter(mode, true, null, null))
        }
    }
}
