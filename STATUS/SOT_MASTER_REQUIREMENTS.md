# SOT Master Requirements & Hardening Status (Oct.5.15)

## 🏗️ Architectural Master Rules (138 Rules)

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

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 627**: Leaf-Level Convergence - Migrated state collection for all overlays and the map container to leaf-level Flow collection to eliminate root-level recomposition pressure. (Resolved Oct.5.15).
*   **SOT ID 626**: Readiness Logic Convergence - Centralized "System Readiness" and "Issue Count" logic in `SessionUiState` to eliminate duplication in leaf screens. (Resolved Oct.5.12).
*   **SOT ID 625**: Side-Effect Latency Audit - Verified isolation of 100Hz vibration pipeline from root UI side-effects in `AppEffectAggregator`. (Resolved Oct.5.11).
*   **SOT ID 624**: Composable Effect Aggregator - Centralized root side-effects into `AppEffectAggregator`. (Resolved Oct.5.10).
*   **SOT ID 623**: Stationary Resource Relaxation - Implemented interval relaxation for heartbeats and forensic sampling. (Resolved Oct.5.9).
*   **SOT ID 622**: Lifecycle-Aware Tick Orchestrator - Migrated service loops to managed periodic orchestration. (Resolved Oct.5.8).

---

## 🏁 Verification Chapters
*   **Chapter 31.246 (Leaf Convergence Audit)**: PASSED - Verified migration of state collection to leaf components in `LogOverlay`, `SettingsOverlay`, `PhoneSetupOverlay`, `AlarmOverlay`, `GlobalStatusBar`, `RibbonsOverlay`, `TrackerDashboard`, `ViewerDashboard`, and `AppMapContainer`. Confirmed reduction of root-level recomposition frequency. (Oct.5.15)
*   **Chapter 31.245 (Readiness Convergence Audit)**: PASSED - Verified elimination of ~100 lines of duplicated logic across `TrackerScreen` and `ViewerScreen`. Confirmed `MainUiState` as the single source of truth for readiness. (Oct.5.12)
*   **Chapter 31.244 (Latency Audit)**: PASSED - Verified `AppEffectAggregator` decoupling from 100Hz pipeline. (Oct.5.11)
*   **Chapter 31.243 (Effect Aggregation Audit)**: PASSED - Verified centralization of UI effects and lifecycle observers. (Oct.5.10)
