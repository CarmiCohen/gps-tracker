package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Pure logic for processing forensic ribbons.
 * July.1.16:
 * - Issue #510: Abandoned Chair Sit Detection. Removed sit-related fields.
 * - Issue #511: Simplify Ribbon Telemetry.
 */
class TelemetryAggregator {

    private val accumulators = mutableMapOf<String, EngineConnectionPoint>()
    
    companion object {
        private const val MAX_BACKFILL_POINTS = 1000
    }

    /**
     * Merges high-resolution points into a "Worst Case" summary for lower resolutions.
     */
    fun mergeWorstCase(acc: EngineConnectionPoint, cur: EngineConnectionPoint): EngineConnectionPoint {
        return acc.copy(
            rtt = max(acc.rtt, cur.rtt),
            remoteSig = min(acc.remoteSig, cur.remoteSig),
            isConnected = acc.isConnected && cur.isConnected,
            hasGps = acc.hasGps && cur.hasGps,
            accuracy = max(acc.accuracy, cur.accuracy),
            maxAccuracy = max(acc.maxAccuracy, cur.maxAccuracy),
            isBatterySteepDischarge = acc.isBatterySteepDischarge || cur.isBatterySteepDischarge,
            isCoolingModeActive = acc.isCoolingModeActive || cur.isCoolingModeActive,
            speed = max(acc.speed, cur.speed),
            bearing = if (cur.hasGps) cur.bearing else acc.bearing,
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

    /**
     * Processes a single telemetry slice and returns any aggregated points that reached their time interval.
     */
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

    /**
     * Generates forensic gap points to maintain ribbon continuity during throttled periods.
     */
    fun backfillGaps(
        lastTickTs: Long,
        now: Long,
        snrSamples: List<EngineSnrSample>,
        sensorSamples: List<EngineSensorSnapshot>,
        baseTemplate: EngineConnectionPoint
    ): List<Pair<RibbonScale, EngineConnectionPoint>> {
        val results = mutableListOf<Pair<RibbonScale, EngineConnectionPoint>>()
        var fillTs = lastTickTs + TICK_INTERVAL_MS
        var pointsGenerated = 0

        while (fillTs < now && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (fillTs / TICK_INTERVAL_MS).toInt()
            
            val fillPoint = baseTemplate.copy(
                ts = fillTs,
                isGap = false,
                currentMa = baseTemplate.currentMa,
                locationPendingReason = baseTemplate.locationPendingReason
            )

            results.addAll(processPoint(fillPoint))
            fillTs += TICK_INTERVAL_MS
            pointsGenerated++
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
        sensorSamples: List<EngineSensorSnapshot>
    ): List<EngineConnectionPoint> {
        val intervalMs = intervalSeconds * TICK_INTERVAL_MS
        val maxGapMs = intervalMs * 240
        val effectiveStartTs = maxOf(lastTickTs, now - maxGapMs)

        var currentTs = alignToInterval(effectiveStartTs, intervalSeconds)
        if (currentTs <= lastTickTs) currentTs += intervalMs

        val gapPoints = mutableListOf<EngineConnectionPoint>()
        var pointsGenerated = 0
        
        while (currentTs < now && pointsGenerated < MAX_BACKFILL_POINTS) {
            val totalSeconds = (currentTs / TICK_INTERVAL_MS).toInt()
            
            gapPoints.add(EngineConnectionPoint(
                ts = currentTs,
                rtt = 0,
                remoteSig = 0,
                isConnected = false,
                isGap = true,
                isTick = isScaleTick(getScaleByKey(ribbonKey), totalSeconds),
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
