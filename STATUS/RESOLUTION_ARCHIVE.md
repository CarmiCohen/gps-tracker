# 🏛️ Resolution Archive - Sep.24.40

## 🏁 Issue #1241: Functional Restoration of History Sync Streams in ViewerService
*   **Resolved**: Sep.24.40
*   **Root Cause**: `ViewerService` lacked a subscription to the `HistoryManager.historyEvents` stream. Instead, it was relying on a legacy pulse listener that didn't capture history backfill or sync-specific events. This resulted in an observation gap on the monitor device where legitimate history lifecycle events (logs and triggers) were not being surfaced.
*   **Remediation**:
    *   **ViewerService.kt**: Implemented `observeHistoryEvents()` which subscribes to `historyManager.historyEvents` using `collectLatest`.
    *   **ViewerService.kt**: Integrated the observer into the `onServiceInitialize()` sequence to ensure immediate stream connectivity upon service startup.
*   **R-ID**: 464

# 🏛️ Resolution Archive - Sep.24.30

## 🏁 Issue #1307: Forensic Sampling Bottleneck During Rapid Event Sequences
*   **Resolved**: Sep.24.30
*   **Root Cause**: The `forensicSamplingLoop` in `TrackerService` used a conflated channel and a fixed `delay(delayMs)` following each capture iteration. If multiple physical spikes (acoustic or light) occurred during the delay period, subsequent high-priority triggers were dropped or significantly delayed, as the loop could only process one trigger per sampling interval. This created a data-loss risk for rapid tampering event sequences.
*   **Remediation**:
    *   **TrackerService.kt**: Refactored `forensicTriggerChannel` from a conflated channel to a buffered `Channel<Boolean>`.
    *   **TrackerService.kt**: Updated the `forensicSamplingLoop` to utilize a non-blocking `withTimeoutOrNull(delayMs)` polling pattern. This allows the loop to respond to high-priority spike signals (`true`) immediately, bypassing the standard interval timer, while still maintaining the expected sampling cadence for regular state captures.
*   **R-ID**: 463

# 🏛️ Resolution Archive - Sep.24.20

## 🏁 Issue #1256: Monotonic Latch Staleness Across Reboots
*   **Resolved**: Sep.24.20
*   **Root Cause**: Timing latches such as siren cooldowns and violation timers persisted via monotonic references (`elapsedRealtime`) became stale and invalid when a device reboot occurred. Since `elapsedRealtime` resets back to zero upon device restart, old high-valued latches from the prior boot session stayed valid relative to the new boot cycle's timing authority, resulting in long-term false lockouts or feature suppression ("Permanent Muzzle" bug).
*   **Remediation**:
    *   **TimeProvider.kt**: Added a `getBootId()` definition to the temporal authority interface.
    *   **AndroidTimeProvider.kt**: Implemented `getBootId()` to read and cache the kernel's unique session identifier from `/proc/sys/kernel/random/boot_id`, fallback to random UUID if unavailable.
    *   **AppAlarmManager.kt**: Enhanced `restoreLogicState()` to compare the current session's boot identifier with the stored reference. If a reboot is detected, it completely invalidates obsolete monotonic latches across all functional roles, restoring safe baseline operation immediately.
*   **R-ID**: 462

## 🏁 Issue #1260: Boot-ID Validation for Persistent Monotonic Latches
*   **Resolved**: Sep.24.20
*   **Root Cause**: Duplicate or side-effect requirement of Issue #1256; missing boot-level synchronization checks for persistent state machines.
*   **Remediation**: Remediated via atomic Boot-ID validation inside `AppAlarmManager.restoreLogicState` to reset timing thresholds securely.

## 🏁 Issue #1301: Missing Persistence for Lux and Acoustic Baselines
*   **Resolved**: Sep.24.10
*   **Root Cause**: Environmental calibration (Lux and Acoustic baselines) was only maintained in memory, resetting on every service restart. This forced a 60-second "learning period" upon startup, during which the system was highly susceptible to false-positive tamper alerts as baselines had not yet stabilized to ambient levels.
*   **Remediation**:
    *   **LocationProcessor.kt**: Implemented detection of significant baseline drift (>1.0 Lux / >1.0 dB) and added `LuxBaselineChanged` and `AcousticFloorChanged` to `ProcessorEvent`.
    *   **LocationSentinel.kt**: Expanded `loadForensicState` to restore persisted Lux and Acoustic anchors.
    *   **TrackerService.kt / ViewerService.kt**: Restored environmental anchors during service initialization and registered reactive DataStore synchronization to ensure calibration survives restarts.
*   **R-ID**: 461

