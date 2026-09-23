# 🏛️ Resolution Archive - Sep.23.50

## 🏁 Issue #1203: Hilt ViewModel Scope Optimization
*   **Resolved**: Sep.23.50
*   **Root Cause**: Role-specific ViewModels (`TrackerViewModel`, `ViewerViewModel`, `SetupViewModel`) were independently subscribing to high-frequency data streams, leading to resource churn (#1211), map state loss during navigation (#1212), swallowed events (#1213), and kinematic state misrouting (#1257).
*   **Remediation**:
    *   Centralized all high-frequency kinematic and diagnostic streams into `MainViewModel`.
    *   Unified `MainUiState` and `DiagnosticState` to act as the single source of truth for all functional roles.
    *   Refactored `MainAppContent`, `TrackerScreen`, and `ViewerScreen` to consume state directly from the activity-scoped `MainViewModel`.
    *   Implemented clean state resets in `MainViewModel` during role switching to ensure fresh session integrity.
    *   Eliminated redundant `hiltViewModel()` instantiations, reducing coroutine allocation churn.
*   **R-ID**: 415 (Updated)

## 🏁 Issue #1204: Unified Hardware Lifecycle & Vendor Hardening
*   **Resolved**: Sep.23.08
*   **Root Cause**: Vendor-specific power management adaptations (Samsung, Xiaomi, Huawei) and WakeLock management were dispersed across services and utility classes.
*   **Remediation**: Created `DeviceHardeningStrategy` and `ProcessPriorityMonitor` to centralize OEM-specific stability logic.
*   **R-ID**: 421

... [Previous entries preserved] ...
