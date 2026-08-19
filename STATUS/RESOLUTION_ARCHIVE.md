# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 648**

## 68. UI Artifact & BiDi Remediation (Aug.18.08)
*   **Issue #205: UI String Rendering Artifacts**.
    - **Resolution**: Identified that strange numbering (`.1`, `.2`) and leading dots in technical screens were caused by BiDi (bidirectional) text mirroring on the test device. Hardened `SettingsComponents.kt` by wrapping technical overlays (e.g., `PhoneSetupOverlay`) in a forced LTR `CompositionLocalProvider`. This ensures that technical telemetry and numbering render consistently regardless of device locale or mirroring settings. (R205)

## 67. Samsung-Specific Permission Hardening (Aug.18.08)
*   **Issue #206: Samsung-Specific Permission Navigation**.
    - **Resolution**: Hardened the `ACTION_MANAGE_OVERLAY_PERMISSION` intent in `MainActivity.kt`. Added a fallback mechanism that catches exceptions when the `package:` URI is rejected by the OS (observed on Samsung A15/API 35) and re-launches the intent without the URI to ensure the user can still reach the settings page. (R206)

## 66. Diagnostic Stress Isolation (Aug.18.08)
*   **Issue #204: Diagnostic Stress Isolation (Sensor Sampling Rates)**.
    - **Resolution**: Implemented temporary diagnostic down-sampling to isolate the impact of high-frequency sensor processing and telemetry logging. Forensic sampling intervals were reduced from 100Hz/10Hz to 4Hz (250ms) and 2Hz (500ms) in `EngineConstants.kt`. Hardware IMU listeners in `AppSensorManager.kt` were switched from `SENSOR_DELAY_FASTEST` to `SENSOR_DELAY_NORMAL`. This diagnostic state allows isolation of context-switching and I/O pressure as root causes for thermal and battery instability. (R204)

... (rest of archive)
