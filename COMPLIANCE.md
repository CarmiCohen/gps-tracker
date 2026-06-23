# Compliance & Operational Requirements (Audit Archive)

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and the complete Resolution Archive for all past issues.

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **Issue #284** | **Light EMA Logic**: Implemented asymmetrical rising/falling EMA factors for light baseline tracking. (Formerly #14) | **Verified (LocationSentinel)** |
| **Issue #263** | **EMA Stability**: EMA Slow constants must be < Fast constants to ensure correct baseline stability. | **Verified (EngineConstants)** |
| **Issue #265** | **Evaluation Efficiency**: Alarm evaluation must be throttled to 1Hz to prevent CPU spikes during 10Hz bursts. | **Verified (TrackerService)** |
| **Issue #266** | **Lux Adaptation**: Lux baseline must support dual-rate (Slow/Fast) adaptation based on motion state. | **Verified (LocationSentinel)** |
| **R872** | **Alert Suppression**: No local alerts/sirens shall trigger on the Tracker device. New alerts must strictly respect the 2s grace period. | **Verified (BehaviorUseCase)** |
| **R915** | **UI Responsiveness**: The Map settings toggle must reliably respond to touch events over the AndroidView. | **Verified (MapComponents)** |
| **R916** | **Settings Persistence**: Users must be able to modify and persist IDs, distance, alert config, and sound selections at all times. | **Verified (SettingsRepository)** |
| **R917** | **Smooth Update**: The app must operate normally after an APK update without requiring a manual "Force Stop" or removal from recents. | **Verified (DataStore + Sticky FGS)** |
| **R921/R926** | **Session Lifecycle**: Exhaustive state reset and mandatory landing page pause to ensure lifecycle stability across sessions. | **Verified (MainViewModel)** |
| **R922** | **LED Logic**: INT LED reflects local relay state; SRV/TRK/DAT/GPS are gated by peer health (isRemote) for end-to-end verification. | **Verified (SharedUiComponents)** |
| **R923** | **Forensic Refresh**: Dashboard recovers immediately upon telemetry receipt using the maximum of GPS and arrival timestamps. | **Verified (DashboardUseCase)** |
| **R933** | **Alert Grace Period**: A mandatory 2-second grace period is enforced between consecutive alert triggers to prevent event flooding. | **Verified (AppAlarmManager)** |
| **R935** | **Icon Branding**: The app icon shall use the John Deere deer logo without any accompanying text. | **Verified (ic_jd_logo.xml)** |
| **R866** | **Branding Accuracy**: JD Branding Green must match exactly #367C2B. | **Verified (Color.kt)** |
| **R867** | **Role Identity**: Default Tracker ID shall be "Ttk" and Default Viewer ID shall be "Cohen". | **Verified (SettingsRepository)** |
| **R868** | **Telemetry Layout**: The Status Card must display `maxAccuracy` for both Tracker and Viewer roles in a unified format. | **Verified (SharedUiComponents)** |
| **Issue #276** | **Xiaomi Gating**: Implemented `ALERT_ID_XIAOMI_SYSTEM_MISSING` to detect and alert on MIUI background restrictions. (Formerly #6) | **Verified (MainAlarmLogic)** |
| **Issue #283** | **Timing Integrity**: Migrated to `SystemClock.elapsedRealtime()` for all debouncing and persistence timing to prevent wall-clock leaks. (Formerly #13) | **Verified (MainRepository)** |
| **Issue #285** | **Forensic I/O**: Implemented safety-flush in service `onDestroy` and monotonic interval checks for history persistence. (Formerly #15) | **Verified (MainRepository)** |
| **Issue 45** | **FGS Compliance**: Correctly passing `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ and asserting types in callbacks. | **Verified (ViewerService)** |
| **Issue 58** | **Module Hardening**: Converted `:core:engine` to a pure `java-library` to enforce zero Android framework dependencies. | **Verified (build.gradle)** |
| **Issue 70** | **Thermal Throttling**: Implemented "Cooling Mode" (46°C/44°C) that throttles GPS polling to protect hardware. | **Verified (IntegrityMonitor)** |
| **Issue 71/72** | **System Integrity**: Monitoring of low/critical storage and OS-level restrictions (Standby Buckets, Power Save) for forensic visibility. | **Verified (IntegrityMonitor)** |
| **Issue 102** | **TimeProvider**: Standardized all duration and timeout logic across the system to use the `TimeProvider` abstraction. | **Verified (Standardized Architecture)** |
| **Issue 115** | **Modularization**: Decoupled `MainViewModel` into feature UseCases (Navigation, Settings, Telemetry, Behavior, Session, Alert, Map). | **Verified (MainViewModel)** |
| **Issue 124** | **GPS Revival**: System retries hardware revival every 2m during stall and escalates to critical after 3 failures. | **Verified (TrackerService)** |
| **Issue 125** | **Monotonic UI**: UI lockout and pulse logic must use monotonic time to survive system clock jumps. | **Verified (MainViewModel)** |
| **Issue 146** | **Startup Performance**: Optimized launch by moving `OsmConfig` to background thread and staggering ViewModel initialization. | **Verified (GpsApplication)** |
| **Issue 148** | **A15 Stability**: Enforced 1000ms GPS polling (`A15_STABLE_GPS_POLLING_MS`) and 5s proximity debounce for Samsung A15 devices. | **Verified (ServiceBehaviorUseCase)** |
| **Issue 149** | **Forensic Parity**: Symbol parity for jump markers (Magenta Squares). Viewers explicitly latch peer visual jumps to local forensics. | **Verified (ViewerService/Map)** |
| **Issue 163** | **Power Tamper Hardening**: Reconnected power tamper detection via `IntegrityMonitor` using `EXTRA_PLUGGED` and auto-recovery. | **Verified (IntegrityMonitor)** |
| **Issue 168** | **Stability Audit Suite**: Implemented GPS Stability Audit suite in TrackerService.kt to track fix reliability and inter-fix gaps. | **Verified (TrackerService)** |
| **Issue 169** | **Version Synchronization**: Synchronized source headers in Tracker/Viewer services with the v8.9.11 baseline for audit integrity. | **Verified (TrackerService/ViewerService)** |
| **Issue 170/172** | **Xiaomi Alert Guard**: Added explicit `isXiaomiDevice` check to gate `ALERT_ID_XIAOMI_SYSTEM_MISSING` violations. | **Verified (MainAlarmLogic)** |
| **Issue 171** | **GPS Transition Muzzle**: Implemented `GPS_TRANSITION_LOG_MUZZLE_MS` (30s) to prevent forensic log flooding. | **Verified (TrackerService)** |
| **Issue 173** | **SIT Marker Persistence**: Reconnected `ALERT_ID_TRACKER_CHAIR` to `forensicUseCase` for persistent map visualization on the Tracker. | **Verified (TrackerService)** |
| **Issue 174** | **Default Identity**: Updated `DEFAULT_TRACKER_ID` to "Ttk" and `DEFAULT_VIEWER_ID` to "Cohen" for verified role identity. | **Verified (SettingsRepository)** |
| **Issue 175** | **Version Update Smoothness**: Verified `MY_PACKAGE_REPLACED` handling in `BootReceiver` for background service continuity. | **Verified (BootReceiver)** |
| **Issue 176** | **Statistics Persistence**: Verified forensic ribbon and statistics accumulation across app restarts using Room and DataStore. | **Verified (HistoryManager/SettingsRepository)** |
| **Issue 177** | **Dead Code Cleanup**: Formally removed redundant telemetry methods in `SyncManager` following consolidation around `pushCurrentStatus`. | **Verified (SyncManager)** |
| **Issue 178/179**| **Forensic Parity Audit**: Verified 100% field parity for `verticalVelocity` and SIT metrics across the pipeline and `RemoteHandler`. | **Verified (RemoteHandler/HistoryManager)** |
| **Issue 189** | **Viewer Background Location**: Implemented 10s background polling and relative distance calculation in `ViewerService`. | **Verified (ViewerService)** |
| **Issue 190** | **Xiaomi Autostart & Boot Resilience**: Implemented robust handling for indeterminate "Unknown" status and `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms. | **Verified (MainAlarmLogic)** |
| **Issue 191** | **Muzzle Window Hardening**: Increased `MUZZLE_WINDOW_DURATION_MS` to 2000ms and added device-specific hysteresis (500ms for A15). | **Verified (SyncManager/TrackerService)** |
| **Issue 192** | **Power Forensic Parity**: Achieved absolute parity for `currentMa` across models, database (v35), and ribbons. | **Verified (SyncManager/HistoryManager)** |
| **Issue 193** | **Zombie Telemetry UX Sweep**: Implemented visual staleness indicators ("Ghost Mode") for all sensor derived dashboard fields and markers when telemetry > 10s old. | **Verified (DashboardUseCase/MapComponents)** |
| **Issue 194** | **SIT Persistence Packet Loss Risk**: Implemented a 10s acknowledged sync loop for discrete SIT events to prevent forensic loss during blackouts. | **Verified (SyncManager/ViewerService)** |
| **Issue 195** | **Room Migration Forensic Audit**: Implemented Room migration (v36) for full table reconstruction and Android 15 compatibility. | **Verified (Database)** |
| **Issue 196** | **Advanced SIT Detection**: Refined "Plunge" state machine and propagated `sitVzTs` for forensic parity. | **Verified (AppSensorManager)** |
| **Issue 197** | **Database Schema Expansion (v38)**: Added `sitVzTs` to history tables for improved chair event reconstruction. | **Verified (Database)** |
| **Issue 198** | **GPS Availability Hardening**: Shortened GPS stall detection to 60s and revival retry to 120s for high-availability tracking. | **Verified (TrackerService)** |
| **Issue 199** | **Toolchain Modernization**: Upgraded to Java 17 and Android SDK 35. Aligned Gradle DSL syntax. | **Verified (build.gradle)** |
| **Issue 200** | **Room Migration Registry**: Registered `MIGRATION_37_38` in `AppModule.kt` to prevent startup crashes. | **Verified (AppModule)** |

