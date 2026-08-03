package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for telemetry traces.
 * Aug.03.50:
 * - Issue #703: Forensic Audit: Trace Recovery Integrity Validation. Added 
 *   Magic Number, header sanity checks, and CRC32 entry validation (R703).
 *   Refactored checksum calculation for API 24 compatibility.
 * Aug.03.47:
 * - Issue #702: Forensic Audit: Trace Serialization Hardening. Implemented 
 *   full binary serialization for optimized traces. Moved string formatting 
 *   out of the hot-path to the background drainer (R702).
 * Aug.03.45:
 * - Issue #700: Forensic Audit: Power-Aware Sampling Scaling. Added 
 *   writeTraceOptimized() to eliminate object churn in 100Hz sampling (R668).
 * Aug.03.37:
 * - Issue #669: Forensic Audit: Database I/O Contention. Implemented MappedByteBuffer 
 *   to decouple 100Hz trace capture from SQLite WAL pressure (R-HARDWARE-01).
 */
@Singleton
class ForensicSpillBuffer @Inject constructor(@ApplicationContext private val context: Context) {

    private val spillFile = File(context.filesDir, FORENSIC_SPILL_FILE_NAME)
    private var mappedBuffer: MappedByteBuffer? = null
    
    // Header: [0..3] Magic, [4..7] Write Index, [8..11] Count
    private val writeIdx = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    // Reusable buffer for CRC calculation to maintain zero-allocation in hot-path.
    // Guarded by synchronized(this) in all usage sites.
    private val checksumBuffer = ByteArray(FORENSIC_SPILL_ENTRY_SIZE)

    private companion object {
        const val MAGIC_NUMBER = 0x46535042 // 'FSPB'
        const val HEADER_SIZE = 1024
        const val CHECKSUM_SIZE = 4
    }

