package com.jd.core.shared

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Chapter 195.1 (ISSUE-NOV10.8-O80 part 2, Nov16.2): stores move once out of `no_backup/`. */
class BackupStoreFileTest {

    private val root: File = Files.createTempDirectory("backup-store").toFile()
    private val legacy = File(File(root, "no_backup"), "store.preferences_pb")
    private val target = BackupStoreFile.location(File(root, "files"), "store.preferences_pb")

    @After
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(file: File, text: String) {
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    @Test
    fun locationIsTheDataStoreFolder() {
        assertEquals(File(File(File(root, "files"), "datastore"), "store.preferences_pb"), target)
    }

    @Test
    fun movesTheOldFileWhenOnlyItExists() {
        write(legacy, "old")
        assertEquals(target, BackupStoreFile.moveIfNeeded(legacy, target))
        assertEquals("old", target.readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun keepsTheNewFileAndDropsTheOldWhenBothExist() {
        write(legacy, "old")
        write(target, "restored")
        BackupStoreFile.moveIfNeeded(legacy, target)
        assertEquals("restored", target.readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun doesNothingWhenNeitherExists() {
        assertEquals(target, BackupStoreFile.moveIfNeeded(legacy, target))
        assertFalse(target.exists())
        assertFalse(legacy.exists())
    }

    @Test
    fun aSecondCallKeepsTheMovedFile() {
        write(legacy, "old")
        BackupStoreFile.moveIfNeeded(legacy, target)
        BackupStoreFile.moveIfNeeded(legacy, target)
        assertTrue(target.exists())
        assertEquals("old", target.readText())
    }
}
