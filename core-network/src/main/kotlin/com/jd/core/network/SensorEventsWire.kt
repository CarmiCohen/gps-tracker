package com.jd.core.network

import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.shared.SensorEventBatch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Wire form of the Sensor's newest events inside the relay's generic settings_update / settings_relay event
 * (ISSUE-NOV10.8-O84), so the relay needs no change and an older Monitor ignores it. It travels from the Sensor to the
 * Monitors of its room; `id` is the Sensor's own ID.
 */
internal object SensorEventsWire {
    const val TYPE = "sensor_events"

    fun encode(sensorId: String, events: List<EventEntry>): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("events", JSONArray().apply {
            events.forEach { event ->
                put(JSONObject().apply {
                    put("i", event.id)
                    put("t", event.timestampMs)
                    put("l", event.level.name)
                    put("m", event.message)
                })
            }
        })
    }

    /**
     * Returns null for another settings type or a malformed payload. Every event is the Sensor's; a level this version
     * does not know, sent by a newer Sensor, is shown as INFO.
     */
    fun decode(data: JSONObject): SensorEventBatch? {
        if (data.optString("type") != TYPE) return null
        return try {
            val array = data.getJSONArray("events")
            SensorEventBatch(
                trackerId = data.getString("id"),
                events = (0 until array.length()).map { index ->
                    val event = array.getJSONObject(index)
                    EventEntry(
                        id = event.getString("i"),
                        timestampMs = event.getLong("t"),
                        source = EventSource.SENSOR,
                        level = EventLevel.entries.find { it.name == event.getString("l") } ?: EventLevel.INFO,
                        message = event.getString("m")
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_events settings_relay")
            null
        }
    }
}
