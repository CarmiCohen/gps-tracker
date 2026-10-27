package com.gps19.app

import com.gps19.core.engine.*
import com.gps19.core.engine.PhysicsUtils.safeDouble
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Models: UI and Persistence data structures for GPS Tracker.
 * Oct.26.6:
 * - SIMP-OCT26.6-S1: Added isConnectionAborted to HudConnectivityState to 
 *   support UI latching for signaling rejections.
 * - GAP-OCT26.5-1: Added conflatedCount to HudConnectivityState.
 */

@Serializable
data class SerializableGeoPoint(val lat: Double, val lng: Double) {
    fun toGeoPoint() = GeoPoint(lat, lng)
}

fun GeoPoint.toSerializable() = SerializableGeoPoint(latitude, longitude)

@Serializable
data class TrailPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long = 0L,
    val status: SentinelStatus = SentinelStatus.VALID,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0
) {
    @Transient
    private var _cachedGeoPoint: GeoPoint? = null

    fun toGeoPoint(): GeoPoint {
        val cached = _cachedGeoPoint
        if (cached != null && cached.latitude == lat && cached.longitude == lng) {
            return cached
        }
        val newPoint = GeoPoint(lat, lng)
        _cachedGeoPoint = newPoint
        return newPoint
    }

    /**
     * contentEquals: Deep parity check to prevent redundant UI snapshot updates (R312).
     */
    fun contentEquals(other: TrailPoint): Boolean {
        return lat == other.lat && lng == other.lng && status == other.status && 
               accuracy == other.accuracy && maxAccuracy == other.maxAccuracy
    }
}

/**
 * TrailBuffer: Optimized container for trail points using primitive arrays.
 * Designed to minimize GC pressure by avoiding thousands of TrailPoint objects (SIMP-OCT15.0-S1).
 */
