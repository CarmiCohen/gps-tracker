package com.gps19.core.engine

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

/**
 * CompressionUtils: Wire-level compression for signaling payloads (Rule 1.130).
 */
object CompressionUtils {
    
    private const val COMPRESSION_THRESHOLD = 512

    /**
     * Compresses the data using GZIP if it exceeds the threshold.
     * Returns a ByteArray where the first byte is the compression flag:
     * 0 = Uncompressed
     * 1 = Gzip
     */
    fun compressIfNeeded(data: ByteArray): ByteArray {
        if (data.size < COMPRESSION_THRESHOLD) {
            val result = ByteArray(data.size + 1)
            result[0] = 0 // Uncompressed
            System.arraycopy(data, 0, result, 1, data.size)
            return result
        }

        return try {
            val bos = ByteArrayOutputStream()
            bos.write(1) // Gzip Flag
            GZIPOutputStream(bos).use { gzip ->
                gzip.write(data)
            }
            bos.toByteArray()
        } catch (e: Exception) {
            // Fallback to uncompressed if compression fails
            val result = ByteArray(data.size + 1)
            result[0] = 0
            System.arraycopy(data, 0, result, 1, data.size)
            result
        }
    }

    /**
     * Decompresses data based on the header flag.
     */
    fun decompress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val flag = data[0].toInt()
        
        if (flag == 0) {
            return data.copyOfRange(1, data.size)
        }
        
        if (flag == 1) {
            return GZIPInputStream(data.inputStream(1, data.size - 1)).use { it.readBytes() }
        }
        
        return data // Unknown flag, return as is (risk mitigation)
    }
}
