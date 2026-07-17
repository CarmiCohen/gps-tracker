package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Pure logic for processing forensic ribbons.
 * v9.3.46:
 * - ANR Optimization (#092): Replaced O(N*M) backfill loops with linear O(N+M) 
 *   sliding window to prevent CPU saturation on Samsung A15 during cold-starts.
 */
class TelemetryAggregator {

    private val accumulators = mutableMapOf<String, EngineConnectionPoint>()
    
    companion object {
        private const val MAX_BACKFILL_POINTS = 1000
    }

    fun mergeWorstCase(acc: EngineConnectionPoint, cur: EngineConnectionPoint): EngineConnectionPoint {
        return acc.copy(
            rtt = max(acc.rtt, cur.rtt),
            remoteSig = min(acc.remoteSig, cur.remoteSig),
            isConnected = acc.isConnected && cur.isConnected,
            hasGps = acc.hasGps && cur.hasGps,
            gpsIndex = min(acc.gpsIndex, cur.gpsIndex),
            accuracy = max(acc.accuracy, cur.accuracy),
            maxAccuracy = max(acc.maxAccuracy, cur.maxAccuracy),
            noiseIdx = max(acc.noiseIdx, cur.noiseIdx),
            luxIdx = max(acc.luxIdx, cur.luxIdx),
            vibeIdx = max(acc.vibeIdx, cur.vibeIdx),
            proxIdx = min(acc.proxIdx, cur.proxIdx),
            liftIdx = max(acc.liftIdx, cur.liftIdx),
            snrIdx = min(acc.snrIdx, cur.snrIdx),
            tiltIdx = max(acc.tiltIdx, cur.tiltIdx),
            baroIdx = max(acc.baroIdx, cur.baroIdx),
            sitVz = if (abs(cur.sitVz) > abs(acc.sitVz)) cur.sitVz else acc.sitVz,
            sitDz = if (abs(cur.sitDz) > abs(acc.sitDz)) cur.sitDz else acc.sitDz,
            sitBaro = if (abs(cur.sitBaro) > abs(acc.sitBaro)) cur.sitBaro else acc.sitBaro,
            sitTilt = if (abs(cur.sitTilt) > abs(acc.sitTilt)) cur.sitTilt else acc.sitTilt,
            sitShock = max(acc.sitShock, cur.sitShock),
            isBatterySteepDischarge = acc.isBatterySteepDischarge || cur.isBatterySteepDischarge,
            isCoolingModeActive = acc.isCoolingModeActive || cur.isCoolingModeActive,
            speed = max(acc.speed, cur.speed),
            bearing = if (cur.hasGps) cur.bearing else acc.bearing,
            isSitDetected = acc.isSitDetected || cur.isSitDetected,
            isSitActive = acc.isSitActive || cur.isSitActive,
            currentMa = min(acc.currentMa, cur.currentMa),
            locationPendingReason = getHigherPriorityReason(acc.locationPendingReason, cur.locationPendingReason)
        )
    }

    private fun getHigherPriorityReason(r1: LocationPendingReason, r2: LocationPendingReason): LocationPendingReason {
        if (r1 == r2) return r1
        val p1 = getReasonPriority(r1)
        val p2 = getReasonPriority(r2)
        return if (p2 >= p1) r2 else r1
    }

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

    fun processPoint(point: EngineConnectionPoint): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val totalSeconds = (point.ts / TICK_INTERVAL_MS).toInt()

        RibbonScale.entries.forEach { scale ->
            if (scale == RibbonScale.FOUR_MIN) {
                results.add(scale to point.copy(isTick = totalSeconds % 60 == 0))
                return@forEach
            }

            val key = scale.key
            val acc = accumulators[key]
            
            val updated = if (acc == null) point else mergeWorstCase(acc, point)
            accumulators[key] = updated

            if (totalSeconds % scale.intervalSeconds == 0) {
                results.add(scale to updated.copy(isTick = isScaleTick(scale, totalSeconds)))
                accumulators.remove(key)
            }
        }
        return results
    }

    fun backfillGaps(
        lastTickTs: Long,
        now: Long,
        snrSamples: List<EngineSnrSample>,
        sensorSamples: List<EngineSensorSnapshot>,
        acousticFloor: Double,
        baseTemplate: EngineConnectionPoint
    ): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        var fillTs = lastTickTs + TICK_INTERVAL_MS
        var pointsGenerated = 0
        
        // v9.3.46: Linear scan optimization
        var snrIdx = 0
        var sensorIdx = 0

        while (fillTs < now && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (fillTs / TICK_INTERVAL_MS).toInt()
            val windowEnd = fillTs + TICK_INTERVAL_MS - 1
            
            // Fast-forward indices to current window
            while (snrIdx < snrSamples.size && snrSamples[snrIdx].ts < fillTs) snrIdx++
            while (sensorIdx < sensorSamples.size && sensorSamples[sensorIdx].ts < fillTs) sensorIdx++

            val resolvedSnr = if (snrIdx < snrSamples.size && snrSamples[snrIdx].ts <= windowEnd) {
                (snrSamples[snrIdx].snr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            } else baseTemplate.snrIdx
            
            val snapshot = if (sensorIdx < sensorSamples.size && sensorSamples[sensorIdx].ts <= windowEnd) sensorSamples[sensorIdx] else null
            
            val resolvedNoise = snapshot?.let { ((it.acoustic - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB) } ?: baseTemplate.noiseIdx
            val resolvedLux = snapshot?.let { (log10(it.lux + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0) } ?: baseTemplate.luxIdx
            val resolvedVibe = snapshot?.let { (it.vibe / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0) } ?: baseTemplate.vibeIdx
            val resolvedProx = snapshot?.proxIdx ?: baseTemplate.proxIdx
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0) } ?: baseTemplate.liftIdx
            val resolvedTilt = snapshot?.let { (it.tilt / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0) } ?: baseTemplate.tiltIdx
            val resolvedBaro = snapshot?.let { (it.lift / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0) } ?: baseTemplate.baroIdx
            val resolvedSit = snapshot?.isSitDetected ?: false

            val fillPoint = baseTemplate.copy(
                ts = fillTs,
                isGap = false,
                noiseIdx = resolvedNoise,
                luxIdx = resolvedLux,
                vibeIdx = resolvedVibe,
                proxIdx = resolvedProx,
                liftIdx = resolvedLift,
                snrIdx = resolvedSnr,
                tiltIdx = resolvedTilt,
                baroIdx = resolvedBaro,
                isSitDetected = resolvedSit,
                currentMa = baseTemplate.currentMa,
                locationPendingReason = baseTemplate.locationPendingReason
            )

            results.addAll(processPoint(fillPoint))
            fillTs += TICK_INTERVAL_MS
            pointsGenerated++
        }
        return results
    }

    fun fillRealGap(
        ribbonKey: String,
        intervalSeconds: Int,
        lastTickTs: Long,
        now: Long,
        snrSamples: List<EngineSnrSample>,
        sensorSamples: List<EngineSensorSnapshot>,
        acousticFloor: Double
    ): List<EngineConnectionPoint> {
        val intervalMs = intervalSeconds * TICK_INTERVAL_MS
        val maxGapMs = intervalMs * 240
        val effectiveStartTs = maxOf(lastTickTs, now - maxGapMs)

        var currentTs = alignToInterval(effectiveStartTs, intervalSeconds)
        if (currentTs <= lastTickTs) currentTs += intervalMs

        val gapPoints = mutableListOf<EngineConnectionPoint>()
        var pointsGenerated = 0
        
        // v9.3.46: Linear scan optimization
        var snrIdx = 0
        var sensorIdx = 0
        
        while (currentTs < now && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (currentTs / TICK_INTERVAL_MS).toInt()
            val windowEnd = currentTs + intervalMs - 1
            
            while (snrIdx < snrSamples.size && snrSamples[snrIdx].ts < currentTs) snrIdx++
            while (sensorIdx < sensorSamples.size && sensorSamples[sensorIdx].ts < currentTs) sensorIdx++

            val resolvedSnr = if (snrIdx < snrSamples.size && snrSamples[snrIdx].ts <= windowEnd) {
                (snrSamples[snrIdx].snr / RIBBON_SNR_SCALE_DB).coerceIn(0.0, 1.0)
            } else 0.0
            
            val snapshot = if (sensorIdx < sensorSamples.size && sensorSamples[sensorIdx].ts <= windowEnd) sensorSamples[sensorIdx] else null
            
            val resolvedNoise = snapshot?.let { ((it.acoustic - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB) } ?: 0.0
            val resolvedLux = snapshot?.let { (log10(it.lux + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedVibe = snapshot?.let { (it.vibe / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedProx = snapshot?.proxIdx ?: 1.0
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedTilt = snapshot?.let { (it.tilt / RIBBON_SIT_TILT_SCALE_DEG).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedBaro = snapshot?.let { (it.lift / RIBBON_SIT_BARO_SCALE_METERS).coerceIn(0.0, 1.0) } ?: 0.0
            val resolvedSit = snapshot?.isSitDetected ?: false

            gapPoints.add(EngineConnectionPoint(
                ts = currentTs,
                rtt = 0,
                remoteSig = 0,
                isConnected = false,
                isGap = true,
                isTick = isScaleTick(getScaleByKey(ribbonKey), totalSeconds),
                noiseIdx = resolvedNoise,
                luxIdx = resolvedLux,
                vibeIdx = resolvedVibe,
                proxIdx = resolvedProx,
                liftIdx = resolvedLift,
                snrIdx = resolvedSnr,
                tiltIdx = resolvedTilt,
                baroIdx = resolvedBaro,
                isSitDetected = resolvedSit,
                isSitActive = false,
                currentMa = 0,
                locationPendingReason = LocationPendingReason.NONE
            ))
            currentTs += intervalMs
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
