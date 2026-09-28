# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.28.3

## 🎯 Current Resumption Focus: Architectural Hardening
Unified Activity Context Provider integrated. Build-time interface validation active.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *(No high-priority gaps remain)*

---

## 💡 Strategic Simplification Ideas (Ideas: 4)

### 🟡 Medium Priority
*   *(No medium-priority ideas remain)*

### 🔵 Low Priority
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Significance*: **Low-Medium (UI Decoupling)**. Extract osmdroid management into a standalone controller to keep UI code declarative.
*   **Issue #1296: Centralized Boot Lifecycle Authority**
    *   *Significance*: **Low (Testability)**. Move monotonic clock recovery logic to a dedicated authority to simplify background service testing.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.
*   **Issue #1354: Gradle Task Deduplication**
    *   *Significance*: **Low (Build Speed)**. Consolidate custom verification tasks into a single convention plugin to reduce configuration time.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1294: Build-Time Interface Validation** (Resolved Sep.28.3)
*   **Issue #1353: Unified Activity Context Provider** (Resolved Sep.28.2)
*   **Issue #1205: Context-Aware Power Optimization** (Resolved Sep.28.1)
*   **Issue #1160: Flyweight & Pooling Expansion** (Resolved Sep.27.17)
*   **Issue #1173: Protobuf-First Persistence** (Resolved Sep.27.16)
*   **Issue #1352: Unified Service Job Orchestration** (Resolved Sep.27.15)
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator** (Resolved Sep.27.14)
*   **Issue #1351: StateSubscription Coroutine Scoping** (Resolved Sep.27.13)
*   **Issue #1350: Unified State Mapping Authority** (Resolved Sep.27.12)
*   **Issue #1202: UI Event Routing Unification** (Resolved Sep.27.11)
*   **Issue #1201: Reactive Siren Lockout** (Resolved Sep.27.10)
*   **Issue #1290: UI State Mapper Consolidation** (Resolved Sep.27.9)
*   **Issue #1346: Physical Device Soak Test (24-Hour Observation)** (Resolved Sep.27.8)
*   **Issue #1172: Smart Signaling Dispatcher** (Resolved Sep.27.7)
*   **Issue #1161: Unified Trajectory & Buffer Management** (Resolved Sep.27.6)
*   **Issue #1349: LocationProcessingState Mutability Reduction** (Resolved Sep.27.5)
*   **Issue #1348: DomainEvent Hierarchy Simplification** (Resolved Sep.27.4)
*   **Issue #1347: Audit and Itemize Strategic Simplification Candidates** (Resolved Sep.27.3)
*   **Issue #1345: Expand Automated Network Stress Tests** (Resolved Sep.27.2)
*   **Issue #1344: Implement Thermal & Memory Forensic Probes** (Resolved Sep.26.12)

---

## 📊 Hardening Progress Dashboard
- **Sep.28.3: [SOT Count: 182 (Rules: 52), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.28.2: [SOT Count: 181 (Rules: 51), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:3, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.28.1: [SOT Count: 180 (Rules: 50), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:3, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.18: [SOT Count: 180 (Rules: 50), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:3, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.17: [SOT Count: 179 (Rules: 49), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:3, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.9: [SOT Count: 171 (Rules: 41), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
