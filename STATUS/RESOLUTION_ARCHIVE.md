# 🏛️ Resolution Archive - Oct.1.8

## 🏁 Issue #1414: Dashboard UNKNOWN state (Local Tracker)
*   **Resolved**: Oct.1.8
*   **Root Cause**: Behavioral state mapping (`MOVING`/`PARKING`) was gated by peer connectivity. In standalone mode (no Viewer), the engine reported "UNKNOWN" because `isPeerActive` was false.
*   **Remediation**: 
    *   **Logic Hardening**: Updated `MonitorService.kt` to force `isTrackerConnected = true` when in local Tracker mode.
    *   **Flow Routing**: Updated `MainViewModel.kt` to route local telemetry updates directly to the `_trackerState` flow, ensuring the dashboard reflects behavioral truth regardless of peer presence.
*   **Significance**: High (Diagnostic Accuracy).
*   **SOT ID**: 589

## 🏁 Issue #1413: Mode-Based Alert Violation (Stealth Regression)
*   **Resolved**: Oct.1.8
*   **Root Cause**: The full-screen `AlarmOverlay` (Red Alert) was promoted on Tracker devices, violating the SOT stealth requirement (R872).
*   **Remediation**: 
    *   **UI Guard**: Implemented an `appMode == "viewer"` guard in `MainAppContent.kt` at the root of the overlay promotion block. 
    *   **Badge Redirection**: Tracker-mode violations are now restricted to status bar badges (**ALM**) and logs only.
*   **Significance**: High (Operational Stealth).
*   **SOT ID**: 588

## 🏁 Issue #1410: Engine Thread-Safety (Fatal CME)
*   **Resolved**: Oct.1.8
*   **Root Cause**: `MainAlarmLogic` was mutating the `activeAlarms` map on a background thread without synchronization, while the UI/Event coordinator was iterating over it during the service tick.
*   **Remediation**: 
    *   **Safe Collection**: Migrated `activeAlarms` to a `val ConcurrentHashMap` in `EngineModels.kt`.
    *   **Persistence guard**: Marked as `@Transient` to prevent restoration overrides.
    *   **Atomic Sync**: Synchronized all map mutations in the core logic.
*   **Significance**: Critical (System Stability).
*   **SOT ID**: 585

## 🏁 Issue #1410: Viewer Persistence (Recurring Alarms)
*   **Resolved**: Oct.1.8
*   **Root Cause**: Alarm acknowledgment state (`lastAlarmAckTs`) was stored in local storage. Fresh installations on the Viewer lost this state, causing them to treat ongoing Tracker violations as new triggers.
*   **Remediation**: 
    *   **Global Authority**: Migrated acknowledgment state to the telemetry stream.
    *   **Idempotent Logic**: Hardened `MainAlarmLogic` to auto-resolve violations triggered prior to the current `lastAlarmAckTs` using `violationStartTs`.
*   **Significance**: High (System Integrity & UX).
*   **SOT ID**: 579

---

# 🏛️ Resolution Archive - Oct.1.7

## 🏁 Issue #1402-B: System-Wide Alarm Overlay Failure
*   **Resolved**: Oct.1.7
*   **Root Cause**: Dependence on `fullScreenIntent` for background promotion was unreliable on restrictive OEMs.
*   **Remediation**: Implemented `AlarmOverlayService` utilizing `SYSTEM_ALERT_WINDOW`.
    *   **Oct.1.8 Update**: Hardened teardown with explicit `disposeComposition()` and `removeViewImmediate()` (R-ID 582).
*   **Significance**: High (Guaranteed Emergency Visibility).
*   **SOT ID**: 578
