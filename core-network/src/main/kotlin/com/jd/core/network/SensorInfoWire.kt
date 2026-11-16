package com.jd.core.network

import com.jd.core.shared.SensorInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import timber.log.Timber

/**
 * Wire form of [SensorInfo] inside the relay's generic settings_update / settings_relay event (ISSUE-NOV10.8-O84
 * part 2), so the relay needs no change and an older Monitor ignores it. It travels from the Sensor to the Monitors of
 * its room; `id` is the Sensor's own ID, `info` the Info as JSON text, whose unknown fields a reader skips.
 */
internal object SensorInfoWire {
    const val TYPE = "sensor_info"

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(sensorId: String, info: SensorInfo): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("info", json.encodeToString(info))
    }

    /** Returns null for another settings type or a malformed payload. */
    fun decode(data: JSONObject): SensorInfo? {
        if (data.optString("type") != TYPE) return null
        return try {
            json.decodeFromString<SensorInfo>(data.getString("info"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_info settings_relay")
            null
        }
    }
}

/**
 * A Monitor's Info screen asking the Sensor of room `id` for its Info (ISSUE-NOV10.8-O84 part 2); the Sensor answers
 * for [com.jd.core.shared.SENSOR_INFO_ANSWER_WINDOW_MS], so only an open screen costs data.
 */
internal object InfoRequestWire {
    const val TYPE = "info_request"

    fun encode(targetId: String): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
    }

    fun isRequest(data: JSONObject): Boolean = data.optString("type") == TYPE
}

/**
 * The relay's `ping_cmd` goes to the whole room as `ping_relay`, the sender included, so the sender's own echo gives the
 * round trip to the relay and back with no relay change (ISSUE-NOV10.8-O84 part 2). `from` tells whose ping it is; `t`
 * is the sender's monotonic time in milliseconds.
 */
internal object LatencyPingWire {
    fun encode(room: String, deviceId: String, sentAtMs: Long): JSONObject = JSONObject().apply {
        put("id", room)
        put("from", deviceId)
        put("t", sentAtMs)
    }

    /** The round trip of this device's own ping, or null for another device's ping or a malformed one. */
    fun roundTripMs(data: JSONObject, deviceId: String, nowMs: Long): Long? {
        if (data.optString("from") != deviceId || !data.has("t")) return null
        return (nowMs - data.optLong("t")).takeIf { it >= 0L }
    }
}
