# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.26.12

## 🎯 Current Resumption Focus: Architectural Hardening
Ready for next priority item.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)

*   **Issue #1345: Expand Automated Network Stress Tests**
    *   *Description*: Implement a simulation in `ConnectivitySuite` that toggles the relay connection status at high frequency to ensure the new `PeerConnectionChanged` events and backoff logic don't leak resources during network flapping.

*   **Issue #1346: Physical Device Soak Test (24-Hour Observation)**
    *   *Description*: Initiate a real-world validation test on target hardware (e.g., Samsung A15) by running Tracker Mode for 24 continuous hours.
    *   *Verification*: Periodically audit stability metrics using `adb logcat -s ForensicAuditor MonitorService | grep "STABILITY AUDIT"`.
    *   *Success Criteria*: Reliability index remains > 98.0% (R500) with zero "Davey" frame drops exceeding 700ms.

### 🟡 Medium Priority (UX, Performance & Auditability)

*   **Issue #1347: Audit and Itemize Strategic Simplification Candidates**
    *   *Description*: Prune legacy complexity across the codebase to match the "Ideas: 17" dashboard metadata count. Explicitly catalog the remaining 15 candidates (e.g., further unifying `LocationProcessor` states or simplifying the `DomainEvent` hierarchy).

---

## 💡 Strategic Simplification Ideas (Ideas: 17)

