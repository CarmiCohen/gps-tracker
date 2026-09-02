# Simplicity Ideas 2 (Sep.02.44)

*   **Idea #238**: Unify `LocationUpdate` (Engine) and `LocationState` (UI). Currently, `TelemetryUseCase` manually maps ~50 fields between these structures. Merging them into a single `@Serializable` class used by both layers would eliminate the mapping boilerplate and reduce allocation churn during high-frequency GPS ticks (R3.1).
*   **Idea #239**: [RESOLVED Sep.02.42] Consolidate `localLocation` and `trackerLocation` into a single `PeerGroup` flow in `MainRepository`.
*   **Idea #240**: [RESOLVED Sep.02.43] Centralize `ContextShadow` instantiation in a Dagger/Hilt module.
*   **Idea #241**: Centralize `ManagedHardware` unregistration timeouts (4000ms) and settling windows (800ms) into a `HardwarePolicy` object to ensure architectural consistency across all future hardware integrations (Sep.02.44).
