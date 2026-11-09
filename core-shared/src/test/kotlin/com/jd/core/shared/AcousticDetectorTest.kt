package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 161 (ISSUE-NOV5.2-O11 Group 4): the Sensor's noise detection. */
class AcousticDetectorTest {

    /** Feeds [level] every 100 ms from [fromMs] for [durationMs]; returns the alert times. */
    private fun AcousticDetector.feed(level: Double, fromMs: Long, durationMs: Long): List<Long> =
        (fromMs until fromMs + durationMs step ACOUSTIC_FRAME_MS).filter { onLevel(level, it) }

    private fun quietRoom(): AcousticDetector = AcousticDetector().apply { feed(-70.0, 0, 5_000) }

    @Test
    fun aJumpOf30DbAboveTheBackgroundAlerts() {
        val detector = quietRoom()
        assertTrue(detector.onLevel(-40.0, 5_000))
    }

    @Test
    fun aJumpBelow30DbDoesNotAlert() {
        val detector = quietRoom()
        assertFalse(detector.onLevel(-41.0, 5_000))
    }

    @Test
    fun noAlertWhileTheBackgroundIsFirstLearned() {
        val detector = AcousticDetector()
        detector.onLevel(-70.0, 0)
        assertFalse(detector.onLevel(-20.0, 1_000))
    }

    @Test
    fun aJumpThatStaysQuietDoesNotAlert() {
        val detector = AcousticDetector().apply { feed(-90.0, 0, 5_000) }
        assertFalse("-60 dBFS is 30 dB above digital silence, but still a whisper", detector.onLevel(-60.0, 5_000))
        assertTrue(detector.onLevel(-50.0, 5_100))
    }

    @Test
    fun oneLongNoiseRaisesOneAlertAndBecomesBackground() {
        val detector = quietRoom()
        val alerts = detector.feed(-30.0, 5_000, 60_000)
        assertEquals(listOf(5_000L), alerts)
        assertTrue("The noise is background after a minute", detector.floorDbfs!! > -32.0)
    }

    @Test
    fun aSecondNoiseAfterTheLockoutAlertsAgain() {
        val detector = quietRoom()
        assertTrue(detector.onLevel(-30.0, 5_000))
        detector.feed(-70.0, 5_100, 4_900)
        assertFalse("Within the lockout", detector.onLevel(-30.0, 10_000))
        detector.feed(-70.0, 10_100, 5_000)
        assertTrue(detector.onLevel(-30.0, 15_100))
    }

    @Test
    fun theBackgroundFollowsAQuieterRoomQuickly() {
        val detector = AcousticDetector().apply { feed(-50.0, 0, 5_000) }
        detector.feed(-75.0, 5_000, 5_000)
        assertTrue(detector.floorDbfs!! < -74.0)
        assertTrue("-44 dBFS was 6 dB above the old background, and is now over 30 dB above the new one", detector.onLevel(-44.0, 10_000))
    }

    @Test
    fun levelOfAllZeroSamplesIsNoSound() {
        assertNull(acousticLevelDbfs(ShortArray(1600)))
        assertNull(acousticLevelDbfs(ShortArray(0)))
    }

    @Test
    fun levelOfAFullScaleSquareWaveIsZeroDbfs() {
        val samples = ShortArray(1600) { if (it % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE }
        assertEquals(0.0, acousticLevelDbfs(samples)!!, 0.01)
    }

    @Test
    fun levelOfATenthOfFullScaleIsMinus20Dbfs() {
        val samples = ShortArray(1600) { if (it % 2 == 0) 3277 else -3277 }
        assertEquals(-20.0, acousticLevelDbfs(samples)!!, 0.01)
    }

    @Test
    fun levelCountsOnlyTheSamplesRead() {
        val samples = ShortArray(1600) { if (it < 800) 3277 else 0 }
        assertEquals(-20.0, acousticLevelDbfs(samples, 800)!!, 0.01)
    }

    @Test
    fun theQuietestLevelIsClamped() {
        val samples = ShortArray(1600).also { it[0] = 1 }
        assertEquals(ACOUSTIC_MIN_LEVEL_DBFS, acousticLevelDbfs(samples)!!, 0.0)
    }
}
