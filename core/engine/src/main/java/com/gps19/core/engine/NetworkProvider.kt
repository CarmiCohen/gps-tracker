package com.gps19.core.engine

/**
 * NetworkListener: Reactive callback for network state changes.
 */
interface NetworkListener {
    fun onNetworkAvailable()
    fun onNetworkLost()
}

/**
 * NetworkProvider: Abstract interface for monitoring network availability.
 * Decouples engine logic from Android's ConnectivityManager for testability.
 * 
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Migrated to property-based 
 *   availability check for Hilt/Compose interoperability.
 */
interface NetworkProvider {
    /**
     * Returns true if the network interface is currently available.
     */
    val isNetworkAvailable: Boolean

    fun registerListener(listener: NetworkListener)
    fun unregisterListener(listener: NetworkListener)
}

/**
 * SignalingTransport: Abstract interface for low-level signaling handshake mechanisms.
 * Handles out-of-band keep-alive and relay wake-up calls.
 */
interface SignalingTransport {
    /**
     * Executes a keep-alive probe to the specified relay URL.
     * Returns the HTTP response code.
     */
    suspend fun performKeepAlive(url: String): Int

    /**
     * Executes an aggressive wake-up sequence to the specified relay URL.
     */
    suspend fun wakeUpRelay(url: String)
}