@Serializable
data class TrailBuffer(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val timestamps: LongArray,
    val statuses: IntArray,
    val accuracies: DoubleArray,
    val maxAccuracies: DoubleArray,
    val size: Int
) {
    fun getPoint(index: Int): TrailPoint {
        return TrailPoint(
            lat = lats[index],
            lng = lngs[index],
            timestamp = timestamps[index],
            status = SentinelStatus.entries.getOrElse(statuses[index]) { SentinelStatus.VALID },
            accuracy = accuracies[index],
            maxAccuracy = maxAccuracies[index]
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrailBuffer) return false
        if (size != other.size) return false
        if (!lats.contentEquals(other.lats)) return false
        if (!lngs.contentEquals(other.lngs)) return false
        if (!timestamps.contentEquals(other.timestamps)) return false
        if (!statuses.contentEquals(other.statuses)) return false
        if (!accuracies.contentEquals(other.accuracies)) return false
        if (!maxAccuracies.contentEquals(other.maxAccuracies)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lats.contentHashCode()
        result = 31 * result + lngs.contentHashCode()
        result = 31 * result + timestamps.contentHashCode()
        result = 31 * result + statuses.contentHashCode()
        result = 31 * result + accuracies.contentHashCode()
        result = 31 * result + maxAccuracies.contentHashCode()
        result = 31 * result + size
        return result
    }

    companion object {
        val EMPTY = TrailBuffer(DoubleArray(0), DoubleArray(0), LongArray(0), IntArray(0), DoubleArray(0), DoubleArray(0), 0)
    }
}

/**
 * RibbonNormals: Container for pre-calculated normalized values [0.0, 1.0].
 * Moved from HistoryBuffer to ingestion point to ensure immutability (SIMP-OCT15.21-S1).
 */
@Serializable
data class RibbonNormals(
    var snr: Float = 0f,
    var noise: Float = 0f,
    var kinetic: Float = 0f,
    var lux: Float = 0f,
    var vibe: Float = 0f,
    var prox: Float = 0f,
    var lift: Float = 0f,
    var bat: Float = 0f,
    var thm: Float = 0f,
    var cur: Float = 0f,
    var sit: Float = 0f,
    var tlt: Float = 0f,
    var baro: Float = 0f,
    var svz: Float = 0f,
    var sdz: Float = 0f,
    var cpu: Float = 0f,
    var iow: Float = 0f,
    var lat: Float = 0f,
    var comm: Float = 0f
) {
    fun copyFrom(other: RibbonNormals) {
        snr = other.snr; noise = other.noise; kinetic = other.kinetic; lux = other.lux
        vibe = other.vibe; prox = other.prox; lift = other.lift; bat = other.bat
        thm = other.thm; cur = other.cur; sit = other.sit; tlt = other.tlt
        baro = other.baro; svz = other.svz; sdz = other.sdz; cpu = other.cpu
        iow = other.iow; lat = other.lat; comm = other.comm
    }
    
    fun reset() {
        snr = 0f; noise = 0f; kinetic = 0f; lux = 0f; vibe = 0f; prox = 0f
        lift = 0f; bat = 0f; thm = 0f; cur = 0f; sit = 0f; tlt = 0f
        baro = 0f; svz = 0f; sdz = 0f; cpu = 0f; iow = 0f; lat = 0f; comm = 0f
    }
}

/**
 * HistoryBuffer: Optimized container for connection history points using primitive arrays.
 */
@Serializable
data class HistoryBuffer(
    val ts: LongArray,
    val rt: LongArray,
    val rtt: IntArray,
    val localSig: IntArray,
    val remoteSig: IntArray,
    val isConnected: BooleanArray,
    val isGap: BooleanArray,
    val isRecoveryEvent: BooleanArray,
    val hasGps: BooleanArray,
    val gpsIndex: DoubleArray,
    val snrIdx: DoubleArray,
    val noiseIdx: DoubleArray,
    val luxIdx: DoubleArray,
    val vibeIdx: DoubleArray,
    val proxIdx: DoubleArray,
    val liftIdx: DoubleArray,
    val tiltIdx: DoubleArray,
    val baroIdx: DoubleArray,
    val isBatterySteepDischarge: BooleanArray,
    val isCoolingModeActive: BooleanArray,
    val currentMa: IntArray,
    val isSitActive: BooleanArray,
    val sitVz: DoubleArray,
    val sitVzTs: LongArray,
    val sitDz: DoubleArray,
    val cpuLoad: DoubleArray,
    val ioWait: DoubleArray,
    val maxIoLatency: LongArray,
    val kineticEnergy: DoubleArray,
    val snrNorm: FloatArray,
    val noiseNorm: FloatArray,
    val kineticNorm: FloatArray,
    val luxNorm: FloatArray,
    val vibeNorm: FloatArray,
    val proxNorm: FloatArray,
    val liftNorm: FloatArray,
    val batNorm: FloatArray,
    val thmNorm: FloatArray,
    val curNorm: FloatArray,
    val sitNorm: FloatArray,
    val tltNorm: FloatArray,
    val baroNorm: FloatArray,
    val svzNorm: FloatArray,
    val sdzNorm: FloatArray,
    val cpuNorm: FloatArray,
    val iowNorm: FloatArray,
    val latNorm: FloatArray,
    val commNorm: FloatArray,
    val size: Int
) {
    fun getPoint(index: Int): ConnectionPoint {
        return ConnectionPoint().apply {
            ts = this@HistoryBuffer.ts[index]
            rt = this@HistoryBuffer.rt[index]
            rtt = this@HistoryBuffer.rtt[index]
            localSig = this@HistoryBuffer.localSig[index]
            remoteSig = this@HistoryBuffer.remoteSig[index]
            isConnected = this@HistoryBuffer.isConnected[index]
            isGap = this@HistoryBuffer.isGap[index]
            isRecoveryEvent = this@HistoryBuffer.isRecoveryEvent[index]
            hasGps = this@HistoryBuffer.hasGps[index]
            gpsIndex = this@HistoryBuffer.gpsIndex[index]
            snrIdx = this@HistoryBuffer.snrIdx[index]
            noiseIdx = this@HistoryBuffer.noiseIdx[index]
            luxIdx = this@HistoryBuffer.luxIdx[index]
            vibeIdx = this@HistoryBuffer.vibeIdx[index]
            proxIdx = this@HistoryBuffer.proxIdx[index]
            liftIdx = this@HistoryBuffer.liftIdx[index]
            tiltIdx = this@HistoryBuffer.tiltIdx[index]
            baroIdx = this@HistoryBuffer.baroIdx[index]
            isBatterySteepDischarge = this@HistoryBuffer.isBatterySteepDischarge[index]
            isCoolingModeActive = this@HistoryBuffer.isCoolingModeActive[index]
            currentMa = this@HistoryBuffer.currentMa[index]
            isSitActive = this@HistoryBuffer.isSitActive[index]
            sitVz = this@HistoryBuffer.sitVz[index]
            sitVzTs = this@HistoryBuffer.sitVzTs[index]
            sitDz = this@HistoryBuffer.sitDz[index]
            cpuLoad = this@HistoryBuffer.cpuLoad[index]
            ioWait = this@HistoryBuffer.ioWait[index]
            maxIoLatency = this@HistoryBuffer.maxIoLatency[index]
            kineticEnergy = this@HistoryBuffer.kineticEnergy[index]
            
            ribbons.apply {
                snr = snrNorm[index]; noise = noiseNorm[index]; kinetic = kineticNorm[index]
                lux = luxNorm[index]; vibe = vibeNorm[index]; prox = proxNorm[index]
                lift = liftNorm[index]; bat = batNorm[index]; thm = thmNorm[index]
                cur = curNorm[index]; sit = sitNorm[index]; tlt = tltNorm[index]
                baro = baroNorm[index]; svz = svzNorm[index]; sdz = sdzNorm[index]
                cpu = cpuNorm[index]; iow = iowNorm[index]; lat = latNorm[index]
                comm = commNorm[index]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistoryBuffer) return false
        if (size != other.size) return false
        if (!ts.contentEquals(other.ts)) return false
        if (!rt.contentEquals(other.rt)) return false
        if (!rtt.contentEquals(other.rtt)) return false
        if (!localSig.contentEquals(other.localSig)) return false
        if (!remoteSig.contentEquals(other.remoteSig)) return false
        if (!isConnected.contentEquals(other.isConnected)) return false
        if (!isGap.contentEquals(other.isGap)) return false
        if (!isRecoveryEvent.contentEquals(other.isRecoveryEvent)) return false
        if (!hasGps.contentEquals(other.hasGps)) return false
        if (!gpsIndex.contentEquals(other.gpsIndex)) return false
        if (!snrIdx.contentEquals(other.snrIdx)) return false
        if (!noiseIdx.contentEquals(other.noiseIdx)) return false
        if (!luxIdx.contentEquals(other.luxIdx)) return false
        if (!vibeIdx.contentEquals(other.vibeIdx)) return false
        if (!proxIdx.contentEquals(other.proxIdx)) return false
        if (!liftIdx.contentEquals(other.liftIdx)) return false
        if (!tiltIdx.contentEquals(other.tiltIdx)) return false
        if (!baroIdx.contentEquals(other.baroIdx)) return false
        if (!isBatterySteepDischarge.contentEquals(other.isBatterySteepDischarge)) return false
        if (!isCoolingModeActive.contentEquals(other.isCoolingModeActive)) return false
        if (!currentMa.contentEquals(other.currentMa)) return false
        if (!isSitActive.contentEquals(other.isSitActive)) return false
        if (!sitVz.contentEquals(other.sitVz)) return false
        if (!sitVzTs.contentEquals(other.sitVzTs)) return false
        if (!sitDz.contentEquals(other.sitDz)) return false
        if (!cpuLoad.contentEquals(other.cpuLoad)) return false
        if (!ioWait.contentEquals(other.ioWait)) return false
        if (!maxIoLatency.contentEquals(other.maxIoLatency)) return false
        if (!kineticEnergy.contentEquals(other.kineticEnergy)) return false
        if (!snrNorm.contentEquals(other.snrNorm)) return false
        if (!noiseNorm.contentEquals(other.noiseNorm)) return false
        if (!kineticNorm.contentEquals(other.kineticNorm)) return false
        if (!luxNorm.contentEquals(other.luxNorm)) return false
        if (!vibeNorm.contentEquals(other.vibeNorm)) return false
        if (!proxNorm.contentEquals(other.proxNorm)) return false
        if (!liftNorm.contentEquals(other.liftNorm)) return false
        if (!batNorm.contentEquals(other.batNorm)) return false
        if (!thmNorm.contentEquals(other.thmNorm)) return false
        if (!curNorm.contentEquals(other.curNorm)) return false
        if (!sitNorm.contentEquals(other.sitNorm)) return false
        if (!tltNorm.contentEquals(other.tltNorm)) return false
        if (!baroNorm.contentEquals(other.baroNorm)) return false
        if (!svzNorm.contentEquals(other.svzNorm)) return false
        if (!sdzNorm.contentEquals(other.sdzNorm)) return false
        if (!cpuNorm.contentEquals(other.cpuNorm)) return false
        if (!iowNorm.contentEquals(other.iowNorm)) return false
        if (!latNorm.contentEquals(other.latNorm)) return false
        if (!commNorm.contentEquals(other.commNorm)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ts.contentHashCode()
        result = 31 * result + rt.contentHashCode()
        result = 31 * result + rtt.contentHashCode()
        result = 31 * result + localSig.contentHashCode()
        result = 31 * result + remoteSig.contentHashCode()
        result = 31 * result + isConnected.contentHashCode()
        result = 31 * result + isGap.contentHashCode()
        result = 31 * result + isRecoveryEvent.contentHashCode()
        result = 31 * result + hasGps.contentHashCode()
        result = 31 * result + gpsIndex.contentHashCode()
        result = 31 * result + snrIdx.contentHashCode()
        result = 31 * result + noiseIdx.contentHashCode()
        result = 31 * result + luxIdx.contentHashCode()
        result = 31 * result + vibeIdx.contentHashCode()
        result = 31 * result + proxIdx.contentHashCode()
        result = 31 * result + liftIdx.contentHashCode()
        result = 31 * result + tiltIdx.contentHashCode()
        result = 31 * result + baroIdx.contentHashCode()
        result = 31 * result + isBatterySteepDischarge.contentHashCode()
        result = 31 * result + isCoolingModeActive.contentHashCode()
        result = 31 * result + currentMa.contentHashCode()
        result = 31 * result + isSitActive.contentHashCode()
        result = 31 * result + sitVz.contentHashCode()
        result = 31 * result + sitVzTs.contentHashCode()
        result = 31 * result + sitDz.contentHashCode()
        result = 31 * result + cpuLoad.contentHashCode()
        result = 31 * result + ioWait.contentHashCode()
        result = 31 * result + maxIoLatency.contentHashCode()
        result = 31 * result + kineticEnergy.contentHashCode()
        result = 31 * result + snrNorm.contentHashCode()
        result = 31 * result + noiseNorm.contentHashCode()
        result = 31 * result + kineticNorm.contentHashCode()
        result = 31 * result + luxNorm.contentHashCode()
        result = 31 * result + vibeNorm.contentHashCode()
        result = 31 * result + proxNorm.contentHashCode()
        result = 31 * result + liftNorm.contentHashCode()
        result = 31 * result + batNorm.contentHashCode()
        result = 31 * result + thmNorm.contentHashCode()
        result = 31 * result + curNorm.contentHashCode()
        result = 31 * result + sitNorm.contentHashCode()
        result = 31 * result + tltNorm.contentHashCode()
        result = 31 * result + baroNorm.contentHashCode()
        result = 31 * result + svzNorm.contentHashCode()
        result = 31 * result + sdzNorm.contentHashCode()
        result = 31 * result + cpuNorm.contentHashCode()
        result = 31 * result + iowNorm.contentHashCode()
        result = 31 * result + latNorm.contentHashCode()
        result = 31 * result + commNorm.contentHashCode()
        result = 31 * result + size
        return result
    }

    companion object {
        val EMPTY = HistoryBuffer(
            LongArray(0), LongArray(0), IntArray(0), IntArray(0), IntArray(0),
            BooleanArray(0), BooleanArray(0), BooleanArray(0), BooleanArray(0),
            DoubleArray(0), DoubleArray(0), DoubleArray(0), DoubleArray(0),
            DoubleArray(0), DoubleArray(0), DoubleArray(0), DoubleArray(0),
            DoubleArray(0), BooleanArray(0), BooleanArray(0), IntArray(0),
            BooleanArray(0), DoubleArray(0), LongArray(0), DoubleArray(0),
            DoubleArray(0), DoubleArray(0), LongArray(0), DoubleArray(0),
            FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0),
            FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0),
            FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0),
            FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 0
        )

        fun fromPoints(points: List<ConnectionPoint>, limit: Int = Int.MAX_VALUE): HistoryBuffer {
            val finalPoints = if (points.size > limit) points.takeLast(limit) else points
            val size = finalPoints.size
            val ts = LongArray(size); val rt = LongArray(size); val rtt = IntArray(size)
            val localSig = IntArray(size); val remoteSig = IntArray(size)
            val isConnected = BooleanArray(size); val isGap = BooleanArray(size)
            val isRecoveryEvent = BooleanArray(size); val hasGps = BooleanArray(size)
            val gpsIndex = DoubleArray(size); val snrIdx = DoubleArray(size)
            val noiseIdx = DoubleArray(size); val luxIdx = DoubleArray(size)
            val vibeIdx = DoubleArray(size); val proxIdx = DoubleArray(size)
            val liftIdx = DoubleArray(size); val tiltIdx = DoubleArray(size)
            val baroIdx = DoubleArray(size); val isBatterySteepDischarge = BooleanArray(size)
            val isCoolingModeActive = BooleanArray(size); val currentMa = IntArray(size)
            val isSitActive = BooleanArray(size); val sitVz = DoubleArray(size)
            val sitVzTs = LongArray(size); val sitDz = DoubleArray(size)
            val cpuLoad = DoubleArray(size); val ioWait = DoubleArray(size)
            val maxIoLatency = LongArray(size); val kineticEnergy = DoubleArray(size)

            val snrNorm = FloatArray(size); val noiseNorm = FloatArray(size)
            val kineticNorm = FloatArray(size); val luxNorm = FloatArray(size)
            val vibeNorm = FloatArray(size); val proxNorm = FloatArray(size)
            val liftNorm = FloatArray(size); val batNorm = FloatArray(size)
            val thmNorm = FloatArray(size); val curNorm = FloatArray(size)
            val sitNorm = FloatArray(size); val tltNorm = FloatArray(size)
            val baroNorm = FloatArray(size); val svzNorm = FloatArray(size)
            val sdzNorm = FloatArray(size); val cpuNorm = FloatArray(size)
            val iowNorm = FloatArray(size); val latNorm = FloatArray(size)
            val commNorm = FloatArray(size)

            finalPoints.forEachIndexed { i, p ->
                ts[i] = p.ts; rt[i] = p.rt; rtt[i] = p.rtt; localSig[i] = p.localSig; remoteSig[i] = p.remoteSig
                isConnected[i] = p.isConnected; isGap[i] = p.isGap; isRecoveryEvent[i] = p.isRecoveryEvent
                hasGps[i] = p.hasGps; gpsIndex[i] = p.gpsIndex; snrIdx[i] = p.snrIdx; noiseIdx[i] = p.noiseIdx
                luxIdx[i] = p.luxIdx; vibeIdx[i] = p.vibeIdx; proxIdx[i] = p.proxIdx; liftIdx[i] = p.liftIdx
                tiltIdx[i] = p.tiltIdx; baroIdx[i] = p.baroIdx; isBatterySteepDischarge[i] = p.isBatterySteepDischarge
                isCoolingModeActive[i] = p.isCoolingModeActive; currentMa[i] = p.currentMa; isSitActive[i] = p.isSitActive
                sitVz[i] = p.sitVz; sitVzTs[i] = p.sitVzTs; sitDz[i] = p.sitDz
                cpuLoad[i] = p.cpuLoad; ioWait[i] = p.ioWait; maxIoLatency[i] = p.maxIoLatency; kineticEnergy[i] = p.kineticEnergy

                snrNorm[i] = p.ribbons.snr
                noiseNorm[i] = p.ribbons.noise
                kineticNorm[i] = p.ribbons.kinetic
                luxNorm[i] = p.ribbons.lux
                vibeNorm[i] = p.ribbons.vibe
                proxNorm[i] = p.ribbons.prox
                liftNorm[i] = p.ribbons.lift
                batNorm[i] = p.ribbons.bat
                thmNorm[i] = p.ribbons.thm
                curNorm[i] = p.ribbons.cur
                sitNorm[i] = p.ribbons.sit
                tltNorm[i] = p.ribbons.tlt
                baroNorm[i] = p.ribbons.baro
                svzNorm[i] = p.ribbons.svz
                sdzNorm[i] = p.ribbons.sdz
                cpuNorm[i] = p.ribbons.cpu
                iowNorm[i] = p.ribbons.iow
                latNorm[i] = p.ribbons.lat
                commNorm[i] = p.ribbons.comm
            }

            return HistoryBuffer(
                ts, rt, rtt, localSig, remoteSig, isConnected, isGap, isRecoveryEvent, hasGps,
                gpsIndex, snrIdx, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, tiltIdx, baroIdx,
                isBatterySteepDischarge, isCoolingModeActive, currentMa, isSitActive,
                sitVz, sitVzTs, sitDz, cpuLoad, ioWait, maxIoLatency, kineticEnergy,
                snrNorm, noiseNorm, kineticNorm, luxNorm, vibeNorm, proxNorm, liftNorm,
                batNorm, thmNorm, curNorm, sitNorm, tltNorm, baroNorm, svzNorm, sdzNorm,
                cpuNorm, iowNorm, latNorm, commNorm, size
            )
        }

        fun merge(b1: HistoryBuffer, b2: HistoryBuffer, limit: Int = 240): HistoryBuffer {
            if (b1.size == 0) return if (b2.size > limit) fromPoints(List(b2.size) { b2.getPoint(it) }.takeLast(limit)) else b2
            if (b2.size == 0) return if (b1.size > limit) fromPoints(List(b1.size) { b1.getPoint(it) }.takeLast(limit)) else b1

            val combined = mutableListOf<ConnectionPoint>()
            for (i in 0 until b1.size) combined.add(b1.getPoint(i))
            for (i in 0 until b2.size) combined.add(b2.getPoint(i))
            
            combined.sortBy { it.ts }
            val deduped = mutableListOf<ConnectionPoint>()
            if (combined.isNotEmpty()) {
                deduped.add(combined[0])
                for (i in 1 until combined.size) {
                    if (combined[i].ts != combined[i - 1].ts) {
                        deduped.add(combined[i])
                    }
                }
            }
            return fromPoints(deduped, limit)
        }

        fun merge(current: HistoryBuffer, newPoints: List<ConnectionPoint>, limit: Int = 240): HistoryBuffer {
            if (newPoints.isEmpty()) return if (current.size > limit) fromPoints(List(current.size) { current.getPoint(it) }.takeLast(limit)) else current
            
            val combined = mutableListOf<ConnectionPoint>()
            for (i in 0 until current.size) combined.add(current.getPoint(i))
            combined.addAll(newPoints)
            
            combined.sortBy { it.ts }
            val deduped = mutableListOf<ConnectionPoint>()
            if (combined.isNotEmpty()) {
                deduped.add(combined[0])
                for (i in 1 until combined.size) {
                    if (combined[i].ts != combined[i - 1].ts) {
                        deduped.add(combined[i])
                    }
                }
            }
            return fromPoints(deduped, limit)
        }
    }
}

