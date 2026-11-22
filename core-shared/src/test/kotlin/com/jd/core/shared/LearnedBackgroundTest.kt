package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

/**
 * Chapter 246 (SIMP-NOV22.10-S1): the background the noise and the light detection both learn, fed spans; and the
 * two detectors use it, so a noise and a light that follow the same course learn the same background.
 */
class LearnedBackgroundTest {

    /** Starts at [start] at 0, then feeds [value] in spans of [stepMs] up to [untilMs]. */
    private fun background(start: Double, value: Double, untilMs: Long, stepMs: Long = 100L): LearnedBackground =
        LearnedBackground().apply {
            start(start, 0L)
            var nowMs = stepMs
            while (nowMs <= untilMs) {
                follow(value, nowMs, stepMs)
                nowMs += stepMs
            }
        }

    @Test
    fun noBackgroundBeforeTheFirstValue() {
        val background = LearnedBackground()
        assertNull(background.level)
        assertTrue(background.isWarmingUp(0L))
        background.follow(10.0, 100L, 100L)
        assertNull("follow before start is ignored", background.level)
    }

    @Test
    fun theWarmUpLastsThreeSecondsFromTheFirstValue() {
        val background = LearnedBackground().apply { start(1.0, 1_000L) }
        assertTrue(background.isWarmingUp(1_000L + BACKGROUND_WARM_UP_MS - 1))
        assertFalse(background.isWarmingUp(1_000L + BACKGROUND_WARM_UP_MS))
    }

    @Test
    fun aFallIsFollowedWithinAboutASecondAndARiseOverAboutTenSeconds() {
        assertEquals(-10.0, background(start = 0.0, value = -10.0, untilMs = 20_000L).level!!, 0.01)
        assertEquals("A minute of a rise is learned", 10.0, background(start = 0.0, value = 10.0, untilMs = 60_000L).level!!, 0.1)
        // After the warm-up: one second down, ten seconds up, each about 63 % of the way.
        val down = LearnedBackground(warmUpMs = 0L).apply { start(0.0, 0L); follow(-10.0, 1_000L, 1_000L) }
        assertEquals(-10.0 * (1 - exp(-1.0)), down.level!!, 1e-9)
        val up = LearnedBackground(warmUpMs = 0L).apply { start(0.0, 0L); follow(10.0, 10_000L, 10_000L) }
        assertEquals(10.0 * (1 - exp(-1.0)), up.level!!, 1e-9)
    }

    @Test
    fun theWarmUpFollowsQuicklyInBothDirections() {
        val up = LearnedBackground().apply { start(0.0, 0L); follow(10.0, 500L, 500L) }
        assertEquals(10.0 * (1 - exp(-1.0)), up.level!!, 1e-9)
    }

    /** The same course in short or long spans learns about the same background: it depends on time, not on calls. */
    @Test
    fun theBackgroundDependsOnTimeNotOnTheSpanLength() {
        val fine = LearnedBackground(warmUpMs = 0L).apply { start(0.0, 0L); (1..50).forEach { follow(10.0, it * 100L, 100L) } }
        val coarse = LearnedBackground(warmUpMs = 0L).apply { start(0.0, 0L); follow(10.0, 5_000L, 5_000L) }
        assertEquals(coarse.level!!, fine.level!!, 1e-9)
    }

    @Test
    fun anEmptySpanChangesNothing() {
        val background = LearnedBackground().apply { start(5.0, 0L); follow(100.0, 0L, 0L) }
        assertEquals(5.0, background.level!!, 0.0)
    }

    /** The noise (100 ms frames) and the light (held readings, fed every 250 ms) learn the same background. */
    @Test
    fun theNoiseAndTheLightLearnTheSameBackground() {
        val noise = AcousticDetector()
        val light = LightDetector()
        // A frame is the level of the 100 ms up to its time; a reading holds from its time: both 10 until 5 s, then 20.
        (0L..10_000L step ACOUSTIC_FRAME_MS).forEach { noise.onLevel(if (it <= 5_000L) 10.0 else 20.0, it) }
        (0L..10_000L step LIGHT_TICK_MS).forEach { light.onLux(if (it < 5_000L) 10.0 else 20.0, it) }
        assertTrue("The background rose towards 20", noise.floorDbfs!! in 13.0..17.0)
        assertEquals(light.floorLux!!, noise.floorDbfs!!, 1e-9)
    }
}
