# System Source of Truth (SoT) - July.29.01 (Latency Monitor Metric Cleanup)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Budget Hardware Hardening (R606)**: On restricted hardware (e.g., Samsung A15), high-frequency platform callbacks (GPS/GNSS) MUST be offloaded to dedicated HandlerThreads. (Issue #606, July.27.11)
*   **Unified Forensic Audit Naming (R623)**: Latency and I/O spike logs MUST follow a standardized naming convention: "Forensic Performance Audit: [Operation] spike ([duration]ms)" for CPU-bound tasks and "Forensic I/O Audit: [Operation] spike ([duration]ms)" for database/disk tasks. All call sites MUST utilize unified thresholds from `EngineConstants.kt`. (Issue #623, July.29.01)
*   **GNSS Callback Conflation Authority (R614)**: High-frequency GNSS hardware callbacks MUST be sampled to prevent downstream flow processing overhead. (Issue #614, July.28.20)
*   **Hardware Timing Audit Authority (R615)**: The system MUST monitor GNSS callback timing for hardware-level instability. (Issue #615, July.28.21)
*   **Repository Event Pipeline Hardening (R616)**: All `MutableSharedFlow` pipelines within the Repository layer MUST utilize `BufferOverflow.DROP_OLDEST`. (Issue #616, July.28.22)
*   **Global SharedFlow Overflow Strategy (R617)**: All reactive event pipelines using `MutableSharedFlow` across all managers and processors MUST enforce `BufferOverflow.DROP_OLDEST`. (Issue #617, July.28.2233)
*   **Forensic UI State Collection Policy (R618)**: To eliminate dispatch latency and micro-stuttering on budget hardware (A15), all UI-bound collections MUST utilize `Dispatchers.Main.immediate`. (Issue #618, July.28.2326)
*   **Dashboard Pipeline Efficiency Policy (R619)**: The UI Dashboard state pipeline MUST be optimized for minimum computational overhead and zero allocation churn. (Issue #619, July.28.24)
*   **State Partitioning Policy (R620)**: Telemetry data MUST be partitioned into high-frequency `KinematicState` and low-frequency `DiagnosticState`. (Issue #620, July.28.24)
*   **UseCase Flow Internalization Policy (R621)**: UI-facing UseCases MUST internalize common flow transformation logic, such as `distinctUntilChanged()`. (Issue #621, July.28.24)
*   **Location Refresh Reactivity Hardening Authority (R622)**: The transition from "Location Pending" to "OK" MUST be debounced by `LOCATION_RECOVERY_DEBOUNCE_MS` (3000ms). The system MUST capture and expose `lastLocationPendingDurationMs` for forensic logging. (Issue #622, July.29.00)
*   **Foreground Service Startup Sync (R607)**: Foreground services MUST establish notification channels synchronously on the Main thread within `onCreate()`. (Issue #607, July.27.12)
*   **Startup Notification Content Authority (R608)**: Services MUST provide role-specific and health-aware notification metadata immediately during `startForeground()`. (Issue #608, July.27.13)
*   **Centralized Health Snapshot Authority (R609)**: `IntegrityMonitor` is the single source of truth for local system health. (Issue #609, July.28.14)
*   **Forensic Heartbeat Decoupling (R610)**: Low-frequency system updates MUST be decoupled from high-frequency logic ticks. (Issue #610, July.28.15)
*   **Forensic Disk Space Reactivity (R611)**: Internal storage monitoring MUST be reactive. (Issue #611, July.28.16)
*   **Standby & Power-Save Reactivity (R612)**: Power Save Mode and App Standby Bucket monitoring MUST be reactive. (Issue #612, July.28.17)
*   **Location Refresh Reactivity Authority (R613)**: The system MUST maintain a reactive Flow for location-pending status within `GpsManager`. (Issue #613, July.28.18)
*   **Deferred Flow Collection (R542)**: Heavy Room-backed flows MUST be collected only within their respective screen routes. (Issue #542, July.24.07)
*   **UI State Decomposition (R547)**: The application UI MUST decompose monolithic state objects into persistent and transient streams. (Issue #547, July.24.08)
*   **Zero-Churn Engine Windows (R547b)**: High-frequency kinematic windows MUST utilize circular primitive buffers. (Issue #547b, July.25.07)
*   **Reactive Siren Surfacing (R547c)**: UI visibility gates MUST be integrated directly into the `TelemetryState` stream. (Issue #547c, July.25.01)
*   **Cold Start Maintenance Coordination (R565)**: Heavy background maintenance tasks MUST be coordinated with the `INITIAL_RENDER_DELAY_MS` window. (Issue #565, July.26.00)
*   **Startup Connection Priority (R575)**: The `ConnectivitySuite` MUST permit immediate connection on cold start. (Issue #575, July.26.01)
*   **Forensic I/O Optimization (R585)**: Database pruning MUST be decoupled from telemetry insertion hot-paths. (Issue #585, July.26.03, July.27.07)
*   **Service Initialization Coordination (R586)**: Services MUST utilize the `onServiceInitialize` suspension hook. (Issue #586, July.26.03)
*   **Shared GNSS Flow Authority (R587)**: `GpsManager` MUST utilize a `SharedFlow` to prevent redundant platform registrations. (Issue #587, July.26.03)
*   **Flow Architecture Standardization (R545c)**: All telemetry flows MUST follow the `SharedFlow` pattern using `shareIn` with `WhileSubscribed(5000)`. (Issue #545c, July.26.03)
*   **Utility Centralization & State Simplification (R588)**: Core logic components MUST utilize centralized utilities in `PhysicsUtils` and `SentinelValidator`. (Issue #588, July.26.04)
*   **High-Frequency Latency Monitoring (R589)**: Critical engine processing cycles MUST be monitored via `LatencyMonitor`. (Issue #589, July.26.04)
*   **Granular Trail Thinning (R548)**: Map trail polylines MUST be simplified using radial distance pruning. (Issue #548, July.25.02)
*   **Forensic Snapshot Pooling (R570)**: Retrieval of sensor samples MUST utilize mutable flyweight objects. (Issue #570, July.25.02)
*   **Flyweight Thread Safety (R570b)**: Sequence-based flyweights MUST be scoped to the method/iterator level. (Issue #570b, July.25.10)
*   **Unified Latency Monitoring (R590)**: Critical native JNI calls and DB transactions MUST be monitored via `LatencyMonitor`. (Issue #590, July.25.11)
*   **Lifecycle Idempotency (R591)**: Registration and initialization sequences MUST be protected by idempotent state guards. (Issue #545, #545b, #591)
*   **Forensic Primitive Buffering (R550)**: Circular primitive arrays MUST be used for historical sample storage. (Issue #550, July.25.02)
*   **Pipeline Serialization Hardening (R560)**: The signaling pipeline MUST utilize pre-allocated buffers and reusable Protobuf builders. (Issue #560, #560b, July.25.03)
*   **Priority-Aware Signaling (R560c)**: The signaling pipeline MUST utilize a Dual-Queue Priority Dispatcher. (Issue #560c, July.25.08)
*   **Signaling Validation (R596b)**: The system MUST provide a diagnostic trigger to simulate heavy signaling load. (Issue #596, July.27.03)
*   **UI Component De-coupling (R598)**: High-frequency data streams MUST be collected locally within their UI components. (Issue #598, July.27.04)
*   **Ribbon Rendering Optimization (R598b)**: Analytical ribbons MUST cache static drawing parameters. (Issue #598, #603, July.27.07)
*   **Forensic Retrieval Auditing (R600)**: Log retrieval flows MUST utilize context-aware buffering and be monitored via `LatencyMonitor`. (Issue #600, July.27.05)
*   **Kinetic Energy Anomaly Detection (R601)**: The Vibration sensor MUST utilize a centralized High-Pass Filter (HPF) and Energy EMA. (Issue #601, July.27.06)
*   **Forensic I/O Concurrency Authority (R605)**: Maintenance tasks (Pruning) MUST NOT block the log insertion hot-path. (Issue #605, July.27.07)
*   **Forensic Peak Retention Authority (R604)**: Aggregation for historical ribbons MUST utilize peak-retention logic for critical indices. (Issue #604, July.27.08)

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
*   **Current Release**: July.29.01.
*   **Source of Truth**: app/build.gradle versionName.
