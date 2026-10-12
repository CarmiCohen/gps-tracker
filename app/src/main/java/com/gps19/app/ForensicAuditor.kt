package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * ForensicAuditor: Encapsulates high-assurance hardware audits (Stability, Jitter, Sensor Rates, Energy).
 * Oct.12.18:
 * - Issue #ISSUE-OCT12.13-11: Temporal Jitter Hardening. Moved lastGnssStatusRt into RoleState 
 *   to prevent local GNSS jitter from leaking into remote viewer roles. 
 *   Added gap protection to recordGnssStatus and recordGpsFix to ignore sleep/suspension intervals (>15s).
 *   Isolated auditSensorRate to specific roles.
 * Oct.10.1 (Restoration Path):
 * - Issue #SIMP-1012-1: Forensic Retrieval Optimization. Standardized on zero-allocation 
 *   audit paths and native frequency verification.
 */
@Singleton
class ForensicAuditor @Inject constructor(
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider
) {
    private class RoleState {
        var maxGnssJitterMs = 0L
        var lastGpsFixRealtime = 0L
        var lastGnssStatusRt = 0L
        var stabilityAuditFixCount = 0
        var stabilityAuditViolationCount = 0
        var lastStabilityAuditTs = 0L

        var lastExpectedIntervalMs = 0L
        var lastIntervalChangeRt = 0L
        
        var isSensorRateAudited = false

        fun reset() {
            maxGnssJitterMs = 0L
            lastGpsFixRealtime = 0L
            lastGnssStatusRt = 0L
            stabilityAuditFixCount = 0
            stabilityAuditViolationCount = 0
            lastStabilityAuditTs = 0L
            lastExpectedIntervalMs = 0L
            lastIntervalChangeRt = 0L
            isSensorRateAudited = false
        }
    }

    private val roleStates = ConcurrentHashMap<AppRole, RoleState>().apply {
        put(AppRole.TRACKER, RoleState())
        put(AppRole.VIEWER_SELF, RoleState())
        put(AppRole.VIEWER_REMOTE, RoleState())
    }

    /**
     * recordGnssStatus: Oct.12.18 role-isolated GNSS jitter tracking.
     */
    fun recordGnssStatus(nowRt: Long, expectedIntervalMs: Long, role: AppRole) {
        val state = roleStates[role] ?: return
        synchronized(state) {
            val lastRt = state.lastGnssStatusRt
            if (lastRt > 0) {
                val interval = nowRt - lastRt
                
                // Issue #ISSUE-OCT12.13-11: Temporal Jitter.
                // Ignore gaps > 15s or 2x expected as jitter. These are temporal gaps 
                // (sleep/suspension), not GNSS hardware clock jitter.
                val gapThreshold = max(GPS_GAP_THRESHOLD_MS, expectedIntervalMs * 2)
                
                if (interval < gapThreshold) {
                    val jitter = abs(interval - expectedIntervalMs)
                    if (jitter > state.maxGnssJitterMs) {
                        state.maxGnssJitterMs = jitter
                    }
                }
            }
            state.lastGnssStatusRt = nowRt
        }
    }

    fun updateExpectedInterval(nowRt: Long, expectedIntervalMs: Long, role: AppRole) {
        val state = roleStates[role] ?: return
        synchronized(state) {
            if (expectedIntervalMs != state.lastExpectedIntervalMs) {
                if (state.lastExpectedIntervalMs != 0L) {
                    state.lastIntervalChangeRt = nowRt
                }
                state.lastExpectedIntervalMs = expectedIntervalMs
            }
        }
    }

    fun isAdaptationMuzzled(nowRt: Long, role: AppRole): Boolean {
        val state = roleStates[role] ?: return false
        val changeRt = state.lastIntervalChangeRt
        if (changeRt == 0L) return false
        return nowRt - changeRt < ADAPTATION_SETTLING_MS
    }

    fun recordGpsFix(nowRt: Long, expectedIntervalMs: Long, role: AppRole): String? {
        val state = roleStates[role] ?: return null
        return synchronized(state) {
            updateExpectedIntervalLocked(nowRt, expectedIntervalMs, state)
            val muzzled = isAdaptationMuzzledLocked(nowRt, state)
            
            var gapMessage: String? = null
            if (state.lastGpsFixRealtime > 0) {
                val gap = nowRt - state.lastGpsFixRealtime
                state.stabilityAuditFixCount++
                
                // Issue #ISSUE-OCT12.13-11: Temporal Jitter.
                val gapThreshold = expectedIntervalMs + GPS_STABILITY_GAP_THRESHOLD_MS
                val suspensionThreshold = max(GPS_GAP_THRESHOLD_MS, expectedIntervalMs * 2)
                
                if (gap > gapThreshold) {
                    if (!muzzled && gap < suspensionThreshold) {
                        state.stabilityAuditViolationCount++
                        gapMessage = "${gap}ms detected during logic pulse."
                    } else if (gap >= suspensionThreshold) {
                        Timber.d("ForensicAuditor: Stability Audit bypassed for suspension gap of ${gap}ms (Role: ${role.name})")
                    }
                }
            }
            state.lastGpsFixRealtime = nowRt
            if (state.lastStabilityAuditTs == 0L) state.lastStabilityAuditTs = nowRt
            gapMessage
        }
    }

    private fun updateExpectedIntervalLocked(nowRt: Long, expectedIntervalMs: Long, state: RoleState) {
        if (expectedIntervalMs != state.lastExpectedIntervalMs) {
            if (state.lastExpectedIntervalMs != 0L) state.lastIntervalChangeRt = nowRt
            state.lastExpectedIntervalMs = expectedIntervalMs
        }
    }

    private fun isAdaptationMuzzledLocked(nowRt: Long, state: RoleState): Boolean {
        if (state.lastIntervalChangeRt == 0L) return false
        return nowRt - state.lastIntervalChangeRt < ADAPTATION_SETTLING_MS
    }

    data class StabilityVerdict(
        val message: String,
        val isJitterViolation: Boolean,
        val isReliabilityViolation: Boolean
    )

    fun evaluateStability(nowRt: Long, role: AppRole): StabilityVerdict? {
        val state = roleStates[role] ?: return null
        val fixCount: Int; val violationCount: Int; val jitter: Long
        
        synchronized(state) {
            if (nowRt - state.lastStabilityAuditTs <= GPS_STABILITY_AUDIT_INTERVAL_MS) return null
            fixCount = state.stabilityAuditFixCount
            violationCount = state.stabilityAuditViolationCount
            jitter = state.maxGnssJitterMs
            if (fixCount == 0 && jitter == 0L) {
                state.lastStabilityAuditTs = nowRt
                return null
            }
            state.stabilityAuditFixCount = 0
            state.stabilityAuditViolationCount = 0
            state.maxGnssJitterMs = 0L
            state.lastStabilityAuditTs = nowRt
        }

        val reliability = if (fixCount > 0) 100.0 * (fixCount - violationCount) / fixCount else 100.0
        val jitterViolation = jitter > GNSS_JITTER_THRESHOLD_MS
        val reliabilityViolation = reliability < GPS_STABILITY_RELIABILITY_THRESHOLD
        
        val msg = "STABILITY AUDIT (${role.name}): Reliability ${reliability.roundToOneDecimal()}% ($violationCount gaps in $fixCount fixes), Max Jitter: ${jitter}ms"
        Timber.i(msg)

        if (reliabilityViolation || jitterViolation) {
            return StabilityVerdict(message = msg, isJitterViolation = jitterViolation, isReliabilityViolation = reliabilityViolation)
        }
        return null
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun resetGnssJitter() {
        roleStates.values.forEach { state -> synchronized(state) { state.maxGnssJitterMs = 0L; state.lastGnssStatusRt = 0L } }
    }

    fun auditSensorRate(isWarming: Boolean, role: AppRole): String? {
        if (isWarming) return null
        val hz = JdHardwareManager.getSensorAuditHz()
        if (hz <= 0.0) return null

        val state = roleStates[role] ?: return null
        return synchronized(state) {
            if (!state.isSensorRateAudited) {
                state.isSensorRateAudited = true
                "Sensor Rate Audit (R-ID 256): ${hz.toInt()} Hz (Native)"
            } else null
        }
    }

    private var revivalStartBattery: BatteryStatus? = null
    @Volatile private var revivalStartRtForFootprint = 0L

    fun captureRevivalStart(nowRt: Long) {
        synchronized(this) {
            if (revivalStartBattery == null) {
                revivalStartBattery = systemStatusProvider.getBatteryStatus()
                revivalStartRtForFootprint = nowRt
            }
        }
    }

    fun computeEnergyFootprint(nowRt: Long, consume: Boolean = true): RevivalEvent.Footprint? {
        val start: BatteryStatus; val startRt: Long
        synchronized(this) {
            start = revivalStartBattery ?: return null
            startRt = revivalStartRtForFootprint
            if (consume) { revivalStartBattery = null; revivalStartRtForFootprint = 0L }
        }
        val current = systemStatusProvider.getBatteryStatus()
        val deltaMa = current.currentMa - start.currentMa
        val deltaTemp = current.temp - start.temp
        val durationMs = nowRt - startRt
        return RevivalEvent.Footprint(deltaMa, deltaTemp, durationMs)
    }

    fun clearRevivalState() { synchronized(this) { revivalStartBattery = null; revivalStartRtForFootprint = 0L } }

    fun reset(role: AppRole? = null) {
        if (role == null) {
            roleStates.values.forEach { synchronized(it) { it.reset() } }
            resetGnssJitter(); clearRevivalState(); JdHardwareManager.resetSensorAudit()
        } else {
            roleStates[role]?.let { synchronized(it) { it.reset() } }
        }
    }

    val maxGnssJitterMs get() = roleStates.values.maxOfOrNull { state -> synchronized(state) { state.maxGnssJitterMs } } ?: 0L
}
