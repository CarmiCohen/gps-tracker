# System Source of Truth (SoT) - Aug.11.21 (Forensic Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Forensic Persistence Hardening (R151)**: (Added Aug.11.21) The system MUST decouple forensic trace persistence from the Main thread. Forensic writes MUST be offloaded to background dispatchers, and the spill-buffer concurrency model MUST utilize granular locking and `MappedByteBuffer` duplication to ensure that background I/O stalls never block the UI or high-priority producers. (Issue #151). **Status: Implemented.**
*   **Forensic Pressure Authority (R669)**: (Added Aug.11.20) The system MUST monitor the `MappedByteBuffer` fill level. When forensic pressure exceeds the `HIGH_PRESSURE_THRESHOLD` (80%), the sampling interval MUST be proactively throttled to `FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS` (250ms) to prevent overflows and protect un-persisted data. (Issue #145). **Status: Implemented.**
*   **Stress Recovery Authority (R141)**: (Added Aug.11.13) The system MUST ensure that all synthetic stress-test latches and thermal safety states are flushed immediately upon recovery. Hardware GPS polling MUST return to its baseline interval (2000ms moving / 60000ms stationary) without "sticky" high-latency states. (Issue #141). **Status: Implemented & Verified.**
*   **Adaptive Polling Strategy (R406a)**: (Updated Aug.11.13) Hardware GPS polling rates MUST be dynamically adjusted based on motion, thermal status, and suspicious triggers. Transitions from slow to fast polling MUST implement a 5000ms "Adaptation Muzzle" to suppress hardware-level stabilization artifacts. (Issue #057 / #406a). **Status: Implemented.**
*   **Forensic Integrity Verification (R143)**: (Added Aug.11.08) The system MUST verify that the Forensic Stress Test (R140) correctly triggers "Silent Failure" recordings (R133). (Issue #143). **Status: Implemented & Verified.**
*   **Phone Setup Overlay Stabilization (R142)**: (Added Aug.11.06) The system MUST ensure that the `PhoneSetupOverlay` remains non-blocking and stable on budget hardware. (Issue #142). **Status: Implemented.**
*   **Automated Forensic Stress Testing (R140)**: (Added Aug.11.05) The system MUST provide an internal mechanism to artificially saturate device resources for forensic validation. (Issue #140). **Status: Implemented.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Updated Aug.11.08) Cross-domain correlation between location stability and hardware load. (Issue #133-Sentinel). **Status: Implemented & Verified.**

### 2. Temporal & Forensic Integrity
*   **Bayesian Uncertainty Authority (R460)**: (Updated Aug.11.16) The system MUST expand geofence thresholds during GPS gaps using time-drifted uncertainty (`acc`). Geofence hysteresis (Return to Safe Range) MUST be gated by the *current* drifted uncertainty, not the static accuracy of the last fix, to prevent false "Safe" clearances when position is unknown. (Issue #144). **Status: Implemented & Verified.**
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Header Layout Direction Locking (R148)**: (Added Aug.11.21) The `HeaderBar` MUST explicitly force `LayoutDirection.Ltr` for its internal layout. This ensures that control icons (Settings, Dashboard, BarChart) are always anchored to the start and navigation icons (System Issues, Log, Map) to the end, regardless of the system locale or parent layout direction. (Issue #148). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Viewer-local events MUST be prefixed with "**This device:**". (Issue #747)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) Synchronization across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.11.21.
*   **Source of Truth**: app/build.gradle versionName.
