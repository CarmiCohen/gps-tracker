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
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for telemetry traces.
 * Aug.08.21:
 * - Issue #126: Forensic Payload Overflow Audit. Implemented safe UTF-8 truncation 
 *   to prevent corruption of multi-byte sequences at the 56-byte boundary (R126).
 * - Issue #125: Forensic Audit: Compression Parity Audit. Integrated gpsHardwareLock 
 *   into bit-packed flags (0x08) to maintain forensic parity (R125).
 * Aug.07.116:
 * - Issue #743: Forensic Spill-Buffer Write Compression. Implemented version 2 
 *   of the binary format with 96-byte entries. Uses bit-packing for flags/battery 
 *   and optimized alignment to maximize message payload (R743).
 */
@Singleton
class ForensicSpillBuffer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {

    private val spillFile = File(context.filesDir, FORENSIC_SPILL_FILE_NAME)
    private var mappedBuffer: MappedByteBuffer? = null
    
    // Pointers and metadata tracked in header
    private val writeIdx = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)
    private val readIdx = AtomicInteger(0)
    
    private val baseTs = AtomicLong(0L)
    private var baseLat: Double = 0.0
    private var baseLng: Double = 0.0

    private val checksumBuffer = ByteArray(FORENSIC_SPILL_ENTRY_SIZE)

    private companion object {
        const val MAGIC_NUMBER = 0x46535042
        const val CURRENT_VERSION = 2 // Issue #743: Bumped for new 96-byte format
        const val HEADER_SIZE = 128
        const val CHECKSUM_SIZE = 4
        
        // Header Offsets
        const val OFF_MAGIC = 0
        const val OFF_VERSION = 4
        const val OFF_CAPACITY = 8
        const val OFF_ENTRY_SIZE = 12
        const val OFF_LAST_WRITE_RT = 16
        const val OFF_WRITE_IDX = 24
        const val OFF_COUNT = 28
        const val OFF_READ_IDX = 32
        const val OFF_BASE_TS = 36
        const val OFF_BASE_LAT = 44
        const val OFF_BASE_LNG = 52

        const val PRECISION_SCALE = 10_000_000.0 // 1e7 for ~1cm precision
        const val WRITE_STALL_THRESHOLD_MS = 5L
    }

    init {
        try {
            val size = (FORENSIC_SPILL_CAPACITY * FORENSIC_SPILL_ENTRY_SIZE).toLong() + HEADER_SIZE
            RandomAccessFile(spillFile, "rw").use { raf ->
                mappedBuffer = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, size).apply {
                    order(ByteOrder.nativeOrder())
                }
            }
            
            val buffer = mappedBuffer
            if (buffer != null) {
                val magic = buffer.getInt(OFF_MAGIC)
                val version = if (magic == MAGIC_NUMBER) buffer.getInt(OFF_VERSION) else -1
                val cap = if (magic == MAGIC_NUMBER) buffer.getInt(OFF_CAPACITY) else -1
                val entrySz = if (magic == MAGIC_NUMBER) buffer.getInt(OFF_ENTRY_SIZE) else -1

                if (magic != MAGIC_NUMBER || version != CURRENT_VERSION || cap != FORENSIC_SPILL_CAPACITY || entrySz != FORENSIC_SPILL_ENTRY_SIZE) {
                    Timber.w("ForensicSpillBuffer: Header mismatch (V$version, S=$entrySz) or new file. Resetting to V$CURRENT_VERSION, S=$FORENSIC_SPILL_ENTRY_SIZE.")
                    resetBuffer()
                } else {
                    val recoveredWrite = buffer.getInt(OFF_WRITE_IDX)
                    val recoveredCount = buffer.getInt(OFF_COUNT)
                    val recoveredRead = buffer.getInt(OFF_READ_IDX)
                    
                    if (recoveredWrite in 0 until FORENSIC_SPILL_CAPACITY && 
                        recoveredCount in 0..FORENSIC_SPILL_CAPACITY &&
                        recoveredRead in 0 until FORENSIC_SPILL_CAPACITY) {
                        writeIdx.set(recoveredWrite)
                        totalCount.set(recoveredCount)
                        readIdx.set(recoveredRead)
                        
                        baseTs.set(buffer.getLong(OFF_BASE_TS))
                        baseLat = buffer.getDouble(OFF_BASE_LAT)
                        baseLng = buffer.getDouble(OFF_BASE_LNG)
                        
                        Timber.i("ForensicSpillBuffer initialized. Recovered: $recoveredCount entries.")
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
        buffer.putInt(OFF_MAGIC, MAGIC_NUMBER)
        buffer.putInt(OFF_VERSION, CURRENT_VERSION)
        buffer.putInt(OFF_CAPACITY, FORENSIC_SPILL_CAPACITY)
        buffer.putInt(OFF_ENTRY_SIZE, FORENSIC_SPILL_ENTRY_SIZE)
        buffer.putLong(OFF_LAST_WRITE_RT, 0L)
        
        buffer.putInt(OFF_WRITE_IDX, 0)
        buffer.putInt(OFF_COUNT, 0)
        buffer.putInt(OFF_READ_IDX, 0)
        
        val now = System.currentTimeMillis()
        buffer.putLong(OFF_BASE_TS, now)
        buffer.putDouble(OFF_BASE_LAT, 0.0)
        buffer.putDouble(OFF_BASE_LNG, 0.0)
        
        writeIdx.set(0)
        totalCount.set(0)
        readIdx.set(0)
        baseTs.set(now)
        baseLat = 0.0
        baseLng = 0.0
    }

    private fun updateBasesIfNeeded(ts: Long, lat: Double, lng: Double) {
        val buffer = mappedBuffer ?: return
        if (baseTs.get() == 0L || baseLat == 0.0) {
            baseTs.set(ts)
            baseLat = lat
            baseLng = lng
            buffer.putLong(OFF_BASE_TS, ts)
            buffer.putDouble(OFF_BASE_LAT, lat)
            buffer.putDouble(OFF_BASE_LNG, lng)
        }
    }

    /**
     * writeTrace: Writes a trace to the buffer.
     * Returns true if successful, false if buffer is full (Overflow).
     */
    fun writeTrace(entry: LogEntry): Boolean {
        return LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false
            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false
                
                updateBasesIfNeeded(entry.timestamp, entry.lat, entry.lng)
                
                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                
                buffer.position(offset)
                
                // Delta Encoding (Issue #706)
                buffer.putInt((entry.timestamp - baseTs.get()).toInt())
                buffer.putInt(((entry.lat - baseLat) * PRECISION_SCALE).toInt())
                buffer.putInt(((entry.lng - baseLng) * PRECISION_SCALE).toInt())
                
                // Telemetry Fields (Floats)
                buffer.putFloat(entry.accuracy.toFloat())
                buffer.putFloat(entry.maxAccuracy.toFloat())
                buffer.putFloat(entry.vibeSnapshot?.toFloat() ?: -1.0f)
                buffer.putFloat(entry.snrSnapshot?.toFloat() ?: -1.0f)
                
                buffer.putFloat(0.0f) // batteryTemp (Reserved for optimized)
                
                // Bit-Packed Metadata (Issue #743 / #125)
                var flags = 0
                if (entry.isImportant) flags = flags or 0x01
                if (entry.isSpecial) flags = flags or 0x02
                if (entry.gpsHardwareLock) flags = flags or 0x08
                
                buffer.put(flags.toByte())
                buffer.put(0.toByte()) // batteryLevel
                
                val rawBytes = entry.message.toByteArray(Charsets.UTF_8)
                val maxMsgLen = FORENSIC_SPILL_ENTRY_SIZE - 36 - CHECKSUM_SIZE // 96 - 36 - 4 = 56
                
                var msgLen = rawBytes.size.coerceAtMost(maxMsgLen)
                
                // Safe UTF-8 Truncation (Issue #126): Ensure we don't split a multi-byte sequence.
                // In UTF-8, continuation bytes start with '10' in binary (0x80 to 0xBF).
                if (msgLen < rawBytes.size) {
                    while (msgLen > 0 && (rawBytes[msgLen].toInt() and 0xC0) == 0x80) {
                        msgLen--
                    }
                }

                buffer.put(msgLen.toByte())
                buffer.put(0.toByte()) // Alignment padding

                buffer.put(rawBytes, 0, msgLen)

                val crc = calculateEntryChecksumLocked(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

                advanceWritePointer()
                true
            }
        }
    }

    /**
     * writeTraceOptimized: Zero-churn binary serialization path.
     * Returns true if successful, false if buffer is full (Overflow).
     */
    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryLevel: Int, isCharging: Boolean, batteryTemp: Double,
        gpsHardwareLock: Boolean = false
    ): Boolean {
        return LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write Optimized",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false
            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false
                
                updateBasesIfNeeded(timestamp, lat, lng)
                
                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                
                buffer.position(offset)
                
                // Delta Encoding (Issue #706)
                buffer.putInt((timestamp - baseTs.get()).toInt())
                buffer.putInt(((lat - baseLat) * PRECISION_SCALE).toInt())
                buffer.putInt(((lng - baseLng) * PRECISION_SCALE).toInt())
                
                buffer.putFloat(accuracy.toFloat())
                buffer.putFloat(maxAccuracy.toFloat())
                buffer.putFloat(vibe.toFloat())
                buffer.putFloat(snr.toFloat())
                
                buffer.putFloat(batteryTemp.toFloat())

                // Bit-Packed Metadata (Issue #743 / #125)
                var flags = 0
                if (isCharging) flags = flags or 0x04
                if (gpsHardwareLock) flags = flags or 0x08
                
                buffer.put(flags.toByte())
                buffer.put(batteryLevel.toByte())
                buffer.put(0.toByte()) // msgLen = 0
                buffer.put(0.toByte()) // Alignment padding

                val crc = calculateEntryChecksumLocked(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                buffer.putInt(offset + FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, crc.toInt())

                advanceWritePointer()
                true
            }
        }
    }

    private fun advanceWritePointer() {
        val buffer = mappedBuffer ?: return
        val currentWrite = writeIdx.get()
        val nextWrite = (currentWrite + 1) % FORENSIC_SPILL_CAPACITY
        writeIdx.set(nextWrite)
        buffer.putInt(OFF_WRITE_IDX, nextWrite)
        
        val newCount = totalCount.incrementAndGet()
        buffer.putInt(OFF_COUNT, newCount)
        
        // Issue #717: Update last write timestamp in header
        buffer.putLong(OFF_LAST_WRITE_RT, timeProvider.elapsedRealtime())
    }

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
                val calculatedCrc = calculateEntryChecksumLocked(buffer, offset, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE).toInt()
                
                if (storedCrc == calculatedCrc) {
                    buffer.position(offset)
                    
                    // Reconstruct from deltas (Issue #706)
                    val ts = baseTs.get() + buffer.getInt()
                    val lat = baseLat + (buffer.getInt() / PRECISION_SCALE)
                    val lng = baseLng + (buffer.getInt() / PRECISION_SCALE)
                    
                    val acc = buffer.getFloat().toDouble()
                    val maxAcc = buffer.getFloat().toDouble()
                    val vibe = buffer.getFloat().toDouble()
                    val snr = buffer.getFloat().toDouble()
                    val batTemp = buffer.getFloat().toDouble()
                    
                    val flags = buffer.get().toInt()
                    val batLevel = buffer.get().toInt() and 0xFF
                    val msgLen = buffer.get().toInt() and 0xFF
                    buffer.get() // Skip alignment padding
                    
                    val important = (flags and 0x01) != 0
                    val special = (flags and 0x02) != 0
                    val charging = (flags and 0x04) != 0
                    val hwLock = (flags and 0x08) != 0

                    val msg = if (msgLen > 0) {
                        val msgBytes = ByteArray(msgLen)
                        buffer.get(msgBytes)
                        String(msgBytes, Charsets.UTF_8)
                    } else {
                        "F_TRACE: P=$batLevel% C=$charging L=$hwLock T=${batTemp.roundToOneDecimal()}°C"
                    }
                    
                    result.add(LogEntry(
                        timestamp = ts, lat = lat, lng = lng, accuracy = acc, maxAccuracy = maxAcc,
                        vibeSnapshot = if (vibe == -1.0) null else vibe,
                        snrSnapshot = if (snr == -1.0) null else snr,
                        isImportant = important, isSpecial = special,
                        message = msg, type = "FORENSIC_TRACE",
                        id = "SYSTEM", role = "tracker",
                        spillIdx = currentRead,
                        gpsHardwareLock = hwLock
                    ))
                }
                currentRead = (currentRead + 1) % FORENSIC_SPILL_CAPACITY
            }
        }
        return result
    }

    fun commitDrain(count: Int) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val actualToConsume = count.coerceAtMost(totalCount.get())
            if (actualToConsume <= 0) return

            val newRead = (readIdx.get() + actualToConsume) % FORENSIC_SPILL_CAPACITY
            readIdx.set(newRead)
            buffer.putInt(OFF_READ_IDX, newRead)

            val newCount = totalCount.get() - actualToConsume
            totalCount.set(newCount)
            buffer.putInt(OFF_COUNT, newCount)
            
            // Issue #725: Adaptive Base Resetting
            if (newCount == 0) {
                baseTs.set(0L)
                baseLat = 0.0
                baseLng = 0.0
                buffer.putLong(OFF_BASE_TS, 0L)
                buffer.putDouble(OFF_BASE_LAT, 0.0)
                buffer.putDouble(OFF_BASE_LNG, 0.0)
            }
        }
    }

    /**
     * calculateEntryChecksumLocked: Zero-allocation checksum calculation.
     * MUST be called within synchronized(this) block.
     */
    private fun calculateEntryChecksumLocked(buffer: ByteBuffer, offset: Int, length: Int): Long {
        val crc = CRC32()
        val originalPos = buffer.position()
        try {
            buffer.position(offset)
            buffer.get(checksumBuffer, 0, length)
            crc.update(checksumBuffer, 0, length)
        } finally {
            buffer.position(originalPos)
        }
        return crc.value
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun hasPending(): Boolean = totalCount.get() > 0

    fun getPendingCount(): Int = totalCount.get()
    
    fun isFull(): Boolean = totalCount.get() >= FORENSIC_SPILL_CAPACITY
}
