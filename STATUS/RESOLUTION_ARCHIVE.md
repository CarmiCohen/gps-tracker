# 🏛️ Resolution Archive - Sep.27.18

## 🏁 Issue #1205: Context-Aware Power Optimization
*   **Resolved**: Sep.27.18
*   **Root Cause**: The tracking engine relied solely on physical vibration indices and temporal duration to determine polling intervals. This meant the system couldn't distinguish between high-intensity stillness (vibrating vehicle) and low-intensity movement, leading to sub-optimal battery usage and potential coordinate gaps during high-velocity transit.
*   **Remediation**:
    *   **EngineModels.kt & LocationUpdate.kt**: Introduced a serializable `ActivityType` enum and integrated it into the zero-allocation telemetry pipeline.
    *   **HardwareSuite.kt**: Implemented a heuristic activity classifier fusing GPS speed and adaptive vibration floors to determine operational context (`STILL`, `WALKING`, `IN_VEHICLE`).
    *   **ServiceBehaviorUseCase.kt**: Refactored `calculateGpsInterval` to scale polling rates based on activity context, allowing immediate relaxation to 60s when `STILL` and enforcing 2s precision when `IN_VEHICLE`.
    *   **Full-Stack Parity**: Updated Room schema (v80), Protobuf definitions, and UI models to ensure activity context is preserved across signaling and history persistence.
*   **SOT ID**: 518 (Context-Aware Power Scaling)

## 🏁 Issue #1160: Flyweight & Pooling Expansion
*   **Resolved**: Sep.27.17
*   **Root Cause**: High-frequency telemetry propagation paths (1Hz tick evaluation and signaling) were allocating fresh `SystemEvaluationSnapshot`, `TrackerStatus`, and `LocationUpdate` objects every second. This created significant GC pressure and memory fragmentation, especially on budget Android hardware.
*   **Remediation**:
    *   **EngineModels.kt & LocationUpdate.kt**: Converted core telemetry DTOs into mutable flyweights by making fields `var` and adding `reset()` and `copyFrom()` methods.
    *   **TelemetryMapper.kt**: Refactored mapping logic to accept "out" parameters, enabling zero-allocation transformation between engine and app DTOs.
    *   **MonitorService.kt**: Introduced private flyweight instances for the tick loop, coordinate processing, and alarm evaluation.
    *   **AppEventCoordinator.kt & ConnectivitySuite.kt**: Migrated event handling and packet processing to use reusable pooled instances, eliminating allocations in the steady-state signaling path.
*   **SOT ID**: 517 (Flyweight & Pooling Expansion)

... (Earlier entries)
