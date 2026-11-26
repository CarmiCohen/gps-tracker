package com.jd.core.shared

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV25.25-O162: STATIONARY to MOVING only after several fast fixes that really moved (user decision 2026-11-26). */
class MovementTrackerTest {

    private val metersPerDegree = 111_195.0

    /** Feeds a fix [northM] north of a fixed point at [timeS] seconds. */
    private fun MovementTracker.fix(timeS: Double, northM: Double, speedKmh: Float?, accuracyM: Float = 5f): Boolean =
        onFix((timeS * 1000).toLong(), 32.0 + northM / metersPerDegree, 34.8, accuracyM, speedKmh?.let { it / 3.6f })

    /** Walking at 5 km/h (1.39 m/s), one fix a second from [fromS]. */
    private fun MovementTracker.walk(fromS: Int, seconds: Int, startM: Double = 0.0): Boolean {
        var moving = false
        for (s in 0 until seconds) moving = fix((fromS + s).toDouble(), startM + s * 1.39, 5f)
        return moving
    }

    @Test
    fun aRestingPhonesFastFixesWithinItsAccuracyAreNoMovement() {
        val tracker = MovementTracker()
        // GPS noise: 3 to 6 km/h for a minute, wandering within 8 m, accuracy 12 m.
        for (s in 0 until 60) assertFalse("$s s", tracker.fix(s.toDouble(), (s % 9) - 4.0, 3f + s % 4, accuracyM = 12f))
    }

    @Test
    fun walkingIsMovingOnceFarEnoughAndLongEnough() {
        val tracker = MovementTracker()
        assertFalse("7 s, 8.3 m: not yet 10 m", tracker.walk(0, 7))
        assertTrue("8 s, 9.7 m... 9 s, 11.1 m", tracker.walk(7, 3, startM = 7 * 1.39))
    }

    @Test
    fun twoFixesAreNotEnoughHoweverFar() {
        val tracker = MovementTracker()
        assertFalse(tracker.fix(0.0, 0.0, 30f))
        assertFalse("Two fixes, 6 s and 50 m apart", tracker.fix(6.0, 50.0, 30f))
        assertTrue("The third", tracker.fix(7.0, 58.0, 30f))
    }

    @Test
    fun aShortRunIsNotEnoughHoweverFar() {
        val tracker = MovementTracker()
        for (s in 0..4) assertFalse("$s s: under 5 s", tracker.fix(s * 0.9, s * 20.0, 60f))
        assertTrue("5 s", tracker.fix(5.0, 100.0, 60f))
    }

    @Test
    fun theDistanceMustExceedTheWorseAccuracy() {
        val tracker = MovementTracker()
        tracker.fix(0.0, 0.0, 10f, accuracyM = 30f)
        tracker.fix(3.0, 12.0, 10f, accuracyM = 5f)
        assertFalse("25 m inside the first fix's 30 m", tracker.fix(6.0, 25.0, 10f, accuracyM = 5f))
        assertTrue("31 m", tracker.fix(7.0, 31.0, 10f, accuracyM = 5f))
    }

    @Test
    fun aSlowFixStartsTheRunOver() {
        val tracker = MovementTracker()
        tracker.walk(0, 5)
        tracker.fix(5.0, 7.0, 1f)
        assertFalse("A new run from 6 s", tracker.walk(6, 7, startM = 7.0))
        assertTrue(tracker.walk(13, 2, startM = 7.0 + 7 * 1.39))
    }

    @Test
    fun theThresholdIs1_5KmH() {
        val slow = MovementTracker()
        for (s in 0..10) assertFalse(slow.fix(s.toDouble(), s * 5.0, 1.5f))
        val fast = MovementTracker()
        var moving = false
        for (s in 0..10) moving = fast.fix(s.toDouble(), s * 5.0, 1.6f)
        assertTrue(moving)
        val noSpeed = MovementTracker()
        for (s in 0..10) assertFalse("No speed is slow", noSpeed.fix(s.toDouble(), s * 5.0, null))
    }

    @Test
    fun stationaryAgainAfter10SlowSeconds() {
        val tracker = MovementTracker()
        assertTrue(tracker.walk(0, 10))
        assertTrue(tracker.fix(10.0, 14.0, 0f))
        assertTrue("A fast fix in between", tracker.fix(15.0, 15.0, 4f))
        for (s in 16..24) assertTrue("$s s", tracker.fix(s.toDouble(), 15.0, 0f))
        assertFalse("10 s slow", tracker.fix(26.0, 15.0, 0f))
        assertFalse("One fast fix does not make MOVING again", tracker.fix(27.0, 30.0, 20f))
    }

    @Test
    fun aFixNotNewerThanTheLastIsIgnored() {
        val tracker = MovementTracker()
        tracker.fix(0.0, 0.0, 30f)
        repeat(5) { tracker.fix(0.0, 0.0, 30f) } // The same row fed again.
        assertFalse(tracker.fix(5.0, 40.0, 30f))
        assertFalse("An older row", tracker.fix(3.0, 20.0, 30f))
        assertTrue(tracker.fix(6.0, 48.0, 30f))
    }
}
