package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 138 (ISSUE-NOV6.4-O34): the Sensor Setup dialog shows the service's real state. */
class SensorServiceStatusTest {

    @Test
    fun aNewStatusIsStopped() {
        assertFalse(SensorServiceStatus().isRunning.value)
    }

    @Test
    fun markRunningThenStoppedFollowsTheService() {
        val status = SensorServiceStatus()

        status.markRunning()
        assertTrue(status.isRunning.value)

        status.markStopped()
        assertFalse(status.isRunning.value)
    }

    @Test
    fun serviceLineShowsRunningOrStopped() {
        assertEquals("Sensor Service: Running", serviceStatusText(true))
        assertEquals("Sensor Service: Stopped", serviceStatusText(false))
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
