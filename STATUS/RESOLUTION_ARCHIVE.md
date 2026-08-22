# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 699**

## 97. Performance & JNI Remediation (Aug.21.09)
*   **Issue #248: UI Thread Stall Remediation**.
    - **Resolution**: Implemented granular flow segmentation in `MainViewModel.kt` using a specialized `HudUiParts` data class and `distinctUntilChanged()`. This eliminates the 1070ms UI thread stall by pruning redundant HudState aggregation triggers during telemetry hydration.
*   **Issue #265: JNI Startup Optimization**.
    - **Resolution**: Migrated `JdHardwareManager` to a coroutine-safe `suspend initialize()` pattern. Native library loading and registration are now offloaded to `Dispatchers.IO` with a `Mutex` to prevent UI thread blocking during bootstrap.
*   **Issue #249/262: Native Resource Hardening**.
    - **Resolution**: Implemented missing `n6` (nativeRelease) in `jdhardware-jni.cpp`. Added explicit lifecycle disposal calls in `TrackerService.onDestroy()` and `ViewerService.onDestroy()` to clear native pointers and prevent `BaseEventQueue.dispose` failures.
*   **Issue #257/271: Samsung I/O Mitigation**.
    - **Resolution**: Aligned background maintenance in `BaseMonitorService.kt` with the 15s `STAGGERED_IO_PRUNING_DELAY_MS`. This eliminates I/O competition with Samsung's Kumiho package auditing during the launch window.

## 96. Forensic Validation & UI Integration (Aug.21.08)
*   **Issue #196-V: Forensic Validation Hook UI**.
    - **Resolution**: Integrated the `SetForensicSimulation` toggle into the `DiagnosticsScreen`. This provides a manual trigger for simulating urban multipath and IO latency spikes, enabling verification of EMA reliability degradation and performance alarms (R196-V).
    - **UI Linkage**: Connected `isForensicStallSimulated` state to `MainAppContent` and established the event path from `DiagnosticsScreen` to `MainRepository`.

## 95. Forensic Hardening & Simulation Path (Aug.21.06)
*   **Issue #196: Forensic Pipeline Hardening**.
    - **Hysteresis Logic**: Implemented overflow hysteresis in `LogManager.kt`. Alerts now only reset when buffer pressure drops below 50%, preventing alert oscillation during high-frequency (100Hz) bursts on budget hardware (R196).
    - **Range-Based Deduplication**: Optimized `LogRepository.kt` to use range-based signature queries (`getExistingForensicSignaturesInRange`). This eliminates full-history table scans, significantly reducing CPU and Memory pressure on the SM-A155F (R197).
    - **Simulation Hook**: Added `setForensicStallSimulation` and `SetForensicSimulation` UI event to allow urban multipath validation without physical hardware movement (R196-V).
*   **Issue #570: TrackerStatus Typo Fix**.
    - **Resolution**: Fixed a typo in `TrackerStatus.toMap` where `violationUptimeMs` was incorrectly referenced as `violation_uptime_ms`, preventing correct telemetry mapping for remote viewers.

## 94. UI Optimization & JNI Verification (Aug.21.04)
*   **Issue #246: UI Thread Optimization**.
    - **Resolution**: Mitigated Davey stalls (>700ms) on Samsung A15 hardware by consolidating the staggered UI hydration sequence in `SettingsComponents.kt` from 10+ steps to 3 broad phases. (R246).
*   **Simplify Idea #1: Reusable Sensitivity Slider**.
    - **Resolution**: Extracted duplicate slider logic into a unified `SensitivitySlider` component. (R246-S).
*   **Issue #244: Native Library Audit**.
    - **Resolution**: Verified successful runtime loading and initialization of `libjdHardware.so` on target SM-A155F hardware. (R244).

## 93. Sensor Calibration Restoration (Aug.21.01)
*   **Issue #247: UI Component Regression**.
    - **Resolution**: Restored missing vibration and tilt sensitivity sliders in `AlertManagementOverlay`. tieing them to `AlertSettings` (R247).

*(Older resolutions preserved in Git history)*
