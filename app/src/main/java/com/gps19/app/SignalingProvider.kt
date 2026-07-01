package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.json.JSONObject

/**
 * Interface for signaling implementations (Socket.io, MQTT, etc.)
 * v8.9.64:
 * - Added setConnectionLostCallback for reactive reconnection triggers.
 * v8.8.21:
 * - Role-Based Standardization: Explicitly enforcing Tracker ID ("T") vs Viewer ID ("C").
 * - Delegated validation to :core:engine:SignalingConstants.
 */
interface SignalingProvider {
    fun connect(url: String, deviceId: String, viewerId: String, isTracker: Boolean)
    fun disconnect()
    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean, force: Boolean = false)
    fun isConnected(): Boolean
    fun getRtt(): Int
    fun clearRtt()
    fun emit(event: String, data: JSONObject)
    /**
     * R944: Emits a binary payload for Protobuf efficiency.
     */
    fun emitBinary(event: String, data: ByteArray)
    fun pushSettings()
    /**
     * R568a: Returns the monotonic timestamp (elapsedRealtime) of the last message received from the relay.
     * Used for zombie connection detection.
     */
    fun getLastRelayTrafficTs(): Long
    
    /**
     * Issue #007: Allows the network manager to register a reactive callback for transport failures.
     */
    fun setConnectionLostCallback(callback: () -> Unit)
}
