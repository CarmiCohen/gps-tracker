package com.jd.core.network

import com.jd.core.shared.SitExampleReport
import com.jd.core.shared.SitLearnRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import timber.log.Timber

/** Unknown fields are skipped, so a newer peer's additions do not break an older reader. */
private val sitJson = Json { ignoreUnknownKeys = true }

/**
 * A Monitor's SEAT PLACEMENT screen asking the Sensor of room `id` about a placement's learned examples, or to record
 * the next sit check as one (ISSUE-NOV26.0-O163 part 2), inside the relay's generic settings_update / settings_relay
 * event, so the relay needs no change and a Sensor before Nov26.25 ignores it. `req` is the request as JSON text.
 */
internal object SitLearnWire {
    const val TYPE = "sit_learn"

    fun encode(targetId: String, request: SitLearnRequest): JSONObject = JSONObject().apply {
        put("id", targetId)
        put("type", TYPE)
        put("req", sitJson.encodeToString(request))
    }

    /** Returns null for another settings type or a malformed payload. */
    fun decode(data: JSONObject): SitLearnRequest? {
        if (data.optString("type") != TYPE) return null
        return try {
            sitJson.decodeFromString<SitLearnRequest>(data.getString("req"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sit_learn settings_relay")
            null
        }
    }
}

/**
 * The Sensor's learned sit examples for the Monitors of its room (ISSUE-NOV26.0-O163 part 2): `id` is the Sensor's own
 * ID, `report` the [SitExampleReport] as JSON text. A Monitor before Nov26.25 ignores it.
 */
internal object SitExamplesWire {
    const val TYPE = "sit_examples"

    fun encode(sensorId: String, report: SitExampleReport): JSONObject = JSONObject().apply {
        put("id", sensorId)
        put("type", TYPE)
        put("report", sitJson.encodeToString(report))
    }

    /** The sending Sensor's ID and its report; null for another settings type or a malformed payload. */
    fun decode(data: JSONObject): Pair<String, SitExampleReport>? {
        if (data.optString("type") != TYPE) return null
        return try {
            data.getString("id") to sitJson.decodeFromString<SitExampleReport>(data.getString("report"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse sit_examples settings_relay")
            null
        }
    }
}
