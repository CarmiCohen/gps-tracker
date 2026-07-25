package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.json.JSONObject

/**
 * SignalingPriority: Classification for frame prioritization.
 * July.25.08:
 * - Issue #560c: Socket-Level Pressure. Introduced priority levels to ensure 
 *   high-frequency pulses (pings/commands) aren't blocked by 64KB telemetry frames.
 */
enum class SignalingPriority {
    HIGH,   // Time-critical: Pings, Pongs, Commands, Identity (Join/Leave)
    NORMAL  // Bulk data: Telemetry, Logs
}

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * July.25.08:
 * - Issue #560c: Added SignalingPriority support to all emit methods.
 * July.25.03:
 * - Issue #560: Pipeline Serialization Hardening. Added length parameter to 
 *   emitBinary to support pre-allocated buffer reuse.
 */
interface SignalingProvider {
    interface RemoteUpdateListener {
        fun onUpdate(data: JSONObject)
        fun onBinaryUpdate(data: ByteArray) 
    }

    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    fun isConnected(): Boolean
    fun isConnecting(): Boolean
    fun getRtt(): Int
    fun clearRtt()
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL)
    fun emitMap(event: String, data: Map<String, Any?>, priority: SignalingPriority = SignalingPriority.NORMAL) 
    fun emitBinary(event: String, routingId: String, data: ByteArray, length: Int = data.size, priority: SignalingPriority = SignalingPriority.NORMAL)
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
    fun setRemoteUpdateListener(listener: RemoteUpdateListener?)
}
