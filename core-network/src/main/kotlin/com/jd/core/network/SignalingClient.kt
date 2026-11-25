package com.jd.core.network

import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.EventEntry
import com.jd.core.shared.GraphRequest
import com.jd.core.shared.GraphSample
import com.jd.core.shared.SensorGraphBatch
import com.jd.core.shared.SensorEventBatch
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.LinkStats
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ReceivedSensorInfo
import com.jd.core.shared.SensorInfo
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.SensorSetupReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * SignalingClient: Unified communication interface.
 * Corrected: Supports Historical Backfill and Data Usage Tracking (Points 77 & 80).
 * Updated Nov1.2: Added telemetry batching for overhead optimization (ISSUE-REVOL33.0-F1).
 */
interface SignalingClient {
    
    val isConnected: Flow<Boolean>
    val viewerStatusFlow: Flow<Boolean>
    val telemetryFlow: Flow<LocationUpdate>
    
    /**
     * Flow of incoming backfill requests (used by Sensor).
     */
    val backfillRequestFlow: Flow<BackfillRequest>

    /**
     * Flow of incoming backfill responses (used by Monitor).
     */
    val backfillResponseFlow: Flow<BackfillResponse>

    /**
     * Connect with Backfill support.
     * @param lastSequenceId The last ID stored in the Monitor's local Room DB.
     */
    suspend fun connect(
        url: String, 
        deviceId: String, 
        secret: String, 
        lastSequenceId: Long = 0L 
    )

    fun disconnect()

    /**
     * Sensor: Tracks bytes sent to provide data transparency to the user.
     */
    fun getMonthlyDataUsageBytes(): Long

    suspend fun sendTelemetry(update: LocationUpdate): Boolean

    /**
     * Optimized batch transmission for telemetry updates (R-ID 80).
     */
    suspend fun sendTelemetryBatch(updates: List<LocationUpdate>): Boolean

    /**
     * Sensor: tells the relay why it is about to go silent, after the rows already taken (ISSUE-NOV22.10-O137).
     * Sent on the client's own scope, so it outlives a stopping service. Best effort: nothing is sent while
     * disconnected, and a word sent while the network drops may be lost.
     */
    fun sendLastWord(cause: com.jd.core.shared.SilenceCause)

    /**
     * Monitor: Requests missing data range (R-ID 768).
     */
    suspend fun requestBackfill(request: BackfillRequest): Boolean

    /**
     * Sensor: answers the backfill request [requestId] with [updates] (R-ID 768), in chunks of 50 rows; only the last
     * carries the completion flag. Stops at the first chunk that cannot be sent, so the Monitor asks again
     * (ISSUE-NOV6.4-O33, SIMP-NOV7.15-S1). Returns true if every chunk was sent.
     */
    suspend fun sendBackfill(requestId: String, updates: List<LocationUpdate>): Boolean

    /**
     * Sensor: geofence configurations sent by the paired Monitor (ISSUE-NOV8.13-O51).
     */
    val geofenceFlow: Flow<GeofenceConfig>

    /**
     * Monitor: sends the user's geofence to the tracker [targetId], which enforces it (ISSUE-NOV8.13-O51).
     * Returns false when not connected; the caller sends again once the tracker is reachable.
     */
    suspend fun sendGeofence(targetId: String, config: GeofenceConfig): Boolean

    /**
     * Sensor: the detection settings sent by the paired Monitor (ISSUE-NOV10.8-O83).
     */
    val sensorBehaviourFlow: Flow<SensorBehaviour>

    /**
     * Monitor: sends the Sensor's detection settings to the tracker [targetId] (ISSUE-NOV10.8-O83). Returns false when
     * not connected; the caller sends again once the tracker is reachable.
     */
    suspend fun sendSensorBehaviour(targetId: String, behaviour: SensorBehaviour): Boolean

    /**
     * Monitor: the newest setup report received from a Sensor, or null when none arrived since [connect]
     * (ISSUE-NOV10.8-O83 part 2). It is kept here, not in a screen, so a report that arrives while the Monitor's screen
     * is closed is shown when it opens. [disconnect] forgets it.
     */
    val sensorSetupFlow: StateFlow<SensorSetupReport?>

    /**
     * Sensor: tells the Monitors of its room what its phone setup lacks (ISSUE-NOV10.8-O83 part 2). Returns false when
     * not connected; the caller sends again once a Monitor is reachable.
     */
    suspend fun sendSensorSetup(report: SensorSetupReport): Boolean

