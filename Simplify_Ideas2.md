# Simplification Ideas - Session Sep.04.40

## 💡 Architectural Simplification Ideas
1.  **Context Authority Consolidation**: The persistent `getPackageName` spam suggests that the `ShadowContext` / `ContextShadow` delegation is either being bypassed by system frameworks or is overly complex. Idea: Explore a bytecode-level transformer to redirect all `getPackageName` calls to a static constant.
2.  **Service Start Orchestration**: To address `BackgroundServiceStartNotAllowedException` on Android 14+, simplify the `BootWorker` / `MaintenanceWorker` logic by moving to a "Work-First" model where the `WorkManager` task performs the initial sync/poke.
3.  **Signaling Handshake UI**: Simplify the `StatusRow` components by merging "SRV" and "GPS" into a single "Connectivity" state if the relay is down, reducing UI noise during the initial handshake phase.
4.  **Telemetry Mapping**: Prune `RealtimeStatus` (Protobuf) to only include active deltas, simplifying the `handleBinaryUpdate` logic in `ConnectivitySuite`.
5.  **Signaling Provider Abstraction**: Simplify `CommunicationManager` by delegating transport-specific configuration to a `SignalingTransportFactory`, reducing the risk of hard-coded transport regressions like #906.
6.  **Unified Identity Aliasing**: Move ID aliasing logic (T -> Trk) directly into a `TransmissionIdentity` value class or the `TrackerStatus` domain model itself to eliminate manual mapping during serialization (R907).
7.  **Hardware Lifecycle State Machine**: (New) Replace the manual `AtomicBoolean` flags and `synchronized` blocks in `HardwareProvider` with a formal state machine (using a Sealed Class or Enum). This would make transitions between `STARTING`, `STARTED`, `STOPPING`, and `IDLE` deterministic and eliminate the complex race-condition checks during rapid hydration restarts (R908).
