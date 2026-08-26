# SOT Master Requirements - GPS Tracker

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 1. Architectural Authority
*   **1.1 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.2 State Flow**: UI state must be exposed via `StateFlow` from ViewModels. `UiStateAggregator` is the central authority for consolidating telemetry and diagnostic flows (R240). Segmented hydration flows (R248) are required for budget hardware performance.
*   **1.3 Foreground Persistence**: `TrackerService` must maintain a foreground notification. Termination of the service is a violation of SOT.
*   **1.4 Navigation Continuity**: Navigation backstack must be managed to prevent redundant route injection or invalid pop operations. Explicit graph-relative `popUpTo` and `launchSingleTop` are required for all mode transitions (R250).
*   **1.5 Hardware Neutrality (R212)**: The system utilizes a neutral hardware namespace (`jdHardware`) to eliminate vendor framework collisions. Legacy binary signatures (`mbrainSDK`) are neutralized in all code and string pools to prevent heuristic OS triggers (Issue #310). Hardware identification logic is decoupled from the application layer via `HardwareSot` (Issue #317).
*   **1.6 Monotonic Authority (R307)**: All maintenance durations and health-check silence detections must prioritize monotonic references (`elapsedRealtime`) to prevent wall-clock corruption during reboots or system time jumps (Issue #307).
*   **1.7 Staggered Hydration Manager (R318/R323)**: To prevent Davey stalls, hydration must be managed by `LifecycleHydrationManager`, providing a multi-level staggered sequence. Level 4 (Idle Map Hydration) must be triggered via `IdleHandler` to ensure heavy engine initialization occurs only after the UI thread is free (Issue #318, #323).

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
*   **3.1 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`, `ToggleSetupBypass`) to verify alarm triggers and facilitate automated soak tests under simulated stress (R196-V, R735).
*   **3.2 Auto-Recovery**: System must restore to the previous active mode within 2s of launch (R243).
*   **3.3 Identity Sanitization (R976)**: Identity sanitization state must be persistent. The warning overlay dismissal must be written to the DataStore to prevent redundant notifications across cold starts (Issue #737).

## 4. History of Changes (Recent)
*   **Aug.26.15**: Resolved Concern #740 (System Issue Counter Mismatch). Synchronized `PhoneSetupOverlay` items with `MainUiState.systemIssuesCount`. Added Step 0 (Precise Location) and corrected completion flag for Step 5 (Auto-start) (R740).
*   **Aug.26.14**: Verified Concern #737 (Identity Sanitization Persistence) survives cold start. Identified concerns #738 (Leak), #739 (A15 Stall), and #740 (UI Mismatch).
*   **Aug.26.13**: Resolved Concern #737 (Identity Sanitization Persistence). Persisted the dismissal of the sanitization warning to prevent re-init noise on cold starts (R976).
*   **Aug.26.12**: Resolved Issue #736 (Compilation Error). Fixed non-exhaustive when expression in CommandRouter due to redundant ClearTrails declaration in Models.kt.
*   **Aug.26.11**: Resolved Issue #735 Hardening (Setup Overlay Bypass). Implemented developer-mode bypass to enable automated soak tests on remote hardware (R735).
