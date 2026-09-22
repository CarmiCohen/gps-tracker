# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.22.27

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Unhandled Edge Cases & Core Logic Bugs
*   **Issue #1184: Broken Thermal Recovery Latency Audit in Trigger-Based Forensic Sampling Loop**
    *   *Description*: In `TrackerService.startForensicSamplingLoop()`, `recoveryTriggerRt` tracks the timestamp when thermal cooling mode deactivates. However, the recovery verification block `if (recoveryTriggerRt > 0 && delayMs < FORENSIC_SAMPLING_INTERVAL_COOLING_MS)` executes *within the exact same loop iteration pass* immediately after deactivation. 
    *   *Risk/Concern*: The audit latency calculation evaluates to 0ms (or sub-millisecond) every time, completely failing to measure the true temporal latency to the next normal sample pass and breaking forensic audit trace precision (Issue #1183).

### Unintended Side Effects & Design Inconsistencies
*   **Issue #1187: Clobbered Fast-Path Baseline Learning on Sensor Thread**
    *   *Description*: In `HardwareSuite.kt`, `lightFastPath.evaluate()` incorporates an EMA smoothing parameter (`alpha`) to track gradual ambient light fluctuations between background ticks. However, `TrackerService.processTick()` forcefully invokes `hardwareSuite.setLightFastPath(...)` every 2 seconds, overriding `lightFastPath.baseline` with `locationProcessor.getLuxBaseline()`.
    *   *Risk/Concern*: Wipes out and neutralizes high-frequency autonomous baseline calibration on the sensor thread, causing redundant step overrides (Issue #1169).
*   **Issue #1189: Double-Counting Vibration Floor Adaptation during GPS Point Processing**
    *   *Description*: In `LocationProcessor.processGpsPoint`, when a new coordinate fix propagates fast-path tamper timestamps via `sentinel.updateSensorState`, if `providedAdaptiveVibrationFloor` is unprovided or defaulted (`-1.0`), the sentinel executes its fallback `else` branch, re-invoking `SentinelValidator.updateVibrationFloor(...)` with the stale `currentVibrationIndex`.
    *   *Risk/Concern*: Causes the vibration floor to adapt multiple times per single tick interval, accelerating baseline decay/growth artificially and distorting stationary detection thresholds (Issue #1143).

---

## 💡 Strategic Simplification Ideas (Ideas: 12)

## 🟢 Resolved Traceability & Metadata Issues

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
- **Current Audit Baseline: [SOT: 409 (Rules: 82, IDs: 409), Resolved: 1165, Open: 3, Testing: 3 (Sub-items: 12), Ideas: 12, QA: 283]**
