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
 * Aug.09.22:
 * - Issue #127: Forensic Drain Latency Hardening. Refactored lock critical sections 
 *   to sub-millisecond durations. Moved UTF-8 truncation, CRC calculation, and 
 *   object reconstruction outside synchronized blocks. Eliminated shared checksum 
 *   buffers to prevent lock inversion with LatencyMonitor pulses (R127).
 * Aug.08.21:
 * - Issue #126: Forensic Payload Overflow Audit. Implemented safe UTF-8 truncation (R126).
 * - Issue #125: Forensic Audit: Compression Parity Audit. Integrated gpsHardwareLock (R125).
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
    
    private val baseTs = AtomicLong(0L)
    private var baseLat: Double = 0.0
    private var baseLng: Double = 0.0

    private companion object {
        const val MAGIC_NUMBER = 0x46535042
        const val CURRENT_VERSION = 2
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
        const val OFF_BASE_TS = 36
        const val OFF_BASE_LAT = 44
        const val OFF_BASE_LNG = 52

        const val PRECISION_SCALE = 10_000_000.0
        const val DRAIN_STALL_THRESHOLD_MS = 5L
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
                    } else {
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

    fun writeTrace(entry: LogEntry): Boolean {
        return LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false

            val rawBytes = entry.message.toByteArray(Charsets.UTF_8)
            val maxMsgLen = FORENSIC_SPILL_ENTRY_SIZE - 36 - CHECKSUM_SIZE
            var msgLen = rawBytes.size.coerceAtMost(maxMsgLen)
            
            if (msgLen < rawBytes.size) {
                while (msgLen > 0 && (rawBytes[msgLen].toInt() and 0xC0) == 0x80) {
                    msgLen--
                }
            }

            val entryData = ByteBuffer.allocate(FORENSIC_SPILL_ENTRY_SIZE).order(ByteOrder.nativeOrder())
            
            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false
                if (baseTs.get() == 0L || baseLat == 0.0) {
                    baseTs.set(entry.timestamp)
                    baseLat = entry.lat
                    baseLng = entry.lng
                    buffer.putLong(OFF_BASE_TS, entry.timestamp)
                    buffer.putDouble(OFF_BASE_LAT, entry.lat)
                    buffer.putDouble(OFF_BASE_LNG, entry.lng)
                }

                entryData.putInt((entry.timestamp - baseTs.get()).toInt())
                entryData.putInt(((entry.lat - baseLat) * PRECISION_SCALE).toInt())
                entryData.putInt(((entry.lng - baseLng) * PRECISION_SCALE).toInt())
                
                entryData.putFloat(entry.accuracy.toFloat())
                entryData.putFloat(entry.maxAccuracy.toFloat())
                entryData.putFloat(entry.vibeSnapshot?.toFloat() ?: -1.0f)
                entryData.putFloat(entry.snrSnapshot?.toFloat() ?: -1.0f)
                entryData.putFloat(0.0f)

                var flags = 0
                if (entry.isImportant) flags = flags or 0x01
                if (entry.isSpecial) flags = flags or 0x02
                if (entry.gpsHardwareLock) flags = flags or 0x08
                entryData.put(flags.toByte())
                entryData.put(0.toByte())
                entryData.put(msgLen.toByte())
                entryData.put(0.toByte())
                entryData.put(rawBytes, 0, msgLen)
                
                entryData.position(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)

                val crc = CRC32()
                crc.update(entryData.array(), 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                entryData.putInt(crc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                
                buffer.position(offset)
                buffer.put(entryData.array())

                advanceWritePointer(buffer)
                true
            }
        }
    }

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
            val entryData = ByteBuffer.allocate(FORENSIC_SPILL_ENTRY_SIZE).order(ByteOrder.nativeOrder())
            
            synchronized(this) {
                if (totalCount.get() >= FORENSIC_SPILL_CAPACITY) return@synchronized false
                
                if (baseTs.get() == 0L || baseLat == 0.0) {
                    baseTs.set(timestamp)
                    baseLat = lat
                    baseLng = lng
                    buffer.putLong(OFF_BASE_TS, timestamp)
                    buffer.putDouble(OFF_BASE_LAT, lat)
                    buffer.putDouble(OFF_BASE_LNG, lng)
                }
                
                entryData.putInt((timestamp - baseTs.get()).toInt())
                entryData.putInt(((lat - baseLat) * PRECISION_SCALE).toInt())
                entryData.putInt(((lng - baseLng) * PRECISION_SCALE).toInt())
                entryData.putFloat(accuracy.toFloat())
                entryData.putFloat(maxAccuracy.toFloat())
                entryData.putFloat(vibe.toFloat())
                entryData.putFloat(snr.toFloat())
                entryData.putFloat(batteryTemp.toFloat())

                var flags = 0
                if (isCharging) flags = flags or 0x04
                if (gpsHardwareLock) flags = flags or 0x08
                
                entryData.put(flags.toByte())
                entryData.put(batteryLevel.toByte())
                entryData.put(0.toByte())
                entryData.put(0.toByte())

                entryData.position(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                val crc = CRC32()
                crc.update(entryData.array(), 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                entryData.putInt(crc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE)
                buffer.position(offset)
                buffer.put(entryData.array())

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

    fun peek(limit: Int): List<LogEntry> {
        return LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = DRAIN_STALL_THRESHOLD_MS,
            operation = "Forensic Peek",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit emptyList()
            val rawEntries = mutableListOf<Pair<Int, ByteArray>>()
            var currentBaseTs = 0L
            var currentBaseLat = 0.0
            var currentBaseLng = 0.0

            synchronized(this) {
                val count = totalCount.get()
                if (count == 0) return@synchronized
                
                val toPeek = count.coerceAtMost(limit)
                var currentRead = readIdx.get()
                currentBaseTs = baseTs.get()
                currentBaseLat = baseLat
                currentBaseLng = baseLng
                
                repeat(toPeek) {
                    val offset = HEADER_SIZE + (currentRead * FORENSIC_SPILL_ENTRY_SIZE)
                    val entryBytes = ByteArray(FORENSIC_SPILL_ENTRY_SIZE)
                    buffer.position(offset)
                    buffer.get(entryBytes)
                    rawEntries.add(currentRead to entryBytes)
                    currentRead = (currentRead + 1) % FORENSIC_SPILL_CAPACITY
                }
            }
            
            if (rawEntries.isEmpty()) return@measureAndAudit emptyList()

            rawEntries.map { (slotIdx, bytes) ->
                val bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
                val storedCrc = bb.getInt(FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                val crc = CRC32()
                crc.update(bytes, 0, FORENSIC_SPILL_ENTRY_SIZE - CHECKSUM_SIZE)
                
                if (storedCrc != crc.value.toInt()) return@map null

                val ts = currentBaseTs + bb.getInt()
                val lat = currentBaseLat + (bb.getInt() / PRECISION_SCALE)
                val lng = currentBaseLng + (bb.getInt() / PRECISION_SCALE)
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
                    val msgBytes = ByteArray(msgLen)
                    bb.get(msgBytes)
                    String(msgBytes, Charsets.UTF_8)
                } else {
                    "F_TRACE: P=$batLevel% C=${(flags and 0x04) != 0} L=${(flags and 0x08) != 0} T=${(round(batTemp * 10) / 10)}°C"
                }

                LogEntry(
                    timestamp = ts, lat = lat, lng = lng, accuracy = acc, maxAccuracy = maxAcc,
                    vibeSnapshot = if (vibe == -1.0) null else vibe,
                    snrSnapshot = if (snr == -1.0) null else snr,
                    isImportant = (flags and 0x01) != 0, isSpecial = (flags and 0x02) != 0,
                    message = msg, type = "FORENSIC_TRACE", id = "SYSTEM", role = "tracker",
                    spillIdx = slotIdx, gpsHardwareLock = (flags and 0x08) != 0
                )
            }.filterNotNull()
        }
    }

    fun commitDrain(count: Int) {
        LatencyMonitor.measureAndAudit(
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
    }

    fun hasPending(): Boolean = totalCount.get() > 0
    fun getPendingCount(): Int = totalCount.get()
    fun isFull(): Boolean = totalCount.get() >= FORENSIC_SPILL_CAPACITY
}
