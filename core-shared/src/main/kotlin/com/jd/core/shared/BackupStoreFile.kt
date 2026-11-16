package com.jd.core.shared

import java.io.File

/**
 * ISSUE-NOV10.8-O80 part 2 (Nov16.2): the stores Auto Backup must copy live in `files/datastore/`.
 * Until Nov16.1 they were in `no_backup/`, which Auto Backup never copies; [moveIfNeeded] moves an
 * old file once, the first time its store is opened.
 */
object BackupStoreFile {

    /** The folder, under a `filesDir`, that DataStore uses and the backup rules include. */
    const val DIRECTORY = "datastore"

    /** The backed-up place of [fileName] under [filesDir]. */
    fun location(filesDir: File, fileName: String): File = File(File(filesDir, DIRECTORY), fileName)

    /**
     * Returns [target] after moving [legacy] there when only [legacy] exists. When both exist,
     * [target] wins (a restored backup or an earlier move) and [legacy] is deleted.
     */
    fun moveIfNeeded(legacy: File, target: File): File {
        if (!legacy.exists()) return target
        if (target.exists()) {
            legacy.delete()
            return target
        }
        target.parentFile?.mkdirs()
        if (!legacy.renameTo(target)) {
            legacy.copyTo(target, overwrite = true)
            legacy.delete()
        }
        return target
    }
}
