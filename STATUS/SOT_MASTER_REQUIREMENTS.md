# SOT Master Requirements & Hardening Status (Oct6.2)

## 🏗️ Architectural Master Rules (143 Rules)

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
*   **1.119 Zero-Drop Safety Telemetry (R660-H)**: Critical safety alerts (`isImportant = true`) MUST NOT be dropped due to log buffer backpressure; implementations MUST provide async fallback paths to await buffer capacity. (Oct6.2 - Issue #AUDIT-1006-5).
*   **1.120 Memory-Aware Loop Throttling (R-ID 592-M)**: Background loops MUST implement aggressive interval relaxation (up to 15s) when `MemoryPressureLevel.CRITICAL` is detected to prevent background OOM. (Oct6.2 - Issue #AUDIT-1006-6).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 632**: Memory Throttling - Enhanced `MonitorService` to relax tick intervals to 15s during critical memory pressure. (Resolved Oct6.2).
*   **SOT ID 631**: Log Reliability - Hardened `LogRepository` to ensure important telemetry bypasses buffer overflow drops via async fallback. (Resolved Oct6.2).
*   **SOT ID 630**: Background Overlay Hardening - Refactored `AlarmOverlayService` to use service-level flow initialization and seeded session state for zero-latency transitions. (Resolved Oct6.1).

---

## 🏁 Verification Chapters
*   **Chapter 31.250 (Telemetry Pressure Audit)**: PASSED - Verified 100Hz log burst integrity and zero-drop behavior for important alerts. (Oct6.2)
*   **Chapter 31.249 (Background Transition Audit)**: PASSED - Verified zero-latency UI appearance for `AlarmOverlayService`. Confirmed stable flow lifecycle via service scope. (Oct6.1)
