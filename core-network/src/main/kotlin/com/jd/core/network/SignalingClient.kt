package com.jd.core.network

import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ViolationEvent
import kotlinx.coroutines.flow.Flow

/**
 * SignalingClient: Unified communication interface.
 * Corrected: Supports Historical Backfill and Data Usage Tracking (Points 77 & 80).
 * Updated Nov1.2: Added telemetry batching for overhead optimization (ISSUE-REVOL33.0-F1).
 */
interface SignalingClient {
    
    val isConnected: Flow<Boolean>
    val viewerStatusFlow: Flow<Boolean>
    val telemetryFlow: Flow<LocationUpdate>
    val violationFlow: Flow<ViolationEvent>
    
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

    suspend fun sendViolation(event: ViolationEvent): Boolean

    /**
     * Monitor: Requests missing data range (R-ID 768).
     */
    suspend fun requestBackfill(request: BackfillRequest): Boolean

    /**
     * Sensor: Sends backfill data in response to a request (R-ID 768).
     */
    suspend fun sendBackfillResponse(response: BackfillResponse): Boolean
}
