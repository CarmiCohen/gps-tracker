# Project Simplification & Hardening Plan (R406)

This document outlines the proposed changes to simplify the `gps-tracker` project, remove redundant logic, and improve maintainability.

## R406a: Unified Heartbeat (2s Standard) - Issue #501 [COMPLETED]
Standardized all periodic tasks and hardware polling to a 2s cycle.
*   **Status**: Fixed in July.11.01.

## R406b: Device Independency - Issue #502 [COMPLETED]
Removed vendor-specific logic (`isXiaomiDevice`, etc.) in favor of `HardwareCapabilities`.
*   **Status**: Fixed in July.1.12.

## R406c: Hilt Removal (Simplification) - Issue #503 [COMPLETED]
Reverted to manual Dependency Injection via `AppContainer`.
*   **Status**: Fixed in July.16.17.

## R406d: Kalman Filter Removal - Issue #504 [COMPLETED]
Deleted `ImmFilter.kt` in favor of EMA smoothing.
*   **Status**: Fixed in July.1.12.

## R406g: Optimization Removal - Issue #508 [COMPLETED]
Removed `HindsightBuffer`, "Muzzle" logic, and `AdaptiveJumpConfidence` multipliers.
*   **Status**: Fixed in July.16.14.

## R406h: Low-Value / High-Complexity Code Removal
*   **Abandon GtoEngine - Issue #509 [COMPLETED]**: Removed trajectory optimization and hindsight promotion.
*   **Abandon Chair Sit Detection - Issue #510 [COMPLETED]**: Removed IMU-based sit detection logic and associated sensor pipeline.
*   **Simplify Ribbon Telemetry - Issue #511 [COMPLETED]**: Purged detailed sensor ribbons (SIT, TLT, BAR, SVZ, SDZ).
*   **Consolidate Sentinel Statuses - Issue #512 [COMPLETED]**: Unified system states into `VALID`, `JUMP`, and `TAMPER`.
*   **Flatten Service Dependencies - Issue #513 [COMPLETED]**: Merged `AppNetworkManager`, `SyncManager`, and `RemoteHandler` into `ConnectivitySuite`.
*   **Simplify GpsManager - Issue #514 [COMPLETED]**: Streamlined to rely on `FusedLocationProviderClient` and immediate GNSS metadata.
*   **Remove "Stationary Anchor" Logic - Issue #515 [COMPLETED]**: Replaced with a simple 0.5m/s speed gate.
*   **De-duplicate "Status" Logic - Issue #516 [COMPLETED]**: Unified device metadata into `SystemHealthState`.
    *   **Status**: Fixed in July.16.22.
*   **Refactor AppAlarmManager - Issue #517 [COMPLETED]**: Eliminated local state flags in favor of `AlarmHistory` in `core:engine`.
    *   **Status**: Fixed in July.16.22.

## Proposed Execution Order
1.  **Phase 1-3**: Completed.
2.  **Phase 4**: Remove Hilt and implement manual DI (Issue #503) - **COMPLETED**.
3.  **Phase 5**: Flatten Service architecture (Issue #513) - **COMPLETED**.
4.  **Phase 6**: Simplify GpsManager (Issue #514) - **COMPLETED**.
5.  **Phase 7**: De-duplicate Status Logic (Issue #516) - **COMPLETED**.
6.  **Phase 8**: Refactor AppAlarmManager (Issue #517) - **COMPLETED**.
