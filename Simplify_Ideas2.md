# Simplification Ideas (Sep.11.10)

## 💡 Idea #243: Map UI Consolidation & Delegation
**Status**: RESOLVED (Sep.11.10)
**Description**: Following the introduction of `MapViewState`, the `TrackerScreen` and `ViewerScreen` still manually managed map tools and passed ~40 parameters. By delegating all map-related UI (buttons, tools, state triggers) strictly to `AppMapContainer`, we reduce screen complexity and ensure the map subsystem is self-contained.
**Resolution**: Eliminated redundant UI overlays and simplified screen signatures. Optimized `MainViewModel` flow triggers using `MapUiParts` segmentation (R-ID 287).

## 💡 Idea #241: HudState Aggregator Refactoring
**Status**: RESOLVED (Sep.10.08)
**Description**: Transitioned to segmented emissions (`HudConnectivityState`, `HudTelemetryState`, `HudHealthState`) to reduce recomposition scope.
**Resolution**: Removed `HudState` facade; UI components now subscribe to specific sub-states directly (R-ID 286).

## 💡 Idea #242: Unified Termination Logic
**Status**: RESOLVED (Sep.10.06)
**Description**: Centralized session termination into a single component to ensure visual consistency across modes.
**Resolution**: Implemented `SessionTerminationButton` in `SharedUiComponents.kt` (R-ID 285).
