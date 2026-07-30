package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Optimized logic for processing forensic ribbons.
 * July.30.31:
 * - Issue #632: Analytical Ribbons: Recovery Markers. Integrated isRecoveryEvent 
 *   into aggregation to ensure forensic visibility of service restoration.
 * July.27.08:
 * - Issue #604: Ribbon Density & Aliasing Audit. Updated merge() to use peak-retention 
 *   (max) for kineticEnergy and sitShock to preserve forensic visibility at 7D scale.
 * July.27.07:
 * - Issue #602: SIT Timestamp Parity Logic. Integrated sitVz, sitVzTs, and sitVzRt 
 *   into aggregation to ensure peak vertical velocity forensic parity.
 */
class TelemetryAggregator {

    private val accumulators = mutableMapOf<String, MutableAggregationPoint>()
    
    companion object {
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
        var proxIdx: Double = 1.0
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
            proxIdx = min(proxIdx, cur.proxIdx)
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
            
            // Issue #604: Peak retention for forensic indices
            sitShock = max(sitShock, cur.sitShock)
            kineticEnergy = max(kineticEnergy, cur.kineticEnergy)
        }

        fun toImmutable(base: EngineConnectionPoint, isTick: Boolean): EngineConnectionPoint {
            return EngineConnectionPoint().apply {
                this.copyFrom(base)
                this.rtt = this@MutableAggregationPoint.rtt
                this.remoteSig = this@MutableAggregationPoint.remoteSig
                this.isConnected = this@MutableAggregationPoint.isConnected
                this.hasGps = this@MutableAggregationPoint.hasGps
                this.isRecoveryEvent = this@MutableAggregationPoint.isRecoveryEvent
                this.accuracy = this@MutableAggregationPoint.accuracy
                this.maxAccuracy = this@MutableAggregationPoint.maxAccuracy
                this.isBatterySteepDischarge = this@MutableAggregationPoint.isBatterySteepDischarge
                this.isCoolingModeActive = this@MutableAggregationPoint.isCoolingModeActive
                this.speed = this@MutableAggregationPoint.speed
                this.bearing = this@MutableAggregationPoint.bearing
                this.currentMa = this@MutableAggregationPoint.currentMa
                this.locationPendingReason = this@MutableAggregationPoint.locationPendingReason
                this.gpsIndex = this@MutableAggregationPoint.gpsIndex
                this.noiseIdx = this@MutableAggregationPoint.noiseIdx
                this.luxIdx = this@MutableAggregationPoint.luxIdx
                this.vibeIdx = this@MutableAggregationPoint.vibeIdx
                this.proxIdx = this@MutableAggregationPoint.proxIdx
                this.liftIdx = this@MutableAggregationPoint.liftIdx
                this.snrIdx = this@MutableAggregationPoint.snrIdx
                this.tiltIdx = this@MutableAggregationPoint.tiltIdx
                this.baroIdx = this@MutableAggregationPoint.baroIdx
                this.isSitDetected = this@MutableAggregationPoint.isSitDetected
                this.isSitActive = this@MutableAggregationPoint.isSitActive
                this.sitVz = this@MutableAggregationPoint.sitVz
                this.sitVzTs = this@MutableAggregationPoint.sitVzTs
                this.sitVzRt = this@MutableAggregationPoint.sitVzRt
                this.sitShock = this@MutableAggregationPoint.sitShock
                this.kineticEnergy = this@MutableAggregationPoint.kineticEnergy
                this.isTick = isTick
            }
        }
    }

    fun processPoint(point: EngineConnectionPoint): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val timeRef = if (point.rt > 0) point.rt else point.ts
        val totalSeconds = (timeRef / TICK_INTERVAL_MS).toInt()

        RibbonScale.entries.forEach { scale ->
            if (scale == RibbonScale.FOUR_MIN) {
                val p = EngineConnectionPoint().apply { copyFrom(point); isTick = totalSeconds % 60 == 0 }
                results.add(scale to p)
                return@forEach
            }

            val key = scale.key
            val acc = accumulators[key]
            
            if (acc == null) {
                accumulators[key] = MutableAggregationPoint().apply { reset(point) }
            } else {
                acc.merge(point)
            }

            if (totalSeconds % scale.intervalSeconds == 0) {
                val finalAcc = accumulators[key]!!
                results.add(scale to finalAcc.toImmutable(point, isScaleTick(scale, totalSeconds)))
                accumulators.remove(key)
            }
        }
        return results
    }

    fun backfillGaps(
        lastTickRt: Long,
        nowRt: Long,
        lastTickTs: Long,
        nowTs: Long,
        snrSamples: Sequence<EngineSnrSample>,
        sensorSamples: Sequence<EngineSensorSnapshot>,
        acousticFloor: Double,
        baseTemplate: EngineConnectionPoint
    ): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val fillPointFlyweight = EngineConnectionPoint()
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        var fillRt = lastTickRt + TICK_INTERVAL_MS
        var fillTs = lastTickTs + TICK_INTERVAL_MS
        var pointsGenerated = 0
        
        val snrIter = snrSamples.iterator()
        val sensorIter = sensorSamples.iterator()
        
        var nextSnr = if (snrIter.hasNext()) snrIter.next() else null
        var nextSensor = if (sensorIter.hasNext()) sensorIter.next() else null

        while (fillRt < nowRt && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (fillRt / TICK_INTERVAL_MS).toInt()
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
                isRecoveryEvent = false // Gaps are not recovery events by definition
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
            }

            results.addAll(processPoint(fillPointFlyweight))
            fillRt += TICK_INTERVAL_MS
            fillTs += TICK_INTERVAL_MS
            pointsGenerated++
        }
        return results
    }

    fun fillRealGap(
        ribbonKey: String,
        intervalSeconds: Int,
        lastTickRt: Long,
        nowRt: Long,
        lastTickTs: Long,
        nowTs: Long,
        snrSamples: Sequence<EngineSnrSample>,
        sensorSamples: Sequence<EngineSensorSnapshot>,
        acousticFloor: Double
    ): List<EngineConnectionPoint> {
        val intervalMs = intervalSeconds * TICK_INTERVAL_MS
        val maxGapMs = intervalMs * 240
        val effectiveStartRt = maxOf(lastTickRt, nowRt - maxGapMs)
        val rtToTsOffset = lastTickTs - lastTickRt

        var currentRt = alignToInterval(effectiveStartRt, intervalSeconds)
        if (currentRt <= lastTickRt) currentRt += intervalMs

        val gapPoints = mutableListOf<EngineConnectionPoint>()
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
            val resolvedProx = snapshot?.proxIdx ?: 1.0
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedTilt = snapshot?.let { (it.tilt / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedBaro = snapshot?.let { (it.lift / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedSit = snapshot?.isSitDetected ?: false
            val resolvedSitVzTs = snapshot?.sitVzTs ?: 0L
            val resolvedSitVzRt = snapshot?.sitVzRt ?: 0L
            val resolvedShock = snapshot?.sitShock ?: 0.0
            val resolvedKinetic = snapshot?.kineticEnergy ?: 0.0

            gapPoints.add(EngineConnectionPoint(
                ts = currentRt + rtToTsOffset,
                rt = currentRt,
                rtt = 0,
                remoteSig = 0,
                isConnected = false,
                isGap = true,
                isRecoveryEvent = false,
                snrIdx = resolvedSnr,
                noiseIdx = resolvedNoise,
                luxIdx = resolvedLux,
                vibeIdx = resolvedVibe,
                proxIdx = resolvedProx,
                liftIdx = resolvedLift,
                tiltIdx = resolvedTilt,
                baroIdx = resolvedBaro,
                isSitDetected = resolvedSit,
                sitVzTs = resolvedSitVzTs,
                sitVzRt = resolvedSitVzRt,
                sitShock = resolvedShock,
                kineticEnergy = resolvedKinetic,
                isTick = isScaleTick(getScaleByKey(ribbonKey), totalSeconds),
                currentMa = 0,
                locationPendingReason = LocationPendingReason.NONE
            ))
            currentRt += intervalMs
            pointsGenerated++
        }
        return gapPoints
    }

    private fun alignToInterval(timestamp: Long, intervalSeconds: Int): Long {
        val totalSec = (timestamp / TICK_INTERVAL_MS).toInt()
        val secondsToNextAlignment = (intervalSeconds - (totalSec % intervalSeconds)) % intervalSeconds
        return timestamp + (secondsToNextAlignment * TICK_INTERVAL_MS)
    }

    private fun getScaleByKey(key: String) = RibbonScale.entries.find { it.key == key } ?: RibbonScale.FOUR_MIN

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
