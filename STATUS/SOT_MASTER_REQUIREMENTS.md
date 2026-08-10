# System Source of Truth (SoT) - Aug.10.25 (Proto Health Parity)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Proto Health Parity (R130)**: (Added Aug.10.25) The `RealtimeStatus` Protobuf definition MUST be synchronized with the system health monitors to include `is_battery_low` and `is_battery_critical` flags. Telemetry Hot-Paths (Binary and JSON) MUST propagate these flags across all signaling roles to ensure forensic health awareness in the Viewer Dashboard (Issue #130-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Storage Pruning Sensitivity (R129)**: (Added Aug.10.24) Database maintenance MUST be battery-aware to prevent I/O-induced power spikes during critical battery states. Pruning operations in `LogRepository` and `MainRepository` MUST be deferred or throttled when `isBatteryLow` or `isBatteryCritical` is detected. The system MUST prioritize battery preservation over background log cleanup unless `isStorageCritical` is also active (Issue #129-Sentinel). **Status: Implemented & Verified.**
*   **Forensic Metadata Pressure Hardening (R128)**: (Added Aug.10.23) The `TelemetryAggregator` MUST prevent "Aggregation Storms" during high-frequency IMU capture. Aggregate ribbon scales (16M and above) MUST use stateful tick-gating. Averaging operations MUST be deferred to the write-path (Issue #128-Sentinel). **Status: Implemented & Verified.**
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
*   **Current Release**: Aug.10.25.
*   **Source of Truth**: app/build.gradle versionName.
