package com.jd.core.network

import com.jd.core.shared.SilenceCause
import org.json.JSONException
import org.json.JSONObject

/**
 * The relay's notice that the Sensor [trackerId] stopped sending rows (ISSUE-NOV10.8-O86), and why ([cause],
 * ISSUE-NOV22.10-O137). [lastSensorTimeMs] and [lastSequenceId] belong to the newest row the relay got from it, or to
 * the Sensor's last word when that is newer, on the Sensor's clock, so the Monitor can tell whether it has heard from
 * the Sensor since. [silentMs] is how long the relay had heard nothing when it sent the notice.
 */
data class SensorSilence(
    val trackerId: String,
    val lastSensorTimeMs: Long,
    val lastSequenceId: Long,
    val silentMs: Long,
    val cause: SilenceCause = SilenceCause.SILENT
)

/** The wire form of [SensorSilence]: JSON text under the push's `notice` key, written by the relay. */
object SensorSilenceWire {
    const val TYPE = "sensor_silent"

    /**
     * Null for another notice type, a notice without the Sensor or its last row's time, or text that is not JSON.
     * A notice without a cause (relay before v6.049) is a plain silence.
     */
    fun decodeText(text: String): SensorSilence? = try {
        val data = JSONObject(text)
        if (data.optString("type") != TYPE) {
            null
        } else {
            SensorSilence(
                trackerId = data.getString("id"),
                lastSensorTimeMs = data.getLong("lastTs"),
                lastSequenceId = data.optLong("lastSeq"),
                silentMs = data.optLong("silentMs"),
                cause = SilenceCause.fromWire(data.optString("cause").ifEmpty { null })
            )
        }
    } catch (_: JSONException) {
        null
    }
}

/**
 * The Sensor's last word before it goes silent (ISSUE-NOV22.10-O137): the `sensor_last_word` event the relay turns
 * into an immediate [SensorSilence] notice. Its `ts` is the Sensor's clock, so a row sent before the word cannot end it.
 */
object LastWordWire {
    const val EVENT = "sensor_last_word"

    fun encode(deviceId: String, cause: SilenceCause, sentAtMs: Long): JSONObject = JSONObject().apply {
        put("id", deviceId)
        put("cause", cause.wire)
        put("ts", sentAtMs)
    }
}
