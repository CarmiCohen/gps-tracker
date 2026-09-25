# 🏛️ Resolution Archive - Sep.25.03

## 🏁 Issue #1323: Residual Imperative Persistence in MonitorService
*   **Resolved**: Sep.25.03
*   **Root Cause**: The Viewer role's self-tracking updates were being persisted via an imperative call to `updateRepositoryLocation` within `MonitorService.onLocationChanged`. This bypassed the reactive `DomainEventBus` architecture used by the Tracker role, creating architectural asymmetry and risking blocking the service thread with database I/O.
*   **Remediation**:
    *   **EngineModels.kt**: Added `DomainEvent.ViewerLocationUpdated` to the core event hierarchy.
    *   **AppEventCoordinator.kt**: Implemented a reactive handler for `ViewerLocationUpdated` that offloads repository persistence to the coordination layer.
    *   **MonitorService.kt**: Removed the imperative `updateRepositoryLocation` method and updated `onLocationChanged` to emit the new event to the bus.
*   **R-ID**: 482

# 🏛️ Resolution Archive - Sep.25.02

## 🏁 Issue #1331: DomainEventBus Capacity Hardening
*   **Resolved**: Sep.25.02
*   **Root Cause**: As the unified signaling backbone for the entire system, the `DomainEventBus` was initially configured with a default buffer capacity. With the convergence of 10+ subsystem flows into a single reactive stream, there was a potential risk of event congestion during high-frequency telemetry ticks, which could lead to backpressure or blocking in the core evaluation loop.
*   **Remediation**:
    *   **DomainEventBus.kt**: Increased `extraBufferCapacity` to 128 to accommodate peak event bursts.
    *   **Hardening**: Implemented `onBufferOverflow = BufferOverflow.DROP_OLDEST` to strictly guarantee that the core `MonitorService` tick loop remains non-blocking even if side-effect consumers (logging, persistence, UI) fall behind.
*   **R-ID**: 481

# 🏛️ Resolution Archive - Sep.25.01

## 🏁 Issue #1322: Multi-Flow Fragmentation Convergence
*   **Resolved**: Sep.25.01
*   **Root Cause**: `AppEventCoordinator` observed 10+ fragmented reactive flows (`IntegrityEvent`, `ProcessorEvent`, etc.), leading to tight coupling between the background evaluation loop and side-effect handlers (logging, persistence, signaling). This created high architectural complexity and hindered module-level unit testing.
*   **Remediation**:
    *   **DomainEventBus**: Migrated to the `:core:engine` module to support core-level signaling without circular dependencies.
    *   **EngineModels.kt**: Centralized all component-level event definitions (`Alarm`, `Integrity`, `Processor`, `Connectivity`, `History`, `Sensor`, `Command`, `Revival`) into a unified `DomainEvent` hierarchy.
    *   **MonitorService.kt**: Injected the global bus into `LocationProcessor` instances and aligned orchestration initialization.
    *   **AppEventCoordinator.kt**: Refactored to a strictly bus-centric model, observing a single `domainEventBus.events` stream to manage all system reactions.
    *   **Component Hardening**: Refactored `AppAlarmManager`, `IntegrityMonitor`, `HardwareSuite`, `ConnectivitySuite`, `HistoryManager`, and `CommandRouter` to emit wrapped events directly to the centralized bus.
*   **R-ID**: 480

# 🏛️ Resolution Archive - Sep.25.00

## 🏁 Issue #1326: Telemetry Data Corruption in LocationProcessor
*   **Resolved**: Sep.25.00
*   **Root Cause**: The `satsUsed` field in `processGpsPoint` was mapped to a zero-placeholder, which was inadvertently propagated into the tracking engine's sentinel logic. This caused incorrect signal quality scoring and corrupted satellite count history logs.
*   **Remediation**:
    *   **LocationProcessor.kt**: Updated `processGpsPoint` to source `satsUsed` directly from the unified `SystemEvaluationSnapshot`.
    *   **Data Integrity**: Eliminated the legacy placeholder, ensuring that the tracking sentinel receives actual hardware satellite counts for validation.
*   **R-ID**: 479

## 🏁 Issue #1325: Unified Snapshot Metadata Gaps
*   **Resolved**: Sep.25.00
*   **Root Cause**: Following the unification of evaluation logic in Sep.24.96, the `SystemEvaluationSnapshot` lacked several metadata fields (satellite counts, proximity indices, vibration rolling sums, and violation uptime statistics) required for downstream signaling and persistence. This forced components to observe fragmented side-channel flows.
*   **Remediation**:
    *   **EngineModels.kt**: Expanded `SystemEvaluationSnapshot` to include all metadata fields required for a full `LocationUpdate`.
    *   **MonitorService.kt**: Refactored the core evaluation loop to populate these fields from the `HardwareSuite` and `SessionManager` state.
    *   **AppEventCoordinator.kt**: Updated mapping logic to utilize the snapshot as the authoritative source for persistence and peer signaling, removing reliance on redundant event metadata.
*   **R-ID**: 478
