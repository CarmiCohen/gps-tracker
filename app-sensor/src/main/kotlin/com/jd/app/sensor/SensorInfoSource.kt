package com.jd.app.sensor

import com.jd.core.shared.SensorInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the Sensor's Info screen reads the running [SensorService]'s Info (ISSUE-NOV10.8-O84 part 2): the service
 * registers its builder while it runs, so the screen asks for a fresh Info only while it is open, and gets none while
 * the service is stopped.
 */
@Singleton
class SensorInfoSource @Inject constructor() {
    @Volatile private var builder: (() -> SensorInfo)? = null

    fun register(build: () -> SensorInfo) {
        builder = build
    }

    fun unregister() {
        builder = null
    }

    /** A fresh Info, or null while the service is stopped. */
    fun current(): SensorInfo? = builder?.invoke()
}
