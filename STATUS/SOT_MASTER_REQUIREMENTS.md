# System Source of Truth (SoT) - Aug.07.05 (Permission Detection Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Permission Detection Hardening (R745)**: (Updated Aug.07.05) The application MUST provide near-instant feedback for permission state changes during setup. The permission cache cooldown MUST NOT exceed 1000ms when setup or diagnostics screens are active, ensuring robust verification of 'Unrestricted' battery mode and Overlays on budget hardware (SM-A155F). (Issue #745)
*   **Forensic Write Compression (R743)**: (Added Aug.07.04) The forensic spill-buffer MUST use a structural compression layer (V2 format) to limit flash IO. Entry size is strictly capped at 96 bytes using bit-packing for flags/battery and optimized field alignment. Capacity is set to 3000 entries. (Issue #743)
*   **Startup Daveys Prevention (R744)**: (Added Aug.05.02) The main thread MUST NOT be blocked for more than 100ms during `MainActivity` initialization. All heavy IO, database checks, or legacy library initializations MUST be offloaded to `Dispatchers.IO` or deferred. (Issue #744)
*   **Proximity Forensic Sensitivity (R742)**: (Added Aug.07.01) Proximity indices MUST implement a debounced linear transition using Exponential Moving Average (EMA) at the sensor sampling level. Telemetry aggregation for proximity MUST use average-based accumulation across all time scales. (Issue #742)
*   **UI Recomposition Optimization (R736)**: (Updated Aug.07.00) Large UI state objects MUST be decomposed into primitive or stable parameters. High-frequency state collection MUST be hoisted. (Issue #736-741)
*   **Startup Critical Path Hardening (R735)**: (Added Aug.05.122) High-cost initializations MUST be deferred using `Provider<T>`. (Issue #735)
*   **Resource Lifecycle Hardening (R734)**: (Added Aug.05.119) All `Closeable` resources MUST be managed via `.use {}`. (Issue #734)
*   **Android 15 (16KB Page Size) Compatibility (R732)**: (Updated Aug.05.118) All native libraries MUST be aligned for 16KB page size. (Issue #732)
*   **JNI Namespace Integrity (R733)**: Utilize the `jdMbrain` namespace. (Issue #733)
*   **Forensic Bloat Prevention (R731)**: Chunk-pruning for `isSpecial` logs. (Issue #731)
*   **Automated Database Integrity Validation (R729)**: Periodic `PRAGMA integrity_check`. (Issue #729)
*   **Storage-Aware Adaptive Pruning (R728)**: Fragmentation-aware pruning. (Issue #728)
*   **UI Ribbon Optimization (R726)**: (Updated Aug.05.113) Use `drawWithCache` and hardware acceleration. (Issue #726)
*   **Forensic Spill-Buffer Authority (R669)**: Circular spill-buffer decoupling. (Issue #669)
*   **Zero-Churn Telemetry Authority (R668)**: Flyweight patterns for UI state management. (Issue #668)
*   **Main-Thread Purity (R526)**: No blocking operations during service or activity initialization. (Issue #526)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)
*   **Strict Forensic Reconstruction (R595)**: Analytical Ribbon "Strict Mode" for forensic auditing. (Issue #595)

### 3. Version Authority
*   **Current Release**: Aug.07.05.
*   **Source of Truth**: app/build.gradle versionName.
