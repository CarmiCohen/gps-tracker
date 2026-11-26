package com.jd.app.sensor

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.SeatPlacement
import com.jd.core.shared.SeatType
import com.jd.core.shared.SensorBehaviour
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 188 (ISSUE-NOV10.8-O83): the Sensor's detection settings come from the Monitor. An installed Sensor keeps its
 * own earlier tilt and vibration limits only until the Monitor's first message, and shows the settings read-only.
 */
class SensorBehaviourStoreTest {

    private val relayUrlKey = stringPreferencesKey("relay_url")

    @Test
    fun theOldSlidersBecomeTheStartingSettingsAtRevisionZero() = runBlocking {
        val legacy = AlertSettings(tiltSensitivity = 0.8f, vibrationSensitivity = 0.2f, tiltAlert = false)
        val stored = preferencesOf(LEGACY_ALERT_SETTINGS_KEY to Json.encodeToString(legacy), relayUrlKey to "wss://relay.example")

        assertTrue(LegacyAlertSettingsMigration.shouldMigrate(stored))
        val migrated = LegacyAlertSettingsMigration.migrate(stored)

        assertFalse("The old settings are removed", LEGACY_ALERT_SETTINGS_KEY in migrated)
        assertEquals("wss://relay.example", migrated[relayUrlKey])
        // The Sensor's old switches did nothing there, so only the two limits carry over; revision 0 loses to any Monitor message.
        assertEquals(
            SensorBehaviour(tiltSensitivity = 0.8f, vibrationSensitivity = 0.2f, revision = 0L),
            decodeSensorBehaviour(migrated[SENSOR_BEHAVIOUR_KEY])
        )
    }

    @Test
    fun aReceivedSettingIsNotOverwrittenByTheOldSliders() = runBlocking {
        val received = SensorBehaviour(noiseAlert = false, revision = 5L)
        val stored = preferencesOf(
            LEGACY_ALERT_SETTINGS_KEY to Json.encodeToString(AlertSettings(tiltSensitivity = 0.9f)),
            SENSOR_BEHAVIOUR_KEY to Json.encodeToString(received)
        )
        val migrated = LegacyAlertSettingsMigration.migrate(stored)
        assertEquals(received, decodeSensorBehaviour(migrated[SENSOR_BEHAVIOUR_KEY]))
        assertFalse(LEGACY_ALERT_SETTINGS_KEY in migrated)
    }

    @Test
    fun aSensorWithoutOldSettingsIsLeftAlone() = runBlocking {
        assertFalse(LegacyAlertSettingsMigration.shouldMigrate(preferencesOf(relayUrlKey to "wss://relay.example")))
        assertEquals("Nothing stored reads as the defaults", SensorBehaviour(), decodeSensorBehaviour(null))
    }

    @Test
    fun theReadOnlyPageShowsEachSwitchAndLimit() {
        val lines = sensorBehaviourLines(SensorBehaviour(tiltAlert = false, noiseSensitivity = 1.0f))
        assertEquals(
            listOf(
                "Sit: On, placement Chair", // ISSUE-NOV25.22-O150, ISSUE-NOV26.0-O163
                "Vibration: On, alert above 0.13 g",
                "Tilt: Off",
                "Noise: On, alert above 15 dB over the background",
                "Light: On, alert above 4× the background", // ISSUE-NOV22.4-O130
                "Lift: On"
            ),
            lines
        )
    }

    /** ISSUE-NOV26.0-O163: the page names the placement chosen on the Monitor, not while sit is off. */
    @Test
    fun theReadOnlyPageNamesTheSeatPlacement() {
        val car = SensorBehaviour(placement = SeatPlacement(id = 2L, name = "Driver", type = SeatType.CAR_SEAT))
        assertEquals("Sit: On, placement Driver (car seat)", sensorBehaviourLines(car).first())
        assertEquals("Sit: Off", sensorBehaviourLines(car.copy(sitAlert = false)).first())
        assertEquals("A placement survives the store", car, decodeSensorBehaviour(Json.encodeToString(car)))
    }

    @Test
    fun noiseSwitchedOffOnTheMonitorIsNoFaultToWarnAbout() {
        assertFalse(MicrophoneState.TURNED_OFF.isNoiseDetectionOff)
        assertTrue(MicrophoneState.NO_PERMISSION.isNoiseDetectionOff)
        assertEquals("Off (noise detection switched off on the Monitor)", microphoneStatusText(MicrophoneState.TURNED_OFF))
    }
}
