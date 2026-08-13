# System Source of Truth (SoT) - Aug.13.12 (Telemetry Path Optimization Complete)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Telemetry Path Optimization (R163)**: (Added Aug.13.12) The system MUST eliminate object churn in the 1Hz telemetry path. `DashboardState` MUST utilize primitive types for all high-frequency data. UI components MUST perform string formatting using `remember` blocks to ensure that new string objects are only allocated when the underlying data changes, minimizing GC pressure on budget hardware. (Issue #163). **Status: Implemented.**
*   **Phone Setup ANR Remediation (R162)**: (Added Aug.13.11) The system MUST utilize a hardened hydration gate (minimum 150ms) in `PhoneSetupOverlay` to allow navigation transitions to settle before rendering heavy layout components. sequential rendering offsets for setup sections MUST be at least 80ms. Static hardware strings and permission descriptions MUST be memoized to prevent redundant resource lookups. Alert animations in `HeaderBar` MUST be suppressed while the setup overlay is active. (Issue #162). **Status: Implemented.**
*   **SELinux Telemetry Remediation (R159)**: (Added Aug.13.10) The system MUST bypass `/proc/loadavg` and `/proc/stat` file access on Android 10 (SDK 29) and higher. (Issue #159). **Status: Implemented.**
*   **Performance Hardening Audit (R158)**: (Added Aug.13.09) The system MUST undergo forensic validation after cumulative performance optimizations (R152-R157). (Issue #158). **Status: Validated & Closed.**
*   **Violation Path Allocation Authority (R157)**: (Added Aug.13.09) The system MUST eliminate object churn in the violation detection and mapping hot-paths. (Issue #157). **Status: Implemented.**
*   **WakeLock Log Throttling Authority (R156)**: (Added Aug.13.08) System-level WakeLock acquisition logging MUST be throttled to a minimum interval (default 60s). (Issue #156). **Status: Implemented.**
*   **Telemetry Flyweight Pooling Authority (R152)**: (Added Aug.13.06) The system MUST utilize **Flyweight Pooling** for all steady-state telemetry processing. (Issue #152). **Status: Implemented.**
*   **Staggered UI Hydration Authority (R153)**: (Added Aug.13.05) The application UI MUST initialize in granular stages using a `hydrationLevel` state. (Issue #153). **Status: Implemented.**
*   **Samsung A15 Detection Hardening (R405)**: (Added Aug.13.04) The system MUST reliably identify Samsung A15 devices. (Issue #150). **Status: Implemented.**
*   **Forensic Drainer Optimization (R146)**: (Added Aug.13.00) The system MUST optimize the telemetry drain loop. (Issue #146). **Status: Implemented.**
*   **Forensic Persistence Hardening (R151)**: (Added Aug.11.21) The system MUST decouple forensic trace persistence from the Main thread. (Issue #151). **Status: Implemented.**
*   **Forensic Pressure Authority (R669)**: (Added Aug.11.20) The system MUST monitor the `MappedByteBuffer` fill level. (Issue #145). **Status: Implemented.**
*   **Stress Recovery Authority (R141)**: (Added Aug.11.13) The system MUST ensure that all synthetic stress-test latches and thermal safety states are flushed immediately upon recovery. (Issue #141). **Status: Implemented & Verified.**
*   **Adaptive Polling Strategy (R406a)**: (Updated Aug.11.13) Hardware GPS polling rates MUST be dynamically adjusted. (Issue #057 / #406a). **Status: Implemented.**
*   **Forensic Integrity Verification (R143)**: (Added Aug.11.08) The system MUST verify that the Forensic Stress Test (R140) correctly triggers "Silent Failure" recordings. (Issue #143). **Status: Implemented & Verified.**
*   **Phone Setup Overlay Stabilization (R142)**: (Added Aug.11.06) The system MUST ensure that the `PhoneSetupOverlay` remains non-blocking and stable on budget hardware. (Issue #142). **Status: Implemented.**
*   **Automated Forensic Stress Testing (R140)**: (Added Aug.11.05) The system MUST provide an internal mechanism to artificially saturate device resources. (Issue #140). **Status: Implemented.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Updated Aug.11.08) Cross-domain correlation between location stability and hardware load. (Issue #133-Sentinel). **Status: Implemented & Verified.**

### 2. Temporal & Forensic Integrity
*   **Bayesian Uncertainty Authority (R460)**: (Updated Aug.11.16) The system MUST expand geofence thresholds during GPS gaps. (Issue #144). **Status: Implemented & Verified.**
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Phone Setup Clutter Reduction (R155)**: (Added Aug.13.07) The `PhoneSetupOverlay` MUST hide completion-dependent action buttons once the corresponding setup step is verified (`isCompleted == true`). (Issue #155). **Status: Implemented.**
*   **Header Layout Direction Locking (R148)**: (Added Aug.11.21) The `HeaderBar` MUST explicitly force `LayoutDirection.Ltr`. (Issue #148). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Viewer-local events MUST be prefixed with "**This device:**". (Issue #747)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) Synchronization across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.13.12.
*   **Source of Truth**: app/build.gradle versionName.
