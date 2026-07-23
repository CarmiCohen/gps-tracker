# Project Issues & Hardening Tracking (July.23.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 350 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Tooling Limitation**: Unable to physically delete `DashboardUseCase.kt` due to lack of file deletion tool in the current environment. The file is orphaned but remains in the filesystem.

---

## 🔴 Open Issues
*   *No open critical issues.*

---

## 🟢 Recently Resolved Issues (July.23.03)
*   **Issue #526: Power Optimization - Adaptive Sensor Sampling**.
    *   **Resolution**: Implemented two-tier power saving. 
        1. **Logic Tier**: Logic tick interval extends from 2s to 10s and Microphone uses a 20% duty cycle.
        2. **Hardware Tier**: Dynamically downgrades `Linear Acceleration` sampling from `SENSOR_DELAY_FASTEST` to `SENSOR_DELAY_NORMAL` when stationary/stalled. Centralized evaluation in `ServiceBehaviorUseCase.kt`.
*   **Issue #525: State Audit - Forensic Propagation Verification**.
    *   **Resolution**: Verified and hardened the end-to-end propagation of forensic parameters. Fixed a mapping bug in `HistoryManager` that caused forensic data loss in local ribbons. Synchronized `ConnectivitySuite` to ensure all 7+ forensic indices are transmitted to remote viewers.
*   **Issue #524: UI Decoupling - Dashboard State Extraction**.
    *   **Resolution**: Extracted UI formatting logic from `DashboardUseCase` into a dedicated `DashboardStateProvider`. `MainViewModel` now depends on the provider, reducing its complexity.
*   **Issue #523: Forensic Snapshot Consolidation**.
    *   **Resolution**: Implemented `AppSensorManager.consumeForensicSnapshot()` to provide an atomic immutable state of all forensic parameters. Refactored `TrackerService.kt` to use this snapshot.

---

## 🟢 Recently Resolved Issues (July.23.01)
*   **Issue #522: SIT Logic Hardening & Forensic Pipeline Unification**.
    *   **Resolution**: Optimized the forensic Sit-Detection (SIT) heuristic in `LocationSentinel.kt`. Unified the local telemetry pipeline in `TrackerService.kt` to feed all 7+ forensic parameters directly into the processor.
