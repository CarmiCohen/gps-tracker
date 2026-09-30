# 🏛️ Resolution Archive - Sep.30.6

## 🏁 Issue #1384: Ribbon Time Ruler Legibility
*   **Resolved**: Sep.30.6
*   **Root Cause**: Vertical space starvation and label truncation. The `ForensicRibbonContainer` used a fixed percentage-based baseline that didn't account for the vertical height required by monospaced timestamps on small screens (A15). The label width was also too narrow for time scale indicators like "4M".
*   **Remediation**:
    *   **Reserved Bottom Padding**: Implemented `bottomReserved` logic in `drawWithCache` to guarantee 16-18dp of clear space for text.
    *   **Monotonic Scaling**: Increased Ribbon Ruler height to 46dp (portrait) and expanded label width to 34dp.
    *   **Contrast Enhancement**: Increased overlay opacity to 95%.
*   **Significance**: High (UX & Forensic Integrity).
*   **SOT ID**: 559 (Scale-Aware Ribbon Layouts)

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