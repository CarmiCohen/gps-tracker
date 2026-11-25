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
 *
 * Version 4 (Nov17.3) also keeps the satellites used and in view, the mean signal and the noise level, so
 * backfilled rows carry them like live rows (ISSUE-NOV15.5-O99 part 2). Version 5 (Nov17.7) also keeps the
 * battery level, charging and temperature (ISSUE-NOV17.3-O102). Version 6 (Nov22.11) also keeps the light level
 * (SIMP-NOV22.4-S1). Version 7 (Nov23.1) stores an unknown temperature as NaN instead of -1, which is also a real
 * temperature (SIMP-NOV17.7-S1); the -1 of versions 5 and 6 is read as not known. Older files are upgraded in place.
 */
class ForensicBuffer(
    private val storageDir: File,
    val capacity: Int = 10000
) {
    private val magic = 0x5245564F 
    private val version = 7
    private val versionWithLux = 6
    private val versionWithBattery = 5
    private val versionWithSatellites = 4
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
    private val offSatsUsed = offStatus + statusSize
    private val offSatsInView = offSatsUsed + 4
    private val offMeanCn0 = offSatsInView + 4 // NaN when not known
    private val offNoise = offMeanCn0 + 4 // NaN when not known
    private val offBattery = offNoise + 4 // -1 when not known, never a real level
    private val offCharging = offBattery + 4 // 1 when charging
    private val offTemp = offCharging + 4 // NaN when not known (-1 in versions 5 and 6, SIMP-NOV17.7-S1)
    private val offLux = offTemp + 4 // NaN when not known
    private val entryDataSize = offLux + 4

    /** The stored fields of an older version; its checksum, from version 3 on, follows them. */
    private fun legacyDataSize(storedVersion: Int) = when (storedVersion) {
        versionWithLux -> entryDataSize
        versionWithBattery -> offLux
        versionWithSatellites -> offBattery
        else -> offSatsUsed
    }

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
            val known = storedVersion in versionWithoutChecksums..version
            if (buffer.getInt(offMagic) != magic || !known) {
                buffer.putInt(offMagic, magic)
                buffer.putInt(offVersion, version)
                clear()
                return
            }

            writeIdx.set(buffer.getInt(offWriteIdx))
            count.set(buffer.getInt(offCount))
            hasWrapped = buffer.getInt(offHasWrapped) == 1
            reservedSequence = buffer.getLong(offReservedSeq)

            if (storedVersion != version) upgradeEntries(buffer, storedVersion)
            if (!isHeaderConsistent()) rebuildHeader(buffer)
        }
    }

    /**
     * Upgrades a version 2 to 6 file to version 7, keeping its entries: a reset would restart sequence
     * numbers at 0. The fields the old version lacks are stored as not known, and so is the -1 temperature of
     * versions 5 and 6, which meant not known there (SIMP-NOV17.7-S1). An entry that failed its
     * checksum stays failed. Entries are rewritten whole from their legacy bytes, and the version changes last,
     * so an interrupted upgrade simply repeats.
     */
    private fun upgradeEntries(buffer: MappedByteBuffer, storedVersion: Int) {
        val legacySize = legacyDataSize(storedVersion)
        val checked = storedVersion != versionWithoutChecksums
        val legacy = ByteArray(legacySize)
        for (slot in 0 until count.get().coerceIn(0, capacity)) {
            val offset = slotOffset(slot)
            for (i in 0 until legacySize) legacy[i] = buffer.get(offset + i)
            crc.reset()
            crc.update(legacy, 0, legacySize)
            val valid = !checked || buffer.getInt(offset + legacySize) == crc.value.toInt()
            System.arraycopy(legacy, 0, entryData, 0, legacySize)
            putUnknownExtras(from = legacySize)
            if (storedVersion >= versionWithBattery && entryView.getFloat(offTemp) == -1f) entryView.putFloat(offTemp, Float.NaN)
            storeSlot(buffer, slot)
            if (!valid) buffer.putInt(offset + entryDataSize, entryChecksum().inv())
        }
        buffer.force()
        buffer.putInt(offVersion, version)
        buffer.force()
    }

    /** Stores as not known every field from [from] on, the ones an older version lacks. */
    private fun putUnknownExtras(from: Int) {
        if (from <= offSatsUsed) {
            entryView.putInt(offSatsUsed, -1)
            entryView.putInt(offSatsInView, -1)
            entryView.putFloat(offMeanCn0, Float.NaN)
            entryView.putFloat(offNoise, Float.NaN)
        }
        if (from <= offBattery) {
            entryView.putInt(offBattery, -1)
            entryView.putInt(offCharging, 0)
            entryView.putFloat(offTemp, Float.NaN)
        }
        if (from <= offLux) entryView.putFloat(offLux, Float.NaN)
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
                            this.vibrationG = entryView.getFloat(offVibration)
                            this.monotonicNanos = entryView.getLong(offMonotonic)
                            this.status = status.ifEmpty { "VALID" }
                            this.satsUsed = entryView.getInt(offSatsUsed)
                            this.satsInView = entryView.getInt(offSatsInView)
                            this.meanCn0DbHz = entryView.getFloat(offMeanCn0).takeUnless { it.isNaN() }
                            this.noiseDbfs = entryView.getFloat(offNoise).takeUnless { it.isNaN() }
                            this.batteryLevel = entryView.getInt(offBattery).takeIf { it >= 0 }
                            this.isCharging = entryView.getInt(offCharging) == 1
                            this.tempCelsius = entryView.getFloat(offTemp).takeUnless { it.isNaN() }
                            this.lightLux = entryView.getFloat(offLux).takeUnless { it.isNaN() }
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

    /** Stores [update]; a [LocationUpdate.isCritical] row forces its header to storage at once (SIMP-NOV7.21-S1). */
    fun writeTrace(update: LocationUpdate) {
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
                entryView.putFloat(offVibration, update.vibrationG)
                entryView.putLong(offMonotonic, update.monotonicNanos)
                
                val statusBytes = update.status.toByteArray(Charsets.UTF_8)
                entryData.fill(0, offStatus, entryDataSize)
                System.arraycopy(statusBytes, 0, entryData, offStatus, minOf(statusBytes.size, statusSize))
                entryView.putInt(offSatsUsed, update.satsUsed)
                entryView.putInt(offSatsInView, update.satsInView)
                entryView.putFloat(offMeanCn0, update.meanCn0DbHz ?: Float.NaN)
                entryView.putFloat(offNoise, update.noiseDbfs ?: Float.NaN)
                entryView.putInt(offBattery, update.batteryLevel ?: -1)
                entryView.putInt(offCharging, if (update.isCharging) 1 else 0)
                entryView.putFloat(offTemp, update.tempCelsius ?: Float.NaN)
                entryView.putFloat(offLux, update.lightLux ?: Float.NaN)
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
                commitHeader(nextIdx, newCount, nextWrapped, newReserved, force = update.isCritical || reserveSpent)

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