/**
 * MapTrailSegment: Used for optimized map rendering of trail chunks.
 */
data class MapTrailSegment(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val color: Int,
    val checksum: Int = 0 
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapTrailSegment) return false
        if (color != other.color) return false
        if (checksum != other.checksum) return false
        if (!lats.contentEquals(other.lats)) return false
        if (!lngs.contentEquals(other.lngs)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lats.contentHashCode()
        result = 31 * result + lngs.contentHashCode()
        result = 31 * result + color
        result = 31 * result + checksum
        return result
    }
}

@Serializable
data class AlertSettings(
    val localInternet: Boolean = true,
    val serverConnection: Boolean = true,
    val relayConnection: Boolean = true,
    val jammerDetection: Boolean = true,
    val signalLoss: Boolean = true,
    val gpsStalling: Boolean = true,
    val distance: Boolean = true,
    val power: Boolean = true,
    val lowBattery: Boolean = true,
    val batteryHealth: Boolean = true, 
    val longTimeGap: Boolean = true,
    val highTemperature: Boolean = true,
    val overrideSilence: Boolean = true,
    val useMaxVolume: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val alarmVolume: Float = 0.8f,
    val useCustomVolume: Boolean = false,
    val vibrationEnabledV2: Boolean = true, 
    val alarmVolumeV2: Float = 0.8f,
    val tiltAlert: Boolean = true,
    val acousticAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tamperAlert: Boolean = true,
    val globalMute: Boolean = false,
    val systemStorageLow: Boolean = true,
    val vibrationSensitivity: Float = 0.5f,
    val tiltSensitivity: Float = 0.5f
)

