# SOT Master Requirements & Hardening Status (Sep.24.04)

## 🏗️ Architectural Master Rules (22 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744/R745/R746/R747/R748/R749/R750/R752/R753/R754/R755/R756/R757)**: **MANDATORY**. Hardware managers (GPS, Sensors, Network, GNSS) must use `ManagedHardware` abstractions for synchronous, trace-logged unregistration. Listener unregistration MUST be processed on the dedicated hardware thread (or Main Looper for OS callbacks) before termination, using synchronous synchronization (e.g., CountDownLatch or task awaiting) to ensure native disposal finishes. Hardware cleanup methods (e.g., `GpsManager.stop()`) must be unconditional to ensure background or revival callbacks are not orphaned if primary flows were never started. (Updated Aug.28.09).
*   **1.9 IPC Optimization (R759)**: High-frequency lookups of system identifiers (e.g., Package Name) must utilize `GpsApplication.PACKAGE_NAME` shadow-cache to prevent repetitive IPC calls and associated OS-level diagnostic log flooding on restricted hardware (Added Aug.28.08).
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic to survive clock regressions and drift (R116).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4-7 (Map Engine & Overlay Hydration) must be triggered via `IdleHandler` and staggered over multiple frames. Heavy initialization of the OSM engine and `SqlTileWriter` MUST be offloaded to a background IO thread in `GpsApplication` and gated via `isOsmReady` to ensure hydration never blocks the Main thread (R318, R323, R739, R758).
*   **2.2 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms). Native initialization must implement exponential backoff retries to ensure reliable binding during background service startup (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/R721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and an LRU strategy for long-term stability (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlay pools and icon caches must use standard collections and be isolated from Compose `Snapshot` observation. Since these are updated imperatively via `AndroidView.update`, standard collections eliminate lock verification failures and frame skips on non-generational GCs (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows (Logs, Trails, Violations, History) must utilize Snap-Isolation via deep-parity throttling (`contentEquals` + `distinctUntilChanged`). This prevents the Compose Recomposer from performing redundant snapshot reconciliation cycles, eliminating lock verification failures and thread synchronization contention on Samsung hardware (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Signal loss and accuracy violations must be suppressed for the first 30 seconds after system activation or mode transition to allow GPS provider stabilization (R315).
*   **2.7 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).

### 3. Forensic & Security Rules
*   **3.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **3.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **3.3 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`, `ToggleSetupBypass`) to verify alarm triggers and facilitate automated soak tests under simulated stress (R196-V, R735).
*   **3.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent. The warning overlay dismissal must be written to the DataStore to prevent redundant notifications across cold starts (R737, R976).
*   **3.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) to eliminate vendor framework collisions. Legacy binary signatures (`mbrainSDK`) are neutralized in all code and string pools to prevent heuristic OS triggers (R212, R310). Hardware identification logic is decoupled from the application layer via `HardwareSot` (R317).

## 🛡️ Core Hardening Baseline
*   **SOT ID 460**: Atomic User Counter Hardening - Guarded the `activeUsers` AtomicInteger in `HardwareSuite.stop()` to prevent it from falling into negative values due to unbalanced lifecycle calls. Hardened the deferred teardown check to use `<= 0`, ensuring resource release (sensors, GNSS) is reliably executed even if counter drift occurred, eliminating long-term battery and resource leaks (R-ID 460). (Resolved Sep.24.04)
*   **SOT ID 459**: Persistent Adaptive Vibration Floor - Implemented persistence for the physical baseline sensitivity anchor. Added `ADAPTIVE_VIBRATION_FLOOR_KEY` to `PreferenceKeys.kt` and updated `LocationProcessor.kt` to emit reactive `VibrationFloorChanged` events upon significant drift detection. Updated `TrackerService.kt` and `ViewerService.kt` to restore this anchor during service initialization and sync changes to DataStore, eliminating sensitivity resets and false-positive tamper alerts after service restarts (R-ID 459). (Resolved Sep.24.03)
*   **SOT ID 458**: Reboot-Aware Monotonic Clock Recovery - Implemented `recoverLastRealtime` in `HistoryManager.kt` using role-prefixed clock drift references. Updated `TrackerService.kt` and `ViewerService.kt` to utilize this logic during service initialization, ensuring monotonic timing anchors remain valid across device reboots by detecting drift divergence and anchoring to the current boot cycle's reference (R-ID 458). (Resolved Sep.24.02)
*   **SOT ID 457**: Fast-Path Allocation Optimization - Refactored `HardwareFastPath` in `HardwareSuite.kt` to allow optional callback parameter assignment. Updated `TrackerService.kt` to omit callbacks inside the periodic tick iteration loop, fully mitigating high allocation churn and garbage collection pressure without degrading light or acoustic spike responses (R-ID 457). (Resolved Sep.24.01)
*   **SOT ID 456**: Redundant Stream & Heartbeat Idempotency - Removed redundant reactive stream subscriptions in `ViewerService.kt`. The `ConnectivityEvent.PeerPulse` is now handled via a single observer, preventing duplicate state updates in `SessionManager` and eliminating redundant heartbeat log entries (R-ID 456). (Resolved Sep.23.80)
*   **SOT ID 455**: Build Vitality & Reactive Stream Convergence - Implemented missing abstract members in `TrackerService`, fully hydrated `MainRepository` delegates for draft settings, and corrected `MainViewModel` flow typing to restore `.value` access. (Resolved Sep.23.72)
*   **SOT ID 453**: Role-Based Storage Namespacing - Implemented physical isolation for logic state persistence using role-prefixed maps (`role_longs`, `role_doubles`, etc.) in `AppSettings`. Refactored `SettingsRepository` and `MainRepository` to route `"T_"` and `"V_"` prefixed keys to these isolated partitions, preventing state corruption and logic leakage when switching functional roles (Tracker vs Viewer) on the same hardware. (Resolved Sep.23.71)
*   **SOT ID 424**: Race Condition & Initialization Safeguard - Introduced `initializationDeferred` using Kotlin coroutines CompletableDeferred in `BaseMonitorService`. This ensures that high-frequency background ticks, telemetry sampling loops, and heartbeat broadcasts are strictly blocked until asynchronous service hydration and database state restoration (`onServiceInitialize()`) are completely finished. fully closing state race conditions under physical stress or system recovery startup cycles. (Resolved Sep.23.70)
*   **SOT ID 423**: Siren Trigger Orchestration - Integrated physical siren activation into the core alarm evaluation loop within `AppAlarmManager`. This ensures that violation detections in background services are immediately and reliably translated into audio synthesis via `AudioSynthesizer`, respecting all role-based stealth requirements and manual silence overrides. (Resolved Sep.23.60)
*   **SOT ID 422**: Centralized Single Source of Truth (SSOT) ViewModel Architecture - Re-consolidated role-specific ViewModels into a unified activity-scoped `MainViewModel`. Established a single point of subscription for high-frequency kinematic and diagnostic data streams, eliminating coroutine allocation churn (#1211) and ensuring map view states and configuration drafts remain persistent during navigation transitions (#1212, #1213). This architecture guarantees atomic state propagation across all functional roles (Tracker, Viewer, Setup). (Resolved Sep.23.50)
*   **SOT ID 421**: Unified Hardware Lifecycle & Vendor Hardening - Consolidated Samsung, Xiaomi, and Huawei-specific power management adaptations and WakeLock policies into a central `DeviceHardeningStrategy`. Implemented functional hardening logic for background execution continuity on Samsung/Huawei/Xiaomi hardware to prevent OS-level service termination (R-ID 421). (Resolved Sep.24.00)
*   **SOT ID 417**: Logic State Persistence Expansion - Enhanced the alarm evaluation history matrix inside `AppAlarmManager` by mapping and embedding `firstTriggerTs`, `firstTriggerRt`, `lastLogTs`, and `lastLogRt` inside JSON persistence payloads. This prevents transient temporal resets after system memory process kills or system wake cycles, fully closing behavioral continuity gaps. (Resolved Sep.23.06)
*   **SOT ID 420**: Shared Overlay Scope - Centralized all shared overlays (`SettingsOverlay`, `LogOverlay`, `RibbonsOverlay`, `GnssDetailOverlay`) into a dedicated `OverlayHost` component within `MainAppContent`. This eliminated extensive callback routing in `TrackerScreen` and `ViewerScreen`, ensuring overlays interact directly with `MainViewModel` and significantly reducing UI boilerplate. (Resolved Sep.23.04)
*   **SOT ID 419**: Unified Draft Settings State Flow - Consolidated configuration draft events and top-level navigation logic into `MainViewModel`. This ensures that all UI components observing the global state reflect user input in real-time, regardless of the active functional role, and eliminates state dispersion between feature-specific ViewModels. (Resolved Sep.23.03)
*   **SOT ID 418**: Siren State Synchronization - Converted `AudioSynthesizer.isLooping` into a `MutableStateFlow` to expose reactive siren activity feedback globally. Subscribed `MainViewModel` to this flow to sync playback states with `DiagnosticState`, completely rectifying the asymmetric role state dispersion loop across presentation layers. (Resolved Sep.23.01)
*   **SOT ID 416**: Config & Trail Import Restoration - Added robust event handling loops for `UiEvent.BulkUpdateSettings` and `UiEvent.LogAction` within `MainViewModel.onEvent`. Integrated `alertSettingsFlow` into `StateSubscriptionUseCase` to guarantee that all configurations loaded from external files are fully propagated to the active user interface state slices without omissions or silent fall-throughs. (Resolved Sep.22.50)
*   **SOT ID 415**: God Object ViewModel Decomposition - [REPLACED BY SOT ID 422] - Originally decomposed MainViewModel into role-specific ViewModels; later refactored back into a centralized SSOT model to resolve high-frequency stream churn and state loss. (Sep.22.30)
*   **SOT ID 414**: Forensic & Sensor Efficiency Optimization - Introduced EvaluationSnapshot to group system health and sensor metrics into an atomic DTO for single-pass consumption in the background tick loop. This ensures consistent telemetry state across the entire processing iteration and minimizes parameter passing overhead. (Resolved Sep.22.32)
*   **SOT ID 413**: Elimination of Multi-pass Fallbacks - Grouped individual sensor parameter clauses in updateSensorState into a structured SensorStateSnapshot to remove imperative value checking bounds and streamline parameter passing. (Resolved Sep.22.31)
*   **SOT ID 412**: Vibration Floor Adaptation Guard - Guarded the fallback autonomous vibration floor adaptation path in `LocationSentinel.updateSensorState` with a check ensuring `vibration >= 0.0`. This prevents multiple uncoordinated adaptations using stale data when called from coordinate fix propagation paths solely to update lockout realtimes or other telemetry vectors. (Resolved Sep.22.30)
*   **SOT ID 411**: Fast-Path Baseline Preservation - Added preserveExistingBaseline parameter to HardwareFastPath update to protect autonomous sensor-thread light baseline learning from 2-second background ticks. (Resolved Sep.22.28)
*   **SOT ID 410**: Thermal Recovery Latency Audit - Corrected the thermal recovery latency audit check to evaluate across iteration passes rather than returning a 0ms intra-iteration result, establishing reliable precision for forensic audit traces. (Resolved Sep.22.28)
*   **SOT ID 408**: Vibration Floor Semantic Alignment - Corrected `getAdaptiveVibrationFloor()` in `LocationProcessor.kt` to return `sentinel.adaptiveVibrationFloor` instead of `sentinel.acousticFloorDb`. This resolves the severe semantic leak across the telemetry pipeline, ensuring actual adaptive vibration baseline metrics are correctly propagated rather than acoustic ones. (Resolved Sep.22.26)
*   **SOT ID 407**: Acoustic Fast-Path Adaptation - Added alpha baseline adaptation parameter to `acousticFastPath.evaluate` in `HardwareSuite.kt`. This ensures the high-frequency acoustic baseline independently tracks ambient background noise levels, maintaining symmetry with the light fast-path and core validation logic (R-ID 407). (Resolved Sep.22.15)
*   **SOT ID 406**: Trigger-Based Forensic Sampling - Transitioned from a fixed-interval loop to a "Signal-on-Spike" model where `HardwareFastPath`, location updates, and logic ticks trigger telemetry capture. This drastically reduces background CPU wakeups and GC pressure by eliminating redundant data points during long stationary periods (R-ID 406). (Resolved Sep.22.11)
*   **SOT ID 405**: State Partitioning & Slicing - Split the monolithic `MainUiState` into specialized slices (`SessionUiState`, `SpatialUiState`, `SettingsUiState`, `MapTriggers`, `SimulationUiState`). Refactored `MainViewModel` and all screen Composables to consume these granular segments, significantly reducing recomposition frequency and isolating volatile triggers (R-ID 405). (Resolved Sep.22.08)
*   **SOT ID 404**: Fast-Path Configuration Convergence - Unified acoustic and light fast-path implementations in `HardwareSuite` using a generic `HardwareFastPath` structure. This centralizes baseline decay, spike detection, and debouncing logic, ensuring symmetric and race-free processing of high-frequency sensor events (R-ID 404). (Resolved Sep.22.08)
*   **SOT ID 403**: Vendor Adaptation Centralization - Consolidated vendor-specific adaptations and loop continuity tweaks into a central `DeviceProfileManager` to keep hardware-dependent behavioral overrides centralized and decoupled from background services (R-ID 403). (Resolved Sep.22.07)
*   **SOT ID 402**: UseCase Functional Consolidation - Consolidated `HomePointUseCase` and `MapUseCase` into a single, high-cohesion `SpatialLogicUseCase` DTO. This reduces the dependency injection surface area of `MainViewModel` and centralizes all spatial operations and map state transformations within a single domain logic layer (R-ID 402). (Resolved Sep.22.05)
*   **SOT ID 401**: DataStore List Mutation Extension - Implemented a generic `mutate` extension function for `DataStore<AppSettings>` to encapsulate atomic, race-free list and field updates. Refactored `SettingsRepository` to use this extension across all persistence methods, streamlining the data layer and eliminating redundant builder/update boilerplate (R-ID 401). (Resolved Sep.22.04)
*   **SOT ID 400**: Atomic Geofence Hydration - Implemented atomic `addHomePoint` and `removeHomePoint` methods in `SettingsRepository` using DataStore's `updateData`. This prevents list corruption and UI stalls during rapid sequential interactive updates. Refactored `MainViewModel` to persist active geofence modes during batch additions, ensuring a friction-less setup experience (R-ID 400). (Resolved Sep.22.03)
*   **SOT ID 399**: Initial GNSS Satellite Count Blanking Prior to Initial Lock - Set default satellite counts to -1 in `LocationUpdate` and `HudTelemetryState`. Updated `UiStateMapper` and `SharedUiComponents` to distinguish -1 (no data) from 0 (jammed/blocked) by displaying "--" until the first hardware fix is processed (R-ID 399). (Resolved Sep.22.00)
*   **SOT ID 398**: Static Role Branding on Selection Screen Cards - Integrated `isPeerActive` check into `MainViewModel` and passed it to `LandingScreen`. The Viewer card now dynamically dims when no telemetry is detected, improving role clarity during the initial handshake phase (R-ID 398). (Resolved Sep.22.00)
*   **SOT ID 397**: Mismatched Temperature Unit Prefix Layout Ordering - Corrected text component placement in `StatusRowData` within `SharedUiComponents.kt` to suffix the degree sign (`0°`) instead of prefixing it, ensuring alignment with SI standard presentation (R-ID 397). (Resolved Sep.22.00)
*   **SOT ID 396**: Unified Session Lifecycle Management - Centralized the zeroing of hardware baseline parameters, temporal lockout registers, forensic latches, and vitality markers into `SessionLifecycleCoordinator`, ensuring atomic integrity upon tracking resets. (Resolved Sep.21.132)
*   **SOT ID 395**: Interface Isolation Utilities - Created `LocationProcessorListener` and `DefaultLocationProcessorListener` with no-op methods to prevent test breakages during interface expansion and stabilize regression testing (R-ID 395). (Resolved Sep.21.131)
*   **SOT ID 394**: GNSS Sampling Logic Consolidation - Encapsulated GNSS sampling policy (standard vs throttled) and auditing triggers in a nested `GnssPolicyEngine` within `HardwareSuite.kt`. This decouples the hardware callback from throttling rules and ensures symmetric auditing of jitter across all performance tiers (R-ID 394). (Resolved Sep.21.128)
*   **SOT ID 393**: Acoustic-SNR Semantic Mismatch & Integration - Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type. Refactored `HistoryManager` and `TelemetryAggregator` to consume this specialized sequence during backfill and gap-filling. This ensures environmental noise telemetry (dB) is semantically isolated from satellite GNSS SNR across the entire forensic pipeline, preventing diagnostic ambiguity in forensic ribbons (R-ID 393). (Resolved Sep.21.127)
*   **SOT ID 392**: Forensic Sequence Hardening - Refactored `CircularStateBuffer.forensicSequence` to use a custom multi-pass sequence implementation that holds internal locks during flyweight transformation. This eliminates the race condition where a high-frequency writer could repurpose objects before the sequence consumer (e.g., `HistoryManager`) could extract their data, and achieves zero-allocation parity by removing the temporary `ArrayList` snapshot (R-ID 392). (Resolved Sep.21.125)
*   **SOT ID 390**: Telemetry Source Abstraction - Refactored alarm evaluation to use unified `AlarmTelemetrySnapshot` and `AlarmServiceContext` DTOs. This eliminates parameter bloat in `AppAlarmManager.evaluateAlarms` and enforces strict isolation between local hardware state and remote telemetry, ensuring that the Viewer's local sensors can no longer inadvertently leak into Tracker alarm logic (R-ID 390). (Resolved Sep.21.123)
*   **SOT ID 389**: HardwareSuite Snapshot Unification - Unified `consumeLogicSnapshot` and `consumeForensicSnapshot` into a single private `privateConsumeSnapshot` method. This eliminates duplicate sensing snapshot extraction code, ensures thread-safety gates, peak resets, and acoustic/vibration floor snapshots are symmetrically maintained (R-ID 389). (Resolved Sep.21.122)
*   **SOT ID 388**: Unified Vibration Authority - Consolidated the `adaptiveVibrationFloor` calculation in `HardwareSuite.kt`. The high-frequency floor is now snapshotted and propagated to `LocationSentinel` via `TrackerService.processTick()`, ensuring that both the hardware layer and the validation engine operate on a single source of truth for stationary detection (R-ID 388). (Resolved Sep.21.121)
*   **SOT ID 387**: Non-Blocking Acoustic Teardown - Removed the synchronous `acousticThread.join(1000)` from `HardwareSuite.stopAcousticMonitoring()`. Resource exclusivity is now maintained via the join-before-start pattern in `startAcousticMonitoring()`, which waits for any lingering thread to exit before initializing a new one. This eliminates service lifecycle stalls and potential ANRs during service termination (R-ID 387). (Resolved Sep.21.121)
*   **SOT ID 386**: GPS Telemetry Conflation Hardening - Replaced single-point location variable in `TrackerService.kt` with a thread-safe `ConcurrentLinkedQueue` buffer. The logic tick now drains and processes all intermediate fixes accumulated between 2-second pulses, preventing the loss of high-resolution trail points and maintaining forensic jitter audit precision (R-ID 386). (Resolved Sep.21.120)
*   **SOT ID 385**: Thread Visibility Hardening - Applied `@Volatile` markers to all critical timing and state variables in `HardwareSuite.kt` (`lastBufferRecordRt`, `stationaryStartRt`, `lastStayAliveRt`, `plungePhase`, etc.) to ensure atomic visibility across the GNSS, Sensor, and Service Tick threads (R-ID 385). (Resolved Sep.20.25)
*   **SOT ID 384**: Forensic State Reset Hardening - Explicitly zeroed all forensic sampling state variables (`recoveryTriggerRt`, `lastWasCooling`, and spatial/IMU gates like `lastForensicLat`) in `TrackerService.resetServiceTimers()`. This ensures that a session restart provides a clean slate for thermal recovery audits and sampling triggers (R-ID 384). (Resolved Sep.20.22)
*   **SOT ID 383**: Vitality Audit Remediation - Reset all vitality update timestamps (`lastInternetUpdateRt`, `lastBatteryUpdateRt`, etc.) in `IntegrityMonitor.resetStats()`. This prevents spurious "Flow Stall" alerts when the service restarts (R-ID 383). (Resolved Sep.20.20)
*   **SOT ID 382**: ForensicAuditor Thread Safety Hardening - Implemented internal synchronization for `RoleState` within `ForensicAuditor.kt`. This ensures atomic check-and-set operations for jitter peaks and stability counters, preventing race conditions between GNSS/Sensor provider threads and the service Tick thread (R-ID 382). (Resolved Sep.20.18)
*   **SOT ID 381**: Light Baseline Synchronization - Implemented periodic re-synchronization of the fast-path baseline in `TrackerService.processTick()`. This prevents divergence between the `LocationSentinel` lux baseline and the high-frequency fast-path check in `HardwareSuite` (R-ID 381). (Resolved Sep.20.18)
*   **SOT ID 380**: Light Fast-Path Integration Hardening - Captured `lastFastPathLightSpikeTs` in `TrackerService` hardware callbacks and propagated it to `LocationProcessor.processGpsPoint()` to trigger immediate locking/tamper responses. Transient light spikes occurring between tick intervals are now acted upon by the validation engine (R-ID 380). (Resolved Sep.20.18)
*   **SOT ID 379**: False GNSS Jitter Remediation - Explicitly zeroed `ForensicAuditor.lastGnssStatusRt` in `reset()` and `resetGnssJitter()`. This ensures that the first GNSS event after a suite restart does not calculate jitter against a timestamp from a previous session (R-ID 379). (Resolved Sep.20.15)
*   **SOT ID 378**: Telemetry Propagation Hardening - Expanded `PendingStatusEntity` and `HistoryEntity` in `Database.kt` (Migration v76) to include `gpsHardwareLock` and `isGnssThrottled` flags. Updated `TelemetryMapper` and `ConnectivitySuite` to ensure these diagnostic markers are persisted during offline drops and correctly reflected on remote peer dashboards (R-ID 378). (Resolved Sep.20.15)
*   **SOT ID 377**: Hardware Lifecycle Hardening - Implemented `clearLifecycleLeftovers()` in `HardwareSuite.kt`. Explicitly zeroed all transient peak accumulators (`secPeakLux`, etc.), proximity markers (`rawProximityNear`), temporal lockouts (`lastAcousticLockoutRt`), and plunge phases on suite stop and baseline reset (R-ID 377). (Resolved Sep.20.15)
*   **SOT ID 376**: Multi-Role Reset Isolation - Updated `HardwareSuite.resetBaseline(roleTag)` and `ForensicAuditor.reset(roleTag)` to support targeted role resets. This ensures that session restarts in the Tracker role do not inadvertently wipe stability or jitter data for an active Viewer role (R-ID 376). (Resolved Sep.20.15)
*   **SOT ID 375**: Light Fast-Path Integration Hardening - Initialized the light sensor fast-path (`setLightFastPath`) in `TrackerService.setupPhysicalFastPaths()` using the baseline from `LocationProcessor` and the `LIGHT_THRESHOLD_LUX_JUMP` threshold. This ensures the specialized light-spike logic in `HardwareSuite` is alive and active for light-based tampering detection. (Resolved Sep.20.10)
*   **SOT ID 374**: Viewer Hardware Isolation Hardening - Resolved a hardware leak in `ViewerService.evaluateAlarmsInternal` where the Viewer's local SNR and vibration snapshots were used to evaluate remote Tracker alarms. Telemetry evaluation now strictly uses `snrIdx` and `vibeIdx` from the remote `TrackerStatus`, ensuring accurate Jammer and Stall detection for tracked devices (R-ID 374). (Resolved Sep.20.02)
*   **SOT ID 373**: GNSS Jitter Audit Correction - Resolved inconsistent jitter calculation in `ForensicAuditor.kt` by replacing the hardcoded 1000ms expected interval with a dynamic parameter. Updated `HardwareSuite.gnssStatusCallback` to calculate the active sampling interval (2s/5s) during GNSS status updates, ensuring that intentional performance throttling no longer triggers false hardware instability alerts (R-ID 373). (Resolved Sep.20.00)
*   **SOT ID 372**: Activity-Denied WakeLock Fallback Hardening - Resolved excessive battery drain in `HardwareSuite.kt` where the system would poke a WakeLock every 10 seconds if Step Detector registration failed, even if the failure was due to user-denied permissions. Implemented a permission check for `ACTIVITY_RECOGNITION` before poking the WakeLock, ensuring the fallback is suppressed when explicitly restricted by the user (R-ID 372). (Resolved Sep.19.13)
*   **SOT ID 371**: Asynchronous Sensor Registration Hardening - Resolved a race condition in `HardwareSuite.kt` where `setPowerSaveMode` could re-register sensors on a stopped suite. Added an explicit `isStarted.get()` check within the posted handler block to ensure sequential lifecycle integrity during rapid mode transitions (R-ID 371). (Resolved Sep.19.12)
*   **SOT ID 370**: Acoustic Monitor Lifecycle Hardening - Resolved a resource race condition in `HardwareSuite.kt` where rapid restarts could cause multiple threads to compete for the `AudioRecord` resource. Implemented `acousticLock` and mandatory thread joining in `startAcousticMonitoring()`, ensuring that any previous monitor session is definitively terminated before a new one initializes (R-ID 370). (Resolved Sep.19.11)
*   **SOT ID 369**: Stale Forensic Buffer Lifecycle Hardening - Resolved an issue in `HardwareSuite.kt` where circular buffers (`sensorBuffer`, `snrBuffer`, `logicSnapshotBuffer`, `forensicSnapshotBuffer`) and the `lastBufferRecordRt` timestamp were not cleared during suite termination. By explicitly resetting these structures in `stop()`, the system now guarantees a clean forensic state for every service session restart, preventing stale data from polluting new monitoring cycles (R-ID 369). (Resolved Sep.19.10)
*   **SOT ID 368**: Snapshot Thread-Safety Hardening - Resolved memory visibility and race conditions in `HardwareSuite.kt snapshotting logic. Applied `@Volatile` to high-frequency shared state variables (lux, acousticDb, tilt, velocity, etc.) to ensure correct cross-thread reads during forensic audits. Unified the synchronization strategy by wrapping both the sensor update handlers and the peak-reset snapshot consumption methods (`consumeLogicSnapshot`, `consumeForensicSnapshot`) in `synchronized(this)`, guaranteeing atomic "read-and-reset" operations under high system load (R-ID 368). (Resolved Sep.19.09)
*   **SOT ID 367**: Forensic Multi-Role Integrity Hardening - Resolved state collision in `ForensicAuditor` by implementing role-based (`T` for Tracker, `V` for Viewer) state tracking using a `ConcurrentHashMap`. Each role now maintains its own stability audit counters, GNSS jitter peaks, and sensor rate audit flags, ensuring accurate forensic reporting when both services run concurrently on the same device (R-ID 367). (Resolved Sep.19.08)

## 🧬 Change History (Recent)
*   **Aug.28.10**: Resolved Concern #758 (UI Thread Congestion). Offloaded OSMDroid engine pre-warming to IO thread and added `isOsmReady` gate to `LifecycleHydrationManager` (R758). Updated Rule 2.1.
*   **Aug.28.09**: Resolved Concern #757 (Persistent BaseEventQueue Leak). Refactored `GpsManager` to perform unconditional cleanup of location callbacks and hardware threads during `stop()`, preventing leaks from orphaned background revival callbacks (R757). Updated Rule 1.8.
*   **Aug.28.08**: Resolved Concern #759 (Logcat Spam Remediation). Migrated `MainActivity` and `BaseMonitorService` to `PACKAGE_NAME` shadow-cache to eliminate repetitive system-level logs (R759). Added Rule 1.9.
*   **Aug.28.07**: Resolved Concern #756 (Persistent GNSS/Network Leak). Hardened `ManagedHardware` with fallback unregistration paths and added explicit trace logging to `GpsManager` and `CommunicationManager` to silence `BaseEventQueue` warnings (R756).
*   **Aug.28.06**: Resolved Concern #755 (GNSS & Network Unregistration Hardening). Standardized GNSS unregistration by implementing `ManagedGnssStatusCallback` in `ManagedHardware.kt`.
*   **Aug.28.05**: Resolved Concern #754 (Managed Sensor Abstraction). Introduced `ManagedSensorListener` and `ManagedDisplayListener` to standardize synchronous hardware unregistration.
*   **Aug.28.03**: Resolved Concern #752 (Persistent BaseEventQueue Leak). Remediated deadlock in ManagedNetworkCallback.unregister by implementing Main Looper detection.
*   **Aug.28.02**: Resolved Concern #751 (Native Connectivity Leak). Implemented Managed Hardware Abstractions (ManagedNetworkCallback, ManagedLocationCallback) to unify deterministic disposal logic (R750).
*   **Aug.28.01**: Resolved Concern #750 (Native Connectivity Leak). Hardened NetworkCallback unregistration in ConnectivitySuite and SystemStatusProvider to ensure synchronous disposal on the Main Looper (R750).
*   **Aug.28.00**: Resolved Concern #749 (Persistent BaseEventQueue Leak). Hardened all callbackFlows in SystemStatusProvider (Internet, Battery, Power) to follow SOT 1.8 with deterministic unregistration in awaitClose (R749).

## 📋 Functional Requirements (144 R-IDs)
*   **R101**: Background location tracking continuity (High-Uptime).
*   **R102**: Real-time telemetry synchronization via Socket.io.
*   **R103**: Forensic event logging with microsecond precision.
*   **R104**: Geo-fencing authority with configurable distance thresholds.
*   **R105**: Battery steep discharge detection and alerting.
*   **R106**: Thermal mitigation and performance throttling (Cooling Mode).
*   **R107**: Offline data buffering and batch synchronization.
*   **R108**: Proactive database pruning (Logs, Trails, History).
*   **R109**: Secure identity sanitization and persistence.
*   **R110**: ApplicationContext enforcement for dependency injection.
*   **R112**: Deterministic service destruction and resource release.
*   **R113**: Thread-safe atomic state management (StateFlow/Mutex).
*   **R114**: Foreground service notification persistence.
*   **R115**: IO offloading from UI thread (Hardened IO).
*   **R116**: Monotonic time reference for interval detection.
*   **R117**: Centralized telemetry repository as Single Source of Truth.
*   **R196-V**: Manual validation hooks for alarm simulation.
*   **R197**: Chunked and staggered database pruning.
*   **R212**: Neutral hardware namespace (jdHardware).
*   **R240**: Centralized UI state aggregation (UiStateAggregator).
*   **R243**: Automated recovery to previous active mode (<2s).
*   **R248**: Segmented hydration flows for budget hardware.
*   **R250**: Navigation backstack continuity (popUpTo/launchSingleTop).
*   **R280**: LRU strategy for shadow-caches.
*   **R301**: JNI watchdog timer (2000ms).
*   **R307**: Maintenance duration monotonic authority.
*   **R309**: Map overlay isolation from Compose Snapshots.
*   **R310**: Heuristic OS trigger neutralization (mbrainSDK).
*   **R312**: Snap-Isolation throttling for telemetry flows.
*   **R314**: ViewModel initialization staggering.
*   **R315**: GPS stabilization grace period (30s).
*   **R317**: Decoupled hardware identification (HardwareSot).
*   **R318**: Map Engine Level 4-7 hydration via IdleHandler.
*   **R319**: Native initialization exponential backoff retry.
*   **R323**: Multi-frame staggered engine initialization.
*   **R700**: Forensic sampling frequency (10ms-100ms).
*   **R715**: Performance spike reliability threshold (0.85 EMA).
*   **R735**: Manual overlay bypass validation hooks.
*   **R737**: DataStore persistence for identity sanitization dismissal.
*   **R738**: Hardware lifecycle synchronization and atomic registration.
*   **R739**: Sub-millisecond execution blocks for map hydration.
*   **R742**: Lifecycle-bound GNSS callback persistence.
*   **R744**: Explicit activeLocationCallback unregistration in stop().
*   **R745**: Hardware thread-queued sensor unregistration.
*   **R746**: Synchronous hardware thread join during disposal.
*   **R747**: FusedLocationProvider unregistration task awaiting.
*   **R748**: CallbackFlow awaitClose unregistration synchronization.
*   **R749**: SystemStatusProvider callbackFlow hardening.
*   **R750**: NetworkCallback synchronous disposal on Main Looper.
*   **R752**: Deadlock-free unregistration utility (Looper detection).
*   **R753**: ManagedBroadcastReceiver standardization.
*   **R754**: ManagedSensor/DisplayListener abstraction.
*   **R755**: Standardized GNSS unregistration timeout (2000ms).
*   **R756**: Trace-logged unregistration handshake verification.
*   **R757**: Unconditional cleanup of revival location callbacks.
*   **R758**: IO-thread pre-warming of OSM engine and gating.
*   **R759**: PackageName shadow-cache for IPC optimization.
*   **R456**: Heartbeat idempotency and stream deduplication in ViewerService.
*   *(Remaining 84 functional requirements preserved in the project's internal technical registry)*

## 4.3. Metric Summary
- **Rules Verified**: 92
- **Total SOT IDs**: 460
- **Resolved Issues**: 1197
- **Open Issues**: 16
- **Testing Coverage**: 3 (Sub-items: 12)
- **Simplification Ideas**: 18
- **QA Validation Tasks**: 284

## 🏁 Verification Chapters
*   **Chapter 31.94 (Atomic User Counter Hardening)**: PASSED - Verified that `activeUsers` in HardwareSuite is guarded against negative values and that deferred teardown robustly unregisters listeners using the <= 0 check (Sep.24.04).
*   **Chapter 31.93 (Persistent Adaptive Vibration Floor)**: PASSED - Successfully implemented persistence for the vibration floor anchor in LocationProcessor and registered role-isolated DataStore sync in background services (Sep.22.30).
*   **Chapter 31.92 (Reboot-Aware Monotonic Clock Recovery)**: PASSED - Successfully implemented reboot detection and synthetic RT anchor recovery in HistoryManager to survive device restarts (Sep.22.30).
*   **Chapter 31.91 (Fast-Path Allocation Optimization)**: PASSED - Refactored HardwareFastPath and TrackerService tick loop to eliminate allocation churn by making spike detection callbacks optional. (Sep.22.30)
*   **Chapter 31.85 (Unified Hardware Lifecycle & Vendor Hardening)**: PASSED - Successfully implemented and verified functional background hardening logic in DeviceHardeningStrategy for Samsung, Huawei, and Xiaomi devices. (Sep.22.30)
*   **Chapter 31.90 (Heartbeat Idempotency)**: PASSED - Verified that ConnectivityEvent.PeerPulse is processed via a single observer in ViewerService, preventing duplicate log events and session updates. (Sep.22.30)
*   **Chapter 31.89 (Role-Based Storage Namespacing)**: PASSED - Successfully verified that Tracker and Viewer logic states are stored in isolated proto maps, preventing cross-role state corruption during functional transitions. (Sep.22.30)
*   **Chapter 31.88 (Race Condition & Initialization Safeguard)**: PASSED - Verified that `initializationDeferred.await()` successfully blocks background tick execution and heartbeat processing until `onServiceInitialize()` completes. (Sep.22.30)
*   **Chapter 31.87 (Siren Trigger Orchestration)**: PASSED - Successfully integrated siren activation/deactivation into the alarm evaluation cycle, ensuring hardware-reactive alerts in background services. (Sep.22.30)
*   **Chapter 31.86 (Centralized SSOT ViewModel Architecture)**: PASSED - Successfully unified Tracker, Viewer, and Setup states into MainViewModel, resolving multi-subscription churn and ephemeral state loss. (Sep.22.30)
*   **Chapter 31.84 (Logic State Persistence Expansion)**: PASSED - Verified seamless preservation of active alarm trigger realtimes and logging timestamps inside persistent JSON structures. (Sep.22.30)
*   **Chapter 31.83 (Shared Overlay Scope)**: PASSED - Successfully centralized shared overlays into OverlayHost within MainAppContent. (Sep.22.30)
*   **Chapter 31.82 (Draft Settings Synchronization)**: PASSED - Verified real-time input reflection across functional roles after consolidating draft logic into MainViewModel. (Sep.22.30)
*   **Chapter 31.81 (Siren State Synchronization)**: PASSED - Converted siren playback feedback to StateFlow, ensuring cross-ViewModel reactive state consistency. (Sep.22.30)
*   **Chapter 31.80 (Config & Trail Import)**: PASSED - Verified seamless configuration and trail point loading via MainFileHelper. (Sep.22.30)
*   **Chapter 31.79 (ViewModel Decomposition)**: PASSED - Successfully decomposed monolithic MainViewModel into role-specific ViewModels. (Sep.22.30)
*   **Chapter 31.78 (Forensic & Sensor Efficiency Optimization)**: PASSED - Grouped telemetry and health fields into unified EvaluationSnapshot DTO. (Sep.22.30)
*   **Chapter 31.77 (Elimination of Multi-pass Fallbacks)**: PASSED - Grouped sensor branches into structured snapshots. (Sep.22.30)
*   **Chapter 31.76 (Vibration Floor Adaptation Guard)**: PASSED - Guarded autonomous vibration floor adaptation fallback. (Sep.22.30)
*   **Chapter 31.75 (Fast-Path Baseline Preservation)**: PASSED - Verified preserveExistingBaseline updates light fast-path correctly. (Sep.22.30)
*   **Chapter 31.74 (Thermal Recovery Latency Audit)**: PASSED - Corrected recovery latency check across iteration passes. (Sep.22.30)
*   **Chapter 31.73 (Vibration Floor Semantic Alignment)**: PASSED - Corrected getAdaptiveVibrationFloor to return adaptiveVibrationFloor. (Sep.22.30)
*   **Chapter 31.72 (Acoustic Fast-Path Adaptation)**: PASSED - Passing dynamic adaptation alpha to acoustic fast path evaluation. (Sep.22.30)
*   **Chapter 31.71 (Trigger Sampling)**: PASSED - Transitioned from fixed-interval loop to reactive signal-on-spike sampling. (Sep.22.30)
*   **Chapter 31.70 (State Partitioning)**: PASSED - Split MainUiState into specialized slices to isolate volatile triggers. (Sep.22.30)
*   **Chapter 31.69 (Fast-Path Unification)**: PASSED - Unified acoustic and light fast-paths in HardwareSuite. (Sep.22.30)
*   **Chapter 31.68 (Vendor Centralization)**: PASSED - Centralized hardware adaptations in DeviceProfileManager. (Sep.22.30)
*   **Chapter 31.67 (UseCase Consolidation)**: PASSED - Verified creation of SpatialLogicUseCase. (Sep.22.30)
*   **Chapter 31.66 (Persistence Refactoring)**: PASSED - Verified generic mutate extension and unified repository operations. (Sep.22.30)
*   **Chapter 31.65 (Atomic Geofence)**: PASSED - Verified race-free home point updates and persistent ADD mode. (Sep.22.30)
*   **Chapter 31.64 (GNSS Count Standard)**: PASSED - Distinguish zero from uninitialized telemetry states. (Sep.22.30)
*   **Chapter 31.62 (Temperature Unit Layout)**: PASSED - Corrected SI unit presentation in StatusRowData. (Sep.22.30)
*   **Chapter 31.61 (Session Lifecycle Coordinator)**: PASSED - Unified background session resets atomically across roles. (Sep.21.132)
*   **Chapter 31.60 (Interface Isolation Utilities)**: PASSED - Created LocationProcessorListener & DefaultLocationProcessorListener. (Sep.21.131)
*   **Chapter 31.59 (Dead Code Elimination)**: PASSED - Removed unused tracking property leftovers. (Sep.21.130)
*   **Chapter 31.58 (GNSS Consolidation)**: PASSED - Verified nested GnssPolicyEngine evaluation pattern. (Sep.21.128)
*   **Chapter 31.57 (Acoustic Refactoring)**: PASSED - Verified HistoryManager/TelemetryAggregator integration of EngineAcousticSample. (Sep.21.127)
*   **Chapter 31.49 (Telemetry Conflation)**: PASSED - Verified location buffer drainage in TrackerService. (Sep.21.120)
*   **Chapter 31.48 (Thread Visibility)**: PASSED - Verified Volatile markers in HardwareSuite. (Sep.22.30)
*   **Chapter 31.47 (Forensic Reset)**: PASSED - Verified TrackerService sampling state reset. (Sep.22.30)
*   **Chapter 31.46 (Vitality Timestamps)**: PASSED - Verified IntegrityMonitor timestamp reset. (Sep.22.30)
*   **Chapter 31.44 (Light Sync)**: PASSED - Verified periodic baseline synchronization in processTick. (Sep.22.30)
*   **Chapter 31.43 (Light Fast-Path)**: PASSED - Verified light spike propagation to LocationProcessor. (Sep.22.30)
*   **Chapter 31.42 (False Jitter)**: PASSED - Verified lastGnssStatusRt reset in ForensicAuditor. (Sep.22.30)
*   **Chapter 31.41 (Telemetry Parity)**: PASSED - Verified diagnostic flag persistence in Database/Mapper. (Sep.22.30)
*   **Chapter 31.40 (Lifecycle Peaks)**: PASSED - Verified clearLifecycleLeftovers in HardwareSuite. (Sep.22.30)
*   **Chapter 31.39 (Multi-Role Reset)**: PASSED - Verified role-based resets in Auditor/HardwareSuite. (Sep.22.30)

---
*Next Audit: Sep.24.10. (Sep.22.30)*
