package com.jd.core.network

import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Wire form of [SensorSetupReport] inside the relay's generic settings_update / settings_relay event
 * (ISSUE-NOV10.8-O83 part 2, user decision 2026-11-15), so the relay needs no change and an older Monitor ignores it.
 * It travels from the Sensor to the Monitors of its room; `id` is the Sensor's own ID.
 */
internal object SensorSetupWire {
    const val TYPE = "sensor_setup"

    fun encode(sensorId: String, report: SensorSetupReport): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("missing", JSONArray().apply { report.missing.forEach { put(it.name) } })
        put("faults", JSONArray().apply { report.faults.forEach { put(it.name) } })
        put("behaviourRev", report.behaviourRevision)
    }

    /**
     * Returns null for another settings type or a malformed payload. A name this version does not know, sent by a newer
     * Sensor, is skipped.
     */
    fun decode(data: JSONObject): SensorSetupReport? {
        if (data.optString("type") != TYPE) return null
        return try {
            SensorSetupReport(
                trackerId = data.getString("id"),
                missing = names(data.getJSONArray("missing")).mapNotNull { name -> SensorSetupItem.entries.find { it.name == name } },
                faults = names(data.getJSONArray("faults")).mapNotNull { name -> SensorFault.entries.find { it.name == name } },
                behaviourRevision = data.getLong("behaviourRev")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_setup settings_relay")
            null
        }
    }

    private fun names(array: JSONArray): List<String> = (0 until array.length()).map { array.getString(it) }
}
