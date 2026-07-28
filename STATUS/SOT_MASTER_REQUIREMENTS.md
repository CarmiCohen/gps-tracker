# System Source of Truth (SoT) - July.27.13 (UX Hardened)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Budget Hardware Hardening (R606)**: On restricted hardware (e.g., Samsung A15), high-frequency platform callbacks (GPS/GNSS) MUST be offloaded to dedicated HandlerThreads. UI state re-computation MUST be sampled (min 3000ms) and conflated to prevent Main Thread starvation during cold-start I/O storms. (Issue #606, July.27.11)
*   **Foreground Service Startup Sync (R607)**: Foreground services MUST establish notification channels and role-specific configurations (Tracker/Viewer) synchronously on the Main thread within `onCreate()` BEFORE invoking `startForeground()`. This prevents "Bad notification" crashes on Android 14+ devices. (Issue #607, July.27.12)
*   **Startup Notification Content Authority (R608)**: To prevent visual flickering, Services MUST provide role-specific and health-aware notification metadata (Battery level, Security status) immediately during the initial `startForeground()` call. (Issue #608, July.27.13)
*   **Deferred Flow Collection (R542)**: Heavy Room-backed flows (logs, trails, violations) MUST be collected only within their respective screen routes (Tracker/Viewer). (Issue #542, July.24.07)
*   **UI State Decomposition (R547)**: The application UI MUST decompose monolithic state objects into persistent and transient streams to minimize heap churn. (Issue #547, July.24.08)
*   **Zero-Churn Engine Windows (R547b)**: High-frequency kinematic windows MUST utilize circular primitive buffers to eliminate GC pressure. (Issue #547b, July.25.07)
*   **Reactive Siren Surfacing (R547c)**: UI visibility gates MUST be integrated directly into the `TelemetryState` stream. (Issue #547c, July.25.01)
*   **Cold Start Maintenance Coordination (R565)**: Heavy background maintenance tasks MUST be coordinated with the `INITIAL_RENDER_DELAY_MS` window. (Issue #565, July.26.00)
*   **Startup Connection Priority (R575)**: The `ConnectivitySuite` MUST permit immediate connection on cold start. (Issue #575, July.26.01)
*   **Forensic I/O Optimization (R585)**: Database pruning MUST be decoupled from telemetry insertion hot-paths. (Issue #585, July.26.03, July.27.07)
*   **Service Initialization Coordination (R586)**: Services MUST utilize the `onServiceInitialize` suspension hook for deterministic startup. (Issue #586, July.26.03)
*   **Shared GNSS Flow Authority (R587)**: `GpsManager` MUST utilize a `SharedFlow` to prevent redundant platform-level GNSS registrations. (Issue #587, July.26.03)
*   **Flow Architecture Standardization (R545c)**: All telemetry flows MUST follow the `SharedFlow` pattern using `shareIn` with `WhileSubscribed(5000)`. (Issue #545c, July.26.03)
*   **Utility Centralization & State Simplification (R588)**: Core logic components MUST utilize centralized utilities in `PhysicsUtils` and `SentinelValidator`. (Issue #588, July.26.04)
*   **High-Frequency Latency Monitoring (R589)**: Critical engine processing cycles MUST be monitored via `LatencyMonitor`. Threshold violations MUST be emitted as forensic logs. (Issue #589, July.26.04)
*   **Granular Trail Thinning (R548)**: Map trail polylines MUST be simplified using radial distance pruning (1.0m threshold). (Issue #548, July.25.02)
*   **Forensic Snapshot Pooling (R570)**: Retrieval of sensor samples MUST utilize mutable flyweight objects to eliminate transient heap allocations. (Issue #570, July.25.02)
*   **Flyweight Thread Safety (R570b)**: Sequence-based flyweights MUST be scoped to the method/iterator level. (Issue #570b, July.25.10)
*   **Unified Latency Monitoring (R590)**: Critical native JNI calls and DB transactions MUST be monitored via `LatencyMonitor`. (Issue #590, July.25.11)
*   **Lifecycle Idempotency (R591)**: Registration and initialization sequences MUST be protected by idempotent state guards. (Issue #545, #545b, #591)
*   **Forensic Primitive Buffering (R550)**: Circular primitive arrays MUST be used for historical sample storage. (Issue #550, July.25.02)
*   **Pipeline Serialization Hardening (R560)**: The signaling pipeline MUST utilize pre-allocated buffers and reusable Protobuf builders. (Issue #560, #560b, July.25.03)
*   **Priority-Aware Signaling (R560c)**: The signaling pipeline MUST utilize a Dual-Queue Priority Dispatcher. (Issue #560c, July.25.08)
*   **Signaling Validation (R596b)**: The system MUST provide a diagnostic trigger to simulate heavy signaling load (e.g., 100-log burst). (Issue #596, July.27.03)
*   **UI Component De-coupling (R598)**: High-frequency data streams MUST be collected locally within their UI components. (Issue #598, July.27.04)
*   **Ribbon Rendering Optimization (R598b)**: Analytical ribbons MUST cache static drawing parameters and utilize optimized O(N) draw loops. (Issue #598, #603, July.27.07)
*   **Forensic Retrieval Auditing (R600)**: Log retrieval flows MUST utilize context-aware buffering and be monitored via `LatencyMonitor`. (Issue #600, July.27.05)
*   **Kinetic Energy Anomaly Detection (R601)**: The Vibration sensor MUST utilize a centralized High-Pass Filter (HPF) and Energy EMA. (Issue #601, July.27.06)
*   **Forensic I/O Concurrency Authority (R605)**: Maintenance tasks (Pruning) MUST NOT block the log insertion hot-path. (Issue #605, July.27.07)
*   **Forensic Peak Retention Authority (R604)**: Aggregation for long-term historical ribbons MUST utilize peak-retention logic (max) for critical kinetic indices. (Issue #604, July.27.08)

### 2. Architectural Integrity & Centralization
*   **Consolidated Constants (R597)**: All engine-specific thresholds MUST be centralized in `core:engine:EngineConstants.kt`. (Issue #597, July.27.00)
*   **Preference Key Authority (R597b)**: All DataStore keys MUST be defined in `app:PreferenceKeys.kt`. (Issue #597, July.27.00)

### 3. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Logic MUST use monotonic `rt` for calculations, while forensic logs use wall-clock `ts`. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity MUST be maintained across all layers. (Issue #118, #122, #525, #601, #602, #604)
*   **Strict Forensic Reconstruction (R595)**: The Analytical Ribbon UI MUST provide a "Strict Mode" for data continuity validation. (Issue #595, July.26.04)
*   **Direct Binary Flow (R541)**: Telemetry MUST prioritize the raw Protobuf binary path. (Issue #541, July.24.05)

### 4. Persistence & Service Reliability
*   **Activation Authority**: `isSystemActive` flag in DataStore is the definitive authority for lifecycle revival.
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted and restored upon revival. (Issue #527)
*   **Boot Redundancy Hardening (R539b)**: `BootReceiver` MUST update `APP_START_TIME_KEY` immediately. (July.24.05)

### 5. Architectural Baselines
*   **Anchor Logic Authority (R990e)**: `AnchorEvaluator` is the central authority for stationary state. (Issue #533b)
*   **Map Overlay Management (R544b)**: OsmDroid object lifecycles MUST be managed by `MapOverlayManager`. (Issue #544, July.24.08)
*   **Type Safety Authority (R999)**: Internal telemetry MUST use `Double` precision. (Issue #077, #532)

### 6. Version Authority
*   **Current Release**: July.27.13.
*   **Source of Truth**: app/build.gradle versionName.
