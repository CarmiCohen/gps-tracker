package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** ISSUE-NOV25.23-O155: the stillness measures of a resting phone and of a slowly rocked one, 50 Hz. */
class StillnessMeterTest {

    private val g = 9.81

    private fun windows(durationMs: Long, sample: (tMs: Long) -> Pair<DoubleArray, Double>): List<StillnessMeter.Window> {
        val meter = StillnessMeter()
        val out = mutableListOf<StillnessMeter.Window>()
        var t = 0L
        while (t <= durationMs) {
            val (a, deviation) = sample(t)
            meter.onSample(a[0], a[1], a[2], deviation, t)?.let(out::add)
            t += 20L
        }
        return out
    }

    @Test
    fun `a resting phone gives one window per 10 s with only its noise`() {
        var flip = false
        val result = windows(20_000L) { flip = !flip; doubleArrayOf(0.0, 0.0, g + if (flip) 0.05 else -0.05) to 0.05 }
        assertEquals(2, result.size)
        val w = result.first()
        assertEquals(500, w.samples)
        assertEquals(5.1, w.rmsMg, 0.1) // 0.05 m/s² is 5.1 mg.
        assertEquals(0, w.over10Mg)
        assertTrue("The noise averages out of the 1 s means", w.slowSdMg < 0.01)
        assertTrue(w.slowTiltDeg < 0.001)
    }

    @Test
    fun `a slow rocking shows in the slow measures, not in the deviation`() {
        // 0.2 degrees at 0.25 Hz, like breathing: the 1 s means turn, the samples stay close to the slow gravity.
        val result = windows(10_000L) { t ->
            val r = Math.toRadians(0.2 * sin(2 * Math.PI * t / 4_000.0))
            doubleArrayOf(g * sin(r), 0.0, g * cos(r)) to 0.0
        }
        val w = result.single()
        assertEquals(0.0, w.rmsMg, 1e-9)
        assertTrue("Tilt ${w.slowTiltDeg}", w.slowTiltDeg > 0.15)
    }

    @Test
    fun `a reset starts a new window`() {
        val meter = StillnessMeter()
        for (t in 0L..8_000L step 20L) assertNull(meter.onSample(0.0, 0.0, g, 0.0, t))
        meter.reset()
        for (t in 20_000L..29_980L step 20L) assertNull(meter.onSample(0.0, 0.0, g, 0.0, t))
        assertNotNull(meter.onSample(0.0, 0.0, g, 0.0, 30_000L))
    }
}
