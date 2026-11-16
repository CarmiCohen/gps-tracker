package com.jd.app.sensor

import android.content.Context
import android.net.Uri
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.ForensicIdentity
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SETTINGS_FILE_VERSION
import com.jd.core.shared.SensorBehaviour
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

/** The [SensorSettingsFile.format] of the Sensor's settings file. */
const val SENSOR_SETTINGS_FORMAT = "GPS Sensor settings"

/** The sensing loop's shortest interval: the service gives at most one sequence number per this many ms. */
internal const val FASTEST_ROW_INTERVAL_MS = 100L

/** Added to the imported floor, for rows written in the same 100 ms steps or by a clock a little behind. */
internal const val SEQUENCE_FLOOR_MARGIN = 1_000L

/**
 * Everything the Sensor needs after a reinstall (ISSUE-NOV10.8-O80, user decisions 2026-11-10 and 2026-11-16): its
 * device ID (the pairing), the relay, the Monitor's detection settings and geofence, so they are enforced before the
 * Monitor's first message, and the next sequence number (ISSUE-NOV5.2-O16).
 */
@Serializable
data class SensorSettingsFile(
    /** Required in a file, so a JSON file without it is refused. */
    @Required val format: String = SENSOR_SETTINGS_FORMAT,
    @Required val formatVersion: Int = SETTINGS_FILE_VERSION,
    val exportedAtMs: Long = 0L,
    val appVersion: String = "",
    val sensorId: String? = null,
    /** Null when the default relay was in use. */
    val relayUrl: String? = null,
    val behaviour: SensorBehaviour = SensorBehaviour(),
    val geofence: GeofenceConfig = GeofenceConfig(),
    /** The number the next row would have had at the export; null when unknown (no service ran since the app started). */
    val nextSequenceId: Long? = null
) {
    /**
     * The lowest sequence number the imported Sensor may use: the exported one, plus every number the old Sensor could
     * have given since the export, at one row per [FASTEST_ROW_INTERVAL_MS], plus [SEQUENCE_FLOOR_MARGIN]. The Monitor
     * then never holds a row with the same number, which it would replace (ISSUE-NOV5.2-O16). Null without a number.
     */
    fun sequenceFloor(nowMs: Long): Long? = nextSequenceId?.takeIf { it > 0 }?.let { next ->
        next + (nowMs - exportedAtMs).coerceAtLeast(0L) / FASTEST_ROW_INTERVAL_MS + SEQUENCE_FLOOR_MARGIN
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun writeSensorSettings(file: SensorSettingsFile, output: OutputStream) = SettingsFileJson.encodeToStream(file, output)

/** Reads a Sensor settings file; throws [SettingsFileException] with a message for the user for any other file. */
@OptIn(ExperimentalSerializationApi::class)
fun readSensorSettings(input: InputStream): SensorSettingsFile {
    val file = try {
        SettingsFileJson.decodeFromStream<SensorSettingsFile>(input)
    } catch (_: IllegalArgumentException) { // Also every SerializationException: not JSON, or not this shape.
        throw SettingsFileException("Not a $SENSOR_SETTINGS_FORMAT file")
    }
    checkSettingsFile(file.format, file.formatVersion, SENSOR_SETTINGS_FORMAT)
    return file
}

/** Writes and reads the Sensor's settings file at a place the user picked (ISSUE-NOV10.8-O80). */
@Singleton
class SensorSettingsTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: SensorDataStore,
    private val identityManager: ForensicIdentityManager,
    private val serviceStatus: SensorServiceStatus
) {
    /** Writes the settings to [uri] and returns what was written. */
    suspend fun export(uri: Uri, nowMs: Long = System.currentTimeMillis()): SensorSettingsFile = withContext(Dispatchers.IO) {
        val next = maxOf(serviceStatus.nextSequence.value, dataStore.sequenceFloor.first())
        val file = dataStore.exportInto(
            SensorSettingsFile(
                exportedAtMs = nowMs,
                appVersion = getAppVersionName(context),
                sensorId = identityManager.selfId.first(),
                nextSequenceId = next.takeIf { it > 0 }
            )
        )
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw SettingsFileException("The file could not be opened")
        output.use { writeSensorSettings(file, it) }
        file
    }

    /** Reads [uri] and stores its settings; the running service reconnects with the imported ID and relay. */
    suspend fun import(uri: Uri, nowMs: Long = System.currentTimeMillis()): SensorSettingsFile = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw SettingsFileException("The file could not be opened")
        val file = input.use { readSensorSettings(it) }
        dataStore.importFrom(file, nowMs)
        file.sensorId?.takeIf { ForensicIdentity.isValid(it) }?.let { identityManager.setSelfId(it) }
        file
    }
}
