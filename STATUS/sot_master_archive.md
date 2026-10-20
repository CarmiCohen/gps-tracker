# SOT Master Archive (Full Forensic Archive)

This document contains all archived architectural rules and historical baselines from version v1.0 through Oct18.8.

## 🏗️ Archived Architectural Rules (1.1 - 1.206)

*   **1.1 - 1.37**: Foundational lifecycle rules for service residency and foreground state management (Archived from v8.8.0).
*   **1.38 Android 15 (16KB Page Size) Compatibility (R732)**: All native libraries MUST be aligned for 16KB page size compatibility using `-Wl,-z,max-page-size=16384` during linking.
*   **1.39 JNI Namespace Integrity (R733)**: All hardware-specific JNI bridge calls MUST strictly utilize the `jdMbrain` namespace. References to legacy `mbrainSDK` identifiers are forbidden.
*   **1.40 Forensic Bloat Prevention (R731)**: The `LogRepository` MUST implement a secondary safety tier that chunk-prunes forensic logs when the total count exceeds `LOG_LIMIT_STRICT` (5000 entries).
*   **1.41 Automated Database Integrity Validation (R729)**: The `MaintenanceWorker` MUST execute a periodic database integrity audit using `PRAGMA integrity_check` every 24 hours.
*   **1.42 Storage-Aware Adaptive Pruning (R728)**: The `LogRepository` MUST implement granular, fragmentation-aware pruning based on `StorageStatsManager`.
*   **1.43 UI Ribbon Optimization (R726)**: Forensic Ribbons MUST utilize `drawWithCache` and hardware acceleration.
*   **1.44 Forensic Delta-Encoding Hardening (R725)**: The `ForensicSpillBuffer` MUST implement Adaptive Base Resetting when drained.
*   **1.45 Package Name Shadow-Caching (R724)**: Utilize the `GpsApplication.PACKAGE_NAME` shadow-cache for all system identifier lookups.
*   **1.46 Non-Blocking Forensic Audit (R723)**: All `/proc` file operations MUST be executed on `Dispatchers.IO`.
*   **1.47 Hardware State Refresh Throttling (R722)**: Polling of expensive system hardware/permission states is restricted to a minimum cooldown of 15s.
*   **1.48 Memory-Mapped Metadata Header Authority (R717)**: The `ForensicSpillBuffer` MUST utilize a 128-byte persistent metadata header at the start of the file for integrity validation.
*   **1.49 Critical Battery Sentinel Authority (R716)**: The system MUST monitor for abnormal battery discharge rates and correlate them with high sensor activity.
*   **1.50 Persistence Health Alerting Authority (R715)**: Trigger an `ALERT_ID_PERFORMANCE_SPIKE` if `forensicReliability` drops below 0.85 for 30s.
*   **1.51 Persistence Reliability Authority (R714)**: The `LogRepository` MUST track flush success rate via an EMA reliability factor.
*   **1.52 Log Buffer Drain Throttling Authority (R713)**: Forensic persistence flushes MUST be load-aware, scaling dynamically based on `cpuLoad`.
*   **1.53 Adaptive Pruning Authority (R712)**: The `LogRepository` MUST implement fill-level aware proactive pruning using dynamic thresholds.
*   **1.54 Thermal-Aware Sampling Authority (R709)**: Forensic sampling MUST be throttled to 500ms whenever `isCoolingModeActive` is true.
*   **1.55 Forensic Convergence Monitoring Authority (R708)**: The `LogRepository` MUST monitor backfill convergence and trigger a warning if the buffer fails to clear.
*   **1.56 Forensic Adaptive Flushing Authority (R707)**: Forensic traces MUST be persisted to the database at 50% capacity or after a 5-second idle period.
*   **1.57 Binary Trace Delta-Encoding Authority (R706)**: Forensic traces MUST utilize header-based delta encoding for high-fidelity fields.
*   **1.58 Transactional Forensic Backfill Authority (R704)**: Forensic traces MUST be drained from the spill-buffer using a transactional peek/commit pattern.
*   **1.59 - 1.206**: Historical rules covering socket conflation, Bayesian uncertainty growth, and OSM pre-warming (Archived from v9.3.0).

---

## 📜 Milestone Snapshot: v8.9.90 (Historical)

*   **Engine Unification**: `MainAlarmLogic` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies.
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads high-frequency sensor event processing to a dedicated `HandlerThread`.
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements a short-circuit reactive reconnection trigger.
*   **Log Spillage Remediation (Issue #005)**: The system mandates a static User Agent and manually defined storage paths for third-party libraries.
*   **Time Integrity**: All alarm evaluations and hardware latches use monotonic time via `TimeProvider.elapsedRealtime()`.
*   **Foreground Service Transition (R967)**: The system maintains a 45-second "Recent UI Pulse" window to bridge Android 14+ transitions.
*   **Data Persistence Integrity (R968)**: All changes to Protobuf schemas must preserve binary compatibility.
