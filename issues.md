# Hardening Phase: Primary Tracking Document (v8.9.29)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[COMPLIANCE.md](COMPLIANCE.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 0 |
| **Validation Tasks** | 🟡 Pending Hardware | 1 |
| **Resolved (this phase)** | 🟢 Archived | 16 |

---

## 1. Physical Hardware Validation: Xiaomi & Samsung (Issue #10 / #190)
*   **Description**: Implementation of Xiaomi Boot Grace and Samsung 10Hz polling is complete in code, but requires physical hardware verification to ensure zero-spike behavior during transition.
*   **Location**: `MainAlarmLogic.kt` / Physical Hardware.
*   **Status**: **Pending Validation**.

---

## Resolved (this phase)
*   **Issue #17**: Role-Aware Alert Title Visibility (Forensic Parity). Refactored `getTrackerTitle()` in `MainAlarmLogic.kt` to be fully role-aware. It now strips "This device:" always, and "Tracker:" or "Viewer:" based on the current mode, ensuring clean local UI while preserving remote peer identification.
*   **Issue #16**: SoT Naming Alignment (IMM). Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision (changed `IMM_STATION_PROBABILITY` to `IMM_STATIONARY_PROBABILITY`).
*   **Issue #15**: GtoEngine Documentation/Code Inconsistency. Implemented `GtoEngine.kt` (sliding-window factor graph logic) and integrated it into `LocationSentinel.kt`. Logic now accounts for Path Efficiency and Mechanical Vibration signatures as specified.
*   **Issue #11**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt`.
*   **Issue #12**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt`.
*   **Issue #9**: Foreground Resilience Hardening. `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened.
*   **Issue #13**: Hardcoded EMA in AppSensorManager. Replaced with `LUX_EMA_FAST`.
*   **Issue #14**: Light EMA Logic Inconsistency. Implemented rising/falling EMA factors in `LocationSentinel.kt`.
