package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** Chapter 162 (ISSUE-NOV5.2-O11 Group 5): tilt and lift of the resting tracker, at 50 Hz like the Sensor. */
class MotionSentinelTest {

    private val g = 9.81

    /** A phone lying flat, sampled every 20 ms; a jitter of 0.02 m/s² alternates in sign, like a real sensor's. */
    private class Phone(val sentinel: MotionSentinel = MotionSentinel()) {
        var nowMs = 0L
        val alerts = mutableListOf<MotionSentinel.Alert>()
        private var tick = 0

        fun feed(durationMs: Long, sample: (tMs: Long) -> DoubleArray) {
            val start = nowMs
            while (nowMs - start < durationMs) {
                val a = sample(nowMs - start)
                val noise = if (tick++ % 2 == 0) 0.02 else -0.02
                sentinel.onAccelerometer(a[0] + noise, a[1], a[2] - noise, nowMs)?.let { alerts += it }
                nowMs += 20L
            }
        }

        fun rest(durationMs: Long, angleDeg: Double = 0.0) = feed(durationMs) { tilted(angleDeg) }

        fun tilted(angleDeg: Double): DoubleArray {
            val r = Math.toRadians(angleDeg)
            return doubleArrayOf(9.81 * sin(r), 0.0, 9.81 * cos(r))
        }
    }

    private fun armedPhone(sensitivity: Float = DEFAULT_TILT_SENSITIVITY): Phone = Phone().apply {
        sentinel.tiltThresholdDegrees = tiltThresholdForSensitivity(sensitivity)
        rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        assertTrue("Armed after resting", sentinel.isArmed)
    }

    @Test
    fun armsOnlyAfterResting30Seconds() {
        val phone = Phone()
        phone.rest(MOTION_ARM_AFTER_STILL_MS - 1_000)
        assertFalse(phone.sentinel.isArmed)
        phone.rest(2_000)
        assertTrue(phone.sentinel.isArmed)
    }

    @Test
    fun aTiltOver15DegreesAlertsOnce() {
        val phone = armedPhone()
        phone.rest(2_000, angleDeg = 20.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.alerts.map { it.kind })
        assertFalse("One disturbance, one alert: disarmed until it rests again", phone.sentinel.isArmed)
    }

    @Test
    fun aTiltUnder15DegreesDoesNotAlert() {
        val phone = armedPhone()
        phone.rest(5_000, angleDeg = 10.0)
        assertTrue(phone.alerts.isEmpty())
    }

    @Test
    fun theSliderSetsTheTiltAngle() {
        assertEquals(15.0, tiltThresholdForSensitivity(0.5f), 1e-6)
        assertEquals(5.0, tiltThresholdForSensitivity(1.0f), 1e-6)
        assertEquals(23.0, tiltThresholdForSensitivity(0.1f), 1e-6)
        val phone = armedPhone(sensitivity = 1.0f)
        phone.rest(2_000, angleDeg = 10.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.alerts.map { it.kind })
    }

