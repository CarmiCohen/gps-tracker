# System Source of Truth (SoT) - July.25.07 (Kernel I/O Optimization)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Deferred Flow Collection (R542)**: Heavy Room-backed flows (logs, trails, violations) MUST be collected only within their respective screen routes (Tracker/Viewer) rather than the top-level MainAppContent to minimize cold-start main thread congestion and eliminate frame skips. (Issue #542, July.24.07)
*   **UI State Decomposition (R547)**: The application UI MUST decompose monolithic state objects into persistent (Settings/Navigation) and transient (Telemetry/Health) streams to minimize heap churn and mitigate kernel-level memory moving overhead (`userfaultfd` fallback) on Android 15 hardware. (Issue #547, July.24.08)
*   **Zero-Churn Engine Windows (R547b)**: To eliminate GC pressure and mitigate missing `userfaultfd: MOVE` support on budget hardware (e.g., Samsung A15), high-frequency kinematic windows in `GtoEngine` and accuracy filters in `LocationProcessor` MUST utilize circular primitive buffers (`DoubleArray`, `LongArray`). Transient object allocations and boxing churn MUST be strictly avoided in the 1Hz-10Hz tick path. (Issue #547b, July.25.07)
*   **Reactive Siren Surfacing (R547c)**: To achieve zero-latency alarm visibility, UI visibility gates (specifically `isRedScreenVisible`) MUST be integrated directly into the `TelemetryState` stream. Computation of these gates MUST occur immediately upon receipt of integrity updates in the ViewModel to bypass global timer pulse latency. (Issue #547c, July.25.01)
*   **Granular Trail Thinning (R548)**: To prevent memory bloat and UI jank during long sessions, map trail polylines MUST be simplified using radial distance pruning. A 1.0m threshold MUST be applied to prune redundant nodes while strictly preserving segment boundaries and valid/jump status changes. (Issue #548, July.25.02)
*   **Forensic Snapshot Pooling (R570)**: To achieve "Zero-Churn" forensic reconstruction, retrieval of sensor and SNR samples MUST utilize mutable flyweight objects (`EngineSensorSnapshot`, `EngineSnrSample`, `ForensicSnapshot`). Telemetry aggregation and backfilling MUST utilize reusable mutable containers (`EngineConnectionPoint`) to eliminate transient heap allocations during high-frequency pulse and forensic reconstruction paths. (Issue #570, July.25.02)
*   **Forensic Primitive Buffering (R550)**: To eliminate heap churn and GC pressure during high-frequency telemetry updates, `GpsManager` and `AppSensorManager` MUST use circular primitive arrays (LongArray, DoubleArray, BooleanArray) for historical sample storage. All sample retrieval for forensic backfilling MUST utilize sequences to bypass intermediate list allocations. (Issue #550, July.25.02)
*   **Pipeline Serialization Hardening (R560)**: To achieve zero-churn telemetry signaling on restricted kernels, the signaling pipeline MUST utilize pre-allocated `ByteArray` buffers and reusable Protobuf builders. High-frequency updates MUST be serialized via `CodedOutputStream` directly into the reusable buffer to eliminate `toByteArray()` heap allocations. (Issue #560, July.25.03)
*   **Buffer Overflow Resilience (R560b)**: To handle GNSS satellite density spikes without heap churn, the Protobuf serialization buffer MUST be self-expanding up to a 64KB safety clamp. Once expanded, the buffer MUST be reused for subsequent pulses to maintain Zero-Churn objectives. (Issue #560b, July.25.06)
*   **Mbrain JNI Hardening (R580)**: The `libmbrainSDK` bridge MUST utilize thread-safe wrappers (`ReentrantLock`) for all native calls to prevent signal collisions during rapid Foreground Service type transitions. All native JNI implementations MUST include explicit null-checking for `jstring` and other reference types to prevent memory safety violations. (Issue #580, July.25.05)
*   **Startup Suppression Window (R993d)**: To prevent Main-thread starvation during cold-start, all Foreground Service notification type updates MUST be suppressed for the first 10 seconds of service life if a previous notification has already been successfully posted. (Issue #534, July.24.02)
*   **Notification IPC Throttling (R993b)**: To prevent Main-thread ANRs during hardware recovery bursts, Foreground Service notification updates MUST be double-throttled: a 2000ms hard gate in `AppNotificationManager` and a 10,000ms global throttle for service type changes in `BaseMonitorService` descendants. (Issue #113, #535, July.24.02)
*   **Foreground Service Immediacy (R406b)**: `startForeground` MUST be invoked directly in the Main-thread `onCreate` of any `LifecycleService`. (July.23.11)
*   **Startup Silence Authority (R993c)**: Background services MUST suppress status notification pulses (Battery/Satellites) until the system is explicitly marked as "Active". (Issue #113, July.23.12)
*   **Cold-Start Hardening (R955b)**: Implement a mandatory 500ms staggered delay before starting base observations. (Issue #099)
*   **Telemetry Churn Authority (R538)**: All high-frequency telemetry paths MUST avoid redundant conversions between `JSONObject` and `Map` to minimize GC pressure during active tracking. (Issue #538, July.24.05)
*   **Mutable Aggregation Authority (R538c)**: Telemetry aggregation logic MUST use mutable state containers for intermediate calculations to eliminate redundant object allocations (`copy()` calls) during high-frequency processing across multiple scales. (Issue #538c, July.24.05)
*   **Direct Map Authority (R538d)**: The signaling pipeline MUST support direct `Map` emission to bypass intermediate `JSONObject` allocations in non-binary telemetry paths. (Issue #538d, July.24.05)
*   **Forensic Stream Authority (R538e/f)**: Forensic backfilling and results processing MUST use lazy `Sequence` iteration and single-pass processing to eliminate intermediate list allocations. (Issue #538e, #538f, July.24.05)
*   **Handshake Hardening (R546)**: To prevent `EngineIOException` and "handshake storms" on budget hardware, the signaling pipeline MUST track the "connecting" state and suppress redundant connection attempts during an active handshake. (Issue #546, July.24.07)
*   **UI Snapshot Integrity (R544)**: High-frequency UI components, specifically map marker and polyline pools, MUST use `SnapshotStateList` to ensure thread-safe reactive updates and prevent `conditionalUpdate` lock verification failures within the Compose Runtime. (Issue #544, July.24.07)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across engine, persistence, telemetry, and UI for all 15+ forensic parameters. (Issue #118, #122, #525)
*   **Remote Peer State Authority (R522)**: All remote tracker telemetry MUST be centralized in `RemoteStatusRepository`. (Issue #522)
*   **Forensic Pipeline Consolidation (R523)**: Use an atomic `ForensicSnapshot` for all sensor-based evaluations. (Issue #523)
*   **Direct Binary Flow (R541)**: Tracker-to-Viewer telemetry MUST prioritize the raw Protobuf binary path to bypass JSON parsing overhead. (Issue #541, July.24.05)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.
*   **Tracker Stealth Authority (R872)**: The device MUST remain silent and visually dark when operating in Tracker mode. (July.23.11)
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted to DataStore and restored upon service revival. (Issue #527)
*   **Boot Redundancy Hardening (R539b)**: The `BootReceiver` MUST update the `APP_START_TIME_KEY` immediately upon execution to ensure `MaintenanceWorker` respects its startup grace period. (July.24.05)

### 4. Dependency & Hardware Hardening
*   **Permission Immediacy (R107c)**: Permission state queries following a user-initiated refresh MUST be synchronous to ensure UI consistency and prevent stale setup alerts. (Issue #098, July.24.01)
*   **Reactive Sensor Recovery (R107d)**: Transitions of critical permissions (specifically `ACTIVITY_RECOGNITION`) from DENIED to GRANTED state MUST trigger an immediate sensor re-synchronization command to the background service. (Issue #098, July.24.01)
*   **Aggressive Sensor Re-Registration (R107e)**: Upon receipt of a sensor sync command, the background service MUST perform a synchronous capability refresh and trigger `unregisterListener` / `registerListener` cycles for hardware sensors to bypass OS permission propagation lag. (Issue #098, July.24.03)
*   **Restoration Permission Authority (R107b)**: The Automatic Restoration flow in `MainAppContent` MUST verify all critical permissions before reviving a session. (Issue #113, July.23.12)
*   **Samsung Stay-Alive Hardening (R405c)**: Engage Accelerometer-based stay-alive pulse on sensor failure. Perform hardware "poke" via `SystemMonitor`. (Issue #098, #113)
*   **Step Detector Permission (R107)**: Explicitly track `android.permission.ACTIVITY_RECOGNITION`. Hardware registration MUST be deferred if permission is not granted. (Issue #098, #107)

### 5. Architectural Baselines
*   **Anchor Logic Authority (R990e)**: `AnchorEvaluator` is the central authority for stationary state. (Issue #533b)
*   **Map Overlay Management (R544b)**: Imperative `osmdroid` object lifecycles (Markers, Polylines) MUST be managed by a standalone `MapOverlayManager` to isolate imperative mutations from the declarative Compose recomposition cycle and prevent internal runtime lock collisions. (Issue #544, July.24.08)
*   **Type Safety Authority (R999)**: All internal telemetry and pipelines MUST use `Double` precision. (Issue #077, #532)

### 6. Version Authority
*   **Current Release**: July.25.07.
*   **Source of Truth**: app/build.gradle versionName.
