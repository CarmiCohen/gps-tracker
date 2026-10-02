# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.2.15

## 🎯 Current Resumption Focus: UI State Optimization
Consolidation of state mappers and granular HUD data binding.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 11)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   **Issue #1290: UI State Mapper Consolidation**
    *   *Significance*: **Medium (Maintainability)**. Merge `UiStateMapper` logic directly into `MainViewModel` now that it is the sole activity-scoped consumer.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Significance*: **Medium (Network)**. Merge conflation and throttling logic into a single reactive "Smart Dispatcher" that handles inter-frame delays and connection freshness flow controls.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Significance*: **Medium (Performance)**. Expand flyweight patterns to all telemetry entities and use ring buffers to eliminate GC churn during long-duration alerts.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Significance*: **Medium (Disk I/O)**. Substitute JSON mapping with pure Protobuf binary pipelines straight into Room BLOB objects for high-frequency history storage.
*   **Issue #1201: Reactive Siren Lockout**
    *   *Significance*: **Medium (Domain Logic)**. Decouple siren cooldown and lockout logic from audio generation by moving it into a dedicated domain UseCase.
*   **Issue #1202: UI Event Routing Unification**
    *   *Significance*: **Medium (Architecture)**. Refactor navigation and global UI commands into a single coordinator to decouple ViewModels from Compose-specific implementation.
*   **Issue #1420: Granular HUD Data Binding**
    *   *Significance*: **Medium (Decoupling)**. Refactor HUD components to consume slice-based interfaces instead of the full `LocationUpdate` monolith to reduce UI-to-Engine coupling.

### 🔵 Low Priority
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low (Refactoring)**. Refactor background services to use a `TickOrchestrator` that handles initialization gates and heartbeat timing internally.
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Audit all `MonitorService` descendants to ensure no redundant reactive streams are active during stationary periods.
*   **Issue #1328: Event Bus Backpressure Risk**
    *   *Significance*: **Low (Robustness)**. Increase `DomainEventBus` capacity and implement a prioritized drop strategy for non-critical telemetry events during high-load bursts.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1176 / SOT ID 599: Native FastPath Transitions.** Resolved Oct.2.15. Offloaded high-frequency sensor spike detection (Acoustic/Light) to JNI to reduce JVM overhead and GC pressure (R-ID 257).
*   **Issue #1314 / SOT ID 598: TrackerStatus Convergence.** Resolved Oct.2.9. Purged redundant TrackerStatus DTO and consolidated all state into LocationUpdate monolith (R598).
*   **Issue #1330 / SOT ID 597: Snap-to-Update Monolith.** Resolved Oct.2.8. Merged SystemEvaluationSnapshot into unified LocationUpdate DTO to eliminate bridge mapping layers (R597).
*   **Issue #1329 / SOT ID 596: Telemetry Mapping Convergence.** Resolved Oct.2.7. Consolidated construction of update DTOs into TelemetryMapper to centralize domain orchestration logic (R-ID 596).
*   **Issue #1175 / SOT ID 595: Real-time Only Path.** Resolved Oct.2.6. Strategically removed forensic backfilling and gap-filling logic to simplify architectural state and reduce heap churn (R-ID 595).
*   **Issue #SIMP-1416-1 / SOT ID 594: Native Sensor Pulse Audit.** Resolved Oct.2.5. Offloaded 250Hz frequency auditing to JNI to eliminate heap churn (R-ID 256).
*   **Issue #1402-B / R-ID 582: WindowManager Lifecycle Hardening.** Resolved Oct.2.3. Implemented full Lifecycle transitions and explicit disposal for AlarmOverlayService (R-ID 582).
*   **Issue #1417: Jitter-Resistant Connectivity Transitions.** Resolved Oct.2.2. Integrated 3s temporal hysteresis for RELAY_OFFLINE and Peer Error suppression (R-ID 593).
*   **Issue #1416: Memory Pressure Mitigation.** Resolved Oct.2.2. Integrated heap-aware throttling for forensic sampling and aggressive GC triggers (R-ID 592).

---

## 💡 Strategic Simplification Ideas (Ideas: 11)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   **Issue #SIMP-1510-1: Native FastPath Convergence.**
    *   *Significance*: **Medium (Architecture)**. Now that native evaluation is stable for Acoustic/Light, expand to `SentinelValidator.isStationary` to fully remove floating-point math from the JVM hot-path.

---

## 📊 Hardening Progress Dashboard
- **Oct.2.15: [SOT Count: 256 (Rules: 113), Open: H:0, M:0, L:0, Ideas: H:0, M:8, L:3, Testing: 15, QA: 362]**
- **Oct.2.9: [SOT Count: 255 (Rules: 112), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:4, Testing: 14, QA: 361]**
- **Oct.2.8: [SOT Count: 254 (Rules: 111), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:4, Testing: 13, QA: 360]**
- **Oct.2.5: [SOT Count: 251 (Rules: 108), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 12, QA: 357]**
