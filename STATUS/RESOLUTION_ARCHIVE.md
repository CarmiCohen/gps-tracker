# 🏛️ Resolution Archive - Sep.29.3

## 🏁 Issue #1382: Spontaneous & Unstoppable Viewer Siren
*   **Resolved**: Sep.29.3
*   **Root Cause**: Asymmetric alerting orchestration. The `AppEventCoordinator` reactively triggered the physical audio loop via `AudioSynthesizer` but failed to invoke the `AppNotificationManager`. Consequently, the Viewer's critical alarm notification and full-screen `AlarmActivity` were never displayed. Without the Red Screen UI, the user had no visible way to acknowledge and dismiss the underlying violations, leading to the siren re-engaging autonomously after each auto-stop lockout period expired.
*   **Remediation**:
    *   **Exposed Alarm Summary**: Added `getActiveAlarmSummary()` to `AppAlarmManager` to provide a list of active violations.
    *   **Synchronized Alerting**: Updated `AppEventCoordinator.observeSirenRequirement()` to trigger `notificationManager.updateAlarmNotification()` and `cancelAlarm()` in lockstep with the physical siren.
*   **Significance**: High (Safety & UX Integrity).
*   **SOT ID**: 556 (Siren UI Synchronization)

## 🏁 Issue #1387: Documentation Version Mismatch
*   **Resolved**: Sep.29.3
*   **Root Cause**: Documentation headers in `issues.md` and `STATUS/` were manually maintained and had fallen out of sync with the intended development cycle (remained at `Sep.29.3`).
*   **Remediation**: Synchronized version headers across `issues.md`, `Handover.md`, and the `STATUS/` directory to the `Sep.29.3` baseline.
*   **Significance**: Low (Process Integrity).
*   **SOT ID**: 555 (Documentation Version Alignment)

## 🏁 Issue #1388: App Deployment Version Inconsistency
*   **Resolved**: Sep.29.3
*   **Root Cause**: The `versionName` property in `app/build.gradle` was set to `Sep.29.3`, lagging behind the target release version.
*   **Remediation**: Updated `versionName` to `Sep.29.3` to ensure correct HUD and metadata display upon deployment.
*   **Significance**: Low (Process Integrity).
*   **SOT ID**: 555 (Documentation Version Alignment)

## 🏁 Issue #1381: Heartbeat Centralization
*   **Resolved**: Sep.29.3
*   **Remediation**: Moved bypass heartbeat loop to `ConnectivitySuite`, delegating telemetry mapping strictly to `AppEventCoordinator` logic.
*   **Significance**: Medium (Architectural Cleanup).
*   **SOT ID**: 554 (Heartbeat Centralization)

...
*(Full historical records maintained in SOT Archive)*