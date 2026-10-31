package com.jd.core.shared

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ForensicBuffer: Ultimate Production Version (Point 181).
 * Implements full bit-level persistence with Wrap-Around and Atomic Sync.
 * Hardened for Direct Boot recovery (R-ID 744, 748, 749).
 * Hardened for Revol31.1: Robust I/O and Disk Space checks (ISSUE-REVOL30.0-F8).
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

    /**
     * Initializes the buffer. Returns true if successful.
     */
    fun initialize(): Boolean {
        return try {
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                SystemLogger.e("ForensicBuffer", "Could not create storage directory: ${storageDir.absolutePath}")
                return false
            }
            setupMapping()
            true
        } catch (e: Exception) {
            SystemLogger.e("ForensicBuffer", "Initialization failed: ${e.message}. Attempting hard reset...")
            try {
                File(storageDir, fileName).delete()
                setupMapping()
                true
            } catch (e2: Exception) {
                SystemLogger.e("ForensicBuffer", "Hard reset failed: ${e2.message}")
                false
            }
        }
    }

    private fun setupMapping() {
        val totalSize = (capacity * entrySize).toLong() + headerSize
        val file = File(storageDir, fileName)

        // Hardening: Verify disk space before allocation (R-ID 748 extension)
        if (!file.exists() || file.length() < totalSize) {
            val available = storageDir.usableSpace
            if (available < totalSize) {
                throw IllegalStateException("Insufficient space for forensic buffer. Need $totalSize, have $available")
            }
        }

        val raf = RandomAccessFile(file, "rw")
        try {
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
        } finally {
            // Channel remains open for the mapped buffer, but we close the RAF
            raf.close()
        }
    }

    private fun commitHeader(wIdx: Int, c: Int, wrapped: Boolean, force: Boolean = false) {
        mappedBuffer?.let {
            it.putInt(offWriteIdx, wIdx)
            it.putInt(offCount, c)
            it.putInt(offHasWrapped, if (wrapped) 1 else 0)
            
            val now = System.currentTimeMillis()
            // Staggered force (5s) to mitigate SPI bus contention (R-ID 749)
            if (force || (now - lastForceTs.get() > 5000)) {
                try {
                    it.force(0, headerSize)
                    lastForceTs.set(now)
                } catch (e: Exception) {
                    SystemLogger.e("ForensicBuffer", "Force sync failed: ${e.message}")
                }
            }
        }
    }

    fun hasPending(): Boolean = count.get() > 0

    /**
     * Reads all traces currently stored in the buffer in chronological order.
     */
    fun readTraces(): List<LocationUpdate> {
        return readRange(0, Long.MAX_VALUE)
    }

    /**
     * Reads traces within a specific sequence ID range (inclusive).
     */
    fun readRange(startSeq: Long, endSeq: Long): List<LocationUpdate> {
        val buffer = mappedBuffer ?: return emptyList()
        val result = mutableListOf<LocationUpdate>()
        synchronized(this) {
            val c = count.get()
            val wIdx = writeIdx.get()
            val startIdx = if (hasWrapped) wIdx else 0
            
            for (i in 0 until c) {
                val physicalIdx = (startIdx + i) % capacity
                val offset = headerSize + (physicalIdx * entrySize)
                
                try {
                    buffer.position(offset)
                    val timestamp = buffer.getLong()
                    val lat = buffer.getDouble()
                    val lng = buffer.getDouble()
                    val accuracy = buffer.getFloat()
                    val speed = buffer.getFloat()
                    val seqId = buffer.getLong()
                    val vib = buffer.getFloat()

                    if (seqId in startSeq..endSeq) {
                        val update = LocationUpdate().apply {
                            this.timestamp = timestamp
                            this.lat = lat
                            this.lng = lng
                            this.accuracy = accuracy
                            this.speed = speed
                            this.sequenceId = seqId
                            this.sensorSnapshot.vibration = vib
                        }
                        result.add(update)
                    }
                } catch (e: Exception) {
                    SystemLogger.e("ForensicBuffer", "Read failed at idx $physicalIdx: ${e.message}")
                    break // Stop reading on corruption or access error
                }
            }
        }
        return result
    }

    fun clear() {
        synchronized(this) {
            writeIdx.set(0)
            count.set(0)
            hasWrapped = false
            commitHeader(0, 0, false, force = true)
        }
    }

    /**
     * Writes a trace record with atomic sync logic.
     */
    fun writeTrace(update: LocationUpdate, isCritical: Boolean = false) {
        val buffer = mappedBuffer ?: return
        synchronized(this) {
            val currentWrite = writeIdx.get()
            val offset = headerSize + (currentWrite * entrySize)
            
            try {
                buffer.position(offset)
                buffer.putLong(update.timestamp)
                buffer.putDouble(update.lat)
                buffer.putDouble(update.lng)
                buffer.putFloat(update.accuracy)
                buffer.putFloat(update.speed)
                buffer.putLong(update.sequenceId)
                buffer.putFloat(update.sensorSnapshot.vibration)
                
                if (isCritical) buffer.force(offset, entrySize)

                var next = currentWrite + 1
                if (next >= capacity) {
                    next = 0
                    hasWrapped = true
                }
                writeIdx.set(next)
                val newCount = if (count.get() < capacity) count.incrementAndGet() else capacity
                commitHeader(next, newCount, hasWrapped, force = isCritical)
            } catch (e: Exception) {
                SystemLogger.e("ForensicBuffer", "Write failed: ${e.message}")
            }
        }
    }
}
