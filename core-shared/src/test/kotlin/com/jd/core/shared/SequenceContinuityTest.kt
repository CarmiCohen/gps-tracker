package com.jd.core.shared

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies Chapter 120.1: sequence numbers continue after a process restart. */
class SequenceContinuityTest {

    private fun update(sequence: Long) = LocationUpdate(sequenceId = sequence, timestamp = sequence)

    @Test
    fun emptyBufferStartsAtZero() {
        val buffer = ForensicBuffer(Files.createTempDirectory("seq").toFile(), capacity = 10)
        buffer.initialize()

        assertEquals(0L, buffer.nextSequenceId())
    }

    @Test
    fun reopenedBufferContinuesAfterTheHighestStoredSequence() {
        val dir = Files.createTempDirectory("seq").toFile()
        ForensicBuffer(dir, capacity = 10).apply {
            initialize()
            (0L..4L).forEach { writeTrace(update(it)) }
        }

        val reopened = ForensicBuffer(dir, capacity = 10).apply { initialize() }

        // Numbers up to the stored reservation are skipped (ISSUE-NOV6.0-O20).
        assertEquals(100L, reopened.nextSequenceId())
    }

    @Test
    fun wrappedBufferStillFindsTheHighestSequence() {
        val buffer = ForensicBuffer(Files.createTempDirectory("seq").toFile(), capacity = 10)
        buffer.initialize()
        (95L..124L).forEach { buffer.writeTrace(update(it)) }

        assertEquals(195L, buffer.nextSequenceId())
    }
}
