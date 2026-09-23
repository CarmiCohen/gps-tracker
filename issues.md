# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.23.06

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Unintended Side Effects & Design Inconsistencies
*(No active open gaps identified. All critical synchronization issues have been resolved.)*

---

## 💡 Strategic Simplification Ideas (Ideas: 13)

*   **Issue #1204: Unified Hardware Lifecycle & Vendor Hardening**
    *   *Description*: Consolidate vendor-specific adaptations (Samsung/Xiaomi/Huawei) and unify WakeLock management into a central `DeviceHardeningStrategy`. Abstract stay-alive pulses into a dedicated `ProcessPriorityMonitor`.
    *   *Significance*: **High (Reliability)**: Critical for preventing background service termination by OEM-specific power managers, ensuring consistent GPS tracking.
*   **Issue #1161: Unified Trajectory & Buffer Management**
    *   *Description*: Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single optimized `TrajectoryBuffer` and consolidate "Parking Anchor" and "Home Point" logic.
    *   *Significance*: **Medium-High (Memory Efficiency)**: Consolidates duplicate caching layers and location window structures into a single unified high-performance buffer.
*   **Issue #1203: Hilt ViewModel Scope Optimization**
    *   *Description*: Evaluate if `TrackerViewModel` and `ViewerViewModel` can be scoped to the navigation backstack entry rather than the standard hilt scope to ensure state is cleanly wiped when exiting a role.
    *   *Significance*: **Medium-High (State Lifecycle Reliability)**: Ensures automatic and clean lifecycle resetting, eliminating manual state cleanup bugs when shifting user roles.
*   **Issue #1172: Smart Signaling Dispatcher**
    *   *Description*: Merge conflation and throttling logic into a single "Smart Dispatcher" that handles inter-frame delays and connection freshness flow controls reactively.
    *   *Significance*: **Medium (Network Efficiency)**: Optimizes server telemetry traffic and handles varying network quality gracefully via adaptive flow controls.
*   **Issue #1163: Stateless & Functional Logic Refactoring**
    *   *Description*: Migrate `LocationProcessor` to a functional model using an immutable `ProcessorState` data class passed with each point.
    *   *Significance*: **Medium (Robustness)**: Eliminates side-effects and concurrency issues in the core point processing pipeline by using functional immutability.
*   **Issue #1160: Flyweight & Pooling Expansion**
    *   *Description*: Expand flyweight patterns to all entities (Telemetry, Violations, SpatialPoints) and use pre-allocated ring buffers for `EngineConnectionPoint` to eliminate GC churn.
    *   *Significance*: **Medium (GC Tuning)**: Minimizes memory fragmentation and prevents periodic UI stutters or service drops due to frequent garbage collection cycles.
*   **Issue #1173: Protobuf-First Persistence**
    *   *Description*: Deprecate verbose JSON mapping boilerplate and parse routines by implementing pure Protobuf binary pipelines straight into Room BLOB objects (`HistoryEntity`).
    *   *Significance*: **Medium (Disk I/O & Clean Code)**: Speeds up database writes and simplifies parsing boilerplate by substituting JSON strings with fast Protobuf serialization.
*   **Issue #1201: Reactive Siren Lockout**
    *   *Description*: Move the siren cooldown/lockout logic from `AudioSynthesizer` into a `SirenUseCase`. This keeps the synthesizer focused purely on signal generation.
    *   *Significance*: **Medium (Domain & Responsibility Isolation)**: Cleanly decouples domain rules from infrastructure/audio synthesis logic.
*   **Issue #1202: UI Event Routing Unification**
    *   *Description*: Refactor the remaining feature-specific navigation events into a single `UiEventDelegate` or `NavigationCoordinator` to further decouple `MainViewModel` from UI details.
    *   *Significance*: **Medium (UI Decoupling & Architecture Cleanliness)**: Centralizes navigation pathways and decouples ViewModels from explicit UI implementation specifics.
*   **Issue #1167: Map Overlay Imperative to Declarative Controller**
    *   *Description*: Extract imperative osmdroid management into a standalone `MapOverlayController` to keep UI code declarative.
    *   *Significance*: **Low-Medium (UI Decoupling)**: Keeps Composable functions clean and isolated from map rendering lifecycles.
*   **Issue #1171: Service & Worker Consolidation**
    *   *Description*: Consider merging `TrackerService` and `ViewerService` into a single `MonitorService` that reactively changes behavior based on the active `appMode`.
    *   *Significance*: **Low (Lifecycle Simplification)**: Unifies background services, reducing Manifest overhead and centralizing OS notifications.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Description*: Consider a strategic option to remove the backlog sync and forensic backfilling entirely to reduce long-term maintenance.
    *   *Significance*: **Strategic (Maintenance Tradeoff)**: Reduces codebase complexity dramatically but at the expense of offline history features.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1164: Persistence of Logic State** (Resolved Sep.23.06)
    *   *Remediation*: Enhanced the JSON serialization matrix of active alarms inside `AppAlarmManager` to capture explicit state markers including trigger realtimes (`firstTriggerRt`) and wall timestamps, preventing duration reset loops after process death.

*   **Issue #1200: Shared Overlay Scope** (Resolved Sep.23.04)
    *   *Remediation*: Centralized all shared overlays into a dedicated `OverlayHost` component within `MainAppContent`. This eliminated extensive callback routing in `TrackerScreen` and `ViewerScreen`.

*   **Issue #1192: Disconnected Settings Input State Flow** (Resolved Sep.23.03)
    *   *Remediation*: Consolidated draft configuration events and top-level navigation logic into `MainViewModel`.

*   **Issue #1193: Asymmetric Audio Control and Siren State Dispersion** (Resolved Sep.23.01)
    *   *Remediation*: Converted `AudioSynthesizer.isLooping` into a `MutableStateFlow` to expose reactive siren activity feedback globally.

*   **Issue #1194: Unified Event Logging and Action Handling** (Resolved Sep.23.01)
    *   *Remediation*: Consolidated global event logging and administrative actions into a dedicated `AppEventCoordinator`.

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 420 (Rules: 84, IDs: 420), Resolved: 1176, Open: 0, Testing: 3 (Sub-items: 12), Ideas: 12, QA: 283]**
