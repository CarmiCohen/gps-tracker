# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.26.11

## 🎯 Current Resumption Focus: Architectural Hardening
Ready for next priority item.

## 🔴 Open Gaps & Unfinished Integration Points

### 🟡 Medium Priority (UX, Performance & Auditability)

*   *(No immediate open gaps)*

---

## 💡 Strategic Simplification Ideas (Ideas: 17)

### 🟡 Medium Priority
*   *(Issue #1336 consolidated into hardening)*
*   *(Issue #1215 resolved: Decommission legacy ViewModels)*

---

## 🟢 Resolved Traceability & Metadata Issues

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

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 501 (Rules: 35, IDs: 501), Resolved: 1245, Open: 0, Testing: 3 (Sub-items: 15), Ideas: 17, QA: 284]**
