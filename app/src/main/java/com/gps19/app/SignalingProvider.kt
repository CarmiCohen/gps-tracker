package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.json.JSONObject

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * July.25.03:
 * - Issue #560: Pipeline Serialization Hardening. Added length parameter to 
 *   emitBinary to support pre-allocated buffer reuse.
 * July.24.07:
 * - Issue #546: Added isConnecting() to prevent redundant handshake attempts.
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
    fun emit(event: String, data: JSONObject)
    fun emitMap(event: String, data: Map<String, Any?>) 
    fun emitBinary(event: String, routingId: String, data: ByteArray, length: Int = data.size)
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
    fun setRemoteUpdateListener(listener: RemoteUpdateListener?)
}
