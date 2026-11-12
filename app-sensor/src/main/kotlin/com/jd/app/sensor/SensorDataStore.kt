package com.jd.app.sensor

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SensorDataStore: Hardened for Direct Boot (Point 77).
 * Refactored Revol30.1: Identity management moved to ForensicIdentityManager.
 */
class SensorDataStore(context: Context) {
    
    private val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else context

    private val dataStore = PreferenceDataStoreFactory.create(
        migrations = listOf(RemoveSirenTypeMigration),
        produceFile = { File(protectedContext.noBackupFilesDir, "sensor_settings.preferences_pb") }
    )

    private val simHashKey = stringPreferencesKey("sim_iccid_hash")
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val alertSettingsKey = stringPreferencesKey("alert_settings_json")
    private val geofenceKey = stringPreferencesKey("geofence_json")

    val relayUrl: Flow<String?> = dataStore.data.map { it[relayUrlKey] }

    val alertSettings: Flow<AlertSettings> = dataStore.data.map { prefs ->
        val jsonStr = prefs[alertSettingsKey]
        if (!jsonStr.isNullOrBlank()) {
            runCatching { Json.decodeFromString<AlertSettings>(jsonStr) }.getOrDefault(AlertSettings())
        } else {
            AlertSettings()
        }
    }

    /** The geofence last received from the Monitor, kept across restarts so it is enforced offline (ISSUE-NOV8.13-O51). */
    val geofence: Flow<GeofenceConfig> = dataStore.data.map { prefs -> decodeGeofence(prefs[geofenceKey]) }

    /** Stores [config] unless the stored one is newer; returns true when stored. */
    suspend fun saveGeofence(config: GeofenceConfig): Boolean {
        var stored = false
        dataStore.edit { prefs ->
            if (config.revision > decodeGeofence(prefs[geofenceKey]).revision) {
                prefs[geofenceKey] = Json.encodeToString(config)
                stored = true
            }
        }
        return stored
    }

    private fun decodeGeofence(json: String?): GeofenceConfig =
        json?.let { runCatching { Json.decodeFromString<GeofenceConfig>(it) }.getOrNull() } ?: GeofenceConfig()

    suspend fun saveSimHash(hash: String) {
        dataStore.edit { it[simHashKey] = hash }
    }

    suspend fun saveRelayUrl(url: String) {
        dataStore.edit { it[relayUrlKey] = url }
    }

    suspend fun saveAlertSettings(settings: AlertSettings) {
        dataStore.edit { prefs ->
            prefs[alertSettingsKey] = Json.encodeToString(settings)
        }
    }
}

/** The siren type key written by the Sensor's former siren page; the Sensor makes no sound (ISSUE-NOV10.8-O82). */
internal val LEGACY_SIREN_TYPE_KEY = stringPreferencesKey("siren_type")

/** Removes [LEGACY_SIREN_TYPE_KEY] from an installed Sensor's settings once, on the first read (ISSUE-NOV10.8-O82). */
internal object RemoveSirenTypeMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = LEGACY_SIREN_TYPE_KEY in currentData

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply { remove(LEGACY_SIREN_TYPE_KEY) }.toPreferences()

    override suspend fun cleanUp() = Unit
}

