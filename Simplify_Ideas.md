# Simplify & Optimization Ideas (Aug.18.04)

## 1. Tracking Engine & Forensic Pipeline
1.1. **Flyweight & Pooling**:
    1.1.1. Expand flyweight patterns to all entities (Telemetry, Violations, SpatialPoints).
    1.1.2. Use pre-allocated ring buffers for `EngineConnectionPoint` and a `LogEntry` object pool.
    1.1.3. Implement thread-local pooled flyweights for high-frequency processing to eliminate GC churn.
    1.1.4. Wrap flyweight access in `use` blocks or specific scopes to ensure thread safety across yield boundaries.
    1.1.5. Implement flyweight patterns for Room insertions to reduce allocation pressure during batch writes.
1.2. **Unified Trajectory & Buffer Management**: 
    1.2.1. Merge `GtoEngine` windows and `LocationSentinel` hindsight buffers into a single, optimized `TrajectoryBuffer`.
    1.2.2. Extract anchor breakout logic from `LocationProcessor` into a standalone `AnchorMonitor` (or `AnchorEvaluator`) class to improve readability of the state machine.
    1.2.3. Implement a `SpatialStabilizer` and `JumpEngine` to consolidate jump scoring and smoothing logic.
    1.2.4. Standardize on `PrimitiveCircularBuffer<T>` and `FixedSizeQueue` utilities for EMA/averaging logic.
    1.2.5. Use a circular buffer for displacement trends to simplify `anchorTrendPoints` logic and make window sizes configurable.
    1.2.6. Merge "Parking Anchor" and "Home Point" logic into a consolidated `SpatialHardLock` manager.
1.3. **Forensic & Sensor Efficiency**:
    1.3.1. Move index calculations (`noiseIdx`, `luxIdx`) directly into `ForensicSnapshot` or a domain mapper.
    1.3.2. Implement a `ForensicIterator` (`forEachInRange`) to avoid `Sequence` object allocations during batch processing.
    1.3.3. Use `EvaluationSnapshot` and `ProximityState` objects to group related sensor/GPS data for atomic processing.
    1.3.4. Consolidate all sensor capturing into a single `consumeForensicSnapshot()` call in the service tick loop to ensure atomic state capture.
    1.3.5. Move index/vitality timestamps into a `VitalityRegistry` (Map-based) with extension functions.
    1.3.6. Abstract delta-encoding and bit-packing into a `ForensicCodec` and use `EngineBitset` for forensic flags.
    1.3.7. Consolidate episodic telemetry (History, Traces) into a shared `ForensicSpillBuffer`.
1.4. **Stateless & Functional Logic**:
    1.4.1. Migrate `LocationProcessor` to a functional model using a `ProcessorState` data class passed with each point.
    1.4.2. Sentinel should return a `StationaryConfidence` object encapsulating both boolean state and probability weight.
    1.4.3. Refactor Bayesian drift calculation into `AlarmEvaluationState`.
    1.4.4. Implement heuristic auto-tuning for anchors based on average SNR and confidence-based damping.
    1.4.5. Apply accuracy-weighted averaging (1/accuracy) for anchor settling so high-precision fixes have more influence.
    1.4.6. Implement generic `Accumulator<T>` for sensor metrics and a centralized `EMA Utility`.
1.5. **Persistence of Logic State**:
    1.5.1. Serialize `AlarmHistory` into the database/DataStore to ensure geofence debounce states and power alarm latches survive process death or system-initiated kills during deep sleep.
1.6. **Sensor and Telemetry Decoupling**:
    1.6.1. Implement a `SensorRepository` that exposes a clean `Flow<Telemetry>` which the Aggregator collects, decoupling the Background Service from data aggregation logic.
1.7. **Primitive Unification**: 
    1.7.1. Move forensic indices to a single `DoubleArray` in `ProcessedLocation`. 
    1.7.2. Use a `ProcStatParser` for system metrics.
1.8. **Engine Snapshots**:
    1.8.1. Create a unified `EngineStateSnapshot` (status, accuracy, behavioral flags) to decouple processor from service logic.
    1.8.2. Pass a complete `SystemSnapshot` to `MainAlarmLogic` to simplify unit testing of urban edge cases.
    1.8.3. The `TrackerStatus` model could contain the `ForensicSnapshot` directly to reduce telemetry mapping boilerplate.
    1.8.4. Consolidate all forensic models into a single shared module to eliminate mapping boilerplate and prevent field-drift.

