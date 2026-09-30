# Forensic Handover (Oct.01.24 - ALARM & CONTRACT AUDIT)

## 🎯 Current System State
*   **Version**: `Oct.01.24` | **Status**: ALARM AUDIT COMPLETE - HARDENING REQUIRED.
*   **Role Prefix Mismatch (#1406)**:
    *   **Discovery**: Critical inconsistency between `CommandRouter` (`V_`) and `AppAlarmManager/AlertUseCase` (`VR_`). 
    *   **Impact**: Manual "Stop/Mute" actions are saved to orphaned keys, causing immediate re-triggering loops.
*   **Lockout Persistence Gap (#1404)**:
    *   **Discovery**: `lastSirenStopRt` is wiped on `boot_id` changes (process kills/reboots).
    *   **Impact**: Active alarms sound again on service recovery even if previously muted.
*   **Requirement Deviation (#1403)**: Siren lockout is 15s (implemented) vs 30s (SOT mandated).

## 🚀 Resumption Focus: Horizontal Hardening
*   **Target**: Stabilize Alarm Muting and Communication Contracts.
*   **Immediate Path**:
    1.  **Standardize Role Identity**: Replace all hardcoded `"T_"`, `"V_"`, and `"VR_"` strings with a unified `AppRole` enum in `core:engine`.
    2.  **Fix Lockout Persistence**: Modify `AppAlarmManager.restoreLogicState` to preserve mute timestamps across reboots.
    3.  **Regression Guarding**: Implement `MainAlarmLogicTest` cases for the 30s lockout and manual dismissal acknowledgement.

---

## 🛡️ Core Architecture Blueprint
1.  **Contractual Integrity**: Communication between modules MUST use strict Kotlin types/Enums, never raw strings.
2.  **Stealth First**: Tracker mode MUST be strictly silent and dark (R872).
3.  **UDF Authority**: UI must strictly reflect the `SystemEvaluationSnapshot` produced by the engine.

---

## 📊 Hardening Progress Dashboard (Oct.01.24)
- **Status**: [SOT Count: 227 (Rules: 75), Open: H:4, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 312]
- **Audit Record**: `Alarms.md` created as the specification for automated regression testing.
