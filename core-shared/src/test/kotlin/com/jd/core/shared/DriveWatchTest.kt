package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV26.0-O163 part 1: a drive is found from the Sensor's fixes, and its end asks about the seat placement. */
class DriveWatchTest {

    private val metersPerDegree = 111_195.0

    /** Feeds a fix [northM] north of a fixed point at [timeS] seconds. */
    private fun DriveWatch.fix(timeS: Int, northM: Double, speedKmh: Float?): Long? =
        onFix(timeS * 1000L, 32.0 + northM / metersPerDegree, 34.8, speedKmh?.let { it / 3.6f })

    /** Drives north at 54 km/h (15 m/s), one fix a second from [fromS] and [startM]; returns the last position. */
    private fun DriveWatch.drive(fromS: Int, seconds: Int, startM: Double = 0.0): Double {
        for (s in 0 until seconds) assertNull("${fromS + s} s", fix(fromS + s, startM + s * 15.0, 54f))
        return startM + (seconds - 1) * 15.0
    }

    /** Stays at [atM] reporting [speedKmh], one fix per 10 s from [fromS]; returns the first drive end seen, if any. */
    private fun DriveWatch.stay(fromS: Int, seconds: Int, atM: Double, speedKmh: Float? = 0f): Long? {
        var s = 0
        while (s < seconds) {
            fix(fromS + s, atM + (s / 10 % 3), speedKmh)?.let { return it }
            s += 10
        }
        return null
    }

    @Test
    fun aDriveEndsOnceParkedForFiveMinutes() {
        val watch = DriveWatch()
        val parkedAtM = watch.drive(0, 120)
        assertTrue("2 min at 54 km/h is a drive", watch.isDriving)
        assertNull("4 min parked: not yet", watch.stay(120, 240, parkedAtM))
        // The position stayed within 50 m since 118 s (the last 4-s step of 60 m), so parked since then.
        assertEquals("parked since its last move over 50 m", 118_000L, watch.stay(360, 120, parkedAtM))
        assertFalse(watch.isDriving)
        assertNull("asked once", watch.stay(480, 600, parkedAtM))
    }

    @Test
    fun aRedLightDoesNotEndTheDrive() {
        val watch = DriveWatch()
        val stopM = watch.drive(0, 60)
        assertNull("2 min at a red light", watch.stay(60, 120, stopM))
        val parkedAtM = watch.drive(180, 60, startM = stopM)
        assertTrue(watch.isDriving)
        assertEquals(239_000L, watch.stay(240, 600, parkedAtM))
    }

    @Test
    fun aLostGpsRepeatingTheLastFastFixStillParks() {
        // An underground car park: the rows repeat the last fix, still at 54 km/h, at one place.
        val watch = DriveWatch()
        val parkedAtM = watch.drive(0, 60)
        assertEquals(58_000L, watch.stay(60, 600, parkedAtM, speedKmh = 54f))
    }

    @Test
    fun aRestingPhonesSpeedSpikesAreNoDrive() {
        val watch = DriveWatch()
        // Rows repeating a 60 km/h spike at one place for 10 min.
        assertNull(watch.stay(0, 600, 0.0, speedKmh = 60f))
        assertFalse(watch.isDriving)
    }

    @Test
    fun walkingRunningOrCyclingIsNoDrive() {
        val watch = DriveWatch()
        for (s in 0 until 600) watch.fix(s, s * 7.0, 25f) // 25 km/h for 4.2 km
        assertFalse(watch.isDriving)
    }

    @Test
    fun fewFastFixesAreNoDriveAndStartOverAfterAPause() {
        val watch = DriveWatch()
        for (s in 0 until 4) watch.fix(s * 60, s * 300.0, 54f) // 4 fast fixes, 900 m apart from the first
        assertFalse("4 fixes", watch.isDriving)
        watch.fix(600, 2_000.0, 54f) // the fifth, 6 min after the fourth
        assertFalse("the count started over", watch.isDriving)
    }

    @Test
    fun anOlderFixIsIgnored() {
        val watch = DriveWatch()
        val parkedAtM = watch.drive(0, 60)
        assertNull(watch.fix(10, parkedAtM, 0f))
        assertTrue(watch.isDriving)
    }

    @Test
    fun noSpeedIsNoCarFix() {
        val watch = DriveWatch()
        for (s in 0 until 120) watch.fix(s, s * 15.0, null)
        assertFalse(watch.isDriving)
    }

    @Test
    fun theQuestionIsAskedOnlyWithoutACarSeatInUse() {
        val chair = AlertSettings()
        assertTrue(chair.asksAfterDrive())
        assertTrue(chair.carSeatPlacements().isEmpty())
        assertTrue(driveQuestionText(chair, "14:05").contains("no car seat placement yet"))

        val withCar = chair.withPlacementAdded("Driver's seat", SeatType.CAR_SEAT, nowMs = 5L)
        assertFalse("the added placement is active", withCar.asksAfterDrive())
        val backOnChair = withCar.withActivePlacement(DEFAULT_SEAT_PLACEMENT.id)
        assertTrue(backOnChair.asksAfterDrive())
        assertEquals(listOf("Driver's seat"), backOnChair.carSeatPlacements().map { it.name })
        val text = driveQuestionText(backOnChair, "14:05")
        assertTrue(text, text.contains("parked since 14:05") && text.contains("In use: Chair"))
    }
}
