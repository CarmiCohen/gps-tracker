package com.gps19.app.sensor

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.*
import com.gps19.core.shared.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.*

/**
 * SensorDataStore: Hardened for Direct Boot (Point 77) and Salted Identity (Point 25).
 * Uses explicit file paths in Device Protected Storage.
 */
class SensorDataStore(context: Context) {
    
    private val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else context

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { File(protectedContext.noBackupFilesDir, "sensor_settings.preferences_pb") }
    )

    private val trackerIdKey = stringPreferencesKey("tracker_id")
    private val saltKey = stringPreferencesKey("identity_salt")
    private val simHashKey = stringPreferencesKey("sim_iccid_hash")

    /**
     * Returns a persistent, salted, and hashed Tracker ID (Point 25).
     * Automatically generates and persists identity if missing.
     */
    val trackerId: Flow<String> = dataStore.data.map { prefs ->
        prefs[trackerIdKey] ?: "PENDING_IDENTITY"
    }

    suspend fun ensureIdentityGenerated() {
        val prefs = dataStore.data.first()
        if (prefs[trackerIdKey] == null) {
            val androidId = Settings.Secure.getString(protectedContext.contentResolver, Settings.Secure.ANDROID_ID)
            val salt = prefs[saltKey] ?: UUID.randomUUID().toString()
            val id = CryptoUtils.hashIdentity(androidId ?: "unknown", salt)
            dataStore.edit {
                it[trackerIdKey] = id
                it[saltKey] = salt
            }
        }
    }

    suspend fun saveSimHash(hash: String) {
        dataStore.edit { it[simHashKey] = hash }
    }
}
