# SOT Master Requirements & Hardening Status (Oct20.22)

## 🏗️ Architectural Master Rules (302 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **1.5 Android 15 (16KB Page Size) Compatibility (R732)**: All native libraries MUST be aligned for 16KB page size compatibility using `-Wl,-z,max-page-size=16384` during linking. (v9.3.0).
*   **1.6 JNI Namespace Integrity (R733)**: All hardware-specific JNI bridge calls MUST strictly utilize the `jdMbrain` namespace to avoid framework collisions with legacy mbrainSDK identifiers. (v9.3.0).
*   **1.7 Forensic Bloat Prevention (R731)**: The `LogRepository` MUST implement a secondary safety tier that chunk-prunes forensic logs when the total count exceeds 5,000 entries. (v9.3.0).
*   **1.8 Automated Database Integrity Validation (R729)**: The `MaintenanceWorker` MUST execute a periodic database integrity audit using `PRAGMA integrity_check` every 24 hours. (v9.3.0).
*   **1.9 Storage-Aware Adaptive Pruning (R728)**: The `LogRepository` MUST implement granular, fragmentation-aware pruning based on `StorageStatsManager` thresholds. (v9.3.0).
*   **1.10 UI Ribbon Optimization (R726)**: Forensic Ribbons MUST utilize `drawWithCache` and hardware acceleration to maintain 60FPS during high-frequency telemetry updates. (v9.3.0).
*   **1.11 Forensic Delta-Encoding Hardening (R725)**: The `ForensicSpillBuffer` MUST implement Adaptive Base Resetting when drained to prevent cumulative precision loss. (v9.3.0).
*   **1.12 Package Name Shadow-Caching (R724)**: Utilize the `GpsApplication.PACKAGE_NAME` shadow-cache for all system identifier lookups to reduce IPC overhead. (v9.3.0).
*   **1.13 Non-Blocking Forensic Audit (R723)**: All `/proc` file operations MUST be executed on `Dispatchers.IO` to prevent UI thread stalls. (v9.3.0).
*   **1.14 Hardware State Refresh Throttling (R722)**: Polling of expensive system hardware/permission states is restricted to a minimum cooldown of 15s. (v9.3.0).
*   **1.15 Memory-Mapped Metadata Header Authority (R717)**: The `ForensicSpillBuffer` MUST utilize a 128-byte persistent metadata header at the start of the file for integrity validation. (v9.3.0).
*   **1.16 Critical Battery Sentinel Authority (R716)**: The system MUST monitor for abnormal battery discharge rates (>5%/10min) and correlate them with high sensor activity. (v9.3.0).
*   **1.17 Persistence Health Alerting Authority (R715)**: Trigger an `ALERT_ID_PERFORMANCE_SPIKE` if `forensicReliability` drops below 0.85 for 30s. (v9.3.0).
*   **1.18 Persistence Reliability Authority (R714)**: The `LogRepository` MUST track flush success rate via an EMA reliability factor for forensic auditing. (v9.3.0).
*   **1.19 Log Buffer Drain Throttling Authority (R713)**: Forensic persistence flushes MUST be load-aware, scaling dynamically based on `cpuLoad`. (v9.3.0).
*   **1.20 Adaptive Pruning Authority (R712)**: The `LogRepository` MUST implement fill-level aware proactive pruning using dynamic device-specific thresholds. (v9.3.0).
*   **1.21 Thermal-Aware Sampling Authority (R709)**: Forensic sampling MUST be throttled to 500ms whenever `isCoolingModeActive` is true. (v9.3.0).
*   **1.22 Forensic Convergence Monitoring Authority (R708)**: The `LogRepository` MUST monitor backfill convergence and trigger a warning if the buffer fails to clear. (v9.3.0).
*   **1.23 Forensic Adaptive Flushing Authority (R707)**: Forensic traces MUST be persisted to the database at 50% capacity or after a 5-second idle period. (v9.3.0).
*   **1.24 Binary Trace Delta-Encoding Authority (R706)**: Forensic traces MUST utilize header-based delta encoding for high-fidelity telemetry fields. (v9.3.0).
*   **1.25 Transactional Forensic Backfill Authority (R704)**: Forensic traces MUST be drained from the spill-buffer using a transactional peek/commit pattern. (v9.3.0).
*   **1.26 - 1.206**: (Archived in `STATUS/sot_master_archive.md`).
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects. (Oct18.8).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers MUST be executed outside of synchronized blocks. (Oct18.8).
*   **1.209 Primitive-Backed Telemetry Buffering (R-ID 727)**: Telemetry data MUST be stored in primitive-backed structures. (Oct18.8).
*   **1.210 Primitive-Backed History Buffering (R-ID 728)**: High-frequency forensic history data MUST be emitted as primitive-backed structures. (Oct18.8).
*   **1.226 Document Integrity Baseline (R-ID 744)**: Maintain a versioned baseline directory (`STATUS/BASELINE/`). (Oct20.0).
*   **1.227 Deterministic Test Flushing (R-ID 745)**: Repositories MUST provide synchronous `flush` mechanisms. (Oct20.2).
*   **1.228 Forensic Circular Overwrite (R-ID 746)**: The `ForensicSpillBuffer` MUST implement a circular overwrite policy during overflow to ensure the most recent telemetry is always preserved. (Oct20.21).
*   **1.229 - 1.302**: (Archived in `STATUS/sot_master_archive.md`).

## 🛡️ Core Hardening Baseline
*   **SOT ID 783**: Test Suite Version Regression - PASSED. Fixed `MainViewModelMappingTest` to align with the current versioning baseline. (Oct20.22 - ISSUE-OCT20.22-F1).
*   **SOT ID 782**: Forensic Circular Overwrite - PASSED. (Oct20.21).
*   **SOT ID 781**: Storage Test Race Conditions - PASSED. (Oct20.10).

---

## ✅ Verification Chapters
*   **Chapter 12.5 (Regression Audit)**: PASSED - Verified full test suite passability after versioning alignment. (Oct20.22).
*   **Chapter 12.4 (Overflow Resiliency)**: PASSED - Validated circular overwrite. (Oct20.21).
