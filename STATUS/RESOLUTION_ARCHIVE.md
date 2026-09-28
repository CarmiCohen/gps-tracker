# 🏛️ Resolution Archive - Sep.28.1

## 🏁 Issue #1205: Context-Aware Power Optimization
*   **Resolved**: Sep.28.1
*   **Root Cause**: The tracking engine relied on physical vibration indices and temporal duration, lacking a reliable source for user activity context. This caused delayed polling relaxation when stationary and potential coordinate gaps during rapid transit.
*   **Remediation**:
    *   **HardwareSuite.kt**: Fully integrated the Google Activity Recognition API bridge to provide high-confidence activity context (`STILL`, `WALKING`, `RUNNING`, `IN_VEHICLE`). Implemented a 2-minute heuristic fallback for cases where Play Services updates are unavailable.
    *   **SystemHealthState.kt & TelemetryMapper.kt**: Integrated `activityType` into the core health model and ensured its propagation through the zero-allocation telemetry pipeline (`mapSnapshotToHealth`).
    *   **ServiceBehaviorUseCase.kt**: Refactored `calculateGpsInterval` to scale polling rates based on activity context, enabling immediate 60s relaxation when `STILL` and enforcing 2s precision when `IN_VEHICLE`.
    *   **Full-Stack Parity**: Updated versioning to Sep.28.1 and synchronized all tracking metadata.
*   **SOT ID**: 518 (Context-Aware Power Scaling)

## 🏁 Issue #1160: Flyweight & Pooling Expansion
*   **Resolved**: Sep.27.17
*   **Root Cause**: High-frequency telemetry propagation paths were allocating fresh objects every second, causing GC pressure on budget hardware.
*   **Remediation**: Converted core telemetry DTOs into mutable flyweights with `reset()` and `copyFrom()` methods, achieving zero object allocations in the steady-state evaluation loop.
*   **SOT ID**: 517 (Flyweight & Pooling Expansion)

... (Earlier entries)
