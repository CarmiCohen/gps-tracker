# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.26.5

## 🎯 Current Resumption Focus: Architectural Hardening
Ready for next priority item.

## 🔴 Open Gaps & Unfinished Integration Points

### 🟡 Medium Priority (UX, Performance & Auditability)

*   *(No immediate open gaps)*

---

## 💡 Strategic Simplification Ideas (Ideas: 17)

### 🟡 Medium Priority
*   *(Issue #1336 consolidated into hardening)*

---

## 🟢 Resolved Traceability & Metadata Issues

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
    *   *Remediation*: Fully centralized telemetry data transformation in `TelemetryMapper`. (SOT ID 486).
*   **Issue #1330: Snap-to-Update Monolith** (Resolved Sep.25.06)
    *   *Remediation*: Unified `SystemEvaluationSnapshot` with the partitioned state structure. (SOT ID 485).
*   **Issue #1327: Pulse-to-Tick Event Collision** (Resolved Sep.25.05)
    *   *Remediation*: Introduced `DomainEvent.PeerConnectionChanged` to handle lifecycle-only notifications. (SOT ID 484).
*   **Issue #1324: Peer Signaling Coupling to Repository** (Resolved Sep.25.04)
    *   *Remediation*: Transitioned peer telemetry persistence to a reactive, bus-driven model. (SOT ID 483).
*   **Issue #1323: Residual Imperative Persistence in MonitorService** (Resolved Sep.25.03)
    *   *Remediation*: Converged Viewer self-tracking persistence into the unified `DomainEventBus`. (SOT ID 482).
*   **Issue #1331: DomainEventBus Capacity Hardening** (Resolved Sep.25.02)
    *   *Remediation*: Increased extra buffer capacity to 128 and introduced DROP_OLDEST overflow policy. (SOT ID 481).
*   **Issue #1322: Multi-Flow Fragmentation Convergence** (Resolved Sep.25.01)
    *   *Remediation*: Converged all component-level event streams into the unified bus. (SOT ID 480).
*   **Issue #1325: Unified Snapshot Metadata Gaps** (Resolved Sep.25.00)
    *   *Remediation*: Expanded `SystemEvaluationSnapshot` for full parity. (SOT ID 478).
*   **Issue #1326: Telemetry Data Corruption in LocationProcessor** (Resolved Sep.25.00)
    *   *Remediation*: Corrected satellite count mapping. (SOT ID 479).

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 492 (Rules: 29, IDs: 492), Resolved: 1236, Open: 0, Testing: 3 (Sub-items: 15), Ideas: 17, QA: 284]**
