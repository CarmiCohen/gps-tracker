# Project Issues & Hardening Tracking (July.20.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 311 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #114 (Risk Note): Monotonic/Wall-Clock Desync**: While investigated and deemed "safe" (1000-point cap prevents OOM), extreme system clock manipulation while the app is killed will still result in intentional timeline gaps. This is an accepted forensic trade-off.

---

## 🔴 Open Issues

### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm that the Accelerometer-based stay-alive pulse (R405c) is sufficient to prevent OS-level service eviction when the hardware Step Detector fails to register.

---

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #117: ViewerService Compilation Restoration**.
    *   **Root Cause**: Unresolved references to `trackerValueLuxBaseline` and `trackerValueAcousticFloorDb` caused by property name mismatch between `RemoteHandler` and `ViewerService`.
    *   **Resolution**: Corrected variable names in `ViewerService.evaluateAlarmsInternal` to match `RemoteHandler` implementation.
*   **Issue #107: Step Detector Hardware Registration Hardening**.
    *   **Root Cause**: Missing `ACTIVITY_RECOGNITION` permission (Manifest and Runtime) prevented hardware Step Detector registration on API 29+.
    *   **Resolution**: Implemented full permission lifecycle (Manifest entry + `MainAppContent` runtime request + `DiagnosticsScreen` health check).
*   **Issue #114: Monotonic Timeline Boundary Audit**.
    *   **Resolution**: Audited `TelemetryAggregator.kt` backfill logic. Verified that the `MAX_BACKFILL_POINTS` limit (1000) and `maxGapMs` cap provide robust protection against memory exhaustion during multi-year system clock drifts.
*   **Issue #115: Startup Scope Hardening (GlobalScope Removal)**.
    *   **Resolution**: Migrated `osmdroid` and `WorkManager` setup to a managed `@ApplicationScope`.
*   **Issue #109 & #111: Startup Performance Hardening**.
    *   **Resolution**: Offloaded I/O intensive operations to `Dispatchers.IO` and decoupled pruning from the UI path.
*   **Issue #110 & #112: mbrainSDK Log Suppression**.
    *   **Resolution**: Filtered vendor-specific `libmbrainSDK` noise in the global `Timber` tree.
*   **Issue #108: MaintenanceWorker Startup Recovery Race**.
    *   **Resolution**: Implemented immediate timestamp refresh in service `onCreate()` methods.

## 🟢 Recently Resolved Issues (July.20.06)
*   **Release Finalization & Version Synchronization**.
*   **Issue #106: Unified Forensic Ribbon Continuity (R106)**.
