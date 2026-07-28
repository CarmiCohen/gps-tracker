# Project Issues & Hardening Tracking (July.28.14)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 445 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #609] [Severity: Med] [Category: Architecture] Health State Fragmentation**.
    - **Concern**: System health data is currently manually pulled from `IntegrityMonitor` and pushed to `TelemetryRepository` during service ticks. This creates multiple points of failure and makes the "Snapshot" inconsistent if ticks are delayed or suppressed by OS power management.

---

## 🔴 Open Issues
*   **[Issue #609] [Severity: Med] [Category: Structural] Centralized Health Snapshot**.
    - **Objective**: Refactor `IntegrityMonitor` to provide a unified health snapshot as a single source of truth. Ensure services and UI consume a consistent state without manual propagation overhead.

---

## 🟢 Recently Resolved Issues (July.27.13)
*   **[Issue #608] [Severity: Low] [Category: UX] Startup Notification Flicker**.
    - **Resolution**: Eliminated the visual flicker where the foreground service would briefly display a default title ("Tracking system active") before rich telemetry was available. Enhanced `AppNotificationManager` with `getPulseMessage()` and updated `TrackerService`/`ViewerService` to perform a synchronous health check during `startForeground()`, ensuring the initial notification contains rich metadata (Battery, Security Status) immediately.
    - **Validation**: Verified that services now boot with full status strings. Added **R608** to `SOT_MASTER_REQUIREMENTS.md`.

## 🟢 Recently Resolved Issues (July.27.12)
*   **[Issue #607] [Severity: Critical] [Category: Stability] Foreground Service Startup Race Condition (Bad Notification)**.
    - **Resolution**: Eliminated the race condition where `BaseMonitorService` attempted to start in the foreground before the notification channel was initialized. Introduced a synchronous `onServicePreInit()` hook in the base class, allowing subclasses to configure the `NotificationManager` (setting Tracker/Viewer roles) before `startForeground()` is invoked on the Main thread in `onCreate()`.
    - **Validation**: Verified that `TrackerService` and `ViewerService` correctly initialize their respective notification modes synchronously.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
