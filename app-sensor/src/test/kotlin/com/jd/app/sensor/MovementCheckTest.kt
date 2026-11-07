package com.jd.app.sensor

import com.jd.core.shared.SentinelLogic.SentinelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies Chapter 139.2: the Sensor checks movement once per new fix, timed by the fixes (ISSUE-NOV7.17-O42). */
class MovementCheckTest {

    private val second = 1_000_000_000L // elapsedRealtimeNanos

    // The two A15 fixes behind the false "OUTLIER - Speed Outlier: 346 m/s" of 2026-11-07 19:46:12 (about 34.5 m apart).
    private fun networkFix(atNanos: Long) = fixForMovementCheck(32.652890, 35.301359, 82.5f, 0f, atNanos)
    private fun gpsFix(atNanos: Long) = fixForMovementCheck(32.653092, 35.301082, 14.3f, 0f, atNanos)

    @Test
    fun theFirstFixIsValid() {
        assertEquals(SentinelStatus.VALID, MovementCheck().check(gpsFix(10 * second), hasPhysicalMotion = false)?.status)
    }

    @Test
    fun aFixSentAgainOnTheNextTicksIsNotCheckedAgain() {
        val check = MovementCheck()
        check.check(gpsFix(10 * second), hasPhysicalMotion = false)

        assertNull(check.check(gpsFix(10 * second), hasPhysicalMotion = false))
        assertNull(check.check(networkFix(9 * second), hasPhysicalMotion = false))
    }

    @Test
    fun theCoarseToGpsCorrectionSeenOnTheA15IsValid() {
        val check = MovementCheck()
        check.check(networkFix(10 * second), hasPhysicalMotion = false)

        // One second is the shortest fix interval the Sensor requests (setMinUpdateIntervalMillis(1000));
        // the 34.5 m lie inside the 82.5 m error of the network fix.
        assertEquals(SentinelStatus.VALID, check.check(gpsFix(11 * second), hasPhysicalMotion = false)?.status)
    }

    @Test
    fun timingByTheFixesKeepsAStillPhoneValid() {
        val check = MovementCheck()
        check.check(gpsFix(10 * second), hasPhysicalMotion = false)
        // About 6 m of drift (0.00005 degrees of latitude) between two fixes 5 s apart is 1.1 m/s, not a jump.
        val drifted = fixForMovementCheck(32.653142, 35.301082, 14.3f, 0f, 15 * second)

        assertEquals(SentinelStatus.VALID, check.check(drifted, hasPhysicalMotion = false)?.status)
    }

    @Test
    fun aRealJumpBetweenFixesIsStillReported() {
        val check = MovementCheck()
        check.check(gpsFix(10 * second), hasPhysicalMotion = false)
        // About 1.1 km in 5 s is 222 m/s.
        val far = fixForMovementCheck(32.663092, 35.301082, 14.3f, 0f, 15 * second)

        assertEquals(SentinelStatus.OUTLIER, check.check(far, hasPhysicalMotion = true)?.status)
    }

    @Test
    fun theCellFixAfterAGpsLossSeenOnTheA15IsNotJudged() {
        // ISSUE-NOV7.19-O43, A15 2026-11-07 20:38: location off for 30 s on a still phone, then a cell fix 3352 m off at
        // 700 m accuracy (reported JUMP 97.5 m/s), then a GPS fix back home (reported OUTLIER 488 m/s).
        val check = MovementCheck()
        check.check(gpsFix(10 * second), hasPhysicalMotion = false)
        val cellFix = fixForMovementCheck(32.683092, 35.301082, 700f, 0f, 44 * second)

        assertNull("A cell fix cannot tell a movement", check.check(cellFix, hasPhysicalMotion = false))
        assertEquals(
            "The GPS fix after it is compared with the last usable fix",
            SentinelStatus.VALID,
            check.check(gpsFix(50 * second), hasPhysicalMotion = false)?.status
        )
    }

    @Test
    fun aFixWithoutAccuracyIsNotJudged() {
        val check = MovementCheck()
        assertNull(check.check(fixForMovementCheck(32.65, 35.30, 0f, 0f, 10 * second), hasPhysicalMotion = false))
        assertEquals(SentinelStatus.VALID, check.check(gpsFix(11 * second), hasPhysicalMotion = false)?.status)
    }

    @Test
    fun aRealJumpAcrossAGpsLossIsStillReported() {
        val check = MovementCheck()
        check.check(gpsFix(10 * second), hasPhysicalMotion = false)
        check.check(fixForMovementCheck(32.683092, 35.301082, 700f, 0f, 44 * second), hasPhysicalMotion = false)
        // About 11 km (0.1 degrees of latitude) in 40 s is 278 m/s.
        val far = fixForMovementCheck(32.753092, 35.301082, 14.3f, 0f, 50 * second)

        assertEquals(SentinelStatus.OUTLIER, check.check(far, hasPhysicalMotion = true)?.status)
    }

    @Test
    fun theFixTimeComesFromTheMonotonicClockInMilliseconds() {
        assertEquals(12_345L, fixForMovementCheck(1.0, 2.0, 3f, 4f, 12_345_000_000L).timestamp)
    }
}
