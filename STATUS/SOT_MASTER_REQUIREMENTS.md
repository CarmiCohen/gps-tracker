# System Source of Truth (SoT) - Aug.05.128 (Map Recomposition Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **UI Recomposition Optimization (R736)**: (Updated Aug.05.128) Large UI state objects MUST be decomposed into primitive or stable parameters when passed to sub-composables. High-frequency state collection (Flows) MUST be hoisted to the highest possible level in the local tree to avoid redundant collectors. Map containers and complex visual layers MUST rely exclusively on primitive parameters to prevent UI thread stalls during telemetry bursts. (Issue #736, #737, #738, #739, #740)
*   **Startup Critical Path Hardening (R735)**: (Added Aug.05.122) High-cost initializations (e.g., memory-mapped files, synchronous I/O) MUST be deferred using `Provider<T>` and accessed on `Dispatchers.IO` to prevent main-thread Davey stalls during cold start. (Issue #735)
*   **Resource Lifecycle Hardening (R734)**: (Added Aug.05.119) All `Closeable` resources MUST be managed via `.use {}` or explicit `try-finally` blocks. Hardware callbacks in `callbackFlow` MUST implement `awaitClose`. (Issue #734)
*   **Android 15 (16KB Page Size) Compatibility (R732)**: (Updated Aug.05.118) All native libraries MUST be aligned for 16KB page size compatibility. (Issue #732)
*   **JNI Namespace Integrity (R733)**: Utilize the `jdMbrain` namespace. (Issue #733)
*   **Forensic Bloat Prevention (R731)**: Chunk-pruning for `isSpecial` logs. (Issue #731)
*   **Automated Database Integrity Validation (R729)**: Periodic `PRAGMA integrity_check`. (Issue #729)
*   **Storage-Aware Adaptive Pruning (R728)**: Fragmentation-aware pruning. (Issue #728)
*   **UI Ribbon Optimization (R726)**: (Updated Aug.05.113) Use `drawWithCache` and hardware acceleration for forensic visualization. Geometry calculations must be cached. (Issue #726)
*   **Forensic Spill-Buffer Authority (R669)**: Circular spill-buffer decoupling. (Issue #669)
*   **Zero-Churn Telemetry Authority (R668)**: Flyweight patterns for UI state management. (Issue #668)
*   **Main-Thread Purity (R526)**: No blocking operations during service or activity initialization. (Issue #526)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)
*   **Strict Forensic Reconstruction (R595)**: Analytical Ribbon "Strict Mode" for forensic auditing. (Issue #595)

### 3. Version Authority
*   **Current Release**: Aug.05.128.
*   **Source of Truth**: app/build.gradle versionName.
