package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Pure logic for processing forensic ribbons.
 * v8.9.5:
 * - Issue 192: Added currentMa and SIT forensic fields to mergeWorstCase for absolute parity.
 * v8.8.21: Extracted from HistoryManager to ensure cross-platform forensic integrity.
 */
class TelemetryAggregator {

    private val accumulators = mutableMapOf<String, EngineConnectionPoint>()

    /**
     * Merges high-resolution points into a "Worst Case" summary for lower resolutions.
     * Logic: Take the MAX of negative metrics (RTT, Noise, Vibe, Current) and MIN of positive (Signal, Accuracy).
     */
    fun mergeWorstCase(acc: EngineConnectionPoint, cur: EngineConnectionPoint): EngineConnectionPoint {
        return acc.copy(
            rtt = max(acc.rtt, cur.rtt),
            remoteSig = min(acc.remoteSig, cur.remoteSig),
            isConnected = acc.isConnected && cur.isConnected,
            hasGps = acc.hasGps && cur.hasGps,
            gpsIndex = min(acc.gpsIndex, cur.gpsIndex),
            noiseIdx = max(acc.noiseIdx, cur.noiseIdx),
            luxIdx = max(acc.luxIdx, cur.luxIdx),
            vibeIdx = max(acc.vibeIdx, cur.vibeIdx),
            proxIdx = min(acc.proxIdx, cur.proxIdx),
            liftIdx = max(acc.liftIdx, cur.liftIdx),
            snrIdx = min(acc.snrIdx, cur.snrIdx),
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
            currentMa = min(acc.currentMa, cur.currentMa) // Hardened for power forensics (highest discharge)
        )
    }

    /**
     * Processes a single telemetry slice and returns any aggregated points that reached their time interval.
     */
    fun processPoint(point: EngineConnectionPoint): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        val totalSeconds = (point.ts / 1000).toInt()

        RibbonScale.values().forEach { scale ->
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

    /**
     * Generates forensic gap points to maintain ribbon continuity during throttled periods.
     */
    fun backfillGaps(
        lastTickTs: Long,
        now: Long,
        snrSamples: List<EngineSnrSample>,
        sensorSamples: List<EngineSensorSnapshot>,
        acousticFloor: Double,
        baseTemplate: EngineConnectionPoint
    ): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        var fillTs = lastTickTs + 1000L

        while (fillTs < now) {
            val totalSeconds = (fillTs / 1000).toInt()
            
            val resolvedSnr = snrSamples.find { it.ts in fillTs..(fillTs + 999) }?.snr?.let { (it / RIBBON_SNR_SCALE_DB).coerceIn(0f, 1f) } ?: baseTemplate.snrIdx
            val snapshot = sensorSamples.find { it.ts in fillTs..(fillTs + 999) }
            
            val resolvedNoise = snapshot?.let { ((it.acoustic - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB).toFloat() } ?: baseTemplate.noiseIdx
            val resolvedLux = snapshot?.let { (log10(it.lux.toDouble() + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0).toFloat() } ?: baseTemplate.luxIdx
            val resolvedVibe = snapshot?.let { (it.vibe / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0).toFloat() } ?: baseTemplate.vibeIdx
            val resolvedProx = snapshot?.proxIdx ?: baseTemplate.proxIdx
            val resolvedLift = snapshot?.let { (it.lift / RIBBON_LIFT_SCALE_METERS).coerceIn(0f, 1f) } ?: baseTemplate.liftIdx
            val resolvedSit = snapshot?.isSitDetected ?: baseTemplate.isSitDetected

            val fillPoint = baseTemplate.copy(
                ts = fillTs,
                isGap = false,
                noiseIdx = resolvedNoise,
                luxIdx = resolvedLux,
                vibeIdx = resolvedVibe,
                proxIdx = resolvedProx,
                liftIdx = resolvedLift,
                snrIdx = resolvedSnr,
                isSitDetected = resolvedSit,
                currentMa = baseTemplate.currentMa
            )

            results.addAll(processPoint(fillPoint))
            fillTs += 1000L
        }
        return results
    }

    /**
     * Specifically handles large blackouts by generating minimal "Gap" markers.
     */
    fun fillRealGap(
        ribbonKey: String,
        intervalSeconds: Int,
        lastTickTs: Long,
        now: Long,
        snrSamples: List<EngineSnrSample>,
        sensorSamples: List<EngineSensorSnapshot>,
        acousticFloor: Double
    ): List<EngineConnectionPoint> {
        val intervalMs = intervalSeconds * 1000L
        val maxGapMs = intervalMs * 240
        val effectiveStartTs = maxOf(lastTickTs, now - maxGapMs)

        var currentTs = alignToInterval(effectiveStartTs, intervalSeconds)
        if (currentTs <= lastTickTs) currentTs += intervalMs

        val gapPoints = mutableListOf<EngineConnectionPoint>()
        while (currentTs < now) {
            val totalSeconds = (currentTs / 1000).toInt()
            
            val samplesInInterval = snrSamples.filter { it.ts in currentTs..(currentTs + intervalMs - 1) }
            val resolvedSnr = if (samplesInInterval.isNotEmpty()) {
                samplesInInterval.minOf { it.snr }.let { (it / RIBBON_SNR_SCALE_DB).coerceIn(0f, 1f) }
            } else 0f
            
            val snapshotsInInterval = sensorSamples.filter { it.ts in currentTs..(currentTs + intervalMs - 1) }
            val resolvedNoise = if (snapshotsInInterval.isNotEmpty()) {
                snapshotsInInterval.maxOf { it.acoustic }.let { ((it - acousticFloor).coerceIn(0.0, RIBBON_NOISE_SCALE_DB) / RIBBON_NOISE_SCALE_DB).toFloat() }
            } else 0f
            val resolvedLux = if (snapshotsInInterval.isNotEmpty()) {
                snapshotsInInterval.maxOf { it.lux }.let { (log10(it.toDouble() + 1.0) / RIBBON_LUX_LOG_SCALE).coerceIn(0.0, 1.0).toFloat() }
            } else 0f
            val resolvedVibe = if (snapshotsInInterval.isNotEmpty()) {
                snapshotsInInterval.maxOf { it.vibe }.let { (it / RIBBON_VIBRATION_SCALE_G).coerceIn(0.0, 1.0).toFloat() }
            } else 0f
            val resolvedProx = if (snapshotsInInterval.isNotEmpty()) snapshotsInInterval.minOf { it.proxIdx } else 1f
            val resolvedLift = if (snapshotsInInterval.isNotEmpty()) {
                snapshotsInInterval.maxOf { it.lift }.let { (it / RIBBON_LIFT_SCALE_METERS).coerceIn(0f, 1f) }
            } else 0f
            val resolvedSit = snapshotsInInterval.any { it.isSitDetected }

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
                isSitDetected = resolvedSit,
                isSitActive = false,
                currentMa = 0
            ))
            currentTs += intervalMs
        }
        return gapPoints
    }

    private fun alignToInterval(timestamp: Long, intervalSeconds: Int): Long {
        val totalSec = (timestamp / 1000).toInt()
        val secondsToNextAlignment = (intervalSeconds - (totalSec % intervalSeconds)) % intervalSeconds
        return timestamp + (secondsToNextAlignment * 1000L)
    }

    private fun getScaleByKey(key: String) = RibbonScale.values().find { it.key == key } ?: RibbonScale.FOUR_MIN

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
