# Project Simplification & Hardening Plan (R406)

This document outlines the proposed changes to simplify the `gps-tracker` project, remove redundant logic, and improve maintainability.

## R406a: Unified Heartbeat (2s Standard) - Issue #501 [COMPLETED]
Standardized all periodic tasks and hardware polling to a 2s cycle to reduce complexity and improve power resilience.
*   **Action**: Standardize all periodic tasks to a 2s tick.
*   **Removal**: Deleted `MOVING_GPS_POLLING_MS`, `STATIONARY_GPS_POLLING_MS`, `HIGH_FREQUENCY_GPS_POLLING_MS`, etc.
*   **Implementation**: Used `TICK_INTERVAL_MS` (2000L) globally.
*   **Status**: Fixed in July.11.01.

## R406b: Device Independency - Issue #502 [COMPLETED]
The codebase is currently littered with `isXiaomiDevice()`, `isSamsungDevice()`, and `isS21FEDevice()` checks.
*   **Action**: Remove vendor-specific logic from the `core:engine`.
*   **Abstraction**: Hardware-specific workarounds moved into `HardwareCapabilities` abstraction.
*   **Logic**: Engine now operates on abstract capabilities (e.g., `requiresWakeLockRenewal`).
*   **Status**: Fixed in July.1.12 (Issue #502).

## R406c: Hilt Removal (Simplification) - Issue #503
Hilt/Dagger adds significant boilerplate and increases build times.
*   **Action**: Revert to manual Dependency Injection or a simple Service Locator.
*   **Strategy**: 
    1.  Remove `@AndroidEntryPoint` and `@Inject` from Services and ViewModels.
    2.  Create a manual `DependencyGraph` in `GpsApplication`.
    3.  Pass dependencies through constructors manually.
*   **Benefit**: Easier debugging, no generated code obfuscating the call stack, and faster compilation.

## R406d: Kalman Filter Removal - Issue #504 [COMPLETED]
The `ImmFilter` (Interacting Multiple Model) adds significant mathematical overhead and is a common source of "stuck" positions or "ghosting".
*   **Action**: Delete `ImmFilter.kt` and `KalmanModel`.
*   **Replacement**: Used Exponential Moving Average (EMA) for position, speed, and bearing smoothing.
*   **Benefit**: Predictable behavior and easier tuning of tracking logic.
*   **Status**: Fixed in July.1.12 (Issue #504).

## R406e: Logic Refactoring (Additional Ideas)
*   **State Machine Consolidation - Issue #505**: The `TrackerService` and `LocationProcessor` have overlapping responsibilities. Merge them into a single `TrackingEngine` that manages both data acquisition and processing.
*   **Event Bus Removal - Issue #506**: If the app uses a complex event bus, replace it with standard Kotlin `Flow` or `SharedFlow` for reactive updates.
*   **Simplify Settings - Issue #507**: Reduce the 100+ constants in `EngineConstants.kt`. Most of these are never changed and add "knob fatigue."

## R406g: Optimization Removal - Issue #508
Several "Zero-Lag" and "Hindsight Correction" optimizations add complexity that is often unnecessary after a proper bootstrap.
*   **Action**: 
    1.  Remove `HindsightBuffer` and associated correction logic.
    2.  Remove "Muzzle" logic (startup logging suppression) once the `BOOTSTRAP_PHASE_MS` is standardized.
    3.  Remove `AdaptiveJumpConfidence` multipliers.
*   **Assumption**: During the 60s bootstrap, we ignore all telemetry. Once active, we treat every point with standard weight.

## R406h: Low-Value / High-Complexity Code Removal
*   **Abandon GtoEngine (Trajectory Promotion) - Issue #509**: The `GtoEngine` (Graph Trajectory Optimization) and "Hindsight Promotion" are extremely complex.
    *   *Suggestion*: If a point is rejected as a jump, it remains rejected. This removes the need for `GtoEngine`, `RejectedPoint`, and complex interpolation logic.
*   **Abandon Chair Sit Detection (R832) - Issue #510**: This logic consumes significant resources (Barometer, Accel, Tilt) and involves complex thresholds.
    *   *Suggestion*: Remove Sit Detection to simplify the sensor pipeline and eliminate many constants.
*   **Simplify Ribbon Telemetry - Issue #511**: Redesign "Ribbons" to store only core metrics (Speed, Accuracy, Battery). Detailed sensor indexes (Lux, Vibe, etc.) can be removed from the database to reduce load.
*   **Consolidate Sentinel Statuses - Issue #512**: Redesign 9+ statuses into three core states: `OK`, `JUMP`, and `TAMPER`.
*   **Flatten Service Dependencies - Issue #513**: `BaseMonitorService` has 15+ injected components. Merge related managers (e.g., `NetworkManager`, `SyncManager`, and `RemoteHandler` into `ConnectivitySuite`).
*   **Simplify GpsManager - Issue #514**: It currently manages GNSS status, SNR buffers, and location updates separately.
    *   *Suggestion*: Rely primarily on `FusedLocationProviderClient`. Remove manual SNR buffering and GNSS callbacks.
*   **Remove "Stationary Anchor" Logic - Issue #515**: The `LocationProcessor` has complex logic to "lock" the location when stationary.
    *   *Suggestion*: Replace with a simple speed gate. If estimated speed < 0.5m/s, do not update the reported coordinate.
*   **De-duplicate "Status" Logic - Issue #516**: `LocationProcessor`, `LocationSentinel`, and `AppAlarmManager` all have logic that determines if the tracker is "OK".
    *   *Suggestion*: Create a single `SystemHealthState` that is the source of truth for all UI and alerts.

## Implementation Priority Recommendations
We recommend implementing the following issues first to establish a stable and simplified foundation:

1.  **Issue #501 (R406a: Unified Heartbeat)**: [DONE] Standardizing the timing loop to 2s simplified all downstream logic.
2.  **Issue #504 (R406d: Kalman Filter Removal)**: [DONE] Removing the `ImmFilter` in favor of simple EMA smoothing will eliminate the most mathematically complex and bug-prone part of the positioning logic.
3.  **Issue #509 (R406h: GtoEngine / Hindsight Removal)**: This will strip out a large amount of complex buffer management and "time-travel" logic that often causes synchronization issues.

## Proposed Execution Order
1.  **Phase 1**: Remove Kalman Filter and replace with EMA smoothing (Issue #504). [COMPLETED]
2.  **Phase 2**: Standardize all timers to 2s and remove device-specific switches (Issue #501 [DONE], Issue #502 [DONE]).
3.  **Phase 4**: Remove Hilt and implement manual DI (Issue #503).
4.  **Phase 3**: Strip out GtoEngine and Hindsight logic (Issue #508, Issue #509).
5.  **Phase 5**: Consolidate Sentinel statuses and flatten Service architecture (Issue #512, Issue #513).
