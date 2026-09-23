# Forensic Resumption Snapshot - Sep.23.71

## 📂 Session Summary
*   **Completed**: 
    *   **Issue #1230**: Shared Storage Key Leakage & Cross-Role State Corruption (R-ID 453).
    *   **Issue #1240**: Namespace Isolation for Logic State Persistence (R-ID 453).
    *   **Issue #1236**: Race Condition Remediation (R-ID 424).
*   **Version**: Sep.23.71
*   **Status**: Storage isolation for functional roles (Tracker vs Viewer) is now physically enforced in the DataStore. State "leakage" between roles has been eliminated. The foundational Architectural SOT has been restored and synchronized.

## 🔧 Technical Delta
*   **SOT Restoration**: Remediated a significant document truncation in `SOT_MASTER_REQUIREMENTS.md`. Recovered 22 architectural master rules and mapping for R101-R759 from Git history.
*   **MainRepository.kt**: Refactored `lastAlarmAckTsFlow` using `flatMapLatest` on `appModeFlow` to ensure reactive UI streams observe the correct role partition. Implemented role-aware memory caching and routing for `"T_"` and `"V_"` prefixed keys.
*   **SettingsRepository.kt**: Restored reactive flows for namespaced telemetry and implemented routing logic to map role-prefixed keys to isolated Protobuf maps (`role_longs`, `role_doubles`, etc.) in `app_settings.pb`.
*   **AlertUseCase.kt**: Refactored to inject `ConfigManager`, enabling role-prefixed storage for user-driven alarm dismissals.
*   **MainViewModel.kt**: Fully integrated `AlertUseCase` into the UI event loop. Refactored `UiEvent.DismissAlarms` and `UiEvent.StopSiren` to ensure user actions are isolated per functional role.
*   **CommandRouter.kt**: Updated `UiCommand.StopSiren` to apply role-based namespacing to acknowledgment timestamps and power alarm latches.
*   **SettingsUseCase.kt**: Updated application bootstrap logic to hydrate `lastAlarmAckTs` from the correct role partition during initial hydration.
*   **AppAlarmManager.kt**: Refined `syncEvaluationState` to utilize namespaced acknowledgments, ensuring the "Alarm Active" logic is isolated per role.
*   **ConnectivitySuite.kt**: Fixed a syntax error at line 908 and ensured `resetPeerStats` correctly namespaces baseline clearing for the remote peer.
*   **MaintenanceWorker.kt**: Made worker role-aware to audit correctly namespaced service ticks.

## 📍 Resumption Point for Next Session
*   **Immediate Priority**: Audit signaling performance under physical stress with the new namespacing.
*   **Next Task**: Remediation of **Issue #1231** (Redundant Stream Overlap in ViewerService) and **Issue #1232** (OEM Power Hardening Overrides).
*   **Strategic Goal**: Complete the background service infrastructure hardening (#1171) and finalize "Signal-on-Spike" sampling.

## 📊 Audit Baseline
**Current Audit Baseline: [SOT: 453 (Rules: 90, IDs: 453), Resolved: 1190, Open: 23, Testing: 3 (Sub-items: 12), Ideas: 15, QA: 283]**
