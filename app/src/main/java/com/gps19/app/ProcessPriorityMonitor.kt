package com.gps19.app

import android.os.SystemClock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProcessPriorityMonitor: Abstracted worker to handle periodic background stay-alive pulses 
 * and manage priority retention across vendor lifecycles.
 * Sep.23.08:
 * - Issue #1204: Unified Hardware Lifecycle & Vendor Hardening. Abstracted stay-alive pulses 
 *   into a dedicated domain component.
 */
@Singleton
class ProcessPriorityMonitor @Inject constructor(
    private val systemMonitor: SystemMonitor
) {
    private var lastStayAlivePulseRt = 0L

    /**
     * Dispatches a stay-alive pulse to ensure the process memory priority is retained.
     */
    fun sendStayAlivePulse(forceWakeLock: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        lastStayAlivePulseRt = now
        Timber.d("ProcessPriorityMonitor: Stay-alive pulse dispatched (Force WakeLock: $forceWakeLock).")
        if (forceWakeLock) {
            systemMonitor.acquireWakeLock(force = true)
        } else {
            systemMonitor.renewWakeLock()
        }
    }

    fun getLastPulseRt(): Long = lastStayAlivePulseRt
}
