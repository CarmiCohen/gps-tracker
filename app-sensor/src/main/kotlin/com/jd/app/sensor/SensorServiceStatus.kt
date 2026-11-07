package com.jd.app.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether [SensorService] is running its sensing components, as reported by the service itself
 * (ISSUE-NOV6.4-O34, updated Nov7.34 for ISSUE-NOV7.2-O36). Tracks running state and start time
 * for live uptime calculations.
 */
@Singleton
class SensorServiceStatus @Inject constructor() {
    private val running = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = running.asStateFlow()

    private val startTime = MutableStateFlow(0L)
    val startTimeMillis: StateFlow<Long> = startTime.asStateFlow()

    fun markRunning(startTimeMs: Long = currentElapsedRealtime()) {
        startTime.value = startTimeMs
        running.value = true
    }

    fun markStopped() {
        startTime.value = 0L
        running.value = false
    }

    companion object {
        fun currentElapsedRealtime(): Long {
            return try {
                android.os.SystemClock.elapsedRealtime()
            } catch (_: Throwable) {
                System.currentTimeMillis()
            }
        }
    }
}

/** Formats elapsed uptime between [startTimeMs] and [nowMs] as HH:mm:ss. */
fun formatUptime(startTimeMs: Long, nowMs: Long): String {
    if (startTimeMs <= 0L || nowMs < startTimeMs) return "00:00:00"
    val diffSec = (nowMs - startTimeMs) / 1000L
    val hours = diffSec / 3600L
    val minutes = (diffSec % 3600L) / 60L
    val seconds = diffSec % 60L
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

/** The service line of the Sensor Setup dialog. */
fun serviceStatusText(isRunning: Boolean, uptimeText: String = ""): String {
    return if (isRunning) {
        if (uptimeText.isNotBlank()) "Sensor Service: Running ($uptimeText)" else "Sensor Service: Running"
    } else {
        "Sensor Service: Stopped"
    }
}

/** The relay line of the Sensor Setup dialog. */
fun relayStatusText(isConnected: Boolean): String =
    "Relay Connection: ${if (isConnected) "Connected" else "Disconnected"}"

