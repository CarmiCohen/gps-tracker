# Architectural Simplification Ideas (Aug.21.06)

Following the implementation of range-based forensic deduplication and UI hydration optimizations, here are remaining recommendations:

## 1. UI State Aggregation (Aug.20.10)
- **Problem**: The `combine` blocks in `MainViewModel.kt` for `dashboardState` and `hudState` are reaching the 7-parameter limit.
- **Simplification**: Extract state aggregation logic into a dedicated `UiStateAggregator` service.

## 2. HUD Parameter Decoupling (Aug.20.10)
- **Simplification**: Further flatten the `StatusBar` hierarchy so child components consume sub-sections of `HudState` directly.

## 3. Dependency Management (Aug.20.10)
- **Simplification**: Move common dependencies into a `Version Catalog` (libs.versions.toml) for consistency across modules.

## 4. Unified Event Bus Reduction
- **Simplification**: Consolidate `UiEvent`, `UiCommand`, and `CommandEvent` into a single reactive `SystemEvent` stream.

## 5. ShadowCache Standardisation
- **Simplification**: Refactor all high-frequency lookups to use the centralized `ShadowCache` to eliminate custom synchronization logic.
