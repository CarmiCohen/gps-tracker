# Project History & Versioning

## v8.9.8
- **Room Migration Registry Fix (Issue 200)**: Registered `MIGRATION_37_38` in `AppModule.kt` to prevent startup crashes on upgraded devices.
- **Multi-version Schema Robustness (Issue 201)**: Hardened `MIGRATION_35_36` with dynamic column detection for the `logs` table, ensuring resilient upgrades across diverse legacy version paths.
- **Zombie Telemetry UX Sweep (Issue 193)**: Finalized forensic UI sweep. Applied `Slate500` ("Ghost Mode") dimming to Map markers, accuracy circles, all sensor-derived fields in the Dashboard, and LogOverlay entries when telemetry is stale (>10s).
- **Xiaomi Boot Resilience (Issue 190)**: Implemented `XIAOMI_BOOT_GRACE_MS` (30s) and monotonic timing to suppress transient "System Not Ready" alarms during the MIUI boot transition.
- **Aggressive Stall Recovery (Issue 198)**: Shortened GPS stall detection to 60s and revival retry to 120s to ensure high-availability tracking on restricted OEM hardware.
- **Build Modernization (Issue 199)**: Upgraded project toolchain to Java 17 and aligned with Android SDK 35. Cleaned up deprecated Gradle DSL syntax.

## v8.9.7
- **Plunge Matching: Advanced SIT Detection (Issue 196)**: Refined the "Plunge" state machine. `AppSensorManager.kt` and `LocationProcessor.kt` now fully propagate `sitVzTs` for forensic parity.
- **SIT Persistence Packet Loss Risk (Issue 194)**: Implemented a reliable, acknowledged event synchronization pipeline for SIT (chair) detection via `SyncManager.flushPendingLogs`.
- **Muzzle Window Hardening (Issue 191)**: Implemented device-specific hysteresis (500ms for A15, 200ms default) in the `SyncManager` handshake to suppress I/O-induced vibration alarms.
- **Database v38 (Issue 197)**: Added `sitVzTs` to `connection_history` and `pending_status_updates`. Implemented `MIGRATION_37_38`.

## v8.9.6
- **Room Migration Forensic Audit (Issue 195)**: Implemented Room migration (v36) to perform full table reconstruction (logs, connection_history, pending_status_updates). Aligned models with DB schema for Android 15 compatibility.
- **Xiaomi Indeterminate Handling (Issue 190)**: Implemented Muzzle/Override logic for Xiaomi "UNKNOWN" autostart status. Added `isXiaomiManualOverride` to bypass alarms when OS reflection fails.
- **Zombie UX Mitigation (Issue 193)**: Introduced `TELEMETRY_UI_STALE_THRESHOLD_MS` (10s) and began system-wide dimming of stale forensic fields.

## v8.9.5
- **Viewer Background Location (Issue 189)**: Injected `GpsManager` into `ViewerService` to enable Viewer-side location tracking. Implemented relative geofencing by calculating distance between Viewer and Tracker in the background.
- **Power Forensic Parity (Issue 192)**: Achieved absolute parity for battery current (`currentMa`) across models, database (v35), and ribbons. Updated `ViewerService` to pass `trackerCurrentMa` to `evaluateAlarms`.
- **evaluateAlarms Parameter Sync (Issue 192)**: Resolved a parameter mismatch in `ViewerService` to ensure all forensic fields are correctly passed to the engine.

## v8.9.4
- **Viewer Engine State Restoration (Issue 187)**: Updated `ViewerService` to load `maxAccuracy`, SIT metrics, and tracker state (spatial anchor) from the repository into `LocationProcessor` on startup, ensuring engine consistency across restarts.

## v8.9.3
- **Historical GPS Timestamp Preservation (Issue 188)**: Added `gpsTs` field to `PendingStatusEntity` (Database v34) and updated `SyncManager` to preserve original hardware fix timestamps during backfill.

## v8.9.2
- **Branding Finalization (Issue 183 / R935)**: Replaced adaptive app icon with high-resolution JD bitmap. Standardized icon resources to `jd_app_icon.xml` and `jd_bitmap.png` on brand-aligned green background. Removed redundant legacy icon XMLs.
- **ViewerService Listener Completion (Issue 185)**: Fully implemented `localProcessorListener` in `ViewerService.kt` for remote-to-local trail and log persistence.
- **GPS Stability Audit (Issue 181)**: De-noised forensic logs by consolidating reliability metrics into 10s intervals during 10Hz polling windows.
- **Muzzle Window Hardening (Issue 184)**: Optimized `SyncManager` with batch deletions and hardened `TrackerService` muzzle state to prevent race conditions during disk I/O.

## v8.9.1
- **Tag Baseline**: Baseline for major version increment.

## v8.8.35
- **Database Schema Cleanup (Issue 159)**: Formally removed legacy `ver` and `vid` columns from SQLite tables via Room Migration v33. Schema is now clean and aligned with the simplified forensic model.
- **Global Version Synchronization (Issue 156)**: Synchronized all documentation (SOT, Alarms, Sentinel) and build scripts to the v8.8.35 baseline.

## v8.8.34
- **Forensic Simplification**: Removed redundant version fields (`ver`, `vid`) from all active data models and telemetry pipelines. The system now injects the version string at emission points via `BuildConfig.VERSION_NAME`.
- **Build Stability (Issue 155)**: Resolved model synchronization conflicts and stale Room stubs following the removal of forensic fields.

