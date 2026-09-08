package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.round

/**
 * ForensicAuditor: Encapsulates high-assurance hardware audits (Stability, Jitter, Sensor Rates, Energy).
 * Sep.08.11:
 * - Issue #936: Forensic Auditor Consolidation (Idea #3). Consolidated Stability 
 *   Audit logic (Reliability % / Jitter) from Tracker/Viewer services (R-ID 280).
 * Sep.06.17:
 * - Issue #922 (Part B): Extracted from HardwareProvider to restore SRP.
 * - R-ID 256: Sensor Rate Auditing.
 * - R-ID 259: Energy Footprint Verdicts.
 */
@Singleton
class ForensicAuditor @Inject constructor(
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider
) {
    // --- GNSS Jitter & Stability Monitoring ---
    var maxGnssJitterMs = 0L; private set
    private var lastGnssStatusRt = 0L
    
    var lastGpsFixRealtime = 0L; private set
    private var stabilityAuditFixCount = 0
    private var stabilityAuditViolationCount = 0
    private var lastStabilityAuditTs = 0L

    fun recordGnssStatus(nowRt: Long) {
        if (lastGnssStatusRt > 0) {
            val interval = nowRt - lastGnssStatusRt
            val jitter = abs(interval - GNSS_EXPECTED_INTERVAL_MS)
            if (jitter > maxGnssJitterMs) {
                maxGnssJitterMs = jitter
            }
        }
        lastGnssStatusRt = nowRt
    }

    /**
     * Records a GPS fix and returns a gap message if a stability violation is detected.
     */
    fun recordGpsFix(nowRt: Long, expectedIntervalMs: Long): String? {
        var gapMessage: String? = null
        if (lastGpsFixRealtime > 0) {
            val gap = nowRt - lastGpsFixRealtime
            stabilityAuditFixCount++
            if (gap > expectedIntervalMs + GPS_STABILITY_GAP_THRESHOLD_MS) {
                stabilityAuditViolationCount++
                gapMessage = "${gap}ms detected during logic pulse."
            }
        }
        lastGpsFixRealtime = nowRt
        if (lastStabilityAuditTs == 0L) lastStabilityAuditTs = nowRt
        return gapMessage
    }

    data class StabilityVerdict(
        val message: String,
        val isJitterViolation: Boolean,
        val isReliabilityViolation: Boolean
    )

    /**
     * Evaluates stability over the audit interval.
     */
    fun evaluateStability(nowRt: Long, roleTag: String): StabilityVerdict? {
        if (nowRt - lastStabilityAuditTs <= GPS_STABILITY_AUDIT_INTERVAL_MS) return null
        
        val fixCount = stabilityAuditFixCount
        val violationCount = stabilityAuditViolationCount
        val jitter = maxGnssJitterMs
        
        if (fixCount == 0 && jitter == 0L) {
            lastStabilityAuditTs = nowRt
            return null
        }

        val reliability = if (fixCount > 0) 100.0 * (fixCount - violationCount) / fixCount else 100.0
        val jitterViolation = jitter > GNSS_JITTER_THRESHOLD_MS
        val reliabilityViolation = reliability < GPS_STABILITY_RELIABILITY_THRESHOLD
        
        var verdict: StabilityVerdict? = null
        
        if (reliabilityViolation || jitterViolation) {
            val msg = StringBuilder("STABILITY AUDIT ($roleTag): ")
            if (reliabilityViolation) {
                msg.append("Reliability ${reliability.roundToOneDecimal()}% ($violationCount gaps in $fixCount fixes). ")
            }
            if (jitterViolation) {
                msg.append("GNSS Jitter: ${jitter}ms (Hardware Instability).")
            }
            verdict = StabilityVerdict(
                message = msg.toString().trim(),
                isJitterViolation = jitterViolation,
                isReliabilityViolation = reliabilityViolation
            )
        }

        // Reset for next window
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        maxGnssJitterMs = 0L
        lastStabilityAuditTs = nowRt
        
        return verdict
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun resetGnssJitter() {
        maxGnssJitterMs = 0L
        lastGnssStatusRt = 0L
    }

    // --- Sensor Rate Audit (R-ID 256) ---
    private var accelEventCount = 0
    private var accelAuditStartRt = 0L
    private var isSensorRateAudited = false

    fun auditSensorRate(nowRt: Long, isWarming: Boolean): String? {
        if (isSensorRateAudited || isWarming) return null
        
        if (accelAuditStartRt == 0L) {
            accelAuditStartRt = nowRt
        }
        
        accelEventCount++
        
        if (nowRt - accelAuditStartRt >= 1000L) {
            val durationSec = (nowRt - accelAuditStartRt) / 1000.0
            val hz = accelEventCount.toDouble() / durationSec
            val isEffective = hz > 200.0
            isSensorRateAudited = true
            val msg = "Sensor Rate Audit (R-ID 256): ${hz.toInt()} Hz. Efficacy: $isEffective"
            Timber.i("ForensicAuditor: $msg")
            return msg
        }
        return null
    }

    // --- Energy Footprint Snapshot (R-ID 259) ---
    private var revivalStartBattery: BatteryStatus? = null
    private var revivalStartRtForFootprint = 0L

    fun captureRevivalStart(nowRt: Long) {
        if (revivalStartBattery == null) {
            revivalStartBattery = systemStatusProvider.getBatteryStatus()
            revivalStartRtForFootprint = nowRt
        }
    }

    fun computeEnergyFootprint(nowRt: Long): HardwareProvider.RevivalEvent.Footprint? {
        val start = revivalStartBattery ?: return null
        val startRt = revivalStartRtForFootprint
        val current = systemStatusProvider.getBatteryStatus()
        
        val deltaMa = current.currentMa - start.currentMa
        val deltaTemp = current.temp - start.temp
        val durationMs = nowRt - startRt
        
        Timber.i("ForensicAuditor: Energy Footprint Verdict (R-ID 259): Delta mA: $deltaMa, Delta Temp: $deltaTemp°C, Duration: ${durationMs}ms")
        
        revivalStartBattery = null
        revivalStartRtForFootprint = 0L
        
        return HardwareProvider.RevivalEvent.Footprint(deltaMa, deltaTemp, durationMs)
    }

    fun clearRevivalState() {
        revivalStartBattery = null
        revivalStartRtForFootprint = 0L
    }

    fun reset() {
        resetGnssJitter()
        lastGpsFixRealtime = 0L
        stabilityAuditFixCount = 0
        stabilityAuditViolationCount = 0
        lastStabilityAuditTs = 0L
        accelEventCount = 0
        accelAuditStartRt = 0L
        isSensorRateAudited = false
        clearRevivalState()
    }
}
