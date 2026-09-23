# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.22.50

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Unintended Side Effects & Design Inconsistencies
*   **Issue #1192: Disconnected Settings Input State Flow**
    *   *Description*: `MainAppContent` passes `settingsState` derived from `MainViewModel` down to `TrackerScreen` and `ViewerScreen`. However, the settings text input fields and controls dispatch draft updates (e.g., `UpdateDraftDeviceId`, `UpdateDraftRelayUrl`) to the feature-specific `TrackerViewModel` or `ViewerViewModel`. Since the screen components observe `MainViewModel` for the draft settings values, the text inputs remain visually frozen and do not reflect user entry.
    *   *Significance*: **Critical (UI Input Lock)**: Completely prevents modifying or saving any application configuration parameters within active role states.
*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion**
    *   *Description*: `SettingsOverlay` triggers `SetSirenType` and siren playback controls via the feature-specific ViewModels, but reads audio playback feedback state from `MainViewModel`'s `diagnosticState`. This creates an asymmetric state loop where audio synthesis state tracking diverges across different roles.
    *   *Significance*: **Medium (State Divergence)**: Leads to race conditions or incorrect playback indication in the UI settings panel.

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

*   **Issue #1191: Broken Import Operations Due to Handled Event Omissions** (Resolved Sep.22.50)
    *   *Remediation*: Added robust handling loops for `UiEvent.BulkUpdateSettings` and `UiEvent.LogAction` within `MainViewModel.onEvent`. Integrated `alertSettingsFlow` into `StateSubscriptionUseCase` to ensure state changes propagate to the presentation layer without omissions, restoring seamless config/trail restoration pipelines (R-ID 416).

*   **Issue #1170: God Object ViewModel Decomposition** (Resolved Sep.22.41)
    *   *Remediation*: Decomposed the monolithic `MainViewModel` into feature-specific ViewModels (`TrackerViewModel`, `ViewerViewModel`, `SetupViewModel`) bound to their respective navigation scopes. Refactored `MainViewModel` into a lightweight coordinator for app-level state and global overlays. This significantly improves memory isolation and isolates recomposition triggers between functional roles (R-ID 415).

*   **Issue #1162: Forensic & Sensor Efficiency Optimization** (Resolved Sep.22.32)
    *   *Remediation*: Refactored the background tick loops in `TrackerService` and `ViewerService` to group system health and sensor metrics into an atomic `EvaluationSnapshot`. This ensures single-pass telemetry processing and minimizes the parameter surface area between the service and engine layers.

*   **Issue #1182: Generic Fast-Path Snapshotting / Elimination of Multi-pass Fallbacks** (Resolved Sep.22.31)
    *   *Remediation*: Grouped individual sensor parameter updates in `updateSensorState` into a structured `SensorStateSnapshot` to eliminate individual fallback clauses, reducing parameter passing complexity and streamlining validation gates.

*   **Issue #1189: Double-Counting Vibration Floor Adaptation during GPS Point Processing** (Resolved Sep.22.30)
    *   *Remediation*: Guarded the autonomous vibration floor adaptation fallback path inside `LocationSentinel.updateSensorState` with a check ensuring `vibration >= 0.0`. This prevents multiple uncoordinated adaptations using stale data when called solely to update lockout realtimes or other telemetry vectors.

*   **Issue #1187: Clobbered Fast-Path Baseline Learning on Sensor Thread** (Resolved Sep.22.28)
    *   *Remediation*: Added a `preserveExistingBaseline` parameter to `HardwareFastPath.update` to prevent the 2-second background process ticks from clobbering and resetting high-frequency autonomous light baseline calibration inside `HardwareSuite.kt`.

*   **Issue #1184: Broken Thermal Recovery Latency Audit in Trigger-Based Forensic Sampling Loop** (Resolved Sep.22.28)
    *   *Remediation*: Corrected the thermal recovery latency audit check to evaluate across iteration passes rather than returning a 0ms intra-iteration result, establishing reliable precision for forensic audit traces.

*   **Issue #1186: Stale Acoustic Fast-Path Baseline and Missing Dynamic Synchronization** (Resolved Sep.22.27)
    *   *Remediation*: Added periodic re-synchronization of the acoustic fast-path baseline with `LocationSentinel`'s contracting acoustic floor within `TrackerService.processTick()`. This ensures high-frequency acoustic monitoring stays perfectly dynamically aligned with the core validation layer.

*   **Issue #1185: Semantic Type Mismatch in LocationProcessor.getAdaptiveVibrationFloor** (Resolved Sep.22.26)
    *   *Remediation*: Corrected `getAdaptiveVibrationFloor()` in `LocationProcessor.kt` to return `sentinel.adaptiveVibrationFloor` instead of `sentinel.acousticFloorDb`. This resolves the severe semantic leak across the telemetry pipeline, ensuring actual adaptive vibration baseline metrics are correctly propagated rather than acoustic ones.

*   **Issue #1188: Lack of Baseline Adaptation alpha for Acoustic Fast Path** (Resolved Sep.22.15)
    *   *Remediation*: Added alpha baseline adaptation parameter to `acousticFastPath.evaluate` in `HardwareSuite.kt`. This ensures the high-frequency acoustic baseline independently tracks ambient background noise levels, maintaining symmetry with the light fast-path and core validation logic (R-ID 407).

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
    *   *Remediation*: Implemented atomic `addHomePoint` and `removeHomePoint` methods in SettingsRepository using DataStore's updateData to prevent race conditions. Refactored `MainViewModel` to persist `ADD` mode during batch operations and force fence visibility (R-ID 400).

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
- **Current Audit Baseline: [SOT: 416 (Rules: 84, IDs: 416), Resolved: 1172, Open: 2, Testing: 3 (Sub-items: 12), Ideas: 10, QA: 283]**
