# Open Issues
- **Issue #190: Xiaomi MIUI 14 Hardware Confirmation**: Gating logic for "Unknown" status and boot grace is implemented, but requires field verification on physical MIUI 14 hardware to ensure no "Denied" spikes occur during boot transitions.

# Fixed Issues
## 1. FIXED Session Lifecycle Stability (R921/R926) - Resolution: Exhaustive state reset implemented in MainViewModel. Mandatory landing page pause and service auto-start implemented in MainAppContent.
## 2. FIXED Logic Layer "Android Leaks" - Resolution: Resolved in v8.7.6.
## 3. FIXED Module Type Constraints - Resolution: Resolved in v8.8.0.
## 4. FIXED Discrepancies with Source of Truth (v8.7.5) - Resolution: Resolved in v8.7.7.
## 5. FIXED Logic Color Redundancy & UI Leak - Resolution: Resolved in v8.8.1.
## 6. FIXED Xiaomi Gating - Resolution: ALERT_ID_XIAOMI_SYSTEM_MISSING implemented and gated in TrackerService and ViewerService (v8.7.9).
## 13. FIXED Timing Integrity Leakage (:app layer) - Resolution: Migrated MainRepository, IntegrityMonitor, and AppSensorManager to use SystemClock.elapsedRealtime() for all debouncing, windowing, and persistence timing.
## 14. FIXED Legacy Documentation & Comment Debt - Resolution: Updated IntegrityMonitor.kt, MaintenanceWorker.kt, WatchdogReceiver.kt, and BootReceiver.kt to remove stale references to the monolithic AppService.
## 15. FIXED Forensic I/O Efficiency & Persistence - Resolution: MainRepository.addHistoryPoints now uses a monotonic interval check. Implemented repository.flushHistory() and integrated it into BaseMonitorService.onDestroy() using runBlocking to ensure pending telemetry is committed during service shutdown.
## 16. FIXED R922: LED and Connectivity Logic - Resolution: Implementation verified in SharedUiComponents.kt and TrackerService.kt. INT LED reflects local relay state; SRV/TRK/DAT/GPS are gated by peer health (isRemote), ensuring end-to-end verification. Deep inspection confirmed root-cause resolution and zero regressions in telemetry/rendering pipelines.
## 17. FIXED R923: Forensic Data Refresh - Resolution: Dashboard logic in DashboardUseCase.kt now recovers immediately upon telemetry receipt by utilizing the maximum of GPS and telemetry timestamps.
## 19. FIXED Dashboard Recovery Parity - Resolution: Implemented instantaneous UI recovery upon telemetry receipt. (v8.8.23).
## 20. FIXED Session Metrics Clock Inconsistency - Resolution: Final technical audit of v8.8.2 remediation complete. Codebase verified for monotonic timing and forensic persistence. (Procedural tagging to follow).
## 21. FIXED Signal Loss Logic Integration - Resolution: TrackerService and ViewerService now calculate silenceDelta using peer activity timestamps and pass it to checkSignalIntegrity, enabling signal loss alarms for both roles.
## 22. FIXED Viewer Infrastructure Monitoring - Resolution: ViewerService now utilizes a full IntegrityMonitor instance to poll local system status, enabling it to detect and alert on its own hardware and internet connectivity failures.
## 23. FIXED Session Metrics Clock Inconsistency - Resolution: TrackerService and ViewerService now both use monotonic time (SystemClock.elapsedRealtime()) for session duration tracking. SyncManager updated to use monotonic time when querying drop statistics, ensuring accurate peer-health reporting.
## 24. FIXED Timing Integrity Leakage (Signal & Battery) - Resolution: Migrated Signal Loss and Battery Steep Discharge monitoring to use monotonic timing (elapsedRealtime()) in IntegrityMonitor, TrackerService, and ViewerService.
## 25. FIXED Viewer GPS Stall Logic Mismatch - Resolution: TrackerGpsStallStartTs is now recorded and evaluated using monotonic time (elapsedRealtime) in both RemoteHandler and ViewerService, resolving the trigger failure.
## 26. FIXED Tracker Peer Pulse Logic Gap - Resolution: Discovered during v8.8.2 audit; TrackerService was not routing network packets to RemoteHandler. Corrected network callback to update lastPeerActivityTs, preventing false Signal Loss alarms on Tracker.
## 27. FIXED R872: Tracker Mode Alert Verification - Resolution: Verified no alerts occur in tracker mode. Suppressed `redScreenVisible` in `BehaviorUseCase.kt` when `appMode == "tracker"`. Fixed a bug in `AppAlarmManager` where simultaneous alerts were suppressed; moved `lastGlobalTriggerTs` update outside the violation loop to allow batch triggering while preserving the 2s grace period.
## 28. FIXED R917: Update Smoothness - Resolution: Verified update recovery infrastructure (`BootReceiver`, `MY_PACKAGE_REPLACED`, `START_STICKY`). Enhanced `MainViewModel` with forensic logging of version updates to ensure auditable session resets and stable post-update navigation.
## 29. FIXED R933: Alert Grace Period - Resolution: Implemented a 2-second grace period (`ALERT_TRIGGER_GRACE_PERIOD_MS`) in `AppAlarmManager` to throttle consecutive alert triggers and prevent event flooding.
## 30. FIXED R916: Settings Configuration Verification - Resolution: Removed role-based UI gating in `OverlayComponents.kt`. Tracker mode users now have full access to modify Viewer ID, Geofence radius, Alert Management, and Sound Setup, ensuring configuration parity across all modes.
## 34. FIXED R868: Tracker Line maxAccuracy UI - Resolution: Corrected `StatusBar` in `SharedUiComponents.kt` to pass `maxTrackerAccuracy` to the Tracker's `StatusRowData`, ensuring it displays the Tracker's own maximum accuracy instead of the Viewer's.
## 38. FIXED AlarmActivity Logic Gap - Resolution: Implemented implementation for `UiCommand.StopSiren` in `AlarmActivity`. The activity now dismisses automatically when alarms are silenced locally or remotely, ensuring UI state synchronization. (v8.8.4).
## 39. FIXED UI State Synchronization Gap (Sit Detection) - Resolution: Updated SharedUiComponents.kt to utilize isSitActive for the SIT analytical ribbon, ensuring stable forensic visualization with cooldown. (v8.8.4).
## 40. FIXED Samsung OEM Field Validation - Resolution: Implemented 10Hz GPS polling (`HIGH_FREQUENCY_GPS_POLLING_MS`) in `TrackerService` for Samsung S21 FE devices to ensure stability under background restrictions. Verified and hardened virtual proximity debouncing (R729) in `AppSensorManager` with a 5s window for Samsung A15 devices to prevent stuttering. (v8.8.11).
## 41. FIXED SIT Forensic Persistence & Engine Purity - Resolution: Consolidated Issues 41, 42, 52, 65, 68. Expanded SIT telemetry fields in database schemas, decoupled forensic latches from engine loops, and eliminated redundant consumption logic for 100% forensic capture (v8.8.11).
## 43. FIXED Timing Integrity Leakage (Networking Layer) - Resolution: Migrated `AppNetworkManager` and `CommunicationManager` to use monotonic `elapsedRealtime()` via `TimeProvider` for reconnection debouncing and zombie socket detection. Integrated `CommunicationManager` into the dependency graph. (v8.8.5).
## 44. FIXED Deep-Link Cold-Start Handling - Resolution: Implemented `handleIntent` in `MainActivity.kt` and called it from `onCreate` and `onNewIntent`. The app now correctly handles `ACTION_NAVIGATE_TO_MAP` deep links even when launched from a killed state. (v8.8.6).
## 45. FIXED Foreground Service Type Compliance - Resolution: Updated `ViewerService.kt` to correctly pass `FOREGROUND_SERVICE_TYPE_LOCATION` to `startForeground` for Android 10+ (targetSdk 35) and added missing `updateForegroundServiceType` assertions in UI/Command callbacks. (v8.8.7).
## 46. FIXED Forensic Telemetry Type Safety - Resolution: Resolved build blockers in v8.8.4 by enforcing strict Double vs Float types and standardizing scaling constants in Tracker/Viewer telemetry pipelines.
## 47. FIXED Xiaomi Permission Consistency & Override - Resolution: Added `is_xiaomi_manual_override` to `app_settings.proto` and implemented DataStore persistence. Updated `MainAlarmLogic.kt` to treat `UNKNOWN` status as a violation unless explicitly overridden (v8.8.9).
## 48. FIXED Acoustic Location Gating Gap - Resolution: Implemented `pendingAcousticViolation` logic in `TrackerService.kt`. Flag is set on acoustic fast-path trigger and reset on valid GPS fix, enabling the "Location Pending" UI state. (v8.8.8).
## 49. FIXED Alarm Discovery Phase Bypass - Resolution: Pass `null` to `AppAlarmManager.evaluateAlarms` in both services, allowing the manager to correctly calculate `BOOTSTRAP` and `DISCOVERING` grace periods based on service uptime. (v8.8.9).
## 50. FIXED Chair Alarm Resolution Inconsistency - Resolution: ViewerService now passes debounced `isSitActive` status to evaluateAlarms, preventing premature alarm resolution on the Viewer. (v8.8.8).
## 51. FIXED Boundary Cleanup & Persistence Integrity - Resolution: Consolidated Issues 51, 54, 57. Isolated networking into `LogRepository` and `OfflineRepository`, resolved repository syntax errors, and implemented explicit buffered telemetry purging (v8.8.11).
## 53. FIXED TrackerService Parameter Naming - Resolution: Corrected `acousticDbMin` to `acousticMinDb` in `TrackerService.kt` to resolve compilation errors following repository migration. (v8.8.11).
## 55. FIXED Physics Threshold Centralization - Resolution: Unified `EARTH_RADIUS_METERS` and other kinematic thresholds in `EngineConstants.kt` to eliminate redundancy and ensure consistency (v8.8.2).
## 56. FIXED Dependency Injection - Resolution: Moved `CommunicationManager` to `AppModule.kt` and injected `SignalingProvider` into `AppNetworkManager's constructor. (v8.8.5).
## 58. FIXED Module Hardening (:core:engine) - Resolution: Converted `:core:engine` to a pure `java-library`, enforcing a strict JVM environment with zero Android framework dependencies (v8.8.2).
## 59. FIXED Documentation & Version Synchronization - Resolution: Consolidated Issues 59, 60, 61, 62. Synchronized all README, DOCS, and internal version identifiers to v8.8.11, and updated forensic definitions for SIT and Xiaomi overrides.
## 63. FIXED Remote SIT Calibration Persistence - Resolution: Implemented `resetChairBaseline()` in `LocationProcessor.kt` which propagates the reset via the `onChairBaselineChanged` listener. (v8.8.11).
## 64. FIXED Acoustic "Location Pending" Traceability - Resolution: Updated `MainAlarmLogic.kt` to include "LOCATION_PENDING" status in the `technicalDetails` of `ViolationReport` for Geofence and Acoustic alerts. (v8.8.11).
## 66. FIXED Dashboard Speed Recovery Parity - Resolution: Resolved a double-division bug in `ViewerService.kt` and standardized speed units to m/s for HistoryManager to ensure forensic parity (v8.8.11).
## 67. FIXED S21 FE Polling Churn - Resolution: Optimized GPS polling in `TrackerService.kt` by caching the S21 FE device check and only updating `GpsManager` when the interval actually changes. (v8.8.11).
## 69. FIXED HistoryManager Efficiency - Resolution: Optimized `HistoryManager.kt` by reusing `Calendar` and `SimpleDateFormat` instances as class members to eliminate per-tick object creation. (v8.8.11).
## 70. FIXED Thermal Throttling Logic - Resolution: Implemented `COOLING` mode in `IntegrityMonitor` and `TrackerService` (46°C trigger / 44°C recovery) to protect hardware by throttling GPS polling to 30s.
## 71. FIXED Storage Integrity Monitoring - Resolution: Implemented `LOW` and `CRITICAL` storage thresholds in `IntegrityMonitor` with safety gates in `MainRepository` to prevent database corruption during space exhaustion.
## 72. FIXED OS-Level Restriction Monitoring - Resolution: Integrated Standby Bucket (`UsageStatsManager`) and Power Save Mode detection in `IntegrityMonitor` to provide forensic visibility into system-level background throttling.
## 73. FIXED Forensic Marker Debouncing - Resolution: Implemented state-latches in `TrackerService` and `ViewerService` to prevent redundant violation logging during sustained signal loss or jammer suspicion states.
## 74. FIXED Physical Tamper Fast-Paths - Resolution: Implemented low-latency sensor callbacks in `TrackerService` for Acoustic and Light triggers, enabling immediate "Suspicious Mode" activation and forensic state capture.
## 75. FIXED Dynamic Parameter Synchronization - Resolution: Service evaluation loops now reactively observe `DataStore` flows for Geofence radius and Alert settings, eliminating the need for service restarts after configuration changes.
## 76. FIXED TIMING MISMATCH (GPS Gap) - Resolution: Migrated `lastValidFixTs` in `LocationProcessor.kt` to monotonic time and implemented `trackerLastValidFixRealtime` in `RemoteHandler.kt` to ensure stable Viewer-side gap detection. (v8.8.12).
## 77. FIXED TIMING MISMATCH (SIT Cooldown) - Resolution: Implemented `lastSitRealtime` (monotonic) in `LocationSentinel.kt` for logic while preserving `lastSitTs` (wall-clock) for forensics. `TrackerService` now uses monotonic time for stable SIT cooldown evaluations. (v8.8.12).
## 78. FIXED TIMING MISMATCH (GPS Stall) - Resolution: `LocationProcessor.kt` now passes monotonic timestamps to `onGpsStallDetected`, ensuring `systemMonitor.gpsStallStartTs` is correctly evaluated against monotonic `nowRealtime`. (v8.8.12).
## 79. FIXED STICKY SIT STATE - Resolution: Re-implemented `consumeSitDetected()` in `LocationSentinel.kt` and `LocationProcessor.kt`. `TrackerService.processTick` now consumes the SIT detection state each cycle, preventing it from remaining permanently active after a trigger. (v8.8.13).
## 80. FIXED FORENSIC PERSISTENCE GAP (Viewer) - Resolution: Implemented state-latches and `addViolation` calls in `ViewerService.kt` for Signal Loss, Jammer, Stall, and Gap alerts, ensuring these events are recorded to the local database for map visualization. (8.8.14).
## 81. FIXED Forensic Persistence Gap (Tracker) - Resolution: Implemented state-latch and repository.addViolation logic in TrackerService.kt for Stall, Tamper, and Geofence violations, ensuring they are recorded to the local forensic database. (v8.8.15).
## 82. FIXED Forensic Persistence Gap (Viewer) - Resolution: Implemented state-latch and repository.addViolation logic in ViewerService.kt for ALERT_ID_TRACKER_GEOFENCE violations, enabling forensic map visualization for peer peer boundary breaches. (v8.8.16).
## 83. FIXED Redundant Tamper/SIT Alerting - Resolution: Decoupled SIT (Chair) alerting from the general tamper flag in `TrackerService.kt`. SIT and Tamper now independently evaluate in the alarm loop, preventing dual-trigger redundancy. (v8.8.18).
## 84. FIXED Missing Thermal Throttling in GPS Polling - Resolution: TrackerService now switches to `COOLING_GPS_POLLING_MS` (30s) when Cooling Mode is active, ensuring heat reduction during thermal events. This check is prioritized over other polling states. (v8.8.17).
## 85. FIXED Forensic Marker Gap (GPS Gap) - Resolution: TrackerService now includes state-latch and persistence logic for ALERT_ID_TRACKER_GAP violations, ensuring these events are recorded in the forensic log. (v8.8.15).
## 86. FIXED Inconsistent Tamper Logic - Resolution: Updated the `onLocationChanged` fast-path in `TrackerService.kt` to include the `SentinelValidator.isLightViolated` check, ensuring parity with the `processTick` loop. (v8.8.19).
## 87. FIXED Missing Database Migrations - Resolution: Added missing migration paths (24 -> 28) to the Room database configuration in `AppModule.kt`, resolving the `IllegalStateException` crash on startup. (v8.8.20).
## 88. FIXED Render Relay URL Discrepancy - Resolution: Corrected default relay URL to `gps-survival-relay.onrender.com` in `Constants.kt` and `SettingsRepository.kt` following discovery that the previous name resulted in 404 responses. (v8.8.12).
## 89. FIXED Relay Server Visibility & Health Check - Resolution: Implemented an HTTP listener in `relay-server/index.js` to provide a \"Live and Active\" status message, preventing 404s and enabling browser-based server verification. (v6.041).
## 90. FIXED Relay Diagnostic Logging - Resolution: Implemented enhanced server-side console logging to track `[JOIN]` and connection/disconnection events with explicit reasons.
## 91. FIXED Event Log Timestamp Visibility - Resolution: Increased timestamp column width from 65dp to 95dp in `LogOverlay` (OverlayComponents.kt) and enforced `maxLines = 1` to prevent time strings from overlapping with messages. (v8.8.12).
## 92. FIXED Event Log Grouping Logic - Resolution: Refined `stripLogVariableParts` in `LogRepository.kt` using raw regex strings to fix `PatternSyntaxException` and ensure correct grouping of repeated events while ignoring dynamic data like coordinates and durations. (v8.8.21).
## 93. FIXED Room-Join Identity Parity - Resolution: Corrected critical room assignment bug in `ViewerService.kt`. The Viewer now correctly joins the Tracker's ID room (T) to ensure bidirectional pulse reception, resolving the \"one-way\" green LED state. (v8.8.21).
## 95. FIXED Identity Collision & Ghost UUIDs - Resolution: Consolidated Issues 93-B and 95-C. Hardened `LogManager` and `LogEntry` to force UUID generation for blank IDs. Implemented DB Migration v29 adding the `role` column to `LogEntity`, enabling multi-role forensic traceability in unified JSON exports. (v8.8.21).
## 96. FIXED Stale Forensic Versioning (Issue 59-C) - Resolution: Dynamic version tagging implemented in `AppAlarmManager.kt` using `BuildConfig.VERSION_NAME`, replacing hardcoded legacy strings. (v8.8.21).
## 97. FIXED Geofence SOT Desync - Resolution: Corrected JSON key mismatch in `RemoteHandler.kt` to support both `home_points` (underscore) and `homePoints` (camelCase), ensuring Viewer geofence settings propagate to the Tracker. (v8.8.21).
## 98. FIXED Xiaomi Instruction Traceability - Resolution: Updated `MainAlarmLogic.kt` to provide explicit guidance in the alarm subtitle for UNKNOWN MIUI status: \"Toggle manual override in Sound Setup\". (v8.8.21).
## 99. FIXED Physical Tamper Race Condition & Trajectory Bug - Resolution: Implemented a 500ms \"Muzzle Window\" (lastSyncFlushTs) in TrackerService to suppress false vibration/shock triggers during I/O jitter. Corrected a critical bug in `LocationSentinel.storeRejected` where class fields were being assigned to themselves, which was breaking the trajectory promotion system. (v8.8.21).
## 100. FIXED Audit :core:engine Purity - Resolution: Verified that no android.* dependencies remain in the core engine source code. Verified files: LocationProcessor, LocationSentinel, ImmFilter, PhysicsUtils, MainAlarmLogic, TelemetryUtils, SentinelValidator. (v8.8.21).
## 101. FIXED Verify Forensic Identity Propagation - Resolution: New version ID correctly picked up by LogManager and SyncManager for forensic tagging. Added 'vid' field to LogEntry, TrackerStatus, LocationState, LocationUpdate, ConnectionPoint, HistoryEntity, and PendingStatusEntity. Implemented DB Migration v30. (v8.8.21).
## 102. FIXED Timing Consistency Check - Resolution: TimeProvider is now the exclusive source of truth for all duration and timeout logic across :app and :core:engine. Standardized timing in Services (BaseMonitorService, TrackerService, ViewerService), Managers (LogManager,SessionManager, GpsManager, AppSensorManager), Background tasks (MaintenanceWorker, WatchdogReceiver), and UI (MainViewModel systemPulse, MapComponents throttle). (v8.8.21).
## 103. FIXED Hardcoded Muzzle Window - Resolution: Centralized `MUZZLE_WINDOW_DURATION_MS` in `EngineConstants.kt` and updated `TrackerService.kt` to use it (v8.8.22).
## 104. FIXED Naming Mismatch (Log Muzzle) - Resolution: Renamed `LOG_MUZZLE_DURATION_MS` to `LOG_MUZZLE_STARTUP_MS` in `Constants.kt` and `LogManager.kt` to align with SoT (v8.8.22).
## 105. FIXED Version Desync (build.gradle) - Resolution: Updated `app/build.gradle` `versionName` to `8.8.22` to match architectural version (v8.8.22).
## 106. FIXED Relay URL Discrepancy (Docs) - Resolution: Updated `SETTINGS_PAGE_DETAIL.md` to reflect the correct relay URL `https://gps-survival-relay.onrender.com` (v8.8.22).
## 107. FIXED Forensic Identity Inconsistency - Resolution: Unified forensic identity across all components and documentation (v8.8.23).
## 108. FIXED Version Stale References - Resolution: Synchronized all source headers and documentation files to the new v8.8.23 baseline (v8.8.23).
## 109. FIXED versionCode Description Mismatch - Resolution: Updated `REQUIREMENTS_SOT.md` to accurately reflect the `yearOffset` implementation used in `build.gradle` (v8.8.23).
## 110. FIXED Modular Engine Hardening - Resolution: Finalized the physical isolation of the `:core:engine` as a pure JVM library. (v8.8.22).
## 111. FIXED SNR Scaling Standardization - Resolution: Replaced hardcoded SNR scaling with `RIBBON_SNR_SCALE_DB` in `TrackerService.kt`. (v8.8.24).
## 112. FIXED Historical Gap Injection - Resolution: Corrected `HistoryManager.kt` to prevent duplicate gap injection. (v8.8.24).
## 113. FIXED Ribbon UI Clipping - Resolution: Adjusted padding and stroke widths in `SharedUiComponents.kt`. (v8.8.24).
## 114. FIXED Forensic Identity Propagation - Resolution: Ensured `vid` is correctly propagated to the relay. (v8.8.25).
## 115. FIXED ViewModel Bloat - Resolution: Decoupled `MainViewModel` into feature-specific UseCases. (v8.8.25).
## 116. FIXED GpsManager Initialization Race - Resolution: Moved `OsmConfig` to background thread. (v8.8.25).
## 117. FIXED Barometer Zeroing Drift - Resolution: Increased `BARO_ZEROING_INTERVAL_MS` to 10 minutes. (v8.8.25).
## 118. FIXED Timing & Forensic Stability - Resolution: Standardized on monotonic time (`TimeProvider.elapsedRealtime()`). (v8.8.22).
## 119. FIXED OEM Restriction Verification - Resolution: Integrated Xiaomi Autostart detection and enabled 10Hz polling. (v8.8.22).
## 120. FIXED Muzzle Window & Forensic Audit - Resolution: Implemented a 500ms "Muzzle Window" during sync I/O. (v8.8.22).
## 121. FIXED LED Logic DecouPLING - Resolution: Fixed status LEDs on the Tracker side. (v8.8.31).
## 122. FIXED App Icon Foreground Branding - Resolution: Updated `ic_jd_logo.xml` to use the deer-only branding. (v8.8.30).
## 123. FIXED Identity Hardening - Resolution: Updated identity branding and versioning baseline. (v8.8.30).
## 124. FIXED GPS Revival Escalation - Resolution: Implemented a 5-minute retry loop and forensic escalation. (v8.8.31).
## 125. FIXED Monotonic UI Lockout - Resolution: Migrated all UI countdowns and pulse logic to `TimeProvider.elapsedRealtime()`. (v8.8.31).
## 126. FIXED Tracker-Side SIT Logging - Resolution: Ensured `ALERT_ID_TRACKER_CHAIR` is recorded to the local forensic database. (v8.8.31).
## 127. FIXED Xiaomi Autostart Indeterminate State - Resolution: Added `XiaomiPermissionStatus.UNKNOWN`. (v8.8.31).
## 128. FIXED Forensic Ribbon Scaling - Resolution: Standardized `RIBBON_VIBRATION_SCALE_G` and `RIBBON_SNR_SCALE_DB`. (v8.8.31).
## 129. FIXED Build Failure: Missing Symbol - Resolution: Re-added `isValidLocation` to `PhysicsUtils.kt`. (v8.8.31).
## 130. FIXED Forensic Verification Suite - Resolution: Implemented `ForensicIdentityTest.kt`. (v8.8.31).
## 131. FIXED Forensic Key Standardization (snake_case) - Resolution: Standardized all JSON keys to snake_case. (v8.8.31).
## 132. FIXED Continuity Audit & Backfill Verification - Resolution: Implemented 1Hz continuity auditing in HistoryManager.kt. (v8.8.31).
## 133. FIXED Xiaomi Background Stability Verification - Resolution: Confirmed 10Hz polling logic. (v8.9.2).
## 135. FIXED Relay Audit Verification - Resolution: Enhanced `join` payload with role and version. (v8.8.35).
## 136. FIXED AppSettings Persistence Gap - Resolution: Updated `TrackerStatusProto` to include missing forensic fields. (v8.8.33).
## 137. FIXED SettingsRepository Alignment - Resolution: Updated `SettingsRepository.kt` to persist cooling/storage flags. (v8.8.33).
## 138. FIXED SyncManager Historical Depth Gap - Resolution: Included forensic fields in the JSON payload. (v8.8.33).
## 139. FIXED Database Forensic Depth Gap - Resolution: Expanded `PendingStatusEntity` to include full forensic fields. (v8.8.33).
## 140. FIXED HistoryManager Version Tagging Bug - Resolution: Supported version-aware tagging in history updates. (v8.8.33).
## 141. FIXED Engineering Constants Typo - Resolution: Corrected `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33).
## 142. FIXED SIT Forensic Depth Gap in History - Resolution: Updated `HistoryEntity` to include SIT metrics. (v8.8.33).
## 143. FIXED SyncManager Historical Version Bug - Resolution: Updated `SyncManager.kt` to use `entity.ver`. (v8.8.33).
## 144. FIXED MainViewModel Logic Duplication - Resolution: Centralized location validation. (v8.8.33).
## 145. FIXED Hardcoded Point Count - Resolution: Replaced hardcoded `240f` with `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33).
## 146. FIXED Startup Performance (Skipped Frames) - Resolution: Moved `OsmConfig` to background thread. (v8.8.35).
## 147. FIXED Compose SnapshotStateList Warnings - Resolution: Migrated to SnapshotStateList in MapComponents.kt. (v8.8.35).
## 148. FIXED GPS Polling Stabilization (A15) - Resolution: Implemented `A15_STABLE_GPS_POLLING_MS`. (v8.8.35).
## 149. FIXED Missing Jump Markers (Forensic Parity) - Resolution: Achieved symbol parity for forensic markers. (v8.8.32).
## 150. FIXED Architectural Alignment (Standardized Alert IDs) - Resolution: Centralized `ALERT_ID_VISUAL_JUMP`. (v8.8.32).
## 151. FIXED Model Synchronization - Resolution: Aligned `:app:Models.kt` and `:core:engine:EngineModels.kt`. (v8.8.33).
## 152. FIXED Missing Database Migrations (v31) - Resolution: Implemented `MIGRATION_30_31`. (v8.8.33).
## 153. FIXED Compilation Errors (Forensic Expansion) - Resolution: Resolved build failures by adding `ver` field. (v8.8.33).
## 154. FIXED Forensic Documentation Debt - Resolution: Replaced legacy `vid` with `ver`. (v8.8.33).
## 155. FIXED Build Failure: Unfinished Forensic Simplification - Resolution: Resolved compilation errors. (v8.8.34).
## 156. FIXED Global Version Desync - Resolution: Synchronized all version strings to v8.8.35.
## 157. FIXED Forensic Documentation Mismatch - Resolution: Updated core documentation to reflect simplified forensic model.
## 158. FIXED Database Schema "Dead Weight" - Resolution: Migrated to a ver-less structure in v33. (v8.8.35).
## 159. FIXED Database Schema Cleanup (Future) - Resolution: Removed 'ver' and 'vid' columns via Room Migration v33. (v8.8.35).
## 160. FIXED Xiaomi Gating Logic Error - Resolution: Decoupled autostart from xiaomiStatus. (v8.8.35).
## 161. FIXED Viewer Alarm Title Confusion - Resolution: Updated titles to "This device:". (v8.8.35).
## 162. FIXED Constant Redundancy - Resolution: Removed duplicated constants. (v8.8.35).
## 163. FIXED Power Tamper Regression - Resolution: Reconnected battery/power callbacks to `IntegrityMonitor`. (v8.8.35).
## 164. FIXED Telemetry Validation Parity - Resolution: Standardized on `PhysicsUtils.isValidLocation`. (v8.8.35).
## 165. FIXED Code Redundancy in Utils.kt - Resolution: Migrated logic to `:core:engine`. (v8.8.36).
## 166. FIXED Build Integrity & Lint - Resolution: Verified build stability following modularization. (v8.8.36).
## 167. FIXED Documentation Debt (SoT) - Resolution: Updated SoT to include Samsung A15 polling. (v8.8.36).
## 168. FIXED Xiaomi 10Hz Stability Preparation - Resolution: Implemented Stability Audit suite. (v8.8.36).
## 169. FIXED Version Header Desync - Resolution: Synchronized source headers in Services. (v8.8.36).
## 170. FIXED Xiaomi Alert Guard - Resolution: Added `isXiaomiDevice()` check. (v8.8.36).
## 171. FIXED GPS Transition Log Muzzle - Resolution: Implemented 30s temporal muzzle for logs. (v8.8.36).
## 172. FIXED Xiaomi False Positives on Non-Xiaomi - Resolution: Added `isXiaomiDevice` flag to Evaluation state. (v8.8.36).
## 173. FIXED Tracker-Side SIT Marker Persistence - Resolution: Reconnected SIT events to local forensics. (v8.8.36).
## 174. FIXED R867: Default Identity Verification - Resolution: Updated default IDs to "Ttk" and "Cohen". (v8.8.36).
## 175. FIXED R917: Version Update Smoothness - Resolution: Verified `MY_PACKAGE_REPLACED` handling. (v8.8.36).
## 176. FIXED R941: Statistics Persistence Verification - Resolution: Confirmed statistics accumulation across restarts. (v8.8.36).
## 177. FIXED Dead Code Cleanup - Resolution: Removed redundant telemetry methods. (v8.8.37).
## 178. FIXED Forensic Parity: verticalVelocity Alignment - Resolution: Implemented full parity for verticalVelocity. (v8.8.37).
## 179. FIXED RemoteHandler SIT Mapping Audit - Resolution: Verified 100% field parity for SIT. (v8.8.37).
## 180. FIXED Forensic Pipeline Verification - Resolution: Verified 1:1 field mapping for verticalVelocity and SIT. (v8.9.2).
## 181. FIXED GPS Stability Audit Verification - Resolution: Reliability metrics emitted every 10s. (v8.9.2).
## 182. FIXED Global Version Synchronization - Resolution: Synchronized all source headers to v8.9.2. (v8.9.2).
## 183. FIXED Legacy Branding Cleanup - Resolution: Standardized to John Deere Green and logo. (v8.9.2).
## 184. FIXED Muzzle Window Hardening - Resolution: Optimized SyncManager and Tracker muzzle state. (v8.9.2).
## 185. FIXED ViewerService Listener Completion - Resolution: Fully implemented remote-to-local trail persistence. (v8.9.2).
## 186. FIXED SoT Documentation Hardening - Resolution: Updated documentation to v8.9.2 baseline. (v8.9.2).
## 187. FIXED Viewer-Side LocationProcessor State Persistence - Resolution: Updated ViewerService to load maxAccuracy. (v8.9.4).
## 188. FIXED Historical GPS Timestamp Loss - Resolution: Added gpsTs to database and sync. (v8.9.3).
## 189. FIXED Viewer Background Location Gap - Resolution: Implemented 10s background polling for Viewers. (v8.9.5).
## 190. FIXED Xiaomi Autostart Unknown Handling - Resolution: Implemented robust handling for indeterminate status. (v8.9.6).
## 191. FIXED Muzzle Window Race Condition - Resolution: Implemented deterministic Muzzle Handshake. (v8.9.6).
## 192. FIXED Power Parity Consistency & evaluateAlarms Mismatch - Resolution: Achieved absolute forensic parity for currentMa. (v8.9.5).
## 193. FIXED Zombie Telemetry UX - Resolution: Implemented visual staleness indicators ("Ghost Mode"). (v8.9.6).
## 194. FIXED SIT Persistence Packet Loss Risk - Resolution: Implemented acknowledged event synchronization pipeline. (v8.9.7).
## 195. FIXED Room Migration Forensic Audit (Android 15) - Resolution: Implemented full table reconstruction migration. (v8.9.6).
## 196. FIXED Plunge Matching: Advanced SIT Detection - Resolution: Refined "Plunge" state machine and sitVzTs propagation. (v8.9.7).
## 197. FIXED Database Schema Expansion (v38) - Resolution: Added sitVzTs to history tables. (v8.9.7).
## 198. FIXED GPS Availability Hardening - Resolution: Shortened GPS stall detection to 60s. (v8.9.8).
## 199. FIXED Toolchain Modernization - Resolution: Upgraded to Java 17 and Android SDK 35. (v8.9.8).
## 200. FIXED Room Migration Registry Fix - Resolution: Registered MIGRATION_37_38. (v8.9.8).
## 201. FIXED Multi-version Schema Robustness - Resolution: Hardened MIGRATION_35_36. (v8.9.8).
## 203. FIXED Documentation Version Desync - Resolution: Synchronized all core documentation (`APP_DESCRIPTION.md`, `SETTINGS_PAGE_DETAIL.md`, `info-elementary-fields.md`, `README.md`, `REQUIREMENTS_SOT.md`) and `issues.md` to the v8.9.9 baseline. (v8.9.9).
## 204. FIXED GPS Stall/Revival Constant Alignment (SoT) - Resolution: Updated `REQUIREMENTS_SoT.md` to match `EngineConstants.kt`: `GPS_STALL_THRESHOLD_MS` = 60s and `GPS_REVIVAL_RETRY_INTERVAL_MS` = 120s. (v8.9.8).
## 205. FIXED Muzzle Window Constant Alignment (SoT) - Resolution: Updated `REQUIREMENTS_SoT.md` to match `EngineConstants.kt`: `MUZZLE_WINDOW_DURATION_MS` = 2000ms. (v8.9.8).
## 206. FIXED Staleness Threshold Alignment (SoT) - Resolution: Updated `REQUIREMENTS_SoT.md` to match `EngineConstants.kt`: `TELEMETRY_UI_STALE_THRESHOLD_MS` = 10s and `GPS_UI_FAIL_THRESHOLD_MS` = 10s, unifying "Ghost Mode" and "Position Health" thresholds. (v8.9.8).
## 207. FIXED REQUIREMENTS_SOT Typo Audit - Resolution: Performed a comprehensive syntax audit on `REQUIREMENTS_SOT.md`. Fixed multiple constants (`TICK_INTERVAL_MS`, `PARKING_ACCEL_LIMIT`, `GPS_STALL`, `ACOUSTIC_ALERT`, `CHAIR_OCCUPIED`, `CHAIR_PLUNGE_DISTANCE_THRESHOLD`, `PING_INTERVAL_MS`, `NETWORK_TIMEOUT_MS`) that used incorrect quote characters (`"`) instead of backticks. (v8.9.9).
## 208. FIXED Log Spatial Anchor Gap - Resolution: Updated `LogManager`, `AppAlarmManager`, `TrackerService`, and `ViewerService` to capture and propagate current coordinates (`lat`/`lng`) during forensic log emission. This enables accurate historical marker reconstruction on the map. (v8.9.10).
