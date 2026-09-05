# Simplification Ideas - Master List (Sep.05.24)

## 💡 Architectural & System Streamlining
1.  **LED Logic Centralization**: Move LED status evaluations (INT, SRV, GPS, VWR, DAT) from `DashboardStateProviderImpl` into a dedicated `LedStateEvaluator` in `:core:engine` to ensure parity between HUD and Notifications.
2.  **Hydration State Consolidation**: Merge the 11-level hydration process into 3 phases: `Pre-flight`, `Engine Warmup`, and `Active` to reduce transient state race conditions.
3.  **Navigation Guard Decorator**: Implement a custom `NavHost` wrapper to enforce `isSystemActive` invariants globally for the `Landing` route.
4.  **Forensic Log Pruning**: Move `MainActivity` stack trace capture to a `ForensicManager` utility to keep the UI layer clean.
5.  **Role-Agnostic Signaling Pipeline**: Unify binary telemetry reception for both Tracker and Viewer modes, removing mode-specific guards.
6.  **Consolidated Hardware Lifecycle Management**: Unify `JdHardwareManager` (poke) and `HardwareProvider` (revival) into a single `HardwarePersistenceManager`.
7.  **Dedicated GNSS Coordinator**: Extract recovery state machines from `HardwareProvider` into a `GnssRevivalCoordinator` (R-ID 260).
8.  **Context Authority Consolidation**: Replace the complex `ContextShadow` delegation with a bytecode-level transformer to redirect `getPackageName` calls to a static constant.
9.  **Service Start Orchestration**: Move to a "Work-First" model where `WorkManager` handles initial sync, only starting FGS if high-priority triggers are detected.
10. **Signaling Handshake UI**: Merge "SRV" and "GPS" HUD badges into a single "Connectivity" indicator during initial handshake phases to reduce UI noise.
11. **Telemetry Mapping**: Prune `RealtimeStatus` (Protobuf) to include only active deltas, simplifying `ConnectivitySuite` updates.
12. **Signaling Provider Abstraction**: Implement a `SignalingTransportFactory` to decouple the core manager from transport-specific (Socket.io) configuration.
13. **Unified Identity Aliasing**: Move ID aliasing logic (T -> Trk) into a `TransmissionIdentity` value class to prevent protocol mismatches (R907).
14. **Hardware Lifecycle State Machine**: Replace manual `AtomicBoolean` flags in `HardwareProvider` with a formal Sealed Class state machine (`STARTING`, `STARTED`, `STOPPING`, `IDLE`).
15. **Watchdog Unification**: Consolidate the `Hydration Watchdog` and the `System Watchdog` into a single `LifecycleWatchdog` to reduce coroutine overhead in `MainViewModel`.

---
**New Ideas Total: 15**
*(Updated Sep.05.24)*
