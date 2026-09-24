# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.24.04

## 🎯 Current Resumption Focus: Background Infrastructure Hardening
Finalizing the audit of background service stability and functional convergence after the role-isolation refactor.

## 🔴 Open Gaps & Unfinished Integration Points

### 🛑 High Priority (Safety, Data Integrity & Core Logic)

*   **Issue #1256: Monotonic Latch Staleness Across Reboots**
    *   *Description*: Persistence of siren cooldowns and violation timers using `elapsedRealtime` causes them to remain valid across reboots. Since `nowRt` resets to 0, old high-value latches can silence sirens for days.
    *   *Contribution*: **Critical (Safety)**. Prevents the "Permanent Muzzle" bug where safety features fail to trigger after a device restart.
*   **Issue #1302: Redundant and Misaligned LocationProcessor in ViewerService**
    *   *Finding*: `ViewerService` inherits an injected `LocationProcessor` from `BaseMonitorService` but also instantiates `selfProcessor` and `remoteProcessor`. The injected instance is updated with sensor data in `processTick`, while `selfProcessor` (used for local Viewer tracking) is not. This results in the Viewer role having degraded motion awareness and incorrect stationary detection for its own device.
    *   *Contribution*: **High (Core Logic)**. Restores proper physical awareness and role fidelity to the Viewer role.
*   **Issue #1307: Forensic Sampling Bottleneck During Rapid Event Sequences**
    *   *Finding*: The `forensicSamplingLoop` in `TrackerService` uses a conflated channel and an internal `delay(delayMs)`. If multiple physical spikes (acoustic/light) occur rapidly, the loop will be stuck in a delay from the first trigger, causing subsequent high-priority triggers to be dropped. Forensic captures should be decoupled from the sampling rate delay during spike events.
    *   *Contribution*: **High (Forensic Integrity)**. Ensures critical evidence (acoustic/light spikes) is never dropped during a theft event.
*   **Issue #1301: Missing Persistence for Lux and Acoustic Baselines** (Gap in #1271)
    *   *Finding*: Issue #1271 implemented persistence for the vibration floor, but omitted Lux and Acoustic baselines. These values reset to defaults on every service restart, leading to a "learning period" where false-positive tamper alerts are highly likely until the environment stabilizes again.
    *   *Contribution*: **High (Alert Quality)**. Eliminates false-positive tamper alerts during the first 60 seconds of service startup.
*   **Issue #1303: Cross-Role HardwareSuite Sensitivity Contamination** (Risk in #1230 / #1271)
    *   *Finding*: `ViewerService.onServiceInitialize` restores the remote tracker's vibration floor from `V_ADAPTIVE_VIBRATION_FLOOR_KEY` and applies it to the singleton `HardwareSuite.setAdaptiveVibrationFloor()`. This incorrectly forces the Viewer device to use the Tracker's physical sensitivity anchor, corrupting local motion detection.
    *   *Contribution*: **High (Safety Isolation)**. Ensures the physical profile of one device does not degrade the sensing accuracy of the other.
*   **Issue #1304: Peer Stat Reset Logic Corrupts Local Tracker State** (Inconsistency in #1230)
    *   *Finding*: `ConnectivitySuite.resetPeerStats()` uses the local role prefix (`T_` or `V_`) to clear baselines. In Tracker mode (`T_`), this causes the service to wipe its *own* Lux and Acoustic baselines on every disconnect/stop, negating the benefit of persistence and causing sensitivity resets.
    *   *Contribution*: **High (Data Integrity)**. Guarantees that environmental calibration survives network drops.

### 🟡 Medium Priority (UX, Performance & Auditability)

*   **Issue #1235: Synchronous Main-Thread runBlocking Invocation in Service Shutdown**
    *   *Description*: `BaseMonitorService.onDestroy()` employs a synchronous `runBlocking` block to flush volatile history frames to the database on the primary thread during termination. Under intense storage pressure or heavy database lock contention, this blocks service teardown long enough to provoke OS watchdog thread starvation kills or generic ANR alerts.
    *   *Contribution*: **Medium (Lifecycle Reliability)**. Prevents "Dirty Shutdowns" and potential database corruption during app termination.
*   **Issue #1308: Missing Forensics Trace Collection in ViewerService**
    *   *Finding*: While `ViewerService` uses the `ForensicAuditor` for stability checks, it lacks the `forensicSamplingLoop` present in `TrackerService`. This prevents detailed forensic logging of the Viewer's environment, which is necessary for auditing monitoring integrity and potential local tampering.
    *   *Contribution*: **Medium (Audit Parity)**. Ensures both ends of the connection provide equal forensic traceability.
*   **Issue #1272: Alarm Notification Leak in Tracker Mode**
    *   *Description*: `AppNotificationManager` guards against showing full-screen intents in Tracker mode, but `AppAlarmManager` continues to persist alarm states. If a user flips from Tracker to Viewer while a background alarm is technically active, the siren triggers immediately, causing unexpected user distress.
    *   *Contribution*: **Medium (UX Polish)**. Prevents unexpected "Siren Jumps" during role transitions.
*   **Issue #1305: Performance Risk: Synchronous Repository Writes on Vibration Floor Jitter** (Risk in #1271)
    *   *Finding*: `LocationProcessor` emits a `VibrationFloorChanged` event for every drift >0.01g, which triggers a `saveDoubleSync` in the repository. On high-vibration environments, this can lead to excessive synchronous I/O on the service thread, potentially causing tick-loop jitter.
    *   *Contribution*: **Medium (Performance)**. Smooths out CPU usage and prevents "UI stutter" during intense physical monitoring.
