# Project Issues & Hardening Tracking (July.23.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 349 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *No open critical issues.*

---

## 🟢 Recently Resolved Issues (July.23.02)
*   **Issue #526: Power Optimization - Adaptive Sensor Sampling**.
    *   **Resolution**: Implemented adaptive logic ticking and acoustic duty cycling. When the device is `STATIONARY` and the GPS is `STALLED`, the logic tick interval extends from 2s to 10s, and the microphone switches to a 20% duty cycle (2s ON / 8s OFF). Centralized the evaluation logic in `ServiceBehaviorUseCase.kt` to maintain architectural purity.
*   **Issue #525: State Audit - Forensic Propagation Verification**.
    *   **Resolution**: Verified and hardened the end-to-end propagation of forensic parameters. Fixed a mapping bug in `HistoryManager` that caused forensic data loss in local ribbons. Synchronized `ConnectivitySuite` to ensure all 7+ forensic indices are transmitted to remote viewers, achieving 100% parity between local history and remote telemetry.
*   **Issue #524: UI Decoupling - Dashboard State Extraction**.
    *   **Resolution**: Extracted UI formatting logic from `DashboardUseCase` into a dedicated `DashboardStateProvider`. `MainViewModel` now depends on the provider, reducing its complexity and enforcing a cleaner separation between business logic and UI state representation.
*   **Issue #523: Forensic Snapshot Consolidation**.
    *   **Resolution**: Implemented `AppSensorManager.consumeForensicSnapshot()` to provide an atomic immutable state of all forensic parameters. Refactored `TrackerService.kt` to use this snapshot, eliminating a logic bug where peak sensor values were being consumed/reset multiple times per tick.

---

## 🟢 Recently Resolved Issues (July.23.01)
*   **Issue #522: SIT Logic Hardening & Forensic Pipeline Unification**.
    *   **Resolution**: Optimized the forensic Sit-Detection (SIT) heuristic in `LocationSentinel.kt`. Unified the local telemetry pipeline in `TrackerService.kt` to feed all 7+ forensic parameters directly into the processor. Completed the "Deep Purge" of `RemoteHandler.kt`.
