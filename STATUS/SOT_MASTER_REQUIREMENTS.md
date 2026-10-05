# SOT Master Requirements & Hardening Status (Oct.5.8)

## 🏗️ Architectural Master Rules (135 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.107 Forensic Diagnostic Expansion (R1344)**: System diagnostic snapshots (`thermalSnapshot`, `heapSnapshot`) MUST be captured and propagated through all telemetry and logging channels. Diagnostic data MUST be merged during history ribbon aggregation to preserve peak stress metrics. Alarm logs MUST carry these snapshots to enable remote correlation of hardware load with trajectory anomalies. (Oct.5.2 - Issue #1344).
*   **1.108 Native FastPath Convergence (R1510-2)**: ALL high-frequency (100Hz+) sensor math primitives, including vector magnitude, high-pass filtering, and energy EMA, MUST be offloaded to the native C++ layer (`jdHardware`). Violation gates for vibration and shock MUST be evaluated natively with CPU-load awareness to eliminate JVM floating-point overhead and battery drain during stationary monitoring. (Oct.5.5 - Issue #SIMP-1510-1).
*   **1.109 Prioritized Event Bus (R1328)**: The `DomainEventBus` MUST maintain a minimum buffer capacity of 512 items to handle 100Hz forensic bursts. ALL `DomainEvent` descendants MUST carry an `EventPriority` metadata field (CRITICAL, HIGH, NORMAL, LOW) to support deterministic backpressure handling and ensure critical alarms are never dropped during buffer saturation. (Oct.5.5 - Issue #1328).
*   **1.110 Granular UI State Collection (R1422)**: High-frequency transient UI states (KinematicState, DiagnosticState) MUST be collected at the leaf screen level (e.g., TrackerScreen) rather than the root (MainAppContent). Root state collection MUST be limited to low-frequency navigation and session metadata to prevent redundant recompositions of the entire UI tree during 100Hz sensor bursts. (Oct.5.6 - Issue #1328).
*   **1.111 JNI Math Batching (R1450)**: High-frequency JNI transitions (100Hz+) MUST be consolidated into batched transactions using `DirectByteBuffer`. Granular math calls (magnitude, HPF, energy) MUST be performed within a single native context to minimize JNI bridge overhead and CPU context switching during sensor bursts. (Oct.5.7 - Issue #1450).
*   **1.112 Lifecycle-Aware Tick Orchestrator (R1293)**: Background service loops MUST be managed by a centralized `TickOrchestrator` to ensure initialization gating and monotonic pacing. Manual `while(isActive)` loops in services are deprecated in favor of orchestrated periodic management that respects service initialization state. (Oct.5.8 - Issue #1293).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 622**: Lifecycle-Aware Tick Orchestrator - Migrated service loops to managed periodic orchestration to ensure initialization gating and monotonic pacing. (Resolved Oct.5.8).
*   **SOT ID 621**: JNI Math Batching - Consolidated granular vibration math into a single 256-byte DirectByteBuffer transaction to reduce JNI bridge overhead. (Resolved Oct.5.7).
*   **SOT ID 620**: Granular UI State Collection - Moved high-frequency state observers from MainAppContent to specialized screens to eliminate UI-induced backpressure. (Resolved Oct.5.6).
*   **SOT ID 619**: Prioritized Event Bus - Increased DomainEventBus capacity to 512 and implemented EventPriority metadata across the hierarchy. (Resolved Oct.5.5).
*   **SOT ID 618**: Native FastPath Convergence (Phase 2) - Fully migrated the 100Hz vibration pipeline to JNI, including magnitude, HPF, Energy, and Violation Gates. (Resolved Oct.5.5).
*   **SOT ID 617**: Forensic Diagnostic Expansion - Integrated thermal and memory snapshots across all telemetry aggregation and logging paths. (Resolved Oct.5.2).

---

## 🏁 Verification Chapters
*   **Chapter 31.241 (Tick Orchestrator Audit)**: PASSED - Verified that heartbeat and tick loops await initialization automatically. Confirmed monotonic pacing stability via `SystemClock.elapsedRealtime`. (Oct.5.8)
*   **Chapter 31.240 (JNI Batching Audit)**: PASSED - Verified reduction of JNI calls from 5 to 1 per vibration tick. Confirmed math parity for HPF and Energy EMA between batched native and JVM fallback. (Oct.5.7)
*   **Chapter 31.239 (UI Recomposition Audit)**: PASSED - Verified that `MainAppContent` no longer recomposes on RTT or battery updates. Confirmed specialized dashboard components only recompose when their specific state slice (e.g., `KinematicState`) changes. (Oct.5.6)
*   **Chapter 31.238 (Event Bus Backpressure Audit)**: PASSED - Verified buffer expansion to 512 items in `DomainEventBus`. Confirmed `EventPriority` implementation in `EngineModels.kt` for all event types. Validated non-blocking emission during simulated 100Hz bursts. (Oct.5.5)
*   **Chapter 31.237 (JNI Vibration Audit)**: PASSED - Verified native offloading of magnitude, HPF, and energy math at 100Hz in `HardwareSuite`. Confirmed violation gate parity between JVM and C++ in `SentinelValidator`. (Oct.5.5)
