# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 580**

## 20. Forensic Stress Validation (Aug.11.05)
*   **Issue #140: Automated Forensic Dashboard Stress Test**.
    *   **Resolution**: Implemented a 5-second multi-threaded saturation routine in `TrackerService`. The test uses `Dispatchers.Default` for trigonometric calculation loops (CPU stress) and `Dispatchers.IO` for 1MB buffer write/read cycles (I/O stress). This provides a repeatable mechanism to verify that the forensic anomaly correlation engine (R133) correctly identifies "Silent Failures" and that UI hydration gates (R137/R139) prevent ANRs even under 90%+ CPU load. (R140)

## 19. Compose Preview Restoration (Aug.11.04)
*   **Issue #136: Update Compose Previews for Decomposed Overlays**.
    *   **Resolution**: Restored Compose Preview functionality for `SettingsOverlay` and `PhoneSetupOverlay` in `SettingsComponents.kt`. Previews were updated to support the decomposed primitive parameters and hydration gating (`isHydrated`) introduced in R135/R137. (R136)

## 18. Tracker Mode Transition Hardening (Aug.11.03)
*   **Issue #139: Persistent ANR on Tracker Mode Transition**.
    *   **Resolution**: Implemented **Deferred UI Hydration** (R139) in `TrackerScreen.kt`. By deferring the rendering of heavy components by 200ms, the navigation transition is allowed to stabilize before the main thread is tasked with expensive UI composition. (R139)

## 17. Service Initialization Hardening (Aug.11.02)
*   **Issue #138: ANR on Tracker Mode Transition**.
    *   **Resolution**: Offloaded all high-frequency flow collections and event observers in `TrackerService` and `ViewerService` to `Dispatchers.Default`. This ensures that service startup does not compete for main-thread resources required for UI rendering. (R138)

## 16. Settings Overlay Optimization (Aug.11.00)
*   **Issue #137: ANR on Settings Overlay Entry**.
    *   **Resolution**: Implemented **Deferred UI Hydration** (R137) in `SettingsOverlay` and `PhoneSetupOverlay`. Content rendering is gated by an internal `isHydrated` state and a 100-150ms delay, eliminating 3000ms+ stalls on budget hardware. (R137)

## 15. UI Transition Stabilization (Aug.10.31)
*   **Issue #135: UI Davey/ANR Mitigation for Overlay Transitions**.
    *   **Resolution**: Refactored `SettingsOverlay`, `PhoneSetupOverlay`, and `DiagnosticsScreen` to use fully decomposed primitive parameters. This isolates complex UI components from high-frequency telemetry recomposition triggers. (R135)

## 14. Forensic Pulse Hardening (Aug.10.29)
*   **Issue #134-Sentinel: Forensic Pulse Frequency Hardening**.
    *   **Resolution**: Implemented a high-frequency "Forensic Pulse" (10s) in `IntegrityMonitor` to reduce the latency of "Silent Failure" detection (CPU/IO correlation). (R134)

## 13. Forensic Anomaly Correlation (Aug.10.28)
*   **Issue #133-Sentinel: Forensic Anomaly Correlation Engine (Silent Failure Detection)**.
    *   **Resolution**: Implemented cross-domain correlation between location stability and hardware resource stress. Added `isSilentFailure` logic to `SentinelValidator` to identify stalls driven by CPU/IO exhaustion (>85% CPU or >800ms IO Latency). (R133)

## 12. Forensic UI Refinement (Aug.10.27)
*   **Issue #132-Sentinel: Forensic UI Dashboard Refinement for Performance Metrics**.
    *   **Resolution**: Integrated `cpuLoad`, `ioWait`, and `maxIoLatency` trends into the Tracker and Viewer Forensic Dashboard UI. (R132)

## 11. Forensic Performance Hardening (Aug.10.26)
*   **Issue #131-Sentinel: Forensic Performance Audit for budget hardware**.
    *   **Resolution**: Integrated rolling maximum I/O latency tracking into `LatencyMonitor` and `IntegrityMonitor`. Hardened performance auditing for budget hardware by triggering forensic alerts upon detecting disk spikes (>1000ms). (R131)

## 10. Forensic Proto Alignment (Aug.10.25)
*   **Issue #130-Sentinel: Proto Health Parity**.
    *   **Resolution**: Synchronized the `RealtimeStatus` Protobuf definition and `TrackerStatus.writeTo` mapping to include `isBatteryLow` and `isBatteryCritical` flags. (R130)

