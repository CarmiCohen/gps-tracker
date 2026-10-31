package com.jd.core.shared

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/**
 * ForensicBufferStressTest: Audits rollover integrity and data persistence.
 * Part of ISSUE-REVOL30.0-F8.
 */
class ForensicBufferStressTest {

    private lateinit var tempDir: File
    private lateinit var buffer: ForensicBuffer

    @BeforeTest
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "forensic_test_${System.nanoTime()}")
        tempDir.mkdirs()
        buffer = ForensicBuffer(tempDir, capacity = 100)
        buffer.initialize()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testRolloverIntegrity() {
        for (i in 1..100) {
            buffer.writeTrace(createUpdate(i.toLong()))
        }
        
        var traces = buffer.readTraces()
        assertEquals(100, traces.size)
        assertEquals(1L, traces.first().sequenceId)
        assertEquals(100L, traces.last().sequenceId)

        for (i in 101..150) {
            buffer.writeTrace(createUpdate(i.toLong()))
        }

        traces = buffer.readTraces()
        assertEquals(100, traces.size)
        assertEquals(51L, traces.first().sequenceId)
        assertEquals(150L, traces.last().sequenceId)
        
        for (i in 0 until 100) {
            assertEquals((51 + i).toLong(), traces[i].sequenceId)
        }
    }

    @Test
    fun testRangeQueryWithRollover() {
        for (i in 1..250) {
            buffer.writeTrace(createUpdate(i.toLong()))
        }

        val range = buffer.readRange(160L, 170L)
        assertEquals(11, range.size)
        assertEquals(160L, range.first().sequenceId)
        assertEquals(170L, range.last().sequenceId)
        
        val partialRange = buffer.readRange(140L, 160L)
        assertEquals(10, partialRange.size)
        assertEquals(151L, partialRange.first().sequenceId)
        assertEquals(160L, partialRange.last().sequenceId)
    }

    @Test
    fun testPersistenceAfterReInitialization() {
        for (i in 1..50) {
            buffer.writeTrace(createUpdate(i.toLong()))
        }

        val sameBuffer = ForensicBuffer(tempDir, capacity = 100)
        sameBuffer.initialize()

        val traces = sameBuffer.readTraces()
        assertEquals(50, traces.size)
        assertEquals(1L, traces.first().sequenceId)
        assertEquals(50L, traces.last().sequenceId)
    }

    @Test
    fun testConcurrentWrites() {
        val threadCount = 4
        val writesPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        
        for (t in 0 until threadCount) {
            executor.execute {
                for (i in 0 until writesPerThread) {
                    buffer.writeTrace(createUpdate((t * writesPerThread + i).toLong()))
                }
            }
        }
        
        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        
        val traces = buffer.readTraces()
        assertEquals(100, traces.size, "Buffer should be full at capacity")
        
        traces.forEach {
            assertTrue(it.sequenceId >= 0)
        }
    }

    @Test
    fun testHeaderCorruptionRecovery() {
        // 1. Write some data
        for (i in 1..10) {
            buffer.writeTrace(createUpdate(i.toLong()))
        }
        
        // 2. Corrupt the magic number in the file
        val file = File(tempDir, "forensic_trace.bin")
        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.writeInt(0xDEADBEEF.toInt())
        raf.close()
        
        // 3. Re-initialize buffer. It should detect corruption and reset.
        val newBuffer = ForensicBuffer(tempDir, capacity = 100)
        newBuffer.initialize()
        
        assertEquals(0, newBuffer.readTraces().size, "Buffer should have been reset after corruption")
        
        // 4. Verify it's still usable
        newBuffer.writeTrace(createUpdate(999L))
        assertEquals(1, newBuffer.readTraces().size)
        assertEquals(999L, newBuffer.readTraces().first().sequenceId)
    }

    private fun createUpdate(seq: Long) = LocationUpdate().apply {
        sequenceId = seq
        timestamp = System.currentTimeMillis()
        lat = 45.0
        lng = 9.0
        sensorSnapshot.vibration = 0.5f
    }
}
