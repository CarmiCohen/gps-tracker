# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.27.13

## 🎯 Current Resumption Focus: Architectural Hardening
Ready for next priority item.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *(No high-priority gaps remain)*

---

## 💡 Strategic Simplification Ideas (Ideas: 8)

### 🟡 Medium Priority
*   **Issue #1294: Build-Time Interface Validation**
    *   *Significance*: **Medium (Quality)**. Implement custom Gradle tasks to verify service implementations before compilation.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Significance*: **Medium (Performance)**. Expand flyweight patterns to all telemetry entities and use ring buffers to eliminate GC churn.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Significance*: **Medium (Performance)**. Substitute JSON mapping with pure Protobuf binary pipelines to speed up disk I/O.
*   **Issue #1205: Context-Aware Power Optimization**
    *   *Significance*: **Medium (Battery)**. Dynamically adjust sensor polling based on activity recognition to extend battery life.

### 🔵 Low Priority
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator**
    *   *Significance*: **Low-Medium (Refactoring)**. Refactor background services to use a TickOrchestrator that handles initialization gates internally.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Significance*: **Low-Medium (UI Decoupling)**. Extract osmdroid management into a standalone controller to keep UI code declarative.
*   **Issue #1296: Centralized Boot Lifecycle Authority**
    *   *Significance*: **Low (Testability)**. Move monotonic clock recovery logic to a dedicated authority to simplify background service testing.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

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
*(Earlier resolutions archived to STATUS/RESOLUTION_ARCHIVE.md)*

---

## 📊 Hardening Progress Dashboard
- **Sep.27.13: [SOT Count: 175 (Rules: 45), Open: H:0, M:0, L:0, Ideas: H:0, M:4, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.12: [SOT Count: 174 (Rules: 44), Open: H:0, M:0, L:0, Ideas: H:0, M:4, L:5, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.11: [SOT Count: 173 (Rules: 43), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:5, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.10: [SOT Count: 172 (Rules: 42), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
- **Sep.27.9: [SOT Count: 171 (Rules: 41), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
