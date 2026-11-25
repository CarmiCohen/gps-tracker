package com.jd.app.monitor

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.importedOver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.monitorDataStore by preferencesDataStore(
    name = "monitor_settings",
    produceMigrations = { listOf(SirenTypeIntoAlertSettingsMigration) }
)

/** The stored [AlertSettings] as JSON. */
internal val ALERT_SETTINGS_KEY = stringPreferencesKey("alert_settings_json")

/** The siren type's own key before SIMP-NOV7.3-S1; now [AlertSettings.sirenType] is the only copy. */
internal val LEGACY_SIREN_TYPE_KEY = stringPreferencesKey("siren_type")

internal fun decodeAlertSettings(json: String?): AlertSettings =
    json?.takeIf { it.isNotBlank() }?.let { runCatching { Json.decodeFromString<AlertSettings>(it) }.getOrNull() } ?: AlertSettings()

/**
 * Moves an installed Monitor's [LEGACY_SIREN_TYPE_KEY] into its stored [AlertSettings] once, on the first read
 * (SIMP-NOV7.3-S1). The Sensor's revision is kept: the siren type is not the Sensor's part.
 */
internal object SirenTypeIntoAlertSettingsMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = LEGACY_SIREN_TYPE_KEY in currentData

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        val sirenType = currentData[LEGACY_SIREN_TYPE_KEY] ?: return@apply
        this[ALERT_SETTINGS_KEY] = Json.encodeToString(decodeAlertSettings(currentData[ALERT_SETTINGS_KEY]).copy(sirenType = sirenType))
        remove(LEGACY_SIREN_TYPE_KEY)
    }.toPreferences()

    override suspend fun cleanUp() = Unit
}

/**
 * MonitorDataStore: Persistent settings for the Watcher application.
 * Hardened Revol30.1: Identity and Target tracking moved to ForensicIdentityManager.
 * This class now handles UI and application-specific settings only.
 */
class MonitorDataStore(private val context: Context) {
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val isDndBypassEnabledKey = booleanPreferencesKey("dnd_bypass_enabled")
    private val alertSettingsKey = ALERT_SETTINGS_KEY
    private val geofenceKey = stringPreferencesKey("geofence_json")
    private val trailClearTrackerKey = stringPreferencesKey("trail_clear_tracker")
    private val trailClearAtKey = longPreferencesKey("trail_clear_at_ms")
    private val notifiedSetupWarningsKey = stringSetPreferencesKey("notified_setup_warnings")

    /**
     * The Sensor phone warnings the last setup notification covered, so each one is notified once, also across
     * restarts of the Monitor (ISSUE-NOV10.8-O86, user decision 2026-11-18).
     */
    val notifiedSetupWarnings: Flow<Set<String>> = context.monitorDataStore.data.map { it[notifiedSetupWarningsKey] ?: emptySet() }

    suspend fun saveNotifiedSetupWarnings(warnings: Set<String>) {
        context.monitorDataStore.edit { it[notifiedSetupWarningsKey] = warnings }
    }

    /** The user's last trail clear, or null (ISSUE-NOV17.7-O107). */
    val trailClear: Flow<TrailClear?> = context.monitorDataStore.data.map { prefs ->
        val trackerId = prefs[trailClearTrackerKey]
        val atMs = prefs[trailClearAtKey]
        if (trackerId != null && atMs != null) TrailClear(trackerId, atMs) else null
    }

    /** Stores [clear]; null forgets the last one, so a new pairing gets the Sensor's whole history again. */
    suspend fun saveTrailClear(clear: TrailClear?) {
        context.monitorDataStore.edit { prefs ->
            if (clear == null) {
                prefs.remove(trailClearTrackerKey)
                prefs.remove(trailClearAtKey)
            } else {
                prefs[trailClearTrackerKey] = clear.trackerId
                prefs[trailClearAtKey] = clear.atMs
            }
        }
    }

    val relayUrl: Flow<String?> = context.monitorDataStore.data.map { it[relayUrlKey] }
    val isDndBypassEnabled: Flow<Boolean> = context.monitorDataStore.data.map { it[isDndBypassEnabledKey] ?: false }

