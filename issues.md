# Project Issues & Hardening Tracking (July.24.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 378 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *None currently identified.*

---

## 🟢 Recently Resolved Issues (July.24.03)
*   **Background Worker Compilation Failure (Issue #536)**.
    *   **Resolution**: Hardened Hilt worker injection by strictly adhering to `@AssistedInject` patterns in `BootServiceStartWorker` and `MaintenanceWorker`. Removed property-level `context` declarations that triggered `NonExistentClass` errors during annotation processing.
*   **Step Detector Permission Stalling (Issue #098)**.
    *   **Resolution**: Implemented a two-tier reactive recovery. `MainViewModel` detects permission grant transitions and signals the `TrackerService`. The Service then performs a synchronous capability refresh and triggers aggressive sensor re-registration in `AppSensorManager`, bypassing OS propagation lag.

## 🟢 Recently Resolved Issues (July.24.02)
*   **Startup ANR Mitigation (Issue #534)**.
    *   **Resolution**: Implemented a 10s startup suppression window for Foreground Service updates in both `TrackerService` and `ViewerService`.
*   **IPC Congestion Hardening (Issue #535)**.
    *   **Resolution**: Enforced a 10,000ms global throttle for `updateForegroundServiceType`.

## 🟢 Recently Resolved Issues (July.24.01)
*   **Permission Refresh Logic Hardening**.
    *   **Resolution**: Hardened `SystemStatusProviderImpl.kt` with a `Mutex`-protected synchronous path for `getPermissionState(forceRefresh = true)`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*)
