# Forensic Resumption Snapshot - Sep.25.05

## 📂 Session Summary
*   **Completed**:
    *   **Issue #1327**: Pulse-to-Tick Event Collision. Introduced `DomainEvent.PeerConnectionChanged` for lifecycle-only peer notifications. Replaced redundant `TickEvaluated` emissions in heartbeat pulse handlers to eliminate unnecessary side-effects and stale telemetry propagation.
    *   **Architecture Hardening**: Decoupled connection statuses from high-frequency core telemetry evaluations, reinforcing the architectural boundary between network states and logic evaluation.
*   **Version**: Sep.25.05
*   **Status**: Peer lifecycle updates are isolated. The system strictly follows a non-blocking connection event propagation pattern.

## 🔧 Technical Delta
*   **EngineModels.kt**: Added `DomainEvent.PeerConnectionChanged(isConnected: Boolean, peerId: String)`.
*   **MonitorService.kt**: Refactored `handleTrackerPulse` and `handleViewerPulse` to emit lightweight `PeerConnectionChanged` instead of `TickEvaluated`.
*   **AppEventCoordinator.kt**: Implemented connection event handler for lifecycle tracking.
*   **app/build.gradle**: Incremented version to `Sep.25.05`.

## 📍 Resumption Point for Next Session
*   **Immediate Priority**: Progress with Issue #1330 (Snap-to-Update Monolith) to eliminate the bridge layer between snapshots and location updates.
*   **Strategic Goal**: Consolidate `LocationUpdate` construction into a single `TelemetryMapper` (Issue #1329) to clean up coordinate orchestration.

## 📊 Audit Baseline
**Current Audit Baseline: [SOT: 484 (Rules: 25, IDs: 484), Resolved: 1227, Open: 0, Testing: 3 (Sub-items: 12), Ideas: 21, QA: 284]**
