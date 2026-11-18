package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 161 (ISSUE-NOV5.2-O11 Group 4): the Sensor's noise detection; chapter 211 (ISSUE-NOV17.10-O111): its
 * debouncing; chapter 220 (ISSUE-NOV17.12-O112): 2 s loud within 3 s, so a coughing fit does not alert.
 */
class AcousticDetectorTest {

    /** Feeds [level] every 100 ms from [fromMs] for [durationMs]; returns the alert times. */
    private fun AcousticDetector.feed(level: Double, fromMs: Long, durationMs: Long): List<Long> =
        (fromMs until fromMs + durationMs step ACOUSTIC_FRAME_MS).filter { onLevel(level, it) }

    private fun quietRoom(): AcousticDetector = AcousticDetector().apply { feed(-70.0, 0, 5_000) }

    /** The frame that completes [ACOUSTIC_CONFIRM_MS] of loud frames from [fromMs]. */
    private fun confirmedAt(fromMs: Long) = fromMs + ACOUSTIC_CONFIRM_MS - ACOUSTIC_FRAME_MS

    @Test
    fun aJumpOf30DbAboveTheBackgroundForTwoSecondsAlerts() {
        val detector = quietRoom()
        assertEquals(listOf(confirmedAt(5_000)), detector.feed(-40.0, 5_000, ACOUSTIC_CONFIRM_MS))
    }

    @Test
    fun aJumpBelow30DbDoesNotAlert() {
        val detector = quietRoom()
        assertTrue(detector.feed(-41.0, 5_000, 3_000).isEmpty())
    }

    @Test
    fun noAlertWhileTheBackgroundIsFirstLearned() {
        val detector = AcousticDetector()
        detector.onLevel(-70.0, 0)
        assertTrue(detector.feed(-20.0, 100, ACOUSTIC_WARM_UP_MS - 200).isEmpty())
    }

    @Test
    fun aJumpThatStaysQuietDoesNotAlert() {
        val detector = AcousticDetector().apply { feed(-90.0, 0, 5_000) }
        assertTrue("-60 dBFS is 30 dB above digital silence, but still a whisper", detector.feed(-60.0, 5_000, 3_000).isEmpty())
        assertEquals(listOf(confirmedAt(8_000)), detector.feed(-50.0, 8_000, ACOUSTIC_CONFIRM_MS))
    }

    /** ISSUE-NOV17.10-O111: a door slam, a dropped spoon: loud, but shorter than [ACOUSTIC_CONFIRM_MS]. */
    @Test
    fun aShortBangDoesNotAlert() {
        val detector = quietRoom()
        var nowMs = 5_000L
        repeat(5) {
            assertTrue(detector.feed(-20.0, nowMs, ACOUSTIC_CONFIRM_MS - 2 * ACOUSTIC_FRAME_MS).isEmpty())
            nowMs += ACOUSTIC_CONFIRM_MS
            assertTrue(detector.feed(-70.0, nowMs, 5_000).isEmpty())
            nowMs += 5_000
        }
    }

    /** ISSUE-NOV17.10-O111: loud frames with gaps, like speech, count when they add up to two seconds within three. */
    @Test
    fun aNoiseWithGapsAlertsWhenItsLoudFramesAddUpToTwoSeconds() {
        val detector = quietRoom()
        // Two loud frames, one quiet: 20 loud frames take 2.9 s.
        val alerts = (0 until 40).filter { detector.onLevel(if (it % 3 != 2) -30.0 else -70.0, 5_000L + it * ACOUSTIC_FRAME_MS) }
        assertEquals("The 20th loud frame", listOf(28), alerts)
        // Sparser, the window passes first.
        val sparse = quietRoom()
        val none = (0 until 90).map { 5_000L + it * ACOUSTIC_FRAME_MS }.filter { sparse.onLevel(if (it / ACOUSTIC_FRAME_MS % 2 == 0L) -30.0 else -70.0, it) }
        assertTrue("$none", none.isEmpty())
    }

    /**
     * ISSUE-NOV17.12-O112: the user's coughs beside the A15 rose 39 to 43 dB over a background of -75 dBFS. A coughing
     * fit (0.3 s loud, 0.2 s pause) for ten seconds filled the old 1 s within 2 s; it never fills 2 s within 3 s.
     */
    @Test
    fun aCoughingFitDoesNotAlert() {
        val detector = AcousticDetector().apply { feed(-75.0, 0, 5_000) }
        val alerts = (0 until 100).map { 5_000L + it * ACOUSTIC_FRAME_MS }.filter { detector.onLevel(if (it / ACOUSTIC_FRAME_MS % 5 < 3L) -33.0 else -75.0, it) }
        assertTrue("$alerts", alerts.isEmpty())
    }

    @Test
    fun oneLongNoiseRaisesOneAlertAndBecomesBackground() {
        val detector = quietRoom()
        val alerts = detector.feed(-30.0, 5_000, 60_000)
        assertEquals(listOf(confirmedAt(5_000)), alerts)
        assertTrue("The noise is background after a minute", detector.floorDbfs!! > -32.0)
    }

    /** The time between two alerts is the Sensor's cool-down ([AlertCoolDown]); the detector only waits for quiet. */
    @Test
    fun aSecondNoiseAfterQuietAlertsAgain() {
        val detector = quietRoom()
        assertEquals(listOf(confirmedAt(5_000)), detector.feed(-30.0, 5_000, ACOUSTIC_CONFIRM_MS))
        val quietFromMs = 5_000 + ACOUSTIC_CONFIRM_MS
        detector.feed(-70.0, quietFromMs, 2_000)
        val againMs = quietFromMs + 2_000
        assertEquals(listOf(confirmedAt(againMs)), detector.feed(-30.0, againMs, ACOUSTIC_CONFIRM_MS))
    }

    @Test
    fun theBackgroundFollowsAQuieterRoomQuickly() {
        val detector = AcousticDetector().apply { feed(-50.0, 0, 5_000) }
        detector.feed(-75.0, 5_000, 5_000)
        assertTrue(detector.floorDbfs!! < -74.0)
        assertEquals(
            "-44 dBFS was 6 dB above the old background, and is now over 30 dB above the new one",
            listOf(confirmedAt(10_000)), detector.feed(-44.0, 10_000, ACOUSTIC_CONFIRM_MS)
        )
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
