package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.round

/**
 * ForensicAuditor: Encapsulates high-assurance hardware audits (Stability, Jitter, Sensor Rates, Energy).
 * Sep.20.00:
 * - Issue #1120: Resolved Inconsistent Jitter Audit during Adaptive GNSS Throttling.
 *   Updated recordGnssStatus to accept dynamic expected interval to prevent false jitter alerts.
 * Sep.19.08:
 * - Issue #1113: Resolved Singleton State Collision in Multi-Role Tick.
 *   Implemented role-based state tracking for stability and sensor audits.
 * - Issue #1119: Resolved Shared Sensor Rate Audit Flag Persistence.
 *   Each role now maintains its own sensor rate audit state.
 */
@Singleton
class ForensicAuditor @Inject constructor(
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider
) {
    private class RoleState {
        var maxGnssJitterMs = 0L
        var lastGpsFixRealtime = 0L
        var stabilityAuditFixCount = 0
        var stabilityAuditViolationCount = 0
        var lastStabilityAuditTs = 0L

        var lastExpectedIntervalMs = 0L
        var lastIntervalChangeRt = 0L
        
        var accelEventCount = 0
        var accelAuditStartRt = 0L
        var isSensorRateAudited = false

        fun reset() {
            maxGnssJitterMs = 0L
            lastGpsFixRealtime = 0L
            stabilityAuditFixCount = 0
            stabilityAuditViolationCount = 0
            lastStabilityAuditTs = 0L
            lastExpectedIntervalMs = 0L
            lastIntervalChangeRt = 0L
            accelEventCount = 0
            accelAuditStartRt = 0L
            isSensorRateAudited = false
        }
    }

    private val roleStates = ConcurrentHashMap<String, RoleState>().apply {
        put("T", RoleState())
        put("V", RoleState())
    }

    // Jitter source tracking
    private var lastGnssStatusRt = 0L

    /**
     * Records GNSS status and tracks jitter relative to the expected interval.
     * Issue #1120: Updated to accept dynamic expectedIntervalMs to handle throttling.
     */
    fun recordGnssStatus(nowRt: Long, expectedIntervalMs: Long) {
        if (lastGnssStatusRt > 0) {
            val interval = nowRt - lastGnssStatusRt
            val jitter = abs(interval - expectedIntervalMs)
            roleStates.values.forEach { state ->
                if (jitter > state.maxGnssJitterMs) {
                    state.maxGnssJitterMs = jitter
                }
            }
        }
        lastGnssStatusRt = nowRt
    }

    /**
     * Updates the expected polling interval and tracks transitions for muzzling.
     */
    fun updateExpectedInterval(nowRt: Long, expectedIntervalMs: Long, roleTag: String) {
        val state = roleStates[roleTag] ?: return
        if (expectedIntervalMs != state.lastExpectedIntervalMs) {
            if (state.lastExpectedIntervalMs != 0L) {
                state.lastIntervalChangeRt = nowRt
            }
            state.lastExpectedIntervalMs = expectedIntervalMs
        }
    }

    /**
     * Returns true if the system is currently in a stability muzzling window (adaptation).
     */
    fun isAdaptationMuzzled(nowRt: Long, roleTag: String): Boolean {
        val state = roleStates[roleTag] ?: return false
        if (state.lastIntervalChangeRt == 0L) return false
        return nowRt - state.lastIntervalChangeRt < ADAPTATION_SETTLING_MS
    }

    /**
     * Records a GPS fix and returns a gap message if a stability violation is detected.
     */
    fun recordGpsFix(nowRt: Long, expectedIntervalMs: Long, roleTag: String): String? {
        updateExpectedInterval(nowRt, expectedIntervalMs, roleTag)
        val state = roleStates[roleTag] ?: return null
        val isMuzzled = isAdaptationMuzzled(nowRt, roleTag)
        
        var gapMessage: String? = null
        if (state.lastGpsFixRealtime > 0) {
            val gap = nowRt - state.lastGpsFixRealtime
            state.stabilityAuditFixCount++
            if (gap > expectedIntervalMs + GPS_STABILITY_GAP_THRESHOLD_MS) {
                if (!isMuzzled) {
                    state.stabilityAuditViolationCount++
                    gapMessage = "${gap}ms detected during logic pulse."
                } else {
                    Timber.d("ForensicAuditor: Stability gap of ${gap}ms muzzled (Adaptation $roleTag).")
                }
            }
        }
        state.lastGpsFixRealtime = nowRt
        if (state.lastStabilityAuditTs == 0L) state.lastStabilityAuditTs = nowRt
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
        val state = roleStates[roleTag] ?: return null
        if (nowRt - state.lastStabilityAuditTs <= GPS_STABILITY_AUDIT_INTERVAL_MS) return null
        
        val fixCount = state.stabilityAuditFixCount
        val violationCount = state.stabilityAuditViolationCount
        val jitter = state.maxGnssJitterMs
        
        if (fixCount == 0 && jitter == 0L) {
            state.lastStabilityAuditTs = nowRt
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
        state.stabilityAuditFixCount = 0
        state.stabilityAuditViolationCount = 0
        state.maxGnssJitterMs = 0L
        state.lastStabilityAuditTs = nowRt
        
        return verdict
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun resetGnssJitter() {
        roleStates.values.forEach { it.maxGnssJitterMs = 0L }
        lastGnssStatusRt = 0L
    }

    /**
     * Audits sensor rate and returns messages for roles that just completed their audit.
     */
    fun auditSensorRate(nowRt: Long, isWarming: Boolean): List<Pair<String, String>> {
        if (isWarming) return emptyList()
        val results = mutableListOf<Pair<String, String>>()
        
        roleStates.forEach { (role, state) ->
            if (state.isSensorRateAudited) return@forEach
            
            if (state.accelAuditStartRt == 0L) {
                state.accelAuditStartRt = nowRt
            }
            
            state.accelEventCount++
            
            if (nowRt - state.accelAuditStartRt >= 1000L) {
                val durationSec = (nowRt - state.accelAuditStartRt) / 1000.0
                val hz = state.accelEventCount.toDouble() / durationSec
                val isEffective = hz > 200.0
                state.isSensorRateAudited = true
                val msg = "Sensor Rate Audit (R-ID 256): ${hz.toInt()} Hz. Efficacy: $isEffective"
                Timber.i("ForensicAuditor: [$role] $msg")
                results.add(role to msg)
            }
        }
        return results
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

    fun computeEnergyFootprint(nowRt: Long, consume: Boolean = true): HardwareSuite.RevivalEvent.Footprint? {
        val start = revivalStartBattery ?: return null
        val startRt = revivalStartRtForFootprint
        val current = systemStatusProvider.getBatteryStatus()
        
        val deltaMa = current.currentMa - start.currentMa
        val deltaTemp = current.temp - start.temp
        val durationMs = nowRt - startRt
        
        Timber.i("ForensicAuditor: Energy Footprint Verdict (R-ID 259): Delta mA: $deltaMa, Delta Temp: $deltaTemp°C, Duration: ${durationMs}ms")
        
        if (consume) {
            revivalStartBattery = null
            revivalStartRtForFootprint = 0L
        }
        
        return HardwareSuite.RevivalEvent.Footprint(deltaMa, deltaTemp, durationMs)
    }

    fun clearRevivalState() {
        revivalStartBattery = null
        revivalStartRtForFootprint = 0L
    }

    fun reset(roleTag: String? = null) {
        if (roleTag == null) {
            roleStates.values.forEach { it.reset() }
            resetGnssJitter()
            clearRevivalState()
        } else {
            roleStates[roleTag]?.reset()
        }
    }

    val maxGnssJitterMs get() = roleStates.values.maxOfOrNull { it.maxGnssJitterMs } ?: 0L

    fun getLastGpsFixRealtime(roleTag: String): Long = roleStates[roleTag]?.lastGpsFixRealtime ?: 0L
}
