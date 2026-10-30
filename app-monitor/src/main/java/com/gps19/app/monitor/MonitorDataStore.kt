package com.gps19.app.monitor

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.monitorDataStore by preferencesDataStore(name = "monitor_settings")

/**
 * MonitorDataStore: Persistent settings for the Watcher application.
 * Hardened Revol30.1: Identity and Target tracking moved to ForensicIdentityManager.
 * This class now handles UI and application-specific settings only.
 */
class MonitorDataStore(private val context: Context) {
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val isDndBypassEnabledKey = booleanPreferencesKey("dnd_bypass_enabled")

    val relayUrl: Flow<String?> = context.monitorDataStore.data.map { it[relayUrlKey] }
    val isDndBypassEnabled: Flow<Boolean> = context.monitorDataStore.data.map { it[isDndBypassEnabledKey] ?: false }

    suspend fun setDndBypass(enabled: Boolean) {
        context.monitorDataStore.edit { it[isDndBypassEnabledKey] = enabled }
    }
}
