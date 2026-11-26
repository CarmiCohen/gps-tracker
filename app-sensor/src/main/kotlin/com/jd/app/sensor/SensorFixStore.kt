package com.jd.app.sensor

import android.location.Location
import android.os.SystemClock
import com.jd.core.shared.MovementTracker
import com.jd.core.ui.FixReading
import com.jd.core.ui.isFixFresh
import com.jd.core.ui.toFixReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The latest fix [SensorService] received: the one copy (SIMP-NOV7.2-S1) its telemetry, sampling rate and Info read as
 * [location], and the HUD as [fix]. Both null while the service is not running or has no fix yet, so the HUD shows no
 * position values. Each fix also feeds the one [MovementTracker] (ISSUE-NOV25.25-O162), read by the row clock as
 * [isMoving] and by the HUD through [FixReading.isMoving].
 */
@Singleton
class SensorFixStore @Inject constructor() {
    private val latest = MutableStateFlow<FixReading?>(null)
    val fix: StateFlow<FixReading?> = latest.asStateFlow()

    /** Written on the service's main looper, read by its sensing loop and sensor threads. */
    @Volatile var location: Location? = null
        private set

    private var movement = MovementTracker()

    /** The debounced movement up to [location] (ISSUE-NOV25.25-O162); false without a fix. */
    @Volatile var isMoving = false
        private set

    /**
     * [location] while it is still the current position by the one freshness rule (`isFixFresh`, SIMP-NOV7.21-S1),
     * null once it is older: the telemetry then sends `NO_FIX`, and the HUD drops its speed at the same age.
     */
    fun currentLocation(nowElapsedNanos: Long = SystemClock.elapsedRealtimeNanos()): Location? =
        location?.takeIf { isFixFresh(it.elapsedRealtimeNanos, nowElapsedNanos) }

    private val satelliteFix = MutableStateFlow<Long?>(null)

    /**
     * When (wall clock) the GNSS chip last reported a fix from enough satellites (`isSatelliteFix`), for the GPS LED
     * (Nov19.5); null before the first one, while the service is not running, or without GNSS status.
     */
    val satelliteFixTimeMs: StateFlow<Long?> = satelliteFix.asStateFlow()

    fun publish(location: Location) {
        val speed = if (location.hasSpeed()) location.speed else null
        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        isMoving = movement.onFix(location.time, location.latitude, location.longitude, accuracy, speed)
        this.location = location
        latest.value = location.toFixReading(isMoving)
    }

    /** A HUD reading without a [Location], for the HUD tests: [location] is cleared, so the two never disagree. */
    fun publish(fix: FixReading) {
        location = null
        latest.value = fix
    }

    fun markSatelliteFix(timeMs: Long) {
        satelliteFix.value = timeMs
    }

    fun clear() {
        movement = MovementTracker()
        isMoving = false
        location = null
        latest.value = null
        satelliteFix.value = null
    }
}