class ConnectionPoint(
    var localId: String = "",
    var ts: Long = 0, 
    var rt: Long = 0, 
    var rtt: Int = 0, 
    var localSig: Int = 10, 
    var remoteSig: Int = 0,
    var isConnected: Boolean = false, 
    var isGap: Boolean = false, 
    var isRecoveryEvent: Boolean = false,
    var gpsAccuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var isTick: Boolean = false, 
    var hasGps: Boolean = false,
    var isBatterySteepDischarge: Boolean = false,
    var isCoolingModeActive: Boolean = false,
    var isBatteryLow: Boolean = false,
    var isBatteryCritical: Boolean = false,
    var speed: Double = 0.0, 
    var bearing: Double = 0.0,
    var currentMa: Int = 0,
    var status: SentinelStatus = SentinelStatus.VALID,
    var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    var gpsIndex: Double = 0.0,
    var snrIdx: Double = 0.0,
    var noiseIdx: Double = 0.0,
    var luxIdx: Double = 0.0,
    var vibeIdx: Double = 0.0,
    var proxIdx: Double = 1.0,
    var liftIdx: Double = 0.0,
    var tiltIdx: Double = 0.0,
    var baroIdx: Double = 0.0,
    var isSitDetected: Boolean = false,
    var isSitActive: Boolean = false,
    var verticalVelocity: Double = 0.0,
    var sitVz: Double = 0.0,
    var sitVzTs: Long = 0L,
    var sitVzRt: Long = 0L,
    var sitDz: Double = 0.0,
    var sitBaro: Double = 0.0,
    var sitTilt: Double = 0.0,
    var sitShock: Double = 0.0,
    var kineticEnergy: Double = 0.0,
    var cpuLoad: Double = 0.0,
    var ioWait: Double = 0.0,
    var maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isUltraLongStationary: Boolean = false,
    var violationUptimeMs: Long = 0L,
    var violationPercentage: Double = 0.0,
    var gpsHardwareLock: Boolean = false,
    var isAnchorLocked: Boolean = false,
    var isGnssThrottled: Boolean = false,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    var stationaryDurationMs: Long = 0L,
    var activityType: ActivityType = ActivityType.UNKNOWN,
    val ribbons: RibbonNormals = RibbonNormals(),
    val forensic: ForensicSnapshot = ForensicSnapshot()
) {
    var snrSnapshot: Double?
        get() = forensic.snr
        set(value) { forensic.snr = value }
    var vibeSnapshot: Double?
        get() = forensic.vibe
        set(value) { forensic.vibe = value }
    var thermalSnapshot: Double?
        get() = forensic.thermal
        set(value) { forensic.thermal = value }
    var heapSnapshot: Double?
        get() = forensic.heap
        set(value) { forensic.heap = value }
    var luxSnapshot: Double?
        get() = forensic.lux
        set(value) { forensic.lux = value }
    var acousticSnapshot: Double?
        get() = forensic.acoustic
        set(value) { forensic.acoustic = value }

    // Oct.7.7 Anomaly Flags
    var isSuspiciousNoise: Boolean
        get() = forensic.isSuspiciousNoise
        set(value) { forensic.isSuspiciousNoise = value }
    var isMemoryPressureThrottled: Boolean
        get() = forensic.isMemoryPressureThrottled
        set(value) { forensic.isMemoryPressureThrottled = value }

    // Oct.7.10 Jammer Discrimination
    var isJammingCandidate: Boolean
        get() = forensic.isJammingCandidate
        set(value) { forensic.isJammingCandidate = value }

    fun copyFrom(other: ConnectionPoint) {
        this.localId = other.localId; this.ts = other.ts; this.rt = other.rt; this.rtt = other.rtt
        this.localSig = other.localSig; this.remoteSig = other.remoteSig; this.isConnected = other.isConnected
        this.isGap = other.isGap; this.isRecoveryEvent = other.isRecoveryEvent; this.gpsAccuracy = other.gpsAccuracy
        this.maxAccuracy = other.maxAccuracy; this.isTick = other.isTick; this.hasGps = other.hasGps
        this.isBatterySteepDischarge = other.isBatterySteepDischarge; this.isCoolingModeActive = other.isCoolingModeActive
        this.isBatteryLow = other.isBatteryLow; this.isBatteryCritical = other.isBatteryCritical
        this.speed = other.speed; this.bearing = other.bearing; this.currentMa = other.currentMa
        this.status = other.status; this.locationPendingReason = other.locationPendingReason
        this.gpsIndex = other.gpsIndex; this.snrIdx = other.snrIdx; this.noiseIdx = other.noiseIdx
        this.luxIdx = other.luxIdx; this.vibeIdx = other.vibeIdx; this.proxIdx = other.proxIdx
        this.liftIdx = other.liftIdx; this.tiltIdx = other.tiltIdx; this.baroIdx = other.baroIdx
        this.isSitDetected = other.isSitDetected; this.isSitActive = other.isSitActive
        this.verticalVelocity = other.verticalVelocity; this.sitVz = other.sitVz
        this.sitVzTs = other.sitVzTs; this.sitVzRt = other.sitVzRt; this.sitDz = other.sitDz
        this.sitBaro = other.sitBaro; this.sitTilt = other.sitTilt; this.sitShock = other.sitShock
        this.kineticEnergy = other.kineticEnergy; this.cpuLoad = other.cpuLoad; this.ioWait = other.ioWait
        this.maxIoLatency = other.maxIoLatency; this.isSilentFailure = other.isSilentFailure
        this.isUltraLongStationary = other.isUltraLongStationary; this.violationUptimeMs = other.violationUptimeMs
        this.violationPercentage = other.violationPercentage
        this.gpsHardwareLock = other.gpsHardwareLock; this.isAnchorLocked = other.isAnchorLocked
        this.isGnssThrottled = other.isGnssThrottled
        this.thermalHeadroom = other.thermalHeadroom
        this.heapAllocatedMb = other.heapAllocatedMb
        this.stationaryDurationMs = other.stationaryDurationMs
        this.activityType = other.activityType
        this.ribbons.copyFrom(other.ribbons)
        this.forensic.copyFrom(other.forensic)
    }

    /**
     * contentEquals: Deep parity check for history ribbon points (R312).
     */
    fun contentEquals(other: ConnectionPoint): Boolean {
        return ts == other.ts && rtt == other.rtt && remoteSig == other.remoteSig && 
               isConnected == other.isConnected && isGap == other.isGap && 
               isRecoveryEvent == other.isRecoveryEvent && hasGps == other.hasGps &&
               gpsIndex == other.gpsIndex && snrIdx == other.snrIdx && vibeIdx == other.vibeIdx &&
               luxIdx == other.luxIdx && noiseIdx == other.noiseIdx && liftIdx == other.liftIdx &&
               tiltIdx == other.tiltIdx && baroIdx == other.baroIdx && isSitActive == other.isSitActive &&
               isUltraLongStationary == other.isUltraLongStationary && violationUptimeMs == other.violationUptimeMs &&
               violationPercentage == other.violationPercentage &&
               gpsHardwareLock == other.gpsHardwareLock && isAnchorLocked == other.isAnchorLocked &&
               isGnssThrottled == other.isGnssThrottled && thermalHeadroom == other.thermalHeadroom && 
               heapAllocatedMb == other.heapAllocatedMb && stationaryDurationMs == other.stationaryDurationMs &&
               activityType == other.activityType &&
               forensic.snr == other.forensic.snr && forensic.vibe == other.vibeSnapshot &&
               forensic.thermal == other.forensic.thermal && forensic.heap == other.forensic.heap &&
               forensic.lux == other.forensic.lux && forensic.acoustic == other.forensic.acoustic &&
               forensic.isSuspiciousNoise == other.forensic.isSuspiciousNoise &&
               forensic.isMemoryPressureThrottled == other.forensic.isMemoryPressureThrottled &&
               forensic.isJammingCandidate == other.forensic.isJammingCandidate
    }

    fun reset() {
        localId = ""; ts = 0; rt = 0; rtt = 0; localSig = 10; remoteSig = 0
        isConnected = false; isGap = false; isRecoveryEvent = false; gpsAccuracy = 0.0
        maxAccuracy = 0.0; isTick = false; hasGps = false; isBatterySteepDischarge = false
        isCoolingModeActive = false; isBatteryLow = false; isBatteryCritical = false
        speed = 0.0; bearing = 0.0; currentMa = 0; status = SentinelStatus.VALID
        locationPendingReason = LocationPendingReason.NONE; gpsIndex = 0.0
        snrIdx = 0.0; noiseIdx = 0.0; luxIdx = 0.0; vibeIdx = 0.0; proxIdx = 1.0; liftIdx = 0.0
        tiltIdx = 0.0; baroIdx = 0.0; isSitDetected = false; isSitActive = false
        sitVz = 0.0; sitVzTs = 0L; sitVzRt = 0L; sitDz = 0.0; sitBaro = 0.0; sitTilt = 0.0
        sitShock = 0.0; kineticEnergy = 0.0; cpuLoad = 0.0; ioWait = 0.0; maxIoLatency = 0L
        isSilentFailure = false; isUltraLongStationary = false; violationUptimeMs = 0L
        violationPercentage = 0.0
        gpsHardwareLock = false; isAnchorLocked = false; isGnssThrottled = false
        thermalHeadroom = 0.0; heapAllocatedMb = 0.0; stationaryDurationMs = 0L; activityType = ActivityType.UNKNOWN
        ribbons.reset()
        forensic.reset()
    }
}

