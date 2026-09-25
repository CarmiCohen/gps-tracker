# SOT Master Requirements & Hardening Status (Sep.22.30)

## 🏗️ Architectural Master Rules (25 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744/R745/R746/R747/R748/R749/R750/R752/R753/R754/R755/R756/R757)**: Hardware managers must use `ManagedHardware` abstractions for synchronous unregistration (Updated Aug.28.09).
*   **1.9 IPC Optimization (R759)**: High-frequency lookups must utilize shadow-caches to prevent OS-level diagnostic log flooding (Added Aug.28.08).
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic (R116).
*   **1.12 Domain Orchestration (R472)**: Domain events (Alarms, Sensors, Connectivity) must be orchestrated by a central `AppEventCoordinator` to decouple domain logic from background service lifecycles (Added Sep.24.93).
*   **1.13 Reactive Domain Bus (R477)**: High-frequency state transitions and telemetry summaries must be propagated via a non-blocking `DomainEventBus` to decouple the core evaluation loop from persistence and signaling side-effects (Added Sep.24.97).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758)**: Hydration must be managed by `LifecycleHydrationManager` with multi-level staggering (R318, R323, R739, R758).
*   **2.2 Native Watchdog & Retry (R301/R319)**: Native calls must be wrapped in a watchdog timer with exponential backoff retries (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlays must use standard collections isolated from Compose observation (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows must utilize deep-parity throttling (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Violations must be suppressed for 30s after activation to allow provider stabilization (R315).
*   **2.7 UI Fluidity**: UI stalls must not exceed 700ms on target hardware.

### 3. Forensic & Security Rules
*   **3.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms (R700).
*   **3.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` drops below 0.85 (R715).
*   **3.3 Validation Hooks**: Provide manual hooks for alarm simulation and soak tests (R196-V, R735).
*   **3.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent in DataStore (R737, R976).
*   **3.5 Hardware Neutrality (R212)**: Use neutral hardware namespaces (`jdHardware`) to eliminate vendor framework collisions (R212, R310, R317).

## 🛡️ Core Hardening Baseline
*   **SOT ID 479**: Telemetry Corruption Remediation - Remediated Issue #1326 by correcting the satellite count mapping in `LocationProcessor`. Eliminated the legacy zero-placeholder in `processGpsPoint` and ensured `satsUsed` is sourced directly from the authoritative evaluation snapshot. (Resolved Sep.25.00).
*   **SOT ID 478**: Unified Snapshot Metadata Completion - Remediated Issue #1325 by expanding `SystemEvaluationSnapshot` to include satellite counts, proximity indices, and violation statistics. Established absolute telemetry parity within the unified snapshot model to eliminate state fragmentation during event-bus propagation. (Resolved Sep.25.00).
*   **SOT ID 477**: Domain Event Bus Integration - Remediated Issue #1291 by implementing a unified `DomainEventBus`. Decoupled the `MonitorService` evaluation loop from all persistence, signaling, and ribbon update side-effects. Centralized reactive handling of telemetry summaries and system transitions into `AppEventCoordinator` (Resolved Sep.24.97).
*   **SOT ID 476**: Role-Switching Atomic State Reset - Remediated Issue #1313 by hardening `MonitorService` to handle dynamic role transitions via a reactive `appModeFlow` observer. Integrated `handleRoleTransition` to atomically reset processing state and jobs, preventing cross-role state contamination. (Resolved Sep.24.96)
*   **SOT ID 475**: Unified Evaluation Snapshot - Remediated Issue #1312 by consolidating `AlarmTelemetrySnapshot` and `SensorStateSnapshot` into a single `SystemEvaluationSnapshot`. This ensures absolute temporal parity between kinematic and health logic. (Resolved Sep.24.96)
*   **SOT ID 474**: Stateless Logic Refactoring - Remediated Issue #1163 by migrating `LocationProcessor`, `LocationSentinel`, and `GtoEngine` to a stateless evaluation model. Consolidated all mutable tracking data into `LocationProcessingState` and transitioned core engines to pure `object` implementations to ensure deterministic kinematic and sensor evaluation (Resolved Sep.24.95).
*   **SOT ID 473**: Stateless Evaluation Consolidation - Remediated Issue #1311 by shifting active alarm map states and tracking metadata entirely into `AlarmEvaluationState`. Fully purged secondary in-memory maps from `AppAlarmManager` to establish complete architectural isolation across role switches. (Resolved Sep.24.94)
*   **SOT ID 472**: Unified Event Orchestration - Centralized alert triggers, audio synthesis, and forensic logging into a high-cohesion `AppEventCoordinator`. Decoupled domain reactions from background service lifecycles and established reactive siren state binding (Issue #1292) between `AppAlarmManager` and `AudioSynthesizer` to ensure absolute parity across functional roles (R-ID 472). (Resolved Sep.24.93)
*   **SOT ID 471**: Service Lifecycle Unification - Consolidated `TrackerService` and `ViewerService` redundant boilerplate into a unified, role-reactive `MonitorService`. (Resolved Sep.24.92)

## 4.2. Change History (Recent)
*   **Sep.25.00**: Resolved Issue #1325 (Unified Snapshot Metadata Gaps) and Issue #1326 (Telemetry Corruption Remediation).
*   **Sep.24.97**: Resolved Issue #1291 (Domain Event Bus Integration). Decoupled evaluation loop from all side-effects (SOT ID 477).
*   **Sep.24.96**: Resolved Issue #1312 (Unified Evaluation Logic Snapshot) and Issue #1313 (Role-Switching Atomic State Reset).
*   **Sep.24.95**: Resolved Issue #1163 (Stateless & Functional Logic Refactoring). Migrated location pipeline to stateless evaluation model (SOT ID 474).

## 📋 Functional Requirements (147 R-IDs)
*   **R101-R117**: Core tracking, telemetry, and forensic rules.
*   **R479**: Telemetry Corruption Remediation.
*   **R478**: Unified Snapshot Metadata Completion.
*   **R477**: Reactive Domain Bus orchestration.
*   **R476**: Dynamic role-switching validation.
*   **R475**: Unified evaluation logic snapshot.
*   *(Remaining requirements preserved in technical registry)*

## 🏁 Verification Chapters
*   **Chapter 31.112 (Telemetry Integrity)**: PASSED - Corrected satellite count mapping and unified metadata propagation. (Sep.22.30)
*   **Chapter 31.111 (Snapshot Metadata Parity)**: PASSED - Expanded SystemEvaluationSnapshot to include all required telemetry fields. (Sep.22.30)
*   **Chapter 31.110 (Domain Event Bus Integration)**: PASSED - Successfully decoupled evaluation loop from persistence and signaling via DomainEventBus. (Sep.22.30)

---
*Next Audit: Oct.01.00. (Sep.22.30)*
