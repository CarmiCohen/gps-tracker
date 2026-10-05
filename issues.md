# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.5.8

## 🎯 Current Resumption Focus: Issue #1295 Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   (All medium-priority simplification ideas resolved).

### 🔵 Low Priority
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Audit all `MonitorService` descendants to ensure no redundant reactive streams are active during stationary periods.
*   **Issue #1426: Composable Effect Aggregator**
    *   *Significance*: **Low (Simplicity)**. Centralize all `LaunchedEffect(Unit)` observers in `MainAppContent` into a single wrapper to reduce boilerplate in the root UI file.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator.** Resolved Oct.5.8. Refactored `BaseMonitorService` tick and heartbeat loops into `TickOrchestrator`. Centralized initialization gating and monotonic pacing. (R1293).
*   **Issue #1450: JNI Math Batching.** Resolved Oct.5.7. Consolidated granular vibration calls (magnitude, HPF, energy, floor update, stationary gate) into a single 256-byte `DirectByteBuffer` transaction (`n19`). Reduced JNI transition overhead from 5 calls per tick to 1, significantly hardening the 100Hz hot-path. (R1450).
*   **Issue #1328: Event Bus Backpressure & UI Performance Hardening.** Resolved Oct.5.6. 
    *   **Phase 1**: Increased `DomainEventBus` buffer to 512 items and implemented prioritized dropping of `LOW` priority events when subscription count exceeds `DOMAIN_EVENT_BUS_HIGH_SUBSCRIPTION_THRESHOLD` (5). 
    *   **Phase 2**: Refactored `MainAppContent`, `TrackerScreen`, and `ViewerScreen` to use granular state slicing. High-frequency state collections (`KinematicState`, `DiagnosticState`) moved from the root UI to specialized screens to eliminate redundant recompositions of the entire UI tree during 100Hz bursts (R1328, R1422).
*   **Issue #SIMP-1510-1: Native FastPath Convergence (Phase 2).** Resolved Oct.5.5. Fully migrated the high-frequency vibration processing pipeline (100Hz) to JNI. (R1510-2).
*   **Issue #1344: Forensic Diagnostic Expansion.** Resolved Oct.5.2. Integrated `thermalSnapshot` and `heapSnapshot` into `EngineConnectionPoint`, `RealtimeStatus`, and `TrackerStatusProto`. (R1344).
*   **Issue #SIMP-1201-1: Logic State Serialization Expansion.** Resolved Oct.5.1. (R1201).

---

## 📊 Hardening Progress Dashboard
- **Oct.5.8: [SOT Count: 276 (Rules: 135), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 36, QA: 415]**
- **Oct.5.7: [SOT Count: 275 (Rules: 134), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:3, Testing: 35, QA: 410]**
- **Oct.5.6: [SOT Count: 274 (Rules: 133), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 34, QA: 405]**
- **Oct.5.5: [SOT Count: 273 (Rules: 132), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 34, QA: 405]**
- **Oct.5.2: [SOT Count: 271 (Rules: 130), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 32, QA: 395]**
- **Oct.5.1: [SOT Count: 270 (Rules: 129), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 31, QA: 395]**
- **Oct.4.6: [SOT Count: 268 (Rules: 127), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:4, Testing: 31, QA: 395]**
