# Forensic Handover (Oct.1.6 - ALARM SIGNALING & UI)

## 🎯 Current System State
*   **Version**: `Oct.1.6` | **Status**: HARDENED (Alarm Logic & Ribbons).
*   **Alarm Signaling**:
    *   **Coupled Logic**: Sirens and Red-Screen promotion are now strictly coupled with "Special" alarms.
    *   **Connectivity Suppression**: `SIGNAL_LOSS` and `GPS_STALL` are removed from `AppAlarmManager.isSpecialType` (Line 245), making them notification-only.
    *   **Manual Silence**: User-initiated stops now enforce a 5-minute lockout (`SILENCE_TIMEOUT_MS`) via `SirenLockoutUseCase`.
*   **UI Hardening**:
    *   **Analytical Ribbons**: Standardized connection header to "CON". Adjusted `baseLineY` (Line 414) and `bottomReserved` (Line 413) in `SharedUiComponents.kt` to prevent drawing overlap with time scale numbers.
    *   **Tick Spacing**: Refined `tickIntervalMs` and `tickAlignMs` (Lines 378-400) for large-scale ribbons (4H/24H/7D) to ensure legibility.
*   **Reactive Promotion**: `AppEventCoordinator` reactively updates high-priority notification summary (Line 245) whenever the active special alarm list changes.

## 🔴 Open Gaps (High Priority)
*   **Issue #1402-B (System Overlay)**: The app uses `fullScreenIntent` and `showWhenLocked`. However, true `SYSTEM_ALERT_WINDOW` floating overlay (drawing over other apps while unlocked) is not yet implemented. Audit is needed on Xiaomi/Samsung devices to see if current activity promotion is sufficient.
*   **Issue #1410 (Viewer Persistence)**: Investigate reports of recurring alarms on Viewer after re-install; check for server-side acknowledge replays.

## 🚀 Resumption Focus: System-Level Alerting
*   **Immediate Path**: 
    1.  Verify `AlarmActivity` visibility when app is backgrounded and device is unlocked.
    2.  If promotion fails, implement `SYSTEM_ALERT_WINDOW` in `AppNotificationManager` and `AlarmActivity`.
    3.  Audit `MainViewModel` (Line 383) to ensure `isRedScreenVisible` reactive trigger doesn't conflict with manual dismissals during high-frequency telemetry.

---

## 📊 Hardening Progress Dashboard (Oct.1.6)
- **Status**: [SOT Count: 246 (Rules: 93), Open: H:1, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 342]
- **Audit Record**: Connectivity sirens decoupled; Ribbon spacing refined; 5m lockout enforced.
