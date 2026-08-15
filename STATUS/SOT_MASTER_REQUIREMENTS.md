# System Source of Truth (SoT) - Aug.14.04 (Viewer Mirror Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Multi-Stream Processor Contention (R173)**: (Added Aug.14.04) The `ViewerService` MUST maintain two distinct `LocationProcessor` instances: one for filtering the viewer's own location ("Self") and one for filtering the remote tracker's telemetry ("Remote"). Interleaving these streams in a single processor is STRICTLY PROHIBITED to prevent filter state corruption. (Issue #173). **Status: Implemented.**
*   **Viewer Forensic Parity (R172)**: (Added Aug.14.04) The system MUST maintain full forensic parity in the viewer-side mirrored state. `LocationProcessor` MUST correctly restore forensic attributes (lastSitTs, sitVz, sitDz, sitBaro, sitTilt, sitShock) from remote telemetry after service restarts or multi-viewer handovers to ensure "Zero-Lag" UI transitions. (Issue #172). **Status: Implemented.**
*   **Forensic Jitter Protection (R171)**: (Added Aug.14.03) The system MUST maintain temporal integrity during multi-viewer forensic streams. Telemetry processing MUST allow a 2s jitter window (`MONOTONIC_JITTER_TOLERANCE_MS`) to prevent data loss while ensuring that aggregators and UI history buffers maintain strict monotonicity via sorted merging and deduplication. (Issue #171). **Status: Implemented.**
*   **Forensic Replay Sync (R170)**: (Added Aug.14.02) The system MUST support coordinate-aware scrubbing in telemetry ribbons. Map cursor positioning MUST utilize O(log N) binary search on historical trail data to ensure frame-perfect alignment with sensor trends (e.g., `vibeIdx` spikes) during 100Hz playback simulation. (Issue #170). **Status: Implemented.**
*   **Geofence-Aware Polling (R169)**: (Added Aug.14.01) The system MUST maintain a safe polling interval (5s for standard, 2s for budget hardware) when a geofence is active and the device is moving, regardless of screen state. (Issue #169). **Status: Implemented.**
*   **Forensic Stress Integrity (R165)**: (Added Aug.14.00) The system MUST maintain stability during a 5-minute sustained 100Hz load. Forensic drainage MUST NOT induce Main-thread Davey stalls or SQLite write contention. (Issue #165). **Status: Implemented & Verified.**
*   **Database Pruning Optimization (R167)**: (Added Aug.14.00) The system MUST utilize a minimum `DB_PRUNE_THRESHOLD` of 500. Pruning operations MUST be governed by a **1-minute temporal cooldown** (`PRUNE_COOLDOWN_MS`). (Issue #167). **Status: Implemented.**
*   **Settings UI Hardening (R166)**: (Added Aug.14.00) The `SettingsOverlay` MUST utilize **Staggered Hydration** (60ms offsets). Log flows for UI display MUST be throttled to 2Hz (`sample(500ms)`). (Issue #166). **Status: Implemented.**
*   **Forensic Log Path Hardening (R164)**: (Added Aug.13.13) The system MUST utilize deterministic composite IDs (`F-timestamp-idx`). Capture raw snapshots in `LogEntry` to defer string formatting. (Issue #164). **Status: Implemented.**
*   **Telemetry Path Optimization (R163)**: (Added Aug.13.12) Eliminate object churn in the 1Hz telemetry path. `DashboardState` MUST utilize primitive types. (Issue #163). **Status: Implemented.**
*   **Phone Setup ANR Remediation (R162)**: (Added Aug.13.11) 150ms hydration gate and 80ms sequential rendering offsets in `PhoneSetupOverlay`. (Issue #162). **Status: Implemented.**
*   **SELinux Telemetry Remediation (R159)**: (Added Aug.13.10) Bypass `/proc/loadavg` and `/proc/stat` file access on Android 10+. (Issue #159). **Status: Implemented.**
*   **Performance Hardening Audit (R158)**: (Added Aug.13.09) End-to-end validation of performance optimizations (R152-R157). (Issue #158). **Status: Validated & Closed.**
*   **Violation Path Allocation Authority (R157)**: (Added Aug.13.09) Eliminate object churn in violation detection mapping. (Issue #157). **Status: Implemented.**
*   **WakeLock Log Throttling Authority (R156)**: (Added Aug.13.08) Acquisition logging throttled to 60s minimum. (Issue #156). **Status: Implemented.**
*   **Telemetry Flyweight Pooling Authority (R152)**: (Added Aug.13.06) Mandatory use of Flyweight Pooling for telemetry processing. (Issue #152). **Status: Implemented.**
*   **Staggered UI Hydration Authority (R153)**: (Added Aug.13.05) Stage-based UI initialization via `hydrationLevel`. (Issue #153). **Status: Implemented.**
*   **Samsung A15 Detection Hardening (R405)**: (Added Aug.13.04) Reliable A15 hardware identification via device/product strings. (Issue #150). **Status: Implemented.**
*   **Forensic Drainer Optimization (R146)**: (Added Aug.13.00) Optimized telemetry drain loop to support high-frequency persistence. (Issue #146). **Status: Implemented.**
*   **Samsung A15 Adaptation Authority (R141b)**: (Added Aug.11.23) Integrated Adaptation Muzzle for budget GPS stability. (Issue #141). **Status: Implemented.**
*   **Forensic Persistence Hardening (R151)**: (Added Aug.11.21) Decouple forensic trace persistence from the Main thread. (Issue #151). **Status: Implemented.**
*   **Forensic Pressure Authority (R669)**: (Added Aug.11.20) Monitor `MappedByteBuffer` fill level and inhibit sampling during pressure. (Issue #145). **Status: Implemented.**
*   **Stress Recovery Authority (R141)**: (Added Aug.11.13) Immediate flush of thermal safety states upon test termination. (Issue #141). **Status: Implemented & Verified.**
*   **Adaptive Polling Strategy (R406a)**: (Updated Aug.11.13) Dynamic GPS polling rates based on motion state. (Issue #057). **Status: Implemented.**

### 2. Temporal & Forensic Integrity
*   **Bayesian Uncertainty Authority (R460)**: (Updated Aug.11.16) Expand geofence thresholds during GPS gaps. (Issue #144). **Status: Implemented & Verified.**
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Phone Setup Clutter Reduction (R155)**: (Added Aug.13.07) Hide completion-dependent buttons once verified. (Issue #155). **Status: Implemented.**
*   **Header Layout Direction Locking (R148)**: (Added Aug.11.21) Explicitly force `LayoutDirection.Ltr`. (Issue #148). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Local event prefixing with "**This device:**". (Issue #747)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) Mandatory documentation synchronization after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) Synchronization across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.14.04.
*   **Source of Truth**: app/build.gradle versionName.
