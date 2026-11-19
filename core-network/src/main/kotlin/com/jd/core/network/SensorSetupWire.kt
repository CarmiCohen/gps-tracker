package com.jd.core.network

import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Wire form of [SensorSetupReport] inside the relay's generic settings_update / settings_relay event
 * (ISSUE-NOV10.8-O83 part 2, user decision 2026-11-15), so an older Monitor ignores it.
 * It travels from the Sensor to the Monitors of its room; `id` is the Sensor's own ID. Since relay v6.047 the relay
 * also pushes the same JSON, as text under the push's `setup` key, when what the Sensor lacks changes (ISSUE-NOV10.8-O86).
 */
object SensorSetupWire {
    const val TYPE = "sensor_setup"

    fun encode(sensorId: String, report: SensorSetupReport, sentAtMs: Long = report.sentAtMs): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("missing", JSONArray().apply { report.missing.forEach { put(it.name) } })
        put("faults", JSONArray().apply { report.faults.forEach { put(it.name) } })
        put("behaviourRev", report.behaviourRevision)
        put("sentAt", sentAtMs)
    }

    /**
     * Returns null for another settings type or a malformed payload. A name this version does not know, sent by a newer
     * Sensor, is skipped; a report without `sentAt` (a Sensor before Nov18.12) has time 0.
     */
    fun decode(data: JSONObject): SensorSetupReport? {
        if (data.optString("type") != TYPE) return null
        return try {
            SensorSetupReport(
                trackerId = data.getString("id"),
                missing = names(data.getJSONArray("missing")).mapNotNull { name -> SensorSetupItem.entries.find { it.name == name } },
                faults = names(data.getJSONArray("faults")).mapNotNull { name -> SensorFault.entries.find { it.name == name } },
                behaviourRevision = data.getLong("behaviourRev"),
                sentAtMs = data.optLong("sentAt")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_setup settings_relay")
            null
        }
    }

    /** [decode] for the pushed JSON text; null for text that is not JSON. */
    fun decodeText(text: String): SensorSetupReport? = try {
        decode(JSONObject(text))
    } catch (_: JSONException) {
        null
    }

    private fun names(array: JSONArray): List<String> = (0 until array.length()).map { array.getString(it) }
}
