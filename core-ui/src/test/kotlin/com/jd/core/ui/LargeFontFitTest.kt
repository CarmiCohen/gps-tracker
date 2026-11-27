package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISSUE-NOV26.29-O165: with the S21FE's large font the Events filters move to a second line instead of being cut off,
 * and the Graphs' tick labels keep a gap instead of touching at the right edge.
 */
class LargeFontFitTest {

    @Test
    fun filtersStayOnThePeriodsLineWhenTheyFit() {
        assertTrue(fitsOneLine(periodsWidth = 600, filtersWidth = 300, gap = 20, width = 1080))
        assertTrue(fitsOneLine(periodsWidth = 600, filtersWidth = 300, gap = 20, width = 920))
    }

    @Test
    fun filtersWrapWhenTheLargeFontWidensTheChips() {
        assertFalse(fitsOneLine(periodsWidth = 760, filtersWidth = 330, gap = 20, width = 1080))
        assertFalse(fitsOneLine(periodsWidth = 600, filtersWidth = 300, gap = 20, width = 919))
    }

    @Test
    fun tickLabelsAreCentredUnderTheirTicks() {
        assertEquals(listOf(50, 450), tickLabelPositions(listOf(0.1f, 0.5f), listOf(100, 100), width = 1000, gap = 20))
    }

    @Test
    fun lastTickLabelPushedInsideIsLeftOutWhenItWouldTouchThePrevious() {
        // 21:10 at 0.85 ends at 910; 21:20 at 1.0 is kept inside at 880, within the 20 px gap, so it is left out.
        assertEquals(listOf(790, null), tickLabelPositions(listOf(0.85f, 1.0f), listOf(120, 120), width = 1000, gap = 20))
    }

    @Test
    fun tickLabelsWithRoomForTheGapAreAllShown() {
        assertEquals(listOf(0, 300, 880), tickLabelPositions(listOf(0f, 0.36f, 1.0f), listOf(120, 120, 120), width = 1000, gap = 20))
    }

    @Test
    fun aLabelWiderThanTheGraphStartsAtItsLeftEdge() {
        assertEquals(listOf(0), tickLabelPositions(listOf(0.5f), listOf(200), width = 100, gap = 20))
    }
}
