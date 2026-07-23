# Project Issues & Hardening Tracking (July.24.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 376 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Hilt-Work Kapt Regression**: Recent dependency alignment for background workers is triggering `NonExistentClass` errors during stub generation.
*   **IPC Congestion**: Even with throttling, frequent notification requests from multiple threads can still put pressure on the system's `NotificationManagerService`.

---

## 🔴 Open Issues
*   **Background Worker Compilation Failure**: `BootServiceStartWorker` and `MaintenanceWorker` failing to compile due to Hilt annotation processing errors.

---

## 🟢 Recently Resolved Issues (July.24.01)
*   **Stale Permission Detection (Issue #098)**.
    *   **Resolution**: Hardened `SystemStatusProviderImpl.kt` by replacing background-only refreshes with a `Mutex`-protected synchronous path for forced refresh requests. This ensures the UI reflects the true OS permission state immediately after a user grant.
*   **Delayed Step Detector Recovery**.
    *   **Resolution**: Implemented reactive sensor synchronization in `MainViewModel.kt`. The app now detects when `ACTIVITY_RECOGNITION` transitions to `GRANTED` and immediately commands the background service to re-register sensors, bypassing the previous 5-minute failure recovery loop.

## 🟢 Recently Resolved Issues (July.23.12)
*   **Main-Thread Notification Flood (ANR)**.
    *   **Resolution**: Implemented hard throttling (2s/5s) in `AppNotificationManager.kt` and `BaseMonitorService.kt`. Suppression of status updates when the system is not explicitly "Active" ensures the Landing Page remains responsive.
*   **Auto-Restoration Permission Stalling**.
    *   **Resolution**: Hardened `MainAppContent.kt` to verify critical permissions (including `ACTIVITY_RECOGNITION`) during the cold-start restoration flow. Missing permissions now trigger the request launcher instead of allowing the service to start and fail in the background.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*)