## 9. Forensic Storage Hardening (Aug.10.24)
*   **Issue #129-Sentinel: Forensic Storage Pruning Sensitivity**.
    *   **Resolution**: Hardened database maintenance against battery-induced I/O spikes. Refactored repositories to defer or throttle pruning operations when `isBatteryLow` is detected. (R129)

## 8. Forensic Telemetry & Metadata Hardening (Aug.10.23)
*   **Issue #128-Sentinel: Forensic Metadata Pressure Hardening**.
    *   **Resolution**: Hardened `TelemetryAggregator.kt` against high-frequency ribbon collisions using a stateful `lastEmittedTick` gate to prevent "Aggregation Storms". (R128)

## 7. Forensic Telemetry Hardening (Aug.09.22)
*   **Issue #127-Telemetry: Forensic Drain Latency Hardening**.
    *   **Resolution**: Optimized `ForensicSpillBuffer.kt` for zero-lock contention, refactoring `peek()` and `writeTrace()` to hold locks only for sub-millisecond memory copies. (R127)
*   **Issue #126-Telemetry: Forensic Payload Overflow Audit**.
    *   **Resolution**: Implemented safe UTF-8 truncation to prevent diagnostic message corruption at the 56-byte boundary. (R126)
*   **Issue #125-Telemetry: Forensic Data Compression Parity Audit**.
    *   **Resolution**: Integrated `gpsHardwareLock` into the V2 binary format flags (0x08). (R125)

## 6. Functional Hardening & Revival (Aug.07.07)
*   **Issue #124-Revival: GPS Hardware Revival Functional Hardening**.
    *   **Resolution**: Hardened the 120s GPS revival loop in `GpsManager.kt` and integrated `revivalEvents` into `IntegrityMonitor.kt`. (R124)

## 5. UI/UX & Forensic Hardening (Aug.07.06)
*   **Issue #753: Restoration of Resolution Archive Integrity**.
    *   **Resolution**: Restored truncated historical records in the archive. (R753)
*   **Issue #752: Status Tracking Integrity Synchronization**.
    *   **Resolution**: Synchronized `issues.md` and `RESOLUTION_ARCHIVE.md` baselines. (R752)
*   **Issue #747: Event & Alert Text Unification**.
    *   **Resolution**: Synchronized all system event and alert text with the authoritative mapping. Viewer-local events now use the "This device:" prefix. (R747)
*   **Issue #746: Missing libmbrainSDK**.
    *   **Resolution**: Transitioned the JNI bridge to the `jdMbrain` namespace to eliminate legacy log noise. (R746)

## 4. Stability & Budget Baseline (July.30.35)
*   **Issue #640: Tracker Mode ANR (Regression)**.
    *   **Resolution**: Implemented aggressive 1000ms throttling for heavy map overlay updates in `MapOverlayManager.kt`.
