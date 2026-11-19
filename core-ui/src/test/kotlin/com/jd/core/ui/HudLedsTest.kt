package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Chapter 232.1: every HUD LED is green OK, red not OK or gray no information (Nov19.5). */
class HudLedsTest {

    @Test
    fun theThreeColorsAreGreenRedAndGray() {
        assertEquals(StatusGreen, ledColor(LedState.OK))
        assertEquals(StatusRed, ledColor(LedState.NOT_OK))
        assertEquals(StatusGrey, ledColor(LedState.UNKNOWN))
    }

    @Test
    fun gpsMeansAFixFromAtLeastFourSatellites() {
        // User decision 2026-11-19 (option b): a Wi-Fi or cell position does not light GPS.
        assertEquals(4, GPS_MIN_SATELLITES)
        assertEquals(false, isSatelliteFix(0))
        assertEquals(false, isSatelliteFix(3))
        assertEquals(true, isSatelliteFix(4))
        assertEquals(false, isSatelliteFix(-1))
    }

    @Test
    fun anOwnGpsFollowsTheAgeOfItsLastSatelliteFix() {
        assertEquals(LedState.OK, gpsLed(isLocationOn = true, satelliteFixAgeMs = 0L))
        assertEquals(LedState.OK, gpsLed(true, GPS_FIX_CURRENT_MS - 1))
        assertEquals(LedState.NOT_OK, gpsLed(true, GPS_FIX_CURRENT_MS))
        assertEquals(LedState.NOT_OK, gpsLed(true, GPS_NO_INFO_AFTER_MS - 1))
        assertEquals("gray after 2 min without a fix", LedState.UNKNOWN, gpsLed(true, GPS_NO_INFO_AFTER_MS))
        assertEquals("gray before the first fix or without GNSS status", LedState.UNKNOWN, gpsLed(true, null))
        assertEquals(120_000L, GPS_NO_INFO_AFTER_MS)
    }

    @Test
    fun aSwitchedOffLocationIsRed() {
        assertEquals(LedState.NOT_OK, gpsLed(isLocationOn = false, satelliteFixAgeMs = 0L))
        assertEquals(LedState.NOT_OK, gpsLed(isLocationOn = false, satelliteFixAgeMs = null))
    }

    @Test
    fun theSensorsGpsOnTheMonitorFollowsTheSatellitesOfItsRows() {
        assertEquals("not heard", LedState.UNKNOWN, remoteGpsLed(isHeard = false, newestRowSatellitesUsed = 8, lastSatelliteFixAgeMs = 0L))
        assertEquals(LedState.OK, remoteGpsLed(isHeard = true, newestRowSatellitesUsed = 8, lastSatelliteFixAgeMs = 0L))
        assertEquals(LedState.OK, remoteGpsLed(true, newestRowSatellitesUsed = GPS_MIN_SATELLITES, lastSatelliteFixAgeMs = 0L))
        assertEquals("too few satellites", LedState.NOT_OK, remoteGpsLed(true, newestRowSatellitesUsed = 3, lastSatelliteFixAgeMs = 30_000L))
        assertEquals(LedState.UNKNOWN, remoteGpsLed(true, newestRowSatellitesUsed = 0, lastSatelliteFixAgeMs = GPS_NO_INFO_AFTER_MS))
        assertEquals(LedState.UNKNOWN, remoteGpsLed(true, newestRowSatellitesUsed = 0, lastSatelliteFixAgeMs = null))
        assertEquals("no GNSS status on the Sensor", LedState.UNKNOWN, remoteGpsLed(true, newestRowSatellitesUsed = -1, lastSatelliteFixAgeMs = 0L))
    }

    @Test
    fun thePeerLedIsGrayWhenItCannotBeKnown() {
        assertEquals(LedState.OK, peerLed(isPaired = true, isRelayConnected = true, isPeerHeard = true))
        assertEquals(LedState.NOT_OK, peerLed(isPaired = true, isRelayConnected = true, isPeerHeard = false))
        assertEquals("relay down", LedState.UNKNOWN, peerLed(isPaired = true, isRelayConnected = false, isPeerHeard = false))
        assertEquals("not paired", LedState.UNKNOWN, peerLed(isPaired = false, isRelayConnected = true, isPeerHeard = false))
    }

    @Test
    fun everyMotionLabelHasTheWidthOfStationary() {
        // Chapter 232.3: the menu bar keeps the label whole and its icons do not move when the state changes.
        assertEquals("STATIONARY", motionLabelText("STATIONARY"))
        assertEquals(" »MOVING« ", motionLabelText("MOVING"))
        assertEquals("    --    ", motionLabelText(NO_VALUE))
        listOf("STATIONARY", "MOVING", "PARKING", NO_VALUE).forEach { assertEquals(MOTION_LABEL_CHARS, motionLabelText(it).length) }
    }

    @Test
    fun theRelayLinkNeedsInternet() {
        // A cut network leaves the socket connected until its ping times out.
        assertEquals(false, relayLinkUp(isInternet = false, isSocketConnected = true))
        assertEquals(false, relayLinkUp(isInternet = true, isSocketConnected = false))
        assertEquals(true, relayLinkUp(isInternet = true, isSocketConnected = true))
    }

    @Test
    fun anOwnLineHasIntSrvGpsAndThePeerOnly() {
        val leds = ownLeds(isInternet = true, isRelayConnected = false, gps = LedState.UNKNOWN, peerLabel = "VWR", peer = LedState.UNKNOWN)

        assertEquals(listOf("INT", "SRV", "GPS", "VWR"), leds.map { it.label })
        assertEquals(listOf(LedState.OK, LedState.NOT_OK, LedState.UNKNOWN, LedState.UNKNOWN), leds.map { it.state })
    }
}
