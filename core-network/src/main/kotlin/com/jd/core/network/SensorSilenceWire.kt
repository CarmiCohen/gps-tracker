package com.jd.core.network

import org.json.JSONException
import org.json.JSONObject

/**
 * The relay's notice that the Sensor [trackerId] sent no row for [silentMs] (ISSUE-NOV10.8-O86). [lastSensorTimeMs]
 * and [lastSequenceId] belong to the newest row the relay got from it, on the Sensor's clock, so the Monitor can tell
 * whether it has heard from the Sensor since.
 */
data class SensorSilence(
    val trackerId: String,
    val lastSensorTimeMs: Long,
    val lastSequenceId: Long,
    val silentMs: Long
)

/** The wire form of [SensorSilence]: JSON text under the push's `notice` key, written by the relay. */
object SensorSilenceWire {
    const val TYPE = "sensor_silent"

    /** Null for another notice type, a notice without the Sensor or its last row's time, or text that is not JSON. */
    fun decodeText(text: String): SensorSilence? = try {
        val data = JSONObject(text)
        if (data.optString("type") != TYPE) {
            null
        } else {
            SensorSilence(
                trackerId = data.getString("id"),
                lastSensorTimeMs = data.getLong("lastTs"),
                lastSequenceId = data.optLong("lastSeq"),
                silentMs = data.optLong("silentMs")
            )
        }
    } catch (_: JSONException) {
        null
    }
}
