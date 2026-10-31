package com.gps19.core.network

import com.gps19.core.shared.LocationUpdate
import com.gps19.core.shared.ViolationEvent
import kotlinx.coroutines.flow.Flow

/**
 * SignalingClient: Unified communication interface.
 * Corrected: Supports Historical Backfill and Data Usage Tracking (Points 77 & 80).
 */
interface SignalingClient {
    
    val isConnected: Flow<Boolean>
    val telemetryFlow: Flow<LocationUpdate>
    val violationFlow: Flow<ViolationEvent>

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
    suspend fun sendViolation(event: ViolationEvent): Boolean
}
