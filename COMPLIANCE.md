# Compliance & Operational Requirements (Audit Archive)

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and the complete Resolution Archive for all past issues.

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **Issue 14** | **Light EMA Logic**: Implemented asymmetrical rising/falling EMA factors for light baseline tracking in the engine. | **Verified (LocationSentinel)** |
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
| **Issue 6** | **Xiaomi Gating**: Implemented `ALERT_ID_XIAOMI_SYSTEM_MISSING` to detect and alert on MIUI background restrictions. | **Verified (MainAlarmLogic)** |
| **Issue 13** | **Timing Integrity**: Migrated to `SystemClock.elapsedRealtime()` for all debouncing and persistence timing to prevent wall-clock leaks. | **Verified (MainRepository)** |
| **Issue 15** | **Forensic I/O**: Implemented safety-flush in service `onDestroy` and monotonic interval checks for history persistence. | **Verified (MainRepository)** |
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
| **Issue 201** | **Schema Robustness**: Hardened `MIGRATION_35_36` with dynamic column detection for resilient upgrades. | **Verified (Database)** |
| **Issue 202** | **Multi-Version Forensic Path**: Verified schema integrity across diverse legacy upgrade paths (v33 to v38). | **Verified (Database)** |
| **Issue 203** | **Documentation Synchronization**: Synchronized all core documentation to the v8.9.11 logic baseline. | **Verified (DOCS)** |
| **Issue 204/205**| **Constant Hardening Audit**: Verified GPS Stall (60s), Revival (120s), and Muzzle Window (2000ms) thresholds across documentation. | **Verified (SoT/EngineConstants)** |
| **Issue 206** | **Staleness Unification Audit**: Verified 10s unification for Ghost Mode and Position Health thresholds. | **Verified (SoT/Dashboard)** |
| **Issue 207** | **SoT Typo Audit**: Fixed incorrect quote characters in constant definitions in REQUIREMENTS_SOT.md. | **Verified (SoT)** |
| **Issue 208** | **Log Spatial Anchor Gap**: Implemented coordinate-aware forensic logging. All critical alerts and system events now include `lat`/`lng` for map reconstruction. | **Verified (LogManager/Services)** |
| **Issue 209** | **Coordinate Propagation Race**: Ensured 1:1 mapping between processing and logging coordinates by expanding the processing listener. | **Verified (LocationProcessor/Services)** |
| **Issue 210** | **Log Manager Redundancy Audit**: Eliminated redundant service-level coordinate logic; standardized on `LogManager` auto-anchoring. | **Verified (TrackerService/ViewerService)** |
| **Issue 211** | **Stability Audit Verbosity**: Gated STABILITY AUDIT logs to only emit when performance falls below 98% reliability or 200ms gap. | **Verified (TrackerService)** |
| **Issue 212** | **Forensic Accuracy Parity**: Logs now preserve and reconstruct historical marker precision using log-specific accuracy values. | **Verified (LogEntry/RemoteHandler)** |
| **Issue 213** | **Forensic Anchor Desync in AppAlarmManager**: Explicitly propagated coordinates and accuracy in alarm logging callbacks. | **Verified (AppAlarmManager/Services)** |
| **Issue 214** | **Redundant Accuracy Fallback Logic**: Unified accuracy fallback in LogManager to prioritize fix accuracy then maxAccuracy. | **Verified (LogManager)** |
| **Issue 215** | **Missing "Recovery" Logs for Stability Audit**: Implemented "STABILITY RESTORED" marker for performance recovery events. | **Verified (TrackerService)** |
| **Issue 216** | **Accuracy Propagation Gaps in ViewerService**: Hardened spatial anchoring for Viewer pulses and session terminations. | **Verified (ViewerService)** |
| **Issue 217** | **LocationProcessor Inconsistency**: Standardized "Merge-on-Stale" logs to use hardened auto-anchoring path. | **Verified (LocationProcessor)** |
| **Issue 218** | **Xiaomi MIUI 14 Heuristic Recovery**: Detects background tick suppression (gap > 15s) and triggers GPS revival and WakeLock renewal. | **Verified (TrackerService)** |
| **Issue 219** | **Adaptive Jump Confidence**: Correlates SNR and Vibration to distinguish between signal reflection and legitimate movement. | **Verified (LocationSentinel)** |
| **Issue 220** | **Hindsight Correction**: Implements retroactive trajectory smoothing using a hindsight rolling buffer. | **Verified (LocationSentinel)** |
| **Issue 221** | **Bayesian Uncertainty Scaling**: UI radius expands at 15m/s during GPS stalls based on `lastValidFixRealtime`. | **Verified (MapComponents)** |
| **Issue 222** | **Hindsight Path Visualization**: Renders "Ghost Paths" (Slate500) for points retroactively promoted. | **Verified (MapComponents)** |
| **Issue 223** | **Forensic Log Enrichment**: SNR and Vibration snapshots attached to jump/stall logs for black-box analysis. | **Verified (LogManager/Services)** |
| **Issue 224** | **SIT Forensic Expansion**: Added tiltIdx and baroIdx to analytical ribbons and telemetry pipeline for enhanced chair event analysis. | **Verified (AnalyticalRibbons/Telemetry)** |
| **Issue 225** | **Analytical Ribbon Persistence**: Verified 240-point in-memory retention for all new forensic indices. | **Verified (HistoryManager)** |
| **Issue 226** | **Contextual Uncertainty**: Implemented `LocationPendingReason` to provide UI context for Bayesian expansion. | **Verified (EngineModels/SyncManager)** |
| **Issue 227** | **Hindsight Transition Smoothing**: Added `promotedPoints` to `SentinelResult` for improved map trail continuity. | **Verified (LocationSentinel)** |
| **Issue 228/229**| **SoT Constant Synchronization**: Standardized GPS_STABILITY_RELIABILITY_THRESHOLD to 98.0% and removed redundant DISTANCE_GRACE_MS. | **Verified (SoT/EngineConstants)** |
| **Issue 230** | **Alert Label Unification**: Standardized `ALERT_ID_TRACKER_CHAIR` subtitle to "Chair occupancy detected". | **Verified (MainAlarmLogic)** |
| **Issue 231** | **Visual Jump Integrity**: Implemented trajectory-aware `VISUAL_JUMP` detection in the core engine. | **Verified (MainAlarmLogic/Sentinel)** |
| **Issue 232** | **Forensic Current Ribbon**: Added `RIBBON_CURRENT_SCALE_MA` (1000mA) for battery current visualization. | **Verified (EngineConstants)** |
| **Issue 233** | **Manual Overrides Recovery**: Ensured `is_xiaomi_manual_override` is correctly propagated during thermal/low-power throttling. | **Verified (IntegrityMonitor)** |
| **Issue 234** | **Vibration Window Stabilization**: Adjusted `VIBRATION_WINDOW_SIZE` to 5 samples for reduced jitter in stationary states. | **Verified (EngineConstants)** |
| **Issue 235** | **Acoustic Floor Decay**: Implemented `ACOUSTIC_FLOOR_CONTRACTION_EMA` (0.995) for passive environmental calibration. | **Verified (LocationSentinel)** |
| **Issue 236** | **Relay Rejoin Strategy**: Verified `NET_REJOIN_THRESHOLD_MS` (15s) logic in `AppNetworkManager` for rapid blackout recovery. | **Verified (Network)** |
| **Issue 237** | **Session Metrics Persistence**: Verified atomic `saveSessionMetricsBulk` calls for reduced DataStore I/O during high-frequency tracking. | **Verified (SettingsRepository)** |
| **Issue 238** | **Siren Auto-Recovery**: Verified `SIREN_RESUME_COOLDOWN_MS` (15s) logic to prevent permanent siren lockout after hardware auto-stop. | **Verified (AppAlarmManager)** |
| **Issue 239** | **Hindsight Buffer Bug**: Fixed bug in `LocationSentinel.kt` where old rejected points were not cleared after trajectory promotion. | **Verified (LocationSentinel)** |
| **Issue 240** | **Contextual Uncertainty Context**: Propagated `locationPendingReason` through `SyncManager` to ensure remote Viewer context. | **Verified (SyncManager)** |
| **Issue 241** | **Forensic Log Enrichment**: Bridged SNR and Vibration snapshots to `AppAlarmManager` for more detailed black-box analysis. | **Verified (AppAlarmManager)** |
| **Issue 242** | **Redundant Index Calculation**: Viewer now utilizes pre-calculated indices transmitted from the tracker's engine. | **Verified (ViewerService)** |
| **Issue 243** | **Visual Jump Sensitivity**: Expanded Tier 3 Jitter gating to ensure no "visual jitters" are missed during low-vibration movement. | **Verified (TrackerService)** |
| **Issue 244** | **Location Pending Persistence**: Ensured `locationPendingReason` survives offline storage and synchronization cycles. | **Verified (SyncManager/Room)** |
| **Issue 245** | **Duplicate SIT Event Rising-Edge**: Centralized SIT rising-edge detection in `RemoteHandler.kt` to eliminate redundant logs. | **Verified (RemoteHandler)** |

