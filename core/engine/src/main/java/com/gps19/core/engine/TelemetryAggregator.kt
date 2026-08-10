package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Optimized logic for processing forensic ribbons.
 * Aug.10.23:
 * - Issue #128: Forensic Metadata Pressure Hardening. Optimized O(N) traversal 
 *   using tick-gating and deferred averaging. Fixed "Aggregation Storm" bug 
 *   where high-frequency IMU caused redundant ribbon emissions (R128).
 * Aug.08.21:
 * - Issue #125: Forensic Audit: Compression Parity Audit. Integrated 
 *   gpsHardwareLock into aggregation logic to ensure parity (R125).
 */
class TelemetryAggregator {

    private val scales = RibbonScale.entries
    private val accumulators = Array(scales.size) { MutableAggregationPoint() }
    private val hasData = BooleanArray(scales.size) { false }
    private val lastEmittedTick = IntArray(scales.size) { -1 }
    
    // Flyweight pool for results to avoid per-scale allocations
    private val resultFlyweights = Array(scales.size) { EngineConnectionPoint() }

    private class MutableAggregationPoint {
        var rtt: Int = 0
        var remoteSig: Int = 0
        var isConnected: Boolean = true
        var hasGps: Boolean = false
        var isRecoveryEvent: Boolean = false
        var accuracy: Double = 0.0
        var maxAccuracy: Double = 0.0
        var isBatterySteepDischarge: Boolean = false
        var isCoolingModeActive: Boolean = false
        var speed: Double = 0.0
        var bearing: Double = 0.0
        var currentMa: Int = 0
        var locationPendingReason: LocationPendingReason = LocationPendingReason.NONE
        var gpsIndex: Double = 0.0
        var noiseIdx: Double = 0.0
        var luxIdx: Double = 0.0
        var vibeIdx: Double = 0.0
        var proxIdx: Double = 0.0
        var proxSum: Double = 0.0
        var proxCount: Int = 0
        var liftIdx: Double = 0.0
        var snrIdx: Double = 0.0
        var tiltIdx: Double = 0.0
        var baroIdx: Double = 0.0
        var isSitDetected: Boolean = false
        var isSitActive: Boolean = false
        var sitVz: Double = 0.0
        var sitVzTs: Long = 0L
        var sitVzRt: Long = 0L
        var sitShock: Double = 0.0
        var kineticEnergy: Double = 0.0
        var gpsHardwareLock: Boolean = false

        fun reset(point: EngineConnectionPoint) {
            rtt = point.rtt
            remoteSig = point.remoteSig
            isConnected = point.isConnected
            hasGps = point.hasGps
            isRecoveryEvent = point.isRecoveryEvent
            accuracy = point.accuracy
            maxAccuracy = point.maxAccuracy
            isBatterySteepDischarge = point.isBatterySteepDischarge
            isCoolingModeActive = point.isCoolingModeActive
            speed = point.speed
            bearing = point.bearing
            currentMa = point.currentMa
            locationPendingReason = point.locationPendingReason
            gpsIndex = point.gpsIndex
            noiseIdx = point.noiseIdx
            luxIdx = point.luxIdx
            vibeIdx = point.vibeIdx
            proxIdx = point.proxIdx
            proxSum = point.proxIdx
            proxCount = 1
            liftIdx = point.liftIdx
            snrIdx = point.snrIdx
            tiltIdx = point.tiltIdx
            baroIdx = point.baroIdx
            isSitDetected = point.isSitDetected
            isSitActive = point.isSitActive
            sitVz = point.sitVz
            sitVzTs = point.sitVzTs
            sitVzRt = point.sitVzRt
            sitShock = point.sitShock
            kineticEnergy = point.kineticEnergy
            gpsHardwareLock = point.gpsHardwareLock
        }

