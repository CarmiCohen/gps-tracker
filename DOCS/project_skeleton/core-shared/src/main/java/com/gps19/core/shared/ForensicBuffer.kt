package com.gps19.core.shared

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ForensicBuffer: Ultimate Production Version (Point 181).
 * Implements full bit-level persistence with Wrap-Around and Atomic Sync.
 */
class ForensicBuffer(
    private val storageDir: File,
    val capacity: Int = 10000
) {
    private val magic = 0x5245564F 
    private val version = 1
    private val entrySize = 128
    private val headerSize = 64
    
    private val offMagic = 0
    private val offVersion = 4
    private val offWriteIdx = 8
    private val offCount = 12
    private val offHasWrapped = 16

    private val fileName = "forensic_trace.bin"
    private var mappedBuffer: java.nio.MappedByteBuffer? = null
    
    private val writeIdx = AtomicInteger(0)
    private val count = AtomicInteger(0)
    private val lastForceTs = AtomicLong(0)
    private var hasWrapped = false

    fun initialize() {
        try {
            setupMapping()
        } catch (e: Exception) {
            File(storageDir, fileName).delete()
            setupMapping()
        }
    }

    private fun setupMapping() {
        val totalSize = (capacity * entrySize).toLong() + headerSize
        val raf = RandomAccessFile(File(storageDir, fileName), "rw")
        if (raf.length() != totalSize) raf.setLength(totalSize)
        
        mappedBuffer = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, totalSize).apply {
            order(ByteOrder.nativeOrder())
            if (getInt(offMagic) != magic) {
                putInt(offMagic, magic)
                putInt(offVersion, version)
                commitHeader(0, 0, false, force = true)
            } else {
                writeIdx.set(getInt(offWriteIdx))
                count.set(getInt(offCount))
                hasWrapped = getInt(offHasWrapped) == 1
            }
        }
    }

    private fun commitHeader(wIdx: Int, c: Int, wrapped: Boolean, force: Boolean = false) {
        mappedBuffer?.let {
            it.putInt(offWriteIdx, wIdx)
            it.putInt(offCount, c)
            it.putInt(offHasWrapped, if (wrapped) 1 else 0)
            
            val now = System.currentTimeMillis()
            if (force || (now - lastForceTs.get() > 5000)) {
                it.force(0, headerSize)
                lastForceTs.set(now)
            }
        }
    }

    /**
     * Corrected: Full Implementation of binary write logic.
     */
    fun writeTrace(update: LocationUpdate, isCritical: Boolean = false) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val currentWrite = writeIdx.get()
            val offset = headerSize + (currentWrite * entrySize)
            
            buffer.position(offset)
            buffer.putLong(update.timestamp)           // 0
            buffer.putDouble(update.lat)                // 8
            buffer.putDouble(update.lng)                // 16
            buffer.putFloat(update.accuracy)            // 24
            buffer.putFloat(update.speed)               // 28
            buffer.putLong(update.sequenceId)           // 32
            buffer.putFloat(update.sensorSnapshot.vibration) // 40
            
            if (isCritical) buffer.force(offset, entrySize)

            var next = currentWrite + 1
            if (next >= capacity) {
                next = 0
                hasWrapped = true
            }
            writeIdx.set(next)
            val newCount = if (count.get() < capacity) count.incrementAndGet() else capacity
            commitHeader(next, newCount, hasWrapped, force = isCritical)
        }
    }
}
