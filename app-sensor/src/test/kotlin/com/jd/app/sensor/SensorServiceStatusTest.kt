package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 138 (ISSUE-NOV6.4-O34, updated Nov7.34 for ISSUE-NOV7.2-O36): the Sensor Setup dialog and console show the service's real state and uptime. */
class SensorServiceStatusTest {

    @Test
    fun aNewStatusIsStopped() {
        val status = SensorServiceStatus()
        assertFalse(status.isRunning.value)
        assertEquals(0L, status.startTimeMillis.value)
    }

    @Test
    fun markRunningThenStoppedFollowsTheService() {
        val status = SensorServiceStatus()

        status.markRunning(1000L)
        assertTrue(status.isRunning.value)
        assertEquals(1000L, status.startTimeMillis.value)

        status.markStopped()
        assertFalse(status.isRunning.value)
        assertEquals(0L, status.startTimeMillis.value)
    }

    @Test
    fun uptimeFormattingIsCorrect() {
        assertEquals("00:00:00", formatUptime(0L, 1000L))
        assertEquals("00:00:00", formatUptime(1000L, 500L))
        assertEquals("00:01:04", formatUptime(1000L, 65000L))
        assertEquals("01:01:00", formatUptime(1000L, 3661000L))
    }

    @Test
    fun serviceLineShowsRunningOrStoppedWithUptime() {
        assertEquals("Sensor Service: Running", serviceStatusText(true))
        assertEquals("Sensor Service: Running (00:01:04)", serviceStatusText(true, "00:01:04"))
        assertEquals("Sensor Service: Stopped", serviceStatusText(false))
        assertEquals("Sensor Service: Stopped", serviceStatusText(false, "00:01:04"))
    }

    @Test
    fun relayLineShowsConnectedOrDisconnected() {
        assertEquals("Relay Connection: Connected", relayStatusText(true))
        assertEquals("Relay Connection: Disconnected", relayStatusText(false))
    }

    @Test
    fun noLineClaimsAnInventedReadyState() {
        listOf(serviceStatusText(true), serviceStatusText(false), relayStatusText(true), relayStatusText(false))
            .forEach { assertFalse(it, it.contains("Ready") || it.contains("Active")) }
    }
}

