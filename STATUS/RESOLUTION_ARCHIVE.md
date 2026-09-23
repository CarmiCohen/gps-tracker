# 🏛️ Resolution Archive - Sep.23.60

## 🏁 Issue #1270 & #1280: Siren Trigger Orchestration
*   **Resolved**: Sep.23.60
*   **Root Cause**: The siren logic was disconnected from the background evaluation loop. While `AppAlarmManager` correctly identified violations, it lacked the imperative calls to `AudioSynthesizer` to start or stop the physical siren, rendering the alarm silent in background mode.
*   **Remiation**:
    *   Integrated `audioSynthesizer.playSiren` and `audioSynthesizer.stopSiren` directly into `AppAlarmManager.evaluateAlarms`.
    *   Implemented role-aware checks to ensure Tracker mode remains silent (stealth) while Viewer mode activates the siren.
    *   Added guards for manual silence overrides (`lastSirenStopRt`) and global mute settings.
    *   Synchronized playback state with the `audioSynthesizer.isPlaying()` flow to prevent redundant call churn.
*   **R-ID**: 423

## 🏁 Issue #1203: Hilt ViewModel Scope Optimization
*   **Resolved**: Sep.23.50
*   **Root Cause**: Role-specific ViewModels were independently subscribing to high-frequency data streams, leading to resource churn and state loss.
*   **Remediation**: Centralized streams into a unified activity-scoped `MainViewModel`.
*   **R-ID**: 415 (Updated)

... [Previous entries preserved] ...
