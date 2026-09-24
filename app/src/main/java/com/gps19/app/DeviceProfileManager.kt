package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeviceProfileManager: Central controller for vendor-specific adaptations and loop continuity tweaks.
 * Sep.24.97:
 * - Issue #1291: Updated executeContinuityTweaks to accept Long for tick counter.
 */
@Singleton
class DeviceProfileManager @Inject constructor(
    private val timeProvider: TimeProvider,
    private val logManager: LogManager,
    private val systemMonitor: SystemMonitor,
    private val hardwareSuite: HardwareSuite,
    private val hardeningStrategy: DeviceHardeningStrategy,
    private val priorityMonitor: ProcessPriorityMonitor
) {
    private var lastStaggeredPokeRt = 0L
    private val STAGGERED_POKE_INTERVAL_MS = 30_000L

    /**
     * Centrally initializes hardware bindings if the device requires native SDK alignment.
     */
    suspend fun initializeHardwareProfile(capabilities: HardwareCapabilities, deviceId: String) {
        if (capabilities.isA15Device) {
            val success = JdHardwareManager.initialize(timeProvider, deviceId)
            if (success) {
                logManager.logServiceEvent("HARDWARE: libjdHardware initialized successfully.", isImportant = true)
            } else {
                logManager.logServiceEvent("HARDWARE: libjdHardware initialization failed.", isImportant = true)
            }
        }
    }

    /**
     * Executes loop continuity tweaks, wake lock renewals, and LED state synchronization.
     */
    suspend fun executeContinuityTweaks(
        capabilities: HardwareCapabilities,
        nowRt: Long,
        serviceTickCounter: Long,
        lastValidFixRt: Long,
        isPowerSaveMode: Boolean,
        localInternetLoss: Boolean,
        isSocketConnected: Boolean,
        isPeerActive: Boolean
    ) {
        hardeningStrategy.applyVendorWakeLockPolicy(capabilities, force = false)
        hardeningStrategy.executeVendorContinuity(capabilities, nowRt, isPowerSaveMode)
        priorityMonitor.sendStayAlivePulse(forceWakeLock = false)

        val isStaggered = capabilities.performanceTier == PerformanceTier.STAGGERED
        if (isStaggered) {
            if (capabilities.isA15Device && JdHardwareManager.isAvailable()) {
                val gpsAge = nowRt - lastValidFixRt
                JdHardwareManager.syncHardwareState(
                    timeProvider = timeProvider,
                    tick = serviceTickCounter.toInt(),
                    status = LedStatus(
                        isPowerSave = isPowerSaveMode,
                        isGpsStale = gpsAge > TELEMETRY_UI_STALE_THRESHOLD_MS,
                        isInternetLoss = localInternetLoss,
                        isRelayLoss = !isSocketConnected,
                        isPeerStale = !isPeerActive
                    )
                )
            } else if (hardwareSuite.shouldPokeHardware(isStaggered, lastStaggeredPokeRt, STAGGERED_POKE_INTERVAL_MS)) {
                lastStaggeredPokeRt = nowRt
                systemMonitor.acquireWakeLock(force = true)
            }
        }
    }

    /**
     * Clean up and release vendor-specific hardware handles.
     */
    fun teardownHardwareProfile(capabilities: HardwareCapabilities) {
        if (capabilities.isA15Device && JdHardwareManager.isAvailable()) {
            val punchResult = JdHardwareManager.punchHardware(timeProvider)
            if (punchResult != 0) {
                logManager.logServiceEvent("HARDWARE: Handshake failed (Code: $punchResult). Forcing release.", isImportant = true)
            }
        }
    }
}
