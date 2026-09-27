# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.27.4

## 🎯 Current Resumption Focus: Architectural Hardening
Ready for next priority item.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)

*   **Issue #1346: Physical Device Soak Test (24-Hour Observation)**
    *   *Description*: Initiate a real-world validation test on target hardware (e.g., Samsung A15) by running Tracker Mode for 24 continuous hours.
    *   *Verification*: Periodically audit stability metrics using `adb logcat -s ForensicAuditor MonitorService | grep "STABILITY AUDIT"`.
    *   *Success Criteria*: Reliability index remains > 98.0% (R500) with zero "Davey" frame drops exceeding 700ms.

---

## 💡 Strategic Simplification Ideas (Ideas: 14)

### 🔴 High Priority
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Significance*: **High (Performance)**. Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer`.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Significance*: **High (Network)**. Merge conflation and throttling logic into a reactive "Smart Dispatcher" to handle connection freshness.

### 🟡 Medium Priority
*   **Issue #1294: Build-Time Interface Validation**
    *   *Significance*: **Medium (Quality)**. Implement custom Gradle tasks to verify service implementations before compilation.
*   **Issue #1290: UI State Mapper Consolidation**
    *   *Significance*: **Medium (Maintainability)**. Merge `UiStateMapper` logic directly into `MainViewModel` to reduce DI surface area.
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
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.
*   **Issue #1349: LocationProcessingState Mutability Reduction**
    *   *Significance*: **Low (Robustness)**. Further reduce fields in `LocationProcessingState` by extracting transient telemetry counters into localized sub-states.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1348: DomainEvent Hierarchy Simplification** (Resolved Sep.27.4)
*   **Issue #1347: Audit and Itemize Strategic Simplification Candidates** (Resolved Sep.27.3)
*   **Issue #1345: Expand Automated Network Stress Tests** (Resolved Sep.27.2)
*   **Issue #1344: Implement Thermal & Memory Forensic Probes** (Resolved Sep.26.12)
*(Earlier resolutions archived to STATUS/RESOLUTION_ARCHIVE.md)*

---

## 📊 Hardening Progress Dashboard
- **Sep.27.4: [SOT: 503 (Rules: 40), Resolved: 1249, Open: H:1, M:0, L:0, Ideas: H:2, M:7, L:5, Testing: 3 (Sub-items: 15), QA: 284]**
