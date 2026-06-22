# Hardening Phase: Primary Tracking Document (v8.9.28)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[COMPLIANCE.md](COMPLIANCE.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 0 |
| **Validation Tasks** | 🟡 Pending Hardware | 1 |
| **Resolved (this phase)** | 🟢 Archived | 13 |

---

## 1. Physical Hardware Validation: Xiaomi & Samsung (Issue #10 / #190)
*   **Description**: Implementation of Xiaomi Boot Grace and Samsung 10Hz polling is complete in code, but requires physical hardware verification to ensure zero-spike behavior during transition.
*   **Location**: `MainAlarmLogic.kt` / Physical Hardware.
*   **Status**: **Pending Validation**.

---

## Resolved (this phase)
*   **Issue #11**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt` covering multi-point transitions, angle/speed tolerances, and buffer pruning logic.
*   **Issue #12**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt` using `last_history_sit_ts` and `SIT_DUPLICATE_GUARD_MS` to prevent redundant SIT forensic markers from relay re-transmissions.
*   **Issue #9**: Foreground Resilience Hardening. `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened with `try-catch` protection and `safeStartForeground` wrapper against Android 14+ `ForegroundServiceStartNotAllowedException`.
*   **Issue #13**: Hardcoded EMA in AppSensorManager. Replaced hardcoded `0.01f` with `LUX_EMA_FAST`.
*   **Issue #14**: Light EMA Logic Inconsistency. Implemented rising/falling EMA factors in `LocationSentinel.kt`.
*   *(Other previously resolved items archived to COMPLIANCE.md)*
