# 🏛️ Resolution Archive - Oct.1.2

## 🏁 Issue #1407: Unified Storage Authority
*   **Resolved**: Oct.1.2
*   **Root Cause**: Namespaced persistent storage operations relied on manual string concatenation of role prefixes (e.g., `role.prefix + KEY`), which was error-prone and bypassed type safety provided by the `AppRole` enum. A critical leakage was found where `CLOCK_DRIFT_REF_KEY` was read globally in `MonitorService`.
*   **Remediation**: 
    *   **Repository Overloads**: Refactored `SettingsRepository` and `MainRepository` to provide storage method overloads accepting `AppRole` as a primary parameter.
    *   **MonitorService Fix**: Resolved a critical leakage in `onServiceInitialize` where `CLOCK_DRIFT_REF_KEY` was being read from the global namespace instead of the role-partitioned storage.
    *   **Global Purge**: Finalized implementation by removing redundant global fields and manual routing fall-throughs from `SettingsRepository`, enforcing `AppRole` authority at the compiler level.
    *   **Namespace Integrity**: Eliminated manual prefixing across the codebase, ensuring all role-based state isolation follows a single, verified authority.
*   **Significance**: High (Structural Integrity).
*   **SOT ID**: 568 (Unified Storage Authority)

---

# 🏛️ Resolution Archive - Sep.30.43

## 🏁 Issue #1406: Role Identity Authority
*   **Resolved**: Sep.30.43
*   **Root Cause**: Critical inconsistency between `CommandRouter` (`V_`) and `AppAlarmManager/AlertUseCase` (`VR_`) caused acknowledgments to be saved to orphaned keys, resulting in immediate re-triggering loops.
*   **Remediation**: 
    *   **Enum Contract**: Introduced `AppRole` enum in `core:engine` to centralize namespace authority.
    *   **Global Migration**: Refactored `CommandRouter`, `MainRepository`, `AlertUseCase`, `AppAlarmManager`, `MonitorService`, `MaintenanceWorker`, `HistoryManager`, and `SettingsUseCase` to use `AppRole.prefix`.
    *   **Traceability**: Eliminated all hardcoded `"T_"` and `"VR_"` string literals from the operational logic.
*   **Significance**: High (Functional Correctness).
*   **SOT ID**: 565 (Unified Role Identity Authority)

...
*(Full historical records maintained in SOT Archive)*
