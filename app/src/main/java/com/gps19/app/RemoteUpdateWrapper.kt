package com.gps19.app

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RemoteUpdateWrapper: Thread-safe callback wrapper for remote signaling updates.
 * v6.140: Decoupled from AppNetworkManager for clean Hilt injection.
 */
@Singleton
class RemoteUpdateWrapper @Inject constructor() {
    private var callback: ((JSONObject) -> Unit)? = null
    fun setCallback(cb: (JSONObject) -> Unit) { callback = cb }
    fun onUpdate(data: JSONObject) { callback?.invoke(data) }
}
