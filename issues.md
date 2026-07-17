# Project Issues & Hardening Tracking (July.17.00)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 303 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Budget Hardware Initialization Spikes (A15 Authority)**: Budget devices are extremely sensitive to Main-thread blocking during cold start. The "Lazy Cascade" (where accessing one lazy property triggers a chain of heavy initializations like Room DB) must be avoided on the Main thread.
*   **Lazy Safety (PUBLICATION mode)**: Using `PUBLICATION` mode prevents Main-thread stalls by allowing concurrent initialization, but we must ensure that the Main thread doesn't "win" the race and perform heavy work. Proactive background warm-up is mandatory.
*   **Foreground Service Timeout**: While `startForeground()` is now decoupled from the database, it must still be called promptly. We defer logic, but not the notification binding itself.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.17.00)
*   **Issue #526: A15 Landing Page Hang (Definitive Root-Cause Fix)**: 
    *   **Architecture**: Transitioned `AppContainer` to use `LazyThreadSafetyMode.PUBLICATION` and moved all UseCase creation into the container as lazy properties. This makes `MainViewModelFactory` zero-cost on the Main thread.
    *   **Service Hardening**: `BaseMonitorService` now defers logic and hardware binding to background scopes, ensuring `onCreate` returns instantly.
    *   **Notification Decoupling**: Refactored `AppNotificationManager` to be independent of the `Repository/Database` chain, allowing instant foreground service binding.
    *   **Background Warm-up**: Implemented proactive priming of the Room DB and Managers in `GpsApplication` on `Dispatchers.IO`.
    *   **Version Authority**: Hard-locked version to `July.17.00` to resolve stale version display issues.
*   **Issue #525**: Acoustic Fast-Path lockout timestamp not propagated to engine.
*   **Issue #524**: Stale `powerAlarmPending` flag in `AlarmHistory`.
*   **Issue #523**: `IntegrityMonitor` Thread Safety.
*   **Issue #522**: `LocationProcessor` Stationary Jitter (Anchor logic).
*   **Issue #521**: `LocationSentinel` Passive Zeroing.
