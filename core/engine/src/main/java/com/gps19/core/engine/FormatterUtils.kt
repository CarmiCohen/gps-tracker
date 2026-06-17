package com.gps19.core.engine

import java.util.Locale

/**
 * FormatterUtils: Pure logic string formatting for the engine.
 */
object FormatterUtils {

    /**
     * Unified duration formatting with day support.
     */
    fun formatDurationUnified(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val totalSeconds = ms / 1000
        val s = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val m = totalMinutes % 60
        val totalHours = totalMinutes / 60
        
        if (totalHours >= 24) {
            val days = totalHours / 24
            val hours = totalHours % 24
            return "${days}d ${hours}h ${m}m"
        }
        
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", totalHours, m, s)
    }

    /**
     * Concise duration formatting for logs.
     */
    fun formatDurationSimple(ms: Long): String {
        val totalSec = ms / 1000
        if (totalSec <= 0L) return "0s"
        if (totalSec < 60) return "${totalSec}s"
        val m = totalSec / 60
        val s = totalSec % 60
        if (m < 60) {
            return if (s == 0L) "${m}m" else "${m}m ${s}s"
        }
        val h = m / 60
        val mm = m % 60
        return if (mm == 0L) "${h}h" else "${h}h ${mm}m"
    }

    /**
     * Formats duration in "Xh YYm" or "Xd Yh ZZm" format for heartbeat logs.
     */
    fun formatDurationHoursMinutes(ms: Long): String {
        val totalMinutes = ms / 60000
        val m = totalMinutes % 60
        val totalHours = totalMinutes / 60
        val h = totalHours % 24
        val d = totalHours / 24

        return if (d > 0) {
            String.format(Locale.getDefault(), "%dd %02dh %02dm", d, h, m)
        } else {
            String.format(Locale.getDefault(), "%dh %02dm", h, m)
        }
    }

    /**
     * Cleans a log message for display.
     */
    fun cleanLogDisplayMessage(message: String): String {
        var m = message
        m = m.replace(Regex("^\\[[^\\]]+\\]\\s*"), "")
        m = m.replace(Regex("\\s*\\((Sustained|Interruption|Duration):[^)]+\\)", RegexOption.IGNORE_CASE), "")
        m = m.replace(Regex("\\s*after an interruption of[^.]+", RegexOption.IGNORE_CASE), "")
        m = m.replace(Regex("(?i)\\b(INFRA|SYSTEM|ALARM|EVENT):?\\s*"), "")
        return m.trim()
    }
}
