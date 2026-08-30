package com.gps19.app

import android.os.Build

/**
 * ForensicSanitizer: Utility to scrub sensitive metadata (internal paths, hardware IDs)
 * from logs and exported files to ensure forensic integrity (Issue #779).
 * Aug.30.13:
 * - Implementation of path scrubbing and hardware identifier normalization.
 */
object ForensicSanitizer {
    private const val INTERNAL_PATH_PATTERN = "/data/(user|data)/[0-9]+/com\\.gps19\\.app"
    private val PATH_REGEX = Regex(INTERNAL_PATH_PATTERN)
    
    // Hardware identifiers to mask in standard logs
    private val HARDWARE_IDENTIFIERS = listOf(
        Build.MODEL,
        Build.MANUFACTURER,
        Build.BOARD,
        Build.DEVICE,
        Build.PRODUCT
    ).filter { it.isNotBlank() && it != "unknown" }

    /**
     * sanitizeMessage: Removes absolute internal paths from log messages.
     */
    fun sanitizeMessage(message: String): String {
        return message.replace(PATH_REGEX, "[INTERNAL_PATH]")
    }

    /**
     * sanitizeStackTrace: Scrubs internal paths from stack traces and truncates.
     */
    fun sanitizeStackTrace(t: Throwable, limit: Int = 500): String {
        val stackTrace = t.stackTraceToString()
        val sanitized = sanitizeMessage(stackTrace)
        return sanitized.take(limit)
    }

    /**
     * scrubHardwareInfo: Normalizes hardware-specific strings unless the log is marked as special.
     */
    fun scrubHardwareInfo(message: String, isSpecial: Boolean = false): String {
        if (isSpecial) return message
        var sanitized = message
        HARDWARE_IDENTIFIERS.forEach { id ->
            if (id.length > 3) { // Avoid masking tiny generic strings
                sanitized = sanitized.replace(id, "[HW_ID]", ignoreCase = true)
            }
        }
        return sanitized
    }
}
