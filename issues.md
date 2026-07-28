# Project Issues & Hardening Tracking (July.27.12)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 444 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #608] [Severity: Low] [Category: UX] Startup Notification Flicker**.
    - **Concern**: Moving `startForeground` to `onCreate` ensures stability but may cause a brief "Default" notification title to flicker before the subclass-specific message is processed in the next Main loop cycle. This is purely cosmetic but worth noting for visual polish.

---

## 🔴 Open Issues
*   *(No active critical blocking issues)*

---

## 🟢 Recently Resolved Issues (July.27.12)
*   **[Issue #607] [Severity: Critical] [Category: Stability] Foreground Service Startup Race Condition (Bad Notification)**.
    - **Resolution**: Eliminated the race condition where `BaseMonitorService` attempted to start in the foreground before the notification channel was initialized. Introduced a synchronous `onServicePreInit()` hook in the base class, allowing subclasses to configure the `NotificationManager` (setting Tracker/Viewer roles) before `startForeground()` is invoked on the Main thread in `onCreate()`.
    - **Validation**: Verified that `TrackerService` and `ViewerService` correctly initialize their respective notification modes synchronously.

## 🟢 Recently Resolved Issues (July.27.11)
*   **[Issue #606] [Severity: High] [Category: Performance] Budget Hardware Stability Hardening**.
    - **Resolution**: Applied extreme hardening measures for restricted hardware (Samsung A15). Migrated GPS/GNSS callbacks from Main Looper to a dedicated `HandlerThread` in `GpsManager`. Implemented 3000ms UI state sampling in `MainViewModel` using `Flow.sample()`. Deferred cold-start `proactivePruning` by 10s to prioritize input dispatch. Raised latency audit thresholds in `EngineConstants` to reduce recursive log churn.
    - **Validation**: Observed UI responsiveness on Samsung A15 during setup interaction. `:app:assembleDebug` success.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
