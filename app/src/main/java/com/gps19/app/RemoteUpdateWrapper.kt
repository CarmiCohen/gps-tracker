package com.gps19.app

import org.json.JSONObject

/**
 * RemoteUpdateWrapper: Thread-safe callback wrapper for remote signaling updates.
 * v6.140: Decoupled from AppNetworkManager for clean Hilt injection.
 */
class RemoteUpdateWrapper() {
    private var callback: ((JSONObject) -> Unit)? = null
    fun setCallback(cb: (JSONObject) -> Unit) { callback = cb }
    fun onUpdate(data: JSONObject) { callback?.invoke(data) }
}
