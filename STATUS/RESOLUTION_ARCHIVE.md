# 🏛️ Resolution Archive - Sep.24.03

## 🏁 Issue #1271: Missing Persistence for Adaptive Vibration Floor
*   **Resolved**: Sep.24.03
*   **Root Cause**: The adaptive vibration floor (baseline physical sensitivity) was only maintained in memory. Upon background service restarts or system-initiated process kills, the floor would reset to its default initial value (0.05g). This caused increased sensitivity and false-positive tamper alerts until the floor could re-adapt.
*   **Remediation**:
    *   **PreferenceKeys.kt**: Added `ADAPTIVE_VIBRATION_FLOOR_KEY` for persistent storage.
    *   **LocationProcessor.kt**: Implemented detection of significant floor drift (>0.01g) and added `VibrationFloorChanged` to `ProcessorEvent`. Updated `loadState` to accept and propagate the saved floor.
    *   **LocationSentinel.kt**: Updated `loadForensicState` to anchor the `adaptiveVibrationFloor` from persistent storage if provided.
    *   **TrackerService.kt / ViewerService.kt**: Integrated DataStore synchronization for the vibration floor, ensuring role-isolated persistence and restoration during service lifecycle transitions.
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

## 🏁 Issue #1232: Empty Stub Implementation of OEM Power Hardening Overrides
*   **Resolved**: Sep.24.00
*   **Root Cause**: `DeviceHardeningStrategy.kt` contained purely cosmetic log message stubs rather than active code implementations, presenting a critical reliability risk for service longevity on specialized OEM hardware.
*   **Remediation**:
    *   **DeviceHardeningStrategy.kt**: Converted cosmetic stubs into functional background execution policies. Implemented active WakeLock acquisition/renewal and grid-aligned Watchdog scheduling specifically tailored for Samsung, Huawei, and Xiaomi background constraints to ensure process retention under aggressive power-saving cycles.
*   **R-ID**: 421

## 🏁 Issue #1231: Redundant Stream Overlap & Duplicate Heartbeat Processing
*   **Resolved**: Sep.23.80
*   **Root Cause**: `ViewerService.kt` contained a duplication error where `ConnectivityEvent.PeerPulse` was being observed by two separate functions (`observeConnectivityEvents` and `observeHistoryEvents`). This caused duplicate processing of heartbeat signals in `SessionManager` and redundant logging.
*   **Remediation**:
    *   **ViewerService.kt**: Removed the redundant `observeHistoryEvents` function and its call during service initialization. Consolidated the peer pulse handling into the primary `observeConnectivityEvents` loop.
*   **R-ID**: 456

## 🏁 Issue #1250: Build Vitality & Reactive Stream Convergence
*   **Resolved**: Sep.23.72
*   **Root Cause**: Regressions following the role-isolation refactor left `TrackerService` with unimplemented abstract members (`onHeartbeat`, `onLocationChanged`) and `MainViewModel` with mis-typed flows, causing widespread compilation failures. Additionally, several reactive streams were missing from the repository layer delegates.
*   **Remediation**:
    *   **TrackerService.kt**: Implemented `onHeartbeat` for notification updates and `onLocationChanged` for GPS fix buffering. Corrected `evaluateAlarmsInternal` signature.
    *   **MainRepository.kt**: Implemented missing delegates for draft settings (`saveDraftSettings`, `commitDraftSettings`, `clearDraftSettings`) and exposed missing flows (`isXiaomiManualOverrideFlow`, `recoveryCountFlow`, `cumulativeRecoveryBlackoutMsFlow`).
    *   **SettingsRepository.kt**: Fully implemented the reactive flows for recovery stats and manual overrides in the DataStore layer.
    *   **MainViewModel.kt**: Converted `trackerTrailFlow` and `viewerTrailFlow` to `StateFlow` to restore access to the `.value` property for trail searching.
*   **R-ID**: 455

## 🏁 Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption
*   **Resolved**: Sep.23.70
*   **Root Cause**: `TrackerService` and `ViewerService` shared the same flat keys in `DataStore` for high-frequency logic state. Switching roles caused state "leakage".
*   **Remediation**: Implemented type-safe namespaced maps (`role_longs`, `role_doubles`, etc.) in `app_settings.proto` and refactored the repository layer to enforce isolation via role-prefixes.
*   **R-ID**: 453