        fun merge(cur: EngineConnectionPoint) {
            rtt = max(rtt, cur.rtt)
            remoteSig = min(remoteSig, cur.remoteSig)
            isConnected = isConnected && cur.isConnected
            hasGps = hasGps && cur.hasGps
            isRecoveryEvent = isRecoveryEvent || cur.isRecoveryEvent
            accuracy = max(accuracy, cur.accuracy)
            maxAccuracy = max(maxAccuracy, cur.maxAccuracy)
            isBatterySteepDischarge = isBatterySteepDischarge || cur.isBatterySteepDischarge
            isCoolingModeActive = isCoolingModeActive || cur.isCoolingModeActive
            speed = max(speed, cur.speed)
            if (cur.hasGps) bearing = cur.bearing
            currentMa = min(currentMa, cur.currentMa)
            locationPendingReason = getHigherPriorityReason(locationPendingReason, cur.locationPendingReason)
            gpsIndex = min(gpsIndex, cur.gpsIndex)
            noiseIdx = max(noiseIdx, cur.noiseIdx)
            luxIdx = max(luxIdx, cur.luxIdx)
            vibeIdx = max(vibeIdx, cur.vibeIdx)
            
            proxSum += cur.proxIdx
            proxCount++
            // Issue #128: Division deferred to writeTo for O(N) pressure reduction
            
            liftIdx = max(liftIdx, cur.liftIdx)
            snrIdx = min(snrIdx, cur.snrIdx)
            tiltIdx = max(tiltIdx, cur.tiltIdx)
            baroIdx = max(baroIdx, cur.baroIdx)
            isSitDetected = isSitDetected || cur.isSitDetected
            isSitActive = isSitActive || cur.isSitActive
            
            if (abs(cur.sitVz) > abs(sitVz)) {
                sitVz = cur.sitVz
                sitVzTs = cur.sitVzTs
                sitVzRt = cur.sitVzRt
            }
            sitShock = max(sitShock, cur.sitShock)
            kineticEnergy = max(kineticEnergy, cur.kineticEnergy)
            gpsHardwareLock = gpsHardwareLock || cur.gpsHardwareLock
        }

        fun writeTo(target: EngineConnectionPoint, base: EngineConnectionPoint, isTick: Boolean) {
            target.copyFrom(base)
            target.rtt = this.rtt
            target.remoteSig = this.remoteSig
            target.isConnected = this.isConnected
            target.hasGps = this.hasGps
            target.isRecoveryEvent = this.isRecoveryEvent
            target.accuracy = this.accuracy
            target.maxAccuracy = this.maxAccuracy
            target.isBatterySteepDischarge = this.isBatterySteepDischarge
            target.isCoolingModeActive = this.isCoolingModeActive
            target.speed = this.speed
            target.bearing = this.bearing
            target.currentMa = this.currentMa
            target.locationPendingReason = this.locationPendingReason
            target.gpsIndex = this.gpsIndex
            target.noiseIdx = this.noiseIdx
            target.luxIdx = this.luxIdx
            target.vibeIdx = this.vibeIdx
            
            // Issue #128: Finalize averaging on write-path
            if (proxCount > 0) {
                this.proxIdx = proxSum / proxCount
            }
            target.proxIdx = this.proxIdx
            
            target.liftIdx = this.liftIdx
            target.snrIdx = this.snrIdx
            target.tiltIdx = this.tiltIdx
            target.baroIdx = this.baroIdx
            target.isSitDetected = this.isSitDetected
            target.isSitActive = this.isSitActive
            target.sitVz = this.sitVz
            target.sitVzTs = this.sitVzTs
            target.sitVzRt = this.sitVzRt
            target.sitShock = this.sitShock
            target.kineticEnergy = this.kineticEnergy
            target.gpsHardwareLock = this.gpsHardwareLock
            target.isTick = isTick
        }
    }

    private companion object {
        private fun getReasonPriority(reason: LocationPendingReason): Int {
            return when (reason) {
                LocationPendingReason.NONE -> 0
                LocationPendingReason.GPS_GAP -> 1
                LocationPendingReason.SIGNAL_LOSS -> 2
                LocationPendingReason.GPS_STALL -> 3
                LocationPendingReason.ACOUSTIC_VIOLATION -> 4
                LocationPendingReason.JAMMER_SUSPICION -> 5
            }
        }

        private fun getHigherPriorityReason(r1: LocationPendingReason, r2: LocationPendingReason): LocationPendingReason {
            if (r1 == r2) return r1
            val p1 = getReasonPriority(r1)
            val p2 = getReasonPriority(r2)
            return if (p2 >= p1) r2 else r1
        }
    }

    fun processPoint(point: EngineConnectionPoint, onResult: (RibbonScale, EngineConnectionPoint) -> Unit) {
        val timeRef = if (point.rt > 0) point.rt else point.ts
        val totalSeconds = (timeRef / TICK_INTERVAL_MS).toInt()

        // 1. FOUR_MIN (Index 0): High-fidelity pass-through. 
        // We always emit every point for the 4M scale to preserve forensic detail.
        val flyweight4M = resultFlyweights[0]
        flyweight4M.copyFrom(point)
        flyweight4M.isTick = totalSeconds % 60 == 0
        onResult(scales[0], flyweight4M)

        // 2. Other scales: Aggregated to prevent "Aggregation Storms".
        // Issue #128: For 100Hz IMU, we merge into accumulators, but only emit at the 
        // first point of a new interval-tick. This ensures O(1) emission per interval.
        for (i in 1 until scales.size) {
            val scale = scales[i]
            val acc = accumulators[i]
            
            // Check for interval boundary
            if (totalSeconds % scale.intervalSeconds == 0) {
                // If this is the FIRST point of a new interval-tick, emit the previous aggregate
                if (lastEmittedTick[i] != totalSeconds) {
                    if (hasData[i]) {
                        val res = resultFlyweights[i]
                        acc.writeTo(res, point, isScaleTick(scale, totalSeconds))
                        onResult(scale, res)
                        hasData[i] = false
                    }
                    lastEmittedTick[i] = totalSeconds
                }
            }

            if (!hasData[i]) {
                acc.reset(point)
                hasData[i] = true
            } else {
                acc.merge(point)
            }
        }
    }

