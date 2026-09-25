# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.25.01

## 🎯 Current Resumption Focus: DTO Convergence & Persistence Alignment
Finalizing the unification of snapshot and update models to eliminate redundant mapping layers.

## 🔴 Open Gaps & Unfinished Integration Points

### 🛑 High Priority (Safety, Data Integrity & Core Logic)

*   **Issue #1323: Residual Imperative Persistence in MonitorService**
    *   *Finding*: Viewer self-tracking bypasses the bus and calls `updateRepositoryLocation` directly.
    *   *Risk*: Asymmetric architecture and blocking I/O risk on the service thread.
*   **Issue #1331: DomainEventBus Capacity Hardening**
    *   *Finding*: Converging 10+ sources into one flow may require a higher buffer capacity or drop strategy. (Formerly #1328).
    *   *Requirement*: Implement `onBufferOverflow = BufferOverflow.DROP_OLDEST` and increase capacity to 128.

### 🟡 Medium Priority (UX, Performance & Auditability)

*   **Issue #1324: Peer Signaling Coupling to Repository**
    *   *Finding*: `ConnectivitySuite` executes direct repository writes for received peer status.
    *   *Requirement*: Offload peer status persistence to `AppEventCoordinator` via `DomainEvent.PeerStatusReceived`.
*   **Issue #1327: Pulse-to-Tick Event Collision**
    *   *Finding*: Pulse handlers reuse `TickEvaluated` event, triggering redundant side-effects with potentially stale data.
    *   *Requirement*: Introduce `DomainEvent.PeerConnectionChanged` for lifecycle-only notifications.

---

## 💡 Strategic Simplification Ideas (Ideas: 19)

### 🛑 High Priority
*   **Issue #1329: Telemetry Mapping Convergence**
    *   *Significance*: **High (Architecture)**. Consolidate `LocationUpdate` construction into a single `TelemetryMapper` to remove redundant mapping logic from the coordinator.

### 🟡 Medium Priority
*   **Issue #1330: Snap-to-Update Monolith**
    *   *Significance*: **Medium (Logic)**. Since `SystemEvaluationSnapshot` now carries ~95% of `LocationUpdate` fields, evaluate merging them into a single polymorphic DTO to eliminate the bridge layer.
*   **Issue #1314: TrackerStatus & Evaluation Snapshot Convergence**
    *   *Significance*: **Medium (Architecture)**. Evaluate if `TrackerStatus` DTO can be merged into `SystemEvaluationSnapshot` to eliminate the mapping layer in `ConnectivitySuite`.
*   **Issue #1161: Unified Trajectory & Buffer Management**
*   **Issue #1294: Build-Time Interface Validation**
*   **Issue #1290: UI State Mapper Consolidation**

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1322: Multi-Flow Fragmentation Convergence** (Resolved Sep.25.01)
    *   *Remediation*: Converged all component-level event streams (Alarm, Integrity, Processor, Connectivity, History, Sensor, Command, Revival) into the unified `DomainEventBus`. Migrated bus infrastructure to core engine to support core-level signaling. (SOT ID 480).
*   **Issue #1325: Unified Snapshot Metadata Gaps** (Resolved Sep.25.00)
    *   *Remediation*: Expanded `SystemEvaluationSnapshot` to include all metadata required for a full `LocationUpdate`. (SOT ID 478).
*   **Issue #1326: Telemetry Data Corruption in LocationProcessor** (Resolved Sep.25.00)
    *   *Remediation*: Corrected satellite count mapping to use the actual `snapshot.satsUsed` value. (SOT ID 479).
*   **Issue #1291: Domain Event Bus Integration** (Resolved Sep.24.97)
*   **Issue #1312: Unified Evaluation Logic Snapshot** (Resolved Sep.24.96)
*   **Issue #1313: Role-Switching Atomic State Reset** (Resolved Sep.24.96)
*   **Issue #1163: Stateless & Functional Logic Refactoring for LocationProcessor** (Resolved Sep.24.95)

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 480 (Rules: 25, IDs: 480), Resolved: 1223, Open: 4, Testing: 3 (Sub-items: 12), Ideas: 19, QA: 284]**