/**
 * ViolationPoint: Represents a geofence or sensor violation on the map.
 */
class ViolationPoint(
    var localId: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var type: String = "", 
    var ts: Long = 0,
    var accuracy: Double = 0.0,
    var maxAccuracy: Double = 0.0,
    var isJammingCandidate: Boolean = false
) {
    private var _cachedGeoPoint: GeoPoint? = null
    fun toGeoPoint(): GeoPoint {
        val cached = _cachedGeoPoint
        if (cached != null && cached.latitude == lat && cached.longitude == lng) return cached
        return GeoPoint(lat, lng).also { _cachedGeoPoint = it }
    }
    fun copyFrom(other: ViolationPoint) {
        this.localId = other.localId; this.lat = other.lat; this.lng = other.lng; this.type = other.type
        this.ts = other.ts; this.accuracy = other.accuracy; this.maxAccuracy = other.maxAccuracy
        this.isJammingCandidate = other.isJammingCandidate
    }

    /**
     * contentEquals: Deep parity check for map violation points (R312).
     */
    fun contentEquals(other: ViolationPoint): Boolean {
        return lat == other.lat && lng == other.lng && type == other.type && 
               ts == other.ts && accuracy == other.accuracy && maxAccuracy == other.maxAccuracy &&
               isJammingCandidate == other.isJammingCandidate
    }
}

