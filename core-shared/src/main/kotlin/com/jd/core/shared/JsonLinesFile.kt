package com.jd.core.shared

import java.io.File

/**
 * A file of one JSON text per line, shared by the [EventLog] and the [GraphHistory] (ISSUE-NOV10.8-O84). Failures are
 * logged under [tag] and never thrown: a history that cannot be stored must not stop the app. Not thread-safe: each
 * owner calls it from its single writer thread.
 */
internal class JsonLinesFile(private val file: File, private val tag: String) {

    /** Every line, or null when the file does not exist or cannot be read. */
    fun readLines(): List<String>? {
        if (!file.exists()) return null
        return try {
            file.readLines()
        } catch (e: Exception) {
            SystemLogger.e(tag, "Read failed: ${e.message}")
            null
        }
    }

    fun append(lines: List<String>) {
        if (lines.isEmpty()) return
        try {
            file.parentFile?.mkdirs()
            file.appendText(lines.joinToString(separator = "") { it + "\n" })
        } catch (e: Exception) {
            SystemLogger.e(tag, "Write failed: ${e.message}")
        }
    }

    /** Replaces the file through a temporary one, so a crash leaves either the old or the new file. */
    fun rewrite(lines: List<String>) {
        try {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(lines.joinToString(separator = "") { it + "\n" })
            if (!temporary.renameTo(file)) {
                file.delete()
                temporary.renameTo(file)
            }
        } catch (e: Exception) {
            SystemLogger.e(tag, "Rewrite failed: ${e.message}")
        }
    }

    fun delete() {
        if (file.exists() && !file.delete()) SystemLogger.e(tag, "Could not delete ${file.name}")
    }
}
