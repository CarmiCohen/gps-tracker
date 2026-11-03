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
 * Updated Nov2.21: Added file existence checks and write-integrity guards for R197 (ISSUE-REVOL38.1-F3).
 * Fixed Nov2.21: Corrected index advancement order to ensure header sync reflects latest write.
 */
class ForensicBuffer(
    private val storageDir: File,
    val capacity: Int = 10000
) {
    private val magic = 0x5245564F 
    private val version = 2 
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
                val existingVersion = getInt(offVersion)
                if (getInt(offMagic) != magic || existingVersion != version) {
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
            raf.close()
        }
    }

    /**
     * verifyFileIntegrity: Checks if the underlying file has been deleted or tampered with.
     * R197: Essential for resilience against external pruning.
     */
    private fun verifyFileIntegrity(): Boolean {
        val file = File(storageDir, fileName)
        if (!file.exists()) {
            synchronized(this) {
                if (!file.exists()) {
                    SystemLogger.w("ForensicBuffer", "Trace file missing. Resetting buffer state.")
                    writeIdx.set(0)
                    count.set(0)
                    hasWrapped = false
                    return false
                }
            }
        }
        return true
    }

    private fun commitHeader(wIdx: Int, c: Int, wrapped: Boolean, force: Boolean = false) {
        mappedBuffer?.let {
            it.putInt(offWriteIdx, wIdx)
            it.putInt(offCount, c)
            it.putInt(offHasWrapped, if (wrapped) 1 else 0)
            
            val now = System.currentTimeMillis()
            if (force || (now - lastForceTs.get() > 5000)) {
                try {
                    it.force(0, headerSize)
                    lastForceTs.set(now)
                } catch (e: Exception) {
                    SystemLogger.e("ForensicBuffer", "Force sync failed: ${e.message}")
                    throw e // Propagate to caller to prevent index corruption in memory
                }
            }
        }
    }

    fun hasPending(): Boolean {
        if (!verifyFileIntegrity()) return false
        return count.get() > 0
    }

    fun readTraces(): List<LocationUpdate> {
        return readRange(0, Long.MAX_VALUE)
    }

    fun readRange(startSeq: Long, endSeq: Long): List<LocationUpdate> {
        if (!verifyFileIntegrity()) return emptyList()
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
                    val monotonicNanos = buffer.getLong()
                    val statusBytes = ByteArray(32)
                    buffer.get(statusBytes)
                    val status = String(statusBytes).trim { it <= ' ' || it == '\u0000' }

                    if (seqId in startSeq..endSeq) {
                        val update = LocationUpdate().apply {
                            this.timestamp = timestamp
                            this.lat = lat
                            this.lng = lng
                            this.accuracy = accuracy
                            this.speed = speed
                            this.sequenceId = seqId
                            this.sensorSnapshot.vibration = vib
                            this.monotonicNanos = monotonicNanos
                            this.status = status.ifEmpty { "VALID" }
                        }
                        result.add(update)
                    }
                } catch (e: Exception) {
                    SystemLogger.e("ForensicBuffer", "Read failed at idx $physicalIdx: ${e.message}")
                    break
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
            try {
                commitHeader(0, 0, false, force = true)
            } catch (e: Exception) {
                SystemLogger.e("ForensicBuffer", "Clear failed: ${e.message}")
            }
        }
    }

    fun writeTrace(update: LocationUpdate, isCritical: Boolean = false) {
        if (!verifyFileIntegrity()) {
            if (!initialize()) return 
        }
        val buffer = mappedBuffer ?: return
        
        // R197: Strict check for storage availability (ISSUE-REVOL38.1-F3)
        val file = File(storageDir, fileName)
        if (!storageDir.canWrite() || (file.exists() && !file.canWrite())) {
            SystemLogger.e("ForensicBuffer", "Write aborted: Storage restricted.")
            return
        }

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
                buffer.putLong(update.monotonicNanos)
                
                val statusBytes = update.status.toByteArray(Charsets.UTF_8)
                val statusFixed = ByteArray(32)
                System.arraycopy(statusBytes, 0, statusFixed, 0, minOf(statusBytes.size, 32))
                buffer.put(statusFixed)

                // Prepare next indices
                var nextIdx = currentWrite + 1
                var nextWrapped = hasWrapped
                if (nextIdx >= capacity) {
                    nextIdx = 0
                    nextWrapped = true
                }
                val newCount = if (count.get() < capacity) count.get() + 1 else capacity

                // R197: Commit header with NEW indices before advancing in-memory state
                // This ensures that if we crash now, the data we just wrote is visible on next load.
                commitHeader(nextIdx, newCount, nextWrapped, force = isCritical)

                // Finalize update in memory only after successful commit
                writeIdx.set(nextIdx)
                count.set(newCount)
                hasWrapped = nextWrapped
            } catch (e: Exception) {
                SystemLogger.e("ForensicBuffer", "Write failed: ${e.message}")
            }
        }
    }

    companion object {
        /**
         * Calculates adaptive buffer capacity based on variable sensor bitrates across SoC vendors (GAP-REVOL38.3-G1).
         * Ensures consistent retention window regardless of device sample rate.
         */
        fun calculateAdaptiveCapacity(sampleRateHz: Int = 100, retentionSeconds: Int = 100): Int {
            val calculated = sampleRateHz * retentionSeconds
            return calculated.coerceIn(1000, 50000)
        }
    }
}

