# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.45 (vSep.02.45)
*   **Issue #122 RESOLVED: Hardware Settling Window Verification**.
    *   **Problem**: Potential native race conditions during hardware teardown on Android 15/Samsung A15 hardware required verification that the 800ms settling window was effective and that all listeners were unregistered before thread death (R891).
    *   **Remediation**: Enhanced `HardwareProvider.stop()` with an `isTeardownActive` atomic gate to prevent callbacks during disposal. Implemented forensic duration tracking for each component (GNSS, Location, Sensors, Display) and added a teardown summary report. Verified that the 800ms settling window provides sufficient buffer for OS-level cleanup.

## 🟢 Sep.02.44 (vSep.02.44)
*   **Issue #893 RESOLVED: Native Resource Disposal Leak Hardening**.
    *   **Problem**: Budget hardware (Samsung A15) exhibited `BaseEventQueue` disposal failures on Android 15 when hardware listeners were unregistered from non-Looper threads or without sufficient settling time.
    *   **Remediation**: Hardened `ManagedHardware` pattern (R893). Standardized `MainLooper` alignment for `ManagedNetworkCallback` and `FusedLocationProvider` registrations. Verified deterministic unregistration sequence in `HardwareProvider.stop()` and confirmed the 800ms settling window before thread termination. Added forensic duration logging to `ManagedUnregistrationHelper`.

## 🟢 Sep.02.43 (vSep.02.43)
*   **Issue #894 RESOLVED: ContextShadow Coverage Expansion**.
    *   **Problem**: Multiple core services (`SystemStatusProvider`, `SystemMonitor`, `AppNotificationManager`) and utilities were using the base `ApplicationContext` for system service lookups. This bypassed the `getOpPackageName` optimizations in `ContextShadow`, causing redundant IPC diagnostic log spam on Samsung A15/Android 15 (R1.14).
    *   **Remediation**: Expanded `ContextShadow` usage to all high-frequency system service access points. Standardized the pattern of creating a local `shadowContext` within singleton services to ensure all downstream framework calls use the optimized package name authority.

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
