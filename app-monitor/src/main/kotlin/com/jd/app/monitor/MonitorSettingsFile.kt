package com.jd.app.monitor

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SETTINGS_FILE_VERSION
import com.jd.core.shared.SettingsFileException
import com.jd.core.shared.SettingsFileJson
import com.jd.core.shared.checkSettingsFile
import com.jd.core.ui.getAppVersionName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** The [MonitorSettingsFile.format] of the Monitor's settings file. */
const val MONITOR_SETTINGS_FORMAT = "GPS Monitor settings"

/**
 * Everything the Monitor needs after a reinstall (ISSUE-NOV10.8-O80, user decisions 2026-11-10 and 2026-11-16): the
 * pairing, the relay, the alarm and siren settings, the Home Points with their distance, and all stored trail rows.
 */
@Serializable
data class MonitorSettingsFile(
    /** Required in a file, so a JSON file without it is refused. */
    @Required val format: String = MONITOR_SETTINGS_FORMAT,
    @Required val formatVersion: Int = SETTINGS_FILE_VERSION,
    val exportedAtMs: Long = 0L,
    val appVersion: String = "",
    val monitorId: String? = null,
    /** The paired Sensor; null when the Monitor was not paired. */
    val trackerId: String? = null,
    /** Null when the default relay was in use. */
    val relayUrl: String? = null,
    val dndBypass: Boolean = false,
    val alertSettings: AlertSettings = AlertSettings(),
    val sirenType: String = "Siren",
    val geofence: GeofenceConfig = GeofenceConfig(),
    val trails: List<TrailEntity> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
fun writeMonitorSettings(file: MonitorSettingsFile, output: OutputStream) = SettingsFileJson.encodeToStream(file, output)

/** Reads a Monitor settings file; throws [SettingsFileException] with a message for the user for any other file. */
@OptIn(ExperimentalSerializationApi::class)
fun readMonitorSettings(input: InputStream): MonitorSettingsFile {
    val file = try {
        SettingsFileJson.decodeFromStream<MonitorSettingsFile>(input)
    } catch (_: IllegalArgumentException) { // Also every SerializationException: not JSON, or not this shape.
        throw SettingsFileException("Not a $MONITOR_SETTINGS_FORMAT file")
    }
    checkSettingsFile(file.format, file.formatVersion, MONITOR_SETTINGS_FORMAT)
    return file
}

/** Writes and reads the Monitor's settings file at a place the user picked (ISSUE-NOV10.8-O80). */
@Singleton
class MonitorSettingsTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: MonitorDataStore,
    private val identityManager: ForensicIdentityManager,
    private val database: MonitorDatabase
) {
    /** Writes the settings to [uri] and returns what was written. */
    suspend fun export(uri: Uri, nowMs: Long = System.currentTimeMillis()): MonitorSettingsFile = withContext(Dispatchers.IO) {
        val file = dataStore.exportInto(
            MonitorSettingsFile(
                exportedAtMs = nowMs,
                appVersion = getAppVersionName(context),
                monitorId = identityManager.selfId.first(),
                trackerId = identityManager.targetId.first(),
                trails = database.trailDao().getAll()
            )
        )
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw SettingsFileException("The file could not be opened")
        output.use { writeMonitorSettings(file, it) }
        file
    }

    /**
     * Reads [uri] and replaces this phone's settings and pairing with the file's. The trail rows are added to the stored
     * ones, so nothing stored is lost. The pairing is set last: it opens the map and starts the session.
     */
    suspend fun import(uri: Uri, nowMs: Long = System.currentTimeMillis()): MonitorSettingsFile = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw SettingsFileException("The file could not be opened")
        val file = input.use { readMonitorSettings(it) }
        dataStore.importFrom(file, nowMs)
        if (file.trails.isNotEmpty()) {
            database.withTransaction { file.trails.chunked(TRAIL_IMPORT_CHUNK).forEach { database.trailDao().insertAll(it) } }
        }
        // A Monitor ID equal to the file's Tracker ID is refused, as in the Setup dialog; the own trail follows a new ID
        // (ISSUE-NOV22.7-O135).
        val oldMonitorId = identityManager.selfId.first()
        file.monitorId?.trim()?.takeIf { it != oldMonitorId && ForensicIdentity.monitorIdProblem(it, file.trackerId) == null }?.let {
            identityManager.setSelfId(it)
            database.trailDao().moveLocalTrail(oldMonitorId, it)
        }
        val trackerId = file.trackerId
        if (trackerId != null && ForensicIdentity.isValid(trackerId)) identityManager.setTargetId(trackerId) else identityManager.clearTargetId()
        file
    }

    private companion object {
        const val TRAIL_IMPORT_CHUNK = 5_000
    }
}
