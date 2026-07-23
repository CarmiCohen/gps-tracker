# Project Issues & Hardening Tracking (July.24.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 376 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #098: Step Detector Permission Stalling**: On Samsung A15 hardware, the Step Detector fails to register even after permissions are granted. A race condition exists between UI permission grants and background service capability re-evaluation.
*   **Issue #536: Worker Dependency Conflict**: Recent dependency alignment for background workers is triggering `NonExistentClass` errors during Hilt stub generation.

---

## 🔴 Open Issues
*   **Issue #098: Step Detector Permission Stalling**: Investigate why `ACTIVITY_RECOGNITION` is reported as missing by the engine after grant. Implement reactive re-registration (Partially implemented via MainViewModel sync).
*   **Issue #536: Background Worker Compilation Failure**: Resolve Hilt annotation processing errors in `BootServiceStartWorker` and `MaintenanceWorker`.

---

## 🟢 Recently Resolved Issues (July.24.02)
*   **Startup ANR Mitigation (Issue #534)**.
    *   **Resolution**: Implemented a 10s startup suppression window for Foreground Service updates in both `TrackerService` and `ViewerService`. This prevents main-thread starvation during the critical cold-start initialization phase on budget hardware.
*   **IPC Congestion Hardening (Issue #535)**.
    *   **Resolution**: Enforced a strict 10,000ms global throttle for `updateForegroundServiceType`. This drastically reduces redundant IPC calls to the system's `NotificationManagerService` when the UI pulses.

## 🟢 Recently Resolved Issues (July.24.01)
*   **Permission Refresh Logic Hardening**.
    *   **Resolution**: Hardened `SystemStatusProviderImpl.kt` with a `Mutex`-protected synchronous path for `getPermissionState(forceRefresh = true)`.
*   **Reactive Sensor Synchronization Protocol**.
    *   **Resolution**: Updated `MainViewModel.kt` to detect permission grant transitions and signal the background service immediately.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*)
