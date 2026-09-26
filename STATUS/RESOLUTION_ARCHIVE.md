# 🏛️ Resolution Archive - Sep.26.3

## 🏁 Issue #1334: Unified GPS Pipeline Hardening & Forensic Audit Integration
*   **Resolved**: Sep.26.3
*   **Root Cause**: 
    1.  **Coordinate Duplication Typo**: `LocationProcessor.loadState` was initializing the spatial anchor with duplicate latitudes (`lat, lat` instead of `lat, lng`), corrupting the filter state on restart.
    2.  **Temporal Inconsistency**: `MonitorService` was tracking `lastGpsTs` using mixed time domains (Realtime vs Wall-clock), which caused intermittent failures in GPS stall detection.
    3.  **Missing Audit Point**: High-frequency GPS bursts in the unified `locationBuffer` were bypassing the `ForensicAuditor.recordGpsFix` stability audit.
*   **Remediation**:
    *   **LocationProcessor.kt**: Corrected the `setSpatialAnchor` call to use both `lat` and `lng`.
    *   **MonitorService.kt**: Standardized `lastGpsTs` tracking to wall-clock time (`loc.time`) for all roles and integrated `forensicAuditor.recordGpsFix` into the unified burst processing loop.
    *   **Unit Tests**: Aligned 42 unit tests with the updated `LocationProcessingState` and `SystemEvaluationSnapshot` pipeline signatures.
*   **R-ID**: 490

# 🏛️ Resolution Archive - Sep.26.2

## 🏁 Issue #1333: Peer Connection State Caching
*   **Resolved**: Sep.26.2
*   **Root Cause**: The system was logging `PEER LIFECYCLE` events every time a peer pulse was received, regardless of whether the connection state actually changed. This resulted in significant log noise and increased write overhead during active signaling sessions.
*   **Remediation**:
    *   **AppEventCoordinator.kt**: Introduced a `ConcurrentHashMap` (`peerConnectionCache`) to track the last known connection state of each remote peer.
    *   **Logic**: Updated `handlePeerConnectionChanged` to suppress logging if the incoming state matches the cached state.
    *   **Lifecycle**: Added cache clearing to the `ResetTimers` command handler to ensure clean state transitions across session boundaries.
*   **R-ID**: 489

# 🏛️ Resolution Archive - Sep.26.1

## 🏁 Issue #1332: Viewer Self-Tracking Snapshot Optimization
*   **Resolved**: Sep.26.1
*   **Root Cause**: The Viewer role's self-tracking followed a separate, imperative code path compared to the Tracker role. This caused architectural asymmetry and required a redundant `ViewerLocationUpdated` event, increasing complexity in the DomainEventBus and coordinator.
*   **Remediation**:
    *   **MonitorService.kt**: Unified the GPS processing pipeline. Both roles now buffer incoming fixes in `locationBuffer` and process them during the primary `processTick` cycle.
    *   **EngineModels.kt**: Removed the redundant `ViewerLocationUpdated` event from the `DomainEvent` hierarchy.
    *   **AppEventCoordinator.kt**: Centralized local telemetry persistence within `handleTickEvaluated`, making it role-agnostic.
*   **R-ID**: 488

# 🏛️ Resolution Archive - Sep.26.0

## 🏁 Issue #1314: TrackerStatus & Evaluation Snapshot Convergence
*   **Resolved**: Sep.26.0
*   **Root Cause**: Transitioning from engine evaluation to remote signaling required a heavy mapping layer (`mapSnapshotToStatus`) with over a dozen parameters. This was because forensic indexes (noise, lux, etc.) were calculated and passed as event metadata rather than being part of the primary state snapshot.
*   **Remediation**:
    *   **MonitorService.kt**: Refactored the evaluation loop to calculate and populate forensic indexes directly into the `SystemEvaluationSnapshot` partitioned states before event emission.
    *   **EngineModels.kt**: Simplified the `TickEvaluated` event by removing redundant metadata fields.
    *   **TelemetryMapper.kt**: Optimized `mapSnapshotToStatus` to leverage the pre-populated snapshot, drastically reducing mapping overhead and parameter passing.
*   **R-ID**: 487

