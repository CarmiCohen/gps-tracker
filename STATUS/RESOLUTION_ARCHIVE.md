# 🏛️ Resolution Archive - Sep.26.8

## 🏁 Issue #1215: Decommission legacy ViewModel artifacts
*   **Resolved**: Sep.26.8
*   **Root Cause**: Architectural technical debt resulting from feature-specific ViewModels (`SetupViewModel`, `TrackerViewModel`, `ViewerViewModel`) that were rendered redundant after the consolidation of all UI state and domain routing into the activity-scoped `MainViewModel` SSOT (Issue #1203).
*   **Remediation**:
    *   **Decommissioning**: Physically wiped and decommissioned `SetupViewModel.kt`, `TrackerViewModel.kt`, and `ViewerViewModel.kt`, leaving only legacy markers.
    *   **Verification**: Confirmed `MainAppContent.kt` and all screens now correctly consume `MainViewModel` without residual dependency on feature ViewModels.
    *   **Architecture**: Codified Rule 1.22 in SOT Master Requirements to prohibit feature-specific ViewModels in favor of the SSOT.
*   **R-ID**: 496

# 🏛️ Resolution Archive - Sep.26.6

## 🏁 Issue #1337: Role-Prefix Collision in Integrity Events
*   **Resolved**: Sep.26.6
*   **Root Cause**: Local integrity events (such as battery low) on a Viewer device were incorrectly processed or had the potential to leak state into the remote tracker's evaluation path via `AppAlarmManager` due to reference prefix ambiguity.
*   **Remediation**:
    *   **AppEventCoordinator.kt**: Restricted `setPowerAlarmPending` updates to Tracker mode only to completely decouple local Viewer device power changes from remote tracker state evaluation.
    *   **AppAlarmManager.kt**: Added strict role-prefix filtering and validation inside `setPowerAlarmPending` and `resetEvaluation` to block role-prefix flipping or invalid write collisions.
*   **R-ID**: 493

# 🏛️ Resolution Archive - Sep.26.4

## 🏁 Issue #1335: Initialization Prefix Unification
*   **Resolved**: Sep.26.4
*   **Root Cause**: `MonitorService.loadLogicState` contained explicit `isTrackerMode` code branching during restoration of the `primaryProcessor` state. This created architectural asymmetry, adding unneeded initialization branches and risking state restoration failures for self-tracking data under changing roles.
*   **Remediation**:
    *   **MonitorService.kt**: Refactored `loadLogicState` to use the role-agnostic `rolePrefix` for all `primaryProcessor` state restoration calls uniformly across both Tracker and Viewer roles. Strictly reserved the `"VR_"` prefix for the remote monitoring `remoteProcessor` component.
*   **R-ID**: 491

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
*   **Root Cause**: Event congestion during peak activity bursts.
*   **Remediation**: Hardened `DomainEventBus` with an extra buffer capacity of 128 and drop oldest overflow policy.
*   **R-ID**: 481
