# Simplification Ideas 2 (Sep.02.68)

## 💡 Active Ideas
*   **Idea #239: Signaling Interface Consolidation**. Now that the telemetry models are unified (#238), the `SignalingProvider` can be simplified by removing redundant `emitMap` and `emitBinary` overloads in favor of a single unified `transmit(Telemetry)` entry point.
*   **Idea #240: ContextShadow Automation**. Explore using a Hilt provider to automatically inject the `ContextShadow` into all services, removing the manual delegation boilerplate in `ConnectivitySuite` and others.
*   **Idea #241: Protobuf Mapping Unification**. Consolidate the `writeTo(RealtimeStatus.Builder)` and `writeTo(TrackerStatusProto.Builder)` logic in `TrackerStatus` into a shared mapping utility to ensure field parity is maintained automatically (Issue #180).
*   **Idea #242: Redundant Logic Loop Pruning**. Consolidate the tick frequency calculations between `TrackerService` and `ServiceBehaviorUseCase` to reduce redundant state evaluations and improve CPU efficiency on A15 hardware.

## ✅ Implemented / Rejected
*   **Idea #243: UI State Flattening for StatusBar**. Flattened the parameter chain by consuming a unified `HudState` object. (Implemented Sep.02.68).
*   **Idea #238: Model Unification**. (Implemented Sep.03.02).
*   **Idea #197: Teardown Auditing**. (Implemented Sep.03.02).
