# SOT Master Requirements & Hardening Status (Oct14.14)

## ? Architectural Master Rules (257 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct14.1 - SIMP-OCT12.18-S1).
*   **1.201 Unified Temporal Mocking (R-ID 719)**: Unified temporal mocks MUST be used across all modules via the `MockTimeProvider` test fixture to ensure consistency and eliminate redundant implementation. (Oct14.1 - SIMP-OCT14.2-S1).
*   **1.202 Unified Conflation Orchestration (R-ID 720)**: Conflation logic within the signaling pipeline MUST use the unified `dispatchConflated` orchestration pattern to ensure atomic state management, consistent burst pressure handling, and adaptive scheduling across all telemetry types. (Oct14.1 - SIMP-OCT13.20-S1).
*   **1.203 Unified Power State Mocking (R-ID 721)**: Mock power state providers MUST be unified via the `MockPowerStateProvider` test fixture to ensure consistent Doze mode simulation across engine and app test modules. (Oct14.1 - SIMP-OCT14.5-S1).
*   **1.204 Pure Event UI Architecture (R-ID 722)**: All UI components MUST utilize the centralized `UiEvent` bus for action propagation, ensuring zero manual lambda prop fatigue and complete decoupling from `MainViewModel` implementation details. (Oct14.1 - SIMP-OCT13.21-S1).
*   **1.205 Consolidated HUD State (R-ID 723)**: HUD-related UI states (Connectivity, Telemetry, Health) MUST be consolidated into a single atomic `HudState` StateFlow in `MainViewModel` to reduce subscription overhead and ensure synchronized updates across HUD components. (Oct14.1 - SIMP-OCT14.8-S1).

...

## ? Core Hardening Baseline
*   **SOT ID 728**: Physical Hardware Verification (A15/S21FE) - PASSED. Verified device deployment, sensor initialization, and foreground service stability on connected hardware. (Oct14.14 - ISSUE-OCT14.14-P1).
*   **SOT ID 727**: DataStore Atomic Mutation Stability - FAILED. Concurrent stress tests revealed race conditions in the mutation pipeline. (Oct14.13 - ISSUE-OCT14.0-F1).
*   **SOT ID 726**: S21FE Instrumental Test Verification (5m) - Verified 5-minute sustained telemetry cycle on Samsung S21FE hardware. Zero forensic data loss. (Oct14.1 - ISSUE-OCT14.12-I1).
*   **SOT ID 725**: A15 Instrumental Test Verification (30m) - Verified 30-minute sustained 1Hz telemetry cycle on Samsung A15 hardware. Zero forensic data loss. (Oct14.1 - ISSUE-OCT14.10-I1).
*   **SOT ID 724**: Full Unit Test Verification - FAILED (86/88). Identified regressions in DataStore mutations and Alarm branding. (Oct14.13 - ISSUE-OCT14.5-H1).
*   **SOT ID 723**: HUD State Consolidation - Consolidated individual HUD connectivity, telemetry, and health flows into a single atomic `hudState`, reducing UI subscription overhead and ensuring atomic component updates. (Oct14.1 - SIMP-OCT14.8-S1).
*   **SOT ID 722**: Pure Event UI Architecture - Migrated all specialized UI lambdas to a centralized event bus, eliminating direct ViewModel casting and standardizing Composable signatures to (UiStateProvider, (UiEvent) -> Unit). (Oct14.1 - SIMP-OCT13.21-S1).
*   **SOT ID 721**: MockPowerStateProvider Unification - Unified the `MockPowerStateProvider` implementation into a shared test-fixture module, eliminating manual mock duplication across instrumented and unit tests. (Oct14.1 - SIMP-OCT14.5-S1).
*   **SOT ID 720**: Unified Signaling Conflation - Unified the conflation orchestration in `SmartSignalingDispatcher` for Maps, Objects, and Logs, reducing redundancy and improving atomic state management under burst pressure. (Oct14.1 - SIMP-OCT13.20-S1).
*   **SOT ID 719**: MockTimeProvider Unification - Unified the `MockTimeProvider` implementation into a shared test-fixture module, eliminating duplication across 4 test suites. (Oct14.1 - SIMP-OCT14.2-S1).
*   **SOT ID 718**: S21FE Hardware Profile Verification - Validated stability on S21FE (`R5CRC14PG4F`) hardware, ensuring thermal and power-save mitigations are compatible with the device's specific jitter profile. (Oct14.1 - ISSUE-OCT14.4-H1).

...

---

## ? Verification Chapters
*   **Chapter 31.803 (Physical Verification)**: PASSED - Verified connected A15/S21FE hardware stability. (Oct14.14).
*   **Chapter 31.802 (DataStore Stress Audit)**: FAILED - Concurrent save collision detected (100 -> 2). (Oct14.13).
*   **Chapter 31.801 (S21FE Soak Verification)**: PASSED - Verified 5m sustained telemetry cycle on S21FE hardware. (Oct14.1).
*   **Chapter 31.800 (A15 Soak Verification)**: PASSED - Verified 30m sustained telemetry cycle on A15 hardware. (Oct14.1).
*   **Chapter 31.799 (Unit Test Verification)**: FAILED - 2 regressions found in app/engine modules. (Oct14.13).
...
