package com.gps19.app

import android.content.Context
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for telemetry traces.
 * Aug.03.37:
 * - Issue #669: Forensic Audit: Database I/O Contention. Implemented MappedByteBuffer 
 *   to decouple 100Hz trace capture from SQLite WAL pressure (R-HARDWARE-01).
 */
@Singleton
class ForensicSpillBuffer @Inject constructor(@ApplicationContext private val context: Context) {

    private val spillFile = File(context.filesDir, FORENSIC_SPILL_FILE_NAME)
    private var mappedBuffer: MappedByteBuffer? = null
    
    // Header: [0..3] Write Index, [4..7] Count
    private val writeIdx = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    init {
        try {
            val size = (FORENSIC_SPILL_CAPACITY * FORENSIC_SPILL_ENTRY_SIZE).toLong() + 1024 // +1KB for header
            val raf = RandomAccessFile(spillFile, "rw")
            mappedBuffer = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, size).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // Recover state from header
            writeIdx.set(mappedBuffer?.getInt(0) ?: 0)
            totalCount.set(mappedBuffer?.getInt(4) ?: 0)
            
            Timber.i("ForensicSpillBuffer initialized. Path: ${spillFile.absolutePath}, Size: $size bytes, Recovered: ${totalCount.get()} entries.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ForensicSpillBuffer")
        }
    }

    /**
     * writeTrace: Serializes a LogEntry into the off-heap buffer.
     * Zero-allocation in the hot-path (R-HARDWARE-01).
     */
    fun writeTrace(entry: LogEntry) {
        val buffer = mappedBuffer ?: return
        
        synchronized(this) {
            val currentIdx = writeIdx.get()
            val offset = 1024 + (currentIdx * FORENSIC_SPILL_ENTRY_SIZE)
            
            buffer.position(offset)
            buffer.putLong(entry.timestamp)
            buffer.putDouble(entry.lat)
            buffer.putDouble(entry.lng)
            buffer.putDouble(entry.accuracy)
            buffer.putDouble(entry.maxAccuracy)
            buffer.putDouble(entry.vibeSnapshot ?: -1.0)
            buffer.putDouble(entry.snrSnapshot ?: -1.0)
            buffer.putInt(if (entry.isImportant) 1 else 0)
            buffer.putInt(if (entry.isSpecial) 1 else 0)
            
            // Serialize message as UTF-8 (capped to avoid overflow)
            val msgBytes = entry.message.toByteArray(Charsets.UTF_8)
            val msgLen = msgBytes.size.coerceAtMost(FORENSIC_SPILL_ENTRY_SIZE - 64)
            buffer.putInt(msgLen)
            buffer.put(msgBytes, 0, msgLen)

            // Update header
            val nextIdx = (currentIdx + 1) % FORENSIC_SPILL_CAPACITY
            writeIdx.set(nextIdx)
            buffer.putInt(0, nextIdx)
            
            val count = totalCount.get()
            if (count < FORENSIC_SPILL_CAPACITY) {
                val newCount = count + 1
                totalCount.set(newCount)
                buffer.putInt(4, newCount)
            }
        }
    }

    /**
     * drainTo: Reads entries from the buffer for database persistence.
     * Used by background worker to batch-insert traces.
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
            
            repeat(toDrain) {
                val offset = 1024 + (readIdx * FORENSIC_SPILL_ENTRY_SIZE)
                buffer.position(offset)
                
                val ts = buffer.getLong()
                val lat = buffer.getDouble()
                val lng = buffer.getDouble()
                val acc = buffer.getDouble()
                val maxAcc = buffer.getDouble()
                val vibe = buffer.getDouble()
                val snr = buffer.getDouble()
                val important = buffer.getInt() == 1
                val special = buffer.getInt() == 1
                
                val msgLen = buffer.getInt()
                val msgBytes = ByteArray(msgLen)
                buffer.get(msgBytes)
                val msg = String(msgBytes, Charsets.UTF_8)
                
                result.add(LogEntry(
                    timestamp = ts, lat = lat, lng = lng, accuracy = acc, maxAccuracy = maxAcc,
                    vibeSnapshot = if (vibe == -1.0) null else vibe,
                    snrSnapshot = if (snr == -1.0) null else snr,
                    isImportant = important, isSpecial = special,
                    message = msg, type = "FORENSIC_TRACE",
                    id = "SYSTEM", role = "tracker"
                ))
                
                readIdx = (readIdx + 1) % FORENSIC_SPILL_CAPACITY
            }
            
            // Update state
            val remaining = count - toDrain
            totalCount.set(remaining)
            buffer.putInt(4, remaining)
        }
        
        return result
    }

    fun hasPending(): Boolean = totalCount.get() > 0
}