## v8.8.32
- **Forensic Parity Fix (Issue 149)**: Achieved symbol parity for forensic markers (Magenta Squares for Jumps, Red Circles for Out-of-Range).
- **Viewer Jump Latching**: Updated `ViewerService.kt` to explicitly detect `remoteHandler.isTrackerVisualJump` and record it via `ServiceForensicUseCase`. This ensures Tracker-calculated jump points persist to the Viewer-side map.
- **Architectural Alignment (Issue 150)**: Replaced string literals with centralized `ALERT_ID_VISUAL_JUMP` constant in `EngineConstants.kt`. Updated all consumption points in `TrackerService.kt`, `ViewerService.kt`, and `MapComponents.kt` to ensure module boundary synchronization.

## v8.8.31
- **Identity Hardening**: Verified and synchronized versioning across the system to maintain the hardened production baseline.
- **GPS Revival Escalation (Issue 124)**: Implemented a robust 5-minute retry loop for GPS hardware revival in `TrackerService.kt`. Added forensic escalation to log a CRITICAL hardware lock after 3 failed attempts, ensuring the Viewer is notified of permanent stalls.
- **Monotonic UI Hardening (Issue 125)**: Migrated `ALARM_OVERLAY_THROTTLE_MS` logic to monotonic time (`elapsedRealtime`) in `MainViewModel.kt` and `BehaviorUseCase.kt` to prevent UI lockout flickering during system clock jumps.
- **Stress Test Purge**: Conducted a forensic sweep of the codebase and successfully removed all legacy stress-test modules and high-frequency simulation flags, ensuring a production-clean baseline.
- **LED Logic Decoupling**: Fixed status LEDs on the Tracker side to reflect local hardware health independently of Viewer connection status.

## v8.8.30
- **Identity Hardening (Issue 123)**: Updated versioning to synchronize with hardened branding and logic baseline.
- **Branding Update (R935)**: Migrated app icon to a text-free John Deere deer logo. Optimized `ic_jd_logo.xml` for adaptive icon compliance.
- **Alert Suppression (R872)**: Suppressed local alerts on Tracker devices. (v8.8.30).

## v8.8.29
- **Internal Identity Unification (A46)**: Standardized internal versioning in `Constants.kt`.
- **Version Bump**: Incremented `versionName` to "8.8.29" in `build.gradle` and synchronized all documentation.

## v8.8.28
- **Architectural Cleanup (A40/Issue 115)**: Decoupled `MainViewModel.kt` into domain-specific UseCases (Navigation, Settings, Telemetry, Behavior, Session, Alert, Map), reducing "God Object" complexity.
- **Internal Identity Unification (A46)**: Synchronized versioning and updated `versionName` in `build.gradle`.
- **Documentation Hardening**: Updated `REQUIREMENTS_SOT.md` with missing physics and behavioral constants. Synchronized all documentation headers to the v8.8.28 baseline.

## v8.8.27
- **Task Scheduling Standardization (Issue 117)**: Migrated `DAILY_CLEANUP_MINUTE` and `DAILY_ARCHIVE_MINUTE` to `EngineConstants.kt` for centralized task control.

## v8.8.26
- **Forensic Propagation (Issue 114)**: Hardened version propagation across the entire telemetry pipeline, ensuring forensic traceability in remote packets and local history.
- **Xiaomi Parity (Issue 116)**: Integrated autostart verification and 10Hz specialized GPS polling for Xiaomi devices to ensure background stability.

## v8.8.25
- **Timing & Persistence Hardening (Issue 112/113)**: Migrated `LogManager` and `MapComponents` to `TimeProvider` for monotonic timing. Standardized history batching thresholds (`5000ms`, `100 points`) in `EngineConstants.kt`.

## v8.8.24
- **SNR Scaling Standardization (Issue 111)**: Replaced hardcoded SNR scaling with `RIBBON_SNR_SCALE_DB` in `TrackerService.kt`.

## v8.8.23
- **Architectural Synchronization (A40)**: Standardized all physics, filtering, and network thresholds in `EngineConstants.kt` to align 100% with the System Source of Truth (`REQUIREMENTS_SOT.md`).
- **Internal Identity Unification (A46)**: Synchronized engine versioning across `Constants.kt` and `SyncManager.kt`. Updated `versionName` in `build.gradle` to "8.8.23".
- **Dashboard Recovery Parity (Issue 19 / R923)**: Implemented instantaneous UI recovery upon telemetry receipt. The dashboard and status card now utilize the maximum of GPS and telemetry timestamps for freshness logic.

## v8.8.22
- **Modular Engine Hardening (Chunk 1)**: Finalized the physical isolation of the `:core:engine` as a pure JVM library. Enforced 100% dependency purity.
- **Timing & Forensic Stability (Chunk 2)**: Standardized all alarm evaluation, SIT cooldowns, and gap detection on monotonic time (`TimeProvider.elapsedRealtime()`).
- **OEM Restriction Verification (Chunk 3)**: Integrated Xiaomi Autostart detection and enabled 10Hz specialized GPS polling for Xiaomi and Samsung S21 FE devices.
- **Muzzle Window & Forensic Audit (Chunk 4)**: Implemented a 500ms "Muzzle Window" during sync I/O to eliminate physical tamper false positives.
