package com.jd.app.monitor

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.jd.core.shared.AlertSettings
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

    val relayUrl: Flow<String?> = context.monitorDataStore.data.map { it[relayUrlKey] }
    val isDndBypassEnabled: Flow<Boolean> = context.monitorDataStore.data.map { it[isDndBypassEnabledKey] ?: false }

    val alertSettings: Flow<AlertSettings> = context.monitorDataStore.data.map { prefs ->
        val jsonStr = prefs[alertSettingsKey]
        if (!jsonStr.isNullOrBlank()) {
            runCatching { Json.decodeFromString<AlertSettings>(jsonStr) }.getOrDefault(AlertSettings())
        } else {
            AlertSettings()
        }
    }

    val sirenType: Flow<String> = context.monitorDataStore.data.map { prefs ->
        prefs[sirenTypeKey] ?: "Siren"
    }

    suspend fun saveRelayUrl(url: String) {
        context.monitorDataStore.edit { it[relayUrlKey] = url }
    }

    suspend fun setDndBypass(enabled: Boolean) {
        context.monitorDataStore.edit { it[isDndBypassEnabledKey] = enabled }
    }

    suspend fun saveAlertSettings(settings: AlertSettings) {
        context.monitorDataStore.edit { prefs ->
            prefs[alertSettingsKey] = Json.encodeToString(settings)
        }
    }

    suspend fun saveSirenType(type: String) {
        context.monitorDataStore.edit { prefs ->
            prefs[sirenTypeKey] = type
        }
    }
}