    init {
        try {
            val size = (FORENSIC_SPILL_CAPACITY * FORENSIC_SPILL_ENTRY_SIZE).toLong() + HEADER_SIZE
            val raf = RandomAccessFile(spillFile, "rw")
            mappedBuffer = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, size).apply {
                order(ByteOrder.nativeOrder())
            }
            
            val buffer = mappedBuffer
            if (buffer != null) {
                val magic = buffer.getInt(0)
                if (magic != MAGIC_NUMBER) {
                    Timber.w("ForensicSpillBuffer: Invalid magic or new file. Resetting.")
                    resetBuffer()
                } else {
                    val recoveredIdx = buffer.getInt(4)
                    val recoveredCount = buffer.getInt(8)
                    
                    if (recoveredIdx in 0 until FORENSIC_SPILL_CAPACITY && 
                        recoveredCount in 0..FORENSIC_SPILL_CAPACITY) {
                        writeIdx.set(recoveredIdx)
                        totalCount.set(recoveredCount)
                        Timber.i("ForensicSpillBuffer initialized. Path: ${spillFile.absolutePath}, Recovered: $recoveredCount entries.")
                    } else {
                        Timber.e("ForensicSpillBuffer: Corrupted header ($recoveredIdx, $recoveredCount). Resetting.")
                        resetBuffer()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ForensicSpillBuffer")
        }
    }

    private fun resetBuffer() {
        val buffer = mappedBuffer ?: return
        buffer.putInt(0, MAGIC_NUMBER)
        buffer.putInt(4, 0)
        buffer.putInt(8, 0)
        writeIdx.set(0)
        totalCount.set(0)
    }

    /**
     * writeTrace: Serializes a LogEntry into the off-heap buffer.
     * Includes CRC32 checksum for integrity validation (R703).
     */
    fun writeTrace(entry: LogEntry) {
        val buffer = mappedBuffer ?: return
        
        synchronized(this) {
            val currentIdx = writeIdx.get()
            val offset = HEADER_SIZE + (currentIdx * FORENSIC_SPILL_ENTRY_SIZE)
            
            buffer.position(offset)
            buffer.putLong(entry.timestamp)
            buffer.putDouble(entry.lat)
            buffer.putDouble(entry.lng)
            buffer.putDouble(entry.accuracy)
            buffer.putDouble(entry.maxAccuracy)
            buffer.putDouble(entry.vibeSnapshot ?: -1.0)
            buffer.putDouble(entry.snrSnapshot ?: -1.0)
            
            // Flags: bit0=important, bit1=special
            var flags = 0
            if (entry.isImportant) flags = flags or 0x01
            if (entry.isSpecial) flags = flags or 0x02
            buffer.putInt(flags)
            
            buffer.putInt(0) // batteryLevel
            buffer.putDouble(0.0) // batteryTemp
            
            // Serialize message as UTF-8 (capped to avoid overflow)
            val msgBytes = entry.message.toByteArray(Charsets.UTF_8)
            val msgLen = msgBytes.size.coerceAtMost(FORENSIC_SPILL_ENTRY_SIZE - 100)
            buffer.putInt(msgLen)
            buffer.put(msgBytes, 0, msgLen)

            // Finalize entry with CRC32
            val crc = calculateEntryChecksum(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
            buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

            // Update header
            val nextIdx = (currentIdx + 1) % FORENSIC_SPILL_CAPACITY
            writeIdx.set(nextIdx)
            buffer.putInt(4, nextIdx)
            
            val count = totalCount.get()
            if (count < FORENSIC_SPILL_CAPACITY) {
                val newCount = count + 1
                totalCount.set(newCount)
                buffer.putInt(8, newCount)
            }
        }
    }

    /**
     * writeTraceOptimized: Zero-allocation entry point for high-frequency telemetry.
     * Hardened with CRC32 validation (R703).
     */
    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryLevel: Int, isCharging: Boolean, batteryTemp: Double
    ) {
        val buffer = mappedBuffer ?: return
        
        synchronized(this) {
            val currentIdx = writeIdx.get()
            val offset = HEADER_SIZE + (currentIdx * FORENSIC_SPILL_ENTRY_SIZE)
            
            buffer.position(offset)
            buffer.putLong(timestamp)
            buffer.putDouble(lat)
            buffer.putDouble(lng)
            buffer.putDouble(accuracy)
            buffer.putDouble(maxAccuracy)
            buffer.putDouble(vibe)
            buffer.putDouble(snr)
            
            // Flags: bit2=isCharging
            var flags = 0
            if (isCharging) flags = flags or 0x04
            buffer.putInt(flags)
            
            buffer.putInt(batteryLevel)
            buffer.putDouble(batteryTemp)
            buffer.putInt(0) // msgLen = 0 (means reconstruct from binary fields)

            // Finalize entry with CRC32
            val crc = calculateEntryChecksum(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
            buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

            val nextIdx = (currentIdx + 1) % FORENSIC_SPILL_CAPACITY
            writeIdx.set(nextIdx)
            buffer.putInt(4, nextIdx)
            
            val count = totalCount.get()
            if (count < FORENSIC_SPILL_CAPACITY) {
                val newCount = count + 1
                totalCount.set(newCount)
                buffer.putInt(8, newCount)
            }
        }
    }

    /**
     * drainTo: Reads entries from the buffer for database persistence.
     * Verifies CRC32 checksum for each entry before reconstruction (R703).
     */
    fun drainTo(limit: Int): List<LogEntry> {
        val buffer = mappedBuffer ?: return emptyList()
        val result = mutableListOf<LogEntry>()
        
        synchronized(this) {
            val count = totalCount.get()
            if (count == 0) return emptyList()
            
            val toDrain = count.coerceAtMost(limit)
            val currentIdx = writeIdx.get()
            var readIdx = (currentIdx - count + FORENSIC_SPILL_CAPACITY) % FORENSIC_SPILL_CAPACITY
            var actuallyDrained = 0
            
            repeat(toDrain) {
                val offset = HEADER_SIZE + (readIdx * FORENSIC_SPILL_ENTRY_SIZE)
                
                // Integrity Check
                val storedCrc = buffer.getInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                val calculatedCrc = calculateEntryChecksum(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE).toInt()
                
                if (storedCrc == calculatedCrc) {
                    buffer.position(offset)
                    val ts = buffer.getLong()
                    val lat = buffer.getDouble()
                    val lng = buffer.getDouble()
                    val acc = buffer.getDouble()
                    val maxAcc = buffer.getDouble()
                    val vibe = buffer.getDouble()
                    val snr = buffer.getDouble()
                    val flags = buffer.getInt()
                    val batLevel = buffer.getInt()
                    val batTemp = buffer.getDouble()
                    
                    val important = (flags and 0x01) != 0
                    val special = (flags and 0x02) != 0
                    val charging = (flags and 0x04) != 0
                    
                    val msgLen = buffer.getInt()
                    val msg = if (msgLen > 0) {
                        val msgBytes = ByteArray(msgLen)
                        buffer.get(msgBytes)
                        String(msgBytes, Charsets.UTF_8)
                    } else {
                        "F_TRACE: P=$batLevel% C=$charging T=${batTemp.roundToOneDecimal()}°C"
                    }
                    
                    result.add(LogEntry(
                        timestamp = ts, lat = lat, lng = lng, accuracy = acc, maxAccuracy = maxAcc,
                        vibeSnapshot = if (vibe == -1.0) null else vibe,
                        snrSnapshot = if (snr == -1.0) null else snr,
                        isImportant = important, isSpecial = special,
                        message = msg, type = "FORENSIC_TRACE",
                        id = "SYSTEM", role = "tracker"
                    ))
                } else {
                    Timber.e("ForensicSpillBuffer: Checksum mismatch at index $readIdx. Skipping corrupted entry.")
                }
                
                readIdx = (readIdx + 1) % FORENSIC_SPILL_CAPACITY
                actuallyDrained++
            }
            
            // Update state (always update count even if some entries were corrupted to keep circular logic)
            val remaining = count - actuallyDrained
            totalCount.set(remaining)
            buffer.putInt(8, remaining)
        }
        
        return result
    }

    /**
     * calculateEntryChecksum: API 24 compatible CRC32 calculation.
     * Uses a pre-allocated ByteArray to avoid object churn.
     */
    private fun calculateEntryChecksum(buffer: ByteBuffer, offset: Int, length: Int): Long {
        val crc = CRC32()
        val temp = buffer.duplicate()
        temp.position(offset)
        temp.get(checksumBuffer, 0, length)
        crc.update(checksumBuffer, 0, length)
        return crc.value
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun hasPending(): Boolean = totalCount.get() > 0
}
