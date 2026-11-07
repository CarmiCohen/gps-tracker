package com.jd.app.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether [SensorService] is running its sensing components, as reported by the service itself
 * (ISSUE-NOV6.4-O34). False until the service has started them, and again after it is destroyed,
 * so no screen can show the service as running when it is not.
 */
@Singleton
class SensorServiceStatus @Inject constructor() {
    private val running = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = running.asStateFlow()

    fun markRunning() {
        running.value = true
    }

    fun markStopped() {
        running.value = false
    }
}

/** The service line of the Sensor Setup dialog. */
fun serviceStatusText(isRunning: Boolean): String =
    "Sensor Service: ${if (isRunning) "Running" else "Stopped"}"

/** The relay line of the Sensor Setup dialog. */
fun relayStatusText(isConnected: Boolean): String =
    "Relay Connection: ${if (isConnected) "Connected" else "Disconnected"}"