## 2. UI & Compose Performance
2.1. **State Partitioning & Aggregation**:
    2.1.1. Split `MainUiState` into `PersistentState` (settings) and `TransientState` (real-time telemetry).
    2.1.2. Decompose state into specialized slices (`MapUiState`, `DashboardUiState`, `MapVisualState`) to minimize recomposition evaluation costs.
    2.1.3. Consolidate remote tracker data into a single `RemoteTrackerState` flow (location + health) derived from the repository.
    2.1.4. Group individual metric flows in `MainViewModel` into a single `TelemetryState` object to reduce the number of collectors in the UI and simplify updates.
    2.1.5. Wrap peer state variables (~40) in `ConnectivitySuite` into a single immutable `PeerState` data class exposed via `StateFlow` to reduce update overhead.
    2.1.6. Use a `SessionStateAggregator` for heavy observations to simplify the cold-start path.
    2.1.7. Standardize on `collectAsStateWithLifecycle` for transient states to reduce ViewModel overhead.
2.2. **Map & Overlay Optimization**:
    2.2.1. Extract imperative osmdroid management into a standalone `MapOverlayController` to keep UI code declarative.
    2.2.2. Implement a "Static Mode" for the map that replaces the live engine with a static snapshot when heavy overlays (Settings/Setup) are active, reducing resource contention.
    2.2.3. Transition to a "dirty-flag" invalidation model where `invalidate()` is only called on data changes.
    2.2.4. Use background Flow operators (`sample`, `conflate`) for coordinate smoothing and EMA calculation.
    2.2.5. Implement distance-based trail sampling and coordinate conflation before the UI layer.
    2.2.6. Use a `rememberSmoothedGeoPoint` state delegate for marker smoothing to declutter composables.
    2.2.7. Implement `StandardMapButton` and a contrast-aware `MapOverlayTheme`.
    2.2.8. Unified History Handling: Simplify the manual map of history flows by using a single flow of a Map managed within a `HistoryManager` or `HistoryProvider`.
2.3. **Rendering & Composable Hygiene**:
    2.3.1. Use `LazyColumn` for complex overlays (e.g., `PhoneSetupOverlay`) to avoid composition pressure.
    2.3.2. Defer heavy data hydration until after transition animations start.
    2.3.3. Move ribbon and custom drawing logic to reusable `Modifier.drawRibbon`.
    2.3.4. Implement a "Low-Performance Mode" triggered by hardware stress (IO-wait) or high CPU load.
    2.3.5. Unified Rendering Engine: Define sensor ribbons via a data-driven configuration list (e.g., in `AnalyticalRibbons`) to make adding/removing sensors easier.
    2.3.6. Use `PreviewParameterProvider` and a `PreviewPrototyper` to simplify UI testing and mocking.
    2.3.7. Implement `Lambda Stability` (memoization) and parameter bundling (`MarkerData`) to reduce recomposition.
    2.3.8. **Visual Gating**: Introduce a `MainViewGate` composable that selectively detaches or replaces high-churn UI components when they are not in the viewport, further reducing recomposition costs in dense dashboards.
2.4. **Reactive Primitives**:
    2.4.1. Use `DashboardPresenter` or `State Reducers` to pre-compute display strings/formatting before they reach Composables.
    2.4.2. Move label formatting (e.g., `standbyBucket` text mapping) from the UI layer into the `DashboardState` mapper to keep presenters "dumb."
    2.4.3. Implement `SharedFlow` for UI triggers to avoid manual `combine` blocks in ViewModels.
    2.4.4. Offload metric calculations (e.g., average blackout duration) to `DashboardStateProvider` as a Flow transformer.

## 3. Hardware, Permissions & System Status
3.1. **Unified Hardware Lifecycle**:
    3.1.1. Create a standalone `HardwareRegistry` that manages its own lifecycle based on a `PermissionFlow`.
    3.1.2. Consolidate vendor-specific adaptations into a central `DeviceProfileManager` or `DeviceHardeningStrategy` (e.g., for Samsung, Xiaomi, Huawei, Oppo).
    3.1.3. Unify WakeLock and "Hardware Punch" logic into a single `HardwareStayAliveManager`.
    3.1.4. Stay-Alive Abstraction: Move stay-alive logic from `onSensorChanged` into a separate `ProcessPriorityMonitor` to clarify its purpose as process lifecycle management.
    3.1.5. Use a `HardwareFeatureManager` to expose capabilities like `requiresJniPunching()`.
    3.1.6. Introduce `AutoCloseableScope` to ensure resource cleanup for background providers.
    3.1.7. Unified Overlay Trigger: Use a single `SharedFlow` in the ViewModel to trigger "System Alerts" or "Setup Prompts" instead of toggling boolean flags from multiple places.
