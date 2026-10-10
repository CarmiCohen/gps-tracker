package com.gps19.app

import com.gps19.core.engine.LocationUpdate
import com.gps19.core.engine.SignalingConstants
import com.gps19.core.engine.SignalingPriority
import com.gps19.core.engine.SignalingPipeline
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated state accessors 
 *   to strict val properties for Hilt/Compose stability.
 */
interface SignalingProvider {
    val signalingFlow: SharedFlow<SignalingEvent>
    val signalingMetrics: StateFlow<SignalingPipeline.Metrics>
    
    val isConnected: Boolean
    val isConnecting: Boolean
    val rtt: Int
    val lastRelayTrafficTs: Long

    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    
    fun clearRtt()
    
    fun emit(event: String, data: JSONObject, priority: SignalingPriority = SignalingPriority.NORMAL)
    fun transmit(status: LocationUpdate, priority: SignalingPriority = SignalingPriority.NORMAL, fromViewer: Boolean = false)

    fun setConnectionLostCallback(callback: () -> Unit)
}
