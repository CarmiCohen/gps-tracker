package com.gps19.core.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * SystemLogger: Shared logging infrastructure for Project Revolution.
 * Hardened: Throttled StateFlow updates and platform-agnostic delegation (Revol30.1).
 */
object SystemLogger {

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        override fun toString(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            return "[$time] $level/$tag: $message"
        }
    }

    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private const val MAX_BUFFER_SIZE = 500
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    /**
     * Platform delegate to allow Android-specific logging (Logcat) without
     * introducing Android dependencies to core-shared (Point 62).
     */
    var logDelegate: ((level: String, tag: String, message: String) -> Unit)? = null

    fun log(level: String, tag: String, message: String) {
        logDelegate?.invoke(level, tag, message)

        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        logBuffer.add(entry)
        while (logBuffer.size > MAX_BUFFER_SIZE) {
            logBuffer.poll()
        }
    }

    /**
     * Throttled UI update to prevent recomposition churn.
     */
    fun updateUiSnapshot() {
        _logs.value = logBuffer.toList()
    }

    fun i(tag: String, message: String) = log("I", tag, message)
    fun e(tag: String, message: String) = log("E", tag, message)
    fun w(tag: String, message: String) = log("W", tag, message)
}