# 🏛️ Resolution Archive - Sep.25.07

## 🏁 Issue #1329: Telemetry Mapping Convergence
*   **Resolved**: Sep.25.07
*   **Root Cause**: `LocationUpdate` construction was fragmented across `AppEventCoordinator` handlers for `TickEvaluated` and `ViewerLocationUpdated`. This resulted in redundant mapping logic, inconsistencies in how `TrackerState` was derived (speed vs. filtered speed), and increased technical debt.
*   **Remediation**:
    *   **TelemetryMapper.kt**: Introduced `mapSnapshotToUpdate` as the central authority for converting a `SystemEvaluationSnapshot` and `ProcessedLocation` into a `LocationUpdate`.
    *   **AppEventCoordinator.kt**: Refactored `handleTickEvaluated` and `handleViewerLocationUpdated` to utilize the centralized mapper, eliminating ~15 lines of manual property assignment and ensuring role-agnostic mapping consistency.
*   **R-ID**: 486

# 🏛️ Resolution Archive - Sep.25.06

## 🏁 Issue #1330: Snap-to-Update Monolith
*   **Resolved**: Sep.25.06
*   **Root Cause**: `SystemEvaluationSnapshot` and `LocationUpdate` had duplicate field structures but were functionally decoupled, forcing `AppEventCoordinator` to perform heavy manual field-to-field mapping (~100 assignments) per background pulse. This introduced significant allocation churn and maintenance overhead.
*   **Remediation**:
    *   **EngineModels.kt**: Refactored `SystemEvaluationSnapshot` to consume the partitioned state structures (`KineticState`, `AtmosphericState`, `IntegrityState`) shared with `LocationUpdate`.
    *   **LocationUpdate.kt**: Hardened `copyFrom` methods for sub-states to enable zero-allocation double-buffering in the repository.
    *   **MonitorService.kt**: Aligned the core evaluation loop with the partitioned DTO structure.
    *   **AppEventCoordinator.kt**: Eliminated the manual mapping layer; updates are now passed directly via snapshot partitions.
*   **R-ID**: 485

# 🏛️ Resolution Archive - Sep.25.05

## 🏁 Issue #1327: Pulse-to-Tick Event Collision
*   **Resolved**: Sep.25.05
*   **Root Cause**: Peer heartbeat handlers in `MonitorService` were reusing the `DomainEvent.TickEvaluated` event to signal connectivity updates. This triggered the full telemetry processing and persistence pipeline (intended for local sensor ticks), leading to redundant repository writes, stale ribbon updates, and unnecessary signaling overhead for every peer pulse.
*   **Remediation**:
    *   **EngineModels.kt**: Introduced `DomainEvent.PeerConnectionChanged(isConnected: Boolean, peerId: String)` to the unified event hierarchy.
    *   **MonitorService.kt**: Replaced redundant `TickEvaluated` emissions in `handleTrackerPulse` and `handleViewerPulse` with the new lifecycle-only event.
    *   **AppEventCoordinator.kt**: Implemented a reactive handler for `PeerConnectionChanged` to log peer lifecycle events without triggering telemetry side-effects.
*   **R-ID**: 484

# 🏛️ Resolution Archive - Sep.25.04

## 🏁 Issue #1324: Peer Signaling Coupling to Repository
*   **Resolved**: Sep.25.04
*   **Root Cause**: `ConnectivitySuite` was executing direct imperative repository writes for received peer telemetry packets. This created tight coupling between the signaling layer and persistence layer, potentially blocking the network processing thread with database I/O and creating architectural inconsistency with the bus-centric model used for local tracking.
*   **Remediation**:
    *   **EngineModels.kt**: Added `DomainEvent.PeerStatusReceived(status: LocationUpdate)` to the unified event hierarchy.
    *   **ConnectivitySuite.kt**: Refactored both binary (Protobuf) and JSON signaling handlers to emit `PeerStatusReceived` to the `DomainEventBus` instead of calling `mainRepository.updateLocation` directly.
    *   **AppEventCoordinator.kt**: Implemented a reactive handler for `PeerStatusReceived` that persists the peer update to the repository asynchronously.
*   **R-ID**: 483

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
