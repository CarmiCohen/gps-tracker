# System Source of Truth (SoT) - Aug.11.07 (Monitoring & Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Phone Setup Overlay Stabilization (R142)**: (Added Aug.11.06) The system MUST ensure that the `PhoneSetupOverlay` remains non-blocking and stable during entry and hydration on budget hardware (Samsung A15 class). The composition depth and resource-intensive permission checks MUST be staggered or offloaded to prevent 2000ms+ main-thread stalls and OS ANR dialogs. (Issue #142). **Status: Implemented (Staggered Incremental Hydration).**
*   **Automated Forensic Stress Testing (R140)**: (Added Aug.11.05) The system MUST provide an internal mechanism to artificially saturate device resources (CPU > 85%, I/O > 40%) for a minimum of 5 seconds. This is used to verify "Silent Failure" detection logic (R133) and ensure UI hydration gates (R137/R139) remain responsive under extreme hardware stress. (Issue #140). **Status: Implemented.**
*   **Compose Preview Coverage Authority (R136)**: (Added Aug.11.04) The system MUST maintain functional Compose Previews for all decomposed overlays (`SettingsOverlay`, `PhoneSetupOverlay`). Previews MUST support mock hydration states to verify both loading and rendered UI paths without requiring a full application build (Issue #136). **Status: Implemented.**
*   **TrackerScreen ANR Remediation (R139)**: (Added Aug.11.02) The system MUST eliminate the 3000ms+ Main-thread stall occurring during the transition from LandingScreen to TrackerScreen on budget hardware (Samsung A15 class). UI composition and state collection MUST be optimized to ensure frame integrity (Issue #139). **Status: Implemented (Deferred UI Hydration).**
*   **Settings Overlay ANR Remediation (R137)**: (Added Aug.10.32) The system MUST eliminate the 3000ms+ Main-thread stall occurring during the transition to the Settings overlay. Navigation to configuration screens MUST be non-blocking and decoupled from high-frequency telemetry processing (Issue #137). **Status: Implemented.**
*   **UI Transition Stabilization for Budget Hardware (R135)**: (Added Aug.10.30) The system MUST ensure that transitions to complex overlays (Settings, Phone Setup, Diagnostics) do not exceed a 500ms Main-thread stall on budget hardware (Samsung A15 class). Telemetry flow updates and UI recomposition MUST be staggered or throttled during active transitions to prevent OS ANR dialogs. (Issue #135). **Status: Implemented.**
*   **Forensic Pulse Frequency Hardening (R134)**: (Added Aug.10.29) The system MUST implement a high-frequency "Forensic Pulse" (10,000ms) for auditing resource-critical correlations (CPU, I/O, and Silent Failure detection). This pulse ensures that transient hardware stress causing "Silent Failures" is detected with minimal latency compared to the legacy 60s integrity heartbeat. Reactive flow stall detection (Storage, Power) MAY maintain the legacy 3-minute threshold logic (Issue #134-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Added Aug.10.28) The system MUST implement cross-domain correlation between location stability and hardware resource stress. A `SILENT_FAILURE` MUST be flagged if a GPS stall is detected while `cpuLoad` >= 85%, `ioWait` >= 40%, or `maxIoLatency` >= 800ms, provided no physical tamper events are active (Issue #133-Sentinel). **Status: Implemented & Verified.**
*   **Forensic UI Dashboard Refinement (R132)**: (Added Aug.10.27) The Forensic Dashboard (Tracker & Viewer) MUST integrate `cpuLoad`, `ioWait`, and `maxIoLatency` trends into the `ForensicSection` visualization (Issue #132-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Performance Audit (R131)**: (Added Aug.10.26) The system MUST track and audit peak I/O latency across all critical subsystems. Budget hardware (Samsung A15) MUST trigger a forensic `PERFORMANCE_SPIKE` alert if disk I/O latency exceeds 1,000ms (Issue #131-Sentinel). **Status: Implemented & Verified.**
*   **Proto Health Parity (R130)**: (Added Aug.10.25) The `RealtimeStatus` Protobuf definition MUST be synchronized with the system health monitors to include `is_battery_low` and `is_battery_critical` flags (Issue #130-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Storage Pruning Sensitivity (R129)**: (Added Aug.10.24) Database maintenance MUST be battery-aware. Pruning operations in `LogRepository` and `MainRepository` MUST be deferred or throttled when `isBatteryLow` or `isBatteryCritical` is detected (Issue #129-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Metadata Pressure Hardening (R128)**: (Added Aug.10.23) The `TelemetryAggregator` MUST prevent "Aggregation Storms" during high-frequency IMU capture. Aggregate ribbon scales (16M and above) MUST use stateful tick-gating (Issue #128-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Drain Latency Hardening (R127)**: (Added Aug.09.22) The `ForensicSpillBuffer` MUST ensure zero-lock contention during high-pressure spills. Drain cycles MUST NOT exceed a 5ms stall threshold under 100Hz sampling (Issue #127-Telemetry). **Status: Implemented & Verified.**
*   **Escalated GPS Revival (R124)**: (Updated Aug.07.07) If a GPS hardware stall is detected, the system MUST trigger an escalated revival pulse every 120,000ms. (Issue #124-Revival).
*   **Forensic Parity Audit (R125)**: (Added Aug.08.21) The forensic spill-buffer V2 format MUST include the `gpsHardwareLock` flag within the bit-packed flags byte (bit 0x08) (Issue #125). **Status: Implemented & Verified.**
*   **Forensic Payload Integrity (R126)**: (Added Aug.08.21) Diagnostic messages written to the forensic spill-buffer MUST be safely truncated to the 56-byte payload limit without corrupting multi-byte UTF-8 sequences. (Issue #126-Telemetry). **Status: Implemented & Verified.**
*   **Forensic Write Compression (R743)**: (Added Aug.07.04) The forensic spill-buffer MUST use a structural compression layer (V2 format). Entry size is strictly capped at 96 bytes. (Issue #743)
*   **Startup I/O Stabilization (R104b)**: (Added Aug.07.06) Defer non-critical maintenance for 15,000ms after startup. (Issue #120b)
*   **JdMbrain Namespace Integrity (R746)**: (Added Aug.07.06) JNI bridge MUST use `jdMbrain` namespace. (Issue #746)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Viewer-local events MUST be prefixed with "**This device:**". Tracker-remote events MUST omit the "**Tracker:**" prefix. (Issue #747)
*   **Log Message Consistency (R748)**: (Added Aug.07.06) Hardcoded log messages MUST follow the R747 terminology. (Issue #748)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) The historical resolution record MUST be synchronized across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.11.07.
*   **Source of Truth**: app/build.gradle versionName.
