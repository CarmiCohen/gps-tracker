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

    // relayStatusTextFollowsTheSrvState went with SIMP-NOV15.5-S1 (user-confirmed, Rule 17): no Setup dialog relay line.

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
        assertEquals("100% 30° ±5.0m 0s", state.telemetryText)
    }

    @Test
    fun unknownValuesShowNoNumber() {
        val state = HudState()

        assertEquals("-- -- -- --", state.telemetryText)
        assertEquals("-- km/h", state.speedText)
        assertEquals(NO_VALUE, state.trackerState)
    }

    @Test
    fun knownValuesAreFormatted() {
        val state = HudState(batteryLevel = 7, isCharging = true, tempCelsius = 41.6f, speedKmh = 12.34f)

        assertEquals("7%⚡ 42° -- --", state.telemetryText)
        assertEquals("12.3 km/h", state.speedText)
        assertEquals("12.3", state.speedNumberText)
    }

    @Test
    fun zeroIsAKnownValue() {
        val state = HudState(batteryLevel = 0, tempCelsius = 0f, speedKmh = 0f)

        assertEquals("0% 0° -- --", state.telemetryText)
        assertEquals("0.0 km/h", state.speedText)
    }

    /**
     * Chapter 219.1, Nov19.6: no icons and no labels (no `B:`, `T:`, `A:`, `AGE:`, no `C`); the `%`, `°`, `±` and
     * `s` tell the values apart.
     */
    @Test
    fun valuesCarryOnlyTheirUnitSigns() {
        val text = HudState(batteryLevel = 55, tempCelsius = 31f, accuracyMeters = 4f, telemetryAgeSec = 7).telemetryText

        assertEquals("55% 31° ±4.0m 7s", text)
        listOf("🔋", "🌡", "B:", "T:", "A:", "AGE:", "C").forEach { assertFalse(text, text.contains(it)) }
    }

    /** Nov19.6: from 9.95 m on the accuracy has no decimal. */
    @Test
    fun largeAccuracyDropsItsDecimal() {
        fun accuracy(m: Float) = HudState(accuracyMeters = m).telemetryText.split(" ")[2]

        assertEquals("±9.9m", accuracy(9.94f))
        assertEquals("±10m", accuracy(9.95f))
        assertEquals("±13m", accuracy(12.5f))
        assertEquals("±96m", accuracy(96f))
    }
}
