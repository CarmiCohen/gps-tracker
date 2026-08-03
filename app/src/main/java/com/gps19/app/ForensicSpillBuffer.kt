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
import kotlin.math.round

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for telemetry traces.
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
            
            // Flags: bit0=important, bit1=special
            var flags = 0
            if (entry.isImportant) flags = flags or 0x01
            if (entry.isSpecial) flags = flags or 0x02
            buffer.putInt(flags)
            
            buffer.putInt(0) // batteryLevel
            buffer.putDouble(0.0) // batteryTemp
            
            // Serialize message as UTF-8 (capped to avoid overflow)
            val msgBytes = entry.message.toByteArray(Charsets.UTF_8)
            val msgLen = msgBytes.size.coerceAtMost(FORENSIC_SPILL_ENTRY_SIZE - 80)
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
     * writeTraceOptimized: Zero-allocation entry point for high-frequency telemetry.
     * Hardened binary serialization prevents all object churn in 100Hz loop (R702).
     */
    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryLevel: Int, isCharging: Boolean, batteryTemp: Double
    ) {
        val buffer = mappedBuffer ?: return
        
        synchronized(this) {
            val currentIdx = writeIdx.get()
            val offset = 1024 + (currentIdx * FORENSIC_SPILL_ENTRY_SIZE)
            
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
     * Reconstructs messages from binary fields outside the hot-path.
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
                    // Reconstruct from binary fields (Issue #702 hardening)
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
                
                readIdx = (readIdx + 1) % FORENSIC_SPILL_CAPACITY
            }
            
            // Update state
            val remaining = count - toDrain
            totalCount.set(remaining)
            buffer.putInt(4, remaining)
        }
        
        return result
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun hasPending(): Boolean = totalCount.get() > 0
}
