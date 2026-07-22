# Project Issues & Hardening Tracking (July.22.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 335 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.

---

## 🔴 Open Issues
*None.*

---

## 🟢 Recently Resolved Issues (July.22.08)
*   **Issue #113: Samsung A15 Fallback Hardening (R405c)**.
    *   **Resolution**: Upgraded the Accelerometer "Stay-Alive Pulse" to perform a hardware "poke" via `SystemMonitor.acquireWakeLock(force = true)` every 10 seconds when the hardware Step Detector is missing or registration fails. This ensures the process signals activity to the OS Power Manager, preventing eviction on budget hardware.
*   **Issue #121: Provider Latency Optimization**.
    *   **Resolution**: Implemented lazy thread-safe caching of the `ConnectivitySuite` instance in `LogManager.kt`. This reduces the overhead of circularity resolution via `Provider<T>` in high-frequency logging paths.
*   **Issue #126b: DI Leftover Purge**.
    *   **Resolution**: Physically decommissioned `AppContainer.kt` and `MainViewModelFactory.kt`. Scrubbed all legacy comments and historical references from `BaseMonitorService.kt`, `GpsApplication.kt`, and the primary engine infrastructure to ensure architectural purity.
*   **Issue #104b: Global Startup Maintenance Authority**.
    *   **Resolution**: Extended the proactive `deepPruneLogs` operation (Requirement R104) to the background service layer via `BaseMonitorService.kt`. This ensures that even background-initiated startups benefit from log pruning, preventing I/O bottlenecks and ANRs on budget hardware.

## 🟢 Recently Resolved Issues (July.22.07)
*   **Issue #031: Stability Audit Hardening**.
    *   **Resolution**: Standardized stability auditing across both `TrackerService` and `ViewerService`. Fixed a logic error in the Viewer role where the audit condition prevented execution.
*   **Issue #108: Startup Recovery Race Hardening**.
    *   **Resolution**: Implemented a `RECOVERY_GRACE_PERIOD_MS` (60s) in `MaintenanceWorker.kt` to protect the staggered startup sequence (Requirement R955b).
