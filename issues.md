# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.2.8

## 🎯 Current Resumption Focus: Forensic Stream Optimization
Hardening of native-offloaded sensor audits and JNI fast-path transitions.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 11)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   **Issue #1314: TrackerStatus & Evaluation Snapshot Convergence**
    *   *Significance*: **Medium (Architecture)**. Evaluate if `TrackerStatus` DTO can be merged into `LocationUpdate` to eliminate the mapping layer in `ConnectivitySuite`.
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

### 🔵 Low Priority
*   **Issue #1176: Native FastPath**
    *   *Significance*: **Low**. Offload `HardwareFastPath` (Acoustic/Light spikes) to JNI to further reduce JVM sensor overhead on budget hardware.
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low (Refactoring)**. Refactor background services to use a `TickOrchestrator` that handles initialization gates and heartbeat timing internally.
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Audit all `MonitorService` descendants to ensure no redundant reactive streams are active during stationary periods.
*   **Issue #1328: Event Bus Backpressure Risk**
    *   *Significance*: **Low (Robustness)**. Increase `DomainEventBus` capacity and implement a prioritized drop strategy for non-critical telemetry events during high-load bursts.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1330 / SOT ID 597: Snap-to-Update Monolith.** Resolved Oct.2.8. Merged SystemEvaluationSnapshot into unified LocationUpdate DTO to eliminate bridge mapping layers (R597).
*   **Issue #1329 / SOT ID 596: Telemetry Mapping Convergence.** Resolved Oct.2.7. Consolidated construction of update DTOs into TelemetryMapper to centralize domain orchestration logic (R-ID 596).
*   **Issue #1175 / SOT ID 595: Real-time Only Path.** Resolved Oct.2.6. Strategically removed forensic backfilling and gap-filling logic to simplify architectural state and reduce heap churn (R-ID 595).
*   **Issue #SIMP-1416-1 / SOT ID 594: Native Sensor Pulse Audit.** Resolved Oct.2.5. Offloaded 250Hz frequency auditing to JNI to eliminate heap churn (R-ID 256).
*   **Issue #1402-B / R-ID 582: WindowManager Lifecycle Hardening.** Resolved Oct.2.3. Implemented full Lifecycle transitions and explicit disposal for AlarmOverlayService (R-ID 582).
*   **Issue #1417: Jitter-Resistant Connectivity Transitions.** Resolved Oct.2.2. Integrated 3s temporal hysteresis for RELAY_OFFLINE and Peer Error suppression (R-ID 593).
*   **Issue #1416: Memory Pressure Mitigation.** Resolved Oct.2.2. Integrated heap-aware throttling for forensic sampling and aggressive GC triggers (R-ID 592).

---

## 📊 Hardening Progress Dashboard
- **Oct.2.8: [SOT Count: 254 (Rules: 111), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:4, Testing: 13, QA: 360]**
- **Oct.2.7: [SOT Count: 253 (Rules: 110), Open: H:0, M:0, L:0, Ideas: H:0, M:8, L:4, Testing: 13, QA: 359]**
- **Oct.2.6: [SOT Count: 252 (Rules: 109), Open: H:0, M:0, L:0, Ideas: H:1, M:8, L:4, Testing: 13, QA: 358]**
- **Oct.2.5: [SOT Count: 251 (Rules: 108), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 12, QA: 357]**
- **Oct.2.3: [SOT Count: 251 (Rules: 107), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 12, QA: 356]**
