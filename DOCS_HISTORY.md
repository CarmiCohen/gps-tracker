# Project History & Versioning

## v8.8.35
- **Database Schema Cleanup (Issue 159)**: Formally removed legacy `ver` and `vid` columns from SQLite tables via Room Migration v33. Schema is now clean and aligned with the simplified forensic model.
- **Global Version Synchronization (Issue 156)**: Synchronized all documentation (SOT, Alarms, Sentinel) and build scripts to the v8.8.35 baseline.

## v8.8.34
- **Forensic Simplification**: Removed redundant version fields (`ver`, `vid`) from all active data models and telemetry pipelines. The system now injects the version string at emission points via `BuildConfig.VERSION_NAME`.
- **Build Stability (Issue 155)**: Resolved model synchronization conflicts and stale Room stubs following the removal of forensic fields.

## v8.8.32
- **Forensic Parity Fix (Issue 149)**: Achieved symbol parity for forensic markers (Magenta Squares for Jumps, Red Circles for Out-of-Range).
- **Viewer Jump Latching**: Updated `ViewerService.kt` to explicitly detect `remoteHandler.isTrackerVisualJump` and record it via `ServiceForensicUseCase`. This ensures Tracker-calculated jump points persist to the Viewer's local map.
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
