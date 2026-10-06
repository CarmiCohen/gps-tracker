package com.gps19.core.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionUtilsTest {

    @Test
    fun `Data below threshold should not be compressed but should have flag`() {
        val smallData = ByteArray(100) { it.toByte() }
        val processed = CompressionUtils.compressIfNeeded(smallData)
        
        assertEquals(101, processed.size)
        assertEquals(0.toByte(), processed[0]) // Uncompressed flag
        assertArrayEquals(smallData, processed.copyOfRange(1, processed.size))
        
        val decompressed = CompressionUtils.decompress(processed)
        assertArrayEquals(smallData, decompressed)
    }

    @Test
    fun `Data above threshold should be compressed`() {
        // Create highly compressible data (repeat values)
        val largeData = ByteArray(1024) { 65.toByte() }
        val processed = CompressionUtils.compressIfNeeded(largeData)
        
        assertEquals(1.toByte(), processed[0]) // Gzip flag
        // Gzip on repeating bytes should be much smaller than 1024
        assertTrue("Compression should reduce size: ${processed.size} vs 1024", processed.size < 1024)
        
        val decompressed = CompressionUtils.decompress(processed)
        assertArrayEquals(largeData, decompressed)
    }

    @Test
    fun `Decompress should handle empty data gracefully`() {
        val empty = ByteArray(0)
        assertArrayEquals(empty, CompressionUtils.decompress(empty))
    }
}
