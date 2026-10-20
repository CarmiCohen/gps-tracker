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
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForensicSpillBuffer: High-performance memory-mapped circular buffer for forensic traces.
 * Oct.20.21:
 * - ISSUE-OCT20.10-F1: Hardened against overflow with circular overwrite (R746).
 *   Ensures latest forensic data is preserved during extreme pressure.
 * Oct.14.18:
 * - Optimized commitDrain by moving MappedByteBuffer.force() outside the synchronized block.
 *   This eliminates lock contention spikes (14ms -> <1ms lock hold time) identified on A15.
 * Oct.13.7:
 * - Introduced PeekResult to accurately track processed entries vs valid entities.
 * - Added logging for CRC mismatches to diagnose under-count issues.
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

    data class PeekResult(
        val entities: List<LogEntity>,
        val processedCount: Int
    )
    
    private companion object {
        const val MAGIC_NUMBER = 0x46535042
        const val CURRENT_VERSION = 5 
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

        const val DRAIN_STALL_THRESHOLD_MS = 10L
        const val WRITE_STALL_THRESHOLD_MS = 10L
        
        const val HIGH_PRESSURE_THRESHOLD = 0.8 
        const val DEFAULT_TRACE_MSG = "FORENSIC_TRACE"
        
        const val DATA_FIELDS_SIZE = 56 
    }

    init {
        try {
            val size = (FORENSIC_SPILL_CAPACITY * FORENSIC_SPILL_ENTRY_SIZE_V5).toLong() + HEADER_SIZE
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

                if (magic != MAGIC_NUMBER || version != CURRENT_VERSION || cap != FORENSIC_SPILL_CAPACITY || entrySz != FORENSIC_SPILL_ENTRY_SIZE_V5) {
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
                    } else {
                        resetBuffer()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Forensic Audit: Buffer init failed")
        }
    }

    fun resetBufferForTest() {
        resetBuffer()
    }

    private fun resetBuffer() {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            buffer.putInt(OFF_MAGIC, MAGIC_NUMBER)
            buffer.putInt(OFF_VERSION, CURRENT_VERSION)
            buffer.putInt(OFF_CAPACITY, FORENSIC_SPILL_CAPACITY)
            buffer.putInt(OFF_ENTRY_SIZE, FORENSIC_SPILL_ENTRY_SIZE_V5)
            buffer.putLong(OFF_LAST_WRITE_RT, 0L)
            buffer.putInt(OFF_WRITE_IDX, 0)
            buffer.putInt(OFF_COUNT, 0)
            buffer.putInt(OFF_READ_IDX, 0)
            
            buffer.putLong(OFF_BASE_TS, timeProvider.currentTimeMillis)
            buffer.putDouble(OFF_BASE_LAT, 0.0)
            buffer.putDouble(OFF_BASE_LNG, 0.0)
            
            writeIdx.set(0)
            totalCount.set(0)
            readIdx.set(0)
            buffer.force()
        }
    }

    fun writeTrace(entry: LogEntry): Boolean {
        val rawBytes = entry.message.toByteArray(Charsets.UTF_8)
        val maxMsgLen = FORENSIC_SPILL_ENTRY_SIZE_V5 - DATA_FIELDS_SIZE - CHECKSUM_SIZE
        var msgLen = rawBytes.size.coerceAtMost(maxMsgLen)
        
        if (msgLen < rawBytes.size) {
            while (msgLen > 0 && (rawBytes[msgLen].toInt() and 0xC0) == 0x80) {
                msgLen--
            }
        }

        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false
            val localWriteBuffer = ByteBuffer.allocate(FORENSIC_SPILL_ENTRY_SIZE_V5).order(ByteOrder.nativeOrder())
            val localCrc = CRC32()

            synchronized(this) {
                val wasFull = totalCount.get() >= FORENSIC_SPILL_CAPACITY
                if (wasFull) {
                    forceCircularAdvance(buffer)
                }

                localWriteBuffer.putLong(entry.timestamp)
                localWriteBuffer.putDouble(entry.lat)
                localWriteBuffer.putDouble(entry.lng)
                
                localWriteBuffer.putFloat(entry.accuracy.toFloat())
                localWriteBuffer.putFloat(entry.maxAccuracy.toFloat())
                localWriteBuffer.putFloat(entry.vibeSnapshot?.toFloat() ?: -1.0f)
                localWriteBuffer.putFloat(entry.snrSnapshot?.toFloat() ?: -1.0f)
                localWriteBuffer.putFloat(entry.tempSnapshot?.toFloat() ?: 0.0f)
                
                localWriteBuffer.putFloat(entry.thermalSnapshot?.toFloat() ?: 0.0f)
                localWriteBuffer.putFloat(entry.heapSnapshot?.toFloat() ?: 0.0f)

                var flags = 0
                if (entry.isImportant) flags = flags or 0x01
                if (entry.isSpecial) flags = flags or 0x02
                if (entry.chargingSnapshot == true) flags = flags or 0x04
                if (entry.gpsHardwareLock) flags = flags or 0x08
                if (entry.coolingSnapshot == true) flags = flags or 0x10
                
                localWriteBuffer.put(flags.toByte())
                localWriteBuffer.put(entry.battSnapshot?.toByte() ?: 0.toByte())
                localWriteBuffer.put(msgLen.toByte())
                localWriteBuffer.put(0.toByte()) 
                
                if (msgLen > 0) {
                    localWriteBuffer.put(rawBytes, 0, msgLen)
                }
                
                localWriteBuffer.position(FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE)
                localCrc.reset()
                localCrc.update(localWriteBuffer.array(), 0, FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE)
                localWriteBuffer.putInt(localCrc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE_V5)
                
                buffer.position(offset)
                buffer.put(localWriteBuffer.array())
                
                advanceWritePointer(buffer)
                !wasFull
            }
        }
    }

    fun writeTraceOptimized(
        timestamp: Long, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double,
        vibe: Double, snr: Double, batteryTemp: Double, batteryLevel: Int, isCharging: Boolean,
        gpsHardwareLock: Boolean = false, thermalHeadroom: Double = 0.0, heapAllocatedMb: Double = 0.0,
        isCooling: Boolean = false
    ): Boolean {
        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider = timeProvider,
            thresholdMs = WRITE_STALL_THRESHOLD_MS,
            operation = "Forensic Write Optimized",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit false
            val localWriteBuffer = ByteBuffer.allocate(FORENSIC_SPILL_ENTRY_SIZE_V5).order(ByteOrder.nativeOrder())
            val localCrc = CRC32()
            
            synchronized(this) {
                val wasFull = totalCount.get() >= FORENSIC_SPILL_CAPACITY
                if (wasFull) {
                    forceCircularAdvance(buffer)
                }
                
                localWriteBuffer.putLong(timestamp)
                localWriteBuffer.putDouble(lat)
                localWriteBuffer.putDouble(lng)
                
                localWriteBuffer.putFloat(accuracy.toFloat())
                localWriteBuffer.putFloat(maxAccuracy.toFloat())
                localWriteBuffer.putFloat(vibe.toFloat())
                localWriteBuffer.putFloat(snr.toFloat())
                localWriteBuffer.putFloat(batteryTemp.toFloat())
                
                localWriteBuffer.putFloat(thermalHeadroom.toFloat())
                localWriteBuffer.putFloat(heapAllocatedMb.toFloat())

                var flags = 0
                if (isCharging) flags = flags or 0x04
                if (gpsHardwareLock) flags = flags or 0x08
                if (isCooling) flags = flags or 0x10
                
                localWriteBuffer.put(flags.toByte())
                localWriteBuffer.put(batteryLevel.toByte())
                localWriteBuffer.put(0.toByte()) // msgLen = 0
                localWriteBuffer.put(0.toByte())

                localWriteBuffer.position(FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE)
                localCrc.reset()
                localCrc.update(localWriteBuffer.array(), 0, FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE)
                localWriteBuffer.putInt(localCrc.value.toInt())

                val currentWrite = writeIdx.get()
                val offset = HEADER_SIZE + (currentWrite * FORENSIC_SPILL_ENTRY_SIZE_V5)
                buffer.position(offset)
                buffer.put(localWriteBuffer.array())

                advanceWritePointer(buffer)
                !wasFull
            }
        }
    }

    /**
     * forceCircularAdvance: Overwrites the oldest entry by moving the read pointer.
     * Part of R713 hardening to ensure latest traces are always preserved.
     */
    private fun forceCircularAdvance(buffer: MappedByteBuffer) {
        val currentRead = readIdx.get()
        val nextRead = (currentRead + 1) % FORENSIC_SPILL_CAPACITY
        readIdx.set(nextRead)
        buffer.putInt(OFF_READ_IDX, nextRead)
        totalCount.decrementAndGet()
        // Note: OFF_COUNT will be updated by the subsequent advanceWritePointer call.
    }

    private fun advanceWritePointer(buffer: MappedByteBuffer) {
        val currentWrite = writeIdx.get()
        val nextWrite = (currentWrite + 1) % FORENSIC_SPILL_CAPACITY
        writeIdx.set(nextWrite)
        buffer.putInt(OFF_WRITE_IDX, nextWrite)
        val newCount = totalCount.incrementAndGet()
        buffer.putInt(OFF_COUNT, newCount)
        buffer.putLong(OFF_LAST_WRITE_RT, timeProvider.elapsedRealtime)
    }

    fun peekToEntities(limit: Int): PeekResult {
        return LatencyMonitor.measureAndAudit<PeekResult>(
            timeProvider = timeProvider,
            thresholdMs = DRAIN_STALL_THRESHOLD_MS,
            operation = "Forensic Peek Entities",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val mainBuffer = mappedBuffer ?: return@measureAndAudit PeekResult(emptyList(), 0)
            val localEntryBytes = ByteArray(FORENSIC_SPILL_ENTRY_SIZE_V5)
            val localCrc = CRC32()
            
            synchronized(this) {
                val readBuffer = mainBuffer.duplicate().order(ByteOrder.nativeOrder())
                val count = totalCount.get()
                if (count == 0) return@synchronized PeekResult(emptyList(), 0)
                
                val toPeekCount = count.coerceAtMost(limit)
                val currentReadIdx = readIdx.get()
                val results = ArrayList<LogEntity>(toPeekCount)
                
                var tempReadIdx = currentReadIdx
                repeat(toPeekCount) {
                    val offset = HEADER_SIZE + (tempReadIdx * FORENSIC_SPILL_ENTRY_SIZE_V5)
                    readBuffer.position(offset)
                    readBuffer.get(localEntryBytes)
                    
                    val localWrapper = ByteBuffer.wrap(localEntryBytes).order(ByteOrder.nativeOrder())
                    localCrc.reset()
                    localCrc.update(localEntryBytes, 0, FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE)
                    
                    val storedCrcLong = localWrapper.getInt(FORENSIC_SPILL_ENTRY_SIZE_V5 - CHECKSUM_SIZE).toLong() and 0xFFFFFFFFL
                    
                    if (storedCrcLong == localCrc.value) {
                        val ts = localWrapper.getLong()
                        val lat = localWrapper.getDouble()
                        val lng = localWrapper.getDouble()

                        val acc = localWrapper.getFloat().toDouble()
                        val maxAcc = localWrapper.getFloat().toDouble()
                        val vibe = localWrapper.getFloat().toDouble()
                        val snr = localWrapper.getFloat().toDouble()
                        val batTemp = localWrapper.getFloat().toDouble()
                        
                        val thermal = localWrapper.getFloat().toDouble()
                        val heap = localWrapper.getFloat().toDouble()

                        val flags = localWrapper.get().toInt()
                        val batLevel = localWrapper.get().toInt() and 0xFF
                        val msgLen = localWrapper.get().toInt() and 0xFF
                        localWrapper.get() // read padding

                        val msg = if (msgLen > 0) {
                            String(localEntryBytes, localWrapper.position(), msgLen, Charsets.UTF_8)
                        } else {
                            DEFAULT_TRACE_MSG
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
                            spillIdx = tempReadIdx,
                            gpsHardwareLock = (flags and 0x08) != 0,
                            tempSnapshot = batTemp,
                            battSnapshot = batLevel,
                            chargingSnapshot = (flags and 0x04) != 0,
                            coolingSnapshot = (flags and 0x10) != 0,
                            thermalSnapshot = thermal,
                            heapSnapshot = heap,
                            checksum = storedCrcLong
                        ))
                    } else {
                        Timber.w("Forensic Audit: CRC mismatch at index $tempReadIdx. Stored: $storedCrcLong, Computed: ${localCrc.value}")
                    }
                    tempReadIdx = (tempReadIdx + 1) % FORENSIC_SPILL_CAPACITY
                }
                PeekResult(results, toPeekCount)
            }
        }
    }

    fun commitDrain(count: Int) {
        LatencyMonitor.measureAndAudit<Unit>(
            timeProvider = timeProvider,
            thresholdMs = DRAIN_STALL_THRESHOLD_MS,
            operation = "Forensic Commit",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = { msg, _ -> Timber.w(msg) }
        ) {
            val buffer = mappedBuffer ?: return@measureAndAudit
            var shouldForce = false
            synchronized(this) {
                val actualToConsume = count.coerceAtMost(totalCount.get())
                if (actualToConsume <= 0) return@synchronized

                val newRead = (readIdx.get() + actualToConsume) % FORENSIC_SPILL_CAPACITY
                readIdx.set(newRead)
                buffer.putInt(OFF_READ_IDX, newRead)

                val newCount = totalCount.get() - actualToConsume
                totalCount.set(newCount)
                buffer.putInt(OFF_COUNT, newCount)
                shouldForce = true
            }
            if (shouldForce) {
                // Issue #SIMP-OCT14.11-S1: Moved force() outside lock to reduce contention.
                buffer.force()
            }
        }
    }

    fun hasPending(): Boolean = totalCount.get() > 0
    fun getPendingCount(): Int = totalCount.get()
    fun isFull(): Boolean = totalCount.get() >= FORENSIC_SPILL_CAPACITY
    
    fun getFillLevel(): Double = totalCount.get().toDouble() / FORENSIC_SPILL_CAPACITY
    fun isHighPressure(): Boolean = getFillLevel() >= HIGH_PRESSURE_THRESHOLD
}
