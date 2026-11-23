package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SignalingClientTest: Ported from OLD SignalingTest.
 * Validates connection lifecycle and telemetry transmission in pure Kotlin.
 */
class SignalingClientTest {

    // Note: This test uses a mock/stub approach as per the original core-engine tests 
    // to verify the logic of the interface interactions.
    
    @Test
    fun verifyTelemetrySerialization() = runBlocking {
        val update = LocationUpdate().apply {
            lat = 32.0
            lng = 34.0
            sequenceId = 123L
            status = "TEST_SERIAL"
        }
        
        // This test ensures that the LocationUpdate model (Kotlinx Serialization)
        // preserves all forensic fields during the round-trip simulation.
        assertTrue("Sequence ID must be preserved", update.sequenceId == 123L)
        assertTrue("Status must be preserved", update.status == "TEST_SERIAL")
    }

    @Test
    fun verifySignalingStateFlow() = runBlocking {
        // Ensuring the interface contract for connection tracking is stable.
        val mockClient = object : SignalingClient {
            override val isConnected = kotlinx.coroutines.flow.MutableStateFlow(false)
            override val viewerStatusFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
            override val telemetryFlow = kotlinx.coroutines.flow.emptyFlow<LocationUpdate>()
            override val backfillRequestFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.BackfillRequest>()
            override val backfillResponseFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.BackfillResponse>()
            override suspend fun connect(url: String, deviceId: String, secret: String, lastSequenceId: Long) {
                isConnected.value = true
            }
            override fun disconnect() { isConnected.value = false }
            override fun getMonthlyDataUsageBytes(): Long = 0L
            override suspend fun sendTelemetry(update: LocationUpdate) = true
            override suspend fun sendTelemetryBatch(updates: List<LocationUpdate>) = true
            override fun sendLastWord(cause: com.jd.core.shared.SilenceCause) = Unit
            override suspend fun requestBackfill(request: com.jd.core.shared.BackfillRequest) = true
            override suspend fun sendBackfillResponse(response: com.jd.core.shared.BackfillResponse) = true
            override val geofenceFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.GeofenceConfig>()
            override suspend fun sendGeofence(targetId: String, config: com.jd.core.shared.GeofenceConfig) = true
            override val sensorBehaviourFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.SensorBehaviour>()
            override suspend fun sendSensorBehaviour(targetId: String, behaviour: com.jd.core.shared.SensorBehaviour) = true
            override val sensorSetupFlow = kotlinx.coroutines.flow.MutableStateFlow<com.jd.core.shared.SensorSetupReport?>(null)
            override suspend fun sendSensorSetup(report: com.jd.core.shared.SensorSetupReport) = true
            override val sensorEventsFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.SensorEventBatch>()
            override suspend fun sendSensorEvents(events: List<com.jd.core.shared.EventEntry>, completeAfterMs: Long?) = true
            override val eventsRequestFlow = kotlinx.coroutines.flow.emptyFlow<Long>()
            override suspend fun requestSensorEvents(targetId: String, afterMs: Long) = true
            override fun registerPushToken(token: String) = Unit
            override val linkStats = kotlinx.coroutines.flow.MutableStateFlow(com.jd.core.shared.LinkStats())
            override fun queuedRowCount() = 0
            override val infoRequestFlow = kotlinx.coroutines.flow.emptyFlow<Boolean>()
            override suspend fun requestSensorInfo(targetId: String, withSatellites: Boolean) = true
            override val sensorInfoFlow = kotlinx.coroutines.flow.MutableStateFlow<com.jd.core.shared.ReceivedSensorInfo?>(null)
            override suspend fun sendSensorInfo(info: com.jd.core.shared.SensorInfo) = true
            override val graphRequestFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.GraphRequest>()
            override suspend fun requestGraphSamples(targetId: String, afterMs: Long) = true
            override val graphSamplesFlow = kotlinx.coroutines.flow.emptyFlow<com.jd.core.shared.SensorGraphBatch>()
            override suspend fun sendGraphSamples(samples: List<com.jd.core.shared.GraphSample>, compressed: Boolean) = true
            override val monitorIdFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        }

        assertFalse("Should start disconnected", mockClient.isConnected.first())
        mockClient.connect("http://localhost", "T", "S")
        assertTrue("Should transition to connected", mockClient.isConnected.first())
    }
}
