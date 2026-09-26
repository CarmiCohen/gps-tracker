# SOT Master Requirements & Hardening Status (Sep.26.6)

## 🏗️ Architectural Master Rules (30 Rules)

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
*   **1.15 Mapping Centralization (R486)**: Telemetry mapping and DTO construction must be centralized in `TelemetryMapper` to ensure consistency across self-tracking, peer signaling, and offline persistence. This includes authority over `LocationUpdate`, `TrackerStatus`, and `PendingStatusEntity` (Issue #1329).
*   **1.16 Peer Lifecycle Suppression (R489)**: Redundant peer lifecycle events must be suppressed at the coordinator level using connection state caching to prevent forensic log saturation (Issue #1333).
*   **1.17 Temporal Authority Alignment (R490)**: External temporal markers (`lastGpsTs`) must strictly utilize GPS wall-clock time for role-agnostic stall detection, while internal stability auditing (`recordGpsFix`) must be integrated into the primary unified burst processing loop to ensure high-frequency jitter validation (Issue #1334).
*   **1.18 Initialization Prefix Unification (R491)**: State restoration for primary processors must utilize the `rolePrefix` abstraction to ensure role-agnostic persistence, while dedicated remote state prefixes (e.g., "VR_") are strictly isolated to secondary peer-monitoring components (Issue #1335).
*   **1.19 Side-Effect Unification (R492)**: Domain side-effects triggered by processor events must be role-agnostic, utilizing the `isPrimary` flag and dynamic `rolePrefix` mapping to ensure consistency across self-tracking and remote-monitoring paths (Issue #1336).
*   **1.20 Integrity Prefix Hardening (R493)**: Local integrity events must never alter or leak state into the remote peer evaluation path; power alarm flags must be validated against expected prefix boundaries to avoid repo write collisions (Issue #1337).

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
*   **SOT ID 493**: Integrity Prefix Hardening - Remediated Issue #1337 by shielding the remote tracker's evaluation state from local Viewer integrity events and adding role-prefix validation in AppAlarmManager. (Resolved Sep.26.6).
*   **SOT ID 492**: AppEventCoordinator Side-Effect Unification - Remediated Issue #1336 by removing role-specific guards in `AppEventCoordinator` and unifying alarm prefix mapping to ensure consistent persistence for both roles. (Resolved Sep.26.5).
*   **SOT ID 491**: Initialization Prefix Unification - Remediated Issue #1335 by unifying `loadLogicState` to use `rolePrefix` for `primaryProcessor` state restoration, eliminating role-specific branching. (Resolved Sep.26.4).
*   **SOT ID 490**: Unified GPS Pipeline Hardening - Remediated Issue #1334 by correcting spatial anchor initialization and standardizing GPS temporal authority across Tracker and Viewer roles. (Resolved Sep.26.3).
*   **SOT ID 489**: Peer Connection State Caching - Remediated Issue #1333 by introducing connection state caching in `AppEventCoordinator` to suppress redundant lifecycle logging. (Resolved Sep.26.2).
*   **SOT ID 488**: Viewer Self-Tracking Unification - Remediated Issue #1332 by unifying GPS processing for all roles and consolidating self-telemetry persistence under `TickEvaluated`. (Resolved Sep.26.1).
*   **SOT ID 487**: TrackerStatus Convergence - Remediated Issue #1314 by pre-populating forensic indexes in `SystemEvaluationSnapshot` to eliminate mapping overhead during remote signaling. (Resolved Sep.26.0).
*   **SOT ID 486**: Telemetry Mapping Convergence - Remediated Issue #1329 by centralizing all telemetry data transformation in `TelemetryMapper`. (Resolved Sep.25.08).
*   **SOT ID 485**: Snap-to-Update Monolith - Remediated Issue #1330 by unifying `SystemEvaluationSnapshot` with partitioned states. (Resolved Sep.25.05).
*   **SOT ID 484**: Peer Lifecycle Decoupling - Remediated Issue #1327 by introducing `PeerConnectionChanged`. (Resolved Sep.25.05).
*   **SOT ID 483**: Peer Status Persistence Decoupling - Remediated Issue #1324 by offloading persistence to `AppEventCoordinator`. (Resolved Sep.25.04).
*   **SOT ID 482**: Residual Imperative Persistence in MonitorService - Remediated Issue #1323 by transitioning updates to `DomainEventBus`. (Resolved Sep.25.03).
*   **SOT ID 481**: DomainEventBus Capacity Hardening - Remediated Issue #1331 by increasing buffer capacity to 128. (Resolved Sep.25.02).
*   **SOT ID 480**: Multi-Flow Fragmentation Convergence - Remediated Issue #1322 by converging all streams into unified `DomainEventBus`. (Resolved Sep.25.01).

## 📋 Functional Requirements (157 R-IDs)
*   **R493**: Hardened Local Integrity Gating.
*   **R492**: Unified Side-Effect Authority.
*   **R491**: Unified Initialization Authority.
*   **R490**: Unified Temporal Authority Alignment.
*   **R489**: Peer Lifecycle Suppression.
*   **R488**: Unified Pipeline Persistence.
*   **R487**: Snapshot Forensic Pre-population.
*   **R486**: Centralized Telemetry Mapping Authority.
*   **R485**: Zero-allocation snap-to-update partitioning.
*   *(Remaining requirements preserved in technical registry)*

## 🏁 Verification Chapters
*   **Chapter 31.126 (Integrity Prefix Hardening)**: PASSED - Shielded peer evaluation states from Viewer mode local integrity leaks. (Sep.26.6)
*   **Chapter 31.125 (Side-Effect Unification)**: PASSED - Verified role-agnostic persistence for primary processors and unified alarm prefixing. (Sep.26.5)
*   **Chapter 31.124 (Initialization Unification)**: PASSED - Verified role-agnostic state restoration for primary processors. (Sep.26.4)
*   **Chapter 31.123 (Temporal Authorization)**: PASSED - Verified role-agnostic GPS stall detection and burst stability auditing. (Sep.26.3)
*   **Chapter 31.122 (Peer State Caching)**: PASSED - Verified that redundant peer lifecycle events are suppressed in logs. (Sep.26.2)
*   **Chapter 31.121 (Viewer Pipeline Unification)**: PASSED - Verified that Viewer self-fixes follow the primary TickEvaluated path. (Sep.26.1)
*   **Chapter 31.120 (Signaling Optimization)**: PASSED - Verified that forensic indexes are pre-calculated and embedded in the engine snapshot. (Sep.26.0)
*   **Chapter 31.119 (Telemetry Mapping Convergence)**: PASSED - Centralized all construction in TelemetryMapper. (Sep.22.30)
*   **Chapter 31.118 (Partitioned State Unification)**: PASSED - verified snapshot partition mapping. (Sep.22.30)

---
*Next Audit: Oct.01.00. (Sep.26.6)*
