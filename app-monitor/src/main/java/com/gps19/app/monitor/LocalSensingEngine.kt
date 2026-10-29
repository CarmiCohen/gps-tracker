package com.gps19.app.monitor

import com.gps19.core.shared.LocationUpdate
import com.gps19.core.shared.SentinelLogic

/**
 * LocalSensingEngine: Applies shared SentinelLogic to the Monitor's own GPS.
 * Ensures the "You" trail is as smooth and jitter-free as the "Them" trail (Point 180).
 */
class LocalSensingEngine {
    private var lastLocation: LocationUpdate? = null

    fun processLocalLocation(rawUpdate: LocationUpdate): LocationUpdate? {
        val last = lastLocation ?: return rawUpdate.also { lastLocation = it }
        
        val result = SentinelLogic.validateMovement(last, rawUpdate, hasPhysicalMotion = true)
        
        return if (result.status == SentinelLogic.SentinelStatus.VALID) {
            lastLocation = rawUpdate
            rawUpdate
        } else {
            // Drop jitter/jump for local user trail
            null
        }
    }
}
