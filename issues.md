# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.25.04

## 🎯 Current Resumption Focus: Pulse-to-Tick Event Collision
Finalizing the decoupling of Peer Pulse from the core evaluation loop.

## 🔴 Open Gaps & Unfinished Integration Points

### 🟡 Medium Priority (UX, Performance & Auditability)

*   **Issue #1327: Pulse-to-Tick Event Collision**
    *   *Finding*: Pulse handlers reuse `TickEvaluated` event, triggering redundant side-effects with potentially stale data.
    *   *Requirement*: Introduce `DomainEvent.PeerConnectionChanged` for lifecycle-only notifications.

---

## 💡 Strategic Simplification Ideas (Ideas: 20)

### 🛑 High Priority
*   **Issue #1329: Telemetry Mapping Convergence**
    *   *Significance*: **High (Architecture)**. Consolidate `LocationUpdate` construction into a single `TelemetryMapper` to remove redundant mapping logic from the coordinator.

### 🟡 Medium Priority
*   **Issue #1330: Snap-to-Update Monolith**
    *   *Significance*: **Medium (Logic)**. Since `SystemEvaluationSnapshot` now carries ~95% of `LocationUpdate` fields, evaluate merging them into a single polymorphic DTO to eliminate the bridge layer.
*   **Issue #1314: TrackerStatus & Evaluation Snapshot Convergence**
    *   *Significance*: **Medium (Architecture)**. Evaluate if `TrackerStatus` DTO can be merged into `SystemEvaluationSnapshot` to eliminate the mapping layer in `ConnectivitySuite`.
*   **Issue #1332: Viewer Self-Tracking Snapshot Optimization**
    *   *Significance*: **Low (Simplicity)**. Now that Viewer persistence is bus-driven, evaluate if `ViewerLocationUpdated` can simply reuse `TickEvaluated` to further unify the event schema.

---

## 🟢 Resolved Traceability & Metadata Issues

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

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 483 (Rules: 25, IDs: 483), Resolved: 1226, Open: 1, Testing: 3 (Sub-items: 12), Ideas: 20, QA: 284]**
