package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * SignalingPriority: Classification for frame prioritization.
 */
enum class SignalingPriority {
    HIGH,   // Time-critical: Pings, Pongs, Commands, Identity (Join/Leave)
    NORMAL  // Bulk data: Telemetry, Logs
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
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL)
    fun emitMap(event: String, data: Map<String, Any?>, priority: SignalingPriority = SignalingPriority.NORMAL) 
    fun emitBinary(event: String, routingId: String, data: ByteArray, length: Int = data.size, priority: SignalingPriority = SignalingPriority.NORMAL)
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
}
