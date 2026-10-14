# SOT Master Requirements & Hardening Status (Oct14.1)

## ✅ Architectural Master Rules (255 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct14.1 - SIMP-OCT12.18-S1).
*   **1.201 Unified Temporal Mocking (R-ID 719)**: Unified temporal mocks MUST be used across all modules via the `MockTimeProvider` test fixture to ensure consistency and eliminate redundant implementation. (Oct14.1 - SIMP-OCT14.2-S1).
*   **1.202 Unified Conflation Orchestration (R-ID 720)**: Conflation logic within the signaling pipeline MUST use the unified `dispatchConflated` orchestration pattern to ensure atomic state management, consistent burst pressure handling, and adaptive scheduling across all telemetry types. (Oct14.1 - SIMP-OCT13.20-S1).
*   **1.203 Unified Power State Mocking (R-ID 721)**: Mock power state providers MUST be unified via the `MockPowerStateProvider` test fixture to ensure consistent Doze mode simulation across engine and app test modules. (Oct14.1 - SIMP-OCT14.5-S1).

...

## ✅ Core Hardening Baseline
*   **SOT ID 721**: MockPowerStateProvider Unification - Unified the `MockPowerStateProvider` implementation into a shared test-fixture module, eliminating manual mock duplication across instrumented and unit tests. (Oct14.1 - SIMP-OCT14.5-S1).
*   **SOT ID 720**: Unified Signaling Conflation - Unified the conflation orchestration in `SmartSignalingDispatcher` for Maps, Objects, and Logs, reducing redundancy and improving atomic state management under burst pressure. (Oct14.1 - SIMP-OCT13.20-S1).
*   **SOT ID 719**: MockTimeProvider Unification - Unified the `MockTimeProvider` implementation into a shared test-fixture module, eliminating duplication across 4 test suites. (Oct14.1 - SIMP-OCT14.2-S1).
*   **SOT ID 718**: S21FE Hardware Profile Verification - Validated stability on S21FE (`R5CRC14PG4F`) hardware, ensuring thermal and power-save mitigations are compatible with the device's specific jitter profile. (Oct14.1 - ISSUE-OCT14.4-H1).

...

---

## 🔍 Verification Chapters
*   **Chapter 31.796 (Power Mock Unification)**: PASSED - Verified all tests (ProductionReadinessAuditTest, HardwareSuiteProfileTest) pass using the centralized MockPowerStateProvider. (Oct14.1).
*   **Chapter 31.795 (Conflation Unification)**: PASSED - Verified all telemetry types (Map, Bin, Log) conflate correctly under burst pressure using the unified dispatcher. (Oct14.1).
*   **Chapter 31.794 (Mock Unification)**: PASSED - Verified all tests pass after migrating to shared MockTimeProvider. (Oct14.1).
*   **Chapter 31.793 (S21FE Audit)**: PASSED - Verified all hardening and stress tests on S21FE hardware. (Oct14.1).
...
