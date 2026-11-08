package com.jd.core.shared

import com.jd.core.shared.SentinelLogic.GeofenceState.INSIDE
import com.jd.core.shared.SentinelLogic.GeofenceState.OUTSIDE
import com.jd.core.shared.SentinelLogic.GeofenceState.UNKNOWN
import com.jd.core.shared.SentinelLogic.GeofenceTransition.ENTER
import com.jd.core.shared.SentinelLogic.GeofenceTransition.EXIT
import com.jd.core.shared.SentinelLogic.GeofenceTransition.NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 156 (ISSUE-NOV8.13-O51): the Sensor checks each fix against the Monitor user's Home Points.
 * One degree of latitude is about 111 km, so 0.001 degrees is about 111 m.
 */
class HomeZoneTest {

    private val home = HomePoint(32.0, 34.0)
    private val config = GeofenceConfig(points = listOf(home), radiusMeters = 100.0, revision = 1L)

    private fun northOfHome(meters: Double) = 32.0 + meters / 111_195.0

    @Test
    fun `leaving the only home point is an exit`() {
        val inside = SentinelLogic.evaluateHomeZone(northOfHome(50.0), 34.0, 5f, config, INSIDE)
        assertEquals(INSIDE, inside.currentState)
        assertEquals(NONE, inside.transition)

        val out = SentinelLogic.evaluateHomeZone(northOfHome(300.0), 34.0, 5f, config, INSIDE)
        assertEquals(OUTSIDE, out.currentState)
        assertEquals(EXIT, out.transition)
        assertEquals(300.0, out.distanceToCenter, 2.0)
    }

    @Test
    fun `coming back is an enter, and the next exit alarms again`() {
        val back = SentinelLogic.evaluateHomeZone(northOfHome(20.0), 34.0, 5f, config, OUTSIDE)
        assertEquals(ENTER, back.transition)
        val again = SentinelLogic.evaluateHomeZone(northOfHome(400.0), 34.0, 5f, config, back.currentState)
        assertEquals(EXIT, again.transition)
    }

    @Test
    fun `the first fix after a start or a change sets the state without a transition`() {
        val away = SentinelLogic.evaluateHomeZone(northOfHome(5_000.0), 34.0, 5f, config, UNKNOWN)
        assertEquals(OUTSIDE, away.currentState)
        assertEquals("Being away when checking starts is not a breach", NONE, away.transition)

        val home = SentinelLogic.evaluateHomeZone(northOfHome(10.0), 34.0, 5f, config, UNKNOWN)
        assertEquals(INSIDE, home.currentState)
        assertEquals(NONE, home.transition)
    }

    @Test
    fun `a fix whose error circle still reaches the zone keeps the previous state`() {
        // 150 m from home with 80 m accuracy: the near edge is 70 m away, inside the 100 m radius.
        val noisy = SentinelLogic.evaluateHomeZone(northOfHome(150.0), 34.0, 80f, config, INSIDE)
        assertEquals(INSIDE, noisy.currentState)
        assertEquals("GPS noise at the boundary raises no breach", NONE, noisy.transition)

        // The same distance with 20 m accuracy: the near edge is 130 m away, so the tracker has left.
        val clear = SentinelLogic.evaluateHomeZone(northOfHome(150.0), 34.0, 20f, config, INSIDE)
        assertEquals(EXIT, clear.transition)
    }

    @Test
    fun `the tracker is home while inside any home point`() {
        val second = HomePoint(northOfHome(1_000.0), 34.0)
        val two = config.withPoint(second)
        val atSecond = SentinelLogic.evaluateHomeZone(northOfHome(1_020.0), 34.0, 5f, two, INSIDE)
        assertEquals(INSIDE, atSecond.currentState)
        assertEquals(NONE, atSecond.transition)
        assertEquals(20.0, atSecond.distanceToCenter, 2.0)

        val between = SentinelLogic.evaluateHomeZone(northOfHome(500.0), 34.0, 5f, two, INSIDE)
        assertEquals(EXIT, between.transition)
    }

    @Test
    fun `no home points means no geofence`() {
        val none = SentinelLogic.evaluateHomeZone(northOfHome(5_000.0), 34.0, 5f, GeofenceConfig(), INSIDE)
        assertEquals(UNKNOWN, none.currentState)
        assertEquals(NONE, none.transition)
        assertFalse(GeofenceConfig().isActive)
    }

    @Test
    fun `edits keep the radius in range and the revision always grows`() {
        assertEquals(GeofenceConfig.MIN_RADIUS_METERS, config.withRadius(1.0).radiusMeters, 0.0)
        assertEquals(GeofenceConfig.MAX_RADIUS_METERS, config.withRadius(10_000.0).radiusMeters, 0.0)
        assertEquals(250.0, config.withRadius(250.0).radiusMeters, 0.0)

        val two = config.withPoint(HomePoint(1.0, 2.0))
        assertEquals(listOf(home), two.withoutPoint(1).points)
        assertEquals("An index that is gone changes nothing", two, two.withoutPoint(5))
        assertTrue(two.cleared().points.isEmpty())
        assertEquals(two.radiusMeters, two.cleared().radiusMeters, 0.0)

        val previous = config.copy(revision = 5_000L)
        assertEquals(6_000L, config.revisedAfter(previous, nowMs = 6_000L).revision)
        assertEquals("A clock that went back still yields a newer revision", 5_001L, config.revisedAfter(previous, nowMs = 10L).revision)
    }
}
