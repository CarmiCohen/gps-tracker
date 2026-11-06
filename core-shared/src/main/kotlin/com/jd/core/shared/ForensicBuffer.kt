package com.jd.core.shared

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32

/**
 * ForensicBuffer: Ultimate Production Version (Point 181).
 * Implements full bit-level persistence with Wrap-Around and Atomic Sync.
 * Hardened for Direct Boot recovery (R-ID 744, 748, 749).
 * Hardened for Revol31.1: Robust I/O and Disk Space checks (ISSUE-REVOL30.0-F8).
 * Updated Nov2.21: Added file existence checks and write-integrity guards for R197 (ISSUE-REVOL38.1-F3).
 * Fixed Nov2.21: Corrected index advancement order to ensure header sync reflects latest write.
 * Hardened Nov6.0: Each entry carries a CRC32, so a half-written entry or one lost to power failure
 * is skipped on read. The header reserves sequence numbers ahead of use (ISSUE-NOV6.0-O20).
 *
 * This module compiles against the JDK, not android.jar, and Gradle builds do not run lint on it.
 * Use only absolute get and put here: position() and the other Buffer calls that JDK 9+ redeclares do not
 * exist on older Android versions, and neither does force(int, int) (ISSUE-NOV6.0-O25).
 */
class ForensicBuffer(
    private val storageDir: File,
    val capacity: Int = 10000
) {
    private val magic = 0x5245564F 
    private val version = 3
    private val versionWithoutChecksums = 2
    private val entrySize = 128
    private val headerSize = 64
    
    private val offMagic = 0
    private val offVersion = 4
    private val offWriteIdx = 8
    private val offCount = 12
    private val offHasWrapped = 16
    private val offReservedSeq = 24

    // Entry layout. The CRC32 of the stored fields follows them, at entryDataSize.
    private val offTimestamp = 0
    private val offLat = 8
    private val offLng = 16
    private val offAccuracy = 24
    private val offSpeed = 28
    private val offSeq = 32
    private val offVibration = 40
    private val offMonotonic = 44
    private val offStatus = 52
    private val statusSize = 32
    private val entryDataSize = offStatus + statusSize

    /** Sequence numbers reserved per header sync; a restart can skip at most this many. */
    private val sequenceReserve = 100L

    private val fileName = "forensic_trace.bin"
    private var mappedBuffer: MappedByteBuffer? = null
    
    private val writeIdx = AtomicInteger(0)
    private val count = AtomicInteger(0)
    private val lastForceTs = AtomicLong(0)
    private var hasWrapped = false
    private var reservedSequence = 0L

    // Working copy of one entry, used only while holding the lock on this object.
    private val entryData = ByteArray(entryDataSize)
    private val entryView = ByteBuffer.wrap(entryData).order(ByteOrder.nativeOrder())
    private val crc = CRC32()

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
            
            val buffer = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, totalSize)
            buffer.order(ByteOrder.nativeOrder())
            mappedBuffer = buffer
            loadHeader(buffer)
        } finally {
            raf.close()
        }
    }

    private fun loadHeader(buffer: MappedByteBuffer) {
        synchronized(this) {
            val storedVersion = buffer.getInt(offVersion)
            if (buffer.getInt(offMagic) != magic || (storedVersion != version && storedVersion != versionWithoutChecksums)) {
                buffer.putInt(offMagic, magic)
                buffer.putInt(offVersion, version)
                clear()
                return
            }

            writeIdx.set(buffer.getInt(offWriteIdx))
            count.set(buffer.getInt(offCount))
            hasWrapped = buffer.getInt(offHasWrapped) == 1
            reservedSequence = buffer.getLong(offReservedSeq)

            if (storedVersion == versionWithoutChecksums) addChecksums(buffer)
            if (!isHeaderConsistent()) rebuildHeader(buffer)
        }
    }

    /**
     * Upgrades a file written before entries had checksums, keeping its entries. A reset would
     * restart sequence numbers at 0. The version changes last, so an interrupted upgrade repeats.
     */
    private fun addChecksums(buffer: MappedByteBuffer) {
        for (slot in 0 until count.get().coerceIn(0, capacity)) {
            loadSlot(buffer, slot)
            buffer.putInt(slotOffset(slot) + entryDataSize, entryChecksum())
        }
        buffer.force()
        buffer.putInt(offVersion, version)
        buffer.force()
    }

    private fun isHeaderConsistent(): Boolean {
        val c = count.get()
        val w = writeIdx.get()
        return c in 0..capacity &&
            w in 0 until capacity &&
            hasWrapped == (c == capacity) &&
            (c == capacity || w == c)
    }

    /** Restores the write position from the entries when an interrupted update broke the header. */
    private fun rebuildHeader(buffer: MappedByteBuffer) {
        SystemLogger.w("ForensicBuffer", "Header inconsistent. Rebuilding it from stored entries.")
        var newestSlot = -1
        var newestSequence = -1L
        var validEntries = 0
        for (slot in 0 until capacity) {
            if (!loadSlot(buffer, slot)) continue
            validEntries++
            val sequence = entryView.getLong(offSeq)
            if (sequence > newestSequence) {
                newestSequence = sequence
                newestSlot = slot
            }
        }

        val slotsUsed = newestSlot + 1
        val newCount = if (validEntries > slotsUsed) capacity else slotsUsed
        writeIdx.set(slotsUsed % capacity)
        count.set(newCount)
        hasWrapped = newCount == capacity
        commitHeader(writeIdx.get(), newCount, hasWrapped, reservedSequence, force = true)
    }

    private fun slotOffset(slot: Int) = headerSize + (slot * entrySize)

    private fun entryChecksum(): Int {
        crc.reset()
        crc.update(entryData, 0, entryDataSize)
        return crc.value.toInt()
    }

    /** Copies a slot into [entryData]. Returns false if it was torn or never reached storage. */
    private fun loadSlot(buffer: MappedByteBuffer, slot: Int): Boolean {
        val offset = slotOffset(slot)
        for (i in 0 until entryDataSize) entryData[i] = buffer.get(offset + i)
        return buffer.getInt(offset + entryDataSize) == entryChecksum()
    }

    /** Copies [entryData] into a slot. The checksum goes last, so a partial write fails the check. */
    private fun storeSlot(buffer: MappedByteBuffer, slot: Int) {
        val offset = slotOffset(slot)
        for (i in 0 until entryDataSize) buffer.put(offset + i, entryData[i])
        buffer.putInt(offset + entryDataSize, entryChecksum())
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
                    reservedSequence = 0L
                    return false
                }
            }
        }
        return true
    }

    private fun commitHeader(wIdx: Int, c: Int, wrapped: Boolean, reserved: Long, force: Boolean = false) {
        mappedBuffer?.let {
            it.putInt(offWriteIdx, wIdx)
            it.putInt(offCount, c)
            it.putInt(offHasWrapped, if (wrapped) 1 else 0)
            it.putLong(offReservedSeq, reserved)

            val now = System.currentTimeMillis()
            if (force || (now - lastForceTs.get() > 5000)) {
                try {
                    it.force()
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

    /**
     * Next sequence number to use, so numbering continues across restarts. Never below the stored
     * reservation: after power loss the newest entries are gone, but the Monitor already holds
     * those numbers and would replace its rows if they were used again.
     */
    fun nextSequenceId(): Long {
        val afterStored = (readTraces().maxOfOrNull { it.sequenceId } ?: -1L) + 1
        return maxOf(afterStored, reservedSequence)
    }

    fun readRange(startSeq: Long, endSeq: Long): List<LocationUpdate> {
        if (!verifyFileIntegrity()) return emptyList()
        val buffer = mappedBuffer ?: return emptyList()
        val result = mutableListOf<LocationUpdate>()
        synchronized(this) {
            val c = count.get()
            val wIdx = writeIdx.get()
            val startIdx = if (hasWrapped) wIdx else 0
            var skipped = 0

            for (i in 0 until c) {
                val physicalIdx = (startIdx + i) % capacity

                try {
                    if (!loadSlot(buffer, physicalIdx)) {
                        skipped++
                        continue
                    }

                    val seqId = entryView.getLong(offSeq)
                    if (seqId in startSeq..endSeq) {
                        val status = String(entryData, offStatus, statusSize, Charsets.UTF_8)
                            .trim { it <= ' ' || it == '\u0000' }
                        val update = LocationUpdate().apply {
                            this.timestamp = entryView.getLong(offTimestamp)
                            this.lat = entryView.getDouble(offLat)
                            this.lng = entryView.getDouble(offLng)
                            this.accuracy = entryView.getFloat(offAccuracy)
                            this.speed = entryView.getFloat(offSpeed)
                            this.sequenceId = seqId
                            this.sensorSnapshot.vibration = entryView.getFloat(offVibration)
                            this.monotonicNanos = entryView.getLong(offMonotonic)
                            this.status = status.ifEmpty { "VALID" }
                        }
                        result.add(update)
                    }
                } catch (e: Exception) {
                    SystemLogger.e("ForensicBuffer", "Read failed at idx $physicalIdx: ${e.message}")
                    break
                }
            }

            if (skipped > 0) SystemLogger.w("ForensicBuffer", "Skipped $skipped damaged entries.")
        }
        return result
    }

    fun clear() {
        synchronized(this) {
            writeIdx.set(0)
            count.set(0)
            hasWrapped = false
            reservedSequence = 0L
            try {
                commitHeader(0, 0, false, 0L, force = true)
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

            try {
                entryView.putLong(offTimestamp, update.timestamp)
                entryView.putDouble(offLat, update.lat)
                entryView.putDouble(offLng, update.lng)
                entryView.putFloat(offAccuracy, update.accuracy)
                entryView.putFloat(offSpeed, update.speed)
                entryView.putLong(offSeq, update.sequenceId)
                entryView.putFloat(offVibration, update.sensorSnapshot.vibration)
                entryView.putLong(offMonotonic, update.monotonicNanos)
                
                val statusBytes = update.status.toByteArray(Charsets.UTF_8)
                entryData.fill(0, offStatus, entryDataSize)
                System.arraycopy(statusBytes, 0, entryData, offStatus, minOf(statusBytes.size, statusSize))
                storeSlot(buffer, currentWrite)

                // Prepare next indices
                var nextIdx = currentWrite + 1
                var nextWrapped = hasWrapped
                if (nextIdx >= capacity) {
                    nextIdx = 0
                    nextWrapped = true
                }
                val newCount = if (count.get() < capacity) count.get() + 1 else capacity

                // Reach storage before this number leaves the device, so it can never be reused.
                val reserveSpent = update.sequenceId >= reservedSequence
                val newReserved = if (reserveSpent) update.sequenceId + sequenceReserve else reservedSequence

                // R197: Commit header with NEW indices before advancing in-memory state
                // This ensures that if we crash now, the data we just wrote is visible on next load.
                commitHeader(nextIdx, newCount, nextWrapped, newReserved, force = isCritical || reserveSpent)

                // Finalize update in memory only after successful commit
                writeIdx.set(nextIdx)
                count.set(newCount)
                hasWrapped = nextWrapped
                reservedSequence = newReserved
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
