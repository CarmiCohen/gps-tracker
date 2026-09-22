# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*(No critical missing functionality identified in current audit path).*

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 💡 Strategic Simplification Ideas (Ideas: 13)

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

*   **Issue #1180: DataStore List Mutation Extension** (Resolved Sep.22.04)
    *   *Remediation*: Implemented generic `DataStore<AppSettings>.mutate` extension to encapsulate atomic, race-free list and field mutations. Refactored `SettingsRepository` methods (`addHomePoint`, `removeHomePoint`, and bulk save operations) to use this extension, eliminating redundant builder/update boilerplate (R-ID 401).

*   **Issue #1179: Corrupted Home Point Addition Logic** (Resolved Sep.22.03)
    *   *Remediation*: Implemented atomic `addHomePoint` and `removeHomePoint` methods in `SettingsRepository` using DataStore's `updateData` to prevent race conditions. Refactored `MainViewModel` to persist `ADD` mode during batch operations and force fence visibility (R-ID 400).

*   **Issue #1178: Initial GNSS Satellite Count Blanking Prior to Initial Lock** (Resolved Sep.22.00)
    *   *Remediation*: Set default satellite counts to -1 in `LocationUpdate` and propagated actual values to UI states. This ensures UI can distinguish between zero satellites (e.g. jammer/tunnel) and "no data" states during initialization (R-ID 399).

*   **Issue #1177: Static Role Branding on Selection Screen Cards** (Resolved Sep.22.00)
    *   *Remediation*: Integrated `isPeerActive` check into `MainViewModel` pulse loop and passed to `LandingScreen`. Role card colors now dynamically dim to `Slate500` when no telemetry is detected, preventing visual confusion (R-ID 398).

*   **Issue #1176: Mismatched Temperature Unit Prefix Layout Ordering** (Resolved Sep.22.00)
    *   *Remediation*: Corrected text component placement in `StatusRowData` to suffix the degree sign (`0°`) instead of prefixing it, ensuring consistent SI unit presentation (R-ID 397).

*   **Issue #1165: Unified Session Lifecycle Management** (Resolved Sep.21.132)
    *   *Remediation*: Centralized session state management in `SessionLifecycleCoordinator` to ensure all hardware peaks, temporal lockouts, and vitality markers are zeroed atomically upon session restart (R-ID 396).

*   **Issue #1174: Interface Isolation Utilities** (Resolved Sep.21.131)
    *   *Remediation*: Created `LocationProcessorListener` and `DefaultLocationProcessorListener` with no-op methods to prevent test breakages during interface expansion and stabilize regression testing (R-ID 395).

*   **Issue #1159: Unused Forensic Auditing Dead Code Elimination** (Resolved Sep.21.130)
    *   *Remediation*: Removed the unused `maxGnssJitterMs` property and obsolete `processReflection` function from `HardwareSuite.kt` to simplify code architecture and prune tracking leftovers.

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 401 (Rules: 82, IDs: 401), Resolved: 1157, Open: 0, Testing: 3 (Sub-items: 11), Ideas: 13, QA: 283]**