3.2. **Sensor Optimization**:
    3.2.1. Use a `SensorDelegate` to handle registration/unregistration logic in `AppSensorManager`.
    3.2.2. Transition to a more reactive, Flow-based sensor emission pattern, allowing components to subscribe only to the sensors they need to reduce CPU overhead.
    3.2.3. Sensor Pulse Decoupling: Expose the stay-alive pulse as a `Flow<Long>` observed by `BaseMonitorService` to trigger OS pings in a centralized way.
    3.2.4. Create a `SensorCapabilityRegistry` (Map-based) to unify sensor operational state and fallback logic.
    3.2.5. Unified Recovery Logic: Implement a generalized `HardwareRecoveryManager` to handle periodic health checks and re-registrations for all critical sensors.
3.3. **Reactive Permissions & Monitoring**:
    3.3.1. Consolidate all permission logic into a `PermissionCoordinator` or a core-engine `PermissionRegistry`.
    3.3.2. Permission State Consolidation: Simplify granular `PermissionState` by having a single `isSystemReady` derived state in the ViewModel to reduce recomposition triggers.
    3.3.3. Implement event-driven permission updates using a `BroadcastReceiver` (e.g., for `ACTION_BATTERY_CHANGED` or system events) instead of polling.
    3.3.4. Consolidate independent polling loops into a single "System Pulse" broadcast from `SystemStatusProvider`.
    3.3.5. Move permission refresh logic into a dedicated flow that reacts to `onResume`.
    3.3.6. Implement a lightweight event bus to notify hardware managers immediately when permissions are granted.
3.4. **Health & Metrics**:
    3.4.1. Use EMA for jitter to create a "Hardware Capability Score" (Health Index).
    3.4.2. Combine /proc and system reads into an atomic "Hardware Snapshot" updated on a background thread.
    3.4.3. Implement `Reactive Storage Monitoring` using a `FileObserver`.
    3.4.4. Provide a unified `SystemHealthSnapshot` via `IntegrityMonitor`.
    3.4.5. Expose socket connectivity and internet availability as a single `ConnectivityStatus` flow to simplify UI badge logic.
    3.4.6. Consolidated Tick Authority: Create a dedicated `SystemHeartbeatManager` for the "Alive" signal to centralize authority and reduce redundant writes.

## 4. Architecture & Lifecycle
4.1. **ViewModel & UseCase Consolidation**:
    4.1.1. Split the "God Object" `MainViewModel` into feature-specific ones (`Tracker`, `Viewer`, `Setup`).
    4.1.2. Merge behavior-related UseCases into a single `BehaviorAuthority` in the core engine.
    4.1.3. Create a `FeedbackController` to handle all side-effects (Audio, Haptic, Visual) in one place.
    4.1.4. Standardize all UI-facing UseCases to emit on `Dispatchers.Main.immediate`.
    4.1.5. Merge `TelemetryUseCase` and `StateSubscriptionUseCase` into a single `ForensicStateUseCase` to handle temporal alignment.
    4.1.6. Create a `ForensicAnalyticsProcessor` to decouple UI events from data math.
    4.1.7. Unified Config Flow: Merge `SettingsUseCase` and `SessionUseCase` into a single `SessionManager` to reduce cross-injection overhead.
    4.1.8. Repository Facade: Consolidate repositories into domain-specific facades or a cohesive "Data Hub" (e.g., splitting `MainRepository` into `Live` and `Storage`).
    4.1.9. Merge specific UseCases into broader domains (e.g., `DeviceHealthUseCase`) to reduce constructor injection overhead.
    4.1.10. Unified State Manager: Extract more state logic into a dedicated `TrackerStateManager` to simplify the ViewModel and improve testability.