    /**
     * Monitor: the Sensor's newest events, each time a Sensor sends them (ISSUE-NOV10.8-O84). They repeat events sent
     * before, which the event log skips.
     */
    val sensorEventsFlow: Flow<SensorEventBatch>

    /**
     * Sensor: sends [events] to the Monitors of its room (ISSUE-NOV10.8-O84); [completeAfterMs], when set, says they are
     * all its peer events newer than it (ISSUE-NOV15.4-O98 part 4). Returns false when not connected; the caller sends
     * again once a Monitor is reachable.
     */
    suspend fun sendSensorEvents(events: List<EventEntry>, completeAfterMs: Long? = null): Boolean

    /** Sensor: each time a Monitor asks for the peer events newer than the given time on the Sensor's clock (ISSUE-NOV15.4-O98 part 4). */
    val eventsRequestFlow: Flow<Long>

    /**
     * Monitor: asks the tracker [targetId] for its peer events newer than [afterMs], the time up to which this Monitor
     * has them all (ISSUE-NOV15.4-O98 part 4). Returns false when not connected.
     */
    suspend fun requestSensorEvents(targetId: String, afterMs: Long): Boolean

    /**
     * Monitor: gives the relay this phone's push [token], now and after every reconnection, so the paired tracker's
     * alarm rows also arrive as high-priority pushes (ISSUE-NOV10.8-O86). [disconnect] takes the token back.
     */
    fun registerPushToken(token: String)

    /**
     * The relay link since the app started, with the latency to the relay, for the Info screens (ISSUE-NOV10.8-O84
     * part 2).
     */
    val linkStats: StateFlow<LinkStats>

    /** Sensor: the rows held while offline, waiting to be sent, for its Info (ISSUE-NOV10.8-O84 part 2). */
    fun queuedRowCount(): Int

    /**
     * Sensor: each time a Monitor's Info screen asks for the Sensor's Info (ISSUE-NOV10.8-O84 part 2); true when it
     * also asks for the satellite list (ISSUE-NOV15.5-O99 part 1).
     */
    val infoRequestFlow: Flow<Boolean>

    /**
     * Monitor: asks the tracker [targetId] for its Info while the Info screen is open (ISSUE-NOV10.8-O84 part 2), with
     * the satellite list only [withSatellites] (ISSUE-NOV15.5-O99 part 1). Returns false when not connected.
     */
    suspend fun requestSensorInfo(targetId: String, withSatellites: Boolean): Boolean

    /**
     * Monitor: the newest Info received from a Sensor, or null when none arrived since [connect] (ISSUE-NOV10.8-O84
     * part 2). [disconnect] forgets it.
     */
    val sensorInfoFlow: StateFlow<ReceivedSensorInfo?>

    /** Sensor: sends its [info] to the Monitors of its room (ISSUE-NOV10.8-O84 part 2). Returns false when not connected. */
    suspend fun sendSensorInfo(info: SensorInfo): Boolean

    /**
     * Sensor: each time a Monitor's Graphs screen asks for the samples newer than the given time on the Sensor's clock
     * (ISSUE-NOV10.8-O84 part 3), and whether it reads the compressed answer (ISSUE-NOV15.6-O100 part 1).
     */
    val graphRequestFlow: Flow<GraphRequest>

    /**
     * Monitor: asks the tracker [targetId] for its samples newer than [afterMs], the newest the Monitor holds, while the
     * Graphs screen is open (ISSUE-NOV10.8-O84 part 3). Returns false when not connected.
     */
    suspend fun requestGraphSamples(targetId: String, afterMs: Long): Boolean

    /** Monitor: each batch of samples a Sensor sends (ISSUE-NOV10.8-O84 part 3). */
    val graphSamplesFlow: Flow<SensorGraphBatch>

    /**
     * Sensor: the ID of the Monitor heard last, from the `from` of its messages, or null while none was heard since the
     * app started (ISSUE-NOV22.7-O135). Kept across reconnections; this phone's own echoes do not count.
     */
    val monitorIdFlow: StateFlow<String?>

    /**
     * Sensor: sends [samples] to the Monitors of its room, at most [com.jd.core.shared.GRAPH_SAMPLES_PER_MESSAGE] a call
     * (ISSUE-NOV10.8-O84 part 3), [compressed] when the asking Monitor reads that form (ISSUE-NOV15.6-O100 part 1).
     * Returns false when not connected.
     */
    suspend fun sendGraphSamples(samples: List<GraphSample>, compressed: Boolean): Boolean
}