---

## 2. Resolution Archive

### 2.1. Hardening Phase Resolutions (v8.9.34)
*   **FIXED EMA Constant Inversion Audit (Issue #263)** - Resolution: Corrected weights in `EngineConstants.kt`. Slow factors (0.001) are now correctly smaller than Fast factors (0.1).
*   **FIXED GtoEngine Magic Number Consolidation (Issue #264)** - Resolution: Moved hardcoded speed and vibration thresholds to `EngineConstants.kt`.
*   **FIXED TrackerService Redundant Evaluation Audit (Issue #265)** - Resolution: Optimized `TrackerService.kt` to prevent redundant alarm triggers during 10Hz GPS bursts; processTick (1Hz) is now the authority.
*   **FIXED Lux EMA Implementation Omission (Issue #266)** - Resolution: Integrated `LUX_EMA_SLOW` and `LUX_EMA_UP_SLOW` in `LocationSentinel.kt` for improved stationary baseline stability.
*   **FIXED Dead Code Cleanup (Issue #267)** - Resolution: Removed unused `isRevivalTriggered` flag from `TrackerService.kt`.
*   **FIXED Acoustic Floor Logic Redundancy (Issue #268)** - Resolution: Eliminated redundant `acousticFloorDb` parameter passing in `LocationProcessor.kt` and `TrackerService.kt`.
*   **FIXED Uptime Consistency (Issue #271)** - Resolution: Consolidated redundant session timing fields into `uptimeMs` in `AppSettingsMigration.kt`. (Formerly #1)
*   **FIXED Battery Profile (Issue #272)** - Resolution: Implemented discharge profiling in `app_settings.proto`. (Formerly #2)
*   **FIXED Network Integrity (Issue #273)** - Resolution: Hardened signaling integrity flags in `app_settings.proto`. (Formerly #3)
*   **FIXED Documentation Gating (Issue #276)** - Resolution: Implemented `ALERT_ID_XIAOMI_SYSTEM_MISSING` and updated audit docs. (Formerly #6)
*   **FIXED Dead State Cleanup: Revival Flag (Issue #289)** - Resolution: Fixed in `TrackerService.kt` by ensuring `isRevivalTriggered` is set to `true` during GPS revival sequences. (Formerly #19)
*   **FIXED Redundant Barometric Baselining (Issue #295)** - Resolution: Fixed by exposing `absoluteAltitude` (raw) in `AppSensorManager.kt`. (Formerly #25)
*   **FIXED SIT Forensic Duplicate Risk (Issue #291)** - Resolution: Fixed in `TelemetryAggregator.kt` by ensuring `isSitDetected` defaults to `false` during backfill. (Formerly #21)
*   **FIXED Acoustic Floor Decay Logic (Issue #292)** - Resolution: Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0` in `LocationSentinel.kt`. (Formerly #22)
*   **FIXED Viewer Offline Detection Logic Gap (Issue #294)** - Resolution: Fixed by calculating Viewer connectivity status in `TrackerService.kt`. (Formerly #24)
*   **FIXED `serviceStartRealtime` Initialization Gap (Issue #296)** - Resolution: Fixed by explicitly initializing `serviceStartRealtime` in services. (Formerly #26)
*   **FIXED Vertical Displacement Failure (Issue #288)** - Resolution: Fixed by correctly bridging `AppSensorManager` and `LocationSentinel`. (Formerly #18)
*   **FIXED Geofence Evaluation Bug (Viewer Side) (Issue #293)** - Resolution: Fixed by using `trackerDistToHome` exclusively in `ViewerService.kt`. (Formerly #23)
*   **FIXED Role-Aware Alert Title Visibility (Issue #287)** - Resolution: Refactored `getTrackerTitle()` in `MainAlarmLogic.kt`. (Formerly #17)
*   **FIXED SoT Naming Alignment (IMM) (Issue #281)** - Resolution: Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision. (Formerly #11)
*   **FIXED GtoEngine Implementation (Issue #285)** - Resolution: Implemented `GtoEngine.kt` as per trajectory optimization spec. (Formerly #15)
*   **FIXED Hindsight Promotion Coverage (Issue #297)** - Resolution: Implemented unit test suite `LocationSentinelHindsightTest.kt`. (Formerly #27)
*   **FIXED SIT Duplicate Guard (Issue #282)** - Resolution: Implemented persistent 15s sanity check in `HistoryManager.kt`. (Formerly #12)
*   **FIXED Foreground Resilience Hardening (Issue #279)** - Resolution: `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened. (Formerly #9)
*   **FIXED Hardcoded EMA in AppSensorManager (Issue #286)** - Resolution: Replaced with `LUX_EMA_FAST`. (Formerly #16)
*   **FIXED Light EMA Logic Inconsistency (Issue #284)** - Resolution: Implemented rising/falling EMA factors in `LocationSentinel.kt`. (Formerly #14)

### 2.2. Previous Phase Resolutions (v8.9.22 - v8.9.27)
*   **FIXED Constant Name Mismatch (Issue #228)** - Resolution: SoT updated to use `PING_INTERVAL_MS` (10,000ms) for both relay heartbeats and log synchronization.
*   **FIXED System Versioning Inconsistency (Issue #203)** - Resolution: All code headers and SoT synchronized to v8.9.27.
*   **FIXED Latitude Conversion Precision (Issue #228)** - Resolution: Both Code and SoT now use the high-precision constant: `111194.92664455874`.
*   **FIXED Version Generation Logic (Issue #199)** - Resolution: SoT updated to reflect Git-based versioning (git rev-list --count HEAD).
*   **FIXED Standardized Alert Title Inconsistency (Issue #230)** - Resolution: Code updated to use "Tracker:" prefix for consistent remote attribution.
*   **FIXED Role Prefix Enforcement (Issue #182)** - Resolution: Section 4.1 of SoT updated with role identity standards ("T"/"C" prefixes).
*   **FIXED Jump Classification Conflict (Issue #231)** - Resolution: SoT Manifest (Section 8) clarified to show Outliers are filtered under the JUMP_ALERT security tier.
*   **FIXED Documentation Internal Contradiction (Issue #276)** - Resolution: Updated `DEVICE_SPECIFIC_ADAPTATIONS.md` to correctly reflect the 200ms `GPS_STABILITY_GAP_THRESHOLD_MS`. (Formerly #6)

### 2.3. Middle Era Resolutions (#100 - #199)
*   **FIXED Toolchain Modernization (#199)** - Resolution: Upgraded to Java 17 and Android SDK 35. (v8.9.8)
*   **FIXED GPS Availability Hardening (#198)** - Resolution: Shortened GPS stall detection to 60s. (v8.9.8)
*   **FIXED Database Schema Expansion (v38) (#197)** - Resolution: Added sitVzTs to history tables. (v8.9.7)
*   **FIXED Plunge Matching: Advanced SIT Detection (#196)** - Resolution: Refined "Plunge" state machine and sitVzTs propagation. (v8.9.7)
*   **FIXED Room Migration Forensic Audit (Android 15) (#195)** - Resolution: Implemented full table reconstruction migration. (v8.9.6)
*   **FIXED SIT Persistence Packet Loss Risk (#194)** - Resolution: Implemented acknowledged event synchronization pipeline. (v8.9.7)
*   **FIXED Zombie Telemetry UX (#193)** - Resolution: Implemented visual staleness indicators ("Ghost Mode"). (v8.9.6)
*   **FIXED Power Parity Consistency & evaluateAlarms Mismatch (#192)** - Resolution: Achieved absolute forensic parity for currentMa. (v8.9.5)
*   **FIXED Muzzle Window Race Condition (#191)** - Resolution: Implemented deterministic Muzzle Handshake. (v8.9.6)
*   **FIXED Xiaomi Autostart Unknown Handling (Logic) (#190)** - Resolution: Implemented robust handling for indeterminate status. Expanded technicalDetails with uptime and grace threshold (v8.9.16). (Note: Hardware verification pending).
*   **FIXED Viewer Background Location Gap (#189)** - Resolution: Implemented 10s background polling for Viewers. (v8.9.5)
*   **FIXED Historical GPS Timestamp Loss (#188)** - Resolution: Added gpsTs to database and sync. (v8.9.3)
*   **FIXED Viewer-Side LocationProcessor State Persistence (#187)** - Resolution: Updated ViewerService to load maxAccuracy. (v8.9.4)
*   **FIXED SoT Documentation Hardening (#186)** - Resolution: Updated documentation to v8.9.2 baseline. (v8.9.2)
*   **FIXED ViewerService Listener Completion (#185)** - Resolution: Fully implemented remote-to-local trail persistence. (v8.9.2)
*   **FIXED Muzzle Window Hardening (#184)** - Resolution: Optimized SyncManager and Tracker muzzle state. (v8.9.2)
*   **FIXED Legacy Branding Cleanup (#183)** - Resolution: Standardized to John Deere Green and logo. (v8.9.2)
*   **FIXED Global Version Synchronization (#182)** - Resolution: Synchronized all source headers to v8.9.2. (v8.9.2)
*   **FIXED GPS Stability Audit Verification (#181)** - Resolution: Reliability metrics emitted every 10s. (v8.9.2)
*   **FIXED Forensic Pipeline Verification (#180)** - Resolution: Verified 1:1 field mapping for verticalVelocity and SIT. (v8.9.2)
*   **FIXED RemoteHandler SIT Mapping Audit (#179)** - Resolution: Verified 100% field parity for SIT. (v8.8.37)
*   **FIXED Forensic Parity: verticalVelocity Alignment (#178)** - Resolution: Implemented full parity for verticalVelocity. (v8.8.37)
*   **FIXED Dead Code Cleanup (#177)** - Resolution: Removed redundant telemetry methods. (v8.8.37)
*   **FIXED R941: Statistics Persistence Verification (#176)** - Resolution: Confirmed statistics accumulation across restarts. (v8.8.36)
*   **FIXED R917: Version Update Smoothness (#175)** - Resolution: Verified `MY_PACKAGE_REPLACED` handling. (v8.8.36)
*   **FIXED R867: Default Identity Verification (#174)** - Resolution: Updated default IDs to "Ttk" and "Cohen". (v8.8.36)
*   **FIXED Tracker-Side SIT Marker Persistence (#173)** - Resolution: Reconnected SIT events to local forensics. (v8.8.36)
*   **FIXED Xiaomi False Positives on Non-Xiaomi (#172)** - Resolution: Added `isXiaomiDevice` flag to Evaluation state. (v8.8.36)
*   **FIXED GPS Transition Log Muzzle (#171)** - Resolution: Implemented 30s temporal muzzle for logs. (v8.8.36)
*   **FIXED Xiaomi Alert Guard (#170)** - Resolution: Added `isXiaomiDevice()` check. (v8.8.36)
*   **FIXED Version Header Desync (#169)** - Resolution: Synchronized source headers in Services. (v8.8.36)
*   **FIXED Xiaomi 10Hz Stability Preparation (#168)** - Resolution: Implemented Stability Audit suite. (v8.8.36)
*   **FIXED Documentation Debt (SoT) (#167)** - Resolution: Updated SoT to include Samsung A15 polling. (v8.8.36)
*   **FIXED Build Integrity & Lint (#166)** - Resolution: Verified build stability following modularization. (v8.8.36)
*   **FIXED Code Redundancy in Utils.kt (#165)** - Resolution: Migrated logic to `:core:engine`. (v8.8.36)
*   **FIXED Telemetry Validation Parity (#164)** - Resolution: Standardized on `PhysicsUtils.isValidLocation`. (v8.8.35)
*   **FIXED Power Tamper Regression (#163)** - Resolution: Reconnected battery/power callbacks to `IntegrityMonitor`. (v8.8.35)
*   **FIXED Constant Redundancy (#162)** - Resolution: Removed duplicated constants. (v8.8.35)
*   **FIXED Viewer Alarm Title Confusion (#161)** - Resolution: Updated titles to "This device:". (v8.8.35)
*   **FIXED Xiaomi Gating Logic Error (#160)** - Resolution: Decoupled autostart from xiaomiStatus. (v8.8.35)
*   **FIXED Database Schema Cleanup (Future) (#159)** - Resolution: Removed 'ver' and 'vid' columns via Room Migration v33. (v8.8.35)
*   **FIXED Database Schema "Dead Weight" (#158)** - Resolution: Migrated to a ver-less structure in v33. (v8.8.35)
*   **FIXED Forensic Documentation Mismatch (#157)** - Resolution: Updated core documentation to reflect simplified forensic model.
*   **FIXED Global Version Desync (#156)** - Resolution: Synchronized all version strings to v8.8.35.
*   **FIXED Build Failure: Unfinished Forensic Simplification (#155)** - Resolution: Resolved compilation errors. (v8.8.34)
*   **FIXED Forensic Documentation Debt (#154)** - Resolution: Replaced legacy `vid` with `ver`. (v8.8.33)
*   **FIXED Compilation Errors (Forensic Expansion) (#153)** - Resolution: Resolved build failures by adding `ver` field. (v8.8.33)
*   **FIXED Missing Database Migrations (v31) (#152)** - Resolution: Implemented `MIGRATION_30_31`. (v8.8.33)
*   **FIXED Model Synchronization (#151)** - Resolution: Aligned `:app:Models.kt` and `:core:engine:EngineModels.kt`. (v8.8.33)
*   **FIXED Architectural Alignment (Standardized Alert IDs) (#150)** - Resolution: Centralized `ALERT_ID_VISUAL_JUMP`. (v8.8.32)
*   **FIXED Missing Jump Markers (Forensic Parity) (#149)** - Resolution: Achieved symbol parity for forensic markers. (v8.8.32)
*   **FIXED GPS Polling Stabilization (A15) (#148)** - Resolution: Implemented `A15_STABLE_GPS_POLLING_MS`. (v8.8.35)
*   **FIXED Compose SnapshotStateList Warnings (#147)** - Resolution: Migrated to SnapshotStateList in MapComponents.kt. (v8.8.35)
*   **FIXED Startup Performance (Skipped Frames) (#146)** - Resolution: Moved `OsmConfig` to background thread. (v8.8.35)
*   **FIXED Hardcoded Point Count (#145)** - Resolution: Replaced hardcoded `240f` with `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33)
*   **FIXED MainViewModel Logic Duplication (#144)** - Resolution: Centralized location validation. (v8.8.33)
*   **FIXED SyncManager Historical Version Bug (#143)** - Resolution: Updated `SyncManager.kt` to use `entity.ver`. (v8.8.33)
*   **FIXED SIT Forensic Depth Gap in History (#142)** - Resolution: Updated `HistoryEntity` to include SIT metrics. (v8.8.33)
*   **FIXED Engineering Constants Typo (#141)** - Resolution: Corrected `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33)
*   **FIXED HistoryManager Version Tagging Bug (#140)** - Resolution: Supported version-aware tagging in history updates. (v8.8.33)
*   **FIXED Database Forensic Depth Gap (#139)** - Resolution: Expanded `PendingStatusEntity` to include full forensic fields. (v8.8.33)
*   **FIXED SyncManager Historical Depth Gap (#138)** - Resolution: Included forensic fields in the JSON payload. (v8.8.33)
*   **FIXED SettingsRepository Alignment (#137)** - Resolution: Updated `SettingsRepository.kt` to persist cooling/storage flags. (v8.8.33)
*   **FIXED AppSettings Persistence Gap (#136)** - Resolution: Updated `TrackerStatusProto` to include missing forensic fields. (v8.8.33)
*   **FIXED Relay Audit Verification (#135)** - Resolution: Enhanced `join` payload with role and version. (v8.8.35)
*   **FIXED Xiaomi Background Stability Verification (#133)** - Resolution: Confirmed 10Hz polling logic. (v8.9.2)
*   **FIXED Continuity Audit & Backfill Verification (#132)** - Resolution: Implemented 1Hz continuity auditing in HistoryManager.kt. (v8.8.31)
*   **FIXED Forensic Key Standardization (snake_case) (#131)** - Resolution: Standardized all JSON keys to snake_case. (v8.8.31)
*   **FIXED Forensic Verification Suite (#130)** - Resolution: Implemented `ForensicIdentityTest.kt`. (v8.8.31)
*   **FIXED Build Failure: Missing Symbol (#129)** - Resolution: Re-added `isValidLocation` to `PhysicsUtils.kt`. (v8.8.31)
*   **FIXED Forensic Ribbon Scaling (#128)** - Resolution: Standardized `RIBBON_VIBRATION_SCALE_G` and `RIBBON_SNR_SCALE_DB`. (v8.8.31)
*   **FIXED Xiaomi Autostart Indeterminate State (#127)** - Resolution: Added `XiaomiPermissionStatus.UNKNOWN`. (v8.8.31)
*   **FIXED Tracker-Side SIT Logging (#126)** - Resolution: Ensured `ALERT_ID_TRACKER_CHAIR` is recorded to the local forensic database. (v8.8.31)
*   **FIXED Monotonic UI Lockout (#125)** - Resolution: Migrated all UI countdowns and pulse logic to `TimeProvider.elapsedRealtime()`. (v8.8.31)
*   **FIXED GPS Revival Escalation (#124)** - Resolution: Implemented a 5-minute retry loop and forensic escalation. (v8.8.31)
*   **FIXED Identity Hardening (#123)** - Resolution: Updated identity branding and versioning baseline. (v8.8.30)
*   **FIXED App Icon Foreground Branding (#122)** - Resolution: Updated `ic_jd_logo.xml` to use the deer-only branding. (v8.8.30)
*   **FIXED LED Logic DecouPLING (#121)** - Resolution: Fixed status LEDs on the Tracker side. (v8.8.31)
*   **FIXED Muzzle Window & Forensic Audit (#120)** - Resolution: Implemented a 500ms "Muzzle Window" during sync I/O. (v8.8.22)
*   **FIXED OEM Restriction Verification (#119)** - Resolution: Integrated Xiaomi Autostart detection and enabled 10Hz polling. (v8.8.22)
*   **FIXED Timing & Forensic Stability (#118)** - Resolution: Standardized on monotonic time (`TimeProvider.elapsedRealtime()`). (v8.8.22)
*   **FIXED Barometer Zeroing Drift (#117)** - Resolution: Increased `BARO_ZEROING_INTERVAL_MS` to 10 minutes. (v8.8.25)
*   **FIXED GpsManager Initialization Race (#116)** - Resolution: Moved `OsmConfig` to background thread. (v8.8.25)
*   **FIXED ViewModel Bloat (#115)** - Resolution: Decoupled `MainViewModel` into feature-specific UseCases. (v8.8.25)
*   **FIXED Forensic Identity Propagation (#114)** - Resolution: Ensured `vid` is correctly propagated to the relay. (v8.8.25)
*   **FIXED Ribbon UI Clipping (#113)** - Resolution: Adjusted padding and stroke widths in `SharedUiComponents.kt`. (v8.8.24)
*   **FIXED Historical Gap Injection (#112)** - Resolution: Corrected `HistoryManager.kt` to prevent duplicate gap injection. (v8.8.24)
*   **FIXED SNR Scaling Standardization (#111)** - Resolution: Replaced hardcoded SNR scaling with `RIBBON_SNR_SCALE_DB` in `TrackerService.kt`. (v8.8.24)
*   **FIXED Modular Engine Hardening (#110)** - Resolution: Finalized the physical isolation of the `:core:engine` as a pure JVM library. (v8.8.22)
*   **FIXED versionCode Description Mismatch (#109)** - Resolution: Updated `REQUIREMENTS_SOT.md` to accurately reflect the `yearOffset` implementation used in `build.gradle` (v8.8.23)
*   **FIXED Version Stale References (#108)** - Resolution: Synchronized all source headers and documentation files to the new v8.8.23 baseline (v8.8.23)
*   **FIXED Forensic Identity Inconsistency (#107)** - Resolution: Unified forensic identity across all components and documentation (v8.8.23)
*   **FIXED Relay URL Discrepancy (Docs) (#106)** - Resolution: Updated `SETTINGS_PAGE_DETAIL.md` to reflect the correct relay URL `https://gps-survival-relay.onrender.com` (v8.8.22)
*   **FIXED Version Desync (build.gradle) (#105)** - Resolution: Updated `app/build.gradle` `versionName` to `8.8.22` to match architectural version (v8.8.22)
*   **FIXED Naming Mismatch (Log Muzzle) (#104)** - Resolution: Renamed `LOG_MUZZLE_DURATION_MS` to `LOG_MUZZLE_STARTUP_MS` in `Constants.kt` and `LogManager.kt` to align with SoT (v8.8.22)
*   **FIXED Hardcoded Muzzle Window (#103)** - Resolution: Centralized `MUZZLE_WINDOW_DURATION_MS` in `EngineConstants.kt` and updated `TrackerService.kt` to use it (v8.8.22)
*   **FIXED Timing Consistency Check (#102)** - Resolution: TimeProvider is now the exclusive source of truth for all duration and timeout logic across :app and :core:engine. Standardized timing in Services. (v8.8.21)
*   **FIXED Verify Forensic Identity Propagation (#101)** - Resolution: New version ID correctly picked up by LogManager and SyncManager for forensic tagging. Implemented DB Migration v30. (v8.8.21)
*   **FIXED Audit :core:engine Purity (#100)** - Resolution: Verified that no android.* dependencies remain in the core engine source code. (v8.8.21)

### 2.4. Legacy Foundation Resolutions (#1 - #99)
*   **FIXED Physical Tamper Race Condition & Trajectory Bug (#99)** - Resolution: Implemented 500ms Muzzle Window and corrected LocationSentinel.storeRejected bug. (v8.8.21)
*   **FIXED Xiaomi Instruction Traceability (#98)** - Resolution: Updated MainAlarmLogic.kt for UNKNOWN MIUI status guidance. (v8.8.21)
*   **FIXED Geofence SOT Desync (#97)** - Resolution: Corrected JSON key mismatch for home_points/homePoints. (v8.8.21)
*   **FIXED Stale Forensic Versioning (Issue 59-C) (#96)** - Resolution: Dynamic version tagging implemented in AppAlarmManager.kt. (v8.8.21)
*   **FIXED Identity Collision & Ghost UUIDs (#95)** - Resolution: Consolidated 93-B/95-C. Hardened LogManager and implemented DB Migration v29. (v8.8.21)
*   **FIXED Room-Join Identity Parity (#93)** - Resolution: Corrected room assignment bug in ViewerService.kt. (v8.8.21)
*   **FIXED Event Log Grouping Logic (#92)** - Resolution: Refined stripLogVariableParts regex. (v8.8.21)
*   **FIXED Event Log Timestamp Visibility (#91)** - Resolution: Increased timestamp column width to 95dp. (v8.8.12)
*   **FIXED Relay Diagnostic Logging (#90)** - Resolution: Enhanced server-side console logging.
*   **FIXED Relay Server Visibility & Health Check (#89)** - Resolution: Implemented HTTP listener in relay-server/index.js. (v6.041)
*   **FIXED Render Relay URL Discrepancy (#88)** - Resolution: Corrected default relay URL. (v8.8.12)
*   **FIXED Missing Database Migrations (#87)** - Resolution: Added missing migration paths (24 -> 28). (v8.8.20)
*   **FIXED Inconsistent Tamper Logic (#86)** - Resolution: Updated onLocationChanged fast-path in TrackerService.kt. (v8.8.19)
*   **FIXED Forensic Marker Gap (GPS Gap) (#85)** - Resolution: Implemented state-latch and persistence logic. (v8.8.15)
*   **FIXED Missing Thermal Throttling in GPS Polling (#84)** - Resolution: Implemented COOLING_GPS_POLLING_MS. (v8.8.17)
*   **FIXED Redundant Tamper/SIT Alerting (#83)** - Resolution: Decoupled SIT alerting from general tamper flag. (v8.8.18)
*   **FIXED Forensic Persistence Gap (Viewer) (#82)** - Resolution: Implemented state-latch for GEOFENCE violations. (v8.8.16)
*   **FIXED Forensic Persistence Gap (Tracker) (#81)** - Resolution: Implemented state-latch for Stall, Tamper, and Geofence. (v8.8.15)
*   **FIXED FORENSIC PERSISTENCE GAP (Viewer) (#80)** - Resolution: Implemented state-latches for Signal Loss, Jammer, Stall, and Gap. (8.8.14)
*   **FIXED STICKY SIT STATE (#79)** - Resolution: Re-implemented consumeSitDetected() in LocationSentinel.kt. (v8.8.13)
*   **FIXED TIMING MISMATCH (GPS Stall) (#78)** - Resolution: Migrated to monotonic timestamps in LocationProcessor.kt. (v8.8.12)
*   **FIXED TIMING MISMATCH (SIT Cooldown) (#77)** - Resolution: Implemented lastSitRealtime in LocationSentinel.kt. (v8.8.12)
*   **FIXED TIMING MISMATCH (GPS Gap) (#76)** - Resolution: Migrated lastValidFixTs to monotonic time. (v8.8.12)
*   **FIXED Dynamic Parameter Synchronization (#75)** - Resolution: Reactive observation of DataStore flows.
*   **FIXED Physical Tamper Fast-Paths (#74)** - Resolution: Low-latency sensor callbacks in TrackerService.
*   **FIXED Forensic Marker Debouncing (#73)** - Resolution: Implemented state-latches in Services.
*   **FIXED OS-Level Restriction Monitoring (#72)** - Resolution: Integrated Standby Bucket and Power Save detection.
*   **FIXED Storage Integrity Monitoring (#71)** - Resolution: Implemented LOW and CRITICAL storage thresholds.
*   **FIXED Thermal Throttling Logic (#70)** - Resolution: Implemented COOLING mode (46°C trigger / 44°C recovery).
*   **FIXED HistoryManager Efficiency (#69)** - Resolution: Optimized calendar and date format reuse. (v8.8.11)
*   **FIXED S21 FE Polling Churn (#67)** - Resolution: Cached device check and updated GpsManager reactively. (v8.8.11)
*   **FIXED Dashboard Speed Recovery Parity (#66)** - Resolution: Resolved double-division bug and standardized units. (v8.8.11)
*   **FIXED Acoustic "Location Pending" Traceability (#64)** - Resolution: Included LOCATION_PENDING status in technicalDetails. (v8.8.11)
*   **FIXED Remote SIT Calibration Persistence (#63)** - Resolution: Implemented resetChairBaseline() in LocationProcessor.kt. (v8.8.11)
*   **FIXED Documentation & Version Synchronization (#59)** - Resolution: Consolidated 59, 60, 61, 62. Synchronized README and DOCS.
*   **FIXED Module Hardening (:core:engine) (#58)** - Resolution: Converted to pure java-library. (v8.8.2)
*   **FIXED Dependency Injection (#56)** - Resolution: Moved CommunicationManager to AppModule.kt. (v8.8.5)
*   **FIXED Physics Threshold Centralization (#55)** - Resolution: Unified constants in EngineConstants.kt. (v8.8.2)
*   **FIXED TrackerService Parameter Naming (#53)** - Resolution: Corrected acousticDbMin to acousticMinDb. (v8.8.11)
*   **FIXED Boundary Cleanup & Persistence Integrity (#51)** - Resolution: Consolidated 51, 54, 57. Isolated networking. (v8.8.11)
*   **FIXED Chair Alarm Resolution Inconsistency (#50)** - Resolution: Debounced isSitActive status propagation. (v8.8.8)
*   **FIXED Alarm Discovery Phase Bypass (#49)** - Resolution: Corrected Discovery grace period calculation. (v8.8.9)
*   **FIXED Acoustic Location Gating Gap (#48)** - Resolution: Implemented pendingAcousticViolation logic. (v8.8.8)
*   **FIXED Xiaomi Permission Consistency & Override (#47)** - Resolution: Implemented manual override in DataStore. (v8.8.9)
*   **FIXED Forensic Telemetry Type Safety (#46)** - Resolution: Enforced strict Double vs Float types.
*   **FIXED Foreground Service Type Compliance (#45)** - Resolution: Updated to FOREGROUND_SERVICE_TYPE_LOCATION for SDK 35. (v8.8.7)
*   **FIXED Deep-Link Cold-Start Handling (#44)** - Resolution: Implemented handleIntent in MainActivity.kt. (v8.8.6)
*   **FIXED Timing Integrity Leakage (Networking Layer) (#43)** - Resolution: Migrated to monotonic elapsedRealtime(). (v8.8.5)
*   **FIXED SIT Forensic Persistence & Engine Purity (#41)** - Resolution: Consolidated 41, 42, 52, 65, 68. (v8.8.11)
*   **FIXED Samsung OEM Field Validation (#40)** - Resolution: 10Hz GPS polling and proximity debouncing for Samsung devices. (v8.8.11)
*   **FIXED UI State Synchronization Gap (Sit Detection) (#39)** - Resolution: Updated SIT analytical ribbon to use isSitActive. (v8.8.4)
*   **FIXED AlarmActivity Logic Gap (#38)** - Resolution: Implemented StopSiren in AlarmActivity. (v8.8.4)
*   **FIXED R868: Tracker Line maxAccuracy UI (#34)** - Resolution: Corrected accuracy display in StatusBar.
*   **FIXED R916: Settings Configuration Verification (#30)** - Resolution: Removed role-based UI gating.
*   **FIXED R933: Alert Grace Period (#29)** - Resolution: Implemented 2s grace period (ALERT_TRIGGER_GRACE_PERIOD_MS).
*   **FIXED R917: Update Smoothness (#28)** - Resolution: Verified update recovery infrastructure.
*   **FIXED R872: Tracker Mode Alert Verification (#27)** - Resolution: Suppressed local alerts in tracker mode.
*   **FIXED Tracker Peer Pulse Logic Gap (#26)** - Resolution: Corrected network callback to update lastPeerActivityTs.
*   **FIXED Viewer GPS Stall Logic Mismatch (#25)** - Resolution: Migrated to monotonic time in RemoteHandler.
*   **FIXED Timing Integrity Leakage (#24)** - Resolution: Migrated Signal/Battery monitors to monotonic timing.
*   **FIXED Session Metrics Clock Inconsistency (#23)** - Resolution: Unified on elapsedRealtime() for session tracking.
*   **FIXED Viewer Infrastructure Monitoring (#22)** - Resolution: Integrated IntegrityMonitor into ViewerService.
*   **FIXED Signal Loss Logic Integration (#21)** - Resolution: Enabled signal loss alarms for both roles.
*   **FIXED Session Metrics Clock Inconsistency (#20)** - Resolution: Verified monotonic timing and forensic persistence.
*   **FIXED Dashboard Recovery Parity (#19)** - Resolution: Instantaneous UI recovery upon telemetry receipt. (v8.8.23)
*   **FIXED R923: Forensic Data Refresh (#17)** - Resolution: Dashboard recovers using max of GPS/arrival timestamps.
*   **FIXED R922: LED and Connectivity Logic (#16)** - Resolution: INT LED reflects local state; others gated by peer.
*   **FIXED Forensic I/O Efficiency & Persistence (#15)** - Resolution: Implemented safety-flush in service onDestroy.
*   **FIXED Legacy Documentation & Comment Debt (#14)** - Resolution: Removed stale references to monolithic AppService.
*   **FIXED Timing Integrity Leakage (:app layer) (#13)** - Resolution: Migrated to SystemClock.elapsedRealtime().
*   **FIXED Xiaomi Gating (#6)** - Resolution: ALERT_ID_XIAOMI_SYSTEM_MISSING implemented and gated. (v8.7.9)
*   **FIXED Logic Color Redundancy & UI Leak (#5)** - Resolution: Resolved in v8.8.1.
*   **FIXED Discrepancies with Source of Truth (v8.7.5) (#4)** - Resolution: Resolved in v8.7.7.
*   **FIXED Module Type Constraints (#3)** - Resolution: Resolved in v8.8.0.
*   **FIXED Logic Layer "Android Leaks" (#2)** - Resolution: Resolved in v8.7.6.
*   **FIXED Session Lifecycle Stability (R921/R926) (#1)** - Resolution: Exhaustive state reset implemented.
