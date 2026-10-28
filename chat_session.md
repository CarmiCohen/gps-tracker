# Chat Session Export - Oct28.16

## 🎯 Task: Remediate Issue #1391 (Alarm Leakage on Tracker)

### 🕵️ Investigation Log

1.  **Issue Identification**: Located Issue #1391 in `STATUS/backlog_shards/issue_1391.md`. The core problem is that alarms (sirens and red screen overlays) are manifesting locally on devices in **Tracker Mode**, violating requirement **R872**.
2.  **Architecture Review**:
    *   `DEVELOPER_GUIDELINES.md` reviewed for compliance.
    *   `issues.md` reviewed for current baseline (Oct27.10).
3.  **Component Analysis**:
    *   **AudioSynthesizer.kt**: `playSiren` includes an `isTrackerMode` check which logs suppression.
    *   **AppEventCoordinator.kt**: `observeSirenRequirement` calls `audioSynthesizer.playSiren` passing `configManager.isTrackerMode`.
    *   **AppAlarmManager.kt**: `shouldPlaySiren()` returns `false` if `isTrackerMode` is true.
    *   **MainAppContent.kt**: `DialogHost` checks `sessionState.appMode == "viewer"` before displaying the `AlarmOverlay`.
    *   **AppNotificationManager.kt**: `updateAlarmNotification` checks `isTrackerMode` and suppresses both the system-wide `AlarmOverlayService` and the full-screen notification intent.
    *   **MonitorService.kt**: Correctly sets the tracker mode in `notificationManager` and `configManager` during initialization and role transitions.
    *   **AlarmOverlayService.kt**: Derived `HudState` locally, but doesn't explicitly check for Tracker mode itself, relying on `AppNotificationManager` to not start it.

### 🚩 Findings & Preliminary Root Cause
*   Although checks exist in several places, the issue report suggests leakage is still occurring.
*   Possible leaks in `UiEventCoordinator.kt`: `UiEvent.ToggleTestSiren` and `UiCommand.ExecuteTestAlarm` in `CommandRouter.kt` were inspected.
*   `CommandRouter.kt`'s `ExecuteTestAlarm` handling already suppresses in Tracker mode.
*   `UiEventCoordinator.kt`'s `ToggleTestSiren` does **not** check for Tracker mode before calling `audioSynthesizer.playSiren`.
*   Requirement **R872** requires absolute silence and darkness on Trackers.

### 🛠️ Planned Remediation (Interrupted)
*   Standardize role-based suppression across all siren triggers.
*   Ensure `AlarmOverlayService` cannot run if the local device is a Tracker, even if started maliciously or via race condition.
*   Verify `AudioSynthesizer.playShortAlert` (used for minor feedback) also respects stealth.

---
**Exported per user request. All processing stopped.**
