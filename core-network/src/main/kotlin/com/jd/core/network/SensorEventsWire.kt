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
 * Monitors of its room; `id` is the Sensor's own ID. `after`, when present, says the events are all the Sensor's peer
 * events newer than it (ISSUE-NOV15.4-O98 part 4); a Monitor before Nov17.5 ignores it. An event's `k`, when present, is
 * its [EventEntry.kind] (ISSUE-NOV23.4-O139); a Monitor before Nov23.4 ignores it.
 */
internal object SensorEventsWire {
    const val TYPE = "sensor_events"

    fun encode(sensorId: String, events: List<EventEntry>, completeAfterMs: Long? = null): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        completeAfterMs?.let { put("after", it) }
        put("events", JSONArray().apply {
            events.forEach { event ->
                put(JSONObject().apply {
                    put("i", event.id)
                    put("t", event.timestampMs)
                    put("l", event.level.name)
                    put("m", event.message)
                    event.kind?.let { put("k", it) }
                })
            }
        })
    }

    /**
     * Returns null for another settings type or a malformed payload. Every event is the Sensor's, marked with its ID so a
     * re-paired Monitor can tell the Sensors apart (ISSUE-NOV15.6-O100 part 7); a level this version does not know,
     * sent by a newer Sensor, is shown as INFO.
     */
    fun decode(data: JSONObject): SensorEventBatch? {
        if (data.optString("type") != TYPE) return null
        return try {
            val array = data.getJSONArray("events")
            val trackerId = data.getString("id")
            SensorEventBatch(
                trackerId = trackerId,
                events = (0 until array.length()).map { index ->
                    val event = array.getJSONObject(index)
                    EventEntry(
                        id = event.getString("i"),
                        timestampMs = event.getLong("t"),
                        source = EventSource.SENSOR,
                        level = EventLevel.entries.find { it.name == event.getString("l") } ?: EventLevel.INFO,
                        message = event.getString("m"),
                        trackerId = trackerId,
                        kind = if (event.has("k")) event.getString("k") else null
                    )
                },
                completeAfterMs = if (data.has("after")) data.getLong("after") else null
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sensor_events settings_relay")
            null
        }
    }
}

/**
 * A Monitor asking the Sensor of room `id` for its peer events newer than `after`, on the Sensor's clock, when it finds
 * it missed some (ISSUE-NOV15.4-O98 part 4). It rides settings_update like the graph request, so the relay needs no
 * change and a Sensor before Nov17.5 ignores it.
 */
internal object EventsRequestWire {
    const val TYPE = "events_request"

    fun encode(targetId: String, afterMs: Long): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
        put("after", afterMs)
    }

    /** The time asked after, or null for another settings type. */
    fun decode(data: JSONObject): Long? = if (data.optString("type") == TYPE) data.optLong("after", 0L) else null
}
