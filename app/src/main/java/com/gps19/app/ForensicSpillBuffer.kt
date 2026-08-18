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
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for telemetry traces.
 * Aug.18.07:
 * - Issue #203: Forensic Multi-Session Alignment Audit. Switched to absolute 
 *   Long timestamps and Double coordinates in the buffer to ensure zero-jitter 
 *   continuity across reboots and service restarts. Incremented to v3 (R203).
 *   Refined commitDrain persistence order for idempotent recovery safety.
 * Aug.18.06:
 * - Issue #202: Forensic Performance. Added peekToEntities() and removed 
 *   obsolete peek() to eliminate intermediate LogEntry allocations (R202).
 */
@Singleton
class ForensicSpillBuffer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {

    private val spillFile = File(context.filesDir, FORENSIC_SPILL_FILE_NAME)
    private var mappedBuffer: MappedByteBuffer? = null
    
    private val writeIdx = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)
    private val readIdx = AtomicInteger(0)
    
    private val entryWriteBuffer = ByteBuffer.allocate(FORENSIC_SPILL_ENTRY_SIZE).order(ByteOrder.nativeOrder())
    private val writeCrc = CRC32()

    private companion object {
        const val MAGIC_NUMBER = 0x46535042
        const val CURRENT_VERSION = 3 // R203: Switched to absolute storage
        const val HEADER_SIZE = 128
        const val CHECKSUM_SIZE = 4
        
        const val OFF_MAGIC = 0
        const val OFF_VERSION = 4
        const val OFF_CAPACITY = 8
        const val OFF_ENTRY_SIZE = 12
        const val OFF_LAST_WRITE_RT = 16
        const val OFF_WRITE_IDX = 24
        const val OFF_COUNT = 28
        const val OFF_READ_IDX = 32
        // Base values kept in header for legacy/meta but not used for entry calculation in v3
        const val OFF_BASE_TS = 36
        const val OFF_BASE_LAT = 44
        const val OFF_BASE_LNG = 52

        const val DRAIN_STALL_THRESHOLD_MS = 5L
        const val WRITE_STALL_THRESHOLD_MS = 5L
        
        const val HIGH_PRESSURE_THRESHOLD = 0.8 // 80% fill level
    }

    init {
        try {
            val exists = spillFile.exists()
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
                    resetBuffer()
                    if (exists) Timber.w("Forensic Persistence Audit: Spill-buffer signature mismatch or version change. Resetting.")
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
                        
                        if (recoveredCount > 0) {
                            Timber.i("Forensic Persistence Audit: Successfully restored $recoveredCount traces from spill-buffer v$CURRENT_VERSION.")
                        }
                    } else {
                        resetBuffer()
                        Timber.w("Forensic Persistence Audit: Spill-buffer indices out of bounds. Resetting.")
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
    }

    fun writeTrace(entry: LogEntry): Boolean {
        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false

            val rawBytes = entry.message.toByteArray(Charsets.UTF_8)
            // R203: Entry layout changed. TS (8) + Lat (8) + Lng (8) + etc (24) = 48 bytes before msg.
            val maxMsgLen = FORENSIC_SPILL_ENTRY_SIZE - 48 - CHECKSUM_SIZE
            var msgLen = rawBytes.size.coerceAtMost(maxMsgLen)
            
            if (msgLen < rawBytes.size) {
                while (msgLen > 0 && (rawBytes[msgLen].toInt() and 0xC0) == 0x80) {
                    msgLen--
                }
            }

            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false

                entryWriteBuffer.clear()
                Arrays.fill(entryWriteBuffer.array(), 48, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, 0.toByte())

                // R203: Absolute values for cross-session continuity
                entryWriteBuffer.putLong(entry.timestamp)
                entryWriteBuffer.putDouble(entry.lat)
                entryWriteBuffer.putDouble(entry.lng)
                
                entryWriteBuffer.putFloat(entry.accuracy.toFloat())
                entryWriteBuffer.putFloat(entry.maxAccuracy.toFloat())
                entryWriteBuffer.putFloat(entry.vibeSnapshot?.toFloat() ?: -1.0f)
                entryWriteBuffer.putFloat(entry.snrSnapshot?.toFloat() ?: -1.0f)
                entryWriteBuffer.putFloat(entry.tempSnapshot?.toFloat() ?: 0.0f)

                var flags = 0
                if (entry.isImportant) flags = flags or 0x01
                if (entry.isSpecial) flags = flags or 0x02
                if (entry.chargingSnapshot == true) flags = flags or 0x04
                if (entry.gpsHardwareLock) flags = flags or 0x08
                
                entryWriteBuffer.put(flags.toByte())
                entryWriteBuffer.put(entry.battSnapshot?.toByte() ?: 0.toByte())
                entryWriteBuffer.put(msgLen.toByte())
                entryWriteBuffer.put(0.toByte()) // Alignment
                entryWriteBuffer.put(rawBytes, 0, msgLen)
                
                entryWriteBuffer.position(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)

                writeCrc.reset()
                writeCrc.update(entryWriteBuffer.array(), 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                entryWriteBuffer.putInt(writeCrc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                
                buffer.position(offset)
                buffer.put(entryWriteBuffer.array())

                advanceWritePointer(buffer)
                true
            }
        }
    }

    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryTemp: Double, batteryLevel: Int, isCharging: Boolean,
        gpsHardwareLock: Boolean = false
    ): Boolean {
        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write Optimized",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false
            
            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false
                
                entryWriteBuffer.clear()
                Arrays.fill(entryWriteBuffer.array(), 48, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE, 0.toByte())

                // R203: Absolute values
                entryWriteBuffer.putLong(timestamp)
                entryWriteBuffer.putDouble(lat)
                entryWriteBuffer.putDouble(lng)
                
                entryWriteBuffer.putFloat(accuracy.toFloat())
                entryWriteBuffer.putFloat(maxAccuracy.toFloat())
                entryWriteBuffer.putFloat(vibe.toFloat())
                entryWriteBuffer.putFloat(snr.toFloat())
                entryWriteBuffer.putFloat(batteryTemp.toFloat())

                var flags = 0
                if (isCharging) flags = flags or 0x04
                if (gpsHardwareLock) flags = flags or 0x08
                
                entryWriteBuffer.put(flags.toByte())
                entryWriteBuffer.put(batteryLevel.toByte())
                entryWriteBuffer.put(0.toByte())
                entryWriteBuffer.put(0.toByte())

                entryWriteBuffer.position(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                writeCrc.reset()
                writeCrc.update(entryWriteBuffer.array(), 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                entryWriteBuffer.putInt(writeCrc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                buffer.position(offset)
                buffer.put(entryWriteBuffer.array())

                advanceWritePointer(buffer)
                true
            }
        }
    }

    private fun advanceWritePointer(buffer: MappedByteBuffer) {
        val currentWrite = writeIdx.get()
        val nextWrite = (currentWrite + 1) % FORENSIC_SPILL_CAPACITY
        writeIdx.set(nextWrite)
        buffer.putInt(OFF_WRITE_IDX, nextWrite)
        val newCount = totalCount.incrementAndGet()
        buffer.putInt(OFF_COUNT, newCount)
        buffer.putLong(OFF_LAST_WRITE_RT, timeProvider.elapsedRealtime())
    }

    /**
     * peekToEntities: Direct buffer to LogEntity conversion.
     * Issue #202: Zero-churn entity generation.
     * R203: Reconstructs entities using absolute Long/Double values.
     */
    fun peekToEntities(limit: Int): List<LogEntity> {
        return LatencyMonitor.measureAndAudit<List<LogEntity>>(
            timeProvider = timeProvider,
            thresholdMs = DRAIN_STALL_THRESHOLD_MS,
            operation = "Forensic Peek Entities",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val mainBuffer = mappedBuffer ?: return@measureAndAudit emptyList()
            val readBuffer = mainBuffer.duplicate().order(ByteOrder.nativeOrder())
            
            var toPeekCount = 0
            var currentReadIdx = 0

            synchronized(this) {
                val count = totalCount.get()
                if (count == 0) return@synchronized
                
                toPeekCount = count.coerceAtMost(limit)
                currentReadIdx = readIdx.get()
            }
            
            if (toPeekCount == 0) return@measureAndAudit emptyList()

            val results = ArrayList<LogEntity>(toPeekCount)
            val entryBytes = ByteArray(FORENSIC_SPILL_ENTRY_SIZE)
            val bb = ByteBuffer.wrap(entryBytes).order(ByteOrder.nativeOrder())
            val crc = CRC32()

            var tempReadIdx = currentReadIdx
            repeat(toPeekCount) {
                val offset = HEADER_SIZE + (tempReadIdx * FORENSIC_SPILL_ENTRY_SIZE)
                readBuffer.position(offset)
                readBuffer.get(entryBytes)
                
                bb.clear()
                crc.reset()
                crc.update(entryBytes, 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                
                val storedCrc = bb.getInt(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                if (storedCrc == crc.value.toInt()) {
                    // R203: Absolute values stored at v3
                    val ts = bb.getLong()
                    val lat = bb.getDouble()
                    val lng = bb.getDouble()

                    val acc = bb.getFloat().toDouble()
                    val maxAcc = bb.getFloat().toDouble()
                    val vibe = bb.getFloat().toDouble()
                    val snr = bb.getFloat().toDouble()
                    val batTemp = bb.getFloat().toDouble()
                    val flags = bb.get().toInt()
                    val batLevel = bb.get().toInt() and 0xFF
                    val msgLen = bb.get().toInt() and 0xFF
                    bb.get() // Alignment

                    val msg = if (msgLen > 0) {
                        String(entryBytes, bb.position(), msgLen, Charsets.UTF_8)
                    } else {
                        "FORENSIC_TRACE"
                    }

                    results.add(LogEntity(
                        localId = "F-$ts-$tempReadIdx",
                        timestamp = ts,
                        message = msg,
                        type = "FORENSIC_TRACE",
                        isImportant = (flags and 0x01) != 0,
                        deviceId = "SYSTEM",
                        viewerId = "",
                        isSpecial = (flags and 0x02) != 0,
                        role = "tracker",
                        lat = lat,
                        lng = lng,
                        accuracy = acc,
                        maxAccuracy = maxAcc,
                        snrSnapshot = if (snr == -1.0) null else snr,
                        vibeSnapshot = if (vibe == -1.0) null else vibe,
                        synced = false,
                        spillIdx = tempReadIdx,
                        gpsHardwareLock = (flags and 0x08) != 0,
                        tempSnapshot = batTemp,
                        battSnapshot = batLevel,
                        chargingSnapshot = (flags and 0x04) != 0
                    ))
                }
                tempReadIdx = (tempReadIdx + 1) % FORENSIC_SPILL_CAPACITY
            }
            results
        }
    }

    /**
     * commitDrain: Commits a processed chunk of traces.
     * R203: Swapped persistence order. We now advance the read pointer before 
     * decrementing the count. In case of crash, idempotency is guaranteed by 
     * signature-based deduplication in the repository.
     */
    fun commitDrain(count: Int) {
        LatencyMonitor.measureAndAudit<Unit>(
            timeProvider = timeProvider,
            thresholdMs = DRAIN_STALL_THRESHOLD_MS,
            operation = "Forensic Commit",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit
            synchronized(this) {
                val actualToConsume = count.coerceAtMost(totalCount.get())
                if (actualToConsume <= 0) return@synchronized

                val newRead = (readIdx.get() + actualToConsume) % FORENSIC_SPILL_CAPACITY
                readIdx.set(newRead)
                buffer.putInt(OFF_READ_IDX, newRead)

                val newCount = totalCount.get() - actualToConsume
                totalCount.set(newCount)
                buffer.putInt(OFF_COUNT, newCount)
            }
        }
    }

    fun hasPending(): Boolean = totalCount.get() > 0
    fun getPendingCount(): Int = totalCount.get()
    fun isFull(): Boolean = totalCount.get() >= FORENSIC_SPILL_CAPACITY
    
    fun getFillLevel(): Double = totalCount.get().toDouble() / FORENSIC_SPILL_CAPACITY
    fun isHighPressure(): Boolean = getFillLevel() >= HIGH_PRESSURE_THRESHOLD
}
