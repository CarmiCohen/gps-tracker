package com.jd.core.network

import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorEventBatch
import com.jd.core.shared.SensorGraphBatch
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorSetupReport
import org.json.JSONObject

/** One message of the relay's generic settings_update / settings_relay event, as [SettingsWire.decode] reads it. */
sealed interface SettingsMessage {
    /** The Monitor's Home Points for the Sensor (ISSUE-NOV8.13-O51). */
    data class Geofence(val config: GeofenceConfig) : SettingsMessage

    /** The Monitor's detection settings for the Sensor (ISSUE-NOV10.8-O83). */
    data class Behaviour(val behaviour: SensorBehaviour) : SettingsMessage

    /** The Sensor's phone setup report (ISSUE-NOV10.8-O83 part 2). */
    data class Setup(val report: SensorSetupReport) : SettingsMessage

    /** The Sensor's newest events (ISSUE-NOV10.8-O84). */
    data class Events(val batch: SensorEventBatch) : SettingsMessage

    /** A Monitor asking for the Sensor's events newer than [afterMs] (ISSUE-NOV15.4-O98 part 4). */
    data class EventsRequest(val afterMs: Long) : SettingsMessage

    /** The Sensor's detailed Info (ISSUE-NOV10.8-O84 part 2). */
    data class Info(val info: SensorInfo) : SettingsMessage

    /** A Monitor's Info screen asking for the Info, with the satellite list when [withSatellites]. */
    data class InfoRequest(val withSatellites: Boolean) : SettingsMessage

    /** A Monitor's Graphs screen asking for samples (ISSUE-NOV10.8-O84 part 3). */
    data class GraphRequest(val request: com.jd.core.shared.GraphRequest) : SettingsMessage

    /** The Sensor's graph samples (ISSUE-NOV10.8-O84 part 3). */
    data class GraphSamples(val batch: SensorGraphBatch) : SettingsMessage

    /** A Monitor asking about a placement's sit examples, or to learn one (ISSUE-NOV26.0-O163 part 2). */
    data class SitLearn(val request: com.jd.core.shared.SitLearnRequest) : SettingsMessage

    /** The Sensor [sensorId]'s sit examples (ISSUE-NOV26.0-O163 part 2). */
    data class SitExamples(val sensorId: String, val report: com.jd.core.shared.SitExampleReport) : SettingsMessage
}

/**
 * The single decoder of the settings event (SIMP-NOV15.3-S1): it reads `type` once and hands the payload to the one
 * wire object that owns it, so a message is decoded once, not by every wire object in turn. Each wire object still
 * returns null for a type that is not its own, which keeps [SensorSetupWire.decodeText] safe for pushed text.
 */
object SettingsWire {
    /** Null for a type this version does not know (from a newer peer) or a malformed payload. */
    fun decode(data: JSONObject): SettingsMessage? = when (data.optString("type")) {
        GeofenceWire.TYPE -> GeofenceWire.decode(data)?.let(SettingsMessage::Geofence)
        SensorBehaviourWire.TYPE -> SensorBehaviourWire.decode(data)?.let(SettingsMessage::Behaviour)
        SensorSetupWire.TYPE -> SensorSetupWire.decode(data)?.let(SettingsMessage::Setup)
        SensorEventsWire.TYPE -> SensorEventsWire.decode(data)?.let(SettingsMessage::Events)
        EventsRequestWire.TYPE -> EventsRequestWire.decode(data)?.let(SettingsMessage::EventsRequest)
        SensorInfoWire.TYPE -> SensorInfoWire.decode(data)?.let(SettingsMessage::Info)
        InfoRequestWire.TYPE -> InfoRequestWire.decode(data)?.let(SettingsMessage::InfoRequest)
        GraphRequestWire.TYPE -> GraphRequestWire.decode(data)?.let(SettingsMessage::GraphRequest)
        GraphSamplesWire.TYPE -> GraphSamplesWire.decode(data)?.let(SettingsMessage::GraphSamples)
        SitLearnWire.TYPE -> SitLearnWire.decode(data)?.let(SettingsMessage::SitLearn)
        SitExamplesWire.TYPE -> SitExamplesWire.decode(data)?.let { (id, report) -> SettingsMessage.SitExamples(id, report) }
        else -> null
    }

    /** The key of the sending Monitor's own ID in each message a Monitor sends to its Sensor (ISSUE-NOV22.7-O135). */
    const val FROM = "from"

    /** [payload], a Monitor's message to its Sensor, with the Monitor's own [monitorId] (ISSUE-NOV22.7-O135). */
    fun fromMonitor(payload: JSONObject, monitorId: String?): JSONObject = payload.apply { monitorId?.let { put(FROM, it) } }

    /**
     * The ID of the Monitor that sent [message], read from its payload [data] (ISSUE-NOV22.7-O135); null for a Sensor's
     * message, and for one from a Monitor before Nov22.7, which sends no ID.
     */
    fun monitorIdOf(data: JSONObject, message: SettingsMessage?): String? = when (message) {
        is SettingsMessage.Geofence, is SettingsMessage.Behaviour, is SettingsMessage.EventsRequest,
        is SettingsMessage.InfoRequest, is SettingsMessage.GraphRequest,
        is SettingsMessage.SitLearn -> data.optString(FROM).takeIf { it.isNotBlank() }
        else -> null
    }
}
