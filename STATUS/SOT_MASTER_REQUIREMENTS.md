# SOT Master Requirements & Hardening Status (Oct14.2)

## 🏗️ Architectural Master Rules (260 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct14.1 - SIMP-OCT12.18-S1).
*   **1.201 Unified Temporal Mocking (R-ID 719)**: Unified temporal mocks MUST be used across all modules via the `MockTimeProvider` test fixture to ensure consistency and eliminate redundant implementation. (Oct14.1 - SIMP-OCT14.2-S1).
*   **1.202 Unified Conflation Orchestration (R-ID 720)**: Conflation logic within the signaling pipeline MUST use the unified `dispatchConflated` orchestration pattern to ensure atomic state management, consistent burst pressure handling, and adaptive scheduling across all telemetry types. (Oct14.1 - SIMP-OCT13.20-S1).
*   **1.203 Unified Power State Mocking (R-ID 721)**: Mock power state providers MUST be unified via the `MockPowerStateProvider` test fixture to ensure consistent Doze mode simulation across engine and app test modules. (Oct14.1 - SIMP-OCT14.5-S1).
*   **1.204 Pure Event UI Architecture (R-ID 722)**: All UI components MUST utilize the centralized `UiEvent` bus for action propagation, ensuring zero manual lambda prop fatigue and complete decoupling from `MainViewModel` implementation details. (Oct14.1 - SIMP-OCT13.21-S1).
*   **1.205 Consolidated HUD State (R-ID 723)**: HUD-related UI states (Connectivity, Telemetry, Health) MUST be consolidated into a single atomic `HudState` StateFlow in `MainViewModel` to reduce subscription overhead and ensure synchronized updates across HUD components. (Oct14.1 - SIMP-OCT14.8-S1).
*   **1.206 Atomic DataStore Transformations (R-ID 724)**: All Read-Modify-Write operations in `SettingsRepository` MUST occur within the DataStore `updateData` atomic block. Methods performing increments or state-dependent updates MUST use functional transformation patterns to prevent race conditions during concurrent access. (Oct14.1 - ISSUE-OCT14.0-F1).
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects (e.g., `HudMapper`, `DashboardMapper`) to ensure ViewModel maintainability, testability, and separation of concerns. (Oct14.2 - SIMP-OCT14.10-S1).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers (e.g., `MappedByteBuffer.force()`) MUST be executed outside of critical synchronized blocks to prevent UI stalls and thread contention, especially on lower-end hardware like Samsung A15. (Oct14.2 - SIMP-OCT14.11-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 730**: Architectural Mapping Extraction - PASSED. Extracted HUD and Dashboard mapping logic into pure mappers, reducing `MainViewModel` complexity. (Oct14.2 - SIMP-OCT14.10-S1).
*   **SOT ID 729**: Forensic Spill Flush Optimization - PASSED. Optimized `ForensicSpillBuffer.commitDrain` by moving `force()` outside the lock to eliminate latency spikes. (Oct14.2 - SIMP-OCT14.11-S1).
*   **SOT ID 728**: Physical Hardware Verification (A15/S21FE) - PASSED. Verified device deployment, sensor initialization, and foreground service stability on connected hardware. (Oct14.1 - ISSUE-OCT14.14-P1).
*   **SOT ID 727**: DataStore Atomic Mutation Stability - PASSED. Hardened `SettingsRepository` with transformation-based updates. Verified via `DataStoreMutationStressTest` (100% concurrent integrity). (Oct14.1 - ISSUE-OCT14.0-F1).
*   **SOT ID 726**: S21FE Instrumental Test Verification (5m) - Verified 5-minute sustained telemetry cycle on Samsung S21FE hardware. Zero forensic data loss. (Oct14.1 - ISSUE-OCT14.12-I1).
*   **SOT ID 725**: A15 Instrumental Test Verification (30m) - Verified 30-minute sustained 1Hz telemetry cycle on Samsung A15 hardware. Zero forensic data loss. (Oct14.1 - ISSUE-OCT14.10-I1).

...

---

## ✅ Verification Chapters
*   **Chapter 31.805 (Forensic Flush Optimization)**: PASSED - Verified lock hold time reduced from 14ms to <1ms on A15. (Oct14.2).
*   **Chapter 31.804 (Architectural Mapping Audit)**: PASSED - Verified HUD/Dashboard state flows correctly propagated via external mappers. (Oct14.2).
*   **Chapter 31.803 (Physical Verification)**: PASSED - Verified connected A15/S21FE hardware stability. (Oct14.1).
...
