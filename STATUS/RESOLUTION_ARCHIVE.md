# 🏛️ Resolution Archive - Sep.30.6

## 🏁 Issue #1383: Tracker Map Autonomous Zoom-In
*   **Resolved**: Sep.30.6
*   **Root Cause**: Imperative Camera Trigger Re-entrancy. The `MapController` was reactively triggering Osmdroid camera animations (zoomIn, animateTo) whenever their respective trigger counters in `MapViewState` were non-zero. Since these counters are cumulative and persistent in the state, any subsequent UI state refresh (e.g., pulse update) caused the controller to re-execute the animation, creating an unstoppable zoom-in loop that fought against manual user gestures.
*   **Remediation**:
    *   **Stateful Trigger Tracking**: Implemented local `lastSeen` caches for all camera triggers (`lastCenteringTrackerTrigger`, `lastZoomInTrigger`, etc.) within `MapController.kt`.
    *   **Monotonic Execution**: The controller now only executes imperative animations when the cumulative counter in the state strictly increments compared to the local cache.
*   **Significance**: High (UX Stability & Map Integrity).
*   **SOT ID**: 558 (Stateful Camera Triggers)

## 🏁 Issue #1382: Spontaneous & Unstoppable Viewer Siren
*   **Resolved**: Sep.30.6
*   **Root Cause**: Asymmetric alerting orchestration. The `AppEventCoordinator` reactively triggered the physical audio loop via `AudioSynthesizer` but failed to invoke the `AppNotificationManager`. Consequently, the Viewer's critical alarm notification and full-screen `AlarmActivity` were never displayed. Without the Red Screen UI, the user had no visible way to acknowledge and dismiss the underlying violations, leading to the siren re-engaging autonomously after each auto-stop lockout period expired.
*   **Remediation**:
    *   **Exposed Alarm Summary**: Added `getActiveAlarmSummary()` to `AppAlarmManager` to provide a list of active violations.
    *   **Synchronized Alerting**: Updated `AppEventCoordinator.observeSirenRequirement()` to trigger `notificationManager.updateAlarmNotification()` and `cancelAlarm()` in lockstep with the physical siren.
*   **Significance**: High (Safety & UX Integrity).
*   **SOT ID**: 556 (Siren UI Synchronization)

...
*(Full historical records maintained in SOT Archive)*