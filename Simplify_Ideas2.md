# Simplification Ideas (Sep.17.01)

## 💡 Architecture & Code De-cluttering
1. **Power & Hardware Provider Convergence**: `UnifiedPowerPolicy` and `HardwareProvider` (and its implementation) share similar responsibilities regarding platform state monitoring. Merging these into a single `HardwareSuite` or similar could reduce dependency injection overhead.
2. **Connectivity Event Bus**: `ConnectivitySuite` uses a `SharedFlow` for internal events. As the number of events grows, consider a more structured `EventBus` or specialized observers to keep the suite's main logic focused on signaling.
3. **Telemetry Mapping**: The telemetry mapping between `TrackerStatus`, `PendingStatusEntity`, and Protobuf models is becoming verbose. Centralizing this into a single, highly-optimized transformer could simplify the `ConnectivitySuite` flush logic.
4. **Event Flow Routing Convergence**: The event mapping inside `MainViewModel.onEvent` has unified many domain event streams, but it contains a long `when` condition. Separating individual module controllers or use case pipelines would prevent future file growth and bloat.
5. **Backfill Buffer Reuse**: In `HistoryManager.backfillAnalyticalGaps`, an `ArrayList` is created per call. While the size is small (60), using a reusable pool or a pre-allocated buffer for backfill results could further reduce transient allocations during recovery bursts.
