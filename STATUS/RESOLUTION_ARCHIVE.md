# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.42 (vSep.02.42)
*   **Issue #898 RESOLVED: HUD Telemetry Stalled in Tracker Mode**.
    *   **Problem**: `MainViewModel` was missing observation logic for `localLocation` and `trackerLocation` flows from `MainRepository`. In tracker mode, telemetry remained at baseline values despite active background sampling by `TrackerService` (R3.1).
    *   **Remediation**: Integrated observation of `localLocation` and `trackerLocation` flows in `MainViewModel.startHeavyObservations()`. Refactored `handleLocationUpdateInternal` to use `TelemetryUseCase` for comprehensive mapping of location, health, and diagnostic data into the `KinematicState` and related UI flows.

## 🟢 Sep.02.41 (vSep.02.41)
*   **Issue #897 RESOLVED: Sensor Sensitivity Sliders Disconnected**.
    *   **Problem**: Vibration and Tilt sliders in UI updated `AlertSettings`, but values were not propagated to the core engine. `MainAlarmLogic` was using hardcoded constants, rendering user sensitivity adjustments ineffective (R2.3).
    *   **Remediation**: Propagated sensitivity settings to the engine (R897). Added `vibrationSensitivity` and `tiltSensitivity` to `AlarmEvaluationState`. Refactored `SentinelValidator` to map the 0.0-1.0 slider range to physical thresholds: Tilt (25° to 5°) and Shock (1.4g to 0.2g). Updated `AppAlarmManager` to sync these values during each evaluation cycle.

## 🟢 Sep.02.40 (vSep.02.40)
*   **Issue #896 RESOLVED: Battery Optimization Navigation Failure**.
    *   **Problem**: The "Open Settings" button in `PhoneSetupOverlay` for Battery Mode failed to trigger the `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent on Samsung A15 devices.
    *   **Remediation**: Implemented robust battery navigation (R896). Hardened `MainActivity.launchBatteryExemptionSetting` with a multi-tier fallback and standardized URI encoding.

---
*For older resolutions, see prior sub-versions.*
