package com.gps19.core.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gps19.core.shared.ForensicIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForensicIdentityManager: Unified identity handling for Sensor and Monitor (Point 25).
 * Centralizes hardware-bound hashed ID generation and target tracking (Revol30.1).
 * Hardened for Direct Boot (Point 77): Uses Device Protected Storage.
 */
@Singleton
class ForensicIdentityManager @Inject constructor(private val context: Context) {
    
    private val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else context

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(protectedContext.noBackupFilesDir, "forensic_identity.preferences_pb") }
    )
    
    private val selfIdKey = stringPreferencesKey("forensic_id")
    private val saltKey = stringPreferencesKey("identity_salt")
    private val targetIdKey = stringPreferencesKey("target_forensic_id")

    /**
     * Flow of this device's forensic identity.
     */
    val selfId: Flow<String> = dataStore.data.map { prefs ->
        prefs[selfIdKey] ?: "PENDING_IDENTITY"
    }

    /**
     * Flow of the paired target's identity (used by Monitor).
     */
    val targetId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[targetIdKey]
    }

    /**
     * Ensures a stable, hardware-bound identity is generated and persisted.
     */
    suspend fun ensureIdentityGenerated(rolePrefix: String) {
        val prefs = dataStore.data.first()
        
        if (prefs[selfIdKey] == null) {
            val androidId = Settings.Secure.getString(protectedContext.contentResolver, Settings.Secure.ANDROID_ID)
            val salt = prefs[saltKey] ?: ForensicIdentity.generateSalt()
            
            // Standardized hashing: rolePrefix + ANDROID_ID + salt
            val id = ForensicIdentity.create("$rolePrefix:$androidId", salt)
            
            dataStore.edit { settings ->
                settings[selfIdKey] = id
                settings[saltKey] = salt
            }
        }
    }

    suspend fun setTargetId(id: String) {
        if (ForensicIdentity.isValid(id)) {
            dataStore.edit { it[targetIdKey] = id }
        }
    }

    suspend fun clearTargetId() {
        dataStore.edit { it.remove(targetIdKey) }
    }
}
