# SOT Master Requirements & Hardening Status (Sep.24.96)

## 🏗️ Architectural Master Rules (24 Rules)

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
*   **SOT ID 476**: Role-Switching Atomic State Reset - Remediated Issue #1313 by hardening `MonitorService` to handle dynamic role transitions via a reactive `appModeFlow` observer. Integrated `handleRoleTransition` to atomically reset processing state and jobs, preventing cross-role state contamination. (Resolved Sep.24.96)
*   **SOT ID 475**: Unified Evaluation Snapshot - Remediated Issue #1312 by consolidating `AlarmTelemetrySnapshot` and `SensorStateSnapshot` into a single `SystemEvaluationSnapshot`. This ensures absolute temporal parity between kinematic and health logic. (Resolved Sep.24.96)
*   **SOT ID 474**: Stateless Logic Refactoring - Remediated Issue #1163 by migrating `LocationProcessor`, `LocationSentinel`, and `GtoEngine` to a stateless evaluation model. Consolidated all mutable tracking data into `LocationProcessingState` and transitioned core engines to pure `object` implementations to ensure deterministic kinematic and sensor evaluation (Resolved Sep.24.95).
*   **SOT ID 473**: Stateless Evaluation Consolidation - Remediated Issue #1311 by shifting active alarm map states and tracking metadata entirely into `AlarmEvaluationState`. Fully purged secondary in-memory maps from `AppAlarmManager` to establish complete architectural isolation across role switches. (Resolved Sep.24.94)
*   **SOT ID 472**: Unified Event Orchestration - Centralized alert triggers, audio synthesis, and forensic logging into a high-cohesion `AppEventCoordinator`. Decoupled domain reactions from background service lifecycles and established reactive siren state binding (Issue #1292) between `AppAlarmManager` and `AudioSynthesizer` to ensure absolute parity across functional roles (R-ID 472). (Resolved Sep.24.93)
*   **SOT ID 471**: Service Lifecycle Unification - Consolidated `TrackerService` and `ViewerService` redundant boilerplate into a unified, role-reactive `MonitorService`. (Resolved Sep.24.92)
*   **SOT ID 470**: Thermal Recovery Latency Audit Correction - Refactored forensic loop sampling metrics in background services to eliminate loop iteration delay errors using authoritative `coolingEnteredRt` (R-ID 470). (Resolved Sep.24.91)
*   **SOT ID 469**: Remote Namespace Isolation - Introduced the `"VR_"` prefix to segregate remote tracker logic state from local session telemetry (R-ID 469). (Resolved Sep.24.90)
*   **SOT ID 468**: Debounced Baseline Persistence - Transitioned high-frequency baseline updates to a non-blocking debounced job model, eliminating tick-loop jitter (R-ID 468). (Resolved Sep.24.80)
*   **SOT ID 467**: Alarm Role Transition Hardening - Hardened `AppAlarmManager` state cleanup during role transitions to prevent unexpected "Siren Jumps" (R-ID 467). (Resolved Sep.24.70)
*   **SOT ID 466**: Viewer Forensic Sampling Loop - Implemented `forensicSamplingLoop` in `ViewerService.kt` to establish absolute cross-role parity for monitoring integrity audits (R-ID 466). (Resolved Sep.24.60)
*   **SOT ID 465**: Non-Blocking History Flush - Transitioned database flush in `onDestroy()` to `@ApplicationScope` with timeout to prevent ANRs (R-ID 465). (Resolved Sep.24.50)
*   **SOT ID 464**: History Sync Restoration - Refactored history observation in `ViewerService.kt` to restore history trace integrity on monitor devices (R-ID 464). (Resolved Sep.24.40)
*   **SOT ID 463**: Decoupled Forensic Spike Sampling - Refactored `forensicSamplingLoop` to bypass sampling rate delays for high-priority physical spikes (R-ID 463). (Resolved Sep.24.30)
*   **SOT ID 462**: Boot-ID Latch Validation - Implemented Boot-ID validation to invalidate obsolete monotonic latches after a device restart (R-ID 462). (Resolved Sep.24.20)
*   **SOT ID 461**: Persistent Lux and Acoustic Baselines - Implemented persistence for environmental calibration, eliminating the 60s learning period after restarts (R-ID 461). (Resolved Sep.24.10)
*   **SOT ID 460**: Atomic User Counter Hardening - Guarded `activeUsers` in `HardwareSuite` against negative values and resource leaks (R-ID 460). (Resolved Sep.24.04)
*   **SOT ID 459**: Persistent Adaptive Vibration Floor - Implemented persistence for the physical baseline sensitivity anchor (R-ID 459). (Resolved Sep.24.03)
*   **SOT ID 458**: Reboot-Aware Monotonic Clock Recovery - Implemented `recoverLastRealtime` to ensure timing anchors remain valid across device reboots (R-ID 458). (Resolved Sep.24.02)
*   **SOT ID 457**: Fast-Path Allocation Optimization - Made spike detection callbacks optional to eliminate allocation churn in the tick loop (R-ID 457). (Resolved Sep.24.01)

## 4.2. Change History (Recent)
*   **Sep.24.96**: Resolved Issue #1312 (Unified Evaluation Logic Snapshot) and Issue #1313 (Role-Switching Atomic State Reset).
*   **Sep.24.95**: Resolved Issue #1163 (Stateless & Functional Logic Refactoring). Migrated location pipeline to stateless evaluation model (SOT ID 474).
*   **Sep.24.94**: Resolved Issue #1311 (AppAlarmManager Stateless Evaluation Model). Relocated active alarms directly into `AlarmEvaluationState` (SOT ID 473).
*   **Sep.24.93**: Resolved Issue #1265 (Unified Event Orchestration). Centralized alert, audio, and logging triggers into `AppEventCoordinator` (SOT ID 472).
*   **Sep.24.92**: Resolved Issue #1261 (Service Lifecycle Unification). Consolidated services into a unified `MonitorService` (SOT ID 471).
*   **Sep.24.91**: Resolved Issue #1234 / #1244 (Heuristic Correction for Thermal Recovery Audits) (SOT ID 470).
*   **Sep.24.90**: Resolved Issue #1306 (Namespace Collision Risk). Introduced `"VR_"` prefix for remote state segregation (SOT ID 469).

## 📋 Functional Requirements (146 R-IDs)
*   **R101-R117**: Core tracking, telemetry, and forensic rules.
*   **R476**: Dynamic role-switching validation.
*   **R475**: Unified evaluation logic snapshot.
*   **R474**: Stateless location evaluation via consolidated state.
*   **R473**: Stateless alarm evaluation via consolidated state.
*   **R472**: Unified Event Orchestration via `AppEventCoordinator`.
*   **R471**: Service Lifecycle Unification via `MonitorService`.
*   *(Remaining requirements preserved in technical registry)*

## 🏁 Verification Chapters
*   **Chapter 31.109 (Role-Switching Atomic State Reset)**: PASSED - Successfully implemented live role transition handling in MonitorService. (Sep.24.96)
*   **Chapter 31.108 (Unified Evaluation Logic Snapshot)**: PASSED - Successfully unified sensor and telemetry snapshots. (Sep.24.96)
*   **Chapter 31.107 (Stateless Logic Refactoring)**: PASSED - Migrated location processor and sentinel to stateless engines. (Sep.24.95)
*   **Chapter 31.106 (Stateless Evaluation Consolidation)**: PASSED - Purged nested mutable memory states from alarm evaluation pipeline. (Sep.24.94)

---
*Next Audit: Oct.01.00. (Sep.24.96)*
