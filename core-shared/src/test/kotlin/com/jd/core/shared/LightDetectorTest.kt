package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 237 (ISSUE-NOV22.4-O130): the Sensor's light detection, like the noise detection: a sudden rise to 4 times the
 * background and at least 20 lx more, bright for 2 s within 3 s, alerts once; a darkening, a flash or a slow dawn does not.
 */
class LightDetectorTest {

    /** Feeds [lux] every [LIGHT_TICK_MS] from [fromMs] for [durationMs], as the monitor's tick does; returns the alert times. */
    private fun LightDetector.feed(lux: Double, fromMs: Long, durationMs: Long): List<Long> =
        (fromMs until fromMs + durationMs step LIGHT_TICK_MS).filter { onLux(lux, it) }

    private fun darkRoom(lux: Double = 2.0): LightDetector = LightDetector().apply { feed(lux, 0, 5_000) }

    @Test
    fun aLampSwitchedOnInADarkRoomAlertsAfterTwoSeconds() {
        val detector = darkRoom()
        assertEquals("The tick that completes 2 s of bright light", listOf(5_000 + LIGHT_CONFIRM_MS), detector.feed(300.0, 5_000, 5_000))
        assertEquals("Compared against the dark background", 2.0, detector.alertReferenceLux!!, 0.5)
    }

    @Test
    fun aLightThatStaysOnAlertsOnceAndBecomesBackground() {
        val detector = darkRoom()
        assertEquals(1, detector.feed(300.0, 5_000, 120_000).size)
        assertTrue("The background follows the light", detector.floorLux!! > 250.0)
    }

    @Test
    fun aRiseOfUnder20LuxDoesNotAlert() {
        val detector = darkRoom(lux = 1.0)
        assertTrue("20 times brighter but only 19 lx more", detector.feed(20.0, 5_000, 5_000).isEmpty())
    }

    @Test
    fun aRiseUnderFourTimesTheBackgroundDoesNotAlert() {
        val detector = darkRoom(lux = 100.0)
        assertTrue(detector.feed(390.0, 5_000, 5_000).isEmpty())
        val again = darkRoom(lux = 100.0)
        assertEquals(1, again.feed(400.0, 5_000, 5_000).size)
    }

    @Test
    fun aShortFlashDoesNotAlert() {
        val detector = darkRoom()
        assertTrue("Headlights sweeping past for 1.5 s", detector.feed(300.0, 5_000, 1_500).isEmpty())
        assertTrue(detector.feed(2.0, 6_500, 5_000).isEmpty())
    }

    @Test
    fun aDarkeningNeverAlerts() {
        val detector = darkRoom(lux = 400.0)
        assertTrue("A hand or a pocket over the sensor", detector.feed(0.0, 5_000, 30_000).isEmpty())
    }

    @Test
    fun aSlowDawnDoesNotAlert() {
        val detector = darkRoom()
        var lux = 2.0
        var alerts = 0
        for (t in 5_000L until 1_805_000L step LIGHT_TICK_MS) {
            lux += 0.05 // 12 lx a minute, 360 lx in half an hour.
            if (detector.onLux(lux, t)) alerts++
        }
        assertEquals(0, alerts)
    }

    @Test
    fun noAlertWhileTheBackgroundIsFirstLearned() {
        val detector = LightDetector()
        detector.onLux(2.0, 0)
        assertTrue(detector.feed(300.0, LIGHT_TICK_MS, BACKGROUND_WARM_UP_MS - LIGHT_TICK_MS).isEmpty())
    }

    @Test
    fun aNewRiseCountsOnlyAfterTheLightFellBelowTheLimit() {
        val detector = darkRoom()
        assertEquals(1, detector.feed(300.0, 5_000, 3_000).size)
        assertTrue(detector.feed(2.0, 8_000, 10_000).isEmpty())
        assertEquals("Switched on again", 1, detector.feed(300.0, 18_000, 3_000).size)
    }

    @Test
    fun aReadingHeldBetweenSparseEventsStillConfirms() {
        val detector = darkRoom()
        // The sensor reports only the change; with no tick the next event, 2 s later, completes the confirmation.
        assertTrue(!detector.onLux(300.0, 5_000))
        assertTrue(detector.onLux(301.0, 7_000))
    }

    @Test
    fun theSliderSetsTheRiseFactor() {
        assertEquals(8.0, lightRatioForSensitivity(0.1f), 1e-6)
        assertEquals(LIGHT_RISE_RATIO, lightRatioForSensitivity(DEFAULT_LIGHT_SENSITIVITY), 1e-6)
        assertEquals(2.0, lightRatioForSensitivity(1.0f), 1e-6)
        assertEquals("Out of range is clamped", 8.0, lightRatioForSensitivity(0f), 1e-6)
        assertEquals("4×", lightLimitLabel(0.5f))
        assertEquals("3.2×", lightLimitLabel(0.7f))
        assertEquals("6×", lightLimitLabel(0.3f))
    }

    @Test
    fun aHigherSensitivityAlertsOnASmallerRise() {
        val sensitive = LightDetector(riseRatio = lightRatioForSensitivity(1.0f)).apply { feed(100.0, 0, 5_000) }
        assertEquals(1, sensitive.feed(250.0, 5_000, 3_000).size)
        val dull = LightDetector(riseRatio = lightRatioForSensitivity(0.1f)).apply { feed(100.0, 0, 5_000) }
        assertTrue(dull.feed(250.0, 5_000, 3_000).isEmpty())
    }

    @Test
    fun theAlertLineNamesTheLightTheBackgroundAndTheFactor() {
        assertEquals("Light alert: 300 lx, background 2 lx, 150×", lightAlertText(300.0, 2.0))
        assertEquals("Light alert: 420 lx, background 100 lx, 4.2×", lightAlertText(420.0, 100.0))
        assertEquals("Light alert: 25 lx, background 0 lx", lightAlertText(25.0, 0.0))
    }

    @Test
    fun theLightSwitchAllowsOnlyTheLightAlert() {
        val off = SensorBehaviour(lightAlert = false)
        assertTrue(!off.allows(LIGHT_ALERT_STATUS))
        assertTrue(off.allows(ACOUSTIC_ALERT_STATUS))
        assertTrue(SensorBehaviour().allows(LIGHT_ALERT_STATUS))
        assertEquals(off, AlertSettings(lightAlert = false).sensorBehaviour())
        assertNull(LightDetector().floorLux)
    }

    @Test
    fun aLightSettingChangeIsANewSensorRevision() {
        val before = AlertSettings(sensorRevision = 5L)
        assertEquals(1_000L, AlertSettings(lightSensitivity = 0.9f).revisedAfter(before, 1_000L).sensorRevision)
        assertEquals(5L, AlertSettings(lightSensitivity = DEFAULT_LIGHT_SENSITIVITY).revisedAfter(before, 1_000L).sensorRevision)
    }
}
