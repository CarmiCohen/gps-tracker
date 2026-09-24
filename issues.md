# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.24.91

## 🎯 Current Resumption Focus: Background Infrastructure Hardening
Finalizing the audit of background service stability and functional convergence after the role-isolation refactor.

## 🔴 Open Gaps & Unfinished Integration Points

### 🛑 High Priority (Safety, Data Integrity & Core Logic)

*   (None currently identified for immediate remediation)

### 🟡 Medium Priority (UX, Performance & Auditability)

*   (None currently identified)

### 🔵 Low Priority (Forensic Precision)

*   (None currently identified)

---

## 🛠️ Required Remediation & Strategic Implementation

### 🛑 High Priority

*   (None currently identified)

### 🟡 Medium Priority

*   **Issue #1261: Refactor Tracker/Viewer Services into Role-Reactive MonitorService**
    *   *Description*: Merge TrackerService and ViewerService redundant boilerplate into a unified, lightweight, role-reactive background service driven by active mode flow changes.
    *   *Contribution*: **Medium (Maintainability)**. Consolidates 400+ lines of redundant boilerplate.
*   **Issue #1265: Unified Event Orchestration via AppEventCoordinator**
    *   *Description*: Centralize independent alert triggers, audio synthesizer calls, and forensic logs into a single high-cohesion coordinator to eliminate cross-component lifecycle dependencies.
    *   *Contribution*: **Medium (Architecture)**. Decouples domain logic from service lifecycles.

### 🔵 Low Priority

*   (None currently identified)

---

## 💡 Strategic Simplification Ideas (Ideas: 21)

### 🛑 High Priority
*   **Issue #1292: Reactive Siren State Binding**
    *   *Significance*: **High (Architecture)**. Move siren lifecycle orchestration into a dedicated coordinator to remove imperative calls from core evaluation logic.
*   **Issue #1291: Domain Event Bus Integration**
    *   *Significance*: **High (Architecture)**. Centralize dispersed logging and triggers into a single `AppEventCoordinator` to eliminate cross-component coupling.

### 🟡 Medium Priority
*   **Issue #1311: AppAlarmManager Stateless Evaluation Model**
    *   *Significance*: **Medium (Architecture)**. Transition `AppAlarmManager` away from intermediate memory state persistence maps towards purely stateless snapshot calculations, further mitigating multi-role transition leakage vectors.
*   **Issue #1310: Stream Orchestration Boilerplate Reduction**
    *   *Significance*: **Medium (Maintainability)**. Tracker and Viewer services share identical patterns for observing alarm, integrity, processor, connectivity, and history event streams. Consolidating these into a shared `ServiceObservationDelegate` or a unified event handler would eliminate 100+ lines of boilerplate.
*   **Issue #1309: Unified Job Management in Monitor Services**
    *   *Significance*: **Medium (Maintainability)**. Tracker and Viewer services manually manage 5-10 nullable Job variables. Migrating to a structured `JobRegistry` would simplify lifecycle management and reduce boilerplate cancellation logic.
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
*   **Issue #1231: Redundant Stream Overlap & Duplicate Heartbeat Processing in ViewerService** (Resolved Sep.23.80)
*   **Issue #1250: Build Vitality & Reactive Stream Convergence** (Resolved Sep.23.72)
*   **Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption** (Resolved Sep.23.70)
*   **Issue #1240: Role-Based Namespace Isolation for Logic State Persistence** (Resolved Sep.23.70)
*   **Issue #1236: Race Conditions and Premature Tick Execution** (Resolved Sep.23.70)
*   **Issue #1270: Disconnected Siren Trigger Mechanism** (Resolved Sep.23.60)
*   **Issue #1280: Integrate Siren Trigger into Alarm Evaluation Loop** (Resolved Sep.23.60)
*   **Issue #1203: Hilt ViewModel Scope Optimization** (Resolved Sep.23.50)
*   **Issue #1204: Unified Hardware Lifecycle & Vendor Hardening** (Resolved Sep.23.08)
*   **Issue #1164: Persistence of Logic State** (Resolved Sep.23.06)
*   **Issue #1200: Shared Overlay Scope** (Resolved Sep.23.04)
*   **Issue #1192: Disconnected Settings Input State Flow** (Resolved Sep.23.03)
*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion** (Resolved Sep.23.01)
*   **Issue #1194: Unified Event Logging and Action Handling** (Resolved Sep.23.01)

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 470 (Rules: 92, IDs: 470), Resolved: 1213, Open: 5, Testing: 3 (Sub-items: 12), Ideas: 21, QA: 284]**
