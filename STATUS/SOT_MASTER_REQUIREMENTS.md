# SOT Master Requirements - GPS Tracker

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 1. Architectural Authority
*   **1.1 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.2 State Flow**: UI state must be exposed via `StateFlow` from ViewModels. `UiStateAggregator` is the central authority for consolidating telemetry and diagnostic flows (R240). Segmented hydration flows (R248) are required for budget hardware performance.
*   **1.3 Foreground Persistence**: `TrackerService` must maintain a foreground notification. Termination of the service is a violation of SOT.
*   **1.4 Navigation Continuity**: Navigation backstack must be managed to prevent redundant route injection or invalid pop operations. Explicit graph-relative `popUpTo` and `launchSingleTop` are required for all mode transitions (R250).
*   **1.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) to eliminate vendor framework collisions. Legacy binary signatures (`mbrainSDK`) are neutralized in all code and string pools to prevent heuristic OS triggers (Issue #310). Hardware identification logic is decoupled from the application layer via `HardwareSot` (Issue #317).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (Issue #307).
*   **1.7 Staggered Hydration Manager (R318)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence (Surface, Core, Full) with environment-specific delays for budget hardware (Issue #318).

## 2. Forensic & Performance Requirements
*   **2.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **2.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **2.3 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
*   **2.4 Native Watchdog & Retry (R301/R319)**: All JNI/native calls must be wrapped in a watchdog timer (2000ms). Native initialization must implement exponential backoff retries to ensure reliable binding during background service startup (Issue #319).
*   **2.5 Shadow-Cache Stability (R280)**: High-frequency lookups must use `ShadowCache` with `ReentrantLock` and optimized initial capacity to prevent race conditions. All caches must implement an LRU strategy for long-term stability (Issue #721).
*   **2.6 Chunked Database Pruning**: All database pruning operations (Logs, Offline Status, connection history, violations, and trail points) must be chunked and staggered (R197).
*   **2.7 Imperative Map Isolation (R309)**: High-frequency map overlay pools and icon caches must use standard collections (`ArrayList`/`HashMap`) and be isolated from Compose `Snapshot` observation. Since these are updated imperatively via `AndroidView.update`, standard collections eliminate lock verification failures and frame skips on non-generational GCs (Issue #309).
*   **2.8 Snap-Isolation Throttling (R312)**: High-frequency telemetry flows (Logs, Trails, Violations, History) must utilize Snap-Isolation via deep-parity throttling (`contentEquals` + `distinctUntilChanged`). This prevents the Compose Recomposer from performing redundant snapshot reconciliation cycles, eliminating lock verification failures and thread synchronization contention on Samsung hardware (Issue #312).
*   **2.9 Staggered Hydration & Observation (R314)**: To prevent Davey stalls during app launch, ViewModel initialization must be staggered. Base observations must start immediately, while heavy telemetry and list-based flows must be delayed by at least 500ms or until the first frame is rendered (Issue #314).
*   **2.10 GPS Warm-up Grace Period (R315)**: Signal loss and accuracy violations must be suppressed for the first 30 seconds after system activation or mode transition to allow GPS provider stabilization (Issue #315).

## 3. Test & Validation Authority
*   **3.1 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`) to verify alarm triggers under simulated stress (R196-V).
*   **3.2 Auto-Recovery**: System must restore to the previous active mode within 2s of launch (R243).

## 4. History of Changes (Recent)
*   **Aug.26.00**: Resolved Issue #318 (A15 Startup Frame Drops) via `LifecycleHydrationManager` and Issue #319 (Monitor Inflation Failure) via native retry logic.
*   **Aug.25.06**: Deployment Verification on SM-A155F. Hardware SOT verified. Identified Issue #318 (Startup Davey) and Issue #319 (Monitor Inflation).
*   **Aug.25.05**: Resolved Issue #317 (Hardware SOT Architectural Decoupling). Migrated detection signatures to `:core:engine:HardwareSot.kt` (R313/R212).
*   **Aug.25.04**: Resolved Issue #313 (Multi-Device Deployment Failure). Unified and hardened hardware detection signatures in `Utils.kt` (R313).
*   **Aug.25.03**: Resolved Issue #316 (Shadow-Cache LRU Documentation Gap). Verified R280 logic via unit tests in `ShadowCacheTest.kt`.
*   **Aug.25.02**: Hardware Verification and Hardening Trace. Identified Issue #313 (A15 Detection Failure).
*   **Aug.25.01**: Resolved Issue #315 (Immediate Signal Loss False Positive). Implemented GPS_WARMUP_GRACE_MS (30s) in `MainAlarmLogic` (R315).
*   **Aug.25.00**: Resolved Issue #314 (Startup Davey Stall). Implemented Staggered Hydration (R314) with A15-specific observation offsets.
