# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 680**

## 93. Sensor Calibration Restoration (Aug.21.01)
*   **Issue #247: UI Component Regression**.
    - **Resolution**: Restored the missing vibration and tilt sensitivity sliders in `AlertManagementOverlay`. Implemented `Slider` components with 0-100% linear scaling tied to `AlertSettings`. Updated `strings.xml` with specialized labels to support manual hardware calibration. (R247).

## 92. Anchor & HUD Hardening (Aug.20.10)
*   **Issue #238: Anchor Evaluator Coordinate Leak**.
    - **Resolution**: Hardened stationary anchoring by restricting coordinate-averaging convergence to points within the 50% "dead zone" of the current breakout threshold. This prevents anchors from "chasing" GPS drift and ensures reliable breakouts in high-SNR scenarios. (R238).
*   **Issue #239: Undefined addPersistentLog**.
    - **Resolution**: Restored the missing `addPersistentLog` implementation in `MainViewModel.kt` and `MainFileHelper.kt` to resolve critical compilation failures across 20+ call sites. (R239).
*   **Issue #240: HUD Component API Drift**.
    - **Resolution**: Migrated `TrackerScreen.kt`, `ViewerScreen.kt`, and `GlobalStatusBar` to consume the unified `HudState` architecture, eliminating legacy parameter mismatches. (R240).
*   **Issue #241: ViewModel Combine Mismatch**.
    - **Resolution**: Synchronized `MainViewModel` combine blocks with updated `DashboardStateProvider` signatures to resolve type-inference and parameter-count errors. (R241).
*   **Issue #235: Missing Dependency Hardening**.
    - **Resolution**: Restored explicit dependency declarations for `DataStore`, `Protobuf`, and `Socket.io` in `app/build.gradle` that were causing package-level unresolved references. (R235).

## 91. Diagnostics Visibility Authority (Aug.20.09)
*   **Issue #233: Diagnostics Screen Visibility layering**.
    - **Resolution**: Modified `MainAppContent.kt` to ensure the `DiagnosticsScreen` is never obscured by initial setup overlays. Tapping "VIEW SYSTEM DIAGNOSTICS" now triggers an explicit dismissal of the `PhoneSetupOverlay`, guaranteeing that technical telemetry and diagnostic data are immediately visible to the user. (R233).

## 90. UI Hardening: Action Button Clipping (Aug.20.08)
*   **Issue #232: PhoneSetup Action Button Clipping**.
    - **Resolution**: Refined the button layout in `PhoneSetupOverlay` and `CleanSetupOverlay` to prevent text truncation on low-density hardware (SM-A155F). Converted fixed `height()` modifiers to flexible `heightIn(min = ...)` to allow for vertical expansion. Optimized action button font sizes to `13.sp` to ensure text wrapping remains legible and unclipped under various display scales. (R232).

## 89. Analytical Telemetry Optimization (Aug.20.07)
*   **Issue #225: Analytical Telemetry Optimization**.
    - **Resolution**: Consolidated the 1:1 parity transformation for forensic metadata into a unified `ForensicMapper.kt`. Refactored `MainRepository.kt`, `HistoryManager.kt`, and `ConnectivitySuite.kt` to utilize this master translator. Added `isBatteryLow` and `isBatteryCritical` to `EngineConnectionPoint` to ensure full architectural parity across engine, persistence, and UI models. (R225).

## 88. Field Monitoring & Forensic Parity (Aug.20.06)
*   **Issue #224: Field Monitoring & Maintenance**.
    - **Resolution**: Validated coordinate stabilization (R224) with 100m threshold. Implemented full forensic parity for high-frequency vertical velocity metadata across the telemetry pipeline. Synchronized `MainRepository.kt` mapping for `sitVzTs`, `sitVzRt`, and `verticalVelocity`. Bumped database to **v73** with `MIGRATION_72_73` to preserve these fields in `pending_status_updates` during network loss. Hardened `ConnectivitySuite.kt` to ensure offline forensic continuity (R224).

