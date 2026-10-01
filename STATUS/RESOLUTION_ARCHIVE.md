# 🏛️ Resolution Archive - Sep.30.43

## 🏁 Issue #1407: Unified Storage Authority
*   **Resolved**: Sep.30.43
*   **Root Cause**: Namespaced persistent storage operations relied on manual string concatenation of role prefixes (e.g., `role.prefix + KEY`), which was error-prone and bypassed type safety provided by the `AppRole` enum.
*   **Remediation**: 
    *   **Repository Overloads**: Refactored `SettingsRepository` and `MainRepository` to provide storage method overloads accepting `AppRole` as a primary parameter.
    *   **Global Migration**: Updated `MonitorService`, `AppAlarmManager`, `HistoryManager`, `MaintenanceWorker`, `SettingsUseCase`, `CommandRouter`, and `AlertUseCase` to use the type-safe role-based API.
    *   **Namespace Integrity**: Eliminated manual prefixing across the codebase, ensuring all role-based state isolation follows a single, verified authority.
*   **Significance**: Maintenance (Refactoring).
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

## 🏁 Issue #1403: Siren Lockout Compliance
*   **Resolved**: Sep.30.43
*   **Root Cause**: Implementation utilized a 15s lockout duration which deviated from the 30s SOT requirement.
*   **Remediation**: 
    *   **Constant Alignment**: Updated `SIREN_RESUME_COOLDOWN_MS` to 30000L in `EngineConstants.kt`.
    *   **System Propagation**: Verified that the updated threshold is correctly picked up by `AppAlarmManager` and `SirenLockoutUseCase`.
*   **Significance**: Medium (SOT Compliance).
*   **SOT ID**: 567 (Standardized Dismissal Lockout)

## 🏁 Issue #1404: Lockout Persistence
*   **Resolved**: Sep.30.43
*   **Root Cause**: `lastSirenStopRt` (monotonic timestamp) was wiped on `boot_id` changes, causing previously muted alarms to sound again after service recovery or process death.
*   **Remediation**: 
    *   **RT Recovery**: Modified `AppAlarmManager.restoreLogicState` to recover `lastSirenStopRt` via `BootLifecycleAuthority` if the boot session is valid.
    *   **State Sync**: Integrated the recovered state into `SirenLockoutUseCase` to maintain UI and audio sync across restarts.
*   **Significance**: Medium (User Experience).
*   **SOT ID**: 566 (Persistent Alarm Lockout)

## 🏁 Issue #1405: Sequential Trigger Mute Protection
*   **Resolved**: Sep.30.43
*   **Root Cause**: `MainAlarmLogic` was resetting the global siren lockout whenever a new alarm type was triggered, preventing a single "Stop" action from muting a sequence of different alerts.
*   **Remediation**: 
    *   **Logic Refactor**: Removed the conditional wipe of `lastSirenStopRt` in `MainAlarmLogic.processActiveAlarms`. Lockout now persists until its natural expiration regardless of new triggers.
*   **Significance**: Medium (Logic Correctness).

---

# 🏛️ Resolution Archive - Sep.30.43

## 🏁 Issue #1390: Camera Action Event Flow
*   **Resolved**: Sep.30.43
*   **Root Cause**: Imperative map commands (zoom, centering) were being driven by cumulative trigger counters within the persistent `MapViewState`.
*   **Remediation**: 
    *   **SharedFlow Migration**: Replaced cumulative counters with a single `SharedFlow<CameraAction>` in `MainViewModel`.
*   **Significance**: Low (Architectural Hygiene).
*   **SOT ID**: 564 (Camera Action Event Flow)

...
*(Full historical records maintained in SOT Archive)*
