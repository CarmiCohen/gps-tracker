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

    private val microphoneState = MutableStateFlow(MicrophoneState.OFF)
    val microphone: StateFlow<MicrophoneState> = microphoneState.asStateFlow()

    fun setMicrophone(state: MicrophoneState) {
        microphoneState.value = state
    }

    private val motionState = MutableStateFlow(MotionState())
    val motion: StateFlow<MotionState> = motionState.asStateFlow()

    fun setMotion(state: MotionState) {
        motionState.value = state
    }

    fun markRunning(startTimeMs: Long = currentElapsedRealtime()) {
        startTime.value = startTimeMs
        running.value = true
    }

    fun markStopped() {
        startTime.value = 0L
        running.value = false
        microphoneState.value = MicrophoneState.OFF
        motionState.value = MotionState()
    }

    fun resetUptime(nowMs: Long = currentElapsedRealtime()) {
        if (running.value) {
            startTime.value = nowMs
        }
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

/** What the Sensor's noise detection is doing (ISSUE-NOV5.2-O11 Group 4). */
enum class MicrophoneState {
    /** The service is not running. */
    OFF,
    /** RECORD_AUDIO is not granted. */
    NO_PERMISSION,
    /** The service was started in the background (after a reboot), where Android forbids the microphone until the Sensor app is opened. */
    NEEDS_APP_OPEN,
    LISTENING,
    /** The microphone delivers only zeros: Android or another app has silenced it. */
    SILENCED,
    /** The phone has no usable microphone, or it failed. */
    UNAVAILABLE
}

/** What tilt and lift detection is doing (ISSUE-NOV5.2-O11 Group 5). [isRunning] is false while the service is stopped. */
data class MotionState(val isRunning: Boolean = false, val isArmed: Boolean = false, val hasBarometer: Boolean = false)

/** The tilt and lift line of the Sensor Setup dialog. */
fun motionStatusText(state: MotionState): String = "Tilt & Lift: " + when {
    !state.isRunning -> "Off"
    state.isArmed -> "Armed (" + (if (state.hasBarometer) "accelerometer + barometer" else "accelerometer") + ")"
    else -> "Arms after the phone rests ${com.jd.core.shared.MOTION_ARM_AFTER_STILL_MS / 1000} s"
}

/** The microphone line of the Sensor Setup dialog. */
fun microphoneStatusText(state: MicrophoneState): String = "Microphone: " + when (state) {
    MicrophoneState.OFF -> "Off"
    MicrophoneState.NO_PERMISSION -> "Audio Denied"
    MicrophoneState.NEEDS_APP_OPEN -> "Open the Sensor app to start listening"
    MicrophoneState.LISTENING -> "Listening"
    MicrophoneState.SILENCED -> "Silenced by the system"
    MicrophoneState.UNAVAILABLE -> "Not available"
}

