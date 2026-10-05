# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.4.6

## 🎯 Current Resumption Focus: Protobuf-First Persistence
Flyweight & Pooling Expansion complete; binary persistence pipeline established. Transitioning to full binary migration in `HistoryManager` and `OfflineRepository` in next cycle.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 9)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   **Issue #1173: Protobuf-First Persistence**
    *   *Significance*: **Medium (Disk I/O)**. Substitute JSON mapping with pure Protobuf binary pipelines straight into Room BLOB objects for high-frequency history storage. (Schema & Mapper Complete).
*   **Issue #SIMP-1201-1: Logic State Serialization Expansion**
    *   *Significance*: **Medium (Simplicity)**. Refactor `saveLogicState` in `SettingsRepository` to accept a structured `AlarmEvaluationState` fragment rather than 10 individual parameters, utilizing Protobuf's native mapping to reduce boilerplate.

### 🔵 Low Priority
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low (Refactoring)**. Refactor background services to use a `TickOrchestrator` that handles initialization gates and heartbeat timing internally.
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Audit all `MonitorService` descendants to ensure no redundant reactive streams are active during stationary periods.
*   **Issue #1328: Event Bus Backpressure Risk**
    *   *Significance*: **Low (Robustness)**. Increase `DomainEventBus` capacity and implement a prioritized drop strategy for non-critical telemetry events during high-load bursts.
*   **Issue #1426: Composable Effect Aggregator**
    *   *Significance*: **Low (Simplicity)**. Centralize all `LaunchedEffect(Unit)` observers in `MainAppContent` into a single wrapper to reduce boilerplate in the root UI file.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1160: Flyweight & Pooling Expansion.** Resolved Oct.4.6. Implemented `RingBufferPool` and centralized `EnginePools` for `LocationUpdate`, `ProcessedLocation`, `SystemHealthState`, and `TrajectoryNode`. Refactored high-frequency paths in `MonitorService` and `LocationProcessor` to use pooled acquisition, eliminating GC churn during long-duration alerts and 100Hz sampling (R1160).
*   **Issue #1425 / SOT ID 613: Unified Clock Authority.** Resolved Oct.4.5. Standardized all telemetry, HUD, and map "freshness" evaluations to strictly use `SystemClock.elapsedRealtime()` via `TimeProvider`. Eliminated wall-clock arithmetic in service triggers, log flush intervals, and user-activity heuristics to prevent logic corruption during NTP syncs (R1425).
*   **Issue #1202 / SOT ID 612: UI Event Routing Unification.** Resolved Oct.4.1. Refactored navigation and procedural UI logic (service startup, permission orchestration) into `UiEventCoordinator`. Introduced `UiEffect` stream to decouple ViewModels and Composables from imperative commands. Centralized mode transition authority in the domain layer (R1202).
*   **Issue #1201 / SOT ID 610: Reactive Siren Lockout.** Resolved Oct.3.9. Decoupled siren cooldown and lockout logic from audio generation and violation detection. Centralized authority in `SirenLockoutUseCase` and purged redundant `lastSirenStopRt` state from engine modules; implemented reactive requirement refreshing in `AppAlarmManager` (R1201).
*   **Issue #1172 / SOT ID 608: Smart Signaling Dispatcher.** Resolved Oct.3.8. Consolidated all signaling triggers (joins, leaves, pings, telemetry) into a reactive coordination layer. Implemented connection-aware delivery and adaptive throttling to allow handshake bursts while maintaining inter-frame delays for telemetry; optimized payloads to native Maps (R1172).
*   **Issue #1423 / SOT ID 607: StatusBar Visual & Logic Hardening.** Resolved Oct.3.7. Switched details row to vertical column in portrait to fix layout stacking; unified color authority between speed and state labels; corrected `isLocalGpsActive` logic and synchronized GPS age with Realtime clock source (R1424).
*   **Issue #1421 / SOT ID 604: HUD Visibility & Hardening.** Resolved Oct.3.6. Forced LTR layout for technical HUD elements to fix RTL mirroring on Hebrew devices; implemented weight-based balancing in `StatusRowData` to prevent telemetry text jumbling in portrait; reduced `HeaderBar` vertical height (R1421).
*   **Issue #910 / SOT ID 606: JNI Implementation Completeness.** Resolved Oct.3.6. Fixed `UnsatisfiedLinkError` by implementing missing native methods (n7-n13) for sensor auditing and stationary detection convergence; verified via successful SRV deployment (R606).
*   **Issue #1422 / SOT ID 605: Connection Sticky-State & Validation.** Resolved Oct.3.6. Fixed bug where `CommunicationManager` would ignore URL/ID changes if already connected; corrected log relay validator logic to prevent tracker logs from being dropped by the viewer; added `ACCESS_NETWORK_STATE` to manifest (R1422).
*   **Issue #1420-S / SOT ID 603: HUD Stabilization.** Resolved Oct.3.2. Synchronized UI call sites with new slice-based interfaces; resolved legacy property mismatches (R1420-S).
*   **Issue #1420 / SOT ID 601: Granular HUD Binding.** Resolved Oct.3.1. Decoupled UI components from `LocationUpdate` monolith via interface slicing (R1420).
*   **Issue #SIMP-1510-1 / SOT ID 602: Native Stationary Convergence.** Resolved Oct.3.1. Offloaded stationary detection and vibration floor EMA to JNI to eliminate JVM math overhead (R1510).
*   **Issue #1290 / SOT ID 600: UI State Mapper Consolidation.** Resolved Oct.2.15. Merged `UiStateCoordinator` logic into `MainViewModel` to reduce architectural complexity and dependency layers (R1290).
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

## 📊 Hardening Progress Dashboard
- **Oct.4.6: [SOT Count: 268 (Rules: 127), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:4, Testing: 31, QA: 395]**
- **Oct.4.5: [SOT Count: 267 (Rules: 126), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:4, Testing: 30, QA: 390]**
- **Oct.4.1: [SOT Count: 266 (Rules: 125), Open: H:0, M:0, L:0, Ideas: H:0, M:4, L:4, Testing: 29, QA: 387]**
- **Oct.3.9: [SOT Count: 265 (Rules: 124), Open: H:0, M:0, L:0, Ideas: H:0, M:4, L:3, Testing: 28, QA: 386]**
- **Oct.3.8: [SOT Count: 264 (Rules: 123), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 27, QA: 385]**
- **Oct.3.7: [SOT Count: 263 (Rules: 122), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:3, Testing: 26, QA: 380]**
- **Oct.3.6: [SOT Count: 262 (Rules: 121), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 25, QA: 375]**
- **Oct.3.5: [SOT Count: 260 (Rules: 118), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 23, QA: 370]**
- **Oct.3.4: [SOT Count: 260 (Rules: 118), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 22, QA: 369]**
- **Oct.3.2: [SOT Count: 259 (Rules: 116), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 21, QA: 368]**
- **Oct.3.1: [SOT Count: 259 (Rules: 116), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 18, QA: 365]**
- **Oct.2.15: [SOT Count: 257 (Rules: 114), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:3, Testing: 16, QA: 363]**
- **Oct.2.9: [SOT Count: 255 (Rules: 112), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:3, Testing: 14, QA: 361]**
- **Oct.2.8: [SOT Count: 254 (Rules: 111), Open: H:0, M:0, L:0, Ideas: H:0, M:7, L:4, Testing: 13, QA: 360]**
- **Oct.2.5: [SOT Count: 251 (Rules: 108), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 12, QA: 357]**