4.2. **Service & Worker Orchestration**:
    4.2.1. Merge `BootReceiver` and `MaintenanceWorker` into a single `ServiceLifecycleManager` or `RevivalUseCase`.
    4.2.2. Replace arbitrary `delay()` calls with a `SystemReadiness` provider (event-driven startup via `onReady` signals).
    4.2.3. Simplify UI Mode Transitions: Implement a `StartupUseCase` that emits a sealed `StartupState` (TrackerMode, ViewerMode, SetupRequired) to make logic reactive.
    4.2.4. Implement a `ServiceRestorationUseCase` to handle transitions from landing page to tracking.
    4.2.5. Introduce an `IdempotentComponent` interface for start/stop guards.
    4.2.6. Lifecycle-Aware Connectivity: Use service lifecycle observers for `ConnectivitySuite` to handle `stop()` calls automatically.
    4.2.7. Promote watchdog logic to `WorkManager` with expedited jobs.
    4.2.8. Use a `ServiceIntentFactory` to centralize service intent creation logic.
    4.2.9. Worker Registry: Consolidate maintenance and other periodic tasks into a single `SystemHealthWorker`.
    4.2.10. Implement a local "Tick Event" bus (Flow) in `TrackerService` to decouple internal managers.
    4.2.11. Centralize Audit Logic: Move stability audit logic into `BaseMonitorService` or a dedicated `StabilityAuditUseCase`.
    4.2.12. Worker Policy: Use `ExistingPeriodicWorkPolicy.UPDATE` for `MaintenanceWorker` to ensure grace period changes apply immediately on app update.
    4.2.13. Service Unification: Consider merging `TrackerService` and `ViewerService` into a single `MonitorService` that changes behavior based on `appMode`.
    4.2.14. State Observation Unification: Replace manual `isUiForeground` `AtomicBoolean` in `BaseMonitorService` with a Hilt-provided `Flow<UiState>` from a dedicated `SessionManager`.
4.3. **DI & Hilt Transition**:
    4.3.1. Complete the migration of all remaining components (BaseMonitorService, descendants) to Hilt and delete manual DI files (`AppContainer.kt`, `MainViewModelFactory.kt`).
    4.3.2. Hilt Navigation: Migrate the UI to Jetpack Navigation with `hiltViewModel()` to enable scoped ViewModels.
4.4. **Initialization & DI**:
    4.4.1. Use a `HardwareInitializer` via `androidx.startup` for deferred JNI loading and background warm-up.
    4.4.2. Implement an `InitializationCoordinator` to manage warm-up phases on `Dispatchers.IO`.
    4.4.3. Inject a `DispatcherProvider` to manage threading policies globally.
    4.4.4. Use a `NativeResult` sealed class for JNI outcomes and a shared `NativeDispatcher` handling locks and retries.
    4.4.5. Introduce `SystemComponent` interface with `start()` and `stop()` to enforce idempotency.
    4.4.6. Move common hardware recovery logic into a `HardwareLifecycleCoordinator`.
    4.4.7. Unified Logger: Remove legacy `Provider<T>` wrappers in `LogManager` if circular dependencies are resolved.

## 5. Communication & Data
5.1. **Smart Signaling Dispatcher**:
    5.1.1. Merge conflation and throttling logic into a single "Smart Dispatcher" that handles inter-frame delays and event overwriting.
    5.1.2. Expose socket connection state as a `StateFlow<Boolean>` for reactive queue control.
    5.1.3. Replace manual loops with `Flow.collect()` on `MutableSharedFlow` with custom transforms for delay/lifecycle management.
    5.1.4. Add a `SignalingPriority` field to Protobuf models instead of string-matching in the manager.
5.2. **Protobuf-First Migration**:
    5.2.1. Remove `org.json` dependencies from the engine and migrate all pings/pongs/pulses to the Protobuf binary path.
    5.2.2. Use `RealtimeStatus` Protobuf objects as the primary carrier, avoiding intermediate JSON/Map layers.
    5.2.3. Implement a pure Kotlin `SignalPayloadGenerator` (Map-based) for signaling.
    5.2.4. Remove "proxy" getters in `ConnectivitySuite` (e.g., `trackerLat`) in favor of observing the `trackerStatus` flow directly.
    5.2.5. Group service-lifecycle flags into a single `ServiceLifecycleState` message in the Protobuf schema.
5.3. **Data & Repository**:
    5.3.1. Consolidate peer status hydration and initialization into a dedicated `PeerRepository`.
    5.3.2. Move complex JSONObject parsing and Protobuf mapping from `ConnectivitySuite` into a `TelemetryUseCase` or `TelemetryParser`.
    5.3.3. Consolidate `ConnectivitySuite` and `RemoteHandler` shared state into a single `PeerStateManager` or `RemoteStatusRepository`.
    5.3.4. Generic Mapping Layer: Implement `BaseMapper` or extension functions for Room entities to UI models to reduce repetitive boilerplate.
    5.3.5. Shared Initialization Logic: Move `initializePeerState` and `applyPeerStatus` hydration into a dedicated `PeerRepository` to decouple from direct DB/Settings management.
