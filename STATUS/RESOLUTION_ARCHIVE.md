# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 570**

## 10. Forensic Proto Alignment (Aug.10.25)
*   **Issue #130-Sentinel: Proto Health Parity**.
    *   **Resolution**: Synchronized the `RealtimeStatus` Protobuf definition and `TrackerStatus.writeTo` mapping to include `isBatteryLow` and `isBatteryCritical` flags. Hardened the entire telemetry pipeline (Binary, JSON, Persistence, and UI) to ensure forensic health parity between the Tracker and Viewer. Implemented Migration 66 to support battery-aware history and pending updates (R130).

## 9. Forensic Storage Hardening (Aug.10.24)
*   **Issue #129-Sentinel: Forensic Storage Pruning Sensitivity**.
    *   **Resolution**: Hardened database maintenance against battery-induced I/O spikes. Refactored `LogRepository.kt` and `MainRepository.kt` to be battery-aware, deferring or throttling pruning operations when `isBatteryLow` or `isBatteryCritical` is detected. Integrated battery pressure flags into `SystemHealthState` and `IntegrityMonitor` to ensure adaptive yielding during SQLite WAL checkpointing (R129).

## 8. Forensic Telemetry & Metadata Hardening (Aug.10.23)
*   **Issue #128-Sentinel: Forensic Metadata Pressure Hardening**.
    *   **Resolution**: Hardened `TelemetryAggregator.kt` against high-frequency ribbon collisions. Implemented a stateful `lastEmittedTick` gate to prevent "Aggregation Storms" during 100Hz IMU spikes. Optimized O(N) traversal by caching `RibbonScale` entries and streamlining aggregation arithmetic (proxIdx averaging deferred to write-path). Verified 10ms processing threshold on A15-equivalent hardware (R128).

## 7. Forensic Telemetry Hardening (Aug.09.22)
*   **Issue #127-Telemetry: Forensic Drain Latency Hardening**.
    *   **Resolution**: Optimized `ForensicSpillBuffer.kt` for zero-lock contention. Refactored `peek()` and `writeTrace()` to hold the `synchronized` lock only for sub-millisecond memory copies. Moved UTF-8 processing, CRC calculations, and object reconstruction outside critical sections. Integrated `LatencyMonitor` performance audits (5ms threshold) to ensure stability under 100Hz sampling (R127).

*   **Issue #126-Telemetry: Forensic Payload Overflow Audit**.
    *   **Resolution**: Implemented safe UTF-8 truncation in `ForensicSpillBuffer.kt` to prevent diagnostic message corruption at the 56-byte boundary. Backtracks to the start of multi-byte sequences (R126).

*   **Issue #125-Telemetry: Forensic Data Compression Parity Audit**.
    *   **Resolution**: Remedied forensic parity gap by integrating `gpsHardwareLock` into the V2 binary format flags (0x08). Synchronized `LogEntry`, `LogEntity` (Migration 65), and `EngineConnectionPoint` (R125).

## 6. Functional Hardening & Revival (Aug.07.07)
*   **Issue #124-Revival: GPS Hardware Revival Functional Hardening**.
    *   **Resolution**: Hardened the 120s GPS revival loop in `GpsManager.kt`. Standardized logs with "this device" locality authority (R747). Integrated `revivalEvents` into `IntegrityMonitor.kt` to ensure `GPS_HARDWARE_LOCK` is surfaced as a system violation and health state flag. Synchronized `SystemHealthState`, `LocationUpdate`, and `TelemetryUseCase` to propagate the lock status across signaling roles (R124).

