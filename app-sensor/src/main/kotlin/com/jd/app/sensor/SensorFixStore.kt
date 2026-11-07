package com.jd.app.sensor

import com.jd.core.ui.FixReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The latest fix [SensorService] received, the same one it sends as telemetry.
 * Null while the service is not running or has no fix yet, so the HUD shows no position values.
 */
@Singleton
class SensorFixStore @Inject constructor() {
    private val latest = MutableStateFlow<FixReading?>(null)
    val fix: StateFlow<FixReading?> = latest.asStateFlow()

    fun publish(fix: FixReading) {
        latest.value = fix
    }

    fun clear() {
        latest.value = null
    }
}