    fun backfillGaps(
        lastTickRt: Long,
        nowRt: Long,
        lastTickTs: Long,
        nowTs: Long,
        snrSamples: Sequence<EngineSnrSample>,
        sensorSamples: Sequence<EngineSensorSnapshot>,
        acousticFloor: Double,
        baseTemplate: EngineConnectionPoint,
        onResult: (RibbonScale, EngineConnectionPoint) -> Unit
    ) {
        val fillPointFlyweight = EngineConnectionPoint()
        var fillRt = lastTickRt + TICK_INTERVAL_MS
        var fillTs = lastTickTs + TICK_INTERVAL_MS
        var pointsGenerated = 0
        
        val snrIter = snrSamples.iterator()
        val sensorIter = sensorSamples.iterator()
        
        var nextSnr = if (snrIter.hasNext()) snrIter.next() else null
        var nextSensor = if (sensorIter.hasNext()) sensorIter.next() else null

        while (fillRt < nowRt && pointsGenerated < MAX_BACKFILL_POINTS) {
            val windowEndRt = fillRt + TICK_INTERVAL_MS - 1
            
            while (nextSnr != null && nextSnr.rt < fillRt) {
                nextSnr = if (snrIter.hasNext()) snrIter.next() else null
            }
            while (nextSensor != null && nextSensor.rt < fillRt) {
                nextSensor = if (sensorIter.hasNext()) sensorIter.next() else null
            }

            val resolvedSnr = if (nextSnr != null && nextSnr.rt <= windowEndRt) {
                (nextSnr.snr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            } else baseTemplate.snrIdx
            
            val snapshot = if (nextSensor != null && nextSensor.rt <= windowEndRt) nextSensor else null
            
            val resolvedNoise = snapshot?.let { ((it.acoustic - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB) } ?: baseTemplate.noiseIdx
            val resolvedLux = snapshot?.let { (log10(it.lux + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0) } ?: baseTemplate.luxIdx
            val resolvedVibe = snapshot?.let { (it.vibe / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0) } ?: baseTemplate.vibeIdx
            val resolvedProx = snapshot?.proxIdx ?: baseTemplate.proxIdx
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0) } ?: baseTemplate.liftIdx
            val resolvedTilt = snapshot?.let { (it.tilt / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0) } ?: baseTemplate.tiltIdx
            val resolvedBaro = snapshot?.let { (it.lift / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0) } ?: baseTemplate.baroIdx
            val resolvedSit = snapshot?.isSitDetected ?: false
            val resolvedSitVzTs = snapshot?.sitVzTs ?: baseTemplate.sitVzTs
            val resolvedSitVzRt = snapshot?.sitVzRt ?: baseTemplate.sitVzRt
            val resolvedShock = snapshot?.sitShock ?: baseTemplate.sitShock
            val resolvedKinetic = snapshot?.kineticEnergy ?: baseTemplate.kineticEnergy

            fillPointFlyweight.apply {
                copyFrom(baseTemplate)
                ts = fillTs
                rt = fillRt
                isGap = false
                isRecoveryEvent = false 
                snrIdx = resolvedSnr
                noiseIdx = resolvedNoise
                luxIdx = resolvedLux
                vibeIdx = resolvedVibe
                proxIdx = resolvedProx
                liftIdx = resolvedLift
                tiltIdx = resolvedTilt
                baroIdx = resolvedBaro
                isSitDetected = resolvedSit
                sitVzTs = resolvedSitVzTs
                sitVzRt = resolvedSitVzRt
                sitShock = resolvedShock
                kineticEnergy = resolvedKinetic
                gpsHardwareLock = baseTemplate.gpsHardwareLock
            }

            processPoint(fillPointFlyweight, onResult)
            fillRt += TICK_INTERVAL_MS
            fillTs += TICK_INTERVAL_MS
            pointsGenerated++
        }
    }

    fun fillRealGap(
        ribbonScale: RibbonScale,
        lastTickRt: Long,
        nowRt: Long,
        lastTickTs: Long,
        snrSamples: Sequence<EngineSnrSample>,
        sensorSamples: Sequence<EngineSensorSnapshot>,
        acousticFloor: Double,
        onResult: (EngineConnectionPoint) -> Unit
    ) {
        val intervalMs = ribbonScale.intervalSeconds * TICK_INTERVAL_MS
        val maxGapMs = intervalMs * 240
        val effectiveStartRt = maxOf(lastTickRt, nowRt - maxGapMs)
        val rtToTsOffset = lastTickTs - lastTickRt

        var currentRt = alignToInterval(effectiveStartRt, ribbonScale.intervalSeconds)
        if (currentRt <= lastTickRt) currentRt += intervalMs

        val flyweight = EngineConnectionPoint()
        var pointsGenerated = 0
        
        val snrIter = snrSamples.iterator()
        val sensorIter = sensorSamples.iterator()
        
        var nextSnr = if (snrIter.hasNext()) snrIter.next() else null
        var nextSensor = if (sensorIter.hasNext()) sensorIter.next() else null
        
        while (currentRt < nowRt && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (currentRt / TICK_INTERVAL_MS).toInt()
            val windowEndRt = currentRt + intervalMs - 1
            
            while (nextSnr != null && nextSnr.rt < currentRt) {
                nextSnr = if (snrIter.hasNext()) snrIter.next() else null
            }
            while (nextSensor != null && nextSensor.rt < currentRt) {
                nextSensor = if (sensorIter.hasNext()) sensorIter.next() else null
            }

            val resolvedSnr = if (nextSnr != null && nextSnr.rt <= windowEndRt) {
                (nextSnr.snr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            } else 0.0
            
            val snapshot = if (nextSensor != null && nextSensor.rt <= windowEndRt) nextSensor else null
            
            val resolvedNoise = snapshot?.let { ((it.acoustic - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB) } ?: 0.0
            val resolvedLux = snapshot?.let { (log10(it.lux + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedVibe = snapshot?.let { (it.vibe / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedProx = snapshot?.proxIdx ?: 0.0
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedTilt = snapshot?.let { (it.tilt / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedBaro = snapshot?.let { (it.lift / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedSit = snapshot?.isSitDetected ?: false
            val resolvedSitVzTs = snapshot?.sitVzTs ?: 0L
            val resolvedSitVzRt = snapshot?.sitVzRt ?: 0L
            val resolvedShock = snapshot?.sitShock ?: 0.0
            val resolvedKinetic = snapshot?.kineticEnergy ?: 0.0

            flyweight.apply {
                ts = currentRt + rtToTsOffset
                rt = currentRt
                rtt = 0
                remoteSig = 0
                isConnected = false
                isGap = true
                isRecoveryEvent = false
                snrIdx = resolvedSnr
                noiseIdx = resolvedNoise
                luxIdx = resolvedLux
                vibeIdx = resolvedVibe
                proxIdx = resolvedProx
                liftIdx = resolvedLift
                tiltIdx = resolvedTilt
                baroIdx = resolvedBaro
                isSitDetected = resolvedSit
                sitVzTs = resolvedSitVzTs
                sitVzRt = resolvedSitVzRt
                sitShock = resolvedShock
                kineticEnergy = resolvedKinetic
                isTick = isScaleTick(ribbonScale, totalSeconds)
                currentMa = 0
                locationPendingReason = LocationPendingReason.NONE
                gpsHardwareLock = false
            }
            onResult(flyweight)
            currentRt += intervalMs
            pointsGenerated++
        }
    }

    private fun alignToInterval(timestamp: Long, intervalSeconds: Int): Long {
        val totalSec = (timestamp / TICK_INTERVAL_MS).toInt()
        val secondsToNextAlignment = (intervalSeconds - (totalSec % intervalSeconds)) % intervalSeconds
        return timestamp + (secondsToNextAlignment * TICK_INTERVAL_MS)
    }

    private fun isScaleTick(scale: RibbonScale, totalSeconds: Int): Boolean {
        return when (scale) {
            RibbonScale.FOUR_MIN -> totalSeconds % 60 == 0
            RibbonScale.SIXTEEN_MIN -> totalSeconds % 240 == 0
            RibbonScale.ONE_HOUR -> totalSeconds % 900 == 0
            RibbonScale.FOUR_HOUR -> totalSeconds % 3600 == 0
            RibbonScale.TWENTY_FOUR_HOUR -> totalSeconds % 21600 == 0
            RibbonScale.SEVEN_DAY -> totalSeconds % 86400 == 0
        }
    }
}