*   **Issue #1306: Namespace Collision Risk for Viewer's Self-Tracking** (Inconsistency in #1230)
    *   *Finding*: In `ViewerService`, the `V_` prefix is used for both the Viewer's session state (ticks) and the remote tracker's logic state (accuracy, baselines). This lack of separation prevents independent auditing of the Viewer's own device performance versus the remote tracker's performance.
    *   *Contribution*: **Medium (Auditability)**. Segregates local telemetry from remote telemetry for cleaner post-mortem analysis.

### 🔵 Low Priority (Forensic Precision)

*   **Issue #1234: Faulty Latency Calculations inside Thermal Recovery Audits**
    *   *Description*: The logic inside `TrackerService.kt`'s `startForensicSamplingLoop()` designed to compute the `Thermal Recovery Latency` metric when exiting cooling mode is fundamentally flawed. It captures the timestamp transition correctly but logs the delta on the immediate subsequent loop iteration, measuring only the duration of a single loop iteration delay (`delayMs`) rather than the true elapsed interval required for device sensor stabilization.
    *   *Contribution*: **Low (Forensic Accuracy)**. Minor fix for performance reporting metrics.

---

## 🛠️ Required Remediation & Strategic Implementation

### 🛑 High Priority

*   **Issue #1260: Boot-ID Validation for Persistent Monotonic Latches**
    *   *Description*: Implement Boot-ID validation checks upon restoring persistent elapsedRealtime latches to detect device reboots and safely invalidate or adjust obsolete temporal locks. Resolves Issue #1256.
    *   *Contribution*: **High (Safety)**. Critical fix for alarm reliability after restart.
*   **Issue #1241: Functional Restoration of History Sync Streams in ViewerService**
    *   *Description*: Refactor the `observeHistoryEvents()` implementation in `ViewerService.kt` to subscribe to legitimate history backfill and sync event streams rather than mirroring the connectivity pulse listener. Resolves logic duplication and observation gaps identified in Issue #1231.
    *   *Contribution*: **High (Functional Parity)**. Ensures history data is correctly visualized on the monitor device.

### 🟡 Medium Priority

*   **Issue #1245: Non-Blocking History Flush on Service Termination**
    *   *Description*: Transition the final history buffer flush in `BaseMonitorService.onDestroy()` from a synchronous `runBlocking` call to a structured teardown coroutine with a strict timeout, or delegate to a WorkManager task to prevent shutdown ANRs. Resolves Issue #1235.
    *   *Contribution*: **Medium (Stability)**. Prevents process-teardown hangs.
*   **Issue #1261: Refactor Tracker/Viewer Services into Role-Reactive MonitorService**
    *   *Description*: Merge TrackerService and ViewerService redundant boilerplate into a unified, lightweight, role-reactive background service driven by active mode flow changes.
    *   *Contribution*: **Medium (Maintainability)**. Consolidates 400+ lines of redundant boilerplate.
*   **Issue #1265: Unified Event Orchestration via AppEventCoordinator**
    *   *Description*: Centralize independent alert triggers, audio synthesizer calls, and forensic logs into a single high-cohesion coordinator to eliminate cross-component lifecycle dependencies.
    *   *Contribution*: **Medium (Architecture)**. Decouples domain logic from service lifecycles.

### 🔵 Low Priority

*   **Issue #1244: Heuristic Correction for Thermal Recovery Audits**
    *   *Description*: Correct the latency measurement logic in `TrackerService.kt` to measure the true elapsed duration between entering and exiting cooling mode rather than measuring individual loop iteration delays. Resolves audit reliability issues in Issue #1234.
    *   *Contribution*: **Low (Accuracy)**. Corrects performance audit logs.

---

## 💡 Strategic Simplification Ideas (Ideas: 18)

### 🛑 High Priority
*   **Issue #1292: Reactive Siren State Binding**
    *   *Significance*: **High (Architecture)**. Move siren lifecycle orchestration into a dedicated coordinator to remove imperative calls from core evaluation logic.
*   **Issue #1291: Domain Event Bus Integration**
    *   *Significance*: **High (Architecture)**. Centralize dispersed logging and triggers into a single `AppEventCoordinator` to eliminate cross-component coupling.

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

*   **Issue #1273: Atomic User Counter Risk in HardwareSuite** (Resolved Sep.24.04)
    *   *Remediation*: Guarded the `activeUsers` AtomicInteger in `HardwareSuite.stop()` to prevent it from falling into negative values. Hardened the deferred teardown check to use `<= 0` (R-ID 460).
*   **Issue #1271: Missing Persistence for Adaptive Vibration Floor** (Resolved Sep.24.04)
    *   *Remediation*: Implemented persistence for the adaptive vibration floor anchor. Added `ADAPTIVE_VIBRATION_FLOOR_KEY` to `PreferenceKeys.kt` (R-ID 459).
*   **Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots** (Resolved Sep.24.02)
    *   *Remediation*: Implemented `recoverLastRealtime` in `HistoryManager.kt` using role-prefixed clock drift references (R-ID 458).
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
- **Current Audit Baseline: [SOT: 460 (Rules: 92, IDs: 460), Resolved: 1200, Open: 18, Testing: 3 (Sub-items: 12), Ideas: 18, QA: 284]**
