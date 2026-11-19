package com.jd.core.ui

import androidx.compose.ui.graphics.Color
import com.jd.core.shared.EventSource
import com.jd.core.shared.GraphTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Verifies Chapter 229: the Monitor's color cannot be taken for the Sensor's (ISSUE-NOV19.2-O123),
 * and the lines of one graph stay apart.
 */
class DeviceColorsTest {

    /** The hue of a color in degrees, 0 to 360. */
    private fun hue(color: Color): Float {
        val r = color.red; val g = color.green; val b = color.blue
        val hi = max(r, max(g, b)); val lo = min(r, min(g, b)); val d = hi - lo
        if (d == 0f) return 0f
        val h = when (hi) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return if (h < 0f) h + 360f else h
    }

    private fun hueDistance(a: Color, b: Color): Float {
        val d = abs(hue(a) - hue(b))
        return min(d, 360f - d)
    }

    @Test
    fun theMonitorAndTheSensorColorsAreFarApart() {
        // The old cyan #06B6D4 was 29 degrees from the green; the brown is about 135.
        assertTrue(hueDistance(MonitorBrown, SensorGreen) >= 90f)
    }

    @Test
    fun theMonitorColorIsOrangeBrown() {
        val h = hue(MonitorBrown)
        assertTrue("hue $h is not orange", h in 15f..40f)
        assertTrue("not dark enough for brown", max(MonitorBrown.red, max(MonitorBrown.green, MonitorBrown.blue)) < 0.9f)
    }

    @Test
    fun theEventLogMarksEachDeviceInItsColor() {
        assertEquals(MonitorBrown, sourceColor(EventSource.MONITOR))
        assertEquals(SensorGreen, sourceColor(EventSource.SENSOR))
    }

    @Test
    fun theLinesOfOneGraphAreFarApart() {
        val tones = GraphTone.entries.map(::toneColor)
        for (i in tones.indices) for (j in i + 1 until tones.size) {
            assertTrue("tones $i and $j too close", hueDistance(tones[i], tones[j]) >= 30f)
        }
    }
}
