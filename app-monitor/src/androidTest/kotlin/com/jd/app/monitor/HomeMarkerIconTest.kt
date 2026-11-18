package com.jd.app.monitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Chapter 217: the Home Point square is drawn at 10 dp, half its former 20 dp (ISSUE-NOV17.7-O104). */
@RunWith(AndroidJUnit4::class)
class HomeMarkerIconTest {

    @Test
    fun homePointSquareIsTenDp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPx = (10 * context.resources.displayMetrics.density).toInt()
        val icon = homeMarkerIcon(context)

        assertEquals(10, HOME_MARKER_SIZE_DP)
        assertEquals(expectedPx, icon.intrinsicWidth)
        assertEquals(expectedPx, icon.intrinsicHeight)
    }
}
