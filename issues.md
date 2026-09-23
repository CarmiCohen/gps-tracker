# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.23.08

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Unintended Side Effects & Design Inconsistencies (Issue #1170 Decomposition Gaps)

*   **Issue #1210: Fragmented State Architecture & Multiple Sources of Truth**
    *   *Description*: The decomposition of `MainViewModel` into role-specific ViewModels (`TrackerViewModel`, `ViewerViewModel`, `SetupViewModel`) introduces an architectural anti-pattern where each role ViewModel creates and maintains its own private, independent instance of `_uiState = MutableStateFlow(MainUiState())`. Instead of acting as lightweight specialized extensions that consume a single unified source of truth, they duplicate the entire application state structure, leading to state fragmentation.
    *   *Significance*: **High (State Consistency)**: Divergent UI states can occur across the application layout since there is no centralized state synchronization between the role ViewModels and the parent coordinator.

*   **Issue #1211: Redundant Stream Subscriptions & Resource Churn**
    *   *Description*: Because each role ViewModel instantiates a full standalone `MainUiState` pipeline, they all independently invoke `stateSubscriptionUseCase.observeRepositorySettings()`, `observeInternetStatus()`, and `observeIntegrityUpdates()`. When ViewModels are scoped to the navigation backstack entry via `hiltViewModel()`, entering and exiting screens triggers continuous creation and destruction of these duplicate reactive streams, causing high coroutine allocation churn and unnecessary CPU/memory overhead.
    *   *Significance*: **Medium-High (Resource Efficiency)**: Unnecessary background collection runs multiply per active role ViewModel, degrading battery and CPU performance.

*   **Issue #1212: Ephemeral Map State Loss due to Navigation Backstack Entry Scoping**
    *   *Description*: Map interactions and view states (such as `isFenceVisible`, `isMapLocked`, `mapFollowMode`, `isViolationsVisible`) are captured and processed purely within the local `_uiState` of `TrackerViewModel` and `ViewerViewModel`. Since these ViewModels are scoped to individual navigation graph destinations, navigating away (e.g., returning to the Landing screen or switching roles) destroys the ViewModel instance. As a result, all user-configured map states are completely lost and reset to defaults upon re-entry.
    *   *Significance*: **High (User Experience & UX Polish)**: Map configuration state is completely ephemeral, leading to an inconsistent and frustrating user experience during role transitions.

*   **Issue #1213: Asymmetric and Swallowed Settings Draft Events**
    *   *Description*: `MainViewModel` retains the exclusive responsibility for managing and committing configuration draft states (`UiEvent.UpdateDraftDeviceId`, `UiEvent.CommitSettings`). However, the `onEvent` entry points of `TrackerViewModel` and `ViewerViewModel` do not handle or delegate these draft configuration events, causing them to be silently swallowed if dispatched within the scope of a feature screen.
    *   *Significance*: **Medium (Functional Correctness)**: Breaks uniform configuration updates if components on a role-specific screen trigger draft updates that target the unhandled local handler.

*   **Issue #1214: Asynchronous Hydration and Visual Race Conditions**
    *   *Description*: `MainViewModel` orchestrates the system hydration sequence through `LifecycleHydrationManager`. The role-specific ViewModels independently listen to the hydration flow and update their local states. Because these updates occur on asynchronous coroutine dispatchers across detached ViewModel instances, a race condition exists where sub-components (like `AppMapContainer` or `HeaderBar`) read conflicting hydration levels, potentially leading to partial, stuttered, or completely broken UI composition blocks during rapid role transitions.
    *   *Significance*: **Medium (UI Rendering Reliability)**: Causes transient glitches or layout flickering when transitioning between modes or cold-starting specialized screens.

*   **Issue #1215: Scope Overlap & Instance Multiplication**
    *   *Description*: `SetupViewModel` is instantiated using `hiltViewModel()` inside `MainAppContent`, which effectively scopes it to the `MainActivity`. Since `MainViewModel` is also scoped to `MainActivity`, the application now maintains two distinct ViewModels for the same lifecycle scope, both observing the same underlying data sources. This leads to redundant memory usage and dual-event processing for global overlays.
    *   *Significance*: **Medium (Architectural Cleanliness)**: Violates the principle of a single ViewModel per scope for related functionality.

*   **Issue #1257: Misrouted Kinematic State in Role Screens**
    *   *Description*: `MainAppContent.kt` passes `kinematicState` collected from `MainViewModel` to `TrackerScreen` and `ViewerScreen`. However, `MainViewModel` does not observe or update its `kinematicState` from local location updates—only `TrackerViewModel` and `ViewerViewModel` do this. Consequently, components like the `TrackerDashboard` receive an empty or zeroed `KinematicState`, breaking local coordinate and speed displays on role-specific screens.
    *   *Significance*: **High (Visual & Functional Correctness)**.

### Background Service Infrastructure & Hardening Gaps (Rigorous Audit of #1171 & Service Overlays)

*   **Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption**
    *   *Description*: Both `TrackerService` and `ViewerService` load and save logic states using identical database/preferences storage keys (`MAX_ACCURACY_KEY`, `LAST_SIT_TS_KEY`, `CHAIR_BASELINE_TILT_KEY`). When running in `Viewer` mode, the app overwrites these local keys with values derived from the *remote* tracker's telemetry stream. This completely clobbers the local device's calibrated hardware baseline profile if the application is subsequently flipped to `Tracker` mode.
    *   *Significance*: **High (Data Integrity & Baseline Degradation)**.

*   **Issue #1231: Redundant Stream Overlap & Duplicate Heartbeat Processing in ViewerService**
    *   *Description*: `ViewerService.kt` contains a copy-paste duplication error where `observeHistoryEvents()` sets up an identical subscription as `observeConnectivityEvents()`. Both blocks launch distinct coroutines that listen to `connectivitySuite.connectivityEvents` and invoke `handleTrackerPulse(event.id)` simultaneously. This causes every single tracker pulse event to process twice, producing racing overhead and dual log entries, while completely failing to observe actual history backfill sync events.
    *   *Significance*: **Medium-High (Logic Bug & Telemetry Churn)**.

*   **Issue #1232: Empty Stub Implementation of OEM Power Hardening Overrides**
    *   *Description*: `DeviceHardeningStrategy.kt` contains purely cosmetic `if` checks for `isHuaweiDevice`, `isSamsungDevice`, and `hasBackgroundRestriction` inside `executeVendorContinuity()`. It merely produces empty diagnostic log messages via `Timber.d` instead of applying actual background stay-alive adaptation rules or intent alarm intervals, leaving background tasks completely vulnerable to aggressive OEM task-killing systems.
    *   *Significance*: **High (Stay-Alive Reliability Risk)**.

*   **Issue #1233: High Allocation Churn & GC Pressure via Fast-Path Re-registration**
    *   *Description*: In `TrackerService.kt`, `hardwareSuite.setLightFastPath` and `setAcousticFastPath` are called inside `processTick()` on *every single service tick loop* (every 1-2 seconds). This forces the continuous instantiation of lambda callback objects and re-binding of hardware suite listeners rather than reusing a single long-lived observer reference, generating considerable garbage collection pressure under prolonged field operation.
    *   *Significance*: **Medium (Performance & CPU Tuning)**.

*   **Issue #1234: Faulty Latency Calculations inside Thermal Recovery Audits**
    *   *Description*: The logic inside `TrackerService.kt`'s `startForensicSamplingLoop()` designed to compute the `Thermal Recovery Latency` metric when exiting cooling mode is fundamentally flawed. It captures the timestamp transition correctly but logs the delta on the immediate subsequent loop iteration, measuring only the duration of a single loop iteration delay (`delayMs`) rather than the true elapsed interval required for device sensor stabilization.
    *   *Significance*: **Low-Medium (Audit Reliability)**.

*   **Issue #1235: Synchronous Main-Thread runBlocking Invocation in Service Shutdown**
    *   *Description*: `BaseMonitorService.onDestroy()` employs a synchronous `runBlocking` block to flush volatile history frames to the database on the primary thread during termination. Under intense storage pressure or heavy database lock contention, this blocks service teardown long enough to provoke OS watchdog thread starvation kills or generic ANR alerts.
    *   *Significance*: **Medium (Lifecycle Reliability)**.

*   **Issue #1236: Race Conditions and Premature Tick Execution during Asynchronous Initialization**
    *   *Description*: `BaseMonitorService.onCreate()` handles core engine initialization asynchronously by kicking off `onServiceInitialize()` in a background coroutine launch. However, network components like `connectivitySuite` remain capable of receiving incoming remote pulse events immediately. If a heartbeat packet arrives before `onServiceInitialize()` completes, the un-synchronized event triggers `startTickLoop()`, invoking `processTick()` on half-initialized or completely empty location processor and alarm manager components.
    *   *Significance*: **High (State Initialization Safety)**.

*   **Issue #1255: Unreliable Monotonic Clock Recovery Across Reboots**
    *   *Description*: `BaseMonitorService` attempts to recover monotonic `lastServiceTickRealtime` after process death using a `CLOCK_DRIFT_REF_KEY` (Wall - Rt). This drift reference is only valid within a single boot cycle. If the device reboots, the drift value becomes invalid, causing `nowRt - lastServiceTickRealtime` to produce massive negative or positive errors in heartbeat and staleness calculations.
    *   *Significance*: **Medium-High (Temporal Forensic Integrity)**.

*   **Issue #1256: Monotonic Latch Staleness Across Reboots**
    *   *Description*: Issue #1164 introduced persistence for siren cooldowns and violation timers (`lastSirenStopRt`, `firstViolationRt`) using `elapsedRealtime`. These monotonic timestamps are stored in DataStore and restored across process restarts. If a device reboots, `nowRt` resets to 0, but the restored latches retain high values from the previous boot, potentially causing components like the `AudioSynthesizer` to permanently silence the siren for days until the device uptime reaches the old boot's timestamp.
    *   *Significance*: **High (Alarm Reliability & Safety Side Effect)**.

### Missing Functionality & Core Integration Gaps (Identified Resolved Item Failures)

*   **Issue #1270: Disconnected Siren Trigger Mechanism**
    *   *Description*: While `AppAlarmManager` tracks unresolved alarms and defines `shouldPlaySiren()`, there is no consumer for this logic in the background services. `TrackerService` and `ViewerService` observe alarm events only for logging purposes. As a result, the physical siren will never sound during an actual theft or geofence violation.
    *   *Significance*: **Blocker (Core Functional Failure)**.

*   **Issue #1271: Missing Persistence for Adaptive Vibration Floor**
    *   *Description*: The `adaptiveVibrationFloor` (calibrated to environmental noise) is not included in `LocationProcessor.loadState()` or DataStore persistence. Upon service restart, the floor resets to `INITIAL_VIBRATION_FLOOR` (0.05G), causing false-positive "Vibration Suspicion" alarms in high-vibration environments until re-adaptation completes.
    *   *Significance*: **High (False Positive Risk)**.

*   **Issue #1272: Alarm Notification Leak in Tracker Mode**
    *   *Description*: `AppNotificationManager` guards against showing full-screen intents in Tracker mode, but `AppAlarmManager` continues to persist alarm states. If a user flips from Tracker to Viewer while a background alarm is technically active, the siren triggers immediately, causing unexpected user distress.
    *   *Significance*: **Medium (UX Side Effect)**.

*   **Issue #1273: Atomic User Counter Risk in HardwareSuite**
    *   *Description*: `HardwareSuite.kt` uses an `AtomicInteger` to track active users. The `decrementAndGet()` call is not guarded. If `stop()` is called more times than `start()` due to lifecycle races, the counter goes negative, permanently preventing hardware shutdown.
    *   *Significance*: **Medium (Resource Leak)**.

*   **Issue #1250: Missing AppEventCoordinator Domain Component**
    *   *Description*: Remediation for Issue #1194 claims consolidation into `AppEventCoordinator`. However, this component does not exist in source; logic remains dispersed between `LogManager`, `CommandRouter`, and `AlertUseCase`.
    *   *Significance*: **Medium (Traceability Gap)**.

*   **Issue #1251: Service Consolidation Gap (Unfinished #1171)**
    *   *Description*: `TrackerService` and `ViewerService` remain distinct redundant classes with duplicated boilerplate. Full resolution of #1171 requires merging these into a single role-reactive `MonitorService`.
    *   *Significance*: **Low-Medium (Structural Simplicity)**.

### Required Remediation & Strategic Implementation (Strategic Recommendations for Resolution)

#### For Issue #1170 (ViewModel Decomposition Hardening)
*   **Issue #1220: Missing State Hoisting for Unified Role ViewModels** (High)
*   **Issue #1221: Absence of Shared NavGraph Scoping for Persistent State** (High)
*   **Issue #1222: Incomplete Event Delegation Routing** (Medium)
*   **Issue #1223: Unconsolidated Trail and Sensor Mapping Logic** (Medium)

#### For Issue #1171 (Service & Background Hardening)
*   **Issue #1240: Role-Based Namespace Isolation for Logic State Persistence** (High)
*   **Issue #1241: Functional Restoration of History Sync Streams in ViewerService** (High)
*   **Issue #1242: Operationalization of OEM Power Hardening Logic** (High)
*   **Issue #1243: Fast-Path Binding Lifecycle Optimization** (Medium)
*   **Issue #1244: Heuristic Correction for Thermal Recovery Audits** (Low-Medium)
*   **Issue #1245: Non-Blocking History Flush on Service Termination** (Medium)
*   **Issue #1246: Critical Section Enforcement during Service Initialization** (High)
*   **Issue #1260: Boot-ID Validation for Persistent Monotonic Latches** (High)
*   **Issue #1261: Refactor Tracker/Viewer Services into Role-Reactive MonitorService** (Medium)

#### For Alarm & Siren Reliability (Core System Restoration)
*   **Issue #1280: Integrate Siren Trigger into Alarm Evaluation Loop**
    *   *Description*: Implement an observer in the monitoring services (or a shared coordinator) that consumes `AppAlarmManager.shouldPlaySiren()` and triggers `AudioSynthesizer.playSiren()` on every logic tick while violations are active.
    *   *Significance*: **Blocker (Restores System Purpose)**.
*   **Issue #1281: Implementation of Persistent Adaptive Vibration Floor**
    *   *Description*: Expand `AppSettings.proto` and `MainRepository` to save/restore the calibrated vibration floor to prevent false alarms on service restart.
    *   *Significance*: **High (Removes False Positives)**.
*   **Issue #1282: Atomic Guard for HardwareSuite User Counter**
    *   *Description*: Enforce a `max(0, ...)` floor on the `activeUsers` atomic counter in `HardwareSuite.stop()` to prevent negative underflow and resource leakage.
    *   *Significance*: **Medium (Lifecycle Stability)**.
*   **Issue #1265: Unified Event Orchestration via AppEventCoordinator** (Medium)

---

## 💡 Strategic Simplification Ideas (Ideas: 12)

*   **Issue #1161: Unified Trajectory & Buffer Management** (Medium-High)
*   **Issue #1203: Hilt ViewModel Scope Optimization** (Medium-High)
*   **Issue #1172: Smart Signaling Dispatcher** (Medium)
*   **Issue #1163: Stateless & Functional Logic Refactoring** (Medium)
*   **Issue #1160: Flyweight & Pooling Expansion** (Medium)
*   **Issue #1173: Protobuf-First Persistence** (Medium)
*   **Issue #1205: Context-Aware Power Optimization** (Medium)
*   **Issue #1201: Reactive Siren Lockout** (Medium)
*   **Issue #1202: UI Event Routing Unification** (Medium)
*   **Issue #1167: Map Overlay Imperative to Declarative Controller** (Low-Medium)
*   **Issue #1171: Service & Worker Consolidation** (Low)
*   **Issue #1175: Real-time Only Path (Pivot Option)** (Strategic)

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1204: Unified Hardware Lifecycle & Vendor Hardening** (Resolved Sep.23.08)
*   **Issue #1164: Persistence of Logic State** (Resolved Sep.23.06)
*   **Issue #1200: Shared Overlay Scope** (Resolved Sep.23.04)
*   **Issue #1192: Disconnected Settings Input State Flow** (Resolved Sep.23.03)
*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion** (Resolved Sep.23.01)
*   **Issue #1194: Unified Event Logging and Action Handling** (Resolved Sep.23.01)

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 421 (Rules: 85, IDs: 421), Resolved: 1177, Open: 35, Testing: 3 (Sub-items: 12), Ideas: 12, QA: 283]**
