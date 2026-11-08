package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 159: debug adb commands are parsed strictly and each app runs only its own (SIMP-NOV8.100-S1). */
class DebugCommandTest {

    private fun parse(action: String?, accepted: Set<String>, vararg extras: Pair<String, String>) =
        DebugCommand.parse(action, accepted) { name -> extras.toMap()[name] }

    @Test
    fun sensorSetsItsId() {
        assertEquals(DebugCommand.SetSensorId("T2"), parse(DebugCommand.SET_ID, DebugCommand.SENSOR_ACTIONS, "id" to " T2 "))
    }

    @Test
    fun blankOrMissingIdIsRejected() {
        assertTrue(parse(DebugCommand.SET_ID, DebugCommand.SENSOR_ACTIONS, "id" to "  ") is DebugCommand.Rejected)
        assertTrue(parse(DebugCommand.PAIR, DebugCommand.MONITOR_ACTIONS) is DebugCommand.Rejected)
    }

    @Test
    fun monitorPairsUnpairsAndEditsHomePoints() {
        assertEquals(DebugCommand.Pair("T"), parse(DebugCommand.PAIR, DebugCommand.MONITOR_ACTIONS, "id" to "T"))
        assertEquals(DebugCommand.Unpair, parse(DebugCommand.UNPAIR, DebugCommand.MONITOR_ACTIONS))
        assertEquals(
            DebugCommand.AddHomePoint(HomePoint(32.08, 34.78)),
            parse(DebugCommand.ADD_HOME, DebugCommand.MONITOR_ACTIONS, "lat" to "32.08", "lng" to "34.78")
        )
        assertEquals(DebugCommand.ClearHomePoints, parse(DebugCommand.CLEAR_HOME, DebugCommand.MONITOR_ACTIONS))
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        val accepted = DebugCommand.MONITOR_ACTIONS
        assertTrue(parse(DebugCommand.ADD_HOME, accepted, "lat" to "91", "lng" to "0") is DebugCommand.Rejected)
        assertTrue(parse(DebugCommand.ADD_HOME, accepted, "lat" to "0", "lng" to "-181") is DebugCommand.Rejected)
        assertTrue(parse(DebugCommand.ADD_HOME, accepted, "lat" to "x", "lng" to "1") is DebugCommand.Rejected)
        assertTrue(parse(DebugCommand.ADD_HOME, accepted, "lat" to "1") is DebugCommand.Rejected)
    }

    @Test
    fun eachAppRunsOnlyItsOwnCommands() {
        assertTrue(parse(DebugCommand.PAIR, DebugCommand.SENSOR_ACTIONS, "id" to "T") is DebugCommand.Rejected)
        assertTrue(parse(DebugCommand.SET_ID, DebugCommand.MONITOR_ACTIONS, "id" to "T") is DebugCommand.Rejected)
        assertTrue(parse(null, DebugCommand.MONITOR_ACTIONS) is DebugCommand.Rejected)
        assertTrue(parse("com.jd.debug.WIPE", DebugCommand.MONITOR_ACTIONS) is DebugCommand.Rejected)
        assertEquals(DebugCommand.State, parse(DebugCommand.STATE, DebugCommand.SENSOR_ACTIONS))
    }
}
