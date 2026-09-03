# Simplification Ideas - Session Sep.03.120

## 💡 Architectural Simplification Ideas
1.  **Context Authority Consolidation**: The persistent `getPackageName` spam suggests that the `ShadowContext` / `ContextShadow` delegation is either being bypassed by system frameworks or is overly complex. Idea: Explore a bytecode-level transformer to redirect all `getPackageName` calls to a static constant, rather than relying on manual Hilt injection wrappers.
2.  **Service Start Orchestration**: To address `BackgroundServiceStartNotAllowedException` on Android 14+, simplify the `BootWorker` / `MaintenanceWorker` logic by moving to a "Work-First" model where the `WorkManager` task performs the initial sync/poke, and only attempts to start a Foreground Service if a high-priority event is detected, rather than always attempting FGS start on boot.
3.  **Signaling Handshake UI**: Simplify the `StatusRow` components by merging "SRV" and "GPS" into a single "Connectivity" state if the relay is down, reducing UI noise during the initial handshake phase.
4.  **Telemetry Mapping**: Prune `RealtimeStatus` (Protobuf) to only include active deltas, simplifying the `handleBinaryUpdate` logic in `ConnectivitySuite`.