## 87. Production Release Packaging (Aug.20.03)
*   **Issue #223: Production Release Packaging**.
    - **Resolution**: Conducted a final production audit and stripped all debug instrumentation and test hooks from the codebase. Removed `SimulateThermalEvent`, `TriggerForensicTest`, and associated stress-test UI elements from layouts, ViewModels, and background services. Updated technical documentation to reflect the production state and verified build integrity (R223).

## 86. Permission Refresh Performance (Aug.20.01)
*   **Issue #222: Permission Refresh Performance Audit**.
    - **Resolution**: Identified and eliminated redundant `RefreshPermissionStatus` event triggers in the `MainAppContent` lifecycle observer. Optimized the staggered hydration sequence in `SettingsComponents.kt` by reducing the inter-frame delay to 50ms. These changes resolved the 800ms+ main-thread jank observed during app resumption on Samsung hardware (R222).

## 85. PhoneSetup UI Clipping (Aug.20.01)
*   **Issue #221: PhoneSetup UI Clipping**.
    - **Resolution**: Optimized the `PhoneSetupOverlay` layout for low-density/budget hardware (specifically SM-A155F). Removed redundant `statusBarsPadding()` and `navigationBarsPadding()` calls in nested components that were compressing the layout. Increased the bottom `Spacer` to 56dp to ensure that the final action buttons in the `ScrollView` are fully visible and interactable (R221).

## 84. Analytical Index Performance Verification (Aug.20.00)
*   **Issue #219: Analytical Index Performance Verification**.
    - **Resolution**: Offloaded the `GpsIndex` calculation in `GpsStatusManager.kt` to `Dispatchers.Default` and implemented a 500ms `sample` throttle. This ensures that weighted averaging of GPS Age, Accuracy, and Satellite count does not induce UI thread jitter during 100Hz forensic bursts, maintaining UI responsiveness (R219).

## 83. Shadow-Cache Hardening (Aug.20.00)
*   **Issue #217: Shadow-Cache Hardening**.
    - **Resolution**: Finalized the generic `ShadowCache<K, V>` utility in `core:engine`. Hardened thread-safety for atomic `getOrPut` operations using synchronized locks to prevent race conditions during high-frequency telemetry bursts. Integrated the cache into `GpsApplication` and `MainRepository` to ensure stable memory footprints during multi-day tracking sessions. (R217)

## 82. Systematic JNI Audit (Aug.19.13)
*   **Issue #218: Systematic JNI Audit**.
    - **Resolution**: Conducted a full audit of the native C++ layer. Verified that all internal identifiers, logic, and logs are fully decoupled from neutralized vendor keywords. Exported JNI functions now strictly utilize abstract identifiers (`n1`-`n5`). Library renamed to `jdHardware` and 16KB page-size alignment implemented for Android 15+ stability. (R218)

## 81. Shadow-Cache Eviction Strategy (Aug.19.13)
*   **Issue #217: Shadow-Cache Eviction Strategy**.
    - **Resolution**: Implemented a generic, thread-safe `ShadowCache<K, V>` utility using an LRU (Least Recently Used) eviction strategy. Integrated the cache into `GpsApplication` for system identifiers and `MainRepository` for trail point pooling. (R217)

## 80. Atomic Counter Consolidation (Aug.19.13)
*   **Issue #216: Atomic Counter Consolidation**.
    - **Resolution**: Grouped disparate `AtomicInteger` performance and pruning counters in `MainRepository.kt` into a single private `RepositoryMetrics` data structure, simplifying state management. (R216)

## 79. Signal Loss Recovery Authority (Aug.19.11)
*   **Issue #213: Signal Loss Recovery Authority**.
    - **Resolution**: Implemented stable `recoveryStartRt` anchor to calculate the 3-second stabilization period following a GPS gap. The system now utilizes a fixed reference point rather than a moving last-fix timestamp, preventing logical locks and ensuring predictable recovery transitions. (R213)

