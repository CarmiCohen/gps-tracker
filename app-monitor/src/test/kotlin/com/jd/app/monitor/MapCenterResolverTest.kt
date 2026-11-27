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

    private fun row(minute: Long, lat: Double, status: String = "VALID", accuracy: Float = 5f) =
        TrailEntity("T", minute * 60_000L, minute, lat, 34.78, accuracy, status)

    @Test
    fun trailSegmentsLeaveOutJumpsOutliersAndCoarseFixes() {
        val trail = listOf(
            row(5, 32.0004), row(4, 33.0, "JUMP"), row(3, 32.0003, "OUTLIER"),
            row(2, 32.0002, accuracy = 900f), row(1, 32.0001), trailPoint(0, 0.0, 0.0)
        )
        assertEquals(listOf(listOf(MapPosition(32.0004, 34.78), MapPosition(32.0001, 34.78))), trailSegments(trail))
        assertEquals(listOf(4L, 3L), gpsJumps(trail).map { it.sequenceId })
    }

    @Test
    fun trailSegmentsBreakOnlyAtALongPauseThatAlsoMoved() {
        // 10 minutes still (same place): one line; then 10 minutes and about 1.1 km: a new line.
        val trail = listOf(row(30, 32.01), row(20, 32.0), row(10, 32.0), row(0, 32.0))
        assertEquals(listOf(1, 3), trailSegments(trail).map { it.size })
    }

    @Test
    fun monitorPhoneTrailDrawsNoSpikes() {
        // The S21FE's own rows (ISSUE-NOV26.29-O164): a 1200 m cell fix 1.4 km off between two home fixes, and a
        // visit 0.6 km away 3 hours after the last home fix and 5 hours before the next.
        fun local(minute: Long, lat: Double, accuracy: Float) =
            TrailEntity("T", minute * 60_000L, minute, lat, 35.30, accuracy, "VALID", isLocalUser = true)
        val trail = listOf(
            local(600, 32.6532, 12f), local(300, 32.6583, 32f), local(299, 32.6583, 83f),
            local(120, 32.6531, 64f), local(119, 32.6658, 1200f), local(118, 32.6531, 12f)
        )
        assertEquals(listOf(1, 2, 2), trailSegments(trail).map { it.size })
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

    // Chapter 238.1 (ISSUE-NOV22.5-O131): one automatic centering per start or return to the foreground.
    @Test
    fun theFirstCenteringZoomsIn() {
        assertEquals(MapCentering(sensor, zoomIn = true), pendingCentering(true, false, sensor))
    }

    @Test
    fun aLaterCenteringKeepsTheUsersZoom() {
        assertEquals(MapCentering(local, zoomIn = false), pendingCentering(true, true, local))
    }

    @Test
    fun newPositionsDoNotMoveTheMapOnceCentered() {
        assertNull(pendingCentering(false, true, sensor))
        assertNull(pendingCentering(false, false, sensor))
    }

    @Test
    fun aPendingCenteringWaitsForAPosition() {
        assertNull(pendingCentering(true, false, null))
    }
}