@Serializable
data class LogEntry(
    val localId: String = "", 
    val timestamp: Long, val message: String, val type: String,
    val isImportant: Boolean, val id: String = "", val viewerId: String = "",
    val count: Int = 1, val extremeValue: Double? = null,
    val durationMs: Long = 0L,
    val isSpecial: Boolean = false, 
    val specialColor: Int? = null,    
    val firstSeenTs: Long = 0L,
    val role: String = "tracker",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Double = 0.0,
    val maxAccuracy: Double = 0.0,
    val forensic: ForensicSnapshot = ForensicSnapshot(),
    val spillIdx: Int = -1,
    val gpsHardwareLock: Boolean = false,
    val tempSnapshot: Double? = null,
    val battSnapshot: Int? = null,
    val chargingSnapshot: Boolean? = null,
    val coolingSnapshot: Boolean? = null,
    val checksum: Long? = null
) {
    var snrSnapshot: Double? 
        get() = forensic.snr
        set(value) { forensic.snr = value }
    var vibeSnapshot: Double? 
        get() = forensic.vibe
        set(value) { forensic.vibe = value }
    var thermalSnapshot: Double? 
        get() = forensic.thermal
        set(value) { forensic.thermal = value }
    var heapSnapshot: Double? 
        get() = forensic.heap
        set(value) { forensic.heap = value }
    var luxSnapshot: Double?
        get() = forensic.lux
        set(value) { forensic.lux = value }
    var acousticSnapshot: Double?
        get() = forensic.acoustic
        set(value) { forensic.acoustic = value }

    // Oct.7.10 Jammer Discrimination
    var isJammingCandidate: Boolean
        get() = forensic.isJammingCandidate
        set(value) { forensic.isJammingCandidate = value }

    /**
     * contentEquals: Deep parity check to suppress redundant Logcat/UI noise (R312).
     */
    fun contentEquals(other: LogEntry): Boolean {
        return timestamp == other.timestamp && message == other.message && 
               count == other.count && durationMs == other.durationMs &&
               lat == other.lat && lng == other.lng && accuracy == other.accuracy &&
               forensic.snr == other.forensic.snr && forensic.vibe == other.forensic.vibe &&
               forensic.thermal == other.forensic.thermal && forensic.heap == other.forensic.heap &&
               forensic.lux == other.forensic.lux && forensic.acoustic == other.forensic.acoustic &&
               forensic.isSuspiciousNoise == other.forensic.isSuspiciousNoise &&
               forensic.isMemoryPressureThrottled == other.forensic.isMemoryPressureThrottled &&
               forensic.isJammingCandidate == other.forensic.isJammingCandidate &&
               coolingSnapshot == other.coolingSnapshot && checksum == other.checksum
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("localId", localId); put("timestamp", timestamp)
            put("localTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            
            val sanitizedMsg = ForensicSanitizer.sanitizeMessage(message)
            val finalMsg = ForensicSanitizer.scrubHardwareInfo(sanitizedMsg, isSpecial)
            put("message", finalMsg)
            
            put("type", type); put("isImportant", isImportant)
            put("id", SignalingConstants.getTransmissionId(id))
            put("viewer_id", SignalingConstants.getTransmissionId(viewerId))
            put("count", count); put("duration_ms", durationMs); put("is_special", isSpecial)
            put("first_seen_ts", if (firstSeenTs == 0L) timestamp else firstSeenTs); put("role", role)
            
            if (lat != 0.0) put("lat", safeDouble(lat))
            if (lng != 0.0) put("lng", safeDouble(lng))
            if (accuracy != 0.0) put("accuracy", safeDouble(accuracy))
            if (maxAccuracy != 0.0) put("max_accuracy", safeDouble(maxAccuracy))
            
            specialColor?.let { put("special_color", it) }
            extremeValue?.let { put("extreme_value", safeDouble(it)) }
            snrSnapshot?.let { put("snr_snapshot", safeDouble(it)) }
            vibeSnapshot?.let { put("vibe_snapshot", safeDouble(it)) }
            if (spillIdx != -1) put("spill_idx", spillIdx)
            if (gpsHardwareLock) put("gps_hw_lock", true)
            tempSnapshot?.let { put("temp_snapshot", safeDouble(it)) }
            battSnapshot?.let { put("batt_snapshot", it) }
            chargingSnapshot?.let { put("charging_snapshot", it) }
            coolingSnapshot?.let { put("cooling_snapshot", it) }
            thermalSnapshot?.let { put("thermal_snapshot", safeDouble(it)) }
            heapSnapshot?.let { put("heap_snapshot", safeDouble(it)) }
            luxSnapshot?.let { put("lux_snapshot", safeDouble(it)) }
            acousticSnapshot?.let { put("acoustic_snapshot", safeDouble(it)) }
            checksum?.let { put("checksum", it) }
            
            put("is_suspicious_noise", forensic.isSuspiciousNoise)
            put("is_memory_pressure_throttled", forensic.isMemoryPressureThrottled)
            put("is_jamming_candidate", forensic.isJammingCandidate)
        }
    }

    companion object {
        fun fromJSONObject(obj: JSONObject): LogEntry {
            val ts = obj.optLong("timestamp")
            val f = ForensicSnapshot(
                snr = if (obj.has("snr_snapshot")) obj.optDouble("snr_snapshot") else null,
                vibe = if (obj.has("vibe_snapshot")) obj.optDouble("vibe_snapshot") else null,
                thermal = if (obj.has("thermal_snapshot")) obj.optDouble("thermal_snapshot") else null,
                heap = if (obj.has("heap_snapshot")) obj.optDouble("heap_snapshot") else null,
                lux = if (obj.has("lux_snapshot")) obj.optDouble("lux_snapshot") else null,
                acoustic = if (obj.has("acoustic_snapshot")) obj.optDouble("acoustic_snapshot") else null,
                isSuspiciousNoise = obj.optBoolean("is_suspicious_noise", false),
                isMemoryPressureThrottled = obj.optBoolean("is_memory_pressure_throttled", false),
                isJammingCandidate = obj.optBoolean("is_jamming_candidate", false)
            )
            return LogEntry(
                localId = obj.optString("localId"), timestamp = ts, message = obj.optString("message"),
                type = obj.optString("type"), isImportant = obj.optBoolean("isImportant"),
                id = obj.optString("id"), viewerId = obj.optString("viewer_id"),
                count = obj.optInt("count", 1), durationMs = obj.optLong("duration_ms", 0L),
                isSpecial = obj.optBoolean("is_special", false),
                specialColor = if (obj.has("special_color")) obj.getInt("special_color") else null,
                firstSeenTs = if (obj.has("first_seen_ts")) obj.getLong("first_seen_ts") else ts,
                role = obj.optString("role", "tracker"),
                extremeValue = obj.optDouble("extreme_value").let { if (it.isNaN() || it.isInfinite()) null else it },
                lat = obj.optDouble("lat", 0.0), lng = obj.optDouble("lng", 0.0),
                accuracy = obj.optDouble("accuracy", 0.0), maxAccuracy = obj.optDouble("max_accuracy", 0.0),
                forensic = f,
                spillIdx = obj.optInt("spill_idx", -1), gpsHardwareLock = obj.optBoolean("gps_hw_lock", false),
                tempSnapshot = if (obj.has("temp_snapshot")) obj.optDouble("temp_snapshot") else null,
                battSnapshot = if (obj.has("batt_snapshot")) obj.optInt("batt_snapshot") else null,
                chargingSnapshot = if (obj.has("charging_snapshot")) obj.optBoolean("charging_snapshot") else null,
                coolingSnapshot = if (obj.has("cooling_snapshot")) obj.optBoolean("cooling_snapshot") else null,
                checksum = if (obj.has("checksum")) obj.optLong("checksum") else null
            )
        }
    }
}

/**
 * Dashboard Component States: Segmented for performance.
 */
@Serializable
data class DashboardConnectivityState(
    val lastSeenTs: Long = 0L,
    val watchdogOk: Boolean = true,
    val watchdogCountdownSec: Long = 0L,
    val totalUptimeMs: Long = 0L,
    val sessionMs: Long = 0L,
    val sinceConnMs: Long = 0L,
    val sinceDiscoMs: Long = 0L,
    val totalDropMs: Long = 0L,
    val maxDropMs: Long = 0L,
    val engineVersion: String = "--",
    val trackerConnIndex: Int = 0,
    val viewerConnIndex: Int = 0,
    val netInterface: String = "UNKNOWN",
    val systemPulse: Long = 0L,
    val isLocalOnline: Boolean = false,
    val isRelayConnected: Boolean = false
)

