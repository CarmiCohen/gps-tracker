package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyntheticEventInjector: Authority for simulating physical hardware events 
 * for remote validation (Chapters 3.2, 5.1, 15.1, 16.1).
 * Oct.18.6:
 * - ISSUE-OCT18.6-F1: Synthetic Physical Event Injection. Implemented hooks 
 *   for SHAKE, GNSS_LOSS, POWER_TAMPER, and NETWORK_LOSS.
 */
@Singleton
class SyntheticEventInjector @Inject constructor(
    private val hardwareSuite: HardwareSuite,
    private val integrityMonitor: IntegrityMonitor,
    private val networkProvider: NetworkProvider,
    private val domainEventBus: DomainEventBus
) {
    private var isShakeSimulated = false
    private var isGnssLossSimulated = false
    private var isNetworkLossSimulated = false
    private var isPowerTamperSimulated = false

    fun simulateShake(active: Boolean) {
        isShakeSimulated = active
        Timber.i("Synthetic Injection: Shake simulation ${if (active) "ENABLED" else "DISABLED"}")
        domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: Physical SHAKE ${if (active) "START" else "STOP"}", active))
        
        if (active) {
            // High vibration floor to simulate shaking
            hardwareSuite.setAdaptiveVibrationFloor(2.0)
        } else {
            hardwareSuite.setAdaptiveVibrationFloor(VIBRATION_STATIONARY_THRESHOLD)
        }
    }

    fun simulateGnssLoss(active: Boolean) {
        isGnssLossSimulated = active
        Timber.i("Synthetic Injection: GNSS Loss simulation ${if (active) "ENABLED" else "DISABLED"}")
        domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: GNSS Signal Loss ${if (active) "START" else "STOP"}", active))
        
        hardwareSuite.simulateGnssLoss(active)
    }

    fun simulatePowerTamper(active: Boolean) {
        isPowerTamperSimulated = active
        Timber.i("Synthetic Injection: Power Tamper simulation ${if (active) "ENABLED" else "DISABLED"}")
        domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: Power Tamper ${if (active) "START" else "STOP"}", active))
        
        if (active) {
            integrityMonitor.onPowerDisconnected()
        } else {
            integrityMonitor.onPowerRestored()
        }
    }

    fun simulateNetworkLoss(active: Boolean) {
        isNetworkLossSimulated = active
        Timber.i("Synthetic Injection: Network Loss simulation ${if (active) "ENABLED" else "DISABLED"}")
        domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: Network Loss ${if (active) "START" else "STOP"}", active))
        
        networkProvider.simulateNetworkLoss(active)
    }

    fun reset() {
        if (isShakeSimulated) simulateShake(false)
        if (isGnssLossSimulated) simulateGnssLoss(false)
        if (isPowerTamperSimulated) simulatePowerTamper(false)
        if (isNetworkLossSimulated) simulateNetworkLoss(false)
    }
}
