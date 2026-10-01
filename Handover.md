# Forensic Handover (Sep.30.70 - STORAGE AUTHORITY UNIFIED)

## 🎯 Current System State
*   **Version**: `Sep.30.70` | **Status**: STORAGE LAYER HARDENED & TYPE-SAFE.
*   **Unified Storage Authority (#1407)**:
    *   **Resolution**: `SettingsRepository` and `MainRepository` refactored to use `AppRole` enum parameters for all namespaced operations.
    *   **Elimination**: Removed manual string concatenation of prefixes (`"T_"`, `"V_"`, `"VR_"`) in `MonitorService`, `AppAlarmManager`, `HistoryManager`, `MaintenanceWorker`, `SettingsUseCase`, `CommandRouter`, and `AlertUseCase`.
    *   **Robustness**: Ambiguous string-based overloads in `SettingsRepository` were deprecated or routed through the type-safe API to prevent key-shadowing bugs.
*   **Muted Alarm Visibility (#1405)**:
    *   **Resolution**: `MainAlarmLogic` updated with `onTriggerMuted` callback.
    *   **Visual Feedback**: `AppAlarmManager` now generates visual `LogEvent` notifications even when the physical siren is silenced by a manual lockout, ensuring violation transparency during the 30s cooldown.

## 🚀 Resumption Focus: Released Path Validation
*   **Target**: Verify the `Sep.30.70` storage isolation under dynamic role-switching (Tracker <-> Viewer).
*   **Immediate Path**:
    1.  **DataStore Migration Verification**: Confirm that existing namespaced data is correctly mapped by the new `AppRole`-based accessors.
    2.  **Field Audit**: Trigger sequential violations during a manual mute to verify visual-only log emissions.

---

## 🛡️ Core Architecture Blueprint
1.  **Storage Authority**: Use `repository.saveLong(role, key, value)` instead of prefixing.
2.  **Lockout Integrity**: Centralized authority via `SirenLockoutUseCase`.
3.  **Namespace Safety**: The `AppRole` enum is the exclusive source of truth for persistent state isolation.

---

## 📊 Hardening Progress Dashboard (Sep.30.70)
- **Status**: [SOT Count: 228 (Rules: 79), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 320]
- **Audit Record**: `SettingsRepository` refactored for type-safe namespacing; manual prefixing eradicated.
