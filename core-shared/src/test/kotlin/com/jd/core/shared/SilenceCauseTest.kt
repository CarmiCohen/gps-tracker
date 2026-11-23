package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Test

/** Chapter 250 (ISSUE-NOV22.10-O137): the causes of a silence on the wire, and a shutdown's cause. */
class SilenceCauseTest {

    @Test
    fun eachCauseReadsBackFromItsWireTextAndAnythingElseIsAPlainSilence() {
        SilenceCause.entries.forEach { assertEquals(it, SilenceCause.fromWire(it.wire)) }
        assertEquals(SilenceCause.entries.size, SilenceCause.entries.map { it.wire }.toSet().size)
        assertEquals(SilenceCause.SILENT, SilenceCause.fromWire(null))
        assertEquals(SilenceCause.SILENT, SilenceCause.fromWire("meteor"))
    }

    @Test
    fun aShutdownIsAnEmptyBatteryOnlyUnpluggedAtOrBelowTheLimit() {
        val limit = SilenceCause.BATTERY_EMPTY_AT_PERCENT
        assertEquals(SilenceCause.BATTERY_EMPTY, SilenceCause.ofShutdown(limit, isPluggedIn = false))
        assertEquals(SilenceCause.BATTERY_EMPTY, SilenceCause.ofShutdown(0, isPluggedIn = false))
        assertEquals(SilenceCause.SHUTDOWN, SilenceCause.ofShutdown(limit + 1, isPluggedIn = false))
        assertEquals(SilenceCause.SHUTDOWN, SilenceCause.ofShutdown(1, isPluggedIn = true))
        assertEquals("Battery not known", SilenceCause.SHUTDOWN, SilenceCause.ofShutdown(null, isPluggedIn = false))
    }
}
