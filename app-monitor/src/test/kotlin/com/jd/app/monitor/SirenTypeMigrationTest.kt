package com.jd.app.monitor

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jd.core.shared.AlertSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SIMP-NOV7.3-S1: an installed Monitor's own siren type key is moved into its stored alert settings once. */
class SirenTypeMigrationTest {

    private val relayUrlKey = stringPreferencesKey("relay_url")

    @Test
    fun theStoredSirenTypeMovesIntoTheAlertSettingsAndTheRestIsKept() = runBlocking {
        val settings = AlertSettings(tiltAlert = false, sensorRevision = 40L)
        val stored = preferencesOf(
            LEGACY_SIREN_TYPE_KEY to "Pulse",
            ALERT_SETTINGS_KEY to Json.encodeToString(settings),
            relayUrlKey to "wss://relay.example"
        )

        assertTrue(SirenTypeIntoAlertSettingsMigration.shouldMigrate(stored))
        val migrated = SirenTypeIntoAlertSettingsMigration.migrate(stored)

        assertFalse(LEGACY_SIREN_TYPE_KEY in migrated)
        assertEquals("Only the siren type changes; the Sensor's revision is kept", settings.copy(sirenType = "Pulse"), decodeAlertSettings(migrated[ALERT_SETTINGS_KEY]))
        assertEquals("wss://relay.example", migrated[relayUrlKey])
    }

    @Test
    fun aMonitorWithoutStoredAlertSettingsGetsTheDefaultsWithItsSirenType() = runBlocking {
        val migrated = SirenTypeIntoAlertSettingsMigration.migrate(preferencesOf(LEGACY_SIREN_TYPE_KEY to "Chimes"))
        assertEquals(AlertSettings(sirenType = "Chimes"), decodeAlertSettings(migrated[ALERT_SETTINGS_KEY]))
    }

    @Test
    fun settingsWithoutTheOldKeyAreLeftAlone() = runBlocking {
        assertFalse(SirenTypeIntoAlertSettingsMigration.shouldMigrate(preferencesOf(relayUrlKey to "wss://relay.example")))
    }
}
