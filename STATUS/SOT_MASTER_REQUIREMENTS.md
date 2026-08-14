# System Source of Truth (SoT) - Aug.14.00 (Persistence & UI Stability Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Database Pruning Optimization (R167)**: (Added Aug.14.00) The system MUST utilize a minimum `DB_PRUNE_THRESHOLD` of 500. Pruning operations MUST be governed by a **1-minute temporal cooldown** (`PRUNE_COOLDOWN_MS`) to prevent SQLite lock contention during 100Hz forensic streams. (Issue #167). **Status: Implemented.**
*   **Settings UI Hardening (R166)**: (Added Aug.14.00) The `SettingsOverlay` MUST utilize **Staggered Hydration** (60ms offsets) to maintain main-thread responsiveness. Log flows for UI display MUST be throttled to a maximum of 2Hz (using `sample(500ms)`) to eliminate object churn under high-frequency ingress. (Issue #166). **Status: Implemented.**
*   **Forensic Log Path Hardening (R164)**: (Added Aug.13.13) The system MUST utilize deterministic composite IDs (`F-timestamp-idx`) for all forensic traces to eliminate UUID generation churn. Capture raw snapshots (`tempSnapshot`, `battSnapshot`, `chargingSnapshot`) in `LogEntry` to defer string formatting. `FORENSIC_SPILL_CAPACITY` = 10,000; `LOG_BUFFER_CAPACITY` = 2,000. (Issue #164). **Status: Implemented.**
*   **Telemetry Path Optimization (R163)**: (Added Aug.13.12) Eliminate object churn in the 1Hz telemetry path. `DashboardState` MUST utilize primitive types. UI components MUST perform string formatting using `remember` blocks. (Issue #163). **Status: Implemented.**
*   **Phone Setup ANR Remediation (R162)**: (Added Aug.13.11) 150ms hydration gate and 80ms sequential rendering offsets in `PhoneSetupOverlay`. Memoize static hardware strings. (Issue #162). **Status: Implemented.**
*   **SELinux Telemetry Remediation (R159)**: (Added Aug.13.10) Bypass `/proc/loadavg` and `/proc/stat` file access on Android 10 (SDK 29) and higher. (Issue #159). **Status: Implemented.**
*   **Performance Hardening Audit (R158)**: (Added Aug.13.09) End-to-end validation of performance optimizations (R152-R157). (Issue #158). **Status: Validated & Closed.**
*   **Violation Path Allocation Authority (R157)**: (Added Aug.13.09) Eliminate object churn in violation detection mapping. (Issue #157). **Status: Implemented.**
*   **WakeLock Log Throttling Authority (R156)**: (Added Aug.13.08) Acquisition logging throttled to 60s minimum. (Issue #156). **Status: Implemented.**
*   **Telemetry Flyweight Pooling Authority (R152)**: (Added Aug.13.06) Mandatory use of Flyweight Pooling for telemetry processing. (Issue #152). **Status: Implemented.**
*   **Staggered UI Hydration Authority (R153)**: (Added Aug.13.05) Stage-based UI initialization via `hydrationLevel`. (Issue #153). **Status: Implemented.**
*   **Samsung A15 Detection Hardening (R405)**: (Added Aug.13.04) Reliable A15 hardware identification via device/product strings. (Issue #150). **Status: Implemented.**
*   **Forensic Drainer Optimization (R146)**: (Added Aug.13.00) Optimized telemetry drain loop to support high-frequency persistence. (Issue #146). **Status: Implemented.**
*   **Forensic Persistence Hardening (R151)**: (Added Aug.11.21) Decouple forensic trace persistence from the Main thread. (Issue #151). **Status: Implemented.**
*   **Forensic Pressure Authority (R669)**: (Added Aug.11.20) Monitor `MappedByteBuffer` fill level and inhibit sampling during pressure. (Issue #145). **Status: Implemented.**
*   **Stress Recovery Authority (R141)**: (Added Aug.11.13) Immediate flush of thermal safety states upon test termination. (Issue #141). **Status: Implemented & Verified.**
*   **Adaptive Polling Strategy (R406a)**: (Updated Aug.11.13) Dynamic GPS polling rates based on motion state. (Issue #057). **Status: Implemented.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Updated Aug.11.08) Cross-domain correlation between location and hardware load. (Issue #133). **Status: Implemented & Verified.**

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
*   **Current Release**: Aug.14.00.
*   **Source of Truth**: app/build.gradle versionName.
