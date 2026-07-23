# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 333**

## 1. Hardening & Persistence (July.23.03)
*   **Issue #531**: Acoustic Duty Cycle Logic Refinement. Refined `ForegroundServiceType` management to use the *intent* to monitor, preventing OS notification flickering during the 8s "OFF" phases of the power-saving duty cycle.
*   **Issue #529**: Geofence Reliability - Accuracy Recovery. Implemented grace logic in `PhysicsUtils.isVisualJump` to suppress false "Visual Jump" alerts during transitions from low to high GPS accuracy.
*   **Issue #528**: DashboardUseCase Tombstone. Decommissioned orphaned `DashboardUseCase.kt` and migrated logic to `DashboardStateProvider`. Scrubbed all code references.
*   **Issue #527**: Siren Persistence. Implemented alarm state persistence using DataStore. Added `restoreState()` to `AppAlarmManager` to ensure audio resumes after service restarts.
*   **Issue #526**: Power Optimization - Adaptive Sensor Sampling. Implemented two-tier power saving (Logic and Hardware) when device is stationary and GPS is stalled.
*   **Issue #525**: State Audit - Forensic Propagation Verification. Hardened end-to-end forensic index telemetry and fixed mapping bugs in local history ribbons.
*   **Issue #524**: UI Decoupling. Extracted UI formatting logic from the dashboard component into `DashboardStateProvider` to reduce ViewModel complexity.
*   **Issue #523**: Forensic Snapshot Consolidation. Implemented `AppSensorManager.consumeForensicSnapshot()` for atomic immutable state evaluation.

## 2. Dead-Weight Purge & Version Finality (July.22.11)
*   **Issue #513**: Dead-Weight Purge. Physically removed 6 redundant/decommissioned files: `AppContainer.kt`, `MainViewModelFactory.kt`, `VideoComponents.kt`, `ChatViewModel.kt`, `WebRtcManager.kt`, and `SIMPLIFICATION_PLAN.md`.
*   **Version Alignment**: Synchronized all authoritative files (`build.gradle`, `SoT`, `Handover`) to resolve tagging conflicts and establish the new July.22.11 baseline.

## 3. DI Purge & Global Startup Maintenance (July.22.09)
*   **Issue #126b**: DI Leftover Purge. Physically decommissioned `AppContainer.kt` and `MainViewModelFactory.kt`. Scrubbed all legacy comments and historical references.
*   **Issue #113**: Samsung A15 Fallback Hardening (R405c). Upgraded the Accelerometer "Stay-Alive Pulse" to perform a hardware "poke" via WakeLock every 10 seconds.
*   **Issue #104b**: Global Startup Maintenance Authority. Extended proactive `deepPruneLogs` to the background service layer.
*   **Issue #121**: Provider Latency Optimization. Implemented lazy thread-safe caching of the `ConnectivitySuite` instance in `LogManager.kt`.
*   **Issue #120b**: Budget Hardware Initialization Spikes. Implemented a 2000ms delay for proactive log pruning in `MainViewModel.kt`.

... [See historical logs for full resolutions]
