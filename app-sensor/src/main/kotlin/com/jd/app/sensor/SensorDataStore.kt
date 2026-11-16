package com.jd.app.sensor

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SensorBehaviour
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
        migrations = listOf(RemoveSirenTypeMigration, LegacyAlertSettingsMigration),
        produceFile = { File(protectedContext.noBackupFilesDir, "sensor_settings.preferences_pb") }
    )

    private val simHashKey = stringPreferencesKey("sim_iccid_hash")
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val geofenceKey = stringPreferencesKey("geofence_json")

    val relayUrl: Flow<String?> = dataStore.data.map { it[relayUrlKey] }

    /** The detection settings last received from the Monitor, kept across restarts (ISSUE-NOV10.8-O83). */
    val sensorBehaviour: Flow<SensorBehaviour> = dataStore.data.map { prefs -> decodeSensorBehaviour(prefs[SENSOR_BEHAVIOUR_KEY]) }

    /** Stores [behaviour] unless the stored one is as new or newer; returns true when stored. */
    suspend fun saveSensorBehaviour(behaviour: SensorBehaviour): Boolean {
        var stored = false
        dataStore.edit { prefs ->
            if (behaviour.revision > decodeSensorBehaviour(prefs[SENSOR_BEHAVIOUR_KEY]).revision) {
                prefs[SENSOR_BEHAVIOUR_KEY] = Json.encodeToString(behaviour)
                stored = true
            }
        }
        return stored
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

    /**
     * The lowest sequence number the service may give a row, set by an imported settings file: a reinstalled Sensor then
     * continues above the numbers the Monitor already holds instead of restarting at 0 (ISSUE-NOV10.8-O80, ISSUE-NOV5.2-O16).
     */
    val sequenceFloor: Flow<Long> = dataStore.data.map { it[SEQUENCE_FLOOR_KEY] ?: 0L }

    /** [file] with the stored settings filled in (ISSUE-NOV10.8-O80). */
    suspend fun exportInto(file: SensorSettingsFile): SensorSettingsFile {
        val prefs = dataStore.data.first()
        return file.copy(
            relayUrl = prefs[relayUrlKey],
            behaviour = decodeSensorBehaviour(prefs[SENSOR_BEHAVIOUR_KEY]),
            geofence = decodeGeofence(prefs[geofenceKey])
        )
    }

    /**
     * Stores [file]'s settings. The detection settings and the geofence are the Monitor's: each is kept only while no
     * newer one is stored, so the Monitor's next message still replaces them. The floor only ever rises.
     */
    suspend fun importFrom(file: SensorSettingsFile, nowMs: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            val relayUrl = file.relayUrl
            if (relayUrl == null) prefs.remove(relayUrlKey) else prefs[relayUrlKey] = relayUrl
            file.sequenceFloor(nowMs)?.let { floor -> prefs[SEQUENCE_FLOOR_KEY] = maxOf(floor, prefs[SEQUENCE_FLOOR_KEY] ?: 0L) }
        }
        saveSensorBehaviour(file.behaviour)
        saveGeofence(file.geofence)
    }
}

/** Where [SensorDataStore.sequenceFloor] is stored. */
internal val SEQUENCE_FLOOR_KEY = longPreferencesKey("sequence_floor")

/** Where [SensorDataStore.sensorBehaviour] is stored. */
internal val SENSOR_BEHAVIOUR_KEY = stringPreferencesKey("sensor_behaviour_json")

internal fun decodeSensorBehaviour(json: String?): SensorBehaviour =
    json?.let { runCatching { Json.decodeFromString<SensorBehaviour>(it) }.getOrNull() } ?: SensorBehaviour()

/** The Alert Settings the Sensor kept before its sliders moved to the Monitor (ISSUE-NOV10.8-O83). */
internal val LEGACY_ALERT_SETTINGS_KEY = stringPreferencesKey("alert_settings_json")

/**
 * Once, on the first read: the tilt and vibration limits set on an installed Sensor become its starting
 * [SensorBehaviour] at revision 0, so it keeps detecting as tuned until the Monitor's first message replaces them (user
 * decision 2026-11-15: the Monitor's values win); then the old settings are removed (ISSUE-NOV10.8-O83).
 */
internal object LegacyAlertSettingsMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = LEGACY_ALERT_SETTINGS_KEY in currentData

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        val legacy = runCatching { Json.decodeFromString<AlertSettings>(currentData[LEGACY_ALERT_SETTINGS_KEY].orEmpty()) }.getOrNull()
        if (legacy != null && SENSOR_BEHAVIOUR_KEY !in currentData) {
            this[SENSOR_BEHAVIOUR_KEY] = Json.encodeToString(
                SensorBehaviour(tiltSensitivity = legacy.tiltSensitivity, vibrationSensitivity = legacy.vibrationSensitivity)
            )
        }
        remove(LEGACY_ALERT_SETTINGS_KEY)
    }.toPreferences()

    override suspend fun cleanUp() = Unit
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