---

## 2. Resolution Archive

### 2.1. Current Phase Resolutions (v8.9.27+)
*   **FIXED Light EMA Logic Inconsistency (Issue #14)** - Resolution: Implemented rising/falling EMA factors (LUX_EMA_UP_FAST, LUX_EMA_DOWN_FAST) in `LocationSentinel.kt` for asymmetrical baseline adaptation. (v8.9.27)
*   **FIXED Constant Name Mismatch (Issue #228)** - Resolution: SoT updated to use `PING_INTERVAL_MS` (10,000ms) for both relay heartbeats and log synchronization.
*   **FIXED System Versioning Inconsistency (Issue #203)** - Resolution: All code headers and SoT synchronized to v8.9.27.
*   **FIXED Latitude Conversion Precision (Issue #228)** - Resolution: Both Code and SoT now use the high-precision constant: `111194.92664455874`.
*   **FIXED Version Generation Logic (Issue #199)** - Resolution: SoT updated to reflect Git-based versioning (git rev-list --count HEAD).
*   **FIXED Standardized Alert Title Inconsistency (Issue #230)** - Resolution: Code updated to use "Tracker:" prefix for consistent remote attribution.
*   **FIXED Role Prefix Enforcement (Issue #182)** - Resolution: Section 4.1 of SoT updated with role identity standards ("T"/"C" prefixes).
*   **FIXED Jump Classification Conflict (Issue #231)** - Resolution: SoT Manifest (Section 8) clarified to show Outliers are filtered under the JUMP_ALERT security tier.
*   **FIXED Documentation Internal Contradiction (Issue #6)** - Resolution: Updated `DEVICE_SPECIFIC_ADAPTATIONS.md` to correctly reflect the 200ms `GPS_STABILITY_GAP_THRESHOLD_MS`.

### 2.2. Middle Era Resolutions (#100 - #199)
## 199. FIXED Toolchain Modernization - Resolution: Upgraded to Java 17 and Android SDK 35. (v8.9.8)
## 198. FIXED GPS Availability Hardening - Resolution: Shortened GPS stall detection to 60s. (v8.9.8)
## 197. FIXED Database Schema Expansion (v38) - Resolution: Added sitVzTs to history tables. (v8.9.7)
## 196. FIXED Plunge Matching: Advanced SIT Detection - Resolution: Refined "Plunge" state machine and sitVzTs propagation. (v8.9.7)
## 195. FIXED Room Migration Forensic Audit (Android 15) - Resolution: Implemented full table reconstruction migration. (v8.9.6)
## 194. FIXED SIT Persistence Packet Loss Risk - Resolution: Implemented acknowledged event synchronization pipeline. (v8.9.7)
## 193. FIXED Zombie Telemetry UX - Resolution: Implemented visual staleness indicators ("Ghost Mode"). (v8.9.6)
## 192. FIXED Power Parity Consistency & evaluateAlarms Mismatch - Resolution: Achieved absolute forensic parity for currentMa. (v8.9.5)
## 191. FIXED Muzzle Window Race Condition - Resolution: Implemented deterministic Muzzle Handshake. (v8.9.6)
## 190. FIXED Xiaomi Autostart Unknown Handling (Logic) - Resolution: Implemented robust handling for indeterminate status. Expanded technicalDetails with uptime and grace threshold (v8.9.16). (Note: Hardware verification pending).
## 189. FIXED Viewer Background Location Gap - Resolution: Implemented 10s background polling for Viewers. (v8.9.5)
## 188. FIXED Historical GPS Timestamp Loss - Resolution: Added gpsTs to database and sync. (v8.9.3)
## 187. FIXED Viewer-Side LocationProcessor State Persistence - Resolution: Updated ViewerService to load maxAccuracy. (v8.9.4)
## 186. FIXED SoT Documentation Hardening - Resolution: Updated documentation to v8.9.2 baseline. (v8.9.2)
## 185. FIXED ViewerService Listener Completion - Resolution: Fully implemented remote-to-local trail persistence. (v8.9.2)
## 184. FIXED Muzzle Window Hardening - Resolution: Optimized SyncManager and Tracker muzzle state. (v8.9.2)
## 183. FIXED Legacy Branding Cleanup - Resolution: Standardized to John Deere Green and logo. (v8.9.2)
## 182. FIXED Global Version Synchronization - Resolution: Synchronized all source headers to v8.9.2. (v8.9.2)
## 181. FIXED GPS Stability Audit Verification - Resolution: Reliability metrics emitted every 10s. (v8.9.2)
## 180. FIXED Forensic Pipeline Verification - Resolution: Verified 1:1 field mapping for verticalVelocity and SIT. (v8.9.2)
## 179. FIXED RemoteHandler SIT Mapping Audit - Resolution: Verified 100% field parity for SIT. (v8.8.37)
## 178. FIXED Forensic Parity: verticalVelocity Alignment - Resolution: Implemented full parity for verticalVelocity. (v8.8.37)
## 177. FIXED Dead Code Cleanup - Resolution: Removed redundant telemetry methods. (v8.8.37)
## 176. FIXED R941: Statistics Persistence Verification - Resolution: Confirmed statistics accumulation across restarts. (v8.8.36)
## 175. FIXED R917: Version Update Smoothness - Resolution: Verified `MY_PACKAGE_REPLACED` handling. (v8.8.36)
## 174. FIXED R867: Default Identity Verification - Resolution: Updated default IDs to "Ttk" and "Cohen". (v8.8.36)
## 173. FIXED Tracker-Side SIT Marker Persistence - Resolution: Reconnected SIT events to local forensics. (v8.8.36)
## 172. FIXED Xiaomi False Positives on Non-Xiaomi - Resolution: Added `isXiaomiDevice` flag to Evaluation state. (v8.8.36)
## 171. FIXED GPS Transition Log Muzzle - Resolution: Implemented 30s temporal muzzle for logs. (v8.8.36)
## 170. FIXED Xiaomi Alert Guard - Resolution: Added `isXiaomiDevice()` check. (v8.8.36)
## 169. FIXED Version Header Desync - Resolution: Synchronized source headers in Services. (v8.8.36)
## 168. FIXED Xiaomi 10Hz Stability Preparation - Resolution: Implemented Stability Audit suite. (v8.8.36)
## 167. FIXED Documentation Debt (SoT) - Resolution: Updated SoT to include Samsung A15 polling. (v8.8.36)
## 166. FIXED Build Integrity & Lint - Resolution: Verified build stability following modularization. (v8.8.36)
## 165. FIXED Code Redundancy in Utils.kt - Resolution: Migrated logic to `:core:engine`. (v8.8.36)
## 164. FIXED Telemetry Validation Parity - Resolution: Standardized on `PhysicsUtils.isValidLocation`. (v8.8.35)
## 163. FIXED Power Tamper Regression - Resolution: Reconnected battery/power callbacks to `IntegrityMonitor`. (v8.8.35)
## 162. FIXED Constant Redundancy - Resolution: Removed duplicated constants. (v8.8.35)
## 161. FIXED Viewer Alarm Title Confusion - Resolution: Updated titles to "This device:". (v8.8.35)
## 160. FIXED Xiaomi Gating Logic Error - Resolution: Decoupled autostart from xiaomiStatus. (v8.8.35)
## 159. FIXED Database Schema Cleanup (Future) - Resolution: Removed 'ver' and 'vid' columns via Room Migration v33. (v8.8.35)
## 158. FIXED Database Schema "Dead Weight" - Resolution: Migrated to a ver-less structure in v33. (v8.8.35)
## 157. FIXED Forensic Documentation Mismatch - Resolution: Updated core documentation to reflect simplified forensic model.
## 156. FIXED Global Version Desync - Resolution: Synchronized all version strings to v8.8.35.
## 155. FIXED Build Failure: Unfinished Forensic Simplification - Resolution: Resolved compilation errors. (v8.8.34)
## 154. FIXED Forensic Documentation Debt - Resolution: Replaced legacy `vid` with `ver`. (v8.8.33)
## 153. FIXED Compilation Errors (Forensic Expansion) - Resolution: Resolved build failures by adding `ver` field. (v8.8.33)
## 152. FIXED Missing Database Migrations (v31) - Resolution: Implemented `MIGRATION_30_31`. (v8.8.33)
## 151. FIXED Model Synchronization - Resolution: Aligned `:app:Models.kt` and `:core:engine:EngineModels.kt`. (v8.8.33)
## 150. FIXED Architectural Alignment (Standardized Alert IDs) - Resolution: Centralized `ALERT_ID_VISUAL_JUMP`. (v8.8.32)
## 149. FIXED Missing Jump Markers (Forensic Parity) - Resolution: Achieved symbol parity for forensic markers. (v8.8.32)
## 148. FIXED GPS Polling Stabilization (A15) - Resolution: Implemented `A15_STABLE_GPS_POLLING_MS`. (v8.8.35)
## 147. FIXED Compose SnapshotStateList Warnings - Resolution: Migrated to SnapshotStateList in MapComponents.kt. (v8.8.35)
## 146. FIXED Startup Performance (Skipped Frames) - Resolution: Moved `OsmConfig` to background thread. (v8.8.35)
## 145. FIXED Hardcoded Point Count - Resolution: Replaced hardcoded `240f` with `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33)
## 144. FIXED MainViewModel Logic Duplication - Resolution: Centralized location validation. (v8.8.33)
## 143. FIXED SyncManager Historical Version Bug - Resolution: Updated `SyncManager.kt` to use `entity.ver`. (v8.8.33)
## 142. FIXED SIT Forensic Depth Gap in History - Resolution: Updated `HistoryEntity` to include SIT metrics. (v8.8.33)
## 141. FIXED Engineering Constants Typo - Resolution: Corrected `MAX_HISTORY_POINTS_PER_RIBBONS`. (v8.8.33)
## 140. FIXED HistoryManager Version Tagging Bug - Resolution: Supported version-aware tagging in history updates. (v8.8.33)
## 139. FIXED Database Forensic Depth Gap - Resolution: Expanded `PendingStatusEntity` to include full forensic fields. (v8.8.33)
## 138. FIXED SyncManager Historical Depth Gap - Resolution: Included forensic fields in the JSON payload. (v8.8.33)
## 137. FIXED SettingsRepository Alignment - Resolution: Updated `SettingsRepository.kt` to persist cooling/storage flags. (v8.8.33)
## 136. FIXED AppSettings Persistence Gap - Resolution: Updated `TrackerStatusProto` to include missing forensic fields. (v8.8.33)
## 135. FIXED Relay Audit Verification - Resolution: Enhanced `join` payload with role and version. (v8.8.35)
## 133. FIXED Xiaomi Background Stability Verification - Resolution: Confirmed 10Hz polling logic. (v8.9.2)
## 132. FIXED Continuity Audit & Backfill Verification - Resolution: Implemented 1Hz continuity auditing in HistoryManager.kt. (v8.8.31)
## 131. FIXED Forensic Key Standardization (snake_case) - Resolution: Standardized all JSON keys to snake_case. (v8.8.31)
## 130. FIXED Forensic Verification Suite - Resolution: Implemented `ForensicIdentityTest.kt`. (v8.8.31)
## 129. FIXED Build Failure: Missing Symbol - Resolution: Re-added `isValidLocation` to `PhysicsUtils.kt`. (v8.8.31)
## 128. FIXED Forensic Ribbon Scaling - Resolution: Standardized `RIBBON_VIBRATION_SCALE_G` and `RIBBON_SNR_SCALE_DB`. (v8.8.31)
## 127. FIXED Xiaomi Autostart Indeterminate State - Resolution: Added `XiaomiPermissionStatus.UNKNOWN`. (v8.8.31)
## 126. FIXED Tracker-Side SIT Logging - Resolution: Ensured `ALERT_ID_TRACKER_CHAIR` is recorded to the local forensic database. (v8.8.31)
## 125. FIXED Monotonic UI Lockout - Resolution: Migrated all UI countdowns and pulse logic to `TimeProvider.elapsedRealtime()`. (v8.8.31)
## 124. FIXED GPS Revival Escalation - Resolution: Implemented a 5-minute retry loop and forensic escalation. (v8.8.31)
## 123. FIXED Identity Hardening - Resolution: Updated identity branding and versioning baseline. (v8.8.30)
## 122. FIXED App Icon Foreground Branding - Resolution: Updated `ic_jd_logo.xml` to use the deer-only branding. (v8.8.30)
## 121. FIXED LED Logic DecouPLING - Resolution: Fixed status LEDs on the Tracker side. (v8.8.31)
## 120. FIXED Muzzle Window & Forensic Audit - Resolution: Implemented a 500ms "Muzzle Window" during sync I/O. (v8.8.22)
## 119. FIXED OEM Restriction Verification - Resolution: Integrated Xiaomi Autostart detection and enabled 10Hz polling. (v8.8.22)
## 118. FIXED Timing & Forensic Stability - Resolution: Standardized on monotonic time (`TimeProvider.elapsedRealtime()`). (v8.8.22)
## 117. FIXED Barometer Zeroing Drift - Resolution: Increased `BARO_ZEROING_INTERVAL_MS` to 10 minutes. (v8.8.25)
## 116. FIXED GpsManager Initialization Race - Resolution: Moved `OsmConfig` to background thread. (v8.8.25)
## 115. FIXED ViewModel Bloat - Resolution: Decoupled `MainViewModel` into feature-specific UseCases. (v8.8.25)
## 114. FIXED Forensic Identity Propagation - Resolution: Ensured `vid` is correctly propagated to the relay. (v8.8.25)
## 113. FIXED Ribbon UI Clipping - Resolution: Adjusted padding and stroke widths in `SharedUiComponents.kt`. (v8.8.24)
## 112. FIXED Historical Gap Injection - Resolution: Corrected `HistoryManager.kt` to prevent duplicate gap injection. (v8.8.24)
## 111. FIXED SNR Scaling Standardization - Resolution: Replaced hardcoded SNR scaling with `RIBBON_SNR_SCALE_DB` in `TrackerService.kt`. (v8.8.24)
## 110. FIXED Modular Engine Hardening - Resolution: Finalized the physical isolation of the `:core:engine` as a pure JVM library. (v8.8.22)
## 109. FIXED versionCode Description Mismatch - Resolution: Updated `REQUIREMENTS_SOT.md` to accurately reflect the `yearOffset` implementation used in `build.gradle` (v8.8.23)
## 108. FIXED Version Stale References - Resolution: Synchronized all source headers and documentation files to the new v8.8.23 baseline (v8.8.23)
## 107. FIXED Forensic Identity Inconsistency - Resolution: Unified forensic identity across all components and documentation (v8.8.23)
## 106. FIXED Relay URL Discrepancy (Docs) - Resolution: Updated `SETTINGS_PAGE_DETAIL.md` to reflect the correct relay URL `https://gps-survival-relay.onrender.com` (v8.8.22)
## 105. FIXED Version Desync (build.gradle) - Resolution: Updated `app/build.gradle` `versionName` to `8.8.22` to match architectural version (v8.8.22)
## 104. FIXED Naming Mismatch (Log Muzzle) - Resolution: Renamed `LOG_MUZZLE_DURATION_MS` to `LOG_MUZZLE_STARTUP_MS` in `Constants.kt` and `LogManager.kt` to align with SoT (v8.8.22)
## 103. FIXED Hardcoded Muzzle Window - Resolution: Centralized `MUZZLE_WINDOW_DURATION_MS` in `EngineConstants.kt` and updated `TrackerService.kt` to use it (v8.8.22)
## 102. FIXED Timing Consistency Check - Resolution: TimeProvider is now the exclusive source of truth for all duration and timeout logic across :app and :core:engine. Standardized timing in Services. (v8.8.21)
## 101. FIXED Verify Forensic Identity Propagation - Resolution: New version ID correctly picked up by LogManager and SyncManager for forensic tagging. Implemented DB Migration v30. (v8.8.21)
## 100. FIXED Audit :core:engine Purity - Resolution: Verified that no android.* dependencies remain in the core engine source code. (v8.8.21)

### 2.3. Legacy Foundation Resolutions (#1 - #99)
## 99. FIXED Physical Tamper Race Condition & Trajectory Bug - Resolution: Implemented 500ms Muzzle Window and corrected LocationSentinel.storeRejected bug. (v8.8.21)
## 98. FIXED Xiaomi Instruction Traceability - Resolution: Updated MainAlarmLogic.kt for UNKNOWN MIUI status guidance. (v8.8.21)
## 97. FIXED Geofence SOT Desync - Resolution: Corrected JSON key mismatch for home_points/homePoints. (v8.8.21)
## 96. FIXED Stale Forensic Versioning (Issue 59-C) - Resolution: Dynamic version tagging implemented in AppAlarmManager.kt. (v8.8.21)
## 95. FIXED Identity Collision & Ghost UUIDs - Resolution: Consolidated 93-B/95-C. Hardened LogManager and implemented DB Migration v29. (v8.8.21)
## 93. FIXED Room-Join Identity Parity - Resolution: Corrected room assignment bug in ViewerService.kt. (v8.8.21)
## 92. FIXED Event Log Grouping Logic - Resolution: Refined stripLogVariableParts regex. (v8.8.21)
## 91. FIXED Event Log Timestamp Visibility - Resolution: Increased timestamp column width to 95dp. (v8.8.12)
## 90. FIXED Relay Diagnostic Logging - Resolution: Enhanced server-side console logging.
## 89. FIXED Relay Server Visibility & Health Check - Resolution: Implemented HTTP listener in relay-server/index.js. (v6.041)
## 88. FIXED Render Relay URL Discrepancy - Resolution: Corrected default relay URL. (v8.8.12)
## 87. FIXED Missing Database Migrations - Resolution: Added missing migration paths (24 -> 28). (v8.8.20)
## 86. FIXED Inconsistent Tamper Logic - Resolution: Updated onLocationChanged fast-path in TrackerService.kt. (v8.8.19)
## 85. FIXED Forensic Marker Gap (GPS Gap) - Resolution: Implemented state-latch and persistence logic. (v8.8.15)
## 84. FIXED Missing Thermal Throttling in GPS Polling - Resolution: Implemented COOLING_GPS_POLLING_MS. (v8.8.17)
## 83. FIXED Redundant Tamper/SIT Alerting - Resolution: Decoupled SIT alerting from general tamper flag. (v8.8.18)
## 82. FIXED Forensic Persistence Gap (Viewer) - Resolution: Implemented state-latch for GEOFENCE violations. (v8.8.16)
## 81. FIXED Forensic Persistence Gap (Tracker) - Resolution: Implemented state-latch for Stall, Tamper, and Geofence. (v8.8.15)
## 80. FIXED FORENSIC PERSISTENCE GAP (Viewer) - Resolution: Implemented state-latches for Signal Loss, Jammer, Stall, and Gap. (8.8.14)
## 79. FIXED STICKY SIT STATE - Resolution: Re-implemented consumeSitDetected() in LocationSentinel.kt. (v8.8.13)
## 78. FIXED TIMING MISMATCH (GPS Stall) - Resolution: Migrated to monotonic timestamps in LocationProcessor.kt. (v8.8.12)
## 77. FIXED TIMING MISMATCH (SIT Cooldown) - Resolution: Implemented lastSitRealtime in LocationSentinel.kt. (v8.8.12)
## 76. FIXED TIMING MISMATCH (GPS Gap) - Resolution: Migrated lastValidFixTs to monotonic time. (v8.8.12)
## 75. FIXED Dynamic Parameter Synchronization - Resolution: Reactive observation of DataStore flows.
## 74. FIXED Physical Tamper Fast-Paths - Resolution: Low-latency sensor callbacks in TrackerService.
## 73. FIXED Forensic Marker Debouncing - Resolution: Implemented state-latches in Services.
## 72. FIXED OS-Level Restriction Monitoring - Resolution: Integrated Standby Bucket and Power Save detection.
## 71. FIXED Storage Integrity Monitoring - Resolution: Implemented LOW and CRITICAL storage thresholds.
## 70. FIXED Thermal Throttling Logic - Resolution: Implemented COOLING mode (46°C trigger / 44°C recovery).
## 69. FIXED HistoryManager Efficiency - Resolution: Optimized calendar and date format reuse. (v8.8.11)
## 67. FIXED S21 FE Polling Churn - Resolution: Cached device check and updated GpsManager reactively. (v8.8.11)
## 66. FIXED Dashboard Speed Recovery Parity - Resolution: Resolved double-division bug and standardized units. (v8.8.11)
## 64. FIXED Acoustic "Location Pending" Traceability - Resolution: Included LOCATION_PENDING status in technicalDetails. (v8.8.11)
## 63. FIXED Remote SIT Calibration Persistence - Resolution: Implemented resetChairBaseline() in LocationProcessor.kt. (v8.8.11)
## 59. FIXED Documentation & Version Synchronization - Resolution: Consolidated 59, 60, 61, 62. Synchronized README and DOCS.
## 58. FIXED Module Hardening (:core:engine) - Resolution: Converted to pure java-library. (v8.8.2)
## 56. FIXED Dependency Injection - Resolution: Moved CommunicationManager to AppModule.kt. (v8.8.5)
## 55. FIXED Physics Threshold Centralization - Resolution: Unified constants in EngineConstants.kt. (v8.8.2)
## 53. FIXED TrackerService Parameter Naming - Resolution: Corrected acousticDbMin to acousticMinDb. (v8.8.11)
## 51. FIXED Boundary Cleanup & Persistence Integrity - Resolution: Consolidated 51, 54, 57. Isolated networking. (v8.8.11)
## 50. FIXED Chair Alarm Resolution Inconsistency - Resolution: Debounced isSitActive status propagation. (v8.8.8)
## 49. FIXED Alarm Discovery Phase Bypass - Resolution: Corrected Discovery grace period calculation. (v8.8.9)
## 48. FIXED Acoustic Location Gating Gap - Resolution: Implemented pendingAcousticViolation logic. (v8.8.8)
## 47. FIXED Xiaomi Permission Consistency & Override - Resolution: Implemented manual override in DataStore. (v8.8.9)
## 46. FIXED Forensic Telemetry Type Safety - Resolution: Enforced strict Double vs Float types.
## 45. FIXED Foreground Service Type Compliance - Resolution: Updated to FOREGROUND_SERVICE_TYPE_LOCATION for SDK 35. (v8.8.7)
## 44. FIXED Deep-Link Cold-Start Handling - Resolution: Implemented handleIntent in MainActivity.kt. (v8.8.6)
## 43. FIXED Timing Integrity Leakage (Networking Layer) - Resolution: Migrated to monotonic elapsedRealtime(). (v8.8.5)
## 41. FIXED SIT Forensic Persistence & Engine Purity - Resolution: Consolidated 41, 42, 52, 65, 68. (v8.8.11)
## 40. FIXED Samsung OEM Field Validation - Resolution: 10Hz GPS polling and proximity debouncing for Samsung devices. (v8.8.11)
## 39. FIXED UI State Synchronization Gap (Sit Detection) - Resolution: Updated SIT analytical ribbon to use isSitActive. (v8.8.4)
## 38. FIXED AlarmActivity Logic Gap - Resolution: Implemented StopSiren in AlarmActivity. (v8.8.4)
## 34. FIXED R868: Tracker Line maxAccuracy UI - Resolution: Corrected accuracy display in StatusBar.
## 30. FIXED R916: Settings Configuration Verification - Resolution: Removed role-based UI gating.
## 29. FIXED R933: Alert Grace Period - Resolution: Implemented 2s grace period (ALERT_TRIGGER_GRACE_PERIOD_MS).
## 28. FIXED R917: Update Smoothness - Resolution: Verified update recovery infrastructure.
## 27. FIXED R872: Tracker Mode Alert Verification - Resolution: Suppressed local alerts in tracker mode.
## 26. FIXED Tracker Peer Pulse Logic Gap - Resolution: Corrected network callback to update lastPeerActivityTs.
## 25. FIXED Viewer GPS Stall Logic Mismatch - Resolution: Migrated to monotonic time in RemoteHandler.
## 24. FIXED Timing Integrity Leakage - Resolution: Migrated Signal/Battery monitors to monotonic timing.
## 23. FIXED Session Metrics Clock Inconsistency - Resolution: Unified on elapsedRealtime() for session tracking.
## 22. FIXED Viewer Infrastructure Monitoring - Resolution: Integrated IntegrityMonitor into ViewerService.
## 21. FIXED Signal Loss Logic Integration - Resolution: Enabled signal loss alarms for both roles.
## 20. FIXED Session Metrics Clock Inconsistency - Resolution: Verified monotonic timing and forensic persistence.
## 19. FIXED Dashboard Recovery Parity - Resolution: Instantaneous UI recovery upon telemetry receipt. (v8.8.23)
## 17. FIXED R923: Forensic Data Refresh - Resolution: Dashboard recovers using max of GPS/arrival timestamps.
## 16. FIXED R922: LED and Connectivity Logic - Resolution: INT LED reflects local state; others gated by peer.
## 15. FIXED Forensic I/O Efficiency & Persistence - Resolution: Implemented safety-flush in service onDestroy.
## 14. FIXED Legacy Documentation & Comment Debt - Resolution: Removed stale references to monolithic AppService.
## 13. FIXED Timing Integrity Leakage (:app layer) - Resolution: Migrated to SystemClock.elapsedRealtime().
## 6. FIXED Xiaomi Gating - Resolution: ALERT_ID_XIAOMI_SYSTEM_MISSING implemented and gated. (v8.7.9)
## 5. FIXED Logic Color Redundancy & UI Leak - Resolution: Resolved in v8.8.1.
## 4. FIXED Discrepancies with Source of Truth (v8.7.5) - Resolution: Resolved in v8.7.7.
## 3. FIXED Module Type Constraints - Resolution: Resolved in v8.8.0.
## 2. FIXED Logic Layer "Android Leaks" - Resolution: Resolved in v8.7.6.
## 1. FIXED Session Lifecycle Stability (R921/R926) - Resolution: Exhaustive state reset implemented.
