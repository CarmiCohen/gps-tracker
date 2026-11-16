package com.jd.core.network

import com.jd.core.shared.GraphSample
import com.jd.core.shared.SensorGraphBatch
import com.jd.core.shared.decodeGraphSamples
import com.jd.core.shared.encodeGraphSamples
import org.json.JSONObject
import timber.log.Timber

/**
 * A Monitor's Graphs screen asking the Sensor of room `id` for its samples newer than `after`, the newest one the
 * Monitor holds on the Sensor's clock (ISSUE-NOV10.8-O84 part 3). Like the Info request it rides the relay's generic
 * settings_update / settings_relay event, so the relay needs no change and an older Sensor ignores it.
 */
internal object GraphRequestWire {
    const val TYPE = "graph_request"

    fun encode(targetId: String, afterMs: Long): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
        put("after", afterMs)
    }

    /** The time after which samples are asked for, or null for another settings type. */
    fun afterMs(data: JSONObject): Long? = if (data.optString("type") == TYPE) data.optLong("after", 0L) else null
}

/**
 * The Sensor's samples for its Monitors (ISSUE-NOV10.8-O84 part 3), at most
 * [com.jd.core.shared.GRAPH_SAMPLES_PER_MESSAGE] a message, far below the relay's 1 MB message limit; `id` is the
 * Sensor's own ID, `samples` the samples as JSON text, one array each.
 */
internal object GraphSamplesWire {
    const val TYPE = "graph_samples"

    fun encode(sensorId: String, samples: List<GraphSample>): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("samples", encodeGraphSamples(samples))
    }

    /** Returns null for another settings type or a malformed payload; a malformed sample alone is skipped. */
    fun decode(data: JSONObject): SensorGraphBatch? {
        if (data.optString("type") != TYPE) return null
        return try {
            SensorGraphBatch(trackerId = data.getString("id"), samples = decodeGraphSamples(data.getString("samples")))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse graph_samples settings_relay")
            null
        }
    }
}