## 78. Advanced Collision Forensic (Aug.19.08)
*   **Issue #212: Advanced Collision Forensic**.
    - **Resolution**: Conducted a deep forensic investigation into the resilient Samsung CFMS `libmbrainSDK` load trigger. Verified through a diagnostic **Identity Swap** that the trigger is a resilient OS-level heuristic. Restored the project to its functional state and accepted the vendor-specific log noise as a benign side-effect. (R212-F)

## 77. Samsung Battery Authority (Aug.19.01)
*   **Issue #214: System Issue Dashboard Audit**.
    - **Resolution**: Confirmed the "1" issue count and automatic setup navigation on Samsung A15 are intentional R405 safety mechanisms for battery exemption validation. (R214)

## 76. JNI Vendor Collision Remediation (Aug.19.01)
*   **Issue #212: JNI Vendor Collision Remediation**.
    - **Resolution**: Remediated early-lifecycle JNI failures on Samsung hardware by transitioning to the neutral `JdHardware` namespace and purging colliding keywords. (R212)

## 75. Final Release Validation (Aug.18.13)
*   **Issue #211: Final Release Validation**.
    - **Resolution**: Verified that the forensic pipeline operates at 100Hz fidelity with acceptable thermal headroom and battery consumption on Samsung A15 hardware. (R211)

## 74. Long-Term Field Hardening (Aug.18.12)
*   **Issue #210: Long-Term Stress Hardening**.
    - **Resolution**: Optimized `LogRepository` deduplication using bit-packed primitive Long signatures, eliminating thousands of `Pair` object allocations. Implemented `TrailPoint` object pooling in `MainRepository`. (R210)

## 73. Fidelity Restoration & Production Scaling (Aug.18.10)
*   **Issue #209: Production Fidelity Restoration**.
    - **Resolution**: Restored forensic sampling intervals to production targets (100Hz) and updated `AppSensorManager` to restore `SENSOR_DELAY_FASTEST`. (R209)

## 72. Main-Thread Bottleneck Remediation (Aug.18.09)
*   **Issue #207: Main-Thread Audit (Frame Hangs)**.
    - **Resolution**: Eliminated 1s+ frame hangs by implementing `derivedStateOf` gating and wrapping imperative MapView updates in `Snapshot.withoutReadObservation`. (R207)

## 71. Urban Edge Case: Multipath Mitigation (Aug.18.05)
*   **Issue #201: Urban Edge Case: Multipath Mitigation Audit**.
    - **Resolution**: Modified `AnchorEvaluator.kt` to prevent binary anchor release when GPS confidence drops but IMU confirms physical stability. (R201)

## 70. Forensic Buffer & Pressure Hardening (Aug.18.00)
*   **Issue #196: Forensic Log Buffer Pressure Audit**.
    - **Resolution**: Increased `LOG_BUFFER_CAPACITY` to 5000 and lowered drain trigger to 25% fill level to prevent `FORENSIC_OVERFLOW` during 100Hz sampling. (R196)

## 69. Battery Health & Logic Hardening (Aug.17.11)
*   **Issue #194: Battery Steep Discharge Logic Hardening**.
    - **Resolution**: Introduced load-aware thresholds (4% normal / 8% high-load) to prevent false positives during 100Hz forensic sampling. (R194)

## 68. Migration Recovery & Schema Hardening (Aug.17.10)
*   **Issue #195: Database Migration Crash Loop**.
    - **Resolution**: Hardened migrations 68-72 to explicitly drop legacy indices and force-fix the `connection_history` table schema. (R195)

## 67. Forensic Persistence & Thermal Recovery (Aug.17.10)
*   **Issue #193: Forensic Signature Persistence Audit**.
    - **Resolution**: Verified zero-data-loss during thermal recovery windows using memory-mapped forensic spill files. (R193)

## 66. Automated Recovery Latency Audit (Aug.17.10)
*   **Issue #192: Recovery Latency Tracking**.
    - **Resolution**: Instrumented cooling-to-active transition to detect recovery latency. (R192)