    val alertSettings: Flow<AlertSettings> = context.monitorDataStore.data.map { prefs -> decodeAlertSettings(prefs[alertSettingsKey]) }

    /** The user's Home Points and radius; the Monitor is their source and the Sensor enforces them (ISSUE-NOV8.13-O51). */
    val geofence: Flow<GeofenceConfig> = context.monitorDataStore.data.map { prefs -> decodeGeofence(prefs[geofenceKey]) }

    /** Applies [change] to the stored geofence and stamps a new revision, so the Sensor takes it over any older copy. */
    suspend fun editGeofence(nowMs: Long = System.currentTimeMillis(), change: (GeofenceConfig) -> GeofenceConfig) {
        context.monitorDataStore.edit { prefs ->
            val current = decodeGeofence(prefs[geofenceKey])
            prefs[geofenceKey] = Json.encodeToString(change(current).revisedAfter(current, nowMs))
        }
    }

    /**
     * Stores [config], received from the Sensor or another Monitor, with its own revision when it is newer than the
     * stored one, so this Monitor shows and edits the one shared set (ISSUE-NOV22.7-O134); returns true when stored.
     */
    suspend fun adoptGeofence(config: GeofenceConfig): Boolean {
        var stored = false
        context.monitorDataStore.edit { prefs ->
            if (config.supersedes(decodeGeofence(prefs[geofenceKey]))) {
                prefs[geofenceKey] = Json.encodeToString(config)
                stored = true
            }
        }
        return stored
    }

    private fun decodeGeofence(json: String?): GeofenceConfig =
        json?.let { runCatching { Json.decodeFromString<GeofenceConfig>(it) }.getOrNull() } ?: GeofenceConfig()

    suspend fun saveRelayUrl(url: String) {
        context.monitorDataStore.edit { it[relayUrlKey] = url }
    }

    suspend fun setDndBypass(enabled: Boolean) {
        context.monitorDataStore.edit { it[isDndBypassEnabledKey] = enabled }
    }

    /**
     * Stores [settings]; a change to the Sensor's part gets a new revision, so the Sensor takes it over any older copy
     * (ISSUE-NOV10.8-O83). The revision is taken from the stored settings, not from [settings], which may be stale.
     */
    suspend fun saveAlertSettings(settings: AlertSettings, nowMs: Long = System.currentTimeMillis()) {
        context.monitorDataStore.edit { prefs ->
            prefs[alertSettingsKey] = Json.encodeToString(settings.revisedAfter(decodeAlertSettings(prefs[alertSettingsKey]), nowMs))
        }
    }

    /** [file] with the stored settings filled in (ISSUE-NOV10.8-O80). */
    suspend fun exportInto(file: MonitorSettingsFile): MonitorSettingsFile {
        val prefs = context.monitorDataStore.data.first()
        return file.copy(
            relayUrl = prefs[relayUrlKey],
            dndBypass = prefs[isDndBypassEnabledKey] ?: false,
            alertSettings = decodeAlertSettings(prefs[alertSettingsKey]),
            geofence = decodeGeofence(prefs[geofenceKey])
        )
    }

    /**
     * Replaces the stored settings with [file]'s in one edit. The alert settings and the geofence get new revisions, so
     * the Sensor takes them over the copies it holds (ISSUE-NOV10.8-O80).
     */
    suspend fun importFrom(file: MonitorSettingsFile, nowMs: Long = System.currentTimeMillis()) {
        context.monitorDataStore.edit { prefs ->
            val relayUrl = file.relayUrl
            if (relayUrl == null) prefs.remove(relayUrlKey) else prefs[relayUrlKey] = relayUrl
            prefs[isDndBypassEnabledKey] = file.dndBypass
            prefs[alertSettingsKey] = Json.encodeToString(file.alertSettings.importedOver(decodeAlertSettings(prefs[alertSettingsKey]), nowMs))
            prefs[geofenceKey] = Json.encodeToString(file.geofence.importedOver(decodeGeofence(prefs[geofenceKey]), nowMs))
        }
    }
}

