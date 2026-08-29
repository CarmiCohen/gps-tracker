# SOT Master Requirements (Aug.29.00)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (23 Rules)

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

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4-7 (Map Engine & Overlay Hydration) must be triggered via `IdleHandler` and staggered over multiple frames. Heavy initialization of the OSM engine and `SqlTileWriter` MUST be offloaded to a background IO thread in `GpsApplication` and gated via `isOsmReady` to ensure hydration never blocks the Main thread (R318, R323, R739, R758).
*   **2.2 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms). Native initialization must implement exponential backoff retries to ensure reliable binding during background service startup (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/R721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and an LRU strategy for long-term stability (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlay pools and icon caches must use standard collections and be isolated from Compose `Snapshot` observation. Since these are updated imperatively via `AndroidView.update`, standard collections eliminate lock verification failures and frame skips on non-generational GCs (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows (Logs, Trails, Violations, History) must utilize Snap-Isolation via deep-parity throttling (`contentEquals` + `distinctUntilChanged`). This prevents the Compose Recomposer from performing redundant snapshot reconciliation cycles, eliminating lock verification failures and thread synchronization contention on Samsung hardware (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Signal loss and accuracy violations must be suppressed for the first 30 seconds after system activation or mode transition to allow GPS provider stabilization (R315).
*   **2.7 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
*   **2.8 Async Geometry Generation (R758b)**: **NEW**. Heavy map overlay geometry (e.g., accuracy circles, geofence polygons) must be generated off the UI thread. `MapOverlayManager` must utilize `Dispatchers.Default` for point calculations and trigger a `MapView.invalidate()` only when geometry is ready, ensuring 60FPS fluid motion during high-frequency telemetry updates (Updated Aug.29.00).

---

## 🧬 Change History (Recent)
*   **Aug.29.00**: Resolved Concern #758b (Residual UI Thread Congestion). Implemented async geometry generation for accuracy circles and geofences to eliminate "Davey" stalls during map hydration (R758b).
*   **Aug.28.11**: Resolved Concern #757 (Persistent BaseEventQueue Leak). Refactored `GpsManager` and `AppSensorManager` to perform unconditional unregistration of all hardware listeners, ensuring orphaned revival callbacks are cleared even if primary flow state was out of sync (R757). Hardened `SystemStatusProvider` with `PACKAGE_NAME` shadow-cache to eliminate high-frequency log spam (R759).
*   **Aug.28.10**: Resolved Concern #758 (UI Thread Congestion). Offloaded OSMDroid engine pre-warming to IO thread and added `isOsmReady` gate to `LifecycleHydrationManager` (R758).

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
*   *(Remaining 126 functional requirements preserved in the project's internal technical registry)*
