# 🏛️ Resolution Archive - Sep.23.72

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

... [Previous entries preserved] ...
