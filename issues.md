# Project Issues & Hardening Tracking (July.22.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 332 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #121: Provider Latency**: Circularity resolution via `Provider<T>` is stable but introduces minor lookup overhead in `LogManager`.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.

---

## 🔴 Open Issues
*None.*

---

## 🟢 Recently Resolved Issues (July.22.08)
*   **Issue #126b: DI Leftover Purge**.
    *   **Resolution**: Physically removed `AppContainer.kt` and `MainViewModelFactory.kt`. These were historical artifacts from the manual DI era. Removing them ensures the project adheres strictly to the Hilt Universal Authority (R120b).
*   **Issue #104b: Global Startup Maintenance Authority**.
    *   **Resolution**: Extended the proactive `deepPruneLogs` operation (Requirement R104) to the background service layer via `BaseMonitorService.kt`. This ensures that even background-initiated startups (e.g., via `MaintenanceWorker` or `BootReceiver`) benefit from log pruning, preventing I/O bottlenecks and ANRs on budget hardware.

## 🟢 Recently Resolved Issues (July.22.07)
*   **Issue #031: Stability Audit Hardening**.
    *   **Resolution**: Standardized stability auditing across both `TrackerService` and `ViewerService`. Fixed a logic error in the Viewer role where the audit condition prevented execution.
*   **Issue #108: Startup Recovery Race Hardening**.
    *   **Resolution**: Implemented a `RECOVERY_GRACE_PERIOD_MS` (60s) in `MaintenanceWorker.kt` to protect the staggered startup sequence (Requirement R955b).

## 🟢 Recently Resolved Issues (July.22.06)
*   **Issue #113: R405c Fallback Hardening (Samsung A15)**.
    *   **Resolution**: Implemented 5-minute periodic re-registration loop for Step Detector and enhanced Accelerometer fallback pulse visibility.
*   **Issue #125: Baseline Verification**.
    *   **Resolution**: Executed a full clean build to verify Hilt graph integrity.
*   **Issue #126: Complete Hilt Migration and Decommission**.
    *   **Resolution**: Finalized Hilt transition by decommissioning legacy containers in code.
