package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.json.JSONObject

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * July.24.07:
 * - Issue #546: Added isConnecting() to prevent redundant handshake attempts.
 * July.24.05:
 * - Issue #538d: Added emitMap to eliminate redundant JSONObject conversions 
 *   in the telemetry path.
 * July.24.04:
 * - Issue #541: Direct Binary Flow. Added onBinaryUpdate to bypass 
 *   JSON overhead for high-frequency telemetry.
 */
interface SignalingProvider {
    interface RemoteUpdateListener {
        fun onUpdate(data: JSONObject)
        fun onBinaryUpdate(data: ByteArray) // New direct path
    }

    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    fun isConnected(): Boolean
    fun isConnecting(): Boolean
    fun getRtt(): Int
    fun clearRtt()
    fun emit(event: String, data: JSONObject)
    fun emitMap(event: String, data: Map<String, Any?>) // Optimized path
    fun emitBinary(event: String, routingId: String, data: ByteArray)
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
    fun setRemoteUpdateListener(listener: RemoteUpdateListener?)
}
