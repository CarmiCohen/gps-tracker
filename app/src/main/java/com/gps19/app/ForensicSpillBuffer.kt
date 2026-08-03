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
 * Aug.03.55:
 * - Issue #704: Forensic Audit: Trace Backfill Flow Hardening. Implemented 
 *   transactional drain (peek/commit) to prevent data loss on DB failure. 
 *   Added readIdx to header for robust circular tracking (R704).
 * Aug.03.50:
 * - Issue #703: Forensic Audit: Trace Recovery Integrity Validation. Added 
 *   Magic Number, header sanity checks, and CRC32 entry validation (R703).
 * Aug.03.47:
 * - Issue #702: Forensic Audit: Trace Serialization Hardening. Implemented 
 *   full binary serialization for optimized traces.
 */
@Singleton
class ForensicSpillBuffer @Inject constructor(@ApplicationContext private val context: Context) {

    private val spillFile = File(context.filesDir, FORENSIC_SPILL_FILE_NAME)
    private var mappedBuffer: MappedByteBuffer? = null
    
    // Header: [0..3] Magic, [4..7] Write Index, [8..11] Total Count, [12..15] Read Index
    private val writeIdx = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)
    private val readIdx = AtomicInteger(0)

    private val checksumBuffer = ByteArray(FORENSIC_SPILL_ENTRY_SIZE)

    private companion object {
        const val MAGIC_NUMBER = 0x46535042
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
                    val recoveredWrite = buffer.getInt(4)
                    val recoveredCount = buffer.getInt(8)
                    val recoveredRead = buffer.getInt(12)
                    
                    if (recoveredWrite in 0 until FORENSIC_SPILL_CAPACITY && 
                        recoveredCount in 0..FORENSIC_SPILL_CAPACITY &&
                        recoveredRead in 0 until FORENSIC_SPILL_CAPACITY) {
                        writeIdx.set(recoveredWrite)
                        totalCount.set(recoveredCount)
                        readIdx.set(recoveredRead)
                        Timber.i("ForensicSpillBuffer initialized. Recovered: $recoveredCount entries (R:$recoveredRead W:$recoveredWrite).")
                    } else {
                        Timber.e("ForensicSpillBuffer: Corrupted header. Resetting.")
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
        buffer.putInt(4, 0) // writeIdx
        buffer.putInt(8, 0) // totalCount
        buffer.putInt(12, 0) // readIdx
        writeIdx.set(0)
        totalCount.set(0)
        readIdx.set(0)
    }

    fun writeTrace(entry: LogEntry) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val currentWrite = writeIdx.get()
            val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
            
            buffer.position(offset)
            buffer.putLong(entry.timestamp)
            buffer.putDouble(entry.lat)
            buffer.putDouble(entry.lng)
            buffer.putDouble(entry.accuracy)
            buffer.putDouble(entry.maxAccuracy)
            buffer.putDouble(entry.vibeSnapshot ?: -1.0)
            buffer.putDouble(entry.snrSnapshot ?: -1.0)
            
            var flags = 0
            if (entry.isImportant) flags = flags or 0x01
            if (entry.isSpecial) flags = flags or 0x02
            buffer.putInt(flags)
            buffer.putInt(0) // batteryLevel
            buffer.putDouble(0.0) // batteryTemp
            
            val msgBytes = entry.message.toByteArray(Charsets.UTF_8)
            val msgLen = msgBytes.size.coerceAtMost(FORENSIC_SPILL_ENTRY_SIZE - 100)
            buffer.putInt(msgLen)
            buffer.put(msgBytes, 0, msgLen)

            val crc = calculateEntryChecksum(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
            buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

            advanceWritePointer()
        }
    }

    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryLevel: Int, isCharging: Boolean, batteryTemp: Double
    ) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val currentWrite = writeIdx.get()
            val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
            
            buffer.position(offset)
            buffer.putLong(timestamp)
            buffer.putDouble(lat)
            buffer.putDouble(lng)
            buffer.putDouble(accuracy)
            buffer.putDouble(maxAccuracy)
            buffer.putDouble(vibe)
            buffer.putDouble(snr)
            
            var flags = 0
            if (isCharging) flags = flags or 0x04
            buffer.putInt(flags)
            buffer.putInt(batteryLevel)
            buffer.putDouble(batteryTemp)
            buffer.putInt(0) // msgLen = 0

            val crc = calculateEntryChecksum(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
            buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

            advanceWritePointer()
        }
    }

    private fun advanceWritePointer() {
        val buffer = mappedBuffer ?: return
        val currentWrite = writeIdx.get()
        val nextWrite = (currentWrite + 1) % FORENSIC_SPILL_CAPACITY
        writeIdx.set(nextWrite)
        buffer.putInt(4, nextWrite)
        
        val count = totalCount.get()
        if (count < FORENSIC_SPILL_CAPACITY) {
            val newCount = count + 1
            totalCount.set(newCount)
            buffer.putInt(8, newCount)
        } else {
            // Buffer full: oldest entry is being overwritten, so advance readIdx too
            val nextRead = (readIdx.get() + 1) % FORENSIC_SPILL_CAPACITY
            readIdx.set(nextRead)
            buffer.putInt(12, nextRead)
        }
    }

    /**
     * peek: Reads entries for backfilling without advancing the read pointer.
     */
    fun peek(limit: Int): List<LogEntry> {
        val buffer = mappedBuffer ?: return emptyList()
        val result = mutableListOf<LogEntry>()
        
        synchronized(this) {
            val count = totalCount.get()
            if (count == 0) return emptyList()
            
            val toPeek = count.coerceAtMost(limit)
            var currentRead = readIdx.get()
            
            repeat(toPeek) {
                val offset = HEADER_SIZE + (currentRead * FORENSIC_SPILL_ENTRY_SIZE)
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
                }
                currentRead = (currentRead + 1) % FORENSIC_SPILL_CAPACITY
            }
        }
        return result
    }

    /**
     * commitDrain: Advances the read pointer after successful database persistence.
     * Safely handles concurrent overwrites by ensuring readIdx only moves forward.
     */
    fun commitDrain(count: Int) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val actualToConsume = count.coerceAtMost(totalCount.get())
            if (actualToConsume <= 0) return

            val newRead = (readIdx.get() + actualToConsume) % FORENSIC_SPILL_CAPACITY
            readIdx.set(newRead)
            buffer.putInt(12, newRead)

            val newCount = totalCount.get() - actualToConsume
            totalCount.set(newCount)
            buffer.putInt(8, newCount)
        }
    }

    @Deprecated("Use peek() and commitDrain() for transactional safety", ReplaceWith("peek(limit)"))
    fun drainTo(limit: Int): List<LogEntry> {
        val entries = peek(limit)
        commitDrain(entries.size)
        return entries
    }

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
