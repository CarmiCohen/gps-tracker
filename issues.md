# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.5.5

## 🎯 Current Resumption Focus: Backpressure Risk Mitigation and JNI Hardening.
JNI Hardening Phase 2 complete. Vibration hot-path (100Hz) fully offloaded to C++. Backpressure Risk Mitigation (Issue #1328) implemented via prioritized bus and increased capacity.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 5)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   (All medium-priority simplification ideas resolved).

### 🔵 Low Priority
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low (Refactoring)**. Refactor background services to use a `TickOrchestrator` that handles initialization gates and heartbeat timing internally.
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Audit all `MonitorService` descendants to ensure no redundant reactive streams are active during stationary periods.
*   **Issue #1426: Composable Effect Aggregator**
    *   *Significance*: **Low (Simplicity)**. Centralize all `LaunchedEffect(Unit)` observers in `MainAppContent` into a single wrapper to reduce boilerplate in the root UI file.
*   **Issue #1450: JNI Math Batching**
    *   *Significance*: **Low (JNI)**. Consolidate granular vibration JNI calls (magnitude, HPF, energy) into a single shared-memory buffer update to minimize JNI transition overhead during 100Hz bursts.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1328: Event Bus Backpressure Risk.** Resolved Oct.5.5. Increased `DomainEventBus` buffer capacity to 512 items to provide 5s of headroom during 100Hz forensic bursts. Introduced `EventPriority` metadata to `DomainEvent` hierarchy to support future prioritized drop strategies. (R1328).
*   **Issue #SIMP-1510-1: Native FastPath Convergence (Phase 2).** Resolved Oct.5.5. Fully migrated the high-frequency vibration processing pipeline (100Hz) to JNI. Offloaded vector magnitude, HPF, Kinetic Energy, and violation gates (Shock/Suspicious) to `jdHardware` C++ layer. Hardened coefficients to strictly match `EngineConstants.kt` and eliminated JVM floating-point math from hot paths (R1510-2).
*   **Issue #1344: Forensic Diagnostic Expansion.** Resolved Oct.5.2. Integrated `thermalSnapshot` and `heapSnapshot` into `EngineConnectionPoint`, `RealtimeStatus`, and `TrackerStatusProto`. Updated `IntegrityMonitor` to capture snapshots during heartbeats. (R1344).
*   **Issue #SIMP-1201-1: Logic State Serialization Expansion.** Resolved Oct.5.1. Refactored `AlarmEvaluationState` persistence to use a consolidated Protobuf map (`role_logic_states`) in DataStore. (R1201).
*   **Issue #1173: Protobuf-First Persistence (Phase 2).** Resolved Oct.5.1. Completed the transition to binary persistence for connection history and pending updates. (R1173).

---

## 📊 Hardening Progress Dashboard
- **Oct.5.5: [SOT Count: 273 (Rules: 131), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 33, QA: 405]**
- **Oct.5.2: [SOT Count: 271 (Rules: 130), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 32, QA: 395]**
- **Oct.5.1: [SOT Count: 270 (Rules: 129), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 31, QA: 395]**
- **Oct.4.6: [SOT Count: 268 (Rules: 127), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:4, Testing: 31, QA: 395]**
