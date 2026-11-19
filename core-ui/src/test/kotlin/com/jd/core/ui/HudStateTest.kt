package com.jd.core.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * HudStateTest: Verifies state logic for SRV LED, GPS LED and Peer Connection (DAT removed in Nov19.5, user-confirmed;
 * the LED rules are in HudLedsTest).
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
    fun relayStatusTextFollowsTheSrvState() {
        assertEquals("Relay Status: Connected", HudState(isRelayConnected = true).relayStatusText)
        assertEquals("Relay Status: Disconnected", HudState(isRelayConnected = false).relayStatusText)
    }

    @Test
    fun testGpsLedState() {
        assertEquals("GPS LED must be green with a current local fix", LedState.OK, gpsLed(true, 1_000L))
        assertEquals("GPS LED must not be green when local fix is missing", LedState.UNKNOWN, gpsLed(true, null))
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
    fun testHudStatusBarFormattingConstraints() {
        val state = HudState(
            appMode = "monitor",
            selfId = "viewer_654321",
            batteryLevel = 100,
            tempCelsius = 30.0f,
            accuracyMeters = 5.0f,
            telemetryAgeSec = 0
        )

        assertEquals("VIEWER", state.idText)
        assertEquals("T", HudState(appMode = "sensor", selfId = "t").idText)
        assertEquals("${B}100%|${T}30°|A:±5.0m|AGE:0s", state.telemetryText)
    }

    @Test
    fun unknownValuesShowNoNumber() {
        val state = HudState()

        assertEquals("${B}--|${T}--|A:--|AGE:--", state.telemetryText)
        assertEquals("-- km/h", state.speedText)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun knownValuesAreFormatted() {
        val state = HudState(batteryLevel = 7, isCharging = true, tempCelsius = 41.6f, speedKmh = 12.34f)

        assertEquals("${B}7%⚡|${T}42°|A:--|AGE:--", state.telemetryText)
        assertEquals("12.3 km/h", state.speedText)
    }

    @Test
    fun zeroIsAKnownValue() {
        val state = HudState(batteryLevel = 0, tempCelsius = 0f, speedKmh = 0f)

        assertEquals("${B}0%|${T}0°|A:--|AGE:--", state.telemetryText)
        assertEquals("0.0 km/h", state.speedText)
    }

    /** Chapter 219.1: an icon marks the battery and the temperature; no `B:`, no `T:`, no `C` (ISSUE-NOV17.7-O106). */
    @Test
    fun batteryAndTemperatureAreMarkedByIconsNotLabels() {
        val text = HudState(batteryLevel = 55, tempCelsius = 31f).telemetryText
        val (battery, temperature) = text.split("|")

        assertEquals("🔋", HUD_BATTERY_ICON)
        assertEquals("🌡️", HUD_TEMPERATURE_ICON)
        assertEquals("${HUD_BATTERY_ICON}55%", battery)
        assertEquals("${HUD_TEMPERATURE_ICON}31°", temperature)
        assertFalse(text, text.contains("B:"))
        assertFalse(text, text.contains("T:"))
        assertFalse(text, text.contains("C"))
    }

    private companion object {
        const val B = HUD_BATTERY_ICON
        const val T = HUD_TEMPERATURE_ICON
    }
}
