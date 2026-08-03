# System Source of Truth (SoT) - Aug.03.45 (Sampling Scaling)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Power-Aware Sampling Authority (R700)**: (Added Aug.03.45) Forensic sampling MUST dynamically scale between 10Hz and 100Hz based on device state. 100Hz (10ms) is permitted ONLY when `isCharging` is true AND `isCoolingModeActive` is false. In all other states, the system MUST throttle to 10Hz (100ms) to preserve battery life and reduce thermal pressure. High-frequency capture MUST utilize the zero-allocation primitive path (`logForensicTraceOptimized`) to ensure R668 compliance. (Issue #700)
*   **Forensic Spill-Buffer Authority (R669)**: (Added Aug.03.37) To prevent SQLite Write-Ahead Log (WAL) contention and "Davey" stalls (momentary UI freezes) during high-frequency telemetry bursts (up to 100Hz), forensic traces MUST be decoupled from database persistence. The system MUST utilize a memory-mapped circular buffer (`MappedByteBuffer`) for off-heap serialization of high-frequency traces. A dedicated background worker MUST drain this buffer into the database in sequential batches during lower I/O activity. (Issue #669)
*   **Zero-Churn Telemetry Authority (R668)**: (Added Aug.01.10) High-frequency telemetry containers (`SystemHealthState`, `LocationState`, `ViolationReport`) MUST utilize mutable flyweight patterns and object pooling. Per-tick object instantiation in the logic hot-path (<= 2000ms) is prohibited. UI state containers (`KinematicState`, `DiagnosticState`) MUST utilize a `pulse` field to trigger reactive updates while retaining mutable member references to eliminate allocation churn on budget hardware (R-HARDWARE-01). (Issue #668)
*   **Zero-Copy JNI State Sync (R667)**: (Added Aug.01.01) High-frequency hardware keep-alive pulses and diagnostic state synchronization between the JVM and native layer MUST utilize a pre-allocated `DirectByteBuffer`. This zero-copy path eliminates `jstring` and object allocation churn during JNI boundary crossings, preventing GC-induced latency spikes on budget hardware (R-HARDWARE-01). Native implementations MUST verify buffer registration before memory access. (Issue #667)
*   **Startup Forensic Audit Hardening (R664)**: (Added Aug.01.00) To eliminate "Davey" stalls (1.7s+) during the critical first-frame rendering path, heavy I/O-intensive library initialization (e.g., `osmdroid` tile providers and SharedPreferences loading) MUST be deferred by a minimum of 3000ms using a background coroutine scope. All global settings collectors (e.g., in `ConfigManager`) MUST be consolidated into background-thread observations (`Dispatchers.IO`) to prevent Hilt-instantiation-time main-thread contention and DataStore/SharedPreferences lock overlap. (Issue #664)
*   **Compose Snapshot Hardening (R657)**: (Hardened July.30.663) To prevent lock verification failures during high-frequency telemetry updates, imperative `AndroidView` update blocks MUST be wrapped in `Snapshot.withoutReadObservation`. Furthermore, high-frequency reactive collections (`SnapshotStateList`) MUST be converted to static `toList()` snapshots before being passed to imperative View update blocks. This strictly decouples imperative view manipulations from the Compose Recomposer's tracking mechanism, eliminating transaction contention, `conditionalUpdate` warnings, and "Davey" stalls on budget hardware (R-HARDWARE-01). (Issue #657, #663)
*   **Log Buffer Pressure Authority (R660)**: To prevent I/O spikes and coroutine overhead during high-frequency telemetry, the logging system MUST utilize a non-blocking circular buffer (Channel-based). Log submission MUST be decoupled from persistence. The `LogRepository` MUST process logs in batches (e.g., 50 entries or 2000ms delay) and utilize SQLite batch inserts within single transactions to minimize disk contention. (Issue #660, July.31.38)
*   **Foreground Service Restoration Hardening (R661)**: All foreground service start attempts, particularly during automatic restoration or deferred recovery, MUST be wrapped in an exhaustive `try-catch (Throwable)` block to intercept `ForegroundServiceStartNotAllowedException`. If the OS denies the start, the system MUST transition to a "Pending" state and defer the attempt until a valid foreground state is established (e.g., in `onResume`). (Issue #661, July.31.01)
*   **16KB Page Size Compatibility (R628)**: All native libraries MUST be aligned for 16KB page size compatibility to support Android 15+ hardware (e.g., Samsung A15). Linker flags MUST include `-Wl,-z,max-page-size=16384`, Gradle MUST set `useLegacyPackaging = false`, and `AndroidManifest.xml` MUST set `android:extractNativeLibs="false"` to ensure the OS respects uncompressed alignment. (Issue #665, July.31.37)
*   **Hardware IPC Throttling (R666)**: On budget hardware (Samsung A15), high-cost system service calls (battery optimization checks, overlay permissions, `checkSelfPermission`) MUST be throttled to a minimum of 5000ms inside `SystemStatusProvider`. Permission polling in `MainViewModel` MUST be relaxed to 5s when the Setup UI is visible to prevent main-thread ANRs caused by `getPackageName` IPC pressure. (Issue #666, July.31.37)
*   **JNI Bridge Preservation (R662)**: The `MbrainHardwareManager` and all native methods MUST be protected from ProGuard/R8 obfuscation to ensure the JNI linker can resolve symbols at runtime. (Issue #662, July.31.37)
*   **Kernel-Level Memory Hardening (R656)**: On devices with limited `userfaultfd` support (e.g., Samsung A15), the application MUST utilize `android:largeHeap="true"` to reduce ART compaction frequency. Additionally, `GpsApplication` MUST implement aggressive `onTrimMemory` and `onLowMemory` handlers to proactively release non-critical caches before the OS triggers high-pressure compaction cycles. (Issue #656, July.31.00)
*   **Budget Baseline Optimization (R-HARDWARE-01)**: The Tracking Engine and UI MUST be optimized for a "Budget Baseline" (Samsung A15). High-end hardware capabilities SHALL be bypassed in favor of cross-device stability, aggressive IPC caching, and main-thread silence. Map overlays MUST implement throttling (e.g., 1000ms) for heavy recalculations. (Issue #640, July.30.35)
*   **High-Contrast Map Controls (R642)**: All map-overlay controls MUST utilize solid backgrounds and minimum 1dp borders to ensure accessibility on high-brightness outdoor tiles. (Issue #642, July.30.56)
*   **Zero-Churn GPS Hot-Path (R653)**: The high-frequency GPS processing chain MUST NOT allocate new result objects on every fix. All result containers MUST utilize mutable flyweights and be reused across ticks. (Issue #653, July.30.55)
*   **Zero-Churn Interpolation (R653b)**: Coordinate interpolation MUST utilize callback-based signaling rather than returning new List instances to eliminate heap churn. (Issue #653, July.30.55)
*   **Permission Reactivity Authority (R635/636)**: The `SystemStatusProvider` maintains a cache TTL for permission states. During setup or diagnostics, it MUST utilize a "Robust Refresh" strategy, performing an immediate check followed by a delayed double-check (1200ms) to account for OS-level propagation latency. (Issue #635, #636, July.30.36)
*   **Map Invalidation Optimization (R641)**: The `MapView` MUST only be invalidated when visual state changes (position, accuracy drift, or overlay visibility) are detected. Continuous invalidation on the Main thread is prohibited. (Issue #641, July.30.40)
*   **Foreground Service Start Hardening (R643)**: On API 31+, verify that the Activity is in the `RESUMED` state before starting a foreground service. Start requests occurring while not resumed MUST be marked as pending and deferred to the next `onResume` event. (Issue #643, July.30.40)
*   **Hardware Cooldown Authority (R651)**: Even for "forced" refreshes, the system MUST enforce a minimum hardware-level cooldown period within `SystemStatusProvider` to prevent IPC bursts and manufacturer-level auditing stalls on budget hardware. (Issue #655, July.30.46)
*   **Startup Transition Authority (R658)**: The Main thread MUST remain silent during activity transitions. Any non-critical initialization MUST be deferred until after the first frame is rendered and the activity reaches the `RESUMED` state. (Issue #658, July.30.47)
*   **JNI Initialization Integrity (R659)**: The `MbrainHardwareManager` MUST verify the integrity of the native library state before every JNI call. (Issue #659, July.30.47)
*   **Hardware Timing Audit Authority (R615)**: The system MUST monitor GNSS callback timing for hardware-level instability. (Issue #615, July.28.21)
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization during cold start. (Issue #526)
*   **Startup ANR Optimization (R627)**: Native library loading and vendor-specific hardware initialization MUST be offloaded to background coroutines. (Issue #627, July.30.25)
*   **Budget Hardware Hardening (R606)**: On restricted hardware, high-frequency platform callbacks (GPS/GNSS) MUST be offloaded to dedicated HandlerThreads. (Issue #606, July.27.11)
*   **Map Overlay Performance Hardening (R639)**: Map overlays MUST implement granular change detection and movement thresholds (e.g., 1.0m) to avoid redundant Main-thread calculations. (Issue #639, July.30.31)
*   **Unified Forensic Audit Naming (R623)**: Latency and I/O spike logs MUST follow standardized naming conventions: "Forensic Performance Audit: [Operation] spike ([duration]ms)". (Issue #623, July.29.22)
*   **JNI Reliability Audit (R625)**: Native JNI bridge implementations MUST include robust error handling and retry mechanisms for interrupted system calls (EINTR). (Issue #625, July.30.25)
*   **Deferred Service Recovery Authority (R626)**: On API 31+, background restoration requests MUST catch `ForegroundServiceStartNotAllowedException`. If restricted, the system MUST parasite an `isRecoveryPending` flag. (Issue #626, July.30.26)
*   **Forensic Recovery Log Aggregation Authority (R630)**: The system MUST aggregate "Service Blackout Duration" metrics across all recovery events and log the current recovery latency along with the running average. (Issue #630, July.30.28)
*   **Foreground Service Start Hardening (R634)**: Catch `ForegroundServiceStartNotAllowedException` during explicit manual or automatic service starts and mark as pending if restricted. (Issue #634, July.30.31)
*   **Permission Logic Integrity (R638)**: All critical permissions tracked in `PermissionState` MUST default to `false`. (Issue #638, July.30.31)
*   **GNSS Callback Conflation Authority (R614)**: High-frequency GNSS hardware callbacks MUST be sampled to prevent downstream flow processing overhead. (Issue #614, July.28.20)
*   **Repository Event Pipeline Hardening (R616)**: All `MutableSharedFlow` pipelines within the Repository layer MUST utilize `BufferOverflow.DROP_OLDEST`. (Issue #616, July.28.22)
*   **Global SharedFlow Overflow Strategy (R617)**: All reactive event pipelines MUST enforce `BufferOverflow.DROP_OLDEST`. (Issue #617, July.28.2233)
*   **Dashboard Pipeline Efficiency Policy (R619)**: The UI Dashboard state pipeline MUST be optimized for zero allocation churn. (Issue #619, July.28.24)
*   **State Partitioning Policy (R620)**: Telemetry data MUST be partitioned into high-frequency `KinematicState` and low-frequency `DiagnosticState`. (Issue #620, July.28.24)
*   **UseCase Flow Internalization Policy (R621)**: UI-facing UseCases MUST internalize common flow transformation logic, such as `distinctUntilChanged()`. (Issue #621, July.28.24)
*   **Location Refresh Reactivity Hardening Authority (R622)**: The transition from "Location Pending" to "OK" MUST be debounced by 3000ms. (Issue #622, July.29.00)
*   **Foreground Service Startup Sync (R607)**: Foreground services MUST establish notification channels synchronously on the Main thread within `onCreate()`. (Issue #607, July.27.12)
*   **Centralized Health Snapshot Authority (R609)**: `IntegrityMonitor` is the single source of truth for local system health. (Issue #609, July.28.14)
*   **Lifecycle Idempotency (R591)**: Registration and initialization sequences MUST be protected by idempotent state guards. (Issue #545, #591)
*   **Pipeline Serialization Hardening (R560)**: The signaling pipeline MUST utilize pre-allocated buffers and reusable Protobuf builders. (Issue #560, July.25.03)
*   **Priority-Aware Signaling (R560c)**: The signaling pipeline MUST utilize a Dual-Queue Priority Dispatcher. (Issue #560c, July.25.08)
*   **UI Component De-coupling (R598)**: High-frequency data streams MUST be collected locally within their UI components. (Issue #598, July.27.04)
*   **Ribbon Rendering Optimization (R598b)**: Analytical ribbons MUST cache static drawing parameters. (Issue #598, #603, July.27.07)
*   **Forensic Retrieval Auditing (R600)**: Log retrieval flows MUST utilize context-aware buffering and be monitored via `LatencyMonitor`. (Issue #600, July.27.05)
*   **Kinetic Energy Anomaly Detection (R601)**: The Vibration sensor MUST utilize a centralized HPF and Energy EMA. (Issue #601, July.27.06)
*   **Forensic I/O Concurrency Authority (R605)**: Maintenance tasks MUST NOT block the log insertion hot-path. (Issue #605, July.27.07)
*   **Forensic Peak Retention Authority (R604)**: Aggregation for historical ribbons MUST utilize peak-retention logic. (Issue #604, July.27.08)

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
*   **Current Release**: Aug.03.45.
*   **Source of Truth**: app/build.gradle versionName.
