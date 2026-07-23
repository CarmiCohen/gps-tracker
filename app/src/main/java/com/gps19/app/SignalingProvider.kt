package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.json.JSONObject

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * July.23.00:
 * - Issue #522: Architectural Consolidation. Added RemoteUpdateListener to 
 *   standardize telemetry reception.
 */
interface SignalingProvider {
    interface RemoteUpdateListener {
        fun onUpdate(data: JSONObject)
    }

    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    fun isConnected(): Boolean
    fun getRtt(): Int
    fun clearRtt()
    fun emit(event: String, data: JSONObject)
    fun emitBinary(event: String, routingId: String, data: ByteArray)
    fun getLastRelayTrafficTs(): Long
    fun setConnectionLostCallback(callback: () -> Unit)
    fun setRemoteUpdateListener(listener: RemoteUpdateListener?)
}
