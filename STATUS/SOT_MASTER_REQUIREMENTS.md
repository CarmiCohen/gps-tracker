# SOT Master Requirements (Aug.29.13)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (30 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744/R745/R746/R747/R748/R749/R750/R752/R753/R754/R755/R756/R757)**: **MANDATORY**. Hardware managers (GPS, Sensors, Network, GNSS) must use `ManagedHardware` abstractions for synchronous, trace-logged unregistration. Listener unregistration MUST be processed on the dedicated hardware thread (or Main Looper for OS callbacks) before termination, using synchronous synchronization (e.g., CountDownLatch or task awaiting) to ensure native disposal finishes. Hardware cleanup methods (e.g., `GpsManager.stop()`) must be unconditional to ensure background or revival callbacks are not orphaned if primary flows were never started. (Updated Aug.28.11).
*   **1.9 IPC Optimization (R759)**: High-frequency lookups of system identifiers (e.g., Package Name) must utilize `GpsApplication.PACKAGE_NAME` shadow-cache to prevent repetitive IPC calls and associated OS-level diagnostic log flooding on restricted hardware (Updated Aug.28.11).
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic to survive clock regressions and drift (R116).
*   **1.12 Telemetry Mapping Authority (R761)**: To ensure SRP and avoid logic duplication, all property transformations between Engine models (e.g., EngineConnectionPoint) and App models (e.g., ConnectionPoint) must be centralized in `TelemetryMapper.kt`. Managers and Services are prohibited from performing direct property mapping. (Updated Aug.29.05).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4-7 (Map Engine & Overlay Hydration) must be triggered via `IdleHandler` and staggered over multiple frames. Heavy initialization of the OSM engine and `SqlTileWriter` MUST be offloaded to a background IO thread in `GpsApplication` and gated via `isOsmReady` to ensure hydration never blocks the Main thread (R318, D323, R739, R758).
*   **2.2 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms). Native initialization must implement exponential backoff retries to ensure reliable binding during background service startup (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and an LRU strategy for long-term stability (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlay pools and icon caches must use standard collections and be isolated from Compose `Snapshot` observation. Since these are updated imperatively via `AndroidView.update`, standard collections eliminate lock verification failures and frame skips on non-generational GCs (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows (Logs, Trails, Violations, History) must utilize Snap-Isolation via deep-parity throttling (`contentEquals` + `distinctUntilChanged`). This prevents the Compose Recomposer from performing redundant snapshot reconciliation cycles, eliminating lock verification failures and thread synchronization contention on Samsung hardware (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Signal loss and accuracy violations must be suppressed for the first 30 seconds after system activation or mode transition to allow GPS provider stabilization (R315).
*   **2.7 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
*   **2.8 Async Geometry Generation (R758b)**: Heavy map overlay geometry (e.g., accuracy circles, geofence polygons) must be generated off the UI thread. `MapOverlayManager` must utilize `Dispatchers.Default` for point calculations and trigger a `MapView.invalidate()` only when geometry is ready, ensuring 60FPS fluid motion during high-frequency telemetry updates (Updated Aug.29.00).
*   **2.9 Segmented Polyline Hydration (R759b)**: Large telemetry trails (>500 points) must be updated using segmented coroutine patterns. `MapOverlayManager` must utilize `yield()` during polyline point assignment to interleave point hydration with UI frames, preventing Main-thread stalls during heavy history rendering (Updated Aug.29.02).
*   **2.10 Technical Telemetry Directionality (R766)**: All technical telemetry UI components (StatusBar, Dashboard, HUD) MUST enforce LTR (Left-to-Right) layout direction via `CompositionLocalProvider` regardless of system locale. This ensures that asymmetric technical data (e.g., speed on right, status badges on left) remains readable and aligned with forensic documentation (Updated Aug.29.13).

### 3. Hardware Authority
*   **3.1 Unified Hardware Provider (R760)**: To reduce thread overhead and synchronize platform callbacks, all GNSS, Location, IMU, and Environmental sensors must be managed by the unified `HardwareProvider`. This component must share a single `HandlerThread` ("HardwareThread") for all OS-level event delivery, ensuring consistent lifecycle management and deterministic unregistration via the `ManagedHardware` framework (Updated Aug.29.03).
*   **3.2 Adaptive Acoustic Duty-Cycle (R762/R762b)**: To optimize battery life during extended stationary periods, acoustic monitoring off-cycles must scale linearly from 8 seconds up to 30 seconds based on stationary duration. The calculation logic MUST be encapsulated in `SentinelValidator.kt` as a pure function to ensure testability and separation from hardware side-effects. (Updated Aug.29.12).
*   **3.3 Ultra-Long Stationary GNSS Relaxation (R763)**: To maximize battery life in long-term surveillance scenarios, GNSS polling intervals must be relaxed to 5 minutes (`ULTRA_LONG_STATIONARY_GPS_POLLING_MS`) when confirmed stationary duration exceeds 4 hours (`ULTRA_LONG_STATIONARY_DURATION_MS`). The transition must be managed by `ServiceBehaviorUseCase` to ensure immediate resumption upon movement detection (Updated Aug.29.08).
*   **3.4 Hardware-State Transparency (R765)**: To ensure user and viewer awareness of low-power relaxation modes, high-level hardware states (e.g., Ultra-Long Stationary) MUST be exposed from `HardwareProvider` and propagated through the telemetry pipeline to UI components (via visual `[ULTRA]` badges) and foreground notifications. This provides deterministic explanations for variable polling frequencies. (Updated Aug.29.12).

### 4. Forensic & Security Rules
*   **4.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **4.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **4.3 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`, `ToggleSetupBypass`) to verify alarm triggers and facilitate automated soak tests under simulated stress (R196-V, R735).
*   **4.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent. The warning overlay dismissal must be written to the DataStore to prevent redundant notifications across cold starts (R737, R976).
*   **4.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) to eliminate vendor framework collisions. Legacy binary signatures (`mbrainSDK`) are neutralized in all code and string pools to prevent heuristic OS triggers (R212, R310). Hardware identification logic is decoupled from the application layer via `HardwareSot` (R317).

---

## 🧬 Change History (Recent)
*   **Aug.29.13**: Resolved Concern #766 (RTL Inconsistency & Truncation). Enforced LTR directionality for technical UI and expanded pending reason width.
*   **Aug.29.12**: Resolved Concern #762 (Acoustic Refinement R762b) and #765 (UI Transparency). Encapsulated acoustic duty-cycle logic in SentinelValidator and integrated [ULTRA] badges for transparency.
*   **Aug.29.11**: Resolved Concern #765 (UI Refinement). Added [ULTRA] visual indicators to HUD and Telemetry Dashboard for hardware state transparency.
*   **Aug.29.10**: Resolved Concern #765 (Ultra-Long Stationary Exposure). Centralized detection and exposed state via Flow for UI/Notification parity.
*   **Aug.29.09**: Resolved Concern #764 (Engine Config Refinement). Consolidated device-specific flags into HardwareCapabilities.
*   **Aug.29.08**: Resolved Concern #763 (Ultra-Long Stationary GNSS Relaxation). Implemented 5min relaxation after 4h immobility.

---

## 📋 Functional Requirements (143 R-IDs)
*   **R101**: Background location tracking continuity (High-Uptime).
*   **R102**: Real-ala-time telemetry synchronization via Socket.io.
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
*   *(Remaining 86 functional requirements preserved in the project's internal technical registry)*
