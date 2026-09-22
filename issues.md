# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.22.11

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*(No critical missing functionality identified in current audit path).*

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 💡 Strategic Simplification Ideas (Ideas: 12)

*   **Issue #1164: Persistence of Logic State**
    *   *Description*: Serialize `AlarmHistory` into the database/DataStore to ensure geofence debounce states and power alarm latches survive process death or deep sleep system kills.
    *   *Contribution*: **High (Reliability)**: Prevents state loss and incorrect alarm re-triggers when the OS terminates background components under memory pressure.
*   **Issue #1182: Generic Fast-Path Snapshotting**
    *   *Description*: Group baseline and spike data into the `ForensicSnapshot` itself. This allows the background service to adjust baselines atomically without redundant `synchronized(this)` blocks in `HardwareSuite`.
    *   *Contribution*: **High (Thread Safety & Performance)**: Simplifies parameter flow between `HardwareSuite` and `LocationProcessor` while reducing synchronization overhead on the high-frequency sensor thread.
*   **Issue #1170: God Object ViewModel Decomposition**
    *   *Description*: Split the monolithic `MainViewModel` into feature-specific ones (`Tracker`, `Viewer`, `Setup`) bound to Jetpack Navigation graph scopes.
    *   *Contribution*: **High (Architecture & Maintainability)**: Deconstructs the core UI controller, reducing memory footprint, isolating screen states, and improving testability.
*   **Issue #1162: Forensic & Sensor Efficiency Optimization**
    *   *Description*: Group related telemetry fields into a unified `EvaluationSnapshot` to allow single-pass atomic consumption of hardware snapshots within the background tick loop.
    *   *Contribution*: **High (Performance)**: Optimizes the background tracking loop by ensuring single-pass atomic data reads, minimizing locking overhead.
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Description*: Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer` and consolidate "Parking Anchor" and "Home Point" logic.
    *   *Contribution*: **Medium-High (Memory Efficiency)**: Consolidates duplicate caching layers and location window structures into a single unified high-performance buffer.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Description*: Merge conflation and throttling logic into a single "Smart Dispatcher" that handles inter-frame delays and connection freshness flow controls reactively.
    *   *Contribution*: **Medium (Network Efficiency)**: Optimizes server telemetry traffic and handles varying network quality gracefully via adaptive flow controls.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Description*: Expand flyweight patterns to all entities (Telemetry, Violations, SpatialPoints) and use pre-allocated ring buffers for `EngineConnectionPoint` and a `LogEntry` pool to eliminate GC churn.
    *   *Contribution*: **Medium (GC Tuning)**: Minimizes memory fragmentation and prevents periodic UI stutters or service drops due to frequent garbage collection cycles.
*   **Issue #1163: Stateless & Functional Logic Refactoring**
    *   *Description*: Migrate `LocationProcessor` to a functional model using an immutable `ProcessorState` data class passed with each point.
    *   *Contribution*: **Medium (Robustness)**: Eliminates side-effects and concurrency issues in the core point processing pipeline by using functional immutability.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Description*: Deprecate verbose JSON mapping boilerplate and parse routines by implementing pure Protobuf binary pipelines straight into Room BLOB objects (`HistoryEntity`).
    *   *Contribution*: **Medium (Disk I/O & Clean Code)**: Speeds up database writes and simplifies parsing boilerplate by substituting JSON strings with fast Protobuf serialization.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Description*: Extract imperative osmdroid management into a standalone `MapOverlayController` to keep UI code declarative and implement background coordinate filtering beforehand.
    *   *Contribution*: **Low-Medium (UI Decoupling)**: Keeps Composable functions clean and isolated from map rendering lifecycles, enabling independent background coordinate processing.
*   **Issue #1171: Service & Worker Consolidation**
    *   *Description*: Consider merging `TrackerService` and `ViewerService` into a single `MonitorService` that reactively changes behavior based on the active `appMode`.
    *   *Contribution*: **Low (Lifecycle Simplification)**: Unifies background services, reducing Manifest overhead and centralizing OS foreground service notifications.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Description*: Consider a strategic option to remove the backlog sync (`PendingStatusDao`) and forensic backfilling (gap interpolation) entirely to reduce long-term maintenance.
    *   *Contribution*: **Strategic (Maintenance Tradeoff)**: Reduces codebase complexity dramatically but at the expense of offline history features; depends heavily on product direction.

---

## 💡 Strategic Simplification Ideas (Ideas: 11)

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1183: Trigger-Based Forensic Sampling** (Resolved Sep.22.11)
    *   *Remediation*: Transitioned from a fixed-interval forensic loop to a "Signal-on-Spike" model where `HardwareFastPath`, location updates, and logic ticks trigger telemetry capture. This drastically reduces background CPU wakeups and GC pressure by eliminating redundant data points during long stationary periods (R-ID 406).

*   **Issue #1166: State Partitioning & Slicing** (Resolved Sep.22.08)
    *   *Remediation*: Split the monolithic `MainUiState` into specialized slices (`SessionUiState`, `SpatialUiState`, `SettingsUiState`, `MapTriggers`, `SimulationUiState`). Refactored `MainViewModel` and all screen Composables to consume these granular segments, significantly reducing recomposition frequency and isolating volatile triggers (R-ID 405).

*   **Issue #1169: Fast-Path Configuration Convergence** (Resolved Sep.22.08)
    *   *Remediation*: Unified acoustic and light fast-path implementations in `HardwareSuite` using a generic `HardwareFastPath` structure. This centralizes baseline decay, spike detection, and debouncing logic, ensuring symmetric and race-free processing of high-frequency sensor events (R-ID 404).

*   **Issue #1168: Vendor Adaptation Centralization** (Resolved Sep.22.07)
    *   *Remediation*: Consolidated vendor-specific adaptations and loop continuity tweaks into a central `DeviceProfileManager` to keep hardware-dependent behavioral overrides centralized and decoupled from background services (R-ID 403).

*   **Issue #1181: UseCase Functional Consolidation** (Resolved Sep.22.05)
    *   *Remediation*: Consolidated `HomePointUseCase` and `MapUseCase` into a single high-cohesion `SpatialLogicUseCase`, streamlining domain logic boundaries and reducing the ViewModel dependency injection surface area (R-ID 402).

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
- **Current Audit Baseline: [SOT: 406 (Rules: 82, IDs: 406), Resolved: 1162, Open: 0, Testing: 3 (Sub-items: 12), Ideas: 12, QA: 283]**
