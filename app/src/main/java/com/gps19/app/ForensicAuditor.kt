package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * ForensicAuditor: Encapsulates high-assurance hardware audits (Jitter, Sensor Rates, Energy).
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
    // --- GNSS Jitter Monitoring ---
    var maxGnssJitterMs = 0L; private set
    private var lastGnssStatusRt = 0L

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
        accelEventCount = 0
        accelAuditStartRt = 0L
        isSensorRateAudited = false
        clearRevivalState()
    }
}
