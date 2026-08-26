# Simplicity Ideas (Aug.26.14)

## 🏗️ Architectural Simplification
1.  **Unified Issue Model**: Replace separate boolean flags for system issues in `UiStateAggregator` with a `List<SystemIssue>` to ensure counter-to-UI parity and simplify rendering in `PhoneSetupOverlay` (Ref: Concern #740).
2.  **Map Lazy-Loading**: Defer OsmDroid tile source initialization until after the first frame is rendered and the camera position is stable to reduce A15 hydration pressure (Ref: Concern #739).
3.  **EventQueue Auto-Dispose**: Implement a scoped lifecycle observer for `BaseEventQueue` within the core engine to prevent manual disposal failures (Ref: Concern #738).