## 🏁 Issue #1302: Redundant and Misaligned LocationProcessor in ViewerService
*   **Resolved**: Sep.24.10
*   **Root Cause**: `ViewerService` maintained independent `selfProcessor` and `remoteProcessor` instances but failed to update `selfProcessor` with local sensor data in the `processTick` loop. This caused the Viewer device to have degraded motion awareness and incorrect stationary detection for its own hardware.
*   **Remediation**:
    *   **ViewerService.kt**: Integrated `selfProcessor.updateSensorData(evalSnapshot.sensor)` into the periodic tick loop, aligning local physical awareness with the tracker's processing logic.

## 🏁 Issue #1303: Cross-Role HardwareSuite Sensitivity Contamination
*   **Resolved**: Sep.24.10
*   **Root Cause**: `ViewerService` was applying the remote tracker's restored vibration floor to the singleton `HardwareSuite`, incorrectly forcing the monitor device to use the tracked device's physical sensitivity profile.
*   **Remediation**:
    *   **ViewerService.kt**: Decoupled local `HardwareSuite` configuration from remote tracker anchors, allowing the monitor device to maintain its own autonomous sensitivity.

## 🏁 Issue #1304: Peer Stat Reset Logic Corrupts Local Tracker State
*   **Resolved**: Sep.24.10
*   **Root Cause**: `ConnectivitySuite.resetPeerStats()` used the local role prefix to clear baselines, causing a Tracker to wipe its own persistent calibration upon every network disconnect.
*   **Remediation**:
    *   **ViewerService.kt**: Corrected the `observeProcessorEvents` routing to ensure only remote tracker stats trigger persistent updates in the Viewer role.

## 🏁 Issue #1273: Atomic User Counter Risk in HardwareSuite
*   **Resolved**: Sep.24.04
*   **Root Cause**: The `activeUsers` AtomicInteger in `HardwareSuite` could potentially fall below zero if `stop()` calls exceeded `start()` calls. Furthermore, the deferred teardown routine used a strict `== 0` check, which would fail if the counter became negative, causing background resources (sensors, GNSS callbacks) to remain orphaned.
*   **Remediation**:
    *   **HardwareSuite.kt**: Guarded the decrement in `stop()` using `updateAndGet { max(0, it - 1) }` to prevent negative values. Hardened the deferred teardown check to use `<= 0` as a fail-safe, ensuring deterministic resource release.
*   **R-ID**: 460

## 🏁 Issue #1271: Missing Persistence for Adaptive Vibration Floor
*   **Resolved**: Sep.24.04
*   **Root Cause**: The adaptive vibration floor (baseline physical sensitivity) was only maintained in memory. Upon background service restarts or system-initiated process kills, the floor would reset to its default initial value (0.05g). This caused increased sensitivity and false-positive tamper alerts until the floor could re-adapt.
*   **Remediation**:
    *   **PreferenceKeys.kt**: Added `ADAPTIVE_VIBRATION_FLOOR_KEY` for persistent storage.
    *   **LocationProcessor.kt**: Implemented detection of significant floor drift (>0.01g) and added `VibrationFloorChanged` to `ProcessorEvent`.
    *   **HardwareSuite.kt**: Added `setAdaptiveVibrationFloor(floor: Double)` to allow manual anchoring of the hardware sensitivity baseline.
    *   **TrackerService.kt / ViewerService.kt**: Restored the vibration floor anchor in both `LocationProcessor` and `HardwareSuite` during service initialization and registered reactive DataStore synchronization for subsequent updates.
*   **R-ID**: 459

## 🏁 Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots
*   **Resolved**: Sep.24.02
*   **Root Cause**: Monotonic timing references (`elapsedRealtime`) were being recovered using wall-clock drift references from previous boot sessions. Since `elapsedRealtime` resets to zero on reboot, using a stale drift resulted in invalid/negative monotonic anchors in the new session.
*   **Remediation**:
    *   **HistoryManager.kt**: Implemented `recoverLastRealtime(lastTs, recoveredDrift)` which detects if the persisted drift has diverged from the current session's drift (indicating a reboot). It then anchors the recovery to the current boot cycle's drift reference.
    *   **TrackerService.kt / ViewerService.kt**: Updated initialization to use `historyManager.recoverLastRealtime` and role-prefixed drift keys (`T_clock_drift_ref` / `V_clock_drift_ref`) to ensure forensic timing continuity survives reboots.
*   **R-ID**: 458

## 🏁 Issue #1233: High Allocation Churn via Fast-Path Re-registration
*   **Resolved**: Sep.24.01
*   **Root Cause**: Fast-path callbacks were being re-registered on every 2-second service tick, causing massive lambda instantiation overhead and allocation churn in the background thread.
*   **Remediation**:
    *   **HardwareSuite.kt**: Refactored `HardwareFastPath` to support nullable/optional callback updates, ensuring that parameters (baseline, thresholds) can be adjusted dynamically without needing to instantiate and pass new callback functions every iteration pass.
    *   **TrackerService.kt**: Omitted the `onSpike` lambda callbacks during the periodic service tick routine, completely eliminating allocation loop overhead while preserving autonomous sensor thread adaptations.
*   **R-ID**: 457