## 65. Forensic Stress Test (Aug.17.10)
*   **Issue #189: 100Hz Forensic Pipeline Verification**.
    - **Resolution**: Successfully executed 5-minute CPU/IO saturation routine without ANRs. (R189)

---

## 🏗️ Legacy Hardening Phase (v9.0.4 - v9.3.0)
*   **Issue #049**: Corrected GlobalStatusBar mapping to use mode-aware location context (v9.2.6).
*   **Issue #044**: Standardized HUD status badges to reflect local device health (v9.2.3).
*   **Issue #030**: Consolidated all schemas into `app/src/main/proto` (v9.3.0).
*   **Issue #400**: Re-anchored Bayesian Uncertainty status messages to the bottom metadata cluster (v9.3.0).
*   **Issue #326**: Enriched Location Pending state with reasons (GPS_GAP, JAMMER) (v9.2.2).
*   **Issue #018**: Implemented coordinate clamping and `isAnchorLocked` flag (v9.2.1).
*   **Issue #048**: Differentiated Telemetry Age from GPS Age in status rows (v9.2.0).
*   **Issue #029**: Propagated local telemetry to repository in Viewer mode (v9.0.3).

## 🏗️ Middle Hardening Era (v8.9.65 - v9.1.7)
*   **Issue #042**: Implemented migration flag to notify UI of auto-sanitization events (v9.3.0).
*   **Issue #041**: Implemented R975 (Regex validation) and automatic storage purging (v8.9.99).
*   **Issue #027**: Reinforced bulk save with atomic uniqueness validation (v8.9.98).
*   **Issue #032**: Implemented `isForensicFresh` gate using `WATCH_DOG_UI_GRACE_MS` (v8.9.96).
*   **Issue #038**: Implemented 5s \"Adaptation Muzzle\" for A15 polling changes (v8.9.94).
*   **Issue #036**: Introduced hardened sensor mismatch and jitter thresholds (v8.9.94).
*   **Issue #005**: Static user agent and manual storage paths for osmdroid (v8.9.91).
*   **Issue #025**: Increased `UI_PULSE_TIMEOUT_MS` to 45s for Android 14+ (v8.9.86).
*   **Issue #023**: Reverted legacy tags to float and added high-precision doubles (v8.9.84).
*   **Issue #021**: Fixed map loop with single-point trail segments (v8.9.82).
*   **Issue #014**: System-Wide Type Safety. Standardized telemetry fields to `Double` (v9.1.7).
*   **Issue #011**: Added `suppressionNote` to `SentinelResult` (v8.9.68).
*   **Issue #012**: Adaptive Proximity Debounce in `AppSensorManager` (v8.9.71).

## 🏗️ Middle Era Resolutions (#100 - #199)
*   **Issue #199**: Toolchain Modernization. Upgraded to Java 17 and Android SDK 35 (v8.9.8).
*   **Issue #198**: Shortened stall detection to 60s (v8.9.8).
*   **Issue #197**: Added `sitVzTs` to history (v8.9.7).
*   **Issue #195**: Implemented table reconstruction migration (v8.9.6).
*   **Issue #194**: Implemented acknowledged event pipeline (v8.9.7).
*   **Issue #193**: Implemented \"Ghost Mode\" indicators (v8.9.6).
*   **Issue #337**: Achieved forensic parity for `currentMa` (v8.9.2).
*   **Issue #191**: Implemented deterministic Muzzle Handshake (v8.9.6).
*   **Issue #190**: Xiaomi Autostart status handling and boot grace (v8.9.16).
*   **Issue #189**: 10s background polling for Viewers (v8.9.5).
*   **Issue #188**: Added `gpsTs` to DB and sync (v8.9.3).
*   **Issue #185**: Remote-to-local trail persistence (v8.9.2).
*   **Issue #115**: Extracted feature-specific UseCases (8.8.25).

## 🏗️ Legacy Foundation Resolutions (#1 - #99)
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
*For older resolutions, see history. (vAug.20.10)
