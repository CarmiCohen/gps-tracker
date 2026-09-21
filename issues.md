# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*(No critical gaps identified. Refactoring of forensic history is complete).*

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 💡 Strategic Simplification Ideas (Ideas: 15)

*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Description*: Expand flyweight patterns to all entities (Telemetry, Violations, SpatialPoints) and use pre-allocated ring buffers for `EngineConnectionPoint` and a `LogEntry` pool to eliminate GC churn.
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Description*: Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer` and consolidate "Parking Anchor" and "Home Point" logic.
*   **Issue #1162: Forensic & Sensor Efficiency Optimization**
    *   *Description*: Group related telemetry fields into a unified `EvaluationSnapshot` to allow single-pass atomic consumption of hardware snapshots within the background tick loop.
*   **Issue #1163: Stateless & Functional Logic Refactoring**
    *   *Description*: Migrate `LocationProcessor` to a functional model using an immutable `ProcessorState` data class passed with each point.
*   **Issue #1164: Persistence of Logic State**
    *   *Description*: Serialize `AlarmHistory` into the database/DataStore to ensure geofence debounce states and power alarm latches survive process death or deep sleep system kills.
*   **Issue #1165: Unified Session Lifecycle Management**
    *   *Description*: Centralize session state management in a `SessionLifecycleCoordinator` to ensure all hardware peaks, temporal lockouts, and vitality markers are zeroed atomically upon session restart.
*   **Issue #1166: State Partitioning & Slicing**
    *   *Description*: Split `MainUiState` into specialized slices (`MapUiState`, `DashboardUiState`) to minimize recomposition evaluation costs and isolate volatile telemetry.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Description*: Extract imperative osmdroid management into a standalone `MapOverlayController` to keep UI code declarative and implement background coordinate filtering beforehand.
*   **Issue #1168: Vendor Adaptation Centralization**
    *   *Description*: Consolidate vendor-specific adaptations and loop continuity tweaks into a central `DeviceProfileManager` or `DeviceHardeningStrategy` (Samsung, A15, S21FE).
*   **Issue #1169: Fast-Path Configuration Convergence**
    *   *Description*: Unify the fast-path implementation in `HardwareSuite` using a generic `HardwareFastPath<T>` delegate that automatically handles its own baseline decay or synchronization.
*   **Issue #1170: God Object ViewModel Decomposition**
    *   *Description*: Split the monolithic `MainViewModel` into feature-specific ones (`Tracker`, `Viewer`, `Setup`) bound to Jetpack Navigation graph scopes.
*   **Issue #1171: Service & Worker Consolidation**
    *   *Description*: Consider merging `TrackerService` and `ViewerService` into a single `MonitorService` that reactively changes behavior based on the active `appMode`.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Description*: Merge conflation and throttling logic into a single "Smart Dispatcher" that handles inter-frame delays and connection freshness flow controls reactively.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Description*: Deprecate verbose JSON mapping boilerplate and parse routines by implementing pure Protobuf binary pipelines straight into Room BLOB objects (`HistoryEntity`).
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Description*: Consider a strategic option to remove the backlog sync (`PendingStatusDao`) and forensic backfilling (gap interpolation) entirely to reduce long-term maintenance.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1174: Interface Isolation Utilities** (Resolved Sep.21.131)
    *   *Remediation*: Created `LocationProcessorListener` and `DefaultLocationProcessorListener` with no-op methods to prevent test breakages during interface expansion and stabilize regression testing (R-ID 395).

*   **Issue #1159: Unused Forensic Auditing Dead Code Elimination** (Resolved Sep.21.130)
    *   *Remediation*: Removed the unused `maxGnssJitterMs` property and obsolete `processReflection` function from `HardwareSuite.kt` to simplify code architecture and prune tracking leftovers.

*   **Issue #1158: GNSS Sampling Logic Consolidation** (Resolved Sep.21.128)
    *   *Remediation*: Encapsulated GNSS sampling policy (standard vs throttled) and auditing triggers in a nested `GnssPolicyEngine` within `HardwareSuite.kt`. This decouples the hardware callback from throttling rules and ensures symmetric auditing of jitter across all performance tiers (R-ID 394).

*   **Issue #1156: Unused Forensic Abstraction** (Resolved Sep.21.127)
    *   *Remediation*: Refactored `HistoryManager.backfillAnalyticalGaps` to consume the specialized `EngineAcousticSample` sequence from `HardwareSuite.getAcousticSamples`, eliminating dead code and ensuring environmental noise is captured with semantic precision (R-ID 393).

*   **Issue #1157: Telemetry Abstraction Integration (Idea 5)** (Resolved Sep.21.127)
    *   *Remediation*: Refactored `HistoryManager.fillRealGap` and `TelemetryAggregator` to consume `EngineAcousticSample` directly. This completes the decoupling of environmental noise (dB) from satellite SNR in the forensic ribbon history (R-ID 393).

*   **Issue #1155: Acoustic-SNR Semantic Mismatch** (Resolved Sep.21.125)
    *   *Remediation*: Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type (R-ID 393).

*   **Issue #1153: Forensic Sequence Race Condition** (Resolved Sep.21.125)
    *   *Remediation*: Refactored `CircularStateBuffer.forensicSequence` to use a multi-pass custom Sequence implementation with locking (R-ID 392).

*   **Issue #1154: Forensic Allocation Spike** (Resolved Sep.21.125)
    *   *Remediation*: Replaced `toList()` snapshot with a lazy, zero-allocation sequence iteration (R-ID 392).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 395 (Rules: 81, IDs: 395), Resolved: 1151, Open: 0, Testing: 3 (Sub-items: 11), Ideas: 15, QA: 283]**
