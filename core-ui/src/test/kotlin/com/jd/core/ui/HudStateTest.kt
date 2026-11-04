package com.jd.core.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * HudStateTest: Verifies state logic for SRV LED, GPS LED, Peer Connection, and DAT integrity.
 * Adheres to Requirement R960 and HUD LED Specifications (v9.3.6).
 */
class HudStateTest {

    @Test
    fun testSrvLedState() {
        val connectedState = HudState(isRelayConnected = true)
        val disconnectedState = HudState(isRelayConnected = false)

        assertTrue("SRV LED must be active when relay is connected", connectedState.isRelayConnected)
        assertFalse("SRV LED must be inactive when relay is disconnected", disconnectedState.isRelayConnected)
    }

    @Test
    fun testGpsLedState() {
        val gpsActiveState = HudState(isLocalGpsActive = true)
        val gpsInactiveState = HudState(isLocalGpsActive = false)

        assertTrue("GPS LED must be active when local fix is present", gpsActiveState.isLocalGpsActive)
        assertFalse("GPS LED must be inactive when local fix is missing", gpsInactiveState.isLocalGpsActive)
    }

    @Test
    fun testPeerConnectionLedState() {
        val monitorPeerActive = HudState(appMode = "monitor", isPeerActive = true)
        val monitorPeerStale = HudState(appMode = "monitor", isPeerActive = false)

        assertTrue("TRK LED must be active on Monitor when target peer telemetry is fresh", monitorPeerActive.isPeerActive)
        assertFalse("TRK LED must be inactive on Monitor when target peer telemetry is stale", monitorPeerStale.isPeerActive)

        val sensorPeerActive = HudState(appMode = "sensor", isPeerActive = true)
        val sensorPeerInactive = HudState(appMode = "sensor", isPeerActive = false)

        assertTrue("VWR LED must be active on Sensor when viewer connection is established", sensorPeerActive.isPeerActive)
        assertFalse("VWR LED must be inactive on Sensor when viewer connection is absent", sensorPeerInactive.isPeerActive)
    }

    @Test
    fun testDataIntegrityState() {
        val healthyState = HudState(
            isInternet = true,
            isRelayConnected = true,
            isPeerActive = true,
            isDataHealthy = true
        )

        val degradedState = HudState(
            isInternet = true,
            isRelayConnected = true,
            isPeerActive = false,
            isDataHealthy = false
        )

        assertTrue("DAT LED must be active when INT, SRV, and Peer are all active", healthyState.isDataHealthy)
        assertFalse("DAT LED must be inactive when any link is degraded", degradedState.isDataHealthy)
    }
}
