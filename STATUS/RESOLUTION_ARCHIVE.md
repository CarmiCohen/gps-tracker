# 🏛️ Resolution Archive - Sep.24.00

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
*   **Remediation**: Implemented type-specific namespaced maps (`role_longs`, `role_doubles`, etc.) in `app_settings.proto` and refactored the repository layer to enforce isolation via role-prefixes.
*   **R-ID**: 453