*   **Issue #634: ForegroundServiceStartNotAllowedException Crash**.
    *   **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`.

## 3. Generic Latency Monitoring (July.25.11)
*   **Issue #590: Latency Monitoring Framework**. 
    *   **Resolution**: Implemented unified `LatencyMonitor` in `:core:engine`. (R590)

## 2. Network Lifecycle Hardening (July.25.12)
*   **Issue #545: Production Logging Leak (`StackLog`)**. 
    *   **Resolution**: Implemented idempotent lifecycle management in `ConnectivitySuite`. (R545)

## 1. Kernel & OS Performance Hardening (July.25.13)
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**. 
    *   **Resolution**: Finalized verification stack for Zero-Churn performance and integrated `LatencyMonitor`. (R547)

---

## 21. Legacy Hardening Phase (v9.0.4 - v9.3.0)
*   **Issue #049**: Corrected GlobalStatusBar mapping to use mode-aware location context (v9.2.6).
*   **Issue #044**: Standardized HUD status badges to reflect local device health (v9.2.3).
*   **Issue #030**: Consolidated all schemas into `app/src/main/proto` (v9.3.0).
*   **Issue #400**: Re-anchored Bayesian Uncertainty status messages to the bottom metadata cluster (v9.3.0).
*   **Issue #326**: Enriched Location Pending state with reasons (GPS_GAP, JAMMER) (v9.2.2).
*   **Issue #018**: Implemented coordinate clamping and `isAnchorLocked` flag (v9.2.1).
*   **Issue #048**: Differentiated Telemetry Age from GPS Age in status rows (v9.2.0).
*   **Issue #029**: Propagated local telemetry to repository in Viewer mode (v9.0.3).

## 22. Middle Hardening Era (v8.9.65 - v9.1.7)
*   **Issue #042**: Implemented migration flag to notify UI of auto-sanitization events (v9.3.0).
*   **Issue #041**: Implemented R975 (Regex validation) and automatic storage purging (v8.9.99).
*   **Issue #027**: Reinforced bulk save with atomic uniqueness validation (v8.9.98).
*   **Issue #032**: Implemented `isForensicFresh` gate using `WATCH_DOG_UI_GRACE_MS` (v8.9.96).
*   **Issue #038**: Implemented 5s "Adaptation Muzzle" for A15 polling changes (v8.9.94).
*   **Issue #036**: Introduced hardened sensor mismatch and jitter thresholds (v8.9.94).
*   **Issue #005**: Static user agent and manual storage paths for osmdroid (v8.9.91).
*   **Issue #025**: Increased `UI_PULSE_TIMEOUT_MS` to 45s for Android 14+ (v8.9.86).
*   **Issue #023**: Reverted legacy tags to float and added high-precision doubles (v8.9.84).
*   **Issue #021**: Fixed map loop with single-point trail segments (v8.9.82).
*   **Issue #014**: System-Wide Type Safety. Standardized telemetry fields to `Double` (v9.1.7).
*   **Issue #011**: Added `suppressionNote` to `SentinelResult` (v8.9.68).
*   **Issue #012**: Adaptive Proximity Debounce in `AppSensorManager` (v8.9.71).

## 23. Middle Era Resolutions (#100 - #199)
*   **Issue #199**: Toolchain Modernization. Upgraded to Java 17 and Android SDK 35 (v8.9.8).
*   **Issue #198**: Shortened stall detection to 60s (v8.9.8).
*   **Issue #197**: Added `sitVzTs` to history (v8.9.7).
*   **Issue #195**: Implemented table reconstruction migration (v8.9.6).
*   **Issue #194**: Implemented acknowledged event pipeline (v8.9.7).
*   **Issue #193**: Implemented "Ghost Mode" indicators (v8.9.6).
*   **Issue #337**: Achieved forensic parity for `currentMa` (v8.9.2).
*   **Issue #191**: Implemented deterministic Muzzle Handshake (v8.9.6).
*   **Issue #190**: Xiaomi Autostart status handling and boot grace (v8.9.16).
*   **Issue #189**: 10s background polling for Viewers (v8.9.5).
*   **Issue #188**: Added `gpsTs` to DB and sync (v8.9.3).
*   **Issue #185**: Remote-to-local trail persistence (v8.9.2).
*   **Issue #115**: Extracted feature-specific UseCases (8.8.25).

## 24. Legacy Foundation Resolutions (#1 - #99)
*   **Issue #302**: Removed role-based UI gating from Settings.
*   **Issue #301**: Implemented 2s alert trigger delay.
*   **Issue #320**: Implemented 500ms Muzzle Window (v8.8.21).
*   **Issue #300**: UNKNOWN MIUI status guidance (v8.8.21).
*   **Issue #297**: Hardened LogManager and DB v29 (v8.8.21).
*   **Issue #286**: Implemented `COOLING_GPS_POLLING_MS` (8.8.17).
*   **Issue #281**: Re-implemented `consumeSitDetected()` (v8.8.13).
*   **Issue #280**: Migrated to monotonic timestamps (v8.8.12).
*   **Issue #001**: Fixed missing columns and migrations (v8.9.62).

---

## 🗺️ Legacy Issue Mapping (Authoritative Unification)
The following legacy IDs have been unified into the #300+ authoritative range.

| Legacy ID | Authoritative ID | Category / Description |
| :--- | :--- | :--- |
| #115 | #322 | Architectural Bloat: ViewModel Decoupling |
| #148 | #453 | Samsung A15 GPS Stalling |
| #180 | #340 | Samsung A15 Proximity Limitation |
| #190 | #455 | Xiaomi Autostart & Boot Resilience |
| #191 | #454 | Samsung A15 Proximity Flutter |
| #214-A | #325-B | Unified Accuracy Fallback Logic |
| #214-M | #347 | Stale Legacy Reference Migration |
| #219 | #332 | SNR-IMU Correlation Validation |
| #220 | #334 | Hindsight Trajectory Correction |
| #221 | #328-B | Bayesian Uncertainty / systemPulseRealtime |
| #224 | #329 | Forensic Ribbon Expansion (tiltIdx/baroIdx) |
| #227 | #327 | Hindsight Transition Smoothing |
| #496 | #326 | Intelligent Uncertainty UX Mapping |
| #497 | #327 | Hindsight Transition Smoothing |
| #337 | #337 | Power Parity: currentMa |
