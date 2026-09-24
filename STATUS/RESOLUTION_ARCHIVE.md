# 🏛️ Resolution Archive - Sep.24.04

## 🏁 Issue #1273: Atomic User Counter Risk in HardwareSuite
*   **Resolved**: Sep.24.04
*   **Root Cause**: The `activeUsers` AtomicInteger in `HardwareSuite` could potentially fall below zero if `stop()` calls exceeded `start()` calls. Furthermore, the deferred teardown routine used a strict `== 0` check, which would fail if the counter became negative, causing background resources (sensors, GNSS callbacks) to remain orphaned.
*   **Remediation**:
    *   **HardwareSuite.kt**: Guarded the decrement in `stop()` using `updateAndGet { max(0, it - 1) }` to prevent negative values. Hardened the deferred teardown check to use `<= 0` as a fail-safe, ensuring deterministic resource release.
*   **R-ID**: 460

## 🏁 Issue #1271: Missing Persistence for Adaptive Vibration Floor
*   **Resolved**: Sep.24.04
*   **Root Cause**: The adaptive vibration floor (baseline physical sensitivity) was only maintained in memory. Upon background service restarts or system-initiated process kills, the floor would reset to its default initial value (0.05g). This caused increased sensitivity and false-positive tamper alerts until the floor could re-adapt. Additionally, `HardwareSuite` was not being updated with the restored value, causing a source-of-truth divergence for stationarity detection.
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
