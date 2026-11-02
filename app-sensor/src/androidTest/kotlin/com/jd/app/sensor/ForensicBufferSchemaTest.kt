package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.ForensicBuffer
import com.jd.core.shared.PoolRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ForensicBufferSchemaTest: Ported from OLD version.
 * Verifies byte-level alignment and wrap-around logic in pure Kotlin.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicBufferSchemaTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var buffer: ForensicBuffer

    @Before
    fun init() {
        hiltRule.inject()
        val testDir = File(System.getProperty("java.io.tmpdir"), "forensic_schema_test")
        testDir.deleteRecursively()
        testDir.mkdirs()
        buffer = ForensicBuffer(testDir)
        buffer.initialize()
    }

    @Test
    fun verifySchemaAlignment() {
        val update = PoolRegistry.locationUpdatePool.acquire().apply {
            timestamp = 12345L
            lat = 32.123456
            lng = 34.654321
            status = "SCHEMA_TEST"
        }
        
        buffer.writeTrace(update)
        val traces = buffer.readTraces()
        
        assertTrue("Traces should not be empty", traces.isNotEmpty())
        val read = traces.last()
        assertEquals(update.timestamp, read.timestamp)
        assertEquals(update.lat, read.lat, 0.000001)
        assertEquals(update.status, read.status)
        
        PoolRegistry.locationUpdatePool.release(update)
    }

    @Test
    fun verifyOverflowOverwrite() {
        val cap = buffer.capacity
        buffer.clear()
        
        val update = PoolRegistry.locationUpdatePool.acquire()
        
        repeat(cap) { i ->
            update.apply {
                timestamp = 1000L + i
                sequenceId = i.toLong()
            }
            buffer.writeTrace(update)
        }
        
        assertTrue(buffer.hasPending())
        
        val overflowTs = 1000L + cap + 1
        update.apply {
            timestamp = overflowTs
            sequenceId = (cap + 1).toLong()
        }
        buffer.writeTrace(update)
        
        val entities = buffer.readTraces()
        assertEquals(cap, entities.size)
        assertFalse("Oldest entry should be overwritten", entities.any { it.timestamp == 1000L })
        assertTrue("Newest entry should be present", entities.any { it.timestamp == overflowTs })
        assertEquals("Head of buffer should be the second entry", 1001L, entities.first().timestamp)
        
        PoolRegistry.locationUpdatePool.release(update)
    }
}
