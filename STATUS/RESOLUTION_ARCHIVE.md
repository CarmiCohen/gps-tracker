# 🏛️ Resolution Archive - Sep.23.70

## 🏁 Issue #1236: Race Conditions during Asynchronous Initialization
*   **Resolved**: Sep.23.70
*   **Root Cause**: The background services (`TrackerService`, `ViewerService`) performed asynchronous initialization (loading settings, restoring state) within `onCreate`. However, the `startTickLoop` and `startHeartbeatLoop` methods could be triggered by external pulses (like UI visibility changes or peer pulses) before `onServiceInitialize` completed, leading to null pointer exceptions or logic execution against unhydrated state.
*   **Remediation**:
    *   Introduced `initializationDeferred: CompletableDeferred<Unit>` in `BaseMonitorService`.
    *   Updated `BaseMonitorService.onCreate` to complete the deferred immediately after `onServiceInitialize()` finishes.
    *   Refactored `startTickLoop()` and `startHeartbeatLoop()` to `await()` the `initializationDeferred` before entering their respective execution cycles.
    *   Updated `TrackerService.startForensicSamplingLoop()` to also await initialization, preventing forensic traces from being captured with zeroed baselines.
*   **R-ID**: 424

## 🏁 Issue #1270 & #1280: Siren Trigger Orchestration
*   **Resolved**: Sep.23.60
*   **Root Cause**: The siren logic was disconnected from the background evaluation loop. While `AppAlarmManager` correctly identified violations, it lacked the imperative calls to `AudioSynthesizer` to start or stop the physical siren, rendering the alarm silent in background mode.
*   **Remiation**:
    *   Integrated `audioSynthesizer.playSiren` and `audioSynthesizer.stopSiren` directly into `AppAlarmManager.evaluateAlarms`.
    *   Implemented role-aware checks to ensure Tracker mode remains silent (stealth) while Viewer mode activates the siren.
    *   Added guards for manual silence overrides (`lastSirenStopRt`) and global mute settings.
    *   Synchronized playback state with the `audioSynthesizer.isPlaying()` flow to prevent redundant call churn.
*   **R-ID**: 423

... [Previous entries preserved] ...