5.4. **DataStore & Persistence**:
    5.4.1. Create a generic extension function for atomic `AppSettings` updates to replace builder boilerplate.
    5.4.2. Use a property delegate or code generation for DataStore to automate key-to-proto mapping.
    5.4.3. Migrate manual `SettingsMapper` logic into a custom DataStore `Converter`.
    5.4.4. Merge `ConfigManager` logic directly into `SettingsRepository`.
    5.4.5. Centralize all DataStore key strings into a single `core:engine` constant file.
    5.4.6. Consolidate `AppSettingsMigration` and sanitization logic into a single `SettingsMigrationManager`.
5.5. **Database & Maintenance**:
    5.5.1. Coordinate all DB tasks (pruning, vacuuming, integrity) into a single `DatabaseMaintenanceManager`.
    5.5.2. Database Migration Automation: Enable `exportSchema = true` and use Room `AutoMigration` for simpler schema changes.
    5.5.3. Refactor the Migration Baseline: Set a new "Baseline" version (e.g., version 50) and use `fallbackToDestructiveMigration()` for very old versions.
    5.5.4. Harmonize Type-Safety for Default Values: Use a sealed class or constants (e.g. `PersistenceDefaults.REAL`) for Entity definitions and migration SQL.
    5.5.5. **Protobuf-First Persistence**: Use Protobuf BLOBs in Room entities like `HistoryEntity` to avoid frequent schema migrations for new sensors.
    5.5.6. Implement batching for log emissions and batching optimizations for Room writes using a bounded queue.
    5.5.7. Pruning Policy: Centralize database pruning logic into a `WorkManager` task rather than triggering inside write operations.
    5.5.8. Bypass JVM heap by binding raw primitive arrays from buffers directly to SQLite `INSERT` statements.
    5.5.9. Use `ByteBufferPool` to manage reusable segments for memory-mapped buffers.
    5.5.10. Implement `Byte Array Pooling` (RingBuffer/BytePool) to manage outgoing signaling fragments.
5.6. **Cleanup**: 
    5.6.1. Prune the `relay-server` submodule if no longer used to simplify project-wide greps.

## 6. Testing, Quality & Utilities
6.1. **Infrastructure & Quality**:
    6.1.1. Create a `DefaultLocationProcessorListener` with no-op methods to prevent test breakages during interface expansion.
    6.1.2. Centralize mock generation for `EngineGeoPoint` and `EngineConnectionPoint` in a unified factory.
    6.1.3. Create isolation tests for components like `AnchorMonitor`, `JumpEngine`, and `LocationHealthEvaluator`.
    6.1.4. Implement a `PerformanceMonitor` for auditing bottlenecks (transactions, serialization, JNI).
    6.1.5. Implement a `ProductionTree` for Timber to filter platform-specific noise.
    6.1.6. Offload `NotificationManager.notify()` calls to a background Handler to protect main thread frames.
    6.1.7. Use `StrictMode` in debug builds to catch disk/network I/O on the main thread.
6.2. **Shared Utilities**:
    6.2.1. Centralize duration formatting, log sanitization, and UTF-8 truncation.
    6.2.2. Move `calculateFreshness` and other physics helpers to `PhysicsUtils`.
    6.2.3. Modularize `EngineConstants.kt` into `MotionConstants.kt`, `SensorConstants.kt`, and `AlertConstants.kt` to reduce compilation overhead.
    6.2.4. Align tick and polling intervals strictly at the `EngineConstants` level.
    6.2.5. Use a global shadow-cache for `PACKAGE_NAME` to reduce Context drilling.
    6.2.6. Implement a `ThrottledLogger` for high-frequency internal events.
    6.2.7. Centralize alert construction in an `AlertDescriptor` factory (or `LocalityTerminologyProvider`) for localization.
    6.2.8. Automate versioning using a shared `version.gradle` and Git-based `versionCode` to prevent manual errors.

## 7. Strategic Simplification
7.1. **Real-time Only Path (Pivot Option)**:
    7.1.1. Consider removing the backlog sync (`PendingStatusDao`) and forensic backfilling (gap interpolation) entirely.
    7.1.2. Move to a "What You See Is What You Got" model to dramatically reduce maintenance for a single developer.
    7.1.3. Sync Flush Rejection: Reject offline points and only process the latest live update.
    7.1.4. Forensic Backfilling Removal: Show gaps in ribbons instead of calculating estimated/interpolated data.
7.2. **Documentation Integrity**:
    7.2.1. Periodically sync `Recently Resolved Issues` from `issues.md` to `STATUS/RESOLUTION_ARCHIVE.md` to prevent sync drift.
    7.2.2. Implement a CI check to verify that `versionName` in `build.gradle` matches documentation headers.
    7.2.3. Use automated scripts for tagging and push events to avoid manual versioning errors.
