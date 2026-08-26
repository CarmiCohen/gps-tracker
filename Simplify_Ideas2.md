# Simplicity Ideas (Aug.26.17)

## 🏗️ Architectural Simplification
1.  **Unified Issue Model**: Replace separate boolean flags for system issues in `UiStateAggregator` with a `List<SystemIssue>` to ensure counter-to-UI parity (Ref: Concern #740).
2.  **Map Lazy-Loading**: Defer OsmDroid tile source initialization until after the first frame is rendered to reduce A15 hydration pressure (Ref: Concern #739).
3.  **EventQueue Auto-Dispose**: Implement a scoped lifecycle observer for `BaseEventQueue` to prevent manual disposal failures (Ref: Concern #738).
4.  **Hydration Component Abstraction**: Create a higher-order `StaggeredHydrator` component to encapsulate the delay/IdleHandler logic for all heavy UI components, removing the manual `hydrationLevel` gating from screen-level code (Ref: Issue #739).
5.  **Hardware Lifecycle Delegate**: Use a dedicated `HardwareLifecycleObserver` to manage start/stop transitions for all hardware managers (Sensors, GPS) in a unified way, reducing synchronization boilerplate and ensuring atomic disposal (Ref: Issue #738).
