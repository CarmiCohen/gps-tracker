# SOT Master Requirements & Hardening Status (Oct27.5)

## 🏗️ Architectural Master Rules (354 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0 / Implemented Oct27.5 - ISSUE-OCT26.2-F1).
*   **1.273 Signaling Transparency (R-ID 512)**: The DAT badge MUST provide real-time numerical feedback for conflated packets through a numerical overlay and long-press diagnostic toast to ensure forensic visibility into pipeline pressure. (Oct27.5 - ISSUE-OCT27.0-F1).
*   **1.274 Forensic Persistence Hardening (R-ID 744)**: The system MUST enforce a UNIQUE constraint on `LogEntity.localId` and utilize optimized drain batching (2s interval, 2000 batch cap) to sustain 100Hz telemetry bursts without I/O wait (IOW) degradation. (Oct27.5 - ISSUE-OCT27.1-F1).
*   **1.275 Test Worker Factory (R-ID 745)**: Instrumented tests MUST utilize a custom `Application` that provides `HiltWorkerFactory` to `WorkManager` to prevent `NoSuchMethodException` when instantiating @HiltWorker classes with @AssistedInject. (Oct27.5 - ISSUE-OCT27.4-F1).
*   **1.276 Signaling Audit Synchronization (R-ID 746)**: Production readiness audits MUST verify signaling connectivity via `TestModeProvider` before burst injection to ensure deterministic conflation traces in high-pressure simulation. (Oct27.5 - ISSUE-OCT27.4-F2).
*   **1.277 HUD Component Scaling (R-ID 747)**: HUD status indicators MUST utilize a shared `MeasurableBadge` component to enforce consistent layout metrics and reduce redundant Compose emission overhead. (Oct27.5 - ISSUE-OCT27.5-F1).
*   **1.278 Forensic Recovery Integrity (R-ID 748)**: The system MUST reliably recover forensic telemetry from the off-heap `ForensicSpillBuffer` upon process restart and persist it to the primary database before advancing the buffer's read pointer to ensure zero-loss integrity. (Oct27.5 - ISSUE-OCT27.6-F1).
*   **1.279 Staggered Persistence (R-ID 749)**: The forensic spill-buffer MUST implement staggered `force()` calls (5s interval) during non-emergency states to mitigate SPI bus contention and prevent telemetry jitter on budget hardware (A15). Full synchronization MUST be enforced during recovery and high-pressure (90%) fill states. (Oct27.5 - ISSUE-OCT27.7-F1).

## 🛡️ Core Hardening Baseline
*   **SOT ID 853**: Forensic SPI Level Audit - PASSED. Verified zero-jitter persistence during 100Hz bursts by implementing staggered force() logic and IO-level auditing. (Oct27.5 - ISSUE-OCT27.7-F1).
*   **SOT ID 852**: Forensic Recovery Integrity - PASSED. Verified zero-loss telemetry persistence during simulated process restarts using ForensicRecoveryAuditTest. (Oct27.5 - ISSUE-OCT27.6-F1).
*   **SOT ID 851**: Unit-Based Badge Scaling - PASSED. Transitioned HUD status badges to a unified MeasurableBadge architecture. (Oct27.5 - ISSUE-OCT27.5-F1).
*   **SOT ID 850**: Test Infrastructure Hardening - PASSED. Integrated `HiltWorkerFactory` into the test application and enforced explicit `WorkManager` initialization. (Oct27.5 - ISSUE-OCT27.4-F1).
*   **SOT ID 849**: Signaling Audit Hardening - PASSED. Verified that `ProductionReadinessAuditTest` correctly synchronizes with `TestModeProvider` and `ConnectivitySuite` to ensure conflation traces. (Oct27.5 - ISSUE-OCT27.4-F2).
*   **SOT ID 848**: Forensic Convergence Audit - PASSED. Verified 100Hz burst sustainability and enforced persistence unique constraints to prevent data duplication. (Oct27.5 - ISSUE-OCT27.1-F1).

## ✅ Verification Chapters
*   **Chapter 4.23 (SPI Level Audit)**: PASSED - Verified that staggered force() logic reduces storage bus contention. IO-level audit correctly captures MappedByteBuffer latency in system health metrics. (Oct27.5).
*   **Chapter 4.22 (Forensic Recovery Audit)**: PASSED - Verified that traces written to the off-heap buffer survive process death and are correctly replayed by the LogRepository after a restart. (Oct27.5).
*   **Chapter 4.21 (UI Component Audit)**: PASSED - Verified that HUD badges correctly inherit shared MeasurableBadge logic, including numerical overlays and background states. (Oct27.5).
*   **Chapter 4.20 (Test Infrastructure Audit)**: PASSED - Verified that `PruningWorker` and other Hilt-injected workers instantiate successfully in instrumented tests using the custom test application. (Oct27.5).
*   **Chapter 4.19 (Forensic Convergence Audit)**: PASSED - Verified that LogRepository drain cycles sustain 100Hz bursts with IOW < 0.40. Confirmed zero duplication via unique localId enforcement. (Oct27.5).
