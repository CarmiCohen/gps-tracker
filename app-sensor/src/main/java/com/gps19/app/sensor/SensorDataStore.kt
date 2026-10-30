package com.gps19.app.sensor

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * SensorDataStore: Hardened for Direct Boot (Point 77).
 * Refactored Revol30.1: Identity management moved to ForensicIdentityManager.
 */
class SensorDataStore(context: Context) {
    
    private val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else context

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { File(protectedContext.noBackupFilesDir, "sensor_settings.preferences_pb") }
    )

    private val simHashKey = stringPreferencesKey("sim_iccid_hash")

    suspend fun saveSimHash(hash: String) {
        dataStore.edit { it[simHashKey] = hash }
    }
}
