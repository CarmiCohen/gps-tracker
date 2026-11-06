package com.jd.core.shared

import java.io.File
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies Chapter 121: stored entries survive interrupted writes, power loss, damaged headers,
 * and the upgrade from file version 2 (ISSUE-NOV6.0-O20).
 *
 * Interruptions are simulated by editing the file into the state they leave behind, because a
 * unit test cannot cut power.
 */
class ForensicEntryIntegrityTest {

    private val capacity = 10
    private val headerSize = 64
    private val entrySize = 128
    private val checksumOffset = 84

    private val offVersion = 4
    private val offWriteIdx = 8
    private val offCount = 12
    private val offReservedSeq = 24

    private val dir: File = Files.createTempDirectory("forensic").toFile()

    private fun update(sequence: Long) =
        LocationUpdate(sequenceId = sequence, timestamp = 1000 + sequence).apply {
            lat = 32.0 + sequence
            lng = 34.0
        }

    private fun openBuffer(directory: File = dir) =
        ForensicBuffer(directory, capacity).apply { assertTrue(initialize()) }

    private fun writeSequences(range: LongRange) {
        val buffer = openBuffer()
        range.forEach { buffer.writeTrace(update(it)) }
    }

    private fun <T> withFile(block: (MappedByteBuffer) -> T): T =
        FileChannel.open(
            File(dir, "forensic_trace.bin").toPath(),
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
        ).use { channel ->
            val mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size())
            mapped.order(ByteOrder.nativeOrder())
            block(mapped)
        }

    private fun slotOffset(slot: Int) = headerSize + slot * entrySize

    private fun sequences(buffer: ForensicBuffer) = buffer.readTraces().map { it.sequenceId }

    @Test
    fun interruptedWriteOverTheOldestEntryIsSkipped() {
        writeSequences(0L..14L)

        // Slot 5 holds sequence 5, the oldest. A crash left only the first new bytes in it.
        withFile { file -> (0 until 16).forEach { file.put(slotOffset(5) + it, 0x7F) } }

        val traces = openBuffer().readTraces()
        assertEquals((6L..14L).toList(), traces.map { it.sequenceId })
        assertEquals(32.0 + 14, traces.last().lat)
        assertEquals(1014L, traces.last().timestamp)
    }

    @Test
    fun checksumWrittenLastMeansAFullyCopiedButUncheckedEntryIsSkipped() {
        writeSequences(0L..14L)

        // Every field of slot 5 is new, but the crash came before the checksum.
        withFile { file -> file.putLong(slotOffset(5) + 32, 15L) }

        assertFalse(15L in sequences(openBuffer()))
    }

    @Test
    fun headerClaimingEntriesThatNeverReachedStorageIsHandled() {
        writeSequences(0L..4L)

        // Power loss: the header reached storage, the data of the two newest entries did not.
        withFile { file ->
            (slotOffset(3) until slotOffset(5)).forEach { file.put(it, 0) }
        }

        assertEquals(listOf(0L, 1L, 2L), sequences(openBuffer()))
    }

    @Test
    fun sequenceNumbersAreNotReusedAfterPowerLoss() {
        val buffer = openBuffer()
        buffer.writeTrace(update(0))

        // What storage holds at this moment; everything after it is lost with the power.
        val stored = Files.createTempDirectory("stored").toFile()
        File(dir, "forensic_trace.bin").copyTo(File(stored, "forensic_trace.bin"))
        (1L..50L).forEach { buffer.writeTrace(update(it)) }

        assertTrue(openBuffer(stored).nextSequenceId() > 50L)
    }

    @Test
    fun everyWrittenNumberIsBelowTheStoredReservation() {
        val buffer = openBuffer()
        for (sequence in 0L..350L) {
            buffer.writeTrace(update(sequence))
            val reserved = withFile { it.getLong(offReservedSeq) }
            assertTrue(sequence < reserved, "Sequence $sequence is not below reservation $reserved")
        }
    }

    @Test
    fun versionTwoFileKeepsItsEntriesAndSequenceAfterUpgrade() {
        writeSequences(0L..14L)
        withFile { file ->
            file.putInt(offVersion, 2)
            file.putLong(offReservedSeq, 0L)
            (0 until capacity).forEach { file.putInt(slotOffset(it) + checksumOffset, 0) }
        }

        val upgraded = openBuffer()

        assertEquals((5L..14L).toList(), sequences(upgraded))
        assertEquals(15L, upgraded.nextSequenceId())
        assertEquals(3, withFile { it.getInt(offVersion) })
        assertEquals((5L..14L).toList(), sequences(openBuffer()))
    }

    @Test
    fun partlyFilledVersionTwoFileDoesNotAdoptUnusedSlots() {
        writeSequences(0L..3L)
        withFile { file ->
            file.putInt(offVersion, 2)
            (0 until capacity).forEach { file.putInt(slotOffset(it) + checksumOffset, 0) }
        }

        val upgraded = openBuffer()
        upgraded.writeTrace(update(4))

        assertEquals((0L..4L).toList(), sequences(upgraded))
    }

    @Test
    fun outOfRangeWriteIndexIsRebuiltAndWritingContinues() {
        writeSequences(0L..14L)
        withFile { it.putInt(offWriteIdx, 999) }

        val recovered = openBuffer()
        assertEquals((5L..14L).toList(), sequences(recovered))

        recovered.writeTrace(update(15))
        assertEquals((6L..15L).toList(), sequences(recovered))
    }

    @Test
    fun headerTornBetweenIndexAndCountIsRebuilt() {
        writeSequences(0L..3L)

        // The index moved to 4 and the crash came before the count did.
        withFile { it.putInt(offCount, 3) }

        val recovered = openBuffer()
        assertEquals((0L..3L).toList(), sequences(recovered))

        recovered.writeTrace(update(4))
        assertEquals((0L..4L).toList(), sequences(recovered))
    }

    @Test
    fun headerTornAtTheWrapPointIsRebuiltInOrder() {
        writeSequences(0L..9L)

        // The index wrapped to 0 and the crash came before the count and wrapped flag did.
        withFile { file ->
            file.putInt(offCount, 9)
            file.putInt(16, 0)
        }

        val recovered = openBuffer()
        assertEquals((0L..9L).toList(), sequences(recovered))

        recovered.writeTrace(update(10))
        assertEquals((1L..10L).toList(), sequences(recovered))
    }

    @Test
    fun resetAfterUnknownVersionIsStoredInTheFile() {
        writeSequences(0L..4L)
        withFile { it.putInt(offVersion, 1) }

        assertEquals(emptyList(), sequences(openBuffer()))

        // Without a write in between, a second open must see the reset too.
        assertEquals(emptyList(), sequences(openBuffer()))
        assertEquals(0, withFile { it.getInt(offCount) })
    }
}
