# 🏛️ Resolution Archive - Oct.1.8

## 🏁 Issue #1410: Viewer Persistence (Recurring Alarms)
*   **Resolved**: Oct.1.8
*   **Root Cause**: Alarm acknowledgment state (`lastAlarmAckTs`) was stored in local storage. Fresh installations on the Viewer lost this state, causing them to treat ongoing Tracker violations (like hardware tamper) as new triggers.
*   **Remediation**: 
    *   **Global Authority**: Migrated acknowledgment state to the telemetry stream. The Tracker now broadcasts its `lastAlarmAckTs`.
    *   **Remote Signaling**: Viewers now emit `acknowledge_alarm` to the Tracker to update the global authority timestamp.
    *   **Idempotent Logic**: Hardened `MainAlarmLogic` to auto-resolve violations triggered prior to the current `lastAlarmAckTs`.
*   **Significance**: High (System Integrity & UX).
*   **SOT ID**: 579

---

# 🏛️ Resolution Archive - Oct.1.7

## 🏁 Issue #1402-B: System-Wide Alarm Overlay Failure
*   **Resolved**: Oct.1.7
*   **Root Cause**: Dependence on `fullScreenIntent` for background promotion was unreliable on restrictive OEMs.
*   **Remediation**: Implemented `AlarmOverlayService` utilizing `SYSTEM_ALERT_WINDOW`.
*   **Significance**: High (Guaranteed Emergency Visibility).
*   **SOT ID**: 578

...
