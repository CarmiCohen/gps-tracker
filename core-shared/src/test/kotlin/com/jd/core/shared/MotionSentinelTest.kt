package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Chapter 162 (ISSUE-NOV5.2-O11 Group 5): tilt and lift of the resting tracker; chapter 164 (Group 7): its vibration.
 * Sampled at 50 Hz like the Sensor.
 */
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
                alerts += sentinel.onAccelerometer(a[0] + noise, a[1], a[2] - noise, nowMs)
                nowMs += 20L
            }
        }

        fun rest(durationMs: Long, angleDeg: Double = 0.0) = feed(durationMs) { tilted(angleDeg) }

        fun tilted(angleDeg: Double): DoubleArray {
            val r = Math.toRadians(angleDeg)
            return doubleArrayOf(9.81 * sin(r), 0.0, 9.81 * cos(r))
        }
    }

    private fun armedPhone(
        sensitivity: Float = DEFAULT_TILT_SENSITIVITY,
        vibrationSensitivity: Float = DEFAULT_VIBRATION_SENSITIVITY
    ): Phone = Phone().apply {
        sentinel.tiltThresholdDegrees = tiltThresholdForSensitivity(sensitivity)
        sentinel.vibrationThreshold = vibrationThresholdForSensitivity(vibrationSensitivity)
        rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        assertTrue("Armed after resting", sentinel.isArmed)
        assertTrue("Vibration armed after resting", sentinel.isVibrationArmed)
    }

    private val Phone.tiltOrLift get() = alerts.filter { it.kind != MotionSentinel.Kind.VIBRATION }

    /** A knock: 0.1 s of 6 m/s² and back, about 0.6 g. */
    private fun Phone.knock() = feed(200) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 100) 6.0 else -6.0) }

    /** [count] knocks, each followed by [restMs] of rest: [VIBRATION_HITS] of them 1.2 s apart make an alert. */
    private fun Phone.knocks(count: Int = VIBRATION_HITS, restMs: Long = 1_000) = repeat(count) {
        knock()
        rest(restMs)
    }

    /** A sideways jolt of [ms2] for 0.1 s and back; about [ms2] / 9.81 g. */
    private fun Phone.jolt(ms2: Double) = feed(200) { t -> doubleArrayOf(if (t < 100) ms2 else 0.0, 0.0, g) }

    private fun Phone.jolts(ms2: Double, count: Int = VIBRATION_HITS) = repeat(count) {
        jolt(ms2)
        rest(1_000)
    }

    /** The phone's own motor: ±1.1 m/s² (0.11 g, as measured on the A15 during a call) alternating every sample. */
    private fun Phone.ownMotor(durationMs: Long) {
        var flip = false
        feed(durationMs) { flip = !flip; doubleArrayOf(if (flip) 1.1 else -1.1, 0.0, g) }
    }

    /** Tipped over [overMs], not in one jolt, then resting at [angleDeg]. */
    private fun Phone.tiltSlowly(angleDeg: Double, overMs: Long = 2_000) {
        feed(overMs) { t -> tilted(angleDeg * t / overMs) }
        rest(2_000, angleDeg)
    }

    @Test
    fun armsOnlyAfterResting30Seconds() {
        val phone = Phone()
        phone.rest(MOTION_ARM_AFTER_STILL_MS - 1_000)
        assertFalse(phone.sentinel.isArmed)
        assertFalse(phone.sentinel.isVibrationArmed)
        phone.rest(2_000)
        assertTrue(phone.sentinel.isArmed)
        assertTrue(phone.sentinel.isVibrationArmed)
    }

    @Test
    fun aTiltOver15DegreesAlertsOnce() {
        val phone = armedPhone()
        phone.tiltSlowly(20.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.alerts.map { it.kind })
        assertFalse("One disturbance, one alert: disarmed until it rests again", phone.sentinel.isArmed)
    }

    @Test
    fun aTiltUnder15DegreesDoesNotAlert() {
        val phone = armedPhone()
        phone.tiltSlowly(10.0)
        phone.rest(3_000, angleDeg = 10.0)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    /** ISSUE-NOV9.4-O67: the sliders show the limit itself, so the label must match what the Sensor applies. */
    @Test
    fun theSlidersShowTheLimitItself() {
        assertEquals("15°", tiltLimitLabel(0.5f))
        assertEquals("5°", tiltLimitLabel(1.0f))
        assertEquals("23°", tiltLimitLabel(0.1f))
        assertEquals("0.13 g", vibrationLimitLabel(0.5f))
        assertEquals("0.10 g", vibrationLimitLabel(1.0f))
        assertEquals("0.82 g", vibrationLimitLabel(0.1f))
    }

    @Test
    fun theSliderSetsTheTiltAngle() {
        assertEquals(15.0, tiltThresholdForSensitivity(0.5f), 1e-6)
        assertEquals(5.0, tiltThresholdForSensitivity(1.0f), 1e-6)
        assertEquals(23.0, tiltThresholdForSensitivity(0.1f), 1e-6)
        val phone = armedPhone(sensitivity = 1.0f)
        phone.tiltSlowly(10.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.alerts.map { it.kind })
    }

    @Test
    fun aPickupOf30CmAlertsAsLift() {
        val phone = armedPhone()
        // Up at 2 m/s² for 0.4 s, then braking for 0.4 s: a·T² = 32 cm, then held there.
        phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 2.0 else -2.0) }
        phone.rest(LIFT_HOLD_MS)
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.tiltOrLift.map { it.kind })
        assertTrue(phone.tiltOrLift.single().detail, phone.tiltOrLift.single().detail.startsWith("accelerometer"))
        // Its start and its braking are two hits over the 0.13 g default, not the three of a vibration (ISSUE-NOV17.10-O111).
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.alerts.map { it.kind })
    }

    /** ISSUE-NOV17.10-O111: a lift must be held; raised and set straight back down is no lift. */
    @Test
    fun aPhoneRaisedAndSetBackDownIsNoLift() {
        val phone = armedPhone()
        // Up 32 cm as above, then at once back down the same way.
        phone.feed(1_600) { t ->
            doubleArrayOf(0.0, 0.0, g + when {
                t < 400 -> 2.0
                t < 1_200 -> -2.0
                else -> 2.0
            })
        }
        phone.rest(3_000)
        assertTrue("${phone.tiltOrLift}", phone.tiltOrLift.isEmpty())
        assertTrue("Still armed", phone.sentinel.isArmed)
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
        phone.knock()
        phone.rest(2_000)
        // A shove across the table: 3 m/s² sideways for 0.3 s each way.
        phone.feed(600) { t -> doubleArrayOf(if (t < 300) 3.0 else -3.0, 0.0, g) }
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.tiltOrLift.isEmpty())
    }

    /**
     * A hard knock on the chair's frame as the A15 saw it under a cushion (Nov25.23, ISSUE-NOV25.23-O154): the spike falls
     * in one 50 Hz sample, [spikeG] up, then the cushion rings ±0.5 m/s² for 1.5 s. Integrated as it was, 1.2 g left
     * 0.24 m/s and a lift of about 40 cm, as on the phone.
     */
    private fun Phone.knockCaughtInOneSample(spikeG: Double) {
        var flip = false
        feed(1_500) { t -> flip = !flip; doubleArrayOf(0.0, 0.0, g + if (t == 0L) spikeG * g else if (flip) 0.5 else -0.5) }
        rest(3_000)
    }

    @Test
    fun aKnockCaughtInOneSampleIsNoLift() {
        for (spikeG in listOf(1.2, 5.7)) {
            val phone = armedPhone()
            phone.knockCaughtInOneSample(spikeG)
            assertTrue("$spikeG g: ${phone.alerts}", phone.tiltOrLift.isEmpty())
            assertTrue("$spikeG g: still armed", phone.sentinel.isArmed)
        }
    }

    /**
     * The knock of 4.78 g on the A15's chair at 09:27:51 (Nov25.25, ISSUE-NOV25.23-O154), replayed from its 0.1 s trace
     * (upward cm/s²): a push over a whole 0.1 s passes the median and leaves 0.77 m/s while the phone rests; it raised
     * `Lift alert: accelerometer, about 79 cm up` until the hold required a speed near zero.
     */
    @Test
    fun aKnockThatPushesForATenthOfASecondIsNoLift() {
        val trace = listOf(795, -68, 2, 205, -29, -12, -388, 140, -23, -15, 5, -3, -4, 4, 0, 2, -1, 1, 2, -2)
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.feed(trace.size * 100L) { t -> doubleArrayOf(0.0, 0.0, g + trace[(t / 100).toInt()] / 100.0) }
        phone.rest(LIFT_GIVE_UP_MS + 1_000)
        assertTrue("${phone.alerts}", phone.alerts.none { it.kind == MotionSentinel.Kind.LIFT })
        assertTrue("$notes", notes.any { it.startsWith("Lift not confirmed: ") && it.contains("the speed never returned to zero (0.6") }) // Flat bins: 0.61, the phone 0.77.
        assertTrue("Still armed", phone.sentinel.isArmed)
    }

    /**
     * The knock of 2.46 g at 09:32:29 (Nov25.25, ISSUE-NOV25.23-O154) left only about 0.28 m/s and raised `about 48 cm up`
     * under the speed rule alone: its trace, with its peak sample in the first bin. A jolt over [LIFT_KNOCK_MS2] voids it.
     */
    @Test
    fun aKnockOver1GVoidsTheLiftIntegration() {
        val trace = listOf(-366, 42, 11, -9, 439, -73, 8, -193, 149, -1, 19, -4, 5, -15, 4, 3, 7, 4, -3, 3, 4)
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.feed(trace.size * 100L) { t -> doubleArrayOf(if (t == 40L) 2.46 * g else 0.0, 0.0, g + trace[(t / 100).toInt()] / 100.0) }
        phone.rest(LIFT_GIVE_UP_MS + 1_000)
        assertTrue("${phone.alerts}", phone.alerts.none { it.kind == MotionSentinel.Kind.LIFT })
        assertTrue("$notes", notes.any { it.startsWith("Lift not confirmed: a knock of 2.4") })
        assertTrue("Still armed", phone.sentinel.isArmed)
    }

    /**
     * A thief may bump the phone first: once it has rested 0.3 s after the knock, a pickup is a lift again. After a 3 g
     * knock the gravity estimate settles for about 0.5 s first, so a pickup within about 0.8 s is not integrated (tilt
     * may still alert).
     */
    @Test
    fun aPickupAfterAKnockIsStillALift() {
        val phone = armedPhone()
        phone.feed(40) { t -> doubleArrayOf(if (t == 0L) 3.0 * g else 0.0, 0.0, g) }
        phone.rest(1_000)
        phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 2.0 else -2.0) }
        phone.rest(LIFT_HOLD_MS + 200)
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.tiltOrLift.map { it.kind })
    }

    /** A snatch: up at 6 m/s² for 0.2 s and braking as hard, 24 cm, held. The knock filter must not hide it (ISSUE-NOV25.23-O154). */
    @Test
    fun aQuickSnatchIsStillALift() {
        val phone = armedPhone()
        phone.feed(400) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 200) 6.0 else -6.0) }
        phone.rest(LIFT_HOLD_MS + 200)
        assertEquals(listOf(MotionSentinel.Kind.LIFT), phone.tiltOrLift.map { it.kind })
    }

    @Test
    fun aRiseThatFallsBackLeavesANoteForTuning() {
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.feed(1_600) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400 || t >= 1_200) 2.0 else -2.0) } // Up 32 cm and back.
        phone.rest(3_000)
        assertTrue("${phone.alerts}", phone.tiltOrLift.isEmpty())
        assertTrue("$notes", notes.single { it.startsWith("Lift") }.startsWith("Lift not confirmed: up to 3"))
    }

    // Vibration (chapter 164, ISSUE-NOV5.2-O11 Group 7).

    @Test
    fun theSliderSetsTheVibrationLimit() {
        assertEquals(0.13 * STANDARD_GRAVITY, vibrationThresholdForSensitivity(0.5f), 1e-6)
        assertEquals(0.1 * STANDARD_GRAVITY, vibrationThresholdForSensitivity(1.0f), 1e-6)
        assertEquals(0.82 * STANDARD_GRAVITY, vibrationThresholdForSensitivity(0.1f), 1e-6)
        assertEquals("Halfway down", (0.82 + 0.13) / 2 * STANDARD_GRAVITY, vibrationThresholdForSensitivity(0.3f), 1e-6)
        assertEquals("Out of range is clamped", 0.1 * STANDARD_GRAVITY, vibrationThresholdForSensitivity(5f), 1e-6)
        var previous = Double.MAX_VALUE
        for (step in 1..10) {
            val limit = vibrationThresholdForSensitivity(step / 10f)
            assertTrue("More sensitive to the right: $step", limit < previous)
            previous = limit
        }
    }

    /** Values measured on the A15 in Nov9.4: a hard knock on the table 0.15 g, its own ringing 0.11 g (ISSUE-NOV9.2-O62). */
    @Test
    fun theDefaultCatchesHardTableKnocksButNotTheOwnRinging() {
        val table = armedPhone()
        table.jolts(0.15 * g)
        assertEquals(listOf(MotionSentinel.Kind.VIBRATION), table.alerts.map { it.kind })
        val ringing = armedPhone()
        ringing.ownMotor(5_000)
        ringing.rest(1_000)
        assertTrue("Even unreported, 0.11 g is under the default: ${ringing.alerts}", ringing.alerts.isEmpty())
    }

    @Test
    fun theOwnRingingNeverAlertsAndLeavesVibrationArmed() {
        val phone = armedPhone(vibrationSensitivity = 1.0f)
        phone.sentinel.isOwnVibrationActive = true
        phone.ownMotor(10_000)
        phone.knock() // A knock during the ringing cannot be told from it.
        phone.sentinel.isOwnVibrationActive = false
        phone.rest(OWN_VIBRATION_TAIL_MS + 200)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        assertTrue("Still armed after the call", phone.sentinel.isVibrationArmed)
        phone.knocks()
        assertEquals("Knocks after the call alert at once", listOf(MotionSentinel.Kind.VIBRATION), phone.alerts.map { it.kind })
    }

    /** ISSUE-NOV17.10-O111: hits during the ringing do not count towards the next alert. */
    @Test
    fun hitsDuringTheRingingDoNotCount() {
        val phone = armedPhone()
        phone.knocks(VIBRATION_HITS - 1)
        phone.sentinel.isOwnVibrationActive = true
        phone.rest(200)
        phone.sentinel.isOwnVibrationActive = false
        phone.rest(OWN_VIBRATION_TAIL_MS + 200)
        phone.knocks(VIBRATION_HITS - 1)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun ringingReportedLateStillCancelsTheAlert() {
        val phone = armedPhone(vibrationSensitivity = 1.0f)
        phone.ownMotor(VIBRATION_HOLD_MS - 60) // The motor starts before Android reports the ringing.
        phone.sentinel.isOwnVibrationActive = true
        phone.ownMotor(3_000)
        phone.sentinel.isOwnVibrationActive = false
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        assertTrue(phone.sentinel.isVibrationArmed)
    }

    @Test
    fun withoutTheReportTheOwnMotorAlertsAtTheHighestSensitivity() {
        val phone = armedPhone(vibrationSensitivity = 1.0f)
        // A steady buzz over the limit is one hit (ISSUE-NOV17.10-O111)...
        phone.ownMotor(3_000)
        phone.rest(1_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        // ...but a ringing vibrates in pulses: each pulse is a hit.
        repeat(VIBRATION_HITS) {
            phone.ownMotor(200)
            phone.rest(300)
        }
        phone.rest(1_000)
        assertEquals("The A15 measurement the ringing report fixes", listOf(MotionSentinel.Kind.VIBRATION), phone.alerts.map { it.kind })
    }

    @Test
    fun knocksAlertOnceAsVibration() {
        val phone = armedPhone()
        phone.knocks(VIBRATION_HITS + 2)
        assertEquals(listOf(MotionSentinel.Kind.VIBRATION), phone.alerts.map { it.kind })
        val detail = phone.alerts.single().detail
        assertTrue(detail, detail.startsWith("4 hits in 3.6 s, strongest 0.6")) // 3 hits in 2.4 s before ISSUE-NOV25.25-O162.
        assertTrue(detail, detail.endsWith("(limit 0.13 g)"))
        assertFalse("One disturbance, one alert", phone.sentinel.isVibrationArmed)
        assertTrue("Knocks leave tilt and lift armed", phone.sentinel.isArmed)
    }

    /** ISSUE-NOV17.10-O111: single knocks on the table, about one an hour on the A15, are no intruder. */
    @Test
    fun fewerHitsThanNeededDoNotAlert() {
        val phone = armedPhone()
        phone.knocks(VIBRATION_HITS - 1)
        phone.rest(VIBRATION_HIT_WINDOW_MS)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        assertTrue("Still armed", phone.sentinel.isVibrationArmed)
        // Hits spread wider than the window never add up.
        repeat(5) { phone.knocks(1, restMs = VIBRATION_HIT_WINDOW_MS / (VIBRATION_HITS - 1)) }
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        // The samples of one knock are one hit, however strong.
        phone.rest(VIBRATION_HIT_WINDOW_MS)
        phone.feed(VIBRATION_HIT_GAP_MS - 40) { t -> doubleArrayOf(0.0, 0.0, g + if ((t / 20) % 2 == 0L) 8.0 else -8.0) }
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun aLightJoltAlertsOnlyAtHighSensitivity() {
        // 0.115 g sideways for 0.1 s and back: between the 0.13 g default and the 0.1 g of the highest setting.
        val normal = armedPhone()
        normal.jolts(0.115 * g)
        normal.rest(2_000)
        assertTrue("${normal.alerts}", normal.alerts.isEmpty())
        val sensitive = armedPhone(vibrationSensitivity = 1.0f)
        sensitive.jolts(0.115 * g)
        assertEquals(listOf(MotionSentinel.Kind.VIBRATION), sensitive.alerts.map { it.kind })
    }

    @Test
    fun restingNoiseNeverAlertsAtTheHighestSensitivity() {
        val phone = armedPhone(vibrationSensitivity = 1.0f)
        phone.rest(300_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun vibrationReArmsAfterTheNextRest() {
        val phone = armedPhone()
        phone.knocks()
        phone.rest(MOTION_ARM_AFTER_STILL_MS - 5_000)
        phone.knocks()
        assertEquals("Knocks before the rest ends are the same disturbance", 1, phone.alerts.size)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        assertTrue(phone.sentinel.isVibrationArmed)
        phone.knocks()
        assertEquals(listOf(MotionSentinel.Kind.VIBRATION, MotionSentinel.Kind.VIBRATION), phone.alerts.map { it.kind })
    }

    @Test
    fun aMovingPhoneNeverArmsVibration() {
        val phone = Phone()
        // A driving car: a 0.6 g bump every 2 s for 3 minutes.
        repeat(90) {
            phone.knock()
            phone.rest(1_800)
        }
        assertFalse(phone.sentinel.isVibrationArmed)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    @Test
    fun aShakenPickupIsBothVibrationAndLift() {
        val phone = armedPhone()
        // Grabbed roughly: [VIBRATION_HITS] sideways jerks of 6 m/s² for 0.1 s, 0.4 s apart (ISSUE-NOV25.25-O162: four
        // since then; three upward jerks during the rise before), then up 32 cm as in aPickupOf30CmAlertsAsLift.
        repeat(VIBRATION_HITS) {
            phone.jolt(6.0)
            phone.rest(200)
        }
        phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 2.0 else -2.0) }
        phone.rest(LIFT_HOLD_MS + 500)
        assertEquals(setOf(MotionSentinel.Kind.VIBRATION, MotionSentinel.Kind.LIFT), phone.alerts.map { it.kind }.toSet())
        assertEquals(2, phone.alerts.size)
    }

    /** ISSUE-NOV17.10-O111: a tilt must last [TILT_CONFIRM_MS]; tipped and set back sooner is no tilt. */
    @Test
    fun aTiltSetBackWithinTwoSecondsDoesNotAlert() {
        val phone = armedPhone()
        phone.rest(TILT_CONFIRM_MS - 500, angleDeg = 25.0)
        phone.rest(5_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        phone.rest(TILT_CONFIRM_MS + 500, angleDeg = 25.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.tiltOrLift.map { it.kind })
    }

    @Test
    fun aTiltLeavesVibrationArmed() {
        val phone = armedPhone()
        phone.tiltSlowly(20.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.alerts.map { it.kind })
        assertTrue(phone.sentinel.isVibrationArmed)
    }

    @Test
    fun afterAnAlertItReArmsAtTheNewPlace() {
        val phone = armedPhone()
        // Turned 30 deg in one sample.
        phone.rest(TILT_CONFIRM_MS + 500, angleDeg = 30.0)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.tiltOrLift.map { it.kind })
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000, angleDeg = 30.0)
        assertTrue("Re-armed lying at 30 deg", phone.sentinel.isArmed)
        assertEquals("No new alert for the same tilt", 1, phone.tiltOrLift.size)
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

    // Sit (ISSUE-NOV25.22-O150): the phone lies on or under a seat and someone sits down.

    /** The seat sinks [dropM] under the phone in [overMs]: down at a steady rate, then braked the same way. */
    private fun Phone.sink(dropM: Double, overMs: Long = 400, angleDeg: Double = 0.0) {
        val half = overMs / 2
        val a = dropM / ((half / 1000.0) * (half / 1000.0)) // a·T² for T = half.
        feed(overMs) { t ->
            val r = tilted(angleDeg * t / overMs)
            val scale = (g + if (t < half) -a else a) / g
            doubleArrayOf(r[0] * scale, r[1] * scale, r[2] * scale)
        }
    }

    private val Phone.sits get() = alerts.filter { it.kind == MotionSentinel.Kind.SIT }

    /**
     * Someone sits down or stands up over the phone under a seat cushion, as measured on the A15 (Nov25.23): a jolt of
     * 0.5 g with the cushion first pressing the phone down 0.3 m/s² ([isDownFirst]) or first letting it spring up, while
     * it tips from [fromDeg] to [toDeg]; then rest there.
     */
    private fun Phone.cushion(fromDeg: Double, toDeg: Double, isDownFirst: Boolean = true, restMs: Long = SIT_REST_MS + 800) {
        val first = if (isDownFirst) -0.3 else 0.3
        feed(300) { t ->
            val r = tilted(fromDeg + (toDeg - fromDeg) * t / 300)
            val scale = (g + if (t < 100) first else if (t < 200) -first else 0.0) / g
            doubleArrayOf(r[0] * scale, r[1] * scale + if (t < 100) 5.0 else 0.0, r[2] * scale)
        }
        rest(restMs, angleDeg = toDeg)
    }

    @Test
    fun aSitOnTheCushionIsASit() {
        val phone = armedPhone()
        phone.cushion(0.0, 1.5)
        assertEquals(listOf(MotionSentinel.Kind.SIT), phone.tiltOrLift.map { it.kind })
        val detail = phone.sits.single().detail
        assertTrue(detail, detail.startsWith("first move down 0."))
        assertTrue(detail, detail.contains("jolt 0.5"))
        // ISSUE-NOV25.23-O155: a sit opens a sitting period instead of disarming; tilt keeps the position before it.
        assertTrue("A sit does not disarm", phone.sentinel.isArmed)
    }

    /** The A15's sit at 23:24:02 tipped it only 0.1 degrees. */
    @Test
    fun aSitThatDoesNotTipThePhoneIsASitToo() {
        val phone = armedPhone()
        phone.cushion(0.0, 0.0)
        assertEquals(listOf(MotionSentinel.Kind.SIT), phone.tiltOrLift.map { it.kind })
    }

    /** Under a cushion standing up jolts and tips the phone as much as sitting down; it moves up first (A15, Nov25.23). */
    @Test
    fun standingUpIsNoSit() {
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.cushion(0.0, 2.0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000, angleDeg = 2.0) // Re-armed under the sitter.
        phone.cushion(2.0, 0.0, isDownFirst = false)
        assertEquals("Only the sit", 1, phone.sits.size)
        assertTrue("$notes", notes.any { it.startsWith("Stand-up in the sitting period") }) // ISSUE-NOV25.23-O155
        assertTrue("Standing up does not disarm", phone.sentinel.isArmed)
        phone.rest(SIT_STOOD_UP_EMPTY_MS + 5_000) // The empty seat ends the sitting period (ISSUE-NOV25.23-O155).
        phone.cushion(0.0, 2.0)
        assertEquals("The next sit alerts again", 2, phone.sits.size)
    }

    /** A hard knock on the chair's frame jolted the A15 1.2 to 5.7 g (Nov25.23); a cushion damps a sit to about 0.5 g. */
    @Test
    fun aHardKnockMovingThePhoneDownFirstIsNoSit() {
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.feed(200) { t -> doubleArrayOf(if (t < 40) 12.0 else 0.0, 0.0, g + if (t < 100) -1.0 else 1.0) }
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.sits.isEmpty())
        assertTrue("$notes", notes.any { it.startsWith("Sit not confirmed (too sharp") })
    }

    /** ISSUE-NOV25.23-O155 (user decision 2026-11-26): until then a second sit alerted again; now it is inside the period. */
    @Test
    fun aSecondSitInsideTheSittingPeriodRaisesNoNewAlert() {
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.cushion(0.0, 2.0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000, angleDeg = 2.0)
        phone.cushion(2.0, 4.0)
        assertEquals(1, phone.sits.size)
        assertTrue("$notes", notes.any { it.startsWith("Sit inside the sitting period, no new alert") })
    }

    @Test
    fun aSitThatTipsThePhoneOverTheTiltLimitIsATilt() {
        val phone = armedPhone()
        phone.cushion(0.0, 20.0, restMs = TILT_CONFIRM_MS + 500)
        assertEquals(listOf(MotionSentinel.Kind.TILT), phone.tiltOrLift.map { it.kind })
    }

    /** A sitter shaking the phone sideways (the A15 saw 0.37 g) adds nothing along the resting direction. */
    @Test
    fun aSitWithStrongSidewaysShakingIsASit() {
        val phone = armedPhone()
        phone.feed(1_000) { t ->
            phone.tilted(2.4 * minOf(t, 300L) / 300).also {
                it[2] += if (t < 100) -0.3 else if (t < 200) 0.3 else 0.0
                it[1] += if ((t / 40) % 2 == 0L) 3.6 else -3.6
            }
        }
        phone.rest(SIT_REST_MS + 800, angleDeg = 2.4)
        assertEquals(listOf(MotionSentinel.Kind.SIT), phone.tiltOrLift.map { it.kind })
    }

    @Test
    fun aPhonePickedUpAndPutBackTurnedIsNoSit() {
        val phone = armedPhone()
        // Up 10 cm and back down, set down 3 degrees turned, within a second.
        phone.feed(800) { t -> phone.tilted(3.0 * t / 800).also { it[2] += if (t < 200 || t >= 600) 2.5 else -2.5 } }
        phone.rest(3_000, angleDeg = 3.0)
        assertTrue("${phone.alerts}", phone.sits.isEmpty())
    }

    @Test
    fun aSlowSinkWithoutAJoltIsNoSit() {
        val phone = armedPhone()
        phone.sink(0.05, overMs = 2_000) // Peak speed 0.05 m/s, never a jolt.
        phone.rest(3_000)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
        assertTrue("Still armed", phone.sentinel.isArmed)
    }

    @Test
    fun aJoltUnderTheSitLimitIsNoSit() {
        val phone = armedPhone()
        phone.sink(0.015, overMs = 200) // Down first, but 0.15 g, as knocks on the chair (0.11 to 0.18 g on the A15).
        phone.rest(3_000)
        assertTrue("${phone.alerts}", phone.sits.isEmpty())
    }

    @Test
    fun aSitBeforeThePhoneHasRestedIsNotDetected() {
        val phone = Phone()
        phone.rest(MOTION_ARM_AFTER_STILL_MS - 5_000)
        phone.cushion(0.0, 2.0)
        phone.rest(3_000)
        assertTrue("${phone.alerts}", phone.sits.isEmpty())
    }

    @Test
    fun aJoltWithoutASignLeavesANoteForTuning() {
        val notes = mutableListOf<String>()
        val phone = Phone(MotionSentinel(onNote = { notes += it })).apply { rest(MOTION_ARM_AFTER_STILL_MS + 1_000) }
        phone.jolt(3.0)
        phone.rest(2_000)
        assertTrue("${phone.alerts}", phone.sits.isEmpty())
        // 0.31 g sideways: no move along the resting direction.
        assertTrue("$notes", notes.single { it.startsWith("Sit not") }.startsWith("Sit not confirmed (no clear first move): "))
        assertTrue("$notes", notes.single { it.startsWith("Sit trace") }.contains("up cm/s2: "))
        phone.cushion(0.0, 2.0)
        assertEquals("A confirmed sit leaves no 'not confirmed' note", 1, notes.count { it.startsWith("Sit not") })
        assertEquals("Each check with a force leaves its trace", 2, notes.count { it.startsWith("Sit trace") })
    }

    @Test
    fun onAPhoneWithABarometerAHeightDropOf15CmWithAJoltIsASit() {
        val phone = Phone()
        phone.sentinel.onPressure(1013.25, 0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25, 5_000)
        phone.jolt(3.0) // 0.31 g sideways: no first move for the accelerometer.
        // 15 cm lower is 0.018 hPa more.
        phone.sentinel.feedPressure(phone, 1013.268, 1013.268, SIT_WINDOW_MS)
        assertEquals(listOf(MotionSentinel.Kind.SIT), phone.tiltOrLift.map { it.kind })
        assertTrue(phone.sits.single().detail, phone.sits.single().detail.contains("barometer drop "))
    }

    @Test
    fun aBarometerDropWithoutAJoltIsNoSit() {
        val phone = Phone()
        phone.sentinel.onPressure(1013.25, 0)
        phone.rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        phone.sentinel.feedPressure(phone, 1013.25, 1013.25, 5_000)
        phone.sentinel.feedPressure(phone, 1013.268, 1013.268, SIT_WINDOW_MS)
        assertTrue("${phone.alerts}", phone.alerts.isEmpty())
    }

    // Sitting period (ISSUE-NOV25.23-O155, user decisions 2026-11-26).

    private class SeatedPhone {
        val notes = mutableListOf<String>()
        val events = mutableListOf<SitPeriodEvent>()
        val phone = Phone(MotionSentinel(onNote = { notes += it }, onSitPeriod = { events += it })).apply {
            rest(MOTION_ARM_AFTER_STILL_MS + 1_000)
        }
        val kinds get() = events.map { it.kind }
    }

    /** A sitter shifting: the phone rocks [amplitudeDeg] around [baseDeg] every 4 s, too slowly to open a sit check. */
    private fun Phone.fidget(durationMs: Long, baseDeg: Double = 2.0, amplitudeDeg: Double = 0.5) =
        feed(durationMs) { t -> tilted(baseDeg + amplitudeDeg * sin(2 * Math.PI * t / 4_000.0)) }

    @Test
    fun aSitterMovingWithinAMinuteConfirmsTheSit() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        assertEquals(1, seat.phone.sits.size)
        seat.phone.rest(15_000, angleDeg = 2.0)
        seat.phone.fidget(25_000)
        assertEquals(listOf(SitPeriodEvent.Kind.CONFIRMED), seat.kinds)
        assertTrue(seat.events.single().message { "" }, seat.events.single().message { "" }.startsWith("Sit confirmed: the sitter moved "))
    }

    @Test
    fun aStillSitterLeavesTheSitUnconfirmedAndThePeriodOpen() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        seat.phone.rest(SIT_CONFIRM_MS + 1_000, angleDeg = 2.0)
        assertEquals(listOf(SitPeriodEvent.Kind.UNCONFIRMED), seat.kinds)
        assertEquals("Sit unconfirmed: no movement within 1 min (an empty seat or a still sitter)", seat.events.single().message { "" })
        seat.phone.cushion(2.0, 3.0) // Shifting, or sitting down again: still the same period.
        assertEquals(1, seat.phone.sits.size)
    }

    @Test
    fun aStandUpFollowedByTwoQuietMinutesEndsThePeriodAtTheStandUp() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        val sitMs = seat.phone.nowMs
        seat.phone.fidget(40_000)
        seat.phone.rest(5_000, angleDeg = 2.0)
        val standUpMs = seat.phone.nowMs
        seat.phone.cushion(2.0, 0.0, isDownFirst = false)
        assertTrue("${seat.notes}", seat.notes.any { it.startsWith("Stand-up in the sitting period") })
        seat.phone.rest(SIT_STOOD_UP_EMPTY_MS - 10_000)
        assertEquals("Not yet", listOf(SitPeriodEvent.Kind.CONFIRMED), seat.kinds)
        seat.phone.rest(15_000)
        val end = seat.events.last()
        assertEquals(SitPeriodEvent.Kind.ENDED_STOOD_UP, end.kind)
        assertEquals("Dated to the stand-up", standUpMs, end.atMs)
        assertTrue("From the sit", end.startMs in sitMs - 2_000..sitMs)
        assertEquals("Sit ended: seated 1 min (A to B)", end.message { if (it == end.startMs) "A" else "B" })
        seat.phone.cushion(0.0, 2.0)
        assertEquals("After the end the next sit alerts again", 2, seat.phone.sits.size)
    }

    @Test
    fun aMovementAfterTheStandUpMeansStillSeated() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        seat.phone.rest(5_000, angleDeg = 2.0)
        seat.phone.cushion(2.0, 1.0, isDownFirst = false) // Leaning forward, moving up first.
        seat.phone.rest(15_000, angleDeg = 1.0)
        seat.phone.fidget(25_000, baseDeg = 1.0)
        seat.phone.rest(SIT_STOOD_UP_EMPTY_MS, angleDeg = 1.0)
        assertTrue("${seat.notes}", seat.notes.any { it.contains("after the stand-up, still seated") })
        assertFalse("${seat.kinds}", seat.kinds.any { it == SitPeriodEvent.Kind.ENDED_STOOD_UP || it == SitPeriodEvent.Kind.ENDED_QUIET })
    }

    @Test
    fun fifteenMinutesWithoutMovementEndThePeriodAtTheLastMovement() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        seat.phone.rest(15_000, angleDeg = 2.0)
        seat.phone.fidget(25_000)
        val movedBy = seat.phone.nowMs
        seat.phone.rest(SIT_QUIET_END_MS + 11_000, angleDeg = 2.0)
        val end = seat.events.last()
        assertEquals(SitPeriodEvent.Kind.ENDED_QUIET, end.kind)
        assertTrue("Dated to the last movement window", end.atMs in movedBy - 10_000..movedBy + 10_000)
        assertTrue(end.message { "" }, end.message { "" }.startsWith("Sit ended: no movement for 15 min; seated "))
    }

    /** Nov26.3 on the A15: a sit raised a vibration alert too (4 hits in 2.1 s, 0.16 g). */
    @Test
    fun aSitsOwnVibrationIsDropped() {
        val seat = SeatedPhone()
        seat.phone.feed(1_200) { t ->
            seat.phone.tilted(1.5 * minOf(t, 300L) / 300).also {
                it[2] += if (t < 100) -0.3 else if (t < 200) 0.3 else 0.0
                if (t % 350 < 40) it[1] += 3.0 // 0.31 g sideways, four hits.
            }
        }
        seat.phone.rest(SIT_REST_MS + 800, angleDeg = 1.5)
        assertEquals(listOf(MotionSentinel.Kind.SIT), seat.phone.alerts.map { it.kind })
        assertTrue("${seat.notes}", seat.notes.any { it == "Vibration alert dropped: the jolts of a sit" })
    }

    @Test
    fun withSitSwitchedOffASitsVibrationStillAlertsAndNoPeriodOpens() {
        val seat = SeatedPhone()
        seat.phone.sentinel.isSitEnabled = false
        seat.phone.feed(1_200) { t ->
            seat.phone.tilted(1.5 * minOf(t, 300L) / 300).also {
                it[2] += if (t < 100) -0.3 else if (t < 200) 0.3 else 0.0
                if (t % 350 < 40) it[1] += 3.0
            }
        }
        seat.phone.rest(SIT_CONFIRM_MS + 1_000, angleDeg = 1.5)
        assertEquals(setOf(MotionSentinel.Kind.SIT, MotionSentinel.Kind.VIBRATION), seat.phone.alerts.map { it.kind }.toSet())
        assertTrue("No period events", seat.events.isEmpty())
    }

    @Test
    fun insideThePeriodOnlyBlowsCountAsVibration() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        seat.phone.rest(2_000, angleDeg = 2.0)
        seat.phone.knocks() // 0.6 g: a sitter's movement.
        assertTrue("${seat.phone.alerts}", seat.phone.alerts.none { it.kind == MotionSentinel.Kind.VIBRATION })
        seat.phone.jolts(12.0) // 1.2 g: a blow.
        seat.phone.rest(SIT_WINDOW_MS) // Held while the sit check the blows opened is undecided.
        assertTrue("${seat.phone.alerts}", seat.phone.alerts.any { it.kind == MotionSentinel.Kind.VIBRATION })
    }

    @Test
    fun insideThePeriodTheAccelerometerLiftIsOnlyNoted() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 0.0)
        seat.phone.rest(5_000)
        seat.phone.feed(800) { t -> doubleArrayOf(0.0, 0.0, g + if (t < 400) 2.0 else -2.0) } // 32 cm up, held.
        seat.phone.rest(LIFT_HOLD_MS + 500)
        assertEquals(listOf(MotionSentinel.Kind.SIT), seat.phone.tiltOrLift.map { it.kind })
        assertTrue("${seat.notes}", seat.notes.any { it.startsWith("Lift not alarmed inside the sitting period") })
    }

    @Test
    fun insideThePeriodANewRestingPositionAlarms() {
        val seat = SeatedPhone()
        seat.phone.cushion(0.0, 2.0)
        seat.phone.rest(5_000, angleDeg = 2.0)
        seat.phone.tiltSlowly(7.0) // Under the 15 degree limit, but no seat tips it so far.
        seat.phone.rest(MOTION_ARM_AFTER_STILL_MS, angleDeg = 7.0)
        val tilt = seat.phone.alerts.single { it.kind == MotionSentinel.Kind.TILT }
        assertTrue(tilt.detail, tilt.detail.startsWith("new resting position, 7.0 deg"))
    }

    /** Chapter 191 (ISSUE-NOV10.8-O84 part 2): the values the Info screens show. */
    @Test
    fun infoValuesFollowTheMotion() {
        val phone = Phone()
        assertNull("No pressure without a barometer", phone.sentinel.pressureHpa)
        phone.rest(5_000)
        assertNull("No tilt before arming", phone.sentinel.tiltDegrees)
        assertTrue("A resting phone barely vibrates", phone.sentinel.vibration < 0.1)

        phone.rest(MOTION_ARM_AFTER_STILL_MS)
        assertEquals("Armed: the tilt from the resting position", 0.0, phone.sentinel.tiltDegrees!!, 1.0)
        phone.tiltSlowly(8.0) // Below the default 15 degrees limit.
        assertEquals(8.0, phone.sentinel.tiltDegrees!!, 1.0)

        phone.knock()
        val peak = phone.sentinel.vibrationPeak
        assertTrue("The knock is the peak: $peak", peak > 5.0)
        phone.rest(VIBRATION_PEAK_WINDOW_MS / 2)
        assertEquals("The peak stays within its window", peak, phone.sentinel.vibrationPeak, 1e-9)
        phone.rest(VIBRATION_PEAK_WINDOW_MS)
        assertTrue("Then it falls back to the resting level", phone.sentinel.vibrationPeak < 0.1)

        phone.sentinel.onPressure(1013.0, phone.nowMs)
        assertEquals(1013.0, phone.sentinel.pressureHpa!!, 1e-9)
    }
}