### 🟡 Medium Priority
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Significance*: **Medium-High (Performance)**. Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer`.
*   **Issue #1294: Build-Time Interface Validation**
    *   *Significance*: **Medium (Quality)**. Implement custom Gradle tasks to verify service implementations before compilation.
*   **Issue #1290: UI State Mapper Consolidation**
    *   *Significance*: **Medium (Maintainability)**. Merge `UiStateMapper` logic directly into `MainViewModel` to reduce DI surface area.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Significance*: **Medium (Network)**. Merge conflation and throttling logic into a reactive "Smart Dispatcher" to handle connection freshness.
*   **Issue #1163: Stateless & Functional Logic Refactoring**
    *   *Significance*: **Medium (Robustness)**. Migrate `LocationProcessor` to a functional model using immutable states.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Significance*: **Medium (Performance)**. Expand flyweight patterns to all telemetry entities and use ring buffers to eliminate GC churn.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Significance*: **Medium (Performance)**. Substitute JSON mapping with pure Protobuf binary pipelines to speed up disk I/O.
*   **Issue #1205: Context-Aware Power Optimization**
    *   *Significance*: **Medium (Battery)**. Dynamically adjust sensor polling based on activity recognition to extend battery life.
*   **Issue #1201: Reactive Siren Lockout**
    *   *Significance*: **Medium (Domain Logic)**. Decouple siren cooldown logic from audio generation by moving it into a dedicated UseCase.
*   **Issue #1202: UI Event Routing Unification**
    *   *Significance*: **Medium (Architecture)**. Refactor navigation into a single coordinator to decouple ViewModels from UI implementation.
*   *(Issue #1336 consolidated into hardening)*
*   *(Issue #1215 resolved: Decommission legacy ViewModels)*

### 🔵 Low Priority
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low-Medium (Refactoring)**. Refactor background services to use a TickOrchestrator that handles initialization gates internally.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Significance*: **Low-Medium (UI Decoupling)**. Extract osmdroid management into a standalone controller to keep UI code declarative.
*   **Issue #1296: Centralized Boot Lifecycle Authority**
    *   *Significance*: **Low (Testability)**. Move monotonic clock recovery logic to a dedicated authority to simplify background service testing.
*   **Issue #1295: Redundant Stream Observer Audit**
    *   *Significance*: **Low (CPU)**. Systematically audit all service descendants to ensure no redundant reactive streams are active.
*   **Issue #1171: Service & Worker Consolidation**
    *   *Significance*: **Low (Maintenance)**. Merge role-specific services into a single monitor service to reduce manifest overhead.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1344: Implement Thermal & Memory Forensic Probes** (Resolved Sep.26.12)
    *   *Remediation*: Integrated OS-level thermal headroom and heap utilization monitoring into the periodic integrity heartbeat loop. Expanded binary forensic trace schema to version 4 to support high-assurance hardware stability auditing during long-duration soak tests. (SOT ID 502).
*   **Issue #1343: Signaling Lifecycle Probes** (Resolved Sep.26.11)
    *   *Remediation*: Deployed high-assurance forensic probes to `SignalingForensicLogger` and integrated them within `ConnectivitySuite` to trace interface handover events and log throttled outbound transmission failures. (SOT ID 501).
*   **Issue #1342: Programmatic 24h Soak Simulation** (Resolved Sep.26.10)
    *   *Remediation*: Implemented accelerated 24-hour simulation in `ProductionReadinessAuditTest.kt` to verify forensic counter stability and reliability math. (SOT ID 500).
*   **Issue #1341: Alias-Aware Identity Uniqueness** (Resolved Sep.26.10)
    *   *Remediation*: Hardened `SettingsRepository.commitDraftSettings` with alias-aware uniqueness checks using `SignalingConstants.areIdsUnique` to prevent role-prefix collisions. (SOT ID 499).
*   **Issue #1340: Hardware Poke Precision Boundary** (Resolved Sep.26.10)
    *   *Remediation*: Fixed boundary logic in `HardwareSuite.shouldPokeHardware` to use inclusive `>=` check, ensuring deterministic wakeups on staggered performance tiers. (SOT ID 498).
*   **Issue #1339: Unified Power Policy Validation** (Resolved Sep.26.9)
    *   *Remediation*: Verified centralized backoff and Doze-deferral consistency across role transitions using `ProductionReadinessAuditTest`. Updated `QA_VALIDATION_STATUS.md` to clear the pending R339 verification entry.
*   **Issue #1215: Decommission setupViewModel boilerplate** (Resolved Sep.26.8)
    *   *Remediation*: Decommissioned legacy feature-specific ViewModels (`SetupViewModel.kt`, `TrackerViewModel.kt`, `ViewerViewModel.kt`) following the consolidation of all UI state and domain routing into the activity-scoped `MainViewModel` SSOT. (SOT ID 496).
*   **Issue #1338: Lifecycle Hydration Staggering Verification** (Resolved Sep.23.50)
    *   *Remediation*: Developed `HydrationStaggeringAuditTest` to verify that `LifecycleHydrationManager` correctly staggers level transitions under 100% CPU/IO saturation, ensuring deterministic startup on constrained hardware. (SOT ID 495).
*   **Issue #1203: Hilt ViewModel Scope Optimization** (Resolved Sep.23.50)
    *   *Remediation*: Optimized Hilt ViewModel scoping and eliminated redundant stream resource churn, state loss, and misrouted kinematic state by unifying all feature screens and overlays to consume the activity-scoped `MainViewModel` directly. (SOT ID 494).
*   **Issue #1337: Role-Prefix Collision in Integrity Events** (Resolved Sep.26.6)
    *   *Remediation*: Refactored `AppEventCoordinator.handleIntegrityEvent` to strictly limit power alarm pending updates to Tracker mode only, preventing local power state leaks into the remote tracker's state during Viewer mode. Hardened `AppAlarmManager` by adding prefix validation within `setPowerAlarmPending` and `resetEvaluation` to prevent role-prefix flipping and write collisions. (SOT ID 493).
*   **Issue #1336: AppEventCoordinator & HistoryManager Side-Effect Unification** (Resolved Sep.26.5)
    *   *Remediation*: Refactored `AppEventCoordinator` to remove role-specific branching guards in `handleProcessorEvent`, enabling consistent persistence for self-tracking state (Max Accuracy, Chair Baseline, etc.) regardless of role. Unified `handleIntegrityEvent` to use dynamic `alarmPrefix` mapping ("T_" or "VR_") for power alarm pending state, ensuring alignment with `MonitorService` restoration logic. (SOT ID 492).
*   **Issue #1335: Initialization Prefix Unification** (Resolved Sep.26.4)
    *   *Remediation*: Unified `MonitorService.loadLogicState` to use `rolePrefix` for the `primaryProcessor` state restoration regardless of role, while reserving the "VR_" prefix strictly for the `remoteProcessor`. This removed the explicit `isTrackerMode` branching during initialization and ensured consistent self-tracking persistence for both Tracker and Viewer roles. (SOT ID 491).
*   **Issue #1334: Unified GPS Pipeline Hardening & Forensic Audit Integration** (Resolved Sep.26.3)
    *   *Remediation*: 
        1. Fixed a critical typo in `LocationProcessor.loadState` where the spatial anchor was incorrectly initialized with duplicate latitudes (`lat, lat` instead of `lat, lng`), preventing filter divergence on service restart.
        2. Standardized `lastGpsTs` tracking to wall-clock time (`loc.time`) uniformly across Tracker and Viewer roles in `MonitorService` to ensure reliable GPS stall detection.
        3. Integrated `ForensicAuditor.recordGpsFix` into the unified `locationBuffer` processing loop to audit stability and jitter during high-frequency GPS bursts.
        4. Refactored 42 unit tests in `:core:engine` to align with the stateless `LocationProcessingState` and unified `SystemEvaluationSnapshot` pipeline signature. (SOT ID 490).
*   **Issue #1333: Peer Connection State Caching** (Resolved Sep.26.2)
    *   *Remediation*: Introduced a state cache in `AppEventCoordinator` to suppress redundant lifecycle logging during peer pulses. (SOT ID 489).
*   **Issue #1332: Viewer Self-Tracking Pipeline Unification** (Resolved Sep.26.1)
    *   *Remediation*: Unified GPS buffering and processing for all roles under a single evaluation path. (SOT ID 488).
*   **Issue #1314: TrackerStatus & Evaluation Snapshot Convergence** (Resolved Sep.26.0)
    *   *Remediation*: Consolidated telemetry DTOs into partitioned engine states. (SOT ID 487).
*   **Issue #1329: Telemetry Mapping Convergence** (Resolved Sep.25.08)
    *   *Remediation*: Fully centralized telemetry data transformation in `TelemetryMapper`. Consolidated mapping for `LocationUpdate`, `TrackerStatus`, and `PendingStatusEntity`, including incoming Proto/JSON signaling payloads. Removed ~250 lines of redundant mapping logic from `ConnectivitySuite` and `AppEventCoordinator`, ensuring a single source of truth for all role-agnostic data conversions. Fixed JSON key typos for cooling and battery state. (SOT ID 486).
*   **Issue #1330: Snap-to-Update Monolith** (Resolved Sep.25.06)
    *   *Remediation*: Unified `SystemEvaluationSnapshot` with the partitioned state structure (`KineticState`, `AtmosphericState`, `IntegrityState`) used by `LocationUpdate`. Refactored `LocationProcessor`, `MonitorService`, and `AppEventCoordinator` to utilize these shared structures, eliminating ~100 lines of manual field-to-field mapping and reducing allocation churn during background pulses. (SOT ID 485).
*   **Issue #1327: Pulse-to-Tick Event Collision** (Resolved Sep.25.05)
    *   *Remediation*: Introduced `DomainEvent.PeerConnectionChanged` to handle lifecycle-only notifications from peer pulses. Replaced redundant `TickEvaluated` emissions in pulse handlers to eliminate unnecessary side-effects and stale telemetry propagation. (SOT ID 484).
*   **Issue #1324: Peer Signaling Coupling to Repository** (Resolved Sep.25.04)
    *   *Remediation*: Transitioned peer telemetry persistence to a reactive, bus-driven model. `ConnectivitySuite` now emits `DomainEvent.PeerStatusReceived` upon validating incoming peer updates, which is then persisted by `AppEventCoordinator`. (SOT ID 483).
*   **Issue #1323: Residual Imperative Persistence in MonitorService** (Resolved Sep.25.03)
    *   *Remediation*: Converged Viewer self-tracking persistence into the unified `DomainEventBus`. Eliminated the imperative `updateRepositoryLocation` method in `MonitorService`, offloading I/O to the `AppEventCoordinator`. (SOT ID 482).
*   **Issue #1331: DomainEventBus Capacity Hardening** (Resolved Sep.25.02)
    *   *Remediation*: Increased extra buffer capacity to 128 and introduced DROP_OLDEST overflow policy. (SOT ID 481).
*   **Issue #1322: Multi-Flow Fragmentation Convergence** (Resolved Sep.25.01)
    *   *Remediation*: Converged all component-level event streams into the unified bus. (SOT ID 480).
*   **Issue #1325: Unified Snapshot Metadata Gaps** (Resolved Sep.25.00)
    *   *Remediation*: Expanded `SystemEvaluationSnapshot` for full parity. (SOT ID 478).
*   **Issue #1326: Telemetry Data Corruption in LocationProcessor** (Resolved Sep.25.00)
    *   *Remediation*: Corrected satellite count mapping. (SOT ID 479).
*   **Issue #1312: Unified Evaluation Logic Snapshot** (Resolved Sep.24.96)
    *   *Remediation*: Consolidated `AlarmTelemetrySnapshot` and `SensorStateSnapshot` into a single `SystemEvaluationSnapshot`. Refactored `MonitorService`, `LocationProcessor`, and `AppAlarmManager` to consume this unified DTO, ensuring absolute temporal parity between kinematic, environmental, and health logic during background evaluation ticks. Aligned property names with `SystemHealthState` for architectural consistency. (SOT ID 475).
*   **Issue #1313: Role-Switching Atomic State Reset** (Resolved Sep.24.96)
    *   *Remediation*: Hardened `MonitorService` to handle runtime role transitions by introducing a reactive `appModeFlow` observer. Implemented `handleRoleTransition` which executes an atomic session reset via `SessionLifecycleCoordinator`, preventing state leakage during Tracker <-> Viewer switches. (SOT ID 476).
*   **Issue #1163: Stateless & Functional Logic Refactoring for LocationProcessor** (Resolved Sep.24.95)
    *   *Remediation*: Migrated `LocationProcessor`, `LocationSentinel`, and `GtoEngine` to a stateless evaluation model. Introduced `LocationProcessingState` to encapsulate all mutable tracking data. Converted core logic engines into pure `object` implementations, ensuring that location validation and anchor management are deterministic functions of their state and inputs. (SOT ID 474).
*   **Issue #1311: AppAlarmManager Stateless Evaluation Model** (Resolved Sep.24.94)
    *   *Remediation*: Fully transitioned `AppAlarmManager` to a stateless evaluation model by moving all operational parameters and active alarm state containers directly into `AlarmEvaluationState`. Eliminated duplicate fields and nested mutable maps to completely preclude cross-role transition state leakage. (SOT ID 473).
*   **Issue #1265: Unified Event Orchestration via AppEventCoordinator** (Resolved Sep.24.93)
    *   *Remediation*: Centralized alert triggers, audio synthesis, and forensic logging into a high-cohesion `AppEventCoordinator`. Decoupled domain reactions from service lifecycles and established reactive siren state binding (Issue #1292) between `AppAlarmManager` and `AudioSynthesizer` to ensure absolute parity across functional roles (R-ID 472).
*   **Issue #1261: Refactor Tracker/Viewer Services into MonitorService** (Resolved Sep.24.92)
    *   *Remediation*: Consolidated TrackerService and ViewerService redundant boilerplate into a unified, lightweight, role-reactive background service. Consolidated stream observation (#1310) and job management (#1309) into a shared lifecycle (R-ID 471).
*   **Issue #1234 / #1244: Heuristic Correction for Thermal Recovery Audits** (Resolved Sep.24.91)
    *   *Remediation*: Refactored `startForensicSamplingLoop` in `TrackerService` and `ViewerService` to calculate Thermal Recovery Latency using an authoritative `coolingEnteredRt` monotonic timestamp populated precisely inside `SystemHealthState` by `IntegrityMonitor` when entering cooling mode, eliminating loop latency measurement errors (SOT ID 470).
*   **Issue #1306: Namespace Collision Risk for Viewer's Self-Tracking** (Resolved Sep.24.90)
    *   *Remediation*: Introduced the `"VR_"` (Viewer-Remote) prefix to strictly segregate the remote tracker's logic state (baselines, accuracy anchors, and alarm evaluation history) from the Viewer's local session telemetry. Updated `RemoteStatusRepository`, `SettingsRepository`, and `MainRepository` to support the new namespace, ensuring that remote peer baseline updates can no longer collide with or corrupt local device performance auditing (R-ID 469).
*   **Issue #1305: Performance Risk: Synchronous Repository Writes on Vibration Floor Jitter** (Resolved Sep.24.80)
    *   *Remediation*: Transitioned high-frequency baseline updates (Vibration, Lux, Acoustic) from the service thread to a debounced, non-blocking coroutine model. Introduced persistent-save jobs with a 1000ms debounce window in both `TrackerService.kt` and `ViewerService.kt`, effectively eliminating tick-loop jitter and synchronous I/O stalls during intense physical vibration or environmental transitions (R-ID 468).
*   **Issue #1272: Alarm Notification Leak in Tracker Mode** (Resolved Sep.24.70)
    *   *Remediation*: Hardened `AppAlarmManager` state cleanup by ensuring `restoreState()` completely flushes in-memory active alarms before early returns. Explicitly updates and resets the `isTrackerMode` role gating flag upon logic state recovery to prevent unexpected "Siren Jumps" during transitions from Tracker to Viewer modes (R-ID 467).
*   **Issue #1308: Missing Forensics Trace Collection in ViewerService** (Resolved Sep.24.60)
    *   *Remediation*: Implemented the `forensicSamplingLoop` and associated channel-driven trigger infrastructure in `ViewerService.kt`. This ensures the monitor role captures local environmental forensics (spatial, IMU, battery, thermal) with the same precision as the Tracker role, enabling comprehensive monitoring integrity audits (R-ID 466).
*   **Issue #1245: Non-Blocking History Flush on Service Termination** (Resolved Sep.24.50)
    *   *Remediation*: Transitioned the final history buffer flush in `BaseMonitorService.onDestroy()` from a synchronous `runBlocking` call to a structured teardown routine offloaded to `applicationScope` with an internal timeout, eliminating main thread shutdown ANRs (R-ID 465).
*   **Issue #1241: Functional Restoration of History Sync Streams in ViewerService** (Resolved Sep.24.40)
    *   *Remediation*: Refactored `observeHistoryEvents()` in `ViewerService.kt` to subscribe to legitimate `historyManager.historyEvents` stream, restoring reactive backfill and sync event visualization on monitor devices (R-ID 464).
*   **Issue #1307: Forensic Sampling Bottleneck During Rapid Event Sequences** (Resolved Sep.24.30)
    *   *Remediation*: Refactored `forensicSamplingLoop` in `TrackerService.kt` to use a buffered boolean channel with non-blocking timeout polling. This ensures physical spikes (acoustic/light) are processed immediately, fully decoupled from the adaptive sampling rate gates (R-ID 463).
*   **Issue #1256: Monotonic Latch Staleness Across Reboots** (Resolved Sep.24.20)
    *   *Remediation*: Implemented Boot-ID validation inside `AppAlarmManager.restoreLogicState` to detect device reboots and safely invalidate obsolete monotonic `elapsedRealtime` latches (cooldowns, violation timers) after a device restart, preventing the permanent muzzle bug (R-ID 462).
*   **Issue #1260: Boot-ID Validation for Persistent Monotonic Latches** (Resolved Sep.24.20)
    *   *Remediation*: Remediated via Boot-ID check in `restoreLogicState`.
*   **Issue #1301: Missing Persistence for Lux and Acoustic Baselines** (Resolved Sep.24.10)
    *   *Remediation*: Implemented persistence for Lux and Acoustic baselines. Expanded `loadForensicState` to restore these anchors and added reactive event emission for significant drift to eliminate the startup learning period (R-ID 461).
*   **Issue #1302: Redundant and Misaligned LocationProcessor in ViewerService** (Resolved Sep.24.10)
    *   *Remediation*: Aligned the Viewer's `selfProcessor` with local sensor updates in `processTick`, ensuring correct motion awareness and role fidelity.
*   **Issue #1303: Cross-Role HardwareSuite Sensitivity Contamination** (Resolved Sep.24.10)
    *   *Remediation*: Decoupled local hardware settings from remote tracker anchors in `ViewerService`, ensuring the monitor device maintains autonomous physical sensitivity.
*   **Issue #1304: Peer Stat Reset Logic Corrupts Local Tracker State** (Resolved Sep.24.10)
    *   *Remediation*: Fixed stat reset routing in `ConnectivitySuite` to prevent local role state corruption during network drops.
*   **Issue #1273: Atomic User Counter Risk in HardwareSuite** (Resolved Sep.24.04)
    *   *Remediation*: Guarded the `activeUsers` AtomicInteger in `HardwareSuite.stop()` to prevent it from falling into negative values. Hardened the deferred teardown check to use `<= 0` (R-ID 460).
*   **Issue #1271: Missing Persistence for Adaptive Vibration Floor** (Resolved Sep.24.04)
    *   *Remediation*: Implemented persistence for the adaptive vibration floor anchor. Added `ADAPTIVE_VIBRATION_FLOOR_KEY` to `PreferenceKeys.kt` (R-ID 459).
*   **Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots** (Resolved Sep.24.02)
    *   *Remediation*: Implemented `recoverLastRealtime` in `HistoryManager.kt` using role-isolated clock drift references (R-ID 458).
*   **Issue #1233: High Allocation Churn via Fast-Path Re-registration** (Resolved Sep.24.01)
    *   *Remediation*: Refactored `HardwareFastPath` to allow optional callback assignment. Updated `TrackerService.kt` to omit callbacks inside the periodic tick loop (R-ID 457).
*   **Issue #1232: Empty Stub Implementation of OEM Power Hardening Overrides** (Resolved Sep.24.00)
*   **Issue #1231: Redundant Stream Observer Audit** (Resolved Sep.23.80)
*   **Issue #1194: Unified Event Logging and Action Handling** (Resolved Sep.23.01)

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 502 (Rules: 36), Resolved: 1246, Open: H:2, M:1, L:0, Ideas: H:0, M:10, L:6 Resolved:1, Testing: 3 (Sub-items: 15), QA: 284]**
