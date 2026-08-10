# System Source of Truth (SoT) - Aug.09.22 (Drain Latency Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Forensic Drain Latency Hardening (R127)**: (Added Aug.09.22) The `ForensicSpillBuffer` MUST ensure zero-lock contention during high-pressure spills. The `synchronized` critical sections MUST be limited to sub-millisecond memory copies. All expensive operations (UTF-8 processing, CRC calculation, and object instantiation) MUST occur outside the lock. Drain cycles MUST NOT exceed a 5ms stall threshold under 100Hz sampling (Issue #127-Telemetry). **Status: Implemented & Verified.**
*   **Escalated GPS Revival (R124)**: (Updated Aug.07.07) If a GPS hardware stall is detected (R745), the system MUST trigger an escalated revival pulse every 120,000ms. If the fix is not recovered after 3 consecutive attempts, a `GPS_HARDWARE_LOCK` critical event MUST be emitted. (Issue #124-Revival).
*   **Forensic Parity Audit (R125)**: (Added Aug.08.21) The forensic spill-buffer V2 format MUST include the `gpsHardwareLock` flag within the bit-packed flags byte (bit 0x08) to maintain state parity across the telemetry pipeline, database persistence, and remote reporting (Issue #125). **Status: Implemented & Verified.**
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
*   **Current Release**: Aug.09.22.
*   **Source of Truth**: app/build.gradle versionName.
