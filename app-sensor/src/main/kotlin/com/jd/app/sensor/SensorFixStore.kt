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

    private val satelliteFix = MutableStateFlow<Long?>(null)

    /**
     * When (wall clock) the GNSS chip last reported a fix from enough satellites (`isSatelliteFix`), for the GPS LED
     * (Nov19.5); null before the first one, while the service is not running, or without GNSS status.
     */
    val satelliteFixTimeMs: StateFlow<Long?> = satelliteFix.asStateFlow()

    fun publish(fix: FixReading) {
        latest.value = fix
    }

    fun markSatelliteFix(timeMs: Long) {
        satelliteFix.value = timeMs
    }

    fun clear() {
        latest.value = null
        satelliteFix.value = null
    }
}
