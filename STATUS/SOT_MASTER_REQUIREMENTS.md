# SOT Master Requirements & Hardening Status (Oct6.1)

## 🏗️ Architectural Master Rules (141 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.107 Forensic Diagnostic Expansion (R1344)**: System diagnostic snapshots (`thermalSnapshot`, `heapSnapshot`) MUST be captured and propagated through all telemetry and logging channels. (Oct.5.2 - Issue #1344).
*   **1.108 Native FastPath Convergence (R1510-2)**: ALL high-frequency (100Hz+) sensor math primitives MUST be offloaded to the native C++ layer. (Oct.5.5 - Issue #SIMP-1510-1).
*   **1.109 Prioritized Event Bus (R1328)**: The `DomainEventBus` MUST maintain a minimum buffer capacity of 512 items to handle 100Hz forensic bursts. (Oct.5.5 - Issue #1328).
*   **1.110 Granular UI State Collection (R1422)**: High-frequency transient UI states MUST be collected at the leaf screen level rather than the root. (Oct.5.6 - Issue #1328).
*   **1.111 JNI Math Batching (R1450)**: High-frequency JNI transitions (100Hz+) MUST be consolidated into batched transactions using `DirectByteBuffer`. (Oct.5.7 - Issue #1450).
*   **1.112 Lifecycle-Aware Tick Orchestrator (R1293)**: Background service loops MUST be managed by a centralized `TickOrchestrator`. (Oct.5.8 - Issue #1293).
*   **1.113 Stationary Resource Relaxation (R1295)**: Background tasks MUST implement interval relaxation during verified stationary periods. (Oct.5.9 - Issue #1295).
*   **1.114 Composable Effect Aggregation (R1426)**: Root-level side-effects MUST be centralized into a dedicated aggregator component (`AppEffectAggregator`). (Oct.5.10 - Issue #1426).
*   **1.115 Leaf-Level Convergence (R1426-2)**: ALL UI overlays and high-frequency components MUST collect their own state from Flows to isolate the root UI tree from telemetry bursts. (Oct.5.15 - Issue #SIMP-1426-2).
*   **1.116 Unified State Provider (R1426-3)**: ALL leaf UI components MUST consume state via a unified `UiStateProvider` interface to eliminate parameter-passing boilerplate. (Oct.5.20 - Issue #SIMP-1426-3).
*   **1.117 Map Logic Offloading (R1426-4)**: Computational map state (center calculation, coordinate smoothing triggers) MUST be processed in the ViewModel's state mapping logic to prevent per-frame composition overhead. (Oct.5.21 - Issue #SIMP-1426-4).
*   **1.118 Stable Background State Management (R-ID 288)**: Background UI services MUST initialize state providers and associated flows at the service lifecycle level (onCreate) to ensure stability, prevent memory leaks, and optimize transition latency. (Oct6.1 - Issue #AUDIT-1006-1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 630**: Background Overlay Hardening - Refactored `AlarmOverlayService` to use service-level flow initialization and seeded session state for zero-latency transitions. (Resolved Oct6.1).
*   **SOT ID 629**: Map Hardening & Boilerplate Reduction - Refactored `TrackerScreen` and `ViewerScreen` to use `UiStateProvider`. Migrated `initialCenter` and coordinate smoothing triggers to `MainViewModel`. (Resolved Oct.5.21).
*   **SOT ID 628**: Unified State Convergence - Implemented `UiStateProvider` in `MainViewModel` and refactored all leaf components to consume it. (Resolved Oct.5.20).
*   **SOT ID 627**: Leaf-Level Convergence - Migrated state collection for all overlays and the map container to leaf-level Flow collection. (Resolved Oct.5.15).

---

## 🏁 Verification Chapters
*   **Chapter 31.249 (Background Transition Audit)**: PASSED - Verified zero-latency UI appearance for `AlarmOverlayService`. Confirmed stable flow lifecycle via service scope. (Oct6.1)
*   **Chapter 31.248 (Map Hardening Audit)**: PASSED - Verified offloading of map computational logic to ViewModel. (Oct.5.21)
