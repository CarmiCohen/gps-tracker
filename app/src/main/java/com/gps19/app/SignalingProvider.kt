package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * SignalingPriority: Classification for frame prioritization.
 * July.27.00 Audit:
 * - Telemetry (Location Updates) promoted to HIGH to prevent head-of-line blocking 
 *   from high-volume forensic logs.
 */
enum class SignalingPriority {
    HIGH,   // Time-critical: Telemetry, Alarms, Pings, Commands, Identity
    NORMAL  // Bulk data: Forensic Logs, Status Snapshots
}

/**
 * SignalingEvent: Reactive event container for incoming relay data.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization. Replaced legacy listener 
 *   with a unified sealed class for reactive stream consumption.
 */
sealed class SignalingEvent {
    data class JsonUpdate(val data: JSONObject) : SignalingEvent()
    data class BinaryUpdate(val data: ByteArray) : SignalingEvent()
}

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * Sep.02.70:
 * - Idea #239: Signaling Interface Consolidation. Removed redundant emitMap 
 *   and emitBinary overloads in favor of a unified transmit(TrackerStatus) 
 *   entry point to simplify the telemetry pipeline (R-ID 239).
 */
interface SignalingProvider {
    val signalingFlow: SharedFlow<SignalingEvent>

    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    fun isConnected(): Boolean
    fun isConnecting(): Boolean
    fun getRtt(): Int
    fun clearRtt()
    
    /**
     * emit: Retained for generic JSON commands (ping, pong, leave, join).
     */
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL)
    
    /**
     * transmit: Unified telemetry transmission. 
     * Handles internal serialization (Protobuf/JSON) and routing.
     */
    fun transmit(status: TrackerStatus, priority: SignalingPriority = SignalingPriority.NORMAL, fromViewer: Boolean = false)

    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
}
