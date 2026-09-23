# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.23.03

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Unintended Side Effects & Design Inconsistencies
*(No active open gaps identified. All critical synchronization issues have been resolved.)*

---

## 💡 Strategic Simplification Ideas (Ideas: 10)

*   **Issue #1164: Persistence of Logic State**
    *   *Description*: Serialize `AlarmHistory` into the database/DataStore to ensure geofence debounce states and power alarm latches survive process death or deep sleep system kills.
    *   *Significance*: **High (Reliability)**: Prevents state loss and incorrect alarm re-triggers when the OS terminates background components under memory pressure.
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Description*: Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer` and consolidate "Parking Anchor" and "Home Point" logic.
    *   *Significance*: **Medium-High (Memory Efficiency)**: Consolidates duplicate caching layers and location window structures into a single unified high-performance buffer.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Description*: Merge conflation and throttling logic into a single "Smart Dispatcher" that handles inter-frame delays and connection freshness flow controls reactively.
    *   *Significance*: **Medium (Network Efficiency)**: Optimizes server telemetry traffic and handles varying network quality gracefully via adaptive flow controls.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Description*: Expand flyweight patterns to all entities (Telemetry, Violations, SpatialPoints) and use pre-allocated ring buffers for `EngineConnectionPoint` and a `LogEntry` pool to eliminate GC churn.
    *   *Significance*: **Medium (GC Tuning)**: Minimizes memory fragmentation and prevents periodic UI stutters or service drops due to frequent garbage collection cycles.
*   **Issue #1163: Stateless & Functional Logic Refactoring**
    *   *Description*: Migrate `LocationProcessor` to a functional model using an immutable `ProcessorState` data class passed with each point.
    *   *Significance*: **Medium (Robustness)**: Eliminates side-effects and concurrency issues in the core point processing pipeline by using functional immutability.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Description*: Deprecate verbose JSON mapping boilerplate and parse routines by implementing pure Protobuf binary pipelines straight into Room BLOB objects (`HistoryEntity`).
    *   *Significance*: **Medium (Disk I/O & Clean Code)**: Speeds up database writes and simplifies parsing boilerplate by substituting JSON strings with fast Protobuf serialization.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Description*: Extract imperative osmdroid management into a standalone `MapOverlayController` to keep UI code declarative and implement background coordinate filtering beforehand.
    *   *Significance*: **Low-Medium (UI Decoupling)**: Keeps Composable functions clean and isolated from map rendering lifecycles, enabling independent background coordinate processing.
*   **Issue #1171: Service & Worker Consolidation**
    *   *Description*: Consider merging `TrackerService` and `ViewerService` into a single `MonitorService` that reactively changes behavior based on the active `appMode`.
    *   *Significance*: **Low (Lifecycle Simplification)**: Unifies background services, reducing Manifest overhead and centralizing OS foreground service notifications.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Description*: Consider a strategic option to remove the backlog sync (`PendingStatusDao`) and forensic backfilling (gap interpolation) entirely to reduce long-term maintenance.
    *   *Significance*: **Strategic (Maintenance Tradeoff)**: Reduces codebase complexity dramatically but at the expense of offline history features; depends heavily on product direction.
*   **Issue #1194: Unified Event Logging and Action Handling**
    *   *Description*: Consolidate global event logging and administrative actions into a dedicated `AppEventCoordinator` or similar domain utility. This reduces the `onEvent` surface area in `MainViewModel` and ensures consistent cross-feature audit logs.
    *   *Significance*: **Low (Refactoring)**: Centralizes cross-cutting logging concerns and simplifies ViewModel event loops.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1192: Disconnected Settings Input State Flow** (Resolved Sep.23.03)
    *   *Remediation*: Consolidated draft configuration events and top-level navigation logic into `MainViewModel`. This ensures that all functional role screens consuming `MainViewModel` state reflect user input in real-time. Simplified `TrackerViewModel` and `ViewerViewModel` by removing redundant draft management code (R-ID 419).

*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion** (Resolved Sep.23.01)
    *   *Remediation*: Converted `AudioSynthesizer.isLooping` into a `MutableStateFlow` to expose reactive siren activity feedback globally. Subscribed `MainViewModel` to this flow to sync playback states with `DiagnosticState`, completely rectifying the asymmetric role state dispersion loop across presentation layers (R-ID 418).

*   **Issue #1191: Broken Import Operations Due to Handled Event Omissions** (Resolved Sep.22.50)
    *   *Remediation*: Added robust handling loops for `UiEvent.BulkUpdateSettings` and `UiEvent.LogAction` within `MainViewModel.onEvent`. Integrated `alertSettingsFlow` into `StateSubscriptionUseCase` to ensure state changes propagate to the presentation layer without omissions, restoring seamless config/trail restoration pipelines (R-ID 416).

*   **Issue #1170: God Object ViewModel Decomposition** (Resolved Sep.22.41)
    *   *Remediation*: Decomposed the monolithic `MainViewModel` into feature-specific ViewModels (`TrackerViewModel`, `ViewerViewModel`, `SetupViewModel`) bound to their respective navigation scopes. Refactored `MainViewModel` into a lightweight coordinator for app-level state and global overlays. This significantly improves memory isolation and isolates recomposition triggers between functional roles (R-ID 415).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 419 (Rules: 84, IDs: 419), Resolved: 1174, Open: 0, Testing: 3 (Sub-items: 12), Ideas: 10, QA: 283]**
