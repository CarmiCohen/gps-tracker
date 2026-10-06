package com.gps19.app

import com.gps19.core.engine.LocationUpdate
import com.gps19.core.engine.SignalingConstants
import com.gps19.core.engine.SignalingPriority
import com.gps19.core.engine.SmartSignalingDispatcher
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * SignalingEvent: Reactive event container for incoming relay data.
 */
sealed class SignalingEvent {
    data class JsonUpdate(val data: JSONObject) : SignalingEvent()
    data class BinaryUpdate(val data: ByteArray) : SignalingEvent()
}

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * Oct.6.7:
 * - Issue #AUDIT-1006-8: Added getDispatcherMetrics() to expose conflation 
 *   efficiency to the UI for field auditing (Rule 1.123).
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
    fun transmit(status: LocationUpdate, priority: SignalingPriority = SignalingPriority.NORMAL, fromViewer: Boolean = false)

    fun getDispatcherMetrics(): SmartSignalingDispatcher.Metrics
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
}
