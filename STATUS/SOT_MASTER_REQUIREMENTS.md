# SOT Master Requirements & Hardening Status (Sep.25.01)

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
*   **1.13 Reactive Domain Bus (R477/R480)**: High-frequency state transitions and telemetry summaries must be propagated via a non-blocking `DomainEventBus` to decouple the core evaluation loop from persistence and signaling side-effects (Added Sep.24.97, Refined Sep.25.01).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318-R758)**: Hydration must be managed by `LifecycleHydrationManager` with multi-level staggering.
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
*   **SOT ID 480**: Multi-Flow Fragmentation Convergence - Remediated Issue #1322 by converging all component-level event streams into the unified `DomainEventBus`. Decoupled `AppEventCoordinator` from 10+ fragmented flows and moved bus infrastructure to core engine to support core-level signaling (Resolved Sep.25.01).
*   **SOT ID 479**: Telemetry Corruption Remediation - Remediated Issue #1326 by correcting the satellite count mapping in `LocationProcessor`. Eliminated the legacy zero-placeholder in `processGpsPoint` (Resolved Sep.25.00).
*   **SOT ID 478**: Unified Snapshot Metadata Completion - Remediated Issue #1325 by expanding `SystemEvaluationSnapshot` to include satellite counts, proximity indices, and violation statistics (Resolved Sep.25.00).
*   **SOT ID 477**: Domain Event Bus Integration - Decoupled the `MonitorService` evaluation loop from all persistence, signaling, and ribbon update side-effects (Resolved Sep.24.97).
*   **SOT ID 476**: Role-Switching Atomic State Reset - Hardened `MonitorService` to handle dynamic role transitions via atomic session resets (Resolved Sep.24.96).
*   **SOT ID 475**: Unified Evaluation Snapshot - Consolidated distinct snapshots into a single `SystemEvaluationSnapshot` (Resolved Sep.24.96).

## 📋 Functional Requirements (148 R-IDs)
*   **R480**: Unified Multi-Flow Convergence.
*   **R479**: Telemetry Corruption Remediation.
*   **R478**: Unified Snapshot Metadata Parity.
*   **R477**: Reactive Domain Bus orchestration.
*   *(Remaining requirements preserved in technical registry)*

## 🏁 Verification Chapters
*   **Chapter 31.113 (Flow Convergence)**: PASSED - Eliminated fragmented flows; unified DomainEventBus orchestrates all system side-effects. (Sep.25.01)
*   **Chapter 31.112 (Telemetry Integrity)**: PASSED - Corrected satellite count mapping and unified metadata propagation. (Sep.22.30)

---
*Next Audit: Oct.01.00. (Sep.25.01)*
