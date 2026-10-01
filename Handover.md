# Forensic Handover (Oct.1.7 - SYSTEM OVERLAY)

## 🎯 Current System State
*   **Version**: `Oct.1.7` | **Status**: HARDENED (Emergency UI).
*   **System-Wide Alerting**:
    *   **Overlay Service**: `AlarmOverlayService` implemented to draw over other apps using `SYSTEM_ALERT_WINDOW` (R578).
    *   **Permission Awareness**: `AppNotificationManager` reactively prompts for overlay permissions and provides a direct "Fix" action if suppressed.
    *   **Activity Convergence**: `MainActivity` handles `ACTION_FIX_PERMISSIONS` to streamline user recovery from policy blocks.
*   **Alarm Signaling**:
    *   **Coupled Logic**: Sirens and Red-Screen promotion are strictly coupled with "Special" alarms.
    *   **Manual Silence**: User-initiated stops enforce a 5-minute lockout (`SILENCE_TIMEOUT_MS`) via `SirenLockoutUseCase`.
*   **Reactive Promotion**: `AppEventCoordinator` reactively updates notification summary; `AppNotificationManager` handles service-level promotion of the overlay.

## 🔴 Open Gaps (High Priority)
*   **Issue #1410 (Viewer Persistence)**: Investigate reports of recurring alarms on Viewer after re-install; check for server-side acknowledge replays.
*   **Resource Management**: Monitor for `WindowManager` leaks on extreme low-memory devices during long-duration alerts.

## 🚀 Resumption Focus: Field Verification & Stability
*   **Immediate Path**: 
    1.  Verify `AlarmOverlayService` stability under heavy background load.
    2.  Audit battery consumption during sustained "Tamper" states where the overlay is active.
    3.  Confirm cross-role (Tracker -> Viewer) signal convergence for overlay dismissal.

---

## 📊 Hardening Progress Dashboard (Oct.1.7)
- **Status**: [SOT Count: 247 (Rules: 94), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 345]
- **Audit Record**: System-wide overlay implemented; Permission recovery streamlined; Build Oct.1.7 successful.
