package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyntheticEventInjector: Authority for simulating physical hardware events 
 * for remote validation (Chapters 3.2, 5.1, 15.1, 16.1).
 * Oct.18.6:
 * - ISSUE-OCT18.6-F1: Synthetic Physical Event Injection. Implemented hooks 
 *   for SHAKE, GNSS_LOSS, POWER_TAMPER, and NETWORK_LOSS.
 * Oct.18.25:
 * - Added simulatePeerPresence to simulate a virtual peer (Tracker/Viewer).
 */
@Singleton
class SyntheticEventInjector @Inject constructor(
    private val hardwareSuite: HardwareSuite,
    private val integrityMonitor: IntegrityMonitor,
    private val networkProvider: NetworkProvider,
    private val domainEventBus: DomainEventBus,
    private val timeProvider: TimeProvider
) {
    private var isShakeSimulated = false
    private var isGnssLossSimulated = false
    private var isNetworkLossSimulated = false
    private var isPowerTamperSimulated = false
    private var isPeerSimulated = false
    private var simulationJob: Job? = null

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

    fun simulatePeerPresence(active: Boolean, isTrackerMode: Boolean, scope: CoroutineScope) {
        isPeerSimulated = active
        simulationJob?.cancel()
        
        if (active) {
            Timber.i("Synthetic Injection: Peer Presence simulation ENABLED (Role: ${if (isTrackerMode) "TRK" else "VWR"})")
            domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: Virtual Peer Presence START", true))
            
            simulationJob = scope.launch(Dispatchers.Default) {
                while (isActive) {
                    val peerId = if (isTrackerMode) "Simulated_Viewer" else "Simulated_Tracker"
                    domainEventBus.emit(ConnectivityEvent.PeerPulse(peerId))
                    
                    if (!isTrackerMode) {
                        // If we are a Viewer, simulate incoming Tracker telemetry
                        val mockUpdate = LocationUpdate().apply {
                            kinetic.apply {
                                lat = 32.0853 + (Math.random() - 0.5) * 0.001
                                lng = 34.7818 + (Math.random() - 0.5) * 0.001
                                speed = 5.0 + Math.random() * 2.0
                                accuracy = 10.0
                                gpsTs = System.currentTimeMillis()
                            }
                            integrity.apply {
                                battery = 85
                                isCharging = false
                            }
                            nowRt = timeProvider.elapsedRealtime
                        }
                        domainEventBus.emit(DomainEvent.PeerStatusReceived(mockUpdate))
                    }
                    
                    delay(2000)
                }
            }
        } else {
            Timber.i("Synthetic Injection: Peer Presence simulation DISABLED")
            domainEventBus.emit(IntegrityEvent.LogEvent("SIMULATION: Virtual Peer Presence STOP", false))
        }
    }

    fun reset() {
        if (isShakeSimulated) simulateShake(false)
        if (isGnssLossSimulated) simulateGnssLoss(false)
        if (isPowerTamperSimulated) simulatePowerTamper(false)
        if (isNetworkLossSimulated) simulateNetworkLoss(false)
        isPeerSimulated = false
        simulationJob?.cancel()
        simulationJob = null
    }
}
