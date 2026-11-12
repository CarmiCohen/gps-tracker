package com.jd.app.sensor

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV10.8-O82: the Sensor has no siren page; the siren type it once saved is removed from its settings. */
class SirenTypeRemovalTest {

    private val relayUrlKey = stringPreferencesKey("relay_url")

    @Test
    fun aStoredSirenTypeIsRemovedAndTheOtherSettingsKept() = runBlocking {
        val stored = preferencesOf(LEGACY_SIREN_TYPE_KEY to "Chimes", relayUrlKey to "wss://relay.example")

        assertTrue(RemoveSirenTypeMigration.shouldMigrate(stored))
        val migrated = RemoveSirenTypeMigration.migrate(stored)

        assertFalse(LEGACY_SIREN_TYPE_KEY in migrated)
        assertEquals("wss://relay.example", migrated[relayUrlKey])
    }

    @Test
    fun settingsWithoutASirenTypeAreLeftAlone() = runBlocking {
        assertFalse(RemoveSirenTypeMigration.shouldMigrate(preferencesOf(relayUrlKey to "wss://relay.example")))
    }
}
