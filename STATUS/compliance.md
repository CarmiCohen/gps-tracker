# Compliance & Operational Requirements (Audit Baseline)

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions (Issue #1 to #199), see [compliance_archive.md](compliance_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R942** | **Dynamic Peer Labeling**: The primary peer activity badge is labeled VWR in Tracker mode and TRK in Viewer mode. | **Verified (SharedUiComponents)** |
| **R943** | **Redundancy Suppression**: The DAT (Data Pipeline) badge is hidden in Tracker mode to reduce UI clutter. | **Verified (SharedUiComponents)** |
| **R810-L** | **Acoustic Monitoring**: Precision thresholds for acoustic jump and floor detection. | **Verified (EngineConstants)** |
| **R810-M** | **Physical Sentinel**: Thresholds for light, tilt, baro-lift, and vibration tamper. | **Verified (EngineConstants)** |
| **R810-N** | **Clock Integrity**: Handling of future-dated packets and clock-drift resilience. | **Verified (UtilsTest)** |
| **R810-P** | **Processing Floor**: Zero-lag filtering thresholds and trajectory promotion gates. | **Verified (EngineConstants)** |
| **Issue #339** | **SIT Rising-Edge Hardening**: Prevent duplicate SIT forensic logs using physical-event latches. (Formerly #244) | **Verified (TrackerService)** |
| **Issue #348** | **Offline Context Propagation**: Propagate `locationPendingReason` through forensic ribbons for historical uncertainty parity. (Formerly #245) | **Verified (HistoryManager/Database)** |
| **Issue #325** | **Authoritative Spatial Anchoring**: Prioritize engine-calculated `maxAccuracy` over raw accuracy. (Formerly #214) | **Verified (LogManager/Dashboard/Map)** |
| **Issue #305** | **Renumbering Cleanup**: Swept and replaced low-number or double-offset references for auditability. | **Verified (Audit Suite)** |
| **Issue #315** | **Network Integrity Hardening**: Hardened signaling integrity flags and RTT scaling. (Formerly #273) | **Verified (SyncManager)** |
| **Issue #372** | **Light EMA Logic**: Implemented asymmetrical rising/falling EMA factors for light baseline tracking. (Formerly #284) | **Verified (LocationSentinel)** |
| **Issue #369** | **EMA Stability**: EMA Slow constants must be < Fast constants to ensure correct baseline stability. (Formerly #263) | **Verified (EngineConstants)** |
| **Issue #370** | **Evaluation Efficiency**: Alarm evaluation must be throttled to 1Hz to prevent CPU spikes during 10Hz bursts. (Formerly #265) | **Verified (TrackerService)** |
| **Issue #371** | **Lux Adaptation**: Lux baseline must support dual-rate (Slow/Fast) adaptation based on motion state. (Formerly #266) | **Verified (LocationSentinel)** |
| **Issue #304** | **Visual Jitter Gate**: Enforced `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m) to suppress mechanical jitter in trajectory analysis. | **Verified (LocationSentinel)** |
| **Issue #362** | **Trajectory Rejection**: Unified trajectory-based outlier rejection via `TRAJECTORY_REJECTION_ACCURACY_MULT`. (Formerly #303) | **Verified (PhysicsUtils)** |
| **R872** | **Alert Suppression**: No local alerts/sirens shall trigger on the Tracker device. New alerts must strictly respect the 2s grace period. | **Verified (BehaviorUseCase)** |
| **R915** | **UI Responsiveness**: The Map settings toggle must reliably respond to touch events over the AndroidView. | **Verified (MapComponents)** |
| **R916** | **Settings Persistence**: Users must be able to modify and persist IDs, distance, alert config, and sound selections at all times. | **Verified (SettingsRepository)** |
| **R917** | **Smooth Update**: The app must operate normally after an APK update without requiring a manual "Force Stop" or removal from recents. | **Verified (DataStore + Sticky FGS)** |
| **R921** | **Exhaustive State Reset**: Mandatory clearing of all telemetry, stats, and session state during role transitions. | **Verified (MainViewModel)** |
| **R922** | **LED Logic**: Role-aware indicators; Tracker shows local health for immediate feedback, Viewer gates by peer health for end-to-end verification. | **Verified (SharedUiComponents)** |
| **R923** | **Forensic Refresh**: Dashboard recovers immediately upon telemetry receipt using the maximum of GPS and arrival timestamps. | **Verified (DashboardUseCase)** |
| **R925** | **Landing Page Pause**: Mandatory 2,000ms pause on landing page before auto-entry to ensure stability. | **Verified (MainAppContent)** |
| **R926** | **Auto-Start Resilience**: Guaranteed background service launch during auto-transition to prevent stale/gray data. | **Verified (MainAppContent)** |
| **R933** | **Alert Grace Period**: A mandatory 2-second grace period is enforced between consecutive alert triggers to prevent event flooding. | **Verified (AppAlarmManager)** |
| **R935** | **Icon Branding**: The app icon shall use the John Deere deer logo without any accompanying text. | **Verified (ic_jd_logo.xml)** |
| **R865** | **Unified Identity Green**: Branding color Green must be explicitly integrated as the primary color theme across backgrounds/status bars. | **Verified (Layouts / Colors)** |
| **R866** | **Branding Accuracy**: JD Branding Green must match exactly #367C2B. | **Verified (Color.kt)** |
| **R867** | **Role Identity**: Default Tracker ID shall be "Ttk" and Default Viewer ID shall be "Cohen". | **Verified (SettingsRepository)** |
| **R868** | **Telemetry Layout**: The Status Card must display `maxAccuracy` for both Tracker and Viewer roles in a unified format. | **Verified (SharedUiComponents)** |
| **R941** | **Statistics Persistence**: Tracking forensic ribbons and statistics accumulation across app restarts using Room and DataStore. | **Verified (HistoryManager/SettingsRepository)** |

## 2. Resolution Archive

### 2.1. Hardening Phase Resolutions (v8.9.39)
*   **FIXED Physical Hardware Validation (Issue #341)** - Resolution: Implemented GPS Stability Audit in `TrackerService`. Monitoring 10Hz intervals and logging "STABILITY GAP" if > 200ms. Added periodic reliability percentage reporting.
*   **FIXED Xiaomi MIUI 14 Heuristic Recovery (Issue #342)** - Resolution: Hardened "Heuristic Recovery Pulse" in `TrackerService`. Detecting tick gaps > 15s to trigger forced GNSS revival and FGS type toggle.
*   **FIXED SIT Rising-Edge Hardening (Issue #339)** - Resolution: Implemented `lastSitLogTs` physical-event latch in `TrackerService.kt` to prevent duplicate forensic logs during rapid sync cycles. (Formerly #244)
*   **FIXED Offline Context Propagation (Issue #348)** - Resolution: Expanded `HistoryEntity` (Database v46) and `EngineConnectionPoint` to carry `locationPendingReason`, ensuring historical uncertainty parity. (Formerly #245)
*   **FIXED R942/R943 Status Bar Refinement** - Resolution: Dynamically renamed the peer activity badge to VWR and removed redundant DAT indicators when in Tracker mode to clarify device-local vs remote-peer health.
*   **FIXED Authoritative Spatial Anchoring (Issue #325)** - Resolution: Strictly prioritize engine-calculated `maxAccuracy` across all UI and logging layers to ensure forensic consistency. (Formerly #214)
*   **FIXED Requirements Conflict Audit (Issue #307)** - Resolution: Resolved all documentation and implementation contradictions for R922 (LEDs), R925 (Timing), and R810 (IDs).
*   **FIXED Renumbering Cleanup (Issue #305)** - Resolution: Replaced all remaining low-number or double-offset references to ensure exact audit traceability below the #350 baseline limit.
*   **FIXED Shadow Constants Remediation (Issue #321)** - Resolution: Replaced localized magic numbers with EngineConstants references inside AppSensorManager and TrackerStateManager. (Formerly #306)
*   **FIXED Architectural Bloat: ViewModel Decoupling (Issue #322)** - Resolution: Decoupled MainViewModel by moving modular tracking code into TelemetryUseCase and other feature UseCases. (Formerly #115)
*   **FIXED Samsung A15 GPS Stalling (Issue #363)** - Resolution: Enforced 1000ms polling heartbeat and implemented active WakeLock renewal on every service tick. Prevents aggressive OEM background GNSS suspension and ensures 100% background persistence on A15 hardware. (Formerly #148)
*   **FIXED Samsung A15 Proximity Flutter (Issue #364)** - Resolution: Implemented a 500ms post-sync muzzle hysteresis window to filter virtual proximity sensor noise triggered by LED/Network I/O activity. Eliminates false tamper alerts during active telemetry synchronization. (Formerly #191)
*   **FIXED Xiaomi Autostart & Boot Resilience (Issue #365)** - Resolution: Implemented robust handling for indeterminate "Unknown" status and `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms. (Formerly #190)
*   **FIXED Tier 3 Jump Floor Contradiction (Issue #304)** - Resolution: Resolved by updating `PhysicsUtils.kt` and `LocationSentinel.kt` to use `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m).
*   **FIXED Trajectory Gating Multiplier (Issue #362)** - Resolution: Resolved by unifying `TRAJECTORY_REJECTION_ACCURACY_MULT`. (Formerly #303)
*   **FIXED Behavioral Magic Numbers (Issue #346)** - Resolution: Resolved by moving thresholds to `EngineConstants.kt`. (Formerly #302)
*   **FIXED Vibration Threshold Inconsistency (Issue #318)** - Resolution: Unified with `VIBRATION_STATIONARY_THRESHOLD`.
*   **FIXED EMA Constant Inversion Audit (Issue #369)** - Resolution: Corrected weights in `EngineConstants.kt`. (Formerly #263)
*   **FIXED GtoEngine Magic Number Consolidation (Issue #345)** - Resolution: Moved hardcoded speed and vibration thresholds to `EngineConstants.kt`. (Formerly #264)
*   **FIXED TrackerService Redundant Evaluation Audit (Issue #370)** - Resolution: Optimized `TrackerService.kt`. (Formerly #265)
*   **FIXED Lux EMA Implementation Omission (Issue #371)** - Resolution: Integrated Slow/Fast variants in `LocationSentinel.kt`. (Formerly #266)
*   **FIXED Dead Code Cleanup (Issue #354)** - Resolution: Removed unused `isRevivalTriggered` flag. (Formerly #267)
*   **FIXED Acoustic Floor Logic Redundancy (Issue #352)** - Resolution: Removed redundant parameter passing. (Formerly #268)
*   **FIXED Uptime Consistency (Issue #357)** - Resolution: Consolidated redundant session timing fields into `uptimeMs`. (Formerly #271)
*   **FIXED Battery Profile (Issue #353)** - Resolution: Implemented discharge profiling in `app_settings.proto`. (Formerly #272)
*   **FIXED Network Integrity Hardening (Issue #315)** - Resolution: Hardened signaling integrity flags in `app_settings.proto` and refined RTT scaling. (Formerly #273)
*   **FIXED Documentation Hardening (Issue #312)** - Resolution: Final sweep of all `.md` files to ensure synchronization with v8.9.37 architecture and renumbered issues.
*   **FIXED Dead State Cleanup: Revival Flag (Issue #344)** - Resolution: Fixed in `TrackerService.kt`. (Formerly #289)
*   **FIXED Redundant Barometric Baselining (Issue #361)** - Resolution: Fixed by exposing `absoluteAltitude`. (Formerly #295)
*   **FIXED SIT Forensic Duplicate Risk (Issue #358)** - Resolution: Fixed in `TelemetryAggregator.kt`. (Formerly #291)
*   **FIXED Acoustic Floor Decay Logic (Issue #343)** - Resolution: Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0`. (Formerly #292)
*   **FIXED Viewer Offline Detection Logic Gap (Issue #360)** - Resolution: Fixed by calculating Viewer connectivity status. (Formerly #294)
*   **FIXED `serviceStartRealtime` Initialization Gap (Issue #335)** - Resolution: Fixed by explicitly initializing in services. (Formerly #296)
*   **FIXED Vertical Displacement Failure (Issue #349)** - Resolution: Fixed by correctly bridging `AppSensorManager` and `LocationSentinel`. (Formerly #288)
*   **FIXED Geofence Evaluation Bug (Viewer Side) (Issue #347)** - Resolution: Fixed by using `trackerDistToHome` exclusively. (Formerly #293)
*   **FIXED Role Aware Alert Title Visibility (Issue #331)** - Resolution: Refactored `getTrackerTitle()`. (Formerly #287)
*   **FIXED SoT Naming Alignment (IMM) (Issue #355)** - Resolution: Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision. (Formerly #281)
*   **FIXED GtoEngine Implementation (Issue #367)** - Resolution: Implemented `GtoEngine.kt` as per trajectory optimization spec. (Formerly #285)
*   **FIXED Hindsight Promotion Coverage (Issue #359)** - Resolution: Implemented unit test suite. (Formerly #297)
*   **FIXED SIT Duplicate Guard (Issue #336)** - Resolution: Implemented persistent 15s sanity check. (Formerly #282)
*   **FIXED Foreground Resilience Hardening (Issue #351)** - Resolution: `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened. (Formerly #279)
*   **FIXED Hardcoded EMA in AppSensorManager (Issue #368)** - Resolution: Replaced with `LUX_EMA_FAST`. (Formerly #286)
*   **FIXED Light EMA Logic Inconsistency (Issue #372)** - Resolution: Implemented rising/falling EMA factors. (Formerly #284)

### 2.2. Previous Phase Resolutions (v8.9.22 - v8.9.27)
*   **FIXED Constant Name Mismatch (Issue #228)** - Resolution: SoT updated to use `PING_INTERVAL_MS` (10,000ms) for both relay heartbeats and log synchronization.
*   **FIXED System Versioning Inconsistency (Issue #203)** - Resolution: All code headers and SoT synchronized to v8.9.27.
*   **FIXED Latitude Conversion Precision (Issue #228)** - Resolution: Both Code and SoT now use the high-precision constant: `111194.92664455874`.
*   **FIXED Version Generation Logic (Issue #199)** - Resolution: SoT updated to reflect Git-based versioning (git rev-list --count HEAD).
*   **FIXED Standardized Alert Title Inconsistency (Issue #230)** - Resolution: Code updated to use "Tracker:" prefix for consistent remote attribution.
*   **FIXED Role Prefix Enforcement (Issue #182)** - Resolution: Section 4.1 of SoT updated with role identity standards ("T"/"C" prefixes).
*   **FIXED Jump Classification Conflict (Issue #231)** - Resolution: SoT Manifest (Section 8) clarified to show Outliers are filtered under the JUMP_ALERT security tier.
*   **FIXED Documentation Internal Contradiction (Issue #313)** - Resolution: Updated `DEVICE_SPECIFIC_ADAPTATIONS.md` to correctly reflect the 200ms `GPS_STABILITY_GAP_THRESHOLD_MS`.
