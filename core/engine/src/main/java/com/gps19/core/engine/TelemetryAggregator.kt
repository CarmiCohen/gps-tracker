package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryAggregator: Optimized logic for processing forensic ribbons.
 * Oct.2.6:
 * - Issue #1175: Real-time Only Path. Removed backfillGaps and fillRealGap 
 *   as part of the strategic simplification of the telemetry pipeline.
 * Sep.21.127:
 * - Issue #1156/1157: Telemetry Abstraction Integration.
 * Sep.13.30:
 * - Issue #1017 Hardening: Added reset().
 */
class TelemetryAggregator {

    private val scales = RibbonScale.entries
    private val accumulators = Array(scales.size) { MutableAggregationPoint() }
    private val hasData = BooleanArray(scales.size) { false }
    private val lastEmittedTick = IntArray(scales.size) { -1 }
    private val lastProcessedTs = LongArray(scales.size) { 0L }
    
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
        var cpuLoad: Double = 0.0
        var ioWait: Double = 0.0
        var maxIoLatency: Long = 0L
        var isSilentFailure: Boolean = false
        var isUltraLongStationary: Boolean = false
        var violationUptimeMs: Long = 0L

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
            cpuLoad = point.cpuLoad
            ioWait = point.ioWait
            maxIoLatency = point.maxIoLatency
            isSilentFailure = point.isSilentFailure
            isUltraLongStationary = point.isUltraLongStationary
            violationUptimeMs = point.violationUptimeMs
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
            cpuLoad = max(cpuLoad, cur.cpuLoad)
            ioWait = max(ioWait, cur.ioWait)
            maxIoLatency = max(maxIoLatency, cur.maxIoLatency)
            isSilentFailure = isSilentFailure || cur.isSilentFailure
            isUltraLongStationary = isUltraLongStationary || cur.isUltraLongStationary
            violationUptimeMs = max(violationUptimeMs, cur.violationUptimeMs)
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
            if (proxCount > 0) { this.proxIdx = proxSum / proxCount }
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
            target.cpuLoad = this.cpuLoad
            target.ioWait = this.ioWait
            target.maxIoLatency = this.maxIoLatency
            target.isSilentFailure = this.isSilentFailure
            target.isUltraLongStationary = this.isUltraLongStationary
            target.violationUptimeMs = this.violationUptimeMs
            target.isTick = isTick
        }
    }

    private companion object {
        private const val MONOTONIC_JITTER_TOLERANCE_MS = 2000L

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

    /**
     * reset: Clears all accumulators and state (R-ID 317).
     */
    fun reset() {
        for (i in scales.indices) {
            hasData[i] = false
            lastEmittedTick[i] = -1
            lastProcessedTs[i] = 0L
        }
    }

    /**
     * processPoint: Main entry for telemetry points.
     */
    fun processPoint(point: EngineConnectionPoint, onResult: (RibbonScale, EngineConnectionPoint) -> Unit) {
        val timeRef = if (point.rt > 0) point.rt else point.ts
        val totalSeconds = (timeRef / TICK_INTERVAL_MS).toInt()

        // 1. FOUR_MIN (Index 0): High-fidelity pass-through.
        val flyweight4M = resultFlyweights[0]
        flyweight4M.copyFrom(point)
        flyweight4M.isTick = totalSeconds % 60 == 0
        
        if (timeRef >= lastProcessedTs[0] - MONOTONIC_JITTER_TOLERANCE_MS) {
            onResult(scales[0], flyweight4M)
            if (timeRef > lastProcessedTs[0]) lastProcessedTs[0] = timeRef
        }

        // 2. Other scales: Aggregated.
        for (i in 1 until scales.size) {
            val scale = scales[i]
            val acc = accumulators[i]
            
            if (lastEmittedTick[i] != -1 && totalSeconds < lastEmittedTick[i]) continue

            if (totalSeconds % scale.intervalSeconds == 0) {
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
}
