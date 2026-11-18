package com.jd.app.monitor

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jd.core.network.ForensicIdentityManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The [MonitorTrailFile.format] of the map's SAVE and LOAD file (ISSUE-NOV17.7-O107). */
const val MONITOR_TRAIL_FORMAT = "GPS Monitor trails"

/** Every stored trail row, the Sensor's and this phone's, saved from the map (ISSUE-NOV17.7-O107). */
@Serializable
data class MonitorTrailFile(
    /** Required in a file, so a JSON file without it is refused. */
    @Required val format: String = MONITOR_TRAIL_FORMAT,
    @Required val formatVersion: Int = SETTINGS_FILE_VERSION,
    val exportedAtMs: Long = 0L,
    val appVersion: String = "",
    val monitorId: String? = null,
    val trackerId: String? = null,
    val trails: List<TrailEntity> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
fun writeMonitorTrails(file: MonitorTrailFile, output: OutputStream) = SettingsFileJson.encodeToStream(file, output)

/** Reads a trail file; throws [SettingsFileException] with a message for the user for any other file. */
@OptIn(ExperimentalSerializationApi::class)
fun readMonitorTrails(input: InputStream): MonitorTrailFile {
    val file = try {
        SettingsFileJson.decodeFromStream<MonitorTrailFile>(input)
    } catch (_: IllegalArgumentException) { // Also every SerializationException: not JSON, or not this shape.
        throw SettingsFileException("Not a $MONITOR_TRAIL_FORMAT file")
    }
    checkSettingsFile(file.format, file.formatVersion, MONITOR_TRAIL_FORMAT)
    return file
}

/** The suggested file name, such as `GPS-Monitor-trails-20261118-1530.json`. */
fun trailFileName(nowMs: Long): String =
    "GPS-Monitor-trails-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(nowMs))}.json"

/**
 * The user cleared the stored trails of [trackerId] at [atMs] (ISSUE-NOV17.7-O107). The Sensor still holds those rows,
 * so without this mark the gap scan asked for them again and the cleared trail came back.
 */
data class TrailClear(val trackerId: String, val atMs: Long)

/** False for a row of the cleared tracker older than the clear: neither live nor backfill brings it back. */
fun TrailClear?.keeps(row: TrailEntity): Boolean = this == null || row.trackerId != trackerId || row.timestamp >= atMs

/** True when the rows before the oldest stored one were cleared, so they are no gap to request. */
fun TrailClear?.clearedStartOf(deviceId: String): Boolean = this?.trackerId == deviceId

/** Saves and loads the trail file at a place the user picked (ISSUE-NOV17.7-O107). */
@Singleton
class MonitorTrailTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityManager: ForensicIdentityManager,
    private val database: MonitorDatabase
) {
    /** Writes every stored row to [uri] and returns what was written. */
    suspend fun save(uri: Uri, nowMs: Long = System.currentTimeMillis()): MonitorTrailFile = withContext(Dispatchers.IO) {
        val file = MonitorTrailFile(
            exportedAtMs = nowMs,
            appVersion = getAppVersionName(context),
            monitorId = identityManager.selfId.first(),
            trackerId = identityManager.targetId.first(),
            trails = database.trailDao().getAll()
        )
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw SettingsFileException("The file could not be opened")
        output.use { writeMonitorTrails(file, it) }
        file
    }

    /** Adds the rows of [uri] to the stored ones; a row stored already is replaced by the file's copy. */
    suspend fun load(uri: Uri): MonitorTrailFile = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw SettingsFileException("The file could not be opened")
        val file = input.use { readMonitorTrails(it) }
        if (file.trails.isNotEmpty()) {
            database.withTransaction { file.trails.chunked(TRAIL_LOAD_CHUNK).forEach { database.trailDao().insertAll(it) } }
        }
        file
    }

    private companion object {
        const val TRAIL_LOAD_CHUNK = 5_000
    }
}
