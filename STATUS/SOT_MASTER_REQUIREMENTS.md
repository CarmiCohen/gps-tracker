# SOT Master Requirements & Hardening Status (Sep.25.05)

## 🏗️ Architectural Master Rules (25 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738-R757)**: Hardware managers must use `ManagedHardware` abstractions for synchronous unregistration.
*   **1.9 IPC Optimization (R759)**: High-frequency lookups must utilize shadow-caches to prevent OS-level diagnostic log flooding.
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic (R116).
*   **1.12 Domain Orchestration (R472)**: Domain events (Alarms, Sensors, Connectivity) must be orchestrated by a central `AppEventCoordinator` to decouple domain logic from background service lifecycles.
*   **1.13 Reactive Domain Bus (R477/R480/R481/R482)**: High-frequency state transitions and telemetry summaries must be propagated via a non-blocking `DomainEventBus` with hardened capacity (128) and overflow dropping to ensure the core evaluation loop remains atomic and non-blocking (Refined Sep.25.03).
*   **1.14 Telemetry Partitioning (R485)**: All high-frequency telemetry DTOs must share partitioned state structures (Kinetic, Atmospheric, Integrity) to eliminate bridge mapping layers and enable zero-allocation flyweight double-buffering (Issue #1330).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318-758)**: Hydration must be managed by `LifecycleHydrationManager` with multi-level staggering.
*   **2.2 Native Watchdog & Retry (R301/R319)**: Native calls must be wrapped in a watchdog timer with exponential backoff retries.
*   **2.3 Shadow-Cache Stability (R280/721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock`.
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlays must use standard collections isolated from Compose observation.
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows must utilize deep-parity throttling.
*   **2.6 GPS Warm-up Grace Period (R315)**: Violations must be suppressed for 30s after activation to allow provider stabilization.
*   **2.7 UI Fluidity**: UI stalls must not exceed 700ms on target hardware.

### 3. Forensic & Security Rules
*   **3.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms (R700).
*   **3.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` drops below 0.85 (R715).
*   **3.3 Validation Hooks**: Provide manual hooks for alarm simulation and soak tests (R196-V, R735).
*   **3.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent in DataStore.
*   **3.5 Hardware Neutrality (R212)**: Use neutral hardware namespaces (`jdHardware`) to eliminate vendor framework collisions.

## 🛡️ Core Hardening Baseline
*   **SOT ID 485**: Snap-to-Update Monolith - Remediated Issue #1330 by unifying `SystemEvaluationSnapshot` with partitioned states used by `LocationUpdate`, eliminating the manual bridge mapping layer. (Resolved Sep.25.05).
*   **SOT ID 484**: Peer Lifecycle Decoupling - Remediated Issue #1327 by introducing `PeerConnectionChanged` to eliminate pulse-to-tick event collisions. (Resolved Sep.25.05).
*   **SOT ID 483**: Peer Status Persistence Decoupling - Remediated Issue #1324 by offloading peer telemetry persistence from `ConnectivitySuite` to `AppEventCoordinator` via the `DomainEventBus`. (Resolved Sep.25.04).
*   **SOT ID 482**: Residual Imperative Persistence in MonitorService - Remediated Issue #1323 by transitioning Viewer self-tracking location updates into the `DomainEventBus` with `ViewerLocationUpdated` event. (Resolved Sep.25.03).
*   **SOT ID 481**: DomainEventBus Capacity Hardening - Remediated Issue #1331 by increasing buffer capacity to 128 and implementing `DROP_OLDEST` strategy. (Resolved Sep.25.02).
*   **SOT ID 480**: Multi-Flow Fragmentation Convergence - Remediated Issue #1322 by converging all component-level event streams into the unified `DomainEventBus` (Resolved Sep.25.01).
*   **SOT ID 479**: Telemetry Corruption Remediation - Remediated Issue #1326 by correcting the satellite count mapping in `LocationProcessor` (Resolved Sep.25.00).
*   **SOT ID 478**: Unified Snapshot Metadata Completion - Remediated Issue #1325 by expanding `SystemEvaluationSnapshot` (Resolved Sep.25.00).

## 📋 Functional Requirements (150 R-IDs)
*   **R485**: Zero-allocation snap-to-update partitioning.
*   **R484**: Lifecycle-only peer connection events.
*   **R483**: Bus-driven peer telemetry persistence.
*   **R482**: Bus-driven Viewer self-tracking location updates.
*   **R481**: Reactive Bus Capacity Hardening.
*   **R480**: Unified Multi-Flow Convergence.
*   **R479**: Telemetry Corruption Remediation.
*   **R478**: Unified Snapshot Metadata Parity.
*   *(Remaining requirements preserved in technical registry)*

## 🏁 Verification Chapters
*   **Chapter 31.118 (Partitioned State Unification)**: PASSED - Verified that SystemEvaluationSnapshot partitions map directly to LocationUpdate without manual assignments. (Sep.25.05)
*   **Chapter 31.117 (Peer Lifecycle Decoupling)**: PASSED - Verified that peer pulses emit PeerConnectionChanged and do not trigger redundant repository/signaling side-effects. (Sep.22.30)
*   **Chapter 31.116 (Peer Bus Persistence)**: PASSED - Verified peer status updates travel through the DomainEventBus to AppEventCoordinator for asynchronous repository writing. (Sep.22.30)
*   **Chapter 31.115 (Viewer Bus Persistence)**: PASSED - Verified Viewer self-tracking updates travel through the DomainEventBus to AppEventCoordinator for asynchronous repository writing. (Sep.22.30)
*   **Chapter 31.114 (Bus Resilience)**: PASSED - Verified non-blocking emission under high load with overflow drop strategy. (Sep.22.30)
*   **Chapter 31.113 (Flow Flow Convergence)**: PASSED - Eliminated fragmented flows; unified DomainEventBus orchestrates all system side-effects. (Sep.22.30)

---
*Next Audit: Oct.01.00. (Sep.25.05)*
