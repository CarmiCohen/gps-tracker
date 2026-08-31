# SOT Master Requirements (Aug.31.06)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (34 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744-R757/R767/R775)**: **MANDATORY**. Hardware managers (GPS, Sensors, Network, GNSS) must use `ManagedHardware` abstractions for synchronous, trace-logged unregistration. Listener unregistration MUST be processed on the dedicated hardware thread (or Main Looper for OS callbacks) before termination, using synchronous synchronization (e.g., CountDownLatch or task awaiting) to ensure native disposal finishes. **Fallback Direct Unregistration (R767)**: If the target looper/thread is unresponsive or terminated during shutdown, managers MUST attempt immediate direct unregistration to prevent native `BaseEventQueue` leaks. **Zero-Raw-Unregistration (R775)**: Manual unregistration via `sensorManager.unregisterListener` outside the `ManagedHardware` flow is prohibited. (Updated Aug.30.05).
*   **1.9 IPC Optimization (R759)**: High-frequency lookups of system identifiers (e.g., Package Name, UID) must utilize `GpsApplication` shadow-caches (e.g., `PACKAGE_NAME`, `MY_UID`). **Overriding getPackageName()** in the Application class is mandatory to ensure all system service calls using the context bypass repetitive IPC-triggered diagnostic logs on Samsung hardware. (Updated Aug.31.06).
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic to survive clock regressions and drift (R116).
*   **1.12 Telemetry Mapping Authority (R761)**: To ensure SRP and avoid logic duplication, all property transformations between Engine models (e.g., EngineConnectionPoint) and App models (e.g., ConnectionPoint) must be centralized in `TelemetryMapper.kt`. Managers and Services are prohibited from performing direct property mapping. (Updated Aug.29.05).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758/R776/R777)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4-7 (Map Engine & Overlay Hydration) must be triggered via `IdleHandler` and staggered over multiple frames. Heavy initialization of the OSM engine and `SqlTileWriter` MUST be offloaded to a background IO thread in `GpsApplication` and gated via `isOsmReady` to ensure hydration never blocks the Main thread. Heavy overlay creation (e.g., violations, home points) MUST be segmented using coroutines and `yield()` to spread load over multiple frames. (Updated Aug.30.07).
*   **2.2 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms). Native initialization must implement exponential backoff retries to ensure reliable binding during background service startup (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and an LRU strategy for long-term stability (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlay pools and icon caches must use standard collections and be isolated from Compose `Snapshot` observation. Since these are updated imperatively via `AndroidView.update`, standard collections eliminate lock verification failures and frame skips on non-generational GCs (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows (Logs, Trails, Violations, History) must utilize Snap-Isolation via deep-parity throttling (`contentEquals` + `distinctUntilChanged`). This prevents the Compose Recomposer from performing redundant snapshot reconciliation cycles, eliminating lock verification failures and thread synchronization contention on Samsung hardware (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Signal loss and accuracy violations must be suppressed for the first 30 seconds after system activation or mode transition to allow GPS provider stabilization (R315).
*   **2.7 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
*   **2.8 Async Geometry Generation (R758b)**: Heavy map overlay geometry (e.g., accuracy circles, geofence polygons) must be generated off the UI thread. `MapOverlayManager` must utilize `Dispatchers.Default` for point calculations and trigger a `MapView.invalidate()` only when geometry is ready, ensuring 60FPS fluid motion during high-frequency telemetry updates (Updated Aug.29.00).
*   **2.9 Segmented Polyline Hydration (R759b)**: Large telemetry trails (>500 points) must be updated using segmented coroutine patterns. `MapOverlayManager` must utilize `yield()` during polyline point assignment to interleave point hydration with UI frames, preventing Main-thread stalls during heavy history rendering (Updated Aug.29.02).
*   **2.10 Technical Telemetry Directionality (R766)**: All technical telemetry UI components (StatusBar, Dashboard, HUD) MUST enforce LTR (Left-to-Right) layout direction via `CompositionLocalProvider` regardless of system locale. This ensures that asymmetric technical data (e.g., speed on right, status badges on left) remains readable and aligned with forensic documentation (Updated Aug.29.13).
*   **2.11 History Sampling Authority (R650/R657)**: To maintain Davey immunity during stress tests, all forensic history flows (4M, 1H, etc.) MUST utilize the `sample()` operator (min 3000ms on A15 hardware) in `MainViewModel`. This protects the Main thread from recomposition storms when high-frequency database writes occur during forensic saturation bursts. (Added Aug.31.02).

### 3. Hardware Authority
*   **3.1 Unified Hardware Provider (R760)**: To reduce thread overhead and synchronize platform callbacks, all GNSS, Location, IMU, and Environmental sensors must be managed by the unified `HardwareProvider`. This component must share a single `HandlerThread` ("HardwareThread") for all OS-level event delivery, ensuring consistent lifecycle management and deterministic unregistration via the `ManagedHardware` framework (Updated Aug.29.03).
*   **3.2 Adaptive Acoustic Duty-Cycle (R762/R762b)**: To optimize battery life during extended stationary periods, acoustic monitoring off-cycles must scale linearly from 8 seconds up to 30 seconds based on stationary duration. The calculation logic MUST be encapsulated in `SentinelValidator.kt` as a pure function to ensure testability and separation from hardware side-effects. (Updated Aug.29.12).
*   **3.3 Ultra-Long Stationary GNSS Relaxation (R763/R778)**: To maximize battery life in long-term surveillance scenarios, GNSS polling intervals must be relaxed to 5 minutes (`ULTRA_LONG_STATIONARY_GPS_POLLING_MS`) when confirmed stationary duration exceeds 4 hours (`ULTRA_LONG_STATIONARY_DURATION_MS`). The transition must be managed by `ServiceBehaviorUseCase` to ensure immediate resumption upon movement detection (Updated Aug.29.08). **State Parity**: The `isUltraLongStationary` flag is retained in the telemetry payload to ensure definitive cross-device transparency during relaxation intervals. (Updated Aug.30.09).
*   **3.4 Hardware-State Transparency (R765)**: To ensure user and viewer awareness of low-power relaxation modes, high-level hardware states (e.g., Ultra-Long Stationary) MUST be exposed from `HardwareProvider` and propagated through the telemetry pipeline to UI components (via visual `[ULTRA]` badges) and foreground notifications. This provides deterministic explanations for variable polling frequencies. (Updated Aug.29.12).

### 4. Forensic & Security Rules
*   **4.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **4.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **4.3 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`, `ToggleSetupBypass`) to verify alarm triggers and facilitate automated soak tests under simulated stress (R196-V, R735).
*   **4.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent. The warning overlay dismissal must be written to the DataStore to prevent redundant notifications across cold starts (R737, R976).
*   **4.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) to eliminate vendor framework collisions. Legacy binary signatures (`mbrainSDK`) are neutralized in all code and string pools to prevent heuristic OS triggers (R212, R310). Hardware identification logic is decoupled from the application layer via `HardwareSot` (R317).
*   **4.6 Forensic Metadata Sanitization (R779)**: **MANDATORY**. All exported logs, trails, and telemetry payloads must be scrubbed of internal absolute paths (e.g., `/data/user/0/...`) and have hardware-specific identifiers (e.g., Build.MODEL) normalized unless explicitly marked as forensic audit traces (`isSpecial`). Sanitization must be applied at the edge of the logging pipeline via `ForensicSanitizer`. (Updated Aug.31.04).
*   **4.7 Binary Protocol Expansion (R782)**: To ensure forensic continuity across hot-path binary updates, all critical performance metrics (e.g., `violationUptimeMs`) and behavioral states (e.g., `isUltraLongStationary`) MUST be carried in the `RealtimeStatus` Protobuf schema. This eliminates state divergence when switching between JSON and binary signaling roles. (Added Aug.31.00).

---

## 🧬 Change History (Recent)
*   **Aug.31.06**: Repetitive getPackageName Log Spam Hardening (#873 Validated). Overrode getPackageName() in GpsApplication to ensure shadow-cache enforcement across all system service calls (R759).
*   **Aug.31.05**: Acoustic Floor Calibration Audit (#810-M Validated). Verified adaptive floor recovery logic via AcousticCalibrationTest. Confirmed correct recovery from saturation to 50dB baseline.
*   **Aug.31.04**: Forensic Replay & Metadata Hardening (#779 Validated). Extended ForensicSanitizer to telemetry mapping and historical audit layers (R779).
*   **Aug.31.03**: Ultra-Long Stationary State Hardening (#762 Validation). Hardened end-to-end propagation of isUltraLongStationary across IntegrityMonitor, TelemetryUseCase, and HistoryManager to ensure definitive badge transparency (R765, R778).
*   **Aug.31.02**: History Sampling Authority (#782 Validation). Hardened ribbon flows in MainViewModel with A15-specific sampling to ensure Davey immunity during forensic stress tests.
*   **Aug.31.00**: Binary Protocol Expansion (#782). Expanded Protobuf schema and implemented database v75 migration to carry violation metrics in hot-path telemetry.

---

## 📋 Functional Requirements (Exhaustive List)
*   **R101**: Background location tracking continuity (High-Uptime).
*   **R102**: Real-ala-time telemetry synchronization via Socket.io.
*   **R103**: Forensic event logging with microsecond precision.
*   **R104**: Geo-fencing authority with configurable distance thresholds.
*   **R104b**: Startup maintenance and resource pre-allocation logic.
*   **R105**: Battery steep discharge detection and alerting.
*   **R106**: Thermal mitigation and performance throttling (Cooling Mode).
*   **R107**: Offline data buffering and batch synchronization.
*   **R107b**: Foreground service stickiness for Samsung A15.
*   **R107d**: Persistent alarm state across service restarts.
*   **R107e**: Explicit hardware permission health check (Diagnostics).
*   **R108**: Proactive database pruning (Logs, Trails, History).
*   **R109**: Secure identity sanitization and persistence.
*   **R110**: ApplicationContext enforcement for dependency injection.
*   **R112**: Deterministic service destruction and resource release.
*   **R113**: Thread-safe atomic state management (StateFlow/Mutex).
*   **R114**: Foreground service notification persistence.
*   **R115**: IO offloading from UI thread (Hardened IO).
*   **R116**: Monotonic time reference for interval detection.
*   **R117**: Centralized telemetry repository as Single Source of Truth.
*   **R118**: Forensic matrix synchronization and Room parity (v59).
*   **R120b**: Hilt worker injection hardening and I/O stabilization.
*   **R133**: Plunge matching and forensic anomaly engine.
*   **R137**: Deferred UI hydration for settings overlays.
*   **R139**: ANR mitigation for high-density tracker screens.
*   **R140**: Automated forensic stress testing under load.
*   **R143**: Forensic integrity verification and crash detection.
*   **R152**: Telemetry flyweight pooling for GC pressure mitigation.
*   **R153**: Staggered UI hydration for startup Davey mitigation.
*   **R155**: PhoneSetupOverlay clutter reduction (hidden completed steps).
*   **R156**: Logcat saturation protection (WakeLock acquisition throttling).
*   **R157**: ViolationPoint flyweight mapping and GeoPoint caching.
*   **R159**: SELinux loadavg denial remediation (SDK 29+).
*   **R162**: Telemetry path object churn elimination (primitive state).
*   **R163**: Network handshake latency optimization.
*   **R170**: Forensic Replay restored with coordinate-aware scrubbing.
*   **R171**: Multi-stream jitter audit and forensic alignment.
*   **R177**: Heap exhaustion prevention and log pruning sensitivity.
*   **R182**: Tracker mode performance hardening (60FPS).
*   **R188**: Corrected telemetry field mapping for coordinates.
*   **R194**: Battery steep discharge load-aware thresholds.
*   **R196**: Zero-loss persistence via memory-mapped spill buffer.
*   **R196-V**: Manual validation hooks for alarm simulation.
*   **R197**: Chunked and staggered database pruning (Storage Pressure).
*   **R201**: Urban multipath mitigation and spatial jump engine.
*   **R202**: Forensic performance and JNI optimization.
*   **R203**: Temporal monotonicity for zero-jitter sequencing.
*   **R207**: UI frame integrity (<100ms render latency).
*   **R210**: Atomic counter safety in central repositories.
*   **R211**: Forensic stress baseline stability (100Hz).
*   **R212**: Neutral hardware namespace (jdHardware).
*   **R217**: Shadow-Cache hardening and thread-safe LRU strategy.
*   **R232**: PhoneSetup button clipping remediation.
*   **R240**: Centralized UI state aggregation (UiStateAggregator).
*   **R243**: Automated recovery to previous active mode (<2s).
*   **R246**: Forensic pipeline range-deduplication and heartbeat.
*   **R247**: Sensor sensitivity sliders and SOT parity.
*   **R248**: Segmented hydration flows for budget hardware.
*   **R250**: Navigation backstack continuity (popUpTo/launchSingleTop).
*   **R280**: LRU strategy for shadow-caches (Storage Pressure).
*   **R301**: JNI watchdog timer (2000ms).
*   **R307**: Maintenance duration monotonic authority.
*   **R309**: Map overlay isolation from Compose Snapshots.
*   **R310**: Heuristic OS trigger neutralization (mbrainSDK).
*   **R312**: Snap-Isolation throttling for telemetry flows.
*   **R314**: ViewModel initialization staggering.
*   **R315**: GPS stabilization grace period (30s).
*   **R316**: Storage integrity monitoring and LRU formalization.
*   **R317**: Decoupled hardware identification (HardwareSot).
*   **R318**: Map Engine Level 4-7 hydration via IdleHandler.
*   **R319**: Native initialization exponential backoff retry.
*   **R320**: Native resource cleanup and JNI page alignment.
*   **R323**: Multi-frame staggered engine initialization.
*   **R334**: Hindsight buffer synchronization across streams.
*   **R337**: Issue ID mismatch remediation for power forensics.
*   **R338**: Unified telemetry freshness and grayout logic.
*   **R400**: Map metadata alignment for coordinate parity.
*   **R403b**: Startup ANR mitigation via heartbeat relaxation.
*   **R405**: Samsung A15 Stay-Alive accelerometer fallback pulse.
*   **R405b**: Unrestricted battery exemption authority (Samsung).
*   **R405c**: Hardware specific adaptive diagnostics.
*   **R406**: System IPC hardening and ANR remediation.
*   **R406b**: Boot-Maintenance race condition protection.
*   **R460**: Bayesian uncertainty expansion and EMA scaling.
*   **R502**: Consolidate Sentinel status logic.
*   **R503**: Manual DI transition (AppContainer decommissioning).
*   **R511**: DataStore singleton violation remediation.
*   **R520**: Signaling command leftover purge.
*   **R521**: Remote settings legacy cleanup.
*   **R522**: Remote Peer State Authority and sync.
*   **R523**: Forensic snapshot consolidation.
*   **R524**: Dashboard state extraction (Architectural Purity).
*   **R526**: Startup responsiveness hardening on budget hardware.
*   **R527**: Siren state restoration persistence.
*   **R529**: Urban accuracy snap mitigation and geofence recovery.
*   **R534**: Telemetry pipeline flyweight refactoring.
*   **R535**: Documentation integrity and audit baseline verification.
*   **R538**: Telemetry memory churn reduction.
*   **R538c**: Mutable aggregation for telemetry churn.
*   **R538d**: Direct map signaling to eliminate conversions.
*   **R538e**: Ribbon backfill results optimization.
*   **R539b**: Boot-maintenance race protection.
*   **R540**: Signaling rejoin loop and IPC congestion fix.
*   **R541**: Direct binary telemetry flow serialization.
*   **R542**: Startup frame skipping and Main thread congestion.
*   **R544**: Compose SnapshotStateList lock verification hardening.
*   **R546**: Signaling handshake instability remediation.
*   **R547**: Kernel performance warning (userfaultfd) remediation.
*   **R555**: Forensic snapshot integrity verification.
*   **R650**: Budget baseline performance compliance (Samsung A15).
*   **R651**: Hardware cooldown enforcement between IPC bursts.
*   **R661**: Foreground service restoration hardening.
*   **R700**: Forensic sampling frequency (10ms-100ms).
*   **R702**: Binary serialization for forensic traces.
*   **R715**: Performance spike reliability threshold (0.85 EMA).
*   **R721**: Samsung A15 Native Collision resolution (LRU).
*   **R728**: Storage-aware adaptive pruning for forensic audit.
*   **R729**: Automated database integrity validation.
*   **R731**: Forensic persistence hardening (Zero-Churn).
*   **R735**: Manual overlay bypass validation hooks.
*   **R736**: Dashboard recomposition optimization (R-RECOMP-01).
*   **R737**: DataStore persistence for identity sanitization dismissal.
*   **R738**: Hardware lifecycle synchronization and atomic registration.
*   **R739**: Sub-millisecond execution blocks for map hydration.
*   **R742**: Lifecycle-bound GNSS callback persistence.
*   **R743**: Forensic spill-buffer write compression.
*   **R744**: Staggered MainViewModel initialization.
*   **R745**: Permission detection responsiveness hardening.
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
*   **R758b**: Async circle geometry generation (Dispatchers.Default).
*   **R759**: PackageName shadow-cache for IPC optimization.
*   **R759b**: Segmented polyline hydration using yield().
*   **R760**: Unified HardwareProvider on a shared HardwareThread.
*   **R761**: Centralized TelemetryMapper authority.
*   **R762**: Adaptive acoustic duty-cycle scaling (8s-30s).
*   **R762b**: Pure function encapsulation for acoustic logic.
*   **R763**: Ultra-long stationary GNSS relaxation (5 mins).
*   **R764**: Consolidated hardware capabilities model.
*   **R765**: Visual [ULTRA] badges for hardware transparency.
*   **R766**: Enforced LTR layout direction for technical UI.
*   **R767**: Fallback direct unregistration for native resource disposal.
*   **R775**: Synchronous unregistration for power-save transitions.
*   **R776**: Segmented violation marker instantiation using yield().
*   **R777**: Segmented home point marker instantiation using yield().
*   **R778**: Definitive isUltraLongStationary flag for state parity.
*   **R779**: Forensic Metadata Sanitization at the logging edge.
*   **R782**: Binary Protocol Expansion for violation metrics.
*   **R799e**: Vivid green enforcement for JD identity.
*   **R810-M**: Acoustic floor calibration logic parity.
*   **R810-P**: Stationary GPS pulse asymmetry mitigation.
*   **R810-L**: Adaptive sensor sampling baseline.
*   **R832**: Tracker-side SIT marker persistence.
*   **R865**: Unified identity green enforcement.
*   **R872**: Tracker stealth enforcement (Neutral namespace).
*   **R917**: Forensic update smoothness audit.
*   **R924**: Legacy R924 sunset and diagnostic migration.
*   **R933**: Alert grace period for transient sensor anomalies.
*   **R941**: Statistics persistence across application cycles.
*   **R951**: Soak test monitoring metrics expansion.
*   **R955b**: Cold-start ANR hardening delay.
*   **R965**: Sensor stability verification on A15.
*   **R967**: Forensic transition verification audit.
*   **R968**: Proto precision upgrade to Double.
*   **R970**: Adaptation settling window hardening.
*   **R971**: G990E display muzzle logic parity.
*   **R972**: Forensic UI expansion (analytical ribbons).
*   **R973**: Proto schema duplication audit.
*   **R974**: Identity persistence hardening (cold start).
*   **R975**: Identity sanitization visibility guard.
*   **R976**: Sanitize warning overlay persistence (DataStore).
*   **R977**: Identity rejection feedback logic.
*   **R978**: TrackerService Hilt refactor for DI purity.
*   **R979**: Forensic logging consolidation.
*   **R985**: Migration verification audit (v38).
*   **R986**: State sync audit for remote peers.
*   **R987**: Speed zeroing verification under urban stress.
*   **R988**: Binary parity verification (Protobuf).
*   **R989**: HUD freshness verification and grayout logic.
*   **R990**: Stationary anchor hard-lock logic.
*   **R990b**: Anchor lock breakout sensitivity.
*   **R990c**: Stationary anchor convergence (8-point window).
*   **R990e**: AnchorEvaluator validation for urban canyons.
*   **R993b**: Hilt worker injection stabilization.
*   **R993c**: Reactive sensor re-registration authority.
*   **R999**: Type Safety Authority (Double Precision).

*(Total: 34 Architectural Rules + 196 Functional R-IDs = 230 Items)*
