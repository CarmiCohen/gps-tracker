# Project Issues & Hardening Tracking (July.27.11)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 443 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #607] [Severity: Critical] [Category: Stability] Foreground Service Startup Race Condition (Bad Notification)**.
    - **Risk**: `BaseMonitorService` attempts to start in the foreground before the notification channel is created by the subclass. This triggers a `CannotPostForegroundServiceNotificationException` on Android 14+ (SAMSUNG A15), leading to immediate app crash.
    - **Mitigation**: Move notification channel initialization to the earliest possible point in the service lifecycle, ensuring it precedes any `startForeground` calls.

*   **[Issue #606] [Severity: High] [Category: Performance] Budget Hardware cold-start ANR (Samsung A15)**.
    - **Risk**: Restricted CPU and I/O on A15 devices lead to Main Thread starvation during cold start when high-frequency telemetry and background maintenance tasks collide.
    - **Mitigation**: Implemented extreme UI throttling (3s sampling), deferred I/O maintenance (10s), and offloaded platform GPS callbacks to a dedicated HandlerThread.

---

## 🔴 Open Issues
*   **[Issue #607] Foreground Service Startup Race Condition**.
    - **Symptoms**: `android.app.RemoteServiceException$CannotPostForegroundServiceNotificationException: Bad notification for startForeground`.
    - **Root Cause**: `BaseMonitorService.onCreate()` calls `startServiceForeground()` before `onServiceInitialize()` has a chance to set the tracker/viewer mode and create the notification channel.

---

## 🟢 Recently Resolved Issues (July.27.11)
*   **[Issue #606] [Severity: High] [Category: Performance] Budget Hardware Stability Hardening**.
    - **Resolution**: Applied extreme hardening measures for restricted hardware (Samsung A15). Migrated GPS/GNSS callbacks from Main Looper to a dedicated `HandlerThread` in `GpsManager`. Implemented 3000ms UI state sampling in `MainViewModel` using `Flow.sample()`. Deferred cold-start `proactivePruning` by 10s to prioritize input dispatch. Raised latency audit thresholds in `EngineConstants` to reduce recursive log churn.
    - **Validation**: Observed UI responsiveness on Samsung A15 during setup interaction. `:app:assembleDebug` success.

## 🟢 Recently Resolved Issues (July.27.08)
*   **[Issue #604] [Severity: Low] [Category: UI] Ribbon Density & Aliasing Audit**.
    - **Resolution**: Refined `TelemetryAggregator.MutableAggregationPoint.merge()` to utilize peak-retention logic (`max()`) for `kineticEnergy` and `sitShock` forensic indices.
    - **Validation**: Verified peak retention in `TelemetryAggregator` logic.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
