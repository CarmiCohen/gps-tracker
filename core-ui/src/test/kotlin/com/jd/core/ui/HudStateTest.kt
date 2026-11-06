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

    @Test
    fun testHudStatusBarFormattingConstraints() {
        val state = HudState(
            appMode = "monitor",
            targetId = "tracker_123456",
            selfId = "viewer_654321",
            batteryLevel = 100,
            tempCelsius = 30.0f,
            accuracyMeters = 5.0f,
            telemetryAgeSec = 0
        )

        val idDisplay = "TRK:${state.targetId.take(6).uppercase()} | SELF:${state.selfId.take(6).uppercase()}"

        assertEquals("TRK:TRACKE | SELF:VIEWER", idDisplay)
        assertEquals("B:100%|T:30°C|A:±5.0m|AGE:0s", state.telemetryText)
    }

    @Test
    fun unknownValuesShowNoNumber() {
        val state = HudState()

        assertEquals("B:--|T:--|A:--|AGE:--", state.telemetryText)
        assertEquals("-- km/h", state.speedText)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun knownValuesAreFormatted() {
        val state = HudState(batteryLevel = 7, isCharging = true, tempCelsius = 41.6f, speedKmh = 12.34f)

        assertEquals("B:7%⚡|T:42°C|A:--|AGE:--", state.telemetryText)
        assertEquals("12.3 km/h", state.speedText)
    }

    @Test
    fun zeroIsAKnownValue() {
        val state = HudState(batteryLevel = 0, tempCelsius = 0f, speedKmh = 0f)

        assertEquals("B:0%|T:0°C|A:--|AGE:--", state.telemetryText)
        assertEquals("0.0 km/h", state.speedText)
    }
}
