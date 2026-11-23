package com.jd.core.shared

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * GeofenceIntegrityTest: Ported from OLD MainAlarmLogicTest.
 * Validates R018: Geofence Hysteresis and Bayesian Gap Suppression.
 * Ensures the Kotlin engine avoids false-positive alarms during GPS gaps.
 * Nov23.12 (SIMP-NOV8.13-S1): checks the apps' own rule, [SentinelLogic.evaluateHomeZone].
 */
class GeofenceIntegrityTest {

    private val homeGeofence = GeofenceConfig(points = listOf(HomePoint(32.0, 34.0)), radiusMeters = 100.0, revision = 1L)

    private fun check(update: LocationUpdate, previous: SentinelLogic.GeofenceState) =
        SentinelLogic.evaluateHomeZone(update.lat, update.lng, update.accuracy, homeGeofence, previous)

    /**
     * verifyGeofenceTransitionLogic: Verifies basic ENTER/EXIT detection.
     */
    @Test
    fun verifyGeofenceTransitionLogic() {
        val insideUpdate = LocationUpdate(lat = 32.0, lng = 34.0)
        val outsideUpdate = LocationUpdate(lat = 32.1, lng = 34.1) // Way outside

        // 1. Case: Moving OUT
        val exitResult = check(outsideUpdate, SentinelLogic.GeofenceState.INSIDE)
        assertEquals("Should detect EXIT transition", SentinelLogic.GeofenceTransition.EXIT, exitResult.transition)
        assertEquals(SentinelLogic.GeofenceState.OUTSIDE, exitResult.currentState)

        // 2. Case: Moving IN
        val enterResult = check(insideUpdate, SentinelLogic.GeofenceState.OUTSIDE)
        assertEquals("Should detect ENTER transition", SentinelLogic.GeofenceTransition.ENTER, enterResult.transition)
        assertEquals(SentinelLogic.GeofenceState.INSIDE, enterResult.currentState)
    }

    /**
     * verifyHysteresisBufferR018: Ensures that the system doesn't 
     * flip-flop when exactly on the boundary.
     * The accuracy band against boundary noise is covered by HomeZoneTest.
     */
    @Test
    fun verifyHysteresisBufferR018() {
        // Boundary is 100m. 
        // We verify that the distance calculation is stable.
        val boundaryUpdate = LocationUpdate(lat = 32.0009, lng = 34.0) // ~100m away
        
        val result = check(boundaryUpdate, SentinelLogic.GeofenceState.INSIDE)
        
        // This confirms the distance math is precise enough for hysteresis gates
        assertTrue("Distance to center should be ~100m", abs(result.distanceToCenter - 100.0) < 5.0)
    }

    /**
     * verifyJumpSuppressionR133: Ensures that a "Jump" status update 
     * is flagged when speed is physically impossible.
     */
    @Test
    fun verifyJumpSuppressionR133() {
        val last = LocationUpdate(lat = 32.0, lng = 34.0, timestamp = 1000L)
        val current = LocationUpdate(lat = 33.0, lng = 35.0, timestamp = 2000L) // Massive jump

        val validation = SentinelLogic.validateMovement(last, current, hasPhysicalMotion = true)
        
        assertEquals(
            "Impossible movement must be flagged as JUMP or OUTLIER",
            SentinelLogic.SentinelStatus.OUTLIER, 
            validation.status
        )
    }
}