## 5. UI/UX & Forensic Hardening (Aug.07.06)
*   **Issue #753: Restoration of Resolution Archive Integrity**.
    *   **Resolution**: Restored truncated historical records (Issues #639, #638, #634) in the archive to satisfy R752 documentation integrity requirements.
*   **Issue #752: Status Tracking Synchronization**.
    *   **Resolution**: Synchronized `issues.md` and `RESOLUTION_ARCHIVE.md` baselines. Formalized Status Tracking Integrity requirement (R752).
*   **Issue #751: Final R747 Terminology Alignment**.
    *   **Resolution**: Performed a final sweep of `event-tables.md` and `EVENTS_DOC.md` to remove all remaining "Tracker:" prefixes and ensure "Device" is used consistently for remote status reporting (R751).
*   **Issue #750: Documentation Locality Synchronization**.
    *   **Resolution**: Synchronized formal documentation (specifically `ALARM_AND_SIREN_MECHANISM.md`, `SETTINGS_PAGE_DETAIL.md`, `EVENTS_AND_LOGGING_MECHANISM.md`, and `GUIDE_AND_SETTINGS.md`) with the R747 authority (R750).
*   **Issue #749: Documentation & Shard Synchronization**.
    *   **Resolution**: Synchronized the historical record to reflect the 558 resolution baseline. Formalized documentation integrity rules in SOT (R749).
*   **Issue #748: Log Message Prefix Cleanup**.
    *   **Resolution**: Hardened log message consistency by removing legacy "Tracker:" prefixes and standardizing "Device" terminology in `IntegrityMonitor.kt` and `ViewerService.kt`. Ensures full compliance with R747 locality rules (R748).
*   **Issue #747: Event & Alert Text Unification**.
    *   **Resolution**: Synchronized all system event and alert text with the authoritative mapping (R747). Viewer-local events now use the "This device:" prefix, and tracker-remote events use "Device" in subtitles to ensure professional consistency and locality clarity. Updated `MainAlarmLogic.kt` and `EngineConstants.kt`.
*   **Issue #746: Missing libmbrainSDK**.
    *   **Resolution**: Fully transitioned the JNI bridge to the `jdMbrain` namespace to eliminate legacy log noise (`Can't load libmbrainSDK`) and avoid collisions with Samsung system libraries. Renamed `MbrainHardwareManager` to `JdMbrainHardwareManager` and updated native source to `jdmbrain-jni.cpp`. Decoupled all service logic from legacy "mbrain" identifiers (R746).
*   **Issue #745: Missing Critical Background Permissions**.
    *   **Resolution**: Hardened permission detection responsiveness by reducing `FORCED_REFRESH_COOLDOWN_MS` from 15s to 1s in `SystemStatusProviderImpl`. This ensures the Setup UI "Refresh" button provides immediate feedback (R745).
*   **Issue #744: Startup Daveys Prevention**.
    *   **Resolution**: Offloaded heavy IO and legacy initializations to background dispatchers in `MainActivity` to prevent main-thread stalls (R744).
*   **Issue #743: Forensic Spill-Buffer Write Compression**.
    *   **Resolution**: Implemented structural compression (V2 format) for the circular spill-buffer. Caps entry size at 96 bytes and increases capacity to 3000 entries (R743).

## 4. Stability & Budget Baseline (July.30.35)
*   **Issue #640: Tracker Mode ANR (Regression)**.
    *   **Root Cause**: Main-thread contention on budget hardware (Samsung A15) caused by high-frequency UI pulses triggering $O(N)$ map overlay reconstructions (trails and accuracy circles).
    *   **Resolution**: Implemented aggressive 1000ms throttling for heavy overlay updates and decoupled tracker/viewer trail processing in `MapOverlayManager.kt`. Enforced 1000ms gating and 2.0m threshold for accuracy circle recalculations.
    - **Impact**: Eliminated system-level unresponsiveness post-relay connection on baseline devices.
*   **Issue #637: Log Spam: getPackageName()**.
    *   **Resolution**: Implemented 2000ms short-term status cache for `isLocalOnline()`.
*   **Issue #639: Tracker Mode ANR on Startup**.
    *   **Resolution**: Implemented granular change detection and polygon caching in `MapOverlayManager.kt`.
*   **Issue #638: Incorrect Permission Defaults**.
    *   **Resolution**: Corrected `PermissionState` data class in `MainUiState.kt`.
*   **Issue #634: ForegroundServiceStartNotAllowedException Crash**.
    *   **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`.

## 1. Kernel & OS Performance Hardening (July.25.13)
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**. 
    *   Finalized verification stack for Zero-Churn performance. 
    *   Integrated `LatencyMonitor` into `dashboardState` computation in `MainViewModel`. 
    *   Added forensic jitter logging for A15 hardware to detect ART compaction stalls on kernels lacking `userfaultfd` support.
*   **Issue #555: Forensic Snapshot Integrity**.
    *   Audited flyweight lifecycles in the telemetry pipeline.
    *   Ensured immutable boundaries between background aggregation and reactive UI consumers.

## 2. Network Lifecycle Hardening (July.25.12)
*   **Issue #545: Production Logging Leak (`StackLog`)**. 
    *   Implemented idempotent lifecycle management in `ConnectivitySuite`. 
    *   Added `isStarted` state guarding to prevent redundant platform-level network callback registrations.

## 3. Generic Latency Monitoring (July.25.11)
*   **Issue #590: Latency Monitoring Framework**. 
    *   Implemented unified `LatencyMonitor` in `:core:engine`. 
    *   Integrated monitoring into JNI (Mbrain), DB (Repository), and Log paths.

## 0. Legacy & Base Logic
[Historical records are preserved in RESOLUTION_SHARD_001.md through RESOLUTION_SHARD_020.md]
