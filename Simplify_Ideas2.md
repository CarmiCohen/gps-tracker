# Simplification Ideas (Sep.10.00)

## 💡 Idea #241: HudState Aggregator Refactoring
**Status**: PROPOSED
**Description**: The `HudState` data class in `EngineModels.kt` currently serves as a monolithic facade for legacy Compose compatibility. As the aggregator transitions to segmented emissions (`HudConnectivityState`, `HudTelemetryState`, `HudHealthState`), we should remove the top-level `HudState` and have the UI components subscribe to the specific sub-states directly. This will reduce recomposition scope and simplify the `MainViewModel` logic.

## 💡 Idea #242: Unified Termination Logic
**Status**: PROPOSED
**Description**: Currently, session termination is handled slightly differently in `TrackerScreen` vs `ViewerScreen` (button labels, confirmation flows). Centralizing this into a single `SessionTerminationComponent` within `SharedUiComponents.kt` would reduce boilerplate and ensure visual consistency across modes.
