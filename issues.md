# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.25.00

## 🎯 Current Resumption Focus: Domain Event Bus Convergence
Finalizing the decoupling of the evaluation loop and component-level reactive flows into a unified domain orchestrator.

## 🔴 Open Gaps & Unfinished Integration Points

### 🛑 High Priority (Safety, Data Integrity & Core Logic)

*   **Issue #1322: Multi-Flow Fragmentation (Issue #1291 Integration)**
    *   *Finding*: Coordinator still observes 10+ fragmented flows (`IntegrityEvent`, `ProcessorEvent`, etc.) rather than a single bus.
    *   *Requirement*: Converge component-level event streams into the unified `DomainEventBus`.
*   **Issue #1323: Residual Imperative Persistence in MonitorService**
    *   *Finding*: Viewer self-tracking bypasses the bus and calls `updateRepositoryLocation` directly.
    *   *Risk*: Asymmetric architecture and blocking I/O risk on the service thread.

### 🟡 Medium Priority (UX, Performance & Auditability)

*   **Issue #1324: Peer Signaling Coupling to Repository**
    *   *Finding*: `ConnectivitySuite` executes direct repository writes for received peer status.
    *   *Requirement*: Offload peer status persistence to `AppEventCoordinator` via `DomainEvent.PeerStatusReceived`.
*   **Issue #1327: Pulse-to-Tick Event Collision**
    *   *Finding*: Pulse handlers reuse `TickEvaluated` event, triggering redundant side-effects with potentially stale data.
    *   *Requirement*: Introduce `DomainEvent.PeerConnectionChanged` for lifecycle-only notifications.

### 🔵 Low Priority (Forensic Precision)

*   **Issue #1328: Event Bus Backpressure Risk**
    *   *Finding*: Converging 10+ sources into one flow may require a higher buffer capacity.
    *   *Requirement*: Increase `DomainEventBus` capacity and implement a drop strategy for non-critical telemetry.

---

## 💡 Strategic Simplification Ideas (Ideas: 18)

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

*   **Issue #1325: Unified Snapshot Metadata Gaps** (Resolved Sep.25.00)
    *   *Remediation*: Expanded `SystemEvaluationSnapshot` to include all metadata required for a full `LocationUpdate` (sats, proximity, violation stats). (SOT ID 478).
*   **Issue #1326: Telemetry Data Corruption in LocationProcessor** (Resolved Sep.25.00)
    *   *Remediation*: Corrected satellite count mapping to use the actual `snapshot.satsUsed` value instead of a zero-placeholder. (SOT ID 479).
*   **Issue #1291: Domain Event Bus Integration** (Resolved Sep.24.97)
*   **Issue #1312: Unified Evaluation Logic Snapshot** (Resolved Sep.24.96)
*   **Issue #1313: Role-Switching Atomic State Reset** (Resolved Sep.24.96)
*   **Issue #1163: Stateless & Functional Logic Refactoring for LocationProcessor** (Resolved Sep.24.95)

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 479 (Rules: 25, IDs: 479), Resolved: 1222, Open: 5, Testing: 3 (Sub-items: 12), Ideas: 18, QA: 284]**
