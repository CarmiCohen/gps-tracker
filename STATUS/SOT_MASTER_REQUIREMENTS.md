# System Source of Truth (SoT) - Aug.07.06 (Revival Hardening Baseline)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Escalated GPS Revival (R124)**: (Added Aug.07.06) If a GPS hardware stall is detected (R745), the system MUST trigger an escalated revival pulse every 120,000ms. If the fix is not recovered after 3 consecutive attempts, a `GPS_HARDWARE_LOCK` critical event MUST be emitted to notify the user. (Issue #754)
*   **Startup I/O Stabilization (R104b)**: (Added Aug.07.06) To prevent "UI ERROR" and frame drops on budget hardware (Samsung A15), all non-critical background maintenance tasks MUST be deferred for at least 15,000ms after application startup. (Issue #120b)
*   **JdMbrain Namespace Integrity (R746)**: (Added Aug.07.06) The JNI bridge for vendor hardware stabilization MUST use the `jdMbrain` namespace exclusively. All legacy references to `libmbrainSDK` are prohibited. (Issue #746)
*   **Permission Detection Hardening (R745)**: (Updated Aug.07.05) The application MUST provide near-instant feedback for permission state changes. Cache cooldown MUST NOT exceed 1000ms during setup. (Issue #745)
*   **Forensic Write Compression (R743)**: (Added Aug.07.04) The forensic spill-buffer MUST use a structural compression layer (V2 format). Entry size is strictly capped at 96 bytes. (Issue #743)
*   **Startup Daveys Prevention (R744)**: (Added Aug.05.02) The main thread MUST NOT be blocked for more than 100ms during `MainActivity` initialization. (Issue #744)
*   **Proximity Forensic Sensitivity (R742)**: (Added Aug.07.01) Proximity indices MUST implement a debounced linear transition using Exponential Moving Average (EMA). (Issue #742)
*   **Main-Thread Purity (R526)**: No blocking operations during service or activity initialization. (Issue #526)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) All system event and alert text MUST follow the authoritative locality mapping. Viewer-local events MUST be prefixed with "**This device:**". Tracker-remote events MUST omit the "**Tracker:**" prefix. (Issue #747)
*   **Log Message Consistency (R748)**: (Added Aug.07.06) Hardcoded log messages MUST follow the R747 terminology. (Issue #748)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) The historical resolution record MUST be synchronized across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)
*   **Documentation Locality Synchronization (R750)**: (Added Aug.07.06) Formal documentation in `DOCS/` MUST be synchronized with the R747 authority. (Issue #750)

### 5. Version Authority
*   **Current Release**: Aug.07.06.
*   **Source of Truth**: app/build.gradle versionName.
