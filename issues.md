# Project Issues & Hardening Tracking (July.17.02)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 305 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Foreground Service Notification Latency**: Throttling the notification to 30 seconds reduces UI update granularity. If a user needs real-time battery/sats status in the tray, this might be perceived as a lag, though it significantly reduces Logcat noise and system overhead.
*   **Boot Persistence Integrity**: The `isSystemActive` flag is critical for UX. If DataStore fails to save this flag during a crash, the service might not restart correctly.
*   **Budget Hardware Initialization Spikes (A15 Authority)**: Budget devices are extremely sensitive to Main-thread blocking during cold start. The "Lazy Cascade" must be avoided.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.17.02)
*   **Issue #R993: Foreground Notification Spam**:
    *   **Root Cause**: `ViewerService` and `TrackerService` were updating the foreground notification on every 2s engine tick.
    *   **Remediation**: Decoupled notification updates from the engine heartbeat. Introduced `NOTIFICATION_THROTTLE_MS` (30s) in `EngineConstants.kt`.
*   **Issue #R994: Unintended Service Activation on Landing Page**:
    *   **Root Cause**: `BootReceiver` started the engine if `appMode` was set, regardless of whether the system was "Armed/Active".
    *   **Remediation**: Introduced a persistent `isSystemActive` flag in `DataStore`. `BootServiceStartWorker` now strictly requires both `appMode` and `isSystemActive` to be true before reviving the engine.

## 🟢 Recently Resolved Issues (July.17.00)
*   **Issue #526: A15 Landing Page Hang (Definitive Root-Cause Fix)**: 
    *   **Architecture**: Transitioned `AppContainer` to use `LazyThreadSafetyMode.PUBLICATION`.
    *   **Service Hardening**: `BaseMonitorService` now defers logic and hardware binding to background scopes.
    *   **Notification Decoupling**: Refactored `AppNotificationManager` to be independent of the `Repository/Database` chain.