@Serializable
data class DashboardTelemetryState(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val gpsSpeedMps: Double = 0.0,
    val trackerAccuracy: Double = 0.0,
    val trackerMaxAcc: Double = 0.0,
    val viewerAccuracy: Double = 0.0,
    val viewerMaxAcc: Double = 0.0,
    val satsUsed: Int = -1,
    val satsView: Int = -1,
    val isSatsIndexWarning: Boolean = false,
    val snr: Double = 0.0,
    val distToHome: Double? = null,
    val distToViewer: Double? = null,
    val isGpsFresh: Boolean = true,
    val isTelemetryFresh: Boolean = true,
    val isLinkFresh: Boolean = true,
    override val isLocationPending: Boolean = false,
    override val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val status: SentinelStatus = SentinelStatus.VALID,
    val tamperReason: String? = null,
    val isTamperDetected: Boolean = false,
    val isUltraLongStationary: Boolean = false,
    val systemPulse: Long = 0L,
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val gpsTs: Long = 0L
) : Locatable

@Serializable
data class DashboardHealthState(
    val batteryLevel: Int = 100,
    val trackerTemp: Double = 0.0,
    val trackerMaxTemp: Double = 0.0,
    val viewerTemp: Double = 0.0,
    val viewerMaxTemp: Double = 0.0,
    val vibration: Double = 0.0,
    val heading: Double = 0.0,
    val tilt: Double = 0.0,
    val acousticDb: Double = 0.0,
    val baroAlt: Double = 0.0,
    val lux: Double = 0.0,
    val isNear: Boolean = true,
    val proximityCm: Double = -1.0,
    val proximityDebounceMs: Long = 0L,
    val rollingVibration: Double = 0.0,
    val kineticEnergy: Double = 0.0,
    val peakShock: Double = 0.0,
    val luxBaseline: Double = 0.0,
    val acousticFloorDb: Double = 0.0,
    val vibrationFloor: Double = 0.0,
    val isMicPending: Boolean = false,
    val isPowerTamper: Boolean = false,
    val violationUptimeMs: Long = 0L,
    val violationPercentage: Double = 0.0,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN",
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val trackerCurrentMa: Int = 0,
    val isBatteryLow: Boolean = false,
    val isBatteryCritical: Boolean = false,
    val cpuLoad: Double = 0.0,
    val ioWait: Double = 0.0,
    val maxIoLatency: Long = 0L,
    var isSilentFailure: Boolean = false,
    var isMaliAnomaly: Boolean = false,
    var isGnssThrottled: Boolean = false,
    var lastEnergyDeltaMa: Int = 0,
    var lastEnergyDeltaTemp: Double = 0.0,
    var lastEnergyDurationMs: Long = 0L,
    var systemPulse: Long = 0L,
    var thermalHeadroom: Double = 0.0,
    var heapAllocatedMb: Double = 0.0,
    var isSuspiciousNoise: Boolean = false,
    var isMemoryPressureThrottled: Boolean = false,
    var isJammingCandidate: Boolean = false
) : BatteryProvider {
    override val battery: Int get() = batteryLevel
    override val isCharging: Boolean get() = trackerCurrentMa < 0
}

/**
 * Monolithic DashboardState facade for legacy compatibility.
 */
@Serializable
data class DashboardState(
    val connectivity: DashboardConnectivityState = DashboardConnectivityState(),
    val telemetry: DashboardTelemetryState = DashboardTelemetryState(),
    val health: DashboardHealthState = DashboardHealthState()
) {
    val maxDropMs get() = connectivity.maxDropMs
    val lastSeenTs get() = connectivity.lastSeenTs
    val totalDropMs get() = connectivity.totalDropMs
    val watchdogOk get() = connectivity.watchdogOk
    val watchdogCountdownSec get() = connectivity.watchdogCountdownSec
    val totalUptimeMs get() = connectivity.totalUptimeMs
    val sessionMs get() = connectivity.sessionMs
    val sinceConnMs get() = connectivity.sinceConnMs
    val sinceDiscoMs get() = connectivity.sinceDiscoMs
    val engineVersion get() = connectivity.engineVersion
    val netInterface get() = connectivity.netInterface
    
    val lat get() = telemetry.lat
    val lng get() = telemetry.lng
    val gpsSpeedMps get() = telemetry.gpsSpeedMps
    val trackerAccuracy get() = telemetry.trackerAccuracy
    val trackerMaxAcc get() = telemetry.trackerMaxAcc
    val viewerAccuracy get() = telemetry.viewerAccuracy
    val viewerMaxAcc get() = telemetry.viewerMaxAcc
    val satsUsed get() = telemetry.satsUsed
    val satsView get() = telemetry.satsView
    val isSatsIndexWarning get() = telemetry.isSatsIndexWarning
    val snr get() = telemetry.snr
    val distToHome get() = telemetry.distToHome
    val distToViewer get() = telemetry.distToViewer
    val isGpsFresh get() = telemetry.isGpsFresh
    val isTelemetryFresh get() = telemetry.isTelemetryFresh
    val isLinkFresh get() = telemetry.isLinkFresh
    val isLocationPending get() = telemetry.isLocationPending
    val locationPendingReason get() = telemetry.locationPendingReason
    val trackerState get() = telemetry.trackerState
    val status get() = telemetry.status
    val tamperReason get() = telemetry.tamperReason
    val isTamperDetected get() = telemetry.isTamperDetected
    val isUltraLongStationary get() = telemetry.isUltraLongStationary
    val activityType get() = telemetry.activityType
    val gpsTs get() = telemetry.gpsTs

    val vibration get() = health.vibration
    val heading get() = health.heading
    val tilt get() = health.tilt
    val acousticDb get() = health.acousticDb
    val baroAlt get() = health.baroAlt
    val lux get() = health.lux
    val isNear get() = health.isNear
    val proximityCm get() = health.proximityCm
    val proximityDebounceMs get() = health.proximityDebounceMs
    val rollingVibration get() = health.rollingVibration
    val kineticEnergy get() = health.kineticEnergy
    val trackerMaxTemp get() = health.trackerMaxTemp
    val viewerMaxTemp get() = health.viewerMaxTemp
    val peakShock get() = health.peakShock
    val vibrationFloor get() = health.vibrationFloor
    val luxBaseline get() = health.luxBaseline
    val acousticFloorDb get() = health.acousticFloorDb
    val isMicPending get() = health.isMicPending
    val isPowerTamper get() = health.isPowerTamper
    val violationUptimeMs get() = health.violationUptimeMs
    val violationPercentage get() = health.violationPercentage
    val isPowerSaveMode get() = health.isPowerSaveMode
    val standbyBucket get() = health.standbyBucket
    val isStorageLow get() = health.isStorageLow
    val isStorageCritical get() = health.isStorageCritical
    val isBatterySteepDischarge get() = health.isBatterySteepDischarge
    val isCoolingModeActive get() = health.isCoolingModeActive
    val trackerCurrentMa get() = health.trackerCurrentMa
    val isBatteryLow get() = health.isBatteryLow
    val isBatteryCritical get() = health.isBatteryCritical
    val cpuLoad get() = health.cpuLoad
    val ioWait get() = health.ioWait
    val maxIoLatency get() = health.maxIoLatency
    val isSilentFailure get() = health.isSilentFailure
    val isMaliAnomaly get() = health.isMaliAnomaly
    val isGnssThrottled get() = health.isGnssThrottled
    val lastEnergyDeltaMa get() = health.lastEnergyDeltaMa
    val lastEnergyDeltaTemp get() = health.lastEnergyDeltaTemp
    val lastEnergyDurationMs get() = health.lastEnergyDurationMs
    val systemPulse get() = health.systemPulse
    val thermalHeadroom get() = health.thermalHeadroom
    val heapAllocatedMb get() = health.heapAllocatedMb
    val isSuspiciousNoise get() = health.isSuspiciousNoise
    val isMemoryPressureThrottled get() = health.isMemoryPressureThrottled
}

