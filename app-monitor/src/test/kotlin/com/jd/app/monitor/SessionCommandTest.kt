package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 151 (ISSUE-NOV8.5-O50): an Activity recreation must not stop the Monitor session.
 * Before the fix, the screen read the not-yet-loaded pairing as "not paired" on every rotation,
 * stopped [MonitorSessionService], lost acknowledged alarms, and sounded them again on restart.
 */
class SessionCommandTest {

    @Test
    fun aPairingNotReadYetLeavesTheSessionAlone() {
        assertNull(sessionCommandFor(null))
    }

    @Test
    fun aStoredTrackerStartsTheSession() {
        assertEquals(SessionCommand.START, sessionCommandFor(Pairing("T")))
    }

    @Test
    fun noStoredTrackerStopsTheSession() {
        assertEquals(SessionCommand.STOP, sessionCommandFor(Pairing(null)))
    }
}
