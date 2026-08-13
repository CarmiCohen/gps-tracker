# System Source of Truth (SoT) - Aug.13.08 (WakeLock Log Saturation Resolved)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **WakeLock Log Throttling Authority (R156)**: (Added Aug.13.08) System-level WakeLock acquisition logging MUST be throttled to a minimum interval (default 60s) using `WAKELOCK_LOG_THROTTLE_MS`. This prevents high-frequency background pulses (e.g., from `AppSensorManager`) from saturating logcat while preserving periodic visibility for forensic audits. (Issue #156). **Status: Implemented.**
*   **Telemetry Flyweight Pooling Authority (R152)**: (Added Aug.13.06) The system MUST utilize **Flyweight Pooling** for all steady-state telemetry processing. The `ConnectionPoint` (App-level) and `EngineConnectionPoint` (Engine-level) MUST be mutable classes. Automatic UUID generation and expensive string concatenations MUST be eliminated from the 1Hz hot-path. The `HistoryManager` and `MainRepository` MUST reuse pre-allocated object instances during aggregation and persistence buffering to eliminate GC pressure on budget hardware (Samsung A15). (Issue #152). **Status: Implemented.**
*   **Staggered UI Hydration Authority (R153)**: (Added Aug.13.05) The application UI MUST initialize in granular stages using a `hydrationLevel` state. Complex components (NavHost, Screen Content, Maps) MUST be deferred across multiple frames during cold boot to prevent Main-thread Davey stalls. (Issue #153). **Status: Implemented.**
*   **Samsung A15 Detection Hardening (R405)**: (Added Aug.13.04) The system MUST reliably identify Samsung A15 devices by inspecting `Build.MODEL`, `Build.PRODUCT`, and `Build.DEVICE` strings. (Issue #150). **Status: Implemented.**
*   **Forensic Drainer Optimization (R146)**: (Added Aug.13.00) The system MUST optimize the telemetry drain loop to eliminate latency spikes and GC pressure. (Issue #146). **Status: Implemented.**
*   **Forensic Persistence Hardening (R151)**: (Added Aug.11.21) The system MUST decouple forensic trace persistence from the Main thread. (Issue #151). **Status: Implemented.**
*   **Forensic Pressure Authority (R669)**: (Added Aug.11.20) The system MUST monitor the `MappedByteBuffer` fill level. (Issue #145). **Status: Implemented.**
*   **Stress Recovery Authority (R141)**: (Added Aug.11.13) The system MUST ensure that all synthetic stress-test latches and thermal safety states are flushed immediately upon recovery. (Issue #141). **Status: Implemented & Verified.**
*   **Adaptive Polling Strategy (R406a)**: (Updated Aug.11.13) Hardware GPS polling rates MUST be dynamically adjusted based on motion, thermal status, and suspicious triggers. (Issue #057 / #406a). **Status: Implemented.**
*   **Forensic Integrity Verification (R143)**: (Added Aug.11.08) The system MUST verify that the Forensic Stress Test (R140) correctly triggers "Silent Failure" recordings. (Issue #143). **Status: Implemented & Verified.**
*   **Phone Setup Overlay Stabilization (R142)**: (Added Aug.11.06) The system MUST ensure that the `PhoneSetupOverlay` remains non-blocking and stable on budget hardware. (Issue #142). **Status: Implemented.**
*   **Automated Forensic Stress Testing (R140)**: (Added Aug.11.05) The system MUST provide an internal mechanism to artificially saturate device resources for forensic validation. (Issue #140). **Status: Implemented.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Updated Aug.11.08) Cross-domain correlation between location stability and hardware load. (Issue #133-Sentinel). **Status: Implemented & Verified.**

### 2. Temporal & Forensic Integrity
*   **Bayesian Uncertainty Authority (R460)**: (Updated Aug.11.16) The system MUST expand geofence thresholds during GPS gaps using time-drifted uncertainty (`acc`). (Issue #144). **Status: Implemented & Verified.**
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Phone Setup Clutter Reduction (R155)**: (Added Aug.13.07) The `PhoneSetupOverlay` MUST hide completion-dependent action buttons once the corresponding setup step is verified (`isCompleted == true`). This ensures a focused and simplified onboarding experience. (Issue #155). **Status: Implemented.**
*   **Header Layout Direction Locking (R148)**: (Added Aug.11.21) The `HeaderBar` MUST explicitly force `LayoutDirection.Ltr` for its internal layout. (Issue #148). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Viewer-local events MUST be prefixed with "**This device:**". (Issue #747)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) Synchronization across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.13.08.
*   **Source of Truth**: app/build.gradle versionName.
