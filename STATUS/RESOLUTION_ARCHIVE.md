# 🏛️ Resolution Archive - Sep.27.17

## 🏁 Issue #1160: Flyweight & Pooling Expansion
*   **Resolved**: Sep.27.17
*   **Root Cause**: High-frequency telemetry propagation paths (1Hz tick evaluation and signaling) were allocating fresh `SystemEvaluationSnapshot`, `TrackerStatus`, and `LocationUpdate` objects every second. This created significant GC pressure and memory fragmentation, especially on budget Android hardware.
*   **Remediation**:
    *   **EngineModels.kt & LocationUpdate.kt**: Converted core telemetry DTOs into mutable flyweights by making fields `var` and adding `reset()` and `copyFrom()` methods.
    *   **TelemetryMapper.kt**: Refactored mapping logic to accept "out" parameters, enabling zero-allocation transformation between engine and app DTOs.
    *   **MonitorService.kt**: Introduced private flyweight instances for the tick loop, coordinate processing, and alarm evaluation.
    *   **AppEventCoordinator.kt & ConnectivitySuite.kt**: Migrated event handling and packet processing to use reusable pooled instances, eliminating allocations in the steady-state signaling path.
*   **SOT ID**: 517 (Flyweight & Pooling Expansion)

## 🏁 Issue #1173: Protobuf-First Persistence
*   **Resolved**: Sep.27.16
*   **Root Cause**: High-frequency alarm state persistence was utilizing `org.json` serialization, leading to repeated string allocations and parsing overhead in the core evaluation loop. This increased GC pressure and disk I/O latency.
*   **Remediation**:
    *   **app_settings.proto**: Added `ActiveAlarmProto` and a role-indexed map (`role_alarms`) to the DataStore schema.
    *   **SettingsMapper.kt**: Implemented direct binary mapping between `ActiveAlarm` domain models and Protobuf messages.
    *   **SettingsRepository.kt**: Refactored to support binary list operations and role-based alarm isolation.
    *   **AppAlarmManager.kt**: Completely removed `org.json` dependencies, migrating to reactive binary persistence for the active alarm registry.
*   **SOT ID**: 516 (Protobuf-First Persistence)

## 🏁 Issue #1352: Unified Service Job Orchestration
*   **Resolved**: Sep.27.15
*   **Root Cause**: While periodic loops were managed by `TickOrchestrator`, other background jobs (GPS collection, settings observation, FGS updates, alarm evaluation) were managed via fragmented `Job?` variables in `MonitorService`. This inconsistency risked lifecycle leaks, race conditions during role transitions, and complicated teardown logic.
*   **Remediation**:
    *   **TickOrchestrator.kt**: Enhanced the orchestrator with `launchJob` and `cancelJob` to manage any lifecycle-bound coroutine, maintaining a unified registry of active tasks.
    *   **MonitorService.kt**: Refactored all background tasks—including `gps_collection`, `gnss_detail`, `service_observers`, `fgs_update`, and `alarm_evaluation`—to use `TickOrchestrator`.
    *   **Lifecycle Integrity**: Ensured all background jobs are gated by the service's initialization state and atomically cancelled during `onDestroy` or role transitions via `tickOrchestrator.cancelAll()`.
*   **SOT ID**: 515 (Unified Service Job Orchestration)

... (Earlier entries)
