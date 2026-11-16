package com.jd.core.shared

import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The settings file both apps export and import, so the phone setup survives a reinstall (ISSUE-NOV10.8-O80, user
 * decisions 2026-11-10 and 2026-11-16). Each app writes its own format, named in the file, so a Sensor file is refused
 * by the Monitor and the other way round.
 */
const val SETTINGS_FILE_VERSION = 1

/** Writes every field, defaults too, so the file is complete; reads files of later versions that only added fields. */
val SettingsFileJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/** A file that is not this app's settings file, or one this version cannot read. The message is shown to the user. */
class SettingsFileException(message: String) : Exception(message)

/** Refuses a file of another [format] than [expected], or of a later [version] than this app reads. */
fun checkSettingsFile(format: String, version: Int, expected: String) {
    if (format != expected) throw SettingsFileException("Not a $expected file")
    if (version > SETTINGS_FILE_VERSION) throw SettingsFileException("The file is from a newer app version; update the app first")
}

/** The suggested file name, such as `GPS-Monitor-settings-20261116-1530.json`. */
fun settingsFileName(appName: String, nowMs: Long): String =
    "GPS-$appName-settings-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(nowMs))}.json"

/**
 * The revision an imported setting gets: newer than the one stored here and the one in the file, so the peer takes it
 * over any copy it holds, as after an edit (ISSUE-NOV10.8-O83).
 */
fun importedRevision(stored: Long, imported: Long, nowMs: Long): Long = maxOf(nowMs, stored + 1, imported + 1)

/** The file's alert settings, with a Sensor part newer than [stored]'s, so the Sensor takes them. */
fun AlertSettings.importedOver(stored: AlertSettings, nowMs: Long): AlertSettings =
    copy(sensorRevision = importedRevision(stored.sensorRevision, sensorRevision, nowMs))

/** The file's geofence, newer than [stored], so the Sensor takes it. */
fun GeofenceConfig.importedOver(stored: GeofenceConfig, nowMs: Long): GeofenceConfig =
    copy(revision = importedRevision(stored.revision, revision, nowMs))
