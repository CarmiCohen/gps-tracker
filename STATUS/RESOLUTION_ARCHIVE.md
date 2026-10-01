# 🏛️ Resolution Archive - Oct.1.7

## 🏁 Issue #1402-B: System-Wide Alarm Overlay Failure
*   **Resolved**: Oct.1.7
*   **Root Cause**: Dependence on `fullScreenIntent` for background promotion was unreliable on restrictive OEMs (Xiaomi, Samsung) when the device was unlocked. Background activity starts were suppressed, preventing the Red Alert screen from appearing over other apps.
*   **Remediation**: 
    *   **System Overlay**: Implemented `AlarmOverlayService` utilizing `SYSTEM_ALERT_WINDOW` to render the `AlarmOverlay` directly to the `WindowManager`.
    *   **Permission Integration**: Updated `AppNotificationManager` to start the overlay service when permission is granted and added a dedicated action to the notification to navigate users to the permission settings.
    *   **Unified UI**: Re-used `AlarmOverlay` composable within the service's `ComposeView` to ensure UI consistency between Activity and Overlay modes.
*   **Significance**: High (Guaranteed Emergency Visibility).
*   **SOT ID**: 578

---

# 🏛️ Resolution Archive - Oct.1.6

## 🏁 Issue #1409/1410/1412: Alarm Signaling & UI Hardening
*   **Resolved**: Oct.1.6
*   **Root Cause**: 
    1.  Connectivity alerts (Signal Loss, GPS Stall) were incorrectly categorized as "Special", causing sirens and Red-Screen promotion without visual context.
    2.  Manual "Stop Siren" action used a short cooldown, allowing re-triggering under specific race conditions or high-frequency telemetry updates.
    3.  Ribbon time scales in `SharedUiComponents.kt` were occluded by drawings due to insufficient bottom-reserved space and tick-alignment jitter.
*   **Remediation**: 
    *   **Logic Isolation**: Hardened `isSpecialType` in `AppAlarmManager` to exclude connectivity events, making them notification-only.
    *   **Persistence**: Standardized manual silence to 5m duration (`SILENCE_TIMEOUT_MS`) via `SirenLockoutUseCase`.
    *   **UI/UX**: Standardized connection header to "CON"; increased ribbon baseline offsets and refined tick alignment logic for large-scale views (4H/24H/7D).
    *   **Reactive Promotion**: Implemented reactive notification summary updates in `AppEventCoordinator` to ensure full-screen intents are refreshed on alarm state changes.
*   **Significance**: High (System Integrity & UX Clarity).
*   **SOT ID**: 575, 576, 577

---

# 🏛️ Resolution Archive - Oct.1.5

...
*(Full historical records maintained in SOT Archive)*