    @Test
    fun aPickupOf30CmAlertsAsLift() {
        val phone = armedPhone()
        // Up at 2 m/s² for 0.4 s, then braking for 0.4 s: a·T² = 32 cm.
        phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 2.0 else -2.0) }
        phone.rest(1_000)
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.alerts.map { it.kind })
        assertTrue(phone.alerts.single().detail.startsWith("accelerometer"))
    }

    @Test
    fun aRiseOf10CmIsNoLift() {
        val phone = armedPhone()
        // 0.6 m/s² for 0.4 s, then braking: a·T² = 9.6 cm.
        phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 0.6 else -0.6) }
        phone.rest(5_000)
        assertTrue(phone.alerts.isEmpty())
        assertTrue("Still armed", phone.sentinel.isArmed)
    }

    @Test
    fun aKnockOrASidewaysShoveIsNeitherTiltNorLift() {
        val phone = armedPhone()
        // A knock: 0.1 s of 6 m/s² and back.
        phone.feed(200) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 100) 6.0 else -6.0) }
        phone.rest(2_000)
        // A shove across the table: 3 m/s² sideways for 0.3 s each way.
        phone.feed(600) { t -> doubleArrayOf(if (t < 300) 3.0 else -3.0, 0.0, g) }
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun afterAnAlertItReArmsAtTheNewPlace() {
        val phone = armedPhone()
        phone.rest(2_000, angleDeg = 30.0)
        assertEquals(1, phone.alerts.size)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000, angleDeg = 30.0)
        assertTrue("Re-armed lying at 30 deg", phone.sentinel.isArmed)
        assertEquals("No new alert for the same tilt", 1, phone.alerts.size)
    }

    @Test
    fun theGravityEstimateFollowsTheRestingPhone() {
        val phone = Phone()
        assertNull(phone.sentinel.gravity())
        phone.rest(3_000, angleDeg = 90.0)
        val gravity = phone.sentinel.gravity()
        assertNotNull(gravity)
        assertEquals(g, gravity!![0], 0.05)
        assertEquals(0.0, gravity[2], 0.05)
    }

    /** The A15 has no gravity sensor: with this estimate its vibration value is real instead of always 0 (Nov9.0-LiftTilt). */
    @Test
    fun theGravityEstimateGivesARealVibrationValue() {
        val phone = Phone()
        phone.rest(3_000, angleDeg = 40.0)
        val resting = phone.tilted(40.0)
        val gravity = phone.sentinel.gravity()!!
        val still = SensingEngine.rotateToWorldSpace(resting[0], resting[1], resting[2], gravity[0], gravity[1], gravity[2])
        assertTrue("Still: ${still[0]} g", still[0] < 0.02)
        // A sideways jolt of 3 m/s² is 0.3 g of horizontal motion, over the Sensor's 0.12 g motion limit.
        val jolt = SensingEngine.rotateToWorldSpace(resting[0], resting[1] + 3.0, resting[2], gravity[0], gravity[1], gravity[2])
        assertTrue("Jolt: ${jolt[0]} g", jolt[0] > 0.12)
        assertEquals("Without any gravity the value was 0", 0.0, SensingEngine.rotateToWorldSpace(resting[0], resting[1] + 3.0, resting[2], 0.0, 0.0, 0.0)[0], 0.0)
    }

    @Test
    fun aSensorPauseBreaksNeitherStillnessNorLift() {
        val phone = armedPhone()
        phone.nowMs += 5_000 // No samples for 5 s, as when the sensor pauses.
        phone.rest(1_000)
        assertTrue(phone.alerts.isEmpty())
    }

    // Barometer: 0.1 hPa is about 0.83 m near sea level.

    private fun MotionSentinel.feedPressure(phone: Phone, fromHpa: Double, toHpa: Double, durationMs: Long, stepMs: Long = 200L) {
        val start = phone.nowMs
        var t = 0L
        while (t < durationMs) {
            val hPa = fromHpa + (toHpa - fromHpa) * t / durationMs
            // The accelerometer keeps reporting a resting phone.
            phone.feed(stepMs) { doubleArrayOf(0.0, 0.0, g) }
            onPressure(hPa, start + t)?.let { phone.alerts += it }
            t += stepMs
        }
    }

    @Test
    fun aBarometricHeightChangeOf80CmAlertsAsLift() {
        val phone = Phone()
        phone.sentinel.onPressure(1013.25, 0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25, 5_000)
        // An elevator: 0.12 hPa (1 m) within 5 s, then steady.
        phone.sentinel.feedPressure(phone, 1013.25, 1013.13, 5_000)
        phone.sentinel.feedPressure(phone, 1013.13, 1013.13, 5_000)
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.alerts.map { it.kind })
        assertTrue(phone.alerts.single().detail.startsWith("barometer"))
    }

    @Test
    fun weatherDriftIsNoLift() {
        val phone = Phone()
        phone.sentinel.onPressure(1013.25, 0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        // 1 hPa per hour (fast weather) for 10 minutes: 1.4 m in total, but slowly.
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25 - 1.0 / 6, 600_000, stepMs = 1_000L)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun aDoorSlamPressureWaveIsNoLift() {
        val phone = Phone()
        phone.sentinel.onPressure(1013.25, 0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25, 5_000)
        phone.sentinel.feedPressure(phone, 1013.55, 1013.55, 600) // +0.3 hPa for 0.6 s.
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25, 10_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun pressureAltitudeMatchesTheStandardAtmosphere() {
        assertEquals(0.0, pressureAltitudeMeters(1013.25), 1e-6)
        assertEquals(0.83, pressureAltitudeMeters(1013.15) - pressureAltitudeMeters(1013.25), 0.02)
    }
}
