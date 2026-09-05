# Simplification Ideas (Sep.05.26)

*   **Threshold Centralization**: Move GPU/Mali detection thresholds from `IntegrityMonitor.kt` to a centralized `HardwareConstants` file to allow for easier hardware-specific tuning.
*   **Animation Abstraction**: Create a `ThrottledAnimation` CompositionLocal or a custom Modifier to handle animation suppression globally based on `hudState.isMaliAnomaly`, removing the need to pass `isThrottled` flags through multiple layers of the UI component tree.
*   **Unified Anomaly Flow**: Consolidate `isThermalThrottling`, `isMaliAnomaly`, and `isSilentFailure` into a single `PerformanceProfile` bitmask in `SystemHealthState` to simplify aggregation logic.
*   **Signaling Configuration Decoupling**: Move `io.socket.client.IO.Options` construction logic into a dedicated `SignalingConfigurationFactory` to keep `CommunicationManager` focused strictly on event routing and lifecycle management.
