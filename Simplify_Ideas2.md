# Simplification Ideas (Sep.16.13)

## 💡 Architecture & Code De-cluttering
1. **Power & Hardware Provider Convergence**: `UnifiedPowerPolicy` and `HardwareProvider` (and its implementation) share similar responsibilities regarding platform state monitoring. Merging these into a single `HardwareSuite` or similar could reduce dependency injection overhead.
2. **Connectivity Event Bus**: `ConnectivitySuite` uses a `SharedFlow` for internal events. As the number of events grows, consider a more structured `EventBus` or specialized observers to keep the suite's main logic focused on signaling.
3. **Telemetry Mapping**: The telemetry mapping between `TrackerStatus`, `PendingStatusEntity`, and Protobuf models is becoming verbose. Centralizing this into a single, highly-optimized transformer could simplify the `ConnectivitySuite` flush logic.