class StatsState(
    var totalConnectedMs: Long = 0L, var sessionConnectedMs: Long = 0L,
    var maxDropMs: Long = 0L, var maxDropTs: Long = 0L, var totalDropMs: Long = 0L,
    var uptimeMs: Long = 0L, var lastConnTs: Long = 0L, var lastDiscTs: Long = 0L,
    var violationUptimeMs: Long = 0L, var violationPercentage: Double = 0.0
) {
    fun copyFrom(other: StatsState) {
        this.totalConnectedMs = other.totalConnectedMs; this.sessionConnectedMs = other.sessionConnectedMs
        this.maxDropMs = other.maxDropMs; this.maxDropTs = other.maxDropTs; this.totalDropMs = other.totalDropMs
        this.uptimeMs = other.uptimeMs; this.lastConnTs = other.lastConnTs; this.lastDiscTs = lastDiscTs
        this.violationUptimeMs = other.violationUptimeMs; this.violationPercentage = other.violationPercentage
    }
    fun update(totalConnectedMs: Long, sessionConnectedMs: Long, maxDropMs: Long, maxDropTs: Long, totalDropMs: Long, uptimeMs: Long, lastConnTs: Long, lastDiscTs: Long) {
        this.totalConnectedMs = totalConnectedMs; this.sessionConnectedMs = sessionConnectedMs
        this.maxDropMs = maxDropMs; this.maxDropTs = maxDropTs; this.totalDropMs = totalDropMs
        this.uptimeMs = uptimeMs; this.lastConnTs = lastConnTs; this.lastDiscTs = lastDiscTs
    }
    fun reset() { update(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L); violationUptimeMs = 0L; violationPercentage = 0.0 }
}

class ConnectivityState(
    var isLocalOnline: Boolean = true,
    var isRelayConnected: Boolean = false,
    var isTrackerConnected: Boolean = false,
    var lastUpdateTs: Long = 0L,
    var lastRemoteActivityTs: Long = 0L,
    var connectedViewers: List<String> = emptyList(),
    var netInterface: String = "UNKNOWN"
) {
    fun copyFrom(other: ConnectivityState) {
        this.isLocalOnline = other.isLocalOnline
        this.isRelayConnected = other.isRelayConnected
        this.isTrackerConnected = other.isTrackerConnected
        this.lastUpdateTs = other.lastUpdateTs
        this.lastRemoteActivityTs = other.lastRemoteActivityTs
        this.connectedViewers = other.connectedViewers
        this.netInterface = other.netInterface
    }

    fun update(
        isLocalOnline: Boolean, isRelayConnected: Boolean, isTrackerConnected: Boolean,
        lastUpdateTs: Long, lastRemoteActivityTs: Long, connectedViewers: List<String>,
        netInterface: String
    ) {
        this.isLocalOnline = isLocalOnline
        this.isRelayConnected = isRelayConnected
        this.isTrackerConnected = isTrackerConnected
        this.lastUpdateTs = lastUpdateTs
        this.lastRemoteActivityTs = lastRemoteActivityTs
        this.connectedViewers = connectedViewers
        this.netInterface = netInterface
    }

    fun reset() {
        update(true, false, false, 0L, 0L, emptyList(), "UNKNOWN")
    }
}

class BatteryState(
    var level: Int = 100, var temp: Double = 0.0, var isCharging: Boolean = false, var isChargingStable: Boolean = false
) {
    fun copyFrom(other: BatteryState) {
        this.level = other.level; this.temp = other.temp; this.isCharging = other.isCharging; this.isChargingStable = other.isChargingStable
    }
    fun reset() { level = 100; temp = 0.0; isCharging = false; isChargingStable = false }
}

/**
 * HUD Component States: Segmented for performance on budget hardware.
 */
@Serializable
data class HudConnectivityState(
    val appMode: String? = null,
    val isInternet: Boolean = false,
    val isRelayConnected: Boolean = false,
    val isTelemetryFresh: Boolean = false,
    val isDataHealthy: Boolean = false,
    val commIndex: Int = 0,
    val remoteCommIndex: Int = 0,
    override val trackerId: String = "TRK",
    override val viewerId: String = "VIEW",
    val watchdogOk: Boolean = true,
    val rtt: Int = 0,
    val remoteSignal: Int = 0,
    val isSystemActive: Boolean = false,
    val isSafeMode: Boolean = false,
    val isStaggered: Boolean = false,
    val isGnssThrottled: Boolean = false,
    val systemPulse: Long = 0L,
    val conflatedCount: Long = 0L,
    val isConnectionAborted: Boolean = false
) : DeviceIdentity

@Serializable
data class HudTelemetryState(
    val isLocalGpsActive: Boolean = false,
    val isGpsFresh: Boolean = false,
    val speedMps: Float = 0f,
    val trackerAccuracy: Float = 0f,
    val maxTrackerAccuracy: Float = 0f,
    val viewerAccuracy: Float = 0f,
    val maxViewerAccuracy: Float = 0f,
    val satsUsed: Int = -1,
    val satsView: Int = -1,
    val viewerSatsUsed: Int = -1,
    val viewerSatsView: Int = -1,
    val distToHome: Double? = null,
    val distToViewer: Double? = null,
    val lastGpsTs: Long = 0L,
    val viewerGpsTs: Long = 0L,
    val trackerState: TrackerState = TrackerState.UNKNOWN,
    val isTrackerLocPending: Boolean = false,
    override val locationPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isViewerLocPending: Boolean = false,
    val viewerLocPendingReason: LocationPendingReason = LocationPendingReason.NONE,
    val isUltraLongStationary: Boolean = false,
    val systemPulse: Long = 0L,
    val activityType: ActivityType = ActivityType.UNKNOWN
) : Locatable {
    override val isLocationPending: Boolean get() = isTrackerLocPending
}

@Serializable
data class HudHealthState(
    override val battery: Int = 100,
    val remoteBattery: Int = -1,
    override val isCharging: Boolean = false,
    val remoteCharging: Boolean = false,
    val trackerTemp: Float = 0f,
    val viewerTemp: Float = 0f,
    val hasActiveAlarms: Boolean = false,
    val isRedScreenSuppressed: Boolean = false,
    val isRedScreenVisible: Boolean = false,
    val isAlarmSilenced: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val activeAlarms: List<AlarmInfo> = emptyList(),
    val progressPulse: Float = 0f,
    val systemPulse: Long = 0L,
    val isMaliAnomaly: Boolean = false,
    val isSuspiciousNoise: Boolean = false,
    val isMemoryPressureThrottled: Boolean = false,
    val isJammingCandidate: Boolean = false
) : BatteryProvider
