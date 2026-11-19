package com.jd.core.ui

import androidx.compose.runtime.Immutable

/**
 * What a HUD LED says (Nov19.5, user request): green OK, red not OK, gray no information. The LEDs no longer take the
 * app's color, and a LED is shown only when its phone can know what it says: the Monitor's line for the Sensor holds
 * GPS alone, as the Monitor cannot see the Sensor's internet or relay connection. DAT and WDG are gone (DAT copied the
 * other LEDs, WDG was always green).
 */
enum class LedState { OK, NOT_OK, UNKNOWN }

@Immutable
data class HudLed(val label: String, val state: LedState)

/**
 * The GPS LED means satellites, in both apps (user decision 2026-11-19, option b): the GNSS chip's fix uses at least
 * this many. Four is the least for a 3D fix. A Wi-Fi or cell position does not count, so indoors the map may still
 * show a position while GPS is red.
 */
const val GPS_MIN_SATELLITES = 4

/** A satellite fix younger than this is current: the Sensor's own limit before it reports `NO_FIX` (FIX_STALE_AFTER_MS). */
const val GPS_FIX_CURRENT_MS = 15_000L

/** Without a satellite fix for this long the GPS LED turns gray: the last signal is too old to tell anything (user decision 2026-11-19). */
const val GPS_NO_INFO_AFTER_MS = 120_000L

fun ledOf(isOk: Boolean): LedState = if (isOk) LedState.OK else LedState.NOT_OK

/** The GNSS chip's fix uses enough satellites ([satellitesUsed] from its status report) to light the GPS LED. */
fun isSatelliteFix(satellitesUsed: Int): Boolean = satellitesUsed >= GPS_MIN_SATELLITES

/**
 * A phone's own GPS, by the time since its GNSS chip last reported a [isSatelliteFix] ([satelliteFixAgeMs], null
 * before the first one or without GNSS status): current green, older red, gray after [GPS_NO_INFO_AFTER_MS]. A
 * switched-off location setting is red. The satellite status keeps coming on a phone standing still, which the
 * Monitor's fixes do not (they wait for 10 m of movement).
 */
fun gpsLed(isLocationOn: Boolean, satelliteFixAgeMs: Long?): LedState = when {
    !isLocationOn -> LedState.NOT_OK
    satelliteFixAgeMs == null || satelliteFixAgeMs >= GPS_NO_INFO_AFTER_MS -> LedState.UNKNOWN
    satelliteFixAgeMs < GPS_FIX_CURRENT_MS -> LedState.OK
    else -> LedState.NOT_OK
}

/**
 * The Sensor's GPS on the Monitor, from the satellites its rows report ([newestRowSatellitesUsed], -1 when the Sensor
 * has no GNSS status). Gray while the Sensor is not heard ([isHeard]) or reports no satellites at all; green when its
 * newest row is a [isSatelliteFix]; red otherwise, gray again once its last satellite fix ([lastSatelliteFixAgeMs]) is
 * [GPS_NO_INFO_AFTER_MS] old. The same rule as the Sensor's own LED.
 */
fun remoteGpsLed(isHeard: Boolean, newestRowSatellitesUsed: Int, lastSatelliteFixAgeMs: Long?): LedState = when {
    !isHeard || newestRowSatellitesUsed < 0 -> LedState.UNKNOWN
    isSatelliteFix(newestRowSatellitesUsed) -> LedState.OK
    lastSatelliteFixAgeMs == null || lastSatelliteFixAgeMs >= GPS_NO_INFO_AFTER_MS -> LedState.UNKNOWN
    else -> LedState.NOT_OK
}

/**
 * TRK on the Monitor, VWR on the Sensor: whether the other app is heard. Gray when this phone is not paired or its relay
 * connection is down, as it then cannot hear the other app at all.
 */
fun peerLed(isPaired: Boolean, isRelayConnected: Boolean, isPeerHeard: Boolean): LedState = when {
    !isPaired || !isRelayConnected -> LedState.UNKNOWN
    else -> ledOf(isPeerHeard)
}

/**
 * The relay link the LEDs show: the socket counts only with internet. A cut network leaves the socket "connected" until
 * its ping times out (seen on the A15 in Nov19.5: INT red, SRV still green after 25 s).
 */
fun relayLinkUp(isInternet: Boolean, isSocketConnected: Boolean): Boolean = isInternet && isSocketConnected

/**
 * The four LEDs of a phone's own line: INT, SRV, GPS and the other app (TRK on the Monitor, VWR on the Sensor).
 * [isRelayConnected] is [relayLinkUp], which [peer] uses too.
 */
fun ownLeds(isInternet: Boolean, isRelayConnected: Boolean, gps: LedState, peerLabel: String, peer: LedState) = listOf(
    HudLed("INT", ledOf(isInternet)),
    HudLed("SRV", ledOf(isRelayConnected)),
    HudLed("GPS", gps),
    HudLed(peerLabel, peer)
)
