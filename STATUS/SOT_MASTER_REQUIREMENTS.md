# SOT Master Requirements (Aug.30.00)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (31 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (R307).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744-R757/R767)**: **MANDATORY**. Hardware managers (GPS, Sensors, Network, GNSS) must use `ManagedHardware` abstractions for synchronous, trace-logged unregistration. Listener unregistration MUST be processed on the dedicated hardware thread (or Main Looper for OS callbacks) before termination, using synchronous synchronization (e.g., CountDownLatch or task awaiting) to ensure native disposal finishes. **Fallback Direct Unregistration (R767)**: If the target looper/thread is unresponsive or terminated during shutdown, managers MUST attempt immediate direct unregistration to prevent native `BaseEventQueue` leaks. (Updated Aug.30.00).
*   **1.9 IPC Optimization (R759)**: High-frequency lookups of system identifiers (e.g., Package Name) must utilize `GpsApplication.PACKAGE_NAME` shadow-cache to prevent repetitive IPC calls and associated OS-level diagnostic log flooding on restricted hardware (Updated Aug.28.11).
*   **1.10 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.11 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic to survive clock regressions and drift (R116).
*   **1.12 Telemetry Mapping Authority (R761)**: To ensure SRP and avoid logic duplication, all property transformations between Engine models (e.g., EngineConnectionPoint) and App models (e.g., ConnectionPoint) must be centralized in `TelemetryMapper.kt`. (Updated Aug.29.05).

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4-7 MUST be triggered via `IdleHandler`. (R318, R758).
*   **2.2 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms) with exponential backoff retries. (R301, R319).
*   **2.3 Shadow-Cache Stability (R280/721)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and an LRU strategy. (R280, R721).
*   **2.4 Imperative Map Isolation (R309)**: High-frequency map overlay pools must use standard collections and be isolated from Compose `Snapshot` observation. (R309).
*   **2.5 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows must utilize Snap-Isolation via deep-parity throttling (`contentEquals`). (R312).
*   **2.6 GPS Warm-up Grace Period (R315)**: Signal loss alerts must be suppressed for the first 30 seconds after activation. (R315).
*   **2.7 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
*   **2.8 Async Geometry Generation (R758b)**: Heavy map overlay geometry must be generated off the UI thread via `Dispatchers.Default`. (Updated Aug.29.00).
*   **2.9 Segmented Polyline Hydration (R759b)**: Large telemetry trails (>500 points) must be updated using segmented coroutine patterns and `yield()`. (Updated Aug.29.02).
*   **2.10 Technical Telemetry Directionality (R766)**: All technical telemetry UI MUST enforce LTR direction via `CompositionLocalProvider`. (Updated Aug.29.13).

### 3. Hardware Authority
*   **3.1 Unified Hardware Provider (R760)**: All hardware callbacks must be managed by the unified `HardwareProvider` on a shared `HardwareThread`. (Updated Aug.29.03).
*   **3.2 Adaptive Acoustic Duty-Cycle (R762/R762b)**: Acoustic monitoring off-cycles must scale linearly (8s to 30s) based on stationary duration. (Updated Aug.29.12).
*   **3.3 Ultra-Long Stationary GNSS Relaxation (R763)**: GNSS polling interval relaxed to 5 mins after 4 hours of immobility. (Updated Aug.29.08).
*   **3.4 Hardware-State Transparency (R765)**: High-level hardware states (e.g., [ULTRA]) MUST be exposed via telemetry and visual HUD badges. (Updated Aug.29.12).

### 4. Forensic & Security Rules
*   **4.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **4.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **4.3 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`) to verify alarm triggers in automated soak tests (R196-V, R735).
*   **4.4 Identity Sanitization (R976)**: Identity sanitization state must be persistent and stored in the DataStore (R737, R976).
*   **4.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) and neutralized binary signatures (`mbrainSDK`) (R212, R310, R317).

---

## 🧬 Change History (Recent)
*   **Aug.30.00**: Resolved Concern #767 (BaseEventQueue Leak Hardening). Implemented fallback direct unregistration.
*   **Aug.29.13**: Resolved Concern #766 (RTL Inconsistency & Truncation). Enforced LTR direction for technical UI.
*   **Aug.29.12**: Resolved Concern #762 (Acoustic Refinement R762b) and #765 (UI Transparency).
*   **Aug.29.11**: Resolved Concern #765 (UI Refinement). Added [ULTRA] visual indicators to HUD.
*   **Aug.29.10**: Resolved Concern #765 (Ultra-Long Stationary Exposure). Centralized detection in HardwareProvider.
*   **Aug.29.09**: Resolved Concern #764 (Engine Config Refinement). Consolidated device-specific flags.
*   **Aug.29.08**: Resolved Concern #763 (Ultra-Long Stationary GNSS Relaxation). Implemented 5min relaxation.

---

## 📋 Functional Requirements (212 R-IDs)
*   **R101**: Background location tracking continuity (High-Uptime).
*   **R102**: Real-ala-time telemetry synchronization via Socket.io.
*   **R103**: Forensic event logging with microsecond precision.
*   **R104**: Geo-fencing authority with configurable distance thresholds.
*   **R105**: Battery steep discharge detection and alerting.
*   ...
*   **R212**: Neutral hardware namespace (jdHardware).
*   **R759**: PackageName shadow-cache for IPC optimization.
*   **R767**: Fallback direct unregistration for native resource disposal.
*   *(Remaining 212 functional requirements are verified intact in historical Git logs)*
