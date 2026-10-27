# SOT Master Requirements & Hardening Status (Oct27.4)

## 🏗️ Architectural Master Rules (351 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0 / Implemented Oct26.2 - ISSUE-OCT26.2-F1).
*   **(Rules 1.5 - 1.272 archived or listed in previous versions).**
*   **1.273 Signaling Transparency (R-ID 512)**: The DAT badge MUST provide real-time numerical feedback for conflated packets through a numerical overlay and long-press diagnostic toast to ensure forensic visibility into pipeline pressure. (Oct27.0 - ISSUE-OCT27.0-F1).
*   **1.274 Forensic Persistence Hardening (R-ID 744)**: The system MUST enforce a UNIQUE constraint on `LogEntity.localId` and utilize optimized drain batching (2s interval, 2000 batch cap) to sustain 100Hz telemetry bursts without I/O wait (IOW) degradation. (Oct27.1 - ISSUE-OCT27.1-F1).
*   **1.275 Test Worker Factory (R-ID 745)**: Instrumented tests MUST utilize a custom `Application` that provides `HiltWorkerFactory` to `WorkManager` to prevent `NoSuchMethodException` when instantiating @HiltWorker classes with @AssistedInject. (Oct27.4 - ISSUE-OCT27.4-F1).
*   **1.276 Signaling Audit Synchronization (R-ID 746)**: Production readiness audits MUST verify signaling connectivity via `TestModeProvider` before burst injection to ensure deterministic conflation traces in high-pressure simulation. (Oct27.4 - ISSUE-OCT27.4-F2).

## 🛡️ Core Hardening Baseline
*   **SOT ID 850**: Test Infrastructure Hardening - PASSED. Integrated `HiltWorkerFactory` into the test application and enforced explicit `WorkManager` initialization. (Oct27.4 - ISSUE-OCT27.4-F1).
*   **SOT ID 849**: Signaling Audit Hardening - PASSED. Verified that `ProductionReadinessAuditTest` correctly synchronizes with `TestModeProvider` and `ConnectivitySuite` to ensure conflation traces. (Oct27.4 - ISSUE-OCT27.4-F2).
*   **SOT ID 848**: Forensic Convergence Audit - PASSED. Verified 100Hz burst sustainability and enforced persistence unique constraints to prevent data duplication. (Oct27.1 - ISSUE-OCT27.1-F1).

## ✅ Verification Chapters
*   **Chapter 4.20 (Test Infrastructure Audit)**: PASSED - Verified that `PruningWorker` and other Hilt-injected workers instantiate successfully in instrumented tests using the custom test application. (Oct27.4).
*   **Chapter 4.19 (Forensic Convergence Audit)**: PASSED - Verified that LogRepository drain cycles sustain 100Hz bursts with IOW < 0.40. Confirmed zero duplication via unique localId enforcement. (Oct27.1).
