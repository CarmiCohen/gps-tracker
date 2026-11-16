package com.jd.app.monitor

import android.content.Context
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

val Context.monitorDataStore by preferencesDataStore(name = "monitor_settings")

/**
 * MonitorDataStore: Persistent settings for the Watcher application.
 * Hardened Revol30.1: Identity and Target tracking moved to ForensicIdentityManager.
 * This class now handles UI and application-specific settings only.
 */
class MonitorDataStore(private val context: Context) {
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val isDndBypassEnabledKey = booleanPreferencesKey("dnd_bypass_enabled")
    private val alertSettingsKey = stringPreferencesKey("alert_settings_json")
    private val sirenTypeKey = stringPreferencesKey("siren_type")
    private val geofenceKey = stringPreferencesKey("geofence_json")

    val relayUrl: Flow<String?> = context.monitorDataStore.data.map { it[relayUrlKey] }
    val isDndBypassEnabled: Flow<Boolean> = context.monitorDataStore.data.map { it[isDndBypassEnabledKey] ?: false }

    val alertSettings: Flow<AlertSettings> = context.monitorDataStore.data.map { prefs -> decodeAlertSettings(prefs[alertSettingsKey]) }

    val sirenType: Flow<String> = context.monitorDataStore.data.map { prefs ->
        prefs[sirenTypeKey] ?: "Siren"
    }

    /** The user's Home Points and radius; the Monitor is their source and the Sensor enforces them (ISSUE-NOV8.13-O51). */
    val geofence: Flow<GeofenceConfig> = context.monitorDataStore.data.map { prefs -> decodeGeofence(prefs[geofenceKey]) }

    /** Applies [change] to the stored geofence and stamps a new revision, so the Sensor takes it over any older copy. */
    suspend fun editGeofence(nowMs: Long = System.currentTimeMillis(), change: (GeofenceConfig) -> GeofenceConfig) {
        context.monitorDataStore.edit { prefs ->
            val current = decodeGeofence(prefs[geofenceKey])
            prefs[geofenceKey] = Json.encodeToString(change(current).revisedAfter(current, nowMs))
        }
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

    private fun decodeAlertSettings(json: String?): AlertSettings =
        json?.takeIf { it.isNotBlank() }?.let { runCatching { Json.decodeFromString<AlertSettings>(it) }.getOrNull() } ?: AlertSettings()

    suspend fun saveSirenType(type: String) {
        context.monitorDataStore.edit { prefs ->
            prefs[sirenTypeKey] = type
        }
    }

    /** [file] with the stored settings filled in (ISSUE-NOV10.8-O80). */
    suspend fun exportInto(file: MonitorSettingsFile): MonitorSettingsFile {
        val prefs = context.monitorDataStore.data.first()
        return file.copy(
            relayUrl = prefs[relayUrlKey],
            dndBypass = prefs[isDndBypassEnabledKey] ?: false,
            alertSettings = decodeAlertSettings(prefs[alertSettingsKey]),
            sirenType = prefs[sirenTypeKey] ?: "Siren",
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
            prefs[sirenTypeKey] = file.sirenType
            prefs[geofenceKey] = Json.encodeToString(file.geofence.importedOver(decodeGeofence(prefs[geofenceKey]), nowMs))
        }
    }
}

