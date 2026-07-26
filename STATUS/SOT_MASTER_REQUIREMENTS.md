# System Source of Truth (SoT) - July.25.12 (Network Lifecycle Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Deferred Flow Collection (R542)**: Heavy Room-backed flows (logs, trails, violations) MUST be collected only within their respective screen routes (Tracker/Viewer) rather than the top-level MainAppContent to minimize cold-start main thread congestion and eliminate frame skips. (Issue #542, July.24.07)
*   **UI State Decomposition (R547)**: The application UI MUST decompose monolithic state objects into persistent (Settings/Navigation) and transient (Telemetry/Health) streams to minimize heap churn and mitigate kernel-level memory moving overhead (`userfaultfd` fallback) on Android 15 hardware. (Issue #547, July.24.08)
*   **Zero-Churn Engine Windows (R547b)**: To eliminate GC pressure and mitigate missing `userfaultfd: MOVE` support on budget hardware (e.g., Samsung A15), high-frequency kinematic windows in `GtoEngine` and accuracy filters in `LocationProcessor` MUST utilize circular primitive buffers (`DoubleArray`, `LongArray`). Transient object allocations and boxing churn MUST be strictly avoided in the 1Hz-10Hz tick path. (Issue #547b, July.25.07)
*   **Reactive Siren Surfacing (R547c)**: To achieve zero-latency alarm visibility, UI visibility gates (specifically `isRedScreenVisible`) MUST be integrated directly into the `TelemetryState` stream. Computation of these gates MUST occur immediately upon receipt of integrity updates in the ViewModel to bypass global timer pulse latency. (Issue #547c, July.25.01)
*   **Granular Trail Thinning (R548)**: To prevent memory bloat and UI jank during long sessions, map trail polylines MUST be simplified using radial distance pruning. A 1.0m threshold MUST be applied to prune redundant nodes while strictly preserving segment boundaries and valid/jump status changes. (Issue #548, July.25.02)
*   **Forensic Snapshot Pooling (R570)**: To achieve "Zero-Churn" forensic reconstruction, retrieval of sensor and SNR samples MUST utilize mutable flyweight objects (`EngineSensorSnapshot`, `EngineSnrSample`, `ForensicSnapshot`). Telemetry aggregation and backfilling MUST utilize reusable mutable containers (`EngineConnectionPoint`) to eliminate transient heap allocations during high-frequency pulse and forensic reconstruction paths. (Issue #570, July.25.02)
*   **Flyweight Thread Safety (R570b)**: To ensure forensic data integrity across asynchronous coroutine boundaries and suspension points, mutable flyweight objects used in sequences MUST be scoped to the method/iterator level. Forensic snapshots handed over to asynchronous consumers (e.g., for alarm evaluation) MUST be returned as fresh instances to prevent race conditions during concurrent telemetry processing. (Issue #570b, July.25.10)
*   **Unified Latency Monitoring (R590)**: To maintain 1Hz engine stability on budget hardware (e.g., Samsung A15), all critical native JNI calls, Database transactions (History/Trail/Violation), and heavy I/O operations MUST be monitored via `LatencyMonitor`. Execution spikes exceeding 50ms (Native) or 500ms (I/O) MUST be logged as forensic warnings to detect "silent jitter" in the high-frequency pulse. (Issue #590, July.25.11)
*   **Idempotent Network Lifecycle (R545)**: To eliminate platform-level diagnostic noise (e.g., Samsung 'StackLog' floods), registration of system-level network callbacks MUST be idempotent. The `ConnectivitySuite` MUST utilize internal state guarding (`isStarted`) to ensure `registerNetworkCallback` is invoked exactly once per application lifecycle, preventing redundant I/O and Logcat saturation. (Issue #545, July.25.12)
*   **Forensic Primitive Buffering (R550)**: To eliminate heap churn and GC pressure during high-frequency telemetry updates, `GpsManager` and `AppSensorManager` use circular primitive arrays (LongArray, DoubleArray, BooleanArray) for historical sample storage. (Issue #550, July.25.02)
*   **Pipeline Serialization Hardening (R560)**: To achieve zero-churn telemetry signaling on restricted kernels, the signaling pipeline MUST utilize pre-allocated `ByteArray` buffers and reusable Protobuf builders. (Issue #560, July.25.03)
*   **Buffer Overflow Resilience (R560b)**: To handle GNSS satellite density spikes without heap churn, the Protobuf serialization buffer MUST be self-expanding up to a 64KB safety clamp. (Issue #560b, July.25.06)
*   **Priority-Aware Signaling (R560c)**: To prevent large frames from blocking time-critical pulses, the signaling pipeline MUST utilize a Dual-Queue Priority Dispatcher. (Issue #560c, July.25.08)
*   **Mbrain JNI Hardening (R580)**: The `libmbrainSDK` bridge MUST utilize thread-safe wrappers (`ReentrantLock`) for all native calls. (Issue #580, July.25.05)
*   **Startup Suppression Window (R993d)**: Suppress Foreground Service notification type updates for the first 10 seconds of service life. (Issue #534, July.24.02)
*   **Notification IPC Throttling (R993b)**: Foreground Service notification updates MUST be double-throttled (2s hard gate / 10s global throttle). (Issue #113, #535, July.24.02)
*   **Foreground Service Immediacy (R406b)**: `startForeground` MUST be invoked directly in the Main-thread `onCreate` of any `LifecycleService`. (July.23.11)
*   **Startup Silence Authority (R993c)**: Suppress status notification pulses until the system is explicitly marked as "Active". (Issue #113, July.23.12)
*   **Telemetry Churn Authority (R538)**: Avoid redundant conversions between `JSONObject` and `Map` in high-frequency telemetry paths. (Issue #538, July.24.05)
*   **Handshake Hardening (R546)**: To prevent "handshake storms," the signaling pipeline MUST track the "connecting" state and suppress redundant connection attempts. (Issue #546, July.24.07)
*   **UI Snapshot Integrity (R544)**: High-frequency UI components MUST use `SnapshotStateList` for thread-safe reactive updates. (Issue #544, July.24.07)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across engine, persistence, telemetry, and UI for all 15+ forensic parameters. (Issue #118, #122, #525)
*   **Direct Binary Flow (R541)**: Tracker-to-Viewer telemetry MUST prioritize the raw Protobuf binary path to bypass JSON parsing overhead. (Issue #541, July.24.05)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.
*   **Tracker Stealth Authority (R872)**: The device MUST remain silent and visually dark when operating in Tracker mode. (July.23.11)
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted to DataStore and restored upon service revival. (Issue #527)
*   **Boot Redundancy Hardening (R539b)**: The `BootReceiver` MUST update the `APP_START_TIME_KEY` immediately upon execution. (July.24.05)

### 4. Architectural Baselines
*   **Anchor Logic Authority (R990e)**: `AnchorEvaluator` is the central authority for stationary state. (Issue #533b)
*   **Map Overlay Management (R544b)**: Imperative `osmdroid` object lifecycles MUST be managed by a standalone `MapOverlayManager`. (Issue #544, July.24.08)
*   **Type Safety Authority (R999)**: All internal telemetry and pipelines MUST use `Double` precision. (Issue #077, #532)

### 5. Version Authority
*   **Current Release**: July.25.12.
*   **Source of Truth**: app/build.gradle versionName.
